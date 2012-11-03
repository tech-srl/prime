package technion.prime.history;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import technion.prime.dom.AppAccessPath;
import technion.prime.dom.AppObject;
import technion.prime.dom.AppType;



/**
 * Represents a concrete set of objects that share certain attributes.
 * Immutable.
 */
public class AbstractObject implements Cloneable, Serializable {
	private static final long serialVersionUID = -3434025464492162458L;

	/**
	 * A collection of access paths which point to the object.
	 */
	HashSet<AppAccessPath> accessPaths = new HashSet<AppAccessPath>();
	
	/**
	 * Object type.
	 */
	protected final AppType type;

	protected boolean isTypeOnly;
	
	/**
	 * Create a new abstract object from an existing object. The object's name will be
	 * used for the initial access path, and its type will be used as the abstract object's
	 * type.
	 * @param initialObject The object that the abstract object refers to.
	 */
	public AbstractObject(AppObject initialObject) {
		this.type = initialObject.getType();
		AppAccessPath initialAccessPath = initialObject.getAccessPath();
		accessPaths.add(initialAccessPath);
	}
	
	/**
	 * Create a new abstract object with a given type. The access path collection will be empty.
	 * @param type
	 */
	public AbstractObject(AppType type) {
		this.type = type;
		isTypeOnly = false;   // sharon: changed from 'true' to 'false'
	}
	
	/**
	 * Create a new abstract object with the same type and access path collection.
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected AbstractObject clone() {
		try {
			AbstractObject cloned = (AbstractObject)super.clone();
			cloned.accessPaths = (HashSet<AppAccessPath>)accessPaths.clone();
			return cloned;
		} catch (CloneNotSupportedException e) {
			// Should not happen.
			assert(false);
			return null;
		}
	}
	
	/**
	 * Expand the access path collection with an additional access path of an object.
	 * When the expansion is done via assignment, the access path which was assigned
	 * from should also be sent.
	 * 
	 * {[a.b], [a.b.c]}
	 * x.y = a.b
	 * {[a.b, x.y], [a.b.c, x.y.c]}
	 * 
	 * @param obj The object whose access path needs to be added.
	 * @param base The access path being assigned from. If null, the access path of <code>
	 * obj</code> will be directly added to the access path collection of this abstract
	 * object. 
	 * @return A new abstract object, identical in type and with potentially additional access paths.
	 */
	public AbstractObject registerObject(AppObject obj, AppObject base) {
		AppAccessPath toAdd = null;
		AppAccessPath objAp = obj.getAccessPath();
		if (base == null) {
			toAdd = objAp;
		} else { 
			AppAccessPath baseAp = base.getAccessPath();
			for (AppAccessPath existingAp : accessPaths) {
				if (existingAp.equals(baseAp)) {
					toAdd = objAp;
					break;
				}
				if (existingAp.prefixedBy(baseAp)) {
					toAdd = objAp.concat(
							existingAp.getSuffix(existingAp.getLength() - baseAp.getLength()));
					break;
				}
			}
		}
		assert(toAdd != null);
		AbstractObject cloned = clone();
		cloned.accessPaths.add(toAdd);
		return cloned;
	}
	
	/**
	 * Unregister an object from this abstract object - this is done by subtracting
	 * the access path of object from the access path collection of this abstract
	 * object.
	 * @param obj The object whose access path needs to be removed.
	 * @return A new abstract object, identical in type but with potentially less
	 * access paths.
	 */
	public AbstractObject unregisterObject(AppObject obj) {
		AppAccessPath ap = obj.getAccessPath();
		List<AppAccessPath> toRemove = new LinkedList<AppAccessPath>();
		for (AppAccessPath existingAp : accessPaths) {
			if (existingAp.prefixedBy(ap)) toRemove.add(existingAp);
		}
		AbstractObject cloned = clone();
		cloned.accessPaths.removeAll(toRemove);
		return cloned;
	}
	
	/**
	 * @return The type of this abstract object.
	 */
	public AppType getType() {
		return type;
	}

	/**
	 * Check whether the object's access path is contained in the access path group
	 * associated with this abstract object. If true, this means this abstract object contains
	 * the object.
	 * @param obj
	 * @return True if this abstract object contains the parameter.
	 */
	public boolean contains(AppObject obj) {
		return accessPaths.contains(obj.getAccessPath());
	}
	
	/**
	 * Check whether the object's access path prefixes any of the access paths associated
	 * with this abstract object. If true, it means this abstract object would affect
	 * this abstract object as well.
	 * @param obj
	 * @return True if this abstract object would be affected by the parameter.
	 */
	public boolean affectedBy(AppObject obj) {
		AppAccessPath objAp = obj.getAccessPath();
		for (AppAccessPath existingAp : accessPaths) {
			if (existingAp.prefixedBy(objAp)) return true;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return accessPaths.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + type.hashCode();
		if (!isTypeOnly) { // sharon: added this check
			result = prime * result + accessPaths.hashCode();
		}
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof AbstractObject)) return false;
		AbstractObject other = (AbstractObject) obj;
		if (!type.equals(other.type)) return false;		
		if (isTypeOnly && other.isTypeOnly) return true; // sharon - added: don't care about access paths if obj is type only.
		if (isTypeOnly || other.isTypeOnly) return false; // sharon - added: if only one is type only - they are not equal.
		if (accessPaths.isEmpty()) return false;
		return accessPaths.equals(other.accessPaths);
	}

	public boolean isTypeOnly() {
		return isTypeOnly;
	}
	
	public AbstractObject cloneAsTypeOnly() {
		AbstractObject result = this.clone();
		result.isTypeOnly = true;
		return result;
	}
	
	/**
	 * Returns the join of this AbstractObject with another AbstractObject.
	 * The join is a union of all access paths, resulting in a "may" analysis.
	 */
	public AbstractObject join(AbstractObject obj) {
		AbstractObject result = clone();
		result.accessPaths.addAll(obj.accessPaths);
		return result;
	}

	public String getRepresentingTypeName() {
		return type.getFullName();
	}

	public boolean containsPathIn(AbstractObject obj) {
		for (AppAccessPath ap : obj.accessPaths) {
			if (accessPaths.contains(ap)) return true;
		}
		return false;
	}

	public boolean containsNoPaths() {
		return accessPaths.isEmpty();
	}
	
}
