package technion.prime.dom.dummy;

import technion.prime.dom.AppType;
import technion.prime.dom.AppMethodRef;


public class DummyAppMethodRef implements AppMethodRef {
	private static final long serialVersionUID = 653929702909478534L;
	
	private final AppType definingType;
	private final AppType returnType;
	private final String name;
	private final String signature;
	
	public DummyAppMethodRef(AppType definingType, AppType returnType, String name, AppType... params) {
		this.definingType = definingType;
		this.returnType = returnType;
		this.name = name;
		String paramString = "";
		for (AppType t : params) {
			paramString += t.getFullName();
		}
		signature = String.format("<%s: %s %s(%s)>",
				definingType.getFullName(), returnType.getFullName(), name, paramString);
	}
	
	@Override
	public int compareTo(AppMethodRef o) {
		return getSignature().compareTo(o.getSignature());
	}

	@Override
	public String getShortName() {
		return name;
	}

	@Override
	public String getSignature() {
		return signature;
	}

	@Override
	public String getLongName() {
		return name + "()";
	}

	@Override
	public boolean isStatic() {
		return name.startsWith("static");
	}

	@Override
	public AppType getContainingType() {
		return definingType;
	}

	@Override
	public boolean isUnknown() {
		return name.endsWith("?");
	}

	@Override
	public AppType getReturnType() {
		return returnType;
	}

	@Override
	public boolean isInit() {
		return name.startsWith("<init>");
	}

	@Override
	public boolean isPhantom() {
		return true;
	}

	@Override
	public boolean isTransparent() {
		return true;
	}

	@Override
	public boolean isOpaque() {
		return false;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		return (obj instanceof AppMethodRef &&
				((AppMethodRef)obj).getSignature().equals(getSignature()));
	}
	
	@Override
	public int hashCode() {
		return getSignature().hashCode();
	}

	@Override
	public String toString() {
		return getSignature();
	}
	
}
