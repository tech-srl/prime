package technion.prime.history.converters;

import technion.prime.Options;
import technion.prime.history.History;
import technion.prime.history.HistoryCollection;
import technion.prime.utils.Logger.CanceledException;

/**
 * Unconditionally merge everything in the input together into one history.
 */
public class MergeAllClusterer implements HistoryConverter {
	private final static int HISTORIES_PER_GROUP = 10;
	private final Options options;
	
	public MergeAllClusterer(Options options) {
		this.options = options;
	}
	
	@Override
	public HistoryCollection convert(HistoryCollection hc)
			throws InterruptedException, CanceledException {
		// This can be trivially implemented but merging into a large history takes time.
		// Instead, we merge HISTORIES_PER_GROUP histories at a time, again and again, until
		// we end up with just one history.
		// This runs O(nlogn) merges, instead of O(n), but the majority of them will be on
		// much smaller histories. HISTORIES_PER_GROUP is the log's base.
		HistoryCollection result = hc;
		do {
			result = mergeGroups(result);
		} while (result.getNumHistories() > 1);
		return result;
	}
	
	private HistoryCollection mergeGroups(HistoryCollection hc)
			throws InterruptedException, CanceledException {
		HistoryCollection result = options.newHistoryCollection();
		HistoryCollection group = options.newHistoryCollection();
		int counter = 0;
		for (History h : hc.getHistories()) {
			group.addHistory(h);
			counter++;
			if (group.getNumHistories() == HISTORIES_PER_GROUP || counter == hc.getNumHistories()) {
				result.addHistory(mergeAll(group));
				group.clear();
			}
		}
		return result;
	}
	
	private History mergeAll(HistoryCollection hc)
			throws InterruptedException, CanceledException {
		History merged = null;
		for (History h : hc.getHistories()) {
			if (merged == null) merged = h.clone();
			else                merged.mergeFrom(h, false);
		}
		return merged;
	}
	
	@Override
	public String getName() {
		return "merge all";
	}

}
