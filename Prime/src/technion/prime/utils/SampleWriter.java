package technion.prime.utils;

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

import technion.prime.dom.AppMethodRef;
import technion.prime.history.History;
import technion.prime.history.edgeset.Edge;
import technion.prime.history.edgeset.EdgeNode;
import technion.prime.statistics.Field;
import technion.prime.statistics.Sample;


public class SampleWriter {
	
	private static final String GRAPHVIZ_SUFFIX = ".dot";

	private static Field[] dotStatFields = new Field[] {
		Sample.NAME,
		Sample.NUM_SAMPLES,
		Sample.MAX_WEIGHT,
		Sample.MAX_DEGREE,
		Sample.AVG_DEGREE
};

	
	public static void write(Sample s, History h, String folder, String filename) throws IOException {
		String content = getGvContent(s,h,s.getId());

		filename = folder + File.separator + filename + GRAPHVIZ_SUFFIX;

		FileUtils.writeStringToFile(new File(filename), content);

		Logger.log("Writer: wrote history to " + filename);

	}

	private static String getGvContent(Sample s,History h, String graphId) {
		StringBuilder sb = new StringBuilder();
		
		appendGvFileHeader(sb, h.getTitle(), graphId);
		appendSampleStatistics(sb,s,h);
		Map<EdgeNode, String> nodeNames = appendGvNodes(sb,h);
		Set<Edge> edgesOnHeaviestRoute = findEdgesOnHeaviestRoute(h);
		for (Edge e : h.edges()) {
			appendGvEdge(nodeNames, sb, e, edgesOnHeaviestRoute.contains(e));
		}
		appendGvFileFooter(sb);
		return sb.toString();
	}

	private static void appendSampleStatistics(StringBuilder sb, Sample s, History h) {
		for (Field f : dotStatFields) {
			String simplifiedTitle = f.getTitle().replace("#","num").replace(" ", "_");
			sb.append("\t" + simplifiedTitle + "=\"" + s.getString(f) + "\"\n");
		}
	}

	private static Map<EdgeNode, String> appendGvNodes(StringBuilder sb, History h) {
		Map<EdgeNode, String> result = new HashMap<EdgeNode, String>();
		int counter = 1; // 0 is reserved for the root
		for (EdgeNode n : h.nodes()) {
			String name = n == h.root() ? "0" : "" + counter++;
			result.put(n, name);
			sb.append("\t" + name);
			if (h.isActive(n)) {
				// String numEnding = StringUtils.prettyPrintNumber(numberOfRoutesEndingAt(n));
				sb.append("[ shape = \"doublecircle\" ");
				// sb.append(String.format("label = \"%s\\n/%s\" ]", name, numEnding));
				sb.append(String.format("label = \"%s\" ]", name));
			}
			sb.append(";\n");
		}
		return result;
	}

	private static Set<Edge> findEdgesOnHeaviestRoute(History h) {
		Set<Edge> result = new HashSet<Edge>();
		EdgeNode curr = h.root();
		while (true) {
			Edge e = getHeaviestOutoingEdge(h,curr);
			if (e == null || result.contains(e)) break;
			result.add(e);
			curr = e.getTo();
		}
		return result;
	}

	private static Edge getHeaviestOutoingEdge(History h, EdgeNode n) {
		Edge result = null;
		for (Edge e : h.getOutgoingEdges(n)) {
			if (result == null || result.getWeight() < e.getWeight()) {
				result = e;
			}
		}
		return result;
	}

	private static void appendGvEdge(Map<EdgeNode, String> nodeNames, StringBuilder sb, Edge e,
			boolean bold) {
		sb.append(String.format("\t%s -> %s [ ",
				nodeNames.get(e.getFrom()),
				nodeNames.get(e.getTo())));

		appendGvLabel(sb, e);
		sb.append("weight = \"" + Math.round(e.getWeight()) + "\" ");
		appendGvTooltip(sb, e);
		if (bold) sb.append("style = \"setlinewidth(2)\" arrowsize = \"1.5\" color=\"blue\"");
		appendGvEdgeExtras(sb, e);

		sb.append("];\n");
	}

	protected static void appendGvEdgeExtras(StringBuilder sb, Edge e) {}

	private static void appendGvTooltip(StringBuilder sb, Edge e) {
		sb.append("URL = \"#\" tooltip = \"");
		boolean first = true;
		for (AppMethodRef m : e.getMethods()) {
			if (first) {
				first = false;
			} else {
				sb.append("; ");
			}
			appendGvFullMethodLabel(sb, m);
		}
		double w = e.getWeight();
		if (w > 0) sb.append(" * " + StringUtils.prettyPrintNumber(w));
		sb.append("\" ");
	}

	private static void appendGvFullMethodLabel(StringBuilder sb, AppMethodRef m) {
		sb.append(m.getSignature());
	}

	private static void appendGvLabel(StringBuilder sb, Edge e) {
		sb.append("label = \"");
		boolean first = true;
		Set<String> seen = new HashSet<String>();
		for (AppMethodRef m : e.getMethods()) {
			if (first) {
				first = false;
			} else {
				sb.append("\\n");
			}
			seen.add(appendGvMethodLabel(seen, sb, m, e.getMethods().size()));
		}
		double w = e.getWeight();
		if (w > 0) sb.append("\\nx " + StringUtils.prettyPrintNumber(w));
		sb.append("\" ");
	}

	private static String appendGvMethodLabel(Set<String> seen, StringBuilder sb, AppMethodRef m,
			int numMethods) {
		String s = m.toString();
		if (seen.contains(s)) return s;
		if (numMethods == 1) {
			String[] parts = s.split("\\.[^(]+\\(");
			if (parts.length != 2) {
				sb.append(s);
				return s;
			}
			int pos = parts[0].length() + 1;
			String line1 = s.substring(0, pos);
			String line2 = s.substring(pos);
			sb.append(line1 + "\\n" + line2);
		} else {
			sb.append(s);
		}
		return s;
	}

	private static void appendGvFileHeader(StringBuilder sb, String label, String graphid) {
		if (graphid == null) {
			graphid = "G";
		}
		sb.append("digraph " + graphid + " {\n" +
				"\tlabel = \"" + label + "\";\n" +
				"\tlabelloc = \"t\";\n" +
				"\trankdir=LR;\n" +
				"\tsize=\"8,5\"\n" +
				"\tnode [shape = circle];\n" +
				"\tHanchor [shape = point style=invis];\n" +
				"\tHanchor -> 0;\n");
	}

	private static void appendGvFileFooter(StringBuilder sb) {
		sb.append("}\n");
	}

	
	public static void writeHierarchyFile(Set<Sample> samples, String folder, String filename) throws IOException {
		String content = new String();
		StringBuffer result = new StringBuffer();
		for (Sample s : samples) {
			result.append(addSampleToHierarchy(content, s));
		}

		String fullname = folder + File.separator + filename;
		FileUtils.writeStringToFile(new File(fullname), result.toString());
	}
	
	private static String addSampleToHierarchy(String h,Sample s) {
		StringBuffer content = new StringBuffer();
		if (s.getSamples().isEmpty())
			return h;
		Set<Sample> currSamples = s.getSamples();
		for (Sample child : currSamples) {
			if (child.getId().equals(s.getId())) {
				throw new RuntimeException("This should not have happened, child and parent have the same id " + s.getId());
			}
			content.append(child.getId() + ", " + s.getId() + "\n");
			content.append(addSampleToHierarchy(h, child));
		}
		return h + content.toString();
	}

	private String dot2img(String dotPath, String outFolder) {
		String outFile = FilenameUtils.getBaseName(dotPath) + "." + FileExtensions.DOT_IMAGE_EXTENSION;
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
	
}




