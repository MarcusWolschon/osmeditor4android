package de.blau.android.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MathUtilTest {

    /**
     * Test floor modulus with positive values
     */
    @Test
    public void positiveValues() {
        assertEquals(1, MathUtil.floorMod(7, 3));
        assertEquals(0, MathUtil.floorMod(6, 3));
    }

    /**
     * Test floor modulus with negative dividend
     */
    @Test
    public void negativeDividend() {
        assertEquals(2, MathUtil.floorMod(-1, 3));
        assertEquals(1, MathUtil.floorMod(-2, 3));
    }

    /**
     * Test floor modulus with negative divisor
     */
    @Test
    public void negativeDivisor() {
        assertEquals(-2, MathUtil.floorMod(1, -3));
    }

    /**
     * Test zero dividend
     */
    @Test
    public void zeroDividend() {
        assertEquals(0, MathUtil.floorMod(0, 5));
    }
}
