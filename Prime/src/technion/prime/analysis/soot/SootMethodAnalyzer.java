package technion.prime.analysis.soot;

import java.util.List;

import soot.Body;
import soot.Scene;
import soot.SootMethod;
import soot.toolkits.graph.UnitGraph;
import soot.util.cfgcmd.CFGToDotGraph;
import soot.util.dot.DotGraph;
import technion.prime.Options;
import technion.prime.analysis.MethodAnalyzer;
import technion.prime.analysis.ProgramState;
import technion.prime.dom.AppMethodDecl;
import technion.prime.dom.AppObject;
import technion.prime.dom.soot.SootAppMethodDecl;
import technion.prime.utils.Logger;
import technion.prime.utils.Logger.CanceledException;


public class SootMethodAnalyzer extends MethodAnalyzer {

	static int method_dump_counter = 0;

	public SootMethodAnalyzer(Options options) {
		super(options);
	}

	@Override
	protected ProgramState getEndResult(AppMethodDecl m, ProgramState initialState,
			List<AppObject> args)
			throws InterruptedException, CanceledException {
		SootAppMethodDecl sootMethod = (SootAppMethodDecl) m;

		UnitGraph ug = null;
		SootMethod sm = null;
		Body b = null;
		try {
			// order of these calls matter, be careful
			b = sootMethod.getBody();
			sm = sootMethod.getSootMethod();
			ug = sootMethod.getUnitGraph();
		} catch (Exception e) {
			Logger.log(String.format("===>failed to analyze method %s", m));
			// e.printStackTrace();
		}

		SootFlowAnalysis analysis = new SootFlowAnalysis(
				options,
				this,
				sm,
				ug,
				Scene.v(),
				initialState);
		/**
		 * EY dumpMethodCFG(ug,b);
		 **/

		analysis.setArgs(args);
		ProgramState result = null;

		result = analysis.analyze();

		return result;
	}

	/**
	 * for debugging only, note the harcoded path and file names [EY]
	 * 
	 * @param g
	 * @param b
	 */
	private void dumpMethodCFG(UnitGraph g, Body b) {
		try {
			CFGToDotGraph dotter = new CFGToDotGraph();
			DotGraph graph = dotter.drawCFG(g, b);
			String name = "/tmp/Method" + method_dump_counter + ".dot";
			Logger.log("writing " + name);
			graph.plot(name);
		} catch (Exception e) {
			e.printStackTrace();
		}
		method_dump_counter++;
	}

}
