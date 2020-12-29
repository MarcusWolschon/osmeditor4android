package de.blau.android.layer.geojson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mapbox.geojson.CoordinateContainer;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;

import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import de.blau.android.App;
import de.blau.android.LayerUtils;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.TestUtils;
import de.blau.android.layer.LayerType;
import de.blau.android.osm.Node;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.util.GeoJSONConstants;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class GeoJSONTest {

    public static final int TIMEOUT         = 115;
    Context                 context         = null;
    AdvancedPrefDatabase    prefDB          = null;
    Main                    main            = null;
    Map                     map             = null;
    UiDevice                device          = null;
    ActivityMonitor         monitor         = null;
    Instrumentation         instrumentation = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        main = mActivityRule.getActivity();
        map = main.getMap();
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        LayerUtils.removeImageryLayers(context);
        de.blau.android.layer.Util.addLayer(context, LayerType.GEOJSON);
        map.setUpLayers(context);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(context)) {
            db.deleteLayer(LayerType.GEOJSON, null);
        }
    }

    /**
     * Import a FeatureCollection
     */
    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void importFeatureCollection() {
        de.blau.android.layer.geojson.MapOverlay layer = main.getMap().getGeojsonLayer();

        loadGeoJOSN(layer, "geojson/featureCollection.geojson");
        map.getViewBox().setBorders(map, layer.getExtent(), false);
        map.invalidate();
        List<Feature> content = layer.getFeatures();
        assertEquals(3, content.size());
        for (Feature feature : content) {
            Geometry geometry = feature.geometry();
            JsonObject properties = feature.properties();
            switch (geometry.type()) {
            case GeoJSONConstants.POINT:
                JsonElement prop = properties.get("prop0");
                assertNotNull(prop);
                assertTrue(prop.isJsonPrimitive());
                assertEquals("value0", prop.getAsString());
                break;
            case GeoJSONConstants.LINESTRING:
                prop = properties.get("prop1");
                assertNotNull(prop);
                assertTrue(prop.isJsonPrimitive());
                assertEquals(0.0, prop.getAsDouble(), 0.000001);
                break;
            case GeoJSONConstants.POLYGON:
                prop = properties.get("prop1");
                assertNotNull(prop);
                assertTrue(prop.isJsonObject());
                assertEquals("that", prop.getAsJsonObject().get("this").getAsString());
                break;
            default:
                fail("Unexpected Feature " + geometry.type());
            }
        }
        try {
            layer.onSaveState(context);
            layer.onRestoreState(context);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        content = layer.getFeatures();
        assertEquals(3, content.size());
    }

    /**
     * Show a Feature in the info dialog
     */
    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void showFeatureInfo() {
        de.blau.android.layer.geojson.MapOverlay layer = main.getMap().getGeojsonLayer();

        loadGeoJOSN(layer, "geojson/featureCollection.geojson");
        layer.setLabel("prop0");
        map.getViewBox().setBorders(map, layer.getExtent(), false);
        map.invalidate();
        List<Feature> content = layer.getFeatures();
        assertEquals(3, content.size());
        for (Feature feature : content) {
            Geometry geometry = feature.geometry();
            switch (geometry.type()) {
            case GeoJSONConstants.POINT:
                App.getLogic().setZoom(map, Map.SHOW_LABEL_LIMIT + 1);
                TestUtils.unlock(device);
                Point p = (Point) geometry;
                TestUtils.clickAtCoordinates(device, map, p.longitude(), p.latitude(), true);
                assertTrue(TestUtils.findText(device, false, GeoJSONConstants.POINT));
                assertTrue(TestUtils.findText(device, false, "value0"));
                TestUtils.clickButton(device, "android:id/button3", true);
                assertTrue(TestUtils.findText(device, false, main.getString(R.string.actionmode_nodeselect)));
                Node n = App.getLogic().getSelectedNode();
                assertNotNull(n);
                assertEquals((int) (p.longitude() * 1E7D), n.getLon());
                assertEquals((int) (p.latitude() * 1E7D), n.getLat());
                TestUtils.clickUp(device);
                break;
            case GeoJSONConstants.POLYGON:
                TestUtils.unlock(device);
                TestUtils.clickAtCoordinates(device, map, 100.5, 0.5, true);
                assertTrue(TestUtils.findText(device, false, GeoJSONConstants.POLYGON));
                assertTrue(TestUtils.findText(device, false, "value0"));
                device.pressBack();
                break;
            }
        }
    }

    /**
     * Import individual geometries
     */
    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void importGeometries() {
        de.blau.android.layer.geojson.MapOverlay layer = main.getMap().getGeojsonLayer();
        loadGeoJOSN(layer, "geojson/lineString.geojson");
        map.getViewBox().setBorders(map, layer.getExtent(), false);
        map.invalidate();
        List<Feature> content = layer.getFeatures();
        assertEquals(1, content.size());
        Feature f = content.get(0);
        Geometry g = f.geometry();
        assertEquals(GeoJSONConstants.LINESTRING, g.type());
        List<Point> lineString = ((LineString) g).coordinates();
        assertEquals(3, lineString.size());
        loadGeoJOSN(layer, "geojson/multiPoint.geojson");
        map.getViewBox().setBorders(map, layer.getExtent(), false);
        map.invalidate();
        content = layer.getFeatures();
        assertEquals(1, content.size());
        f = content.get(0);
        g = f.geometry();
        assertEquals(GeoJSONConstants.MULTIPOINT, g.type());
        @SuppressWarnings("unchecked")
        List<Point> multiPoint = ((CoordinateContainer<List<Point>>) g).coordinates();
        assertEquals(4, multiPoint.size());
        loadGeoJOSN(layer, "geojson/holeyMultiPolygon.geojson");
        map.getViewBox().setBorders(map, layer.getExtent(), false);
        map.invalidate();
        content = layer.getFeatures();
        assertEquals(1, content.size());
        f = content.get(0);
        g = f.geometry();
        assertEquals(GeoJSONConstants.MULTIPOLYGON, g.type());
        @SuppressWarnings("unchecked")
        List<List<List<Point>>> multiPolygon = ((CoordinateContainer<List<List<List<Point>>>>) g).coordinates();
        assertEquals(2, multiPolygon.size()); // 2 polygons
        assertEquals(1, multiPolygon.get(0).size()); // 1 ring
        assertEquals(2, multiPolygon.get(1).size()); // 2 rings
        loadGeoJOSN(layer, "geojson/point.geojson");
        map.getViewBox().setBorders(map, layer.getExtent(), false);
        map.invalidate();
        content = layer.getFeatures();
        assertEquals(1, content.size());
        f = content.get(0);
        g = f.geometry();
        assertEquals(GeoJSONConstants.POINT, g.type());
        Point point = ((Point) g);
        assertEquals(30d, point.longitude(), 0.00001D);
        assertEquals(10d, point.latitude(), 0.00001D);
    }

    /**
     * Load a file into the geojson layer
     * 
     * @param layer the Layer
     * @param fileName the filename
     */
    void loadGeoJOSN(de.blau.android.layer.geojson.MapOverlay layer, String fileName) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream(fileName);
        try {
            layer.loadGeoJsonFile(context, is, false);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }
}
