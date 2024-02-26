package de.blau.android.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class ReverseTest {

    /**
     * Test that way reversing has the intended effects
     */
    @Test
    public void reverse() {
        reverseTag("direction","up","down");
        reverseTag("direction","down","up");
        reverseTag("direction","NW","SE");
        reverseTag("direction","45°","225°");
        reverseTag("direction","45","225");
        reverseTag("direction","forward","backward");
        reverseTag("direction","backward","forward");
        reverseTag("incline","up","down");
        reverseTag("incline","down","up");
        reverseTag("incline","10°","-10°");
        reverseTag("incline","-10°","10°");
        reverseTag("oneway","yes","-1");
        reverseTag("conveying","forward","backward");
        reverseTag("conveying","backward","forward");
        reverseTag("priority","forward","backward");
        reverseTag("priority","backward","forward");
    }

    /**
     * Test that way reversing has the intended effects
     */
    @Test
    public void reverseSide() {
        Node e = OsmElementFactory.createNode(-1L, 1L, System.currentTimeMillis() / 1000, OsmElement.STATE_CREATED, 0, 0);
        Map<String, String> tags = new HashMap<>();
        tags.put("traffic_sign", "stop");
        tags.put(Tags.KEY_SIDE, Tags.VALUE_RIGHT);
        e.setTags(tags);
        Map<String, String> dirTags = Reverse.getDirectionDependentTags(e);
        assertTrue(dirTags.containsKey(Tags.KEY_SIDE));
        Reverse.reverseDirectionDependentTags(e, dirTags, true);
        assertTrue(e.hasTagWithValue(Tags.KEY_SIDE, Tags.VALUE_LEFT));
        //
        tags = new HashMap<>();
        tags.put(Tags.KEY_HIGHWAY, Tags.VALUE_CYCLIST_WAITING_AID);
        tags.put(Tags.KEY_SIDE, Tags.VALUE_RIGHT);
        e.setTags(tags);
        dirTags = Reverse.getDirectionDependentTags(e);
        assertFalse(dirTags.containsKey(Tags.KEY_SIDE));
    }
    
    /**
     * Get the tag value changed by assuming that the way was reversed 
     * 
     * @param key the key
     * @param value the original value
     * @param result what the value after reversing should be
     */
    private void reverseTag(String key, String value, String result) {
        Way e = OsmElementFactory.createWay(-1L, 1L, System.currentTimeMillis() / 1000, OsmElement.STATE_CREATED);
        // don't bother added way nodes for now
        
        Map<String, String> tags = new HashMap<>();
        tags.put(key, value);
        
        e.setTags(tags);
        Map<String, String> dirTags = Reverse.getDirectionDependentTags(e);
        assertTrue(dirTags.containsKey(key));
        assertEquals(value,dirTags.get(key));
        Reverse.reverseDirectionDependentTags(e, dirTags, true);
        assertTrue(e.hasTagWithValue(key, result));
    }
}