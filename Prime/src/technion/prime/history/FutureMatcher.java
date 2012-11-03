package technion.prime.history;

import java.util.HashSet;
import java.util.Set;

import technion.prime.dom.AppMethodRef;


// Nodes match if the intersection of their outgoing methods is not empty, when disregarding unknowns.
public class FutureMatcher implements Matcher {
	private static final long serialVersionUID = 679147207597496803L;

	public FutureMatcher(int length) {
		// If anyone wants to increase the match length, situations such as
		// f -> ? -> g -> ?
		// need to be considered; meanwhile only length 1 is supported.
		if (length != 1) throw new IllegalArgumentException("Future match currently only supports length 1.");
	}
	
	@Override
	public boolean matches(Node n1, Node n2) {
		if (n1.equals(n2)) return true;
		// get outgoing methods, disregarding self loops
		Set<AppMethodRef> out1 = n1.getOutgoingMethods();
		out1.removeAll(n1.getIncomingMethods());
		Set<AppMethodRef> out2 = n2.getOutgoingMethods();
		out2.removeAll(n2.getIncomingMethods());
		
		// If both are empty they are considered matching; if only one is empty,
		// they do not match.
		if (out1.isEmpty() && out2.isEmpty()) return true;
		if (out1.isEmpty() || out2.isEmpty()) return false;
		
		out1.retainAll(out2);
		Set<AppMethodRef> unknowns = new HashSet<AppMethodRef>();
		for(AppMethodRef method : out1){
			if (method.isUnknown()) unknowns.add(method);
		}
		out1.removeAll(unknowns);
		return !out1.isEmpty();
	}

}
