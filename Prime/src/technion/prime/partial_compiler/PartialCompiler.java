package technion.prime.partial_compiler;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.CompilationUnit;

import ca.mcgill.cs.swevo.ppa.PPAOptions;
import ca.mcgill.cs.swevo.ppa.ui.PPAUtil;

public class PartialCompiler {
	public static class LoadException extends Exception {
		private static final long serialVersionUID = 3762063373107022335L;

		public LoadException(String string) {
			super(string);
		}
	}
	
	static final String REQ_NAME = "req1";
	private static Map<String, LoadedFile> loadedFiles = new HashMap<String, LoadedFile>();
	private static boolean oldAutobuild;
	
	public static String getPackageFromSourceFile(String filename) throws IOException {
		return PPAUtil.getPackageFromFile(new File(filename));
	}
	
	public static LoadedFile loadFile(String filename) throws LoadException {
		if (loadedFiles.containsKey(filename) == false) {
			CompilationUnit cu = PPAUtil.getCU(new File(filename), new PPAOptions(), REQ_NAME, false);
			if (cu == null) throw new LoadException("Could not load compilation unit from file " + filename);
			loadedFiles.put(filename, new LoadedFile(cu));
		}
		return loadedFiles.get(filename);
	}
	
	private static IProject getPPAProject() {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(PPAUtil.getPPAProjectName(REQ_NAME));
	}

	public static void cleanup() {
		try {
			//PPAUtil.cleanUpAll(REQ_NAME);
			loadedFiles.clear();
			getPPAProject().delete(true, true, null);
		} catch (CoreException e) {
			// Swallow it
		}
	}

	public static void startBatch() {
		IWorkspace ws = ResourcesPlugin.getWorkspace();
		IWorkspaceDescription desc = ws.getDescription();
		oldAutobuild = desc.isAutoBuilding();
		desc.setAutoBuilding(false);
		try {
			ws.setDescription(desc);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	public static void endBatch() {
		IWorkspace ws = ResourcesPlugin.getWorkspace();
		IWorkspaceDescription desc = ws.getDescription();
		desc.setAutoBuilding(oldAutobuild);
		try {
			ws.setDescription(desc);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
}
