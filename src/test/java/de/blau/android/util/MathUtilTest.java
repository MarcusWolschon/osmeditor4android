package de.blau.android.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

    /**
     * Test mod with positive arguments
     */
    @Test
    public void testModPositive() {
        assertEquals("5 mod 3 should be 2", 2, MathUtil.mod(5, 3));
        assertEquals("7 mod 4 should be 3", 3, MathUtil.mod(7, 4));
        assertEquals("10 mod 10 should be 0", 0, MathUtil.mod(10, 10));
    }

    /**
     * Test mod with negative dividend
     */
    @Test
    public void testModNegativeDividend() {
        assertEquals("-1 mod 5 should be 4", 4, MathUtil.mod(-1, 5));
        assertEquals("-2 mod 5 should be 3", 3, MathUtil.mod(-2, 5));
        assertEquals("-5 mod 3 should be 1", 1, MathUtil.mod(-5, 3));
    }

    /**
     * Test mod with zero dividend
     */
    @Test
    public void testModZeroDividend() {
        assertEquals("0 mod 5 should be 0", 0, MathUtil.mod(0, 5));
        assertEquals("0 mod 1 should be 0", 0, MathUtil.mod(0, 1));
    }

    /**
     * Test mod with modulus of 1
     */
    @Test
    public void testModModulusOne() {
        assertEquals("5 mod 1 should be 0", 0, MathUtil.mod(5, 1));
        assertEquals("-3 mod 1 should be 0", 0, MathUtil.mod(-3, 1));
    }

    /**
     * Test mod wrapping around for way node indices This is the primary use case in the PR: cycling through way nodes
     */
    @Test
    public void testModForWayNodeCycling() {
        // 5 nodes in a way: indices 0-4
        int wayNodeCount = 5;

        // Forward cycling
        assertEquals("Index 0 mod 5 should be 0", 0, MathUtil.mod(0, wayNodeCount));
        assertEquals("Index 4 mod 5 should be 4", 4, MathUtil.mod(4, wayNodeCount));
        assertEquals("Index 5 mod 5 should be 0 (wrap around)", 0, MathUtil.mod(5, wayNodeCount));

        // Backward cycling (negative indices)
        assertEquals("Index -1 mod 5 should be 4", 4, MathUtil.mod(-1, wayNodeCount));
        assertEquals("Index -2 mod 5 should be 3", 3, MathUtil.mod(-2, wayNodeCount));
        assertEquals("Index -5 mod 5 should be 0", 0, MathUtil.mod(-5, wayNodeCount));
        assertEquals("Index -6 mod 5 should be 4", 4, MathUtil.mod(-6, wayNodeCount));
    }

    /**
     * Test mod with larger values
     */
    @Test
    public void testModLargeValues() {
        assertEquals("100 mod 7 should be 2", 2, MathUtil.mod(100, 7));
        assertEquals("1000 mod 13 should be 12", 12, MathUtil.mod(1000, 13));
        assertEquals("-100 mod 7 should be 5", 5, MathUtil.mod(-100, 7));
    }

    /**
     * Test that mod always returns non-negative results
     */
    @Test
    public void testModAlwaysNonNegative() {
        for (int i = -20; i <= 20; i++) {
            int result = MathUtil.mod(i, 10);
            assertTrue("Result should be non-negative for mod(" + i + ", 10)", result >= 0);
            assertTrue("Result should be less than modulus for mod(" + i + ", 10)", result < 10);
        }
    }
}
