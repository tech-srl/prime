package technion.prime.eclipse;

import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;

import technion.prime.eclipse.preferences.PreferenceConstants;

import technion.prime.utils.Stage;
import technion.prime.utils.StringFilter;

import technion.prime.DefaultOptions;

public class EclipseOptions extends DefaultOptions {
	private static final long serialVersionUID = -4605698463349515952L;
	
	private transient IPreferenceStore prefs;
	private transient IProgressMonitor eclipseMonitor;

	private StringFilter reportedFilter;
	
	public EclipseOptions() {
		try {
			prefs = Activator.getDefault().getPreferenceStore();
		} catch (Exception e) {
			prefs = null;
		}
	}
	
	/* (non-Javadoc)
	 * @see technion.prime.DefaultOptions#getSingleActionTimeout(technion.prime.utils.Stage)
	 */
	@Override
	public long getSingleActionTimeout(Stage stage) {
		String prefName;
		switch (stage) {
		case DOWNLOADING:
			prefName = PreferenceConstants.Integers.SINGLE_DOWNLOAD_TIMEOUT.name();
			break;
		case COMPILING:
			prefName = PreferenceConstants.Integers.SINGLE_COMPILE_TIMEOUT.name();
			break;
		case ANALYZING:
			prefName = PreferenceConstants.Integers.SINGLE_ANALYZE_TIMEOUT.name();
			break;
		default:
			return -1;
		}
		return prefs.getInt(prefName);
	}
	
	/* (non-Javadoc)
	 * @see technion.prime.DefaultOptions#getStageTimeout(technion.prime.utils.Stage)
	 */
	@Override
	public long getStageTimeout(Stage stage) {
		String prefName;
		switch (stage) {
		case DOWNLOADING:
			prefName = PreferenceConstants.Integers.ALL_DOWNLOAD_TIMEOUT.name();
			break;
		case COMPILING:
			prefName = PreferenceConstants.Integers.ALL_COMPILE_TIMEOUT.name();
			break;
		case ANALYZING:
			prefName = PreferenceConstants.Integers.ALL_ANALYZE_TIMEOUT.name();
			break;
		default:
			return -1;
		}
		return prefs.getInt(prefName);
	}
	
	/* (non-Javadoc)
	 * @see technion.prime.DefaultOptions#isStageParallel(technion.prime.utils.Stage)
	 */
	@Override
	public boolean isStageParallel(Stage stage) {
		String perfName;
		switch (stage) {
		case COMPILING:
			perfName = PreferenceConstants.Booleans.COMPILE_IN_PARALLEL.name();
			break;
		case ANALYZING:
			perfName = PreferenceConstants.Booleans.ANALYZE_IN_PARALLEL.name();
			break;
		default:
			return false;
		}
		return prefs.getBoolean(perfName);
	}
	
	/* (non-Javadoc)
	 * @see technion.prime.DefaultOptions#shouldGenerateOutputFromUnclustered()
	 */
	@Override
	public boolean shouldGenerateOutputFromUnclustered() {
		return prefs.getBoolean(PreferenceConstants.Booleans.GENERATE_UNCLUSTERED.name());
	}
	
	/* (non-Javadoc)
	 * @see technion.prime.DefaultOptions#shouldShowExceptions()
	 */
	@Override
	public boolean shouldShowExceptions() {
		return prefs.getBoolean(PreferenceConstants.Booleans.SHOW_EXCEPTIONS.name());
	}
	
	public IProgressMonitor getEclipseMonitor() {
		return eclipseMonitor;
	}
	
	public void setEclipseMonitor(IProgressMonitor m) {
		eclipseMonitor = m;
	}
	
	/* (non-Javadoc)
	 * @see technion.prime.DefaultOptions#getCvsExecutablePath()
	 */
	@Override
	public String getCvsExecutablePath() {
		return prefs.getString(PreferenceConstants.Strings.CVS_PATH.name());
	}
	
	/* (non-Javadoc)
	 * @see technion.prime.DefaultOptions#getGitExecutablePath()
	 */
	@Override
	public String getGitExecutablePath() {
		return prefs.getString(PreferenceConstants.Strings.GIT_PATH.name());
	}
	
	/* (non-Javadoc)
	 * @see technion.prime.DefaultOptions#getOutputDir()
	 */
	@Override
	public String getOutputDir() {
		return prefs.getString(PreferenceConstants.Strings.OUTPUT_PATH.name());
	}
	
	/* (non-Javadoc)
	 * @see technion.prime.DefaultOptions#getParallelOperationsThreadCount()
	 */
	@Override
	public int getParallelOperationsThreadCount() {
		return prefs.getInt(PreferenceConstants.Integers.THREAD_POOL_SIZE.name());
	}
	
	/* (non-Javadoc)
	 * @see technion.prime.DefaultOptions#getTempDir()
	 */
	@Override
	public String getTempDir() {
		return prefs.getString(PreferenceConstants.Strings.TEMPDIR_PATH.name());
	}
	
	/* (non-Javadoc)
	 * @see technion.prime.DefaultOptions#isMonitoredByEclipse()
	 */
	@Override
	public boolean isMonitoredByEclipse() {
		return eclipseMonitor != null;
	}

	public String getLocalInputDir() {
		return prefs.getString(PreferenceConstants.Strings.DEBUG_PATH.name());
	}

	public void setLocalInputDir(String path) {
		prefs.setValue(PreferenceConstants.Strings.DEBUG_PATH.name(), path);
	}

	public int getDefaultNumSamples() {
		return prefs.getInt(PreferenceConstants.Integers.NUM_RESULTS.name());
	}

	public void setDefaultNumSamples(int numSamples) {
		prefs.setValue(PreferenceConstants.Integers.NUM_RESULTS.name(), numSamples);
	}

	public void setStageTimeout(Stage stage, long timeout) {
		String prefName;
		switch (stage) {
		case DOWNLOADING:
			prefName = PreferenceConstants.Integers.ALL_DOWNLOAD_TIMEOUT.name();
			break;
		case COMPILING:
			prefName = PreferenceConstants.Integers.ALL_COMPILE_TIMEOUT.name();
			break;
		case ANALYZING:
			prefName = PreferenceConstants.Integers.ALL_ANALYZE_TIMEOUT.name();
			break;
		default:
			return;
		}
		prefs.setValue(prefName, timeout);
	}
	
	/* (non-Javadoc)
	 * @see technion.prime.DefaultOptions#getDotExecutablePath()
	 */
	@Override
	public String getDotExecutablePath() {
		return prefs.getString(PreferenceConstants.Strings.DOT_PATH.name());
	}

	public void setTypeFilter(String typeFilter) {
		reportedFilter =
				new StringFilter(Pattern.compile(typeFilter), StringFilter.PATTERN_MATCH_NONE, true, false);
	}
	
	@Override
	public StringFilter getFilterReported() {
		return reportedFilter;
	}
}
