package technion.prime.dom.soot;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import soot.Scene;
import soot.SootMethodRef;
import soot.Type;
import technion.prime.dom.AppMethodRef;
import technion.prime.dom.AppType;
import technion.prime.utils.StringUtils;

public class SootAppMethodRef extends SootSceneItem implements AppMethodRef {
	private static final long serialVersionUID = -8447132709504525078L;
	private enum Attribute {
		STATIC,
		PHANTOM,
		OPAQUE,
		CONSTRUCTOR
	}
	
	private final String signature;
	private final String name;
	private final String longName;
	private final AppType containingClass;
	private final AppType returnType;
	
	private String representation;
	private EnumSet<Attribute> attrs = EnumSet.noneOf(Attribute.class);
	
	public SootAppMethodRef(Scene scene, SootMethodRef m, boolean isPhantom, boolean opaque) {
		super(scene);
		// EY: how about we throw the scene away? do we ever need it?
		deleteScene();
		if (m.isStatic()) attrs.add(Attribute.STATIC);
		if (isPhantom) attrs.add(Attribute.PHANTOM);
		if (opaque) attrs.add(Attribute.OPAQUE);
		signature = m.getSignature();
		name = m.name();
		if (name.startsWith("<init>")) attrs.add(Attribute.CONSTRUCTOR);
		longName = calculateLongName(m);
		containingClass = new SootAppType(scene, m.declaringClass().getType());
		returnType = new SootAppType(scene, m.returnType());
	}

	@Override
	public String getShortName() {
		return name;
	}
	
	@Override
	public String toString() {
		if (representation == null) {
			representation = calculateRepresentation();
		}
		return representation;
	}

	protected String calculateRepresentation() {
		String container = containingClass.getShortName();
		if (isInit()) {
			return String.format("new %s", longName.replace("<init>", container));
		} else {
			return String.format("%s%s.%s",
					isStatic() ? "static " : "",
					containingClass != null ? container : "?",
					longName);
		}
	}

	@Override
	public int hashCode() {
		return getSignature().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		return (obj instanceof AppMethodRef &&
				((AppMethodRef)obj).getSignature().equals(getSignature()));
	}

	@Override
	public String getSignature() {
		return signature;
	}

	@Override
	public String getLongName() {
		return longName;
	}
	
	@Override
	public boolean isStatic() {
		return attrs.contains(Attribute.STATIC);
	}

	@Override
	public AppType getContainingType() {
		return containingClass;
	}

	@Override
	public boolean isUnknown() {
		return false;
	}

	@Override
	public int compareTo(AppMethodRef o) {
		return getSignature().compareTo(o.getSignature());
	}
	
	@Override
	public AppType getReturnType() {
		return returnType;
	}
	
	private String calculateLongName(SootMethodRef m) {
		List<String> types = new LinkedList<String>();
		for (Object o : m.parameterTypes()) {
			Type t = (Type)o;
			String[] typeParts = t.toString().split("\\.");
			types.add(typeParts[typeParts.length - 1]);
		}
		return getShortName() + "(" + StringUtils.join(types, ",") + ")";
	}

	@Override
	public boolean isInit() {
		return name.startsWith("<init>");
	}

	@Override
	public boolean isPhantom() {
		return attrs.contains(Attribute.PHANTOM);
	}

	@Override
	public boolean isTransparent() {
		return !isOpaque();
	}

	@Override
	public boolean isOpaque() {
		return attrs.contains(Attribute.OPAQUE);
	}
	
}
