package io.vespucci.util.collections;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.osm.OsmElement;
import io.vespucci.osm.OsmElementFactory;

/**
 * long to OsmElement HashMap
 * 
 * This only implements the, roughly equivalent to Map, interfaces that we current (might) need and is rather Vespucci
 * specific, however could easily be adopted to different implementations of an OSM element as long as you can easily
 * get the element id.
 * 
 * This implementation will only require at most 8*next_power_of_2(capacity/fillfactor) space.
 * 
 * This is based on public domain code see http://unlicense.org from Mikhail Vorontsov, see https://github.com/mikvor
 * The original code used a interleaved key/value implementation in one array, however since the stored value already
 * contains the key additional copies are not needed.
 * 
 * This code is not thread safe with the exception of iterating over the array when rehashing and requires external
 * synchronization if inserts and removals need to be consistent.
 * 
 * @version 0.3
 * @author simon
 */
@SuppressLint("UseSparseArrays")
public class LongOsmElementMap<V extends OsmElement> implements Iterable<V>, Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 3L; // NOTE if you change the
                                                     // hashing algorithm you
                                                     // need to increment
                                                     // this

    public interface SelectElement<W extends OsmElement> {
        /**
         * Select an element
         * 
         * @param element an OsmELement of type W
         * @return true if the element should be selected
         */
        boolean select(@NonNull W element);
    }

    private static final OsmElement FREE_KEY           = null;
    private final OsmElement        removedKey;                // Note see constructor for important note
    private static final float      DEFAULT_FILLFACTOR = 0.75f;
    private static final int        DEFAULT_CAPACITY   = 16;

    /** Keys and values */
    private OsmElement[] data;

    /** Fill factor, must be between (0 and 1) */
    private final float fillFactor;
    /** We will resize a map once it reaches this size */
    private int         threshold;
    /** Current map size */
    private int         size;
    /** Mask to calculate the original position */
    private long        mask;

    /**
     * Create a new map with default values for capacity and fill factor
     */
    public LongOsmElementMap() {
        this(DEFAULT_CAPACITY, DEFAULT_FILLFACTOR);
    }

    /**
     * Create a new map with the specified size and the default fill factor
     * 
     * @param size initial size of the map
     */
    public LongOsmElementMap(final int size) {
        this(size, DEFAULT_FILLFACTOR);
    }

    /**
     * Create a new map with the specified size and fill factor
     * 
     * @param size initial size of the map
     * @param fillFactor target fill factor
     */
    private LongOsmElementMap(final int size, final float fillFactor) {
        if (fillFactor <= 0 || fillFactor >= 1) {
            throw new IllegalArgumentException("FillFactor must be in (0, 1)");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive!");
        }
        final int capacity = Tools.arraySize(size, fillFactor);
        this.mask = capacity - 1L;
        this.fillFactor = fillFactor;

        data = new OsmElement[capacity];

        this.threshold = (int) (capacity * fillFactor);

        // NOTE can't be static as it has to be serialized and de-serialized
        removedKey = OsmElementFactory.createNode(Long.MIN_VALUE, 1, -1, OsmElement.STATE_CREATED, 0, 0);
    }

    /**
     * Create a shallow copy of the specified map
     * 
     * @param map the map to copy
     */
    public LongOsmElementMap(@NonNull LongOsmElementMap<? extends V> map) {
        mask = map.mask;
        fillFactor = map.fillFactor;
        threshold = map.threshold;
        size = map.size;
        removedKey = map.removedKey;
        data = Arrays.copyOf(map.data, map.data.length);
    }

    /**
     * Return a single element with the specified key
     * 
     * @param key the key we want to return a value for
     * @return the required element or null if it cannot be found
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public V get(final long key) {
        int ptr = (int) (Tools.phiMix(key) & mask);

        OsmElement e = data[ptr];
        if (e == FREE_KEY) {
            return null;
        }
        if (e.getOsmId() == key) {
            return (V) e;
        }
        while (true) {
            ptr = (int) ((ptr + 1) & mask); // that's next index
            e = data[ptr];
            if (e == FREE_KEY) {
                return null;
            }
            if (e.getOsmId() == key) {
                return (V) e;
            }
        }
    }

    /**
     * Add a single element to the map
     * 
     * @param key the key
     * @param value the value
     * @return the previous value if one existed
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public V put(final long key, @Nullable final V value) {
        int ptr = (int) (Tools.phiMix(key) & mask);

        OsmElement e = data[ptr];

        if (e == FREE_KEY) { // end of chain already
            data[ptr] = value;
            if (size >= threshold) {
                rehash(data.length * 2); // size is set inside
            } else {
                ++size;
            }
            return null;
        } else if (e.getOsmId() == key) { // we check FREE and REMOVED prior to this call
            data[ptr] = value;
            return (V) e;
        }

        int firstRemoved = -1;
        if (e == removedKey) {
            firstRemoved = ptr; // we may find a key later
        }

        while (true) {
            ptr = (int) ((ptr + 1) & mask); // the next index calculation
            e = data[ptr];
            if (e == FREE_KEY) {
                if (firstRemoved != -1) {
                    ptr = firstRemoved;
                }
                data[ptr] = value;
                if (size >= threshold) {
                    rehash(data.length * 2); // size is set inside
                } else {
                    ++size;
                }
                return null;
            } else if (e.getOsmId() == key) {
                data[ptr] = value;
                return (V) e;
            } else if (e == removedKey && firstRemoved == -1) {
                firstRemoved = ptr;
            }
        }
    }

    /**
     * Add all elements from map
     * 
     * @param map the Map to add
     */
    public void putAll(@NonNull LongOsmElementMap<V> map) {
        ensureCapacity(data.length + map.size());
        for (V e : map) { // trivial implementation for now
            put(e.getOsmId(), e);
        }
    }

    /**
     * Add all elements from c
     * 
     * @param c the Collection to add
     */
    public void putAll(@NonNull Collection<V> c) {
        ensureCapacity(data.length + c.size());
        for (V e : c) { // trivial implementation for now
            put(e.getOsmId(), e);
        }
    }

    /**
     * Remove element with the specified key from the map, does not shrink the underlying array
     * 
     * @param key the key we want to remove
     * @return the removed value or null if it didn't exist
     */
    @Nullable
    public OsmElement remove(final long key) {
        int ptr = (int) (Tools.phiMix(key) & mask);
        OsmElement e = data[ptr];
        if (e == FREE_KEY) {
            return null; // end of chain already
        } else if (e.getOsmId() == key) { // we check FREE and REMOVED prior to this call
            --size;
            if (data[(int) ((ptr + 1) & mask)] == FREE_KEY) { // this shortens the chain
                data[ptr] = FREE_KEY;
            } else {
                data[ptr] = removedKey;
            }
            return e;
        }
        while (true) {
            ptr = (int) ((ptr + 1) & mask); // that's next index calculation
            e = data[ptr];
            if (e == FREE_KEY) {
                return null;
            } else if (e.getOsmId() == key) {
                --size;
                if (data[(int) ((ptr + 1) & mask)] == FREE_KEY) { // this shortens the chain
                    data[ptr] = FREE_KEY;
                } else {
                    data[ptr] = removedKey;
                }
                return e;
            }
        }
    }

    /**
     * Return true if the map contains an object with the specified key
     * 
     * @param key the key to check
     * @return true if an entry for key could be found
     */
    public boolean containsKey(long key) {
        int ptr = (int) (Tools.phiMix(key) & mask);
        OsmElement e = data[ptr];
        if (e == FREE_KEY) {
            return false;
        }
        if (e.getOsmId() == key) { // note this assumes REMOVED_KEY doesn't match
            return true;
        }
        while (true) {
            ptr = (int) ((ptr + 1) & mask); // the next index
            e = data[ptr];
            if (e == FREE_KEY) {
                return false;
            }
            if (e.getOsmId() == key) {
                return true;
            }
        }
    }

    /**
     * Return all values in the map. Note: they are returned unordered
     * 
     * @return a List of the values
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public List<V> values() {
        int found = 0;
        List<V> result = new ArrayList<>(size);
        for (OsmElement v : data) {
            if (v != FREE_KEY && v != removedKey) {
                result.add((V) v);
                found++;
                if (found >= size) { // found all
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Return specific values
     * 
     * @param result pre-allocated List
     * @param s function for selecting the element
     * @return a List of the values
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public List<V> values(@NonNull List<V> result, @NonNull SelectElement<V> s) {
        int found = 0;
        for (OsmElement v : data) {
            if (v != FREE_KEY && v != removedKey) {
                if (s.select((V) v)) {
                    result.add((V) v);
                }
                found++;
                if (found >= size) { // found all
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Return the number of elements in the map
     * 
     * @return the number of elements in the map
     */
    public int size() {
        return size;
    }

    /**
     * Return true if the map is empty
     * 
     * @return true if the map is empty
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Provide capacity for minimumCapacity elements without need for growing the underlying array and rehashing.
     * 
     * @param minimumCapacity the capacity to ensure
     */
    private void ensureCapacity(int minimumCapacity) {
        int newCapacity = Tools.arraySize(minimumCapacity, fillFactor);
        if (newCapacity > data.length) {
            rehash(newCapacity);
        }
    }

    /**
     * Rehash the map
     * 
     * @param newCapacity new size
     */
    @SuppressWarnings("unchecked")
    private void rehash(final int newCapacity) {
        synchronized (this) {
            threshold = (int) (newCapacity * fillFactor);
            mask = newCapacity - 1L;

            final int oldCapacity = data.length;
            final OsmElement[] oldData = data;

            data = new OsmElement[newCapacity];

            size = 0;

            for (int i = 0; i < oldCapacity; i++) {
                final OsmElement e = oldData[i];
                if (e != FREE_KEY && e != removedKey) {
                    put(e.getOsmId(), (V) e);
                }
            }
        }
    }

    /**
     * Rehash the map - needed when id's have changed etc.
     */
    @SuppressWarnings("unchecked")
    public void rehash() {
        synchronized (this) {
            final OsmElement[] oldData = data;
            data = new OsmElement[data.length];
            size = 0;

            for (int i = 0; i < data.length; i++) {
                final OsmElement e = oldData[i];
                if (e != FREE_KEY && e != removedKey) {
                    put(e.getOsmId(), (V) e);
                }
            }
        }
    }

    /**
     * Iterator that skips FREE_KEY and REMOVED_KEY values
     */
    @NonNull
    @Override
    public Iterator<V> iterator() {
        synchronized (this) {
            return new SafeIterator<>(this);
        }
    }

    private static class SafeIterator<W extends OsmElement> implements Iterator<W> {
        int                index      = 0;
        int                found      = 0;
        final int          sizeTemp;
        final OsmElement[] dataTemp;
        OsmElement         cachedNext = null;
        final OsmElement   removedTemp;

        /**
         * Construct a new iterator
         */
        SafeIterator(@NonNull final LongOsmElementMap<W> map) {
            sizeTemp = map.size;
            dataTemp = map.data;
            removedTemp = map.removedKey;
        }

        @Override
        public boolean hasNext() {
            cachedNext = null;
            while (true) {
                if (found >= sizeTemp || index >= dataTemp.length) { // already returned all elements
                    return false;
                } else {
                    OsmElement e = dataTemp[index];
                    if (e != FREE_KEY && e != removedTemp) {
                        found++;
                        cachedNext = e;
                        return true;
                    } else {
                        index++;
                    }
                }
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public W next() {
            if (cachedNext != null) {
                index++;
                return (W) cachedNext;
            }
            while (true) {
                if (index >= dataTemp.length) { // already returned all elements
                    throw new NoSuchElementException();
                } else {
                    OsmElement e = dataTemp[index];
                    if (e != FREE_KEY && e != removedTemp) {
                        index++;
                        return (W) e;
                    } else {
                        index++;
                    }
                }
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException(); // could be implemented
        }

    }

    /**
     * for stats and debugging
     * 
     * @return a Map containing some statistics
     */
    @NonNull
    public Map<Integer, Integer> getChainStats() {
        Map<Integer, Integer> result = new HashMap<>();
        for (V v : values()) {
            int len = 0;
            long key = v.getOsmId();
            int ptr = (int) (Tools.phiMix(key) & mask);
            OsmElement e = data[ptr];
            if (e.getOsmId() == key) { // note this assumes REMOVED_KEY doesn't match
                if (result.containsKey(len)) {
                    result.put(len, result.get(len) + 1);
                } else {
                    result.put(len, 1);
                }
                continue;
            }
            while (true) {
                len++;
                ptr = (int) ((ptr + 1) & mask); // the next index
                e = data[ptr];
                if (e.getOsmId() == key) {
                    if (result.containsKey(len)) {
                        result.put(len, result.get(len) + 1);
                    } else {
                        result.put(len, 1);
                    }
                    break;
                }
            }
        }
        return result;
    }
}
