package de.blau.android.easyedit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import de.blau.android.App;
import de.blau.android.LayerUtils;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.TestUtils;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;

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
        LayerUtils.removeImageryLayers(context);
        map = main.getMap();
        map.setPrefs(main, prefs);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        logic = App.getLogic();
        logic.deselectAll();
        TestUtils.loadTestData(main, "test2.osm");
        TestUtils.stopEasyEdit(main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        TestUtils.stopEasyEdit(main);
        TestUtils.zoomToNullIsland(logic, map);
        prefDB.selectAPI(AdvancedPrefDatabase.ID_DEFAULT);
        prefDB.close();
    }

    /**
     * Select, show info dialog, delete (check that nodes are deleted), undelete
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void selectWay() {
        map.getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.clickAtCoordinates(device, map, 8.3893820, 47.3895626, true);
        assertTrue(TestUtils.clickText(device, false, "Path", false, false));
        Way way = App.getLogic().getSelectedWay();
        assertNotNull(way);
        List<Node> origWayNodes = new ArrayList<>(way.getNodes());
        assertEquals(104148456L, way.getOsmId());
        // add some tags to way nodes so that we can check if they get deleted properly
        Node shouldBeDeleted1 = (Node) App.getDelegator().getOsmElement(Node.NAME, 1201766241L);
        assertNotNull(shouldBeDeleted1);
        java.util.Map<String, String> tags = new HashMap<>();
        tags.put("created_by", "vespucci test");
        logic.setTags(main, shouldBeDeleted1, tags);
        Node shouldntBeDeleted1 = (Node) App.getDelegator().getOsmElement(Node.NAME, 635762224L);
        assertNotNull(shouldntBeDeleted1);
        tags = new HashMap<>();
        tags.put("shop", "vespucci test");
        logic.setTags(main, shouldntBeDeleted1, tags);
        //
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        assertTrue(TestUtils.clickOverflowButton(device));
        String menuInfo = context.getString(R.string.menu_information);
        TestUtils.scrollTo(menuInfo, false);
        assertTrue(TestUtils.clickText(device, false, menuInfo, true, false));
        assertTrue(TestUtils.findText(device, false, "asphalt"));
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.done), true, false));
        assertTrue(TestUtils.clickOverflowButton(device));
        String menuDelete = context.getString(R.string.delete);
        TestUtils.scrollTo(menuDelete, false);
        assertTrue(TestUtils.clickText(device, false, menuDelete, true, false));
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.deleteway_wayandnodes), true, false));
        assertEquals(OsmElement.STATE_DELETED, way.getState());
        assertEquals(OsmElement.STATE_DELETED, shouldBeDeleted1.getState());
        Node shouldBeDeleted2 = (Node) App.getDelegator().getOsmElement(Node.NAME, 635762221);
        assertEquals(OsmElement.STATE_DELETED, shouldBeDeleted2.getState());
        assertEquals(OsmElement.STATE_MODIFIED, shouldntBeDeleted1.getState());
        Node shouldntBeDeleted2 = (Node) App.getDelegator().getOsmElement(Node.NAME, 1201766174);
        assertEquals(OsmElement.STATE_UNCHANGED, shouldntBeDeleted2.getState());
        // undo
        assertTrue(TestUtils.clickMenuButton(device, context.getString(R.string.undo), false, false));
        assertEquals(OsmElement.STATE_UNCHANGED, way.getState());
        TestUtils.clickText(device, true, context.getString(R.string.okay), true, false); // for the tip alert
        assertTrue(way.hasParentRelation(6490362L));
        assertEquals(1, way.getParentRelations().size());
        List<Node> nodes = way.getNodes();
        assertEquals(origWayNodes.size(), nodes.size());
        for (int i = 0; i < nodes.size(); i++) {
            assertEquals(origWayNodes.get(i), nodes.get(i));
        }
    }

    /**
     * Select, show info dialog, delete (check that nodes are deleted), undelete
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void splitAndMergeWay() {
        map.getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.clickAtCoordinates(device, map, 8.3893820, 47.3895626, true);
        assertTrue(TestUtils.clickText(device, false, "Path", false, false));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        Way way = App.getLogic().getSelectedWay();
        assertNotNull(way);
        assertEquals(104148456L, way.getOsmId());
        Node splitNode = (Node) App.getDelegator().getOsmElement(Node.NAME, 1201766241L);
        assertTrue(TestUtils.clickMenuButton(device, main.getString(R.string.menu_split), false, false));
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.menu_split), 1000));
        TestUtils.clickAtCoordinates(device, map, splitNode.getLon(), splitNode.getLat());
        assertTrue(TestUtils.textGone(device, context.getString(R.string.actionmode_wayselect), 5000));
        TestUtils.clickAtCoordinates(device, map, 8.3893820, 47.3895626, true);
        assertTrue(TestUtils.clickText(device, false, "Path", false, false));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        way = App.getLogic().getSelectedWay();
        assertNotNull(way);
        assertTrue(TestUtils.clickMenuButton(device, main.getString(R.string.menu_join), false, false));
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.menu_join), 1000));
        Node node = (Node) App.getDelegator().getOsmElement(Node.NAME, 635762221L);
        TestUtils.clickAtCoordinates(device, map, node.getLon(), node.getLat());
        assertTrue(TestUtils.textGone(device, context.getString(R.string.menu_join), 5000));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        way = App.getLogic().getSelectedWay();
        assertNotNull(way);
        assertTrue(way.hasNode(node));
    }

    /**
     * Select way, select way nodes
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void selectWayNodes() {
        map.getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.clickAtCoordinates(device, map, 8.3893820, 47.3895626, true);
        assertTrue(TestUtils.clickText(device, false, "Path", false, false));
        Way way = App.getLogic().getSelectedWay();
        assertNotNull(way);
        List<Node> wayNodes = new ArrayList<>(way.getNodes());
        assertEquals(104148456L, way.getOsmId());
        //
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        assertTrue(TestUtils.clickOverflowButton(device));
        String selectWayNodes = context.getString(R.string.menu_select_way_nodes);
        TestUtils.scrollTo(selectWayNodes, false);
        assertTrue(TestUtils.clickText(device, false, selectWayNodes, true, false));
        assertTrue(TestUtils.findText(device, false, context.getResources().getQuantityString(R.plurals.actionmode_object_count, 2, 15)));
        List<Node> selectedNodes = logic.getSelectedNodes();
        assertEquals(wayNodes.size(), selectedNodes.size());
        for (Node n : wayNodes) {
            if (!selectedNodes.contains(n)) {
                fail("Missing node " + n);
            }
        }
    }

    /**
     * Select, extract segment, turn into a bridge
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void extractSegment() {
        map.getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.clickAtCoordinates(device, map, 8.3893820, 47.3895626, true);
        assertTrue(TestUtils.clickText(device, false, "Path", false, false));
        Way way = App.getLogic().getSelectedWay();
        assertNotNull(way);
        assertEquals(104148456L, way.getOsmId());

        assertTrue(TestUtils.clickOverflowButton(device));
        TestUtils.scrollTo("Extract segment", false);
        assertTrue(TestUtils.clickText(device, false, "Extract segment", true, false));
        assertTrue(TestUtils.findText(device, false, "Select segment"));

        TestUtils.clickAtCoordinates(device, map, 8.3893820, 47.3895626, true);
        assertTrue(TestUtils.findText(device, false, "Set tags"));
        assertTrue(TestUtils.clickMenuButton(device, context.getString(R.string.bridge), false, true));

        // should be in property editor now
        assertTrue(TestUtils.findText(device, false, "Bridge"));
        assertTrue(TestUtils.clickHome(device, true));

        Way segment = logic.getSelectedWay();
        assertNotNull(segment);
        assertEquals(2, segment.nodeCount());
        assertTrue(segment.hasTag(Tags.KEY_BRIDGE, Tags.VALUE_YES));
        assertTrue(segment.hasTag(Tags.KEY_LAYER, "1"));
    }

    /**
     * Select, drag way handle, try to upload, undo
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void geometryImprovement() {
        map.getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 22);
        TestUtils.clickAtCoordinates(device, map, 8.3893820, 47.3895626, true);
        assertTrue(TestUtils.clickText(device, false, "Path", false, false));
        Way way = App.getLogic().getSelectedWay();
        List<Node> origWayNodes = new ArrayList<>(way.getNodes());
        assertNotNull(way);
        assertEquals(104148456L, way.getOsmId());
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));

        int originalNodeCount = App.getDelegator().getApiNodeCount();

        // drag a handle
        TestUtils.drag(device, map, 8.3893800, 47.389559, 8.38939, 47.389550, false, 10);
        assertEquals(origWayNodes.size() + 1, way.getNodes().size());
        assertEquals(originalNodeCount + 1, App.getDelegator().getApiNodeCount());

        // try to upload the way, check that we have two elements and then cancel
        assertTrue(TestUtils.clickOverflowButton(device));
        TestUtils.scrollTo("Upload element", false);
        assertTrue(TestUtils.clickText(device, false, "Upload element", true, true));
        assertTrue(TestUtils.findText(device, false, "Upload these 2 changes?"));
        assertTrue(TestUtils.clickText(device, false, "NO", true, true));

        // undo
        TestUtils.unlock(device);
        assertTrue(TestUtils.clickMenuButton(device, context.getString(R.string.undo), false, false));
        assertEquals(OsmElement.STATE_UNCHANGED, way.getState());
        TestUtils.clickText(device, true, context.getString(R.string.okay), true, false); // for the tip alert
        List<Node> nodes = way.getNodes();
        assertEquals(origWayNodes.size(), nodes.size());
        for (int i = 0; i < nodes.size(); i++) {
            assertEquals(origWayNodes.get(i), nodes.get(i));
        }
        assertEquals(originalNodeCount, App.getDelegator().getApiNodeCount());
    }
}
