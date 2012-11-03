package technion.prime.postprocessing.search;

import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.validators.PositiveInteger;

import technion.prime.DefaultOptions;
import technion.prime.Options;
import technion.prime.dom.AppMethodRef;
import technion.prime.history.History;
import technion.prime.history.HistoryCollection;
import technion.prime.history.Ordering;
import technion.prime.history.converters.AutomataInclusionClusterer;
import technion.prime.history.converters.AutomataSameClusterer;
import technion.prime.history.converters.ConverterStack;
import technion.prime.history.converters.HistoryConverter;
import technion.prime.history.converters.MergeAllClusterer;
import technion.prime.history.converters.MethodSameClusterer;
import technion.prime.history.converters.MethodSimilarityClusterer;
import technion.prime.history.converters.OrderingInclusionClusterer;
import technion.prime.history.converters.OrderingSimilarityClusterer;
import technion.prime.history.converters.RelaxedInclusionClusterer;
import technion.prime.history.converters.TypeIntersectionClusterer;
import technion.prime.history.converters.TypeSameClusterer;
import technion.prime.history.converters.UnknownEliminator;
import technion.prime.history.edgeset.Edge;
import technion.prime.history.edgeset.EdgeHistory;
import technion.prime.history.edgeset.EdgeHistoryBuilder;
import technion.prime.history.edgeset.EdgeHistoryBuilder.UnknownType;
import technion.prime.history.edgeset.EdgeHistoryCollection;
import technion.prime.history.edgeset.EdgeNode;
import technion.prime.history.edgeset.EdgeSequence;
import technion.prime.utils.ConcurrencyUtils;
import technion.prime.utils.JavaFileUtils;
import technion.prime.utils.Logger;
import technion.prime.utils.Logger.CanceledException;
import technion.prime.utils.MultiMap;

@SuppressWarnings("unused")
public class Search {
	private static final int MAX_NUM_RESULT = 2 << 20;

	@Parameter(names = {"-b", "--base"}, required = true,
			description = "Path to a base cached file or a folder containing cached files")
	private String baseHistoryCollectionPath = ".";
	
	@Parameter(names = {"-n", "--num"}, required = false, validateWith = PositiveInteger.class,
			description = "Maximum number of search results to return. " +
			"Omitting this means returning all the found results " +
			"(capped at " + MAX_NUM_RESULT + ")")
	private int numResults = MAX_NUM_RESULT;
	
	@Parameter(names = {"-q", "--query"}, required = false,
			description = "Path to a query cached file or a folder containing cached files")
	private String queryHistoryCollectionPath;
	
	@Parameter(names = {"-o", "--output-file"}, required = false,
			description = "Output file path")
	private String outputFilePath;
	
//	@Parameter(names = {"-s", "--source"}, required = false,
//			description = "If provided, search results will be searched for in the directory " +
//			"provided by this option, and the code snippets will be displayed if found")
//	private String sourceRootPath;
	
	private Options options;
	
	public static void main(String[] args) {
		new Search(new DefaultOptions()).search(args);
	}
	
	public Search(Options options) {
		this.options = options;
	}
	
	public void search(String[] args) {
		try {
			new JCommander(this, args);
		} catch (ParameterException e) {
			System.err.println(e.getMessage());
			new JCommander(this).usage();
		}
		
		Logger.setup(options, true);
		HistoryCollection base = null;
		try {
			Logger.log("Loading history collection(s)...");
			base = JavaFileUtils.loadAllHistoryCollections(options, baseHistoryCollectionPath);
		} catch (IOException e) {
			Logger.error("Could not load history collection: " + e.getMessage());
			return;
		}
		if (baseHistoryCollectionPath.endsWith("preprocessed.cached") == false) {
			try {
				base = preprocessBase(base);
			} catch (InterruptedException e) {
				Logger.warn("Could not complete clustering");
			} catch (CanceledException e) {
				// Silently quit
				return;
			}
		}
		System.out.println("Total number of histories in base is " + base.getNumHistories());
		try {
			if (queryHistoryCollectionPath != null) {
				HistoryCollection query = null;
				try {
					query = JavaFileUtils.loadAllHistoryCollections(options, queryHistoryCollectionPath);
				} catch (IOException e) {
					Logger.error("Could not load query histories: " + e.getMessage());
					return;
				}
				printSearchHistogram(query, base);
			} else {
				History query = createQuery(options);
				searchAndPrintResults(query, base);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (CanceledException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	private HistoryCollection intersectWithQuery(HistoryCollection hc, History query) throws InterruptedException, CanceledException {
		Logger.log("Intersecting " + hc.getNumHistories() + " results by query...");
		HistoryCollection result = options.newHistoryCollection();
		for (History h : hc.getHistories()) {
			HistoryCollection tempHc = options.newHistoryCollection();
			tempHc.addHistory(h);
			EdgeHistory eliminated = (EdgeHistory)query.eliminateUnknowns(tempHc);
			History intersected = ((EdgeHistory)h).intersect(eliminated);
			intersected.clearSources();
			((Set<EdgeHistory>)intersected.getSources()).add((EdgeHistory)h);
			intersected.setTitle("intersected");
			result.addHistory(intersected);
		}
		return result;
	}

	private void printSearchHistogram(HistoryCollection query, HistoryCollection base) throws InterruptedException, CanceledException {
		double threshold = 0.001;
		MultiMap<Integer, History> historiesByDepth = new MultiMap<Integer, History>();
		TreeSet<Integer> depths = new TreeSet<Integer>();
		for (History h : query.getHistories()) {
			int depth;
			try {
				depth = h.getDepth();
			} catch (InterruptedException e) {
				Logger.error("Could not calculate history depth");
				Logger.exception(e);
				return;
			} catch (CanceledException e) {
				return;
			}
			depths.add(depth);
			historiesByDepth.put(depth, h);
		}
		System.out.printf("Depth\t# histories\tAverage # matches > %.2f similarity\tStands for%n", threshold);
		for (int depth : depths) {
			int sumUnconsolidated = 0;
			int sumConsolidated = 0;
			int count = 0;
			for (History h : historiesByDepth.getAll(depth)) {
				Collection<SearchResult> results = search(h, base);
				for (SearchResult sr : results) {
					if (sr.getScore() < threshold || isNoise(sr.getHistory())) continue;
					sumUnconsolidated += getSourceHistories(sr.getHistory()).size();
					sumConsolidated++;
				}
				count++;
			}
			if (count == 0) continue;
			System.out.printf("%d\t%d\t%.2f\t%.2f%n",
					depth,
					count,
					(double) sumConsolidated / count,
					(double) sumUnconsolidated / count);
		}
	}

	private Set<History> getSourceHistories(History h) {
		Set<History> result = new HashSet<History>();
		LinkedList<History> queue = new LinkedList<History>();
		queue.add(h);
		while (queue.isEmpty() == false) {
			History current = queue.pop();
			if (current.getSources().isEmpty()) {
				result.add(current);
				continue;
			}
			for (History source : current.getSources()) {
				queue.add(source);
			}
		}
		return result;
	}
	
	public HistoryCollection searchAndReturnResult(History h, HistoryCollection base) throws InterruptedException, CanceledException {
		base = preprocessBase(base);
		return searchAndReturnResultsFromPreprocessed(h, base);
	}
	
	public HistoryCollection searchAndReturnResultsFromPreprocessed(History h, HistoryCollection base) throws InterruptedException, CanceledException {
		HistoryCollection hc = options.newHistoryCollection();
		List<SearchResult> results = search(h, base);
		for (SearchResult sr : results) {
			if (sr.getScore() > 0) {
				History resultHistory = sr.getHistory();
				resultHistory.setTitle(sr.getScore() + ": " + resultHistory.getTitle());
				if (isNoise(resultHistory)) continue; // ignore noise
				if (sr.getScore() > 0) {
					hc.addHistory(resultHistory);
				}
			}
		}
		hc = postProcess(hc, h);
		return hc;
	}

	private void searchAndPrintResults(History h, HistoryCollection base) throws InterruptedException, CanceledException {
		List<SearchResult> results = search(h, base);
		Collections.sort(results);
		
		HistoryCollection hc = options.newHistoryCollection();
		
		for (int i = 0; i < Math.min(numResults, results.size()); i++) {
			SearchResult sr = results.get(i);
			History resultHistory = sr.getHistory();
			resultHistory.setTitle(sr.getScore() + ": " + resultHistory.getTitle());
			if (isNoise(resultHistory)) continue; // ignore noise
			if (sr.getScore() > 0.0) {
				hc.addHistory(resultHistory);
				printResult(sr);
			}
		}
		
		hc = postProcess(hc, h);
		
		if (hc.getNumHistories() > 0) {
			String outputFilename = outputFilePath != null? outputFilePath :
				String.format("%s/%s.cached",
					options.getOutputDir(),
					new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date()));
			try {
				System.out.println("Saving results to " + outputFilename + "...");
				hc.save(outputFilename);
				System.out.println("Done.");
			} catch (IOException e) {
				System.err.println("Could not save results file: " + e.getMessage());
			}
		} else {
			System.out.println("No results found.");
		}
	}

	private HistoryCollection extractTopSequences(History query, HistoryCollection hc, int numSequences) throws InterruptedException, CanceledException {
		Logger.log(String.format("Extracting %d sequences from each of the %d results...",
				numSequences, hc.getNumHistories()));
		HistoryCollection result = options.newHistoryCollection();
		for (History h : hc.getHistories()) {
			result.unionFrom(extractTopSequences(query, h, numSequences));
		}
		Logger.log("Done extracting top sequences.");
		return result;
	}
	
	@SuppressWarnings("unchecked")
	HistoryCollection extractTopSequences(History query, History h, int numSequences) throws InterruptedException, CanceledException {
		EdgeHistory eh = (EdgeHistory)h;
		Comparator<EdgeSequence> comparer = new Comparator<EdgeSequence>() {
			@Override
			public int compare(EdgeSequence s1, EdgeSequence s2) {
				return Double.compare(calculateSequenceScore(s1), calculateSequenceScore(s2));
			}
		};
		eh = eh.normalize();
		PriorityQueue<EdgeSequence> top = new PriorityQueue<EdgeSequence>(numSequences, comparer);
		
		int sequenceCounter = 1;
		for (EdgeSequence s : eh.buildMethodSequences(-1, 1, null)) {
			ConcurrencyUtils.checkState();
			sequenceCounter++;
			if (sequenceCounter % 10000 == 0) {
				Logger.log("  analyzing sequence " + sequenceCounter + ": " + s.toString());
			}
			if (query != null && ((EdgeHistory)s.createHistory(options)).includesWithUnknown(query) == false) {
				continue;
			}
			top.add(s);
			if (top.size() > numSequences) top.remove();
		}
		HistoryCollection result = options.newHistoryCollection();
		for (EdgeSequence s : top) {
			History newHistory = s.createHistory(options);
			newHistory.clearSources();
			((Set<EdgeHistory>)newHistory.getSources()).add(eh);
			newHistory.setTitle("Sequence with score " + calculateSequenceScore(s));
			result.addHistory(newHistory);
		}
		return result;
	}
	
	private double calculateSequenceScore(EdgeSequence s) {
		double chance = 1;
		for (Edge e : s) {
			chance *= e.getWeight();
		}
		return chance;
//		double sum = 0;
//		for (Edge e : s) {
//			sum += e.getWeight();
//		}
//		return sum;
	}

	private boolean isNoise(History resultHistory) {
		return resultHistory.getTitle().contains(" (noise): ");
	}

	private void printResult(SearchResult r) {
		String title = "";
		for (History h : getSourceHistories(r.getHistory())) {
			title += h.getTitle() + " ";
		}
//		History exampleHistory = getExampleHistory(r.getHistory());
//		String title = exampleHistory.getTitle();
		double score = r.getScore();
		System.out.printf("%.2f\t%d histories\t%s%n", score, getSourceHistories(r.getHistory()).size(),
				title);
	}

	private static History getExampleHistory(History h) {
		History result = h;
		while (result.getSources().isEmpty() == false) {
			result = result.getSources().iterator().next();
		}
		return result;
	}
	
	private HistoryCollection preprocessBase(HistoryCollection base)
			throws InterruptedException, CanceledException {
		ConverterStack cs = new ConverterStack(options, new HistoryConverter[] {
				new AutomataSameClusterer(options),
//				new AutomataInclusionClusterer(options),
				new RelaxedInclusionClusterer(options),
				new UnknownEliminator(options),
				new AutomataSameClusterer(options),
//				new RelaxedInclusionClusterer(options),
//				new AutomataInclusionClusterer(options),
//				new OrderingInclusionClusterer(options),
//				new OrderingSimilarityClusterer(options),
//				new MethodSameClusterer(options),
//				new MethodSimilarityClusterer(options),
//				new TypeSameClusterer(options),
//				new TypeIntersectionClusterer(options),
//				new MergeAllClusterer(options),
		});
		return cs.convert(base);
	}
	
	private HistoryCollection postProcess(HistoryCollection hc, History h) throws InterruptedException, CanceledException {
		HistoryCollection hc_new = hc;
		
//		hc = hc_new;
//		HistoryCollection hc_new = intersectWithQuery(hc, h);
//		if (hc_new.isEmpty()) return hc;

		hc = hc_new;
		hc_new = extractTopSequences(h, hc, 20);
		if (hc_new.isEmpty()) return hc;
		
		hc = hc_new;
		hc_new = removeNonIncluded(hc, h);
		if (hc_new.isEmpty()) return hc;
		
		hc = hc_new;
		hc_new = new AutomataSameClusterer(options).convert(hc);
		if (hc_new.getNumHistories() >= hc.getNumHistories()) return hc;
		
		hc = hc_new;
		hc_new = new AutomataInclusionClusterer(options).convert(hc);
		if (hc_new.getNumHistories() >= hc.getNumHistories()) return hc;
		
		return hc_new;
	}
	
	private HistoryCollection removeNonIncluded(HistoryCollection hc, History query) throws InterruptedException, CanceledException {
		HistoryCollection result = options.newHistoryCollection();
		for (History h : hc.getHistories()) {
			EdgeHistory eh = (EdgeHistory)h;
			if (eh.includesWithUnknown(query)) result.addHistory(eh);
		}
		return result;
	}

	private static History createQuery(Options options) {
		// For ranking table
		// =================
		// selenium click(): 0.05, 8
//		return new EdgeHistoryBuilder(options)
//			.withEdge().fromRoot().to("H1").definedBy("org.openqa.selenium.WebElement").buildEdge()
//			.withEdge().from("H1").to("H2").name("click").definedBy("org.openqa.selenium.WebElement").buildEdge()
//			.buildHistory();
		// cli parse(): 0.05, 8
//		return new EdgeHistoryBuilder(options)
//			.withEdge().fromRoot().to("H1").definedBy("org.apache.commons.cli.CommandLineParser").buildEdge()
//			.withEdge().from("H1").to("H2").name("getValue").params("org.apache.commons.cli.Option").definedBy("org.apache.commons.cli.CommandLine").buildEdge()
//			.buildHistory();
		// cli parse(): 0.05, 8
//		return new EdgeHistoryBuilder(options)
//			.withEdge().fromRoot().to("H1").definedBy("org.apache.commons.net.ftp.FTPClient").buildEdge()
//			.withEdge().from("H1").to("H2").name("login").params("java.lang.String", "java.lang.String").definedBy("org.apache.commons.net.ftp.FTPClient").buildEdge()
//			.buildHistory();
		// JDBC PreparedStatement.executeUpdate(): 0.1, 8
//		return new EdgeHistoryBuilder(options)
//			.withEdge().fromRoot().to("H1").definedBy("java.sql.Connection").buildEdge()
//			.withEdge().from("H1").to("H2").name("executeUpdate").definedBy("java.sql.PreparedStatement").buildEdge()
//			.buildHistory();
		// JDBC PreparedStatement.executeUpdate(): 0.1, 12
//		return new EdgeHistoryBuilder(options)
//			.withEdge().fromRoot().to("H1").definedBy("java.sql.Connection").buildEdge()
//			.withEdge().from("H1").to("H2").name("rollback").definedBy("java.sql.Connection").buildEdge()
//			.buildHistory();
		// GEF Command.canExecute()
//		return new EdgeHistoryBuilder(options)
//			.withEdge().fromRoot().to("H1").definedBy("org.eclipse.gef.GraphicalViewer").buildEdge()
//			.withEdge().from("H1").to("H2").name("setEditPartFactory").params("org.eclipse.gef.EditPartFactory").definedBy("org.eclipse.gef.GraphicalViewer").buildEdge()
//			.buildHistory();
		// UI PlatformUI.createAndRunWorkbench
//		return new EdgeHistoryBuilder(options)
//			.withEdge().fromRoot().to("H1").definedBy("org.eclipse.ui.IEditorInput").buildEdge()
//			.withEdge().from("H1").to("H2").name("getName").params().definedBy("org.eclipse.ui.IEditorInput").buildEdge()
//			.buildHistory();
//		return new EdgeHistoryBuilder(options)
//			.withEdge().fromRoot().to("H1").definedBy("org.eclipse.ui.ISelection").buildEdge()
//			.withEdge().from("H1").to("H2").name("isEmpty").params().definedBy("org.eclipse.ui.ISelection").buildEdge()
//			.buildHistory();
		// org.eclipse.jdt.core.dom.CompilationUnit.accept(org.eclipse.jdt.core.dom.ASTVisitor)
		return new EdgeHistoryBuilder(options)
			.withEdge().fromRoot().to("H1").definedBy("org.eclipse.core.resources.IProject").buildEdge()
			.withEdge().from("H1").to("H2").name("open").params().definedBy("org.eclipse.core.resources.IProject").buildEdge()
			.buildHistory();
		// =======================
//		return new EdgeHistoryBuilder(options)
//				.withEdge().fromRoot().to("H1").name("<init>").definedBy("org.openqa.selenium.firefox.FirefoxDriver").buildEdge()
//				.withEdge().from("H1").to("H2").definedBy("org.openqa.selenium.WebDriver").buildEdge()
//				.withEdge().from("H2").to("H3").name("click").definedBy("org.openqa.selenium.WebElement").buildEdge()
//				.buildHistory();
//		return new EdgeHistoryBuilder(options)
//				.withEdge().fromRoot().to("H1").name("<init>").definedBy("org.openqa.selenium.firefox.FirefoxDriver").buildEdge()
//				.withEdge().from("H1").to("H2").definedBy("org.openqa.selenium.WebDriver").buildEdge()
//				.withEdge().from("H2").to("H3").name("submit").definedBy("org.openqa.selenium.WebElement").buildEdge()
//				.buildHistory();
//		return new EdgeHistoryBuilder(options)
//				.withEdge().fromRoot().to("H1").name("<init>").definedBy("org.openqa.selenium.htmlunit.HtmlUnitDriver").buildEdge()
//				.withEdge().from("H1").to("H2").definedBy("org.openqa.selenium.WebDriver").buildEdge()
//				.withEdge().from("H2").to("H3").name("getPageSource").definedBy("org.openqa.selenium.WebDriver").buildEdge()
//				.buildHistory();
//		return new EdgeHistoryBuilder(options)
//				.withEdge().fromRoot().to("H1").definedBy("org.apache.commons.net.ftp.FTPClient").buildEdge()
//				.withEdge().from("H1").to("H2").name("login").params("java.lang.String", "java.lang.String").definedBy("org.apache.commons.net.ftp.FTPClient").buildEdge()
//				.withEdge().from("H2").to("H3").definedBy("org.apache.commons.net.ftp.FTPClient").buildEdge()
//				.withEdge().from("H3").to("H4").name("retrieveFile").params("java.lang.String", "java.io.FileOutputStream").definedBy("org.apache.commons.net.ftp.FTPClient").buildEdge()
//				.buildHistory();
//		return new EdgeHistoryBuilder(options)
//			.withEdge().fromRoot().to("H1").definedBy("org.eclipse.ui.part.IPageSite").buildEdge()
//			.withEdge().from("H1").to("H2").name("getMenuManager").definedBy("org.eclipse.ui.IActionBars").buildEdge()
//			.buildHistory();
//		return new EdgeHistoryBuilder(options)
//			.withEdge().fromRoot().to("H1").definedBy("java.sql.Connection").buildEdge()
//			.withEdge().from("H1").to("H2").name("executeQuery").params("java.lang.String").definedBy("java.sql.Statement").buildEdge()
//			.withEdge().from("H2").to("H3").params("java.lang.String").definedBy("java.sql.ResultSet").buildEdge()
//			.withEdge().from("H3").to("H4").name("getString").params("java.lang.String").definedBy("java.sql.ResultSet").buildEdge()
//			.buildHistory();
//		return new EdgeHistoryBuilder(options)
//			.withEdge().fromRoot().to("H1").definedBy("javax.naming.InitialContext").buildEdge()
//			.withEdge().from("H1").to("H2").name("getString").params("java.lang.String").definedBy("java.sql.ResultSet").buildEdge()
//			.buildHistory();
//		return new EdgeHistoryBuilder(options)
//			.withEdge().fromRoot().to("H1").name("<init>").definedBy("org.apache.commons.cli.GnuParser").buildEdge()
//			.withEdge().from("H1").to("H2").definedBy("java.lang.String").buildEdge()
//			.buildHistory();
//		return new EdgeHistoryBuilder(options)
//			.withEdge().fromRoot().to("H1").definedBy("org.apache.commons.cli.OptionBuilder").buildEdge()
//			.withEdge().from("H1").to("H2").name("create").params("java.lang.String").definedBy("org.apache.commons.cli.OptionBuilder").buildEdge()
//			.buildHistory();
//		return new EdgeHistoryBuilder(options)
//			.withEdge().fromRoot().to("H1").name("<init>").definedBy("javax.naming.InitialContext").buildEdge()
//			.withEdge().from("H1").to("H2").definedBy("javax.naming.InitialContext").buildEdge()
//			.withEdge().from("H2").to("H3").name("executeQuery").definedBy("java.sql.PreparedStatement").buildEdge()
//			.withEdge().from("H3").to("H4").definedBy("java.sql.ResultSet").buildEdge()
//			.withEdge().from("H4").to("H5").name("getDouble").params("java.lang.String").definedBy("java.sql.ResultSet").buildEdge()
//			.buildHistory();
//		return new EdgeHistoryBuilder(options)
//			.withEdge().fromRoot().to("H1").definedBy("java.sql.Connection").buildEdge()
//			.withEdge().from("H1").to("H2").name("updateString").params("java.lang.String", "java.lang.String").definedBy("java.sql.ResultSet").buildEdge()
//			.buildHistory();
//		return new EdgeHistoryBuilder(options)
//			.withEdge().fromRoot().to("H1").definedBy("org.eclipse.ui.texteditor.ITextEditor").buildEdge()
//			.withEdge().from("H1").to("H2").name("getOffset").definedBy("org.eclipse.jface.text.ITextSelection").buildEdge()
//			.buildHistory();
//		return new EdgeHistoryBuilder(options)
//			.withEdge().fromRoot().to("H0").name("<init>").definedBy("org.eclipse.gef.editparts.AbstractTreeEditPart").buildEdge()
//			.withEdge().from("H0").to("H1").definedBy("org.eclipse.gef.EditPart").buildEdge()
//			.withEdge().from("H1").to("H2").name("getFigure").definedBy("org.eclipse.GraphicalEditPart").buildEdge()
//			.buildHistory();
//		return new EdgeHistoryBuilder(options)
//			.withEdge().fromRoot().to("H1").definedBy("org.eclipse.gef.EditPart").buildEdge()
//			.withEdge().from("H1").to("H2").name("getBackgroundColor").definedBy("org.eclipse.gef.IFigure").buildEdge()
//			.buildHistory();
//		<init>
//		connect(String)
//		?
//				storeFile(String, InputStream)
//				?
//						disconnect
//		return new EdgeHistoryBuilder(options)
//			.withEdge().fromRoot().to("H1").name("<init>").definedBy("org.apache.commons.net.ftp.FTPClient").buildEdge()
//			.withEdge().from("H1").to("H2").definedBy("org.apache.commons.net.ftp.FTPClient").buildEdge()
//			.withEdge().from("H2").to("H3").name("storeFile").definedBy("org.apache.commons.net.ftp.FTPClient").buildEdge()
//			.withEdge().from("H3").to("H4").name("retrieveFile").params("java.lang.String", "java.io.FileOutputStream").definedBy("org.apache.commons.net.ftp.FTPClient").buildEdge()
//			.buildHistory();
	}


	public static class SearchResult implements Comparable<SearchResult>, Serializable {
		private static final long serialVersionUID = 122399917200464815L;
		
		private final History h;
		private final double score;
		public SearchResult(History h, double score) {
			this.h = h;
			this.score = score;
		}
		public History getHistory() { return h; }
		public double getScore() { return score; }
		
		@Override
		public int compareTo(SearchResult sr) {
			if (this.equals(sr)) return 0;
			if (score != sr.score) return - Double.compare(score, sr.score);
			if (h.getSources().size() != sr.h.getSources().size()) return
					sr.h.getSources().size() - h.getSources().size();
			// The following may return 0 for non-equal objects... but at least
			// it's consistent, so it should be good enough here.
			return hashCode() - sr.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			return obj instanceof SearchResult &&
				h.equals(((SearchResult)obj).h) &&
				score == ((SearchResult)obj).score;
		}
		
		@Override
		public int hashCode() {
			return h.hashCode() ^ Double.valueOf(score).hashCode();
		}
		
		@Override
		public String toString() {
			return String.format("(%.2f: %s)", score, h.toString());
		}
		
	}
	
	private static class Feature {
//		private Set<Ordering> orderings;
		private final History h;

		public Feature(History h) {
//			orderings = h.getOrderings();
			this.h = h;
		}
		
		
		private boolean match(Ordering o1, Ordering o2) {
			return methodsMatch(o1.first, o2.first) && methodsMatch(o1.second, o2.second);
		}
		
		private boolean morePreciseMatch(Ordering lessPrecise, Ordering morePrecise) {
			return methodsMorePreciseMatch(lessPrecise.first, morePrecise.first) &&
					methodsMorePreciseMatch(lessPrecise.second, morePrecise.second);
		}
		
		private boolean methodsMatch(AppMethodRef m1, AppMethodRef m2) {
			return m1.isUnknown() || m2.isUnknown() || m1.equals(m2);
		}
		
		private boolean methodsMorePreciseMatch(AppMethodRef lessPrecise, AppMethodRef morePrecise) {
			return lessPrecise.isUnknown() || lessPrecise.equals(morePrecise);
		}

		public boolean contains(Feature f) {
			try {
				return ((EdgeHistory)h).includesWithUnknown(f.h);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (CanceledException e) {
			}
			return false;
		}
		
		@Override
		public String toString() {
			return h.toString();
		}
	}
	
	public List<SearchResult> search(History query, HistoryCollection base) throws InterruptedException, CanceledException {
		Feature queryFeature = new Feature(query);
		List<SearchResult> result = new ArrayList<SearchResult>(base.getNumHistories());
		for (History h : base.getHistories()) {
			result.add(createSearchResult(queryFeature, h));
		}
		return result;
	}

	private SearchResult createSearchResult(Feature queryFeature, History base) throws InterruptedException, CanceledException {
		double score;
		Feature baseFeature = new Feature(base);
		if (baseFeature.contains(queryFeature) == false) {
			score = 0;
		} else {
			score = base.getDepth();
//			score = base.getNumNodes();
//			score /= 10;
//			if (score > 1) score = 1;
//			score = queryFeature.similarity(baseFeature);
		}
		return new SearchResult(base, score);
	}
	
}
