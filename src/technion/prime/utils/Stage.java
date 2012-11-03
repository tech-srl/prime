package technion.prime.utils;

/**
 * Process stages, used for logging and debugging.
 */
public enum Stage {
	SEARCHING("searching", 1),
	DOWNLOADING("downloading", 5),
	COMPILING("compiling", 5),
	LOADING("loading", 1),
	ANALYZING("analyzing", 10),
	CLUSTERING("clustering", 5),
	COLLECTING_STATISTICS("collecting statistics", 1);
	
	private static int ECLIPSE_PROGRESS_RESOLUTION = 10;
	
	private String name;
	private int weight;
	
	public static int sumWork() {
		int sum = 0;
		for (Stage s : values()) {
			sum += s.getWeight();
		}
		return sum;
	}
	
	private Stage(String name, int weight) {
		this.name = name;
		this.weight = weight;
	}
	
	public String getName() {
		return name;
	}
	
	public int getWeight() {
		return weight * ECLIPSE_PROGRESS_RESOLUTION;
	}
}