package technion.prime.analysis;

import java.util.LinkedList;
import java.util.List;

import javax.management.RuntimeErrorException;

import technion.prime.dom.AppType;
import technion.prime.dom.AppMethodDecl;
import technion.prime.dom.AppObject;
import technion.prime.utils.ConcurrencyUtils;
import technion.prime.utils.Logger;
import technion.prime.utils.Logger.CanceledException;
import technion.prime.Options;


public abstract class MethodAnalyzer {
	private class StackFrame {
		List<AppObject> ret = new LinkedList<AppObject>();
	}

	protected final Options options;
	private LinkedList<StackFrame> stack = new LinkedList<StackFrame>();
	private List<AppObject> lastReturned;

	public MethodAnalyzer(Options options) {
		this.options = options;
	}

	private boolean shouldAnalyze(AppMethodDecl m) {
		if (m.isConcrete() == false) return false;
		AppType t = m.getDeclaringType();
		// Do not analyze if we asked it to be filtered out:
		if (options.getFilterAnalyzed().failsFilter(t.getFullName())) return false;
		// Or if it's an API method:
		if (options.getFilterOpaqueTypes().passesFilter(t.getFullName())) return false;
		return true;
	}

	public ProgramState analyzeMethod(AppMethodDecl m, ProgramState initialState,
			List<AppObject> args) throws InterruptedException, CanceledException {
		ConcurrencyUtils.checkState();

		if (shouldAnalyze(m) == false) {
			return initialState;
		}

		if (stack.size() > options.getInterproceduralDepth()) {
			// Recursion checks go here, though we ignore it because a stack size limit covers
			// recursion anyway.
			return initialState;
		}
		Logger.log(String.format("Analyzing method %s", m.getSignature()));
		try {
			enterMethod(m);
			ProgramState result = null;
			result = getEndResult(m, initialState, args);
			leaveMethod(m);
			return result == null ? initialState : result;
		} catch (InterruptedException e) {
			Logger.log("Seriously?");
		} catch (CanceledException e) {
			Logger.log("Seriously?");
		}
		throw new RuntimeException("blat");
	}

	protected abstract ProgramState getEndResult(AppMethodDecl m, ProgramState initialState,
			List<AppObject> args)
			throws InterruptedException, CanceledException;

	private void enterMethod(AppMethodDecl m) {
		StackFrame s = new StackFrame();
		stack.push(s);
	}

	private void leaveMethod(AppMethodDecl m) {
		lastReturned = stack.peek().ret;
		stack.pop();
	}

	public void addReturned(AppObject o) {
		stack.peek().ret.add(o);
	}

	public List<AppObject> getLastReturned() {
		return lastReturned;
	}
}
