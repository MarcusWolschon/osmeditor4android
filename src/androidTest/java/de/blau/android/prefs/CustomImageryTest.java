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
import android.view.View;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.Splash;
import de.blau.android.TestUtils;
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

        TestUtils.grantPermissons();
        TestUtils.dismissStartUpDialogs(main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        instrumentation.removeMonitor(monitor);
        main.deleteDatabase(TileLayerDatabase.DATABASE_NAME);
        main.finish();
        instrumentation.waitForIdleSync();
    }

    /**
     * Start prefs, custom imagery, add, load mbtiles
     */
    @Test
    public void customImageryValidMBTiles() {
        try {
            TestUtils.copyFileFromResources("map.mbtiles", "mbtiles");
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        Preferences prefs = new Preferences(main);

        prefs.setBackGroundLayer("NONE");
        main.getMap().setPrefs(main, prefs);

        monitor = instrumentation.addMonitor(PrefEditor.class.getName(), null, false);

        Assert.assertTrue(TestUtils.clickButton("de.blau.android:id/menu_config", true));

        PrefEditor prefEditor = (PrefEditor) instrumentation.waitForMonitorWithTimeout(monitor, 40000); // wait for main

        Assert.assertTrue(TestUtils.clickText(device, false, main.getString(R.string.config_customlayers_title), true));

        Assert.assertTrue(TestUtils.clickButton("de.blau.android:id/add", true));

        Assert.assertTrue(TestUtils.clickButton("de.blau.android:id/file_button", true));

        Assert.assertTrue(TestUtils.clickText(device, false, "mbtiles", true));

        Assert.assertTrue(TestUtils.clickText(device, false, "map.mbtiles", true));

        Assert.assertTrue(TestUtils.findText(device, false, "My Map"));

        Assert.assertTrue(TestUtils.findText(device, false, "57.0527713171221"));

        Assert.assertTrue(TestUtils.clickText(device, false, main.getString(R.string.save_and_set), true));

        Assert.assertTrue(TestUtils.findText(device, false, "My Map"));

        Assert.assertTrue(TestUtils.clickText(device, false, main.getString(R.string.done), true));

        Assert.assertTrue(TestUtils.clickUp(device));

    }
}