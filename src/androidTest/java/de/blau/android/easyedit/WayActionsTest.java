package de.blau.android.easyedit;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

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
import de.blau.android.R;
import de.blau.android.SignalHandler;
import de.blau.android.TestUtils;
import de.blau.android.osm.ApiTest;
import de.blau.android.osm.Node;
import de.blau.android.osm.Way;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.tasks.Note;
import de.blau.android.tasks.Task;
import de.blau.android.util.Coordinates;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class WayActionsTest {

    Context              context = null;
    AdvancedPrefDatabase prefDB  = null;
    Main                 main    = null;
    UiDevice             device  = null;
    Map                  map     = null;
    Logic                logic   = null;

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
        prefs.enableSimpleActions(true);
        main.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                main.showSimpleActionsButton();
            }
        });

        map = main.getMap();
        map.setPrefs(main, prefs);
        TestUtils.grantPermissons();
        TestUtils.dismissStartUpDialogs(main);
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
        if (logic != null) {
            logic.deselectAll();
        }
        TestUtils.zoomToLevel(main, 18);
        App.getTaskStorage().reset();
    }

    /**
     * Create a new way from menu and clicks at two more locations and finishing via home button, then square
     */
    @Test
    public void square() {
        map.getDataLayer().setVisible(true);
        TestUtils.zoomToLevel(main, 21);
        TestUtils.unlock();
        TestUtils.clickButton("de.blau.android:id/simpleButton", true);
        Assert.assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_add_way), true));
        Assert.assertTrue(TestUtils.findText(device, false, context.getString(R.string.simple_add_way)));
        TestUtils.clickAtCoordinates(map, 8.3886384, 47.3892752, true);
        device.waitForIdle(1000);
        Assert.assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_createpath)));
        device.waitForIdle(1000);
        TestUtils.clickAtCoordinates(map, 8.3887655, 47.3892752, false);
        device.waitForIdle(1000);
        TestUtils.clickAtCoordinates(map, 8.38877, 47.389202, false);
        device.waitForIdle(1000);
        TestUtils.clickHome(device);
        Assert.assertTrue(TestUtils.findText(device, false, context.getString(R.string.tag_form_unknown_element)));
        TestUtils.clickUp(device);
        device.waitForIdle(1000);
        Way way = App.getLogic().getSelectedWay();
        Assert.assertNotNull(way);
        Assert.assertTrue(way.getOsmId() < 0);
        Assert.assertEquals(3, way.nodeCount());
        Coordinates[] coords = Coordinates.nodeListToCooardinateArray(map.getWidth(), map.getHeight(), map.getViewBox(), way.getNodes());
        Coordinates v1 = coords[0].subtract(coords[1]);
        Coordinates v2 = coords[2].subtract(coords[1]);
        double theta = Math.toDegrees(Math.acos(Coordinates.dotproduct(v1, v2) / (v1.length() * v2.length())));
        System.out.println("Original angle " + theta);
        Assert.assertEquals(92.45, theta, 0.1);
        Assert.assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        TestUtils.clickOverflowButton();
        TestUtils.clickText(device, false, "Square way", false);
        coords = Coordinates.nodeListToCooardinateArray(map.getWidth(), map.getHeight(), map.getViewBox(), way.getNodes());
        v1 = coords[0].subtract(coords[1]);
        v2 = coords[2].subtract(coords[1]);
        theta = Math.toDegrees(Math.acos(Coordinates.dotproduct(v1, v2) / (v1.length() * v2.length())));
        System.out.println("New angle " + theta);
        Assert.assertEquals(90.01, theta, 0.01);
        device.waitForIdle(1000);
        TestUtils.clickHome(device);
    }
}
