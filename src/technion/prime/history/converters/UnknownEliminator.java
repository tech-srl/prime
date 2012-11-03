package technion.prime.history.converters;

import technion.prime.Options;
import technion.prime.history.History;
import technion.prime.history.HistoryCollection;
import technion.prime.utils.Logger;
import technion.prime.utils.Logger.CanceledException;

public class UnknownEliminator implements HistoryConverter {
	private final Options options;
	private int counter;

	public UnknownEliminator(Options options) {
		this.options = options;
	}

	@Override
	public HistoryCollection convert(HistoryCollection hc) throws InterruptedException,
			CanceledException {
		HistoryCollection result = hc;
		
		int numUnknowns = result.getNumUnknownEdges();
		int prevNumUnknowns;
		do {
			counter = 0;
			prevNumUnknowns = numUnknowns;
			Logger.log(String.format("  %d unknown edges remaining...", numUnknowns));
			result = eliminateUnknowns(result);
			numUnknowns = result.getNumUnknownEdges();
		} while (prevNumUnknowns != numUnknowns);
		
		return result;
	}

	private HistoryCollection eliminateUnknowns(HistoryCollection hc) throws InterruptedException, CanceledException {
		HistoryCollection result = options.newHistoryCollection();
		for (History h : hc.getHistories()) {
			History eliminated = h.eliminateUnknowns(hc);
			eliminated.setTitle("unknown-less history #" + counter++);
			result.addHistory(eliminated);
		}
		return result;
	}

	@Override
	public String getName() {
		return "unknown eliminator";
	}
	
}
