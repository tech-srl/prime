package technion.prime.eclipse.actions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;

public class RunBatchDialog extends RunDialog {
	
	private String[] queryList;
	private List list;

	public RunBatchDialog(Shell parentShell) {
		super(parentShell, "Prime - Batch Mode");
	}
	
	@Override
	public void create() {
		super.create();
	}
	
	public RunBatchDialog withQueryList(String[] list) {
		queryList = list;
		return this;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite c = createDialogAreaComposite(parent);
        createQueryList(c);
        createNumResultsField(c);
		return c;
	}
	
	private void createQueryList(Composite c) {
		list = new List(c, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.horizontalSpan = 2;
		list.setLayoutData(gridData);
	}

	@Override
	public int open() {
		list.setItems(queryList);
		return super.open();
	}
}
