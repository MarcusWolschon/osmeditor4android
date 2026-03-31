package de.blau.android.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class IntegerUtilTest {

    /**
     * Test unsigned long conversion
     */
    @Test
    public void toUnsignedLong() {
        assertEquals(0L, IntegerUtil.toUnsignedLong(0));
        assertEquals(42L, IntegerUtil.toUnsignedLong(42));
        assertEquals(2147483647L, IntegerUtil.toUnsignedLong(Integer.MAX_VALUE));
        assertEquals(4294967295L, IntegerUtil.toUnsignedLong(-1));
        assertEquals(2147483648L, IntegerUtil.toUnsignedLong(Integer.MIN_VALUE));
    }
}
