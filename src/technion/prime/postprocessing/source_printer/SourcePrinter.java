package technion.prime.postprocessing.source_printer;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import technion.prime.DefaultOptions;
import technion.prime.Options;
import technion.prime.history.History;
import technion.prime.history.HistoryCollection;
import technion.prime.utils.JavaFileUtils;
import technion.prime.utils.Logger;
import technion.prime.utils.MultiMap;

public class SourcePrinter {
	private final static Pattern SIGNATURE_PATTERN = Pattern.compile(
			"source: <([^:]+): ([^>]+)>");

	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.printf("Usage is %s <name of cached history collection file / folder containing history collections>%n",
					SourcePrinter.class.getName());
			return;
		}
		Options options = new DefaultOptions();
		Logger.setup(options, false);
		HistoryCollection hc;
		try {
			hc = JavaFileUtils.loadAllHistoryCollections(options, args[0]);
		} catch (IOException e) {
			Logger.error("Could not load history collection: " + e.getMessage());
			return;
		}
		new SourcePrinter(options).printSources(hc);
	}
	
	public SourcePrinter(Options options) {
		// options is unused
	}
	
	public void printSources(HistoryCollection hc) {
		String result = getClassAndMethodList(findMethodsByClasses(hc));
		System.out.println(result);
	}


	/**
	 * Give a history collection, creates a mapping between class name and method names.
	 * Each class name appearing in any histories in the input history collection is added as a key
	 * to the output collection, and is mapped to all the methods that belong to it which appear
	 * in the history collection.
	 * 
	 * @param hc The input history collection.
	 * @return A mapping from class names to a set of method names. 
	 */
	protected MultiMap<String, String> findMethodsByClasses(HistoryCollection hc) {
		MultiMap<String, String> result = new MultiMap<String, String>();
		for (History h : hc.getHistories()) {
			Matcher m = SIGNATURE_PATTERN.matcher(h.getTitle());
			if (m.matches() == false) continue;
			String className = m.group(1);
			String methodName = m.group(2);
			result.put(className, methodName);
		}
		return result;
	}

	protected String getClassAndMethodList(MultiMap<String, String> methodsByClasses) {
		StringBuilder sb = new StringBuilder();
		for (String className : methodsByClasses.keySet()) {
			sb.append(classNameToOutput(className) + "\n");
			for (String methodName : methodsByClasses.getAll(className)) {
				sb.append("   " + methodNameToOutput(className, methodName) + "\n");
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	protected String classNameToOutput(String className) {
		return className;
	}

	protected String methodNameToOutput(String className, String methodName) {
		return methodName;
	}
	
}
