package technion.prime.dom;

import java.util.List;

/**
 * An application, composed of multiple source files.
 */
public interface App {
	/**
	 * Get all classes defined in the application.
	 * This includes basic classes loaded for all Java code (from rt.jar).
	 * @return All classes.
	 */
	List<? extends AppClass> getClasses();

	/**
	 * Unloads one of the loaded classes.
	 * @param c A loaded class.
	 */
	void unloadClass(AppClass c);

}
