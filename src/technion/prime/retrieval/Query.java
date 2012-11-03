package technion.prime.retrieval;

/**
 * Represents a search query for a search engine.
 */
public class Query {
	private final String searchString;
	public Query(String searchString) {
		this.searchString = searchString;
	}
	public String getSearchString() {
		return searchString;
	}
}
