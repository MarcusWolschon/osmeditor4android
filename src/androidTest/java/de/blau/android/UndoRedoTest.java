package de.blau.android;

import java.io.IOException;
import java.io.InputStream;
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
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Way;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerServer;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class UndoRedoTest {

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
        TestUtils.stopEasyEdit(main);
        map.getViewBox().fitToBoundingBox(map, map.getDataLayer().getExtent());
        logic.updateStyle();
        map.getDataLayer().setVisible(true);
        map.invalidate();
        TestUtils.unlock();
        device.waitForWindowUpdate(null, 2000);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        logic.deselectAll();
        TestUtils.zoomToLevel(main, 18);
    }

    /**
     * Change a node, show the undo/redo dialog, undo, redo
     */
    @Test
    public void dialog() {
        TestUtils.clickAtCoordinates(map, 8.38782, 47.390339, true);
        Assert.assertTrue(TestUtils.clickText(device, false, "Toilets", false));
        Node node = App.getLogic().getSelectedNode();
        Assert.assertNotNull(node);
        Assert.assertEquals(3465444349L, node.getOsmId());
        Assert.assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));
        Assert.assertTrue(TestUtils.clickOverflowButton());
        Assert.assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_set_position), true));
        Assert.assertTrue(TestUtils.findText(device, false, "8.3878200"));
        Assert.assertTrue(TestUtils.findText(device, false, "47.3903390"));
        UiObject editText = device.findObject(new UiSelector().clickable(true).textStartsWith("8.3878200"));
        try {
            editText.click(); // NOTE this seems to be necessary
            editText.setText("8.3878100");
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertTrue(TestUtils.clickText(device, false, context.getString(R.string.set), true));
        Assert.assertEquals(OsmElement.STATE_MODIFIED, node.getState());
        Assert.assertEquals((long) (8.3878100 * 1E7D), node.getLon());

        // start undo redo dialog and undo
        Assert.assertTrue(TestUtils.clickMenuButton(context.getString(R.string.undo), true, true));
        Assert.assertTrue(TestUtils.clickText(device, false, "Undo", false));
        Assert.assertTrue(TestUtils.clickTextContains(device, false, "3465444349", true)); // undo
        Assert.assertEquals(OsmElement.STATE_UNCHANGED, node.getState());
        Assert.assertEquals((long) (8.3878200 * 1E7D), node.getLon());

        // start undo redo dialog and redo
        Assert.assertTrue(TestUtils.clickMenuButton(context.getString(R.string.undo), true, true));
        Assert.assertTrue(TestUtils.clickText(device, false, "Redo", false));
        Assert.assertTrue(TestUtils.clickTextContains(device, false, "3465444349", true)); // undo
        Assert.assertEquals(OsmElement.STATE_MODIFIED, node.getState());
        Assert.assertEquals((long) (8.3878100 * 1E7D), node.getLon());
    }
}
