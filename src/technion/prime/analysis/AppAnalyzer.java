package technion.prime.analysis;

import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import technion.prime.statistics.AnalysisDetails;
import technion.prime.utils.ConcurrencyUtils;
import technion.prime.utils.Logger.CanceledException;
import technion.prime.utils.Stage;
import technion.prime.utils.Logger;
import technion.prime.dom.App;
import technion.prime.dom.AppClass;
import technion.prime.dom.AppMethodDecl;
import technion.prime.history.History;
import technion.prime.history.HistoryCollection;
import technion.prime.history.converters.AutomataSameClusterer;
import technion.prime.Options;

public class AppAnalyzer {
	private final Options options;
	
	public AppAnalyzer(Options options) {
		this.options = options;
	}

	/**
	 * Analyze all the classes loaded into an application abstraction.
	 * @param a The application abstraction in which the classes are loaded.
	 * @return A new history collection with all the typestate information.
	 * @throws CanceledException 
	 */
	public HistoryCollection analyzeApp(App a) throws CanceledException {
		int totalClassCount = a.getClasses().size();
		// Prepare a list of eligible classes.
		// Must appear before the stage starts because this is used to calculate the stage length.
		LinkedList<AppClass> classes = new LinkedList<AppClass>();
		for (AppClass c : a.getClasses()) {
			if (c.isInterface() || c.isPhantom() || options.getFilterAnalyzed().failsFilter(c.getName())) continue;
			classes.add(c);
		}
		int classCount = classes.size();
		
		Logger.startStage(Stage.ANALYZING, classCount);
		Logger.debug("base filter:  " + options.getFilterBaseTracked().toString());
		Logger.debug("final filter: " + options.getFilterReported().toString());
		Logger.debug("analyzed filter: " + options.getFilterAnalyzed().toString());
		Logger.debug("opaque types filter: " + options.getFilterOpaqueTypes().toString());
		Logger.debug("found " + classCount + "/" + totalClassCount + " eligible classes.");
		
		// Analyze!
		HistoryCollection hc = options.newHistoryCollection();
		int analyzed = 0;
		for (AppClass c : classes) {
			try {
				Logger.progress();
				HistoryCollection singleResult = analyzeClass(c, analyzed, classCount);
				hc.unionFrom(singleResult);
				analyzed++;
				//a.unloadClass(c);
			} catch (InterruptedException e) {
				continue;
			}
		}
		Logger.endStage(String.format("analyzed %d/%d classes, got %d nodes in %d abstract objects.",
				analyzed, classCount, hc.getNumNodes(), hc.getNumHistories()));
		
		return hc;
	}
	
	private HistoryCollection analyzeClass(AppClass c, int classNum, int classCount) throws CanceledException {
		if (c.isInterface() || c.isPhantom() || options.getFilterAnalyzed().failsFilter(c.getName())) {
			assert(false);
			return null;
		}
		Logger.debug("analyzing class " + (classNum + 1) + "/" + classCount + " '" + c.getName() + "'",
				false, false, false, true, true);
		
		final AtomicInteger methodCount = new AtomicInteger(0);
		final AtomicInteger methodsSucceeded = new AtomicInteger(0);
		LinkedList<Callable<HistoryCollection>> tasks = new LinkedList<Callable<HistoryCollection>>();
		for (final AppMethodDecl m : c.getMethods()) {
			tasks.add(new Callable<HistoryCollection>() {
				@Override
				public HistoryCollection call() throws CanceledException {
					HistoryCollection hc = null;
					ProgramState finalState = null;
					try {
						ConcurrencyUtils.checkState();
						finalState = options.newMethodAnalyzer().analyzeMethod(m, null, null);
						ConcurrencyUtils.checkState();
					} catch (InterruptedException e) {
						// Swallow.
					}
					if (finalState == null) {
						Logger.debug("*", true, false, false, false, false);
					} else {
						methodsSucceeded.getAndIncrement();
						finalState.removeUntrackedHistories();
						hc = finalState.toHistoryCollection();
						try {
							// Cluster together all identical histories.
							// Do not increase weight - we assume that
							// since they come from the same method, they very likely
							// represent the same code.
							// FIXME this actually hides a BUG which causes too many histories to
							// appear on specific cases. To approach the bug, disable this
							// and then re-run the project tests to pinpoint the issue.
							hc = new AutomataSameClusterer(options) {
								@Override protected String clusterName(
										AutomataSameClusterer.Key key, int counter) {
									return key.h.getTitle();
								}
								@Override protected History clusterHistories(History h1, History h2)
										throws InterruptedException, CanceledException {
									return h1;
								}
							}.convert(hc);
						} catch (InterruptedException e) {
							// Nothing
						}
						hc.clearAllSources();
						Logger.debug(".", false, false, false, false, false);
					}
					methodCount.getAndIncrement();
					return hc;
				}
			});
		}
		long methodTimeout = options.getSingleActionTimeout(Stage.ANALYZING);
		long stageTimeout = options.getStageTimeout(Stage.ANALYZING);
		LinkedList<HistoryCollection> hcs = options.isStageParallel(Stage.ANALYZING) ?
			ConcurrencyUtils.callInParallel("parallel-method-analysis", tasks, methodTimeout, stageTimeout) :
			ConcurrencyUtils.callSequentially("sequential-method-analysis", tasks, methodTimeout);
		HistoryCollection result = options.newHistoryCollection();
		for (HistoryCollection hc : hcs) {
			if (hc == null) continue;
			result.unionFrom(hc);
		}
		Logger.debug(" " + methodsSucceeded.get() + "/" + methodCount.get() + " methods done.", false, false, true, false, false);
		AnalysisDetails details = options.getOngoingAnalysisDetails();
		int oldMethodsSucceeded = details.getInteger(AnalysisDetails.METHODS_SUCCEEDED);
		int oldMethodsTotal = details.getInteger(AnalysisDetails.TOTAL_METHODS);
		details.setField(AnalysisDetails.METHODS_SUCCEEDED, oldMethodsSucceeded + methodsSucceeded.get());
		details.setField(AnalysisDetails.TOTAL_METHODS, oldMethodsTotal + methodCount.get());
		if (methodsSucceeded.get() == 0) details.addUnanalyzableClass(c);
		return result;
	}
}
