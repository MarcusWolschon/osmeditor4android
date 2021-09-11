package de.blau.android.easyedit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import de.blau.android.osm.Relation;
import de.blau.android.osm.Way;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ExtendedSelectionTest {

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
        TestUtils.stopEasyEdit(main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        TestUtils.stopEasyEdit(main);
        TestUtils.zoomToLevel(device, main, 18);
    }

    /**
     * Select node, select 2nd node, de-select
     */
    @Test
    public void selectNodes() {
        TestUtils.loadTestData(main, "test2.osm");
        TestUtils.zoomToLevel(device, main, 18); // if we are zoomed in too far we might not get the selection popups
        map.getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.sleep(2000);
        TestUtils.clickAtCoordinates(device, map, 8.38782, 47.390339, true);
        TestUtils.findText(device, false, "Toilets", 10000);
        assertTrue(TestUtils.clickText(device, false, "Toilets", false, false));
        Node node = App.getLogic().getSelectedNode();
        assertNotNull(node);
        assertEquals(3465444349L, node.getOsmId());
        int origLon = node.getLon();
        int origLat = node.getLat();
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));
        assertTrue(TestUtils.clickOverflowButton(device));
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_extend_selection), true, false));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_multiselect)));
        // double clicking doesn't currently work reliably in tests TestUtils.doubleClickAtCoordinates(device, map,
        // 8.3877977, 47.3897371, true); // NOSONAR
        TestUtils.clickAtCoordinates(device, map, 8.3877977, 47.3897371, true);
        assertTrue(TestUtils.clickText(device, false, "Excrement", false, false));
        assertEquals(2, logic.getSelectedNodes().size());
        TestUtils.zoomToLevel(device, main, 22);
        TestUtils.drag(device, map, 8.3877977, 47.3897371, 8.3879, 47.38967, true, 100);

        int deltaLon = node.getLon() - origLon;
        int deltaLat = node.getLat() - origLat;

        assertEquals(8.3879 - 8.3877977, deltaLon / 1E7D, 0.0001);
        assertEquals(47.38967 - 47.3897371, deltaLat / 1E7D, 0.0001);
        TestUtils.clickUp(device);
    }

    /**
     * Select two ways then merge
     */
    @Test
    public void selectAndMergeWays() {
        TestUtils.loadTestData(main, "test2.osm");
        TestUtils.zoomToLevel(device, main, 18); // if we are zoomed in too far we might not get the selection popups
        map.getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.clickAtCoordinates(device, map, 8.3893820, 47.3895626, true);
        assertTrue(TestUtils.clickText(device, false, "Path", false, false));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        Way way = App.getLogic().getSelectedWay();
        assertNotNull(way);
        assertEquals(104148456L, way.getOsmId());
        assertTrue(TestUtils.clickMenuButton(device, context.getString(R.string.menu_split), false, true));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.menu_split)));
        TestUtils.clickAtCoordinates(device, map, 8.3899934, 47.3898778, true);
        TestUtils.textGone(device, context.getString(R.string.menu_split), 1);
        TestUtils.clickAtCoordinates(device, map, 8.3899204, 47.3898603, true);
        assertTrue(TestUtils.clickText(device, false, "Path", false, false));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        assertTrue(TestUtils.clickOverflowButton(device));
        TestUtils.scrollTo(context.getString(R.string.menu_extend_selection));
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_extend_selection), true, false));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_multiselect)));
        TestUtils.clickAtCoordinates(device, map, 8.3900912, 47.3899572, true);
        assertTrue(TestUtils.clickText(device, false, "Path", false, false));
        assertTrue(TestUtils.findText(device, false, context.getResources().getQuantityString(R.plurals.actionmode_object_count, 2, 2)));
        assertTrue(TestUtils.clickMenuButton(device, context.getString(R.string.menu_merge), false, true));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
    }

    /**
     * Select two polygons then merge
     */
    @Test
    public void selectAndMergePolygons() {
        TestUtils.loadTestData(main, "rings.osm");
        TestUtils.zoomToLevel(device, main, 18);
        map.getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 23);
        TestUtils.clickAtCoordinates(device, map, -0.1425731, 51.5019184, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        Way way = App.getLogic().getSelectedWay();
        assertNotNull(way);
        assertEquals(-1L, way.getOsmId());
        assertTrue(TestUtils.clickOverflowButton(device));
        TestUtils.scrollTo(context.getString(R.string.menu_extend_selection));
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_extend_selection), true, false));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_multiselect)));
        TestUtils.clickAtCoordinates(device, map, -0.1425372, 51.5019187, true);
        assertTrue(TestUtils.findText(device, false, context.getResources().getQuantityString(R.plurals.actionmode_object_count, 2, 2)));
        TestUtils.clickAtCoordinates(device, map, -0.1425558, 51.5018948, true);
        assertTrue(TestUtils.findText(device, false, context.getResources().getQuantityString(R.plurals.actionmode_object_count, 3, 3)));
        // de-select polygon 2
        TestUtils.clickAtCoordinates(device, map, -0.1425372, 51.5019187, true);
        assertTrue(TestUtils.findText(device, false, context.getResources().getQuantityString(R.plurals.actionmode_object_count, 2, 2)));

        assertTrue(TestUtils.clickMenuButton(device, context.getString(R.string.menu_merge), false, true));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_relationselect)));
        assertTrue(way.hasParentRelations());
        List<Relation> parents = way.getParentRelations();
        assertEquals(1,parents.size());
        Relation mp = parents.get(0);
        assertEquals(2, mp.getMembers().size());
    }
}
