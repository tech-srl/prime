//package technion.prime.utils;
//
//import java.io.File;
//import java.io.IOException;
//
//import org.eclipse.core.runtime.IProgressMonitor;
//import org.eclipse.jface.preference.IPreferenceStore;
//
//import technion.prime.eclipse.Activator;
//import technion.prime.eclipse.preferences.PreferenceConstants;
//
//public class Settings {
//	private static IPreferenceStore prefs;
//	private static Settings instance;
//	private static String defaultTempFolder = System.getProperty("java.io.tmpdir") + "/prime/temp";
//	private static String defaultOutputFolder = System.getProperty("java.io.outputdir") + "/prime/output";
//	private IProgressMonitor eclipseMonitor;
//	
//	static {
//		try {
//			prefs = Activator.getDefault().getPreferenceStore();
//		} catch (Exception e) {
//			prefs = null;
//		}
//	}
//	
//	private static Settings getInstance() {
//		if (instance == null) instance = new Settings();
//		return instance;
//	}
//	
//	public static long getSingleActionTimeout(Stage stage) {
//		String prefName;
//		switch (stage) {
//		case DOWNLOADING:
//			prefName = PreferenceConstants.Integers.SINGLE_DOWNLOAD_TIMEOUT.name();
//			break;
//		case COMPILING:
//			prefName = PreferenceConstants.Integers.SINGLE_COMPILE_TIMEOUT.name();
//			break;
//		case ANALYZING:
//			prefName = PreferenceConstants.Integers.SINGLE_ANALYZE_TIMEOUT.name();
//			break;
//		default:
//			return -1;
//		}
//		return prefs.getInt(prefName);
//	}
//	
//	public static long getStageTimeout(Stage stage) {
//		String prefName;
//		switch (stage) {
//		case DOWNLOADING:
//			prefName = PreferenceConstants.Integers.ALL_DOWNLOAD_TIMEOUT.name();
//			break;
//		case COMPILING:
//			prefName = PreferenceConstants.Integers.ALL_COMPILE_TIMEOUT.name();
//			break;
//		case ANALYZING:
//			prefName = PreferenceConstants.Integers.ALL_ANALYZE_TIMEOUT.name();
//			break;
//		default:
//			return -1;
//		}
//		return prefs.getInt(prefName);
//	}
//	
//	public static boolean runInParallel(Stage stage) {
//		String perfName;
//		switch (stage) {
//		case COMPILING:
//			perfName = PreferenceConstants.Booleans.COMPILE_IN_PARALLEL.name();
//			break;
//		case ANALYZING:
//			perfName = PreferenceConstants.Booleans.ANALYZE_IN_PARALLEL.name();
//			break;
//		default:
//			return false;
//		}
//		return prefs.getBoolean(perfName);
//	}
//	
//	public static boolean shouldGenerateOutputFromUnclustered() {
//		return prefs.getBoolean(PreferenceConstants.Booleans.GENERATE_UNCLUSTERED.name());
//	}
//	
//	public static boolean shouldIgnoreJslTypes() {
//		return prefs.getBoolean(PreferenceConstants.Booleans.IGNORE_JSL.name());
//	}
//	
//	public static boolean shouldShowExceptions() {
//		return prefs.getBoolean(PreferenceConstants.Booleans.SHOW_EXCEPTIONS.name());
//	}
//	
//	public static String getCvsExecutablePath() {
//		return prefs.getString(PreferenceConstants.Strings.CVS_PATH.name());
//	}
//	
//	public static String getGitExecutablePath() {
//		return prefs.getString(PreferenceConstants.Strings.GIT_PATH.name());
//	}
//	
//	public static IProgressMonitor getEclipseMonitor() {
//		return getInstance().eclipseMonitor;
//	}
//	
//	public static void setEclipseMonitor(IProgressMonitor m) {
//		getInstance().eclipseMonitor = m;
//	}
//
//	public static int getDefaultNumSamples() {
//		return prefs.getInt(PreferenceConstants.Integers.NUM_RESULTS.name());
//	}
//
//	public static void setDefaultNumSamples(int numSamples) {
//		prefs.setValue(PreferenceConstants.Integers.NUM_RESULTS.name(), numSamples);
//	}
//
//	public static String getOutputFolder() {
//		return prefs.getString(PreferenceConstants.Strings.OUTPUT_PATH.name());
//	}
//	
//	public static String getTempFolder() {
//		return prefs.getString(PreferenceConstants.Strings.TEMPDIR_PATH.name());
//	}
//
//	public static String getDefaultDebugFolder() {
//		return prefs.getString(PreferenceConstants.Strings.DEBUG_PATH.name());
//	}
//	
//	public static void setDefaultDebugFolder(String path) {
//		prefs.setValue(PreferenceConstants.Strings.DEBUG_PATH.name(), path);
//	}
//	
//	public static int getThreadPoolSize() {
//		return prefs.getInt(PreferenceConstants.Integers.THREAD_POOL_SIZE.name());
//	}
//
//	public static void setThreadPoolSize(int n) {
//		prefs.setValue(PreferenceConstants.Integers.THREAD_POOL_SIZE.name(), n);
//	}
//
//	public static boolean isolateFailedCompiles() {
//		return prefs.getBoolean(PreferenceConstants.Booleans.ISOLATE_UNCOMPILABLE.name());
//	}
//	
//	public static String getDefaultTempFolder() {
//		try {
//			return new File(defaultTempFolder).getCanonicalPath();
//		} catch (IOException e) {
//			Logger.exception(e);
//			return defaultTempFolder;
//		}
//	}
//	
//	public static String getDefaultOutputDir() {
//		try {
//			return new File(defaultOutputFolder).getCanonicalPath();
//		} catch (IOException e) {
//			Logger.exception(e);
//			return defaultOutputFolder;
//		}
//	}
//
//	public static void setStageTimeout(Stage stage, long n) {
//		String prefName;
//		switch (stage) {
//		case DOWNLOADING:
//			prefName = PreferenceConstants.Integers.ALL_DOWNLOAD_TIMEOUT.name();
//			break;
//		case COMPILING:
//			prefName = PreferenceConstants.Integers.ALL_COMPILE_TIMEOUT.name();
//			break;
//		case ANALYZING:
//			prefName = PreferenceConstants.Integers.ALL_ANALYZE_TIMEOUT.name();
//			break;
//		default:
//			return;
//		}
//		prefs.setValue(prefName, n);
//	}
//
//	public static String getDotExecutablePath() {
//		return prefs.getString(PreferenceConstants.Strings.DOT_PATH.name());
//	}
//
//	
//
//}
