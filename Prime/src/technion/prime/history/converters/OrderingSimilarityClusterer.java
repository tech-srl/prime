package technion.prime.history.converters;

import java.util.HashMap;
import java.util.Map;

import com.google.gdata.util.common.base.Pair;

import technion.prime.Options;
import technion.prime.dom.AppMethodRef;
import technion.prime.history.History;
import technion.prime.history.HistoryCollection;
import technion.prime.history.Ordering;
import technion.prime.utils.Logger.CanceledException;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instances;

public class OrderingSimilarityClusterer extends MethodSimilarityClusterer {
	
	private Map<Pair<AppMethodRef, AppMethodRef>, Integer> allPairs;

	public OrderingSimilarityClusterer(Options options) {
		super(options);
	}
	
	@Override
	public String getName() {
		return "similar ordering";
	}

	/*@Override
	public boolean initialize(HistoryCollection hc) throws InterruptedException, CanceledException {
		allPairs = getPairIndices(hc);
		if (allPairs == null || allPairs.isEmpty()) {
			return false;
		}
		return true;
	}*/
	
	@Override
	public HistoryCollection convert(HistoryCollection hc) throws InterruptedException, CanceledException {
		allPairs = getPairIndices(hc);
		if (allPairs == null || allPairs.isEmpty()) {
			return hc.clone();
		}
		return super.convert(hc);
	}
	
	/*private Map<Integer, AppMethodRef> getAllMethods(HistoryCollection hc) {
		Map<Integer, AppMethodRef> allMethods = new HashMap<Integer, AppMethodRef>();
		int count=0;
		for (History h : hc.getHistories()) {
			for (AppMethodRef m : h.getAllParticipatingMethods()) {
				if (!allMethods.values().contains(m)){
					allMethods.put(count++, m);
				}
			}
		}
		return allMethods;
	}*/
	
	private Map<Pair<AppMethodRef, AppMethodRef>, Integer> getPairIndices(HistoryCollection hc) {
		Map<Pair<AppMethodRef, AppMethodRef>, Integer> result = new HashMap<Pair<AppMethodRef, AppMethodRef>, Integer>();
		for (History h : hc.getHistories()) {
			for (Ordering o : h.getOrderings()) {
				if (o.first.isUnknown() || o.second.isUnknown()) // sharon: added here too
					continue; // Unknown edges do not count as actual methods.
				result.put(o, -1);
			}
		}
		if (result.isEmpty())
			return null;
		int i = 0;
		for (Map.Entry<Pair<AppMethodRef, AppMethodRef>, Integer> e : result.entrySet()) {
			e.setValue(i++);
		}
		return result;
	}

/*
	private Map<Pair<AppMethodRef, AppMethodRef>, Integer> getPairIndices(HistoryCollection hc) {
		Map<Pair<AppMethodRef, AppMethodRef>, Integer> result = new HashMap<Pair<AppMethodRef, AppMethodRef>, Integer>();
		Map<Integer, AppMethodRef> allMethods = getAllMethods(hc);				
		if (allMethods.isEmpty())
			return null;		
		
		Logger.log(String.format("Found %d methods...",
				allMethods.size()));
		// insert all pairs to result
		int i = 0;		
		for(int j=0; j< allMethods.size() && i <300; j++){
			for (int k=0; k <allMethods.size() && i <300; k++){ 
				Pair<AppMethodRef, AppMethodRef> ordering = new Pair<AppMethodRef, AppMethodRef>(allMethods.get(j), allMethods.get(k));
				result.put(ordering, i++);
			}
		}		
		Logger.log(String.format("Found %d pairs...",
				i));
		return result;
	}
*/
	
	/*@Override
	protected Instances buildInstances() {
		FastVector v = new FastVector();
		for (Map.Entry<Pair<AppMethodRef, AppMethodRef>, Integer> e : allPairs.entrySet()) {
			Attribute a = new Attribute(e.getKey().first.getSignature()+";"+e.getKey().second.getSignature(),
					e.getValue());
			Logger.log(String.format("Inserting attribute %s at index %d ...",
					a, e.getValue()));			
			v.insertElementAt(a, e.getValue());
		}
		return new Instances("before", v, 0);
	}*/
	
	@Override
	protected FastVector buildAttributes() {
		// initialize vector by all method attributes
		//FastVector v = super.buildAttributes();
		FastVector v = new FastVector();
		int base = v.size();
		for (Map.Entry<Pair<AppMethodRef, AppMethodRef>, Integer> e : allPairs.entrySet()) {
			Attribute a = new Attribute(e.getKey().first.getSignature()+";"+e.getKey().second.getSignature(),
					e.getValue()+base);
			v.insertElementAt(a, e.getValue()+base);
		}
		return v;
	}
	
	@Override
	protected double[] buildAttributesVector(Instances is, History h)
			throws InterruptedException, CanceledException {
		/*double[] method_attrs = super.buildAttributesVector(is, h);
		double[] attrs = new double[method_attrs.length + allPairs.size()];
		for (int i=0; i < method_attrs.length; i++) {
			attrs[i] = method_attrs[i];
		}*/
		double[] attrs = new double[allPairs.size()];
		for (Ordering o : h.getOrderings()) {
			if (o.first.isUnknown() || o.second.isUnknown())
				continue; // Unknown edges do not count as actual methods.
			if (!allPairs.containsKey(o)) {
				continue;
			}
			attrs[allPairs.get(o)] = 1;
		}
		return attrs;
	}

	/*@Override
	protected Instance buildInstance(Instances is, History h)
			throws InterruptedException, CanceledException {
		ConcurrencyUtils.checkState();
		double[] attrs = new double[allPairs.size()];
		for (Pair<AppMethodRef, AppMethodRef> p : h.getOrdering()) {
			if (p.first.isUnknown() || p.second.isUnknown())
				continue; // Unknown edges do not count as actual methods.
			if (!allPairs.containsKey(p)) {
				Logger.log(String.format("Ignoring pair %s...", p));
				continue;
			}
			attrs[allPairs.get(p)] = 1;
		}
		int numSamples = calcNumSamples(h);
		Instance result = new Instance(numSamples, attrs);
		result.setDataset(is);
		return result;
	}*/
	
	@Override
	protected String getClusterName(int cluster, int counter) {
		return String.format("ordering similarity #%d%s",
				counter, cluster == -1 ? " (noise)" : "");
	}
	
	@Override
	protected int get_minpoints_initial() {
		return 5;
	}
	
	@Override
	protected int get_minpoints_max() {
		return 50;
	}
	
	@Override
	protected double calculateInitialRadius(Instances is) {
		return Math.sqrt(super.calculateInitialRadius(is)) * 0.01;
	}
	
	@Override
	protected double get_radius_changePerIteration() {
		return super.get_radius_changePerIteration() * 1.25;
	}
	
	@Override
	protected String getTitle(History h) {
		// TODO Auto-generated method stub
		return super.getTitle(h);
	}

}
