package technion.prime.history.edgeset;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import technion.prime.DefaultOptions;
import technion.prime.utils.Logger;
import technion.prime.utils.Logger.CanceledException;

public class NormalizationTest {
	private DefaultOptions options;

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
	}
	
	private EdgeHistoryBuilder b() {
		return new EdgeHistoryBuilder(options);
	}
	
	@Test
	public void testNormalization1() throws InterruptedException, CanceledException {
		// input: a*1
		// output: a*1
		EdgeHistory input = b()
				.withEdge().fromRoot().to("H1").name("a").weight(1).buildEdge()
				.buildHistory();
		EdgeHistory expected = input;
		EdgeHistory result = input.normalize();
		assertTrue(expected.equalContent(result));
	}
	
	@Test
	public void testNormalization2() throws InterruptedException, CanceledException {
		// input: a*1 | b*1
		// output: a*0.5 | b*0.5
		EdgeHistory input = b()
				.withEdge().fromRoot().to("H1").name("a").weight(1).buildEdge()
				.withEdge().fromRoot().to("H2").name("b").weight(1).buildEdge()
				.buildHistory();
		EdgeHistory expected = b()
				.withEdge().fromRoot().to("H1").name("a").weight(0.5).buildEdge()
				.withEdge().fromRoot().to("H2").name("b").weight(0.5).buildEdge()
				.buildHistory();
		EdgeHistory result = input.normalize();
		assertTrue(expected.equalContent(result));
	}
	
	@Test
	public void testNormalization3() throws InterruptedException, CanceledException {
		// input: a*4 (b*2 | c*3)
		// output: a*1 (b*0.4 | c*0.6)
		EdgeHistory input = b()
				.withEdge().fromRoot().to("H1").name("a").weight(4).buildEdge()
				.withEdge().from("H1").to("H2").name("b").weight(2).buildEdge()
				.withEdge().from("H1").to("H3").name("c").weight(3).buildEdge()
				.buildHistory();
		EdgeHistory expected = b()
				.withEdge().fromRoot().to("H1").name("a").weight(1).buildEdge()
				.withEdge().from("H1").to("H2").name("b").weight(0.4).buildEdge()
				.withEdge().from("H1").to("H3").name("c").weight(0.6).buildEdge()
				.buildHistory();
		EdgeHistory result = input.normalize();
		assertTrue(expected.equalContent(result));
	}
}
