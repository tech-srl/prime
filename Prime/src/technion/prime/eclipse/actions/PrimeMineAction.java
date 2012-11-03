package technion.prime.eclipse.actions;

import org.eclipse.jface.window.Window;

import technion.prime.PrimeAnalyzer;

import technion.prime.history.HistoryCollection;
import technion.prime.utils.JavaFileUtils;
import technion.prime.utils.Stage;
import technion.prime.utils.Logger.CanceledException;

public class PrimeMineAction extends PrimeAction {
	private String queryString;
	private int numSamples;
	private long downloadTimeout;
	private boolean batch;

	@Override
	public boolean runWithSelection(String entireSelection) {
		batch = entireSelection.contains("\n");
		boolean completed = batch ?
				runBatchAction(entireSelection) : runMineAction(entireSelection);

		if (completed == false) return false;
		
		updateDefaultValues();
		return true;
	}
	
	private boolean runMineAction(String selection) {
		boolean status = openRunDialog(selection);
		if (status == false) return false;
		performMineAction(false);
		return true;
	}
	
	private String quote(String s) {
		return s.replaceAll("\\.", "\\\\.").replaceAll("\\*", "\\\\*");
	}
	
	private boolean runBatchAction(String entireSelection) {
		boolean status = openBatchDialog(entireSelection);
		if (status == false) return false;
		String[] splitSelection = entireSelection.split("\\n");
		for (String singleSelection : splitSelection) {
			if (singleSelection.trim().isEmpty()) continue;
			options.setTypeFilter(generateTypeFilter(singleSelection.trim()));
			queryString = generateQueryString(singleSelection.trim());
			performMineAction(true);
		}
		return true;
	}

	private String generateTypeFilter(String s) {
		if (s.contains(".*")) return quote(s).replaceAll("\\\\\\.\\\\\\*", ".*");
		String surrounding = JavaFileUtils.getSurroundingPackage(s);
		return quote(surrounding) + "\\..*";
	}
	
	private String generateQueryString(String s) {
		String surrounding = s.contains(".*") ?
				// oh god the humanity
			quote(s).replaceAll("\\\\\\.\\\\\\*$", "").replaceAll("\\\\\\.\\\\\\*", ".*") :
			JavaFileUtils.getSurroundingPackage(s) + "\\.";
		return "import\\s+" + surrounding;
	}

	@Override
	protected void getDefaultValues() {
		super.getDefaultValues();
		numSamples = options.getDefaultNumSamples();
		downloadTimeout = options.getStageTimeout(Stage.DOWNLOADING);
	}
	
	@Override
	protected void updateDefaultValues() {
		options.setDefaultNumSamples(numSamples);
		options.setStageTimeout(Stage.DOWNLOADING, downloadTimeout);
	}

	private boolean openRunDialog(String selection) {
		RunMineDialog d = new RunMineDialog(window.getShell());
		d.create();
		d.withTypeFilter(generateTypeFilter(selection));
		d.withQueryString(generateQueryString(selection));
		d.withNumResults(numSamples);
		d.withDownloadTimeout(downloadTimeout);
		int status = d.open();
		if (status != Window.OK) return false;
		
		options.setTypeFilter(d.getTypeFilter());
		queryString = d.getQueryString();
		numSamples = d.getNumResults();
		downloadTimeout = d.getDownloadTimeout();
		return true;
	}
	
	private boolean openBatchDialog(String entireSelection) {
		RunBatchDialog d = new RunBatchDialog(window.getShell());
		d.create();
		d.withNumResults(numSamples);
		d.withQueryList(entireSelection.split("\\n"));
		int status = d.open();
		if (status != Window.OK) return false;
		
		numSamples = d.getNumResults();
		return true;
	}
	
	private void performMineAction(boolean batch) {
		performAction("Mining with query: " + queryString);
	}

	@Override
	protected HistoryCollection runFlow(PrimeAnalyzer analyzer) throws CanceledException {
		analyzer.addQuery(queryString, numSamples);
		HistoryCollection result = analyzer.analyze(true);
		return result;
	}

}