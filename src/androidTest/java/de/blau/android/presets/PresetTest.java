package de.blau.android.presets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.TestUtils;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.osm.Tags;
import de.blau.android.presets.Preset.PresetElement;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.util.SearchIndexUtils;

/**
 * This is just a convenient way of generating the default preset dump
 * 
 * @author simon
 *
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class PresetTest {

    Main     main;
    Preset[] presets;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Before test setup
     */
    @Before
    public void setup() {
        main = (Main) mActivityRule.getActivity();
        presets = App.getCurrentPresets(main);
        TestUtils.grantPermissons();
        TestUtils.dismissStartUpDialogs(main);
    }

    /**
     * Test that we match the expected PresetItem
     */
    @Test
    public void matching() {
        //
        HashMap<String, String> tags = new HashMap<String, String>();
        tags.put("amenity", "restaurant");
        PresetItem restaurant = Preset.findBestMatch(presets, tags);
        Assert.assertEquals("Restaurant", restaurant.getName());
        // Splitting
        ArrayList<String> values = new ArrayList<String>();
        values.add("chinese;fondue");
        values.add("japenese,steak");
        ArrayList<String> result = Preset.splitValues(values, restaurant, "cuisine");
        Assert.assertEquals(3, result.size());
        Assert.assertTrue(result.contains("chinese"));
        Assert.assertTrue(result.contains("fondue"));
        Assert.assertTrue(result.contains("japenese,steak"));
        Assert.assertNull(Preset.splitValues(null, restaurant, "cuisine"));
        values.add(null);
        Assert.assertEquals(3, Preset.splitValues(values, restaurant, "cuisine").size());
        // lanes uses |
        PresetItem lanes = presets[0].getItemByName("Single direction roads");
        Assert.assertNotNull(lanes);
        values.clear();
        values.add("left|right");
        result = Preset.splitValues(values, lanes, "turn:lanes");
        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.contains("left"));
        Assert.assertTrue(result.contains("right"));
    }

    /**
     * Deprecated items should not be in the search index
     */
    @Test
    public void deprecation() {
        PresetItem landuseFarm = presets[0].getItemByName("Farm (deprecated)");
        PresetItem placeFarm = presets[0].getItemByName("Farm");
        Assert.assertNotNull(landuseFarm);
        List<PresetElement> result = SearchIndexUtils.searchInPresets(main, "farm", ElementType.CLOSEDWAY, 2, 10);
        Assert.assertFalse(result.contains(landuseFarm));
        Assert.assertTrue(result.contains(placeFarm));
    }
    
    /**
     * Optional items should be that
     */
    @Test
    public void optional() {
        PresetItem path = presets[0].getItemByName("Path");
        Assert.assertNotNull(path);
        // name is in a chunk that is loaded in an optional section
        Assert.assertFalse(path.hasKey(Tags.KEY_NAME, false));
        Assert.assertTrue(path.hasKey(Tags.KEY_NAME, true));
        // mtb:scale is directly in an optional section
        Assert.assertFalse(path.hasKey("mtb:scale", false));
        Assert.assertTrue(path.hasKey("mtb:scale", true)); 
    }
}
