package technion.prime.clusterer;

import technion.prime.history.History;

public interface HistoryClusterer {
	public void addSample(History h);
	public ClusteringResult cluster(FeatureFunction ff, ScoringFunction sf);
}
