package de.blau.android.util.collections;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * This data structure is a combination of a Map and Set. Each key can be assigned not one, but multiple values. Sorted
 * map/set implementations are used to guarantee (case-sensitive) alphabetical sorting of entries.
 * 
 * @author Jan
 *
 * @param <K> Key type
 * @param <V> Type of the values to be associated with the keys
 */
public class MultiHashMap<K, V> implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private Map<K, Set<V>>    map;
    private boolean           sorted;

    /** Creates a regular, unsorted MultiHashMap */
    public MultiHashMap() {
        this(false);
    }

    /**
     * Creates a MultiHashMap.
     * 
     * @param sorted if true, Tree maps/sets will be used, if false, regular HashMap/HashSets will be used.
     */
    public MultiHashMap(boolean sorted) {
        this.sorted = sorted;
        if (sorted) {
            map = new TreeMap<>();
        } else {
            map = new HashMap<>();
        }
    }

    /**
     * Check for key in map
     * 
     * @param key the key for the entry
     * @return true if key exists in map
     */
    public boolean containsKey(K key) {
        return map.containsKey(key);
    }

    /**
     * Adds item to the set of values associated with the key (null items are not added)
     * 
     * @param key key for the item to add
     * @param item the item to add
     * @return true if the element was added, false if it was already in the set or null
     */
    public boolean add(K key, V item) {
        Set<V> values = map.get(key);
        if (values == null) {
            values = (sorted ? new TreeSet<V>() : new HashSet<V>());
            map.put(key, values);
        }
        return item != null && values.add(item);
    }

    /**
     * Adds all items to the set of values associated with the key
     * 
     * @param key the key
     * @param items an array containing the items
     */
    public void add(K key, V[] items) {
        Set<V> values = map.get(key);
        if (values == null) {
            values = (sorted ? new TreeSet<V>() : new HashSet<V>());
            map.put(key, values);
        }
        values.addAll(Arrays.asList(items));
    }

    /**
     * Adds all items to the set of values associated with the key
     * 
     * @param key the key
     * @param items a set containing the items
     */
    private void add(K key, Set<V> items) {
        Set<V> values = map.get(key);
        if (values == null) {
            values = (sorted ? new TreeSet<V>() : new HashSet<V>());
            map.put(key, values);
        }
        values.addAll(items);
    }

    /**
     * Removes the item from the set associated with the given key
     * 
     * @param key key of the item we want to remove
     * @param item the item to remove
     * @return true if the item was in the set
     */
    public boolean removeItem(K key, V item) {
        Set<V> values = map.get(key);
        if (values != null)
            return values.remove(item);
        return false;
    }

    /**
     * Completely removes all values associated with a key
     * 
     * @param key key of the values we want to remove
     */
    public void removeKey(K key) {
        map.remove(key);
    }

    /**
     * Gets the list of items associated with a key.
     * 
     * @param key the key to use
     * @return a unmodifiable list of the items associated with the key, may be empty but never null
     */
    public Set<V> get(K key) {
        Set<V> values = map.get(key);
        if (values == null)
            return Collections.emptySet();
        return Collections.unmodifiableSet(values);
    }

    /**
     * Guess what.
     */
    public void clear() {
        map.clear();
    }

    /**
     * Get a Set containing all keys of the Map
     * 
     * @return a Set of the keys
     */
    public Set<K> getKeys() {
        return map.keySet();
    }

    /**
     * Get a Set containing all values of the Map
     * 
     * @return a Set of the values
     */
    public Set<V> getValues() {
        Set<V> retval = new LinkedHashSet<>();
        for (K key : getKeys()) {
            retval.addAll(get(key));
        }
        return retval;
    }

    /**
     * add all key/values from source to this Map
     * 
     * @param source a MultiHashMap from which to add keys and values
     */
    public void addAll(MultiHashMap<K, V> source) {
        for (K key : source.getKeys()) {
            add(key, source.get(key));
        }

    }
}
