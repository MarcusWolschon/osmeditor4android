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
import com.google.gson.JsonPrimitive;
import com.mapbox.geojson.Point;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.App;
import de.blau.android.resources.DataStyle;
import de.blau.android.util.mvt.VectorTileDecoder;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
@LargeTest
public class ExpressionTest {

    /**
     * Pre test setup
     */
    @Before
    public void setup() {
        // default values are currently taken from the data style
        DataStyle styles = App.getDataStyle(ApplicationProvider.getApplicationContext());
        styles.getStylesFromFiles(ApplicationProvider.getApplicationContext());
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
        JsonArray array = new JsonArray();
        array.add("has");
        array.add("s1");
        Object o = Symbol.evaluateExpression(array, feature);
        assertTrue(o instanceof Boolean);
        assertTrue((Boolean) o);
        array = new JsonArray();
        array.add("has");
        array.add("s2");
        o = Symbol.evaluateExpression(array, feature);
        assertTrue(o instanceof Boolean);
        assertFalse((Boolean) o);
        array = new JsonArray();
        array.add("has");
        JsonArray getArray = new JsonArray();
        getArray.add("get");
        getArray.add("test");
        array.add(getArray);
        o = Symbol.evaluateExpression(array, feature);
        assertTrue(o instanceof Boolean);
        assertTrue((Boolean) o);
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
        JsonArray array = new JsonArray();
        array.add("!has");
        array.add("s2");
        Object o = Symbol.evaluateExpression(array, feature);
        assertTrue(o instanceof Boolean);
        assertTrue((Boolean) o);
        array = new JsonArray();
        array.add("!has");
        array.add("s1");
        o = Symbol.evaluateExpression(array, feature);
        assertTrue(o instanceof Boolean);
        assertFalse((Boolean) o);
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
        JsonArray array = new JsonArray();
        array.add("get");
        array.add("s1");
        assertEquals("string", Symbol.evaluateExpression(array, feature));
        array = new JsonArray();
        array.add("get");
        array.add("s2");
        assertNull(Symbol.evaluateExpression(array, feature));
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
        JsonArray array = new JsonArray();
        array.add("to-boolean");
        JsonArray getArray = new JsonArray();
        getArray.add("get");
        getArray.add("test");
        array.add(getArray);
        Object o = Symbol.evaluateExpression(array, feature);
        assertTrue(o instanceof Boolean);
        assertFalse((boolean) o);
        //
        array = new JsonArray();
        array.add("to-boolean");
        getArray = new JsonArray();
        getArray.add("get");
        getArray.add("s1");
        array.add(getArray);
        o = Symbol.evaluateExpression(array, feature);
        assertTrue(o instanceof Boolean);
        assertTrue((boolean) o);
        //
        array = new JsonArray();
        array.add("to-boolean");
        getArray = new JsonArray();
        getArray.add("get");
        getArray.add("test2");
        array.add(getArray);
        o = Symbol.evaluateExpression(array, feature);
        assertTrue(o instanceof Boolean);
        assertFalse((boolean) o);
    }

    /**
     * Test toNumber expression
     */
    @Test
    public void toNumberTest() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("n1", "string");
        attributes.put("n2", null);
        attributes.put("n3", 1111);
        attributes.put("n4", true);
        attributes.put("n5", false);

        Point point = Point.fromLngLat(0, 0);
        VectorTileDecoder.Feature feature = new VectorTileDecoder.Feature("test", 256, point, attributes, -1);
        JsonArray array = new JsonArray();
        array.add("to-number");
        JsonArray getArray = new JsonArray();
        getArray.add("get");
        getArray.add("n1");
        array.add(getArray);
        Object o = Symbol.evaluateExpression(array, feature);
        assertTrue(o instanceof Number);
        assertEquals(0, ((Number) o).intValue());
        //
        getArray.set(1, new JsonPrimitive("n2"));
        o = Symbol.evaluateExpression(array, feature);
        assertTrue(o instanceof Number);
        assertEquals(0, ((Number) o).intValue());
        //
        getArray.set(1, new JsonPrimitive("n3"));
        o = Symbol.evaluateExpression(array, feature);
        assertTrue(o instanceof Number);
        assertEquals(1111, ((Number) o).intValue());
        //
        getArray.set(1, new JsonPrimitive("n4"));
        o = Symbol.evaluateExpression(array, feature);
        assertTrue(o instanceof Number);
        assertEquals(1, ((Number) o).intValue());
        //
        getArray.set(1, new JsonPrimitive("n5"));
        o = Symbol.evaluateExpression(array, feature);
        assertTrue(o instanceof Number);
        assertEquals(0, ((Number) o).intValue());
    }

    /**
     * Test match expression
     */
    @Test
    public void matchTest() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("n1", "bench");
        attributes.put("n2", "fountain");
        attributes.put("n3", "nothing");

        Point point = Point.fromLngLat(0, 0);
        VectorTileDecoder.Feature feature = new VectorTileDecoder.Feature("test", 256, point, attributes, -1);
        JsonArray array = new JsonArray();
        array.add("match");
        JsonArray getArray = new JsonArray();
        getArray.add("get");
        getArray.add("n1");
        array.add(getArray);
        array.add("bench");
        array.add("icon-bench");
        array.add("something");
        array.add("icon-something");
        array.add("fountain");
        array.add("icon-fountain");
        array.add("unknown");
        Object o = Symbol.evaluateExpression(array, feature);
        assertTrue(o instanceof JsonPrimitive);
        assertEquals("icon-bench", ((JsonPrimitive) o).getAsString());

        getArray.set(1, new JsonPrimitive("n2"));
        o = Symbol.evaluateExpression(array, feature);
        assertTrue(o instanceof JsonPrimitive);
        assertEquals("icon-fountain", ((JsonPrimitive) o).getAsString());

        getArray.set(1, new JsonPrimitive("n3"));
        o = Symbol.evaluateExpression(array, feature);
        assertTrue(o instanceof JsonPrimitive);
        assertEquals("unknown", ((JsonPrimitive) o).getAsString());
    }

    /**
     * Test match expression
     */
    @Test
    public void matchTest2() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("n1", "bench");
        attributes.put("n2", "fountain");
        attributes.put("n3", "nothing");

        Point point = Point.fromLngLat(0, 0);
        VectorTileDecoder.Feature feature = new VectorTileDecoder.Feature("test", 256, point, attributes, -1);
        JsonArray array = new JsonArray();
        array.add("match");
        JsonArray getArray = new JsonArray();
        getArray.add("get");
        getArray.add("n1");
        array.add(getArray);
        JsonArray labelArray = new JsonArray();
        labelArray.add("something");
        labelArray.add("bench");
        array.add(labelArray);
        array.add("icon-bench");

        array.add("fountain");
        array.add("icon-fountain");
        array.add("unknown");
        Object o = Symbol.evaluateExpression(array, feature);
        assertTrue(o instanceof JsonPrimitive);
        assertEquals("icon-bench", ((JsonPrimitive) o).getAsString());

        getArray.set(1, new JsonPrimitive("n2"));
        o = Symbol.evaluateExpression(array, feature);
        assertTrue(o instanceof JsonPrimitive);
        assertEquals("icon-fountain", ((JsonPrimitive) o).getAsString());

        getArray.set(1, new JsonPrimitive("n3"));
        o = Symbol.evaluateExpression(array, feature);
        assertTrue(o instanceof JsonPrimitive);
        assertEquals("unknown", ((JsonPrimitive) o).getAsString());
    }
}
