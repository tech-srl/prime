package technion.prime.analysis.issta07;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import technion.prime.analysis.soot.SootFlowAnalysis;
import technion.prime.utils.ConcurrencyUtils;
import technion.prime.utils.CollectionUtils;
import technion.prime.utils.Logger;
import technion.prime.utils.MultiMap;
import technion.prime.dom.AppAccessPath;
import technion.prime.dom.AppAnnotation;
import technion.prime.dom.AppMethodDecl;
import technion.prime.dom.AppType;
import technion.prime.dom.UnknownMethodFromField;
import technion.prime.dom.UnknownMethodFromParam;
import technion.prime.Options;
import technion.prime.dom.UnknownMethod;
import technion.prime.history.History;
import technion.prime.dom.AppMethodRef;
import technion.prime.dom.AppObject;
import technion.prime.dom.soot.SootAppObject;
import technion.prime.history.HistoryCollection;
import technion.prime.utils.Logger.CanceledException;
import technion.prime.analysis.ProgramState;
import technion.prime.analysis.Label;


public class ProgramStateImpl implements ProgramState {
	private final Options options;
	private Set<AbstractObject> objects;
	private Set<AppAccessPath> seenAccessPaths;
	private AppMethodDecl method;

	public ProgramStateImpl(Options options) {
		this.options = options;
		objects = createObjectSet();
		seenAccessPaths = new HashSet<AppAccessPath>();
	}

	@Override
	public void copyFrom(ProgramState ps) {
		ProgramStateImpl psi = downcast(ps);
		objects = createObjectSet();
		objects.addAll(psi.objects);
		seenAccessPaths = new HashSet<AppAccessPath>(psi.seenAccessPaths);
	}

	@Override
	public void joinFrom(ProgramState ps) throws InterruptedException, CanceledException {
		ProgramStateImpl psi = downcast(ps);
		objects.addAll(psi.objects);
		seenAccessPaths.addAll(psi.seenAccessPaths);
		mergeHistoriesForMatchingObjects();
		removeRedundantObjects();
	}

	private void mergeHistoriesForMatchingObjects() throws InterruptedException, CanceledException {
		Set<AbstractObject> newObjects = createObjectSet();
		MultiMap<AbstractObject, History> buckets = createMergeBuckets();
		for (AbstractObject obj : buckets.keySet()) {
			ConcurrencyUtils.checkState();
			History merged = null;
			for (History h : buckets.getAll(obj)) {
				if (merged == null) merged = h.clone();
				else merged.joinFrom(h);
			}
			AbstractObject newObject = obj.clone();
			newObject.setHistory(merged);
			newObjects.add(newObject);
		}
		objects = newObjects;
	}

	private MultiMap<AbstractObject, History> createMergeBuckets() throws InterruptedException,
			CanceledException {
		MultiMap<AbstractObject, History> result = new MultiMap<AbstractObject, History>();
		for (AbstractObject obj1 : objects) {
			ConcurrencyUtils.checkState();
			boolean found = false;
			for (AbstractObject obj2 : result.keySet()) {
				ConcurrencyUtils.checkState();
				if (shouldBeMerged(obj1, obj2)) {
					result.put(obj2, obj1.getHistory());
					found = true;
					break;
				}
			}
			if (found == false) {
				result.put(obj1, obj1.getHistory());
			}
		}
		return result;
	}

	private boolean shouldBeMerged(AbstractObject obj1, AbstractObject obj2)
			throws InterruptedException, CanceledException {
		// return obj1.morePreciseWithoutHistoryThan(obj2) ||
		// obj2.morePreciseWithoutHistoryThan(obj1);
		return obj1.sameContentWithoutHistory(obj2);
	}

	@Override
	public void assignmentFromNew(Label l, AppObject lhs)
			throws InterruptedException, CanceledException {
		Set<AbstractObject> newObjects = createObjectSet();
		for (AbstractObject obj : objects) {
			newObjects.add(obj.assignment(lhs, null));
		}
		objects = newObjects;
		boolean found = false;
		for (AbstractObject obj : objects) {
			ConcurrencyUtils.checkState();
			if (obj.getLabel().equals(l)) {
				found = true;
				obj.setUnique(false);
			} else {
				obj.newUniqueAccessPath(lhs);
			}
		}
		if (found) {
			objects.add(createStaleObject(l, lhs, seenAccessPaths));
		} else {
			objects.add(createFreshObject(l, lhs));
		}
		seenAccessPaths.add(lhs.getAccessPath());
		removeRedundantObjects();
	}

	@Override
	public void assignmentFromPhantomMethod(Label l, AppObject lhs, AppObject receiver,
			AppMethodRef m, Iterable<? extends AppObject> args) throws InterruptedException,
			CanceledException {
		Set<AbstractObject> newObjects = createObjectSet();
		for (AbstractObject obj : objects) {
			ConcurrencyUtils.checkState();
			newObjects.add(obj.assignment(lhs, null));
		}
		objects = newObjects;

		AbstractObject newObj = lhs.getType().isPrimitive() ?
				createFreshObject(l, lhs) :
				createStaleObject(l, lhs, Collections.<AppAccessPath> emptySet());

		// Creation context
		if (isBaseTracked(receiver)) {
			History h = getHistoryOf(receiver);
			if (h != null) newObj.setHistory(h);
		}

		objects.add(newObj);
		removeRedundantObjects();
		seenAccessPaths.add(lhs.getAccessPath());
	}

	@Override
	public void assignmentFromObject(Label l, AppObject lhs, AppObject rhs)
			throws InterruptedException, CanceledException {
		// Create the rhs, if it doesn't exist already and isn't null.
		if (rhs != null && rhs.isNull() == false) addNewObjectIfMissing(l, rhs);

		Set<AbstractObject> newObjects = createObjectSet();

		for (AbstractObject obj : objects) {
			ConcurrencyUtils.checkState();
			newObjects.add(obj.assignment(lhs, rhs));
		}

		objects = newObjects;
		removeRedundantObjects();
		seenAccessPaths.add(lhs.getAccessPath());
	}

	@Override
	public void assignmentFromAllOf(Label l, AppObject lhs, Iterable<? extends AppObject> rhss)
			throws InterruptedException, CanceledException {
		Set<AbstractObject> newObjects = createObjectSet();
		for (AppObject rhs : rhss) {
			for (AbstractObject obj : objects) {
				ConcurrencyUtils.checkState();
				newObjects.add(obj.assignment(lhs, rhs));
			}
		}
		objects = newObjects;
		removeRedundantObjects();
		seenAccessPaths.add(lhs.getAccessPath());
	}

	@Override
	public void assignmentFromUntracked(Label l, SootAppObject lhs) throws InterruptedException,
			CanceledException {
		Set<AbstractObject> newObjects = createObjectSet();
		for (AbstractObject obj : objects) {
			ConcurrencyUtils.checkState();
			newObjects.add(obj.assignment(lhs, null));
		}
		objects = newObjects;
		removeRedundantObjects();
		seenAccessPaths.add(lhs.getAccessPath());
	}

	@Override
	public void methodCall(Label l, AppObject receiver, AppMethodRef m,
			Iterable<? extends AppObject> args) throws InterruptedException, CanceledException {

		if (receiver != null) {
			methodCallWithNonNullReceiver(l, receiver, m, args);
		}

		if (m.isPhantom() && m.isTransparent()) {
			for (AppObject arg : args) {
				AppType t = arg.getType();
				AppMethodRef unknownMethod = new UnknownMethod(t, m);
				methodCall(l, arg, unknownMethod, Collections.<AppObject> emptySet());
			}
		} else if (m.isOpaque() && receiver == null) {
			for (AppObject arg : args) {
				methodCall(l, arg, m, Collections.<AppObject> emptySet());
			}
		}

		removeRedundantObjects();
	}

	private void methodCallWithNonNullReceiver(Label l, AppObject receiver, AppMethodRef m,
			Iterable<? extends AppObject> args) throws InterruptedException, CanceledException {
		if (isBaseTracked(receiver) == false) return;

		Set<Label> labelsPointedToByReceiver = new HashSet<Label>();
		for (AbstractObject obj : objects) {
			ConcurrencyUtils.checkState();
			if (obj.mustBe(receiver)) labelsPointedToByReceiver.add(obj.getLabel());
		}

		Set<AbstractObject> newObjects = new HashSet<AbstractObject>();
		for (AbstractObject obj : objects) {
			newObjects.addAll(
					obj.methodCall(receiver, m, args, labelsPointedToByReceiver.size() == 1));
		}
		objects = newObjects;
	}

	@Override
	public HistoryCollection toHistoryCollection() {
		HistoryCollection result = options.newHistoryCollection();
		for (AbstractObject obj : objects) {
			History h = obj.getHistory();
			h.setTitle("source: " + method.getSignature());
			result.addHistory(h);
		}
		return result;
	}

	@Override
	public void removeUntrackedHistories() {
		Set<AbstractObject> newObjects = new HashSet<AbstractObject>();
		object_loop: for (AbstractObject obj : objects) {
			for (AppType t : obj.getHistory().getAllParticipatingTypes()) {
				if (isTrackedType(t)) {
					newObjects.add(obj);
					continue object_loop;
				}
			}
		}
		objects = newObjects;
	}

	private ProgramStateImpl downcast(Object o) {
		return (ProgramStateImpl) o;
	}

	/**
	 * Remove spurious AbstractObjects: objects for which this state already contains a more precise
	 * object.
	 * 
	 * @throws CanceledException
	 * @throws InterruptedException
	 */
	private void removeRedundantObjects() throws InterruptedException, CanceledException {
		return;
		// Set<AbstractObject> toRemove = new HashSet<AbstractObject>();
		// for (AbstractObject obj1 : objects) {
		// for (AbstractObject obj2 : objects) {
		// ConcurrencyUtils.checkState();
		// if (obj1 == obj2) continue;
		// if (obj1.sameContent(obj2)) toRemove.add(obj1);
		// // if (obj1.morePreciseThan(obj2)) toRemove.add(obj1);
		// }
		// }
		// objects.removeAll(toRemove);
	}

	private Set<AbstractObject> createObjectSet() {
		return new HashSet<AbstractObject>();
	}

	private AbstractObject createFreshObject(Label l, AppObject lhs) {
		return new AbstractObject(options, l, lhs, true, Collections.<AppAccessPath> emptySet());
	}

	private AbstractObject createStaleObject(Label l,
			AppObject lhs, Set<AppAccessPath> seenAccessPaths)
			throws InterruptedException, CanceledException {
		AbstractObject result =
				new AbstractObject(options, l, lhs, !options.isMayAnalysis(), seenAccessPaths);
		result.getHistory().extendWithMethodCall(new UnknownMethod(lhs.getType(), null), 1);
		return result;
	}

	/**
	 * @param appObj
	 * @return A cloned version of the merge of all histories associated with appObj, or null if
	 *         there is no such history.
	 */
	private History getHistoryOf(AppObject appObj) throws InterruptedException, CanceledException {
		History h = null;
		for (AbstractObject obj : objects) {
			ConcurrencyUtils.checkState();
			if (obj.mustBe(appObj)) {
				if (h == null) h = obj.getHistory().clone();
				else h.joinFrom(obj.getHistory());
			}
		}
		return h;
	}

	private void addNewObjectIfMissing(Label l, AppObject appObj) throws InterruptedException,
			CanceledException {
		AppAccessPath objAp = appObj.getAccessPath();
		if (seenAccessPaths.contains(objAp)) return;
		Set<AbstractObject> newObjects = createObjectSet();
		for (AbstractObject obj : objects) {
			ConcurrencyUtils.checkState();
			newObjects.add(obj.newGlobalAccessPath(appObj));
		}
		if (appObj.getType().isPrimitive()) {
			// A primitive is not a reference, so it cannot be referenced by anything else.
			newObjects.add(createFreshObject(l, appObj));
		} else {
			newObjects.add(createStaleObject(l, appObj, seenAccessPaths));
		}
		seenAccessPaths.add(objAp);
		objects = newObjects;
	}

	private boolean isTrackedType(AppType t) {
		boolean result =options.getFilterReported().passesFilter(t.getFullName());
		//Logger.log(String.format("isTrackedType? %s returned %b",t.getFullName(),result));
		return result;
	}

	private boolean isBaseTracked(AppObject appObj) {
		boolean result =options.getFilterBaseTracked().passesFilter(appObj.getType().getFullName());
		//Logger.log(String.format("isBaseTracked? %s returned %b",appObj.getType().getFullName(),result));
		return result;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{ // ");
		sb.append(objects.size());
		sb.append(" objects\n");
		boolean first = true;
		for (AbstractObject obj : objects) {
			if (first) {
				first = false;
			} else {
				sb.append(", \n");
			}
			sb.append("\t");
			sb.append(obj.toString());
		}
		sb.append("\n}");
		return sb.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ProgramStateImpl == false) return false;
		ProgramStateImpl psi = downcast(obj);
		if (seenAccessPaths.equals(psi.seenAccessPaths) == false) return false;

		return CollectionUtils.sameElements(objects, psi.objects, new Comparator<AbstractObject>() {
			@Override
			public int compare(AbstractObject o1, AbstractObject o2) {
				try {
					return o1.sameContent(o2) ? 0 : 1;
				} catch (InterruptedException e) {
					throw new SootFlowAnalysis.RuntimeInterruptedException(e);
				} catch (CanceledException e) {
					throw new SootFlowAnalysis.RuntimeInterruptedException(e);
				}
			}
		});
	}

	@Override
	public void setAnalyzedMethod(AppMethodDecl method) {
		this.method = method;
	}

	@Override
	public void assignmentFromNewParameter(Label l, SootAppObject obj) throws InterruptedException,
			CanceledException {
		if (seenAccessPaths.contains(obj.getAccessPath())) return;
		AbstractObject result =
				new AbstractObject(options, l, obj, !options.isMayAnalysis(), seenAccessPaths);
		AppMethodRef amr = options.separateUnknownSources() ? new UnknownMethodFromParam(
				obj.getType()) : new UnknownMethod(obj.getType(), null);
		result.getHistory().extendWithMethodCall(amr, 1);
		objects.add(result);
		seenAccessPaths.add(obj.getAccessPath());
	}

	@Override
	public void assignmentFromNewField(Label l, SootAppObject lhs, SootAppObject rhs,
			List<AppAnnotation> annotations) throws InterruptedException, CanceledException {
		if (seenAccessPaths.contains(rhs.getAccessPath())) {
			assignmentFromObject(l, lhs, rhs);
			return;
		}
		AbstractObject result =
				new AbstractObject(options, l, rhs, !options.isMayAnalysis(), seenAccessPaths);
		seenAccessPaths.add(rhs.getAccessPath());
		result = result.assignment(lhs, rhs);
		seenAccessPaths.add(lhs.getAccessPath());

		AppMethodRef amr = options.separateUnknownSources() ? new UnknownMethodFromField(
				lhs.getType(), annotations) : new UnknownMethod(lhs.getType(), null);

		result.getHistory().extendWithMethodCall(amr, 1);
		objects.add(result);
	}

	@Override
	public void methodCallOnAll(AppMethodRef m) throws InterruptedException, CanceledException {
		Set<AbstractObject> newObjects = createObjectSet();
		for (AbstractObject obj : objects) {
			newObjects.add(obj.unconditionalMethodCall(m));
		}
		objects = newObjects;
	}

}
