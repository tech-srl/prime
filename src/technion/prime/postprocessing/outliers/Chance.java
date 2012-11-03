package technion.prime.postprocessing.outliers;

import technion.prime.dom.AppMethodRef;

public class Chance implements Comparable<Chance> {

	private final AppMethodRef method;
	private final double chance;

	public Chance(AppMethodRef m, double d) {
		this.method = m;
		this.chance = d;
	}

	@Override
	public int compareTo(Chance o) {
		// o.chance before chance because we sort from highest to lowest
		return Double.compare(o.chance, chance);
	}
	
	public AppMethodRef getMethod() {
		return method;
	}
	
	public double getChance() {
		return chance;
	}

}
