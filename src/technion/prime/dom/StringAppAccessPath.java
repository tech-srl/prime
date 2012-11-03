package technion.prime.dom;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * Immutable
 */
public class StringAppAccessPath implements AppAccessPath, Serializable {
	private static final long serialVersionUID = 7035298505697906748L;

	private final AppMethodDecl containingMethod; // locals only
	private final AppType containingType; // fields only
	private final List<String> fields;
	transient private Integer hash;
	
	public StringAppAccessPath(AppMethodDecl containingMethod,
			AppType containingType, List<String> fields) {
		this.containingMethod = containingMethod;
		this.containingType = containingType;
		this.fields = fields;
	}

	@Override
	public boolean prefixedBy(AppAccessPath ap) {
		StringAppAccessPath sap = downcast(ap);
		
		// Cannot be prefixed by a smaller AP
		if (fields.size() < sap.fields.size()) return false;
		
		// If local, verify same anchor
		if (isLocal()) {
			if (sap.isLocal() == false || containingMethod.equals(sap.containingMethod) == false)
				return false;
		} else {
			if (sap.isLocal()) return false;
		}
		
		// If static, verify same anchor
		if (isField()) {
			if (sap.isField() == false || containingType.equals(sap.containingType) == false)
				return false;
		} else {
			if (sap.isField()) return false;
		}
		
		for (int i = 0 ; i < sap.fields.size() ; i++) {
			if (fields.get(i).equals(sap.fields.get(i)) == false) return false;
		}
		
		return true;
	}
	
	@Override
	public int hashCode() {
		if (hash == null) {
			hash = calculateHash();
		}
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof StringAppAccessPath)) return false;
		
		// Since the hash is cached, it's efficient to check it to quickly
		// rule out equivalence:
		if (hashCode() != obj.hashCode()) return false;
		
		StringAppAccessPath other = (StringAppAccessPath) obj;
		if (containingType == null) {
			if (other.containingType != null)
				return false;
		} else if (!containingType.equals(other.containingType))
			return false;
		if (containingMethod == null) {
			if (other.containingMethod != null)
				return false;
		} else if (!containingMethod.equals(other.containingMethod))
			return false;
		if (fields == null) {
			if (other.fields != null)
				return false;
		} else if (!fields.equals(other.fields))
			return false;
		return true;
	}

	@Override
	public int getLength() {
		return fields.size();
	}

	@Override
	public AppAccessPath concat(AppAccessPath ap) {
		StringAppAccessPath sap = downcast(ap);
		
		List<String> newFields = new ArrayList<String>();
		newFields.addAll(fields);
		newFields.addAll(sap.fields);
		
		return new StringAppAccessPath(containingMethod, containingType, newFields);
	}

	@Override
	public boolean isLocal() {
		return containingMethod != null;
	}

	@Override
	public boolean isField() {
		return containingType != null;
	}

	@Override
	public AppAccessPath getSuffix(int n) {
		List<String> newFields = new ArrayList<String>();
		
		for (int i = fields.size() - n ; i < fields.size() ; i++) {
			newFields.add(fields.get(i));
		}
		
		return new StringAppAccessPath(containingMethod, containingType, newFields);
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		if (isField()) result.append(containingType.getShortName() + "::");
		result.append(StringUtils.join(fields, '.'));
		if (isLocal()) result.append("@" + containingMethod.toString());
		return result.toString();
	}
	
	private StringAppAccessPath downcast(AppAccessPath ap) {
		if (ap instanceof StringAppAccessPath == false)
			throw new IllegalArgumentException("A StringAppAccessPath can only be compared with another StringAppAccessPath");
		return (StringAppAccessPath)ap;
	}
	
	private int calculateHash() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((containingType == null) ? 0 : containingType.hashCode());
		result = prime
				* result
				+ ((containingMethod == null) ? 0 : containingMethod.hashCode());
		result = prime * result + ((fields == null) ? 0 : fields.hashCode());
		return result;
	}

}
