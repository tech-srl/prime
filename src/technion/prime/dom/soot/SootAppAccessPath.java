package technion.prime.dom.soot;
/*
package aum.dom.soot;
import java.util.ArrayList;
import java.util.List;

import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Value;
import soot.jimple.Constant;
import soot.jimple.StaticFieldRef;
import aum.dom.AppAccessPath;
import aum.dom.AppObject;

public class SootAppAccessPath extends SootSceneItem implements AppAccessPath {
	
	private final AppObject original;
	private final String name;
	private final AppObject anchor;
	private final SootClass definingClass;
	private final SootMethod containingMethod;

	public SootAppAccessPath(Scene scene, AppObject original, String name, AppObject anchor,
			SootClass definingClass, SootMethod containingMethod) {
		super(scene);
		this.original = original;
		this.name = name;
		this.anchor = anchor;
		this.definingClass = definingClass;
		this.containingMethod = containingMethod;
	}

	private SootAppAccessPath downcast(AppAccessPath ap) {
		if (ap instanceof SootAppAccessPath == false)
			throw new IllegalArgumentException("A SootAppAccessPath can only be compared with another SootAppAccessPath");
		return (SootAppAccessPath)ap;
	}

	@Override
	public boolean prefixedBy(AppAccessPath ap) {
		SootAppAccessPath sap = downcast(ap);
		return (anchor.equals(sap.anchor) || anchor.getAccessPath().prefixedBy(ap));
	}

	@Override
	public int getLength() {
		return 1 + (anchor == null ? 0 : anchor.getAccessPath().getLength());
	}
	
	private AppAccessPath createAp(AppObject original, AppObject lastAnchor) {
		SootAppAccessPath sap = downcast(original.getAccessPath());
		return new SootAppAccessPath(
				scene,
				original,
				sap.name,
				sap.anchor == null ? lastAnchor : sap.anchor,
				sap.definingClass,
				containingMethod);
	}

	@Override
	public AppAccessPath concat(AppAccessPath ap) {
		SootAppAccessPath sap = downcast(ap);
		AppAccessPath newAp = null;
		newAp = new SootAppAccessPath(scene, null, sap.name, createAp(sap.anchor), sap.definingClass, containingMethod);
		
		String newName = sap.name;
		AppObject newAnchor = sap.anchor;
		
		while (sap.anchor != null) {
			AppObject newAnchor = sap.anchor;
			sap = downcast(sap.anchor.getAccessPath());
		}
		sap.anchor
		
		ArrayList<Value> newValues = new ArrayList<Value>();
		newValues.addAll(values);
		newValues.addAll(sap.values);
		
		return new SootAppAccessPath(scene, newValues);
	}

	@Override
	public boolean isLocal() {
		return values.get(0) instanceof Local;
	}

	@Override
	public boolean isField() {
		Value first = values.get(0);
		return first instanceof StaticFieldRef || first instanceof Constant;
	}

	@Override
	public AppAccessPath getSuffix(int n) {
		ArrayList<Value> newValues = new ArrayList<Value>(n);
		
		for (int i = getLength() - n ; i < getLength() ; i++) {
			newValues.add(values.get(i));
		}
		
		return new SootAppAccessPath(scene, newValues);
	}
}
 */
