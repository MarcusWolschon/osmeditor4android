package io.vespucci.util.collections;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Fixed sized HashMap with MRU semantics
 * 
 * Note that we do not extend Map
 * 
 * @param <K> Key
 * @param <V> Value
 */
public class MRUHashMap<K extends Serializable, V extends Serializable> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final MRUList<K> list;
    private final Map<K, V>  map;

    /**
     * Constract a new instance
     * 
     * @param size the maximum number of entries
     */
    public MRUHashMap(int size) {
        list = new MRUList<>(size);
        map = new HashMap<>();
    }

    /**
     * Get the current size of the collection
     * 
     * @return the size
     */
    public int size() {
        return list.size();
    }

    /**
     * Check if the collection is empty
     * 
     * @return tru eif empty
     */
    public boolean isEmpty() {
        return list.isEmpty();
    }

    /**
     * Get the object for key
     * 
     * @param key the key
     * @return the stored object or null
     */
    @SuppressWarnings("unchecked")
    public V get(Object key) {
        V result = map.get(key);
        if (result != null) {
            list.push((K) key);
        }
        return result;
    }

    /**
     * Add the key and value to the map
     * 
     * Note that this does not have the Map.put signature
     * 
     * @param key the key
     * @param value the value
     * @return the value removed from the map or null
     */
    public V put(K key, V value) {
        K removedKey = list.push(key);
        V removedValue = null;
        if (removedKey != null) {
            removedValue = map.remove(removedKey);
        }
        map.put(key, value);
        return removedValue;
    }

    /**
     * Remove the entry for a key
     * 
     * @param key the key
     * @return the removed object or null
     */
    public V remove(Object key) {
        V removed = map.remove(key);
        if (removed != null) {
            list.remove(key);
        }
        return removed;
    }

    /**
     * Empty the collection
     */
    public void clear() {
        list.clear();
        map.clear();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((list == null) ? 0 : list.hashCode());
        result = prime * result + ((map == null) ? 0 : map.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MRUHashMap<?, ?>)) {
            return false;
        }
        MRUHashMap<?, ?> other = (MRUHashMap<?, ?>) obj;
        if (list == null) {
            if (other.list != null) {
                return false;
            }
        } else if (!list.equals(other.list)) {
            return false;
        }
        if (map == null) {
            if (other.map != null) {
                return false;
            }
        } else if (!map.equals(other.map)) {
            return false;
        }
        return true;
    }
}
