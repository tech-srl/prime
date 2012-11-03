package technion.prime.history.converters;

import technion.prime.utils.Logger.CanceledException;

import technion.prime.Options;
import technion.prime.history.History;

public class AutomataSameClusterer extends SameClusterer<AutomataSameClusterer.Key> {
	public class Key {
		public History h;
		
		@Override
		public boolean equals(Object obj) {
			try {
				return (obj instanceof Key) && h.equalContent(((Key)obj).h);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} catch (CanceledException e) {
				throw new RuntimeException(e);
			}
		}
		
		@Override
		public int hashCode() {
			return h.contentHash();
		}
	}
	
	public AutomataSameClusterer(Options options) {
		super(options);
	}
	
	@Override
	public String getName() {
		return "same histories";
	}
	
	@Override
	protected Key getKey(History h) {
		Key key = new Key();
		key.h = h.cloneWeightless();
		return key;
	}
	
	@Override
	protected String clusterName(Key key, int counter) {
		return String.format("same history #%d", counter);
	}
	
}
