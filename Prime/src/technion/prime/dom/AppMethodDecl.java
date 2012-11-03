package technion.prime.dom;

import java.io.Serializable;

/**
 * A method declaration.
 * This is different from AppMethodRef which is just a reference to a method,
 * not its declaration.
 */
public interface AppMethodDecl extends Serializable {
	boolean isConcrete();

	AppType getDeclaringType();

	String getSignature();
}
