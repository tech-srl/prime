//package technion.prime.clusterer;
//
//import java.util.HashSet;
//import java.util.IdentityHashMap;
//import java.util.Map;
//import java.util.Set;
//
//import technion.prime.history.History;
//import technion.prime.history.HistoryCollection;
//import technion.prime.utils.Logger;
//import technion.prime.utils.Logger.CanceledException;
//import weka.core.Instance;
//import weka.core.Instances;
//
//public class WekaIterativeDBScanClusterer implements HistoryClusterer {
//	private final Set<History> histories = new HashSet<History>();
//	
//	@Override
//	public void addSample(History h) {
//		histories.add(h);
//	}
//
//	@Override
//	public ClusteringResult cluster(FeatureFunction ff, ScoringFunction sf) {
//		Instances is = buildInstances();
//		Logger.log(String.format("Preparing clusterer with %d attributes ...", is.numAttributes()));
//		Map<History, Instance> instanceByHistory = fillInstances(is);
//		ClusteringResult result = clusterInstances(is, instanceByHistory);
//	}
//	
//	protected Map<History, Instance> fillInstances(Instances is)
//			throws InterruptedException, CanceledException {
//		Map<History, Instance> result = new IdentityHashMap<History, Instance>();
//		for (History h : histories) {
//			Instance instance = null;
//			instance = buildInstance(is, h);
//			is.add(instance);
//			result.put(h, instance);
//		}
//		return result;
//	}
//
//	private Instance buildInstance(Instances is, History h) {
//		FeatureFunction ff;
//		ff.calculateFeatureVector(h);
//	}
//
//}
