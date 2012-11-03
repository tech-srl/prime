package technion.prime.history.converters;

import java.util.HashSet;
import java.util.Set;

import technion.prime.Options;
import technion.prime.history.History;
import technion.prime.dom.AppMethodRef;

public class MethodSameClusterer extends SameClusterer<MethodSameClusterer.Key> {

	public MethodSameClusterer(Options options) {
		super(options);
	}

	public class Key {
		Set<AppMethodRef> methods;
		@Override
		public boolean equals(Object obj) {
			return (obj instanceof Key) && ((Key)obj).methods.equals(methods);
		}
		@Override
		public int hashCode() {
			return methods.hashCode();
		}
	}
	
	@Override
	public String getName() {
		return "same methods";
	}

	@Override
	protected Key getKey(History h) {
		Key key = new Key();
		key.methods = new HashSet<AppMethodRef>();
		for (AppMethodRef m : h.getAllParticipatingMethods()) {
			if (m.isUnknown() == false) key.methods.add(m);
		}
		return key;
	}
	
	@Override
	protected String clusterName(Key key, int counter) {
		return String.format("same methods #%d: %s", counter, key.methods.toString());
	}

}
