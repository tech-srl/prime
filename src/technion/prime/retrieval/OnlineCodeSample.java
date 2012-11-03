package technion.prime.retrieval;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.tools.bzip2.CBZip2InputStream;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import technion.prime.Options;

import technion.prime.utils.ConcurrencyUtils;
import technion.prime.utils.Logger;
import technion.prime.utils.StreamGobbler;
import technion.prime.utils.Logger.CanceledException;

/**
 * Represents a code sample.
 */
public abstract class OnlineCodeSample implements CodeSample, Serializable {
	private static final long serialVersionUID = 4222145206519889887L;
	
	private static boolean svnInitialized = false;
	protected final Options options;
	
	
	public OnlineCodeSample(Options options) {
		this.options = options;
	}
	
	/**
	 * Save this code sample to a java file. The filename will be obtained by examining
	 * the public class of the code file.
	 * @param baseFolder Base folder for saving the file. The file might be ultimately saved
	 * to nested folders, depending on its package.
	 * @return The full name of the file saved.
	 * @throws CanceledException 
	 * @throws InterruptedException 
	 */
	public abstract String saveTo(String baseFolder) throws CanceledException, InterruptedException;
	
	protected String getFromZip(InputStream in, String folder, String filename)
			throws IOException, InterruptedException, CanceledException {
		ZipInputStream zin = new ZipInputStream(in);
		ZipEntry ze = zin.getNextEntry();
		while (ze != null && !ze.getName().equals(filename)) {
			zin.closeEntry();
			ze = zin.getNextEntry();
			ConcurrencyUtils.checkState();
		}
		return getFromInputStream(zin, folder, filename);
	}

	protected String getFromTar(InputStream in, String folder, String filename)
			throws IOException, InterruptedException, CanceledException {
		TarInputStream tin = new TarInputStream(in);
		TarEntry te = tin.getNextEntry();
		while (te != null && !te.getName().equals(filename)) {
			te = tin.getNextEntry();
			ConcurrencyUtils.checkState();
		}
		return getFromInputStream(tin, folder, filename);
	}

	protected String getFromGZippedTar(InputStream in, String folder, String filename)
			throws IOException, InterruptedException, CanceledException {
		GZIPInputStream gzis = new GZIPInputStream(in);
		BufferedInputStream bis = new BufferedInputStream(gzis);
		return getFromTar(bis, folder, filename);
	}

	protected String getFromBZippedTar(InputStream in, String folder, String filename)
			throws IOException, InterruptedException, CanceledException {
		CBZip2InputStream bzis = new CBZip2InputStream(in);
		BufferedInputStream bis = new BufferedInputStream(bzis);
		return getFromTar(bis, folder, filename);
	}

	protected String getFromGit(final String folder, String url, final String filename) throws IOException,
			InterruptedException, CanceledException {
		String commandline = getGitExecutablePath() + " archive" + " --remote="
			+ url + " HEAD" + " " + filename;
		Logger.debug("> " + commandline);
		return getFromExternalProcess(commandline, new InputHandler() {
			@Override
			public String handle(InputStream in) throws IOException,
			InterruptedException, CanceledException {
				return getFromTar(in, folder, filename);
			}
		}, toStringHandler);
	}

	protected String getFromSvn(String folder, String url, String filename) throws IOException,
			InterruptedException, CanceledException {
		if (svnInitialized == false) {
			// This can still be called multiple times from different threads;
			// however there's no harm in multiple calls while there IS a cost
			// for synchronizing this, so this section isn't marked as critical.
			initSvn();
			svnInitialized = true;
		}
		String name = "";
		String password = "";
		SVNRepository repo = null;
		ByteArrayInputStream bais = null;
		try {
			repo = SVNRepositoryFactory.create(SVNURL
					.parseURIEncoded(url));
			ISVNAuthenticationManager authManager = SVNWCUtil
			.createDefaultAuthenticationManager(name, password);
			repo.setAuthenticationManager(authManager);

			SVNProperties repoProperties = new SVNProperties();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			repo.getFile(filename, -1, repoProperties, baos);
			String mimeType = repoProperties
			.getStringValue(SVNProperty.MIME_TYPE);
			if (SVNProperty.isTextMimeType(mimeType) == false) {
				Logger.warn("SVN entry is not a text file.");
				return null;
			}
			// Full buffer... expensive in memory:
			bais = new ByteArrayInputStream(baos.toByteArray());
			return getFromInputStream(bais, folder, filename);
		} catch (SVNException e) {
			Logger.warn("could not get file from SVN repository: "
					+ e.getMessage());
		} finally {
			if (bais != null)
				bais.close();
			if (repo != null)
				repo.closeSession();
		}
		return null;
	}

	protected static void initSvn() {
		DAVRepositoryFactory.setup();
		SVNRepositoryFactoryImpl.setup();
		FSRepositoryFactory.setup();
	}

	protected String getFromCvs(final String folder, String url, String filename)
			throws IOException, InterruptedException, CanceledException {
		String[] parts = url.split(" ");
		String repoAccessString = parts[1];
		String moduleString = parts[2];
		if (moduleString != null) {
			if (moduleString.equals(".")) {
				moduleString = "";
			} else {
				moduleString += "/";
			}

		}
		String commandline = getCvsExecutablePath() + // CVS executable
		" -Q" + // quiet mode
		" -d" + repoAccessString + // repository path (with URL and
		// login details)
		" checkout" + // the CVS command
		" -p" + // send file content to standard output
		" " + moduleString + filename; // filename

		String content = getFromExternalProcessWithThreads(commandline, null,
				null);

		return getFromString(content, folder, filename);
	}
	
	protected String getGitExecutablePath() {
		return options.getGitExecutablePath();
	}

	protected String getCvsExecutablePath() {
		return options.getCvsExecutablePath();
	}
	
	protected interface InputHandler {
		String handle(InputStream in) throws IOException, InterruptedException,
				CanceledException;
	}

	static protected InputHandler toStringHandler = new InputHandler() {
		@Override
		public String handle(InputStream in) throws IOException {
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(in));
			StringBuilder sb = new StringBuilder();
			int c;
			while ((c = reader.read()) != -1)
				sb.append((char) c);
			return sb.toString();
		}
	};
	
	/**
	 * Right now this is just eating the cvs output and printing it to stdout it
	 * should never get stuck, but results are not used down the line.
	 * 
	 * @param commandline
	 * @param inHandler
	 * @param errHandler
	 * @return Process result.
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws CanceledException
	 */
	protected synchronized String getFromExternalProcessWithThreads(String commandline,
			InputHandler inHandler, InputHandler errHandler)
			throws IOException, InterruptedException, CanceledException {
		
		StringBuilder sb = new StringBuilder();

		final Process proc = Runtime.getRuntime().exec(commandline);
		
		StreamGobbler errorGobbler = new StreamGobbler(
				proc.getErrorStream(), "ERROR", System.err, null);

		StreamGobbler outputGobbler = new StreamGobbler(
				proc.getInputStream(), "OUTPUT",null, sb);
		
		errorGobbler.start();
		outputGobbler.start();
		
		outputGobbler.join();
		errorGobbler.join();
		
		// have to wait a bit to avoid the server aborting our cvs call 
		Thread.sleep(30000);
		
		int status = proc.waitFor();
		if (status != 0) {
			Logger.warn("Failed download using " + commandline);
		}
//		Logger.debug("> " + commandline + " => " + sb.length());
		return sb.toString();
	}

	protected String getFromExternalProcess(String commandline,
			InputHandler inHandler, InputHandler errHandler)
			throws IOException, InterruptedException, CanceledException {
		Logger.debug("> " + commandline);
		final Process proc = Runtime.getRuntime().exec(commandline);
		InputStream err = proc.getErrorStream();
		InputStream in = proc.getInputStream();

		String error = errHandler.handle(err);
		String filename = inHandler.handle(in);

		int status = 1;
		try {
			status = proc.waitFor();
		} finally {
			in.close();
			err.close();
			proc.destroy();
		}
		if (status != 0) {
			throw new IOException(error.replaceAll("\n", " ").trim());
		}
		return filename;
	}

	private String getFromString(String in, String folder, String filename)
			throws IOException {
		String basename = FilenameUtils.getName(filename);
		String uncompressedFilename = FilenameUtils.concat(folder, basename);
		File uncompressedFile = new File(uncompressedFilename);
		if (uncompressedFile.exists()) {
			return uncompressedFilename;
		}

		BufferedWriter bw = new BufferedWriter(new FileWriter(
				uncompressedFilename));
		bw.write(in);
		bw.flush();
		bw.close();

		return uncompressedFilename;
	}

	protected String getFromInputStream(InputStream in, String folder, String filename)
			throws IOException, InterruptedException, CanceledException {
		String baseName = FilenameUtils.getName(filename);
		String uncompressedFilename = FilenameUtils.concat(folder, baseName);
		File uncompressedFile = new File(uncompressedFilename);
		if (uncompressedFile.exists()) {
			return uncompressedFilename;
		}
		FileOutputStream fos = FileUtils.openOutputStream(new File(
				uncompressedFilename));
		copy(in, fos);
		return uncompressedFilename;
	}

	protected void copy(InputStream in, OutputStream out) throws IOException,
			InterruptedException, CanceledException {
		int available = 0;
		while ((available = in.available()) > 0) {
			byte[] buffer = new byte[available];
			in.read(buffer);
			if (in.available() == 0)
				out.write(buffer, 0, available - 1);
			else
				out.write(buffer);
			ConcurrencyUtils.checkState();
		}
		in.close();
		out.close();
	}
}
