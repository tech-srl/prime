package technion.prime.dom.soot;

import java.util.ArrayList;
import java.util.List;

import soot.Local;
import soot.NullType;
import soot.Scene;
import soot.SootClass;
import soot.SootFieldRef;
import soot.SootMethod;
import soot.Value;
import soot.jimple.CastExpr;
import soot.jimple.Constant;
import soot.jimple.FieldRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.NullConstant;
import soot.jimple.ParameterRef;
import soot.jimple.Ref;
import soot.jimple.ThisRef;

import technion.prime.dom.AppType;
import technion.prime.dom.AppAccessPath;
import technion.prime.dom.AppMethodDecl;
import technion.prime.dom.AppObject;
import technion.prime.dom.StringAppAccessPath;

public class SootAppObject extends SootSceneItem implements AppObject {
	
	private Value v;
	transient private SootMethod m;
	
	public SootAppObject(Scene scene, Value v, SootMethod containingMethod) {
		super(scene);
		m = containingMethod;
		this.v = v;
	}
	
	@Override
	public AppAccessPath getAccessPath() {
		return getStringAccessPath();
		//return getSootAccessPath();
	}
	
	private AppAccessPath getStringAccessPathFromValue(Value v) {
		AppMethodDecl definingMethod = null;
		AppType definingType = null;
		List<String> fields = new ArrayList<String>();
		
		if (v instanceof Local) {
			definingMethod = new SootAppMethodDecl(scene, m);
			fields.add(((Local) v).getName());
		}
		
		if (v instanceof ThisRef) {
			SootClass c = scene.getSootClass(v.getType().toString());
			definingType = new SootAppType(scene, c.getType());
			fields.add("this");
		}
		
		if (v instanceof ParameterRef) {
			definingMethod = new SootAppMethodDecl(scene, m);
			fields.add("$parameter" + ((ParameterRef)v).getIndex());
		}
		
		if (v instanceof Constant) {
			fields.add("constant");
		}
		
		if (v instanceof FieldRef) {
			SootClass c = ((FieldRef) v).getFieldRef().declaringClass();
			definingType = new SootAppType(scene, c.getType());
//			if (v instanceof StaticFieldRef) {
//				definingClass = new SootAppClass(scene, ((StaticFieldRef) v).getFieldRef().declaringClass());
//			}
			if (v instanceof InstanceFieldRef) {
				fields.add(definingType.getShortName());
				// If access path can take field aliases into consideration, the above line:
				//   fields.add(definingType.getShortName());
				// be changed to 
				//   fields.add(((InstanceFieldRef)v).getBase().toString());
			}
			fields.add(((FieldRef)v).getFieldRef().name());
		}
		
		if (v instanceof CastExpr) {
			return getStringAccessPathFromValue(((CastExpr) v).getOp());
		}
		
		if (fields.isEmpty()) {
			fields.add(v.toString());
		}
		
		return new StringAppAccessPath(definingMethod, definingType, fields);
	}

	private AppAccessPath getStringAccessPath() {
		return getStringAccessPathFromValue(v);
	}

	/*
	private AppAccessPath getSootAccessPath() {
		SootClass definingClass = null;
		SootMethod containingMethod = null;
		AppObject anchor = null;
		String name;
		
		if (v instanceof Local) {
			containingMethod = m;
			name = ((Local) v).getName();
		}
		
		if (v instanceof InstanceFieldRef) {
			anchor = new SootAppObject(scene, ((InstanceFieldRef) v).getBase(), containingMethod);
			name = ((InstanceFieldRef) v).getFieldRef().name();
		}
		
		if (v instanceof StaticFieldRef) {
			definingClass = ((StaticFieldRef) v).getFieldRef().declaringClass();
			name = ((StaticFieldRef) v).getFieldRef().name();
		}
		return new SootAppAccessPath(scene, name, anchor, definingClass, containingMethod);
	}
*/
	
	@Override
	public String getVarName() {
		if (v instanceof FieldRef) {
			String base = null;
			SootFieldRef fieldRef = ((FieldRef)v).getFieldRef();
			if (((FieldRef)v).getFieldRef().isStatic()) {
				base = fieldRef.declaringClass().getName();
			} else {
				base = ((InstanceFieldRef)v).getBase().toString();
				//base = fieldRef.getSignature();
//				return fieldRef.name();
			}
			return base + "." + fieldRef.name();
		}
		if (v instanceof ThisRef) {
			return "this";
		}
		if (v instanceof ParameterRef) {
			return "$param" + ((ParameterRef)v).getIndex();
		}
		assert(v.toString().contains(" ") == false);
		return v.toString();
	}

	@Override
	public SootAppType getType() {
		return new SootAppType(scene, v.getType());
	}
	
	@Override
	public boolean isNull() {
		return (v.getType() instanceof NullType || v instanceof NullConstant); 
	}
	
	@Override
	public String toString() {
		return getVarName() + "(" + getType().getFullName() + ")";
	}

	@Override
	public int hashCode() {
		return 101 * getVarName().hashCode() + 13 * getType().hashCode();
		//return v.equivHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AppObject == false) return false;
		AppObject appObj = (AppObject)obj;
		return (appObj.getVarName().equals(getVarName()) &&
				appObj.getType().equals(getType()));
		//return v.equivTo(((SootAppObject)obj).v);
	}

	@Override
	public boolean isConstant() {
		return v instanceof Constant;
	}
	
	@Override
	public boolean isVariable() {
		return isVariable(v);
	}
	
	private static boolean isVariable(Value v) {
		if (v instanceof CastExpr) {
			return isVariable(((CastExpr) v).getOp());
		}
		return v instanceof Ref || v instanceof Local;
	}

}
