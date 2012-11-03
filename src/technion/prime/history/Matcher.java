package technion.prime.history;

import java.io.Serializable;

public interface Matcher extends Serializable {
	boolean matches(Node n1, Node n2);
}
