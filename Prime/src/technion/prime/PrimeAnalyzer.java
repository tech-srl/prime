package technion.prime;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Map.Entry;

import org.apache.commons.io.FilenameUtils;

import technion.prime.history.converters.TypeSameClusterer;
import technion.prime.history.converters.MethodSameClusterer;
import technion.prime.utils.PrecompiledClassFile;
import technion.prime.retrieval.Gatherer;
import technion.prime.history.converters.HistoryConverter;
import technion.prime.history.converters.AutomataSameClusterer;
import technion.prime.history.converters.AutomataInclusionClusterer;
import technion.prime.history.converters.ConverterStack;
import technion.prime.history.converters.MethodSimilarityClusterer;
import technion.prime.history.converters.OrderingInclusionClusterer;
import technion.prime.history.converters.OrderingSimilarityClusterer;
import technion.prime.history.converters.RelaxedInclusionClusterer;
import technion.prime.history.converters.TypeIntersectionClusterer;
import technion.prime.history.converters.UnknownEliminator;
import technion.prime.analysis.soot.SootAppLoader;
import technion.prime.utils.CompiledItem;
import technion.prime.analysis.AppAnalyzer;
import technion.prime.dom.App;
import technion.prime.history.HistoryCollection;
import technion.prime.partial_compiler.LoadedFile;
import technion.prime.partial_compiler.PartialCompiler;
import technion.prime.partial_compiler.PartialCompiler.LoadException;
import technion.prime.retrieval.CodeSample;
import technion.prime.statistics.AnalysisDetails;
import technion.prime.utils.ConcurrencyUtils;
import technion.prime.utils.Logger;
import technion.prime.utils.OutputHider;
import technion.prime.utils.Stage;
import technion.prime.utils.Logger.CanceledException;


@SuppressWarnings("unused")
public class PrimeAnalyzer {
	public enum Extension {
		SOURCE("java"),
		CLASS("class"),
		JAR("jar"),
		CACHED_RESULT("cached"),
		REPORT("report.txt")
		;
		private String s;
		Extension(String s) { this.s = s; }
		public String get() { return s; }
	}

	private static final int NUM_HISTORY_THRESHOLD = 100;

	private static ConverterStack converterStack;
	
	private final Options options;
	private final Map<String, Integer> queries = new HashMap<String, Integer>();
	private final Queue<String> sourceFiles = new LinkedList<String>();
	private final Queue<String> jarFiles = new LinkedList<String>();
	private final Queue<String> cachedHcFiles = new LinkedList<String>();
	private final Queue<CompiledItem> compiledItems = new LinkedList<CompiledItem>();
	
	private String identifier;
	private long duration = -1;

	private boolean compileOnly;

	/**
	 * Create a new Prime analyzer with default options.
	 */
	public PrimeAnalyzer() {
		this(new DefaultOptions());
	}
	
	/**
	 * Create a new Prime analyzer.
	 * @param options Analyzer options.
	 */
	public PrimeAnalyzer(Options options) {
		this.options = options;
		initialiaze();
	}
	
	private void initialiaze() {
		Logger.setup(options, options.isVerboseDebugging());
		ConcurrencyUtils.setInstance(options);
	}
	
	public void setCompileOnly(boolean flag) {
		compileOnly = flag;
	}

	public void addQuery(String query, int numResults) {
		queries.put(query, numResults);
	}
	
	public void addInputFile(String path) {
		String extension = FilenameUtils.getExtension(path);
		try {
			if (extension.equals(Extension.SOURCE.get())) sourceFiles.add(path);
			else if (extension.equals(Extension.CLASS.get())) compiledItems.add(new PrecompiledClassFile(path));
			else if (extension.equals(Extension.JAR.get())) jarFiles.add(path);
			else if (extension.equals(Extension.CACHED_RESULT.get())) cachedHcFiles.add(path);
		} catch (IOException e) {
			Logger.warn("Could not add input file " + path);
			Logger.exception(e);
		} catch (NullPointerException e) {
			Logger.warn("Error while adding input file " + path);
			Logger.exception(e);
		}
	}
	
	public HistoryCollection analyze(boolean saveToCache) throws CanceledException {
		long startTime = System.currentTimeMillis();
		identifier = calculateTimestampString();
		
		setupActiveQuery();
		
		HistoryCollection analyzed = produceHistoryCollection();
		if (compileOnly) return null;
		
		HistoryCollection clustered = analyzed;
		if (analyzed.isEmpty() == false) {
			if (saveToCache) saveToCache(analyzed, "_final");
			// Cluster results
			if (shouldCluster(analyzed)) {
				clustered = cluster(analyzed, getConverterStack());
				saveToCache(clustered, "_clustered");
			}
		}
		AnalysisDetails details = options.getOngoingAnalysisDetails();
		details.setFinalHistoryCollection(clustered);
		details.prepareSamples();
		details.prepareReport();
		
		duration = System.currentTimeMillis() - startTime;
		
		generateReport();
		
		generateHTMLReport();
		
		Logger.log("Analysis complete in " + Logger.formattedDuration(duration) + ".");
		
		return clustered;
	}

	private boolean shouldCluster(HistoryCollection hc) {
		return hc.isFromClustering() == false && options.shouldCluster();
	}

	public HistoryCollection produceHistoryCollection() throws CanceledException {
		// Anything which isn't already in compiled form should be converted:
		downloadQueries();
		compileSources();
		if (compileOnly) return null;
		
		AppAnalyzer analyzer = new AppAnalyzer(options);
		HistoryCollection analyzed = options.newHistoryCollection();
		
		// Load and analyze one jar at a time:
		analyzeJars(analyzer, analyzed);
		
		// Load and analyze a chunk of classes at a time:
		analyzeClassChunks(analyzer, analyzed);
		
		// Merge all cached history collections:
		mergeCachedHistoryCollections(analyzed);
		
		Logger.log("Total of " + analyzed.getNumHistories() + " histories.");
		
		return analyzed;
	}
	
	private void setupActiveQuery() {
		String queryName = queries.isEmpty() ? "<local>" : queries.keySet().iterator().next();
		AnalysisDetails details = options.getOngoingAnalysisDetails();
		details.setField(AnalysisDetails.QUERY_STRING, queryName);
		details.setField(AnalysisDetails.TYPE_FILTER, options.getFilterReported().toString());
	}

	private void generateReport() {
		AnalysisDetails details = options.getOngoingAnalysisDetails();
		try {
			String reportFilename = saveReport(details, options.getOutputDir(), calculateTimestampString());
			Logger.log("Report file saved to " + reportFilename);
		} catch (IOException e) {
			Logger.warn("Could not save report file, printing it to output instead:");
			details.printReport();
		}
	}
	
	private void generateHTMLReport() {
		AnalysisDetails details = options.getOngoingAnalysisDetails();
		try {	
			details.saveToHtml(options.getOutputDir(), "index.html");
			details.writeHierarchyFile(options.getOutputDir(), "hierarchy.txt");
			Logger.log("HTML Report file saved");
		} catch (IOException e) {
			Logger.warn("Could not save HTML report file, skipping.");
		}
	}
	
	private String saveReport(AnalysisDetails details, String outputDir, String timestampString) throws IOException {
		String filename = FilenameUtils.concat(outputDir, timestampString + "."
				+ Extension.REPORT.get());
		details.saveReport(filename);
		return filename;
	}

	public long getDuration() {
		if (duration == -1) {
			//throw new IllegalStateException("analysis has not been run yet");
			return 0;
		}
		return duration;
	}

	private String saveToCache(HistoryCollection hc, String suffix) {
		String filename = String.format("%s/%s%s.%s",
				options.getOutputDir(), identifier, suffix, Extension.CACHED_RESULT.get());
		try {
			hc.save(filename);
			Logger.log("Cached result saved to " + filename);
		} catch (IOException e) {
			Logger.warn("Could not save cached results to " + filename);
			Logger.exception(e);
		}
		return filename;
	}
	
	private static String calculateTimestampString() {
		return new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
	}

	/**
	 * @param analyzed
	 * @throws CanceledException
	 */
	private void mergeCachedHistoryCollections(HistoryCollection analyzed)
			throws CanceledException {
		for (String s : cachedHcFiles) {
			try {
				Logger.debug("Loading " + s + "...");
				HistoryCollection hc = HistoryCollection.load(s, options.getHistoryCollectionType());
				hc.recursivelySetOptions(options);
				mergedIntoAnalyzed(analyzed, hc);
			} catch (InterruptedException e) {
				// Swallow
			} catch (IOException e) {
				Logger.exception(e);
			}
		}
	}

	/**
	 * @param analyzer
	 * @param analyzed
	 * @throws CanceledException
	 */
	private void analyzeClassChunks(AppAnalyzer analyzer,
			HistoryCollection analyzed) throws CanceledException {
		List<CompiledItem> itemsInChunk = new LinkedList<CompiledItem>();
		int counter = 0;
		int saveCounter = 0;
		for (CompiledItem item : compiledItems) {
			itemsInChunk.add(item);
			counter++;
			if (itemsInChunk.size() >= 20/*options.getAnalysisChunkSize()*/ || counter == compiledItems.size()) {
				try {
					ConcurrencyUtils.checkState();
					analyzeClasses(analyzer, analyzed, itemsInChunk);
					if (analyzed.getNumHistories() > NUM_HISTORY_THRESHOLD) {
						// The HistoryCollection is growing too much for us to handle.
						// Save an intermediate result, then clear it.
						Logger.log(String.format("Accumulated %d>%d histories, flushing...",
								analyzed.getNumHistories(),
								NUM_HISTORY_THRESHOLD));
						saveToCache(analyzed, "_" + saveCounter++);
						analyzed.clear();
					}
				} catch (InterruptedException e) {
					// Swallow. Yes, this means we lose the whole chunk.
				} catch (RuntimeException e) {
					// We could not process this batch
					Logger.exception(e);
				} finally {
					itemsInChunk.clear();
				}
			}
		}
	}

	/**
	 * @param analyzer
	 * @param analyzed
	 * @param classes
	 * @throws CanceledException
	 * @throws InterruptedException
	 */
	private void analyzeClasses(AppAnalyzer analyzer, HistoryCollection analyzed, List<CompiledItem> classes)
			throws CanceledException, InterruptedException {
		App a = loadClasses(classes);
		HistoryCollection hc = analyzer.analyzeApp(a);
		mergedIntoAnalyzed(analyzed, hc);
	}

	/**
	 * @param analyzer
	 * @param analyzed
	 * @throws CanceledException
	 */
	private void analyzeJars(AppAnalyzer analyzer, HistoryCollection analyzed)
			throws CanceledException {
		for (final String jar : jarFiles) {
			try {
				@SuppressWarnings("serial")
				App a = loadJars(new LinkedList<String>() {{add(jar);}});
				HistoryCollection hc = analyzer.analyzeApp(a);
				mergedIntoAnalyzed(analyzed, hc);
			} catch (InterruptedException e) {
				// Swallow
			}
		}
	}
	
	/**
	 * @param into Merge into this history collection.
	 * @param from Merge from this history collection.
	 */
	private void mergedIntoAnalyzed(HistoryCollection into, HistoryCollection from) throws InterruptedException, CanceledException {
		into.unionFrom(from);
		into.filterEmptyHistories();
	}

	private void compileSources() throws CanceledException {
		if (sourceFiles.isEmpty()) return;
		Logger.startStage(Stage.COMPILING, sourceFiles.size());
		PartialCompiler.startBatch();
		int count = 0;
		int index = 0;
		for (String s : sourceFiles) {
			index++;
			if (index % 100 == 0) {
				PartialCompiler.endBatch();
				PartialCompiler.startBatch();
			}
			try {
				Logger.progress();
				Collection<CompiledItem> compiled = compile(s);
				Logger.log(String.format("Compiled %d classes from source %d/%d: %s",
						compiled.size(),
						index,
						sourceFiles.size(),
						s));

				if (compiled.isEmpty()) continue;
				
				compiledItems.addAll(compiled);
				count++;
			} catch (InterruptedException ex) {
				// Swallow
			}
		}
		PartialCompiler.endBatch();
		PartialCompiler.cleanup();
		Logger.endStage(String.format("Compiled %d/%d sources", count, sourceFiles.size()));
	}

	private void downloadQueries() throws CanceledException {
		if (queries.isEmpty()) return;
		List<CodeSample> samples = new LinkedList<CodeSample>();
		for (Entry<String, Integer> e : queries.entrySet()) {
			try {
				samples.addAll(search(e.getKey(), e.getValue()));
			} catch (InterruptedException ex) {
				// Swallow
			}
		}
		
		Logger.startStage(Stage.DOWNLOADING, samples.size());
		int count = 0;
		for (CodeSample s : samples) {
			try {
				String filename = download(s);
				if (filename == null) continue;
				sourceFiles.add(filename);
				count++;
				Logger.progress();
			} catch (InterruptedException ex) {
				// Swallow
			}
		}
		Logger.endStage(String.format("Saved %d/%d samples", count, samples.size()));
	}

	/**
	 * Search for results for the given query.
	 * @param query Search query.
	 * @param numResults Number of results to search for.
	 * @return An array of the found code samples.
	 */
	private Collection<CodeSample> search(String query, int numResults) throws InterruptedException, CanceledException {
		Logger.startStage(Stage.SEARCHING, numResults);
		Gatherer g = options.getGatherer();
		List<CodeSample> samples = g.getNextSamples(
				new technion.prime.retrieval.Query(query), numResults);
		Logger.endStage("located " + samples.size() + " results");
		return samples;
	}
	
	/**
	 * Download a code sample into a file under the temp dir.
	 * @param sample Code sample to download.
	 * @return Full paths of saved files.
	 */
	private String download(CodeSample sample) throws CanceledException, InterruptedException {
		return sample.getFilename();
	}
	
	/**
	 * Compile a file.
	 * @param filename Java source file.
	 * @return A collection containing all the files compiled from this file. In case the compilation failed, this will be empty.
	 */
	private Collection<CompiledItem> compile(String filename) throws CanceledException, InterruptedException {
		try {
			LoadedFile lf = PartialCompiler.loadFile(filename);
			ConcurrencyUtils.checkState();
			OutputHider outputHider = new OutputHider();
			Collection<CompiledItem> classes = null;
			try {
				classes = lf.compile(options.getTempDir());
			} finally {
				outputHider.release();
			}
			ConcurrencyUtils.checkState();
			return classes;
//		} catch (FileNotFoundException e) {
//			if (StatisticsManager.isActiveQuery())
//				StatisticsManager.getActiveQuery()
//						.addUncompilableFile(filename);
//			Logger.warn("could not load " + filename);
//			Logger.exception(e);
		} catch (LoadException e) {
			options.getOngoingAnalysisDetails().addUncompilableFile(filename);
			Logger.warn("could not load " + filename);
			Logger.exception(e);
		} catch (RuntimeException e) {
			options.getOngoingAnalysisDetails().addUncompilableFile(filename);
			Logger.warn("could not compile " + filename);
			Logger.exception(e);
		}
		return new LinkedList<CompiledItem>();
	}
	
	private App loadJars(Collection<String> jars) throws CanceledException {
		SootAppLoader loader = new SootAppLoader(options);
		loader.addEntireJars(jars);
		return loader.load();
	}
	
	private App loadClasses(Collection<CompiledItem> items) throws CanceledException {
		SootAppLoader loader = new SootAppLoader(options);
		loader.addCompiledItems(items);
		return loader.load();
	}
	
	public ConverterStack getConverterStack() {
		if (converterStack == null) {
			converterStack = createNewConverterStack();
		}
		return converterStack;
	}
	
	private ConverterStack createNewConverterStack() {
		// Should be sorted from "strictest" to "most flexible", where a stricter clusterer
		// is one merging together histories which are more closely related.
		
		HistoryConverter[] converters = new HistoryConverter[] {
				// By automata
//				new AutomataInclusionClusterer(options),
				new AutomataSameClusterer(options),
				new RelaxedInclusionClusterer(options),
				new UnknownEliminator(options),
				new AutomataSameClusterer(options),
				new RelaxedInclusionClusterer(options),
//				new AutomataSameClusterer(options),
//				new AutomataInclusionClusterer(options),
				// By ordering
//				new OrderingInclusionClusterer(options),
//				new OrderingSimilarityClusterer(options),
				// By methods
//				new MethodSameClusterer(options),
//				new MethodSimilarityClusterer(options),
				// By method types
				new TypeSameClusterer(options),
//				new TypeInclusionClusterer(options),
				new TypeIntersectionClusterer(options),
//				new TypeSimilarityClusterer(options),
			};
		return new ConverterStack(options, converters);
	}

	private HistoryCollection cluster(HistoryCollection hc, ConverterStack cs) throws CanceledException {
		Logger.startStage(Stage.CLUSTERING, cs.size() + 3);
		hc.clearAllSources();
		HistoryCollection result;
		try {
			result = getConverterStack().convert(hc);
			Logger.progress();
//			Logger.log("Generating output files under " + options.getOutputDir());
//			getConverterStack().generateOutputFiles(options.getOutputDir());
			Logger.progress();
		} catch (InterruptedException e) {
			return hc;
		}
		Logger.endStage(String.format("clustered %d nodes in %d automata into %d nodes in %d automata",
				hc.getNumNodes(), hc.getNumHistories(),
				result.getNumNodes(), result.getNumHistories()));
		return result;
	}
	
	/**
	 * @param items Compiled items to analyze.
	 */
	public void addCompiledItems(Collection<CompiledItem> items) {
		compiledItems.addAll(items);
	}
	
}
