package de.blau.android;

import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.TestUtils;
import de.blau.android.osm.ViewBox;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerServer;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class CoordinatesOLCTest {

    public static final int TIMEOUT    = 90;
    MockWebServerPlus       mockServer = null;
    Context                 context    = null;
    AdvancedPrefDatabase    prefDB     = null;
    Main                    main       = null;
    UiDevice                device     = null;
    Map                     map        = null;
    Logic                   logic      = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        main = mActivityRule.getActivity();
        Preferences prefs = new Preferences(context);
        prefs.setBackGroundLayer(TileLayerServer.LAYER_NONE); // try to avoid downloading tiles
        prefs.setOverlayLayer(TileLayerServer.LAYER_NOOVERLAY);
        main.getMap().setPrefs(main, prefs);

        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        final CountDownLatch signal1 = new CountDownLatch(1);
        logic = App.getLogic();
        logic.deselectAll();
        TestUtils.stopEasyEdit(main);
        map = logic.getMap();
        logic.updateStyle();
        map.getDataLayer().setVisible(true);
        map.invalidate();
        TestUtils.unlock(device);
        device.waitForWindowUpdate(null, 2000);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        logic.deselectAll();
        TestUtils.zoomToLevel(device, main, 18);
    }

    /**
     * Go to a coordinate pair
     */
    @Test
    public void coordinates() {
        Assert.assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/menu_gps", true));
        Assert.assertTrue(TestUtils.clickText(device, false, "Go to coordinates", true));
        UiObject coordEditText = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/text_line_edit"));
        try {
            coordEditText.click();
            coordEditText.setText("0.1 / 0.2");
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
        TestUtils.clickButton(device, "android:id/button1", true);
        ViewBox box = map.getViewBox();
        double[] center = box.getCenter();
        Assert.assertEquals(0.2D, center[0], 0.01);
        Assert.assertEquals(0.1D, center[1], 0.01);
    }

    /**
     * Search for multiple objects
     */
    @Test
    public void OLC() {
        Assert.assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/menu_gps", true));
        Assert.assertTrue(TestUtils.clickText(device, false, "Go to coordinates", true));
        UiObject coordEditText = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/text_line_edit"));
        try {
            coordEditText.click();
            coordEditText.setText("6PH57VP3+PR6");
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
        TestUtils.clickButton(device, "android:id/button1", true);
        ViewBox box = map.getViewBox();
        double[] center = box.getCenter();
        Assert.assertEquals(103.85452D, center[0], 0.01);
        Assert.assertEquals(1.28679D, center[1], 0.01);
    }
}
