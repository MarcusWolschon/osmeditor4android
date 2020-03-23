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
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.rule.ActivityTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.Until;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.SignalHandler;
import de.blau.android.TestUtils;
import de.blau.android.osm.ApiTest;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Way;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerServer;
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
        if (logic != null) {
            logic.deselectAll();
        }
        TestUtils.zoomToLevel(device, main, 18);
        App.getTaskStorage().reset();
    }

    /**
     * Create a new way from menu and clicks at two more locations and finishing via home button, then square
     */
    @SdkSuppress(minSdkVersion=26)
    @Test
    public void square() {
        map.getDataLayer().setVisible(true);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.unlock(device);
        TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/simpleButton", true);
        Assert.assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_add_way), true));
        Assert.assertTrue(TestUtils.findText(device, false, context.getString(R.string.simple_add_way)));
        TestUtils.clickAtCoordinates(device, map, 8.3886384, 47.3892752, true);
        device.waitForIdle(1000);
        Assert.assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_createpath)));
        device.waitForIdle(1000);
        TestUtils.clickAtCoordinates(device, map, 8.3887655, 47.3892752, true);
        device.waitForIdle(1000);
        TestUtils.clickAtCoordinates(device, map, 8.38877, 47.389202, true);
        device.waitForIdle(1000);
        TestUtils.clickUp(device);
        Assert.assertTrue(TestUtils.findText(device, false, context.getString(R.string.tag_form_untagged_element)));
        TestUtils.clickHome(device);
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
        Assert.assertEquals(92.33, theta, 0.1);
        Assert.assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        TestUtils.clickOverflowButton(device);
        TestUtils.clickText(device, false, "Straighten", false);
        device.wait(Until.findObject(By.res(device.getCurrentPackageName() + ":string/Done")), 1000);
        coords = Coordinates.nodeListToCooardinateArray(map.getWidth(), map.getHeight(), map.getViewBox(), way.getNodes());
        v1 = coords[0].subtract(coords[1]);
        v2 = coords[2].subtract(coords[1]);
        theta = Math.toDegrees(Math.acos(Coordinates.dotproduct(v1, v2) / (v1.length() * v2.length())));
        System.out.println("New angle " + theta);
        Assert.assertEquals(90.00, theta, 0.05);
        device.waitForIdle(1000);
        TestUtils.clickUp(device);
    }

    /**
     * Select, remove two nodes
     */
    @SdkSuppress(minSdkVersion=26)
    @Test
    public void removeNodeFromWay() {
        map.getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.clickAtCoordinates(device, map, 8.3893820, 47.3895626, true);
        Assert.assertTrue(TestUtils.clickText(device, false, "Path", false));
        Way way = App.getLogic().getSelectedWay();
        List<Node> origWayNodes = new ArrayList<>(way.getNodes());
        Assert.assertNotNull(way);
        Assert.assertEquals(104148456L, way.getOsmId());
        //
        Assert.assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        Assert.assertTrue(TestUtils.clickOverflowButton(device));
        Assert.assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_remove_node_from_way), true));
        Assert.assertTrue(TestUtils.findText(device, false, context.getString(R.string.menu_remove_node_from_way)));
        // delete an untagged way node somewhere in the middle
        int origSize = way.getNodes().size();
        Node testNode1 = way.getNodes().get(origSize - 4);
        TestUtils.clickAtCoordinatesWaitNewWindow(device, map, testNode1.getLon(), testNode1.getLat());
        Assert.assertEquals(OsmElement.STATE_DELETED, testNode1.getState());
        Assert.assertEquals(origSize - 1, way.getNodes().size());
        // delete the end node that is shared by some other ways
        TestUtils.clickAtCoordinates(device, map, 8.3893820, 47.3895626, true);
        Assert.assertTrue(TestUtils.clickText(device, false, "Path", false));
        Assert.assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        Assert.assertTrue(TestUtils.clickOverflowButton(device));
        Assert.assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_remove_node_from_way), true));
        Assert.assertTrue(TestUtils.findText(device, false, context.getString(R.string.menu_remove_node_from_way)));
        origSize = way.getNodes().size();
        Node testNode2 = way.getLastNode();
        List<Way> ways = logic.getWaysForNode(testNode2);
        Assert.assertEquals(4, ways.size());
        Assert.assertTrue(ways.contains(way));
        TestUtils.clickAtCoordinatesWaitNewWindow(device, map, testNode2.getLon(), testNode2.getLat());
        Assert.assertEquals(OsmElement.STATE_UNCHANGED, testNode2.getState());
        Assert.assertEquals(origSize - 1, way.getNodes().size());
        ways = logic.getWaysForNode(testNode2);
        Assert.assertEquals(3, ways.size());
        Assert.assertFalse(ways.contains(way));
    }
}
