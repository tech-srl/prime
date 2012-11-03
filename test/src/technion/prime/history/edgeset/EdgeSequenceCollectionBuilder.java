package technion.prime.history.edgeset;

import java.util.Collection;
import java.util.LinkedList;

import technion.prime.history.History;

public class EdgeSequenceCollectionBuilder {
	LinkedList<EdgeSequence> sequences = new LinkedList<EdgeSequence>();
	
	public EdgeSequenceCollectionBuilder withHistory(History h) {
		sequences.addLast(edgeSequenceFromLinearHistory((EdgeHistory)h));
		return this;
	}
	
	public EdgeSequenceCollectionBuilder withSequence(EdgeSequence s) {
		sequences.addLast(s);
		return this;
	}

	public Collection<EdgeSequence> buildSequenceCollection() {
		return sequences;
	}

	private EdgeSequence edgeSequenceFromLinearHistory(EdgeHistory h) {
		EdgeSequence result = new EdgeSequence();
		EdgeNode curr = h.getRoot();
		while (h.getOutgoingEdges(curr).isEmpty() == false) {
			Edge outgoing = h.getOutgoingEdges(curr).iterator().next();
			result.addLast(outgoing);
			curr = outgoing.getTo();
		}
		return result;
	}
	
}