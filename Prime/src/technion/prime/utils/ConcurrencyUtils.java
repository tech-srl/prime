package technion.prime.utils;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.IProgressMonitor;

import technion.prime.eclipse.EclipseOptions;

import technion.prime.Options;

import technion.prime.utils.Logger.CanceledException;


public class ConcurrencyUtils {
	private static class CustomThreadFactory implements ThreadFactory {
		private final String basename;
		private int counter;
		private volatile boolean canceled;
		private LinkedList<Thread> threads = new LinkedList<Thread>();
		
		public CustomThreadFactory(String basename) {
			this.basename = basename;
		}
		
		@Override
		public Thread newThread(Runnable r) {
			if (isCanceled()) return null;
			Thread t = new Thread(r, getNextName());
			t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
				@Override
				public void uncaughtException(Thread t, Throwable e) {
					if (e instanceof CanceledException) setCanceled();
				}
			});
			threads.add(t);
			return t;
		}

		private String getNextName() {
			return basename + "-" + counter++;
		}
		
		public void interruptAllCreatedThreads() {
			for (Thread t : threads) t.interrupt();
		}
		
		private synchronized void setCanceled() {
			canceled = true;
		}
		
		public synchronized boolean isCanceled() {
			return canceled;
		}
	}
	
	private static ConcurrencyUtils instance;
	public static ConcurrencyUtils getInstance() {
		return instance;
	}
	public static void setInstance(Options options) {
		instance = new ConcurrencyUtils(options);
	}

	private final Options options;
	
	public ConcurrencyUtils(Options options) {
		this.options = options;
	}

	/**
	 * Run all the runnables one after the other, though the order between them is not guaranteed.
	 * If the timeout duration is reached for a runnable, its execution will stop and the next one
	 * will begin.
	 * @param rs
	 * @param timeoutForEach
	 * @throws CanceledException 
	 */
	public static void runSequentially(String name, Iterable<? extends Runnable> rs, long timeoutForEach) throws CanceledException {
		CustomThreadFactory factory = new CustomThreadFactory(name);
		ExecutorService e = Executors.newSingleThreadExecutor(factory);
		LinkedList<Future<?>> fs = new LinkedList<Future<?>>();
		for (Runnable r : rs) {
			fs.addLast(e.submit(r));
		}
		for (Future<?> f : fs) {
			try {
				f.get(timeoutForEach, TimeUnit.MILLISECONDS);
				checkState();
			} catch (TimeoutException ex) {
				f.cancel(true);
				factory.interruptAllCreatedThreads();
			} catch (InterruptedException ex) {
				factory.interruptAllCreatedThreads();
				return;
			} catch (ExecutionException ex) {
				if (ex.getCause() instanceof CanceledException) {
					throw (CanceledException)ex.getCause();
				}
				Logger.exception(ex);
				// Swallow
			}
		}
		e.shutdown();
	}
	
	/**
	 * Run all the callables one after the other, though the order between them is not guaranteed.
	 * If the timeout duration is reached for a callable, its execution will stop and the next one
	 * will begin.
	 * @param <T>
	 * @param cs
	 * @param timeoutForEach
	 * @return The results of all the callables that were succesfully ran. Since some can fail,
	 * it is not guaranteed to be of the same length as the given callable iterable.
	 * @throws CanceledException 
	 */
	public static <T> LinkedList<T> callSequentially(String name, Iterable<? extends Callable<T>> cs, long timeoutForEach) throws CanceledException {
		CustomThreadFactory factory = new CustomThreadFactory(name);
		ExecutorService e = Executors.newSingleThreadExecutor(factory);
		LinkedList<Future<T>> fs = new LinkedList<Future<T>>();
		for (Callable<T> c : cs) {
			fs.addLast(e.submit(c));
		}
		LinkedList<T> result = new LinkedList<T>();
		for (Future<T> f : fs) {
			try {
				T t = f.get(timeoutForEach, TimeUnit.MILLISECONDS);
				//T t = f.get();
				if (t != null) result.add(t);
				checkState();
			} catch (InterruptedException e1) {
				Logger.log("interrupted");
				continue;
			} catch (ExecutionException e1) {
				if (e1.getCause() instanceof CanceledException) {
					throw (CanceledException)e1.getCause();
				}
				Logger.exception(e1);
				// Swallow
			} catch (TimeoutException e1) {
				f.cancel(true);
				Logger.log("timed out");
			} catch (OutOfMemoryError oom) {
				f.cancel(true);
				Logger.log("OOM-ed");
				System.gc();
				//------- was out before --- factory.interruptAllCreatedThreads();
			}
		}
		e.shutdownNow();
		return result;
	}
	
	public static void runInParallel(String name, Iterable<? extends Runnable> rs, final long taskTimeout, long totalTimeout) {
		CustomThreadFactory taskFactory = new CustomThreadFactory(name);
		CustomThreadFactory timeoutFactory = new CustomThreadFactory("timeout");
		ExecutorService taskExecutor = Executors.newFixedThreadPool(getInstance().options.getParallelOperationsThreadCount(), taskFactory);
		final ExecutorService timeoutExecutor = Executors.newFixedThreadPool(getInstance().options.getParallelOperationsThreadCount(), timeoutFactory);
		for (final Runnable r : rs) {
			taskExecutor.execute(new Runnable() {
				@Override
				public void run() {
					runWithTimeout(r, taskTimeout, timeoutExecutor);
				}
			});
		}
		taskExecutor.shutdown();
		try {
			taskExecutor.awaitTermination(totalTimeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException ex) {
			taskFactory.setCanceled();
			taskFactory.interruptAllCreatedThreads();
		}
		taskExecutor.shutdownNow();
		timeoutExecutor.shutdownNow();
	}
	
	public static <T> LinkedList<T> callInParallel(String name, Iterable<? extends Callable<T>> cs, final long taskTimeout, long totalTimeout) throws CanceledException {
		CustomThreadFactory taskFactory = new CustomThreadFactory(name);
		CustomThreadFactory timeoutFactory = new CustomThreadFactory(name + "-timeout");
		ExecutorService taskExecutor = Executors.newFixedThreadPool(getInstance().options.getParallelOperationsThreadCount(), taskFactory);
		final ExecutorService timeoutExecutor = Executors.newFixedThreadPool(getInstance().options.getParallelOperationsThreadCount(), timeoutFactory);
		LinkedList<Future<T>> fs = new LinkedList<Future<T>>();
		for (final Callable<T> c : cs) {
			fs.addLast(taskExecutor.submit(new Callable<T>() {
				@Override
				public T call() throws Exception {
					return callWithTimeout(c, taskTimeout, timeoutExecutor);
				}
			}));
		}
		taskExecutor.shutdown();
		try {
			taskExecutor.awaitTermination(totalTimeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException ex) {
			return null;
		}
		taskExecutor.shutdownNow();
		
		LinkedList<T> result = new LinkedList<T>();
		for (Future<T> f : fs) {
			try {
				T t = f.get();
				if (t != null) result.addLast(t);
				checkState();
			} catch (InterruptedException e1) {
				// Swallow but break out of loop
				break;
			} catch (ExecutionException e1) {
				if (e1.getCause() instanceof CanceledException) {
					throw (CanceledException)e1.getCause();
				}
				// Swallow
			}
		}
		
		timeoutExecutor.shutdown();
		
		return result;
	}
	
	private static void runWithTimeout(final Runnable r, long timeout, ExecutorService timeoutExecutor) {
		FutureTask<?> task = new FutureTask<Object>(r, null);
		timeoutExecutor.execute(task);
		try {
			task.get(timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			task.cancel(true);
			return;
		} catch (ExecutionException e) {
			Logger.exception(e.getCause());
			return;
		} catch (TimeoutException e) {
			task.cancel(true);
			return;
		}
	}
	
	private static <T> T callWithTimeout(final Callable<T> c, long timeout, ExecutorService timeoutExecutor) throws InterruptedException, ExecutionException, TimeoutException {
		FutureTask<T> task = new FutureTask<T>(c);
		timeoutExecutor.execute(task);
		return task.get(timeout, TimeUnit.MILLISECONDS);
	}
	
	public static void checkState() throws InterruptedException, CanceledException {
		if (instance != null && getInstance().options.isMonitoredByEclipse()) {
			IProgressMonitor m = ((EclipseOptions)getInstance().options).getEclipseMonitor();
			if (m != null && m.isCanceled()) {
				throw new CanceledException();
			}
		}
		if (Thread.interrupted()) {
			throw new InterruptedException();
		}
	}
}
