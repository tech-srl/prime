package technion.prime.dom;

public class UnknownAppType implements AppType {
	private static final long serialVersionUID = -2344905558921804709L;

	@Override
	public String getFullName() {
		return "UNKNOWNP.UNKNOWN";
	}

	@Override
	public String getShortName() {
		return "UNKNOWN";
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
		return getFullName();
	}

	@Override
	public boolean isPrimitive() {
		return false;
	}

	@Override
	public boolean isUnknown() {
		return true;
	}

	@Override
	public boolean isVoid() {
		return false;
	}

}
