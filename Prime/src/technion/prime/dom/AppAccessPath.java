package technion.prime.dom;

public interface AppAccessPath {
	/**
	 * Whether another access path prefixes this one.
	 * @param ap
	 * @return Whether the argument prefixes this access path. Also returns true
	 * if both are equal.
	 */
	public boolean prefixedBy(AppAccessPath ap);
	
	public int getLength();
	
	/**
	 * Return a new access path made from the concatenation of this access path with another one.
	 * The containing class / method will be determined by the receiver.
	 * @param ap
	 * @return A new access path.
	 */
	public AppAccessPath concat(AppAccessPath ap);
	
	/**
	 * Checks whether this access path stands for a local variable. "this" and parameters
	 * are also locals.
	 * @return True if this access path stands for a local variable.
	 */
	public boolean isLocal();
	
	/**
	 * @return True if this access path stands for a field.
	 */
	public boolean isField();
	
	@Override
	public String toString();
	
	/**
	 * Return a new access path composed of the last n elements of this access path.
	 * @param n
	 * @return A new access path.
	 */
	public AppAccessPath getSuffix(int n);
}
