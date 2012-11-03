package technion.prime.history.converters;

import java.util.HashSet;
import java.util.Set;

import technion.prime.Options;
import technion.prime.history.History;


public class AutomataInclusionClusterer extends InclusionClusterer {
	
	public AutomataInclusionClusterer(Options options) {
		super(options);
	}

	@Override
	protected History pickOneContainer(Set<History> containers) {
		History result = null;
		for (History h : containers) {
			if (result == null || h.getNumNodes() > result.getNumNodes()) {
				result = h;
			}
		}
		return result;
	}

	@Override
	protected Set<History> getContainers(Iterable<? extends History> histories, History h) {
		Set<History> containers = new HashSet<History>();
		for (History potentialContainer : histories) {
			if (h == potentialContainer || potentialContainer.includes(h)) {
				containers.add(potentialContainer);
			}
		}
		return containers;
	}

	@Override
	public String getName() {
		return "automata inclusion";
	}

	@Override
	protected String getTitle(History h, int counter) {
		return "automata inclusion #" + counter;
	}

}
