package technion.prime.history.edgeset;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import technion.prime.DefaultOptions;
import technion.prime.utils.Logger;
import technion.prime.utils.Logger.CanceledException;

public class MethodSequenceTest {
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
	
	private EdgeSequenceCollectionBuilder e() {
		return new EdgeSequenceCollectionBuilder();
	}
	
	private void assertEquivalentSequenceCollections(
			Collection<EdgeSequence> expected,
			Iterable<EdgeSequence> result) {
		assertEquals(expected.size(), size(result));
		Set<EdgeSequence> matched = new HashSet<EdgeSequence>();
		for (EdgeSequence expectedSequence : expected) {
			boolean found = false;
			for (EdgeSequence resultSequence : result) {
				found = expectedSequence.sameMethods(resultSequence);
				if (found) {
					matched.add(expectedSequence);
					break;
				}
			}
			assertTrue(found);
		}
		assertTrue(matched.containsAll(expected));
	}
	
	private int size(Iterable<?> iterable) {
		int result = 0;
		Iterator<?> iter = iterable.iterator();
		while (iter.hasNext()) {
			result++;
			iter.next();
		}
		return result;
	}

	@Test
	public void testSingleton() throws InterruptedException, CanceledException {
		// input: a
		// expected: {a}
		EdgeHistory input = b()
				.withEdge().fromRoot().to("H1").name("a").buildEdge()
				.buildHistory();
		Collection<EdgeSequence> expected = e()
				.withHistory(b()
					.withEdge().fromRoot().to("H1").name("a").buildEdge()
					.buildHistory())
				.buildSequenceCollection();
		Iterable<EdgeSequence> result = input.buildMethodSequences(1, -1, null);
		assertEquivalentSequenceCollections(expected, result);
	}
	
	@Test
	public void testBranch() throws InterruptedException, CanceledException {
		// input: a(b|c)
		// expected: {ab, ac}
		EdgeHistory input = b()
				.withEdge().fromRoot().to("H1").name("a").buildEdge()
				.withEdge().from("H1").to("H2").name("b").buildEdge()
				.withEdge().from("H1").to("H3").name("c").buildEdge()
				.buildHistory();
		Collection<EdgeSequence> expected = e()
				.withHistory(b()
					.withEdge().fromRoot().to("H1").name("a").buildEdge()
					.withEdge().from("H1").to("H2").name("b").buildEdge()
					.buildHistory())
				.withHistory(b()
					.withEdge().fromRoot().to("H1").name("a").buildEdge()
					.withEdge().from("H1").to("H2").name("c").buildEdge()
					.buildHistory())
				.buildSequenceCollection();
		Iterable<EdgeSequence> result = input.buildMethodSequences(1, -1, null);
		assertEquivalentSequenceCollections(expected, result);
	}
	
	@Test
	public void testBranchAndConverge() throws InterruptedException, CanceledException {
		// input: a(b|c)d
		// expected: {abd, acd}
		EdgeHistory input = b()
				.withEdge().fromRoot().to("H1").name("a").buildEdge()
				.withEdge().from("H1").to("H2").name("b").buildEdge()
				.withEdge().from("H1").to("H3").name("c").buildEdge()
				.withEdge().from("H2").to("H4").name("d").buildEdge()
				.withEdge().from("H3").to("H4").name("d").buildEdge()
				.buildHistory();
		Collection<EdgeSequence> expected = e()
				.withHistory(b()
					.withEdge().fromRoot().to("H1").name("a").buildEdge()
					.withEdge().from("H1").to("H2").name("b").buildEdge()
					.withEdge().from("H2").to("H3").name("d").buildEdge()
					.buildHistory())
				.withHistory(b()
					.withEdge().fromRoot().to("H1").name("a").buildEdge()
					.withEdge().from("H1").to("H2").name("c").buildEdge()
					.withEdge().from("H2").to("H3").name("d").buildEdge()
					.buildHistory())
				.buildSequenceCollection();
		Iterable<EdgeSequence> result = input.buildMethodSequences(1, -1, null);
		assertEquivalentSequenceCollections(expected, result);
	}
	
	@Test
	public void testDoubleBranchAndConverge() throws InterruptedException, CanceledException {
		// input: a(b|c)d(e|f)g
		// expected: {abdeg, abdfg, acdeg, acdfg}
		EdgeHistory input = b()
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
		Collection<EdgeSequence> expected = e()
				.withHistory(b()
					.withEdge().fromRoot().to("H1").name("a").buildEdge()
					.withEdge().from("H1").to("H2").name("b").buildEdge()
					.withEdge().from("H2").to("H3").name("d").buildEdge()
					.withEdge().from("H3").to("H4").name("e").buildEdge()
					.withEdge().from("H4").to("H5").name("g").buildEdge()
					.buildHistory())
				.withHistory(b()
					.withEdge().fromRoot().to("H1").name("a").buildEdge()
					.withEdge().from("H1").to("H2").name("b").buildEdge()
					.withEdge().from("H2").to("H3").name("d").buildEdge()
					.withEdge().from("H3").to("H4").name("f").buildEdge()
					.withEdge().from("H4").to("H5").name("g").buildEdge()
					.buildHistory())
				.withHistory(b()
					.withEdge().fromRoot().to("H1").name("a").buildEdge()
					.withEdge().from("H1").to("H2").name("c").buildEdge()
					.withEdge().from("H2").to("H3").name("d").buildEdge()
					.withEdge().from("H3").to("H4").name("e").buildEdge()
					.withEdge().from("H4").to("H5").name("g").buildEdge()
					.buildHistory())
				.withHistory(b()
					.withEdge().fromRoot().to("H1").name("a").buildEdge()
					.withEdge().from("H1").to("H2").name("c").buildEdge()
					.withEdge().from("H2").to("H3").name("d").buildEdge()
					.withEdge().from("H3").to("H4").name("f").buildEdge()
					.withEdge().from("H4").to("H5").name("g").buildEdge()
					.buildHistory())
				.buildSequenceCollection();
		Iterable<EdgeSequence> result = input.buildMethodSequences(1, -1, null);
		assertEquivalentSequenceCollections(expected, result);
	}
	
	@Test
	public void testLoop() throws InterruptedException, CanceledException {
		// input: aba
		// expected: {aba}
		EdgeHistory input = b()
				.withEdge().fromRoot().to("H1").name("a").buildEdge()
				.withEdge().from("H1").to("H2").name("b").buildEdge()
				.withEdge().from("H2").to("H1").name("a").buildEdge()
				.buildHistory();
		Collection<EdgeSequence> expected = e()
				.withHistory(b()
					.withEdge().fromRoot().to("H1").name("a").buildEdge()
					.withEdge().from("H1").to("H2").name("b").buildEdge()
					.withEdge().from("H2").to("H3").name("a").buildEdge()
					.buildHistory())
				.buildSequenceCollection();
		Iterable<EdgeSequence> result = input.buildMethodSequences(1, -1, null);
		assertEquivalentSequenceCollections(expected, result);
	}
	
	@Test
	public void testLoopWithBranch() throws InterruptedException, CanceledException {
		// input: ab(a|c)
		// expected: {abc, aba}
		EdgeHistory input = b()
				.withEdge().fromRoot().to("H1").name("a").buildEdge()
				.withEdge().from("H1").to("H2").name("b").buildEdge()
				.withEdge().from("H2").to("H1").name("a").buildEdge()
				.withEdge().from("H2").to("H3").name("c").buildEdge()
				.buildHistory();
		Collection<EdgeSequence> expected = e()
				.withHistory(b()
					.withEdge().fromRoot().to("H1").name("a").buildEdge()
					.withEdge().from("H1").to("H2").name("b").buildEdge()
					.withEdge().from("H2").to("H3").name("c").buildEdge()
					.buildHistory())
				.withHistory(b()
					.withEdge().fromRoot().to("H1").name("a").buildEdge()
					.withEdge().from("H1").to("H2").name("b").buildEdge()
					.withEdge().from("H2").to("H3").name("a").buildEdge()
					.buildHistory())
				.buildSequenceCollection();
		Iterable<EdgeSequence> result = input.buildMethodSequences(1, -1, null);
		assertEquivalentSequenceCollections(expected, result);
	}
	
	@Test
	public void testSelfLoop() throws InterruptedException, CanceledException {
		// input: abbc
		// expected: {abbc, abc}
		EdgeHistory input = b()
				.withEdge().fromRoot().to("H1").name("a").buildEdge()
				.withEdge().from("H1").to("H2").name("b").buildEdge()
				.withEdge().from("H2").to("H2").name("b").buildEdge()
				.withEdge().from("H2").to("H3").name("c").buildEdge()
				.buildHistory();
		Collection<EdgeSequence> expected = e()
				.withSequence(b()
					.withEdge().fromRoot().to("H1").name("a").buildEdge()
					.withEdge().from("H1").to("H2").name("b").buildEdge()
					.withEdge().from("H2").to("H3").name("c").buildEdge()
					.buildSequence())
				.withSequence(b()
					.withEdge().fromRoot().to("H1").name("a").buildEdge()
					.withEdge().from("H1").to("H2").name("b").buildEdge()
					.withEdge().from("H2").to("H3").name("b").buildEdge()
					.withEdge().from("H3").to("H4").name("c").buildEdge()
					.buildSequence())
				.buildSequenceCollection();
		Iterable<EdgeSequence> result = input.buildMethodSequences(1, -1, null);
		assertEquivalentSequenceCollections(expected, result);
	}

}
