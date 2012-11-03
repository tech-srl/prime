package technion.prime.history.converters;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import technion.prime.Options;
import technion.prime.history.History;
import technion.prime.history.Ordering;

public class OrderingInclusionClusterer extends InclusionClusterer {
	
	private Map<History, Set<Ordering>> orderings = 
			new HashMap<History, Set<Ordering>>();

	public OrderingInclusionClusterer(Options options) {
		super(options);
	}

	@Override
	public String getName() {
		return "ordering inclusion";
	}

	@Override
	protected Set<History> getContainers(Iterable<? extends History> histories, History h) {
		Set<History> result = new HashSet<History>();
		for (History potentialContainer : histories) {
			if (getOrdering(potentialContainer).containsAll(getOrdering(h))) {
				result.add(potentialContainer);
			}
		}
		assert(result.contains(h));
		return result;
	}

	@Override
	protected History pickOneContainer(Set<History> containers) {
		History result = null;
		for (History h : containers) {
			if (result == null || getOrdering(h).size() > getOrdering(result).size()) {
				result = h;
			}
		}
		return result;
	}

	@Override
	protected String getTitle(History h, int counter) {
		return String.format("ordering inclusion #%d: %d pairs over %s",
				counter,
				h.getOrderings().size(),
				h.getAllParticipatingMethods());
	}
	
	private Set<Ordering> getOrdering(History h) {
		Set<Ordering> result = orderings.get(h);
		if (result == null) {
			result = h.getOrderings();
			orderings.put(h, result);
		}
		return result;
	}

}
