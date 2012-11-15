package technion.prime.partial_compiler;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.jdt.internal.compiler.ClassFile;

import technion.prime.utils.CompiledItem;


@SuppressWarnings("restriction")
public class PPACompiledClass extends CompiledItem {

	private String cfFileName;
	private String cfCompoundName;

	public PPACompiledClass(ClassFile cf, String baseFolder) {
		this.cfFileName = cf.fileName().toString();

		StringBuilder result = new StringBuilder();
		for (char[] part : cf.getCompoundName()) {
			if (result.length() > 0) result.append(".");
			result.append(part);
		}

		this.cfCompoundName = result.toString();

		this.filename = getFilenameFromBaseFolder(baseFolder);
		this.basePath = baseFolder;
	}

	private String getFilenameFromBaseFolder(String baseFolder) {
		return FilenameUtils.concat(baseFolder, cfFileName + ".class");
	}

	@Override
	public String getFilename() {
		return filename;
	}

	@Override
	public String getClassName() {
		return cfCompoundName;
	}
}
