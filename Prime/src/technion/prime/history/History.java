package technion.prime.history;

import java.io.IOException;
import java.io.Serializable;
import java.util.Set;

import technion.prime.Options;
import technion.prime.dom.AppMethodRef;
import technion.prime.dom.AppType;
import technion.prime.history.edgeset.Edge;
import technion.prime.history.edgeset.EdgeNode;
import technion.prime.utils.Logger.CanceledException;

/**
 * A method invocation history, in the form of an automaton. Guaranteed attributes
 * of the automaton:
 * <ul>
 * <li>Single initial entry
 * <li>Every node may or may not be a receiving node
 * <li>Edges are labeled by a collection of type-method pairs.
 * <li>Two edges which are labeled by the same methods must enter the same node
 * <li>Edges are weighted with a floating-point number
 * </ul> 
 * Mutable.
 * Equality and hash are identity-based; use equalContent() for content equality.
 */
public interface History extends Cloneable, Serializable {

	/**
	 * @return The number of nodes in this history.
	 */
	int getNumNodes();

	/**
	 * @return Maximum number of edges one can traverse without returning to
	 * the same node.
	 * @throws InterruptedException
	 * @throws CanceledException
	 */
	int getDepth() throws InterruptedException, CanceledException;

	/**
	 * @return Maximum fan-out degree in the automaton.
	 * @throws InterruptedException
	 * @throws CanceledException
	 */
	int getMaxDegree() throws InterruptedException, CanceledException;

	/**
	 * @return Average edge weight.
	 */
	double getAverageWeight();

	/**
	 * @return Maximum edge weight.
	 */
	double getMaximumWeight();

	/**
	 * @return Number of different types appearing in the automaton.
	 * @throws InterruptedException
	 * @throws CanceledException
	 */
	int getNumParticipatingTypes() throws InterruptedException, CanceledException;

	/**
	 * @return Number of edges in the automaton.
	 */
	int getNumEdges();

	/**
	 * @return Number of edges in the automaton which are labeled by unknown methods.
	 */
	int getNumUnknownEdges();

	/**
	 * Extends this history with a method call.
	 * @param m The method which was invoked.
	 * @param weight The weight of the call.
	 * @throws InterruptedException
	 * @throws CanceledException
	 */
	void extendWithMethodCall(AppMethodRef m, double weight) throws InterruptedException, CanceledException;

	/**
	 * Note: since history equality is identity-based, then
	 * <ul>
	 * <li><code>h == h.clone()</code> will always be false
	 * <li><strong><code>h.equals(h.clone())</code> will always be false</strong>
	 * <li><code>h.equalContent(h.clone())</code> will always be true
	 * </ul>
	 * @return Create a new copy of this history with equal content.
	 */
	History clone();

	/**
	 * Remove all edges with weight below the given weight.
	 * @param threshold Weight threshold.
	 */
	void filterLowWeightEdges(double threshold);

	/**
	 * @return A set of all the types containing the methods on the edges
	 * of this automaton.
	 */
	Set<AppType> getAllParticipatingTypes();
	
	/**
	 * @return A set of all the methods appearing on edges in this automaton.
	 */
	Set<AppMethodRef> getAllParticipatingMethods();

	/**
	 * Merge the items from a different history into this one, maintaining an "and" relation -
	 * meaning we consider this as if both this and the argument history have happened.
	 * 
	 * Used for merging histories together in the summarization phase.
	 * 
	 * This is a symmetric operation unless inclusion is true.
	 * 
	 * @param h
	 * @param inclusion If true, h's root can be matched to any node, not just this root.
	 * @throws CanceledException 
	 * @throws InterruptedException 
	 */
	void mergeFrom(History h, boolean inclusion) throws InterruptedException, CanceledException;
	
	/**
	 * Merge the items from a different history into this one, maintaining an "or" relation -
	 * meaning we consider this as if either this or argument history have happened.
	 * This is a symmetric operation.
	 * 
	 * Used for joining histories together in the analysis stage.
	 * 
	 * @param h
	 * @throws CanceledException 
	 * @throws InterruptedException 
	 */
	void joinFrom(History h) throws InterruptedException, CanceledException;
	
	/**
	 * Generate an output graph file in graphviz format from this history.
	 * @param outputFolder Folder to save the graph file to.
	 * @param counter Global counter, should be unique for each call to this method.
	 * @return The filename of the saved graphviz file.
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws CanceledException
	 */
	String generateGraphvizOutput(String outputFolder, int counter) throws IOException, InterruptedException, CanceledException;

	String generateGraphvizOutput(String outputPath, String fileName) throws IOException;
	
	String writeGraphvizFile(String outputPath, String filename, String graphId) throws IOException;
	
	/**
	 * Generate an output xml file from this history.
	 * @param outputFolder Folder to save the file to.
	 * @param counter Global counter, should be unique for each call to this method.
	 * @return The filename of the saved xml file.
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws CanceledException
	 */
	String generateXmlOutput(String outputFolder, int counter) throws IOException, InterruptedException, CanceledException;

	/**
	 * If the root node is an active node, but not the *only* active node, make it no longer active.
	 */
	void removeRootFromActive();

	/**
	 * @param h Another history.
	 * @return True iff this history contains a subgraph with equal content
	 * to the argument.
	 */
	boolean includes(History h);

	/**
	 * @param h Another history.
	 * @return True iff the content of both histories is equivalent.
	 * @throws CanceledException 
	 * @throws InterruptedException 
	 */
	boolean equalContent(History h) throws InterruptedException, CanceledException;
	
	/**
	 * @return True iff this automaton contains only one node - a root node -
	 * and no edges.
	 */
	boolean containsOnlyRoot();
	
	/**
	 * @return A clone of this history in which all the edges have weight 0.
	 */
	History cloneWeightless();
	
	/**
	 * @return The histories from which this history was created.
	 */
	Set<? extends History> getSources();
	
	/**
	 * @return Title of this history.
	 */
	String getTitle();
	
	/**
	 * @param title The new title of this history.
	 */
	void setTitle(String title);

	/**
	 * @return Hash for content.
	 */
	int contentHash();

	/**
	 * Remove all the sources of this history.
	 */
	void clearSources();
	
	/**
	 * @return If this history was creating during the clustering phase, from other histories.
	 */
	boolean isFromClustering();

	/**
	 * Set the current execution options to this object and all its childs.
	 */
	void recursivelySetOptions(Options options);

	/**
	 * @return A set of all the API types participating in this history.
	 */
	Set<AppType> getAllParticipatingApiTypes();

	/**
	 * @return A set of method ordering.
	 */
	Set<Ordering> getOrderings();
	
	History eliminateUnknowns(HistoryCollection base) throws InterruptedException, CanceledException;

	Set<Edge> edges();

	Set<EdgeNode> nodes();
	
	public EdgeNode root();

	boolean isActive(EdgeNode n);

	Set<Edge> getOutgoingEdges(EdgeNode n);
}
