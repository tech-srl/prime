package technion.prime.eclipse.actions;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

import org.eclipse.swt.SWT;

public class RunDialog extends TitleAreaDialog {
	private static final int MIN_NUM_RESULTS = 1;
	private static final int MAX_NUM_RESULTS = 100000000;
	private static final int SPINNER_INCREMENT = 100;
	
	private Spinner numResultsSpinner;
	private int numResults;
	private String title;

	public RunDialog(Shell shell, String title) {
		super(shell);
		this.title = title;
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}
	
	public RunDialog withNumResults(int n) {
		numResultsSpinner.setSelection(n);
		return this;
	}
	
	public int getNumResults() {
		return numResults;
	}
	
	@Override
	public void create() {
		super.create();
		setTitle(title);
	}
	
	protected Composite createDialogAreaComposite(Composite parent) {
		Composite c = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 10;
		layout.marginWidth = 10;
		layout.verticalSpacing = 10;
		layout.horizontalSpacing = 10;
		layout.numColumns = 2;
		c.setLayout(layout);
		c.setLayoutData(new GridData(GridData.FILL_BOTH));
		c.setFont(parent.getFont());
		return c;
	}
	
	protected void createNumResultsField(Composite c) {
		String tooltip = "The total number of results to request from online sources";
		
		Label l = new Label(c, SWT.NONE);
		l.setText("Number of results:");
		l.setToolTipText(tooltip);
		
        numResultsSpinner = new Spinner(c, SWT.BORDER);
        numResultsSpinner.setMinimum(MIN_NUM_RESULTS);
        numResultsSpinner.setMaximum(MAX_NUM_RESULTS);
        numResultsSpinner.setIncrement(SPINNER_INCREMENT);
        numResultsSpinner.setToolTipText(tooltip);
	}
	
	@Override
	protected void okPressed() {
		numResults = numResultsSpinner.getSelection();
		super.okPressed();
	}

}

