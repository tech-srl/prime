package technion.prime.dom;

public class UnknownMethod implements AppMethodRef {
	private static final long serialVersionUID = -8603158747991952611L;

	/**
	 * The type of the receiver of this method.
	 */
	private final AppType receiverType;
	
	/**
	 * If this method was invoked from inside a method, this will contain that
	 * method; null otherwise.
	 */
	private final AppMethodRef contextMethod;
	
	private String signature;

	public UnknownMethod(AppType receiverType, AppMethodRef contextMethod) {
		this.receiverType = receiverType;
		this.contextMethod = contextMethod;
	}
	
	@Override
	public String getShortName() {
		return "?";
	}

	@Override
	public String getSignature() {
		if (signature == null) {
			signature = calculateSignature();
		}
		return signature;
	}
	
	protected String calculateSignature() {
		return String.format("<%s: %s ?()>", receiverType.getFullName(), getReturnType().getFullName());
	}

	@Override
	public String getLongName() {
		if (contextMethod == null) return "?";
		return String.format("? @ %s.%s",
				contextMethod.getContainingType().getShortName(),
				contextMethod.getShortName());
	}
	
	@Override
	public boolean isStatic() {
		return false;
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
	public AppType getContainingType() {
		return receiverType;
	}

	@Override
	public boolean isUnknown() {
		return true;
	}

	@Override
	public int compareTo(AppMethodRef o) {
		return getSignature().compareTo(o.getSignature());
	}

	@Override
	public AppType getReturnType() {
		return new UnknownAppType();
	}

	@Override
	public String toString() {
		if (contextMethod == null) return receiverType.getShortName() + ".?";
		return String.format("%s.? @ %s.%s",
				receiverType.getShortName(),
				contextMethod.getContainingType().getShortName(),
				contextMethod.getShortName());
	}

	@Override
	public boolean isInit() {
		return false;
	}

	@Override
	public boolean isPhantom() {
		return true;
	}

	@Override
	public boolean isTransparent() {
		return false;
	}

	@Override
	public boolean isOpaque() {
		return true;
	}

}
