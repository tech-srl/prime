package technion.prime;

import technion.prime.history.History;

import technion.prime.analysis.ProgramState;

import java.io.Serializable;

import technion.prime.history.Matcher;
import technion.prime.analysis.MethodAnalyzer;
import technion.prime.history.HistoryCollection;
import technion.prime.retrieval.Gatherer;
import technion.prime.statistics.AnalysisDetails;
import technion.prime.utils.Stage;
import technion.prime.utils.StringFilter;


public interface Options extends Serializable {
	/**
	 * @return Temporary folder for Prime miscellaneous files.
	 */
	String getTempDir();
	
	/**
	 * @return Output folder where the Prime results will be saved.
	 */
	String getOutputDir();
	
	/**
	 * @return Filter determining which types will have their histories tracked.
	 */
	StringFilter getFilterBaseTracked();
	
	/**
	 * @return Filter determining which types will be reported at the end of the run.
	 */
	StringFilter getFilterReported();
	
	/**
	 * @return Filter determining on which types will have their methods loaded and analyzed.
	 */
	StringFilter getFilterAnalyzed();
	
	/**
	 * @return Filter determining which types will have their methods considered opaque.
	 */
	StringFilter getFilterOpaqueTypes();
	
	/**
	 * @param stage
	 * @return Timeout for a single action of the stage.
	 */
	long getSingleActionTimeout(Stage stage);
	
	/**
	 * @param stage
	 * @return Timeout for the entire stage.
	 */
	long getStageTimeout(Stage stage);
	
	/**
	 * @param stage
	 * @return Whether the stage should be ran in parallel (true) or sequentially (false).
	 */
	boolean isStageParallel(Stage stage);
	
	/**
	 * @return Whether output results from unclustered samples should be generated.
	 */
	boolean shouldGenerateOutputFromUnclustered();
	
	/**
	 * @return Whether exception details should be printed.
	 */
	boolean shouldShowExceptions();
	
	/**
	 * @return Whether the run is monitored by an Eclipse monitor.
	 */
	boolean isMonitoredByEclipse();
	
	/**
	 * @return Whether debugging messages should be verbose.
	 */
	boolean isVerboseDebugging();
	
	/**
	 * @return Number of method calls would should step into during inter-procedural analysis.
	 */
	int getInterproceduralDepth();

	/**
	 * @return The number of threads which should be used for parallel operations.
	 */
	int getParallelOperationsThreadCount();

	/**
	 * @return Full path to where git can be found, or null if git isn't supported.
	 */
	String getGitExecutablePath();

	/**
	 * @return Full path to where cvs can be found, or null if cvs isn't supported.
	 */
	String getCvsExecutablePath();

	/**
	 * @return The number of classes to load and analyze at a time.
	 */
	int getAnalysisChunkSize();

	/**
	 * @return A gatherer to be used for handling queries given to Prime.
	 */
	Gatherer getGatherer();

	/**
	 * @return The actual concrete type used for for the history collection.
	 */
	Class<? extends HistoryCollection> getHistoryCollectionType();

	/**
	 * @return Path to dot binary. May be null, but then some operations
	 * won't be supported.
	 */
	String getDotExecutablePath();

	/**
	 * @return A new, empty, history collection.
	 */
	HistoryCollection newHistoryCollection();

	/**
	 * @return A new method analyzer.
	 */
	MethodAnalyzer newMethodAnalyzer();
	
	/**
	 * @return The matcher used for merging histories.
	 */
	Matcher getMatcher();

	/**
	 * @return Should partial (inclusion) merge be used for the method similarity
	 * clusterer.
	 */
	boolean isMethodSimilarityUnionPartial();

	/**
	 * @return A new, empty, program state.
	 */
	ProgramState newProgramState();

	/**
	 * @return A new initial history.
	 */
	History newHistory();

	/**
	 * Use this for debugging only; can have substantial impact on performance.
	 * @return Whether to verify the structural integrity of a history after each update.
	 */
	boolean useHistoryInvariant();

	/**
	 * @return True if upon encountering x.f() we only consider abstract objects with the type of
	 * x as one of their seen types as candidates for update.
	 */
	boolean isSameTypeRequiredForReceiver();

	/**
	 * @return Whether the results should be clustered or just saved.
	 */
	boolean shouldCluster();

	/**
	 * If this is false, the analysis is not sound.
	 * 
	 * @return Whether the analysis should consider whether some other objects may point to
	 * an abstract object.
	 */
	boolean isMayAnalysis();
	
	/**
	 * @return Ongoing analysis details. The returned value should maintain its state throughout the
	 * analysis.
	 */
	AnalysisDetails getOngoingAnalysisDetails();
	
	
	boolean separateUnknownSources();

}
