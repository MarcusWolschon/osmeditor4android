package de.blau.android.easyedit;

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
public class NodeTest {

    Context              context = null;
    AdvancedPrefDatabase prefDB  = null;
    Main                 main    = null;
    UiDevice             device  = null;
    Map                  map     = null;
    Logic                logic   = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

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

    @After
    public void teardown() {
        logic.deselectAll();
        TestUtils.zoomToLevel(main, 18);
    }

    /**
     * Select, show info dialog, show position dialog, delete, undelete
     */
    @Test
    public void selectNode() {
        TestUtils.clickAtCoordinates(map, 8.38782, 47.390339, true);
        Assert.assertTrue(TestUtils.clickText(device, false, "Toilets", false));
        Node node = App.getLogic().getSelectedNode();
        Assert.assertNotNull(node);
        Assert.assertEquals(3465444349L, node.getOsmId());
        Assert.assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));
        Assert.assertTrue(TestUtils.clickOverflowButton());
        Assert.assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_information), true));
        Assert.assertTrue(TestUtils.findText(device, false, "permissive"));
        Assert.assertTrue(TestUtils.clickText(device, false, context.getString(R.string.done), true));
        Assert.assertTrue(TestUtils.clickOverflowButton());
        Assert.assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_set_position), true));
        Assert.assertTrue(TestUtils.findText(device, false, "8.3878200"));
        Assert.assertTrue(TestUtils.findText(device, false, "47.3903390"));
        Assert.assertTrue(TestUtils.clickText(device, false, context.getString(R.string.cancel), true));
        Assert.assertTrue(TestUtils.clickMenuButton(context.getString(R.string.delete)));
        Assert.assertEquals(OsmElement.STATE_DELETED, node.getState());
        Assert.assertTrue(TestUtils.clickMenuButton(context.getString(R.string.undo)));
        Assert.assertEquals(OsmElement.STATE_UNCHANGED, node.getState());
    }
    
    /**
     * Select, drag, undo
     */
    @Test
    public void dragNode() {
        TestUtils.clickAtCoordinates(map, 8.38782, 47.390339, true);
        Assert.assertTrue(TestUtils.clickText(device, false, "Toilets", true));
        Node node = App.getLogic().getSelectedNode();
        Assert.assertNotNull(node);
        Assert.assertEquals(3465444349L, node.getOsmId());
        Assert.assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));
        
        TestUtils.drag(map, 8.38782, 47.390339, 8.388, 47.391, true);
 
        Assert.assertEquals(OsmElement.STATE_MODIFIED, node.getState());
        
        Assert.assertEquals(8.388, node.getLon()/1E7D,0.001);
        Assert.assertEquals(47.391,node.getLat()/1E7D,0.001);
        
        Assert.assertTrue(TestUtils.clickMenuButton(context.getString(R.string.undo)));
        Assert.assertEquals(OsmElement.STATE_UNCHANGED, node.getState());
    }
    
    /**
     * Select, unjoin, merge
     */
    @Test
    public void unjoinMergeWays() {
        TestUtils.zoomToLevel(main, 21);
        TestUtils.clickAtCoordinates(map, 8.3874926, 47.3884640, true);
        Assert.assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));
        Node node = App.getLogic().getSelectedNode();
        Assert.assertNotNull(node);
        Assert.assertEquals(633468419L, node.getOsmId());
        
        List<Way>waysBefore = logic.getWaysForNode(node);
        Assert.assertEquals(2, waysBefore.size());
        Assert.assertTrue(TestUtils.clickMenuButton(context.getString(R.string.menu_unjoin)));
        
        List<Way>waysUnjoined = logic.getWaysForNode(node);
        Assert.assertEquals(1, waysUnjoined.size());
        Way unjoinedWay = waysUnjoined.get(0);
        Assert.assertEquals(OsmElement.STATE_UNCHANGED, node.getState());
        Assert.assertEquals(OsmElement.STATE_UNCHANGED, unjoinedWay.getState());
        
        TestUtils.clickAtCoordinates(map, 8.3874926, 47.3884640, false);
        Assert.assertTrue(TestUtils.clickText(device, false, "node", false)); // the first node in the list
        Assert.assertTrue(TestUtils.clickMenuButton(context.getString(R.string.menu_merge)));
        
        List<Way>waysAfter = logic.getWaysForNode(node);
        Assert.assertEquals(2, waysAfter.size());
    }
}
