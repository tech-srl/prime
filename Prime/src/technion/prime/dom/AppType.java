package technion.prime.dom;

import java.io.Serializable;

public interface AppType extends Serializable {
	/**
	 * @return Full type name, including containing package, e.g. a.b.C
	 */
	String getFullName();
	
	/**
	 * @return Short type name, e.g. C
	 */
	String getShortName();
	
	/**
	 * @return True if the type is primitive (not a class, not a reference). Void is not considered
	 * primitive for this method.
	 */
	boolean isPrimitive();
	
	/**
	 * @return True if this represents an unknown type.
	 */
	boolean isUnknown();

	/**
	 * @return True if this is the "void" type.
	 */
	boolean isVoid();
}
