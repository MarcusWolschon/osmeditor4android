package de.blau.android.prefs;

import java.io.IOException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Intent;
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
import de.blau.android.JavaResources;
import de.blau.android.LayerUtils;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.Splash;
import de.blau.android.TestUtils;
import de.blau.android.layer.LayerDialogTest;
import de.blau.android.resources.TileLayerDatabase;

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
     * Start prefs, custom imagery, add, load mbtiles
     */
    @Test
    public void customImageryValidMBTiles() {
        try {
            JavaResources.copyFileFromResources(main, "map.mbt", null, "mbtiles", false);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        Preferences prefs = new Preferences(main);
        LayerUtils.removeImageryLayers(main);
        main.getMap().setPrefs(main, prefs);
        monitor = instrumentation.addMonitor(PrefEditor.class.getName(), null, false);
        Assert.assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/menu_config", true));
        instrumentation.waitForMonitorWithTimeout(monitor, 40000); // wait for main
        Assert.assertTrue(TestUtils.clickText(device, false, main.getString(R.string.config_customlayers_title), true, false));
        Assert.assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/add", true));
        Assert.assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/file_button", true));
        TestUtils.selectFile(device, main, "mbtiles", "map.mbt", false);
        Assert.assertTrue(TestUtils.findText(device, false, "My Map"));
        Assert.assertTrue(TestUtils.findText(device, false, "57.0527713171221"));
        Assert.assertTrue(TestUtils.clickText(device, false, main.getString(R.string.save_and_set), true, false));
        Assert.assertTrue(TestUtils.findText(device, false, "My Map"));
        Assert.assertTrue(TestUtils.clickText(device, false, main.getString(R.string.done), true, false));
        Assert.assertTrue(TestUtils.clickHome(device, true));
        UiObject2 extentButton = TestUtils.getLayerButton(device, "My Map", LayerDialogTest.EXTENT_BUTTON);
        extentButton.clickAndWait(Until.newWindow(), 2000);
    }

    /**
     * Start prefs, custom imagery, add, load invalid mbtiles
     */
    @Test
    public void customImageryInvalidMBTiles() {
        try {
            JavaResources.copyFileFromResources(main, "map-no-meta.mbt", null, "mbtiles", false);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        Preferences prefs = new Preferences(main);
        LayerUtils.removeImageryLayers(main);
        main.getMap().setPrefs(main, prefs);
        monitor = instrumentation.addMonitor(PrefEditor.class.getName(), null, false);
        Assert.assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/menu_config", true));
        instrumentation.waitForMonitorWithTimeout(monitor, 40000); // wait for main
        Assert.assertTrue(TestUtils.clickText(device, false, main.getString(R.string.config_customlayers_title), true, false));
        Assert.assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/add", true));
        Assert.assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/file_button", true));
        TestUtils.selectFile(device, main, "mbtiles", "map-no-meta.mbt", false);
        UiObject url = device.findObject(new UiSelector().resourceId(device.getCurrentPackageName() + ":id/url"));
        try {
            Assert.assertEquals("", url.getText()); // url not set
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
    }
}