package de.blau.android.layer.geojson;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
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
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.TestUtils;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerServer;
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
        main = (Main) mActivityRule.getActivity();
        map = main.getMap();
        TestUtils.grantPermissons();
        TestUtils.dismissStartUpDialogs(main);
        Preferences prefs = new Preferences(context);
        // allow downloading tiles here
        prefs.setBackGroundLayer(TileLayerServer.LAYER_NONE);
    }

    /**
     * Post test teardown
     */
    @After
    public void teardown() {
    }

    /**
     * Import a FeatureCollection
     */
    @Test
    public void importFeatureCollection() {
        de.blau.android.layer.geojson.MapOverlay layer = main.getMap().getGeojsonLayer();

        loadGeoJOSN(layer, "geojson/featureCollection.geojson");
        map.getViewBox().setBorders(map, layer.getExtent(), false);
        map.invalidate();
        List<Feature> content = layer.getFeatures();
        Assert.assertEquals(3, content.size());
        for (Feature feature : content) {
            Geometry geometry = feature.geometry();
            JsonObject properties = feature.properties();
            switch (geometry.type()) {
            case GeoJSONConstants.POINT:
                JsonElement prop = properties.get("prop0");
                Assert.assertNotNull(prop);
                Assert.assertTrue(prop.isJsonPrimitive());
                Assert.assertEquals("value0", prop.getAsString());
                break;
            case GeoJSONConstants.LINESTRING:
                prop = properties.get("prop1");
                Assert.assertNotNull(prop);
                Assert.assertTrue(prop.isJsonPrimitive());
                Assert.assertEquals(0.0, prop.getAsDouble(), 0.000001);
                break;
            case GeoJSONConstants.POLYGON:
                prop = properties.get("prop1");
                Assert.assertNotNull(prop);
                Assert.assertTrue(prop.isJsonObject());
                Assert.assertEquals("that", prop.getAsJsonObject().get("this").getAsString());
                break;
            default:
                Assert.fail("Unexpected Feature " + geometry.type());
            }
        }
        try {
            layer.onSaveState(context);
            layer.onRestoreState(context);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        content = layer.getFeatures();
        Assert.assertEquals(3, content.size());
    }

    /**
     * Show a Feature in the info dialog
     */
    @Test
    public void showFeatureInfo() {
        de.blau.android.layer.geojson.MapOverlay layer = main.getMap().getGeojsonLayer();

        loadGeoJOSN(layer, "geojson/featureCollection.geojson");
        layer.setLabel("prop0");
        map.getViewBox().setBorders(map, layer.getExtent(), false);
        map.invalidate();
        List<Feature> content = layer.getFeatures();
        Assert.assertEquals(3, content.size());
        for (Feature feature : content) {
            Geometry geometry = feature.geometry();
            switch (geometry.type()) {
            case GeoJSONConstants.POINT:
                App.getLogic().setZoom(map, Map.SHOW_LABEL_LIMIT + 1);
                TestUtils.unlock();
                Point p = (Point) geometry;
                TestUtils.clickAtCoordinates(map, p.longitude(), p.latitude(), true);
                Assert.assertTrue(TestUtils.findText(device, false, GeoJSONConstants.POINT));
                Assert.assertTrue(TestUtils.findText(device, false, "value0"));
                device.pressBack();
                break;
            case GeoJSONConstants.POLYGON:
                TestUtils.unlock();
                TestUtils.clickAtCoordinates(map, 100.5, 0.5, true);
                Assert.assertTrue(TestUtils.findText(device, false, GeoJSONConstants.POLYGON));
                Assert.assertTrue(TestUtils.findText(device, false, "value0"));
                device.pressBack();
                break;
            }
        }
    }

    /**
     * Import individual geometries
     */
    @Test
    public void importGeometries() {
        de.blau.android.layer.geojson.MapOverlay layer = main.getMap().getGeojsonLayer();
        loadGeoJOSN(layer, "geojson/lineString.geojson");
        map.getViewBox().setBorders(map, layer.getExtent(), false);
        map.invalidate();
        List<Feature> content = layer.getFeatures();
        Assert.assertEquals(1, content.size());
        Feature f = content.get(0);
        Geometry g = f.geometry();
        Assert.assertEquals(GeoJSONConstants.LINESTRING, g.type());
        List<Point> lineString = ((LineString) g).coordinates();
        Assert.assertEquals(3, lineString.size());
        loadGeoJOSN(layer, "geojson/multiPoint.geojson");
        map.getViewBox().setBorders(map, layer.getExtent(), false);
        map.invalidate();
        content = layer.getFeatures();
        Assert.assertEquals(1, content.size());
        f = content.get(0);
        g = f.geometry();
        Assert.assertEquals(GeoJSONConstants.MULTIPOINT, g.type());
        @SuppressWarnings("unchecked")
        List<Point> multiPoint = ((CoordinateContainer<List<Point>>) g).coordinates();
        Assert.assertEquals(4, multiPoint.size());
        loadGeoJOSN(layer, "geojson/holeyMultiPolygon.geojson");
        map.getViewBox().setBorders(map, layer.getExtent(), false);
        map.invalidate();
        content = layer.getFeatures();
        Assert.assertEquals(1, content.size());
        f = content.get(0);
        g = f.geometry();
        Assert.assertEquals(GeoJSONConstants.MULTIPOLYGON, g.type());
        @SuppressWarnings("unchecked")
        List<List<List<Point>>> multiPolygon = ((CoordinateContainer<List<List<List<Point>>>>) g).coordinates();
        Assert.assertEquals(2, multiPolygon.size()); // 2 polygons
        Assert.assertEquals(1, multiPolygon.get(0).size()); // 1 ring
        Assert.assertEquals(2, multiPolygon.get(1).size()); // 2 rings
        loadGeoJOSN(layer, "geojson/point.geojson");
        map.getViewBox().setBorders(map, layer.getExtent(), false);
        map.invalidate();
        content = layer.getFeatures();
        Assert.assertEquals(1, content.size());
        f = content.get(0);
        g = f.geometry();
        Assert.assertEquals(GeoJSONConstants.POINT, g.type());
        Point point = ((Point) g);
        Assert.assertEquals(30d, point.longitude(), 0.00001D);
        Assert.assertEquals(10d, point.latitude(), 0.00001D);
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
            layer.loadGeoJsonFile(context, is);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }
}
