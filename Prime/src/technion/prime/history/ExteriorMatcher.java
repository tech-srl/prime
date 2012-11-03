package technion.prime.history;

import java.util.Set;

import technion.prime.dom.AppMethodRef;

public class ExteriorMatcher implements Matcher {
	private static final long serialVersionUID = 1940431232707740404L;

	public ExteriorMatcher(int length) {
		// If anyone wants to increase the match length, situations such as
		// f -> ? -> g -> ?
		// need to be considered; meanwhile only length 1 is supported.
		if (length != 1) throw new IllegalArgumentException("Exterior match currently only supports length 1.");
	}
	
	@Override
	public boolean matches(Node n1, Node n2) {
		if (n1.equals(n2)) return true;
		
		// Verify the incoming method sets are the same (may also both be empty):
		if (matchingMethodSets(getMethods(n1), getMethods(n2)) == false) {
			return false;
		}
		
		// If the incoming method sets contain unknowns, verify their previous nodes all match.
		// This is guaranteed to end because we do not allow unknown loops.
		if (containsUnknown(getMethods(n1)) && containsUnknown(getMethods(n2))) {
			for (Node prev1 : getNodes(n1)) {
				for (Node prev2 : getNodes(n2)) {
					if (matchingMethodSets(getMethods(prev1), getMethods(prev2)) == false) {
						return false;
					}
				}
			}
		}
		
		return true;
	}
	
	private boolean containsUnknown(Set<AppMethodRef> methods) {
		for (AppMethodRef m : methods) {
			if (m.isUnknown()) return true;
		}
		return false;
	}

	public boolean matchingMethodSets(Set<AppMethodRef> set1, Set<AppMethodRef> set2) {
		if (set1.isEmpty() != set2.isEmpty()) return false;
		
		for (AppMethodRef m1 : set1) {
			for (AppMethodRef m2 : set2) {
				if (matchingMethods(m1, m2) == false) return false;
			}
		}
		return true;
	}
	
	/**
	 * @param n
	 * @return The methods associated with a node.
	 */
	protected Set<AppMethodRef> getMethods(Node n) {
		return n.getIncomingMethods();
	}

	/**
	 * @param n
	 * @return The nodes associated with a node.
	 */
	protected Set<? extends Node> getNodes(Node n) {
		return n.getPreviousNodes();
	}

	/**
	 * Check whether two methods match.
	 * 
	 * @param m1
	 * @param m2
	 * @return True iff both methods are considered matching for the sake of comparing or merging
	 * histories.
	 */
	protected boolean matchingMethods(AppMethodRef m1, AppMethodRef m2) {
		if (m1 == m2) return true;
		// Methods must have the same static modifier
		if (m1.isStatic() != m2.isStatic()) {
			return false;
		}
		// Constructors and unknown methods are matched according to their entire signature
		if ((m1.isInit() && m2.isInit()) || (m1.isUnknown() && m2.isUnknown())) {
			return m1.equals(m2);
		}
		// Otherwise, methods are matched according to name only, not the whole signature, because
		// * Overloaded methods should be matched
		// * Overridden methods should be matched
		// * A method with unknown parameter types should be matched with a method with known
		//   parameter types, if they otherwise have the same name
		return m1.getShortName().equals(m2.getShortName());
	}

}
