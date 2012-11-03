package technion.prime.history.converters;

import java.util.HashMap;
import java.util.Map;

import technion.prime.Options;
import technion.prime.history.History;
import technion.prime.history.HistoryCollection;
import technion.prime.utils.Logger.CanceledException;

public abstract class SameClusterer<Key> implements HistoryConverter {
	private final Options options;
	
	public SameClusterer(Options options) {
		this.options = options;
	}
	
	@Override
	public HistoryCollection convert(HistoryCollection hc) throws InterruptedException, CanceledException {
		Map<Key, History> buckets = new HashMap<Key, History>();
		HistoryCollection result = options.newHistoryCollection();
		int count = 0;
		for (History h : hc.getHistories()) {
			Key key = getKey(h);
			if (buckets.containsKey(key)) {
				clusterHistories(buckets.get(key), h);
			} else {
				History newHistory = h.clone();
				newHistory.setTitle(clusterName(key, count++));
				result.addHistory(newHistory);
				buckets.put(key, newHistory);
			}
		}
		return result;
	}
	
	protected abstract Key getKey(History h);

	protected abstract String clusterName(Key key, int counter);
	
	protected History clusterHistories(History h1, History h2) throws InterruptedException, CanceledException {
		h1.mergeFrom(h2, false);
		return h1;
	}
	
}
