package de.blau.android.presets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.TestUtils;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.osm.Tags;
import de.blau.android.presets.Preset.PresetElement;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.util.SearchIndexUtils;

/**
 * NOTE These tests assumes the default test is at position 0
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
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        main = mActivityRule.getActivity();
        presets = App.getCurrentPresets(main);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
    }

    /**
     * Test that we match the expected PresetItem
     */
    @Test
    public void matching() {
        //
        Map<String, String> tags = new HashMap<>();
        tags.put("amenity", "restaurant");
        PresetItem restaurant = Preset.findBestMatch(presets, tags);
        Assert.assertEquals("Restaurant", restaurant.getName());
        // Splitting
        List<String> values = new ArrayList<>();
        values.add("chinese;fondue");
        values.add("japenese,steak");
        List<String> result = Preset.splitValues(values, restaurant, "cuisine");
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
     * A random value on a top level tag shouldn't match
     */
    @Test
    public void noMatch() {
        Map<String, String> tags = new HashMap<>();
        tags.put("leisure", "123456789");
        PresetItem match = Preset.findBestMatch(presets, tags);
        Assert.assertNull(match);
    }
    
    /**
     * Deprecated items should not be in the search index
     */
    @Test
    public void deprecation() {
        PresetItem landuseFarm = presets[0].getItemByName("Farm (deprecated)");
        PresetItem placeFarm = presets[0].getItemByName("Farm");
        Assert.assertNotNull(landuseFarm);
        List<PresetElement> result = SearchIndexUtils.searchInPresets(main, "farm", ElementType.CLOSEDWAY, 2, 10, null);
        Assert.assertFalse(result.contains(landuseFarm));
        Assert.assertTrue(result.contains(placeFarm));
    }

    /**
     * Test including / excluding PresetItems based on country
     */
    @Test
    public void includeExcludeCountry() {
        PresetItem motorVehicleCH = presets[0].getItemByName("Motor vehicle (CH)");
        Assert.assertNotNull(motorVehicleCH);
        motorVehicleCH.setRegions("CH");
        PresetItem motorVehicle = presets[0].getItemByName("Motor vehicle");
        Assert.assertNotNull(motorVehicle);
        motorVehicle.setRegions("CH");
        motorVehicle.setExcludeRegions(true);
        List<PresetElement> result = SearchIndexUtils.searchInPresets(main, "motor vehicle", ElementType.WAY, 2, 10, null);
        Assert.assertTrue(result.contains(motorVehicleCH));
        Assert.assertTrue(result.contains(motorVehicle));
        result = SearchIndexUtils.searchInPresets(main, "motor vehicle", ElementType.WAY, 2, 10, de.blau.android.util.Util.wrapInList("CH"));
        Assert.assertTrue(result.contains(motorVehicleCH));
        Assert.assertFalse(result.contains(motorVehicle));
        result = SearchIndexUtils.searchInPresets(main, "motor vehicle", ElementType.WAY, 2, 10, de.blau.android.util.Util.wrapInList("DE"));
        Assert.assertFalse(result.contains(motorVehicleCH));
        Assert.assertTrue(result.contains(motorVehicle));
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
