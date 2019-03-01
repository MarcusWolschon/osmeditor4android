package de.blau.android.layer;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
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
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;
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
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerDatabase;
import de.blau.android.resources.TileLayerServer;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class LayerDialogTest {

    private static final int VISIBLE_BUTTON = 0;
    private static final int EXTENT_BUTTON  = 1;
    private static final int MENU_BUTTON    = 3;

    Context              context         = null;
    AdvancedPrefDatabase prefDB          = null;
    Main                 main            = null;
    UiDevice             device          = null;
    Map                  map             = null;
    Logic                logic           = null;
    Splash               splash          = null;
    ActivityMonitor      monitor         = null;
    Instrumentation      instrumentation = null;

    @Rule
    public ActivityTestRule<Splash> mActivityRule = new ActivityTestRule<>(Splash.class, false, false);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        monitor = instrumentation.addMonitor(Main.class.getName(), null, false);

        Intent intent = new Intent(Intent.ACTION_MAIN);
        splash = mActivityRule.launchActivity(intent);

        main = (Main) instrumentation.waitForMonitorWithTimeout(monitor, 30000); // wait for main

        Preferences prefs = new Preferences(context);
        prefs.setBackGroundLayer(TileLayerServer.LAYER_MAPNIK); // need to have this on for testing here
        prefs.setOverlayLayer(TileLayerServer.LAYER_NOOVERLAY);
        map = main.getMap();
        map.setPrefs(main, prefs);
        TestUtils.grantPermissons();
        TestUtils.dismissStartUpDialogs(main);
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
        instrumentation.removeMonitor(monitor);
        context.deleteDatabase(TileLayerDatabase.DATABASE_NAME);
        try {
            main.finish();
        } catch (Exception e) {
        }
        instrumentation.waitForIdleSync();
    }

    /**
     * Show dialog, zoom to extent, hide layer, try to select object, show layer
     */
    @Test
    public void dataLayer() {
        TestUtils.zoomToLevel(main, 21);
        UiObject2 extentButton = getLayerButton(context.getString(R.string.layer_data), EXTENT_BUTTON);
        extentButton.clickAndWait(Until.newWindow(), 2000);
        BoundingBox box = map.getViewBox();
        // <bounds minlat='47.3881338' minlon='8.3863771' maxlat='47.3908067' maxlon='8.3911514' origin='CGImap 0.6.0
        System.out.println("Viewbox extent " + box);
        System.out.println("Data layer extent " + map.getDataLayer().getExtent());
        // (6068 thorn-01.openstreetmap.org)' />
        Assert.assertEquals(8.3911514, box.getRight() / 1E7D, 0.003);
        Assert.assertEquals(8.3863771, box.getLeft() / 1E7D, 0.003);
        //
        UiObject2 visibleButton = getLayerButton(context.getString(R.string.layer_data), VISIBLE_BUTTON);
        visibleButton.click();
        TestUtils.clickText(device, true, context.getString(R.string.done), false);
        Assert.assertFalse(map.getDataLayer().isVisible());
        TestUtils.unlock();
        TestUtils.clickAtCoordinates(map, 8.38782, 47.390339, false);
        Assert.assertFalse(TestUtils.clickText(device, false, "Toilets", false)); // nothing should happen
        visibleButton = getLayerButton(context.getString(R.string.layer_data), VISIBLE_BUTTON);
        visibleButton.click();
        TestUtils.clickText(device, true, context.getString(R.string.done), false);
        Assert.assertTrue(map.getDataLayer().isVisible());
    }

    /**
     * Find task layer and disable
     */
    @Test
    public void taskLayer() {
        UiObject2 menuButton = getLayerButton(context.getString(R.string.layer_tasks), MENU_BUTTON);
        menuButton.clickAndWait(Until.newWindow(), 1000);
        Assert.assertTrue(TestUtils.clickText(device, false, context.getString(R.string.disable), true));
        Preferences prefs = new Preferences(context);
        Assert.assertFalse(prefs.areBugsEnabled());
        prefs.setBugsEnabled(true);
    }

    /**
     * Load geojson file and check if we can zoom to extent
     */
    @Test
    public void geoJsonLayer() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("geojson/multiPoint.geojson");
        try {
            map.getGeojsonLayer().loadGeoJsonFile(context, is);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        UiObject2 extentButton = getLayerButton(context.getString(R.string.layer_geojson), EXTENT_BUTTON);
        extentButton.click();
        UiObject2 menuButton = getLayerButton(context.getString(R.string.layer_geojson), MENU_BUTTON);
        menuButton.clickAndWait(Until.newWindow(), 1000);
        Assert.assertTrue(TestUtils.clickText(device, false, context.getString(R.string.discard), true));
        Assert.assertFalse(map.getGeojsonLayer().isEnabled());
    }

    /**
     * Set the background layer to osm standard style
     */
    @Test
    public void backgroundLayer() {
        UiObject2 menuButton = getLayerButton("OpenStreetMap", MENU_BUTTON);
        menuButton.clickAndWait(Until.newWindow(), 1000);
        Assert.assertTrue(TestUtils.clickText(device, false, context.getString(R.string.layer_select_imagery), true));
        TestUtils.clickText(device, true, context.getString(R.string.none), true);
        Preferences prefs = new Preferences(context);
        Assert.assertEquals(TileLayerServer.LAYER_NONE, prefs.backgroundLayer());
    }

    /**
     * Get one of the buttons for a specific layer
     * 
     * @param layer the name of the layer
     * @param buttonIndex the index of the button in the TableRow
     * @return an UiObject2 for the button in question
     */
    private UiObject2 getLayerButton(@NonNull String layer, int buttonIndex) {
        Assert.assertTrue(TestUtils.clickResource(device, true, "de.blau.android:id/layers", true));
        BySelector bySelector = By.textStartsWith(layer);
        UiObject2 layerName = device.wait(Until.findObject(bySelector), 500);
        UiObject2 tableRow = layerName.getParent();
        List<UiObject2> tableCells = tableRow.getChildren();
        UiObject2 extentButton = tableCells.get(buttonIndex);
        return extentButton;
    }
}
