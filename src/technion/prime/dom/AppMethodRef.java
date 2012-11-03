package technion.prime.dom;

import java.io.Serializable;

public interface AppMethodRef extends Comparable<AppMethodRef>, Serializable {
	/**
	 * Just the name of the method, e.g. "f".
	 * @return Method name.
	 */
	String getShortName();

	/**
	 * Full signature of the method.
	 * Unique, similar to how it appears in bytecode.
	 * @return The full signature of the method.
	 */
	String getSignature();

	/**
	 * A convenient method name in the form of
	 * method(Type1 p1, Type2 p2).
	 * Does not include container type, and does not include the full name of parameter types, only short names.
	 * @return The long name of the method.
	 */
	String getLongName();

	/**
	 * @return True if this is a reference to a static method.
	 */
	boolean isStatic();

	/**
	 * @return Get the type inside which the referenced method was defined.
	 */
	AppType getContainingType();

	/**
	 * @return True if this is a reference to an unknown method.
	 */
	boolean isUnknown();

	/**
	 * @return The return type the referenced method.
	 */
	AppType getReturnType();

	/**
	 * @return True if this is a reference to a constructor.
	 */
	boolean isInit();

	/**
	 * @return True if this is a reference to a method for which we do not have
	 * the code.
	 */
	boolean isPhantom();
	
	/**
	 * @return True if this is a method we would like to analyze.
	 */
	boolean isTransparent();
	
	/**
	 * @return True if this is a method we would not like to analyze.
	 */
	boolean isOpaque();

}
