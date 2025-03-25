package io.vespucci.util;

/**
 * 
 * Java 8 things that didn't make it in to Android
 *
 */
public final class IntegerUtil {
    
    /**
     * Private constructor to stop instantiation
     */
    private IntegerUtil() {
        // empty
    }
    /**
     * Convert the argument to an unsigned long
     * 
     * @param x an int
     * @return a long
     */
    public static long toUnsignedLong(int x) {
        return x & 0xffffffffL;
    }
}
