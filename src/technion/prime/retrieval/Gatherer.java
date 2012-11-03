package technion.prime.retrieval;

import java.util.List;

import technion.prime.utils.Logger.CanceledException;

/**
 * Represents some source which can return code samples, given a query.
 * A typical gatherer interfaces with a code search engine.
 */
public abstract class Gatherer {
	/**
	 * Get the <code>numberOfSamples</code> next samples for the query provided.
	 * @param q
	 * @param numberOfSamples
	 * @return A list with all located code samples.
	 * @throws InterruptedException 
	 * @throws CanceledException 
	 */
	public abstract List<CodeSample> getNextSamples(Query q, int numberOfSamples) throws InterruptedException, CanceledException;
}
