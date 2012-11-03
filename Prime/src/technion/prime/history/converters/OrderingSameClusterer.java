package technion.prime.history.converters;

import java.util.Set;

import technion.prime.Options;
import technion.prime.history.History;
import technion.prime.history.Ordering;

public class OrderingSameClusterer extends SameClusterer<Set<Ordering>> {

	public OrderingSameClusterer(Options options) {
		super(options);
	}

	@Override
	public String getName() {
		return "same ordering";
	}

	@Override
	protected Set<Ordering> getKey(History h) {
		return h.getOrderings();
	}

	@Override
	protected String clusterName(Set<Ordering> key, int counter) {
		return String.format("same ordering #%d: %s", counter, key.toString());
	}

}
