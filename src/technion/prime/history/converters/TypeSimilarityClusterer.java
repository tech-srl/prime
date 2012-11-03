package technion.prime.history.converters;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import technion.prime.Options;
import technion.prime.dom.AppType;
import technion.prime.dom.UnknownAppType;
import technion.prime.history.History;
import technion.prime.history.HistoryCollection;
import technion.prime.utils.ConcurrencyUtils;
import technion.prime.utils.Logger.CanceledException;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class TypeSimilarityClusterer extends SimilarityClusterer {

	private Map<AppType, Integer> allTypes;

	public TypeSimilarityClusterer(Options options) {
		super(options);
	}
	
	@Override
	public HistoryCollection convert(HistoryCollection hc) throws InterruptedException, CanceledException {
		allTypes = getTypeIndices(hc);
		if (allTypes == null || allTypes.isEmpty()) {
			return hc.clone();
		}
		return super.convert(hc);
	}
	
	private Set<AppType> getTypes(History h) {
		return h.getAllParticipatingApiTypes();
	}
	
	private Map<AppType, Integer> getTypeIndices(HistoryCollection hc) {
		Map<AppType, Integer> result = new HashMap<AppType, Integer>();
		for (History h : hc.getHistories()) {
			for (AppType t : getTypes(h)) {
				result.put(t, -1);
			}
		}
		if (result.isEmpty())
			return null;
		int i = 0;
		for (Map.Entry<AppType, Integer> e : result.entrySet()) {
			e.setValue(i++);
		}
		return result;
	}

	@Override
	public String getName() {
		return "type similarity";
	}

	@Override
	protected double computeScore(HistoryCollection hc) {
		return computeAverageDensity(hc);
	}
	
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

	@Override
	protected String getClusterName(int cluster, int counter) {
		return String.format("type similarity #%d%s",
				counter, cluster == -1 ? " (noise)" : "");
	}

	@Override
	protected Instances buildInstances() {
		FastVector v = new FastVector();
		for (Map.Entry<AppType, Integer> e : allTypes.entrySet()) {
			Attribute a = new Attribute(e.getKey().getFullName(), e.getValue());
			v.insertElementAt(a, e.getValue());
		}
		return new Instances("before", v, 0);
	}

	@Override
	protected Instance buildInstance(Instances is, History h)
			throws InterruptedException, CanceledException {
		ConcurrencyUtils.checkState();
		double[] attrs = new double[allTypes.size()];
		for (AppType t : getTypes(h)) {
			if (t instanceof UnknownAppType)
				continue; // Unknown types do not count as actual types.
			attrs[allTypes.get(t)] = 1;
		}
		int numSamples = calcNumSamples(h);
		Instance result = new Instance(numSamples, attrs);
		result.setDataset(is);
		return result;
	}
	
	private int calcNumSamples(History h) {
		int sum = 1;
		for (History source : h.getSources()) {
			sum += calcNumSamples(source);
		}
		return sum;
	}

	@Override
	protected double getGoodEnoughScoreMultiplier() {
		return 0.75;
	}

	@Override
	protected int getIterationLimit() {
		return 5;
	}

	@Override
	protected double get_minpoints_perSampleWeight() {
		return 0;
	}

	@Override
	protected int get_minpoints_initial() {
		return 1;
	}

	@Override
	protected int get_minpoints_max() {
		return 1;
	}

	@Override
	protected double get_radius_changePerIteration() {
		return 0.75;
	}
	
	@Override
	protected double calculateInitialRadius(Instances is) {
		return 1.5;
	}

	@Override
	protected String getTitle(History h) {
		return h.getTitle() + ": " +  getTypes(h).toString();
	}

}
