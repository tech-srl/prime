package technion.prime.postprocessing.slicing;

import technion.prime.Options;
import technion.prime.history.History;
import technion.prime.history.HistoryCollection;


/**
 * Report all the methods exhibiting the sliced-on behavior.
 */
public class Slicer {
	public interface Predicate {
		boolean passes(History h);
	}
	
	private final Options options;

	public Slicer(Options options) {
		this.options = options;
	}
	
	/**
	 * Creates a new history collection which only contains histories that pass the predicate.
	 * 
	 * @param hc Input history.
	 * @param p Predicate that passes or fails histories.
	 * @return A new history collection composed of all the histories in the input history
	 * collection for which the predicate returns true.
	 */
	public HistoryCollection slice(HistoryCollection hc, Predicate p) {
		HistoryCollection result = options.newHistoryCollection();
		for (History h : hc.getHistories()) {
			if (p.passes(h)) result.addHistory(h);
		}
		return result;
	}
	
}
