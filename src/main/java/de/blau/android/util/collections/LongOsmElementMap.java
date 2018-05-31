package de.blau.android.util.collections;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmElementFactory;

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
    private static final long serialVersionUID = 2L; // NOTE if you change the
                                                     // hashing algorithm you
                                                     // need to increment
                                                     // this

    private static final OsmElement FREE_KEY           = null;
    private final OsmElement        removedKey;                // Note see constructor for important note
    private static final float      DEFAULT_FILLFACTOR = 0.75f;
    private static final int        DEFAULT_CAPACITY   = 16;

    /** Keys and values */
    private OsmElement[] m_data;

    /** Fill factor, must be between (0 and 1) */
    private final float m_fillFactor;
    /** We will resize a map once it reaches this size */
    private int         m_threshold;
    /** Current map size */
    private int         m_size;
    /** Mask to calculate the original position */
    private long        m_mask;

    /**
     * Create a new map with default values for capacity and fill factor
     */
    public LongOsmElementMap() {
        this(DEFAULT_CAPACITY, DEFAULT_FILLFACTOR);
    }

    /**
     * Create a new map with the specified size and the default fill factor
     * 
     * @param size
     */
    public LongOsmElementMap(final int size) {
        this(size, DEFAULT_FILLFACTOR);
    }

    /**
     * Create a new map with the specified size and fill factor
     * 
     * @param size
     * @param fillFactor
     */
    private LongOsmElementMap(final int size, final float fillFactor) {
        if (fillFactor <= 0 || fillFactor >= 1) {
            throw new IllegalArgumentException("FillFactor must be in (0, 1)");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive!");
        }
        final int capacity = Tools.arraySize(size, fillFactor);
        m_mask = capacity - 1L;
        m_fillFactor = fillFactor;

        m_data = new OsmElement[capacity];

        m_threshold = (int) (capacity * fillFactor);

        // NOTE can't be static as it has to be serialized and de-serialized
        removedKey = OsmElementFactory.createNode(Long.MIN_VALUE, 1, -1, OsmElement.STATE_CREATED, 0, 0);
    }

    /**
     * Create a shallow copy of the specified map
     * 
     * @param map the map to copy
     */
    public LongOsmElementMap(LongOsmElementMap<? extends V> map) {
        m_mask = map.m_mask;
        m_fillFactor = map.m_fillFactor;
        m_threshold = map.m_threshold;
        m_size = map.m_size;
        removedKey = map.removedKey;
        m_data = Arrays.copyOf(map.m_data, map.m_data.length);
    }

    /**
     * Return a single element with the specified key
     * 
     * @param key
     * @return the required element or null if it cannot be found
     */
    @SuppressWarnings("unchecked")
    public V get(final long key) {
        int ptr = (int) ((Tools.phiMix(key) & m_mask));

        OsmElement e = m_data[ptr];
        if (e == FREE_KEY) {
            return null;
        }
        if (e.getOsmId() == key) {
            return (V) e;
        }
        while (true) {
            ptr = (int) ((ptr + 1) & m_mask); // that's next index
            e = m_data[ptr];
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
    public V put(final long key, final V value) {
        int ptr = (int) ((Tools.phiMix(key) & m_mask));

        OsmElement e = m_data[ptr];

        if (e == FREE_KEY) // end of chain already
        {
            m_data[ptr] = (OsmElement) value;
            if (m_size >= m_threshold) {
                rehash(m_data.length * 2); // size is set inside
            } else {
                ++m_size;
            }
            return null;
        } else if (e.getOsmId() == key) { // we check FREE and REMOVED prior to this call
            m_data[ptr] = (OsmElement) value;
            return (V) e;
        }

        int firstRemoved = -1;
        if (e == removedKey) {
            firstRemoved = ptr; // we may find a key later
        }

        while (true) {
            ptr = (int) ((ptr + 1) & m_mask); // the next index calculation
            e = m_data[ptr];
            if (e == FREE_KEY) {
                if (firstRemoved != -1) {
                    ptr = firstRemoved;
                }
                m_data[ptr] = (OsmElement) value;
                if (m_size >= m_threshold) {
                    rehash(m_data.length * 2); // size is set inside
                } else {
                    ++m_size;
                }
                return null;
            } else if (e.getOsmId() == key) {
                m_data[ptr] = (OsmElement) value;
                return (V) e;
            } else if (e == removedKey) {
                if (firstRemoved == -1) {
                    firstRemoved = ptr;
                }
            }
        }
    }

    /**
     * Add all elements from map
     * 
     * @param map the Map to add
     */
    public void putAll(LongOsmElementMap<V> map) {
        ensureCapacity(m_data.length + map.size());
        for (V e : map) { // trivial implementation for now
            put(e.getOsmId(), e);
        }
    }

    /**
     * Add all elements from c
     * 
     * @param c the Collection to add
     */
    public void putAll(Collection<V> c) {
        ensureCapacity(m_data.length + c.size());
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
    public OsmElement remove(final long key) {
        int ptr = (int) (Tools.phiMix(key) & m_mask);
        OsmElement e = m_data[ptr];
        if (e == FREE_KEY) {
            return null; // end of chain already
        } else if (e.getOsmId() == key) { // we check FREE and REMOVED prior to this call
            --m_size;
            if (m_data[(int) ((ptr + 1) & m_mask)] == FREE_KEY) { // this shortens the chain
                m_data[ptr] = FREE_KEY;
            } else {
                m_data[ptr] = removedKey;
            }
            return e;
        }
        while (true) {
            ptr = (int) ((ptr + 1) & m_mask); // that's next index calculation
            e = m_data[ptr];
            if (e == FREE_KEY) {
                return null;
            } else if (e.getOsmId() == key) {
                --m_size;
                if (m_data[(int) ((ptr + 1) & m_mask)] == FREE_KEY) { // this shortens the chain
                    m_data[ptr] = FREE_KEY;
                } else {
                    m_data[ptr] = removedKey;
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
        int ptr = (int) ((Tools.phiMix(key) & m_mask));
        OsmElement e = m_data[ptr];
        if (e == FREE_KEY) {
            return false;
        }
        if (e.getOsmId() == key) { // note this assumes REMOVED_KEY doesn't match
            return true;
        }
        while (true) {
            ptr = (int) ((ptr + 1) & m_mask); // the next index
            e = m_data[ptr];
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
    public List<V> values() {
        int found = 0;
        ArrayList<V> result = new ArrayList<>(m_size);
        for (OsmElement v : m_data) {
            if (v != FREE_KEY && v != removedKey) {
                result.add((V) v);
                found++;
                if (found >= m_size) { // found all
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
        return m_size;
    }

    /**
     * Return true if the map is empty
     * 
     * @return true if the map is empty
     */
    public boolean isEmpty() {
        return m_size == 0;
    }

    /**
     * Provide capacity for minimumCapacity elements without need for growing the underlying array and rehashing.
     * 
     * @param minimumCapacity the capacity to ensure
     */
    private void ensureCapacity(int minimumCapacity) {
        int newCapacity = Tools.arraySize(minimumCapacity, m_fillFactor);
        if (newCapacity > m_data.length) {
            rehash(newCapacity);
        }
    }

    @SuppressWarnings("unchecked")
    private void rehash(final int newCapacity) {
        synchronized (this) {
            m_threshold = (int) (newCapacity * m_fillFactor);
            m_mask = newCapacity - 1L;

            final int oldCapacity = m_data.length;
            final OsmElement[] oldData = m_data;

            m_data = new OsmElement[newCapacity];

            m_size = 0;

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
            final OsmElement[] oldData = m_data;
            m_data = new OsmElement[m_data.length];
            m_size = 0;

            for (int i = 0; i < m_data.length; i++) {
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
        return new SafeIterator();
    }

    class SafeIterator implements Iterator<V> {
        int          index       = 0;
        int          found       = 0;
        int          m_size_temp = 0;
        OsmElement[] m_data_temp = null;
        OsmElement   cachedNext  = null;

        SafeIterator() {
            synchronized (LongOsmElementMap.this) {
                m_size_temp = m_size;
                m_data_temp = m_data;
            }
        }

        @Override
        public boolean hasNext() {
            cachedNext = null;
            while (true) {
                if (found >= m_size_temp || index >= m_data_temp.length) { // already returned all elements
                    return false;
                } else {
                    OsmElement e = m_data_temp[index];
                    if (e != FREE_KEY && e != removedKey) {
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
        public V next() {
            if (cachedNext != null) {
                index++;
                return (V) cachedNext;
            }
            while (true) {
                if (index >= m_data_temp.length) { // already returned all elements
                    throw new NoSuchElementException();
                } else {
                    OsmElement e = m_data_temp[index];
                    if (e != FREE_KEY && e != removedKey) {
                        index++;
                        return (V) e;
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
     * @return
     */
    public HashMap<Integer, Integer> getChainStats() {
        HashMap<Integer, Integer> result = new HashMap<>();
        for (V v : values()) {
            int len = 0;
            long key = v.getOsmId();
            int ptr = (int) ((Tools.phiMix(key) & m_mask));
            OsmElement e = m_data[ptr];
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
                ptr = (int) ((ptr + 1) & m_mask); // the next index
                e = m_data[ptr];
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
