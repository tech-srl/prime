package technion.prime.eclipse.views;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;

import technion.prime.dom.AppClass;
import technion.prime.history.History;
import technion.prime.history.HistoryCollection;
import technion.prime.statistics.Field;
import technion.prime.statistics.AnalysisDetails;
import technion.prime.statistics.Sample;
import technion.prime.utils.Logger;
import technion.prime.utils.Logger.CanceledException;

public class TreeResultsView extends ViewPart {
	
	public static final String ID = "technion.prime.eclipse.views.TreeResultsView";
	
	private static final Field[] sample_columns = new Field[] {
		Sample.NAME,
		Sample.NUM_SAMPLES,
		Sample.PERCENTAGE_SAMPLES,
		Sample.SIZE,
		Sample.MAX_WEIGHT,
		Sample.DEPTH,
		Sample.NUM_TYPES,
		Sample.NUM_EDGES,
		Sample.NUM_UNKNOWN_EDGES
	};
	
	private static final Field[] query_columns = new Field[] {
		AnalysisDetails.QUERY_STRING,
		AnalysisDetails.NUM_SAMPLES,
	};
	
	private static final String FILENAME_PROPERTY = "filename";
	private static final String SAMPLE_PROPERTY = "sample";
	private static final String HISTORY_PROPERTY = "history";
	
	private Clipboard clipboard;
	private Tree tree;
	private Text statisticsText;
	private List uncompilableList;
	private List unanalyzableList;
	private Action resultLineDoubleClick;
	private Action uncompilableDoubleClick;
	private Action unanalyzableDoubleClick;
	private Action copyAction;
	private Action saveAsHtmlAction;
	private Action saveAsXmlAction;
	private Action saveCachedFileAction;
	private TabFolder tabFolder;
	private Image image_ascending;
	private Image image_descending;

	private AnalysisDetails details;
	private String filterString = "";
	private Field sort_field = sample_columns[1];
	private boolean sort_ascending = false;
	private Set<Sample> childPassesFilter = new HashSet<Sample>();
	private Set<Sample> parentPassesFilter = new HashSet<Sample>();
	private int saveCounter = 0;
	private String outputFolder;
	
	public void setResults(AnalysisDetails details) {
		if (details == null) return;
		
		this.details = details;
		
		// Populate tree:
		drawTree();
		// Populate statistics:
		statisticsText.setText(details.getProcessDetails());
		// Populate uncompilable:
		for (String s : details.getUncompilableSources()) {
			uncompilableList.add(s);
		}
		// Populate unanalyzable:
		for (AppClass c : details.getUnanalyzableClasses()) {
			if (c.getClassFileName() == null) continue;
			uncompilableList.add(c.getClassFileName());
		}
	}
	
	private void drawTree() {
		if (details == null) return;
		
		Set<Sample> expanded = new HashSet<Sample>();
		for (TreeItem ti : tree.getItems()) {
			expanded.addAll(getExpanded(ti));
		}
		boolean wasQueryExpanded = tree.getItemCount() > 0 ? tree.getItem(0).getExpanded() : true;
		tree.removeAll();
		if (details != null) createTreeItemFromAnalysisDetails(tree, expanded);
		TreeItem root = tree.getItems()[0];
		//TreeItem root = tree.getTopItem();
		if (root != null) root.setExpanded(wasQueryExpanded);
	}
	
	private Set<Sample> getExpanded(TreeItem parent) {
		Set<Sample> result = new HashSet<Sample>();
		if (parent.getExpanded()) result.add((Sample)parent.getData(SAMPLE_PROPERTY));
		for (TreeItem ti : parent.getItems()) {
			result.addAll(getExpanded(ti));
		}
		return result;
	}

	private void createTreeItemFromAnalysisDetails(Tree parent, Set<Sample> toExpand) {
		TreeItem ti = new TreeItem(parent, SWT.NONE);
		int i = 0;
		for (Field f : query_columns) {
			ti.setText(i++, f == null ? "" : details.getString(f));
		}
		Set<Sample> filtered = filter(details.getSamples(), false);
		Sample[] sorted = sortSamples(filtered, sort_field, sort_ascending);
		for (Sample s : sorted) {
			createTreeItemFromSample(ti, s, toExpand);
		}
	}
	
	private Set<Sample> filter(Set<Sample> samples, boolean parentPassed) {
		Set<Sample> result = new HashSet<Sample>();
		for (Sample s : samples) {
			boolean thisPassed = passesFilter(s);
			Set<Sample> filtered = filter(s.getSamples(), parentPassed || thisPassed);
			boolean childPassed = filtered.size() > 0;
			
			if (parentPassed) parentPassesFilter.add(s);
			if (childPassed) childPassesFilter.add(s);
			if (childPassed || thisPassed) result.add(s);
		}
		return result;
	}
	
	private boolean passesFilter(Sample s) {
		if (filterString.isEmpty()) return true;
		boolean matchesAll = true;
		for (String word : filterString.split(" ")) {
			matchesAll &= word.toLowerCase().equals(word) ?
				// It's all lower-case
				s.getString(Sample.NAME).toLowerCase().contains(word.toLowerCase()) :
				// At least one upper-case character
				s.getString(Sample.NAME).contains(word);
			if (matchesAll == false) break;
		}
		return matchesAll;
	}

	private void createTreeItemFromSample(TreeItem parent, Sample s, Set<Sample> toExpand) {
		TreeItem ti = new TreeItem(parent, SWT.NONE);
		boolean directlyPassesFilter = passesFilter(s);
		if (!(directlyPassesFilter || childPassesFilter.contains(s) || parentPassesFilter.contains(s))) {
			ti.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_GRAY));
		}
		int i = 0;
		boolean skip = false;
		for (Field f : sample_columns) {
			// Stripe columns:
			if (i % 2 == 1) ti.setBackground(i, ti.getDisplay().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
			
			if (f == null || skip) {
				ti.setText(i++, "");
				skip = false;
				continue;
			}
			if (f == Sample.NUM_SAMPLES && s.getInteger(f) == 0) {
				ti.setText(i++, "");
				skip = true;
				continue;
			}
			ti.setText(i, s.getString(f));
			if (isPoor(f, s) && directlyPassesFilter) {
				ti.setForeground(0, parent.getDisplay().getSystemColor(SWT.COLOR_RED));
				ti.setForeground(i, parent.getDisplay().getSystemColor(SWT.COLOR_RED));
			} else if (isGreat(f, s) && directlyPassesFilter) {
				ti.setForeground(i, parent.getDisplay().getSystemColor(SWT.COLOR_BLUE));
			}
			i++;
		}
		ti.setData(SAMPLE_PROPERTY, s);
		ti.setData(HISTORY_PROPERTY, details.getSampleHistory(s));
		Sample[] sorted = sortSamples(s.getSamples(), sort_field, sort_ascending);
		for (Sample inner : sorted) {
			createTreeItemFromSample(ti, inner, toExpand);
		}
		if (toExpand.contains(s)) ti.setExpanded(true);
	}

	private boolean isPoor(Field f, Sample s) {
		if (f == Sample.DEPTH) return s.getInteger(f) <= 1;
		if (f == Sample.SIZE) return s.getInteger(f) <= 1;
		if (f == Sample.NUM_EDGES) return s.getInteger(f) <= 1;
		if (f == Sample.NUM_TYPES) return s.getInteger(f) <= 0;
		if (f == Sample.MAX_WEIGHT) return s.getDouble(f) <= 0;
		if (f == Sample.NUM_SAMPLES) return s.getInteger(f) == 1;
		return false;
	}
	
	private boolean isGreat(Field f, Sample s) {
		if (f == Sample.DEPTH) return s.getInteger(f) >= 5;
		if (f == Sample.NUM_SAMPLES) return s.getInteger(f) >= 100;
		if (f == Sample.MAX_WEIGHT) return s.getDouble(f) >= 100;
		if (f == Sample.PERCENTAGE_SAMPLES) return s.getDouble(f) > 50;
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		clipboard = new Clipboard(parent.getShell().getDisplay());
		tabFolder = new TabFolder(parent, SWT.BORDER);
		image_ascending = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_FORWARD);
		image_descending = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_BACK);
		
		createTreeTab(tabFolder);
		createStatisticsTab(tabFolder);
		createUncompilableTab(tabFolder);
		createUnanalyzableTab(tabFolder);
		
		makeActions();
		hookActions();
		contributeToActionBars();
	}
	
	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		bars.getToolBarManager().add(copyAction);
		bars.getToolBarManager().add(saveAsHtmlAction);
		bars.getToolBarManager().add(saveAsXmlAction);
		bars.getToolBarManager().add(saveCachedFileAction);
	}
	
	private void createUncompilableTab(TabFolder tabFolder) {
		TabItem tab = new TabItem(tabFolder, SWT.NONE);
		tab.setText("Uncompilable files");
		Control c = createUncompilableList(tabFolder);
		tab.setControl(c);
	}

	private Control createUncompilableList(TabFolder tabFolder) {
		uncompilableList = new List(tabFolder, SWT.H_SCROLL | SWT.V_SCROLL);
		uncompilableList.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// Called on double-click
				uncompilableDoubleClick.run();
			}
		});
		return uncompilableList;
	}
	
	private void createUnanalyzableTab(TabFolder tabFolder) {
		TabItem tab = new TabItem(tabFolder, SWT.NONE);
		tab.setText("Unanalyzable classes");
		Control c = createUnanalyzableList(tabFolder);
		tab.setControl(c);
	}

	private Control createUnanalyzableList(TabFolder tabFolder) {
		unanalyzableList = new List(tabFolder, SWT.H_SCROLL | SWT.V_SCROLL);
		unanalyzableList.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// Called on double-click
				unanalyzableDoubleClick.run();
			}
		});
		return unanalyzableList;
	}
	
	private void createTreeTab(TabFolder parent) {
		TabItem tab = new TabItem(parent, SWT.NONE);
		tab.setText("Results table");
		Control table = createTreeArea(tabFolder);
		tab.setControl(table);
	}

	private Control createTreeArea(TabFolder parent) {
		Composite c = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(4, false);
		c.setLayout(layout);
		createSearchBox(c);
		createTree(c);
		
		return c;
	}

	private void createTree(Composite parent) {
		tree = new Tree(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.SINGLE);
		tree.setHeaderVisible(true);
		GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalSpan = 4;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		tree.setLayoutData(gridData);
		for (Field f : sample_columns) {
			TreeColumn tc = new TreeColumn(tree, SWT.LEFT);
			tc.setText(f.getTitle());
			tc.setMoveable(true);
			tc.setResizable(true);
			tc.setWidth(f == Sample.NAME ? 800 : 100);
			tc.addSelectionListener(createColumnClickAction(tc, f));
		}
		tree.addKeyListener(new KeyListener() {
			@Override public void keyReleased(KeyEvent e) {
				if (tree.getSelectionCount() == 1) {
					switch (e.keyCode) {
					case SWT.ARROW_RIGHT: tree.getSelection()[0].setExpanded(true); break;
					case SWT.ARROW_LEFT: tree.getSelection()[0].setExpanded(false); break;
					}
				}
			}
			@Override public void keyPressed(KeyEvent e) {}
		});
	}
	
	private SelectionListener createColumnClickAction(final TreeColumn tc, final Field f) {
		return new SelectionListener() {
			@Override public void widgetSelected(SelectionEvent e) {
				sort_ascending = sort_field == f ? !sort_ascending : false;
				sort_field = f;
				for (TreeColumn tc : tree.getColumns()) {
					tc.setImage(null);
				}
				Image image = sort_ascending ? image_ascending : image_descending;
				tc.setImage(image);
				drawTree();
			}
			
			@Override public void widgetDefaultSelected(SelectionEvent e) {}
		};
	}
	
	private Sample[] sortSamples(Set<Sample> samples, final Field f, final boolean ascending) {
		Comparator<Sample> comp = new Comparator<Sample>() {
			@Override public int compare(Sample s1, Sample s2) {
				int order;
				if (f.getType() == String.class) {
					String string1 = s1.getString(f);
					String string2 = s2.getString(f);
					if (sharePrefixBeforeNumber(string1, string2)) {
						order = compareSuffixNumber(string1, string2);
					} else {
						order = string1.compareTo(string2);
					}
				} else {
					order = (int)Math.signum(s1.getDouble(f) - s2.getDouble(f));
				}
				return ascending ? order : -order;
			}
		};
		Sample[] result = samples.toArray(new Sample[samples.size()]);
		Arrays.sort(result, comp);
		return result;
	}
	
	private String getPrefixBeforeNumber(String s) {
		Pattern p = Pattern.compile("[^0-9]*");
		Matcher m = p.matcher(s);
		return m.group();
	}

	protected int compareSuffixNumber(String string1, String string2) {
		string1 = string1.substring(getPrefixBeforeNumber(string1).length());
		string2 = string2.substring(getPrefixBeforeNumber(string2).length());
		return Double.compare(Double.parseDouble(string1), Double.parseDouble(string2));
	}

	protected boolean sharePrefixBeforeNumber(String s1, String s2) {
		return getPrefixBeforeNumber(s1).equals(getPrefixBeforeNumber(s2));
	}

	private void createStatisticsTab(TabFolder tabFolder) {
		TabItem tab = new TabItem(tabFolder, SWT.NONE);
		tab.setText("Statistics");
		Control statistics = createStatistics(tabFolder);
		tab.setControl(statistics);
	}
	
	private Control createStatistics(Composite parent) {
		statisticsText = new Text(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		return statisticsText;
	}
	
	private void createSearchBox(Composite parent) {
		Label searchLabel = new Label(parent, SWT.NONE);
		searchLabel.setText("Filter: ");
		final Text searchText = new Text(parent, SWT.BORDER | SWT.SEARCH);
		searchText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		final Button searchButton = new Button(parent, SWT.NONE);
		searchButton.setText("Apply");
		searchButton.addSelectionListener(new SelectionListener() {
			@Override public void widgetSelected(SelectionEvent e) {
				filterString = searchText.getText();
				childPassesFilter.clear();
				parentPassesFilter.clear();
				searchText.selectAll();
				drawTree();
			}
			@Override public void widgetDefaultSelected(SelectionEvent e) {}
		});
		final Button clearButton = new Button(parent, SWT.NONE);
		clearButton.setText("Clear");
		clearButton.addSelectionListener(new SelectionListener() {
			@Override public void widgetSelected(SelectionEvent e) {
				searchText.setText("");
				searchButton.notifyListeners(SWT.Selection, new Event());
			}
			@Override public void widgetDefaultSelected(SelectionEvent e) {}
		});
		searchText.addKeyListener(new KeyListener() {
			@Override public void keyReleased(KeyEvent e) {
				switch (e.keyCode) {
				case SWT.CR: searchButton.notifyListeners(SWT.Selection, new Event()); break;
				case SWT.ESC: clearButton.notifyListeners(SWT.Selection, new Event()); break;
				}
			}
			@Override public void keyPressed(KeyEvent e) {}
		});
	}
	
	private void makeActions() {
		makeResultLineDoubleClickAction();
		makeUncompilableDoubleClickAction();
		makeUnanalyzableDoubleClickAction();
		makeCopyAction();
		makeSaveAsHtmlAction();
		makeSaveAsXmlAction();
		makeSaveCacheFileAction();
	}

	private void makeSaveAsXmlAction() {
		saveAsXmlAction = new Action() {
			@Override
			public void run() {
				if (details == null) return;
				DirectoryDialog dd = new DirectoryDialog(tree.getShell());
				String folder = dd.open();
				if (folder == null) return;
				try {
					Logger.log("Generating XML output...");
					HistoryCollection hc = details.getFinalHistoryCollection();
					hc.generateXmlOutput(folder);
					Logger.log("Done.");
				} catch (InterruptedException e) {
					Logger.exception(e);
				} catch (CanceledException e) {
					Logger.exception(e);
				} catch (IOException e) {
					Logger.exception(e);
				}
			}
		};
		saveAsXmlAction.setToolTipText("Save each clustered sample as XML file.");
		saveAsXmlAction.setText("Save As XML...");
	}
	
	private void makeSaveCacheFileAction() {
		saveCachedFileAction = new Action() {
			@Override
			public void run() {
				if (details == null) return;
				FileDialog fd = new FileDialog(tree.getShell(), SWT.SAVE);
				String filepath = fd.open();
				if (filepath == null) return;
				try {
					// This would probably crash if the cs was not the one actually used for clustering:
					details.getFinalHistoryCollection().save(filepath);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		saveCachedFileAction.setToolTipText("Save the final result to a cached file.");
		saveCachedFileAction.setText("Save cache file...");
	}

	private void makeSaveAsHtmlAction() {
		saveAsHtmlAction = new Action() {
			@Override
			public void run() {
				if (details == null) return;
				MessageDialog.openError(tree.getShell(), "Unsupported operation",
						"This functionality is currently disabled.");
				DirectoryDialog dd = new DirectoryDialog(tree.getShell());
				String folder = dd.open();
				if (folder != null) folder = null;
				if (folder == null) return;
				@SuppressWarnings("unused")
				String filename = "index.xhtml";
				try {
					Logger.log("Generating HTML and SVG output...");
					details.saveToHtml(folder, filename);
					Logger.log("Done.");
				} catch (IllegalStateException e) {
					MessageDialog.openInformation(tree.getShell(), "Could not find dot.exe", e.getMessage());
				} catch (IOException e) {
					Logger.exception(e);
				}
			}
		};
		saveAsHtmlAction.setToolTipText("Save tree with images to an html file.");
		saveAsHtmlAction.setText("Save As HTML...");
	}

	private void makeCopyAction() {
		copyAction = new Action() {
			@Override
			public void run() {
				String toCopy = null;
				switch (tabFolder.getSelectionIndex()) {
				case 0:
					StringBuilder sb = new StringBuilder();
					for (TreeItem ti : tree.getSelection()) {
						Object filename = ti.getData(FILENAME_PROPERTY);
						if (filename != null) sb.append(filename.toString());
						else sb.append(ti.getText(0));
					}
					if (sb.length() > 0) toCopy = sb.toString();
				case 2:
					String[] s1 = uncompilableList.getSelection();
					if (s1.length > 0) toCopy = s1[0];
					break;
				case 3:
					String[] s2 = unanalyzableList.getSelection();
					if (s2.length > 0) toCopy = s2[0];
					break;
				}
				if (toCopy != null) copyToClipboard(toCopy);
			}
		};
		copyAction.setToolTipText("Copy text from selection to clipboard.");
		copyAction.setText("Copy to clipboard");
	}

	private void makeUnanalyzableDoubleClickAction() {
		unanalyzableDoubleClick = new Action() {
			@Override
			public void run() {
				String[] selection = unanalyzableList.getSelection();
				if (selection.length > 0) {
					String filename = (String)unanalyzableList.getData(selection[0]);
					openFile(filename);
				}
			}
		};
	}

	private void makeUncompilableDoubleClickAction() {
		uncompilableDoubleClick = new Action() {
			@Override
			public void run() {
				String[] selection = uncompilableList.getSelection();
				if (selection.length > 0) {
					openFile(selection[0]);
				}
			}
		};
	}

	private void makeResultLineDoubleClickAction() {
		resultLineDoubleClick = new Action() {
			@Override
			public void run() {
				String filename = getResultSelectionFile();
				if (filename != null) openFile(filename);
			}
		};
	}
	
	private String getResultSelectionFile() {
		TreeItem[] selection = tree.getSelection();
		if (selection.length != 1) return null;
		if (selection[0] == tree.getItem(0)) {
			// First line - use this for expanding / collapsing all
			toggleExpandAll();
			return null;
		}
		Object filename = selection[0].getData(FILENAME_PROPERTY);
		if (filename == null) {
			History h = (History)selection[0].getData(HISTORY_PROPERTY);
			try {
				filename = h.generateGraphvizOutput(outputFolder, saveCounter++);
				selection[0].setData(FILENAME_PROPERTY, filename);
			} catch (IOException e) {
				MessageDialog.openError(tree.getShell(), "Output generation saved", 
						e.getMessage());
			} catch (InterruptedException e) {
				MessageDialog.openError(tree.getShell(), "Interrupted during saving", 
						e.getMessage());
			} catch (CanceledException e) {
				// Cancellation means quickly and silently stopping.
			}
		}
		return filename.toString();
	}
	
	private void toggleExpandAll() {
		boolean changeTo = ! tree.getItem(0).getExpanded();
		toggleExpand(tree.getItem(0), changeTo);
	}

	private void toggleExpand(TreeItem item, boolean changeTo) {
		item.setExpanded(changeTo);
		for (TreeItem child : item.getItems()) {
			toggleExpand(child, changeTo);
		}
	}

	protected void copyToClipboard(String s) {
		clipboard.setContents(new Object[]{s}, new Transfer[] {TextTransfer.getInstance()});
	}
	
	private void openFile(String filename) {
		if (filename.isEmpty()) {
			MessageDialog.openInformation(tree.getShell(), "No file available", 
					"There was an internal error, no file is available.");
			return;
		}
		File fileToOpen = new File(filename);

		IFileStore fileStore = EFS.getLocalFileSystem().getStore(fileToOpen.toURI());
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

		try {
			IDE.openEditorOnFileStore(page, fileStore);
		} catch (PartInitException e) {
			Logger.exception(e);
		}
	}

	private void hookActions() {
		tree.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				resultLineDoubleClick.run();
			}
		});
	}

	@Override
	public void setFocus() {
		tree.setFocus();
	}

	public void setOutputFolder(String outputFolder) {
		this.outputFolder = outputFolder;
	}

}
