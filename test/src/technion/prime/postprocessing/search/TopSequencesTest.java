package technion.prime.postprocessing.search;

import static technion.prime.history.HistoryTestUtils.assertEqualContentHistories;
import org.junit.Before;
import org.junit.Test;

import technion.prime.DefaultOptions;
import technion.prime.history.HistoryCollection;
import technion.prime.history.edgeset.EdgeHistory;
import technion.prime.history.edgeset.EdgeHistoryBuilder;
import technion.prime.utils.Logger;
import technion.prime.utils.Logger.CanceledException;

public class TopSequencesTest {
	private DefaultOptions options;
	private Search search;

	@Before
	public void setUp() {
		options = new DefaultOptions() {
			private static final long serialVersionUID = -7543041204688327426L;

			@Override
			public boolean useHistoryInvariant() {
				return true;
			}
		};
		Logger.setup(options, false);
		search = new Search(options);
	}
	
	private EdgeHistoryBuilder b() {
		return new EdgeHistoryBuilder(options);
	}
	
	@Test
	public void testExtract1() throws InterruptedException, CanceledException {
		// a*3
		// expect top 3 to be {a*3}
		EdgeHistory input = b()
				.withEdge().fromRoot().to("H0").name("a").weight(3).buildEdge()
				.buildHistory();
		EdgeHistory expected = input;
		HistoryCollection result = search.extractTopSequences(null, input, 3);
		assertEqualContentHistories(result, expected);
	}
	
	@Test
	public void testExtract2() throws InterruptedException, CanceledException {
		// a*3 | b*4 | c*5
		// expect top 2 to be {b*4, c*5}
		EdgeHistory input = b()
				.withEdge().fromRoot().to("H0").name("a").weight(3).buildEdge()
				.withEdge().fromRoot().to("H1").name("b").weight(4).buildEdge()
				.withEdge().fromRoot().to("H2").name("c").weight(5).buildEdge()
				.buildHistory();
		EdgeHistory expected1 = b()
				.withEdge().fromRoot().to("H1").name("b").weight(4).buildEdge()
				.buildHistory();
		EdgeHistory expected2 = b()
				.withEdge().fromRoot().to("H1").name("c").weight(5).buildEdge()
				.buildHistory();
		HistoryCollection result = search.extractTopSequences(null, input, 2);
		assertEqualContentHistories(result, expected1, expected2);
	}
	
	@Test
	public void testExtract3() throws InterruptedException, CanceledException {
		// a*1 (b*2 c*3 | d*3 e*1)
		// expect top 1 to be {a*1, b*2, c*3}
		EdgeHistory input = b()
				.withEdge().fromRoot().to("H0").name("a").weight(1).buildEdge()
				.withEdge().from("H0").to("H1").name("b").weight(2).buildEdge()
				.withEdge().from("H1").to("H2").name("c").weight(3).buildEdge()
				.withEdge().from("H0").to("H3").name("d").weight(3).buildEdge()
				.withEdge().from("H3").to("H4").name("e").weight(1).buildEdge()
				.buildHistory();
		EdgeHistory expected = b()
				.withEdge().fromRoot().to("H0").name("a").weight(1).buildEdge()
				.withEdge().from("H0").to("H1").name("b").weight(2).buildEdge()
				.withEdge().from("H1").to("H2").name("c").weight(3).buildEdge()
				.buildHistory();
		HistoryCollection result = search.extractTopSequences(null, input, 1);
		assertEqualContentHistories(result, expected);
	}
	
	@Test
	public void testExtract4() throws InterruptedException, CanceledException {
		// a*3 (b*100 c*100 | d*4 e*5)
		// query is a?e
		// expect top 1 to be {a*3, d*4, e*5}
		EdgeHistory input = b()
				.withEdge().fromRoot().to("H0").name("a").weight(3).buildEdge()
				.withEdge().from("H0").to("H1").name("b").weight(100).buildEdge()
				.withEdge().from("H1").to("H2").name("c").weight(100).buildEdge()
				.withEdge().from("H0").to("H3").name("d").weight(4).buildEdge()
				.withEdge().from("H3").to("H4").name("e").weight(5).buildEdge()
				.buildHistory();
		EdgeHistory query = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").buildEdge()
				.withEdge().from("H1").to("H2").name("e").buildEdge()
				.buildHistory();
		EdgeHistory expected = b()
				.withEdge().fromRoot().to("H0").name("a").weight(3).buildEdge()
				.withEdge().from("H0").to("H1").name("d").weight(4).buildEdge()
				.withEdge().from("H1").to("H2").name("e").weight(5).buildEdge()
				.buildHistory();
		HistoryCollection result = search.extractTopSequences(query, input, 1);
		assertEqualContentHistories(result, expected);
	}
}
