package technion.prime.history.converters;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import technion.prime.Options;
import technion.prime.dom.AppType;
import technion.prime.dom.dummy.DummyAppType;
import technion.prime.history.History;
import technion.prime.history.HistoryCollection;
import technion.prime.utils.Logger.CanceledException;
import technion.prime.utils.Partition;

/**
 * Clusters together all histories that share at least one API type.
 * 
 * If we have the histories with types {A, B}, {B, C}, {C, D} and {E, F}, then all but the last
 * will be clustered together.
 */
public class TypeIntersectionClusterer implements HistoryConverter {
	private final AppType nonApiType = new DummyAppType("<non-api>");
	private final Options options;

	public TypeIntersectionClusterer(Options options) {
		this.options = options;
	}
	
	@Override
	public HistoryCollection convert(HistoryCollection hc)
			throws InterruptedException, CanceledException {
		Map<AppType, Partition<AppType>> partitions = preparePartitions(hc);
		partition(hc, partitions);
		Collection<History> mergedHistories = mergeByPartition(hc, partitions);
		
		int counter = 0;
		HistoryCollection result = options.newHistoryCollection();
		for (History h : mergedHistories) {
			result.addHistory(h);
			h.setTitle(getTitle(h, counter++));
		}
		return result;
	}
	
	private Map<AppType, Partition<AppType>> preparePartitions(HistoryCollection hc) {
		Map<AppType, Partition<AppType>> partitions = new HashMap<AppType, Partition<AppType>>();
		for (AppType t : hc.getAllParticipatingApiTypes()) {
			partitions.put(t, Partition.singleton(t));
		}
		// And add one more which will be used for all non-api types
		partitions.put(nonApiType, Partition.singleton(nonApiType));
		return partitions;
	}
	
	private void partition(HistoryCollection hc, Map<AppType, Partition<AppType>> partitions) {
		for (History h : hc.getHistories()) {
			Partition<AppType> first = null;
			for (AppType t : h.getAllParticipatingApiTypes()) {
				if (first == null) {
					first = partitions.get(t);
				} else {
					first.merge(partitions.get(t));
				}
			}
		}
	}

	private Collection<History> mergeByPartition(HistoryCollection hc,
			Map<AppType, Partition<AppType>> partitions) throws InterruptedException,
			CanceledException {
		Map<Partition<?>, History> mergedHistories = new HashMap<Partition<?>, History>();
		for (History h : hc.getHistories()) {
			Set<AppType> types = h.getAllParticipatingApiTypes();
			AppType t = types.isEmpty() ? nonApiType : types.iterator().next();
			Partition<?> p = partitions.get(t).find();
			History merged = mergedHistories.get(p);
			if (merged == null) {
				mergedHistories.put(p, h.clone());
			} else {
				merged.mergeFrom(h, true);
			}
		}
		return mergedHistories.values();
	}
	
	private String getTitle(History h, int counter) {
		return String.format("type intersection cluster #%d: %s",
				counter,
				h.getAllParticipatingApiTypes().toString());
	}

	@Override
	public String getName() {
		return "type intersection";
	}

}
