package technion.prime.eclipse.preferences;

import org.eclipse.jface.preference.*;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;

import technion.prime.eclipse.Activator;

/**
 * This class represents a preference page that
 * is contributed to the Preferences dialog. By 
 * subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows
 * us to create a page that is small and knows how to 
 * save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They
 * are stored in the preference store that belongs to
 * the main plug-in class. That way, preferences can
 * be accessed directly via the preference store.
 */

public class PrimePreferences
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {

	public PrimePreferences() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("Prime Preferences.");
	}
	
	/**
	 * Creates the field editors. Field editors are abstractions of
	 * the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and
	 * restore itself.
	 */
	@Override
	public void createFieldEditors() {
		addField(new DirectoryFieldEditor(PreferenceConstants.Strings.OUTPUT_PATH.name(),
				"Output folder for result files:", getFieldEditorParent()));
		addField(new DirectoryFieldEditor(PreferenceConstants.Strings.TEMPDIR_PATH.name(),
				"Temporary folder:", getFieldEditorParent()));
		addField(new IntegerFieldEditor(PreferenceConstants.Integers.SINGLE_DOWNLOAD_TIMEOUT.name(),
				"Timeout for downloading each file (in ms):", getFieldEditorParent()));
		addField(new IntegerFieldEditor(PreferenceConstants.Integers.ALL_DOWNLOAD_TIMEOUT.name(),
				"Timeout for entire download stage (in ms):", getFieldEditorParent()));
		addField(new IntegerFieldEditor(PreferenceConstants.Integers.SINGLE_COMPILE_TIMEOUT.name(),
				"Timeout for compiling each file (in ms):", getFieldEditorParent()));
		addField(new IntegerFieldEditor(PreferenceConstants.Integers.ALL_COMPILE_TIMEOUT.name(),
				"Timeout for entire compile stage (in ms):", getFieldEditorParent()));
		addField(new IntegerFieldEditor(PreferenceConstants.Integers.SINGLE_ANALYZE_TIMEOUT.name(),
				"Timeout for analyzing each method (in ms):", getFieldEditorParent()));
		addField(new IntegerFieldEditor(PreferenceConstants.Integers.ALL_ANALYZE_TIMEOUT.name(),
				"Timeout for entire analysis stage (in ms):", getFieldEditorParent()));
		addField(new IntegerFieldEditor(PreferenceConstants.Integers.THREAD_POOL_SIZE.name(),
				"&Thread pool size for concurrent operations:", getFieldEditorParent()));
		addField(new BooleanFieldEditor(PreferenceConstants.Booleans.GENERATE_UNCLUSTERED.name(),
				"&Generate output from unclustered results", getFieldEditorParent()));
		BooleanFieldEditor ignoreJSL = new BooleanFieldEditor(PreferenceConstants.Booleans.IGNORE_JSL.name(),
				"&Ignore Java Standard Library classes", getFieldEditorParent());
		ignoreJSL.getDescriptionControl(getFieldEditorParent()).setToolTipText("If this option is enabled, code inside the Java Standard Libraries will not be examined\neven if the query is about a type from the Java Standard Library.");
		addField(ignoreJSL);
		addField(new BooleanFieldEditor(PreferenceConstants.Booleans.SHOW_EXCEPTIONS.name(),
				"&Show exception details", getFieldEditorParent()));
		addField(new FileFieldEditor(PreferenceConstants.Strings.CVS_PATH.name(),
				"Path of CVS executable:", getFieldEditorParent()));
		addField(new FileFieldEditor(PreferenceConstants.Strings.GIT_PATH.name(),
				"Path of Git executable:", getFieldEditorParent()));
		addField(new FileFieldEditor(PreferenceConstants.Strings.DOT_PATH.name(),
				"Path of dot executable:", getFieldEditorParent()));
		addField(new BooleanFieldEditor(PreferenceConstants.Booleans.COMPILE_IN_PARALLEL.name(),
				"&Compile in parallel", getFieldEditorParent()));
		addField(new BooleanFieldEditor(PreferenceConstants.Booleans.ANALYZE_IN_PARALLEL.name(),
				"&Analyze in parallel", getFieldEditorParent()));
		addField(new BooleanFieldEditor(PreferenceConstants.Booleans.ISOLATE_UNCOMPILABLE.name(),
				"&Isolate uncompilable files", getFieldEditorParent()));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	@Override
	public void init(IWorkbench workbench) {
	}
	
	@Override
	public boolean okToLeave() {
		return true;
	}
	
	@Override
	public boolean isValid() {
		return true;
	}
	
}