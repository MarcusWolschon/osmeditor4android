package de.blau.android.propertyeditor;

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

import android.app.Activity;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.rule.ActivityTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.UiDevice;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.SignalHandler;
import de.blau.android.TestUtils;
import de.blau.android.osm.ApiTest;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerServer;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AddressTest {

    Context              context         = null;
    AdvancedPrefDatabase prefDB          = null;
    Main                 main            = null;
    UiDevice             device          = null;
    ActivityMonitor      monitor         = null;
    Instrumentation      instrumentation = null;
    Map                  map             = null;
    Logic                logic           = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        instrumentation = InstrumentationRegistry.getInstrumentation();
        monitor = instrumentation.addMonitor(PropertyEditor.class.getName(), null, false);
        main = mActivityRule.getActivity();
        Preferences prefs = new Preferences(context);
        prefs.setBackGroundLayer(TileLayerServer.LAYER_NONE); // try to avoid downloading tiles
        prefs.setOverlayLayer(TileLayerServer.LAYER_NOOVERLAY);
        prefs.enableSimpleActions(false);
        main.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                main.hideSimpleActionsButton();
            }
        });
        map = main.getMap();
        map.setPrefs(main, prefs);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        final CountDownLatch signal1 = new CountDownLatch(1);
        logic = App.getLogic();
        logic.deselectAll();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("test2.osm");
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
        App.getTaskStorage().reset();
        TestUtils.stopEasyEdit(main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        logic.deselectAll();
        TestUtils.zoomToLevel(device, main, 18);
        App.getTaskStorage().reset();
    }

    /**
     * Create a new Node by long click and check that we get correct street suggestion and then correct house number
     * from prediction
     */
    @SdkSuppress(minSdkVersion=26)
    @Test
    public void newAddress() {
        map.getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.longClickAtCoordinates(device, map, 8.3893454, 47.3901898, true);
        Assert.assertTrue(TestUtils.findText(device, false, context.getString(R.string.menu_add)));
        TestUtils.clickMenuButton(device, main.getString(R.string.tag_menu_address), false, true);
        Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        Assert.assertTrue(propertyEditor instanceof PropertyEditor);
        Assert.assertTrue(TestUtils.findText(device, false, "Bergstrasse"));
        Assert.assertTrue(TestUtils.clickMenuButton(device, "More options", false, true));
        TestUtils.clickText(device, false, main.getString(R.string.tag_menu_reset_address_prediction), true);
        Assert.assertTrue(TestUtils.clickMenuButton(device, main.getString(R.string.tag_menu_address), false, true));
        Assert.assertTrue(TestUtils.findText(device, false, "35"));
        TestUtils.clickHome(device);
    }
}
