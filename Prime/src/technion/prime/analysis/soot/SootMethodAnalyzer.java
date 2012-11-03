package technion.prime.analysis.soot;

import java.util.List;

import soot.Scene;

import technion.prime.analysis.ProgramState;

import technion.prime.utils.Logger.CanceledException;
import technion.prime.Options;
import technion.prime.dom.soot.SootAppMethodDecl;
import technion.prime.dom.AppMethodDecl;
import technion.prime.dom.AppObject;
import technion.prime.analysis.MethodAnalyzer;


public class SootMethodAnalyzer extends MethodAnalyzer {

	public SootMethodAnalyzer(Options options) {
		super(options);
	}
	
	@Override
	protected ProgramState getEndResult(AppMethodDecl m, ProgramState initialState, List<AppObject> args)
			throws InterruptedException, CanceledException {
		SootAppMethodDecl sootMethod = (SootAppMethodDecl)m;
		SootFlowAnalysis analysis = new SootFlowAnalysis(
				options,
				this,
				sootMethod.getSootMethod(),
				sootMethod.getUnitGraph(),
				Scene.v(),
				initialState);
		analysis.setArgs(args);
		return analysis.analyze();
	}

}
