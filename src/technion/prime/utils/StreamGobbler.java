package technion.prime.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

public class StreamGobbler extends Thread {
	InputStream is;
	String type;
	OutputStream os;
	StringBuilder sb;

	public StreamGobbler(InputStream is, String type) {
		this(is, type, null);
	}

	public StreamGobbler(InputStream is, String type, OutputStream redirect) {
		this.is = is;
		this.type = type;
		this.os = redirect;
	}

	public StreamGobbler(InputStream is, String type, OutputStream redirect, StringBuilder sb) {
		this.is = is;
		this.type = type;
		this.os = redirect;
		this.sb = sb;
	}
	
	@Override
	public void run() {
		try {
			PrintWriter pw = null;
			if (os != null)
				pw = new PrintWriter(os);

			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line = null;
			while ((line = br.readLine()) != null) {
				if (pw != null)
					pw.println(line);
				if (sb != null) {
					sb.append(line);
					sb.append("\n");
				}
			}
			if (pw != null)
				pw.flush();
		} catch (IOException ioe) {
			Logger.exception(ioe);
		}
	}
}