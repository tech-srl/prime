package technion.prime.history.edgeset;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import net.jimblackler.Utils.CollectionAbortedException;
import net.jimblackler.Utils.Collector;
import net.jimblackler.Utils.ResultHandler;
import net.jimblackler.Utils.ThreadedYieldAdapter;

import org.apache.commons.io.FileUtils;

import technion.prime.utils.MultiMap;
import technion.prime.Options;
import technion.prime.dom.AppMethodRef;
import technion.prime.dom.AppType;
import technion.prime.dom.UnknownMethod;
import technion.prime.history.ExteriorMatcher;
import technion.prime.history.FutureMatcher;
import technion.prime.history.History;
import technion.prime.history.HistoryCollection;
import technion.prime.history.Ordering;
import technion.prime.utils.ConcurrencyUtils;
import technion.prime.utils.DocNode;
import technion.prime.utils.Logger;
import technion.prime.utils.Logger.CanceledException;
import technion.prime.utils.StringUtils;


public class EdgeHistory implements History {
	private static final long serialVersionUID = 1L;

	private static final String GRAPHVIZ_SUFFIX = ".dot";

	protected static final double SEQUENCE_EDGE_THRESHOLD = 0.1;
	protected static final int SEQUENCE_LENGTH_THRESHOLD = 10;

	private transient Options options;

	private HashSet<EdgeHistory> sources = new HashSet<EdgeHistory>();
	private HashSet<EdgeNode> active;
	private HashSet<Edge> edges;
	private HashSet<EdgeNode> nodes;
	private EdgeNode root;
	private MultiMap<EdgeNode, Edge> ingoing;
	private MultiMap<EdgeNode, Edge> outgoing;
	private String title = "";

	private transient Integer contentHash;
	private transient int underMutation;
	private transient EdgeHistory weightlessClone;

	private void invariant() {
		if (containsOnlyRoot()) {
			makeSure(nodes.contains(root));
			makeSure(nodes.size() == 1);
			makeSure(edges.isEmpty());
			makeSure(active.contains(root));
			makeSure(ingoing.isEmpty());
			makeSure(outgoing.isEmpty());
			return;
		}

		// Make sure all edge nodes appear in the node set
		Set<EdgeNode> seenNodes = new HashSet<EdgeNode>();
		for (Edge e : edges) {
			seenNodes.add(e.getFrom());
			seenNodes.add(e.getTo());
		}
		makeSure(seenNodes.equals(nodes));

		// Make sure all nodes appear as an edge node of some edge
		for (EdgeNode n : nodes) {
			boolean found = false;
			for (Edge e : edges) {
				if (n.equals(e.getTo())) {
					found = true;
					break;
				}
				if (n.equals(e.getFrom())) {
					found = true;
					break;
				}
			}
			makeSure(found);
		}

		// Make sure the root appears in the nodes
		makeSure(nodes.contains(root));

		// Make sure all the active appear in the nodes
		for (EdgeNode n : active) {
			makeSure(nodes.contains(n));
		}

		// Make sure all nodes have a correct incoming and outgoing set
		for (EdgeNode n : nodes) {
			Set<Edge> seenIncoming = new HashSet<Edge>();
			Set<Edge> seenOutgoing = new HashSet<Edge>();
			for (Edge e : edges) {
				if (e.getTo().equals(n)) seenIncoming.add(e);
				if (e.getFrom().equals(n)) seenOutgoing.add(e);
			}
			makeSure(getIngoingEdges(n).equals(seenIncoming));
			makeSure(getOutgoingEdges(n).equals(seenOutgoing));
		}

		// Make sure the root, and only the root, has no incoming edges
		for (EdgeNode n : nodes) {
			if (n == root) makeSure(ingoing.containsKey(n) == false);
			else makeSure(ingoing.containsKey(n));
		}

		// Make sure there are no two consecutive unknowns
		for (Edge e1 : edges) {
			for (Edge e2 : edges) {
				if (e1 == e2) continue;
				if (e1.getTo().equals(e2.getFrom()) &&
						e1.isUnknown() && e2.isUnknown()) {
					makeSure(false);
				}
			}
		}

		// Make sure there are no unknown self loops
		for (Edge e : edges) {
			if (e.getTo().equals(e.getFrom()) &&
					e.isUnknown()) {
				makeSure(false);
			}
		}

		// Make sure there are no loops on the root
		for (Edge e : edges) {
			if (e.getFrom().equals(root)) {
				makeSure(e.getTo().equals(root) == false);
			}
		}

		// Make sure no edge contains both a regular method and an unknown method
		for (Edge e : edges) {
			boolean seenUnknown = false;
			boolean seenKnown = false;
			for (AppMethodRef m : e.getMethods()) {
				boolean unknown = m.isUnknown();
				seenUnknown |= unknown;
				seenKnown |= !unknown;
				if (seenUnknown && seenKnown) makeSure(false);
			}
		}

		makeSure(notSource(this));
	}

	private boolean notSource(History h) {
		if (sources.contains(h)) return false;
		for (EdgeHistory source : sources) {
			if (source.notSource(h) == false) return false;
		}
		return true;
	}

	private void makeSure(boolean b) {
		if (b == false) {
			Logger.warn("history invariant failed");
			throw new IllegalStateException();
		}
	}

	/**
	 * Run this at the start of every method which modifies this instance.
	 */
	private void preUpdate() {
		underMutation++;
		if (underMutation > 1) return;
		contentHash = null;
		weightlessClone = null;
	}

	/**
	 * Run this at the end of every method which modifies this instance.
	 */
	private void postUpdate() {
		underMutation--;
		if (underMutation > 0) return;
		if (options.useHistoryInvariant()) invariant();
	}

	public EdgeHistory(Options options) {
		this.options = options;
		preUpdate();
		initialize();
		createRoot();
		postUpdate();
	}

	/**
	 * Assuming this history is empty, create a new root node.
	 */
	private void createRoot() {
		preUpdate();
		root = new EdgeNode();
		addNode(root);
		addActive(root);
		postUpdate();
	}

	private void initialize() {
		preUpdate();
		edges = new HashSet<Edge>();
		nodes = new HashSet<EdgeNode>();
		active = new HashSet<EdgeNode>();
		ingoing = new MultiMap<EdgeNode, Edge>();
		outgoing = new MultiMap<EdgeNode, Edge>();
		postUpdate();
	}

	@Override
	public boolean containsOnlyRoot() {
		return edges.isEmpty() && nodes.contains(root) && nodes.size() == 1;
	}

	void addEdge(Edge e) {
		edges.add(e);
		ingoing.put(e.getTo(), e);
		outgoing.put(e.getFrom(), e);
	}

	private void removeEdge(Edge e) {
		edges.remove(e);
		ingoing.removeValue(e.getTo(), e);
		outgoing.removeValue(e.getFrom(), e);
	}

	void addNode(EdgeNode n) {
		nodes.add(n);
	}

	private void removeNode(EdgeNode n) {
		preUpdate();
		nodes.remove(n);
		ingoing.removeKey(n);
		outgoing.removeKey(n);
		postUpdate();
	}

	private void addActive(EdgeNode n) {
		preUpdate();
		active.add(n);
		postUpdate();
	}

	private void clearActive() {
		preUpdate();
		active.clear();
		postUpdate();
	}

	@Override
	public boolean equalContent(History h) throws InterruptedException, CanceledException {
		// We do not check equality of active set.

		if (this == h) return true;
		if (h instanceof EdgeHistory == false) return false;
		EdgeHistory eh = (EdgeHistory) h;
		if (contentHash() != eh.contentHash()) return false;

		Set<Edge> found = new HashSet<Edge>();
		for (Edge this_edge : edges) {
			EdgeNode other_from = eh.findMatching(this_edge.getFrom(), this);
			if (other_from == null) return false;
			EdgeNode other_to = eh.findMatching(this_edge.getTo(), this);
			if (other_to == null) return false;
			Edge other_edge = eh.findEdge(other_from, other_to);
			if (other_edge == null) return false;
			found.add(other_edge);
			if (this_edge.equalContent(other_edge) == false) return false;
		}

		return found.containsAll(eh.edges);

	}

	@Override
	public int contentHash() {
		if (contentHash == null) {
			contentHash = 31;
			contentHash += 31 * edges.size();
			contentHash += 31 * getAllParticipatingMethods().hashCode();
		}
		return contentHash;
	}

	/**
	 * Attempts a downcast from a History into an EdgeHistory; throws IllegalArgumentException
	 * instead of ClassCastException if failing.
	 * 
	 * @param h
	 * @return (EdgeHistory)h
	 */
	private EdgeHistory downcast(History h) {
		if (h instanceof EdgeHistory == false) throw new IllegalArgumentException();
		return (EdgeHistory) h;
	}

	@Override
	public void joinFrom(History h) throws InterruptedException, CanceledException {
		if (this.equals(h)) return;
		if (h.containsOnlyRoot()) return;
		EdgeHistory eh = downcast(h);
		Map<EdgeNode, EdgeNode> map = mergeEdgesFrom(eh, eh.edges, false, false);
		mergeActiveFrom(map, eh);
	}

	@Override
	public void mergeFrom(History h, boolean inclusion) throws InterruptedException,
			CanceledException {
		if (h == this) return;
		if (h.containsOnlyRoot()) return;
		EdgeHistory eh = downcast(h);
		Map<EdgeNode, EdgeNode> map = mergeEdgesFrom(eh, eh.edges, true, inclusion);
		mergeActiveFrom(map, eh);
		sources.add(eh);
	}

	/**
	 * Given two edges, creates a new edge which is a combination of the two, between the specified
	 * nodes. The method group is the union of both edges' method groups, and the weight is either
	 * the sum (if union is true) or the maximum (if union is false) of the weights.
	 * 
	 * @param from
	 *            From node of the new edge.
	 * @param to
	 *            To node of the new edge.
	 * @param e1
	 *            Source edge 1.
	 * @param e2
	 *            Source edge 2.
	 * @param union
	 *            If true, the new weight will be the sum of both weights; otherwise, it will be the
	 *            maximal one among them.
	 * @return The newly-created edge.
	 */
	private Edge edgeMerge(EdgeNode from, EdgeNode to, Edge e1, Edge e2, boolean union) {
		Set<AppMethodRef> methods = new HashSet<AppMethodRef>();
		methods.addAll(e1.getMethods());
		methods.addAll(e2.getMethods());
		double w1 = e1.getWeight();
		double w2 = e2.getWeight();
		double weight = union ? w1 + w2 : Math.max(w1, w2);
		return new Edge(from, to, methods, weight);
	}

	/**
	 * Finds an edge between from and to. There will only be one such edge.
	 * 
	 * @param from
	 *            From node.
	 * @param to
	 *            To node.
	 * @return The edge between the nodes, or null if none exists.
	 */
	protected Edge findEdge(EdgeNode from, EdgeNode to) {
		for (Edge e : getIngoingEdges(to)) {
			if (e.getFrom().equals(from)) return e;
		}
		return null;
	}

	/**
	 * @param m
	 *            A mapping from nodes in <code>otherHistory</code> to nodes in this history.
	 * @param n
	 *            A node in <code>otherHistory</code>.
	 * @param otherHistory
	 *            The history to which <code>n</code> belongs.
	 * @return A node in this history matching <code>n</code>, if one was found, or a new node if
	 *         none was found; in which case <code>m</code> is updated to map <code>n</code> to this
	 *         new node.
	 * @throws CanceledException
	 * @throws InterruptedException
	 */
	private EdgeNode findMatchingOrCreate(Map<EdgeNode, EdgeNode> m, EdgeNode n,
			EdgeHistory otherHistory) throws InterruptedException, CanceledException {
		if (m.containsKey(n)) return m.get(n);
		EdgeNode existingNode = findMatching(n, otherHistory);
		if (existingNode == null) {
			existingNode = new EdgeNode();
			addNode(existingNode);
		}
		m.put(n, existingNode);
		return existingNode;
	}

	/**
	 * @param n
	 *            A node in <code>otherHistory</code>
	 * @param otherHistory
	 *            Another history.
	 * @return A node in this history matching <code>n</code> if one exists; null otherwise.
	 * @throws CanceledException
	 * @throws InterruptedException
	 */
	private EdgeNode findMatching(EdgeNode n, EdgeHistory otherHistory)
			throws InterruptedException, CanceledException {
		if (otherHistory.getIngoingEdges(n).isEmpty()) return root;
		for (EdgeNode existingNode : nodes) {
			ConcurrencyUtils.checkState();
			if (matching(this, existingNode, otherHistory, n)) return existingNode;
		}
		return null;
	}

	/**
	 * Check if two nodes match, according to the matcher in use.
	 * 
	 * @param h1
	 *            History containing <code>n1</code>.
	 * @param n1
	 *            Node 1.
	 * @param h2
	 *            History containing <code>n2</code>.
	 * @param n2
	 *            Node 2.
	 * @return True if both nodes match.
	 */
	private boolean matching(EdgeHistory h1, EdgeNode n1, EdgeHistory h2, EdgeNode n2) {
		return options.getMatcher().matches(new EdgeSetNode(h1, n1), new EdgeSetNode(h2, n2));
	}

	/**
	 * Check if two nodes match, according to a FutureMatcher.
	 * 
	 * @param h1
	 *            History containing <code>n1</code>.
	 * @param n1
	 *            Node 1.
	 * @param h2
	 *            History containing <code>n2</code>.
	 * @param n2
	 *            Node 2.
	 * @return True if both nodes match.
	 */
	private boolean futureMatching(EdgeHistory h1, EdgeNode n1, EdgeHistory h2, EdgeNode n2) {
		FutureMatcher matcher = new FutureMatcher(1);
		return matcher.matches(new EdgeSetNode(h1, n1), new EdgeSetNode(h2, n2));
	}

	/**
	 * Merge the active set from another history into this history.
	 * 
	 * @param m
	 *            A mapping between nodes in <code>h</code> and nodes in this history.
	 * @param h
	 *            The other history.
	 */
	private void mergeActiveFrom(Map<EdgeNode, EdgeNode> m, EdgeHistory h) {
		for (EdgeNode n : h.active) {
			addActive(m.get(n));
		}
	}

	/**
	 * Register a method call by unconditionally adding a new edge to a node node per each active
	 * node, without checking matches against existing nodes. This does not modify the active node
	 * set.
	 * 
	 * @param m
	 *            The method to add an edge for.
	 * @param weight
	 *            The weight of the new edge to create.
	 * @return All the edges that were created this way. The size of this collection should be the
	 *         same as the size of the active node set.
	 */
	private Set<Edge> addMethodCallNoMerge(AppMethodRef m, double weight) {
		Set<Edge> result = new HashSet<Edge>();
		for (EdgeNode n : active) {
			EdgeNode newNode = new EdgeNode();
			addNode(newNode);
			Edge newEdge = new Edge(n, newNode, m, weight);
			addEdge(newEdge);

			result.add(newEdge);
		}
		return result;
	}

	// public void concatenate(History h) {
	// if (h instanceof EdgeHistory == false) throw new IllegalArgumentException();
	// EdgeHistory eh = (EdgeHistory)h;
	// EdgeHistory extended = clone();
	// for (Edge e : eh.getOutgoingEdges(eh.root)) {
	// for (EdgeNode a : active) {
	// Edge newEdge = new Edge(a, e.getTo(), e.getMethods(), e.getWeight());
	// extended.addEdge(newEdge);
	// }
	//
	// }
	// }

	@Override
	public void extendWithMethodCall(AppMethodRef m, double weight) throws InterruptedException,
			CanceledException {
		preUpdate();
		// First, create a new identical history with a new node connected
		// from all the active nodes.
		EdgeHistory h = clone();
		Set<Edge> newEdges = h.addMethodCallNoMerge(m, weight);

		// Then merge it into this history. Union is true because we are extending
		// the current history with a new operation, not merging existing histories.
		// BUT setting it to false meanwhile, fearing that we need to limit
		// the weights to 1 during analysis, for fixed-point.
		Map<EdgeNode, EdgeNode> map = mergeEdgesFrom(h, newEdges, false, false);

		// Finally, set the active to only include the latest nodes
		clearActive();
		for (Edge e : newEdges) {
			addActive(map.get(e.getTo()));
		}
		postUpdate();
	}

	protected Map<EdgeNode, EdgeNode> mergeEdgesFrom(
			EdgeHistory h,
			Iterable<Edge> newEdges,
			boolean union,
			boolean inclusion) throws InterruptedException, CanceledException {
		return mergeEdgesFrom(h, newEdges, union, inclusion, null);
	}

	/**
	 * Merge edges from another history into this history, creating new nodes as necessary.
	 * 
	 * @param h
	 *            The other history.
	 * @param newEdges
	 *            The new edges, already existing in the other history, which we want to merge into
	 *            this history. May contain all the edges in the other history.
	 * @param union
	 *            In case an edge is already contained in this history, if union is true its weight
	 *            will be increased by the weight of the matching edge; otherwise, its weight will
	 *            be set to be the maximum between the weights.
	 * @param inclusion
	 *            False if the other history's root should only be matched to this history's root;
	 *            true if it can be matched with any node.
	 * @return A mapping from edges in <code>h</code> to edges in this history.
	 * @throws InterruptedException
	 * @throws CanceledException
	 */
	protected Map<EdgeNode, EdgeNode> mergeEdgesFrom(
			EdgeHistory h,
			Iterable<Edge> newEdges,
			boolean union,
			boolean inclusion,
			Set<Edge> onlyUpdate) throws InterruptedException, CanceledException {
		// A mapping from nodes in h to nodes in this history.
		Map<EdgeNode, EdgeNode> map = new HashMap<EdgeNode, EdgeNode>();
		for (Edge e : newEdges) {
			ConcurrencyUtils.checkState();
			EdgeNode from = null;
			if (inclusion && e.getFrom() == h.getRoot()) {
				// The root is matched by outgoing, rather than ingoing, edges
				from = findFutureMatching(h, e.getFrom());
				if (from != null) map.put(e.getFrom(), from);
			}
			if (from == null) {
				from = findMatchingOrCreate(map, e.getFrom(), h);
			}
			EdgeNode to = findMatchingOrCreate(map, e.getTo(), h);
			if (from.equals(to) && e.isUnknown()) {
				// We do not allow unknown loops.
			} else {
				updateEdgeFromOtherEdge(from, to, e, union, onlyUpdate);
			}
		}
		return map;
	}

	/**
	 * @param h
	 *            Another history containing <code>n</code>
	 * @param n
	 *            A node belonging to <code>h</code>
	 * @return The first node whose immediate future matches <code>n</code>'s future, or null if no
	 *         such node exists.
	 */
	private EdgeNode findFutureMatching(EdgeHistory h, EdgeNode n) {
		for (EdgeNode existingNode : nodes) {
			if (futureMatching(this, existingNode, h, n)) return existingNode;
		}
		return null;
	}

	/**
	 * If an edge from <code>from</code> to <code>to</code> exists, update it with data from
	 * <code>otherEdge</code>; otherwise create a new edge from <code>from</code> to <code>to</code>
	 * with data derived from <code>otherEdge</code>
	 * 
	 * @param from
	 *            The from node.
	 * @param to
	 *            The to node.
	 * @param otherEdge
	 *            The other edge to get data from.
	 * @param union
	 *            If true, weight from both edges will be summed; otherwise, the maximum weight will
	 *            be chosen.
	 * @param only
	 *            If not null, an update will only happen if the existing edge belongs to this set.
	 */
	protected void updateEdgeFromOtherEdge(EdgeNode from, EdgeNode to, Edge otherEdge,
			boolean union, Set<Edge> only) {
		Edge existingEdge = findEdge(from, to);
		if (existingEdge != null) {
			if (only != null && only.contains(existingEdge) == false) return;
			removeEdge(existingEdge);
			addEdge(edgeMerge(from, to, existingEdge, otherEdge, union));
		} else {
			addEdge(new Edge(from, to, otherEdge.getMethods(), otherEdge.getWeight()));
		}
	}

	@Override
	public EdgeHistory clone() {
		EdgeHistory h = null;
		try {
			h = (EdgeHistory) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
		h.copyFrom(this);
		return h;
	}

	protected void copyFrom(EdgeHistory h) {
		edges = new HashSet<Edge>(h.edges);
		nodes = new HashSet<EdgeNode>(h.nodes);
		active = new HashSet<EdgeNode>(h.active);
		ingoing = h.ingoing.clone();
		outgoing = h.outgoing.clone();
		sources = new HashSet<EdgeHistory>();
		sources.add(h);
		root = h.root;
	}

	@Override
	public String toString() {
		Set<AppType> types = getAllParticipatingTypes();
		return String.format("History[#nodes=%d,#types=%d,first_type=%s]",
				getNumNodes(),
				types.size(),
				types.isEmpty() ? "N/A" : types.iterator().next());
	}

	@Override
	public int getNumNodes() {
		return nodes.size();
	}

	@Override
	public void filterLowWeightEdges(double threshold) {
		preUpdate();

		Set<Edge> lowWeightEdges = getLowWeightEdges(threshold);

		Map<EdgeNode, Double> weightRemovedPerNode = removeEdges(lowWeightEdges);

		// Reconnect disconnected segments
		Set<EdgeNode> noIncoming = getNodesWithNoIncoming();
		removeOrReconnectDisconnectedNodes(noIncoming, weightRemovedPerNode);

		postUpdate();
	}

	/**
	 * For each node in <code>noIncoming</code>, if it has no outgoing edges remove it; otherwise
	 * comment root to it with an unknown method.
	 * 
	 * @param noIncoming
	 *            A set of nodes with no incoming edges.
	 * @param weightRemovedPerNode
	 *            A mapping between nodes and the total weight removed by removing some of their
	 *            incoming edges.
	 */
	private void removeOrReconnectDisconnectedNodes(Set<EdgeNode> noIncoming,
			Map<EdgeNode, Double> weightRemovedPerNode) {
		for (EdgeNode n : noIncoming) {
			if (outgoing.containsKey(n) == false) {
				removeNode(n);
				active.remove(n);
			} else {
				assert (weightRemovedPerNode.containsKey(n));

				double weight;
				if (weightRemovedPerNode.containsKey(n)) {
					weight = weightRemovedPerNode.get(n);
				} else {
					weight = 0;
					Logger.warn("There was a no-incoming non-root edge.");
				}
				addEdge(new Edge(root, n, new UnknownMethod(null, null), weight));
			}
		}
	}

	/**
	 * @param limit
	 *            Maximum weight to be included in the set.
	 * @return A set of all edges in the graph with weight less than <code>limit</code>.
	 */
	private Set<Edge> getLowWeightEdges(double limit) {
		Set<Edge> lowWeightEdges = new HashSet<Edge>();
		for (Edge e : edges) {
			if (e.getWeight() < limit) lowWeightEdges.add(e);
		}
		return lowWeightEdges;
	}

	/**
	 * @param toRemove
	 *            Set of edges to remove.
	 * @return Mapping between nodes and the sum of weights removed from their incoming set.
	 */
	private Map<EdgeNode, Double> removeEdges(Set<Edge> toRemove) {
		Map<EdgeNode, Double> weightRemovedPerNode = new HashMap<EdgeNode, Double>();
		for (Edge e : toRemove) {
			removeEdge(e);
			// Collect sum of incoming weight from "to" nodes:
			EdgeNode to = e.getTo();
			double oldWeight = weightRemovedPerNode.containsKey(to) ? weightRemovedPerNode.get(to)
					: 0;
			weightRemovedPerNode.put(to, oldWeight + e.getWeight());
		}
		return weightRemovedPerNode;
	}

	/**
	 * @param toRemove
	 *            Set of nodes to remove.
	 */
	private void removeNodes(Set<EdgeNode> toRemove) {
		for (EdgeNode e : toRemove) {
			removeNode(e);
		}
	}

	/**
	 * @return A set of all nodes that have no incoming edges, excluding the root.
	 */
	private Set<EdgeNode> getNodesWithNoIncoming() {
		Set<EdgeNode> noIncoming = new HashSet<EdgeNode>();
		for (EdgeNode n : nodes) {
			if (n.equals(root)) continue; // Don't include the root:
			if (getIngoingEdges(n).isEmpty()) noIncoming.add(n);
		}
		return noIncoming;
	}

	@Override
	public double getMaximumWeight() {
		double max = -1;
		for (Edge e : edges) {
			if (e.isUnknown()) continue; // skip unknown methods
			if (e.isConstructor()) continue; // skip constructors
			double w = e.getWeight();
			if (w > max) max = w;
		}
		return max;
	}

	@Override
	public int getDepth() {
		return getDepthFrom(new HashMap<EdgeNode, Integer>(), root);
	}

	/**
	 * @param seen
	 *            A mapping between seen nodes and the depth this method returned for them.
	 * @param n
	 *            Initial node.
	 * @return The maximum number of edges that can be traversed starting from <code>n</code> and
	 *         without visiting any of the nodes in <code>seen.keySet()</code>.
	 */
	private int getDepthFrom(HashMap<EdgeNode, Integer> seen, EdgeNode n) {
		if (seen.containsKey(n)) {
			return seen.get(n);
		}
		seen.put(n, 0); // To prevent infinite walk on loops
		int max = 0;
		for (Edge e : getOutgoingEdges(n)) {
			int d = 1 + getDepthFrom(seen, e.getTo());
			if (d > max) max = d;
		}
		seen.put(n, max);
		return max;
	}

	@Override
	public int getMaxDegree() {
		int max = -1;
		for (EdgeNode n : nodes) {
			int num = getOutgoingEdges(n).size();
			if (num > max) max = num;
		}
		return max;
	}

	@Override
	public double getAverageWeight() {
		double sum = 0;
		int count = 0;
		for (Edge e : edges) {
			if (e.isUnknown()) continue;
			double w = e.getWeight();
			if (w <= 0) continue;
			count++;
			sum += e.getWeight();
		}
		if (count == 0) return -1;
		return sum / count;
	}

	@Override
	public int getNumParticipatingTypes() {
		Set<AppType> types = new HashSet<AppType>();
		for (Edge e : edges) {
			for (AppMethodRef m : e.getMethods()) {
				types.add(m.getContainingType());
			}
		}
		return types.size();
	}

	@Override
	public int getNumEdges() {
		return edges.size();
	}

	@Override
	public int getNumUnknownEdges() {
		int count = 0;
		for (Edge e : edges) {
			for (AppMethodRef m : e.getMethods()) {
				if (m.isUnknown()) {
					count++;
					break;
				}
			}
		}
		return count;
	}

	@Override
	public String generateGraphvizOutput(String outputPath, int counter) throws IOException {
		String filename = getGvFilename(outputPath, counter);
		return generateGraphvizOutput(outputPath, filename);
	}

	/**
	 * @param outputPath
	 * @param counter
	 * @return Filename to be used for the generated graphviz file.
	 */
	private String getGvFilename(String outputPath, int counter) {
		String result = "";
		result += "History_" + counter;
		result = outputPath + File.separator + result + GRAPHVIZ_SUFFIX;
		return result;
	}

	/**
	 * @param outputPath
	 * @param counter
	 * @return Filename to be used for the generated XML file.
	 */
	private String getXmlFilename(String outputPath, int counter) {
		String result = "";
		result += "History_" + counter;
		result = outputPath + File.separator + result + ".xml";
		return result;
	}

	/**
	 * @return Graphviz file content.
	 */
	private String getGvContent() {
		StringBuilder sb = new StringBuilder();
		appendGvFileHeader(sb, getTitle());
		Map<EdgeNode, String> nodeNames = appendGvNodes(sb);
		Set<Edge> edgesOnHeaviestRoute = findEdgesOnHeaviestRoute();
		for (Edge e : edges) {
			appendGvEdge(nodeNames, sb, e, edgesOnHeaviestRoute.contains(e));
		}
		appendGvFileFooter(sb);
		return sb.toString();
	}

	private Map<EdgeNode, String> appendGvNodes(StringBuilder sb) {
		Map<EdgeNode, String> result = new HashMap<EdgeNode, String>();
		int counter = 1; // 0 is reserved for the root
		for (EdgeNode n : nodes) {
			String name = n == root ? "0" : "" + counter++;
			result.put(n, name);
			sb.append("\t" + name);
			if (active.contains(n)) {
				// String numEnding = StringUtils.prettyPrintNumber(numberOfRoutesEndingAt(n));
				sb.append("[ shape = \"doublecircle\" ");
				// sb.append(String.format("label = \"%s\\n/%s\" ]", name, numEnding));
				sb.append(String.format("label = \"%s\" ]", name));
			}
			sb.append(";\n");
		}
		return result;
	}

	// private int numberOfRoutesEndingAt(EdgeNode n) {
	// double balance = 0;
	// for (Edge e : getIngoingEdges(n)) balance += e.getWeight();
	// for (Edge e : getOutgoingEdges(n)) balance -= e.getWeight();
	// return (int)balance;
	// }

	private Set<Edge> findEdgesOnHeaviestRoute() {
		Set<Edge> result = new HashSet<Edge>();
		EdgeNode curr = root;
		while (true) {
			Edge e = getHeaviestOutoingEdge(curr);
			if (e == null || result.contains(e)) break;
			result.add(e);
			curr = e.getTo();
		}
		return result;
	}

	private Edge getHeaviestOutoingEdge(EdgeNode n) {
		Edge result = null;
		for (Edge e : getOutgoingEdges(n)) {
			if (result == null || result.getWeight() < e.getWeight()) {
				result = e;
			}
		}
		return result;
	}

	private void appendGvEdge(Map<EdgeNode, String> nodeNames, StringBuilder sb, Edge e,
			boolean bold) {
		sb.append(String.format("\t%s -> %s [ ",
				nodeNames.get(e.getFrom()),
				nodeNames.get(e.getTo())));

		appendGvLabel(sb, e);
		sb.append("weight = \"" + Math.round(e.getWeight()) + "\" ");
		appendGvTooltip(sb, e);
		if (bold) sb.append("style = \"setlinewidth(2)\" arrowsize = \"1.5\" color=\"blue\"");
		appendGvEdgeExtras(sb, e);

		sb.append("];\n");
	}

	protected void appendGvEdgeExtras(StringBuilder sb, Edge e) {}

	private void appendGvTooltip(StringBuilder sb, Edge e) {
		sb.append("URL = \"#\" tooltip = \"");
		boolean first = true;
		for (AppMethodRef m : e.getMethods()) {
			if (first) {
				first = false;
			} else {
				sb.append("; ");
			}
			appendGvFullMethodLabel(sb, m);
		}
		double w = e.getWeight();
		if (w > 0) sb.append(" * " + StringUtils.prettyPrintNumber(w));
		sb.append("\" ");
	}

	private void appendGvFullMethodLabel(StringBuilder sb, AppMethodRef m) {
		sb.append(m.getSignature());
	}

	private void appendGvLabel(StringBuilder sb, Edge e) {
		sb.append("label = \"");
		boolean first = true;
		Set<String> seen = new HashSet<String>();
		for (AppMethodRef m : e.getMethods()) {
			if (first) {
				first = false;
			} else {
				sb.append("\\n");
			}
			seen.add(appendGvMethodLabel(seen, sb, m, e.getMethods().size()));
		}
		double w = e.getWeight();
		if (w > 0) sb.append("\\nx " + StringUtils.prettyPrintNumber(w));
		sb.append("\" ");
	}

	private String appendGvMethodLabel(Set<String> seen, StringBuilder sb, AppMethodRef m,
			int numMethods) {
		String s = m.toString();
		if (seen.contains(s)) return s;
		if (numMethods == 1) {
			String[] parts = s.split("\\.[^(]+\\(");
			if (parts.length != 2) {
				sb.append(s);
				return s;
			}
			int pos = parts[0].length() + 1;
			String line1 = s.substring(0, pos);
			String line2 = s.substring(pos);
			sb.append(line1 + "\\n" + line2);
		} else {
			sb.append(s);
		}
		return s;
	}

	private void appendGvFileHeader(StringBuilder sb, String label) {
		sb.append("digraph finite_state_machine {\n" +
				"\tlabel = \"" + label + "\";\n" +
				"\tlabelloc = \"t\";\n" +
				"\trankdir=LR;\n" +
				"\tsize=\"8,5\"\n" +
				"\tnode [shape = circle];\n" +
				"\tHanchor [shape = point style=invis];\n" +
				"\tHanchor -> 0;\n");
	}

	private void appendGvFileFooter(StringBuilder sb) {
		sb.append("}\n");
	}

	@SuppressWarnings("unchecked")
	public Set<Edge> getIngoingEdges(EdgeNode n) {
		Set<Edge> result = ingoing.getAll(n);
		return result != null ? result : Collections.EMPTY_SET;
	}

	@SuppressWarnings("unchecked")
	public Set<Edge> getOutgoingEdges(EdgeNode n) {
		Set<Edge> result = outgoing.getAll(n);
		return result != null ? result : Collections.EMPTY_SET;
	}

	@Override
	public Set<AppMethodRef> getAllParticipatingMethods() {
		Set<AppMethodRef> result = new HashSet<AppMethodRef>();
		for (Edge e : edges) {
			result.addAll(e.getMethods());
		}
		return result;
	}

	@Override
	public String generateXmlOutput(String outputPath, int counter) throws IOException {
		Map<EdgeNode, String> nodeNames = new HashMap<EdgeNode, String>();
		DocNode doc = new DocNode(null, "automaton", null);
		doc.setAttribute("id", "A" + java.util.UUID.randomUUID().toString());
		doc.setAttribute("title", getTitle());
		doc.setAttribute("root", "H0");
		doc.add("type", "multiple");
		for (Edge e : edges) {
			DocNode edgeDocNode = doc.add("edge", null);
			edgeDocNode.setAttribute("src", nodeNames.get(e.getFrom()));
			edgeDocNode.setAttribute("dst", nodeNames.get(e.getTo()));
			edgeDocNode.setAttribute("weight", String.valueOf(e.getWeight()));
			for (AppMethodRef m : e.getMethods()) {
				edgeDocNode.add("label", m.getSignature());
			}
		}
		String filename = getXmlFilename(outputPath, counter);
		doc.save(filename);
		return filename;
	}

	@Override
	public void removeRootFromActive() {
		if (active.size() > 1 || active.contains(root) == false) active.remove(root);
	}

	// sharon
	// @Override
	// public boolean includes(History history)
	// {
	// if (history instanceof EdgeHistory == false) return false;
	// EdgeHistory otherHistory = (EdgeHistory)history;
	//
	// // compute a mapping of the nodes
	// Map<EdgeNode, EdgeNode> nodeToNode = computeIsomorphism(otherHistory);
	// if (nodeToNode == null) return false;
	// Map<EdgeNode, EdgeNode> otherToCurr = reverseMapping(nodeToNode);
	//
	// otherToCurr.remove(otherHistory.root);
	// EdgeNode matchForOtherRoot = computeRootMatch(otherHistory.root, otherHistory, otherToCurr);
	// if (matchForOtherRoot == null) return false;
	//
	// otherToCurr.put(otherHistory.root, matchForOtherRoot);
	//
	// return verifyInclusion(otherToCurr, otherHistory);
	// }

	@Override
	public boolean includes(History h) {
		EdgeHistory weightlessThis = (EdgeHistory) cloneWeightless();
		EdgeHistory weightlessThat = downcast(h.cloneWeightless());
		boolean found = false;
		for (EdgeNode n : weightlessThis.nodes) {
			found |= weightlessThis.includesPathsStartingFrom(
					new HashMap<EdgeNode, EdgeNode>(),
					n,
					weightlessThat,
					weightlessThat.getRoot());
		}
		return found;
	}

	private boolean includesPathsStartingFrom(
			Map<EdgeNode, EdgeNode> matching,
			EdgeNode localNode,
			EdgeHistory otherHistory,
			EdgeNode otherNode) {
		matching.put(localNode, otherNode);
		Set<Edge> edges1 = this.getOutgoingEdges(localNode);
		Set<Edge> edges2 = otherHistory.getOutgoingEdges(otherNode);
		for (Edge e2 : edges2) {
			Edge matchingEdge = null;
			for (Edge e1 : edges1) {
				if (e1.getMethods().containsAll(e2.getMethods())) {
					matchingEdge = e1;
					break;
				}
			}
			if (matchingEdge == null) return false;
			EdgeNode new1 = matchingEdge.getTo();
			EdgeNode new2 = e2.getTo();
			if (matching.containsKey(new1)) {
				if (matching.get(new1).equals(new2)) {
					continue;
				}
				return false;
			}
			if (includesPathsStartingFrom(matching, new1, otherHistory, new2) == false) return false;
		}
		return true;
	}

	// private EdgeNode computeRootMatch(EdgeNode otherRoot, EdgeHistory otherHistory, Map<EdgeNode,
	// EdgeNode> otherToCurr) {
	// for (EdgeNode n : nodes) {
	// if (futureMatches(n, otherRoot, otherHistory, otherToCurr)) return n;
	// }
	// return null;
	// }

	// private boolean futureMatches(EdgeNode currNode, EdgeNode otherNode,
	// EdgeHistory otherHistory, Map<EdgeNode, EdgeNode> otherToCurr) {
	// for (Edge e : otherHistory.getOutgoingEdges(otherNode)) {
	// EdgeNode other_toNode = e.getTo();
	// if (existsEdge(currNode, otherToCurr.get(other_toNode)) == false) return false;
	// }
	// return true;
	// }

	// private boolean existsEdge(EdgeNode to, EdgeNode from) {
	// for (Edge e : edges) {
	// if (e.getFrom().equals(from) && e.getTo().equals(to)) return true;
	// }
	// return false;
	// }

	// sharon
	// private Map<EdgeNode, EdgeNode> reverseMapping(Map<EdgeNode, EdgeNode> mapping) {
	// Map<EdgeNode, EdgeNode> reverse = new HashMap<EdgeNode,EdgeNode>();
	// for (Map.Entry<EdgeNode, EdgeNode> e : mapping.entrySet()) {
	// reverse.put(e.getValue(), e.getKey());
	// }
	// return reverse;
	// }

	// sharon
	// private boolean verifyInclusion(Map<EdgeNode, EdgeNode> otherToCurr, EdgeHistory
	// otherHistory) {
	// for (Edge e : otherHistory.edges) {
	// EdgeNode currFrom = otherToCurr.get(e.getFrom());
	// EdgeNode currTo = otherToCurr.get(e.getTo());
	// if (existsEdge(currFrom, currTo) == false) return false;
	// }
	// return true;
	// /*
	// for (EdgeNode oFrom : otherHistory.nodes)
	// {
	// for (EdgeNode oTo : otherHistory.nodes)
	// {
	// // if (!otherToCurr.containsKey(oFrom) || !otherToCurr.containsKey(oTo)) return false;
	// EdgeNode from = otherToCurr.get(oFrom);
	// EdgeNode to = otherToCurr.get(oTo);
	//
	// Set<Edge> edges = getEdgesBetweenNodes(from, to);
	// Set<Edge> otherEdges = otherHistory.getEdgesBetweenNodes(oFrom, oTo);
	//
	// if (otherEdges == null) continue;
	// if (edges == null) return false;
	// if
	// (!getParticipatingMethods(edges).containsAll(otherHistory.getParticipatingMethods(otherEdges))){
	// return false;
	// }
	// }
	// }
	//
	// return true;
	// */
	// }

	public EdgeNode getRoot() {
		return root;
	}

	@Override
	public History cloneWeightless() {
		if (weightlessClone == null) {
			weightlessClone = clone();
			for (Edge e : edges) {
				weightlessClone.removeEdge(e);
				weightlessClone.addEdge(new WeightlessEdge(e));
			}
		}
		return weightlessClone;
	}

	@Override
	public Set<AppType> getAllParticipatingTypes() {
		Set<AppType> result = new HashSet<AppType>();
		for (Edge e : edges) {
			for (AppMethodRef m : e.getMethods()) {
				AppType t = m.getContainingType();
				if (t != null) result.add(t);
			}
		}
		return result;
	}

	@Override
	public Set<AppType> getAllParticipatingApiTypes() {
		Set<AppType> result = new HashSet<AppType>();
		for (Edge e : edges) {
			for (AppMethodRef m : e.getMethods()) {
				AppType t = m.getContainingType();
				if (t != null &&
						m.isUnknown() == false &&
						options.getFilterReported().passesFilter(t.getFullName())) {
					result.add(t);
				}
			}
		}
		return result;
	}

	@Override
	public Set<? extends History> getSources() {
		return sources;
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	public void clearSources() {
		sources.clear();
	}

	@Override
	public boolean isFromClustering() {
		return !sources.isEmpty();
	}

	@Override
	public void recursivelySetOptions(Options options) {
		this.options = options;
		for (History h : getSources()) {
			h.recursivelySetOptions(options);
		}
	}

	public boolean isActive(EdgeNode n) {
		return active.contains(n);
	}

	private Set<Edge> edgesReachableFrom(EdgeNode start) {
		Set<Edge> result = new HashSet<Edge>();
		Set<EdgeNode> front = new HashSet<EdgeNode>(Collections.singleton(start));
		int prevSize;
		do {
			prevSize = result.size();
			Set<EdgeNode> newFront = new HashSet<EdgeNode>();
			for (EdgeNode n : front) {
				for (Edge e : getOutgoingEdges(n)) {
					result.add(e);
					newFront.add(e.getTo());
				}
			}
			front = newFront;
		} while (prevSize != result.size());
		return result;
	}

	private Set<EdgeNode> nodesReachableFrom(EdgeNode start) {
		Set<EdgeNode> result = new HashSet<EdgeNode>();
		Set<EdgeNode> front = new HashSet<EdgeNode>(Collections.singleton(start));
		int prevSize;
		do {
			prevSize = result.size();
			Set<EdgeNode> newFront = new HashSet<EdgeNode>();
			for (EdgeNode n : front) {
				for (Edge e : getOutgoingEdges(n)) {
					newFront.add(e.getTo());
					result.add(e.getTo());
				}
			}
			front = newFront;
		} while (prevSize != result.size());
		return result;
	}

	private Set<Edge> edgesReachableTo(EdgeNode end) {
		Set<Edge> result = new HashSet<Edge>();
		Set<EdgeNode> front = new HashSet<EdgeNode>(Collections.singleton(end));
		int prevSize;
		do {
			prevSize = result.size();
			Set<EdgeNode> newFront = new HashSet<EdgeNode>();
			for (EdgeNode n : front) {
				for (Edge e : getIngoingEdges(n)) {
					result.add(e);
					newFront.add(e.getFrom());
				}
			}
			front = newFront;
		} while (prevSize != result.size());
		return result;
	}

	@Override
	public Set<Ordering> getOrderings() {
		Set<Ordering> result = new HashSet<Ordering>();
		for (Edge e : edges) {
			for (Edge reachable : edgesReachableFrom(e.getTo())) {
				for (AppMethodRef m1 : e.getMethods()) {
					for (AppMethodRef m2 : reachable.getMethods()) {
						result.add(new Ordering(m1, m2));
					}
				}
			}
		}
		return result;
	}

	/**
	 * If this history contains a node that has a method appearing on one of its incoming edges,
	 * return that node; otherwise return null.
	 * 
	 * @param m
	 * @return
	 */
	public EdgeNode findNodeWithIncoming(AppMethodRef m) {
		for (Edge e : edges) {
			if (e.getMethods().contains(m)) return e.getTo();
		}
		return null;
	}

	/**
	 * Check whether this history includes another history. This takes unknown edges in the other
	 * history into consideration. This method answers the question, "is there a possible assignment
	 * for unknown edges in h, such that this history will strictly contain h?"
	 * 
	 * @param h
	 * @return
	 * @throws InterruptedException
	 * @throws CanceledException
	 */
	public boolean includesWithUnknown(History h) throws InterruptedException, CanceledException {
		if (this == h) return true;
		EdgeHistory including = this;
		EdgeHistory included = (EdgeHistory) h;

		// 1. Find matching nodes, but ignore unknown edges
		Map<EdgeNode, EdgeNode> matches = new HashMap<EdgeNode, EdgeNode>();
		outer: for (EdgeNode included_node : included.nodes) {
			Set<Edge> included_ingoing = included.getIngoingEdges(included_node);
			if (included_ingoing.isEmpty()) {
				matches.put(included_node, including.root);
				continue;
			}
			if (included_ingoing.iterator().next().isUnknown()) {
				continue;
			}
			for (EdgeNode including_node : including.nodes) {
				Set<Edge> including_ingoing = including.getIngoingEdges(including_node);
				if (including_ingoing.isEmpty() || including_ingoing.iterator().next().isUnknown()) {
					continue;
				}
				if (matching(included, included_node, including, including_node)) {
					matches.put(included_node, including_node);
					continue outer;
				}
			}
			// If we got here, there was a node in included which was not matched
			// in including.
			return false;
		}

		// 2. Find matching edges, ignore unknown edges
		for (Edge included_edge : included.edges) {
			if (included_edge.isUnknown()) continue;

			EdgeNode including_from = matches.get(included_edge.getFrom());
			EdgeNode including_to = matches.get(included_edge.getTo());
			if (including_from == null || including_to == null) continue;
			Edge including_edge = including.findEdge(including_from, including_to);
			if (including_edge == null) return false;
			if (including_edge.isUnknown()) return false; // Because included_edge isn't
			if (((ExteriorMatcher) options.getMatcher()).matchingMethodSets(
					included_edge.getMethods(), including_edge.getMethods()) == false) {
				return false;
			}
		}

		// 3. Check reachability for unknown edges
		for (Edge included_edge : included.edges) {
			if (included_edge.isUnknown() == false) continue;

			EdgeNode included_from = included_edge.getFrom();
			for (Edge included_nextEdge : included.getOutgoingEdges(included_edge.getTo())) {
				EdgeNode included_nextNode = included_nextEdge.getTo();

				boolean found = false;
				for (Edge including_edge : including.edgesReachableFrom(matches.get(included_from))) {
					if (including_edge.getTo().equals(matches.get(included_nextNode))) {
						found = true;
						break;
					}
				}
				if (found == false) return false;
			}
		}

		return true;
	}

	private Set<AppMethodRef> getMethodsInEdges(Iterable<? extends Edge> edges) {
		Set<AppMethodRef> result = new HashSet<AppMethodRef>();
		for (Edge e : edges) {
			result.addAll(e.getMethods());
		}
		return result;
	}

	@Override
	public History eliminateUnknowns(HistoryCollection base) throws InterruptedException,
			CanceledException {
		EdgeHistory result = clone();
		for (Edge e : edges) {
			if (e.isUnknown()) result.eliminateUnknown(e, base);
		}
		result.removeDisconnectedParts();
		return result;
	}

	private void eliminateUnknown(Edge unknownEdge, HistoryCollection base)
			throws InterruptedException, CanceledException {
		preUpdate();
		Set<AppMethodRef> fromMethods = getMethodsInEdges(getIngoingEdges(unknownEdge.getFrom()));
		Set<Edge> toEdges = new HashSet<Edge>(getOutgoingEdges(unknownEdge.getTo()));

		if (fromMethods.isEmpty() && toEdges.isEmpty()) return;

		Set<Edge> toRemove = new HashSet<Edge>();

		if (toEdges.isEmpty()) {
			Set<Edge> snapshot = new HashSet<Edge>(edges);
			for (History baseHistory : base.getHistories()) {
				EdgeHistory edgeBaseHistory = (EdgeHistory) baseHistory;
				EdgeNode fromNode = null;
				for (AppMethodRef fromMethod : fromMethods) {
					fromNode = edgeBaseHistory.findNodeWithIncoming(fromMethod);
					if (fromNode != null) break;
				}
				if (fromNode == null && fromMethods.isEmpty() == false) {
					// This history does not contain our "from" node
					continue;
				}
				addSubHistoryToEnd(
						unknownEdge.getWeight(), unknownEdge.getFrom(), edgeBaseHistory, fromNode,
						snapshot);
			}
		}

		for (Edge toEdge : toEdges) {
			EdgeNode toNode = null;
			Set<Edge> snapshot = new HashSet<Edge>(edges);

			for (History baseHistory : base.getHistories()) {
				EdgeHistory edgeBaseHistory = (EdgeHistory) baseHistory;
				EdgeNode fromNode = null;
				for (AppMethodRef fromMethod : fromMethods) {
					fromNode = edgeBaseHistory.findNodeWithIncoming(fromMethod);
					if (fromNode != null) break;
				}
				if (fromNode == null && fromMethods.isEmpty() == false) {
					// This history does not contain our "from" node
					continue;
				}

				for (AppMethodRef toMethod : toEdge.getMethods()) {
					toNode = edgeBaseHistory.findNodeWithIncoming(toMethod);
					if (toNode != null) break;
				}
				if (toNode == null) continue;

				if (fromNode == null) {
					boolean added = addSubHistoryFromStart(
							toEdge.getWeight(), toEdge.getTo(), edgeBaseHistory, toNode, snapshot);
					if (added) {
						toRemove.add(toEdge);
					}
					continue;
				}

				// If we got here, edgeBaseHistory might contain an automata
				// from fromNode to toNode that is a valid replacement for
				// the unknown edge.
				boolean added = addSubHistoryBothWays(
						toEdge.getWeight(), unknownEdge.getFrom(), toEdge.getTo(),
						edgeBaseHistory, fromNode, toNode, snapshot);
				if (added) {
					toRemove.add(toEdge);
				}
			}
		}

		for (Edge e : toRemove) {
			removeEdge(e);
		}
		if (toRemove.size() == toEdges.size()) {
			// All edges following the unknown edge have been removed
			removeEdge(unknownEdge);
			removeNode(unknownEdge.getTo());
		}
		postUpdate();
	}

	/**
	 * Take the sub-history between otherFrom and otherTo in other, and add it between thisFrom and
	 * thisTo in this history. Changes this history.
	 * 
	 * @param from
	 *            An initial node in this history.
	 * @param to
	 *            A final node in this history.
	 * @param h
	 *            The other history.
	 * @param fromNode
	 * @param toNode
	 * @throws CanceledException
	 * @throws InterruptedException
	 */
	private boolean addSubHistoryBothWays(
			double weight, EdgeNode thisFrom, EdgeNode thisTo,
			EdgeHistory other, EdgeNode otherFrom, EdgeNode otherTo,
			Set<Edge> only) throws InterruptedException, CanceledException {
		Set<Edge> reachableFrom = other.edgesReachableFrom(otherFrom);
		Set<Edge> reachableTo = other.edgesReachableTo(otherTo);
		Set<Edge> reachable = reachableFrom;
		reachable.retainAll(reachableTo);

		return addSubHistory(weight, other, reachable, only);
	}

	private boolean addSubHistoryToEnd(
			double weight, EdgeNode thisFrom,
			EdgeHistory other, EdgeNode otherFrom,
			Set<Edge> only) throws InterruptedException, CanceledException {
		Set<Edge> reachable = other.edgesReachableFrom(otherFrom);

		return addSubHistory(weight, other, reachable, only);
	}

	private boolean addSubHistoryFromStart(
			double weight, EdgeNode thisTo,
			EdgeHistory other, EdgeNode otherTo,
			Set<Edge> only) throws InterruptedException, CanceledException {
		Set<Edge> reachable = other.edgesReachableTo(otherTo);

		return addSubHistory(weight, other, reachable, only);
	}

	private boolean addSubHistory(double weight, EdgeHistory other, Set<Edge> reachable,
			Set<Edge> only)
			throws InterruptedException, CanceledException {
		if (reachable.isEmpty()) return false;
		for (Edge e : reachable)
			if (e.isUnknown()) return false;

		Set<Edge> weightedReachable = new HashSet<Edge>(reachable.size());
		for (Edge e : reachable) {
			weightedReachable.add(new Edge(e.getFrom(), e.getTo(), e.getMethods(), weight));
		}
		mergeEdgesFrom(other, weightedReachable, true, false, only);

		return true;
	}

	public String getEdgeString() {
		StringBuilder sb = new StringBuilder();
		for (Edge e : edges) {
			sb.append(e.toString() + "   " + e.getMethods().iterator().next().getSignature());
			sb.append("\n");
		}
		return sb.toString();
	}

	/**
	 * Returns a history which is contained in this history, but only contains simple sequences that
	 * have non-empty intersection with the argument.
	 * 
	 * @param h
	 * @return
	 * @throws CanceledException
	 * @throws InterruptedException
	 */
	public History sequenceIntersect(EdgeHistory h) throws InterruptedException, CanceledException {
		EdgeHistory result = (EdgeHistory) options.newHistory();
		result.sources.add(this);
		int sequenceCount = 0;
		for (EdgeSequence s : buildMethodSequences(1, -1, null)) {
			Logger.debug("  slicing sequence " + sequenceCount++);
			ConcurrencyUtils.checkState();
			if (methodContainsIgnoringUnknowns(
					getMethodsInEdges(s), h.getAllParticipatingMethods())) {
				result.mergeEdgesFrom(this, s, false, false);
			}
		}
		return result;
	}

	private boolean methodContainsIgnoringUnknowns(
			Set<AppMethodRef> containing,
			Set<AppMethodRef> contained) {
		for (AppMethodRef m : contained) {
			if (m.isUnknown()) continue;
			if (containing.contains(m) == false) return false;
		}
		return true;
	}

	/**
	 * Returns all the linear method sequences contained in this history. Notice that since the
	 * history might contain loops, it is important to bound either the number of times an edge may
	 * appear in a sequence, or the number of times a node may appear.
	 * 
	 * @param maxEdgeRepetitions
	 *            The maximum number of times a single edge may appear in a sequence. Use -1 for
	 *            unlimited.
	 * @param maxNodeRepetition
	 *            The maximum number of times a single node may appear in a sequence. Use -1 for
	 *            unlimited.
	 * @return A collection of edge sequences, each representing a single sequence in this history.
	 */
	public Iterable<EdgeSequence> buildMethodSequences(
			final int maxEdgeRepetitions,
			final int maxNodeRepetitions,
			final Set<AppType> extraTypes) {
		return new ThreadedYieldAdapter<EdgeSequence>().adapt(new Collector<EdgeSequence>() {

			@Override
			public void collect(ResultHandler<EdgeSequence> handler)
					throws CollectionAbortedException {
				LinkedList<EdgeNode> nodes = new LinkedList<EdgeNode>();
				nodes.add(root);
				yieldNextInner(handler, new LinkedList<Edge>(), nodes, root);
			}

			private void yieldNextInner(ResultHandler<EdgeSequence> handler,
					LinkedList<Edge> edges,
					LinkedList<EdgeNode> nodes,
					EdgeNode curr) {
				if (nodes.size() > SEQUENCE_LENGTH_THRESHOLD) return;
				Set<Edge> out = getOutgoingEdges(curr);
				// boolean added = false;
				for (Edge e : out) {
					if (e.getWeight() < SEQUENCE_EDGE_THRESHOLD) continue;
					edges.addLast(e);
					nodes.addLast(e.getTo());
					try {
						if (maxEdgeRepetitions > 0 && countInstances(edges, e) > maxEdgeRepetitions) {
							continue;
						}
						if (maxNodeRepetitions > 0
								&& countInstances(nodes, e.getTo()) > maxNodeRepetitions) {
							continue;
						}
						yieldNextInner(handler, edges, nodes, e.getTo());
						// added |= true;
					} finally {
						edges.removeLast();
						nodes.removeLast();
					}
				}
				if (edges.size() > 0 /* && added == false */) {
					try {
						handler.handleResult(new EdgeSequence(edges, extraTypes));
					} catch (CollectionAbortedException e1) {}
				}
			}

		});
	}

	/**
	 * Create a new history which only contains parts in this history which are contained in the
	 * argument history.
	 * 
	 * @param other
	 * @return
	 * @throws CanceledException
	 * @throws InterruptedException
	 */
	public EdgeHistory intersect(EdgeHistory other) throws InterruptedException, CanceledException {
		preUpdate();
		try {
			Map<EdgeNode, EdgeNode> thisToOtherNodes = new HashMap<EdgeNode, EdgeNode>();
			for (EdgeNode n : other.nodes) {
				EdgeNode matchingLocalNode = findMatching(n, other);
				if (matchingLocalNode == null) continue;
				thisToOtherNodes.put(matchingLocalNode, n);
			}
			thisToOtherNodes.put(root, other.root);

			Set<Edge> toRemove = new HashSet<Edge>();
			for (Edge e : edges) {
				// if (e.isUnknown()) continue;
				EdgeNode otherFrom = thisToOtherNodes.get(e.getFrom());
				if (otherFrom == null) {
					toRemove.add(e);
					continue;
				}
				EdgeNode otherTo = thisToOtherNodes.get(e.getTo());
				if (otherTo == null) {
					toRemove.add(e);
					continue;
				}
				Edge matching = other.findEdge(otherFrom, otherTo);
				if (matching == null
						|| containsAllNames(e.getMethods(), matching.getMethods()) == false) {
					toRemove.add(e);
					continue;
				}
			}

			EdgeHistory result = clone();
			result.removeEdges(toRemove);
			result.removeDisconnectedParts();
			return result;
		} finally {
			postUpdate();
		}
	}

	private boolean containsAllNames(Set<AppMethodRef> container, Set<AppMethodRef> contained) {
		for (AppMethodRef containedMethod : contained) {
			if (containedMethod.isUnknown()) continue;
			boolean found = false;
			for (AppMethodRef containerMethod : container) {
				if (containerMethod.isUnknown()) continue;
				if (containedMethod.getShortName().equals(containerMethod.getShortName())) {
					found = true;
					break;
				}
			}
			if (found == false) return false;
		}
		return true;
	}

	private void removeDisconnectedParts() {
		Set<Edge> remainingEdges = edgesReachableFrom(root);
		Set<EdgeNode> remainingNodes = nodesReachableFrom(root);
		remainingNodes.add(root);
		Set<Edge> edgesToRemove = new HashSet<Edge>(edges);
		edgesToRemove.removeAll(remainingEdges);
		Set<EdgeNode> nodesToRemove = new HashSet<EdgeNode>(nodes);
		nodesToRemove.removeAll(remainingNodes);
		removeEdges(edgesToRemove);
		removeNodes(nodesToRemove);
	}

	public Iterable<EdgeSequence> getTopEdgeSequences(int numSequences,
			int maxRepetitionsInSequence,
			Comparator<EdgeSequence> comparer) {
		PriorityQueue<EdgeSequence> top = new PriorityQueue<EdgeSequence>(numSequences + 1,
				comparer);
		LinkedList<EdgeNode> seenNodes = new LinkedList<EdgeNode>();
		seenNodes.add(root);
		getTopEdgeSequencesHelper(
				numSequences,
				maxRepetitionsInSequence,
				root,
				top,
				new LinkedList<Edge>(),
				seenNodes);
		return top;
	}

	private void getTopEdgeSequencesHelper(
			int numSequences,
			int maxRepetitionsInSequence,
			EdgeNode currentNode,
			PriorityQueue<EdgeSequence> top,
			LinkedList<Edge> seenEdges,
			LinkedList<EdgeNode> seenNodes) {
		Set<Edge> outgoing = getOutgoingEdges(currentNode);
		if (outgoing.isEmpty()) return;
		for (Edge e : outgoing) {
			int numSeen = countInstances(seenNodes, e.getTo());
			if (numSeen + 1 > maxRepetitionsInSequence) continue;
			seenEdges.addLast(e);
			seenNodes.addLast(e.getTo());
			try {
				EdgeSequence newSequence = new EdgeSequence(seenEdges, null);
				top.add(newSequence);
				if (top.size() > numSequences) {
					EdgeSequence removed = top.remove();
					if (removed == newSequence) {
						continue;
					}
				}
				getTopEdgeSequencesHelper(numSequences,
						maxRepetitionsInSequence,
						e.getTo(),
						top,
						seenEdges,
						seenNodes);
			} finally {
				seenEdges.removeLast();
			}
		}
	}

	private <T> int countInstances(Iterable<? extends T> iterable, T element) {
		int result = 0;
		for (T t : iterable)
			if (element == t) result++;
		return result;
	}

	public EdgeHistory normalize() {
		EdgeHistory result = clone();
		for (EdgeNode n : result.nodes) {
			result.normalize(n);
		}
		return result;
	}

	private void normalize(EdgeNode n) {
		double weightSum = 0;
		Set<Edge> toChange = new HashSet<Edge>();
		for (Edge e : getOutgoingEdges(n)) {
			weightSum += e.getWeight();
			toChange.add(e);
		}
		for (Edge e : toChange) {
			Edge newEdge = new Edge(e.getFrom(), e.getTo(), e.getMethods(), e.getWeight()
					/ weightSum);
			removeEdge(e);
			addEdge(newEdge);
		}
	}

	@Override
	public String generateGraphvizOutput(String outputPath, String filename) throws IOException {
		String content = getGvContent();

		filename = outputPath + File.separator + filename + GRAPHVIZ_SUFFIX;

		FileUtils.writeStringToFile(new File(filename), content);

		Logger.log("wrote history to " + filename);

		return filename;
	}

}
