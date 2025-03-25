package io.vespucci.prefs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import android.os.Build;
import android.view.View;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;
import io.vespucci.R;
import io.vespucci.JavaResources;
import io.vespucci.LayerUtils;
import io.vespucci.Main;
import io.vespucci.TestUtils;
import io.vespucci.contract.Paths;
import io.vespucci.layer.LayerDialogTest;
import io.vespucci.prefs.PrefEditor;
import io.vespucci.prefs.Preferences;
import io.vespucci.resources.TileLayerDatabase;
import io.vespucci.util.FileUtil;

/**
 * Note these tests are not mocked
 * 
 * @author simon
 *
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class CustomImageryTest {

    Main            main            = null;
    View            v               = null;
    ActivityMonitor monitor         = null;
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
        monitor = instrumentation.addMonitor(Main.class.getName(), null, false);
        main = mActivityRule.getActivity();
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.stopEasyEdit(main);
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
     * Start prefs, custom imagery, add, load mbtiles
     */
    @Test
    public void customImageryValidMBTiles() {
        File mbtiles = null;
        try {
            mbtiles = JavaResources.copyFileFromResources(main, "map.mbt", null, "mbtiles");
            Preferences prefs = new Preferences(main);
            LayerUtils.removeImageryLayers(main);
            main.getMap().setPrefs(main, prefs);
            monitor = instrumentation.addMonitor(PrefEditor.class.getName(), null, false);
            assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/menu_config", true));
            instrumentation.waitForMonitorWithTimeout(monitor, 40000); // wait for prefs
            assertTrue(TestUtils.clickText(device, false, main.getString(R.string.config_customlayers_title), true, false));
            assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/add", true));
            assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/file_button", true));
            TestUtils.selectFile(device, main, "mbtiles", "map.mbt", true);
            assertTrue(TestUtils.findText(device, false, "My Map"));
            assertTrue(TestUtils.findText(device, false, "57.0527713171221"));
            assertTrue(TestUtils.clickText(device, false, main.getString(R.string.save_and_set), true, false));
            assertTrue(TestUtils.findText(device, false, "My Map"));
            assertTrue(TestUtils.clickText(device, false, main.getString(R.string.done), true, false));
            assertTrue(TestUtils.clickHome(device, true));
            UiObject2 extentButton = TestUtils.getLayerButton(device, "My Map", LayerDialogTest.EXTENT_BUTTON);
            extentButton.clickAndWait(Until.newWindow(), 2000);
            File imported = new File(FileUtil.getPublicDirectory(FileUtil.getPublicDirectory(), Paths.DIRECTORY_PATH_IMPORTS), "map.mbt");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                assertTrue(imported.exists());
                imported.delete();
            } else {
                assertFalse(imported.exists());
            }
        } catch (IOException e) {
            fail(e.getMessage());
        } finally {
            if (mbtiles != null) {
                mbtiles.delete();
            }
        }
    }

    /**
     * Start prefs, custom imagery, add, load invalid mbtiles
     */
    @Test
    public void customImageryInvalidMBTiles() {
        File mbtiles = null;
        try {
            mbtiles = JavaResources.copyFileFromResources(main, "map-no-meta.mbt", null, "mbtiles");
            Preferences prefs = new Preferences(main);
            LayerUtils.removeImageryLayers(main);
            main.getMap().setPrefs(main, prefs);
            monitor = instrumentation.addMonitor(PrefEditor.class.getName(), null, false);
            assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/menu_config", true));
            instrumentation.waitForMonitorWithTimeout(monitor, 40000); // wait for prefs
            assertTrue(TestUtils.clickText(device, false, main.getString(R.string.config_customlayers_title), true, false));
            assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/add", true));
            assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/file_button", true));
            TestUtils.selectFile(device, main, "mbtiles", "map-no-meta.mbt", true);
            UiObject url = device.findObject(new UiSelector().resourceId(device.getCurrentPackageName() + ":id/url"));
            try {
                assertEquals("", url.getText()); // url not set
            } catch (UiObjectNotFoundException e) {
                fail(e.getMessage());
            }
            File imported = new File(FileUtil.getPublicDirectory(FileUtil.getPublicDirectory(), Paths.DIRECTORY_PATH_IMPORTS), "map-no-meta.mbt");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                assertTrue(imported.exists());
                imported.delete();
            } else {
                assertFalse(imported.exists());
            }
        } catch (IOException e) {
            fail(e.getMessage());
        } finally {
            if (mbtiles != null) {
                mbtiles.delete();
            }
        }
    }
}