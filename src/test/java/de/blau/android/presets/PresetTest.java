package de.blau.android.presets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.xml.sax.SAXException;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.App;
import de.blau.android.JavaResources;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.osm.Tags;
import de.blau.android.util.SearchIndexUtils;

/**
 * NOTE These tests assumes the default preset is at position 0
 * 
 * @author simon
 *
 */
@RunWith(RobolectricTestRunner.class)
@LargeTest
public class PresetTest {

    Preset[] presets;

    /**
     * Before test setup
     */
    @Before
    public void setup() {
        presets = App.getCurrentPresets(ApplicationProvider.getApplicationContext());
    }

    /**
     * Test that we match the expected PresetItem
     */
    @Test
    public void matching() {
        //
        Map<String, String> tags = new HashMap<>();
        tags.put("amenity", "restaurant");
        PresetItem restaurant = Preset.findBestMatch(presets, tags, null);
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
        PresetItem match = Preset.findBestMatch(presets, tags, null);
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
        List<PresetElement> result = SearchIndexUtils.searchInPresets(ApplicationProvider.getApplicationContext(), "farm", ElementType.CLOSEDWAY, 2, 10, null);
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
        List<PresetElement> result = SearchIndexUtils.searchInPresets(ApplicationProvider.getApplicationContext(), "motor vehicle", ElementType.WAY, 2, 10,
                null);
        Assert.assertTrue(result.contains(motorVehicleCH));
        Assert.assertTrue(result.contains(motorVehicle));
        result = SearchIndexUtils.searchInPresets(ApplicationProvider.getApplicationContext(), "motor vehicle", ElementType.WAY, 2, 10,
                de.blau.android.util.Util.wrapInList("CH"));
        Assert.assertTrue(result.contains(motorVehicleCH));
        Assert.assertFalse(result.contains(motorVehicle));
        result = SearchIndexUtils.searchInPresets(ApplicationProvider.getApplicationContext(), "motor vehicle", ElementType.WAY, 2, 10,
                de.blau.android.util.Util.wrapInList("DE"));
        Assert.assertFalse(result.contains(motorVehicleCH));
        Assert.assertTrue(result.contains(motorVehicle));
    }

    /**
     * Matching test including / excluding PresetItems based on country
     */
    @Test
    public void includeExcludeCountryMatching() {
        try {
            File testPresetFile = JavaResources.copyFileFromResources(ApplicationProvider.getApplicationContext(), "test_preset1.xml", null, "test_preset");
            Preset testPreset = new Preset(ApplicationProvider.getApplicationContext(), testPresetFile.getParentFile(), null, false);
            Map<String, String> tags = new HashMap<>();
            tags.put(Tags.KEY_HIGHWAY, Tags.VALUE_MOTORWAY_LINK);
            PresetItem us = Preset.findBestMatch(new Preset[] { testPreset }, tags, "US");
            assertNotNull(us);
            assertEquals("Motorway Link (US)", us.getName());
            PresetItem ch = Preset.findBestMatch(new Preset[] { testPreset }, tags, "CH");
            assertNotNull(ch);
            assertEquals("Motorway Link", ch.getName());
        } catch (IOException | NoSuchAlgorithmException | ParserConfigurationException | SAXException e) {
            fail(e.getMessage());
        }
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
