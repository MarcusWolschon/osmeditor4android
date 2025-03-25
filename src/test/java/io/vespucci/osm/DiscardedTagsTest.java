package io.vespucci.osm;

import static io.vespucci.osm.DelegatorUtil.toE7;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import io.vespucci.osm.DiscardedTags;
import io.vespucci.osm.Node;
import io.vespucci.osm.OsmElementFactory;
import io.vespucci.osm.StorageDelegator;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=33)
@LargeTest
public class DiscardedTagsTest {

    @Test
    public void removeDiscarded() {
        DiscardedTags toDiscard = new DiscardedTags(ApplicationProvider.getApplicationContext());

        OsmElementFactory factory = (new StorageDelegator()).getFactory();
        Node n = factory.createNodeWithNewId(toE7(51.476), toE7(0.006));

        Map<String, String> tags = new HashMap<>();
        tags.put("osmarender:rendernames", "test");
        tags.put("name", "test");
        n.setTags(tags);
        toDiscard.remove(n);
        assertFalse(n.hasTag("osmarender:rendernames", "test"));
        assertTrue(n.hasTag("name", "test"));
    }

    @Test
    public void onlyDiscarded() {
        DiscardedTags toDiscard = new DiscardedTags(ApplicationProvider.getApplicationContext());

        OsmElementFactory factory = (new StorageDelegator()).getFactory();
        Node n = factory.createNodeWithNewId(toE7(51.476), toE7(0.006));

        Map<String, String> tags = new HashMap<>();
        tags.put("osmarender:rendernames", "test");

        n.setTags(tags);
        assertTrue(toDiscard.only(n));

        tags.put("name", "test");
        n.setTags(tags);
        assertFalse(toDiscard.only(n));
    }
}
