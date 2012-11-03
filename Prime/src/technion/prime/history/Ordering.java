package technion.prime.history;

import technion.prime.dom.AppMethodRef;

import com.google.gdata.util.common.base.Pair;

public class Ordering extends Pair<AppMethodRef, AppMethodRef> {
	private static final long serialVersionUID = -9105179045492067084L;

	public Ordering(AppMethodRef first, AppMethodRef second) {
		super(first, second);
	}
}
