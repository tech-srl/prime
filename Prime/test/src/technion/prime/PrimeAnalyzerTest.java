package technion.prime;

import java.util.regex.Pattern;

import static technion.prime.history.HistoryTestUtils.assertEqualContentHistories;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import technion.prime.utils.Stage;
import technion.prime.history.edgeset.EdgeHistoryBuilder;
import technion.prime.history.edgeset.EdgeHistoryBuilder.UnknownType;
import technion.prime.history.History;
import technion.prime.utils.StringFilter;
import technion.prime.utils.Logger.CanceledException;
import technion.prime.history.HistoryCollection;


/**
 * Full analysis tests using the expected test files.
 * Does not include clustering.
 */
public class PrimeAnalyzerTest {
	private static String testPath;
	private PrimeAnalyzer analyzer;
	private Options options;
	
	@BeforeClass
	public static void setUpClass() {
		testPath = "test/expected/";
	}
	
	@SuppressWarnings("serial")
	@Before
	public void setUp() {
		final StringFilter opaqueFilter = new StringFilter(
				Pattern.compile("A"), Pattern.compile("^$"), true, false);
		final StringFilter reportedFilter = opaqueFilter;
		options = new DefaultOptions() {
			@Override public StringFilter getFilterOpaqueTypes() { return opaqueFilter; }
			@Override public StringFilter getFilterReported() { return reportedFilter; }
			@Override public long getSingleActionTimeout(Stage stage) { return Integer.MAX_VALUE; }
			@Override public long getStageTimeout(Stage stage) { return Integer.MAX_VALUE; }
			@Override public boolean shouldShowExceptions() { return true; }
			@Override public boolean useHistoryInvariant() { return true; }
			@Override public boolean isSameTypeRequiredForReceiver() {
				// Warning: some of the tests below will not longer work if this is false, and need
				// modification.
				return true;
			}
			@Override public boolean isMayAnalysis() { return false; }
		};
		analyzer = new PrimeAnalyzer(options);
	}
	
	// Auxiliary
	// =========
	
	private EdgeHistoryBuilder b() {
		return new EdgeHistoryBuilder(options);
	}
	
	private HistoryCollection analyze(String className) throws CanceledException {
		analyzer.addInputFile(testPath + className + ".class");
		return analyzer.produceHistoryCollection();
	}
	
	// Tests
	// =====
	
	@Test
	public void testInitOnly() throws CanceledException, InterruptedException {
		String className = "TestInitOnly";
		HistoryCollection result = analyze(className);
		History expected = b()
				.withEdge().fromRoot().to("H1").name("<init>").definedBy("A").buildEdge()
				.buildHistory();
		assertEqualContentHistories(result, expected);
	}
	
	@Test
	public void testInitAndMethods() throws CanceledException, InterruptedException {
		String className = "TestInitAndMethods";
		HistoryCollection result = analyze(className);
		History expected = b()
				.withEdge().fromRoot().to("H1").name("<init>").definedBy("A").buildEdge()
				.withEdge().from("H1").to("H2").name("f").definedBy("A").returningUnknown().buildEdge()
				.withEdge().from("H2").to("H3").name("g").definedBy("A").returningUnknown().buildEdge()
				.buildHistory();
		assertEqualContentHistories(result, expected);
	}
	
	@Test
	public void testMethodOnParameter() throws CanceledException, InterruptedException {
		String className = "TestMethodOnParameter";
		HistoryCollection result = analyze(className);
		
		History expected1 = b()
				.withEdge().fromRoot().to("H1").definedBy("A").unknownType(UnknownType.FROM_PARAMETER).buildEdge()
				.withEdge().from("H1").to("H2").name("f").definedBy("A").returningUnknown().buildEdge()
				.buildHistory();
		assertEqualContentHistories(result, expected1);
	}
	
	@Test
	public void testBranch() throws CanceledException, InterruptedException {
		String className = "TestBranch";
		HistoryCollection result = analyze(className);
		History expected = b()
				.withEdge().fromRoot().to("H1").name("<init>").definedBy("A").buildEdge()
				.withEdge().from("H1").to("H2").name("f").definedBy("A").returningUnknown().buildEdge()
				.withEdge().from("H2").to("H3").name("g1").definedBy("A").returningUnknown().buildEdge()
				.withEdge().from("H2").to("H4").name("g2").definedBy("A").returningUnknown().buildEdge()
				.withEdge().from("H3").to("H5").name("h").definedBy("A").returningUnknown().buildEdge()
				.withEdge().from("H4").to("H5").name("h").definedBy("A").returningUnknown().buildEdge()
				.buildHistory();
		assertEqualContentHistories(result, expected);
	}
	
	@Test
	public void testLoop() throws CanceledException, InterruptedException {
		String className = "TestLoop";
		HistoryCollection result = analyze(className);
		History expected = b()
				.withEdge().fromRoot().to("H1").name("<init>").definedBy("A").buildEdge()
				.withEdge().from("H1").to("H2").name("f").definedBy("A").returningUnknown().buildEdge()
				.withEdge().from("H2").to("H3").name("g").definedBy("A").returningUnknown().buildEdge()
				.withEdge().from("H3").to("H3").name("g").definedBy("A").returningUnknown().buildEdge()
				.withEdge().from("H3").to("H4").name("h").definedBy("A").returningUnknown().buildEdge()
				.withEdge().from("H2").to("H4").name("h").definedBy("A").returningUnknown().buildEdge()
				.buildHistory();
		assertEqualContentHistories(result, expected);
	}
	
	@Test
	public void testSingleCreationContext() throws CanceledException, InterruptedException {
		String className = "TestSingleCreationContext";
		HistoryCollection result = analyze(className);
		History expected = b()
				.withEdge().fromRoot().to("H1").name("<init>").definedBy("B").buildEdge()
				.withEdge().from("H1").to("H2").name("f").definedBy("B").returningUnknown().buildEdge()
				.withEdge().from("H2").to("H3").name("g").definedBy("B").returning("A").buildEdge()
				.withEdge().from("H3").to("H4").name("h").definedBy("A").returningUnknown().buildEdge()
				.buildHistory();
		assertEqualContentHistories(result, expected);
	}
	
	@Test
	public void testPhantomMethod() throws InterruptedException, CanceledException {
		String className = "TestPhantomMethod";
		HistoryCollection result = analyze(className);
		History expected = b()
				.withEdge().fromRoot().to("H1").name("<init>").definedBy("A").buildEdge()
				.withEdge().from("H1").to("H2").name("f").definedBy("A").returningUnknown().buildEdge()
				.withEdge().from("H2").to("H3").definedBy("A").buildEdge()
				.withEdge().from("H3").to("H4").name("g").definedBy("A").returningUnknown().buildEdge()
				.buildHistory();
		assertEqualContentHistories(result, expected);
	}
	
	@Test
	public void testCallsAroundAssignment() throws InterruptedException, CanceledException {
		String className = "TestCallsAroundAssignment";
		HistoryCollection result = analyze(className);
		History expected1 = b()
				.withEdge().fromRoot().to("H1").name("<init>").definedBy("A").buildEdge()
				.withEdge().from("H1").to("H2").name("f").definedBy("A").returningUnknown().buildEdge()
				.buildHistory();
		History expected2 = b()
				.withEdge().fromRoot().to("H1").name("<init>").definedBy("A").buildEdge()
				.withEdge().from("H1").to("H2").name("g").definedBy("A").returningUnknown().buildEdge()
				.withEdge().from("H2").to("H3").name("h").definedBy("A").returningUnknown().buildEdge()
				.buildHistory();
		assertEqualContentHistories(result, expected1, expected2);
	}
	
	@Test
	public void testAssignmentFromOpaque() throws InterruptedException, CanceledException {
		String className = "TestAssignmentFromOpaque";
		HistoryCollection result = analyze(className);
		History expected1 = b()
				.withEdge().fromRoot().to("H1").name("<init>").definedBy("A").buildEdge()
				.withEdge().from("H1").to("H2").name("f").definedBy("A").returningUnknown().buildEdge()
				.withEdge().from("H2").to("H3").name("copy").definedBy("A").returning("A").buildEdge()
				.buildHistory();
		History expected2 = b()
				.withEdge().fromRoot().to("H1").name("<init>").definedBy("A").buildEdge()
				.withEdge().from("H1").to("H2").name("f").definedBy("A").returningUnknown().buildEdge()
				.withEdge().from("H2").to("H3").name("copy").definedBy("A").returning("A").buildEdge()
				.withEdge().from("H3").to("H4").name("g").definedBy("A").returningUnknown().buildEdge()
				.buildHistory();
		assertEqualContentHistories(result, expected1, expected2);
	}
	
	@Test
	public void testLoopAndCondition() throws CanceledException, InterruptedException {
		String className = "TestLoopAndCondition";
		HistoryCollection result = analyze(className);
		// What a is pointed to
		History expected1 = b()
				.withEdge().fromRoot().to("H1").name("<init>").definedBy("A").buildEdge()
				.withEdge().from("H1").to("H2").name("f").definedBy("A").returningUnknown().buildEdge()
				.withEdge().from("H2").to("H3").name("condition").definedBy("A").returning("boolean").buildEdge()
				.withEdge().from("H3").to("H4").name("g").definedBy("A").returningUnknown().buildEdge()
				.withEdge().from("H4").to("H3").name("condition").definedBy("A").returning("boolean").buildEdge()
				.withEdge().from("H3").to("H5").name("h").definedBy("A").returningUnknown().buildEdge()
				.buildHistory();
		// The history of the boolean created from a.condition()
		History expected2 = b()
				.withEdge().fromRoot().to("H1").name("<init>").definedBy("A").buildEdge()
				.withEdge().from("H1").to("H2").name("f").definedBy("A").returningUnknown().buildEdge()
				.withEdge().from("H2").to("H3").name("condition").definedBy("A").returning("boolean").buildEdge()
				.withEdge().from("H3").to("H4").name("g").definedBy("A").returningUnknown().buildEdge()
				.withEdge().from("H4").to("H3").name("condition").definedBy("A").returning("boolean").buildEdge()
				.buildHistory();
		assertEqualContentHistories(result, expected1, expected2);
	}
	
	@Test
	public void testStaticCall() throws InterruptedException, CanceledException {
		String className = "TestStaticCall";
		HistoryCollection result = analyze(className);
		History expected = b()
				.withEdge().fromRoot().to("H1").name("<init>").definedBy("A").buildEdge()
				.withEdge().from("H1").to("H2").name("f").definedBy("A").returningUnknown().buildEdge()
				.withEdge().from("H2").to("H3").name("staticMethod").definedBy("A").returningUnknown().params("A").buildEdge()
				.withEdge().from("H3").to("H4").name("g").definedBy("A").returningUnknown().buildEdge()
				.buildHistory();
		assertEqualContentHistories(result, expected);
	}
	
	@Test
	public void testField() throws InterruptedException, CanceledException {
		String className = "TestField";
		HistoryCollection result = analyze(className);
		History expected = b()
				.withEdge().fromRoot().to("H1").definedBy("A").unknownType(UnknownType.FROM_FIELD).buildEdge()
				.withEdge().from("H1").to("H2").name("f").definedBy("A").returningUnknown().buildEdge()
				.buildHistory();
		assertEqualContentHistories(result, expected);
	}
	
	@Test
	public void testInitializedField() throws CanceledException, InterruptedException {
		String className = "TestInitializedField";
		HistoryCollection result = analyze(className);
		History expected1 = b()
				.withEdge().fromRoot().to("H1").definedBy("A").unknownType(UnknownType.FROM_FIELD).buildEdge()
				.withEdge().from("H1").to("H2").name("f").definedBy("A").returningUnknown().buildEdge()
				.buildHistory();
		History expected2 = b()
				.withEdge().fromRoot().to("H1").name("<init>").definedBy("A").buildEdge()
				.buildHistory();
		assertEqualContentHistories(result, expected1, expected2);
	}
	
	@Test
	public void testInterProceduralField() throws CanceledException, InterruptedException {
		String className = "TestInterProceduralField";
		HistoryCollection result = analyze(className);
		History expectedFromMethod1 = b()
				.withEdge().fromRoot().to("H1").definedBy("A").unknownType(UnknownType.FROM_FIELD).buildEdge()
				.withEdge().from("H1").to("H2").name("f").definedBy("A").returningUnknown().buildEdge()
				.withEdge().from("H2").to("H3").name("g").definedBy("A").returningUnknown().buildEdge()
				.withEdge().from("H3").to("H4").name("h").definedBy("A").returningUnknown().buildEdge()
				.buildHistory();
		History expectedFromMethod2 = b()
				.withEdge().fromRoot().to("H1").definedBy("A").unknownType(UnknownType.FROM_FIELD).buildEdge()
				.withEdge().from("H1").to("H2").name("g").definedBy("A").returningUnknown().buildEdge()
				.buildHistory();
		assertEqualContentHistories(result, expectedFromMethod1, expectedFromMethod2);
	}
	
	@Test
	public void testInterProceduralParameter() throws CanceledException, InterruptedException {
		String className = "TestInterProceduralParameter";
		HistoryCollection result = analyze(className);
		History expectedFromMethod1 = b()
				.withEdge().fromRoot().to("H1").name("<init>").definedBy("A").buildEdge()
				.withEdge().from("H1").to("H2").name("f").definedBy("A").returningUnknown().buildEdge()
				.withEdge().from("H2").to("H3").name("g").definedBy("A").returningUnknown().buildEdge()
				.withEdge().from("H3").to("H4").name("h").definedBy("A").returningUnknown().buildEdge()
				.buildHistory();
		History expectedFromMethod2 = b()
				.withEdge().fromRoot().to("H1").definedBy("A").unknownType(UnknownType.FROM_PARAMETER).buildEdge()
				.withEdge().from("H1").to("H2").name("g").definedBy("A").returningUnknown().buildEdge()
				.buildHistory();
		assertEqualContentHistories(result, expectedFromMethod1, expectedFromMethod2);
	}
	
	@Test
	public void testInterProceduralReturn() throws CanceledException, InterruptedException {
		String className = "TestInterProceduralReturn";
		HistoryCollection result = analyze(className);
		History expectedFromMethod1 = b()
				.withEdge().fromRoot().to("H1").name("<init>").definedBy("A").buildEdge()
				.withEdge().from("H1").to("H2").name("f").definedBy("A").returningUnknown().buildEdge()
				.withEdge().from("H2").to("H3").name("g").definedBy("A").returningUnknown().buildEdge()
				.buildHistory();
		History expectedFromMethod2 = b()
				.withEdge().fromRoot().to("H1").name("<init>").definedBy("A").buildEdge()
				.withEdge().from("H1").to("H2").name("f").definedBy("A").returningUnknown().buildEdge()
				.buildHistory();
		assertEqualContentHistories(result, expectedFromMethod1, expectedFromMethod2);
	}
	
	@Test
	public void testInterProceduralParameterAndReturn() throws CanceledException, InterruptedException {
		String className = "TestInterProceduralParameterAndReturn";
		HistoryCollection result = analyze(className);
		History expectedFromMethod1 = b()
				.withEdge().fromRoot().to("H1").name("<init>").definedBy("A").buildEdge()
				.withEdge().from("H1").to("H2").name("f").definedBy("A").returningUnknown().buildEdge()
				.withEdge().from("H2").to("H3").name("g").definedBy("A").returningUnknown().buildEdge()
				.withEdge().from("H3").to("H4").name("h").definedBy("A").returningUnknown().buildEdge()
				.buildHistory();
		History expectedFromMethod2 = b()
				.withEdge().fromRoot().to("H1").definedBy("A").unknownType(UnknownType.FROM_PARAMETER).buildEdge()
				.withEdge().from("H1").to("H2").name("g").definedBy("A").returningUnknown().buildEdge()
				.buildHistory();
		assertEqualContentHistories(result, expectedFromMethod1, expectedFromMethod2);
	}
	
	@Test
	public void testFocus() throws CanceledException, InterruptedException {
		String className = "TestFocus";
		HistoryCollection result = analyze(className);
		History expected_a1_1 = b()
				.withEdge().fromRoot().to("H1").name("<init>").definedBy("A").buildEdge()
				.withEdge().from("H1").to("H2").name("f1").definedBy("A").returningUnknown().buildEdge()
				.buildHistory();
		History expected_a1_2 = b()
				.withEdge().fromRoot().to("H1").name("<init>").definedBy("A").buildEdge()
				.withEdge().from("H1").to("H2").name("f1").definedBy("A").returningUnknown().buildEdge()
				.withEdge().from("H2").to("H3").name("g").definedBy("A").returningUnknown().buildEdge()
				.withEdge().from("H3").to("H4").name("h").definedBy("A").returningUnknown().buildEdge()
				.buildHistory();
		History expected_a2 = b()
				.withEdge().fromRoot().to("H1").name("<init>").definedBy("A").buildEdge()
				.withEdge().from("H1").to("H2").name("f2").definedBy("A").returningUnknown().buildEdge()
				.withEdge().from("H2").to("H3").name("g").definedBy("A").returningUnknown().buildEdge()
				.withEdge().from("H3").to("H4").name("h").definedBy("A").returningUnknown().buildEdge()
				.buildHistory();
		assertEqualContentHistories(result, expected_a1_1, expected_a1_2, expected_a2);
	}
	
}
