package technion.prime.history.edgeset;

import java.util.HashSet;
import java.util.Set;

import technion.prime.dom.AppMethodRef;
import technion.prime.history.Node;

class EdgeSetNode implements Node {
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + h.hashCode();
		result = prime * result + n.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o instanceof EdgeSetNode == false) return false;
		EdgeSetNode node = (EdgeSetNode)o;
		return h.equals(node.h) && n.equals(node.n);
	}

	private final EdgeHistory h;
	private final EdgeNode n;
	
	public EdgeSetNode(EdgeHistory h, EdgeNode n) {
		this.h = h;
		this.n = n;
	}
	
	@Override
	public Set<AppMethodRef> getIncomingMethods() {
		Set<AppMethodRef> methods = new HashSet<AppMethodRef>();
		for (Edge e : h.getIngoingEdges(n)) {
			methods.addAll(e.getMethods());
		}
		return methods;
	}
	
	@Override
	public Set<AppMethodRef> getOutgoingMethods() {
		Set<AppMethodRef> methods = new HashSet<AppMethodRef>();
		for (Edge e : h.getOutgoingEdges(n)) {
			methods.addAll(e.getMethods());
		}
		return methods;
	}

	@Override
	public Set<Node> getPreviousNodes() {
		Set<Node> nodes = new HashSet<Node>();
		for (Edge e : h.getIngoingEdges(n)) {
			nodes.add(new EdgeSetNode(h, e.getFrom()));
		}
		return nodes;
	}
	
	@Override
	public String toString() {
		return n.toString();
	}
}
