package technion.prime.history;

public class IdentityAbstractObject extends AbstractObject {
	private static final long serialVersionUID = 484216158495438282L;

	public IdentityAbstractObject(AbstractObject base) {
		super(base.getType());
	}
	
	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}
	
	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}
}
