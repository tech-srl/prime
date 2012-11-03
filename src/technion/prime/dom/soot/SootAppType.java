package technion.prime.dom.soot;

import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.Type;
import technion.prime.dom.AppType;

public class SootAppType extends SootSceneItem implements AppType {
	private static final long serialVersionUID = 7092406485704312730L;

	private final String name;
	private final boolean isPrimitive;
	
	private transient String shortName;

	public SootAppType(Scene scene, Type t) {
		super(scene);
		assert(t.toString().contains(" ") == false);
		isPrimitive = t instanceof PrimType;
		name = t instanceof RefType ?
			((RefType) t).getClassName() :
			t.toString();
	}

	@Override
	public String getFullName() {
		return name;
	}
	
	@Override
	public boolean equals(Object obj) {
		return (obj instanceof AppType && ((AppType)obj).getFullName().equals(getFullName()));
	}
	
	@Override
	public int hashCode() {
		return getFullName().hashCode();
	}
	
	@Override
	public String toString() {
		return getShortName();
	}

	@Override
	public String getShortName() {
		if (shortName == null) {
			shortName = name.substring(name.lastIndexOf('.') + 1);
		}
		return shortName;
	}

	@Override
	public boolean isPrimitive() {
		return isPrimitive;
	}

	@Override
	public boolean isUnknown() {
		return false;
	}

	@Override
	public boolean isVoid() {
		return name.equals("void");
	}

}
