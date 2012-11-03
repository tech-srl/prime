package technion.prime.history.converters;

import java.util.HashSet;
import java.util.Set;

import technion.prime.Options;
import technion.prime.history.History;
import technion.prime.history.edgeset.EdgeHistory;
import technion.prime.utils.Logger.CanceledException;

public class RelaxedInclusionClusterer extends InclusionClusterer {

	public RelaxedInclusionClusterer(Options options) {
		super(options);
	}

	@Override
	public String getName() {
		return "relaxed inclusion";
	}

	@Override
	protected Set<History> getContainers(Iterable<? extends History> histories, History h) {
		Set<History> result = new HashSet<History>();
		for (History container : histories) {
			try {
				if (((EdgeHistory)container).includesWithUnknown(h)) result.add(container);
			} catch (InterruptedException e) {
				continue;
			} catch (CanceledException e) {
				return null;
			}
		}
		return result;
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
	protected String getTitle(History h, int counter) {
		return String.format("relaxed inclusion #%d", counter);
	}

}
