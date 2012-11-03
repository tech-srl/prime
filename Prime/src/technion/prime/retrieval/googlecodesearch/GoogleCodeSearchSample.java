package technion.prime.retrieval.googlecodesearch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import com.google.gdata.data.codesearch.CodeSearchEntry;

import technion.prime.retrieval.OnlineCodeSample;
import technion.prime.statistics.AnalysisDetails;
import technion.prime.utils.ConcurrencyUtils;
import technion.prime.utils.JavaFileUtils;
import technion.prime.utils.Logger;
import technion.prime.utils.Logger.CanceledException;
import technion.prime.Options;


public class GoogleCodeSearchSample extends OnlineCodeSample {
	private static final long serialVersionUID = -1682276991305499232L;

	private enum PackageType {
		UNKNOWN, ARCHIVE_ZIP, ARCHIVE_TAR, ARCHIVE_TGZ, ARCHIVE_TBZ, SC_SVN, SC_CVS, SC_GIT,
	}

	private final String packageUrl;
	private final String packageName;
	private final String filenameInPackage;

	public GoogleCodeSearchSample(Options options, CodeSearchEntry e) {
		super(options);
		this.packageUrl = e.getPackage().getUri();
		this.packageName = e.getPackage().getName();
		this.filenameInPackage = e.getFile().getName();
	}

	@Override
	public String saveTo(String baseFolder) throws InterruptedException, CanceledException {
		String unpreparedFile = null;
		String preparedFile = null;
		try {
			if (getPackageType() == PackageType.UNKNOWN) {
				// Try getting the data directly from Google
//				String result = new GoogleCodeSearchDirectSample(entry).saveTo(baseFolder);
//				if (result != null) return result;
				Logger.warn("unsupported package type: " + packageUrl);
				reportUnsupportedRepositoryType();
				return null;
			}
			unpreparedFile = getFile(options.getTempDir());
			ConcurrencyUtils.checkState();
			if (unpreparedFile == null) {
				reportErrorInDownload("could not save file " + getPackageType() + ": " + packageUrl + ":" + filenameInPackage);
				return null;
			}
			preparedFile = JavaFileUtils.copyAndPackageTo(unpreparedFile,
					baseFolder, false);
			ConcurrencyUtils.checkState();
			return preparedFile;
		} catch (FileNotFoundException e) {
			try {
				ConcurrencyUtils.checkState();
			} catch (InterruptedException e1) {
				return null;
			}
			reportMissingFileInPackage();
		} catch (IOException e) {
			try {
				ConcurrencyUtils.checkState();
			} catch (InterruptedException e1) {
				return null;
			}
			reportErrorInDownload(e.getMessage());
		} finally {
			if (unpreparedFile != null
					&& unpreparedFile.equals(preparedFile) == false)
				new File(unpreparedFile).delete();
		}
		return null;
	}

	private void reportErrorInDownload(String message) {
		Logger.warn("IO error with package " + packageUrl + ": " + message);
		options.getOngoingAnalysisDetails().incrementField(AnalysisDetails.FAILED_DOWNLOADS);
	}

	private void reportMissingFileInPackage() {
		Logger.warn("file " + filenameInPackage + " could not be found in "
				+ packageUrl);
		options.getOngoingAnalysisDetails().incrementField(AnalysisDetails.FAILED_DOWNLOADS);
	}

	private String getFile(String toFolder) throws IOException,
			InterruptedException, CanceledException {
		String result = null;
		PackageType type = getPackageType();
		switch (type) {
		case ARCHIVE_ZIP:
		case ARCHIVE_TAR:
		case ARCHIVE_TGZ:
		case ARCHIVE_TBZ: {
			InputStream in = new URL(packageUrl).openStream();
			switch (type) {
			case ARCHIVE_ZIP:
				result = getFromZip(in, toFolder, filenameInPackage);
				break;
			case ARCHIVE_TAR:
				result = getFromTar(in, toFolder, filenameInPackage);
				break;
			case ARCHIVE_TGZ:
				result = getFromGZippedTar(in, toFolder, filenameInPackage);
				break;
			case ARCHIVE_TBZ:
				result = getFromBZippedTar(in, toFolder, filenameInPackage);
				break;
			}
			in.close();
		}
			break;
		case SC_SVN:
			result = getFromSvn(toFolder, packageUrl, filenameInPackage);
			break;
		case SC_CVS:
			result = getFromCvs(toFolder, packageUrl, filenameInPackage);
			break;
		case SC_GIT:
			result = getFromGit(toFolder, packageUrl, filenameInPackage);
			break;
		case UNKNOWN:
			assert (false);
			break;
		}
		return result;
	}

	private void reportUnsupportedRepositoryType() {
		options.getOngoingAnalysisDetails().incrementField(AnalysisDetails.UNSUPPORTED_REPOSITORIES);
	}

	private PackageType getPackageType() {
		if (gitSupported()
				&& (packageUrl.startsWith("git") || packageUrl.endsWith(".git")))
			return PackageType.SC_GIT;
		if (packageUrl.startsWith("svn"))
			return PackageType.SC_SVN;
		if (packageUrl.endsWith(".zip"))
			return PackageType.ARCHIVE_ZIP;
		if (packageUrl.matches(".*\\.(tgz|tar\\.gz)$"))
			return PackageType.ARCHIVE_TGZ;
		if (packageUrl.matches(".*\\.(tbz|tb2|tar\\.bz2)$"))
			return PackageType.ARCHIVE_TBZ;
		if (packageUrl.endsWith(".tar"))
			return PackageType.ARCHIVE_TAR;
		if (packageUrl.contains("svn"))
			return PackageType.SC_SVN; // Heuristic...
		if (cvsSupported() && packageUrl.isEmpty()
				&& packageName.startsWith("cvs")) {
			return PackageType.SC_CVS;
		}
		return PackageType.UNKNOWN;
	}

	private boolean cvsSupported() {
		return getCvsExecutablePath() != null
				&& getCvsExecutablePath().isEmpty() == false;
	}

	private boolean gitSupported() {
		return getGitExecutablePath() != null
				&& getGitExecutablePath().isEmpty() == false;
	}

	@Override
	public String toString() {
		return filenameInPackage;
	}
	
	/* (non-Javadoc)
	 * @see technion.prime.retrieval.CodeSample#getFilename()
	 */
	@Override
	public String getFilename() throws CanceledException, InterruptedException {
		return saveTo(options.getTempDir());
	}

}
