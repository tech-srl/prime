package technion.prime.history.edgeset;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import technion.prime.Options;
import technion.prime.history.History;
import technion.prime.history.HistoryCollection;


public class EdgeHistoryCollection extends HistoryCollection {
	private static final long serialVersionUID = 1L;
	
	Collection<EdgeHistory> histories = new LinkedList<EdgeHistory>();
	
	public EdgeHistoryCollection(Options options) {
		super(options);
	}
	
	@Override
	public boolean isEmpty() {
		return histories.isEmpty();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj instanceof EdgeHistoryCollection == false) return false;
		EdgeHistoryCollection hc = (EdgeHistoryCollection)obj;
		return histories.equals(hc.histories);
	}
	
	@Override
	public int hashCode() {
		return histories.hashCode();
	}
	
	@Override
	public String toString() {
		return histories.toString();
	}

	@Override
	public void generateGraphvizOutput(String outputPath) throws IOException {
		int i = 0;
		for (EdgeHistory h : histories) {
			h.generateGraphvizOutput(outputPath, i++);
		}
	}
	
	@Override
	public void generateXmlOutput(String outputPath) throws IOException {
		int i = 0;
		for (EdgeHistory h : histories) {
			h.generateXmlOutput(outputPath, i++);
		}
	}
	
	@Override
	public void addHistory(History h) {
		histories.add((EdgeHistory)h);
	}

	@Override
	public Collection<? extends History> getHistories() {
		return histories;
	}
	
	@Override
	public void filterEmptyHistories() {
		Set<EdgeHistory> emptyHistories = new HashSet<EdgeHistory>();
		for (EdgeHistory h : histories) {
			if (h.containsOnlyRoot()) emptyHistories.add(h);
		}
		histories.removeAll(emptyHistories);
	}

	@Override
	protected void removeAllHistories() {
		histories.clear();
	}

	@Override
	public void removeHistory(History h) {
		histories.remove(h);
	}

	@Override
	public void clear() {
		histories.clear();
	}

}
