package de.blau.android.layer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Intent;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;
import ch.poole.android.screenshotrule.ScreenshotRule;
import de.blau.android.App;
import de.blau.android.LayerUtils;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.MockTileServer;
import de.blau.android.R;
import de.blau.android.Splash;
import de.blau.android.TestUtils;
import de.blau.android.layer.data.MapOverlay;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Way;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerDatabase;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.views.layers.MapTilesLayer;
import okhttp3.mockwebserver.MockWebServer;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class LayerDialogTest {

    private static final int VISIBLE_BUTTON = 0;
    public static final int  EXTENT_BUTTON  = 1;
    public static final int  MENU_BUTTON    = 3;

    AdvancedPrefDatabase prefDB          = null;
    Main                 main            = null;
    UiDevice             device          = null;
    Map                  map             = null;
    Logic                logic           = null;
    Splash               splash          = null;
    ActivityMonitor      monitor         = null;
    Instrumentation      instrumentation = null;
    MockWebServer        tileServer      = null;

    @Rule
    public ActivityTestRule<Splash> mActivityRule = new ActivityTestRule<>(Splash.class, false, false);

    @Rule
    public ScreenshotRule screenshotRule = new ScreenshotRule();

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.getTargetContext().deleteDatabase(TileLayerDatabase.DATABASE_NAME);
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        monitor = instrumentation.addMonitor(Main.class.getName(), null, false);

        Intent intent = new Intent(Intent.ACTION_MAIN);
        splash = mActivityRule.launchActivity(intent);

        main = (Main) instrumentation.waitForMonitorWithTimeout(monitor, 30000); // NOSONAR wait for main
        assertNotNull(main);
        TestUtils.grantPermissons(device);
        Preferences prefs = new Preferences(main);
        tileServer = MockTileServer.setupTileServer(main, prefs, "ersatz_background.mbt", true);
        map = main.getMap();
        map.setPrefs(main, prefs);

        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.loadTestData(main, "test2.osm");
        map.getDataLayer().setVisible(true);
        TestUtils.stopEasyEdit(main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        try {
            tileServer.close();
        } catch (IOException e) {
            // ignore
        }
        instrumentation.getTargetContext().deleteDatabase(TileLayerDatabase.DATABASE_NAME);
        instrumentation.removeMonitor(monitor);
        instrumentation.waitForIdleSync();
    }

    /**
     * Show dialog, zoom to extent, hide layer, try to select object, show layer
     */
    @Test
    public void dataLayer() {
        TestUtils.zoomToLevel(device, main, 21);
        String dataLayerName = map.getDataLayer().getName();
        UiObject2 extentButton = TestUtils.getLayerButton(device, dataLayerName, EXTENT_BUTTON);
        extentButton.clickAndWait(Until.newWindow(), 2000);
        BoundingBox box = map.getViewBox();
        // <bounds minlat='47.3881338' minlon='8.3863771' maxlat='47.3908067' maxlon='8.3911514' origin='CGImap 0.6.0
        System.out.println("Viewbox extent " + box);
        System.out.println("Data layer extent " + map.getDataLayer().getExtent());
        // (6068 thorn-01.openstreetmap.org)' />
        assertEquals(8.3911514, box.getRight() / 1E7D, 0.003);
        assertEquals(8.3863771, box.getLeft() / 1E7D, 0.003);
        //
        UiObject2 visibleButton = TestUtils.getLayerButton(device, dataLayerName, VISIBLE_BUTTON);
        visibleButton.click();
        TestUtils.clickText(device, true, main.getString(R.string.done), false, false);
        assertFalse(map.getDataLayer().isVisible());
        TestUtils.unlock(device);
        TestUtils.clickAtCoordinates(device, map, 8.38782, 47.390339, false);
        assertFalse(TestUtils.clickText(device, false, "Toilets", false, false)); // nothing should happen
        visibleButton = TestUtils.getLayerButton(device, dataLayerName, VISIBLE_BUTTON);
        visibleButton.click();

        TestUtils.clickText(device, true, main.getString(R.string.done), false, false);
        assertTrue(map.getDataLayer().isVisible());

    }

    /**
     * Show dialog, zoom to extent, hide layer, try to select object, show layer
     */
    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void dataLayerPrune() {
        TestUtils.zoomToLevel(device, main, 22);
        String dataLayerName = map.getDataLayer().getName();
        StorageDelegator delegator = App.getDelegator();
        assertEquals(929, delegator.getCurrentStorage().getNodeCount());
        assertEquals(99, delegator.getCurrentStorage().getWayCount());
        assertEquals(5, delegator.getCurrentStorage().getRelationCount());
        TestUtils.unlock(device);
        TestUtils.clickAtCoordinates(device, map, 8.38782, 47.390339, true);
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.actionmode_nodeselect)));
        TestUtils.clickUp(device);
        UiObject2 menuButton = TestUtils.getLayerButton(device, dataLayerName, MENU_BUTTON);
        menuButton.clickAndWait(Until.newWindow(), 1000);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.prune), true, false));

        TestUtils.clickText(device, true, main.getString(R.string.done), false, false);

        assertNotNull(delegator.getOsmElement(Node.NAME, 3465444349L));
        assertNull(delegator.getOsmElement(Way.NAME, 206010346L));

        assertEquals(387, delegator.getCurrentStorage().getNodeCount());
        assertEquals(14, delegator.getCurrentStorage().getWayCount());
        assertEquals(1, delegator.getCurrentStorage().getRelationCount());
    }

    /**
     * Show dialog, move data layer up one and then down
     */
    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void layerMove() {
        final MapOverlay dataLayer = map.getDataLayer();
        String dataLayerName = dataLayer.getName();
        UiObject2 menuButton = TestUtils.getLayerButton(device, dataLayerName, MENU_BUTTON);
        menuButton.clickAndWait(Until.newWindow(), 1000);
        int origPos = dataLayer.getIndex();
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.move_up), true, false));
        assertEquals(origPos + 1, dataLayer.getIndex());
        TestUtils.clickText(device, true, main.getString(R.string.done), false, false);
        menuButton = TestUtils.getLayerButton(device, dataLayerName, MENU_BUTTON);
        menuButton.clickAndWait(Until.newWindow(), 1000);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.move_down), true, false));
        assertEquals(origPos, dataLayer.getIndex());
        TestUtils.clickText(device, true, main.getString(R.string.done), false, false);
    }

    /**
     * Find task layer and enable
     */
    @Test
    public void taskLayer() {
        LayerUtils.addTaskLayer(main);
        assertNotNull(main.getMap().getTaskLayer());
        UiObject2 menuButton = TestUtils.getLayerButton(device, main.getString(R.string.layer_tasks), MENU_BUTTON);
        menuButton.clickAndWait(Until.newWindow(), 1000);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.discard), true, false));
        assertNull(main.getMap().getTaskLayer());
    }

    /**
     * Load geojson file and check if we can zoom to extent, style, discard
     */
    @Test
    public void geoJsonLayer() {
        de.blau.android.layer.Util.addLayer(main, LayerType.GEOJSON);
        map.setUpLayers(main);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("geojson/featureCollection.geojson");
        try {
            map.getGeojsonLayer().loadGeoJsonFile(main, is, false);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        UiObject2 extentButton = TestUtils.getLayerButton(device, main.getString(R.string.layer_geojson), EXTENT_BUTTON);
        extentButton.click();

        UiObject2 menuButton = TestUtils.getLayerButton(device, main.getString(R.string.layer_geojson), MENU_BUTTON);
        menuButton.clickAndWait(Until.newWindow(), 1000);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.layer_change_style), true, false));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.okay), true, false));
        TestUtils.sleep();
        menuButton.clickAndWait(Until.newWindow(), 1000);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_information), true, false));
        assertTrue(TestUtils.findText(device, false, "MultiLineString"));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.done), true, false));
        TestUtils.sleep();
        menuButton.clickAndWait(Until.newWindow(), 1000);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.discard), true, false));
        assertNull(map.getGeojsonLayer());
    }

    /**
     * Set to "mapnik" displaying the info dialog 1st
     */
    @Test
    public void backgroundLayer() {
        UiObject2 menuButton = TestUtils.getLayerButton(device, "Vespucci Test", MENU_BUTTON);
        menuButton.clickAndWait(Until.newWindow(), 1000);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.layer_select_imagery), true, false));
        screenshotRule.screenshot(main, "imagery_selection_1");
        TestUtils.clickText(device, true, main.getString(R.string.okay), true, false); // for the tip alert
        screenshotRule.screenshot(main, "imagery_selection_2");
        UiObject2 text = TestUtils.findObjectWithText(device, false, "OpenStreetMap (Standard)", 1000);
        List<UiObject2> children = text.getParent().getChildren();
        assertNotNull(children.get(1).clickAndWait(Until.newWindow(), 1000));
        assertTrue(TestUtils.clickText(device, true, main.getString(R.string.done), true, false));
        assertNotNull(text.clickAndWait(Until.newWindow(), 1000));
        TestUtils.sleep();
        main.getMap().invalidate();
        TestUtils.sleep();
        MapTilesLayer layer = main.getMap().getBackgroundLayer();
        assertNotNull(layer);
        assertEquals(TileLayerSource.LAYER_MAPNIK, layer.getTileLayerConfiguration().getId());
    }

    /**
     * Checks if filters are correct
     */
    @Test
    public void layerFilter() {
        try (TileLayerDatabase db = new TileLayerDatabase(ApplicationProvider.getApplicationContext())) {
            TileLayerSource.addOrUpdateCustomLayer(main, db.getWritableDatabase(), "TERRAINTEST", null, -1, -1, "Terrain Test", null,
                    TileLayerSource.Category.elevation, TileLayerSource.TYPE_TMS, null, 0, 19, false, "");
            TileLayerSource.getListsLocked(main, db.getReadableDatabase(), true);
        }
        assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/layers", true));
        assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/add", true));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_layers_add_backgroundlayer), true, false));
        TestUtils.clickText(device, true, main.getString(R.string.okay), true, false);
        assertTrue(TestUtils.clickText(device, true, main.getString(R.string.layer_category_all), true, false));
        assertTrue(TestUtils.clickText(device, true, main.getString(R.string.layer_category_photo), true, false));
        assertTrue(TestUtils.clickText(device, true, main.getString(R.string.layer_category_elevation), true, false));
        assertTrue(TestUtils.clickText(device, true, "Terrain Test", false, false));
    }
}
