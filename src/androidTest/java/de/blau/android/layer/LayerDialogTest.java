package de.blau.android.layer;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Intent;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.SignalHandler;
import de.blau.android.Splash;
import de.blau.android.TestUtils;
import de.blau.android.osm.ApiTest;
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
    private static final int MENU_BUTTON    = 3;

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

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        monitor = instrumentation.addMonitor(Main.class.getName(), null, false);

        Intent intent = new Intent(Intent.ACTION_MAIN);
        splash = mActivityRule.launchActivity(intent);

        main = (Main) instrumentation.waitForMonitorWithTimeout(monitor, 30000); // wait for main
        Assert.assertNotNull(main);
        TestUtils.grantPermissons(device);
        Preferences prefs = new Preferences(main);
        TestUtils.removeImageryLayers(main);
        tileServer = TestUtils.setupTileServer(main, prefs, "ersatz_background.mbt");
        map = main.getMap();
        map.setPrefs(main, prefs);

        TestUtils.dismissStartUpDialogs(device, main);
        final CountDownLatch signal1 = new CountDownLatch(1);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("test2.osm");
        logic = App.getLogic();
        logic.readOsmFile(main, is, false, new SignalHandler(signal1));
        try {
            signal1.await(ApiTest.TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        try {
            is.close();
        } catch (IOException e1) {
        }
        map.getDataLayer().setVisible(true);
        TestUtils.stopEasyEdit(main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        if (main != null) {
            main.deleteDatabase(TileLayerDatabase.DATABASE_NAME);
            main.finish();
        } else {
            System.out.println("main is null");
        }
        try {
            tileServer.close();
        } catch (IOException e) {
            // ignore
        }
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
        Assert.assertEquals(8.3911514, box.getRight() / 1E7D, 0.003);
        Assert.assertEquals(8.3863771, box.getLeft() / 1E7D, 0.003);
        //
        UiObject2 visibleButton = TestUtils.getLayerButton(device, dataLayerName, VISIBLE_BUTTON);
        visibleButton.click();
        TestUtils.clickText(device, true, main.getString(R.string.done), false, false);
        Assert.assertFalse(map.getDataLayer().isVisible());
        TestUtils.unlock(device);
        TestUtils.clickAtCoordinates(device, map, 8.38782, 47.390339, false);
        Assert.assertFalse(TestUtils.clickText(device, false, "Toilets", false, false)); // nothing should happen
        visibleButton = TestUtils.getLayerButton(device, dataLayerName, VISIBLE_BUTTON);
        visibleButton.click();

        TestUtils.clickText(device, true, main.getString(R.string.done), false, false);
        Assert.assertTrue(map.getDataLayer().isVisible());

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
        Assert.assertEquals(929, delegator.getCurrentStorage().getNodeCount());
        Assert.assertEquals(99, delegator.getCurrentStorage().getWayCount());
        Assert.assertEquals(5, delegator.getCurrentStorage().getRelationCount());
        TestUtils.unlock(device);
        TestUtils.clickAtCoordinates(device, map, 8.38782, 47.390339, true);
        Assert.assertTrue(TestUtils.findText(device, false, main.getString(R.string.actionmode_nodeselect)));
        TestUtils.clickUp(device);
        UiObject2 menuButton = TestUtils.getLayerButton(device, dataLayerName, MENU_BUTTON);
        menuButton.clickAndWait(Until.newWindow(), 1000);
        Assert.assertTrue(TestUtils.clickText(device, false, main.getString(R.string.prune), true, false));

        TestUtils.clickText(device, true, main.getString(R.string.done), false, false);

        Assert.assertNotNull(delegator.getOsmElement(Node.NAME, 3465444349L));
        Assert.assertNull(delegator.getOsmElement(Way.NAME, 206010346L));

        Assert.assertEquals(387, delegator.getCurrentStorage().getNodeCount());
        Assert.assertEquals(14, delegator.getCurrentStorage().getWayCount());
        Assert.assertEquals(5, delegator.getCurrentStorage().getRelationCount());
    }

    /**
     * Find task layer and enable
     */
    @Test
    public void taskLayer() {
        TestUtils.addTaskLayer(main);
        Assert.assertNotNull(main.getMap().getTaskLayer());
        UiObject2 menuButton = TestUtils.getLayerButton(device, main.getString(R.string.layer_tasks), MENU_BUTTON);
        menuButton.clickAndWait(Until.newWindow(), 1000);
        Assert.assertTrue(TestUtils.clickText(device, false, main.getString(R.string.discard), true, false));
        Assert.assertNull(main.getMap().getTaskLayer());
    }

    /**
     * Load geojson file and check if we can zoom to extent, style, discard
     */
    @Test
    public void geoJsonLayer() {
        de.blau.android.layer.Util.addLayer(main, LayerType.GEOJSON);
        map.setUpLayers(main);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("geojson/multiPoint.geojson");
        try {
            map.getGeojsonLayer().loadGeoJsonFile(main, is);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        UiObject2 extentButton = TestUtils.getLayerButton(device, main.getString(R.string.layer_geojson), EXTENT_BUTTON);
        extentButton.click();
        // TODO check that the zooming worked

        UiObject2 menuButton = TestUtils.getLayerButton(device, main.getString(R.string.layer_geojson), MENU_BUTTON);
        menuButton.clickAndWait(Until.newWindow(), 1000);
        Assert.assertTrue(TestUtils.clickText(device, false, main.getString(R.string.layer_change_style), true, false));
        Assert.assertTrue(TestUtils.clickText(device, false, main.getString(R.string.okay), true, false));
        TestUtils.sleep();
        menuButton.clickAndWait(Until.newWindow(), 1000);
        Assert.assertTrue(TestUtils.clickText(device, false, main.getString(R.string.discard), true, false));
        Assert.assertNotNull(map.getGeojsonLayer());
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(main)) {
            db.deleteLayer(LayerType.GEOJSON, null);
        }
    }

    /**
     * Set to None
     */
    @Test
    public void backgroundLayer() {
        UiObject2 menuButton = TestUtils.getLayerButton(device, "Vespucci Test", MENU_BUTTON);
        menuButton.clickAndWait(Until.newWindow(), 1000);
        Assert.assertTrue(TestUtils.clickText(device, false, main.getString(R.string.layer_select_imagery), true, false));
        TestUtils.clickText(device, true, main.getString(R.string.okay), true, false); // for the tip alert
        TestUtils.clickText(device, true, main.getString(R.string.none), true, false);
        TestUtils.sleep();
        MapTilesLayer layer = main.getMap().getBackgroundLayer();
        Assert.assertNotNull(layer);
        Assert.assertEquals(TileLayerSource.LAYER_NONE, layer.getTileLayerConfiguration().getId());
    }
}
