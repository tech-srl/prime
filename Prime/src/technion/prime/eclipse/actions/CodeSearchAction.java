package technion.prime.eclipse.actions;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.jface.window.Window;

import technion.prime.PrimeAnalyzer;
import technion.prime.history.History;
import technion.prime.history.HistoryCollection;
import technion.prime.history.edgeset.EdgeHistory;
import technion.prime.partial_compiler.PartialCompiler;
import technion.prime.partial_compiler.PartialCompiler.LoadException;
import technion.prime.postprocessing.search.Search;
import technion.prime.statistics.AnalysisDetails;
import technion.prime.utils.CompiledItem;
import technion.prime.utils.Logger;
import technion.prime.utils.Stage;
import technion.prime.utils.Logger.CanceledException;

public class CodeSearchAction extends PrimeAction {
	private String path;
	private String[] baseFiles;
	private String query;
	private HistoryCollection base;
	private boolean preprocessed;

	@Override
	protected HistoryCollection runFlow(PrimeAnalyzer miner) throws CanceledException {
		Logger.skipStage(Stage.SEARCHING, "code search");
		Logger.skipStage(Stage.DOWNLOADING, "code search");
//		Logger.skipStage(Stage.COMPILING, "code search");
//		Logger.skipStage(Stage.LOADING, "code search");
//		Logger.skipStage(Stage.ANALYZING, "code search");
		try {
			File codeFile = createCodeFile(query);
			CompiledItem compiled = compile(codeFile);
			miner.addInputFile(compiled.getFilename());
			HistoryCollection queryHc = miner.produceHistoryCollection();
			History queryHistory = chooseQueryHistory(queryHc);
			if (queryHistory == null) {
				showErrorMessage("Could not analyze query", true);
				return null;
			}
			Logger.log("Searching for matches of:\n" + stringify(queryHistory));
			if (preprocessed) {
				return new Search(options).searchAndReturnResultsFromPreprocessed(queryHistory, base);
			} else {
				return new Search(options).searchAndReturnResult(queryHistory, base);
			}
		} catch (IOException e) {
			showErrorMessage(e.getMessage(), true);
			return null;
		} catch (InterruptedException e) {
			showErrorMessage(e.getMessage(), true);
			return null;
		} catch (LoadException e) {
			showErrorMessage(e.getMessage(), true);
			return null;
		}
	}

	private History chooseQueryHistory(HistoryCollection hc) throws InterruptedException, CanceledException {
		History result = null;
		for (History h : hc.getHistories()) {
			if (result == null || h.getDepth() > result.getDepth()) result = h;
		}
		return result;
	}

	private String stringify(History h) {
		return ((EdgeHistory)h).getEdgeString();
	}

	private CompiledItem compile(File codeFile) throws LoadException {
		PartialCompiler.cleanup();
		PartialCompiler.startBatch();
		List<CompiledItem> compiled =
				PartialCompiler.loadFile(codeFile.getAbsolutePath()).compile(options.getTempDir());
		PartialCompiler.endBatch();
		PartialCompiler.cleanup();
		return compiled.get(0);
	}

	private File createCodeFile(String s) throws IOException {
		File file = new File(String.format("%s/$Search.java", options.getTempDir()));
		file.deleteOnExit();
		FileUtils.writeStringToFile(file, s);
		return file;
	}

	@Override
	protected boolean runWithSelection(String selection) {
		CodeSearchDialog d = new CodeSearchDialog(window.getShell());
		d.create();
		int status = d
			.withFolder(options.getLocalInputDir())
			.withTypeFilter("")
			.withQuery(selection)
			.open();
		if (status != Window.OK) return false;
		options.setTypeFilter(d.getTypeFilter());
		path = d.getFolder();
		baseFiles = d.getFiles();
		query = d.getQuery();
		if (baseFiles.length == 0) {
			showErrorMessage("Must select at least one base file", false);
			return false;
		}
		
		base = options.newHistoryCollection();
		for (String file : d.getFiles()) {
			Logger.debug("Loading " + file + "...");
			preprocessed |= file.endsWith("preprocessed.cached");
			try {
				HistoryCollection hc = HistoryCollection.load(file, options.getHistoryCollectionType());
				hc.recursivelySetOptions(options);
				base.unionFrom(hc);
				base.filterEmptyHistories();
			} catch (IOException e) {
				showErrorMessage(e.getMessage(), true);
				return false;
			} catch (InterruptedException e) {
				showErrorMessage(e.getMessage(), true);
				return false;
			} catch (CanceledException e) {
				return false;
			}
		}
		
		performSearchAction();
		AnalysisDetails details = options.getOngoingAnalysisDetails();
		details.setFinalHistoryCollection(analysisResult);
		try {
			details.prepareSamples();
		} catch (CanceledException e) {
			return false;
		}
		details.prepareReport();
		return true;
	}

	private void performSearchAction() {
		performAction("code search");
	}

	@Override
	protected void updateDefaultValues() {
		options.setLocalInputDir(path);
	}

}
