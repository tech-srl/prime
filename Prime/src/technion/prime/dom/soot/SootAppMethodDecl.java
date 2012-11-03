package technion.prime.dom.soot;

import java.security.Permission;
import java.util.HashMap;
import java.util.Map;

import soot.Body;
import soot.Scene;
import soot.SootMethod;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;

import technion.prime.utils.OutputHider;
import technion.prime.dom.AppMethodDecl;

public class SootAppMethodDecl extends SootSceneItem implements AppMethodDecl {
	private static final long serialVersionUID = -1203927842912527593L;
	
	private static Map<SootMethod, String> signatures = new HashMap<SootMethod, String>();
	
	// Used for lazy retrieval of body. Would be invalid after deserialization,
	// hence transient. Also discarded after the unit graph is generated.
	transient private SootMethod m;
	transient private UnitGraph unitGraph;
	
	private final String representation;
	private final boolean isConcrete;
	private final SootAppType declaringType;

	private String signature;

	public SootAppMethodDecl(Scene scene, SootMethod m) {
		super(scene);
		signature = signatures.get(m);
		if (signature == null) {
			signature = m.getSignature();
			signatures.put(m, signature);
		}
		representation = m.getName() + "(" + m.getParameterCount() + ")";
		isConcrete = m.isConcrete();
		declaringType = new SootAppType(scene, m.getDeclaringClass().getType());
		this.m = m;
	}

	@Override
	public int hashCode() {
		return signature.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof SootAppMethodDecl))
			return false;
		SootAppMethodDecl other = (SootAppMethodDecl) obj;
		return signature.equals(other.signature);
	}
	
	@Override
	public String toString() {
		return representation;
	}

	@Override
	public boolean isConcrete() {
		return isConcrete;
	}
	
	@Override
	public SootAppType getDeclaringType() {
		return declaringType;
	}
	
	public SootMethod getSootMethod() {
		return m;
	}
	
	public UnitGraph getUnitGraph() {
		if (unitGraph == null) {
			OutputHider h = new OutputHider();
			try {
				Body b = getBody();
				if (b == null) throw new NullPointerException("cannot get body from method");
				unitGraph = new ExceptionalUnitGraph(b);
				m = null;
			} finally {
				h.release();
			}
		}
		return unitGraph;
	}
	
	private Body getBody() {
		final SecurityManager baseSecurityManager = System.getSecurityManager();
		System.setSecurityManager(new SecurityManager() {
			@Override
			public void checkPermission(Permission perm) {
				// First check with the base manager:
				if (baseSecurityManager != null) baseSecurityManager.checkPermission(perm);
				// Then throw an exception if it's an exit:
				if (perm.getName() != null && perm.getName().startsWith("exitVM")) {
					throw new SecurityException();
				}
			}
		});
		try {
			return m.retrieveActiveBody();
		} finally {
			System.setSecurityManager(baseSecurityManager);
		}
	}
	
	@Override
	public String getSignature() {
		return signature;
	}

}
