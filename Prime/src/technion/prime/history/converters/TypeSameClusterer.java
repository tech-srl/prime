package technion.prime.history.converters;

import java.util.Collections;
import java.util.Set;

import technion.prime.dom.AppType;
import technion.prime.dom.dummy.DummyAppType;
import technion.prime.history.History;
import technion.prime.Options;

public class TypeSameClusterer extends SameClusterer<TypeSameClusterer.Key> {

	class Key {
		Set<AppType> types;
		@Override
		public boolean equals(Object obj) {
			return (obj instanceof Key) && ((Key)obj).types.equals(types);
		}
		@Override
		public int hashCode() {
			return types.hashCode();
		}
	}
	
	public TypeSameClusterer(Options options) {
		super(options);
	}

	@Override
	public String getName() {
		return "same types";
	}

	@Override
	protected Key getKey(History h) {
		Key key = new Key();
		key.types = h.getAllParticipatingApiTypes();
		if (key.types.isEmpty()) {
			key.types = Collections.<AppType>singleton(new DummyAppType("java.lang.Object"));
		}
		return key;
	}
	
	@Override
	protected String clusterName(Key key, int counter) {
		return String.format("same types #%d: %s", counter, key.types.toString());
	}

}
