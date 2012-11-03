package technion.prime.analysis;

/**
 * A statement identifier in the input code.
 */
public class Label {
	private Object uniqueObject;
	
	/**
	 * Create a new label from an object guaranteed to be unique per statement.
	 * @param uniqueObject An object which is unique per statement.
	 */
	public Label(Object uniqueObject) {
		this.uniqueObject = uniqueObject;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj instanceof Label == false) return false;
		return uniqueObject.equals(((Label)obj).uniqueObject);
	}
	
	@Override
	public int hashCode() {
		return uniqueObject.hashCode();
	}
	
	@Override
	public String toString() {
		return hashCode() + ":" + uniqueObject.toString();
	}
}
