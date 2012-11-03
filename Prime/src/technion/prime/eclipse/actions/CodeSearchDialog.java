package technion.prime.eclipse.actions;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import technion.prime.utils.JavaFileUtils;

public class CodeSearchDialog extends TitleAreaDialog {
	
	private static final String INVALID_FOLDER_STRING = "<invalid folder>";
	private static final String UPDATING_STRING = "<updating...>";
	private static final String EMPTY_FOLDER_STRING = "<no matching files found>";
	
	private Text folderText;
	private Text typeFilterText;
	private List fileList;
	private Text queryText;
	
	private String folder;
	private String typeFilter;
	private String[] files;
	private String[] items;
	private String query;
	
	private Thread updatingThread;

	public CodeSearchDialog(Shell parentShell) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}
	
	public CodeSearchDialog withFolder(String s) {
		this.folderText.setText(s);
		return this;
	}
	
	public CodeSearchDialog withTypeFilter(String s) {
		typeFilterText.setText(s);
		return this;
	}
	
	public CodeSearchDialog withQuery(String s) {
		queryText.setText(s);
		return this;
	}
	
	public String getFolder() {
		return folder;
	}
	
	public String getTypeFilter() {
		return typeFilter;
	}
	
	public String[] getFiles() {
		return files;
	}
	
	public String getQuery() {
		return query;
	}
	
	@Override
	public void create() {
		super.create();
		setTitle("Prime - code search");
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite c = createDialogAreaComposite(parent);
		createTypeFilterField(c);
		createFolderField(c);
        createFileList(c);
        createQueryField(c);
		return c;
	}
	
	private void createQueryField(Composite c) {
		String tooltip = "Code search query";
		Label l = new Label(c, SWT.NONE);
		l.setText("Query:");
		l.setToolTipText(tooltip);
		
		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.horizontalSpan = 2;
		gridData.verticalSpan = 5;
		queryText = new Text(c, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
		queryText.setLayoutData(gridData);
		queryText.setToolTipText(tooltip);
	}

	private Composite createDialogAreaComposite(Composite parent) {
		Composite c = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 10;
		layout.marginWidth = 10;
		layout.verticalSpacing = 10;
		layout.horizontalSpacing = 10;
		layout.numColumns = 3;
		c.setLayout(layout);
		c.setLayoutData(new GridData(GridData.FILL_BOTH));
		c.setFont(parent.getFont());
		c.setSize(800, 600);
		return c;
	}
	
	private void createTypeFilterField(Composite c) {
		String tooltip = "Which types to include in the analysis";
		
		Label l = new Label(c, SWT.NONE);
		l.setText("Type filter (regex):");
		l.setToolTipText(tooltip);
		
        typeFilterText = new Text(c, SWT.BORDER);
        typeFilterText.setToolTipText(tooltip);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		typeFilterText.setLayoutData(gridData);
	}
	
	private void createFolderField(Composite c) {
		String tooltip = "Folder in which to search (recursively) for files to run";
		Label l = new Label(c, SWT.NONE);
		l.setText("Folder:");
		l.setToolTipText(tooltip);
		
		folderText = new Text(c, SWT.BORDER);
		folderText.setToolTipText(tooltip);
		folderText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		folderText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				updateList();
			}
		});
		
		Button b = new Button(c, SWT.NONE);
		b.setText("Browse...");
		b.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String path = new DirectoryDialog(CodeSearchDialog.this.getShell()).open();
				if (path == null) return;
				folderText.setText(path);
				//updateList();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
	}
	
	private void updateList() {
		if (updatingThread != null) {
			updatingThread.interrupt();
		}
		fileList.removeAll();
		
		final String path = folderText.getText();
		if (new File(path).isDirectory() == false) {
			fileList.add(INVALID_FOLDER_STRING);
			return;
		}
		
		fileList.add(UPDATING_STRING);
		updatingThread = new Thread() {
			@Override
			public void run() {
				try {
					Collection<String> fileList = new LinkedList<String>(JavaFileUtils.getCachedFilesInFolder(path, true));
					if (Thread.interrupted()) return;
					items = fileList.toArray(new String[fileList.size()]);
				} catch (Exception e) {
					return;
				}
				updateDone();
			}
		};
		updatingThread.start();
	}
	
	
	
	private void updateDone() {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				if (items.length == 0) {
					fileList.removeAll();
					fileList.add(EMPTY_FOLDER_STRING);
				} else {
					fileList.setItems(items);
				}
			}
		});
	}
	
	private void createFileList(Composite c) {
		fileList = new List(c, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 3;
		fileList.setLayoutData(gridData);
	}
	
	@Override
	protected void okPressed() {
		files = fileList.getSelection();
		folder = folderText.getText();
		typeFilter = typeFilterText.getText();
		query = queryText.getText();
		super.okPressed();
	}

}
