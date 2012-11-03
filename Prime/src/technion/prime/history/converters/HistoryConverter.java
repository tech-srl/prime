package technion.prime.history.converters;

import technion.prime.history.HistoryCollection;
import technion.prime.utils.Logger.CanceledException;

/**
 * Can convert a HistoryCollection into another one,
 * while maintaining an association between past histories and new ones,
 * meaning it's possible to find the history / histories which are the source of
 * any new history.
 */
public interface HistoryConverter {
	/**
	 * Given a HistoryCollection, returns a new HistoryCollection which is some conversion
	 * of the histories in the original one. Does not guarantee anything about the size of
	 * the new collection.
	 * @param hc Input HistoryCollection. Should not be changed by this method.
	 * @return A new HistoryCollection which contains all histories generated from the input HistoryCollection.
	 * @throws InterruptedException If the process was interrupted.
	 * @throws CanceledException If the process was canceled.
	 */
	HistoryCollection convert(HistoryCollection hc) throws InterruptedException, CanceledException;
	
	/**
	 * Converter name.
	 */
	String getName();
	
}
