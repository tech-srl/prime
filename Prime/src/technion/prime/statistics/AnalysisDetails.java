package technion.prime.statistics;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import technion.prime.Options;

import technion.prime.dom.AppClass;
import technion.prime.dom.AppType;
import technion.prime.history.History;
import technion.prime.history.HistoryCollection;
import technion.prime.utils.Logger;
import technion.prime.utils.Logger.CanceledException;


public class AnalysisDetails extends FieldHolder {
	public static Field QUERY_STRING = new Field("query string", String.class);
	public static Field TYPE_FILTER = new Field("type filter", String.class);
	public static Field PROCESS_DURATION = new Field("duration", Long.class);
	public static Field NUM_REQUESTED_RESULTS = new Field("results", Integer.class);
	public static Field NUM_AVAILABLE_RESULTS = new Field("available", Integer.class);
	public static Field DOWNLOADED_FILES = new Field("downloaded", Integer.class);
	public static Field COMPILED_CLASSES = new Field("compiled", Integer.class);
	public static Field UNSUPPORTED_REPOSITORIES = new Field("unsupported repos", Integer.class);
	public static Field FAILED_DOWNLOADS = new Field("failed downloads", Integer.class);
	public static Field METHODS_SUCCEEDED = new Field("methods succeeded", Integer.class);
	public static Field TOTAL_METHODS = new Field("total methods", Integer.class);
	public static Field INTERRUPTED_DOWNLOADS = new Field("interrupted downloads", Integer.class);
	public static Field DUPLICATE_FILES = new Field("duplicate files", Integer.class);
	public static Field LOADED_CLASSES = new Field("loaded classes", Integer.class);
	public static Field NUM_SAMPLES = new Field("# samples", Integer.class);

	private Set<Sample> samples = new HashSet<Sample>();
	private Set<String> uncompilableSources = new HashSet<String>();
	private Set<AppClass> unanalyzableClasses = new HashSet<AppClass>();
	private Map<AppType, Integer> seenAsReturnType = new HashMap<AppType, Integer>();
	private Map<AppType, Integer> seenAsParameterType = new HashMap<AppType, Integer>();
	private Map<History, Sample> sampleByHistory = new HashMap<History, Sample>();
	private Map<Sample, History> historyBySample = new HashMap<Sample, History>();

	private final Options options;
	private StringBuilder report;
	private HistoryCollection finalHistoryCollection;

	public AnalysisDetails(Options options) {
		this.options = options;
	}

	public synchronized void addUncompilableFile(String filename) {
		uncompilableSources.add(filename);
	}

	public synchronized void addUnanalyzableClass(AppClass c) {
		unanalyzableClasses.add(c);
	}

	public synchronized void incrementAsReturnType(AppType t) {
		int oldVal = 0;
		if (seenAsReturnType.containsKey(t)) {
			oldVal = seenAsReturnType.get(t);
		}
		seenAsReturnType.put(t, oldVal + 1);
	}

	public synchronized void incrementAsParameterType(AppType t) {
		int oldVal = 0;
		if (seenAsParameterType.containsKey(t)) {
			oldVal = seenAsParameterType.get(t);
		}
		seenAsParameterType.put(t, oldVal + 1);
	}

	public Set<String> getUncompilableSources() {
		return uncompilableSources;
	}

	public Set<AppClass> getUnanalyzableClasses() {
		return unanalyzableClasses;
	}

	public Set<Sample> getSamples() {
		return samples;
	}

	public void prepareSamples() throws CanceledException {
		createSamples(finalHistoryCollection);

		int numSamples = 0;
		for (Sample s : samples) {
			numSamples += getNumSamples(s);
		}
		setField(NUM_SAMPLES, numSamples);
		for (Sample s : samples) {
			updateSamplePercentage(s, numSamples);
		}
	}

	private void createSamples(HistoryCollection hc) throws CanceledException {
		if (hc == null) return;
		for (History h : hc.getHistories()) {
			try {
				samples.add(getHistorySample(h));
			} catch (InterruptedException e) {
				// Continue to next sample
			}
		}
	}

	public History getSampleHistory(Sample s) {
		return historyBySample.get(s);
	}

	private Sample getHistorySample(History h) throws InterruptedException, CanceledException {
		Sample result = sampleByHistory.get(h);
		if (result == null) {
			result = createSample(h);
			sampleByHistory.put(h, result);
			historyBySample.put(result, h);
		}
		return result;
	}

	private Sample createSample(History h) throws InterruptedException, CanceledException {
		Sample s = new Sample();
		s.setField(Sample.NAME, h.getTitle());
		s.setField(Sample.SIZE, h.getNumNodes());
		s.setField(Sample.DEPTH, h.getDepth());
		s.setField(Sample.MAX_DEGREE, h.getMaxDegree());
		s.setField(Sample.AVG_WEIGHT, h.getAverageWeight());
		s.setField(Sample.MAX_WEIGHT, h.getMaximumWeight());
		s.setField(Sample.NUM_TYPES, h.getNumParticipatingTypes());
		s.setField(Sample.NUM_EDGES, h.getNumEdges());
		s.setField(Sample.NUM_UNKNOWN_EDGES, h.getNumUnknownEdges());
		for (History src : h.getSources()) {
			s.addSample(getHistorySample(src));
		}
		return s;
	}

	private void updateSamplePercentage(Sample s, int totalSampleNum) {
		int numSamples = s.getInteger(Sample.NUM_SAMPLES);
		double ps = (double) numSamples / totalSampleNum;
		s.setField(Sample.PERCENTAGE_SAMPLES, ps * 100); // *100 just for % display
		for (Sample inner : s.getSamples()) {
			updateSamplePercentage(inner, numSamples);
		}
	}

	private int getNumSamples(Sample s) {
		if (s.containsOtherSamples() == false) return 1;
		// If there are child samples, start counting with 0, meaning
		// do not count the current sample.
		int result = 0;
		for (Sample child : s.getSamples())
			result += getNumSamples(child);
		s.setField(Sample.NUM_SAMPLES, result);
		return result;
	}

	public void prepareReport() {
		report = new StringBuilder();
		printProcessDetails(report);
		if (samples.isEmpty()) {
			report.append("< no results were mined >\n");
			return;
		}
	}

	public String getProcessDetails() {
		StringBuilder sb = new StringBuilder();
		printProcessDetails(sb);
		return sb.toString();
	}

	public void printReport() {
		System.out.println(report);
	}

	public void saveReport(String filename) throws IOException {
		FileUtils.writeStringToFile(new File(filename), report.toString());
	}

	private void printProcessDetails(StringBuilder sb) {
		sb.append("===============================================================================================================================");
		sb.append("\n");
		sb.append("Query string: " + getField(QUERY_STRING));
		sb.append("\n");
		sb.append("Type filter: " + getField(TYPE_FILTER));
		sb.append("\n");
		sb.append("Google reports " + getField(NUM_AVAILABLE_RESULTS) + " total available results.");
		sb.append("\n");
		sb.append("Out of " + getField(NUM_REQUESTED_RESULTS) + " requested results,");
		sb.append("\n");
		sb.append("  " + getField(UNSUPPORTED_REPOSITORIES)
				+ " were in unsupported repository types;");
		sb.append("\n");
		sb.append("  " + getField(INTERRUPTED_DOWNLOADS) + " timed out;");
		sb.append("\n");
		sb.append("  " + getField(DUPLICATE_FILES)
				+ " were duplicates of files already downloaded;");
		sb.append("\n");
		sb.append("  " + getField(FAILED_DOWNLOADS) + " could not be downloaded for other reasons;");
		sb.append("\n");
		sb.append("  " + getField(DOWNLOADED_FILES) + " were downloaded.");
		sb.append("\n");
		sb.append("Out of the " + getField(DOWNLOADED_FILES) + " downloaded files,");
		sb.append("\n");
		sb.append("  " + uncompilableSources.size() + " did not produce any class files;");
		sb.append("\n");
		sb.append("  " + ((Integer) getField(DOWNLOADED_FILES) - uncompilableSources.size())
				+ " produced a total of " + getField(COMPILED_CLASSES) + " classes.");
		sb.append("\n");
		sb.append("Out of " + getField(COMPILED_CLASSES) + " class files,");
		sb.append("\n");
		sb.append("  " + getField(LOADED_CLASSES) + " were successfully loaded.");
		sb.append("\n");
		sb.append("Out of " + getField(TOTAL_METHODS) + " methods encountered,");
		sb.append("\n");
		sb.append("  " + getField(METHODS_SUCCEEDED) + " were successfully analyzed.");
		sb.append("\n");
		sb.append("Total process time: "
				+ Logger.formattedDuration((Long) getField(PROCESS_DURATION)));
		sb.append("\n");
	}

	// public void saveToHtml(String folder, String indexFilename) throws IOException {}
	private static Field[] htmlFields = new Field[] {
			Sample.NAME,
			Sample.NUM_SAMPLES,
			Sample.MAX_WEIGHT,
			Sample.MAX_DEGREE,
			Sample.AVG_DEGREE
	};

	private static String DOT_IMAGE_EXTENSION = "svg";

	public void saveToHtml(String folder, String indexFilename) throws IOException {
		StringBuilder sb = new StringBuilder();
		appendHtmlHeader(sb);

		for (Sample s : samples) {
			sb.append("<hr />\n");
			sb.append("<hr />\n");
			addHtmlSample(sb, s, folder);
		}

		appendHtmlFooter(sb);
		FileUtils.writeStringToFile(new File(FilenameUtils.concat(folder, indexFilename)),
				sb.toString());
	}

	private void appendHtmlHeader(StringBuilder sb) {
		sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
		sb.append("<html lang=\"en-US\" xml:lang=\"en-US\" xmlns=\"http://www.w3.org/1999/xhtml\">");
		sb.append("<head>\n");
		sb.append("<title>Prime results</title>\n");
		sb.append("</head>\n");
		sb.append("<body>\n");
	}

	private void appendHtmlFooter(StringBuilder sb) {
		sb.append("</body>\n");
		sb.append("</html>\n");
	}

	public void writeHierarchyFile(String folder, String filename) throws IOException {
		String content = new String();
		StringBuffer result = new StringBuffer();
		for (Sample s : samples) {
			result.append(addSampleToHierarchy(content, s));
		}

		String fullname = folder + File.separator + filename;
		FileUtils.writeStringToFile(new File(fullname), result.toString());
	}
	
	private String addSampleToHierarchy(String h,Sample s) {
		StringBuffer content = new StringBuffer();
		if (s.getSamples().isEmpty())
			return h;
		for (Sample child : s.getSamples()) {
			content.append(child.id + ", " + s.id + "\n");
			if (child.id.equals(s.id)) {
				throw new RuntimeException("This should not have happened, child and parent have the same id " + s.id);
			}
			content.append(addSampleToHierarchy(h, child));
			System.out.println("Content : " + content);
		}
		return h + content.toString();
	}
	

	private void addHtmlSample(StringBuilder sb, Sample s, String folder) throws IOException {
		// Write header:
		sb.append("<p style=\"margin-left: 20px\" align=\"left\">\n");
		// Write data:
		for (Field f : htmlFields) {
			sb.append(f.getTitle() + ": " + s.getString(f) + "<br />\n");
		}

		if (s.parent != null) sb.append("parent: "
				+ String.format("<a href=\"%s\">%s</a>\n", s.parent.id, s.parent.id));

		// Write image:
		String filename = s.id;
		History h = getSampleHistory(s);
		h.writeGraphvizFile(folder, filename, s.id);

		// URL image = path2url(dot2img(filename, folder));
		String image = filename + "." + DOT_IMAGE_EXTENSION;
		sb.append(String.format("<a href=\"%s\"><img src=\"%s\" /></a>\n", image, image));
		// Write children:
		for (Sample child : s.getSamples()) {
			// if (hasFilename(child)) {
			sb.append("<hr />\n");
			addHtmlSample(sb, child, folder);
			// }
		}
		// Write footer:
		sb.append("</p>\n");
	}

	// private boolean hasFilename(Sample s) {
	// return new File(s.id).exists();
	// }

	private String dot2img(String dotPath, String outFolder) {
		String outFile = FilenameUtils.getBaseName(dotPath) + "." + DOT_IMAGE_EXTENSION;
		// String outFile = FilenameUtils.concat(outFolder, FilenameUtils.getBaseName(dotPath) + "."
		// + DOT_IMAGE_EXTENSION);
		// String command = String.format(
		// "%s -o%s -T%s -q %s",
		// options.getDotExecutablePath(), outFile, DOT_IMAGE_EXTENSION, dotPath);
		// // try {
		// Runtime.getRuntime().exec(command).waitFor();
		// } catch (IOException e) {
		// Logger.exception(e);
		// return null;
		// } catch (InterruptedException e) {
		// Logger.exception(e);
		// return null;
		// }
		return outFile;
	}

	private URL path2url(String path) {
		try {
			return new File(path).toURI().toURL();
		} catch (MalformedURLException e) {
			Logger.exception(e);
			return null;
		}
	}

	public void setFinalHistoryCollection(HistoryCollection hc) {
		finalHistoryCollection = hc;
	}

	public HistoryCollection getFinalHistoryCollection() {
		return finalHistoryCollection;
	}

}