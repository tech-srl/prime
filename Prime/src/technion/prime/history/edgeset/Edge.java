package technion.prime.history.edgeset;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import technion.prime.dom.AppMethodRef;
import technion.prime.utils.StringUtils;

/**
 * Immutable edge in EdgeHistory.
 * Notice that a single edge may be shared between different histories.
 */
public class Edge implements Cloneable, Serializable {
	private static final long serialVersionUID = 6705447602212958205L;
	
	private final EdgeNode from;
	private final EdgeNode to;
	private final Set<AppMethodRef> methods;
	private final double weight;
	private transient Boolean isUnknown;
	private transient Boolean isInit;
	private transient Integer hash;
	
	public Edge(EdgeNode from, EdgeNode to, Set<AppMethodRef> methods, double weight) {
		this.from = from;
		this.to = to;
		this.methods = methods;
		this.weight = weight;
	}
		
	private int calculateHash() {
		final int prime = 31;
		int result = 1;
		result = prime * result + from.hashCode();
		result = prime * result + methods.hashCode();
		result = prime * result + to.hashCode();
		result = prime * result + (int)weight;
		return result;
	}

	public Edge(EdgeNode from, EdgeNode to, AppMethodRef method, double weight) {
		this(from, to, new HashSet<AppMethodRef>(), weight);
		this.methods.add(method);
	}

	public EdgeNode getTo() {
		return to;
	}
	
	public EdgeNode getFrom() {
		return from;
	}
	
	public double getWeight() {
		return weight;
	}
	
	@Override
	protected Edge clone() {
		try {
			return (Edge)super.clone();
		} catch (CloneNotSupportedException e) {
			assert(false); // Edge should implement Cloneable
			return null;
		}
	}

	public Set<AppMethodRef> getMethods() {
		return methods;
	}

	@Override
	public int hashCode() {
		if (hash == null) {
			hash = calculateHash();
		}
		return hash;
	}

	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o instanceof Edge == false) return false;
		if (hashCode() != o.hashCode()) return false;
		Edge e = (Edge)o;
		return from.equals(e.from) &&
				to.equals(e.to) &&
				methods.equals(e.methods) &&
				weight == e.weight;
	}
	
	public boolean equalContent(Edge e) {
		return methods.equals(e.methods) &&
				weight == e.weight;
	}
	
	@Override
	public String toString() {
		return from.toString() + "--" +
			methods.iterator().next().getShortName() +
			"*" + StringUtils.prettyPrintNumber(weight) +
			"->" + to.toString();
	}

	public boolean isUnknown() {
		if (isUnknown == null) {
			isUnknown = false;
			for (AppMethodRef m : methods) {
				if (m.isUnknown()) {
					isUnknown = true;
					break;
				}
			}
		}
		return isUnknown;
	}

	public boolean isConstructor() {
		if (isInit == null) {
			for (AppMethodRef m : methods) {
				if (m.isInit()) {
					isInit = true;
					break;
				}
			}
			isInit = false;
		}
		return isInit;
	}
	
}
