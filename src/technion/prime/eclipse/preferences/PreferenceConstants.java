package technion.prime.eclipse.preferences;

/**
 * Constant definitions for plug-in preferences
 */
public class PreferenceConstants {
	
	private static int calculateThreadPoolSize() {
		// Number of processors:
		int p = Runtime.getRuntime().availableProcessors();
		// Maximum memory that can be used, in MB:
		int m = (int)(Runtime.getRuntime().maxMemory() / (2 << 20));
		// Made-up formula:
		return Math.min(p * 100, m / 10);
	}
	
	public enum Integers {
		NUM_RESULTS(10000),
		SINGLE_DOWNLOAD_TIMEOUT(30000),
		ALL_DOWNLOAD_TIMEOUT(600000),
		SINGLE_COMPILE_TIMEOUT(20000),
		ALL_COMPILE_TIMEOUT(600000),
		SINGLE_ANALYZE_TIMEOUT(5000),
		ALL_ANALYZE_TIMEOUT(600000),
		THREAD_POOL_SIZE(calculateThreadPoolSize());
		
		public final int defaultValue;
		
		Integers(int defaultValue) {
			this.defaultValue = defaultValue;
		}
		
		public int getDefault() {
			return defaultValue;
		}
	}
	
	public enum Strings {
		OUTPUT_PATH(System.getProperty("java.io.tmpdir") + "/prime/output"),
		TEMPDIR_PATH(System.getProperty("java.io.tmpdir") + "/prime/temp"),
		DEBUG_PATH(""),
		CVS_PATH(""),
		GIT_PATH(""),
		DOT_PATH("");
		
		public final String defaultValue;
		
		Strings(String defaultValue) {
			this.defaultValue = defaultValue;
		}
		
		public String getDefault() {
			return defaultValue;
		}
	}
	
	public enum Booleans {
		GENERATE_UNCLUSTERED(false),
		USE_DEBUG_MODE(false),
		IGNORE_JSL(true),
		SHOW_EXCEPTIONS(false),
		COMPILE_IN_PARALLEL(false),
		ANALYZE_IN_PARALLEL(false),
		ISOLATE_UNCOMPILABLE(false);
		
		public final boolean defaultValue;
		
		Booleans(boolean defaultValue) {
			this.defaultValue = defaultValue;
		}
		
		public boolean getDefault() {
			return defaultValue;
		}
	}
	
}
