package technion.prime.history.converters;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import technion.prime.Options;
import technion.prime.history.History;
import technion.prime.history.HistoryCollection;
import technion.prime.utils.ConcurrencyUtils;
import technion.prime.utils.Logger.CanceledException;


public abstract class InclusionClusterer implements HistoryConverter {
	private final Options options;
	
	public InclusionClusterer(Options options) {
		this.options = options;
	}

	@Override
	public HistoryCollection convert(HistoryCollection hc) throws InterruptedException, CanceledException {
		// A map from a history to its top-level container
		Map<History, History> topLevelContainers = computeTopLevelContainers(hc.getHistories());
		// A map from a top-level history to a merge of all histories contained in it
		Map<History, History> topLevelToMerged = new HashMap<History, History>();
		
		for (History h : hc.getHistories()) {
			ConcurrencyUtils.checkState();
			History topLevel = topLevelContainers.get(h);
			History merged = topLevelToMerged.get(topLevel);
			if (merged == null) {
				merged = topLevel.clone();
				topLevelToMerged.put(topLevel, merged);
			}
			if (h != topLevel) {
				merged.mergeFrom(h, true);
			}
		}
		
		HistoryCollection result = options.newHistoryCollection();
		int count = 0;
		for (History h : topLevelToMerged.values()) {
			h.setTitle(getTitle(h, count++));
			result.addHistory(h);
		}
		return result;
	}
	
	/**
	 * @param histories
	 * @return A mapping from each history to the topmost history containing it. If none contains
	 * it, will map the history to itself.
	 * @throws CanceledException 
	 * @throws InterruptedException 
	 */
	protected Map<History, History> computeTopLevelContainers(Iterable<? extends History> histories)
			throws InterruptedException, CanceledException {
		Map<History, History> result = new HashMap<History, History>();
		Map<History, History> containers = computeContainers(histories);
		for (History h : histories) {
			ConcurrencyUtils.checkState();
			Set<History> seenContainers = new HashSet<History>();
			seenContainers.add(h);
			History container = h;
			while (seenContainers.contains(containers.get(container)) == false) {
				container = containers.get(container);
				seenContainers.add(container);
			}
			result.put(h, container);
		}
		return result;
	}
	
	/**
	 * This method uses getContainers() to determine the single container for each history.
	 * @param histories
	 * @return A mapping from each history to a single history containing it. Will map to self
	 * if no other history contains it.
	 * @throws CanceledException 
	 * @throws InterruptedException 
	 */
	protected Map<History, History> computeContainers(Iterable<? extends History> histories)
			throws InterruptedException, CanceledException {
		Map<History, History> result = new HashMap<History, History>();
		for (History h : histories) {
			ConcurrencyUtils.checkState();
			Set<History> containers = getContainers(histories, h);
			History container = pickOneContainer(containers);
			result.put(h, container);
		}
		return result;
	}

	/**
	 * @param histories
	 * @param h
	 * @return A set of all the histories in <code>histories</code> which contain
	 * <code>h</code>. By definition, the returned set will always include <code>h</code> itself.
	 */
	protected abstract Set<History> getContainers(Iterable<? extends History> histories, History h);
	
	/**
	 * Pick one container from a list of valid containers.
	 * @param containers
	 * @return The chosen container.
	 */
	protected abstract History pickOneContainer(Set<History> containers);
	
	/**
	 * @param h A clustered history.
	 * @param counter A number guaranteed to be unique for each cluster in this clusterer.
	 * @return Cluster title.
	 */
	protected abstract String getTitle(History h, int counter);
}
