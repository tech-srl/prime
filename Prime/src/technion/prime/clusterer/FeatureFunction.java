package technion.prime.clusterer;

import technion.prime.history.History;

public interface FeatureFunction {
	double[] calculateFeatureVector(History h);
}
