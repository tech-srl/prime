package technion.prime.partial_compiler;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ClassFile;

import technion.prime.utils.CompiledItem;

import technion.prime.partial_compiler.PartialCompiler.LoadException;
import technion.prime.utils.Logger;

import ca.mcgill.cs.swevo.ppa.ui.PPAUtil;

@SuppressWarnings("restriction")
public class LoadedFile {
	private CompilationUnit cu;
	
	public LoadedFile(CompilationUnit cu) {
		this.cu = cu;
	}

	public String getPackage() {
		PackageDeclaration pd = cu.getPackage();
		return pd == null ? "" : pd.getName().getFullyQualifiedName();
	}

	private File getTargetDir(String baseDir) {
		File f = new File(FilenameUtils.concat(baseDir, getPackage().replaceAll("\\.", "/")));
		f.mkdirs();
		return f;
	}

	public LinkedList<CompiledItem> compile(String baseDir) {
		try {
			ClassFile[] classFiles = PPAUtil.compileCU(cu, getTargetDir(baseDir));
			LinkedList<CompiledItem> result = new LinkedList<CompiledItem>();
			for (ClassFile c : classFiles) {
				result.add(new PPACompiledClass(c, baseDir));
			}
			return result;
		} catch (AssertionError e) {
			return new LinkedList<CompiledItem>();
		}
	}
	
	public class CodeSegment {
		public String methodName;
		public int startLine;
		public int endLine;
	}
	
	public LinkedList<CodeSegment> isolate(String isolationFolder) {
		// The goal is to isolate the method which cannot get compiled.
		LinkedList<CodeSegment> result = new LinkedList<CodeSegment>();
		AST ast = cu.getAST();
		
		// Go over each type
		for (Object o1 : cu.types()) {
			int num = 0;
			AbstractTypeDeclaration atd = (AbstractTypeDeclaration)o1;
			if (atd instanceof TypeDeclaration == false) continue;
			TypeDeclaration td = (TypeDeclaration)atd;
			// Go over all methods
			for (MethodDeclaration md : td.getMethods()) {
				// Create a new compilation unit with just this type and just this method.
				// This is done by a full copy and then deleting anything unmatching.
				CompilationUnit partial = (CompilationUnit)ASTNode.copySubtree(ast, cu);
				CompilationUnit.copySubtree(ast, cu);
				for (Object o2 : partial.types()) {
					AbstractTypeDeclaration partialAtd = (AbstractTypeDeclaration)o2;
					if (partialAtd instanceof TypeDeclaration == false) continue;
					TypeDeclaration partialTd = (TypeDeclaration)partialAtd;
					if (td.getName().getIdentifier().equals(partialTd.getName().getIdentifier()) == false) {
						partialAtd.delete();
					} else {
						// Same type.
						
						// Remove 'final' from all fields:
						for (FieldDeclaration partialFd : partialTd.getFields()) {
							@SuppressWarnings("rawtypes")
							Iterator iter = partialFd.modifiers().iterator();
							while (iter.hasNext()) {
								IExtendedModifier mod = (IExtendedModifier)iter.next();
								if (mod instanceof Modifier && ((Modifier)mod).isFinal()) {
									iter.remove();
								}
							}
						}
						
						// Remove all methods except the current one:
						for (MethodDeclaration partialMd : partialTd.getMethods()) {
							if (md.getName().getIdentifier().equals(partialMd.getName().getIdentifier()) == false) {
								partialMd.delete();
							}
						}
					}
				}
				if (canBeCompiled(partial, num++, isolationFolder)) continue;
				// If we got here, it cannot be compiled!
				// It means md is a trouble method in the trouble type td.
				CodeSegment cs = new CodeSegment();
				cs.methodName = md.getName().getFullyQualifiedName();
				cs.startLine = cu.getLineNumber(md.getStartPosition());
				cs.endLine = cu.getLineNumber(md.getStartPosition() + md.getLength());
				result.add(cs);
			}
		}
		return result;
	}
	
	private boolean canBeCompiled(CompilationUnit partial, int n, String isolationFolder) {
		String oldTypename = ((AbstractTypeDeclaration)partial.types().get(0)).getName().getFullyQualifiedName();
		String newTypename = oldTypename + n;
		String content = partial.toString();
		content.replaceAll(Pattern.quote(oldTypename), newTypename);
		String filename = newTypename + ".java";
		File file = new File(filename);
		try {
			FileUtils.writeStringToFile(file, content);
			LinkedList<CompiledItem> classes = PartialCompiler.loadFile(filename).compile(isolationFolder);
			return classes != null && classes.size() > 0;
		} catch (AssertionError e) {
			Logger.exception(e);
			return false;
		} catch (IOException e) {
			Logger.exception(e);
			return false;
		} catch (LoadException e) {
			Logger.exception(e);
			return false;
		}
	}

}
