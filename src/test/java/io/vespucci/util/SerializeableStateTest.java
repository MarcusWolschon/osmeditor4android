package io.vespucci.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;

import io.vespucci.util.SerializableState;

public class SerializeableStateTest {

    /**
     * Check that we correctly handle Serializable objects
     */
    @Test
    public void serializableTest() {
        SerializableState state = new SerializableState();
        Serializable s = new HashMap<String, String>();
        state.putSerializable("1", s);
        assertEquals(s, state.getSerializable("1"));
        assertNull(state.getSerializable("2"));
        state.putLong("2", 1L);
        assertNull(state.getList("2"));
    }

    /**
     * Check that we correctly handle Long objects
     */
    @Test
    public void longTest() {
        SerializableState state = new SerializableState();
        Long l = Long.valueOf(1L);
        state.putLong("1", 1L);
        assertEquals(l, state.getLong("1"));
        assertNull(state.getSerializable("2"));
        state.putSerializable("2", "test");
        assertNull(state.getLong("2"));
    }

    /**
     * Check that we correctly handle List objects
     */
    @Test
    public void listTest() {
        SerializableState state = new SerializableState();
        List<String> l = new ArrayList<>();
        state.putList("1", l);
        assertEquals(l, state.getList("1"));
        assertNull(state.getList("2"));
        state.putLong("2", 1L);
        assertNull(state.getList("2"));
    }
}