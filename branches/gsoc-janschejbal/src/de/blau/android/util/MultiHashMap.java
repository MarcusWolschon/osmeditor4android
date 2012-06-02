package de.blau.android.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * This data structure is a combination of HashMap and HashSet.
 * Each key can be assigned not one, but multiple values.
 * @author Jan
 *
 * @param <K> Key type
 * @param <V> Type of the values to be associated with the keys
 */
public class MultiHashMap<K, V> {
	
	private HashMap<K, HashSet<V>> map = new HashMap<K, HashSet<V>>();
	
	/**
	 * Adds item to the set of values associated with keys
	 * @returns true if the element was added, false if it was already in the set
	 */
	public boolean add(K key, V item) {
		HashSet<V> values = map.get(key);
		if (values == null) {
			values = new HashSet<V>();
			map.put(key, values);
		}
		return values.add(item);
	}
	
	/**
	 * Removes the item from the set associated with the given key
	 * @param key
	 * @param item the item to remove
	 * @return true if the item was in the set
	 */
	public boolean removeItem(K key, V item) {
		HashSet<V> values = map.get(key);
		if (values != null) return values.remove(item);
		return false;
	}

	/**
	 * Completely removes all values associated with a key
	 * @param key
	 */
	public void removeKey(K key) {
		map.remove(key);
	}
	
	/**
	 * Gets the list of items associated with a key.
	 * @param key
	 * @return a unmodifiable list of the items associated with the key, may be empty but never null
	 */
	public Set<V> get(K key) {
		HashSet<V> values = map.get(key);
		if (values == null) return Collections.emptySet();
		return Collections.unmodifiableSet(values);
	}
	
	/**
	 * Guess what.
	 */
	public void clear() {
		map.clear();
	}


}
