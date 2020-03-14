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
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.support.test.uiautomator.Until;
import android.view.View;
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
            TestUtils.copyFileFromResources("map.mbt", "mbtiles");
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        Preferences prefs = new Preferences(main);
        prefs.setBackGroundLayer("NONE");
        main.getMap().setPrefs(main, prefs);
        monitor = instrumentation.addMonitor(PrefEditor.class.getName(), null, false);
        Assert.assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/menu_config", true));
        instrumentation.waitForMonitorWithTimeout(monitor, 40000); // wait for main
        Assert.assertTrue(TestUtils.clickText(device, false, main.getString(R.string.config_customlayers_title), true));
        Assert.assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/add", true));
        Assert.assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/file_button", true));
        TestUtils.selectFile(device, "mbtiles", "map.mbt");
        Assert.assertTrue(TestUtils.findText(device, false, "My Map"));
        Assert.assertTrue(TestUtils.findText(device, false, "57.0527713171221"));
        Assert.assertTrue(TestUtils.clickText(device, false, main.getString(R.string.save_and_set), true));
        Assert.assertTrue(TestUtils.findText(device, false, "My Map"));
        Assert.assertTrue(TestUtils.clickText(device, false, main.getString(R.string.done), true));
        Assert.assertTrue(TestUtils.clickHome(device));
        UiObject2 extentButton = TestUtils.getLayerButton(device, "My Map", LayerDialogTest.EXTENT_BUTTON);
        extentButton.clickAndWait(Until.newWindow(), 2000);
        Assert.assertTrue(main.getMap().getBackgroundLayer().getTileProvider().connected()); // check that the service
                                                                                             // // is still running
    }

    /**
     * Start prefs, custom imagery, add, load invalid mbtiles
     */
    @Test
    public void customImageryInvalidMBTiles() {
        try {
            TestUtils.copyFileFromResources("map-no-meta.mbt", "mbtiles");
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        Preferences prefs = new Preferences(main);
        prefs.setBackGroundLayer("NONE");
        main.getMap().setPrefs(main, prefs);
        monitor = instrumentation.addMonitor(PrefEditor.class.getName(), null, false);
        Assert.assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/menu_config", true));
        instrumentation.waitForMonitorWithTimeout(monitor, 40000); // wait for main
        Assert.assertTrue(TestUtils.clickText(device, false, main.getString(R.string.config_customlayers_title), true));
        Assert.assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/add", true));
        Assert.assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/file_button", true));
        TestUtils.selectFile(device, "mbtiles", "map-no-meta.mbt");
        UiObject url = device.findObject(new UiSelector().resourceId(device.getCurrentPackageName() + ":id/url"));
        try {
            Assert.assertEquals("", url.getText()); // url not set
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
    }
}