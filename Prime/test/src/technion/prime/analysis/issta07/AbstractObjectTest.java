package technion.prime.analysis.issta07;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import technion.prime.utils.Logger.CanceledException;

import technion.prime.DefaultOptions;
import technion.prime.Options;
import technion.prime.dom.AppType;
import technion.prime.dom.AppAccessPath;
import technion.prime.analysis.Label;
import technion.prime.dom.AppObject;
import technion.prime.dom.dummy.DummyAppType;
import technion.prime.dom.dummy.DummyAppObject;

public class AbstractObjectTest {
	
	private final static String[] aps = new String[] {
			"x", "y", "a", "b", "x.f", "y.f"
	};
	
	private AppType t = new DummyAppType("T");
	private AppObject temp = createDummyObject("temp");
	private Collection<AbstractObject> objs;
	private int abstractObjectCount = 0;
	private Options options;
	
	class AbstractObjectBuilder {
		Collection<String> must = new LinkedList<String>();
		Collection<String> mustNot = new LinkedList<String>();
		AbstractObjectBuilder must(String... aps) {
			must.addAll(Arrays.asList(aps));
			return this;
		}
		AbstractObjectBuilder mustNot(String... aps) {
			mustNot.addAll(Arrays.asList(aps));
			return this;
		}
		AbstractObject build(boolean unique, boolean may) {
			AbstractObject result = new AbstractObject(
					options,
					new Label(abstractObjectCount++),
					temp,
					false,
					Collections.<AppAccessPath>emptySet());
			result.removeMustAccessPath(temp);
			for (String m : must) {
				if (m == null) continue;
				result.addMustAccessPath(createDummyObject(m));
			}
			for (String m : mustNot) {
				if (m == null) continue;
				result.addMustNotAccessPath(createDummyObject(m));
			}
			result.setUnique(unique);
			result.setMay(may);
			return result;
		}
		
	}
	
	private AppObject createDummyObject(String ap) {
		return new DummyAppObject(ap, t);
	}
	
//	private AppMethodRef createDummyMethod(String name) {
//		AppType voidType = new DummyAppType("void");
//		return new DummyAppMethodRef(t, voidType, name);
//	}
	
	@Before
	public void setUp() {
		options = new DefaultOptions();
		objs = new LinkedList<AbstractObject>();
		for (int combination = 0 ; combination < Math.pow(3, aps.length) ; combination++) {
			AbstractObjectBuilder b = new AbstractObjectBuilder();
			for (int i = 0 ; i < aps.length ; i++) {
				int choice = (combination / (int)(Math.pow(3, i))) % 3;
				switch (choice) {
					case 0: break;
					case 1: b.must(aps[i]); break;
					case 2: b.mustNot(aps[i]); break;
				}
			}
			objs.add(b.build(false, false));
			objs.add(b.build(false, true));
			objs.add(b.build(true, false));
			objs.add(b.build(true, true));
		}
	}
	
	@Test
	public void testAddMustAP() throws InterruptedException, CanceledException {
		AbstractObject beforeObj = new AbstractObjectBuilder().must("x").build(false, false);
		AbstractObject afterObj = beforeObj.clone();
		AppObject y = createDummyObject("y");
		afterObj.addMustAccessPath(y);
		assertTrue(afterObj.mustBe(y));
	}
	
	@Test
	public void testClone() throws InterruptedException, CanceledException {
		for (AbstractObject obj : objs) {
			AbstractObject after = obj.clone();
			assertTrue(obj != after);
			assertFalse(obj.equals(after));
			assertTrue(obj.sameContent(after));
			assertTrue(obj.contentHashCode() == after.contentHashCode());
		}
	}
	
	@Test
	public void testAssignmentToNull() throws InterruptedException, CanceledException {
		AppObject x = createDummyObject("x");
		AppObject x_dot_f = createDummyObject("x.f");
		for (AbstractObject obj : objs) {
			AbstractObject newObj = obj.assignment(x, null);
			assertTrue(newObj.mustNotBe(x));
			assertFalse(newObj.mustBe(x));
			assertFalse(newObj.mustBe(x_dot_f));
		}
	}
	
	@Test
	public void testAssignmentNoFields() throws InterruptedException, CanceledException {
		AppObject x = createDummyObject("x");
		AppObject x_dot_f = createDummyObject("x.f");
		AppObject y = createDummyObject("y");
		AppObject y_dot_f = createDummyObject("y.f");
		for (AbstractObject obj : objs) {
			boolean must_y = obj.mustBe(y);
			boolean must_y_dot_f = obj.mustBe(y_dot_f);
			boolean mustNot_y = obj.mustNotBe(y);
			AbstractObject newObj = obj.assignment(x, y);
			if (must_y) assertTrue(newObj.mustBe(x));
			if (must_y_dot_f) assertTrue(newObj.mustBe(x_dot_f));
			if (mustNot_y == false) assertFalse(newObj.mustNotBe(x));
		}
	}
	
	@Test
	public void testAssignmentToField() throws InterruptedException, CanceledException {
		AppObject x_dot_f = createDummyObject("x.f");
		AppObject y = createDummyObject("y");
		AppObject y_dot_f = createDummyObject("y.f");
		AppObject x_dot_f_dot_f = createDummyObject("x.f.f");
		for (AbstractObject obj : objs) {
			boolean must_y = obj.mustBe(y);
			boolean must_y_dot_f = obj.mustBe(y_dot_f);
			boolean mustNot_y = obj.mustNotBe(y);
			AbstractObject newObj = obj.assignment(x_dot_f, y);
			if (must_y) assertTrue(newObj.mustBe(x_dot_f));
			if (must_y_dot_f) assertTrue(newObj.mustBe(x_dot_f_dot_f));
			if (mustNot_y == false) assertFalse(newObj.mustNotBe(x_dot_f));
			assertTrue(newObj.getMay());
		}
	}
	
	@Test
	public void testAssignmentFromField() throws InterruptedException, CanceledException {
		AppObject x = createDummyObject("x");
		AppObject y_dot_f = createDummyObject("y.f");
		for (AbstractObject obj : objs) {
			boolean must_y_dot_f = obj.mustBe(y_dot_f);
			boolean mustNot_y_dot_f = obj.mustNotBe(y_dot_f);
			AbstractObject newObj = obj.assignment(x, y_dot_f);
			if (must_y_dot_f) assertTrue(newObj.mustBe(x));
			if (mustNot_y_dot_f == false) assertFalse(newObj.mustNotBe(x));
		}
	}
	
//	@Test
//	public void testMethodCall() throws InterruptedException, CanceledException {
//		AppObject x = createDummyObject("x");
//		AppMethodRef m = createDummyMethod("foo");
//		for (AbstractObject obj : objs) {
//			boolean mustNot_x = obj.mustNotBe(x);
//			boolean must_x = obj.mustBe(x);
//			boolean may = obj.getMay();
//			boolean unique = obj.isUnique();
//			Collection<AbstractObject> newObjects =
//					obj.methodCall(x, m, Collections.<AppObject>emptyList(), true);
//			int expectedSize = 0;
//			if (mustNot_x == false && (must_x || may)) expectedSize++;
//			if (mustNot_x || (must_x == false && !unique)) expectedSize++;
//			assertEquals(expectedSize, newObjects.size());
//		}
//	}

}
