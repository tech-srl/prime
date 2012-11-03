package technion.prime.browser;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.layout.TreeColumnLayout;

@SuppressWarnings("unused")
public class HistoryCollectionBrowser {

	protected Shell shell;
	private Text folderSelectorTextBox;
	private List availableCollectionsList;
	private List addedCollectionsList;
	private List availableConvertersList;
	private List selectedConverterList;
	private TabFolder tabFolder;

	/**
	 * Launch the application.
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			HistoryCollectionBrowser window = new HistoryCollectionBrowser();
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Open the window.
	 */
	public void open() {
		Display display = Display.getDefault();
		createContents();
		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	/**
	 * Create the contents of the window.
	 */
	protected void createContents() {
		shell = new Shell();
		shell.setSize(450, 300);
		shell.setText("SWT Application");
		shell.setLayout(new FillLayout(SWT.HORIZONTAL));
		
		tabFolder = new TabFolder(shell, SWT.NONE);
		
		createSelectedCollectionsTab(tabFolder);
		createSelectedConvertersTab(tabFolder);
		createBrowseTab(tabFolder);
	}
	
	private void createSelectedConvertersTab(TabFolder parent) {
		TabItem selectConvertersTab = new TabItem(parent, SWT.NONE);
		selectConvertersTab.setText("Select Converters");
		
		Composite selectConvertersTabComposite = new Composite(parent, SWT.NONE);
		selectConvertersTab.setControl(selectConvertersTabComposite);
		selectConvertersTabComposite.setLayout(new RowLayout(SWT.HORIZONTAL));
		
		availableConvertersList = new List(selectConvertersTabComposite, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
		
		Composite selectConvertersTabButtonsComposite = new Composite(selectConvertersTabComposite, SWT.NONE);
		selectConvertersTabButtonsComposite.setLayout(new RowLayout(SWT.VERTICAL));
		
		Button addConverter = new Button(selectConvertersTabButtonsComposite, SWT.NONE);
		addConverter.setText("Add");
		
		Button removeConverter = new Button(selectConvertersTabButtonsComposite, SWT.NONE);
		removeConverter.setText("Remove");
		
		Button moveConverterUp = new Button(selectConvertersTabButtonsComposite, SWT.NONE);
		moveConverterUp.setText("Up");
		
		Button moveConverterDown = new Button(selectConvertersTabButtonsComposite, SWT.NONE);
		moveConverterDown.setText("Down");
		
		selectedConverterList = new List(selectConvertersTabComposite, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
	}
	
	private void createBrowseTab(TabFolder parent) {
		TabItem tbtmBrowse = new TabItem(parent, SWT.NONE);
		tbtmBrowse.setText("Browse");
		
		Composite composite = new Composite(tabFolder, SWT.NONE);
		tbtmBrowse.setControl(composite);
		composite.setLayout(new RowLayout(SWT.VERTICAL));
		
		Composite composite_1 = new Composite(composite, SWT.NONE);
		
		Composite composite_2 = new Composite(composite, SWT.NONE);
		composite_2.setLayout(new TreeColumnLayout());
		
		TreeViewer treeViewer = new TreeViewer(composite_2, SWT.BORDER);
		Tree tree = treeViewer.getTree();
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
		
		Composite composite_3 = new Composite(composite, SWT.NONE);

	}

	private void createSelectedCollectionsTab(TabFolder parent) {
		TabItem selectedCollectionsTab = new TabItem(parent, SWT.NONE);
		selectedCollectionsTab.setText("Select Collections");
		
		Composite selectedCollectionsTabComposite = new Composite(parent, SWT.NONE);
		selectedCollectionsTab.setControl(selectedCollectionsTabComposite);
		RowLayout rl_selectedCollectionsTabComposite = new RowLayout(SWT.VERTICAL);
		rl_selectedCollectionsTabComposite.wrap = false;
		rl_selectedCollectionsTabComposite.fill = true;
		rl_selectedCollectionsTabComposite.center = true;
		selectedCollectionsTabComposite.setLayout(rl_selectedCollectionsTabComposite);
		
		Composite folderSelectorComposite = new Composite(selectedCollectionsTabComposite, SWT.NONE);
		RowLayout rl_composite_1 = new RowLayout(SWT.HORIZONTAL);
		rl_composite_1.center = true;
		folderSelectorComposite.setLayout(rl_composite_1);
		
		Label folderSelectorLabel = new Label(folderSelectorComposite, SWT.NONE);
		folderSelectorLabel.setText("Folder:");
		
		folderSelectorTextBox = new Text(folderSelectorComposite, SWT.BORDER);
		
		Composite selectedCollectionsListsComposite = new Composite(selectedCollectionsTabComposite, SWT.NONE);
		RowLayout rl_composite_3 = new RowLayout(SWT.HORIZONTAL);
		rl_composite_3.fill = true;
		selectedCollectionsListsComposite.setLayout(rl_composite_3);
		
		availableCollectionsList = new List(selectedCollectionsListsComposite, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
		
		Composite selectedCollectionsButtonsComposite = new Composite(selectedCollectionsListsComposite, SWT.NONE);
		selectedCollectionsButtonsComposite.setLayout(new RowLayout(SWT.VERTICAL));
		
		Button addCollection = new Button(selectedCollectionsButtonsComposite, SWT.NONE);
		addCollection.setText("Add");
		
		Button addAllCollections = new Button(selectedCollectionsButtonsComposite, SWT.NONE);
		addAllCollections.setText("Add All");
		
		addedCollectionsList = new List(selectedCollectionsListsComposite, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
	}
}
