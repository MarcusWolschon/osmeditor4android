package de.blau.android.util.collections;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import androidx.annotation.NonNull;

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
    private boolean           ordered;

    /** Creates a regular, unsorted MultiHashMap */
    public MultiHashMap() {
        this(false, false);
    }

    /**
     * Creates a MultiHashMap.
     * 
     * @param sorted if true, Tree maps/sets will be used, if false, regular HashMap/HashSets will be used.
     */
    public MultiHashMap(boolean sorted) {
        this(sorted, false);
    }

    /**
     * Creates a MultiHashMap
     * 
     * @param sorted if true, Tree maps/sets will be used,
     * @param ordered if true and sorted false, a Map will be used that maintains insertion order
     */
    public MultiHashMap(boolean sorted, boolean ordered) {
        this.sorted = sorted;
        this.ordered = ordered;
        if (sorted) {
            map = new TreeMap<>();
        } else if (ordered) {
            map = new LinkedHashMap<>();
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
    public boolean containsKey(@NonNull K key) {
        return map.containsKey(key);
    }

    /**
     * Adds item to the set of values associated with the key (null items are not added)
     * 
     * @param key key for the item to add
     * @param item the item to add
     * @return true if the element was added, false if it was already in the set or null
     */
    public boolean add(@NonNull K key, @NonNull V item) {
        Set<V> values = getSet(key);
        return values.add(item);
    }

    /**
     * Adds all items to the set of values associated with the key
     * 
     * @param key the key
     * @param items the items
     */
    @SuppressWarnings("unchecked")
    public void add(@NonNull K key, @NonNull V... items) {
        Set<V> values = getSet(key);
        values.addAll(Arrays.asList(items));
    }

    /**
     * Get the Set containing the current values or allocated a new one
     * 
     * @param key the key
     * @return a Set
     */
    @NonNull
    private Set<V> getSet(@NonNull K key) {
        Set<V> values = map.get(key);
        if (values == null) {
            if (sorted) {
                values = new TreeSet<>();
            } else if (ordered) {
                values = new LinkedHashSet<>();
            } else {
                values = new HashSet<>();
            }
            map.put(key, values);
        }
        return values;
    }

    /**
     * Adds all items to the set of values associated with the key
     * 
     * @param key the key
     * @param items a set containing the items
     */
    private void add(@NonNull K key, @NonNull Set<V> items) {
        Set<V> values = getSet(key);
        values.addAll(items);
    }

    /**
     * Removes the item from the set associated with the given key
     * 
     * @param key key of the item we want to remove
     * @param item the item to remove
     * @return true if the item was in the set
     */
    public boolean removeItem(@NonNull K key, @NonNull V item) {
        Set<V> values = map.get(key);
        if (values != null) {
            return values.remove(item);
        }
        return false;
    }

    /**
     * Completely removes all values associated with a key
     * 
     * @param key key of the values we want to remove
     */
    public void removeKey(@NonNull K key) {
        map.remove(key);
    }

    /**
     * Gets the list of items associated with a key.
     * 
     * @param key the key to use
     * @return a unmodifiable list of the items associated with the key, may be empty but never null
     */
    @NonNull
    public Set<V> get(@NonNull K key) {
        Set<V> values = map.get(key);
        if (values == null) {
            return Collections.emptySet();
        }
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
    @NonNull
    public Set<V> getValues() {
        Set<V> retval = new LinkedHashSet<>();
        for (K key : getKeys()) {
            retval.addAll(get(key));
        }
        return retval;
    }

    /**
     * Add all key/values from source to this Map
     * 
     * @param source a MultiHashMap from which to add keys and values
     */
    public void addAll(@NonNull MultiHashMap<K, V> source) {
        for (K key : source.getKeys()) {
            add(key, source.get(key));
        }

    }

    /**
     * Check if this MultiHashMap is empty
     * 
     * @return true if the MultiHashMap is empty
     */
    public boolean isEmpty() {
        if (map.isEmpty()) {
            return true;
        }
        // check that all sets are empty
        for (Set<V> value : map.values()) {
            if (!value.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get the number of entries in the MultiHashMap
     * 
     * @return the number of entries
     */
    public int size() {
        return map.size();
    }

    @Override
    public int hashCode() {
        return Objects.hash(map, sorted);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MultiHashMap)) {
            return false;
        }

        @SuppressWarnings("rawtypes")
        MultiHashMap other = (MultiHashMap) obj;
        return Objects.equals(map, other.map);
    }
}
