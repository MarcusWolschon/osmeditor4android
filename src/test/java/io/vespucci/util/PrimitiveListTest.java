package io.vespucci.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import io.vespucci.util.collections.FloatPrimitiveList;
import io.vespucci.util.collections.LongPrimitiveList;

public class PrimitiveListTest {

    /**
     * Test FloatPrimitiveList
     */
    @Test
    public void floatList() {
        FloatPrimitiveList list = new FloatPrimitiveList(5);
        for (int i = 0; i < 6; i++) {
            list.add(i);
        }
        for (int i = 0; i < 6; i++) {
            assertEquals(i, (int) list.get(i));
        }
        assertEquals(6, list.size());
        assertEquals(10, list.getArray().length);
        assertEquals(6, list.values().length);

        list.set(3, 111f);
        assertEquals(111f, list.get(3), 0.1f);

        try {
            list.get(6);
            fail("should throw exception");
        } catch (IndexOutOfBoundsException ex) {
            // expected
        }

        try {
            list.set(6, 222);
            fail("should throw exception");
        } catch (IndexOutOfBoundsException ex) {
            // expected
        }
        
        list.truncate(5);
        assertEquals(list.size(), 5);
    }
    
    /**
     * Test LongPrimitiveList
     */
    @Test
    public void longList() {
        LongPrimitiveList list = new LongPrimitiveList(5);
        for (int i = 0; i < 6; i++) {
            list.add(i);
        }
        for (int i = 0; i < 6; i++) {
            assertEquals(i, (int) list.get(i));
        }
        assertEquals(6, list.size());
        assertEquals(10, list.getArray().length);
        assertEquals(6, list.values().length);

        list.set(3, 111);
        assertEquals(111, list.get(3));

        assertTrue(list.contains(111));
        
        assertFalse(list.contains(222));
        
        try {
            list.get(6);
            fail("should throw exception");
        } catch (IndexOutOfBoundsException ex) {
            // expected
        }

        try {
            list.set(6, 222);
            fail("should throw exception");
        } catch (IndexOutOfBoundsException ex) {
            // expected
        }
        
        list.truncate(5);
        assertEquals(list.size(), 5);
    }
}