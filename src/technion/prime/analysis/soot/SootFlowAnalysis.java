package technion.prime.analysis.soot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import soot.Local;
import soot.ResolutionFailedException;
import soot.Scene;
import soot.SootField;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Unit;
import soot.Value;
import soot.jimple.CastExpr;
import soot.jimple.FieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.ParameterRef;
import soot.jimple.Ref;
import soot.jimple.ReturnStmt;
import soot.jimple.internal.AbstractDefinitionStmt;
import soot.jimple.internal.AbstractInstanceInvokeExpr;
import soot.jimple.internal.AbstractNewExpr;
import soot.jimple.internal.JInvokeStmt;
import soot.tagkit.AnnotationTag;
import soot.tagkit.Host;
import soot.tagkit.Tag;
import soot.tagkit.VisibilityAnnotationTag;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

import technion.prime.utils.Logger;
import technion.prime.analysis.ProgramState;
import technion.prime.analysis.Label;
import technion.prime.Options;
import technion.prime.analysis.MethodAnalyzer;
import technion.prime.dom.AppAnnotation;
import technion.prime.dom.AppMethodDecl;
import technion.prime.dom.AppMethodRef;
import technion.prime.dom.AppObject;
import technion.prime.dom.AppType;
import technion.prime.dom.TypeOnlyAppAnnotation;
import technion.prime.dom.dummy.DummyAppType;
import technion.prime.dom.soot.SootAppMethodDecl;
import technion.prime.dom.soot.SootAppMethodRef;
import technion.prime.dom.soot.SootAppObject;
import technion.prime.dom.soot.SootAppType;
import technion.prime.utils.ConcurrencyUtils;
import technion.prime.utils.JavaFileUtils;
import technion.prime.utils.Logger.CanceledException;


/**
 * Prime's version of a Soot's forward flow analysis
 */
public class SootFlowAnalysis extends ForwardFlowAnalysis<Unit, ProgramState> {
	
	/**
	 * Used to propagate checked exceptions through methods not declaring them
	 * (and inherited, so can't add them).
	 */
	@SuppressWarnings("serial")
	public static class RuntimeInterruptedException extends RuntimeException {
		public RuntimeInterruptedException(Exception e) {
			super(e);
		}
	}

	private final Scene scene;
	private final ProgramState initialState;
	private final MethodAnalyzer analyzer;
	private final SootMethod method;
	private final Options options;
	private List<AppObject> inputArgs;
	
	/**
	 * @param options Prime options.
	 * @param analyzer The analyzer running this analysis. The analyzer is used for analyzing called method
	 * (for inter-procedural analysis).
	 * @param method The method being analyzed.
	 * @param graph Unit graph for the method being analyzed.
	 * @param scene Soot scene.
	 * @param initialState Initial history collection. May be null, meaning to start with an empty history.
	 */
	public SootFlowAnalysis(Options options, MethodAnalyzer analyzer, SootMethod method, UnitGraph graph, Scene scene, ProgramState initialState) {
		super(graph);
		if (graph == null) throw new NullPointerException("analyzed method body cannot be null");
		
		this.options = options;
		this.analyzer = analyzer;
		this.method = method;
		this.scene = scene;
		this.initialState = initialState;
	}
	
	/**
	 * Analyze a method.
	 * @return The final state at the ends of this method.
	 * @throws InterruptedException 
	 * @throws CanceledException 
	 */
	public ProgramState analyze() throws InterruptedException, CanceledException {
		try {
			doAnalysis();
		} catch (RuntimeInterruptedException e) {
			Exception inner = (Exception) e.getCause();
			Logger.exception(inner);
			if (inner instanceof InterruptedException) throw (InterruptedException)inner;
			if (inner instanceof CanceledException) throw (CanceledException)inner;
			if (inner instanceof RuntimeException) throw (RuntimeException)inner;
			assert(false);
		}
		
		ProgramState finalState = options.newProgramState();
		finalState.setAnalyzedMethod(new SootAppMethodDecl(scene, method));
		for (Unit o : graph.getTails()) {
			finalState.joinFrom(getFlowAfter(o));
		}
		return finalState;
	}
	
	@Override
	protected void flowThrough(ProgramState in, Unit u, ProgramState out) {
		checkInterrupted();
		
		try {
			Label label = createLabel(u);
			out.copyFrom(in);
			
			// Check for method invocation
			if (u instanceof JInvokeStmt) {
				handleMethodCall(label, in, out, ((JInvokeStmt)u).getInvokeExpr());
			}
			
			// Check for assignment
			if (u instanceof AbstractDefinitionStmt) {
				handleAssignment(label, in, out, (AbstractDefinitionStmt)u);
			}
			
			// Return statement, for inter-procedural
			if (u instanceof ReturnStmt) {
				handleReturn(label, in, (ReturnStmt)u);
			}
			
		} catch (Exception e) {
			throw new RuntimeInterruptedException(e);
		}
	}

	private void checkInterrupted() {
		try {
			ConcurrencyUtils.checkState();
		} catch (Exception e) {
			throw new RuntimeInterruptedException(e);
		}
	}
	
	private Label createLabel(Unit u) {
		return new Label(u);
	}

	private void handleAssignment(Label label, ProgramState in, ProgramState out, AbstractDefinitionStmt stmt) throws InterruptedException, CanceledException {
		SootAppObject lhs = new SootAppObject(scene, stmt.getLeftOp(), method); // was with box
		
		if (isInvocation(stmt.getRightOp())) {
			// Handle method-call rhs
			InvokeExpr methodCall = (InvokeExpr) stmt.getRightOp();
			handleAssignmentFromMethodCall(label, in, out, methodCall, lhs);
			return;
		}
		
		// Copy the state before doing the actual assignment.
		out.copyFrom(in);
		Value rhs = getRhs(stmt.getRightOp());
		
		if (isNamableExpr(rhs)) {
			handleAssignmentFromObject(label, out, stmt, lhs, rhs);
		} else if (isNew(rhs)) {
			out.assignmentFromNew(label, lhs);
		} else {
			// If we got here we don't know what the rhs is, but it surely wipes out the lhs.
			out.assignmentFromUntracked(label, lhs);
		}
	}

	private void handleAssignmentFromObject(Label label, ProgramState out,
			AbstractDefinitionStmt stmt, SootAppObject lhs, Value rhs)
					throws InterruptedException, CanceledException {
		if (isParameter(rhs)) {
			if (isKnownParameter(rhs)) {
				out.assignmentFromObject(label, lhs, getKnownParameter(rhs));
			} else {
				out.assignmentFromNewParameter(label, lhs);
			}
		} else {
			SootAppObject namedRhs = new SootAppObject(scene, stmt.getRightOpBox().getValue(), method);
			if (isField(rhs)) {
				List<AppAnnotation> annotations = null;
				SootField field = getField((FieldRef)rhs);
				if (field != null) {
					annotations = getAnnotations(field);
				}
				out.assignmentFromNewField(label, lhs, namedRhs, annotations);
			} else {
				out.assignmentFromObject(label, lhs, namedRhs);
			}
		}
	}

	private boolean isField(Value v) {
		return v instanceof FieldRef;
	}

	private SootField getField(FieldRef f) {
		try {
			return f.getFieldRef().resolve();
		} catch (ResolutionFailedException e) {
			return null;
		}
	}

	private List<AppAnnotation> getAnnotations(Host host) {
		List<AppAnnotation> result = new LinkedList<AppAnnotation>();
		for (Tag t : host.getTags()) {
			if (t instanceof VisibilityAnnotationTag) {
				for (AnnotationTag a : ((VisibilityAnnotationTag) t).getAnnotations()) {
					result.add(new TypeOnlyAppAnnotation(createTypeFromSignature(a.getType())));
				}
			}
		}
		return result;
	}
	
	private AppType createTypeFromSignature(String sig) {
		return new DummyAppType(JavaFileUtils.signature2classname(sig));
	}

	private void handleAssignmentFromMethodCall(Label label, ProgramState in, ProgramState out,
			InvokeExpr methodCall, SootAppObject lhs) throws InterruptedException,
			CanceledException {
		boolean known = handleMethodCall(label, in, out, methodCall);
		if (known) {
			// The method is known - transfer its returned values
			//if (methodCall.getMethodRef().returnType() instanceof VoidType == false)
			if (analyzer.getLastReturned() != null)
				out.assignmentFromAllOf(label, lhs, analyzer.getLastReturned());
		} else if (methodCall instanceof InstanceInvokeExpr) {
			// Method is a phantom
			AppObject receiver = getReceiverObject(methodCall);
			String declaringClassName = receiver.getType().getFullName();
			boolean isApiMethod = options.getFilterReported().passesFilter(declaringClassName);
			AppMethodRef m = getMethodRef(methodCall.getMethodRef(), true, isApiMethod);
			Collection<AppObject> args = getArgumentObjects(methodCall);
			out.assignmentFromPhantomMethod(label, lhs, receiver, m, args);
		}
	}
	
	private boolean isParameter(Value v) {
		return v instanceof ParameterRef;
	}

	private boolean isNew(Value v) {
		return v instanceof AbstractNewExpr;
	}

	private AppObject getKnownParameter(Value v) {
		ParameterRef p = (ParameterRef)v;
		return inputArgs.get(p.getIndex());
	}

	/**
	 * @param v
	 * @return True If the value representing a parameter which is represented in the state.
	 */
	private boolean isKnownParameter(Value v) {
		if (v instanceof ParameterRef == false) return false;
		if (inputArgs == null) return false;
		
		ParameterRef p = (ParameterRef)v;
		int i = p.getIndex();
		if (inputArgs.size() < i - 1) return false;
		assert(inputArgs.get(i).getType().equals(new SootAppType(scene, p.getType())));
		return true;
	}

	private boolean isInvocation(Value v) {
		 return v instanceof InvokeExpr;
	}

	/**
	 * Whether an expression has a name - i.e. it's a local variable or
	 * named reference (parameter / this / field etc.).
	 */
	private boolean isNamableExpr(Value v) {
		return v instanceof Ref || v instanceof Local;
	}

	private Value getRhs(Value v) {
		// The Jimple rhs commonly hides behind a cast.
		if (v instanceof CastExpr) return getRhs(((CastExpr) v).getOp());
		return v;
	}

	/**
	 * Handles a method call.
	 * @param in
	 * @param out
	 * @param expr
	 * @return Whether the call was to a concrete (we know its implementation) method.
	 * @throws CanceledException 
	 * @throws InterruptedException 
	 */
	private boolean handleMethodCall(Label label, ProgramState in, ProgramState out, InvokeExpr expr) throws InterruptedException, CanceledException {
		SootMethodRef methodRef = expr.getMethodRef();
		
		// Collect method information
		boolean isConcrete = false; // Whether we can access the method's implementation
		SootMethod resolvedMethod = null;
		try {
			if (methodRef.declaringClass().isPhantomClass() == false) {
				resolvedMethod = methodRef.resolve();
				isConcrete = resolvedMethod.isConcrete();
			}
		} catch (soot.ResolutionFailedException e) {
			// Nothing, just leave isConcrete as false.
		}
		boolean isStatic = methodRef.isStatic();
		AppObject receiver = isStatic ? null : getReceiverObject(expr);
		List<AppObject> arguments = getArgumentObjects(expr);
		String declaringClassName = methodRef.declaringClass().getName();
		boolean isApiMethod = options.getFilterReported().passesFilter(declaringClassName);
		boolean isTransparent = options.getFilterAnalyzed().passesFilter(declaringClassName);
		
		boolean analyzed = false;
		if (isConcrete && isTransparent) {
			// If we have access to the method's body, analyze it.
			try {
				handleInnerMethodCall(receiver, in, out, resolvedMethod, arguments);
				analyzed = true;
			} catch (InterruptedException e) {
				throw new RuntimeInterruptedException(e);
			} catch (CanceledException e) {
				throw new RuntimeInterruptedException(e);
			} catch (Exception e) {
				// The inner method could not be analyzed, but we can continue analyzing
				// the rest of this method.
			}
		}
		AppMethodRef m = getMethodRef(methodRef, !isConcrete, isApiMethod);
		try {
			out.methodCall(label, receiver, m, arguments);
		} catch (Exception e) {
			// Wrap any exception in a RuntimeInterruptedException
			throw new RuntimeInterruptedException(e);
		}
		
		handleExtras(label, in, out, methodRef);
		
		return analyzed;
	}
	
	private void handleExtras(Label label, ProgramState in, ProgramState out, SootMethodRef methodRef)
			throws InterruptedException, CanceledException {
		if (methodRef.isStatic() &&
				methodRef.declaringClass().getName().equals("java.lang.Thread") &&
				methodRef.name().equals("sleep")) {
			// This is Thread.sleep()
			AppMethodRef m = new SootAppMethodRef(scene, methodRef, true, true);
			out.methodCallOnAll(m);
			return;
		}
	}

	private AppMethodRef getMethodRef(SootMethodRef m, boolean isPhantom, boolean isOpaque) {
		return new SootAppMethodRef(scene, m, isPhantom, isOpaque);
	}

	private void handleInnerMethodCall(
			AppObject receiver,
			ProgramState in,
			ProgramState out,
			SootMethod resolvedMethod,
			List<AppObject> arguments) throws InterruptedException, CanceledException {
		AppMethodDecl calledMethod = new SootAppMethodDecl(scene, resolvedMethod);
		out.copyFrom(analyzer.analyzeMethod(calledMethod, in, arguments));
	}

	private List<AppObject> getArgumentObjects(InvokeExpr expr) {
		ArrayList<AppObject> result = new ArrayList<AppObject>();
		for (Object arg : expr.getArgs()) {
			result.add(new SootAppObject(scene, (Value)arg, method));
		}
		return result;
	}
	
	private AppObject getReceiverObject(InvokeExpr expr) {
		Value receiver = ((AbstractInstanceInvokeExpr)expr).getBase();
		return new SootAppObject(scene, receiver, method);
	}

	private void handleReturn(Label label, ProgramState in, ReturnStmt u) {
		analyzer.addReturned(new SootAppObject(scene, u.getOp(), method));
	}

	@Override
	protected void copy(ProgramState source, ProgramState dest) {
		try {
			dest.copyFrom(source);
		} catch (Exception e) {
			throw new RuntimeInterruptedException(e);
		}
	}
	
	@Override
	protected ProgramState entryInitialFlow() {
		ProgramState initial = initialState != null ? initialState : options.newProgramState();
		return initial;
	}
	
	@Override
	protected void merge(ProgramState in1, ProgramState in2, ProgramState out) {
		try {
			out.copyFrom(in1);
			out.joinFrom(in2);
		} catch (InterruptedException e) {
			throw new RuntimeInterruptedException(e);
		} catch (CanceledException e) {
			throw new RuntimeInterruptedException(e);
		}
	}
	
	@Override
	protected ProgramState newInitialFlow() {
		return options.newProgramState();
	}
	
	/**
	 * Set the objects associated with the arguments.
	 * @param args
	 */
	public void setArgs(List<AppObject> args) {
		this.inputArgs = args;
	}

}
