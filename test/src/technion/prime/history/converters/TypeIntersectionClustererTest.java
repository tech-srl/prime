package technion.prime.history.converters;

import static technion.prime.history.HistoryTestUtils.assertEqualContentHistories;

import org.junit.Before;
import org.junit.Test;

import technion.prime.DefaultOptions;
import technion.prime.history.History;
import technion.prime.history.HistoryCollection;
import technion.prime.history.edgeset.EdgeHistoryBuilder;
import technion.prime.utils.Logger.CanceledException;
import technion.prime.utils.Stage;

public class TypeIntersectionClustererTest {
	private DefaultOptions options;

	@Before
	public void setUp() {
		options = new DefaultOptions() {
			private static final long serialVersionUID = -5937606229088138088L;
			@Override public long getSingleActionTimeout(Stage stage) { return Integer.MAX_VALUE; }
			@Override public long getStageTimeout(Stage stage) { return Integer.MAX_VALUE; }
			@Override public boolean shouldShowExceptions() { return true; }
			@Override public boolean useHistoryInvariant() { return true; }
		};
	}
	
	private EdgeHistoryBuilder b() {
		return new EdgeHistoryBuilder(options);
	}
	
	@Test
	public void testIntersection() throws InterruptedException, CanceledException {
		History history1 = b()
				.withEdge().fromRoot().to("H1").name("f1").definedBy("A").buildEdge()
				.withEdge().from("H1").to("H2").name("f2").definedBy("B").buildEdge()
				.buildHistory();
		History history2 = b()
				.withEdge().fromRoot().to("H1").name("f3").definedBy("B").buildEdge()
				.withEdge().from("H1").to("H2").name("f4").definedBy("C").buildEdge()
				.buildHistory();
		History history3 = b()
				.withEdge().fromRoot().to("H1").name("f5").definedBy("C").buildEdge()
				.withEdge().from("H1").to("H2").name("f6").definedBy("D").buildEdge()
				.buildHistory();
		History history4 = b()
				.withEdge().fromRoot().to("H1").name("f7").definedBy("E").buildEdge()
				.withEdge().from("H1").to("H2").name("f8").definedBy("F").buildEdge()
				.buildHistory();
		
		History expected1 = history1.clone();
		expected1.mergeFrom(history2, true);
		expected1.mergeFrom(history3, true);
		
		HistoryCollection input = options.newHistoryCollection();
		input.addHistory(history1);
		input.addHistory(history2);
		input.addHistory(history3);
		input.addHistory(history4);
		
		TypeIntersectionClusterer c = new TypeIntersectionClusterer(options);
		HistoryCollection result = c.convert(input);
		assertEqualContentHistories(result, expected1, history4);
	}
}
