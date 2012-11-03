package technion.prime.dom.dummy;

import java.util.Arrays;
import java.util.List;

import technion.prime.dom.StringAppAccessPath;

import technion.prime.dom.AppAccessPath;
import technion.prime.dom.AppObject;
import technion.prime.dom.AppType;

public class DummyAppObject implements AppObject {
	
	private String varName;
	private AppType type;
	
	public DummyAppObject(String varName, AppType type) {
		this.varName = varName;
		this.type = type;
	}
	
	@Override
	public AppType getType() {
		return type;
	}

	@Override
	public String getVarName() {
		return varName;
	}
	
	@Override
	public boolean isNull() {
		return varName.equals("null");
	}
	
	@Override
	public String toString() {
		return getVarName() + "(" + getType().toString() + ")";
	}

	@Override
	public int hashCode() {
		return 101 * getVarName().hashCode() + 13 * getType().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AppObject == false) return false;
		AppObject appObj = (AppObject)obj;
		return (appObj.getVarName().equals(getVarName()) &&
				appObj.getType().equals(getType()));
	}

	@Override
	public boolean isConstant() {
		return false;
	}

	@Override
	public boolean isVariable() {
		return false;
	}

	@Override
	public AppAccessPath getAccessPath() {
		List<String> fields = Arrays.asList(varName.split("\\."));
		return new StringAppAccessPath(null, type, fields);
	}

}
