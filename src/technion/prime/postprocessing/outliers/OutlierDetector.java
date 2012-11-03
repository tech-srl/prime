package technion.prime.postprocessing.outliers;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import technion.prime.DefaultOptions;
import technion.prime.Options;
import technion.prime.PrimeAnalyzer;
import technion.prime.dom.AppMethodRef;
import technion.prime.dom.AppType;
import technion.prime.dom.dummy.DummyAppType;
import technion.prime.history.History;
import technion.prime.history.HistoryCollection;
//import technion.prime.history.converters.AutomataInclusionClusterer;
import technion.prime.history.converters.ConverterStack;
import technion.prime.history.converters.HistoryConverter;
import technion.prime.history.converters.MergeAllClusterer;
import technion.prime.history.converters.AutomataSameClusterer;
//import technion.prime.history.converters.OrderingInclusionClusterer;
//import technion.prime.history.converters.OrderingSameClusterer;
import technion.prime.history.edgeset.AnnotatedEdgeHistory;
import technion.prime.history.edgeset.Edge;
import technion.prime.history.edgeset.EdgeHistory;
import technion.prime.history.edgeset.EdgeSequence;
import technion.prime.utils.JavaFileUtils;
import technion.prime.utils.Logger;
import technion.prime.utils.Logger.CanceledException;
import technion.prime.utils.StringFilter;


public class OutlierDetector {
	private static final int MAX_OUTLIER_COUNT = 10000;
	private static final int MAX_NUM_SEEN_IN_SEQUENCE = 1;
	
	// Static
	// ======
	
	public static void main(String[] args) throws InterruptedException, CanceledException, IOException {
		if (args.length != 5) {
			System.out.printf("Usage is %s <api filter> <base input files> <to analyze files> <threshold> <output folder>%n",
					OutlierDetector.class.getName());
			return;
		}
		
		// Prepare values
		final StringFilter filter = getFilter(args[0]);
		Options options = new DefaultOptions() {
			private static final long serialVersionUID = -1713438534798487481L;
			@Override public StringFilter getFilterOpaqueTypes() { return filter; }
			@Override public StringFilter getFilterReported() { return filter; }
		};
		Logger.setup(options, false);
		double threshold = getThreshold(options, args[3]);
		String outputDir = getOutputDir(options, args[4]);
		
		Logger.log("Loading input...");
		HistoryCollection input = JavaFileUtils.loadAllHistoryCollections(options, args[2]);
		if (input.isEmpty()) {
			Logger.error("No input histories to compare to base histories.");
			return;
		}
		
		Logger.log("Loading base...");
		HistoryCollection base = JavaFileUtils.loadAllHistoryCollections(options, args[1]);
		if (base.isEmpty()) {
			Logger.error("No base histories to compare against.");
			return;
		}
		
		
		base = preprocess(options, base);
		Set<AppType> extraTypes = findExtraTypes(options, base);
		Logger.log(String.format("Whitelisted types: %s", extraTypes));
		
		Logger.log(String.format("Comparing %d histories against %d histories",
				input.getNumHistories(), base.getNumHistories()));
		
		// Detect outliers
		OutlierDetector calculator = new OutlierDetector(options, extraTypes);
		HistoryCollection outliers = calculator.findOutliers(input, base, threshold, outputDir);
		
		// Report
		Logger.log(String.format("%d/%d histories marked as outliers, saving cached file...",
				outliers.getNumHistories(),
				input.getNumHistories()));
		String outputFile = outputDir + "/outliers." + PrimeAnalyzer.Extension.CACHED_RESULT.get();
		outliers.save(outputFile);
		Logger.log("Done, results saved to " + outputFile);
	}
	
	private static Set<AppType> findExtraTypes(Options options, HistoryCollection hc) {
		Set<AppType> result = new HashSet<AppType>();
		// Add all return types from API methods to the set
		for (History h : hc.getHistories()) {
			for (AppMethodRef m : h.getAllParticipatingMethods()) {
				AppType containing = m.getContainingType();
				if (options.getFilterReported().passesFilter(containing.getFullName())) {
					AppType returnType = m.getReturnType();
					if (returnType.isPrimitive() || returnType.isUnknown() || returnType.isVoid()) {
						continue;
					}
					result.add(m.getReturnType());
				}
			}
		}
		result.remove(new DummyAppType("java.lang.Object")); // Object is never interesting
		return result;
	}

	private static HistoryCollection preprocess(Options options, HistoryCollection hc)
			throws InterruptedException, CanceledException {
		ConverterStack cs = new ConverterStack(options, new HistoryConverter[] {
				new AutomataSameClusterer(options),
//				new AutomataInclusionClusterer(options),
//				new OrderingSameClusterer(options),
//				new OrderingInclusionClusterer(options),
				new MergeAllClusterer(options)
		});
		return cs.convert(hc);
	}

	private static double getThreshold(Options options, String source) {
		return Double.valueOf(source);
	}
	
	private static String getOutputDir(Options options, String source) {
		return source;
	}
	
	private static StringFilter getFilter(String source) {
		return new StringFilter(Pattern.compile(source), StringFilter.PATTERN_MATCH_NONE, true, false);
	}
	
	// Instance
	// ========
	
	private final Options options;
	private final Set<AppType> extraTypes;
	private Map<History, AnnotatedEdgeHistory> annotatedHistories;
	
	public OutlierDetector(Options options, Set<AppType> extraTypes) {
		this.options = options;
		this.extraTypes = extraTypes;
	}
	
	public HistoryCollection findOutliers(
			HistoryCollection input,
			HistoryCollection base,
			double threshold,
			String outputDir) throws InterruptedException, CanceledException {
		int sequenceCounter = 0;
		annotatedHistories = new HashMap<History, AnnotatedEdgeHistory>();
		Set<History> outlyingHistories = new HashSet<History>();
		int i = 0;
		outer: for (History h : input.getHistories()) {
			for (EdgeSequence s : sequencesBelowThreshold(h, base, threshold)) {
				outlyingHistories.add(handleOutlierSequence(h, s, sequenceCounter, outputDir));
				sequenceCounter++;
				if (outlyingHistories.size() == MAX_OUTLIER_COUNT) {
					Logger.log(String.format("Truncated after %d results.", MAX_OUTLIER_COUNT));
					break outer;
				}
			}
			if (i++ % 100 == 0) System.out.print(".");
		}
		System.out.println();
		HistoryCollection result = options.newHistoryCollection();
		result.addAll(outlyingHistories);
		return result;
	}
	
	private History handleOutlierSequence(History h, EdgeSequence s, int counter, String outputDir) {
		AnnotatedEdgeHistory annotatedHistory = getOrCreateAnnotated((EdgeHistory)h);
		for (Edge e : s) {
			annotatedHistory.annotate(e);
		}
//		try {
//			annotatedHistory.generateGraphvizOutput(outputDir, counter);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		return annotatedHistory;
	}

	private AnnotatedEdgeHistory getOrCreateAnnotated(EdgeHistory h) {
		AnnotatedEdgeHistory result = annotatedHistories.get(h);
		if (result == null) {
			result = new AnnotatedEdgeHistory(options, h);
			annotatedHistories.put(h, result);
		}
		return result;
	}

	private Collection<EdgeSequence> sequencesBelowThreshold(
			History input, HistoryCollection base, double threshold) {
		Collection<EdgeSequence> result = new LinkedList<EdgeSequence>();
		Iterable<EdgeSequence> sequences =
			((EdgeHistory)input).buildMethodSequences(MAX_NUM_SEEN_IN_SEQUENCE, -1, extraTypes);
		for (EdgeSequence sequence : sequences) {
			sequence.calcChanceAgainst(base);
			if (sequence.getChance() < threshold) result.add(sequence);
		}
		return result;
	}
	
}
