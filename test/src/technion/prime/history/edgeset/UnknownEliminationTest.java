package technion.prime.history.edgeset;

import org.junit.Before;
import org.junit.Test;

import technion.prime.DefaultOptions;
import technion.prime.history.History;
import technion.prime.history.HistoryCollection;
import technion.prime.utils.Logger;
import technion.prime.utils.Logger.CanceledException;
import static org.junit.Assert.assertTrue;

public class UnknownEliminationTest {
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
	public void testUnknownElimination1() throws InterruptedException, CanceledException {
		// a?c to {abc}
		// expect abc
		History query = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").buildEdge()
				.withEdge().from("H1").to("H2").name("c").buildEdge()
				.buildHistory();
		History baseHistory = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").name("b").buildEdge()
				.withEdge().from("H1").to("H2").name("c").buildEdge()
				.buildHistory();
		History expected = baseHistory;
		HistoryCollection base = options.newHistoryCollection();
		base.addHistory(baseHistory);
		History result = query.eliminateUnknowns(base);
		assertTrue(result.equalContent(expected));
	}
	
	@Test
	public void testUnknownElimination2() throws InterruptedException, CanceledException {
		// a?d to {abcd}
		// expect abcd
		History query = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").buildEdge()
				.withEdge().from("H1").to("H2").name("d").buildEdge()
				.buildHistory();
		History baseHistory = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").name("b").buildEdge()
				.withEdge().from("H1").to("H2").name("c").buildEdge()
				.withEdge().from("H2").to("H3").name("d").buildEdge()
				.buildHistory();
		History expected = baseHistory;
		HistoryCollection base = options.newHistoryCollection();
		base.addHistory(baseHistory);
		History result = query.eliminateUnknowns(base);
		assertTrue(result.equalContent(expected));
	}
	
	@Test
	public void testUnknownElimination3() throws InterruptedException, CanceledException {
		// a?b to {ab}
		// expect ab
		History query = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").buildEdge()
				.withEdge().from("H1").to("H2").name("b").buildEdge()
				.buildHistory();
		History baseHistory = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").name("b").buildEdge()
				.buildHistory();
		History expected = baseHistory;
		HistoryCollection base = options.newHistoryCollection();
		base.addHistory(baseHistory);
		History result = query.eliminateUnknowns(base);
		assertTrue(result.equalContent(expected));
	}
	
	@Test
	public void testUnknownElimination4() throws InterruptedException, CanceledException {
		// a?d to {a(b|c)d}
		// expect a(b|c)d
		History query = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").buildEdge()
				.withEdge().from("H1").to("H2").name("d").buildEdge()
				.buildHistory();
		History baseHistory = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").name("b").buildEdge()
				.withEdge().from("H1").to("H2").name("d").buildEdge()
				.withEdge().from("H0").to("H3").name("c").buildEdge()
				.withEdge().from("H3").to("H2").name("d").buildEdge()
				.buildHistory();
		History expected = baseHistory;
		HistoryCollection base = options.newHistoryCollection();
		base.addHistory(baseHistory);
		History result = query.eliminateUnknowns(base);
		assertTrue(result.equalContent(expected));
	}
	
	@Test
	public void testUnknownElimination5() throws InterruptedException, CanceledException {
		// a?c to {a(bc|de)}
		// expect abc
		History query = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").buildEdge()
				.withEdge().from("H1").to("H2").name("c").buildEdge()
				.buildHistory();
		History baseHistory = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").name("b").buildEdge()
				.withEdge().from("H1").to("H2").name("c").buildEdge()
				.withEdge().from("H0").to("H3").name("d").buildEdge()
				.withEdge().from("H3").to("H4").name("e").buildEdge()
				.buildHistory();
		History expected = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").name("b").buildEdge()
				.withEdge().from("H1").to("H2").name("c").buildEdge()
				.buildHistory();
		HistoryCollection base = options.newHistoryCollection();
		base.addHistory(baseHistory);
		History result = query.eliminateUnknowns(base);
		assertTrue(result.equalContent(expected));
	}
	
	@Test
	public void testUnknownElimination6() throws InterruptedException, CanceledException {
		// a? to {abc}
		// expect abc
		History query = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").buildEdge()
				.buildHistory();
		History baseHistory = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").name("b").buildEdge()
				.withEdge().from("H1").to("H2").name("c").buildEdge()
				.buildHistory();
		History expected = baseHistory;
		HistoryCollection base = options.newHistoryCollection();
		base.addHistory(baseHistory);
		History result = query.eliminateUnknowns(base);
		assertTrue(result.equalContent(expected));
	}
	
	@Test
	public void testUnknownElimination7() throws InterruptedException, CanceledException {
		// ?c to {abc}
		// expect abc
		History query = b()
				.withEdge().fromRoot().to("H0").buildEdge()
				.withEdge().from("H0").to("H1").name("c").buildEdge()
				.buildHistory();
		History baseHistory = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").name("b").buildEdge()
				.withEdge().from("H1").to("H2").name("c").buildEdge()
				.buildHistory();
		History expected = baseHistory;
		HistoryCollection base = options.newHistoryCollection();
		base.addHistory(baseHistory);
		History result = query.eliminateUnknowns(base);
		assertTrue(result.equalContent(expected));
	}
	
	@Test
	public void testUnknownElimination8() throws InterruptedException, CanceledException {
		// ? to {abc}
		// expect ?
		History query = b()
				.withEdge().fromRoot().to("H0").buildEdge()
				.buildHistory();
		History baseHistory = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").name("b").buildEdge()
				.withEdge().from("H1").to("H2").name("c").buildEdge()
				.buildHistory();
		History expected = query;
		HistoryCollection base = options.newHistoryCollection();
		base.addHistory(baseHistory);
		History result = query.eliminateUnknowns(base);
		assertTrue(result.equalContent(expected));
	}
	
	@Test
	public void testUnknownElimination9() throws InterruptedException, CanceledException {
		// a?c to {a?c}
		// expect a?c
		History query = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").buildEdge()
				.withEdge().from("H1").to("H2").name("c").buildEdge()
				.buildHistory();
		History baseHistory = query;
		History expected = query;
		HistoryCollection base = options.newHistoryCollection();
		base.addHistory(baseHistory);
		History result = query.eliminateUnknowns(base);
		assertTrue(result.equalContent(expected));
	}
	
	@Test
	public void testUnknownElimination10() throws InterruptedException, CanceledException {
		// abc to {a?c}
		// expect abc
		History query = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").name("b").buildEdge()
				.withEdge().from("H1").to("H2").name("c").buildEdge()
				.buildHistory();
		History baseHistory = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").buildEdge()
				.withEdge().from("H1").to("H2").name("c").buildEdge()
				.buildHistory();
		History expected = query;
		HistoryCollection base = options.newHistoryCollection();
		base.addHistory(baseHistory);
		History result = query.eliminateUnknowns(base);
		assertTrue(result.equalContent(expected));
	}
	
	@Test
	public void testUnknownElimination11() throws InterruptedException, CanceledException {
		// a?d to {abd, acd}
		// expect a(b|c)d
		History query = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").buildEdge()
				.withEdge().from("H1").to("H2").name("d").buildEdge()
				.buildHistory();
		History baseHistory1 = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").name("b").buildEdge()
				.withEdge().from("H1").to("H2").name("d").buildEdge()
				.buildHistory();
		History baseHistory2 = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").name("c").buildEdge()
				.withEdge().from("H1").to("H2").name("d").buildEdge()
				.buildHistory();
		History expected = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").name("b").buildEdge()
				.withEdge().from("H1").to("H2").name("d").buildEdge()
				.withEdge().from("H0").to("H3").name("c").buildEdge()
				.withEdge().from("H3").to("H2").name("d").buildEdge()
				.buildHistory();
		HistoryCollection base = options.newHistoryCollection();
		base.addHistory(baseHistory1);
		base.addHistory(baseHistory2);
		History result = query.eliminateUnknowns(base);
		assertTrue(result.equalContent(expected));
	}
	
	@Test
	public void testUnknownElimination12() throws InterruptedException, CanceledException {
		// a?c?e to {abcde}
		// expect abcde
		History query = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").buildEdge()
				.withEdge().from("H1").to("H2").name("c").buildEdge()
				.withEdge().from("H2").to("H3").buildEdge()
				.withEdge().from("H3").to("H4").name("e").buildEdge()
				.buildHistory();
		History baseHistory = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").name("b").buildEdge()
				.withEdge().from("H1").to("H2").name("c").buildEdge()
				.withEdge().from("H2").to("H3").name("d").buildEdge()
				.withEdge().from("H3").to("H4").name("e").buildEdge()
				.buildHistory();
		History expected = baseHistory;
		HistoryCollection base = options.newHistoryCollection();
		base.addHistory(baseHistory);
		History result = query.eliminateUnknowns(base);
		assertTrue(result.equalContent(expected));
	}
	
	@Test
	public void testUnknownElimination13() throws InterruptedException, CanceledException {
		// a?c?e to {abc, cde}
		// expect abcde
		History query = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").buildEdge()
				.withEdge().from("H1").to("H2").name("c").buildEdge()
				.withEdge().from("H2").to("H3").buildEdge()
				.withEdge().from("H3").to("H4").name("e").buildEdge()
				.buildHistory();
		History baseHistory1 = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").name("b").buildEdge()
				.withEdge().from("H1").to("H2").name("c").buildEdge()
				.buildHistory();
		History baseHistory2 = b()
				.withEdge().fromRoot().to("H0").name("c").buildEdge()
				.withEdge().from("H0").to("H1").name("d").buildEdge()
				.withEdge().from("H1").to("H2").name("e").buildEdge()
				.buildHistory();
		History expected = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").name("b").buildEdge()
				.withEdge().from("H1").to("H2").name("c").buildEdge()
				.withEdge().from("H2").to("H3").name("d").buildEdge()
				.withEdge().from("H3").to("H4").name("e").buildEdge()
				.buildHistory();
		HistoryCollection base = options.newHistoryCollection();
		base.addHistory(baseHistory1);
		base.addHistory(baseHistory2);
		History result = query.eliminateUnknowns(base);
		assertTrue(result.equalContent(expected));
	}
	
	@Test
	public void testUnknownElimination14() throws InterruptedException, CanceledException {
		// ?(b|c) to {ab}
		// expect ab|?c
		History query = b()
				.withEdge().fromRoot().to("H0").buildEdge()
				.withEdge().from("H0").to("H1").name("b").buildEdge()
				.withEdge().from("H0").to("H2").name("c").buildEdge()
				.buildHistory();
		History baseHistory = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").name("b").buildEdge()
				.buildHistory();
		History expected = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").name("b").buildEdge()
				.withEdge().fromRoot().to("H2").buildEdge()
				.withEdge().from("H2").to("H3").name("c").buildEdge()
				.buildHistory();
		HistoryCollection base = options.newHistoryCollection();
		base.addHistory(baseHistory);
		History result = query.eliminateUnknowns(base);
		assertTrue(result.equalContent(expected));
	}
	
	@Test
	public void testUnknownElimination15() throws InterruptedException, CanceledException {
		// a?(c|d) to {abc}
		// expect a(bc|?d)
		History query = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").buildEdge()
				.withEdge().from("H1").to("H2").name("c").buildEdge()
				.withEdge().from("H1").to("H3").name("d").buildEdge()
				.buildHistory();
		History baseHistory = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").name("b").buildEdge()
				.withEdge().from("H1").to("H2").name("c").buildEdge()
				.buildHistory();
		History expected = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").name("b").buildEdge()
				.withEdge().from("H1").to("H2").name("c").buildEdge()
				.withEdge().from("H0").to("H3").buildEdge()
				.withEdge().from("H3").to("H4").name("d").buildEdge()
				.buildHistory();
		HistoryCollection base = options.newHistoryCollection();
		base.addHistory(baseHistory);
		History result = query.eliminateUnknowns(base);
		assertTrue(result.equalContent(expected));
	}
	
	@Test
	public void testUnknownElimination16() throws InterruptedException, CanceledException {
		// a?c to {ab}
		// expect a?c
		History query = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").buildEdge()
				.withEdge().from("H1").to("H2").name("c").buildEdge()
				.buildHistory();
		History baseHistory = b()
				.withEdge().fromRoot().to("H0").name("a").buildEdge()
				.withEdge().from("H0").to("H1").name("b").buildEdge()
				.buildHistory();
		History expected = query;
		HistoryCollection base = options.newHistoryCollection();
		base.addHistory(baseHistory);
		History result = query.eliminateUnknowns(base);
		assertTrue(result.equalContent(expected));
	}
	
	@Test
	public void testWeights1() throws InterruptedException, CanceledException {
		// a*3 ?*4 c*5 to {a*6 b*7 c*8}
		// expect a*3 b*5 c*5
		History query = b()
				.withEdge().fromRoot().to("H0").weight(3).name("a").buildEdge()
				.withEdge().from("H0").to("H1").weight(4).buildEdge()
				.withEdge().from("H1").to("H2").weight(5).name("c").buildEdge()
				.buildHistory();
		History baseHistory = b()
				.withEdge().fromRoot().to("H0").weight(6).name("a").buildEdge()
				.withEdge().from("H0").to("H1").weight(7).name("b").buildEdge()
				.withEdge().from("H1").to("H2").weight(8).name("c").buildEdge()
				.buildHistory();
		History expected = b()
				.withEdge().fromRoot().to("H0").weight(3).name("a").buildEdge()
				.withEdge().from("H0").to("H1").weight(5).name("b").buildEdge()
				.withEdge().from("H1").to("H2").weight(5).name("c").buildEdge()
				.buildHistory();
		HistoryCollection base = options.newHistoryCollection();
		base.addHistory(baseHistory);
		History result = query.eliminateUnknowns(base);
		assertTrue(result.equalContent(expected));
	}
	
	@Test
	public void testWeights2() throws InterruptedException, CanceledException {
		// a*3 ?*4 to {a*6 b*7 c*8}
		// expect a*3 b*4 c*4
		History query = b()
				.withEdge().fromRoot().to("H0").weight(3).name("a").buildEdge()
				.withEdge().from("H0").to("H1").weight(4).buildEdge()
				.buildHistory();
		History baseHistory = b()
				.withEdge().fromRoot().to("H0").weight(6).name("a").buildEdge()
				.withEdge().from("H0").to("H1").weight(7).name("b").buildEdge()
				.withEdge().from("H1").to("H2").weight(8).name("c").buildEdge()
				.buildHistory();
		History expected = b()
				.withEdge().fromRoot().to("H0").weight(3).name("a").buildEdge()
				.withEdge().from("H0").to("H1").weight(4).name("b").buildEdge()
				.withEdge().from("H1").to("H2").weight(4).name("c").buildEdge()
				.buildHistory();
		HistoryCollection base = options.newHistoryCollection();
		base.addHistory(baseHistory);
		History result = query.eliminateUnknowns(base);
		assertTrue(result.equalContent(expected));
	}
	
	@Test
	public void testWeights3() throws InterruptedException, CanceledException {
		// ?*5 c*4 to {a*6 b*7 c*8}
		// expect a*4 b*4 c*4
		History query = b()
				.withEdge().fromRoot().to("H0").weight(5).buildEdge()
				.withEdge().from("H0").to("H1").weight(4).name("c").buildEdge()
				.buildHistory();
		History baseHistory = b()
				.withEdge().fromRoot().to("H0").weight(6).name("a").buildEdge()
				.withEdge().from("H0").to("H1").weight(7).name("b").buildEdge()
				.withEdge().from("H1").to("H2").weight(8).name("c").buildEdge()
				.buildHistory();
		History expected = b()
				.withEdge().fromRoot().to("H0").weight(4).name("a").buildEdge()
				.withEdge().from("H0").to("H1").weight(4).name("b").buildEdge()
				.withEdge().from("H1").to("H2").weight(4).name("c").buildEdge()
				.buildHistory();
		HistoryCollection base = options.newHistoryCollection();
		base.addHistory(baseHistory);
		History result = query.eliminateUnknowns(base);
		assertTrue(result.equalContent(expected));
	}
	
	@Test
	public void testWeights4() throws InterruptedException, CanceledException {
		// ?*5 (b*4 | d*3) to {a*6 b*7, c*8 d*9}
		// expect (a*4 b*4 | c*3 d*3)
		History query = b()
				.withEdge().fromRoot().to("H0").weight(5).buildEdge()
				.withEdge().from("H0").to("H1").weight(4).name("b").buildEdge()
				.withEdge().from("H0").to("H2").weight(3).name("d").buildEdge()
				.buildHistory();
		History baseHistory1 = b()
				.withEdge().fromRoot().to("H0").weight(6).name("a").buildEdge()
				.withEdge().from("H0").to("H1").weight(7).name("b").buildEdge()
				.buildHistory();
		History baseHistory2 = b()
				.withEdge().fromRoot().to("H0").weight(8).name("c").buildEdge()
				.withEdge().from("H0").to("H1").weight(9).name("d").buildEdge()
				.buildHistory();
		History expected = b()
				.withEdge().fromRoot().to("H0").weight(4).name("a").buildEdge()
				.withEdge().from("H0").to("H1").weight(4).name("b").buildEdge()
				.withEdge().fromRoot().to("H2").weight(3).name("c").buildEdge()
				.withEdge().from("H2").to("H3").weight(3).name("d").buildEdge()
				.buildHistory();
		HistoryCollection base = options.newHistoryCollection();
		base.addHistory(baseHistory1);
		base.addHistory(baseHistory2);
		History result = query.eliminateUnknowns(base);
		assertTrue(result.equalContent(expected));
	}
	
	@Test
	public void testWeights5() throws InterruptedException, CanceledException {
		// a*3 ?*4 d*5 to {a*6 b*7 d*8, a*9 b*10 d*11, a*12 c*13 d*14}
		// expect a*3 (b*5 d*5 | c*5 d*5)
		History query = b()
				.withEdge().fromRoot().to("H0").weight(3).name("a").buildEdge()
				.withEdge().from("H0").to("H1").weight(4).buildEdge()
				.withEdge().from("H1").to("H2").weight(5).name("d").buildEdge()
				.buildHistory();
		History baseHistory1 = b()
				.withEdge().fromRoot().to("H0").weight(6).name("a").buildEdge()
				.withEdge().from("H0").to("H1").weight(7).name("b").buildEdge()
				.withEdge().from("H1").to("H2").weight(8).name("d").buildEdge()
				.buildHistory();
		History baseHistory2 = b()
				.withEdge().fromRoot().to("H0").weight(9).name("a").buildEdge()
				.withEdge().from("H0").to("H1").weight(10).name("b").buildEdge()
				.withEdge().from("H1").to("H2").weight(11).name("d").buildEdge()
				.buildHistory();
		History baseHistory3 = b()
				.withEdge().fromRoot().to("H0").weight(12).name("a").buildEdge()
				.withEdge().from("H0").to("H1").weight(13).name("c").buildEdge()
				.withEdge().from("H1").to("H2").weight(14).name("d").buildEdge()
				.buildHistory();
		History expected = b()
				.withEdge().fromRoot().to("H0").weight(3).name("a").buildEdge()
				.withEdge().from("H0").to("H1").weight(5).name("b").buildEdge()
				.withEdge().from("H0").to("H2").weight(5).name("c").buildEdge()
				.withEdge().from("H1").to("H3").weight(5).name("d").buildEdge()
				.withEdge().from("H2").to("H3").weight(5).name("d").buildEdge()
				.buildHistory();
		HistoryCollection base = options.newHistoryCollection();
		base.addHistory(baseHistory1);
		base.addHistory(baseHistory2);
		base.addHistory(baseHistory3);
		History result = query.eliminateUnknowns(base);
		assertTrue(result.equalContent(expected));
	}
	
}
