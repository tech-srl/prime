package technion.prime.retrieval.googlecodesearch;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import technion.prime.Options;

import com.google.gdata.data.codesearch.CodeSearchEntry;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import technion.prime.retrieval.OnlineCodeSample;
import technion.prime.statistics.AnalysisDetails;
import technion.prime.utils.JavaFileUtils;
import technion.prime.utils.Logger;

public class GoogleCodeSearchDirectSample extends OnlineCodeSample {
	private static final long serialVersionUID = -2509000102144265604L;
	
	private final String packageUrl;
	private final String filenameInPackage;
	private final String id;

	public GoogleCodeSearchDirectSample(Options options, CodeSearchEntry e) {
		super(options);
		this.packageUrl = e.getPackage().getUri();
		this.id = e.getId();
		this.filenameInPackage = e.getFile().getName();
	}
	
	@Override
	public String saveTo(String baseFolder) {
		String packageId = getPackageId();
		String address = String.format(
				"http://codesearch.google.com/codesearch/json?file_info_request=b&package_id=%s&path=%s&highlight_query=&file_info_request=e&",
				packageId, filenameInPackage);
		InputStreamReader in = null;
		String unprepared = null;
		String prepared = null;
		try {
			in = new InputStreamReader(new URL(address).openStream());
			unprepared = saveFromJson(baseFolder, in);
			prepared = JavaFileUtils.copyAndPackageTo(unprepared, baseFolder, false);
		} catch (MalformedURLException e) {
			reportUnavailableFile(e.getMessage());
		} catch (IOException e) {
			reportErrorInDownload(e.getMessage());
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (unprepared != null && unprepared.equals(prepared) == false) {
				new File(unprepared).delete();
			}
		}
		return prepared;
	}
	
	private String getPackageId() {
		int hashIndex = id.indexOf("#");
		return id.substring(hashIndex + 1, id.indexOf("/", hashIndex));
	}

	private void reportUnavailableFile(String message) {
		Logger.warn("Unavailable file in package " + packageUrl + ": " + message);
		options.getOngoingAnalysisDetails().incrementField(AnalysisDetails.FAILED_DOWNLOADS);
	}

	private void reportErrorInDownload(String message) {
		Logger.warn("IO error with package " + packageUrl + ": " + message);
		options.getOngoingAnalysisDetails().incrementField(AnalysisDetails.FAILED_DOWNLOADS);
	}

	private String saveFromJson(String baseFolder, InputStreamReader in) throws IOException {
		Gson gson = new Gson();
		JsonResponse1 r1 = gson.fromJson(new JsonReader(in), JsonResponse1.class);
		JsonResponse2 r2 = r1.file_info_response[0];
		String filename = FilenameUtils.getName(r2.file_info.name);
		String content = stripHtml(r2.file_info.html_content);
		FileUtils.writeStringToFile(new File(filename), content);
		return filename;
	}
	
	private String stripHtml(String s) {
		// This implementation is actually incorrect, because
		// the tag
		// <a title=">">
		// will leave
		// ">
		// But for this specific case, it's good enough, since we know precisely
		// what the HTML contains.
		String stripped = s.replaceAll("<[^>]+>", "");
		// Repair XML stuff
		stripped = stripped.replaceAll("&lt;", "<");
		stripped = stripped.replaceAll("&gt;", ">");
		stripped = stripped.replaceAll("&quot;", "\"");
		stripped = stripped.replaceAll("&amp;", "&"); // must be last
		return stripped;
	}

	private static class JsonResponse1 {
		JsonResponse2[] file_info_response;
	}
	
	private static class JsonResponse2 {
		JsonResponse3 file_info;
	}
	
	private static class JsonResponse3 {
		String name;
		String html_content;
	}

	/* (non-Javadoc)
	 * @see technion.prime.retrieval.CodeSample#getFilename()
	 */
	@Override
	public String getFilename() {
		return saveTo(options.getTempDir());
	}

}
