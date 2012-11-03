package technion.prime.eclipse.actions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

public class RunMineDialog extends RunDialog {
	
	private static final int MIN_DOWNLOAD_TIMEOUT = 1;
	private static final int MAX_DOWNLOAD_TIMEOUT = 31556926; // 1 year
	private static final int DOWNLOAD_TIMEOUT_SPINNER_INCREMENT = 60;
	private Text typeFilterText;
	private Text queryStringText;
	private String typeFilter;
	private String queryString;
	private Spinner downloadTimeoutSpinner;
	private long downloadTimeout;

	public RunMineDialog(Shell shell) {
		super(shell, "Prime");
	}
	
	public String getTypeFilter() {
		return typeFilter;
	}
	
	public String getQueryString() {
		return queryString;
	}
	
	public long getDownloadTimeout() {
		return downloadTimeout;
	}
	
	public RunMineDialog withQueryString(String s) {
		queryStringText.setText(s);
		return this;
	}
	
	public RunMineDialog withTypeFilter(String s) {
		typeFilterText.setText(s);
		return this;
	}
	
	public RunMineDialog withDownloadTimeout(long l) {
		downloadTimeoutSpinner.setSelection((int)(l / 1000));
		return this;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite c = createDialogAreaComposite(parent);
		createTypeFilterField(c);
        createQueryField(c);
        createNumResultsField(c);
        createDownloadTimeoutField(c);
		return c;
	}
	
	private void createDownloadTimeoutField(Composite c) {
		String tooltip = "The number of seconds to dedicate to the download phase";
		
		Label l = new Label(c, SWT.NONE);
		l.setText("Download phase timeout (in seconds):");
		l.setToolTipText(tooltip);
		
		downloadTimeoutSpinner = new Spinner(c, SWT.BORDER);
		downloadTimeoutSpinner.setMinimum(MIN_DOWNLOAD_TIMEOUT);
		downloadTimeoutSpinner.setMaximum(MAX_DOWNLOAD_TIMEOUT);
		downloadTimeoutSpinner.setIncrement(DOWNLOAD_TIMEOUT_SPINNER_INCREMENT);
		downloadTimeoutSpinner.setToolTipText(tooltip);
	}

	private void createTypeFilterField(Composite c) {
		String tooltip = "Which types to include in the analysis";
		
		Label l = new Label(c, SWT.NONE);
		l.setText("Type filter (regex):");
		l.setToolTipText(tooltip);
		
        typeFilterText = new Text(c, SWT.BORDER);
        typeFilterText.setToolTipText(tooltip);
        typeFilterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}

	private void createQueryField(Composite c) {
		String tooltip = "The query that will be used for searching online";
		
		Label l = new Label(c, SWT.NONE);
		l.setText("Query (regex):");
		l.setToolTipText(tooltip);
		
        queryStringText = new Text(c, SWT.BORDER);
        queryStringText.setToolTipText(tooltip);
        queryStringText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}
	
	@Override
	protected void okPressed() {
		typeFilter = typeFilterText.getText();
		queryString = queryStringText.getText();
		downloadTimeout = downloadTimeoutSpinner.getSelection() * 1000;
		super.okPressed();
	}
	
}
