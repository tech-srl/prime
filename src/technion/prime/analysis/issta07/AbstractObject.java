package technion.prime.analysis.issta07;

import technion.prime.utils.ConcurrencyUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import technion.prime.Options;
import technion.prime.analysis.Label;
import technion.prime.dom.AppAccessPath;
import technion.prime.dom.AppMethodRef;
import technion.prime.dom.AppObject;
import technion.prime.dom.AppType;
import technion.prime.history.History;
import technion.prime.utils.Logger.CanceledException;


/**
 * An abstract object, representing a group of concrete objects.
 * Defined as &lt;Label, Must, May, MustNot, Unique, History&gt;<br/>
 * Where<br/>
 * <ul>
 *   <li>Label - A Label object. Uniquely identifies the code location where this abstract
 *   object is first encountered.</li>
 *   <li>Must - access path group. All concrete objects represented by this abstract objects
 *   must be referenced by one of the access paths in this group.</li>
 *   <li>May - Flag which is true if other access paths may point to one of the concrete objects
 *   represented by this abstract object.</li>
 *   <li>Unique - Flag which is true if this abstract object is the only abstract object defined
 *   associated with its specific label.</li>
 *   <li>History - Invocation history associated with this abstract object.</li>
 * </ul>
 * Abstract objects are mutable. Equality and hash code are based on identity -
 * use sameContent() to check equality by content. 
 */
public class AbstractObject implements Cloneable, Serializable {
	private static final long serialVersionUID = -388418290412496994L;
	
	private final Options options;
	private final Label label;
	
	private HashSet<AppType> types = new HashSet<AppType>();
	private HashSet<AppAccessPath> must = new HashSet<AppAccessPath>();
	private HashSet<AppAccessPath> mustNot = new HashSet<AppAccessPath>();
	private boolean unique;
	private boolean may;
	private History h;
	
	private transient Integer contentHash;

	
	/**
	 * Run before any mutation to this object.
	 */
	private void prepareForUpdate() {
		contentHash = null;
	}
	
	/**
	 * @param options Prime options.
	 * @param label The label in which the abstract object first appears.
	 * @param obj The initial app object represented by by this abstract object.
	 * @param fromNew Whether this abstract object was created from a
	 * <code>new T()</code> expression.
	 * @param otherAccessPaths Other access paths currently known in the program.
	 */
	public AbstractObject(Options options, Label label, AppObject obj, boolean fromNew, Set<AppAccessPath> otherAccessPaths) {
		this.options = options;
		this.label = label;
		addMustAccessPath(obj);
		if (fromNew) {
			unique = true;
			mustNot.addAll(otherAccessPaths);
		} else {
			may = true;
		}
		h = options.newHistory();
	}

	/**
	 * A method call is encountered; update this object accordingly. If the
	 * method's receiver may not point to this object, no update will take place
	 * and the result will only contain <code>this</code>.
	 * 
	 * @param receiver The method's receiver.
	 * @param m The method.
	 * @param arguments Method arguments.
	 * @param noOtherLabelsForReceiver True if there are no other abstract objects in the state
	 * which must be pointed by the receiver.
	 * @return A list of new objects created from this call (may be more than
	 * one due to focus operators). Notice the list may contain this method's
	 * receiver (<code>this</code>) as well.
	 * @throws InterruptedException
	 * @throws CanceledException
	 */
	public Collection<AbstractObject> methodCall(
			AppObject receiver,
			AppMethodRef m,
			Iterable<? extends AppObject> arguments,
			boolean noOtherLabelsForReceiver)
					throws InterruptedException, CanceledException {
		prepareForUpdate();
		
		Set<AbstractObject> result = new HashSet<AbstractObject>();
		if (mustNotBe(receiver) == false &&
				(mustBe(receiver) || may) &&
				mayBeOfType(receiver.getType())) {
			AbstractObject newObject = clone();
			newObject.strongUpdate(m);
			newObject.addMustAccessPath(receiver);
			result.add(newObject);
		}
		if (mustNotBe(receiver) ||
				(mustBe(receiver) == false && may &&
				!(unique && noOtherLabelsForReceiver))
				|| result.isEmpty()) {
			AbstractObject newObject = clone();
			newObject.addMustNotAccessPath(receiver);
			result.add(newObject);
		}
		
		return result;
	}
	
	/**
	 * @param t
	 * @return True if this abstract object may be of type <code>t</code>.
	 */
	private boolean mayBeOfType(AppType t) {
		if (options.isSameTypeRequiredForReceiver()) {
			return types.contains(t);
		}
		return true;
	}

	private void strongUpdate(AppMethodRef m) throws InterruptedException, CanceledException {
		h.extendWithMethodCall(m, 1);
	}

	/**
	 * This method only checks against the Must set, it does not verify that <code>obj</code>
	 * does not appear in the MustNot set!
	 * @param obj
	 * @return True if <code>obj</code> appears in the Must set.
	 * @throws CanceledException 
	 * @throws InterruptedException 
	 */
	public boolean mustBe(AppObject obj) throws InterruptedException, CanceledException {
		return containedInAccessPathGroup(obj, must);
	}
	
	/**
	 * This method only checks against the MustNot set, it does not verify that <code>obj</code>
	 * does not appear in the Must set!
	 * @param obj
	 * @return True if <code>obj</code> appears in the MustNot set.
	 * @throws CanceledException 
	 * @throws InterruptedException 
	 */
	public boolean mustNotBe(AppObject obj) throws InterruptedException, CanceledException {
		return containedInAccessPathGroup(obj, mustNot);
	}
	
	public boolean isUnique() {
		return unique;
	}
	
	private static boolean containedInAccessPathGroup(AppObject obj, Set<AppAccessPath> paths) throws InterruptedException, CanceledException {
		for (AppAccessPath path : paths) {
			ConcurrencyUtils.checkState();
			if (obj.getAccessPath().equals(path)) return true;
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public AbstractObject clone() {
		AbstractObject obj = null;
		try {
			obj = (AbstractObject) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
		obj.types = (HashSet<AppType>) types.clone();
		obj.must = (HashSet<AppAccessPath>) must.clone();
		obj.mustNot = (HashSet<AppAccessPath>) mustNot.clone();
		obj.may = may;
		obj.unique = unique;
		obj.h = h.clone();
		return obj;
	}

	/**
	 * Assignment. Call this method whenever an assignment occurs; this is this method's job
	 * to decide whether this abstract object is, or isn't, affected.
	 * @param lhs Left-hand side of the assignment.
	 * @param rhs Right-hand side of the assignment.
	 * @return A version of this AbstractObject after the assignment. May return "this" if the
	 * assignment had no effect.
	 * @throws CanceledException 
	 * @throws InterruptedException 
	 */
	public AbstractObject assignment(AppObject lhs, AppObject rhs) throws InterruptedException, CanceledException {
		if (rhs == null || rhs.isNull()) {
			return assignmentFromNull(lhs);
		}
		
		AbstractObject result = clone();
		result.prepareForUpdate();
		
		AppAccessPath lhsAp = lhs.getAccessPath();
		AppAccessPath rhsAp = rhs.getAccessPath();
		
		for (AppAccessPath ap : must) {
			ConcurrencyUtils.checkState();
			if (ap.equals(rhsAp)) {
				result.addMustAccessPath(lhs);
			} else if (ap.prefixedBy(rhsAp)) {
				result.must.add(lhsAp.concat(
						ap.getSuffix(ap.getLength() - rhsAp.getLength())));
			}
		}
		
		if (mustNotBe(rhs) == false) {
			result.mustNot.remove(lhsAp);
		} else {
			// This is an extension of ISSTA'07
			result.mustNot.add(lhsAp);
		}
		
		if (lhsAp.getLength() > 1 && options.isMayAnalysis()) result.setMay(true);
		
		return result;
	}
	
	private AbstractObject assignmentFromNull(AppObject lhs) throws InterruptedException, CanceledException {
		AbstractObject result = clone();
		result.prepareForUpdate();
		
		AppAccessPath lhsAp = lhs.getAccessPath();
		for (AppAccessPath ap : must) {
			ConcurrencyUtils.checkState();
			if (ap.prefixedBy(lhsAp)) {
				result.must.remove(ap);
			}
		}
		result.mustNot.add(lhsAp);
		
		return result;
	}

	void addMustAccessPath(AppObject obj) {
		must.add(obj.getAccessPath());
		types.add(obj.getType());
	}
	
	void removeMustAccessPath(AppObject obj) {
		must.remove(obj.getAccessPath());
	}
	
	void addMustNotAccessPath(AppObject obj) {
		mustNot.add(obj.getAccessPath());
	}
	
	public AbstractObject newGlobalAccessPath(AppObject lhs) {
		AbstractObject result = clone();
		
		result.prepareForUpdate();
		
		if (may == false) result.addMustNotAccessPath(lhs);
		
		return result;
	}

	public void newUniqueAccessPath(AppObject obj) {
		prepareForUpdate();
		
		addMustNotAccessPath(obj);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("<");
		sb.append("Must=" + must.toString() + ", ");
		sb.append("May=" + may + ", ");
		sb.append("MustNot=" + mustNot.toString() + ", ");
		sb.append("Unique=" + unique + ", ");
		sb.append("HistorySize=" + h.getNumNodes());
		sb.append(">");
		return sb.toString();
	}

	public Label getLabel() {
		return label;
	}

	public void setUnique(boolean b) {
		unique = b;
	}
	
	public boolean sameContent(AbstractObject obj) throws InterruptedException, CanceledException {
		if (this == obj) return true;
		if (contentHashCode() != obj.contentHashCode()) {
			// Efficient because contentHashCode is cached
			return false;
		}
		return
				may == obj.may &&
				unique == obj.unique &&
				must.equals(obj.must) &&
				mustNot.equals(obj.mustNot) &&
				types.equals(obj.types) &&
				h.equalContent(obj.h);
	}
	
	public int contentHashCode() {
		if (contentHash == null) {
			contentHash = 31;
			contentHash += 31 * must.hashCode();
			contentHash += may ? 31 : 0;
			contentHash += 31 * mustNot.hashCode();
			contentHash += unique ? 31 : 0;
			contentHash += 31 * types.hashCode();
			contentHash += 31 * h.contentHash();
		}
		return contentHash;
	}

	/**
	 * @return The history associated with this abstract object.
	 */
	public History getHistory() {
		return h;
	}

	/**
	 * Set the history associated with this abstract object.
	 * @param h
	 */
	public void setHistory(History h) {
		this.h = h;
	}

	/**
	 * Set the may bit of this abstract object.
	 * @param may
	 */
	public void setMay(boolean may) {
		this.may = may;
	}

	/**
	 * @return The may bit.
	 */
	public boolean getMay() {
		return may;
	}

	/**
	 * @param obj
	 * @return True if this object is more precise than the argument.
	 * @throws CanceledException 
	 * @throws InterruptedException 
	 */
	public boolean morePreciseThan(AbstractObject obj) throws InterruptedException, CanceledException {
		return
				label.equals(obj.label) &&
				must.containsAll(obj.must) &&
				may &&
				mustNot.containsAll(obj.mustNot) &&
				(!unique || obj.unique) &&
				obj.h.includes(this.h);
//				h.equalContent(obj.h);
	}

	/**
	 * @param obj
	 * @return True if both abstract objects are identical except for their history.
	 */
	public boolean sameContentWithoutHistory(AbstractObject obj) {
		return
				label.equals(obj.label) &&
				must.equals(obj.must) &&
				may == obj.may &&
				mustNot.equals(obj.mustNot) &&
				unique == obj.unique;
	}

	public boolean morePreciseWithoutHistoryThan(AbstractObject obj) {
		return
				label.equals(obj.label) &&
				must.containsAll(obj.must) &&
				(!may || obj.may) &&
				mustNot.containsAll(obj.mustNot) &&
				(!unique || obj.unique);
	}

	public AbstractObject unconditionalMethodCall(AppMethodRef m) throws InterruptedException, CanceledException {
		AbstractObject result = clone();
		result.strongUpdate(m);
		return result;
	}

}
