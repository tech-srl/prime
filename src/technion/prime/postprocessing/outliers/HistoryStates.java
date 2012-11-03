package technion.prime.postprocessing.outliers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import technion.prime.dom.AppMethodRef;
import technion.prime.dom.AppType;
import technion.prime.history.History;
import technion.prime.history.HistoryCollection;

public class HistoryStates {
	private ArrayList<HistoryState> states;
	
	public HistoryStates(HistoryCollection hc, Set<AppType> extraTypes) {
		states = new ArrayList<HistoryState>(hc.getNumHistories());
		for (History h : hc.getHistories()) {
			states.add(new HistoryState(h, extraTypes));
		}
	}
	
	public double takeEdge(AppMethodRef m) {
		double chance = getChance(m);
		for (HistoryState hs : states) {
			hs.takeEdge(m);
		}
		return chance;
	}
	
	public Collection<Chance> getChances() {
		SortedSet<Chance> result = new TreeSet<Chance>();
		for (AppMethodRef m : getAllNextMethods()) {
			double methodWeight = 0;
			double totalWeight = 0;
			for (HistoryState hs : states) {
				methodWeight += hs.getMethodWeight(m);
				totalWeight += hs.getTotalWeight();
			}
			result.add(new Chance(m, totalWeight == 0 ? 0 : methodWeight / totalWeight));
		}
		return result;
	}
	
	public double getChance(AppMethodRef m) {
		double methodWeight = 0;
		double totalWeight = 0;
		for (HistoryState hs : states) {
			methodWeight += hs.getMethodWeight(m);
			totalWeight += hs.getTotalWeight();
		}
		if (totalWeight == 0) return 0;
		return methodWeight / totalWeight;
	}
	
	public Set<AppMethodRef> getAllNextMethods() {
		Set<AppMethodRef> result = new HashSet<AppMethodRef>();
		for (HistoryState hs : states) {
			result.addAll(hs.nextMethods());
		}
		return result;
	}
	
	public int numActiveHistories() {
		int result = 0;
		for (HistoryState hs : states) {
			if (hs.reachedDeadend() == false) result++;
		}
		return result;
	}

	public int numHistories() {
		return states.size();
	}
}
