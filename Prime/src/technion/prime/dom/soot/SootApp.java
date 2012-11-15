package technion.prime.dom.soot;

import java.util.LinkedList;
import java.util.List;

import soot.Scene;
import soot.SootClass;
import technion.prime.dom.App;
import technion.prime.dom.AppClass;
import technion.prime.utils.Logger;

/**
 * An application loaded by Soot.
 */
public class SootApp implements App {
	private final Scene scene;
	private final List<SootAppClass> classes = new LinkedList<SootAppClass>();

	public SootApp(Scene scene) {
		this.scene = scene;
	}
	
	public void addLoadedClass(SootClass c) {
		boolean isPhantom = c.isPhantom();
		classes.add(new SootAppClass(scene, c, isPhantom, null));
		if (isPhantom == false) {
			c.setApplicationClass();
			c.setResolvingLevel(SootClass.BODIES);
		}
	}
	
	@Override
	public List<? extends AppClass> getClasses() {
		assert(classes != null);
		return classes;
	}

	@Override
	public void unloadClass(AppClass c) {
		SootAppClass sc = (SootAppClass)c;
		try {
			scene.removeClass(sc.getSootClass());
			scene.getClasses().remove(sc);
		} catch (RuntimeException e) {
			Logger.exception(e);
			// Do nothing.
		}
	}
	
	public static void reset() {
		SootAppMethodDecl.reset();
	}
	
}
