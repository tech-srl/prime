package technion.prime.dom;

public interface AppObject {
	public String getVarName();
	public AppType getType();
	public boolean isNull();
	public boolean isConstant();
	boolean isVariable();
	/**
	 * @return The access path containing this value.
	 */
	public AppAccessPath getAccessPath();
}
