package technion.prime.analysis.soot;

import org.apache.commons.lang.StringUtils;

import soot.G;
import soot.Scene;
import soot.SootClass;
import soot.SourceLocator;
import soot.Timer;

import technion.prime.utils.OutputHider;

import technion.prime.utils.ConcurrencyUtils;
import technion.prime.utils.Logger.CanceledException;
import technion.prime.utils.Logger;
import technion.prime.utils.Stage;
import technion.prime.utils.CompiledItem;
import technion.prime.dom.App;
import technion.prime.dom.soot.SootApp;
import technion.prime.statistics.AnalysisDetails;
import technion.prime.analysis.AppLoader;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;


public class SootAppLoader implements AppLoader {
	private final static String JAVA_HOME = System.getProperty("java.home");
	private final static String RT_JAR_LOCATION_1 = JAVA_HOME + "/lib/rt.jar";
	private final static String RT_JAR_LOCATION_2 = JAVA_HOME + "/../Classes/alt-rt.jar";
	private final static String JCE_JAR_LOCATION = JAVA_HOME + "/lib/jce.jar";
	private static Collection<String> basicClassPathElements;
	
	private Scene scene;
	private technion.prime.Options primeOptions;
	private soot.options.Options sootOptions;
	private Collection<CompiledItem> compiledItems = new HashSet<CompiledItem>();
	private Collection<String> entireFoldersOrJars = new HashSet<String>();

	public SootAppLoader(technion.prime.Options primeOptions) {
		this.primeOptions = primeOptions;
	}

	@Override
	public void addCompiledItems(Collection<CompiledItem> items) {
		compiledItems.addAll(items);
	}
	
	@Override
	public void addEntireFolders(Collection<String> folders) {
		addEntireFoldersOrJars(folders);
	}
	
	@Override
	public void addEntireJars(Collection<String> filenames) {
		addEntireFoldersOrJars(filenames);
	}
	
	private void addEntireFoldersOrJars(Collection<String> filenames) {
		entireFoldersOrJars.addAll(filenames);
	}
	
	private String calculateClasspath() {
		HashSet<String> result = new HashSet<String>();
		for (CompiledItem item : compiledItems) {
			result.add(item.getBasePath());
		}
		result.addAll(entireFoldersOrJars);
		result.addAll(getBasicClassPathElements());
		return StringUtils.join(result, File.pathSeparator);
	}
	
	private static Collection<String> getBasicClassPathElements() {
		if (basicClassPathElements == null) {
			basicClassPathElements = calculateBasicClassPathElements();
		}
		return basicClassPathElements;
	}
	
	private static Collection<String> calculateBasicClassPathElements() {
		Collection<String> result = new LinkedList<String>();
		
		if (new File(RT_JAR_LOCATION_1).exists()) result.add(RT_JAR_LOCATION_1);
		if (new File(RT_JAR_LOCATION_2).exists()) result.add(RT_JAR_LOCATION_2);
		if (result.isEmpty()) {
			String message = String.format("Could not find neither %s nor %s",
					RT_JAR_LOCATION_1, RT_JAR_LOCATION_2);
			throw new RuntimeException(message);
		}
		
		if (new File(JCE_JAR_LOCATION).exists()) result.add(JCE_JAR_LOCATION);
		return result;
	}

	private Collection<String> createClassList() {
		LinkedList<String> result = new LinkedList<String>();
		for (CompiledItem item : compiledItems) {
			result.add(item.getClassName());
		}
		return result;
	}
	
	/* (non-Javadoc)
	 * @see technion.prime.analysis.AppLoader#load(java.util.Collection)
	 */
	@Override
	public App load() throws CanceledException {
		int count = 0;
		int numTotal = -1;
		Logger.startStage(Stage.LOADING, compiledItems.size());
		try {
			initializeSoot();
			SootApp app = new SootApp(scene);
			for (SootClass c : scene.getClasses(SootClass.BODIES)) {
				try {
					app.addLoadedClass(c);
					if (c.isConcrete()) count++;
					ConcurrencyUtils.checkState();
				} catch (InterruptedException e) {
					// Swallow
				}
			}
			numTotal = app.getClasses().size();
			return app;
		} finally {
			Logger.endStage(String.format(
					"loaded %d concrete classes out of %d total listed classes.",
					count, numTotal));
			primeOptions.getOngoingAnalysisDetails().setField(AnalysisDetails.LOADED_CLASSES, count);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void initializeSoot() {
		OutputHider h = new OutputHider();
		try {
			soot.G.reset();
			sootOptions = soot.options.Options.v();
			sootOptions.set_allow_phantom_refs(true);
			sootOptions.set_output_format(soot.options.Options.output_format_jimple);
			sootOptions.set_output_dir(primeOptions.getTempDir());
			sootOptions.set_src_prec(soot.options.Options.src_prec_only_class); // Ignore Java files, load only class files.
			sootOptions.set_soot_classpath(calculateClasspath());
			sootOptions.set_whole_program(true);
			sootOptions.set_verbose(false);
			sootOptions.classes().addAll(createClassList());
			sootOptions.set_process_dir(new LinkedList<String>(entireFoldersOrJars));
			scene = Scene.v();
			loadClassesInSoot();
			scene.loadNecessaryClasses();
		} finally {
			h.release();
		}
	}
	
	@SuppressWarnings("unchecked")
	private void loadClassesInSoot() {
		// Load basic stuff such as java.lang.String
		scene.loadBasicClasses();
		loadExtraBasicClasses();
		tryLoadClasses((Iterable<String>)sootOptions.classes());
		
		scene.loadDynamicClasses();
		for (String path : (Iterable<String>)sootOptions.process_dir()) {
			tryLoadClasses((Iterable<String>)SourceLocator.v().getClassesUnder(path));
		}
		scene.setDoneResolving();
	}
	
	private void tryLoadClasses(Iterable<String> classes) {
		for (String className : classes) {
			try {
				SootClass c = scene.loadClassAndSupport(className);
				if (c == null) {
					Logger.warn("Could not load " + className);
					continue;
				}
				c.setApplicationClass();
			} catch (RuntimeException e) {
				Logger.warn("Could not load " + className + ": " + e.getMessage());
				try {
					// Soot may leave timers running when an exception is thrown
					List<Timer> timers = new LinkedList<Timer>(G.v().Timer_outstandingTimers);
					for (Timer t : timers) {
						t.end();
					}
				} catch (RuntimeException e2) {
					Logger.exception(e2);
				}
			}
		}
	}

	private void loadExtraBasicClasses() {
		// Some classes which are usually loaded to HIERARCHY level need to be loaded to SIGNATURES
		// level.
		String[] required = new String[] {
				"java.lang.StackOverflowError",
				"java.lang.ClassCircularityError",
				"java.lang.VerifyError",
				"java.lang.UnknownError",
				"java.lang.InstantiationError",
		};
		for (String s : required) {
			scene.tryLoadClass(s, SootClass.SIGNATURES);
		}
	}
	
}
