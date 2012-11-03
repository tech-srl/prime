package technion.prime.history.edgeset;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import technion.prime.Options;
import technion.prime.dom.AppMethodRef;
import technion.prime.dom.AppType;
import technion.prime.history.History;
import technion.prime.history.HistoryCollection;
import technion.prime.postprocessing.outliers.HistoryStates;
import technion.prime.utils.Logger;
import technion.prime.utils.Logger.CanceledException;

public class EdgeSequence implements Iterable<Edge> {
	private LinkedList<Edge> data = new LinkedList<Edge>();
	private double chance;
	private Set<AppType> extraTypes;
	
	public EdgeSequence() {}
	
	public EdgeSequence(Edge e, Set<AppType> extraTypes) {
		this.extraTypes = extraTypes;
		addFirst(e);
	}
	
	public EdgeSequence(LinkedList<Edge> edges, Set<AppType> extraTypes) {
		this.extraTypes = extraTypes;
		data.addAll(edges);
	}

	public void addFirst(Edge e) {
		data.addFirst(e);
	}
	
	public void addLast(Edge e) {
		data.addLast(e);
	}
	
	@Override
	public Iterator<Edge> iterator() {
		return data.iterator();
	}
	
	public double getChance() {
		return chance;
	}
	
	public void calcChanceAgainst(HistoryCollection base) {
		HistoryStates hs = new HistoryStates(base, extraTypes);
		chance = 1;
		for (Edge e : data) {
			chance *= hs.takeEdge(e.getMethods().iterator().next());
		}
	}

	public boolean containsAll(Collection<Edge> edges) {
		return data.containsAll(edges);
	}
	
	@Override
	public String toString() {
		return data.toString();
	}
	
	public boolean sameMethods(EdgeSequence s) {
		if (data.size() != s.data.size()) return false;
		for (int i = 0; i < data.size(); i++) {
			if (data.get(i).getMethods().equals(s.data.get(i).getMethods()) == false) {
				return false;
			}
		}
		return true;
	}

	public History createHistory(Options options) {
		EdgeHistory temp = (EdgeHistory)options.newHistory();
		for (Edge e : data) {
			temp.addNode(e.getTo());
			temp.addEdge(e);
		}
		EdgeHistory result = (EdgeHistory)options.newHistory();
		try {
			result.mergeEdgesFrom(temp, data, false, false);
		} catch (InterruptedException e) {
			Logger.exception(e);
		} catch (CanceledException e1) {
			return null;
		}
		return result;
	}

	public Set<AppMethodRef> getAllParticipatingMethods() {
		Set<AppMethodRef> result = new HashSet<AppMethodRef>();
		for (Edge e : data) result.addAll(e.getMethods());
		return result;
	}
}