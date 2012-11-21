package technion.prime;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import org.apache.commons.io.FilenameUtils;

import technion.prime.analysis.AppAnalyzer;
import technion.prime.analysis.soot.SootAppLoader;
import technion.prime.dom.App;
import technion.prime.history.HistoryCollection;
import technion.prime.history.converters.AutomataInclusionClusterer;
import technion.prime.history.converters.AutomataSameClusterer;
import technion.prime.history.converters.ConverterStack;
import technion.prime.history.converters.HistoryConverter;
import technion.prime.history.converters.MethodSameClusterer;
import technion.prime.history.converters.MethodSimilarityClusterer;
import technion.prime.history.converters.OrderingInclusionClusterer;
import technion.prime.history.converters.OrderingSameClusterer;
import technion.prime.history.converters.OrderingSimilarityClusterer;
import technion.prime.history.converters.RelaxedInclusionClusterer;
import technion.prime.history.converters.TypeInclusionClusterer;
import technion.prime.history.converters.TypeSameClusterer;
import technion.prime.history.converters.TypeSimilarityClusterer;
import technion.prime.history.converters.UnknownEliminator;
import technion.prime.partial_compiler.LoadedFile;
import technion.prime.partial_compiler.PartialCompiler;
import technion.prime.partial_compiler.PartialCompiler.LoadException;
import technion.prime.retrieval.CodeSample;
import technion.prime.retrieval.Gatherer;
import technion.prime.statistics.AnalysisDetails;
import technion.prime.utils.CompiledItem;
import technion.prime.utils.ConcurrencyUtils;
import technion.prime.utils.Logger;
import technion.prime.utils.Logger.CanceledException;
import technion.prime.utils.OutputHider;
import technion.prime.utils.PrecompiledClassFile;
import technion.prime.utils.Stage;


@SuppressWarnings("unused")
public class PrimeAnalyzer {
	public enum Extension {
		SOURCE("java"),
		CLASS("class"),
		JAR("jar"),
		CACHED_RESULT("cached"),
		REPORT("report.txt");
		private String s;

		Extension(String s) {
			this.s = s;
		}

		public String get() {
			return s;
		}
	}

	private static final int NUM_HISTORY_THRESHOLD = 100;

	private static final int COMPILATION_BATCH_SIZE = 10;

	private static final long MB = 1048576;

	private static ConverterStack converterStack;
	private String converterStackFile; 
	
	private final Options options;
	private final Map<String, Integer> queries = new HashMap<String, Integer>();
	private final Queue<String> sourceFiles = new LinkedList<String>();
	private final Queue<String> jarFiles = new LinkedList<String>();
	private final Queue<String> cachedHcFiles = new LinkedList<String>();
	private final Queue<CompiledItem> compiledItems = new LinkedList<CompiledItem>();

	private String identifier;
	private long duration = -1;

	private boolean compileOnly;
	private boolean forceClustering;

	/**
	 * Create a new Prime analyzer with default options.
	 */
	public PrimeAnalyzer() {
		this(new DefaultOptions());
	}

	/**
	 * Create a new Prime analyzer.
	 * 
	 * @param options
	 *            Analyzer options.
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

	public void setForceClustering(boolean flag) {
		forceClustering = flag;
	}

	public void addQuery(String query, int numResults) {
		queries.put(query, numResults);
	}

	public void addInputFile(String path) {
		String extension = FilenameUtils.getExtension(path);
		try {
			if (extension.equals(Extension.SOURCE.get())) sourceFiles.add(path);
			else if (extension.equals(Extension.CLASS.get())) compiledItems
					.add(new PrecompiledClassFile(path));
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
			boolean shouldCluster = shouldCluster(analyzed) || forceClustering;
			Logger.log(String.format("Should cluster %b", shouldCluster));
			if (shouldCluster) {
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
		return (!hc.isFromClustering() && options.shouldCluster());
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
			String reportFilename = saveReport(details, options.getOutputDir(),
					calculateTimestampString());
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

	private String saveReport(AnalysisDetails details, String outputDir, String timestampString)
			throws IOException {
		String filename = FilenameUtils.concat(outputDir, timestampString + "."
				+ Extension.REPORT.get());
		details.saveReport(filename);
		return filename;
	}

	public long getDuration() {
		if (duration == -1) {
			// throw new IllegalStateException("analysis has not been run yet");
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
				HistoryCollection hc = HistoryCollection
						.load(s, options.getHistoryCollectionType());
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
			if (counter % 100 == 0) {
				Logger.log(String.format("---Analyzed %d classes.\n", counter));
			}
			if (itemsInChunk.size() >= 20/* options.getAnalysisChunkSize() */
					|| counter == compiledItems.size()) {
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
	private void analyzeClasses(AppAnalyzer analyzer, HistoryCollection analyzed,
			List<CompiledItem> classes)
			throws CanceledException, InterruptedException {
		App a = loadClasses(classes);
		HistoryCollection hc = analyzer.analyzeApp(a);
		Logger.log(String.format("Analyzed %d classes, produced %d histories", classes.size(),
				hc.getNumHistories()));
		mergedIntoAnalyzed(analyzed, hc);
		Logger.log(String.format("After merging got %d histories", analyzed.getNumHistories()));
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
				App a = loadJars(new LinkedList<String>() {
					{
						add(jar);
					}
				});
				HistoryCollection hc = analyzer.analyzeApp(a);
				mergedIntoAnalyzed(analyzed, hc);
			} catch (InterruptedException e) {
				// Swallow
			}
		}
	}

	/**
	 * @param into
	 *            Merge into this history collection.
	 * @param from
	 *            Merge from this history collection.
	 */
	private void mergedIntoAnalyzed(HistoryCollection into, HistoryCollection from)
			throws InterruptedException, CanceledException {
		into.unionFrom(from);
		into.filterEmptyHistories();
	}

	private void compileSources() throws CanceledException {
		if (sourceFiles.isEmpty()) return;
		Logger.startStage(Stage.COMPILING, sourceFiles.size());
		PartialCompiler.startBatch();
		int count = 0;
		int index = 0;
		for (final String s : sourceFiles) {
			index++;
			if (index % COMPILATION_BATCH_SIZE == 0) {
				PartialCompiler.endBatch();
				long beforeCleanup = Runtime.getRuntime().freeMemory() / MB;
				Logger.log(String.format("Batch ended at index %d with free mem %d", index,
						beforeCleanup));
				PartialCompiler.cleanup();
				long afterCleanup = Runtime.getRuntime().freeMemory() / MB;
				Logger.log(String.format("After cleanup free mem %d (reclaimed %d)", afterCleanup,
						(afterCleanup - beforeCleanup)));
				PartialCompiler.startBatch();
			}
			try {
				Logger.progress();
				Logger.log(String.format("Staring compilation of %s", s));

				/**
				 * EY: this was my hack, I put it aside for a sec --- ScheduledExecutorService
				 * executor = Executors.newScheduledThreadPool(2); final
				 * Future<Collection<CompiledItem>> handler = executor.submit( new
				 * Callable<Collection<CompiledItem>>(){ public Collection<CompiledItem> call()
				 * throws CanceledException, InterruptedException { return compile(s); }});
				 * executor.schedule(new Runnable(){ public void run(){ handler.cancel(true); } },
				 * 10000, TimeUnit.MILLISECONDS); Collection<CompiledItem> compiled = handler.get();
				 **/

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
			// } catch (ExecutionException e) {
			// Logger.log(String.format("Compiler task failed for source %s",s));
			// // Swallow
			// }
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
	 * 
	 * @param query
	 *            Search query.
	 * @param numResults
	 *            Number of results to search for.
	 * @return An array of the found code samples.
	 */
	private Collection<CodeSample> search(String query, int numResults)
			throws InterruptedException, CanceledException {
		Logger.startStage(Stage.SEARCHING, numResults);
		Gatherer g = options.getGatherer();
		List<CodeSample> samples = g.getNextSamples(
				new technion.prime.retrieval.Query(query), numResults);
		Logger.endStage("located " + samples.size() + " results");
		return samples;
	}

	/**
	 * Download a code sample into a file under the temp dir.
	 * 
	 * @param sample
	 *            Code sample to download.
	 * @return Full paths of saved files.
	 */
	private String download(CodeSample sample) throws CanceledException, InterruptedException {
		return sample.getFilename();
	}

	/**
	 * Compile a file.
	 * 
	 * @param filename
	 *            Java source file.
	 * @return A collection containing all the files compiled from this file. In case the
	 *         compilation failed, this will be empty.
	 */
	private Collection<CompiledItem> compile(String filename) throws CanceledException,
			InterruptedException {
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
			// } catch (FileNotFoundException e) {
			// if (StatisticsManager.isActiveQuery())
			// StatisticsManager.getActiveQuery()
			// .addUncompilableFile(filename);
			// Logger.warn("could not load " + filename);
			// Logger.exception(e);
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

	public enum ConverterTypes {
		AUT_SAME("automata-same","AutomataSameClusterer"),
		METHOD_SAME("method-same","MethdSameClusterer"),
		ORDER_SAME("order-same","OrderingSameClusterer"),
		TYPE_SAME("type-same","TypeSameClusterer"),
		AUT_INCL("automata-inclusion","AutomataInclusionClusterer"),
		ORDER_INCL("order-inclusion","OrderingInclusionClusterer"),
		RELAX_INCL("relaxed-inclusion","RelaxedInclusionClusterer"),
		TYPE_INCL("type-inclusion","TypeInclusionClusterer"),
		METHOD_SIM("method-similarity","MethodSimilarityClusterer"),
		ORDER_SIM("order-similarity","OrderingSimilarityClusterer"),
		TYPE_SIM("type-similarity","TypeSimilarityClusterer"),
		UNKNOWN_ELIM("unknown-elimination","UnknownEliminator");
		
		private String name;
		private String impl; 
		
		ConverterTypes(String s,String implClass) {
			this.name = s;
			this.impl = implClass;
		}

		public String getName() {
			return name;
		}
		public String getClassName() {
			return impl;
		}
	}
	
	private ConverterStack readCoverterStackFromFile(String filename) {
		Logger.log(String.format("Reading converter stack from: %s",filename));
		ArrayList<HistoryConverter> converters = new ArrayList<HistoryConverter>();
		try {
			FileInputStream fstream = new FileInputStream(filename);
			Logger.log(String.format("converter stack FD: %s",fstream.getFD().toString()));
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			 
			String line;
			while ((line = br.readLine()) != null) {
				Logger.log(line);
				HistoryConverter hc = getHistoryConverter(line);
				if (hc != null)
					converters.add(hc);
				else
					Logger.log(String.format("got null for converter %s",line));
			}
			in.close();
		} catch (Exception e) {
			Logger.log("Error: " + e.getMessage());
		}
		
		if (converters.isEmpty())
			return null;
		else {
			return new ConverterStack(options, converters.toArray(new HistoryConverter[converters.size()]));
		}
	}

	/** 
	 * forgive me father for I have sinned 
	 * do this properly with reflection at some point. 
	 * @param line
	 * @return
	 */
	private HistoryConverter getHistoryConverter(String line) {
		line = line.trim();
		if (line.equals(ConverterTypes.AUT_SAME.getName())) {
			return new AutomataSameClusterer(options);
		} else if (line.equals(ConverterTypes.METHOD_SAME.getName())) {
			return new MethodSameClusterer(options);
		} else if (line.equals(ConverterTypes.ORDER_SAME.getName())) {
			return new OrderingSameClusterer(options);
		} else if (line.equals(ConverterTypes.TYPE_SAME.getName())) {
			return new TypeSameClusterer(options);
		} else if (line.equals(ConverterTypes.AUT_INCL.getName())) {
			return new AutomataInclusionClusterer(options);
		} else if (line.equals(ConverterTypes.ORDER_INCL.getName())) {
			return new OrderingInclusionClusterer(options);
		} else if (line.equals(ConverterTypes.RELAX_INCL.getName())) {
			return new RelaxedInclusionClusterer(options);
		} else if (line.equals(ConverterTypes.TYPE_INCL.getName())) {
			return new TypeInclusionClusterer(options);
		} else if (line.equals(ConverterTypes.METHOD_SIM.getName())) {
			return new MethodSimilarityClusterer(options);
		} else if (line.equals(ConverterTypes.ORDER_SIM.getName())) {
			return new OrderingSimilarityClusterer(options);
		} else if (line.equals(ConverterTypes.TYPE_SIM.getName())) {
			return new TypeSimilarityClusterer(options);
		} else if (line.equals(ConverterTypes.UNKNOWN_ELIM.getName())) {
			return new UnknownEliminator(options);
		}
		Logger.log("oh no, null!");
		return null;
	}

	private ConverterStack createNewConverterStack() {
		// Should be sorted from "strictest" to "most flexible", where a stricter clusterer
		// is one merging together histories which are more closely related.

		ConverterStack result = null;
		try { 
			result = readCoverterStackFromFile(converterStackFile);
		} catch (Exception e) {
			e.printStackTrace(); 
		}
		if (result != null) return result;
		
		HistoryConverter[] converters = new HistoryConverter[] {
				// By automata
				// new AutomataInclusionClusterer(options),
				new AutomataSameClusterer(options),
				// new RelaxedInclusionClusterer(options),
				// -- new UnknownEliminator(options),
				// -- new AutomataSameClusterer(options),
				// -- new RelaxedInclusionClusterer(options),
				// new AutomataSameClusterer(options),
				// new AutomataInclusionClusterer(options),
				// By ordering
				// new OrderingInclusionClusterer(options),
				// new OrderingSimilarityClusterer(options),
				// By methods
				// new MethodSameClusterer(options),
				// new MethodSimilarityClusterer(options),
				// By method types
				// new TypeSameClusterer(options),
				// new TypeInclusionClusterer(options),
				// new TypeIntersectionClusterer(options),
				// new TypeSimilarityClusterer(options),
		};
		return new ConverterStack(options, converters);
	}

	private HistoryCollection cluster(HistoryCollection hc, ConverterStack cs)
			throws CanceledException {
		Logger.startStage(Stage.CLUSTERING, cs.size() + 3);
		hc.clearAllSources();
		HistoryCollection result;
		try {
			result = getConverterStack().convert(hc);
			Logger.progress();
			// Logger.log("Generating output files under " + options.getOutputDir());
			// getConverterStack().generateOutputFiles(options.getOutputDir());
			Logger.progress();
		} catch (InterruptedException e) {
			return hc;
		}
		Logger.endStage(String.format(
				"clustered %d nodes in %d automata into %d nodes in %d automata",
				hc.getNumNodes(), hc.getNumHistories(),
				result.getNumNodes(), result.getNumHistories()));
		return result;
	}

	/**
	 * @param items
	 *            Compiled items to analyze.
	 */
	public void addCompiledItems(Collection<CompiledItem> items) {
		compiledItems.addAll(items);
	}

	public void setConverterStackFile(String converterStackFile) {
		this.converterStackFile = converterStackFile;
	}

}
