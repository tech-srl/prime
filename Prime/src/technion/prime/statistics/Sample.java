package technion.prime.statistics;

import java.util.HashSet;
import java.util.Set;

/**
 * A sample represents one history, that was potentially generated from clustering
 * a bunch of other samples.
 */
public class Sample extends FieldHolder {
	public static long sampleCount = 0;
	public static Field NAME = new Field("name", String.class);
	public static Field NUM_SAMPLES = new Field("num samples", Integer.class);
	public static Field PERCENTAGE_SAMPLES = new Field("perc samples", Double.class);
	public static Field SIZE = new Field("size", Integer.class);
	public static Field DEPTH = new Field("depth", Integer.class);
	public static Field MAX_DEGREE = new Field("max degree", Integer.class);
	public static Field AVG_DEGREE = new Field("avg degree", Double.class);
	public static Field MAX_WEIGHT = new Field("max weight", Double.class);
	public static Field AVG_WEIGHT = new Field("avg weight", Double.class);
	public static Field NUM_TYPES = new Field("# types", Integer.class);
	public static Field NUM_EDGES = new Field("# edges", Integer.class);
	public static Field NUM_UNKNOWN_EDGES = new Field("# ? edges", Integer.class);
	
	protected Set<Sample> samples = new HashSet<Sample>();
	protected Sample parent;
	protected String id;
	
	public Sample() {
		id = "Sample" + sampleCount;
		sampleCount++;
	}
	
	public void addSample(Sample s) {
		samples.add(s);
		// EY: it is not clear that a sample only has one parent, should check
		s.setParent(this);
	}
	public Set<Sample> getSamples() {
		return samples;
	}
	public boolean containsOtherSamples() {
		return ! samples.isEmpty();
	}
	public Sample getParent() {
		return parent;
	}
	public void setParent(Sample s) {
		parent = s;
	}
	
	public String getId() {
		return id;
	}
	
}
