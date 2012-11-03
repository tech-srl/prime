package technion.prime.clusterer;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import technion.prime.history.History;
import technion.prime.utils.MultiMap;

public class ClusteringResult {
	public static class Cluster implements Serializable {
		private static final long serialVersionUID = 6239860379307454106L;
		public boolean isNoise() { return false; }
	}
	
	public static class NoiseCluster extends Cluster {
		private static final long serialVersionUID = -3184634903125533934L;
		public boolean isNoise() { return true; }
	}
	
	private Map<History, Cluster> clusterByHistory = new HashMap<History, Cluster>();
	private final MultiMap<Cluster, History> historyByCluster = new MultiMap<Cluster, History>();
	
	public Cluster getCluster(History h) {
		return clusterByHistory.get(h);
	}
	
	public int getNumClusters() {
		return historyByCluster.size();
	}
}
