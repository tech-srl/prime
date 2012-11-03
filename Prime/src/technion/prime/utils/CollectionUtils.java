package technion.prime.utils;

import soot.util.IdentityHashSet;

import java.util.Collection;
import java.util.Comparator;

public class CollectionUtils {
	public static <T> boolean sameElements(Collection<T> c1, Collection<T> c2) {
		for (T t : c1) {
			if (c2.contains(t) == false) return false;
		}
		for (T t : c2) {
			if (c1.contains(t) == false) return false;
		}
		return true;
	}
	
	public static <T> boolean sameElements(Collection<T> c1, Collection<T> c2, Comparator<T> comp) {
		if (c1.size() != c2.size()) return false;
		
		IdentityHashSet<T> matched = new IdentityHashSet<T>();
		for (T t1 : c1) {
			boolean found = false;
			for (T t2 : c2) {
				if (matched.contains(t2)) continue;
				if (comp.compare(t1, t2) == 0) {
					matched.add(t2);
					found = true;
					break;
				}
			}
			if (found == false) return false;
		}
		return true;
	}
}
