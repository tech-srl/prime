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

public class RunLocalDialog extends TitleAreaDialog {
	public enum SourceChoice {
		SOURCES("use source files"),
		COMPILED("use compiled files"),
		CACHED("use analysis results file");
		
		private String title;
		
		private SourceChoice(String title) {
			this.title = title;
		}

		public String getTitle() {
			return title;
		}
	}
	
	private static final String INVALID_FOLDER_STRING = "<invalid folder>";
	private static final String UPDATING_STRING = "<updating...>";
	private static final String EMPTY_FOLDER_STRING = "<no matching files found>";
	
	private Text folderText;
	private Text typeFilterText;
	private List fileList;
	private Button[] choices;
	
	private String folder;
	private String typeFilter;
	private SourceChoice choice;
	private String[] files;
	
	private Thread updatingThread;

	public RunLocalDialog(Shell shell) {
		super(shell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}
	
	public RunLocalDialog withFolder(String s) {
		this.folderText.setText(s);
		return this;
	}
	
	public RunLocalDialog withTypeFilter(String s) {
		typeFilterText.setText(s);
		return this;
	}
	
	public String getFolder() {
		return folder;
	}
	
	public String getTypeFilter() {
		return typeFilter;
	}
	
	public SourceChoice getSourceChoice() {
		return choice;
	}
	
	public String[] getFiles() {
		return files;
	}
	
	@Override
	public void create() {
		super.create();
		setTitle("Prime - local mode");
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite c = createDialogAreaComposite(parent);
		createTypeFilterField(c);
		createFolderField(c);
		createSourceChoiceButtons(c);
        createFileList(c);
		return c;
	}
	
	@Override
	public int open() {
		updateList();
		return super.open();
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
				String path = new DirectoryDialog(RunLocalDialog.this.getShell()).open();
				if (path == null) return;
				folderText.setText(path);
				//updateList();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
	}
	
	private void createSourceChoiceButtons(Composite c) {
		choices = new Button[SourceChoice.values().length];
		int i = 0;
		for (SourceChoice choice : SourceChoice.values()) {
			Button b = new Button(c, SWT.RADIO);
			b.setText(choice.getTitle());
			b.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					updateList();
				}
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {}
			});
			if (i == 0) b.setSelection(true);
			choices[i++] = b;
		}
		// There should be i options, last option should have index i - 1.
		if (i % 3 > 0) {
			GridData gridData = new GridData();
			gridData.horizontalSpan = 3 - ((i-1) % 3);
			choices[i-1].setLayoutData(gridData);
		}
	}
	
	private void createFileList(Composite c) {
		fileList = new List(c, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.horizontalSpan = 3;
		fileList.setLayoutData(gridData);
		fileList.addSelectionListener(new SelectionListener() {
			@Override public void widgetSelected(SelectionEvent e) {}
			@Override public void widgetDefaultSelected(SelectionEvent e) {
				itemDoubleClicked();
			}
		});
	}

	private void itemDoubleClicked() {
		if (getUpdatedSourceChoice() == SourceChoice.CACHED) {
			okPressed();
		}
	}

	String[] items;
	
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
		final SourceChoice choice = getUpdatedSourceChoice();
		updatingThread = new Thread() {
			@Override
			public void run() {
				try {
					Collection<String> fileList = new LinkedList<String>();
					switch (choice) {
					case SOURCES:
						fileList.addAll(JavaFileUtils.getJavaFilesInFolder(path, true));
						break;
					case COMPILED:
						fileList.addAll(JavaFileUtils.getJarsInFolder(path, true));
						fileList.addAll(JavaFileUtils.getClassFilesInFolder(path, true));
						break;
					case CACHED:
						fileList.addAll(JavaFileUtils.getCachedFilesInFolder(path, true));
						break;
					}
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
		return c;
	}
	
	private SourceChoice getUpdatedSourceChoice() {
		for (int i = 0 ; i < SourceChoice.values().length ; i++) {
			if (choices[i].getSelection()) {
				return SourceChoice.values()[i];
			}
		}
		throw new IllegalStateException("no choice selected");
	}
	
	@Override
	protected void okPressed() {
		choice = getUpdatedSourceChoice();
		if (choice == SourceChoice.CACHED) {
			files = fileList.getSelection();
		} else {
			files = fileList.getItems();
		}
		folder = folderText.getText();
		typeFilter = typeFilterText.getText();
		super.okPressed();
	}
	
	@Override
	public boolean close() {
		if (updatingThread != null) updatingThread.interrupt();
		return super.close();
	}
	
}