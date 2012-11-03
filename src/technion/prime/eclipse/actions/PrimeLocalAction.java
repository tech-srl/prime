package technion.prime.eclipse.actions;

import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import technion.prime.PrimeAnalyzer;
import technion.prime.eclipse.actions.RunLocalDialog.SourceChoice;
import technion.prime.history.HistoryCollection;
import technion.prime.utils.Logger;
import technion.prime.utils.Logger.CanceledException;
import technion.prime.utils.Stage;

/**
 * Our sample action implements workbench action delegate.
 * The action proxy will be created by the workbench and
 * shown in the UI. When the user tries to use the action,
 * this delegate will be created and execution will be 
 * delegated to it.
 * @see IWorkbenchWindowActionDelegate
 */
public class PrimeLocalAction extends PrimeAction {
	private String path;
	private SourceChoice choice;
	private String[] files;
	
	@Override
	public boolean runWithSelection(String selection) {
		RunLocalDialog d = new RunLocalDialog(window.getShell());
		d.create();
		int status = d
			.withFolder(options.getLocalInputDir())
			.withTypeFilter(selection)
			.open();
		if (status != Window.OK) return false;
		options.setTypeFilter(d.getTypeFilter());
		path = d.getFolder();
		choice = d.getSourceChoice();
		files = d.getFiles();
		if (files.length == 0) {
			showErrorMessage("Must select at least one input file", false);
			return false;
		}
		
		performLocalAction();
		return true;
	}
	
	private void performLocalAction() {
		performAction("local prime");
	}

	@Override
	protected void updateDefaultValues() {
		if (path != null) options.setLocalInputDir(path);
	}
	
	@Override
	protected HistoryCollection runFlow(PrimeAnalyzer analyzer) throws CanceledException {
		Logger.skipStage(Stage.SEARCHING, "local mode used - collecting files from " + path);
		Logger.skipStage(Stage.DOWNLOADING,
				"local mode used, " + files.length + " files found in local folder.");
		HistoryCollection result = null;
		
		switch (choice) {
		case SOURCES:
			for (String file : files) analyzer.addInputFile(file);
			result = analyzer.analyze(true);
			break;
		case COMPILED:
			Logger.skipStage(Stage.COMPILING, "local mode used, using class files.");
			for (String file : files) analyzer.addInputFile(file);
			result = analyzer.analyze(true);
			break;
		case CACHED:
			Logger.skipStage(Stage.COMPILING, "local mode used, previous results already compiled.");
			Logger.skipStage(Stage.LOADING, "local mode used, previous results already loaded.");
			Logger.skipStage(Stage.ANALYZING, "local mode used, previous results already analyzed.");
			for (String file : files) analyzer.addInputFile(file);
			result = analyzer.analyze(false);
			break;
		}
		
		return result;
	}
	
}