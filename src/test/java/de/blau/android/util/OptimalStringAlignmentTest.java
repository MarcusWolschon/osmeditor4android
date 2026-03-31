package de.blau.android.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OptimalStringAlignmentTest {

    /**
     * Identical strings should have zero edit distance
     */
    @Test
    public void identical() {
        assertEquals(0, OptimalStringAlignment.editDistance("abc", "abc", 10));
    }

    /**
     * Empty strings
     */
    @Test
    public void empty() {
        assertEquals(0, OptimalStringAlignment.editDistance("", "", 10));
        assertEquals(3, OptimalStringAlignment.editDistance("abc", "", 10));
        assertEquals(3, OptimalStringAlignment.editDistance("", "abc", 10));
    }

    /**
     * Single character substitution
     */
    @Test
    public void substitution() {
        assertEquals(1, OptimalStringAlignment.editDistance("cat", "bat", 10));
    }

    /**
     * Transposition should count as one edit
     */
    @Test
    public void transposition() {
        assertEquals(1, OptimalStringAlignment.editDistance("ca", "ac", 10));
        assertEquals(1, OptimalStringAlignment.editDistance("abc", "bac", 10));
    }

    /**
     * Insertion and deletion
     */
    @Test
    public void insertionAndDeletion() {
        assertEquals(1, OptimalStringAlignment.editDistance("abc", "ab", 10));
        assertEquals(1, OptimalStringAlignment.editDistance("ab", "abc", 10));
    }

    /**
     * Should return -1 when distance exceeds threshold
     */
    @Test
    public void threshold() {
        assertEquals(-1, OptimalStringAlignment.editDistance("abc", "xyz", 1));
        assertEquals(3, OptimalStringAlignment.editDistance("abc", "xyz", 3));
    }

    /**
     * Strings longer than the thread-local buffer size (64)
     */
    @Test
    public void longStrings() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append('a');
        }
        String a = sb.toString();
        String b = sb.substring(0, 99) + "b";
        assertEquals(1, OptimalStringAlignment.editDistance(a, b, 10));
    }
}
