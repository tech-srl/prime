package technion.prime;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.ParseException;

import technion.prime.utils.StringFilter;
import technion.prime.utils.Stage;
import technion.prime.history.HistoryCollection;
import technion.prime.utils.JavaFileUtils;
import technion.prime.utils.Logger;
import technion.prime.Options;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;


public class PrimeMain {
	private final static boolean DEBUG = false;
	
	private enum PrimeCommandLineOptions {
		QUERY("queries"),
		COMPILED("class-files"),
		COMPILED_IN_JAR("jarred-class-files"),
		CACHED("cache-file"),
		SOURCE("source-files"),
		QUERY_SIZE("query-size"),
		OUTPUT_DIR("output-dir"),
		MERGE_PARTIAL("union-partial"),
		TEMP_DIR("temp-dir"),
		API_PATTERN("api-pattern"),
		COMPILE_ONLY("compile-only"),
		;

		String optionString;

		private PrimeCommandLineOptions(String optionString) {
			this.optionString = optionString;
		}
	}
	
	public static void main(String[] args) {
		analyze(args, null);
	}
	
	public static HistoryCollection analyze(String[] args, Options overrideWith) {
		CommandLine line = parseCommandLine(args);
		if (line == null) return null;
		
		String query = line.getOptionValue(PrimeCommandLineOptions.QUERY.optionString);
		String numResultsStr = line.getOptionValue(
				PrimeCommandLineOptions.QUERY_SIZE.optionString);
		String sourceFolder = line
				.getOptionValue(PrimeCommandLineOptions.SOURCE.optionString);
		String classFolder = line
				.getOptionValue(PrimeCommandLineOptions.COMPILED.optionString);
		String jarFolder = line
				.getOptionValue(PrimeCommandLineOptions.COMPILED_IN_JAR.optionString);
		String cacheFile = line
				.getOptionValue(PrimeCommandLineOptions.CACHED.optionString);
		boolean compileOnly = line
				.hasOption(PrimeCommandLineOptions.COMPILE_ONLY.optionString);
		
		Options primeOptions = overrideWith != null? overrideWith :
			createOptionsFromCommandLineArgs(line);

		PrimeAnalyzer analyzer = new PrimeAnalyzer(primeOptions);
		HistoryCollection hc;
		try {
			// Calculate the HistoryCollection
			boolean loadedCacheFiles = false;
			if (query != null) {
				analyzer.addQuery(query, Integer.valueOf(numResultsStr));
			}
			List<String> files = new LinkedList<String>();
			if (sourceFolder != null) {
				files.addAll(JavaFileUtils.getJavaFilesInFolder(sourceFolder, true));
			}
			if (classFolder != null) {
				files.addAll(JavaFileUtils.getClassFilesInFolder(classFolder, true));
			}
			if (jarFolder != null) {
				files.addAll(JavaFileUtils.getJarsInFolder(jarFolder, true));
			}
			if (cacheFile != null) {
				files.add(cacheFile);
				loadedCacheFiles = true;
			}
			for (String f : files) analyzer.addInputFile(f);
			analyzer.setCompileOnly(compileOnly);
			hc = analyzer.analyze(! loadedCacheFiles);
			if (hc == null && compileOnly == false) {
				Logger.error("Could not generate a HistoryCollection");
				return null;
			}

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		Logger.debug("Mining done, total time: " +
				Logger.formattedDuration(analyzer.getDuration()));

		return hc;
	}
	
	public static Options createOptionsFromCommandLineArgs(CommandLine line) {
		final String outputDir = line.getOptionValue(
				PrimeCommandLineOptions.OUTPUT_DIR.optionString,
				System.getProperty("java.io.tmpdir") + "/prime/out");
		final String tempDir = line.getOptionValue(
				PrimeCommandLineOptions.TEMP_DIR.optionString,
				System.getProperty("java.io.tmpdir") + "/prime/temp");
		final boolean partialMerge = line
				.hasOption(PrimeCommandLineOptions.MERGE_PARTIAL.optionString);
		String apiPatternString = line
				.getOptionValue(PrimeCommandLineOptions.API_PATTERN.optionString);
		StringFilter apiFilter = null;
		if (apiPatternString != null) {
			Pattern apiPattern = Pattern.compile(apiPatternString);
			apiFilter = new StringFilter(apiPattern, StringFilter.PATTERN_MATCH_NONE, true, false);
		}
		final StringFilter apiFilter2 = apiFilter;
		
		return new DefaultOptions() {
			private static final long serialVersionUID = 5051441602752092277L;
			
			@Override public String getTempDir() { return tempDir; }
			@Override public String getOutputDir() { return outputDir; }
			@Override public boolean isMethodSimilarityUnionPartial() { return partialMerge; }
			@SuppressWarnings("unused")
			@Override public long getSingleActionTimeout(Stage stage) {
				if (DEBUG && stage == Stage.ANALYZING) return Integer.MAX_VALUE;
				return super.getSingleActionTimeout(stage);
			}
			@Override public boolean shouldShowExceptions() { return DEBUG; }
			@Override public StringFilter getFilterOpaqueTypes() {
				return apiFilter2 == null ? super.getFilterOpaqueTypes() : apiFilter2;
			}
			@Override public StringFilter getFilterReported() {
				return apiFilter2 == null ? super.getFilterOpaqueTypes() : apiFilter2;
			}
			@Override public boolean useHistoryInvariant() {
				return DEBUG;
			}
			@Override public boolean shouldCluster() {
				return false;
			}
			@Override public boolean isMayAnalysis() {
				return false;
			}
		};
	}
	
	@SuppressWarnings("static-access")
	private static org.apache.commons.cli.Options buildOptions() {
		org.apache.commons.cli.Options options = new org.apache.commons.cli.Options();
		OptionGroup input = new OptionGroup();
		input.setRequired(true);
		Option query = OptionBuilder
				.withLongOpt(PrimeCommandLineOptions.QUERY.optionString)
				.withArgName("query").hasArg()
				.withDescription("Query to use for the search engine")
				.create('q');
		Option source = OptionBuilder
				.withLongOpt(PrimeCommandLineOptions.SOURCE.optionString)
				.withArgName("folder").hasArg()
				.withDescription("Folder containing source files")
				.create('s');
		Option compiled = OptionBuilder
				.withLongOpt(PrimeCommandLineOptions.COMPILED.optionString)
				.withArgName("folder").hasArg()
				.withDescription("Folder containing class files")
				.create('c');
		Option jars = OptionBuilder
				.withLongOpt(PrimeCommandLineOptions.COMPILED_IN_JAR.optionString)
				.withArgName("folder").hasArg()
				.withDescription("Folder containing jar files")
				.create('j');
		Option cached = OptionBuilder
				.withLongOpt(PrimeCommandLineOptions.CACHED.optionString)
				.withArgName("file").hasArg()
				.withDescription("File containing a cached run")
				.create('f');
		input.addOption(query);
		input.addOption(source);
		input.addOption(compiled);
		input.addOption(jars);
		input.addOption(cached);

		Option querySize = OptionBuilder
				.withLongOpt(PrimeCommandLineOptions.QUERY_SIZE.optionString)
				.withArgName("integer")
				.hasArg()
				.withDescription(
						"Number of results to download when using the query option")
				.create('n');

		Option outputDir = OptionBuilder
				.withLongOpt(PrimeCommandLineOptions.OUTPUT_DIR.optionString)
				.withArgName("folder")
				.hasArg()
				.withDescription(
						"Directory to save the output in")
				.create('o');
		
		Option tempDir = OptionBuilder
				.withLongOpt(PrimeCommandLineOptions.TEMP_DIR.optionString)
				.withArgName("temp")
				.hasArg()
				.withDescription(
						"Directory to use as a temporary folder")
				.create('t');

		Option partialMerge = OptionBuilder
				.withLongOpt(PrimeCommandLineOptions.MERGE_PARTIAL.optionString)
				.withDescription("Use partial merge during")
				.create('m');
		
		Option apiPattern = OptionBuilder
				.withLongOpt(PrimeCommandLineOptions.API_PATTERN.optionString)
				.hasArg()
				.withDescription("Classes matching this regex pattern will be considered API classes")
				.create('a');
		
		Option compileOnlyOption = OptionBuilder
				.withLongOpt(PrimeCommandLineOptions.COMPILE_ONLY.optionString)
				.withDescription("If present, will only compile source files, will not run analysis")
				.create("i");

		options.addOptionGroup(input);
		options.addOption(querySize);
		options.addOption(outputDir);
		options.addOption(tempDir);
		options.addOption(partialMerge);
		options.addOption(apiPattern);
		options.addOption(compileOnlyOption);
		return options;
	}
	
	private static CommandLine parseCommandLine(String[] args) {
		org.apache.commons.cli.Options options = buildOptions();
		CommandLineParser parser = new GnuParser();
		CommandLine line = null;
		
		try {
			line = parser.parse(options, args);
			printOptions(line);
		} catch (ParseException e) {
			System.out.println("Incorrect usage: " + e.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(PrimeAnalyzer.class.getName(), options);
			return null;
		}
		return line;
	}
	
	private static void printOptions(CommandLine line) {
		for (PrimeCommandLineOptions po : PrimeCommandLineOptions.values()) {
			String option = po.optionString;
			System.out.println(option + " = " + line.getOptionValue(option));
		}
	}

}
