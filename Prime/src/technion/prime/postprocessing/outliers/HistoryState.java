package technion.prime.postprocessing.outliers;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import technion.prime.dom.AppMethodRef;
import technion.prime.dom.AppType;
import technion.prime.history.History;
import technion.prime.history.edgeset.Edge;
import technion.prime.history.edgeset.EdgeHistory;
import technion.prime.history.edgeset.EdgeNode;

public class HistoryState {
	private EdgeHistory h;
	private Set<EdgeNode> current = new HashSet<EdgeNode>();
	private Map<Edge, Boolean> nonInterestingEdges = new HashMap<Edge, Boolean>();
	private Set<AppType> extraTypes;

	public HistoryState(History h, Set<AppType> extraTypes) {
		this.h = (EdgeHistory)h;
		this.extraTypes = extraTypes;
		current.add(this.h.getRoot());
	}

	public void takeEdge(AppMethodRef m) {
		Set<EdgeNode> newCurrent = new HashSet<EdgeNode>();
		for (EdgeNode n : current) {
			for (Edge e : h.getOutgoingEdges(n)) {
				if (matches(m, e)) newCurrent.add(e.getTo());
				if (shouldRemain(m, e)) newCurrent.add(n);
			}
		}
		current = newCurrent;
	}

	public double getMethodWeight(AppMethodRef m) {
		double weight = 0;
		for (EdgeNode n : current) {
			for (Edge e : h.getOutgoingEdges(n)) {
				if (matches(m, e)) weight += e.getWeight();
			}
		}
		return weight;
	}

	public double getTotalWeight() {
		double weight = 0;
		for (EdgeNode n : current) {
			for (Edge e : h.getOutgoingEdges(n)) {
				weight += e.getWeight();
			}
		}
		return weight;
	}

	public Collection<? extends AppMethodRef> nextMethods() {
		Set<AppMethodRef> result = new HashSet<AppMethodRef>();
		for (EdgeNode n : current) {
			for (Edge e : h.getOutgoingEdges(n)) {
				result.addAll(e.getMethods());
			}
		}
		return result;
	}
	
	public boolean reachedDeadend() {
		return current.isEmpty();
	}

	private boolean matches(AppMethodRef m, Edge e) {
		return alwaysMatch(m, e) || e.getMethods().contains(m);
	}
	
	private boolean shouldRemain(AppMethodRef m, Edge e) {
		return alwaysMatch(m, e);
	}
	
	private boolean alwaysMatch(AppMethodRef m, Edge e) {
		return m.isUnknown() || e.isUnknown() || isNonInterestingMethod(m) || isNonInterestingEdge(e);
	}
	
	private boolean isNonInterestingMethod(AppMethodRef m) {
		return m.isTransparent() &&
			(extraTypes == null || (extraTypes.contains(m.getContainingType()) == false));
	}
	
	private boolean isNonInterestingEdge(Edge e) {
		Boolean result = nonInterestingEdges.get(e);
		if (result == null) {
			result = calcIsNonInterestingEdge(e);
			nonInterestingEdges.put(e, result);
		}
		return result;
	}

	private boolean calcIsNonInterestingEdge(Edge e) {
		for (AppMethodRef m : e.getMethods()) {
			if (isNonInterestingMethod(m)) return true;
		}
		return false;
	}
	
}
