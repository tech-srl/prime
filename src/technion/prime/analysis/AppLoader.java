package technion.prime.analysis;

import java.util.Collection;

import technion.prime.utils.Logger.CanceledException;
import technion.prime.dom.App;
import technion.prime.utils.CompiledItem;

public interface AppLoader {
	/**
	 * @return A new app, containing the items previously inserted into this loader.
	 * @throws CanceledException
	 */
	App load() throws CanceledException;

	/**
	 * @param items
	 */
	void addCompiledItems(Collection<CompiledItem> items);

	/**
	 * @param folders
	 */
	void addEntireFolders(Collection<String> folders);

	/**
	 * @param filenames
	 */
	void addEntireJars(Collection<String> filenames);
}
