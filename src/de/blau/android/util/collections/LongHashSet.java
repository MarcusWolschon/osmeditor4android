package de.blau.android.util.collections;

import java.io.Serializable;
import java.util.Arrays;

import android.annotation.SuppressLint;
import android.os.Build;

/**
 * long HashSet
 * 
 * Fast storage of unique long values, based on public domain code see http://unlicense.org from Mikhail
 * Vorontsov, see https://github.com/mikvor
 * 
 * Currently doe not Implement the Iterable interface as returning primitive types would require Java 8
 * 
 * This code is not thread safe and requires external synchronization if inserts and removals need to be 
 * made in a consistent fashion. 
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

	private static final long FREE_KEY = 0; 
	private static final float DEFAULT_FILLFACTOR = 0.75f;
	private static final int DEFAULT_CAPACITY = 16;

	/** Keys and values */
	private long[] m_data;

	/** Fill factor, must be between (0 and 1) */
	private final float m_fillFactor;
	/** We will resize a map once it reaches this size */
	private int m_threshold;
	/** Current map size */
	private int m_size;
	/** Mask to calculate the original position */
	private long m_mask;
	/** Do we have 'free' key in the map? */
    private boolean m_hasFreeKey;

	/**
	 * Create a new map with default values for capacity and fill factor
	 */
	public LongHashSet() {
		this(DEFAULT_CAPACITY, DEFAULT_FILLFACTOR);
	}

	/**
	 * Create a new map with the specified size and the default fill factor
	 * @param size
	 */
	public LongHashSet(final int size) {
		this(size, DEFAULT_FILLFACTOR);
	}

	/**
	 * Create a new map with the specified size and fill factor
	 * @param size
	 * @param fillFactor
	 */
	public LongHashSet(final int size, final float fillFactor) {
		if (fillFactor <= 0 || fillFactor >= 1) {
			throw new IllegalArgumentException("FillFactor must be in (0, 1)");
		}
		if (size <= 0) {
			throw new IllegalArgumentException("Size must be positive!");
		}
		final int capacity = Tools.arraySize(size, fillFactor);
		m_mask = capacity - 1;
		m_fillFactor = fillFactor;

		m_data = new long[capacity];

		m_threshold = (int) (capacity * fillFactor);
		
		m_hasFreeKey = false;
	}

	/**
	 * Create a shallow copy of the specified map
	 * @param map
	 */
	@SuppressLint("NewApi")
	public LongHashSet(LongHashSet map) {
		m_mask = map.m_mask;
		m_fillFactor = map.m_fillFactor;
		m_threshold = map.m_threshold;
		m_size = map.m_size;
		m_hasFreeKey = map.m_hasFreeKey;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
			m_data = Arrays.copyOf(map.m_data, map.m_data.length);
		} else { // sigh
			m_data = new long[map.m_data.length];
			System.arraycopy(map.m_data, 0, m_data, 0, m_data.length);
		}
	}


	/**
	 * Add a single element to the map
	 * @param key
	 * @param value
	 * @return
	 */
	public void put(final long key) {
		if (key == FREE_KEY) {
			m_hasFreeKey = true;
			return;
		}
		int ptr = (int) ((Tools.phiMix(key) & m_mask));
		long e = m_data[ptr];

		if (e == FREE_KEY) // end of chain already
		{
			m_data[ptr] = key;
			if (m_size >= m_threshold) {
				rehash(m_data.length * 2); // size is set inside
			} else {
				++m_size;
			}
			return;
		} else if (e == key) { // we check FREE and REMOVED prior to this call
			return;
		}

		while (true) {
			ptr = (int) ((ptr + 1) & m_mask); // the next index calculation
			e = m_data[ptr];
			if (e == FREE_KEY) {
				m_data[ptr] = key;
				if (m_size >= m_threshold) {
					rehash(m_data.length * 2); // size is set inside
				} else {
					++m_size;
				}
				return;
			} else if (e == key) {
				return;
			} 
		}
	}

	/**
	 * Add all elements from map 
	 * @param map
	 */
//	public void putAll(LongHashSet map) {
//		ensureCapacity(m_data.length + map.size());
//		for (Long e : map) { // trivial implementation for now
//			put(e);
//		}
//	}

	/**
	 * Remove element with the specified key from the map, 
	 * does not shrink the underlying array
	 * @param key
	 * @return
	 */
	public boolean remove(final long key) {
		if (key == FREE_KEY) {
			m_hasFreeKey = false;
			return true;
		}
		int ptr = (int) (Tools.phiMix(key) & m_mask);
		long e = m_data[ptr];
		if (e == FREE_KEY) {
			return false; // end of chain already
		} else if (e == key) // we check FREE and REMOVED prior to this call
		{
			--m_size;
			shiftKeys( ptr );
			return true;
		}
		while (true) {
			ptr = (int) ((ptr + 1) & m_mask); // that's next index calculation
			e = m_data[ptr];
			if (e == FREE_KEY) {
				return false;
			} else if (e == key) {
				--m_size;
				shiftKeys( ptr );
				return true;
			}
		}
	}

    private int shiftKeys(int pos)
    {
        // Shift entries with the same hash.
        int last, slot;
        long k;
        final long[] data = this.m_data;
        while ( true )
        {
            pos = (int) (((last = pos) + 1) & m_mask);
            while ( true )
            {
                if ((k = data[pos]) == FREE_KEY)
                {
                    data[last] = FREE_KEY;
                    return last;
                }
                slot = (int) ((Tools.phiMix(k) & m_mask));//calculate the starting slot for the current key
                if (last <= pos ? last >= slot || slot > pos : last >= slot && slot > pos) break;
                pos = (int) ((pos + 1) & m_mask); //go to the next entry
            }
            data[last] = k;
        }
    }
	
	/**
	 * Return true if the map contains an object with the specified key
	 * @param key
	 * @return
	 */
	public boolean contains(long key) {
		if (key == FREE_KEY) {
			return true;
		}
		int ptr = (int) ((Tools.phiMix(key) & m_mask));
		long e = m_data[ptr];
		if (e == FREE_KEY) {
			return false;
		}
		if (e == key) { // note this assumes REMOVED_KEY doesn't match
			return true;
		}
		while (true) {
			ptr = (int) ((ptr + 1) & m_mask); // the next index
			e = m_data[ptr];
			if (e == FREE_KEY) {
				return false;
			}
			if (e == key) {
				return true;
			}
		}	
	}

	/**
	 * Return all values in the set.
	 * Note: they are returned unordered
	 * @return
	 */
	public long[] values() {
		int found = 0;
		long[] result = new long[m_size];
		for (int i=0; i< m_data.length; i++) {
			long v =m_data[i];
			if (v != FREE_KEY) {
				result[found] = v;
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
	 * @return
	 */
	public int size() {
		return m_size;
	}

	/**
	 * Return true if the map is empty
	 * @return
	 */
	public boolean isEmpty() {
		return m_size == 0;
	}
	
	public void clear() {
		for (int i=0;i<m_data.length;i++) {
			m_data[i] = FREE_KEY;
		}
		m_size = 0;
		m_hasFreeKey = false;
	}

	/**
	 * Provide capacity for minimumCapacity elements
	 * without need for growing the underlying array and
	 * rehashing.
	 * @param minimumCapacity
	 */
	public void ensureCapacity(int minimumCapacity) {
		int newCapacity = Tools.arraySize(minimumCapacity, m_fillFactor);
		if (newCapacity > m_data.length) {
			rehash(newCapacity);
		}
	}

	private void rehash(final int newCapacity) {
		m_threshold = (int) (newCapacity * m_fillFactor);
		m_mask = newCapacity - 1;

		final int oldCapacity = m_data.length;
		final long[] oldData = m_data;

		m_data = new long[newCapacity];
		
		m_size = 0;

		for (int i = 0; i < oldCapacity; i++) {
			final long e = oldData[i];
			if (e != FREE_KEY) {
				put(e);
			}
		}
	}
}
