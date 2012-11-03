package technion.prime.dom;

public class UnknownMethodFromParam extends UnknownMethod {
	private static final long serialVersionUID = -4372490088214158050L;

	public UnknownMethodFromParam(AppType receiverType) {
		super(receiverType, null);
	}
	
	@Override
	public String toString() {
		return String.format("%s from parameter", this.getContainingType().getShortName());
	}
	
	@Override
	public String calculateSignature() {
		return super.calculateSignature() + "p";
	}

}
