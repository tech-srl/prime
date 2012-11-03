package technion.prime.dom.soot;

import java.util.LinkedList;
import java.util.List;



import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import technion.prime.dom.AppMethodDecl;

public class SootAppClass extends SootSceneItem implements technion.prime.dom.AppClass {
	private static final long serialVersionUID = -3148912919142284788L;
	
	private final boolean isPhantom;
	private final boolean isInterface;
	private final String fullName;
	private List<SootAppMethodDecl> methods;
	private String classFileName;
	
	private final transient SootClass c;

	public SootAppClass(Scene scene, SootClass c, boolean phantom) {
		super(scene);
		this.c = c;
		isPhantom = phantom;
		isInterface = c.isInterface();
		fullName = c.getName();
	}
	
	public SootClass getSootClass() {
		return c;
	}
	
	public SootAppClass(Scene scene, SootClass c, boolean phantom, String classFileName) {
		this(scene, c, phantom);
		this.classFileName = classFileName;
	}

	@Override
	public Iterable<? extends AppMethodDecl> getMethods() {
		if (methods == null) {
			methods = calculateMethods(c);
		}
		return methods;
	}

	@Override
	public int hashCode() {
		return fullName.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof SootAppClass))
			return false;
		SootAppClass other = (SootAppClass) obj;
		if (fullName == null) {
			if (other.fullName != null)
				return false;
		} else if (!fullName.equals(other.fullName))
			return false;
		return true;
	}
	
	@Override
	public String getName() {
		return fullName;
	}
	
	@Override
	public String toString() {
		return getName();
	}

	@Override
	public boolean isInterface() {
		return isInterface;
	}
	
	@Override
	public boolean isPhantom() {
		return isPhantom;
	}
	
	@Override
	public String getClassFileName() {
		return classFileName;
	}
	
	private List<SootAppMethodDecl> calculateMethods(SootClass c) {
		List<SootAppMethodDecl> result = new LinkedList<SootAppMethodDecl>();
		for (SootMethod m : c.getMethods()) {
			result.add(new SootAppMethodDecl(scene, m));
		}
		return result;
	}
	
}
