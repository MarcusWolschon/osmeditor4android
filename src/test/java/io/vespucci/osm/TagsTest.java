package io.vespucci.osm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.vespucci.osm.Tags;

public class TagsTest {

    /**
     * Test that name keys are recognized
     */
    @Test
    public void nameKeys() {
        assertTrue(Tags.isLikeAName(Tags.KEY_NAME));
        assertTrue(Tags.isLikeAName(Tags.KEY_ADDR_STREET));
        assertTrue(Tags.isLikeAName(Tags.KEY_NAME + ":test"));
        assertFalse(Tags.isLikeAName(Tags.KEY_ACCESS));
    }
}
