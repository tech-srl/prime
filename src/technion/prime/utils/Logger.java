package technion.prime.utils;

import java.io.PrintStream;
import java.util.Calendar;
import java.util.LinkedList;

import org.eclipse.core.runtime.IProgressMonitor;

import technion.prime.Options;
import technion.prime.eclipse.EclipseOptions;

public class Logger {
	/**
	 * Thrown when the user requested canceling the operation.
	 * Unlike InterruptedException, if this exception is thrown it means
	 * that the entire process needs to be stopped, not just the current operation.
	 */
	@SuppressWarnings("serial")
	public static class CanceledException extends Exception {}
	
	public Logger(Options options) {
		this.options = options;
	}

	private boolean monitoredByEclipse() {
		return options.isMonitoredByEclipse();
	}
	
	private IProgressMonitor getEclipseMonitor() {
		return ((EclipseOptions)options).getEclipseMonitor();
	}
	
	private class ProcessStage {
		private final String name;
		private final long startTime;
		private final int workTotal;
		private final int eclipseWeightForThisStage;

		private long endTime = -1;
		private int workDone;
		private int workReportedToEclipse;
		
		public ProcessStage(Stage stage, int work) {
			this.name = stage.getName();
			workTotal = work;
			eclipseWeightForThisStage = stage.getWeight();
			startTime = System.currentTimeMillis();
		}
		
		public void progress() throws InterruptedException, CanceledException {
			workDone++;
			ConcurrencyUtils.checkState();
			if (monitoredByEclipse()) eclipseProgress();
		}
		
		private void eclipseProgress() {
			double workFraction = (double)workDone / workTotal;
			int whatEclipseShouldShow = (int)(workFraction * eclipseWeightForThisStage);
			if (whatEclipseShouldShow > workReportedToEclipse) {
				int work = whatEclipseShouldShow - workReportedToEclipse;
				getEclipseMonitor().worked(work);
				workReportedToEclipse += work;
			}
		}
		
		public void end() {
			endTime = System.currentTimeMillis();
			if (monitoredByEclipse()) getEclipseMonitor().worked(eclipseWeightForThisStage - workReportedToEclipse);
		}
		
		public long getDurationMillis() {
			if (endTime == -1) throw new IllegalStateException("stage still not ended");
			return endTime - startTime;
		}

		public String getName() {
			return name;
		}
	}

	private static final String INDENT = "  ";
	private static Logger instance;
	
	public static void setup(Options options, boolean showDebug) {
		instance = new Logger(options);
		instance.showDebug = showDebug;
	}
	
	private static Logger getLogger() {
		if (instance == null) throw new IllegalStateException("Logger isn't initialized");
		return instance;
	}
	
	public static void reset() {
		instance = null;
	}
	
	public static void startStage(Stage stageType, int work) {
		getLogger().internalStartStage(stageType, work);
	}
	
	public static void endStage(String message) {
		getLogger().internalEndStage(message);
	}
	
	public static void skipStage(Stage stageType, String message) {
		getLogger().internalSkipStage(stageType, message);
	}
	
	public static void progress() throws InterruptedException, CanceledException {
		getLogger().internalProgress();
	}
	
	private boolean showDebug = true;
	private boolean midLine = false;
	private LinkedList<ProcessStage> stages = new LinkedList<ProcessStage>();
	private Options options;
	
	public static void log(String s) {
		getLogger().internalLog(s);
	}
	
	public static void warn(String s) {
		getLogger().internalWarn(s);
	}
	
	public static void exception(Throwable e) {
		getLogger().internalException(e);
	}
	
	public static void error(String s) {
		getLogger().internalError(s);
	}
	
	public static void debug(String s) {
		getLogger().internalDebug(s);
	}
	
	public static void debug(String message, boolean problem, boolean startOnNewline, boolean endOnNewline, boolean addTimestamp, boolean indent) {
		getLogger().internalDebug(
				message,
				problem,
				startOnNewline,
				endOnNewline,
				addTimestamp,
				indent);
	}
	
	public void internalDebug(
			String message,
			boolean problem,
			boolean startOnNewline,
			boolean endOnNewline,
			boolean addTimestamp,
			boolean indent) {
		PrintStream out = problem ? System.err : System.out;
		if (startOnNewline) out.println();
		if (addTimestamp) out.print(formattedTime() + " | ");
		if (indent) out.print(getIndent());
		out.print(message);
		if (endOnNewline) out.println();
	}
	
	/**
	 * Inform the user about something.
	 * @param s
	 */
	private void internalLog(String s) {
		println(System.out, s);
	}
	
	/**
	 * Inform the user about something which only interests
	 * a user who is also a developer.
	 * @param s
	 */
	private void internalDebug(String s) {
		if (showDebug) println(System.out, s);
	}
	
	/**
	 * Inform the user about a recoverable issue.
	 * @param s
	 */
	private void internalWarn(String s) {
		println(System.err, "[WARNING] " + s);
	}
	
	/**
	 * Inform the user about a non-recoverable issue.
	 * This call should be followed by application termination.
	 * @param s
	 */
	private void internalError(String s) {
		println(System.err, "[ERROR] " + s + ", terminating.");
	}
	
	private void println(PrintStream out, String s) {
		if (Thread.interrupted()) {
			Thread.currentThread().interrupt();
			return;
		}
		out.println(formattedTime() + " | " + getIndent() + s);
		midLine = false;
	}
	
	private String getIndent() {
		if (midLine) return "";
		return StringUtils.repeat(INDENT, stages.size());
	}
	
	public static String formattedDuration(long time) {
		if (time == 0) return "0:00:00";
		return String.format("%d:%02d:%02d", time/3600000, (time/60000)%60,	(time/1000)%60);
	}
	
	private static String formattedTime() {
		Calendar c = Calendar.getInstance();
		return String.format("%d:%02d:%02d.%03d",
				c.get(Calendar.HOUR_OF_DAY),
				c.get(Calendar.MINUTE),
				c.get(Calendar.SECOND),
				c.get(Calendar.MILLISECOND));
	}

	private void internalProgress() throws InterruptedException, CanceledException {
		if (stages.isEmpty()) return;
		stages.getLast().progress();
	}
	
	private void internalStartStage(Stage stageType, int work) {
		internalDebug("starting " + stageType.getName() + " stage...");
		if (monitoredByEclipse()) getEclipseMonitor().subTask("Current stage: " + stageType.getName());
		stages.add(new ProcessStage(stageType, work));
	}
	
	private void internalEndStage(String message) {
		ProcessStage s = stages.pop();
		s.end();
		long duration = s.getDurationMillis();
		internalDebug(s.getName() + " stage complete in " + formattedDuration(duration) + ". " + message);
	}
	
	private void internalSkipStage(Stage stageType, String message) {
		internalDebug("skipping " + stageType.getName() + " stage: " + message);
		new ProcessStage(stageType, 1).end();
	}
	
	private void internalException(Throwable e) {
		if (options.shouldShowExceptions()) e.printStackTrace();
	}

	public static void log(PrintStream out, String message) {
		getLogger().println(out, message);
	}

}
