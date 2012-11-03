package technion.prime.history.edgeset;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import technion.prime.DefaultOptions;
import technion.prime.utils.Logger.CanceledException;

public class InclusionNoUnknownTest {
	private DefaultOptions options;

	@Before
	public void setUp() {
		options = new DefaultOptions();
	}
	
	private EdgeHistoryBuilder b() {
		return new EdgeHistoryBuilder(options);
	}
	
	@Test
	public void testInclusionNoUnknown1() throws InterruptedException, CanceledException {
		// a < a
		EdgeHistory including = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.buildHistory();
		EdgeHistory included = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.buildHistory();
		assertTrue(including.includesWithUnknown(included));
	}
	
	@Test
	public void testInclusionNoUnknown2() throws InterruptedException, CanceledException {
		// a < ab
		EdgeHistory including = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").name("b").buildEdge()
				.buildHistory();
		EdgeHistory included = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.buildHistory();
		assertTrue(including.includesWithUnknown(included));
	}
	
	@Test
	public void testInclusionNoUnknown3() throws InterruptedException, CanceledException {
		// a < a?
		EdgeHistory including = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").buildEdge()
				.buildHistory();
		EdgeHistory included = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.buildHistory();
		assertTrue(including.includesWithUnknown(included));
	}
	
	@Test
	public void testInclusionNoUnknown4() throws InterruptedException, CanceledException {
		// a? < a
		EdgeHistory including = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.buildHistory();
		EdgeHistory included = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").buildEdge()
				.buildHistory();
		assertTrue(including.includesWithUnknown(included));
	}
	
	@Test
	public void testInclusionNoUnknown5() throws InterruptedException, CanceledException {
		// a? < ab
		EdgeHistory including = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").name("b").buildEdge()
				.buildHistory();
		EdgeHistory included = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").buildEdge()
				.buildHistory();
		assertTrue(including.includesWithUnknown(included));
	}
	
	@Test
	public void testInclusionNoUnknown6() throws InterruptedException, CanceledException {
		// a?c < abc
		EdgeHistory including = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").name("b").buildEdge()
				.withEdge().from("H1").to("H2").name("c").buildEdge()
				.buildHistory();
		EdgeHistory included = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").buildEdge()
				.withEdge().from("H1").to("H2").name("c").buildEdge()
				.buildHistory();
		assertTrue(including.includesWithUnknown(included));
	}
	
	@Test
	public void testInclusionNoUnknown7() throws InterruptedException, CanceledException {
		// a?c < ac
		EdgeHistory including = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").name("c").buildEdge()
				.buildHistory();
		EdgeHistory included = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").buildEdge()
				.withEdge().from("H1").to("H2").name("c").buildEdge()
				.buildHistory();
		assertTrue(including.includesWithUnknown(included));
	}
	
	@Test
	public void testInclusionNoUnknown8() throws InterruptedException, CanceledException {
		// a?d < abcd
		EdgeHistory including = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").name("b").buildEdge()
				.withEdge().from("H1").to("H2").name("c").buildEdge()
				.withEdge().from("H2").to("H3").name("d").buildEdge()
				.buildHistory();
		EdgeHistory included = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").buildEdge()
				.withEdge().from("H1").to("H2").name("d").buildEdge()
				.buildHistory();
		assertTrue(including.includesWithUnknown(included));
	}
	
	@Test
	public void testInclusionNoUnknown9() throws InterruptedException, CanceledException {
		// ?a < ab
		EdgeHistory including = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").name("b").buildEdge()
				.buildHistory();
		EdgeHistory included = b()
				.withEdge().fromRoot().to("H0").buildEdge()
				.withEdge().from("H0").to("H1").name("a").buildEdge()
				.buildHistory();
		assertTrue(including.includesWithUnknown(included));
	}
	
	@Test
	public void testInclusionNoUnknown10() throws InterruptedException, CanceledException {
		// ?a < a
		EdgeHistory including = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.buildHistory();
		EdgeHistory included = b()
				.withEdge().fromRoot().to("H0").buildEdge()
				.withEdge().from("H0").to("H1").name("a").buildEdge()
				.buildHistory();
		assertTrue(including.includesWithUnknown(included));
	}
	
	@Test
	public void testInclusionNoUnknown11() throws InterruptedException, CanceledException {
		// abc /< a?c
		EdgeHistory including = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").buildEdge()
				.withEdge().from("H1").to("H2").name("c").buildEdge()
				.buildHistory();
		EdgeHistory included = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").name("b").buildEdge()
				.withEdge().from("H1").to("H2").name("c").buildEdge()
				.buildHistory();
		assertFalse(including.includesWithUnknown(included));
	}
	
	@Test
	public void testInclusionNoUnknown12() throws InterruptedException, CanceledException {
		// a /< b
		EdgeHistory including = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.buildHistory();
		EdgeHistory included = b()
				.withEdge().fromRoot().to("H0").name("b").buildEdge()
				.buildHistory();
		assertFalse(including.includesWithUnknown(included));
	}
	
	@Test
	public void testInclusionNoUnknown13() throws InterruptedException, CanceledException {
		// ?b < ab
		EdgeHistory including = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").name("b").buildEdge()
				.buildHistory();
		EdgeHistory included = b()
				.withEdge().fromRoot().to("H0").buildEdge()
				.withEdge().from("H0").to("H1").name("b").buildEdge()
				.buildHistory();
		assertTrue(including.includesWithUnknown(included));
	}
	
}
