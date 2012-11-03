package technion.prime.eclipse.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import technion.prime.eclipse.Activator;


/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		
		for (PreferenceConstants.Integers constant : PreferenceConstants.Integers.values()) {
			store.setDefault(constant.name(), constant.getDefault());
		}
		
		for (PreferenceConstants.Strings constant : PreferenceConstants.Strings.values()) {
			store.setDefault(constant.name(), constant.getDefault());
		}
		
		for (PreferenceConstants.Booleans constant : PreferenceConstants.Booleans.values()) {
			store.setDefault(constant.name(), constant.getDefault());
		}
	}

}
