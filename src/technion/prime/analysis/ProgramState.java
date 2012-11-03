package technion.prime.analysis;

import java.util.List;

import technion.prime.dom.soot.SootAppObject;
import technion.prime.dom.AppAnnotation;
import technion.prime.dom.AppMethodDecl;
import technion.prime.dom.AppMethodRef;
import technion.prime.dom.AppObject;
import technion.prime.history.HistoryCollection;
import technion.prime.utils.Logger.CanceledException;

/**
 * A program state to be used during the analysis.
 * Mutable - that's how Soot works.
 * The methods copyFrom(), joinFrom(), equals() and hashCode() must be properly implemented
 * for the fixed-point calculation to eventually halt.
 */
public interface ProgramState extends Cloneable {
	/**
	 * Copy a different ProgramState into this one, overriding everything in the
	 * local ProgramState.
	 * @param ps The other ProgramState.
	 */
	void copyFrom(ProgramState ps)
			 throws InterruptedException, CanceledException;
	
	/**
	 * Join a different ProgramState into this one, using the join operator of the analysis.
	 * @param ps The other ProgramState
	 */
	void joinFrom(ProgramState ps)
			 throws InterruptedException, CanceledException;
	
	/**
	 * Register an assignment from a new object, e.g.
	 * 
	 * <code>x = new T()</code>
	 * 
	 * @param l The label in which the assignment occurs.
	 * @param lhs The left-hand-side object (<code>x</code> in above example).
	 */
	void assignmentFromNew(Label l, AppObject lhs)
			 throws InterruptedException, CanceledException;
	
	/**
	 * Register an assignment from a phantom (code is unknown) method call, e.g.
	 * 
	 * <code>x = y.unknownMethod(a, b)</code>.
	 * 
	 * @param l The label in which the assignment occurs.
	 * @param lhs The left-hand-side object (<code>x</code> in above example).
	 * @param receiver The method's receiver (<code>y</code> in above example).
	 * @param m The phantom method invoked (<code>unknownMethod</code> in above example).
	 * @param args Method arguments (&lt;<code>a, b</code>&gt; in above example).
	 */
	void assignmentFromPhantomMethod(Label l, AppObject lhs, AppObject receiver, AppMethodRef m, Iterable<? extends AppObject> args)
			 throws InterruptedException, CanceledException;
	
	/**
	 * Register an assignment from another object, e.g.
	 * 
	 * <code>x = y</code>.
	 * 
	 * @param l The label in which the assignment occurs.
	 * @param lhs The left-hand-side object (<code>x</code> in above example).
	 * @param rhs The right-hand-side object (<code>y</code> in above example).
	 */
	void assignmentFromObject(Label l, AppObject lhs, AppObject rhs)
			 throws InterruptedException, CanceledException;
	
	/**
	 * Register an assignment from all of the the possible right-hand-side objects,
	 * as alternatives. For example, use this for
	 * 
	 * <code>x = ?</code> where "?" is either <code>y</code> or <code>z</code>.
	 * 
	 * @param l The label in which the assignment occurs.
	 * @param lhs The left-hand-side object (<code>x</code> in above example).
	 * @param rhss The possible objects in the right-hand-side (<code>x</code> and <code>y</code>
	 * in above example).
	 */
	void assignmentFromAllOf(Label l, AppObject lhs, Iterable<? extends AppObject> rhss)
			 throws InterruptedException, CanceledException;
	
	/**
	 * Register an assignment from null or from an untracked object, e.g.
	 * 
	 * <code>x = null</code>.
	 * 
	 * @param l The label in which the assignment occurs.
	 * @param lhs The left-hand-side object (<code>x</code> in above example). 
	 */
	void assignmentFromUntracked(Label l, SootAppObject lhs)
			 throws InterruptedException, CanceledException;
	
	/**
	 * Register a method call, e.g.
	 * 
	 * <code>x.f(a, b)</code>.
	 * 
	 * @param l The label in which the method invocation occurs.
	 * @param receiver The method's receiver (<code>x</code> in above example).
	 * @param m The phantom method invoked (<code>f</code> in above example).
	 * @param args Method arguments (&lt;<code>a, b</code>&gt; in above example).
	 */
	void methodCall(Label l, AppObject receiver, AppMethodRef m, Iterable<? extends AppObject> args)
			 throws InterruptedException, CanceledException;
	
	/**
	 * @return A collection containing all the histories in this state.
	 */
	HistoryCollection toHistoryCollection();

	/**
	 * Remove all histories that do not contain directly tracked types.
	 */
	void removeUntrackedHistories();

	/**
	 * Set the method during the analysis of which this state appeared.
	 * @param method
	 */
	void setAnalyzedMethod(AppMethodDecl method);

	/**
	 * Add a new unknown parameter to the state, if no existing object matching it is found.
	 * 
	 * Basically, called at the beginning of a method, but ignored in inter-procedurally-called
	 * methods.
	 * @param l
	 * @param obj
	 * @throws CanceledException 
	 * @throws InterruptedException 
	 */
	void assignmentFromNewParameter(Label l, SootAppObject obj) throws InterruptedException, CanceledException;

	/**
	 * Add a new unknown field to the state, if no existing object matching it is found.
	 * 
	 * Basically, called at the beginning of a method, but ignored in inter-procedurally-called
	 * methods.
	 * @param l
	 * @param lhs
	 * @param rhs
	 * @param annotations
	 * @throws CanceledException 
	 * @throws InterruptedException 
	 */
	void assignmentFromNewField(Label l, SootAppObject lhs, SootAppObject rhs, List<AppAnnotation> annotations)
			throws InterruptedException, CanceledException;

	/**
	 * Register a method call on all abstract objects.
	 * 
	 * @param m The phantom method invoked (<code>f</code> in above example).
	 * @throws CanceledException 
	 * @throws InterruptedException 
	 */
	void methodCallOnAll(AppMethodRef m) throws InterruptedException, CanceledException;

}
