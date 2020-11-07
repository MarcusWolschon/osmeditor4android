package de.blau.android.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.blau.android.presets.ValueWithCount;
import de.blau.android.util.StringWithDescription.LocaleComparator;

public class StringWithDescriptionTest {

    private static final String DESCRIPTION_OUTPUT = "test - description";
    private static final String DESCRIPTION        = "description";
    private static final String TEST               = "test";

    /**
     * Test the numerous constructors
     */
    @Test
    public void constructorTest() {
        StringWithDescription swd = new StringWithDescription(TEST);
        assertNull(swd.getDescription());
        assertEquals(TEST, swd.getValue());
        assertEquals(TEST, swd.toString());

        swd = new StringWithDescription(TEST, DESCRIPTION);
        assertEquals(DESCRIPTION, swd.getDescription());
        assertEquals(TEST, swd.getValue());
        assertEquals(DESCRIPTION_OUTPUT, swd.toString());

        StringWithDescription toCopy = new StringWithDescription(TEST, DESCRIPTION);
        swd = new StringWithDescription(toCopy);
        assertEquals(DESCRIPTION, swd.getDescription());
        assertEquals(TEST, swd.getValue());
        assertEquals(DESCRIPTION_OUTPUT, swd.toString());

        ValueWithCount vwc = new ValueWithCount(TEST, DESCRIPTION);
        swd = new StringWithDescription(vwc);
        assertEquals(DESCRIPTION, swd.getDescription());
        assertEquals(TEST, swd.getValue());
        assertEquals(DESCRIPTION_OUTPUT, swd.toString());
    }

    /**
     * Test comparing 
     */
    @Test
    public void compare() {
        StringWithDescription a = new StringWithDescription("A");
        StringWithDescription b = new StringWithDescription("B");
        assertTrue(a.equals("A"));
        assertFalse(a.equals("B"));
        StringWithDescription a2 = new StringWithDescription("A");
        assertTrue(a.equals(a2));
        assertFalse(a.equals(b));
        assertEquals(-1, a.compareTo(b));
        assertEquals(1, b.compareTo(a));
        assertEquals(0, a.compareTo(a));
        LocaleComparator comparator = new StringWithDescription.LocaleComparator();
        assertEquals(-1, comparator.compare(a, b));
        assertEquals(1, comparator.compare(b, a));
        assertEquals(0, comparator.compare(a, a));
        a.setDescription("test A");
        b.setDescription("test B");
        assertEquals(-1, comparator.compare(a, b));
        assertEquals(1, comparator.compare(b, a));
        assertEquals(0, comparator.compare(a, a));
        assertTrue(a.hashCode() != b.hashCode()); // not really that interesting
    }
}