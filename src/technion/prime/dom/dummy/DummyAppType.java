package technion.prime.dom.dummy;

import technion.prime.dom.AppType;

public class DummyAppType implements AppType {
	private static final long serialVersionUID = -6683420968685135130L;
	private static final String[] PRIMITIVE_TYPE_NAMES = new String[] {
		"int", "long", "float", "double", "char", "boolean", "short"
	};
	
	private final String typeName;

	public DummyAppType(String typeName) {
		this.typeName = typeName;
	}

	@Override
	public String getFullName() {
		return typeName;
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
	public String getShortName() {
		return getFullName().replaceAll("([^.])+\\.", "");
	}
	
	@Override
	public String toString() {
		return getShortName();
	}

	@Override
	public boolean isPrimitive() {
		for (String s : PRIMITIVE_TYPE_NAMES) {
			if (typeName.equals(s)) return true;
		}
		return false;
	}

	@Override
	public boolean isUnknown() {
		return false;
	}

	@Override
	public boolean isVoid() {
		return typeName.equals("void");
	}

}
