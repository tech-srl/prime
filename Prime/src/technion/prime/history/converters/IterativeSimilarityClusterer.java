//package technion.prime.history.converters;
//
//import java.util.Enumeration;
//import java.util.HashMap;
//import java.util.IdentityHashMap;
//import java.util.Map;
//
//import technion.prime.Options;
//import technion.prime.history.History;
//import technion.prime.history.HistoryCollection;
//import technion.prime.utils.ConcurrencyUtils;
//import technion.prime.utils.Logger;
//import technion.prime.utils.Logger.CanceledException;
//import weka.clusterers.Clusterer;
//import weka.core.Instance;
//import weka.core.Instances;
//
//public abstract class IterativeSimilarityClusterer implements HistoryConverter {
//	private static final int MAX_TITLE_LENGTH = 1000;
//
//	private final Options options;
//	
//	protected History currentNoiseCluster;
//	
//	public IterativeSimilarityClusterer(Options options) {
//		this.options = options;
//	}
//	
//	@Override
//	public HistoryCollection convert(HistoryCollection hc) throws InterruptedException, CanceledException {
//		Instances is = buildInstances();
//		Logger.log(String.format("Preparing clusterer with %d attributes...", is.numAttributes()));
//		Map<History, Instance> instanceByHistory = fillInstances(is, hc);
//		HistoryCollection result = clusterInstances(hc, is, instanceByHistory);
//		for (History h : result.getHistories()) {
//			String title = getTitle(h);
//			if (title.length() > MAX_TITLE_LENGTH) {
//				title = title.substring(0, MAX_TITLE_LENGTH) + " ...";
//			}
//			h.setTitle(title);
//		}
//		return result;
//	}
//	
//	HistoryCollection clusterInstances(HistoryCollection hc, Instances is,
//			Map<History, Instance> instanceByHistory) throws InterruptedException,
//			CanceledException {
//		int low_minPoints = 2;
//		int high_minPoints = is.numInstances();
//		double low_radius = 1;
//		double high_radius = is.numAttributes();
//		
//		while (true) {
////			double 
//		}
//		
//		 int minPoints = 11;//calculateMinPoints(is);
//		double radius = calculateInitialRadius(is);
//		int iterationCount = 0;
//		double goodEnough = computeScore(hc) * getGoodEnoughScoreMultiplier();
////		Logger.debug("  good enough score will be: " + goodEnough);
//		Clusterer c = null;
//		HistoryCollection result;
//		while (true) {
//			iterationCount++;
//			ConcurrencyUtils.checkState();
////			Logger.debug(String.format("  clustering iteration %d. Radius: %f. MinPoints: %d",
////					numIterations, radius, minPoints));
//			try {
//				c = createDBScanClusterer(radius, minPoints);
//				c.buildClusterer(is);
//			} catch (Exception e) {
//				Logger.exception(e);
//				return hc.clone();
//			}
//			result = mergeByClusterer(c, instanceByHistory, hc);
//			double score = computeScore(result);
//			
////			Logger.debug(String.format("  got %d clusters with score %f", numClusters, score));
//			radius = updateRadiusForIteration(radius);
//			
//			if (iterationCount >= getIterationLimit()) break;
//			if (score <= goodEnough) break;
//		}
//		return result;
//	}
//	
//	private int calculateMinPoints(Instances is) {
//		double sumWeights = 0;
//		@SuppressWarnings("unchecked")
//		Enumeration<Instance> e = is.enumerateInstances();
//		while (e.hasMoreElements()) {
//			Instance instance = e.nextElement();
//			sumWeights += instance.weight();
//		}
//		int result = (int) (get_minpoints_initial() + get_minpoints_perSampleWeight() * sumWeights);
//		return result > get_minpoints_max() ? get_minpoints_max() : result;
//	}
//	
//	private double updateRadiusForIteration(double prevRadius) {
//		return prevRadius * get_radius_changePerIteration();
//	}
//
//	private Clusterer createDBScanClusterer(double radius, int minPoints) {
//		MyDBScan dbs = new MyDBScan();
//		dbs.setEpsilon(radius);
//		dbs.setMinPoints(minPoints);
//		return dbs;
//	}
//
//	private HistoryCollection mergeByClusterer(
//			Clusterer c,
//			Map<History, Instance> instances,
//			HistoryCollection hc)
//			throws InterruptedException, CanceledException {
//		Map<Integer, History> clusters = new HashMap<Integer, History>();
//		int counter = 0;
//		for (History h : hc.getHistories()) {
//			ConcurrencyUtils.checkState();
//			int cluster;
//			Instance instance = instances.get(h);
//			if (instance == null) continue;
//			try {
//				cluster = c.clusterInstance(instance);
//				// alternatively, get most likely out of: c.distributionForInstance(instance);
//			} catch (Exception e) {
//				Logger.warn("unclussified instance encountered");
//				// Mark it as noise
//				cluster = -1;
//			}
//			if (clusters.containsKey(cluster)) {
//				History unionInto = clusters.get(cluster);
//				unionInto.mergeFrom(h, options.isMethodSimilarityUnionPartial());
//			} else {
//				History base = h.clone();
//				String clusterName = getClusterName(cluster, counter);
//				base.setTitle(clusterName);
//				counter++;
//				clusters.put(cluster, base);
//			}
//		}
//		currentNoiseCluster = clusters.get(-1);
//		HistoryCollection result = options.newHistoryCollection();
//		result.addAll(clusters.values());
//		return result;
//	}
//	
//	/**
//	 * Calculate the score from a history collection. Lower is better.
//	 * @param hc
//	 * @return Numeric (double) score.
//	 */
//	protected abstract double computeScore(HistoryCollection hc);
//
//	/**
//	 * @param cluster
//	 * @param counter A unique number per cluster, given for easily providing an index.
//	 * @return The name of the cluster.
//	 */
//	protected abstract String getClusterName(int cluster, int counter);
//
//	/**
//	 * 
//	 * @param is
//	 * @param hc
//	 * @return A mapping from a history to the instance created from it.
//	 * @throws InterruptedException
//	 * @throws CanceledException
//	 */
//	protected Map<History, Instance> fillInstances(Instances is, HistoryCollection hc)
//			throws InterruptedException, CanceledException {
//		Map<History, Instance> result = new IdentityHashMap<History, Instance>();
//		for (History h : hc.getHistories()) {
//			Instance instance = null;
//			instance = buildInstance(is, h);
//			is.add(instance);
//			result.put(h, instance);
//		}
//		return result;
//	}
//
//	/**
//	 * @return Instances for the clusterer.
//	 */
//	protected abstract Instances buildInstances();
//
//	/**
//	 * Create a single instance from a history.
//	 * @param is
//	 * @param h
//	 * @return A new instance created from <code>h</code>.
//	 * @throws InterruptedException
//	 * @throws CanceledException
//	 */
//	protected abstract Instance buildInstance(Instances is, History h) throws InterruptedException,
//			CanceledException;
//	
//	protected abstract double getGoodEnoughScoreMultiplier();
//	
//	protected abstract int getIterationLimit();
//	
//	// Minpoints determines how many samples are needed for something to be considered a real
//	// cluster, other than just noise.
//	// If you get too much noise, reduce it; if you get too much, increase it.
//	// It is calculated as a percentage of the number of samples (considering their weight).
//	
//	protected abstract double get_minpoints_perSampleWeight();
//
//	protected abstract int get_minpoints_initial();
//
//	protected abstract int get_minpoints_max();
//	
//	/**
//	 * Radius determines how close do samples have to be to be "dense" enough for a cluster.
//	 * If two samples were not clustered and you think they should have been clustered, increase
//	 * it. If two samples were clustered and you think they should not have been clustered,
//	 * decrease it.
//	 * @param is
//	 * @return
//	 */
//	protected abstract double calculateInitialRadius(Instances is);
//	
//	/**
//	 * How the radius changes in each iteration of the algorithm.
//	 * @return The radius change.
//	 */
//	protected abstract double get_radius_changePerIteration();
//	
//	
//	protected abstract String getTitle(History h);
//
//}
