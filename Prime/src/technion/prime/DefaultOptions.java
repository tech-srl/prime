package technion.prime;

import technion.prime.history.edgeset.EdgeHistory;

import technion.prime.history.History;

import java.util.regex.Pattern;

import technion.prime.analysis.issta07.ProgramStateImpl;
import technion.prime.analysis.ProgramState;
import technion.prime.analysis.soot.SootMethodAnalyzer;
import technion.prime.history.ExteriorMatcher;
import technion.prime.history.Matcher;
import technion.prime.analysis.MethodAnalyzer;
import technion.prime.history.edgeset.EdgeHistoryCollection;
import technion.prime.history.HistoryCollection;
import technion.prime.retrieval.googlecodesearch.GoogleCodeSearchGatherer;
import technion.prime.retrieval.Gatherer;
import technion.prime.statistics.AnalysisDetails;
import technion.prime.utils.Stage;
import technion.prime.utils.StringFilter;

/**
 * Default Prime options. For customizations, you can inherit this class
 * and override only the elements you want to change, or use of its pre-made
 * subtypes.
 */
public class DefaultOptions implements Options {
	private static final long serialVersionUID = -4860053516957806791L;
	
	protected String defaultTempFolder = System.getProperty("java.io.tmpdir") + "/prime/temp";
	protected String defaultOutputFolder = System.getProperty("user.home") + "/prime/output";
	protected Matcher matcher = new ExteriorMatcher(1);
	protected AnalysisDetails details = new AnalysisDetails(this);

	protected Pattern JSL_TYPES =
			Pattern.compile("((java|javax|sun|com\\.sun|org\\.w3c|org\\.xml)\\.\\S+)|" + // Built-in Java types
			                "int|long|boolean|byte|float|double|char|" + // Primitives
			                "(\\S+\\[\\])" // Array types
			                );
	
	/* (non-Javadoc)
	 * @see technion.prime.Options#getTempDir()
	 */
	@Override
	public String getTempDir() {
		return defaultTempFolder;
	}

	/* (non-Javadoc)
	 * @see technion.prime.Options#getOutputDir()
	 */
	@Override
	public String getOutputDir() {
		return defaultOutputFolder;
	}
	
	/* (non-Javadoc)
	 * @see technion.prime.Options#getBaseTrackedTypeFilter()
	 */
	@Override
	public StringFilter getFilterBaseTracked() {
		return StringFilter.ALWAYS_PASSING;
	}

	/* (non-Javadoc)
	 * @see technion.prime.Options#getTrackedTypeFilter()
	 */
	@Override
	public StringFilter getFilterReported() {
		return StringFilter.ALWAYS_PASSING;
	}

	/* (non-Javadoc)
	 * @see technion.prime.Options#getAnalyzedTypeFilter()
	 */
	@Override
	public StringFilter getFilterAnalyzed() {
		return new StringFilter(StringFilter.PATTERN_MATCH_ALL, JSL_TYPES, false, true);
	}

	/* (non-Javadoc)
	 * @see technion.prime.Options#getSingleActionTimeout(technion.prime.utils.Stage)
	 */
	@Override
	public long getSingleActionTimeout(Stage stage) {
		long timeout;
		switch (stage) {
			case DOWNLOADING:
				timeout = 30000;
				break;
			case COMPILING:
				timeout = 20000;
				break;
			case ANALYZING:
				timeout = 7000;
				break;
			default:
				return -1;
		}
		return timeout;
	}

	/* (non-Javadoc)
	 * @see technion.prime.Options#getStageTimeout(technion.prime.utils.Stage)
	 */
	@Override
	public long getStageTimeout(Stage stage) {
		return 600000;
	}

	@Override
	public boolean isStageParallel(Stage stage) {
		return false;
	}

	@Override
	public boolean shouldGenerateOutputFromUnclustered() {
		return false;
	}

	@Override
	public boolean shouldShowExceptions() {
		return false;
	}

	@Override
	public boolean isMonitoredByEclipse() {
		return false;
	}

	@Override
	public boolean isVerboseDebugging() {
		return true;
	}

	@Override
	public int getInterproceduralDepth() {
		return 2;
	}

	@Override
	public int getParallelOperationsThreadCount() {
		return 200;
	}

	@Override
	public String getGitExecutablePath() {
		return null;
	}

	@Override
	public String getCvsExecutablePath() {
		return null;
	}

	@Override
	public int getAnalysisChunkSize() {
		return 100;
	}

	@Override
	public Gatherer getGatherer() {
		return new GoogleCodeSearchGatherer(this);
	}

	@Override
	public Class<? extends HistoryCollection> getHistoryCollectionType() {
		return EdgeHistoryCollection.class;
	}

	@Override
	public String getDotExecutablePath() {
		return "dot";
	}

	@Override
	public HistoryCollection newHistoryCollection() {
		return new EdgeHistoryCollection(this);
	}

	@Override
	public MethodAnalyzer newMethodAnalyzer() {
		return new SootMethodAnalyzer(this);
	}

	@Override
	public Matcher getMatcher() {
		return matcher;
	}

	@Override
	public boolean isMethodSimilarityUnionPartial() {
		return false;
	}
	
	@Override
	public ProgramState newProgramState() {
		return new ProgramStateImpl(this);
	}

	@Override
	public History newHistory() {
		return new EdgeHistory(this);
	}

	@Override
	public StringFilter getFilterOpaqueTypes() {
		return new StringFilter(StringFilter.PATTERN_MATCH_NONE, StringFilter.PATTERN_MATCH_ALL, false, false);
	}

	@Override
	public boolean useHistoryInvariant() {
		return false;
	}
	
	@Override
	public boolean isSameTypeRequiredForReceiver() {
		return true;
	}
	
	@Override
	public boolean shouldCluster() {
		return true;
	}

	@Override
	public boolean isMayAnalysis() {
		return true;
	}

	@Override
	public AnalysisDetails getOngoingAnalysisDetails() {
		return details;
	}

	@Override
	public boolean separateUnknownSources() {
		return false;
	}
	
}
