package technion.prime.postprocessing.popularity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import technion.prime.DefaultOptions;
import technion.prime.Options;
import technion.prime.dom.AppMethodRef;
import technion.prime.dom.AppType;
import technion.prime.dom.dummy.DummyAppMethodRef;
import technion.prime.dom.dummy.DummyAppType;
import technion.prime.history.History;
import technion.prime.history.HistoryCollection;
import technion.prime.history.edgeset.Edge;
import technion.prime.history.edgeset.EdgeHistory;
import technion.prime.history.edgeset.EdgeNode;
import technion.prime.utils.JavaFileUtils;

/**
 * Print a table of popular methods after a given method. 
 */
public class Popularity {
	private static final Pattern METHOD_SIGNATURE_PATTERN = Pattern.compile(
			"([\\w.]+):(\\w+)\\(([\\w.,]*)\\):([\\w.]+)");
	
	public static void main(String[] args) {
		if (args.length != 2 || METHOD_SIGNATURE_PATTERN.matcher(args[1]).matches() == false) {
			System.out.println("Usage is " + Popularity.class.getName() +
					" <history collection file or folder> <method signature>");
			System.out.println("Where signature should be in the format:");
			System.out.println("ContainerType:methodName(TypeOfParam1,TypeOfParam2):ReturnType");
			System.out.println("Remember to use qualified type names");
			return;
		}
		
		Options options = new DefaultOptions();
		HistoryCollection hc;
		try {
			hc = JavaFileUtils.loadAllHistoryCollections(options, args[0]);
		} catch (IOException e) {
			System.err.println("Could not load history collection: " + e.getMessage());
			return;
		}
		Matcher matcher = METHOD_SIGNATURE_PATTERN.matcher(args[1]);
		if (matcher.matches() == false) throw new AssertionError();
		String[] rawParams = matcher.group(3).split(",");
		AppType[] params = new AppType[rawParams.length];
		for (int i = 0; i < rawParams.length; i++) params[i] = new DummyAppType(rawParams[i]);
		AppMethodRef m = new DummyAppMethodRef(
				new DummyAppType(matcher.group(1)),
				new DummyAppType(matcher.group(4)),
				matcher.group(2),
				params);
		
		Map<AppMethodRef, Double> popularity = getPopularityTableAfter(hc, m);
		
		double totalWeight = 0; for (Double d : popularity.values()) totalWeight += d;
		
		List<String> lines = getPopularityTableLines(popularity, totalWeight);
		printTable(m, totalWeight, lines);
	}
	
	public static Map<AppMethodRef, Double> getPopularityTableAfter(HistoryCollection hc, AppMethodRef m) {
		Map<AppMethodRef, Double> result = new HashMap<AppMethodRef, Double>();
		for (History h : hc.getHistories()) {
			EdgeHistory eh = (EdgeHistory)h;
			EdgeNode n = eh.findNodeWithIncoming(m);
			if (n == null) continue;
			for (Edge e : eh.getOutgoingEdges(n)) {
				AppMethodRef edgeMethod = e.getMethods().iterator().next();
				Double curr = result.get(edgeMethod);
				if (curr == null) curr = 0.0;
				result.put(edgeMethod, curr + e.getWeight());
			}
		}
		return result;
	}
	
	public static List<String> getPopularityTableLines(Map<AppMethodRef, Double> popularity, double totalWeight) {
		List<String> lines = new ArrayList<String>(popularity.size());
		if (totalWeight == 0) return lines;
		
		final Map<String, Double> lineWeight = new HashMap<String, Double>();
		for (Map.Entry<AppMethodRef, Double> e : popularity.entrySet()) {
			String line = String.format("%-26s\t%.2f%%\t\t%.1f",
					e.getKey().toString(),
					e.getValue() * 100 / totalWeight,
					e.getValue());
			lines.add(line);
			lineWeight.put(line, e.getValue());
		}
		Collections.sort(lines, new Comparator<String>() {
			public int compare(String o1, String o2) {
				return - lineWeight.get(o1).compareTo(lineWeight.get(o2));
			};
		});
		return lines;
	}
	
	public static void printTable(AppMethodRef m, double totalWeight, List<String> lines) {
		System.out.println("Popularity contest after: " + m.getLongName());
		System.out.printf("Total weight = %.2f%n", totalWeight);
		System.out.println("Name\t\t\t\tPercentage\tRaw Weight");
		System.out.println("==========================================================");
		for (String line : lines) {
			System.out.println(line);
		}
	}
}
