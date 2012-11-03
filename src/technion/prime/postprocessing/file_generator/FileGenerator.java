package technion.prime.postprocessing.file_generator;

import java.io.IOException;

import technion.prime.DefaultOptions;
import technion.prime.Options;
import technion.prime.history.HistoryCollection;
import technion.prime.utils.JavaFileUtils;
import technion.prime.utils.Logger;
import technion.prime.utils.Logger.CanceledException;

/**
 * Generates output files from a history collection.
 */
public class FileGenerator {
	public static void main(String[] args) {
		if (args.length != 2) {
			System.out.println("Given a history collection, this generates output dot and XML files" +
					" from all the histories in it.");
			System.out.printf("Usage is %s <history collection file / folder with history collections> <output folder>%n",
					FileGenerator.class.getName());
			return;
		}
		Options options = new DefaultOptions();
		Logger.setup(options, false);
		HistoryCollection hc;
		try {
			Logger.log("Loading history collection...");
			hc = JavaFileUtils.loadAllHistoryCollections(options, args[0]);
			Logger.log("Loaded history collection with " + hc.getNumHistories() + " samples.");
		} catch (IOException e) {
			Logger.error("Could not load input history collection: " + e.getMessage());
			return;
		}
		Logger.log("Generating output files...");
		try {
			new FileGenerator(options).generateFiles(hc, args[1]);
		} catch (CanceledException e) {
			// Just quit immediately
			return;
		} catch (IOException e) {
			Logger.error("Could not generate output file: " + e.getMessage());
			return;
		}
		Logger.log("Done.");
	}

	public FileGenerator(Options options) {
		// Currently options is unused
	}
	
	/**
	 * Generate output files. Each history in the input history collection generates one .dot
	 * file under a dot/ subfolder and one XML file under an xml/ subfolder.
	 * 
	 * @param hc A history collection to generate output for.
	 * @param outputFolder The base folder to use for the output files.
	 * @throws CanceledException The operation was externally canceled.
	 * @throws IOException There was an error saving one of the files.
	 */
	public void generateFiles(HistoryCollection hc, String outputFolder) throws CanceledException, IOException {
		try {
			hc.generateGraphvizOutput(outputFolder + "/dot/");
			hc.generateXmlOutput(outputFolder + "/xml/");
		} catch (InterruptedException e) {
			throw new CanceledException();
		}
	}
}
