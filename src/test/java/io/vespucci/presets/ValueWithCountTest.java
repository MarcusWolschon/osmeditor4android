package io.vespucci.presets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.vespucci.presets.ValueWithCount;

public class ValueWithCountTest {

    private static final String DESCRIPTION_OUTPUT = "test - description";
    private static final String DESCRIPTION        = "description";
    private static final String TEST               = "test";

    /**
     * Test the numerous constructors
     */
    @Test
    public void constructorTest() {
        ValueWithCount vwc = new ValueWithCount(TEST);
        assertNull(vwc.getDescription());
        assertEquals(TEST, vwc.getValue());
        assertEquals(TEST, vwc.toString());

        vwc = new ValueWithCount(TEST, DESCRIPTION);
        assertEquals(DESCRIPTION, vwc.getDescription());
        assertEquals(TEST, vwc.getValue());
        assertEquals(DESCRIPTION_OUTPUT, vwc.toString());

        vwc = new ValueWithCount(TEST, DESCRIPTION, true);
        assertEquals(DESCRIPTION, vwc.getDescription());
        assertEquals(DESCRIPTION, vwc.toString());

        vwc = new ValueWithCount(TEST, 111);
        assertNull(vwc.getDescription());
        assertEquals(TEST, vwc.getValue());
        assertEquals("test (111)", vwc.toString());
    }

    /**
     * Test comparing
     */
    @Test
    public void compare() {
        ValueWithCount a = new ValueWithCount("A");
        ValueWithCount b = new ValueWithCount("B");
        ValueWithCount a2 = new ValueWithCount("A");
        ValueWithCount a3 = new ValueWithCount("A", 4);
        ValueWithCount a4 = new ValueWithCount("A", 5);
        assertTrue(a.equals(a2)); // NOSONAR
        assertFalse(a.equals(b)); // NOSONAR
        assertEquals(-1, a.compareTo(b));
        assertEquals(1, b.compareTo(a));
        assertEquals(0, a.compareTo(a));
        assertEquals(1, a3.compareTo(a4));
        assertEquals(-1, a4.compareTo(a3));
        assertEquals(0, a4.compareTo(a4));
        assertNotEquals(a.hashCode(), b.hashCode()); // not really that interesting
    }
}