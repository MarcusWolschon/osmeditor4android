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
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerSource;

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
        prefDB = new AdvancedPrefDatabase(context);
        prefDB.deleteAPI("Test");
        prefDB.addAPI("Test", "Test", "", null, null, "user", "pass", false);
        prefDB.selectAPI("Test");
        Preferences prefs = new Preferences(context);
        prefs.setBackGroundLayer(TileLayerSource.LAYER_NONE); // try to avoid downloading tiles
        prefs.setOverlayLayer(TileLayerSource.LAYER_NOOVERLAY);
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
        TestUtils.stopEasyEdit(main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        TestUtils.stopEasyEdit(main);
        TestUtils.zoomToLevel(device, main, 18);
        prefDB.selectAPI(AdvancedPrefDatabase.ID_DEFAULT);
    }

    /**
     * Select, show info dialog, delete (check that nodes are deleted), undelete
     */
    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void selectWay() {
        map.getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.clickAtCoordinates(device, map, 8.3893820, 47.3895626, true);
        Assert.assertTrue(TestUtils.clickText(device, false, "Path", false, false));
        Way way = App.getLogic().getSelectedWay();
        Assert.assertNotNull(way);
        List<Node> origWayNodes = new ArrayList<>(way.getNodes());
        Assert.assertEquals(104148456L, way.getOsmId());
        // add some tags to way nodes so that we can check if they get deleted properly
        Node shouldBeDeleted1 = (Node) App.getDelegator().getOsmElement(Node.NAME, 1201766241L);
        Assert.assertNotNull(shouldBeDeleted1);
        java.util.Map<String, String> tags = new HashMap<>();
        tags.put("created_by", "vespucci test");
        logic.setTags(main, shouldBeDeleted1, tags);
        Node shouldntBeDeleted1 = (Node) App.getDelegator().getOsmElement(Node.NAME, 635762224L);
        Assert.assertNotNull(shouldntBeDeleted1);
        tags = new HashMap<>();
        tags.put("shop", "vespucci test");
        logic.setTags(main, shouldntBeDeleted1, tags);
        //
        Assert.assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        Assert.assertTrue(TestUtils.clickOverflowButton(device));
        String menuInfo = context.getString(R.string.menu_information);
        TestUtils.scrollTo(menuInfo);
        Assert.assertTrue(TestUtils.clickText(device, false, menuInfo, true, false));
        Assert.assertTrue(TestUtils.findText(device, false, "asphalt"));
        Assert.assertTrue(TestUtils.clickText(device, false, context.getString(R.string.done), true, false));
        Assert.assertTrue(TestUtils.clickOverflowButton(device));
        String menuDelete = context.getString(R.string.delete);
        TestUtils.scrollTo(menuDelete);
        Assert.assertTrue(TestUtils.clickText(device, false, menuDelete, true, false));
        Assert.assertTrue(TestUtils.clickText(device, false, context.getString(R.string.deleteway_wayandnodes), true, false));
        Assert.assertEquals(OsmElement.STATE_DELETED, way.getState());
        Assert.assertEquals(OsmElement.STATE_DELETED, shouldBeDeleted1.getState());
        Node shouldBeDeleted2 = (Node) App.getDelegator().getOsmElement(Node.NAME, 635762221);
        Assert.assertEquals(OsmElement.STATE_DELETED, shouldBeDeleted2.getState());
        Assert.assertEquals(OsmElement.STATE_MODIFIED, shouldntBeDeleted1.getState());
        Node shouldntBeDeleted2 = (Node) App.getDelegator().getOsmElement(Node.NAME, 1201766174);
        Assert.assertEquals(OsmElement.STATE_UNCHANGED, shouldntBeDeleted2.getState());
        // undo
        Assert.assertTrue(TestUtils.clickMenuButton(device, context.getString(R.string.undo), false, false));
        Assert.assertEquals(OsmElement.STATE_UNCHANGED, way.getState());
        TestUtils.clickText(device, true, context.getString(R.string.okay), true, false); // for the tip alert
        Assert.assertTrue(way.hasParentRelation(6490362L));
        Assert.assertEquals(1, way.getParentRelations().size());
        List<Node> nodes = way.getNodes();
        Assert.assertEquals(origWayNodes.size(), nodes.size());
        for (int i = 0; i < nodes.size(); i++) {
            Assert.assertEquals(origWayNodes.get(i), nodes.get(i));
        }
    }

    /**
     * Select, extract segment, turn into a bridge
     */
    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void extractSegment() {
        map.getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.clickAtCoordinates(device, map, 8.3893820, 47.3895626, true);
        Assert.assertTrue(TestUtils.clickText(device, false, "Path", false, false));
        Way way = App.getLogic().getSelectedWay();
        Assert.assertNotNull(way);
        Assert.assertEquals(104148456L, way.getOsmId());

        Assert.assertTrue(TestUtils.clickOverflowButton(device));
        TestUtils.scrollTo("Extract segment");
        Assert.assertTrue(TestUtils.clickText(device, false, "Extract segment", true, false));
        Assert.assertTrue(TestUtils.findText(device, false, "Select segment"));

        TestUtils.clickAtCoordinates(device, map, 8.3893820, 47.3895626, true);
        Assert.assertTrue(TestUtils.findText(device, false, "Set tags"));
        Assert.assertTrue(TestUtils.clickMenuButton(device, context.getString(R.string.bridge), false, true));

        // should be in property editor now
        Assert.assertTrue(TestUtils.findText(device, false, "Bridge"));
        Assert.assertTrue(TestUtils.clickHome(device, true));

        Way segment = logic.getSelectedWay();
        Assert.assertNotNull(segment);
        Assert.assertEquals(2, segment.nodeCount());
        Assert.assertTrue(segment.hasTag(Tags.KEY_BRIDGE, Tags.VALUE_YES));
        Assert.assertTrue(segment.hasTag(Tags.KEY_LAYER, "1"));
    }

    /**
     * Select, drag way handle, try to upload, undo
     */
    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void geometryImprovement() {
        map.getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.clickAtCoordinates(device, map, 8.3893820, 47.3895626, true);
        Assert.assertTrue(TestUtils.clickText(device, false, "Path", false, false));
        Way way = App.getLogic().getSelectedWay();
        List<Node> origWayNodes = new ArrayList<>(way.getNodes());
        Assert.assertNotNull(way);
        Assert.assertEquals(104148456L, way.getOsmId());
        Assert.assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));

        int originalNodeCount = App.getDelegator().getApiNodeCount();

        // drag a handle
        TestUtils.drag(device, map, 8.3893800, 47.389559, 8.38939, 47.389550, false, 10);
        Assert.assertEquals(origWayNodes.size() + 1, way.getNodes().size());
        Assert.assertEquals(originalNodeCount + 1, App.getDelegator().getApiNodeCount());

        // try to upload the way, check that we have two elements and then cancel
        Assert.assertTrue(TestUtils.clickOverflowButton(device));
        TestUtils.scrollTo("Upload element");
        Assert.assertTrue(TestUtils.clickText(device, false, "Upload element", true, true));
        Assert.assertTrue(TestUtils.findText(device, false, "Upload these 2 changes?"));
        Assert.assertTrue(TestUtils.clickText(device, false, "NO", true, true));

        // undo
        Assert.assertTrue(TestUtils.clickMenuButton(device, context.getString(R.string.undo), false, false));
        Assert.assertEquals(OsmElement.STATE_UNCHANGED, way.getState());
        TestUtils.clickText(device, true, context.getString(R.string.okay), true, false); // for the tip alert
        List<Node> nodes = way.getNodes();
        Assert.assertEquals(origWayNodes.size(), nodes.size());
        for (int i = 0; i < nodes.size(); i++) {
            Assert.assertEquals(origWayNodes.get(i), nodes.get(i));
        }
        Assert.assertEquals(originalNodeCount, App.getDelegator().getApiNodeCount());
    }
}
