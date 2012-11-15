package technion.prime.dom.soot;

import soot.Scene;

public abstract class SootSceneItem {
	transient protected Scene scene;
	
	// For serialization:
	protected SootSceneItem() {}
	
	public SootSceneItem(Scene scene) {
		this.scene = scene;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((scene == null) ? 0 : scene.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof SootSceneItem))
			return false;
		SootSceneItem other = (SootSceneItem) obj;
		if (scene == null) {
			if (other.scene != null)
				return false;
		} else if (!scene.equals(other.scene))
			return false;
		return true;
	}

	public void deleteScene() {
		this.scene = null;
	}
}
