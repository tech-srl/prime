package technion.prime.history.converters;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import technion.prime.Options;
import technion.prime.dom.AppType;
import technion.prime.history.History;

public class TypeInclusionClusterer extends InclusionClusterer {

	public TypeInclusionClusterer(Options options) {
		super(options);
	}

	@Override
	public String getName() {
		return "type inclusion";
	}
	
	private Set<AppType> getTypes(History h) {
		return h.getAllParticipatingApiTypes();
	}

	@Override
	protected Set<History> getContainers(Iterable<? extends History> histories, History h) {
		Set<History> result = new HashSet<History>();
		Set<AppType> types = getTypes(h);
		if (types.isEmpty()) {
			// Can happen if the only methods are unknown methods.
			return Collections.singleton(h);
		}
		for (History potentialContainer : histories) {
			if (potentialContainer == h || getTypes(potentialContainer).containsAll(types)) {
				result.add(potentialContainer);
			}
		}
		return result;
	}

	@Override
	protected History pickOneContainer(Set<History> containers) {
		if (containers.size() == 1) return containers.iterator().next();
		
		int maxNumParticipatingTypes = Integer.MIN_VALUE;
//		int minNumParticipatingTypes = Integer.MAX_VALUE;
		History result = null;
		for (History h : containers) {
			int numTypes = getTypes(h).size();
			if (numTypes > maxNumParticipatingTypes) {
//			if (numTypes < minNumParticipatingTypes) {
				result = h;
				maxNumParticipatingTypes = numTypes;
//				minNumParticipatingTypes = numTypes;
			}
		}
		return result;
	}

	@Override
	protected String getTitle(History h, int counter) {
		return String.format("type inclusion #%d: %s", counter, getTypes(h).toString());
	}

}
