package technion.prime.dom;

import java.io.Serializable;

/**
 * A single class in the application.
 */
public interface AppClass extends Serializable {
	/**
	 * All the class's methods.
	 * @return All methods.
	 */
	Iterable<? extends AppMethodDecl> getMethods();

	String getName();

	boolean isInterface();

	boolean isPhantom();

	String getClassFileName();

	void deleteExternalInfo();
}
