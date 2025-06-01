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
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;
import org.xml.sax.SAXException;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.util.Base64;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.App;
import de.blau.android.JavaResources;
import de.blau.android.UnitTestUtils;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.osm.RelationUtilTest;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
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
@Config(sdk = 33)
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
        PresetItem restaurant = Preset.findBestMatch(presets, tags, null, null);
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
        assertTrue(restaurant.hasKeyValue("cuisine", "anything")); // that this works doesn't make sense
        assertTrue(restaurant.hasKeyValue("takeaway", "only"));
        assertTrue(restaurant.hasKeyValue("self_service", ""));

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
     * Test that we match the expected PresetItem
     */
    @Test
    public void matching2() {
        PresetItem item = presets[0].getItemByName("Traffic Sign Forward", null);
        assertNotNull(item);
        Map<String, String> tags = new HashMap<>();
        tags.put("traffic_sign:forward", "something"); // this is a text field
        assertEquals(1, item.matchesRecommended(tags, null));
        PresetItem sign = Preset.findBestMatch(presets, tags, null, null);
        assertEquals(item, sign);
        assertTrue(item.hasKeyValue("traffic_sign:forward", "anything")); // test field
    }

    /**
     * Test that we match a multipolygon properly
     */
    @Test
    public void matching3() {
        //
        Map<String, String> tags = new HashMap<>();
        tags.put(Tags.KEY_TYPE, Tags.VALUE_MULTIPOLYGON);
        tags.put(Tags.KEY_PLACE, "farm");
        PresetItem match = Preset.findBestMatch(presets, tags, null, ElementType.RELATION, false, null);
        assertEquals("Multipolygon", match.getName());
        match = Preset.findBestMatch(presets, tags, null, null, false, null);
        assertEquals("Farm", match.getName());
    }

    /**
     * Test that ignoring tags for matching works
     */
    @Test
    public void matching4() {
        Map<String, String> tags = new HashMap<>();
        tags.put(Tags.KEY_INDOOR, Tags.VALUE_ROOM);
        tags.put(Tags.VALUE_ROOM, Tags.KEY_SHOP);
        tags.put(Tags.KEY_SHOP, "supermarket");
        PresetItem match = Preset.findBestMatch(presets, tags, null, null);
        assertNotNull(match);
        assertTrue(match.hasKeyValue(Tags.KEY_INDOOR, Tags.VALUE_ROOM));
        Map<String, String> ignore = new HashMap<>();
        ignore.put(Tags.KEY_INDOOR, Tags.VALUE_ROOM);
        match = Preset.findBestMatch(presets, tags, null, ignore);
        assertNotNull(match);
        assertTrue(match.hasKeyValue(Tags.KEY_SHOP, "supermarket"));
    }

    /**
     * Test that we match a bicycle route relation
     */
    @Test
    public void matching5() {
        //
        Map<String, String> tags = new HashMap<>();
        tags.put(Tags.KEY_TYPE, Tags.VALUE_ROUTE);
        tags.put(Tags.VALUE_ROUTE, "bicycle");
        PresetItem match = Preset.findBestMatch(presets, tags, null, ElementType.RELATION, false, null);
        assertEquals("Bicycle Route", match.getName());
    }

    /**
     * Remove an item
     */
    @Test
    public void deleteItem() {
        PresetItem item = presets[0].getItemByName("Traffic Sign Backward", null);
        assertNotNull(item);
        Map<String, String> tags = new HashMap<>();
        tags.put("traffic_sign:backward", "something"); // this is a text field
        PresetItem sign = Preset.findBestMatch(presets, tags, null, null);
        assertEquals(item, sign);
        presets[0].deleteItem(item);
        assertNull(presets[0].getItemByName("Traffic Sign Backward", null));
        assertNull(Preset.findBestMatch(presets, tags, null, null));
    }

    /**
     * Load a preset with an SVG icon
     */
    @Test
    public void svgIcons() {
        //
        presets = getTestPreset("svg-test.zip");
        Preset p = getPresetWithDescription(presets, "Testing preset");
        assertNotNull(p);
        PresetItem item = p.getItemByName("Test Item", null);
        assertNotNull(item);
        assertNotNull(item.getIconpath());
        System.out.println(item.getIconpath());
        BitmapDrawable icon = item.getMapIcon(ApplicationProvider.getApplicationContext());
        assertNotNull(icon);
        Bitmap bitmap = icon.getBitmap();
        ByteBuffer dst = ByteBuffer.allocate(bitmap.getByteCount());
        bitmap.copyPixelsToBuffer(dst);
        try {
            assertEquals("5h9B1X2yCMX5KjXEznGYVwkko/yH7rqDRB/O7l1qKGU",
                    Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(dst.array()), Base64.NO_PADDING | Base64.NO_WRAP));
        } catch (NoSuchAlgorithmException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Find a preset with a specific description
     * 
     * @param presets array of Preset
     * @param description the description
     * @return the Preset
     */
    private Preset getPresetWithDescription(@NonNull Preset[] presets, @NonNull String description) {
        for (Preset preset : presets) {
            if (description.equals(preset.getShortDescription())) {
                return preset;
            }
        }
        fail("Preset with " + description + " not found");
        return null;
    }

    /**
     * Test that we don't match a preset without the object and that we do with.
     */
    @Test
    public void objectMatching() {
        presets = getTestPreset("test-preset.xml");
        Map<String, String> tags = new HashMap<>();
        tags.put("imaginary", "tag");
        PresetItem test = Preset.findBestMatch(presets, tags, null, null);
        assertNull(test);

        tags.clear();
        tags.put("imaginary2", "tag");
        test = Preset.findBestMatch(presets, tags, null, null);
        assertNotNull(test);
        assertEquals("Test Item 2", test.getName());

        // match via key
        tags.clear();
        tags.put("highway", "tag");
        test = Preset.findBestMatch(presets, tags, null, null);
        assertNotNull(test);
        assertEquals("Test Item 3", test.getName());

        // exception to top level key match
        tags.clear();
        tags.put("highway", "tag2");
        test = Preset.findBestMatch(presets, tags, null, null);
        assertNull(test);
    }

    /**
     * Add a test preset to current presets and return them
     * 
     * @param presetName file name of the test preset
     * @return
     */
    private Preset[] getTestPreset(@NonNull String presetName) {
        final Context applicationContext = ApplicationProvider.getApplicationContext();
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(applicationContext)) {
            File preset = JavaResources.copyFileFromResources(applicationContext, presetName, null, "/");
            String presetId = java.util.UUID.randomUUID().toString();
            db.addPreset(presetId, "Test preset", "", true);
            File presetDir = db.getPresetDirectory(presetId);
            presetDir.mkdir();
            Uri inputUri = Uri.parse(preset.toURI().toString());
            ContentResolver contentResolver = ApplicationProvider.getApplicationContext().getContentResolver();
            ShadowContentResolver shadowContentResolver = shadowOf(contentResolver);
            shadowContentResolver.registerInputStream(inputUri, new FileInputStream(preset));
            PresetLoader.load(applicationContext, inputUri, presetDir, presetName);
            return db.getCurrentPresetObject();
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
        return null;
    }

    /**
     * A random value on a top level tag shouldn't match
     */
    @Test
    public void noMatch() {
        Map<String, String> tags = new HashMap<>();
        tags.put("leisure", "123456789");
        PresetItem match = Preset.findBestMatch(presets, tags, null, null);
        assertNull(match);
    }

    /**
     * Deprecated items should not be in the search index
     */
    @Test
    public void deprecation() {
        PresetItem temp = presets[0].getItemByName("Farm", null, true);
        PresetElementPath landuseFarmPath = temp.getPath(presets[0].getRootGroup());
        assertNotNull(landuseFarmPath);
        PresetItem landuseFarm = (PresetItem) Preset.getElementByPath(presets[0].getRootGroup(), landuseFarmPath, null, true);
        assertNotNull(landuseFarm);
        assertTrue(landuseFarm.hasKeyValue(Tags.KEY_LANDUSE, "farm"));
        temp = presets[0].getItemByName("Farm", null, false);
        PresetElementPath placeFarmPath = temp.getPath(presets[0].getRootGroup());
        PresetItem placeFarm = (PresetItem) Preset.getElementByPath(presets[0].getRootGroup(), placeFarmPath, null, false);
        assertNotNull(placeFarm);
        assertTrue(placeFarm.hasKeyValue(Tags.KEY_PLACE, "farm"));
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
            Preset testPreset = new Preset(ApplicationProvider.getApplicationContext(), testPresetFile.getParentFile(), false);
            Map<String, String> tags = new HashMap<>();
            tags.put(Tags.KEY_HIGHWAY, Tags.VALUE_MOTORWAY_LINK);
            PresetItem us = Preset.findBestMatch(new Preset[] { testPreset }, tags, Arrays.asList("US"), null);
            assertNotNull(us);
            assertEquals("Motorway Link (US)", us.getName());
            PresetItem ch = Preset.findBestMatch(new Preset[] { testPreset }, tags, Arrays.asList("CH"), null);
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
        presets = getTestPreset("test-preset.xml");
        Preset testPreset = null;
        for (Preset p : presets) {
            if ("Testing preset".equals(p.getShortDescription())) {
                testPreset = p;
                break;
            }
        }
        assertNotNull(testPreset);
        PresetItem testItem = testPreset.getItemByName("Test Same Name", Arrays.asList("CH"));
        assertNotNull(testItem);
        assertTrue(testItem.hasKeyValue("samething", "is in CH"));
        PresetElementPath testPath = testItem.getPath(testPreset.getRootGroup());
        assertNotNull(testPath);
        PresetItem testItemByPath = (PresetItem) Preset.getElementByPath(testPreset.getRootGroup(), testPath, Arrays.asList("CH"), false);
        assertEquals(testItem, testItemByPath);

        testItem = testPreset.getItemByName("Test Same Name", Arrays.asList("DE"));
        assertNotNull(testItem);
        assertTrue(testItem.hasKeyValue("samething", "not in CH"));
        testPath = testItem.getPath(testPreset.getRootGroup());
        assertNotNull(testPath);
        testItemByPath = (PresetItem) Preset.getElementByPath(testPreset.getRootGroup(), testPath, Arrays.asList("DE"), false);
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

    /**
     * Check that translation contextes are maintained for combos in chunks
     */
    @Test
    public void chunkTranslationContext() {
        PresetItem p = presets[0].getItemByName("Primary", null);
        assertNotNull(p);
        PresetField field = p.getField("bus_bay");
        assertNotNull(field);
        assertEquals("bus_bay", field.getTextContext());
        assertEquals("bus_bay", ((PresetComboField) field).getValuesContext());
    }

    /**
     * Check that keys with language variants are grouped together
     */
    @Test
    public void i18nGrouping() {
        //
        Map<String, String> tags = new HashMap<>();
        tags.put("amenity", "restaurant");
        PresetItem restaurant = Preset.findBestMatch(presets, tags, null, null);
        assertEquals("Restaurant", restaurant.getName());
        LinkedHashMap<PresetField, String> map = new LinkedHashMap<>();
        for (PresetField field : restaurant.getFields().values()) {
            if ((field instanceof PresetTextField) && Tags.KEY_NAME.equals(((PresetTextField) field).getKey())) {
                map.put(field, "English");
            } else {
                map.put(field, "");
            }
        }
        map.put(new PresetTextField("name:de"), "Deutsch");
        int size = map.size();
        Util.groupI18nKeys(Tags.I18N_KEYS, map);
        Iterator<Entry<PresetField, String>> it = map.entrySet().iterator();
        boolean found = false;
        while (it.hasNext()) {
            Entry<PresetField, String> entry = it.next();
            PresetField field = entry.getKey();
            if ((field instanceof PresetTextField) && Tags.KEY_NAME.equals(((PresetTextField) field).getKey())) {
                if ("English".equals(entry.getValue())) {
                    found = true;
                    assertTrue("Deutsch".equals(it.next().getValue()));
                    break;
                }
            }
        }
        assertTrue(found);
        assertEquals(size, map.size());
    }

    /**
     * Test that label fields are added
     */
    @Test
    public void labels() {
        //
        Map<String, String> tags = new HashMap<>();
        tags.put("amenity", "charging_station");
        PresetItem charger = Preset.findBestMatch(presets, tags, null, null);
        assertEquals("Charging Station", charger.getName());
        boolean found = false;
        for (PresetField field : charger.getFields().values()) {
            if (field instanceof PresetLabelField && "Available Sockets/Cables:".equals(((PresetLabelField) field).getLabel())) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    /**
     * Match a traffic sign node on a way (match_expression=child highway=*)
     */
    @Test
    public void matchExpression1() {
        StorageDelegator d = UnitTestUtils.loadTestData(getClass(), "test2.osm");
        Node n = (Node) d.getOsmElement(Node.NAME, 633468436L);
        Map<String, String> tags = new HashMap<>();
        tags.put("traffic_sign", "stop");
        App.getLogic().setTags(null, n, tags);
        PresetItem sign = Preset.findBestMatch(ApplicationProvider.getApplicationContext(), presets, tags, null, n, false);
        assertEquals("Stop sign", sign.getName());
    }

    /**
     * Match a traffic sign node on a separate node (match_expression=-child highway=*)
     */
    @Test
    public void matchExpression2() {
        StorageDelegator d = UnitTestUtils.loadTestData(getClass(), "test2.osm");
        Node n = (Node) d.getOsmElement(Node.NAME, 101792984L);
        Map<String, String> tags = new HashMap<>();
        tags.put("traffic_sign", "stop");
        App.getLogic().setTags(null, n, tags);
        PresetItem sign = Preset.findBestMatch(ApplicationProvider.getApplicationContext(), presets, tags, null, n, false);
        assertEquals("Stop sign (separate)", sign.getName());
    }

    /**
     * Iteratively match until we only have address tags left
     */
    @Test
    public void matchAddresses() {
        StorageDelegator d = UnitTestUtils.loadTestData(getClass(), "test2.osm");
        Way w = (Way) d.getOsmElement(Way.NAME, 96291973L);
        Map<String, String> tags = new HashMap<>(w.getTags());
        PresetItem match = Preset.findBestMatch(ApplicationProvider.getApplicationContext(), presets, tags, null, null, true);
        assertTrue(match.hasKeyValue(Tags.KEY_AMENITY, "townhall"));
        for (String key : match.getFields().keySet()) {
            tags.remove(key);
        }
        match = Preset.findBestMatch(ApplicationProvider.getApplicationContext(), presets, tags, null, null, true);
        assertTrue(match.hasKey(Tags.KEY_BUILDING));
        for (String key : match.getFields().keySet()) {
            tags.remove(key);
        }
        match = Preset.findBestMatch(ApplicationProvider.getApplicationContext(), presets, tags, null, null, true);
        assertTrue(match.hasKey(Tags.KEY_ADDR_STREET));
    }

    /**
     * Check that a simple checkgroup is created properly
     */
    @Test
    public void checkgroup() {
        PresetItem item = presets[0].getItemByName("Weather Station", null);
        assertNotNull(item);
        PresetField field = getFieldByHint(item, "Instruments");
        assertTrue(field instanceof PresetCheckGroupField);
        PresetCheckField check = ((PresetCheckGroupField) field).getCheckField("weather:thermometer");
        assertNotNull(check);
        assertEquals("man_made", ((PresetCheckGroupField) field).getTextContext());
    }

    /**
     * Get a field with the hint field
     * 
     * @param item the PresetItem holding the field
     * @param hint the hint
     * @return a field or null
     */
    @Nullable
    private PresetField getFieldByHint(@NonNull PresetItem item, @NonNull String hint) {
        for (PresetField field : item.getFields().values()) {
            if (field instanceof PresetTagField && hint.equals(((PresetTagField) field).getHint())) {
                return field;
            }
        }
        return null;
    }
}
