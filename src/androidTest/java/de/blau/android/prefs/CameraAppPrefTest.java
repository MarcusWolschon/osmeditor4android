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
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.Splash;
import de.blau.android.TestUtils;
import de.blau.android.layer.LayerDialogTest;
import de.blau.android.layer.LayerType;
import de.blau.android.resources.TileLayerDatabase;

/**
 * Note these tests are not mocked
 * 
 * @author simon
 *
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class CameraAppPrefTest {

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
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        main = mActivityRule.getActivity();
        Preferences prefs = new Preferences(main);
        LayerUtils.removeImageryLayers(main);
        Map map = main.getMap();
        map.setPrefs(main, prefs);

        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        instrumentation.removeMonitor(monitor);
        instrumentation.waitForIdleSync();
    }

    /**
     * Start prefs, advanced prefs
     */
    @Test
    public void dummyCameraApp() {
        monitor = instrumentation.addMonitor(PrefEditor.class.getName(), null, false);
        Assert.assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/menu_config", true));
        instrumentation.waitForMonitorWithTimeout(monitor, 40000); //

        TestUtils.scrollToAndSelect(device, main.getString(R.string.config_advancedprefs), new UiSelector().scrollable(true));

        Assert.assertTrue(TestUtils.clickText(device, false, main.getString(R.string.config_advancedprefs), true));
        Assert.assertTrue(TestUtils.findText(device, false, main.getString(R.string.config_advancedprefs)));

        Assert.assertTrue(TestUtils.clickText(device, false, main.getString(R.string.config_category_view), true, false));
        Assert.assertTrue(TestUtils.findText(device, false, main.getString(R.string.config_category_view)));

        Assert.assertTrue(TestUtils.clickText(device, false, main.getString(R.string.config_selectCameraApp_title), true, false));

        Assert.assertTrue(TestUtils.clickText(device, false, "Dummy camera", true, false));
        Assert.assertTrue(TestUtils.findText(device, false, "Dummy camera"));

        Assert.assertTrue(TestUtils.clickText(device, false, main.getString(R.string.config_selectCameraApp_title), true, false));
        Assert.assertTrue(TestUtils.clickText(device, false, main.getString(R.string.config_system_camera), true, false));
        Assert.assertTrue(TestUtils.findText(device, false, main.getString(R.string.config_system_camera)));
    }
}