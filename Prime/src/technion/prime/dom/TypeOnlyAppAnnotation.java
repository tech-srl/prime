package technion.prime.dom;

import java.io.Serializable;

public class TypeOnlyAppAnnotation implements AppAnnotation, Serializable {
	private static final long serialVersionUID = 3166457262299140367L;
	
	private final AppType type;
	
	public TypeOnlyAppAnnotation(AppType t) {
		type = t;
	}
	
	@Override
	public String toString() {
		return "@" + type.getShortName();
	}

}
