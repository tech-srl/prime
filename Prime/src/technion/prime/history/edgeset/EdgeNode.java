package technion.prime.history.edgeset;

import java.io.Serializable;

/**
 * Empty class. Since a node may appear in multiple histories, this is just an identifier
 * to use for the edges - it doesn't do anything by itself.
 * 
 * EdgeSetNode is the actual class implementing the Node interface.
 */
public class EdgeNode implements Serializable {
	private static final long serialVersionUID = 8991624797651434594L;

	/**
	 * For debugging purposes only.
	 */
	@Override
	public String toString() {
		return "H" + hashCode() % 10000;
	}
}