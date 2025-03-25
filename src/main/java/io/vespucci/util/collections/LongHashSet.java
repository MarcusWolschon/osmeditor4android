package io.vespucci.util.collections;

import java.io.Serializable;
import java.util.Arrays;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;

/**
 * long HashSet
 * 
 * Fast storage of unique long values, based on public domain code see http://unlicense.org from Mikhail Vorontsov, see
 * https://github.com/mikvor
 * 
 * Currently doe not Implement the Iterable interface as returning primitive types would require Java 8
 * 
 * This code is not thread safe and requires external synchronization if inserts and removals need to be made in a
 * consistent fashion.
 * 
 * @version 0.1
 * @author simon
 */
@SuppressLint("UseSparseArrays")
public class LongHashSet implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L; // NOTE if you change the
                                                     // hashing algorithm you
                                                     // need to increment
                                                     // this

    private static final long  FREE_KEY           = 0;
    /**
     * Default fill factor
     */
    private static final float DEFAULT_FILLFACTOR = 0.75f;
    /**
     * Default capacity
     */
    private static final int   DEFAULT_CAPACITY   = 16;

    /** Keys and values */
    private long[] data;

    /** Fill factor, must be between (0 and 1) */
    private final float fillFactor;
    /** We will resize a map once it reaches this size */
    private int         threshold;
    /** Current map size */
    private int         size;
    /** Mask to calculate the original position */
    private long        mask;
    /** Do we have 'free' key in the map? */
    private boolean     hasFreeKey;

    /**
     * Create a new map with default values for capacity and fill factor
     */
    public LongHashSet() {
        this(DEFAULT_CAPACITY, DEFAULT_FILLFACTOR);
    }

    /**
     * Create a new map with the specified size and the default fill factor
     * 
     * @param size initial capacity of the set
     */
    public LongHashSet(final int size) {
        this(size, DEFAULT_FILLFACTOR);
    }

    /**
     * Create a new map with the specified size and fill factor
     * 
     * @param size initial capacity of the set
     * @param fillFactor fillfactor to us instead of the default
     */
    private LongHashSet(final int size, final float fillFactor) {
        if (fillFactor <= 0 || fillFactor >= 1) {
            throw new IllegalArgumentException("FillFactor must be in (0, 1)");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive!");
        }
        final int capacity = Tools.arraySize(size, fillFactor);
        this.mask = capacity - 1L;
        this.fillFactor = fillFactor;

        data = new long[capacity];

        threshold = (int) (capacity * fillFactor);

        hasFreeKey = false;
    }

    /**
     * Create a shallow copy of the specified set
     * 
     * @param set the set to copy
     */
    @SuppressLint("NewApi")
    public LongHashSet(@NonNull LongHashSet set) {
        mask = set.mask;
        fillFactor = set.fillFactor;
        threshold = set.threshold;
        size = set.size;
        hasFreeKey = set.hasFreeKey;
        data = Arrays.copyOf(set.data, set.data.length);
    }

    /**
     * Add a single element to the map
     * 
     * @param value value to add
     */
    public void put(final long value) {
        if (value == FREE_KEY) {
            hasFreeKey = true;
            return;
        }
        int ptr = (int) (Tools.phiMix(value) & mask);
        long e = data[ptr];

        if (e == FREE_KEY) { // end of chain already
            data[ptr] = value;
            if (size >= threshold) {
                rehash(data.length * 2); // size is set inside
            } else {
                ++size;
            }
            return;
        } else if (e == value) { // we check FREE and REMOVED prior to this call
            return;
        }

        while (true) {
            ptr = (int) ((ptr + 1) & mask); // the next index calculation
            e = data[ptr];
            if (e == FREE_KEY) {
                data[ptr] = value;
                if (size >= threshold) {
                    rehash(data.length * 2); // size is set inside
                } else {
                    ++size;
                }
                return;
            } else if (e == value) {
                return;
            }
        }
    }

    /**
     * Add all longs from an array
     * 
     * @param array the long array
     */
    public void putAll(@NonNull long[] array) {
        ensureCapacity(data.length + array.length);
        for (long e : array) { // trivial implementation for now
            put(e);
        }
    }

    /**
     * Remove element with the specified value from the set, does not shrink the underlying array
     * 
     * @param value value to remove
     * @return true if found and removed
     */
    public boolean remove(final long value) {
        if (value == FREE_KEY) {
            hasFreeKey = false;
            return true;
        }
        int ptr = (int) (Tools.phiMix(value) & mask);
        long e = data[ptr];
        if (e == FREE_KEY) {
            return false; // end of chain already
        } else if (e == value) { // we check FREE and REMOVED prior to this call
            --size;
            shiftKeys(ptr);
            return true;
        }
        while (true) {
            ptr = (int) ((ptr + 1) & mask); // that's next index calculation
            e = data[ptr];
            if (e == FREE_KEY) {
                return false;
            } else if (e == value) {
                --size;
                shiftKeys(ptr);
                return true;
            }
        }
    }

    /**
     * Shift entries with the same hash.
     * 
     * @param pos starting pos
     * @return free slot
     */
    private int shiftKeys(int pos) {
        int last;
        int slot;
        long k;
        final long[] temp = this.data;
        while (true) {
            last = pos;
            pos = (int) ((pos + 1) & mask);
            while (true) {
                if ((k = temp[pos]) == FREE_KEY) {
                    temp[last] = FREE_KEY;
                    return last;
                }
                slot = (int) (Tools.phiMix(k) & mask);// calculate the starting slot for the current key
                if (last <= pos ? last >= slot || slot > pos : last >= slot && slot > pos) {
                    break;
                }
                pos = (int) ((pos + 1) & mask); // go to the next entry
            }
            temp[last] = k;
        }
    }

    /**
     * Return true if the map contains an object with the specified value
     * 
     * @param value value to check for
     * @return true if value was found
     */
    public boolean contains(long value) {
        if (value == FREE_KEY) {
            return true;
        }
        int ptr = (int) (Tools.phiMix(value) & mask);
        long e = data[ptr];
        if (e == FREE_KEY) {
            return false;
        }
        if (e == value) { // note this assumes REMOVED_KEY doesn't match
            return true;
        }
        while (true) {
            ptr = (int) ((ptr + 1) & mask); // the next index
            e = data[ptr];
            if (e == FREE_KEY) {
                return false;
            }
            if (e == value) {
                return true;
            }
        }
    }

    /**
     * Return all values in the set. Note: they are returned unordered
     * 
     * @return array containing the values
     */
    @NonNull
    public long[] values() {
        int found = 0;
        long[] result = new long[size];
        for (long v : data) {
            if (v != FREE_KEY) {
                result[found] = v;
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
     * @return the element count
     */
    public int size() {
        return size;
    }

    /**
     * Return if the set is empty
     * 
     * @return true if the set is empty
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Remove all elements from the set
     */
    public void clear() {
        for (int i = 0; i < data.length; i++) {
            data[i] = FREE_KEY;
        }
        size = 0;
        hasFreeKey = false;
    }

    /**
     * Provide capacity for minimumCapacity elements without need for growing the underlying array and rehashing.
     * 
     * @param minimumCapacity minimum capacity
     */
    public void ensureCapacity(int minimumCapacity) {
        int newCapacity = Tools.arraySize(minimumCapacity, fillFactor);
        if (newCapacity > data.length) {
            rehash(newCapacity);
        }
    }

    /**
     * Recalculate the hashes for the whole set
     * 
     * @param newCapacity new capacity
     */
    private void rehash(final int newCapacity) {
        threshold = (int) (newCapacity * fillFactor);
        mask = newCapacity - 1L;

        final int oldCapacity = data.length;
        final long[] oldData = data;

        data = new long[newCapacity];

        size = 0;

        for (int i = 0; i < oldCapacity; i++) {
            final long e = oldData[i];
            if (e != FREE_KEY) {
                put(e);
            }
        }
    }
}
