package de.blau.android.util.collections;
import com.zaxxer.sparsebits.SparseBitSet;

/**
 * A SparseBitSet that will treat the index as unsigned values
 * 
 * Only those methods we actually use are implemented
 * 
 * @author simon
 *
 */
public class UnsignedSparseBitSet {

    static final int TOP_BIT       = 0x80000000;
    static final int UNSIGNED_BITS = 0x7FFFFFFF;

    SparseBitSet bottom = new SparseBitSet();
    SparseBitSet top    = new SparseBitSet();

    /**
     * Sets the bit at the specified index
     * 
     * @param i the index treated as an unsigned int
     */
    public void set(int i) {
        if ((i & TOP_BIT) != 0) {
            top.set(i & UNSIGNED_BITS);
        } else {
            bottom.set(i);
        }
    }

    /**
     * Sets the bit at the specified index to false.
     * 
     * @param i the index treated as an unsigned int
     */
    public void clear(int i) {
        if ((i & TOP_BIT) != 0) {
            top.clear(i & UNSIGNED_BITS);
        } else {
            bottom.clear(i);
        }

    }

    /**
     * Increment the supplied index by one
     * 
     * @param i the index treated as an unsigned int
     * @return the next largest index value
     */
    public static int inc(int i) {
        long l = Integer.toUnsignedLong(i) + 1;
        return (int) (l & 0xFFFFFFFF);
    }

    /**
     * Returns the index of the first bit that is set to true that occurs on or after the specified starting index. If
     * no such it exists then-1 is returned.
     * 
     * @param i the index to start checking from (inclusive)
     * @return the index of the next set bit
     */
    public int nextSetBit(int i) {
        if ((i & TOP_BIT) != 0) {
            return top.nextSetBit(i & UNSIGNED_BITS) | TOP_BIT;
        } else {
            int next = bottom.nextSetBit(i);
            if (next == -1) {
                next = top.nextSetBit(0);
                if (next != -1) {
                    next = next | TOP_BIT;
                }
                return next;
            }
            return bottom.nextSetBit(i);
        }
    }

    /**
     * Returns the value of the bit with the specified index. The value is true if the bit with the index i is currently
     * set in this Set; otherwise, the result is false.
     * 
     * @param i the index treated as an unsigned int
     * @return the boolean value of the bit with the specified index.
     */
    public boolean get(int i) {
        if ((i & TOP_BIT) != 0) {
            return top.get(i & UNSIGNED_BITS);
        } else {
            return bottom.get(i);
        }
    }

    /**
     * Returns the number of bits set to true in this Set.
     * 
     * @return the number of bits set to true in this Set
     */
    public long cardinality() {
        return (long) bottom.cardinality() + (long) top.cardinality();
    }
}
