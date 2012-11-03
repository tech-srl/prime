package technion.prime.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Maps a single key to a set of values.
 * Not thread-safe. Does not support null values.
 * @author amishne
 *
 * @param <K> Key type. Must correctly implement hashCode().
 * @param <V> Value type. Must correctly implement hashCode().
 */
public class MultiMap<K extends Serializable, V extends Serializable> implements Cloneable, Serializable {
	private static final long serialVersionUID = -6981370535692644211L;
	
	private final Map<K, Set<V>> data;
	
	public MultiMap() {
		data = new HashMap<K, Set<V>>();
	}
	
	private Set<V> createSet() {
		return new HashSet<V>();
	}
	
	/**
	 * Add a value to the items associated with a key. If the key
	 * does not yet exist, creates it. If an equal value is
	 * already associated with the key, nothing happens.
	 * @param key Key to add the value to.
	 * @param value Value to add.
	 * @throws IllegalArgumentException If the value is null.
	 */
	public void put(K key, V value) {
		if (value == null) throw new IllegalArgumentException("cannot use null as value");
		if (data.containsKey(key) == false) data.put(key, createSet());
		data.get(key).add(value);
	}
	
	/**
	 * Get a single value from the set of values associated
	 * with a key.
	 * @param key
	 * @return One of the values associated with the key.
	 * Returns null if the key does not exist.
	 */
	public V getOne(K key) {
		Set<V> values = data.get(key);
		if (values == null) return null;
		assert values.size() > 0;
		return values.iterator().next();
	}
	
	/**
	 * Get all the values associated with a key. Returns null
	 * if the key does not exist. It is impossible for this method to return an empty set.
	 * Non-defensive.
	 * @param key
	 * @return All the values associated with the key.
	 * Returns null if the key does not exist.
	 */
	public Set<V> getAll(K key) {
		return data.get(key);
	}
	
	/**
	 * Remove all the keys and values from this map.
	 */
	public void clear() {
		data.clear();
	}
	
	/**
	 * @return True if there are no keys and values in this map.
	 */
	public boolean isEmpty() {
		return data.isEmpty();
	}
	
	/**
	 * @return A set of all the keys.
	 */
	public Set<K> keySet() {
		return data.keySet();
	}
	
	/**
	 * @return All the values stored in the MultiMap.
	 */
	public Collection<V> values() {
		Collection<V> result = new ArrayList<V>(data.size());
		for (Set<V> values : data.values()) {
			result.addAll(values);
		}
		return result;
	}
	
	/**
	 * @return Number of keys in the map.
	 */
	public int size() {
		return data.size();
	}
	
	@Override
	public String toString() {
		return data.toString();
	}

	/**
	 * @return Get one of the keys in this map. Returns null if this map is empty.
	 */
	public K getOneKey() {
		return data.keySet().isEmpty() ? null : data.keySet().iterator().next();
	}

	/**
	 * Remove a key and all the values associated with it.
	 * @param k The key to remove.
	 */
	public void removeKey(K k) {
		data.remove(k);
	}
	
	/**
	 * Remove the value <code>v</code> from the set of values associated with the key
	 * <code>k</code>. Does nothing if either <code>k<code> does not exist, or it exists but
	 * <code>v</code> is not associated with it.
	 * @param k
	 * @param v
	 */
	public void removeValue(K k, V v) {
		Set<V> set = data.get(k);
		if (set == null) return;
		if (set.size() == 1) removeKey(k);
		else set.remove(v);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public MultiMap<K, V> clone() {
		MultiMap<K, V> result = new MultiMap<K, V>();
		for (Map.Entry<K, Set<V>> e : data.entrySet()) {
			result.data.put(e.getKey(), (Set<V>) ((HashSet<V>)e.getValue()).clone());
		}
		return result;
	}

	/**
	 * @param k
	 * @return True if the map contains the given key.
	 */
	public boolean containsKey(K k) {
		return data.containsKey(k);
	}

}
