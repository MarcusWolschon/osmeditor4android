package io.vespucci.resources;

import static io.vespucci.osm.DelegatorUtil.addWayToStorage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import io.vespucci.App;
import io.vespucci.JavaResources;
import io.vespucci.contract.Paths;
import io.vespucci.osm.Node;
import io.vespucci.osm.StorageDelegator;
import io.vespucci.osm.Tags;
import io.vespucci.osm.Way;
import io.vespucci.resources.DataStyle;
import io.vespucci.resources.DataStyle.FeatureStyle;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
@LargeTest
public class DataStyleTest {

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        DataStyle styles = App.getDataStyle(ApplicationProvider.getApplicationContext());
        styles.reset();
    }

    /**
     * Test matching for a building with and without a house number
     */
    @Test
    public void buildingTest() {
        DataStyle styles = App.getDataStyle(ApplicationProvider.getApplicationContext());
        styles.getStylesFromFiles(ApplicationProvider.getApplicationContext());
        final StorageDelegator delegator = App.getDelegator();
        Way w = addWayToStorage(delegator, true);
        Map<String, String> tags = new TreeMap<>();
        tags.put(Tags.KEY_BUILDING, Tags.VALUE_YES);
        delegator.setTags(w, tags);
        assertEquals(5, styles.getStyleList(ApplicationProvider.getApplicationContext()).length);
        assertEquals(styles.getBuiltinStyleName(), styles.getCurrent().getName());
        styles.getStyle(DataStyle.getBuiltinStyleName());
        styles.switchTo("Color Round Nodes");
        assertEquals("Color Round Nodes", styles.getCurrent().getName());
        FeatureStyle style = styles.matchStyle(w);
        assertEquals(((int) Long.parseLong("ffcc9999", 16)), style.getPaint().getColor());
        assertNull(style.getLabelKey());
        assertFalse(style.usePresetIcon());
        assertFalse(style.isArea());
        tags.put(Tags.KEY_ADDR_HOUSENUMBER, "111");
        delegator.setTags(w, tags);
        style = styles.matchStyle(w);
        assertEquals(Tags.KEY_ADDR_HOUSENUMBER, style.getLabelKey());
    }

    /**
     * Load a custom style besides the standard ones
     */
    @Test
    public void customStyle() {
        try {
            JavaResources.copyFileFromResources(ApplicationProvider.getApplicationContext(), "test-style.xml", null, "/" + Paths.DIRECTORY_PATH_STYLES);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        DataStyle styles = App.getDataStyle(ApplicationProvider.getApplicationContext());
        styles.getStylesFromFiles(ApplicationProvider.getApplicationContext());
        assertEquals(6, styles.getStyleList(ApplicationProvider.getApplicationContext()).length);
        // matching test

        final StorageDelegator delegator = App.getDelegator();
        Node tree = delegator.getFactory().createNodeWithNewId(0, 0);
        delegator.insertElementSafe(tree);
        Map<String, String> tags = new TreeMap<>();
        tags.put(Tags.KEY_NATURAL, "tree");
        delegator.setTags(tree, tags);
        styles.switchTo("Test Style");
        assertEquals("Test Style", styles.getCurrent().getName());
        FeatureStyle style = styles.matchStyle(tree);
        assertTrue(style.usePresetIcon());
        tags.put("height", "1");
        delegator.setTags(tree, tags);
        style = styles.matchStyle(tree);
        assertTrue(style.getIconPath().endsWith("tree_height.png"));
        tags.put("circumference", "2");
        delegator.setTags(tree, tags);
        style = styles.matchStyle(tree);
        assertTrue(style.getIconPath().endsWith("tree_height_circumference.png"));
        tags.put("diameter_crown", "3");
        delegator.setTags(tree, tags);
        style = styles.matchStyle(tree);
        assertTrue(style.getIconPath().endsWith("tree_all.png"));
    }
}
