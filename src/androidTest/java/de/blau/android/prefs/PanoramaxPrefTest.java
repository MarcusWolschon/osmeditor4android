package de.blau.android.prefs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.view.View;
import androidx.core.os.BuildCompat.PrereleaseSdkCheck;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import de.blau.android.LayerUtils;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.TestUtils;
import de.blau.android.prefs.AdvancedPrefDatabase.Geocoder;

/**
 * Note these tests are not mocked
 * 
 * @author simon
 *
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class PanoramaxPrefTest {

    Main              main                = null;
    View              v                   = null;
    ActivityMonitor   monitor             = null;
    Instrumentation   instrumentation     = null;
    UiDevice          device              = null;
    MockWebServerPlus mockInstancesServer = null;

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

        mockInstancesServer = new MockWebServerPlus();

        Preferences prefs = new Preferences(main);
        prefs.setPanoramaxInstancesUrl(mockInstancesServer.url("/"));
        LayerUtils.removeImageryLayers(main);
        Map map = main.getMap();
        map.setPrefs(main, prefs);

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
        try {
            mockInstancesServer.server().shutdown();
        } catch (IOException ioex) {
            System.out.println("Stopping mock webserver exception " + ioex);
        }
    }

    /**
     * Start prefs, advanced prefs, image opts, immagestores
     */
    @Test
    public void instancesTest() {
        mockInstancesServer.enqueue("panoramax_instances");
        monitor = instrumentation.addMonitor(PrefEditor.class.getName(), null, false);
        assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/menu_config", true));
        instrumentation.waitForMonitorWithTimeout(monitor, 40000); //

        if (!TestUtils.scrollToAndSelect(device, main.getString(R.string.config_advancedprefs), new UiSelector().scrollable(true), 0)) {
            fail("Didn't find " + main.getString(R.string.config_advancedprefs));
        }

        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.config_advancedprefs), true));
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.config_advancedprefs)));

        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.config_category_camera_and_images), true, false));
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.config_imageStores_title)));

        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.config_imageStores_title), true, false));
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.manage_image_stores)));

        // assertTrue(TestUtils.clickText(device, false, main.getString(R.string.urldialog_add_geocoder), true, false));
        TestUtils.clickMenuButton(device, main.getString(R.string.urldialog_add_image_store), false, true);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.imagestores_add_from_panoramax_list), true, false));

        assertTrue(TestUtils.clickText(device, false, "IGN", true, false, 20000));
        UiObject name = TestUtils.findObjectWithResourceId(device, false, device.getCurrentPackageName() + ":id/listedit_editName");
        UiObject url = TestUtils.findObjectWithResourceId(device, false, device.getCurrentPackageName() + ":id/listedit_editValue_2");
        try {
            assertEquals("IGN", name.getText());
            assertEquals("https://panoramax.ign.fr/api", url.getText());
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.okay), true, false));
        assertTrue(TestUtils.findText(device, false, "IGN"));
        TestUtils.clickHome(device, false);
        TestUtils.clickHome(device, false);
    }
}