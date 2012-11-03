package technion.prime.eclipse.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.internal.Workbench;

import technion.prime.eclipse.EclipseOptions;
import technion.prime.PrimeAnalyzer;
import technion.prime.eclipse.views.TreeResultsView;
import technion.prime.history.HistoryCollection;
import technion.prime.partial_compiler.PartialCompiler;
import technion.prime.statistics.AnalysisDetails;
import technion.prime.utils.Logger;
import technion.prime.utils.Logger.CanceledException;
import technion.prime.utils.Stage;


@SuppressWarnings("restriction")
public abstract class PrimeAction implements IWorkbenchWindowActionDelegate {
	private static final String TOOL_NAME = "Prime";
	protected EclipseOptions options;
	
	protected IWorkbenchWindow window;
	
//	protected String typeFilter;
	protected String outputFolder;
	protected HistoryCollection analysisResult;
	protected PrimeAnalyzer analyzer;
	protected long startTime;

	protected IRunnableWithProgress createActionRunnable(final String name) {
		return new IRunnableWithProgress() {
			
			@Override
			public void run(IProgressMonitor monitor) {
				monitor.beginTask(name, Stage.sumWork());
				options.setEclipseMonitor(monitor);
				analyzer = new PrimeAnalyzer(options);
				try {
					analysisResult = runFlow(analyzer);
					if (analysisResult == null) {
						Logger.log("No unclustered results.");
						return;
					}
				} catch (Logger.CanceledException e) {
					return;
				} finally {
					monitor.done();
				}
			}
		};
	}
	
	protected void performAction(String name) {
		try {
			Logger.reset();
			long startTime = System.currentTimeMillis();
			ProgressMonitorDialog d = new ProgressMonitorDialog(window.getShell());
			d.run(true, true, createActionRunnable(name));
			if (d.getProgressMonitor().isCanceled()) return;
	
			if (analysisResult == null || analysisResult.isEmpty()) {
				showMessage("No clustered results.", true);
			}
	
			Long duration = System.currentTimeMillis() - startTime;
			Logger.debug("Mining done, total time: " + Logger.formattedDuration(duration));
			options.getOngoingAnalysisDetails().setField(AnalysisDetails.PROCESS_DURATION, duration);
		} catch (InterruptedException e) {
			// Do nothing.
		} catch (InvocationTargetException e) {
			Throwable inner = e.getCause();
			Logger.exception(inner);
			String message = "Analysis failed: " + inner.getClass().getName() + " exception raised.";
			if (inner.getMessage() != null) message += "\n" + inner.getMessage();
			inner.printStackTrace();
			showErrorMessage(message, true);
		} catch (RuntimeException e) {
			Logger.exception(e);
			String message = "Analysis failed: " + e.getClass().getName() + " exception raised.";
			if (e.getMessage() != null) message += "\n" + e.getMessage();
			showErrorMessage(message, true);
		} finally {
			cleanup();
		}
	}
	
	abstract protected HistoryCollection runFlow(PrimeAnalyzer miner) throws CanceledException;
	
	@Override
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}
	
	@Override
	public void run(IAction action) {
		String selectedText = getSelectedString();
		if (invalidSelectedString(selectedText)) {
			showErrorMessage("Not enough text selected.", false);
			return;
		}
		PartialCompiler.cleanup();
		options = new EclipseOptions();
		Logger.setup(options, false);
		getDefaultValues();
		boolean ran = runWithSelection(selectedText);
		if (ran == false) return;
		reportStatistics();
		updateDefaultValues();
	}
	
	abstract protected boolean runWithSelection(String selection);
	
	abstract protected void updateDefaultValues();
	
	protected void getDefaultValues() {
		outputFolder = options.getOutputDir();
	}
	
	protected void showMessage(String s, boolean log) {
		if (log) Logger.log(s);
		else MessageDialog.openInformation(window.getShell(), TOOL_NAME, s);
	}
	
	protected void showErrorMessage(String s, boolean log) {
		if (log) Logger.warn(s);
		else MessageDialog.openInformation(window.getShell(), TOOL_NAME, s);
	}

	protected void reportStatistics() {
		try {
			window.getActivePage().showView(TreeResultsView.ID);
		} catch (PartInitException e) {
			// Should not happen.
			Logger.exception(e);
		}
		TreeResultsView view = (TreeResultsView)window.getActivePage().findView(TreeResultsView.ID);
		view.setResults(options.getOngoingAnalysisDetails());
		view.setOutputFolder(options.getOutputDir());
		printReport();
	}

	protected void printReport() {
		options.getOngoingAnalysisDetails().printReport();
	}
	
	protected String getSelectedString() {
		ISelection s = 
			Workbench.getInstance().getActiveWorkbenchWindow().getSelectionService().getSelection();
		if (s instanceof ITextSelection)
			return ((ITextSelection)s).getText();
		return "";
	}

	protected boolean invalidSelectedString(String s) {
		return s == null;
	}
	
	protected void cleanup() {
		Logger.reset();
		PartialCompiler.cleanup();
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// Do nothing
	}

	@Override
	public void dispose() {
		// Do nothing
	}
	
}
