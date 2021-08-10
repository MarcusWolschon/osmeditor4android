package de.blau.android.layer.mvt;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Intent;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.EventCondition;
import androidx.test.uiautomator.SearchCondition;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiObject2Condition;
import androidx.test.uiautomator.Until;
import de.blau.android.App;
import de.blau.android.JavaResources;
import de.blau.android.LayerUtils;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.Splash;
import de.blau.android.TestUtils;
import de.blau.android.contract.Ui;
import de.blau.android.layer.LayerDialogTest;
import de.blau.android.layer.LayerType;
import de.blau.android.layer.MapViewLayer;
import de.blau.android.prefs.PrefEditor;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerDatabase;
import de.blau.android.util.mvt.VectorTileRenderer;
import de.blau.android.util.mvt.style.Layer;
import de.blau.android.util.mvt.style.Style;

/**
 * Note these tests are not mocked
 * 
 * @author simon
 *
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MvtTest {

    private static final String TILEMAKER             = "Tilemaker";
    private static final String LIECHTENSTEIN_MBTILES = "liechtenstein.mbtiles";

    Main            main            = null;
    View            v               = null;
    Splash          splash          = null;
    ActivityMonitor monitor         = null;
    Instrumentation instrumentation = null;
    UiDevice        device          = null;

    /**
     * Manual start of activity so that we can set up the monitor for main
     */
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

        main = (Main) instrumentation.waitForMonitorWithTimeout(monitor, 40000); // wait for main

        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        instrumentation.removeMonitor(monitor);
        if (main != null) {
            main.deleteDatabase(TileLayerDatabase.DATABASE_NAME);
            main.finish();
        } else {
            System.out.println("main is null");
        }
        instrumentation.waitForIdleSync();
    }

    /**
     * Start prefs, custom imagery, add, load mbtiles, change style, load style
     */
    @Test
    public void mvtMBTilesTest() {
        try {
            JavaResources.copyFileFromResources(main, LIECHTENSTEIN_MBTILES, null, "mbtiles");
            JavaResources.copyFileFromResources(main, "osm-liberty.json", null, "mbtiles");
            JavaResources.copyFileFromResources(main, "osm-liberty-sprite.json", null, "mbtiles");
            JavaResources.copyFileFromResources(main, "osm-liberty-sprite.png", null, "mbtiles");
        } catch (IOException e) {
            fail(e.getMessage());
        }
        Preferences prefs = new Preferences(main);
        LayerUtils.removeImageryLayers(main);
        main.getMap().setPrefs(main, prefs);
        monitor = instrumentation.addMonitor(PrefEditor.class.getName(), null, false);
        assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/menu_config", true));
        instrumentation.waitForMonitorWithTimeout(monitor, 40000); // wait for main
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.config_customlayers_title), true, false));
        assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/add", true));
        assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/file_button", true));
        TestUtils.selectFile(device, main, "mbtiles", LIECHTENSTEIN_MBTILES, false);
        assertTrue(TestUtils.findText(device, false, TILEMAKER));
        assertTrue(TestUtils.findText(device, false, "47.271280"));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.save_and_set), true, false));
        assertTrue(TestUtils.findText(device, false, TILEMAKER));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.done), true, false));
        assertTrue(TestUtils.clickHome(device, true));
        // go to somewhere in Vaduz
        Map map = App.getLogic().getMap();
        UiObject2 extentButton = TestUtils.getLayerButton(device, TILEMAKER, LayerDialogTest.EXTENT_BUTTON);
        extentButton.clickAndWait(Until.newWindow(), 2000);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 20);
        TestUtils.clickAtCoordinates(device, map, 9.5104, 47.1561619);
        assertTrue(TestUtils.clickTextContains(device, false, "housenumber", true));
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.vt_feature_information), 1000));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.Done), true));
        UiObject2 menuButton = TestUtils.getLayerButton(device, TILEMAKER, LayerDialogTest.MENU_BUTTON);
        menuButton.clickAndWait(Until.newWindow(), 2000);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.layer_change_style), true));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.okay), true));
        menuButton.clickAndWait(Until.newWindow(), 2000);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.layer_load_style), true));
        TestUtils.selectFile(device, main, "mbtiles", "osm-liberty.json", false);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.Done), true));
        // check that we actually loaded something
        de.blau.android.layer.mvt.MapOverlay layer = (MapOverlay) map.getLayer(LayerType.IMAGERY, "TILEMAKERTOOPENMAPTILESSCHEMA");
        assertNotNull(layer);
        Style style = layer.getStyle();
        assertNotNull(style);
        Layer taxiway = null;
        for (Layer l : style.getLayers()) {
            if ("aeroway_taxiway".equals(l.getId())) {
                taxiway = l;
                break;
            }
        }
        assertNotNull(taxiway);
    }
}