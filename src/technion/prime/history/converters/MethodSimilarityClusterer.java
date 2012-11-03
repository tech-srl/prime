package technion.prime.history.converters;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import technion.prime.dom.AppMethodRef;
import technion.prime.history.History;
import technion.prime.history.HistoryCollection;
import technion.prime.Options;
import technion.prime.utils.ConcurrencyUtils;
import technion.prime.utils.Logger.CanceledException;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;


public class MethodSimilarityClusterer extends SimilarityClusterer {
	private Map<AppMethodRef, Integer> allMethods;
	
	public MethodSimilarityClusterer(Options options) {
		super(options);
	}
	
	@Override
	public String getName() {
		return "similar methods";
	}

	
	@Override
	public HistoryCollection convert(HistoryCollection hc) throws InterruptedException, CanceledException {
		allMethods = getMethodIndices(hc);
		if (allMethods == null || allMethods.isEmpty()) {
			return hc.clone();
		}
		return super.convert(hc);
	}
	
	/*
	@Override
	protected boolean initialize(HistoryCollection hc) throws InterruptedException, CanceledException {
		allMethods = getMethodIndices(hc);
		if (allMethods == null || allMethods.isEmpty()) {
			return false;
		}
		return true;
	}*/
	
	@Override
	protected String getTitle(History h) {
		return h.getTitle() + ": " +  h.getAllParticipatingMethods().toString();
	};

	private double computeAverageDensity(HistoryCollection hc) {
		double totalDensity = 0;
		int numHistories = 0;
		for (History h : hc.getHistories()) {
			if (h == currentNoiseCluster) continue;
			if (h.containsOnlyRoot()) continue;
			double density = (double) h.getNumEdges() / (double) h.getNumNodes();
			totalDensity += density;
			numHistories++;
		}
		double avgDensity = totalDensity / numHistories;
		return avgDensity;
	}
	
	@SuppressWarnings("unused")
	private double computeAverageSize(HistoryCollection hc) {
		double totalSize = 0;
		int numHistories = 0;
		for (History h : hc.getHistories()) {
			if (h == currentNoiseCluster) continue;
			totalSize += h.getNumNodes();
			numHistories++;
		}
		return totalSize / numHistories;
	}
	
	@SuppressWarnings("unused")
	private double computeAverageEdgeCount(HistoryCollection hc) {
		double totalSize = 0;
		int numHistories = 0;
		for (History h : hc.getHistories()) {
			if (h == currentNoiseCluster) continue;
			totalSize += h.getNumEdges();
			numHistories++;
		}
		return totalSize / numHistories;
	}
	
	@SuppressWarnings("unused")
	private double computeMedianSize(HistoryCollection hc) {
		List<Integer> sizes = getAllSizes(hc);
		if (sizes.isEmpty()) return 0;
		Collections.sort(sizes);
		return sizes.get(sizes.size() / 2);
	}
	
	private List<Integer> getAllSizes(HistoryCollection hc) {
		LinkedList<Integer> result = new LinkedList<Integer>();
		for (History h : hc.getHistories()) {
			if (h == currentNoiseCluster) continue;
			result.add(h.getNumNodes());
		}
		return result;
	}

	@Override
	protected double computeScore(HistoryCollection hc) {
		return computeAverageDensity(hc);
	}

	@Override
	protected String getClusterName(int cluster, int counter) {
		return String.format("method similarity #%d%s",
				counter, cluster == -1 ? " (noise)" : "");
	}

	protected int calcNumSamples(History h) {
		int sum = 1;
		for (History source : h.getSources()) {
			sum += calcNumSamples(source);
		}
		return sum;
	}

	@Override
	protected Instances buildInstances() {
		/*FastVector v = new FastVector();
		for (Map.Entry<AppMethodRef, Integer> e : allMethods.entrySet()) {
			Attribute a = new Attribute(e.getKey().getSignature(),
					e.getValue());
			v.insertElementAt(a, e.getValue());
		}
		return new Instances("before", v, 0);*/
		return new Instances("before", buildAttributes(), 0);
	}
	
	protected FastVector buildAttributes(){
		FastVector v = new FastVector();
		for (Map.Entry<AppMethodRef, Integer> e : allMethods.entrySet()) {
			Attribute a = new Attribute(e.getKey().getSignature(),
					e.getValue());
			v.insertElementAt(a, e.getValue());
		}
		return v;
	}

	private Map<AppMethodRef, Integer> getMethodIndices(HistoryCollection hc) {
		Map<AppMethodRef, Integer> result = new HashMap<AppMethodRef, Integer>();
		for (History h : hc.getHistories()) {
			for (AppMethodRef m : h.getAllParticipatingMethods()) {
				result.put(m, -1);
			}
		}
		if (result.isEmpty())
			return null;
		int i = 0;
		for (Map.Entry<AppMethodRef, Integer> e : result.entrySet()) {
			e.setValue(i++);
		}
		return result;
	}

	@Override
	protected Instance buildInstance(Instances is, History h)
			throws InterruptedException, CanceledException {
		ConcurrencyUtils.checkState();
		/*double[] attrs = new double[allMethods.size()];
		for (AppMethodRef m : h.getAllParticipatingMethods()) {
			if (m.isUnknown())
				continue; // Unknown edges do not count as actual methods.
			attrs[allMethods.get(m)] = 1;
		}*/
		double[] attrs = buildAttributesVector(is, h);
		int numSamples = calcNumSamples(h);
		Instance result = new Instance(numSamples, attrs);
		result.setDataset(is);
		return result;
	}
	
	protected double[] buildAttributesVector(Instances is, History h)
			throws InterruptedException, CanceledException {
		double[] attrs = new double[allMethods.size()];
		for (AppMethodRef m : h.getAllParticipatingMethods()) {
			if (m.isUnknown())
				continue; // Unknown edges do not count as actual methods.
			attrs[allMethods.get(m)] = 1;
		}
		return attrs;
	}
	
	@Override
	protected double getGoodEnoughScoreMultiplier() {
		return 0.75;
	}

	@Override
	protected int getIterationLimit() {
		return 15;
	}

	@Override
	protected double get_minpoints_perSampleWeight() {
		return 0.03;
	}

	@Override
	protected int get_minpoints_initial() {
		return 1;
	}

	@Override
	protected int get_minpoints_max() {
		return 10;
	}

	@Override
	protected double get_radius_changePerIteration() {
		return 0.75;
	}

	@Override
	protected double calculateInitialRadius(Instances is) {
		return 1 + 0.01 * is.numAttributes();
	}

}
