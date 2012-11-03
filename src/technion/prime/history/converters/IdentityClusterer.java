package technion.prime.history.converters;

import technion.prime.history.HistoryCollection;

public class IdentityClusterer implements HistoryConverter {

	@Override
	public HistoryCollection convert(HistoryCollection hc) {
		return hc;
	}

	@Override
	public String getName() {
		return "identity";
	}

}
