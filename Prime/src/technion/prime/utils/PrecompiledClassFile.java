
package technion.prime.utils;

import java.io.IOException;

public class PrecompiledClassFile extends CompiledItem {
	public PrecompiledClassFile(String filename) throws IOException {
		this.filename = filename;
		this.className = JavaFileUtils.getClassNameFromClassFile(filename);
		this.basePath = JavaFileUtils.getBaseFolderFromClassFile(filename);
	}
}
