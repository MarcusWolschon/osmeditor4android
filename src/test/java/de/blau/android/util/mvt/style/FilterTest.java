package de.blau.android.util.mvt.style;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.google.gson.JsonArray;
import com.mapbox.geojson.Point;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.App;
import de.blau.android.resources.DataStyleManager;
import de.blau.android.util.mvt.VectorTileDecoder;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
@LargeTest
public class FilterTest {

    /**
     * Pre test setup
     */
    @Before
    public void setup() {
        // default values are currently taken from the data style
        DataStyleManager styles = App.getDataStyleManager(ApplicationProvider.getApplicationContext());
        styles.getStylesFromFiles(ApplicationProvider.getApplicationContext());
    }

    /**
     * Test eq filter
     */
    @Test
    public void eqTest() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("s1", "string");
        attributes.put("i1", 111);
        Point point = Point.fromLngLat(0, 0);
        VectorTileDecoder.Feature feature = new VectorTileDecoder.Feature("test", 256, point, attributes, -1);
        Symbol symbol = new Symbol("test");
        JsonArray array = new JsonArray();
        array.add("==");
        array.add("s1");
        array.add("string");
        assertTrue(symbol.evaluateFilter(array, feature));
        array = new JsonArray();
        array.add("==");
        array.add("s2");
        array.add("string");
        assertFalse(symbol.evaluateFilter(array, feature));
        array = new JsonArray();
        array.add("==");
        array.add("s1");
        array.add("string2");
        assertFalse(symbol.evaluateFilter(array, feature));
        array = new JsonArray();
        array.add("==");
        array.add("i1");
        array.add(111);
        assertTrue(symbol.evaluateFilter(array, feature));
        array = new JsonArray();
        array.add("==");
        array.add("i2");
        array.add(111);
        assertFalse(symbol.evaluateFilter(array, feature));
        array = new JsonArray();
        array.add("==");
        array.add("i1");
        array.add(112);
        assertFalse(symbol.evaluateFilter(array, feature));
    }

    /**
     * Test neq filter
     */
    @Test
    public void neqTest() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("s1", "string");
        attributes.put("i1", 111);
        Point point = Point.fromLngLat(0, 0);
        VectorTileDecoder.Feature feature = new VectorTileDecoder.Feature("test", 256, point, attributes, -1);
        Symbol symbol = new Symbol("test");
        JsonArray array = new JsonArray();
        array.add("!=");
        array.add("s1");
        array.add("string");
        assertFalse(symbol.evaluateFilter(array, feature));
        array = new JsonArray();
        array.add("!=");
        array.add("s2");
        array.add("string");
        assertTrue(symbol.evaluateFilter(array, feature)); // s2 doesn't exist
        array = new JsonArray();
        array.add("!=");
        array.add("s1");
        array.add("string2");
        assertTrue(symbol.evaluateFilter(array, feature));
        array = new JsonArray();
        array.add("!=");
        array.add("i1");
        array.add(111);
        assertFalse(symbol.evaluateFilter(array, feature));
        array = new JsonArray();
        array.add("!=");
        array.add("i2");
        array.add(111);
        assertTrue(symbol.evaluateFilter(array, feature)); // s2 doesn't exist
        array = new JsonArray();
        array.add("!=");
        array.add("i1");
        array.add(112);
        assertTrue(symbol.evaluateFilter(array, feature));
    }

    /**
     * Test lt filter (test just covers numbers
     */
    @Test
    public void ltTest() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("i1", 111);
        Point point = Point.fromLngLat(0, 0);
        VectorTileDecoder.Feature feature = new VectorTileDecoder.Feature("test", 256, point, attributes, -1);
        Symbol symbol = new Symbol("test");
        JsonArray array = new JsonArray();
        array.add("<");
        array.add("i1");
        array.add(111);
        assertFalse(symbol.evaluateFilter(array, feature));
        array = new JsonArray();
        array.add("<");
        array.add("i2");
        array.add(111);
        assertFalse(symbol.evaluateFilter(array, feature)); // s2 doesn't exist interpretation unclear
        array = new JsonArray();
        array.add("<");
        array.add("i1");
        array.add(112);
        assertTrue(symbol.evaluateFilter(array, feature));
    }

    /**
     * Test gt filter (test just covers numbers
     */
    @Test
    public void gtTest() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("i1", 111);
        Point point = Point.fromLngLat(0, 0);
        VectorTileDecoder.Feature feature = new VectorTileDecoder.Feature("test", 256, point, attributes, -1);
        Symbol symbol = new Symbol("test");
        JsonArray array = new JsonArray();
        array.add(">");
        array.add("i1");
        array.add(111);
        assertFalse(symbol.evaluateFilter(array, feature));
        array = new JsonArray();
        array.add(">");
        array.add("i2");
        array.add(111);
        assertFalse(symbol.evaluateFilter(array, feature)); // s2 doesn't exist, interpretation unclear
        array = new JsonArray();
        array.add(">");
        array.add("i1");
        array.add(110);
        assertTrue(symbol.evaluateFilter(array, feature));
    }

    /**
     * Test in filter
     */
    @Test
    public void inTest() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("s1", "string");
        Point point = Point.fromLngLat(0, 0);
        VectorTileDecoder.Feature feature = new VectorTileDecoder.Feature("test", 256, point, attributes, -1);
        Symbol symbol = new Symbol("test");
        JsonArray array = new JsonArray();
        array.add("in");
        array.add("s1");
        array.add("string");
        array.add("string2");
        assertTrue(symbol.evaluateFilter(array, feature));
        array = new JsonArray();
        array.add("in");
        array.add("s1");
        array.add("string2");
        array.add("string3");
        assertFalse(symbol.evaluateFilter(array, feature));
    }

    /**
     * Test !in filter
     */
    @Test
    public void notInTest() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("s1", "string");
        Point point = Point.fromLngLat(0, 0);
        VectorTileDecoder.Feature feature = new VectorTileDecoder.Feature("test", 256, point, attributes, -1);
        Symbol symbol = new Symbol("test");
        JsonArray array = new JsonArray();
        array.add("!in");
        array.add("s1");
        array.add("string");
        array.add("string2");
        assertFalse(symbol.evaluateFilter(array, feature));
        array = new JsonArray();
        array.add("!in");
        array.add("s1");
        array.add("string2");
        array.add("string3");
        assertTrue(symbol.evaluateFilter(array, feature));
    }

    /**
     * Test all filter
     */
    @Test
    public void allTest() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("s1", "string");
        attributes.put("i1", 111);
        Point point = Point.fromLngLat(0, 0);
        VectorTileDecoder.Feature feature = new VectorTileDecoder.Feature("test", 256, point, attributes, -1);
        Symbol symbol = new Symbol("test");
        JsonArray array = new JsonArray();
        array.add("all");
        JsonArray array2 = new JsonArray();
        array2.add("==");
        array2.add("s1");
        array2.add("string");
        JsonArray array3 = new JsonArray();
        array3.add("==");
        array3.add("i1");
        array3.add(111);
        array.add(array2);
        array.add(array3);
        assertTrue(symbol.evaluateFilter(array, feature));
        array = new JsonArray();
        array.add("all");
        array2 = new JsonArray();
        array2.add("==");
        array2.add("s1");
        array2.add("string");
        array3 = new JsonArray();
        array3.add(">=");
        array3.add("i1");
        array3.add(112);
        array.add(array2);
        array.add(array3);
        assertFalse(symbol.evaluateFilter(array, feature));
    }

    /**
     * Test any filter
     */
    @Test
    public void anyTest() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("s1", "string");
        attributes.put("i1", 111);
        Point point = Point.fromLngLat(0, 0);
        VectorTileDecoder.Feature feature = new VectorTileDecoder.Feature("test", 256, point, attributes, -1);
        Symbol symbol = new Symbol("test");
        JsonArray array = new JsonArray();
        array.add("any");
        JsonArray array2 = new JsonArray();
        array2.add("!=");
        array2.add("s1");
        array2.add("string");
        JsonArray array3 = new JsonArray();
        array3.add("<=");
        array3.add("i1");
        array3.add(110);
        array.add(array2);
        array.add(array3);
        assertFalse(symbol.evaluateFilter(array, feature));
        array = new JsonArray();
        array.add("any");
        array2 = new JsonArray();
        array2.add("==");
        array2.add("s1");
        array2.add("string");
        array3 = new JsonArray();
        array3.add(">=");
        array3.add("i1");
        array3.add(112);
        array.add(array2);
        array.add(array3);
        assertTrue(symbol.evaluateFilter(array, feature));
    }
}
