package technion.prime.retrieval.googlecodesearch;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import com.google.gdata.client.GoogleService.CaptchaRequiredException;
import com.google.gdata.client.Query.CustomParameter;
import com.google.gdata.client.Query;
import com.google.gdata.client.codesearch.CodeSearchService;
import com.google.gdata.data.codesearch.CodeSearchEntry;
import com.google.gdata.data.codesearch.CodeSearchFeed;
import com.google.gdata.util.ServiceException;

import technion.prime.retrieval.Gatherer;
import technion.prime.statistics.AnalysisDetails;
import technion.prime.utils.Logger;
import technion.prime.utils.Logger.CanceledException;
import technion.prime.Options;
import technion.prime.retrieval.CodeSample;


public class GoogleCodeSearchGatherer extends Gatherer {
	private static URL FEED_URL;
	private static int MAX_RESULTS_PER_CALL = 100;
	private static long MILLIS_TO_WAIT_BETWEEN_CALLS = 10000;
	
	protected final Options options;
	private int pos = 1;
	CodeSearchService searchService = new CodeSearchService("Technion-ApiHelper-1.0");
	
	static {
		try {
			FEED_URL = new URL("http://www.google.com/codesearch/feeds/search");
		} catch (MalformedURLException e) {
			assert(false);
		}
	}
	
	public GoogleCodeSearchGatherer(Options options) {
		this.options = options;
	}
	
	@Override
	public List<CodeSample> getNextSamples(technion.prime.retrieval.Query q, int numberOfSamples) throws InterruptedException, CanceledException {
		LinkedList<CodeSample> results = new LinkedList<CodeSample>();
		int total = -1;
		int toDownload = -1;
		boolean first = true;
		int prevPos = pos;
		while (pos < numberOfSamples) {
			int querySize = Math.min(numberOfSamples - results.size(), MAX_RESULTS_PER_CALL);
			CodeSearchFeed feed = null;
			try {
				Query googleQuery = userQueryToGoogleQuery(q, querySize);
				feed = searchService.getFeed(googleQuery, CodeSearchFeed.class);
				if (first) {
					first = false;
					total = feed.getTotalResults();
					toDownload = Math.min(total, numberOfSamples);
					Logger.debug("using query: " + googleQuery.getFullTextQuery());
					Logger.debug("Google reports " + total + " available results.");
				}
			} catch (CaptchaRequiredException ex) {
				try {
					Thread.sleep(MILLIS_TO_WAIT_BETWEEN_CALLS);
				} catch (InterruptedException e1) {
					return null;
				}
			} catch (ServiceException ex) {
				Logger.warn("could not retrieve more results from google code search: " + ex.getMessage());
				return results;
			} catch (IOException ex) {
				Logger.warn("IO error when trying to retrieve results from google code search: " + ex.getMessage());
			}
			for (CodeSearchEntry e : feed.getEntries()) {
				results.add(createCodeSearchSample(e));
				pos++;
				Logger.progress();
			}
			options.getOngoingAnalysisDetails().setField(AnalysisDetails.NUM_AVAILABLE_RESULTS, total);
			Logger.debug("Located " + results.size() + "/" + toDownload + " available sources.");
			if (pos >= total || prevPos == pos) {
				Logger.debug("Could not provide additional results.");
				break;
			}
			prevPos = pos;
		}
		return results;
	}
	
	protected CodeSample createCodeSearchSample(CodeSearchEntry e) {
		return new GoogleCodeSearchSample(options, e);
	}

	private Query userQueryToGoogleQuery(technion.prime.retrieval.Query userQuery, int numberOfSamples) {
		Query q = new Query(FEED_URL);
		//String classPackageString = JavaFileUtils.getSurroundingPackage(userQuery.getSearchString());
		//String literalQuery = Matcher.quoteReplacement(classPackageString);
		q.setFullTextQuery(userQuery.getSearchString() + " file:^.*\\.java$");
		q.setMaxResults(numberOfSamples);
		q.setStartIndex(pos);
		// Don't filter out redundancies
		q.addCustomParameter(new CustomParameter("filter", "0"));
		return q;
	}

}
