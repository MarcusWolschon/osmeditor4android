package de.blau.android.util.collections;

import java.util.Arrays;

/**
 * Simple list like collection for primitive float values
 * 
 * @author simon
 *
 */
public class FloatPrimitiveList {
    private float[] array;
    private int     size     = 0;
    private int     capacity = 0;

    /**
     * Construct a new instance with capacity initial capacity
     * 
     * @param capacity initial capacity
     */
    public FloatPrimitiveList(int capacity) {
        array = new float[capacity];
        this.capacity = capacity;
    }

    public FloatPrimitiveList() {
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
     * Add a float at the end of the list
     * 
     * THis will increase the size of the backing array if necessary
     * 
     * @param f float to add
     */
    public void add(float f) {
        if (size == capacity) {
            capacity = capacity * 2;
            array = Arrays.copyOf(array, capacity);
        }
        array[size] = f;
        size++;
    }

    /**
     * Get the float at position i
     * 
     * @param i position we want the value for
     * @return the requested float
     */
    public float get(int i) {
        if (i > size - 1) {
            throw new IndexOutOfBoundsException(Integer.toString(i) + " is larger than " + Integer.toString(size));
        }
        return array[i];
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
     * @return that float values in an array
     */
    public float[] values() {
        return Arrays.copyOf(array, size);
    }

    /**
     * Get the array backing this object
     * 
     * This is useful if you want to avoid allocating a new array and copying the contents, only useful together with
     * the value of size()
     * 
     * @return the backing float array
     */
    public float[] getArray() {
        return array;
    }
}
