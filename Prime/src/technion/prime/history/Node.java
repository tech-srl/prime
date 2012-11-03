package technion.prime.history;

import java.util.Set;

import technion.prime.dom.AppMethodRef;

public interface Node {
	Set<AppMethodRef> getIncomingMethods();

	// sharon
	Set<AppMethodRef> getOutgoingMethods();
	
	Set<? extends Node> getPreviousNodes();
	
	
}
