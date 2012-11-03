package technion.prime.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

public class ExternalApplicationRunner implements Runnable {
	private class Gobbler extends Thread {
		InputStream is;
		List<String> lines = new LinkedList<String>();

		Gobbler(InputStream is) {
			this.is = is;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			BufferedReader in = new BufferedReader(new InputStreamReader(is));
			while (true) {
				if (interrupted()) return;
				String line;
				try {
					line = in.readLine();
				} catch (IOException e) {
					break;
				}
				if (line == null) break;
				lines.add(line);
			}
		}
	}
	
	public static final int TIMED_OUT_RETURN_VALUE = -1;
	
	private final String commandline;
	private int returnValue;
	private Gobbler inputGobbler;
	private Gobbler errorGobbler;
	private long timeout;
	private boolean timedOut;
	
	public ExternalApplicationRunner(String commandline) {
		this.commandline = commandline;
	}
	
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		Process p = null;
		InputStream in = null;
		InputStream err = null;
		Thread waitForFinish = null;
		try {
			long start = System.currentTimeMillis();
			p = Runtime.getRuntime().exec(new String[] {"/bin/sh", "-c", commandline});
			in = p.getInputStream();
			err = p.getErrorStream();
			inputGobbler = new Gobbler(in);
			errorGobbler = new Gobbler(err);
			inputGobbler.start();
			errorGobbler.start();
			inputGobbler.join(timeout);
			long remaining = System.currentTimeMillis() - start;
			errorGobbler.join(remaining);
			final Process p2 = p;
			waitForFinish = new Thread(new Runnable() {
				@Override public void run() {
					try {
						p2.waitFor();
					} catch (InterruptedException e) {}
				}
			});
			waitForFinish.start();
			remaining = System.currentTimeMillis() - start;
			waitForFinish.join(remaining);
			returnValue = p.exitValue();
		} catch (IllegalThreadStateException e) {
			timedOut = true;
		} catch (IOException e) {
			Logger.exception(e);
		} catch (InterruptedException e) {
			Logger.exception(e);
		} finally {
			if (p != null) p.destroy();
			try {
				if (in != null) in.close();
			} catch (IOException e) {
				Logger.exception(e);
			}
			try {
				if (err != null) err.close();
			} catch (IOException e) {
				Logger.exception(e);
			}
			inputGobbler.interrupt();
			errorGobbler.interrupt();
			waitForFinish.interrupt();
		}
		
	}
	
	public List<String> getOutputLines() {
		return inputGobbler.lines;
	}
	
	public String getError() {
		if (timedOut) return "timed out after " + Logger.formattedDuration(timeout);
		String message = StringUtils.join(errorGobbler.lines, "\n");
		return message + "\n";
	}
	
	public int getReturnValue() {
		if (timedOut) return TIMED_OUT_RETURN_VALUE;
		return returnValue;
	}
}
