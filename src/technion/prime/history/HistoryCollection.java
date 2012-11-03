package technion.prime.history;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import technion.prime.Options;
import technion.prime.dom.AppMethodRef;
import technion.prime.dom.AppType;
import technion.prime.utils.Logger;
import technion.prime.utils.Logger.CanceledException;


public abstract class HistoryCollection implements Cloneable, Serializable {
	
	private static final long serialVersionUID = 7307442924797515299L;

	/**
	 * Analysis options.
	 */
	protected transient Options options;
	
	public HistoryCollection(Options options) {
		this.options = options;
	}
	
	public abstract void addHistory(History h);


	/**
	 * Filters out all edges which have a lower weight than the maximum weight.
	 * @param threshold 1.0 means filter all, 0.0 means filter none,
	 * 0.3 means filter the lowest 30%.
	 * @throws CanceledException 
	 * @throws InterruptedException 
	 */
	public void filterLowWeightEdges(double threshold) throws InterruptedException, CanceledException {
		for (History h : getHistories()) {
			h.filterLowWeightEdges(threshold);
			Logger.progress();
		}
	}
	
	public int getNumHistories() {
		return getHistories().size();
	}

	public int getNumNodes() {
		int sum = 0;
		for (History h : getHistories()) {
			sum += h.getNumNodes();
		}
		return sum;
	}

	public abstract Collection<? extends History> getHistories();

	/**
	 * Deep clone.
	 */
	@Override
	public HistoryCollection clone() {
		HistoryCollection result = null;
		try {
			result = (HistoryCollection) super.clone();
		} catch (CloneNotSupportedException e) {
			assert(false);
		}
		result.removeAllHistories();
		for (History h : getHistories()) {
			result.addHistory(h.clone());
		}
		return result;
	}

	protected abstract void removeAllHistories();

	public abstract void generateGraphvizOutput(String outputPath) throws InterruptedException, CanceledException, IOException;
	
	protected void addSeenAsReturnType(AppType returnType) {
		if (options.getFilterBaseTracked().failsFilter(returnType.getFullName())) return;
		options.getOngoingAnalysisDetails().incrementAsReturnType(returnType);
	}
	
	public abstract void generateXmlOutput(String outputPath)
			throws InterruptedException, CanceledException, IOException;

	public abstract void filterEmptyHistories() throws InterruptedException, CanceledException;
	
	public Set<AppMethodRef> getAllParticipatingMethods() {
		Set<AppMethodRef> result = new HashSet<AppMethodRef>();
		for (History h : getHistories()) {
			result.addAll(h.getAllParticipatingMethods());
		}
		return result;
	}
	
	public void save(String filename) throws IOException {
		File file = new File(filename);
		File parent = file.getParentFile();
		if (parent != null) parent.mkdirs();
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
		oos.writeObject(this);
		oos.close();
//		FileOutputStream fos = new FileOutputStream(filename);
//		javax.xml.bind.JAXB.marshal(this, fos);
	}
	
	public static HistoryCollection load(String filename, Class<? extends HistoryCollection> type) throws IOException {
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename));
		HistoryCollection result;
		try {
			result = (HistoryCollection)ois.readObject();
		} catch (ClassNotFoundException e) {
			Logger.exception(e);
			return null;
		}
		return result;
//		FileInputStream fis = new FileInputStream(filename);
//		return javax.xml.bind.JAXB.unmarshal(fis, type);
	}

	/**
	 * Adds all the histories in the argument into this history collection.
	 * @param histories Histories to add.
	 */
	public void addAll(Iterable<? extends History> histories) {
		for (History h : histories) addHistory(h);
	}
	
	public void unionFrom(HistoryCollection hc) {
		addAll(hc.getHistories());
	}

	public boolean isEmpty() {
		return getHistories().isEmpty();
	}

	/**
	 * Remove a single history from this collection.
	 * @param h
	 */
	public abstract void removeHistory(History h);

	/**
	 * Remove all the histories in this collection.
	 */
	public abstract void clear();

	public void clearAllSources() {
		for (History h : getHistories()) h.clearSources();
	}

	public boolean isFromClustering() {
		return getNumHistories() > 0 && getHistories().iterator().next().isFromClustering();
	}

	public void recursivelySetOptions(Options options) {
		this.options = options;
		for (History h : getHistories()) {
			h.recursivelySetOptions(options);
		}
	}

	public Set<AppType> getAllParticipatingTypes() {
		Set<AppType> result = new HashSet<AppType>();
		for (History h : getHistories()) {
			result.addAll(h.getAllParticipatingTypes());
		}
		return result;
	}

	public Set<AppType> getAllParticipatingApiTypes() {
		Set<AppType> result = new HashSet<AppType>();
		for (History h : getHistories()) {
			result.addAll(h.getAllParticipatingApiTypes());
		}
		return result;
	}
	
	public int getNumUnknownEdges() {
		int result = 0;
		for (History h : getHistories()) result += h.getNumUnknownEdges();
		return result;
	}
	
}
