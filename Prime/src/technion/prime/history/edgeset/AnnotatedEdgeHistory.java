package technion.prime.history.edgeset;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import technion.prime.Options;
import technion.prime.utils.Logger.CanceledException;


public class AnnotatedEdgeHistory extends EdgeHistory {
	private static final long serialVersionUID = -4259054145059612316L;
	private Set<Edge> annotated;

	public AnnotatedEdgeHistory(Options options, EdgeHistory h) {
		super(options);
		copyFrom(h);
		clearSources();
		setTitle(h.getTitle());
	}

	public void annotate(Edge e) {
		annotated.add(e);
	}
	
	@Override
	protected void appendGvEdgeExtras(StringBuilder sb, Edge e) {
		if (annotated.contains(e)) {
			sb.append("color = \"red\" ");
		}
	}
	
	@Override
	protected void copyFrom(EdgeHistory h) {
		super.copyFrom(h);
		annotated = new HashSet<Edge>();
		if (h instanceof AnnotatedEdgeHistory) {
			annotated.addAll(((AnnotatedEdgeHistory)h).annotated);
		}
	}
	
	@Override
	public String getTitle() {
		return "annotated " + super.getTitle();
	}
	
	@Override
	protected Map<EdgeNode, EdgeNode> mergeEdgesFrom(EdgeHistory h, Iterable<Edge> newEdges,
			boolean union, boolean inclusion) throws InterruptedException, CanceledException {
		Map<EdgeNode, EdgeNode> result = super.mergeEdgesFrom(h, newEdges, union, inclusion);
		
		// Every edge that was annotated in the other history should get its matching edge
		// annotated in this history.
		
		// Find all the annotated edges in the other history:
		Set<Edge> annotatedInOther = new HashSet<Edge>();
		if (h instanceof AnnotatedEdgeHistory) {
			for (Edge e : newEdges) {
				if (((AnnotatedEdgeHistory) h).annotated.contains(e)) {
					annotatedInOther.add(e);
				}
			}
		}
		
		// Annotate their matching:
		for (Edge e : annotatedInOther) {
			Edge localEdge = findEdge(result.get(e.getFrom()), result.get(e.getTo()));
			annotate(localEdge);
		}
		
		return result;
	}
	
//	@Override
//	protected void updateEdgeFromOtherEdge(EdgeNode from, EdgeNode to, Edge otherEdge, boolean union) {
//		super.updateEdgeFromOtherEdge(from, to, otherEdge, union);
//		if (annotated.contains(otherEdge)) annotated.add(findEdge(from, to));
//	}
	
}
