package technion.prime.utils;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import technion.prime.Options;
import technion.prime.PrimeAnalyzer;

import technion.prime.history.HistoryCollection;
import technion.prime.history.edgeset.EdgeHistoryCollection;
import technion.prime.partial_compiler.PartialCompiler;

public class JavaFileUtils {
	
	/**
	 * Given a filename of a java source file, returns the base path - i.e.
	 * the path that should be used for the classpath.
	 * 
	 * For example, if the file
	 * /a/b/c/MyClass.java
	 * contains the class with the full name b.c.MyClass, then this method will return
	 * the path "/a/".
	 * 
	 * @param filename
	 * @return The base file path.
	 * @throws IOException If the file could not be accessed or does not contain a class.
	 */
	public static String getBaseFolderFromSourceFile(String filename) throws IOException {
		String className = getClassNameFromSourceFile(filename);
		String suffix = className.replaceAll("\\.", "/");
		return filename.substring(0, filename.lastIndexOf(suffix));
	}
	
	/**
	 * Given a filename of a java class file, returns the base path - i.e.
	 * the path that should be used for the classpath.
	 * 
	 * For example, if the file
	 * /a/b/c/MyClass.class
	 * contains the class with the full name b.c.MyClass, then this method will return
	 * the path "/a/".
	 * 
	 * @param filename
	 * @return The base class path.
	 * @throws IOException
	 */
	public static String getBaseFolderFromClassFile(String filename) throws IOException {
		String className = getClassNameFromClassFile(filename);
		return getBaseFolderFromClassFile(filename, className);
	}
	
	/**
	 * Given a filename of a java class file, returns the base path - i.e.
	 * the path that should be used for the classpath.
	 * 
	 * For example, if the file
	 * /a/b/c/MyClass.class
	 * contains the class with the full name b.c.MyClass, then this method will return
	 * the path "/a/".
	 * 
	 * @param filename
	 * @param className The fully-qualified name of the class inside this file.
	 * Use the other overload of this method if you don't know it.
	 * @return The base class path.
	 * @throws IOException If the classfile is not in an appropriate path on the file system.
	 */
	public static String getBaseFolderFromClassFile(String filename, String className) throws IOException {
		try {
			int classNamePartLength = className.length() + ".class".length();
			return filename.substring(0, filename.length() - classNamePartLength);
		} catch (IndexOutOfBoundsException e) {
			throw new IOException("classfile not properly located on filesystem");
		}
	}

	/**
	 * Copy a file to another folder, and also move it to the appropriate package.
	 * So copyAndPackageTo("mydir/myfile.java", "myroot"), with myfile.java containing
	 * the class x.y.A, will result in copying myfile.java to
	 * "myroot/x/y/myfile.java"
	 * @param filename
	 * @param root
	 * @return The full path of the new copy of the file.
	 * @throws IOException
	 */
	public static String copyAndPackageTo(String filename, String root, boolean overwrite) throws IOException {
		String folder = getPackageFromSourceFile(filename).replaceAll("\\.", "/");
		String newFilename = FilenameUtils.concat(
				FilenameUtils.concat(root, folder), FilenameUtils.getName(filename));
		if (filename.equals(newFilename)) return filename;
		if (overwrite == false && new File(newFilename).exists()) return newFilename;
		FileUtils.copyFile(new File(filename), new File(newFilename));
		return newFilename;
	}
	
	private static String getPackageFromSourceFile(String filename) throws IOException {
		return PartialCompiler.getPackageFromSourceFile(filename);
	}

	/**
	 * Given a Java code file, returns the FULL name of the public class within.
	 * @param filename
	 * @return Fully-qualified name of the public class defined in the file.
	 * @throws IOException 
	 */
	public static String getClassNameFromSourceFile(String filename) throws IOException {
		String packageName = getPackageFromSourceFile(filename);
		return (packageName.isEmpty() ? "" : packageName + ".") + FilenameUtils.getBaseName(filename);
	}
	
	public static String getClassNameFromClassFile(String baseDir, String filename) {
		assert(filename.startsWith(baseDir));
		String result = filename; // Not really needed but prettier
		result = result.substring(baseDir.length());
		result = result.replaceFirst("\\.class$", "");
		result = result.replaceAll("[/\\\\]+", ".");
		if (result.startsWith(".")) result = result.substring(1);

		return result;
	}
	
	/**
	 * This gets the fully-qualified class name from a class file.
	 * Code is from <a href="http://stackoverflow.com/a/1650442/242762">this post</a> by
	 * <a href="http://stackoverflow.com/users/180659/jarnbjo">jarnbjo</a>.
	 * Algorithm is based on the <a href="http://java.sun.com/docs/books/jvms/second_edition/html/ClassFile.doc.html">Java class file format</a>.
	 * @param filename
	 * @return The fully-qualified class name, e.g. "a.b.T"
	 * @throws IOException
	 */
	public static String getClassNameFromClassFile(String filename) throws IOException {
		InputStream is = new FileInputStream(filename);
		DataInputStream dis = new DataInputStream(is);
		dis.readLong(); // skip header and class version
		int constant_pool_count = (dis.readShort()&0xffff)-1;
		short[] classes = new short[constant_pool_count];
		String[] strings = new String[constant_pool_count];
		for (int i = 0 ; i < constant_pool_count ; i++) {
			int t = dis.read();
			switch(t) {
			case 1: // utf 8
				strings[i] = dis.readUTF(); break;
			case 3: // integer
			case 4: // float
			case 9: // fieldref
			case 10: // methodref
			case 11: // interface methodref
			case 12: // name and type
				dis.readInt(); break;
			case 5: // long
			case 6: // double
				dis.readLong(); i++; break;
			case 7: // class
				classes[i] = dis.readShort(); break;
			case 8: // string
				dis.readShort(); break;
			default:
				throw new IOException("Malformed class file");
			}
		}
		dis.readShort(); // skip access flags
		int thisClass = dis.readShort()&0xffff;
		String result = null;
		try {
			result = strings[classes[thisClass-1]-1].replace('/', '.');
		} catch (IndexOutOfBoundsException e) {
			throw new IOException("Malformed class file");
		} finally {
			is.close();
		}
		return result;
	}
	
	/**
	 * Given a pattern that might refer to a class or package name, returns that name.
	 * 
	 * For example
	 * 
	 * ".*a\.b\.C.*" -> a.b.C
	 * 
	 * @param filter
	 * @return Class or package name.
	 */
	public static String getClassNameFromPattern(String filter) {
		return filter.replaceAll("\\.\\*", "").replaceAll("\\\\\\.", ".");
	}
	
	/**
	 * Given a full class name, get the name of the package in which it is defined.
	 * Given a package, just returns it.
	 * If the input doesn't look like a name of a class or package, it is returned as is.
	 * 
	 * Capital letters are used to discern whether the input is a class or package.
	 * 
	 * For example:
	 * 
	 * "a.b.c" -> "a.b.c"
	 * "a.b.C" -> "a.b"
	 * "hello there" -> "hello there"
	 * 
	 * @param s Class or package name.
	 * @return Full class name.
	 */
	public static String getSurroundingPackage(String s) {
		if (s.isEmpty()) return s;
		// If it doesn't look like a class name, just return it.
		if (s.matches("^[a-zA-Z0-9._]+$") == false) return s;
		
		String[] parts = s.split("\\.");
		if (parts.length == 1) return s;
		String result = s;
		if (Character.isUpperCase(parts[parts.length - 1].charAt(0))) {
			result = s.substring(0, s.lastIndexOf("."));
		}
		return result;
	}
	
	public static String getRegexForSurroundingPackage(String classOrPackageRegex) {
		String s = getClassNameFromPattern(classOrPackageRegex);
		// If it doesn't look like a class name, just return it.
		if (s.matches("^[a-zA-Z0-9._]+$") == false) return classOrPackageRegex;
		
		String[] parts = s.split("\\\\\\.");
		if (parts.length == 1) return classOrPackageRegex;
		String result = s;
		if (Character.isUpperCase(parts[parts.length - 1].charAt(0))) {
			result = s.substring(0, s.lastIndexOf("\\."));
		}
		return result;
	}
	
	/**
	 * Given a folder path, return all the java source files in that folder.
	 * @param folder
	 * @return An array of file paths.
	 * @throws InterruptedException 
	 */
	public static List<String> getJavaFilesInFolder(String folder, boolean recurse) throws InterruptedException {
		return getAllFilesInFolder(folder, "java", recurse);
	}
	
	/**
	 * Given a folder path, return all the java class files in that folder.
	 * @param folder
	 * @return An array of file paths.
	 * @throws InterruptedException 
	 */
	public static List<String> getClassFilesInFolder(String folder, boolean recurse) throws InterruptedException {
		return getAllFilesInFolder(folder, "class", recurse);
	}
	
	/**
	 * Given a folder path, return all the jar files in that folder.
	 * @param folder
	 * @param recurse
	 * @return An array of file paths.
	 * @throws InterruptedException
	 */
	public static List<String> getJarsInFolder(String folder, boolean recurse) throws InterruptedException {
		return getAllFilesInFolder(folder, "jar", recurse);
	}
	
	/**
	 * Get all the files in a folder (possibly including subfolders) with a given extension.
	 * This method is interruptable.
	 * @param folder The path of the folder.
	 * @param extension The extension to use. Do not include the dot (.).
	 * @param recursive Whether to collect files from subfolders (and their subfolders, etc.) as well.
	 * @return An array of file paths.
	 * @throws InterruptedException 
	 */
	private static List<String> getAllFilesInFolder(String folder, String extension, boolean recursive) throws InterruptedException {
		List<String> results = new LinkedList<String>();
		File root = new File(folder);
		for (File f : root.listFiles()) {
			if (Thread.interrupted()) throw new InterruptedException();
			if (recursive && f.isDirectory()) results.addAll(getAllFilesInFolder(f.getAbsolutePath(), extension, recursive));
			else if (f.getName().endsWith("." + extension)) results.add(f.getAbsolutePath());
		}
		return results;
	}

	public static List<String> getCachedFilesInFolder(String path, boolean recursive) throws InterruptedException {
		return getAllFilesInFolder(path, PrimeAnalyzer.Extension.CACHED_RESULT.get(), recursive);
	}
	
	/**
	 * Converts bytecode-style class signature, e.g.
	 * <code>Ljava/lang/String;</code>
	 * to java-style class qualified class name, e.g.
	 * <code>java.lang.String</code>
	 * Also works on generic types.
	 * @param sig
	 * @return
	 */
	public static String signature2classname(String sig) {
		if (sig == null) return null;
		sig = sig.replace('/', '.');
		sig = sig.replaceAll("\\bL", "");
		sig = sig.replaceAll(";", " ");
		return sig;
	}
	
	/**
	 * Loads all cached history collections found at the {@code source}.
	 * If source is a file name, it is loaded as a cached history collection.
	 * If source is a folder name, all the history collections directly within it (but not in
	 * sub-folders) are loaded and a new history is returned which contains all the histories
	 * across all the loaded history collections.
	 * 
	 * @param options Prime options for use in loading.
	 * @param source File or folder name.
	 * @return A history collection loaded from the source.
	 * @throws IOException If any file could not be properly opened as a cached history collection,
	 * or if source is neither a file nor a folder.
	 */
	public static HistoryCollection loadAllHistoryCollections(Options options, String source)
			throws IOException {
		HistoryCollection result = null;
		File f = new File(source);
		if (f.isFile()) {
			result = HistoryCollection.load(source, EdgeHistoryCollection.class);
		} else if (f.isDirectory()) {
			String[] filenames = f.list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(PrimeAnalyzer.Extension.CACHED_RESULT.get());
				}
			});
			result = options.newHistoryCollection();
			for (String filename : filenames) {
				HistoryCollection loaded =
						HistoryCollection.load(source + "/" + filename, EdgeHistoryCollection.class);
				result.addAll(loaded.getHistories());
			}
		}
		if (result == null) throw new IOException(f.toString() + " is neither a file nor a folder");
		result.recursivelySetOptions(options);
		return result;
	}

}
