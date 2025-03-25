package io.vespucci.util.collections;

import java.io.Serializable;
import java.util.Arrays;

import androidx.annotation.NonNull;

/**
 * Simple list like collection for primitive long values
 * 
 * @author simon
 *
 */
public class LongPrimitiveList implements Serializable {
    private static final long serialVersionUID = 1L;

    private long[] array;
    private int    size     = 0;
    private int    capacity = 0;

    /**
     * Construct a new instance with capacity initial capacity
     * 
     * @param capacity initial capacity
     */
    public LongPrimitiveList(int capacity) {
        array = new long[capacity];
        this.capacity = capacity;
    }

    /**
     * COnstruct a new instance with the default initial capacity
     */
    public LongPrimitiveList() {
        this(12);
    }

    /**
     * Count of actual floats in the object
     * 
     * @return count of floats stored in the object
     */
    public int size() {
        return size;
    }

    /**
     * Add a long at the end of the list
     * 
     * THis will increase the size of the backing array if necessary
     * 
     * @param l long to add
     */
    public void add(long l) {
        if (size == capacity) {
            capacity = capacity * 2;
            array = Arrays.copyOf(array, capacity);
        }
        array[size] = l;
        size++;
    }

    /**
     * Get the long at position i
     * 
     * @param i position we want the value for
     * @return the requested long
     */
    public long get(int i) {
        if (i > size - 1) {
            indexOutOfBounds(size, i);
        }
        return array[i];
    }

    /**
     * Throw an IndexOutOfBoundsException
     * 
     * @param size current list size
     * @param i the index
     */
    static void indexOutOfBounds(int size, int i) {
        throw new IndexOutOfBoundsException(Integer.toString(i) + " is larger than " + Integer.toString(size));
    }

    /**
     * Set the long at position i
     *
     * @param i position we want the value for
     * @param l the value to be set
     */
    public void set(int i, long l) {
        if (i > size - 1) {
            indexOutOfBounds(size, i);
        }
        array[i] = l;
    }

    /**
     * Truncate the list
     *
     * @param s new size to be set
     */
    public void truncate(int s) {
        if (s > size) {
            indexOutOfBounds(size, s);
        }
        size = s;
    }

    /**
     * Reset the contents
     * 
     * Note this doesn't shrink the backing array
     */
    public void clear() {
        size = 0;
    }

    /**
     * Get a float array containing all values
     * 
     * @return that long values in an array
     */
    @NonNull
    public long[] values() {
        return Arrays.copyOf(array, size);
    }

    /**
     * Check if the list contains the value
     * 
     * @param l the long we are looking for
     * @return true if l is present
     */
    public boolean contains(long l) {
        for (int i = 0; i < size; i++) {
            if (array[i] == l) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the array backing this object
     * 
     * This is useful if you want to avoid allocating a new array and copying the contents, only useful together with
     * the value of size()
     * 
     * @return the backing long array
     */
    @NonNull
    public long[] getArray() {
        return array;
    }
}
