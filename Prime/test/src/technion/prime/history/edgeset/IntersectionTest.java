package technion.prime.history.edgeset;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import technion.prime.DefaultOptions;
import technion.prime.utils.Logger;
import technion.prime.utils.Logger.CanceledException;

public class IntersectionTest {
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
		// a to a
		// expect a
		EdgeHistory lhs = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.buildHistory();
		EdgeHistory rhs = lhs;
		EdgeHistory expected = lhs;
		EdgeHistory result = lhs.intersect(rhs);
		assertTrue(result.equalContent(expected));
	}
	
	@Test
	public void testIntersection2() throws InterruptedException, CanceledException {
		// ab to ac
		// expect a
		EdgeHistory lhs = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").name("b").buildEdge()
				.buildHistory();
		EdgeHistory rhs = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").name("c").buildEdge()
				.buildHistory();
		EdgeHistory expected = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.buildHistory();
		EdgeHistory result = lhs.intersect(rhs);
		assertTrue(result.equalContent(expected));
	}
	
	@Test
	public void testIntersection3() throws InterruptedException, CanceledException {
		// a(b|c) to ab
		// expect ab
		EdgeHistory lhs = b()
				.withEdge().fromRoot().to("H1").name("a").buildEdge()
				.withEdge().from("H1").to("H2").name("b").buildEdge()
				.withEdge().from("H1").to("H3").name("c").buildEdge()
				.buildHistory();
		EdgeHistory rhs = b()
				.withEdge().fromRoot().to("H1").name("a").buildEdge()
				.withEdge().from("H1").to("H2").name("b").buildEdge()
				.buildHistory();
		EdgeHistory expected = rhs;
		EdgeHistory result = lhs.intersect(rhs);
		assertTrue(result.equalContent(expected));
	}
	
	@Test
	public void testIntersection4() throws InterruptedException, CanceledException {
		// a(b|c)d to abd
		// expect abd
		EdgeHistory lhs = b()
				.withEdge().fromRoot().to("H1").name("a").buildEdge()
				.withEdge().from("H1").to("H2").name("b").buildEdge()
				.withEdge().from("H1").to("H3").name("c").buildEdge()
				.withEdge().from("H2").to("H4").name("d").buildEdge()
				.withEdge().from("H3").to("H4").name("d").buildEdge()
				.buildHistory();
		EdgeHistory rhs = b()
				.withEdge().fromRoot().to("H1").name("a").buildEdge()
				.withEdge().from("H1").to("H2").name("b").buildEdge()
				.withEdge().from("H2").to("H3").name("d").buildEdge()
				.buildHistory();
		EdgeHistory expected = rhs;
		EdgeHistory result = lhs.intersect(rhs);
		assertTrue(result.equalContent(expected));
	}
	
	@Test
	public void testIntersection5() throws InterruptedException, CanceledException {
		// a(b|c)d(e|f)g to abdeg
		// expect abdeg
		EdgeHistory lhs = b()
				.withEdge().fromRoot().to("H1").name("a").buildEdge()
				.withEdge().from("H1").to("H2").name("b").buildEdge()
				.withEdge().from("H1").to("H3").name("c").buildEdge()
				.withEdge().from("H2").to("H4").name("d").buildEdge()
				.withEdge().from("H3").to("H4").name("d").buildEdge()
				.withEdge().from("H4").to("H5").name("e").buildEdge()
				.withEdge().from("H4").to("H6").name("f").buildEdge()
				.withEdge().from("H5").to("H7").name("g").buildEdge()
				.withEdge().from("H6").to("H7").name("g").buildEdge()
				.buildHistory();
		EdgeHistory rhs = b()
				.withEdge().fromRoot().to("H1").name("a").buildEdge()
				.withEdge().from("H1").to("H2").name("b").buildEdge()
				.withEdge().from("H2").to("H3").name("d").buildEdge()
				.withEdge().from("H3").to("H4").name("e").buildEdge()
				.withEdge().from("H4").to("H5").name("g").buildEdge()
				.buildHistory();
		EdgeHistory expected = rhs;
		EdgeHistory result = lhs.intersect(rhs);
		assertTrue(result.equalContent(expected));
	}
	
	@Test
	public void testIntersectionWithUnknownsParam() throws InterruptedException, CanceledException {
		// a(U) to a(?)
		// expect a(U)
		EdgeHistory lhs = b()
				.withEdge().fromRoot().to("H0").name("a").params("U").buildEdge()
				.buildHistory();
		EdgeHistory rhs = b()
				.withEdge().fromRoot().to("H0").name("a").params("PUNKNOWN.UNKNOWN").buildEdge()
				.buildHistory();
		EdgeHistory expected = lhs;
		EdgeHistory result = lhs.intersect(rhs);
		assertTrue(result.equalContent(expected));
	}
	
	@Test
	public void testIntersectionWithUnknowns1() throws InterruptedException, CanceledException {
		// a?c to abc
		// expect a?
		EdgeHistory lhs = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").buildEdge()
				.withEdge().from("H1").to("H2").name("c").buildEdge()
				.buildHistory();
		EdgeHistory rhs = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").name("b").buildEdge()
				.withEdge().from("H1").to("H2").name("c").buildEdge()
				.buildHistory();
		EdgeHistory expected = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").buildEdge()
				.buildHistory();
		EdgeHistory result = lhs.intersect(rhs);
		assertTrue(result.equalContent(expected));
	}
	
	public void testIntersectionWithUnknowns2() throws InterruptedException, CanceledException {
		// abc to a?c
		// expect a?
		EdgeHistory lhs = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").name("b").buildEdge()
				.withEdge().from("H1").to("H2").name("c").buildEdge()
				.buildHistory();
		EdgeHistory rhs = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").buildEdge()
				.withEdge().from("H1").to("H2").name("c").buildEdge()
				.buildHistory();
		EdgeHistory expected = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").buildEdge()
				.buildHistory();
		EdgeHistory result = lhs.intersect(rhs);
		assertTrue(result.equalContent(expected));
	}
	
	public void testIntersectionWithUnknowns3() throws InterruptedException, CanceledException {
		// a?c to a?c
		// expect a?c
		EdgeHistory lhs = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").name("b").buildEdge()
				.withEdge().from("H1").to("H2").name("c").buildEdge()
				.buildHistory();
		EdgeHistory rhs = lhs;
		EdgeHistory expected = lhs;
		EdgeHistory result = lhs.intersect(rhs);
		assertTrue(result.equalContent(expected));
	}
	
	
	
}
