package de.blau.android.easyedit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;

import org.junit.After;
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
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;

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

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        main = mActivityRule.getActivity();
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
        map.getViewBox().fitToBoundingBox(map, map.getDataLayer().getExtent());
        logic.updateStyle();
        map.getDataLayer().setVisible(true);
        TestUtils.zoomToLevel(device, main, 20);
        map.invalidate();
        TestUtils.unlock(device);
        device.waitForWindowUpdate(null, 2000);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        TestUtils.stopEasyEdit(main);
        TestUtils.zoomToNullIsland(logic, map);
    }

    /**
     * Select, show info dialog, show position dialog, delete, undelete
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void selectNode() {
        Node node = (Node) App.getDelegator().getOsmElement(Node.NAME, 3465444349L);
        assertNotNull(node);
        TestUtils.clickAtCoordinates(device, map, node.getLon(), node.getLat(), true);
        TestUtils.clickAwayTip(device, context);
        assertTrue(TestUtils.clickTextContains(device, "Toilets", true, 5000));
        node = App.getLogic().getSelectedNode();
        assertNotNull(node);
        assertEquals(3465444349L, node.getOsmId());
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));
        assertTrue(TestUtils.clickOverflowButton(device));
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_information), true, false));
        assertTrue(TestUtils.findText(device, false, "permissive"));
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.done), true, false));
        assertTrue(TestUtils.clickOverflowButton(device));
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_set_position), true, false));
        assertTrue(TestUtils.findText(device, false, "8.3878200"));
        assertTrue(TestUtils.findText(device, false, "47.3903390"));
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.cancel), true, false));
        assertTrue(TestUtils.clickOverflowButton(device));
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.delete), true, false));
        assertEquals(OsmElement.STATE_DELETED, node.getState());
        assertTrue(TestUtils.clickMenuButton(device, context.getString(R.string.undo), false, false));
        assertEquals(OsmElement.STATE_UNCHANGED, node.getState());
        TestUtils.clickText(device, true, context.getString(R.string.okay), true, false); // for the tip alert
    }

    /**
     * Select, drag, undo
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void dragNode() {
        TestUtils.clickAtCoordinates(device, map, 8.38782, 47.390339, true);
        TestUtils.clickAwayTip(device, context);
        assertTrue(TestUtils.clickTextContains(device, "Toilets", true, 5000));
        Node node = App.getLogic().getSelectedNode();
        assertNotNull(node);
        assertEquals(3465444349L, node.getOsmId());
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));

        TestUtils.drag(device, map, 8.38782, 47.390339, 8.388, 47.391, true, 30);

        assertEquals(OsmElement.STATE_MODIFIED, node.getState());

        assertEquals(8.388, node.getLon() / 1E7D, 0.001);
        assertEquals(47.391, node.getLat() / 1E7D, 0.001);

        assertTrue(TestUtils.clickMenuButton(device, context.getString(R.string.undo), false, false));
        assertEquals(OsmElement.STATE_UNCHANGED, node.getState());
        TestUtils.clickText(device, true, context.getString(R.string.okay), true, false); // for the tip alert
    }

    /**
     * Select, unjoin, merge
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void unjoinMergeWays() {
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.clickAtCoordinates(device, map, 8.3874964, 47.3884769, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));
        Node node = App.getLogic().getSelectedNode();
        assertNotNull(node);
        assertEquals(633468419L, node.getOsmId());

        List<Way> waysBefore = logic.getWaysForNode(node);
        assertEquals(2, waysBefore.size());
        assertTrue(TestUtils.clickMenuButton(device, context.getString(R.string.menu_unjoin), false, true));

        List<Way> waysUnjoined = logic.getWaysForNode(node);
        assertEquals(1, waysUnjoined.size());
        Way unjoinedWay = waysUnjoined.get(0);
        assertEquals(OsmElement.STATE_UNCHANGED, node.getState());
        assertEquals(OsmElement.STATE_UNCHANGED, unjoinedWay.getState());

        TestUtils.clickAtCoordinates(device, map, 8.3874964, 47.3884769, false);
        TestUtils.clickAwayTip(device, context);
        assertTrue(TestUtils.clickTextContains(device, false, " #633468419", false)); // the first node in the list
        assertTrue(TestUtils.clickMenuButton(device, context.getString(R.string.menu_merge), false, true));

        List<Way> waysAfter = logic.getWaysForNode(node);
        assertEquals(2, waysAfter.size());
    }

    /**
     * Select, unjoin, merge multiple nodes
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void unjoinMergeNodes() {
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.unlock(device);
        TestUtils.clickAtCoordinates(device, map, 8.3866386, 47.3904394, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));
        Node node = App.getLogic().getSelectedNode();
        assertNotNull(node);
        assertEquals(600181872L, node.getOsmId());
        final StorageDelegator delegator = App.getDelegator();
        assertEquals(4, delegator.getCurrentStorage().getWays(node).size());

        int apiNodeCount = delegator.getApiNodeCount();
        assertTrue(TestUtils.clickMenuButton(device, context.getString(R.string.menu_unjoin), false, true));
        assertEquals(apiNodeCount + 3, delegator.getApiNodeCount());

        TestUtils.clickAtCoordinates(device, map, 8.3866386, 47.3904394, false);
        TestUtils.clickAwayTip(device, context);
        assertTrue(TestUtils.clickTextContains(device, false, " #-2221", true));

        assertTrue(TestUtils.clickMenuButton(device, context.getString(R.string.menu_merge), false, true));

        // merge all
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.merge_with_all_nodes), true, false));
        TestUtils.textGone(device, context.getString(R.string.toast_merged), 2000);

        assertEquals(apiNodeCount + 1, delegator.getApiNodeCount());

        node = App.getLogic().getSelectedNode();
        assertNotNull(node);
        assertEquals(600181872L, node.getOsmId());
        assertEquals(4, delegator.getCurrentStorage().getWays(node).size());
    }

    /**
     * Select node that is member of a way, append to it
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void append() {
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.clickAtCoordinates(device, map, 8.3878990, 47.3891959, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));
        Node node = App.getLogic().getSelectedNode();
        assertNotNull(node);
        assertEquals(633468436L, node.getOsmId());
        assertTrue(TestUtils.clickMenuButton(device, context.getString(R.string.menu_append), false, true));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.add_way_node_instruction)));
        TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/simpleButton", true);
    }

    /**
     * Select node that is member of multiple ways, append to it
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void appendWithMenu() {
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.clickAtCoordinates(device, map, 8.3879569, 47.3893814, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));
        Node node = App.getLogic().getSelectedNode();
        assertNotNull(node);
        assertEquals(599672190L, node.getOsmId());
        assertTrue(TestUtils.clickMenuButton(device, context.getString(R.string.menu_append), false, true));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.append_context_title)));
        assertTrue(TestUtils.clickText(device, false, "Kirchstrasse", true, false));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_createpath)));
        TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/simpleButton", true);
    }

    /**
     * Select node, set direction tag, rotate
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void rotate() {
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.clickAtCoordinates(device, map, 8.3881577, 47.3886924, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));
        Node node = App.getLogic().getSelectedNode();
        assertNotNull(node);
        java.util.Map<String, String> tags = new HashMap<>();
        tags.put("traffic_sign", "stop");
        tags.put(Tags.KEY_DIRECTION, "90");
        App.getLogic().setTags(main, node, tags);
        main.getEasyEditManager().invalidate();
        if (!TestUtils.clickMenuButton(device, context.getString(R.string.menu_rotate), false, false)) {
            TestUtils.clickOverflowButton(device);
            TestUtils.clickText(device, false, context.getString(R.string.menu_rotate), false, false);
        }
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_rotate)));

        TestUtils.drag(device, map, 8.3882867, 47.38887072, 8.3882853, 47.3886022, false, 100);

        TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/simpleButton", true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect), 2000));

        assertEquals(148, Integer.parseInt(node.getTagWithKey(Tags.KEY_DIRECTION)), 5);
    }
}
