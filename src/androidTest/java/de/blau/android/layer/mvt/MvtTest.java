package de.blau.android.layer.mvt;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.view.View;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;
import de.blau.android.App;
import de.blau.android.JavaResources;
import de.blau.android.LayerUtils;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.TestUtils;
import de.blau.android.layer.LayerDialogTest;
import de.blau.android.layer.LayerType;
import de.blau.android.prefs.PrefEditor;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerDatabase;
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
    Instrumentation instrumentation = null;
    UiDevice        device          = null;

    /**
     * Manual start of activity so that we can set up the monitor for main
     */
    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        main = mActivityRule.getActivity();

        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.zoomToNullIsland(App.getLogic(), main.getMap());
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
        instrumentation.waitForIdleSync();
    }

    /**
     * Start prefs, custom imagery, add, load mbtiles, change style, load style
     */
    @Test
    public void mvtMBTilesTest() {
        try {
            File tiles = JavaResources.copyFileFromResources(main, LIECHTENSTEIN_MBTILES, null, "mbtiles");
            File styleFile = JavaResources.copyFileFromResources(main, "osm-liberty.json", null, "mbtiles");
            File spriteJson = JavaResources.copyFileFromResources(main, "osm-liberty-sprite.json", null, "mbtiles");
            File spritePng = JavaResources.copyFileFromResources(main, "osm-liberty-sprite.png", null, "mbtiles");
            ActivityMonitor monitor = null;
            try {
                Preferences prefs = new Preferences(main);
                LayerUtils.removeImageryLayers(main);
                main.getMap().setPrefs(main, prefs);
                monitor = instrumentation.addMonitor(PrefEditor.class.getName(), null, false);
                assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/menu_config", true));
                instrumentation.waitForMonitorWithTimeout(monitor, 40000); // wait for main
                assertTrue(TestUtils.clickText(device, false, main.getString(R.string.config_customlayers_title), true, false));
                assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/add", true));
                assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/file_button", true));
                TestUtils.selectFile(device, main, "mbtiles", LIECHTENSTEIN_MBTILES, true);
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
                TestUtils.selectFile(device, main, "mbtiles", "osm-liberty.json", true);
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
            } finally {
                if (tiles != null) {
                    tiles.delete();
                }
                if (styleFile != null) {
                    styleFile.delete();
                }
                if (spriteJson != null) {
                    spriteJson.delete();
                }
                if (spritePng != null) {
                    spritePng.delete();
                }
                if (monitor != null) {
                    instrumentation.removeMonitor(monitor);
                }
            }
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Load a Mapbox-GL file with a source config and check if it creates a layer entry
     */
    @Test
    public void mvtFromStyleTest() {
        try {
            File rob = JavaResources.copyFileFromResources(main, "rob.json", null, "/");
            try {
                Preferences prefs = new Preferences(main);
                LayerUtils.removeImageryLayers(main);
                main.getMap().setPrefs(main, prefs);
                assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/layers", true));
                assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/add", true));
                assertTrue(TestUtils.clickText(device, false, main.getString(R.string.layer_add_layer_from_mvt_style), false));
                TestUtils.selectFile(device, main, null, "rob.json", true);
                assertTrue(TestUtils.findText(device, false, "postcodes-source", 10000));
            } finally {
                if (rob != null) {
                    rob.delete();
                }
            }
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }
}