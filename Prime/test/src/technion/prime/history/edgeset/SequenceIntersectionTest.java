package technion.prime.history.edgeset;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import technion.prime.DefaultOptions;
import technion.prime.history.History;
import technion.prime.utils.Logger;
import technion.prime.utils.Logger.CanceledException;

public class SequenceIntersectionTest {
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
	public void testIntersection1() throws InterruptedException, CanceledException {
		// a with a
		// expected: a
		EdgeHistory match = b()
				.withEdge().fromRoot().to("H1").name("a").buildEdge()
				.buildHistory();
		EdgeHistory query = match;
		EdgeHistory expected = match;
		History result = match.sequenceIntersect(query);
		assertTrue(expected.equalContent(result));
	}
	
	@Test
	public void testIntersection2() throws InterruptedException, CanceledException {
		// a(b|c) with ab
		// expected: ab
		EdgeHistory match = b()
				.withEdge().fromRoot().to("H1").name("a").buildEdge()
				.withEdge().from("H1").to("H2").name("b").buildEdge()
				.withEdge().from("H1").to("H3").name("c").buildEdge()
				.buildHistory();
		EdgeHistory query = b()
				.withEdge().fromRoot().to("H1").name("a").buildEdge()
				.withEdge().from("H1").to("H2").name("b").buildEdge()
				.buildHistory();
		EdgeHistory expected = query;
		History result = match.sequenceIntersect(query);
		assertTrue(expected.equalContent(result));
	}
	
	@Test
	public void testIntersection3() throws InterruptedException, CanceledException {
		// abc with ac
		// expected: abc
		EdgeHistory match = b()
				.withEdge().fromRoot().to("H1").name("a").buildEdge()
				.withEdge().from("H1").to("H2").name("b").buildEdge()
				.withEdge().from("H2").to("H3").name("c").buildEdge()
				.buildHistory();
		EdgeHistory query = b()
				.withEdge().fromRoot().to("H1").name("a").buildEdge()
				.withEdge().from("H1").to("H2").name("c").buildEdge()
				.buildHistory();
		EdgeHistory expected = match;
		History result = match.sequenceIntersect(query);
		assertTrue(expected.equalContent(result));
	}
	
	@Test
	public void testIntersection4() throws InterruptedException, CanceledException {
		// abc with a?c
		// expected: abc
		EdgeHistory match = b()
				.withEdge().fromRoot().to("H1").name("a").buildEdge()
				.withEdge().from("H1").to("H2").name("b").buildEdge()
				.withEdge().from("H2").to("H3").name("c").buildEdge()
				.buildHistory();
		EdgeHistory query = b()
				.withEdge().fromRoot().to("H1").name("a").buildEdge()
				.withEdge().from("H1").to("H2").buildEdge()
				.withEdge().from("H2").to("H3").name("c").buildEdge()
				.buildHistory();
		EdgeHistory expected = match;
		History result = match.sequenceIntersect(query);
		assertTrue(expected.equalContent(result));
	}
	
	@Test
	public void testIntersection5() throws InterruptedException, CanceledException {
		// a(b|c)e(f|g)h with ?b?f?
		// expected: abefh
		EdgeHistory match = b()
				.withEdge().fromRoot().to("H1").name("a").buildEdge()
				.withEdge().from("H1").to("H2").name("b").buildEdge()
				.withEdge().from("H1").to("H3").name("c").buildEdge()
				.withEdge().from("H2").to("H4").name("e").buildEdge()
				.withEdge().from("H3").to("H4").name("e").buildEdge()
				.withEdge().from("H4").to("H5").name("f").buildEdge()
				.withEdge().from("H4").to("H6").name("g").buildEdge()
				.withEdge().from("H5").to("H7").name("h").buildEdge()
				.withEdge().from("H6").to("H7").name("h").buildEdge()
				.buildHistory();
		EdgeHistory query = b()
				.withEdge().fromRoot().to("H1").buildEdge()
				.withEdge().from("H1").to("H2").name("b").buildEdge()
				.withEdge().from("H2").to("H3").buildEdge()
				.withEdge().from("H3").to("H4").name("f").buildEdge()
				.withEdge().from("H4").to("H5").buildEdge()
				.buildHistory();
		EdgeHistory expected = b()
				.withEdge().fromRoot().to("H1").name("a").buildEdge()
				.withEdge().from("H1").to("H2").name("b").buildEdge()
				.withEdge().from("H2").to("H4").name("e").buildEdge()
				.withEdge().from("H4").to("H5").name("f").buildEdge()
				.withEdge().from("H5").to("H7").name("h").buildEdge()
				.buildHistory();
		History result = match.sequenceIntersect(query);
		assertTrue(expected.equalContent(result));
	}
	
	@Test
	public void testIntersection6() throws InterruptedException, CanceledException {
		// (a|b)c with a
		// expected: ac
		EdgeHistory match = b()
				.withEdge().fromRoot().to("H1").name("a").buildEdge()
				.withEdge().fromRoot().to("H2").name("b").buildEdge()
				.withEdge().from("H1").to("H3").name("c").buildEdge()
				.withEdge().from("H2").to("H3").name("c").buildEdge()
				.buildHistory();
		EdgeHistory query = b()
				.withEdge().fromRoot().to("H1").name("a").buildEdge()
				.buildHistory();
		EdgeHistory expected = b()
				.withEdge().fromRoot().to("H1").name("a").buildEdge()
				.withEdge().from("H1").to("H2").name("c").buildEdge()
				.buildHistory();
		History result = match.sequenceIntersect(query);
		assertTrue(expected.equalContent(result));
	}
}
