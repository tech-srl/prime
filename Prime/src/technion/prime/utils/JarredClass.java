package technion.prime.utils;

public class JarredClass extends CompiledItem {
	public JarredClass(String filename, String className) {
		this.filename = filename;
		this.className = className;
		this.basePath = filename;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return filename + " : " + className;
	}
}
