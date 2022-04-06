package de.blau.android.presets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.robolectric.Shadows.shadowOf;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowContentResolver;
import org.xml.sax.SAXException;

import android.content.ContentResolver;
import android.net.Uri;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.App;
import de.blau.android.JavaResources;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.osm.Tags;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.PresetLoader;
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
        App.newLogic();
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
        assertEquals("Restaurant", restaurant.getName());
        // Splitting
        List<String> values = new ArrayList<>();
        values.add("chinese;fondue");
        values.add("japenese,steak");
        List<String> result = Preset.splitValues(values, restaurant, "cuisine");
        assertEquals(3, result.size());
        assertTrue(result.contains("chinese"));
        assertTrue(result.contains("fondue"));
        assertTrue(result.contains("japenese,steak"));
        assertNull(Preset.splitValues(null, restaurant, "cuisine"));
        values.add(null);
        assertEquals(3, Preset.splitValues(values, restaurant, "cuisine").size());
        // lanes uses |
        PresetItem lanes = presets[0].getItemByName("Single direction roads", null);
        assertNotNull(lanes);
        values.clear();
        values.add("left|right");
        result = Preset.splitValues(values, lanes, "turn:lanes");
        assertEquals(2, result.size());
        assertTrue(result.contains("left"));
        assertTrue(result.contains("right"));
    }

    /**
     * Test that we don't match a preset without the object and that we do with.
     */
    @Test
    public void objectMatching() {
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(ApplicationProvider.getApplicationContext())) {
            File preset = JavaResources.copyFileFromResources(ApplicationProvider.getApplicationContext(), "test-preset.xml", null, "/");
            String presetId = java.util.UUID.randomUUID().toString();
            db.addPreset(presetId, "Test preset", "", true);
            File presetDir = db.getPresetDirectory(presetId);
            presetDir.mkdir();
            Uri inputUri = Uri.parse(preset.toURI().toString());
            ContentResolver contentResolver = ApplicationProvider.getApplicationContext().getContentResolver();
            ShadowContentResolver shadowContentResolver = shadowOf(contentResolver);
            shadowContentResolver.registerInputStream(inputUri, new FileInputStream(preset));
            PresetLoader.load(ApplicationProvider.getApplicationContext(), inputUri, presetDir, "test-preset.xml");
            App.resetPresets();
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
        //
        presets = App.getCurrentPresets(ApplicationProvider.getApplicationContext());
        Map<String, String> tags = new HashMap<>();
        tags.put("imaginary", "tag");
        PresetItem test = Preset.findBestMatch(presets, tags, null);
        assertNull(test);

        tags.clear();
        tags.put("imaginary2", "tag");
        test = Preset.findBestMatch(presets, tags, null);
        assertNotNull(test);
        assertEquals("Test Item 2", test.getName());

        // match via key
        tags.clear();
        tags.put("highway", "tag");
        test = Preset.findBestMatch(presets, tags, null);
        assertNotNull(test);
        assertEquals("Test Item 3", test.getName());

        // exception to top level key match
        tags.clear();
        tags.put("highway", "tag2");
        test = Preset.findBestMatch(presets, tags, null);
        assertNull(test);
    }

    /**
     * A random value on a top level tag shouldn't match
     */
    @Test
    public void noMatch() {
        Map<String, String> tags = new HashMap<>();
        tags.put("leisure", "123456789");
        PresetItem match = Preset.findBestMatch(presets, tags, null);
        assertNull(match);
    }

    /**
     * Deprecated items should not be in the search index
     */
    @Test
    public void deprecation() {
        PresetItem landuseFarm = presets[0].getItemByName("Farm (deprecated)", null);
        PresetItem placeFarm = presets[0].getItemByName("Farm", null);
        assertNotNull(landuseFarm);
        List<PresetElement> result = SearchIndexUtils.searchInPresets(ApplicationProvider.getApplicationContext(), "farm", ElementType.CLOSEDWAY, 2, 10, null);
        assertFalse(result.contains(landuseFarm));
        assertTrue(result.contains(placeFarm));
    }

    /**
     * Test including / excluding PresetItems based on country
     */
    @Test
    public void includeExcludeCountry() {
        PresetItem motorVehicleCH = presets[0].getItemByName("Motor vehicle (CH)", null);
        assertNotNull(motorVehicleCH);
        motorVehicleCH.setRegions("CH");
        PresetItem motorVehicle = presets[0].getItemByName("Motor vehicle", null);
        assertNotNull(motorVehicle);
        motorVehicle.setRegions("CH");
        motorVehicle.setExcludeRegions(true);
        List<PresetElement> result = SearchIndexUtils.searchInPresets(ApplicationProvider.getApplicationContext(), "motor vehicle", ElementType.WAY, 2, 10,
                null);
        assertTrue(result.contains(motorVehicleCH));
        assertTrue(result.contains(motorVehicle));
        result = SearchIndexUtils.searchInPresets(ApplicationProvider.getApplicationContext(), "motor vehicle", ElementType.WAY, 2, 10,
                de.blau.android.util.Util.wrapInList("CH"));
        assertTrue(result.contains(motorVehicleCH));
        assertFalse(result.contains(motorVehicle));
        result = SearchIndexUtils.searchInPresets(ApplicationProvider.getApplicationContext(), "motor vehicle", ElementType.WAY, 2, 10,
                de.blau.android.util.Util.wrapInList("DE"));
        assertFalse(result.contains(motorVehicleCH));
        assertTrue(result.contains(motorVehicle));
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
     * Test that we can find items with the same name that differ in which region they apply to
     */
    @Test
    public void sameName() {
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(ApplicationProvider.getApplicationContext())) {
            File preset = JavaResources.copyFileFromResources(ApplicationProvider.getApplicationContext(), "test-preset.xml", null, "/");
            String presetId = java.util.UUID.randomUUID().toString();
            db.addPreset(presetId, "Test preset", "", true);
            File presetDir = db.getPresetDirectory(presetId);
            presetDir.mkdir();
            Uri inputUri = Uri.parse(preset.toURI().toString());
            ContentResolver contentResolver = ApplicationProvider.getApplicationContext().getContentResolver();
            ShadowContentResolver shadowContentResolver = shadowOf(contentResolver);
            shadowContentResolver.registerInputStream(inputUri, new FileInputStream(preset));
            PresetLoader.load(ApplicationProvider.getApplicationContext(), inputUri, presetDir, "test-preset.xml");
            App.resetPresets();
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
        //
        presets = App.getCurrentPresets(ApplicationProvider.getApplicationContext());
        Preset testPreset = null;
        for (Preset p : presets) {
            if ("Testing preset".equals(p.getShortDescription())) {
                testPreset = p;
                break;
            }
        }
        assertNotNull(testPreset);
        PresetItem testItem = testPreset.getItemByName("Test Same Name", "CH");
        assertNotNull(testItem);
        assertTrue(testItem.hasKeyValue("samething", "is in CH"));
        PresetElementPath testPath = testItem.getPath(testPreset.getRootGroup());
        assertNotNull(testPath);
        PresetItem testItemByPath = (PresetItem) Preset.getElementByPath(testPreset.getRootGroup(), testPath, "CH");
        assertEquals(testItem, testItemByPath);

        testItem = testPreset.getItemByName("Test Same Name", "DE");
        assertNotNull(testItem);
        assertTrue(testItem.hasKeyValue("samething", "not in CH"));
        testPath = testItem.getPath(testPreset.getRootGroup());
        assertNotNull(testPath);
        testItemByPath = (PresetItem) Preset.getElementByPath(testPreset.getRootGroup(), testPath, "DE");
        assertEquals(testItem, testItemByPath);
    }

    /**
     * Optional items should be that
     */
    @Test
    public void optional() {
        PresetItem path = presets[0].getItemByName("Path", null);
        assertNotNull(path);
        // name is in a chunk that is loaded in an optional section
        assertFalse(path.hasKey(Tags.KEY_NAME, false));
        assertTrue(path.hasKey(Tags.KEY_NAME, true));
        // mtb:scale is directly in an optional section
        assertFalse(path.hasKey("mtb:scale", false));
        assertTrue(path.hasKey("mtb:scale", true));
    }
}
