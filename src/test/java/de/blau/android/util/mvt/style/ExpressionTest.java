package de.blau.android.util.mvt.style;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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
import de.blau.android.resources.DataStyle;
import de.blau.android.util.mvt.VectorTileDecoder;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=33)
@LargeTest
public class ExpressionTest {

    /**
     * Pre test setup
     */
    @Before
    public void setup() {
        // default values are currently taken from the data style
        DataStyle.getStylesFromFiles(ApplicationProvider.getApplicationContext());
    }
    
    /**
     * Test has expression
     */
    @Test
    public void hasTest() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("s1", "string");
        attributes.put("i1", 111);
        attributes.put("test", "i1");
        Point point = Point.fromLngLat(0, 0);
        VectorTileDecoder.Feature feature = new VectorTileDecoder.Feature("test", 256, point, attributes, -1);
        Symbol symbol = new Symbol("test");
        JsonArray array = new JsonArray();
        array.add("has");
        array.add("s1");
        Object o = symbol.evaluateExpression(array, feature);
        assertTrue(o instanceof Boolean);
        assertTrue((Boolean)o);
        array = new JsonArray();
        array.add("has");
        array.add("s2");
        o = symbol.evaluateExpression(array, feature);
        assertTrue(o instanceof Boolean);
        assertFalse((Boolean)o);
        array = new JsonArray();
        array.add("has");
        JsonArray getArray = new JsonArray();
        getArray.add("get");
        getArray.add("test");
        array.add(getArray);
        o = symbol.evaluateExpression(array, feature);
        assertTrue(o instanceof Boolean);
        assertTrue((Boolean)o);
    }
    
    /**
     * Test !has expression
     */
    @Test
    public void notHasTest() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("s1", "string");
        attributes.put("i1", 111);
        attributes.put("test", "i1");
        Point point = Point.fromLngLat(0, 0);
        VectorTileDecoder.Feature feature = new VectorTileDecoder.Feature("test", 256, point, attributes, -1);
        Symbol symbol = new Symbol("test");
        JsonArray array = new JsonArray();
        array.add("!has");
        array.add("s2");
        Object o = symbol.evaluateExpression(array, feature);
        assertTrue(o instanceof Boolean);
        assertTrue((Boolean)o);
        array = new JsonArray();
        array.add("!has");
        array.add("s1");
        o = symbol.evaluateExpression(array, feature);
        assertTrue(o instanceof Boolean);
        assertFalse((Boolean)o);
    }
    
    /**
     * Test get expression
     */
    @Test
    public void getTest() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("s1", "string");
        attributes.put("i1", 111);
        attributes.put("test", "i1");
        Point point = Point.fromLngLat(0, 0);
        VectorTileDecoder.Feature feature = new VectorTileDecoder.Feature("test", 256, point, attributes, -1);
        Symbol symbol = new Symbol("test");
        JsonArray array = new JsonArray();
        array.add("get");
        array.add("s1");
        assertEquals("string", symbol.evaluateExpression(array, feature));
        array = new JsonArray();
        array.add("get");
        array.add("s2");
        assertNull(symbol.evaluateExpression(array, feature));
    }
    
    /**
     * Test toBooleean expression
     */
    @Test
    public void toBooleanTest() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("s1", "string");
        attributes.put("i1", null);
        attributes.put("test", 0);
        attributes.put("test2", false);
        Point point = Point.fromLngLat(0, 0);
        VectorTileDecoder.Feature feature = new VectorTileDecoder.Feature("test", 256, point, attributes, -1);
        Symbol symbol = new Symbol("test");
        JsonArray array = new JsonArray();
        array.add("to-boolean");
        JsonArray getArray = new JsonArray();
        getArray.add("get");
        getArray.add("test");
        array.add(getArray);
        Object o = symbol.evaluateExpression(array, feature);
        assertTrue(o instanceof Boolean);
        assertFalse((boolean) o);
        //
        array = new JsonArray();
        array.add("to-boolean");
        getArray = new JsonArray();
        getArray.add("get");
        getArray.add("s1");
        array.add(getArray);
        o = symbol.evaluateExpression(array, feature);
        assertTrue(o instanceof Boolean);
        assertTrue((boolean) o);
        //
        array = new JsonArray();
        array.add("to-boolean");
        getArray = new JsonArray();
        getArray.add("get");
        getArray.add("test2");
        array.add(getArray);
        o = symbol.evaluateExpression(array, feature);
        assertTrue(o instanceof Boolean);
        assertFalse((boolean) o);
    }
}
