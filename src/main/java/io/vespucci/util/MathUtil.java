package io.vespucci.util;

/**
 * Android doesn't have floor mode before API 24
 */
public final class MathUtil {

    /**
     * Private constructor
     */
    private MathUtil() {
        // empty
    }
    
    /**
     * Floor modulus 
     * 
     * See Math.floorMod
     * 
     * @param x dividend 
     * @param y dividor
     * @return the floor modulus of x and y
     */
    public static int floorMod(int x, int y) {
        int r = x / y;
        if ((x ^ y) < 0 && (r * y != x)) {
            r--;
        }
        return x - r * y;
    }    
}
