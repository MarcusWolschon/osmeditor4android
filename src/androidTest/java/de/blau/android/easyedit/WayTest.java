package de.blau.android.easyedit;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
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
public class WayTest {

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
     * Select, show info dialog, delete (check that nodes are deleted), undelete
     */
    @Test
    public void selectWay() {
        map.getDataLayer().setVisible(true);
        TestUtils.unlock();
        TestUtils.zoomToLevel(main, 21);
        TestUtils.clickAtCoordinates(map, 8.3893820, 47.3895626, true);
        Assert.assertTrue(TestUtils.clickText(device, false, "Path", false));
        Way way = App.getLogic().getSelectedWay();
        List<Node>origWayNodes = new ArrayList<>(way.getNodes());
        Assert.assertNotNull(way);
        Assert.assertEquals(104148456L, way.getOsmId());
        // add some tags to way nodes so that we can check if they get deleted properly
        Node shouldBeDeleted1 = (Node) App.getDelegator().getOsmElement(Node.NAME, 1201766241L);
        Assert.assertNotNull(shouldBeDeleted1);
        java.util.Map<String,String> tags = new HashMap<>();
        tags.put("created_by", "vespucci test");
        logic.setTags(main, shouldBeDeleted1, tags);
        Node shouldntBeDeleted1 = (Node) App.getDelegator().getOsmElement(Node.NAME, 635762224L);
        Assert.assertNotNull(shouldntBeDeleted1);
        tags = new HashMap<>();
        tags.put("shop", "vespucci test");
        logic.setTags(main, shouldntBeDeleted1, tags);
        //
        Assert.assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        Assert.assertTrue(TestUtils.clickOverflowButton());
        Assert.assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_information), true));
        Assert.assertTrue(TestUtils.findText(device, false, "asphalt"));
        Assert.assertTrue(TestUtils.clickText(device, false, context.getString(R.string.done), true));
        Assert.assertTrue(TestUtils.clickOverflowButton());
        Assert.assertTrue(TestUtils.clickText(device, false, context.getString(R.string.delete), true));
        Assert.assertTrue(TestUtils.clickText(device, false, context.getString(R.string.deleteway_wayandnodes), true));
        Assert.assertEquals(OsmElement.STATE_DELETED, way.getState());
        Assert.assertEquals(OsmElement.STATE_DELETED, shouldBeDeleted1.getState());
        Node shouldBeDeleted2 = (Node) App.getDelegator().getOsmElement(Node.NAME, 635762221);
        Assert.assertEquals(OsmElement.STATE_DELETED, shouldBeDeleted2.getState());
        Assert.assertEquals(OsmElement.STATE_MODIFIED, shouldntBeDeleted1.getState());
        Node shouldntBeDeleted2 = (Node) App.getDelegator().getOsmElement(Node.NAME, 1201766174);
        Assert.assertEquals(OsmElement.STATE_UNCHANGED, shouldntBeDeleted2.getState());
        // undo
        Assert.assertTrue(TestUtils.clickMenuButton(context.getString(R.string.undo), false, true));
        Assert.assertEquals(OsmElement.STATE_UNCHANGED, way.getState());
        Assert.assertTrue(way.hasParentRelation(6490362L));
        List<Node>nodes = way.getNodes();
        Assert.assertEquals(origWayNodes.size(), nodes.size());
        for (int i=0;i<nodes.size();i++) {
            Assert.assertEquals(origWayNodes.get(i),nodes.get(i));
        }
    }
}
