package technion.prime.history.edgeset;

public class WeightlessEdge extends Edge {
	private static final long serialVersionUID = -3285363185220202775L;

	public WeightlessEdge(Edge e) {
		super(e.getFrom(), e.getTo(), e.getMethods(), 0);
	}
}
