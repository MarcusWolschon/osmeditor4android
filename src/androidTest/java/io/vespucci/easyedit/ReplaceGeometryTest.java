package io.vespucci.easyedit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.LayerUtils;
import io.vespucci.Logic;
import io.vespucci.Main;
import io.vespucci.Map;
import io.vespucci.TestUtils;
import io.vespucci.osm.Node;
import io.vespucci.osm.OsmElement;
import io.vespucci.osm.Relation;
import io.vespucci.osm.StorageDelegator;
import io.vespucci.osm.Tags;
import io.vespucci.osm.Way;
import io.vespucci.prefs.AdvancedPrefDatabase;
import io.vespucci.prefs.Preferences;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ReplaceGeometryTest {

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
        logic.updateStyle();
        map.getDataLayer().setVisible(true);
    }

    /**
     * 
     */
    private void loadData(@NonNull String data) {
        TestUtils.loadTestData(main, data);
        TestUtils.stopEasyEdit(main);
        map.getViewBox().fitToBoundingBox(map, map.getDataLayer().getExtent());
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
     * Select, replace with way
     */
    @Test
    public void replaceNode() {
        loadData("replace_geometry3.osm");
        Node node = (Node) App.getDelegator().getOsmElement(Node.NAME, -14L);
        assertNotNull(node);
        assertTrue(node.hasTag(Tags.KEY_SHOP, "convenience"));
        TestUtils.clickAtCoordinates(device, map, node.getLon(), node.getLat(), true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));
        node = App.getLogic().getSelectedNode();
        assertNotNull(node);
        assertEquals(-14L, node.getOsmId());

        assertTrue(TestUtils.clickOverflowButton(device));
        TestUtils.scrollTo(context.getString(R.string.menu_replace_geometry), false);
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_replace_geometry), true, false));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_subtitle_replace_geometry)));
        TestUtils.clickAtCoordinates(device, map, 8.3760111D, 47.3981113D);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));

        assertFalse(node.hasTag(Tags.KEY_SHOP, "convenience"));
        Way w = App.getLogic().getSelectedWay();
        assertEquals(-1, w.getOsmId());
        assertTrue(w.hasTag(Tags.KEY_SHOP, "convenience"));
        assertTrue(w.hasNode(node));
    }

    /**
     * Select, replace with way
     */
    @Test
    public void replaceNodeWithRelation() {
        loadData("replace_geometry4.osm");
        Node node = (Node) App.getDelegator().getOsmElement(Node.NAME, -14L);
        assertNotNull(node);
        assertTrue(node.hasTag(Tags.KEY_SHOP, "convenience"));
        TestUtils.clickAtCoordinates(device, map, node.getLon(), node.getLat(), true);
        TestUtils.sleep();
        TestUtils.clickAwayTip(device, context);
        assertTrue(TestUtils.clickTextContains(device, "Convenience", true, 5000));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));
        node = App.getLogic().getSelectedNode();
        assertNotNull(node);
        assertEquals(-14L, node.getOsmId());

        assertTrue(TestUtils.clickOverflowButton(device));
        TestUtils.scrollTo(context.getString(R.string.menu_replace_geometry), false);
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_replace_geometry), true, false));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_subtitle_replace_geometry)));
        TestUtils.clickAtCoordinates(device, map, 8.3760111D, 47.3981113D);
        TestUtils.sleep();
        TestUtils.clickAwayTip(device, context);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.issue_replace_member_element_replaced)));
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.Done), true, false));

        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));

        assertFalse(node.hasTag(Tags.KEY_SHOP, "convenience"));
        Way w = App.getLogic().getSelectedWay();
        assertEquals(-1, w.getOsmId());
        assertTrue(w.hasTag(Tags.KEY_SHOP, "convenience"));
        assertTrue(w.hasNode(node));
        List<Relation> parents = w.getParentRelations();
        assertNotNull(parents);
        assertEquals(1, parents.size());
        assertTrue(parents.get(0).hasTag(Tags.KEY_TYPE, "enforcement"));
    }

    /**
     * Select, replace with geometry from way
     */
    @Test
    public void replaceWay() {
        loadData("replace_geometry2.osm");
        Node node = (Node) App.getDelegator().getOsmElement(Node.NAME, -14L);
        assertNotNull(node);
        TestUtils.clickAtCoordinates(device, map, 8.3760111D, 47.3981113D);

        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        Way way = App.getLogic().getSelectedWay();
        assertNotNull(way);
        assertEquals(-1L, way.getOsmId());

        assertTrue(TestUtils.clickOverflowButton(device));
        TestUtils.scrollTo(context.getString(R.string.menu_replace_geometry), false);
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_replace_geometry), true, false));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_subtitle_replace_geometry)));
        TestUtils.clickAtCoordinates(device, map, 8.3761799D, 47.3979480D);

        assertTrue(TestUtils.findText(device, false, context.getString(R.string.remove_geometry_source)));
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.Yes), true, false));
        TestUtils.clickAwayTip(device, context);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.replace_geometry_issue_title)));
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.Done), true, false));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));

        Way w = App.getLogic().getSelectedWay();
        assertEquals(-1, w.getOsmId());
        assertFalse(w.hasNode(node));
    }
}
