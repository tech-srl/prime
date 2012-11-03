package technion.prime.partial_compiler;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.jdt.internal.compiler.ClassFile;

import technion.prime.utils.CompiledItem;

@SuppressWarnings("restriction")
public class PPACompiledClass extends CompiledItem {
	private final ClassFile cf;
	
	public PPACompiledClass(ClassFile cf, String baseFolder) {
		this.cf = cf;
		this.filename = getFilenameFromBaseFolder(baseFolder);
		this.basePath = baseFolder;
	}
	
	private String getFilenameFromBaseFolder(String baseFolder) {
		return FilenameUtils.concat(baseFolder, new String(cf.fileName()) + ".class");
	}

	@Override
	public String getFilename() {
		return filename;
	}
	
	@Override
	public String getClassName() {
		StringBuilder result = new StringBuilder();
		for (char[] part : cf.getCompoundName()) {
			if (result.length() > 0) result.append(".");
			result.append(part);
		}
		return result.toString();
	}
}
