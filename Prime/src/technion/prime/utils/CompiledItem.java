package technion.prime.utils;

public abstract class CompiledItem {
	protected String filename;
	protected String className;
	protected String basePath;
	
	/**
	 * @return Filename containing this compiled item. May either be a .class
	 * file or a .jar file.
	 */
	public String getFilename() {
		return filename;
	}
	
	/**
	 * @return Full class name.
	 */
	public String getClassName() {
		return className;
	}
	
	/**
	 * @return Path that needs to be added to the classpath in order to load
	 * this item.
	 */
	public String getBasePath() {
		return basePath;
	}
	
	
	
}
