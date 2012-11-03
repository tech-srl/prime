package technion.prime.dom;

import java.util.Collection;


public class UnknownMethodFromField extends UnknownMethod {
	private static final long serialVersionUID = 7697898234376716128L;
	
	private final Collection<AppAnnotation> annotations;

	public UnknownMethodFromField(AppType receiverType, Collection<AppAnnotation> annotations) {
		super(receiverType, null);
		this.annotations = annotations;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("%s from field", getContainingType().getShortName()));
		if (annotations.isEmpty() == false) {
			sb.append(" (");
			boolean first = true;
			for (AppAnnotation a : annotations) {
				if (first) {
					first = false;
				} else {
					sb.append(", ");
				}
				sb.append(a.toString());
			}
			sb.append(")");
		}
		return sb.toString();
	}
	
	@Override
	public String calculateSignature() {
		return super.calculateSignature() + "f";
	}

}
