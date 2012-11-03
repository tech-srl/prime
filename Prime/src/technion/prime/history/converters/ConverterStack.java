package technion.prime.history.converters;

import technion.prime.Options;
import technion.prime.history.HistoryCollection;
import technion.prime.utils.Logger;
import technion.prime.utils.Logger.CanceledException;

/**
 * A stack of converters, executed in the sequence in which they are given in the constructor.
 */
public class ConverterStack {
	// converters[i] is the (i+1)th converter in the stack.
	private final HistoryConverter[] converters;
	
	// layerOutputs[i] is the output of converters[i]
//	private final HistoryCollection[] layerOutputs;
	
	public ConverterStack(Options options, HistoryConverter[] converters) {
//		this.options = options;
		this.converters = converters;
//		layerOutputs = new HistoryCollection[converters.length];
	}
	
	public HistoryCollection convert(HistoryCollection input)
			throws InterruptedException, CanceledException {
		input.clearAllSources();
		HistoryCollection[] layerOutputs = new HistoryCollection[converters.length];
		for (int i = 0 ; i < converters.length ; i++) {
			HistoryConverter c = converters[i];
			HistoryCollection hc = i == 0 ? input : layerOutputs[i-1];
			Logger.log(String.format("Converting %d histories with %s clusterer...",
					hc.getNumHistories(), c.getName()));
			try {
				layerOutputs[i] = c.convert(hc);
			} catch(InterruptedException e) {
				// Just skip the converter
				continue;
			} finally {
				Logger.progress();
			}
		}
		HistoryCollection output = layerOutputs[layerOutputs.length-1];
		Logger.log("Final layer contains " + output.getNumHistories() + " clusters.");
		return output;
	}
	
//	public void generateOutputFiles(String outputFolder) throws CanceledException, InterruptedException {
//		counter = 0;
//		for (int i = 0 ; i < converters.length ; i++) {
////			if (options.shouldGenerateOutputFromUnclustered() == false && i == 0) continue;
//			
//			String layerOutputFolder = FilenameUtils.concat(outputFolder, "layer_" + i);
//			Set<History> histories = getAllHistoriesOfLayer(i);
//			for (History h : histories) {
//				Sample s = getHistorySample(h);
//				generateOutput(h, s, layerOutputFolder);
//			}
//		}
//	}
//	
//	public void updateQueryResults(QueryResults q) throws CanceledException {
//		for (History h : output.getHistories()) {
//			try {
//				q.addSample(getHistorySample(h));
//			} catch (InterruptedException e) {
//				throw new RuntimeException("Sample generation failed");
//			}
//		}
//	}
	
//	private void generateOutput(History h, Sample s, String outputFolder) throws CanceledException {
//		String dotOutputFolder = FilenameUtils.concat(outputFolder, "dot");
//		String xmlOutputFolder = FilenameUtils.concat(outputFolder, "xml");
//		String filename = null;
//		try {
//			filename = h.generateGraphvizOutput(dotOutputFolder, counter);
//			h.generateXmlOutput(xmlOutputFolder, counter);
//			counter++;
//		} catch (IOException e) {
//			Logger.exception(e);
//			Logger.warn("could not generate output from history");
//		} catch (InterruptedException e) {
//			Logger.exception(e);
//			Logger.warn("interrupted while generating output from history");
//		}
//		s.setField(Sample.FILENAME, filename);
//	}
//	
//	public Sample getHistorySample(History h) throws InterruptedException, CanceledException {
//		if (sampleByHistory.containsKey(h) == false) {
//			sampleByHistory.put(h, createSample(h));
//		}
//		return sampleByHistory.get(h);
//	}
//	
//	private Sample createSample(History h) throws InterruptedException, CanceledException {
//		Sample s = new Sample();
//		s.setField(Sample.NAME, h.getTitle());
//		s.setField(Sample.SIZE, h.getNumNodes());
//		s.setField(Sample.DEPTH, h.getDepth());
//		s.setField(Sample.MAX_DEGREE, h.getMaxDegree());
//		s.setField(Sample.AVG_WEIGHT, h.getAverageWeight());
//		s.setField(Sample.MAX_WEIGHT, h.getMaximumWeight());
//		s.setField(Sample.NUM_TYPES, h.getNumParticipatingTypes());
//		s.setField(Sample.NUM_EDGES, h.getNumEdges());
//		s.setField(Sample.NUM_UNKNOWN_EDGES, h.getNumUnknownEdges());
//		for (History src : h.getSources()) {
//			s.addSample(getHistorySample(src));
//		}
//		return s;
//	}
//
//	public Set<History> getAllHistoriesOfLayer(int i) {
//		if (historiesByLayer.containsKey(i) == false) throw new NoSuchElementException();
//		return historiesByLayer.getAll(i);
//	}

	public int size() {
		return converters.length;
	}

//	public HistoryCollection getFinalHistoryCollection() {
//		return output;
//	}
//
//	public void setOutput(HistoryCollection output) {
//		this.output = output;
//	}
	
}
