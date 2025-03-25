package io.vespucci.easyedit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.Until;
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
import io.vespucci.osm.Tags;
import io.vespucci.osm.ViewBox;
import io.vespucci.osm.Way;
import io.vespucci.prefs.API;
import io.vespucci.prefs.AdvancedPrefDatabase;
import io.vespucci.prefs.Preferences;
import io.vespucci.prefs.API.AuthParams;
import io.vespucci.propertyeditor.PropertyEditorActivity;
import io.vespucci.util.Coordinates;
import okhttp3.HttpUrl;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class WayActionsTest {

    Context                 context = null;
    Main                    main    = null;
    UiDevice                device  = null;
    Map                     map     = null;
    Logic                   logic   = null;
    private Instrumentation instrumentation;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getTargetContext();
        main = mActivityRule.getActivity();
        Preferences prefs = new Preferences(context);
        prefs.setAutolockDelay(300000L);
        main.updatePrefs(prefs);
        LayerUtils.removeImageryLayers(context);
        prefs.enableSimpleActions(true);
        main.runOnUiThread(() -> main.showSimpleActionsButton());

        map = main.getMap();
        map.setPrefs(main, prefs);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        logic = App.getLogic();
        logic.deselectAll();
        TestUtils.loadTestData(main, "test2.osm");
        App.getTaskStorage().reset();
        TestUtils.stopEasyEdit(main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        TestUtils.stopEasyEdit(main);
        TestUtils.zoomToNullIsland(logic, map);
        App.getTaskStorage().reset();
    }

    /**
     * Create a new way from menu and clicks at two more locations and finishing via home button, then square
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void square() {
        map.getDataLayer().setVisible(true);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.unlock(device);
        TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/simpleButton", true);
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_add_way), true, false));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.add_way_start_instruction)));
        TestUtils.clickAtCoordinates(device, map, 8.3886384, 47.3892752, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.add_way_node_instruction), 1000));

        TestUtils.clickAtCoordinates(device, map, 8.3887655, 47.3892752, true);
        TestUtils.sleep();
        TestUtils.clickAtCoordinates(device, map, 8.38877, 47.389202, true);
        TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/simpleButton", true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.tag_form_untagged_element)));
        TestUtils.clickHome(device, true);
        Way way = App.getLogic().getSelectedWay();
        assertNotNull(way);
        assertTrue(way.getOsmId() < 0);
        assertEquals(3, way.nodeCount());
        Coordinates[] coords = Coordinates.nodeListToCoordinateArray(map.getWidth(), map.getHeight(), map.getViewBox(), way.getNodes());
        Coordinates v1 = coords[0].subtract(coords[1]);
        Coordinates v2 = coords[2].subtract(coords[1]);
        double theta = Math.toDegrees(Math.acos(Coordinates.dotproduct(v1, v2) / (v1.length() * v2.length())));
        System.out.println("Original angle " + theta);
        assertEquals(92.33, theta, 0.25);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        TestUtils.clickOverflowButton(device);
        TestUtils.clickText(device, false, context.getString(R.string.menu_straighten), false, false);
        device.wait(Until.findObject(By.res(device.getCurrentPackageName() + ":string/Done")), 1000);
        coords = Coordinates.nodeListToCoordinateArray(map.getWidth(), map.getHeight(), map.getViewBox(), way.getNodes());
        v1 = coords[0].subtract(coords[1]);
        v2 = coords[2].subtract(coords[1]);
        theta = Math.toDegrees(Math.acos(Coordinates.dotproduct(v1, v2) / (v1.length() * v2.length())));
        System.out.println("New angle " + theta);
        assertEquals(90.00, theta, 0.05);
        device.waitForIdle(1000);
        TestUtils.clickUp(device);
    }

    /**
     * Select, remove two nodes
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void removeNodeFromWay() {
        map.getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.clickAtCoordinates(device, map, 8.3893820, 47.3895626, true);
        TestUtils.clickText(device, true, context.getString(R.string.okay), true, false); // Tip
        assertTrue(TestUtils.clickText(device, false, "↓ Path", false, false));
        Way way = App.getLogic().getSelectedWay();
        List<Node> origWayNodes = new ArrayList<>(way.getNodes());
        assertNotNull(way);
        assertEquals(104148456L, way.getOsmId());
        //
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        assertTrue(TestUtils.clickOverflowButton(device));
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_remove_node_from_way), true, false));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.menu_remove_node_from_way)));
        // delete an untagged way node somewhere in the middle
        int origSize = way.getNodes().size();
        Node testNode1 = way.getNodes().get(origSize - 4);
        TestUtils.clickAtCoordinatesWaitNewWindow(device, map, testNode1.getLon(), testNode1.getLat());
        assertEquals(OsmElement.STATE_DELETED, testNode1.getState());
        assertEquals(origSize - 1, way.getNodes().size());
        // delete the end node that is shared by some other ways
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        assertTrue(TestUtils.clickOverflowButton(device));
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_remove_node_from_way), true, false));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.menu_remove_node_from_way)));
        origSize = way.getNodes().size();
        Node testNode2 = way.getLastNode();
        List<Way> ways = logic.getWaysForNode(testNode2);
        assertEquals(4, ways.size());
        assertTrue(ways.contains(way));
        TestUtils.clickAtCoordinatesWaitNewWindow(device, map, testNode2.getLon(), testNode2.getLat());
        assertEquals(OsmElement.STATE_UNCHANGED, testNode2.getState());
        assertEquals(origSize - 1, way.getNodes().size());
        ways = logic.getWaysForNode(testNode2);
        assertEquals(3, ways.size());
        assertFalse(ways.contains(way));
    }

    /**
     * Select way, create route, add a further segment
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void createRoute() {
        map.getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.clickAtCoordinates(device, map, 8.3893820, 47.3895626, true);
        TestUtils.clickText(device, true, context.getString(R.string.okay), true, false); // Tip
        assertTrue(TestUtils.clickText(device, false, "↓ Path", false, false));
        Way way = App.getLogic().getSelectedWay();
        assertNotNull(way);
        assertEquals(104148456L, way.getOsmId());
        //
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        assertTrue(TestUtils.clickOverflowButton(device));
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_create_route), true, false));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_add_segment)));

        // add 50059937
        Way way2 = (Way) App.getDelegator().getOsmElement(Way.NAME, 50059937L);
        assertNotNull(way2);
        Node way2Node = way2.getNodes().get(2);
        TestUtils.clickAtCoordinates(device, map, way2Node.getLon(), way2Node.getLat(), true);
        TestUtils.sleep();
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_add_segment)));

        TestUtils.clickUp(device);
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.cancel), true, false));

        ActivityMonitor monitor = instrumentation.addMonitor(PropertyEditorActivity.class.getName(), null, false);
        // finish
        TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/simpleButton", true);
        Activity PropertyEditorActivity = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        assertNotNull(PropertyEditorActivity);
        TestUtils.sleep(2000);
        TestUtils.clickText(device, false, context.getString(R.string.cancel), true, false);
        assertTrue(TestUtils.clickHome(device, true));
        instrumentation.removeMonitor(monitor);

        List<Relation> rels = logic.getSelectedRelations();
        assertNotNull(rels);
        assertEquals(1, rels.size());
        final Relation route = rels.get(0);
        assertEquals(2, route.getMembers().size());
        assertNotNull(route.getMember(Way.NAME, 104148456L));
        assertNotNull(route.getMember(Way.NAME, 50059937L));
    }

    /**
     * Select way, select route, add a further segment, re-select first segment, add 2nd segment
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void addToRoute() {
        map.getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.clickAtCoordinates(device, map, 8.3884403, 47.3884988, true);
        TestUtils.clickText(device, true, context.getString(R.string.okay), true, false); // Tip
        assertTrue(TestUtils.clickText(device, false, "↑ Bergstrasse", true, false));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        Way way = App.getLogic().getSelectedWay();
        assertNotNull(way);
        assertEquals(119104094L, way.getOsmId());
        //
        assertTrue(TestUtils.clickOverflowButton(device));
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_add_to_route), true, false));
        assertTrue(TestUtils.clickText(device, false, "Bus 305", true, false));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_add_segment)));

        // add 47001849
        Way way2 = (Way) App.getDelegator().getOsmElement(Way.NAME, 47001849L);
        assertNotNull(way2);
        Node way2Node = way2.getNodes().get(2);
        TestUtils.clickAtCoordinates(device, map, way2Node.getLon(), way2Node.getLat(), true);
        TestUtils.sleep();

        // download would require mocking
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.ignore), true, false));

        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_reselect_first_segment)));

        // reselect 119104094 and then 47001849
        TestUtils.clickAtCoordinates(device, map, 8.3884403, 47.3884988, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_add_segment)));
        TestUtils.clickAtCoordinates(device, map, way2Node.getLon(), way2Node.getLat(), true);
        TestUtils.sleep();

        ActivityMonitor monitor = instrumentation.addMonitor(PropertyEditorActivity.class.getName(), null, false);
        // finish
        TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/simpleButton", true);
        Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        assertNotNull(propertyEditor);
        TestUtils.sleep(5000);
        TestUtils.clickText(device, false, context.getString(R.string.cancel), true, false);
        assertTrue(TestUtils.clickHome(device, true));
        instrumentation.removeMonitor(monitor);

        TestUtils.clickText(device, false, context.getString(R.string.okay), true, false); // TIP
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.tag_conflict_title))); // warning dialog
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.done), true, false)); // warning dialog

        List<Relation> rels = logic.getSelectedRelations();
        assertNotNull(rels);
        assertEquals(1, rels.size());
        final Relation route = rels.get(0);
        assertEquals(2807173L, route.getOsmId());
        assertNotNull(route.getMember(Way.NAME, 119104094L));
        assertNotNull(route.getMember(Way.NAME, 47001849L));
    }

    /**
     * Select from way, select via node, re-select from, select to way
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void createTurnRestriction() {
        map.getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.clickAtCoordinates(device, map, 8.3884403, 47.3884988, true);
        TestUtils.clickText(device, true, context.getString(R.string.okay), true, false); // Tip
        assertTrue(TestUtils.clickText(device, false, "↑ Bergstrasse", false, false));
        Way way = App.getLogic().getSelectedWay();
        assertNotNull(way);
        assertEquals(119104094L, way.getOsmId());
        //
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));

        if (!TestUtils.clickMenuButton(device, context.getString(R.string.actionmode_restriction), false, true)) {
            assertTrue(TestUtils.clickOverflowButton(device));
            assertTrue(TestUtils.clickText(device, false, context.getString(R.string.actionmode_restriction), true, false));
        }

        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_restriction_via)));

        // add via node 633468409
        Node via = (Node) App.getDelegator().getOsmElement(Node.NAME, 633468409L);
        assertNotNull(via);
        TestUtils.clickAtCoordinates(device, map, via.getLon(), via.getLat(), true);
        TestUtils.sleep();
        // download would require mocking
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.ignore), true, false));

        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_restriction_restart_from)));

        // reselect 119104094 and then 47001849
        TestUtils.clickAtCoordinates(device, map, 8.3884403, 47.3884988, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_restriction_to)));

        ActivityMonitor monitor = instrumentation.addMonitor(PropertyEditorActivity.class.getName(), null, false);
        // click to way 49855525
        TestUtils.clickAtCoordinates(device, map, 8.3879168, 47.3883856, true);

        Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        assertNotNull(propertyEditor);
        TestUtils.sleep(5000);
        assertTrue(TestUtils.clickText(device, false, "No left turn", true, false));
        assertTrue(TestUtils.findText(device, false, "No left turn"));
        TestUtils.clickHome(device, true);
        instrumentation.removeMonitor(monitor);

        TestUtils.clickText(device, false, context.getString(R.string.okay), true, false); // TIP
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.tag_conflict_title))); // warning dialog
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.done), true, false)); // warning dialog

        List<Relation> rels = logic.getSelectedRelations();
        assertNotNull(rels);
        assertEquals(1, rels.size());
        final Relation restriction = rels.get(0);
        assertEquals(3, restriction.getMembers().size());
        assertEquals(1, restriction.getMembersWithRole(Tags.ROLE_FROM).size());
        // from will have a different id as it has been split assertEquals(119104094L,
        // restriction.getMembersWithRole(Tags.ROLE_FROM).get(0).getElement().getOsmId());
        assertEquals(1, restriction.getMembersWithRole(Tags.ROLE_VIA).size());
        assertEquals(633468409L, restriction.getMembersWithRole(Tags.ROLE_VIA).get(0).getElement().getOsmId());
        assertEquals(1, restriction.getMembersWithRole(Tags.ROLE_TO).size());
        assertEquals(49855525L, restriction.getMembersWithRole(Tags.ROLE_TO).get(0).getElement().getOsmId());
    }

    /**
     * Create a new way from menu and append at the end and the start
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void append() {
        map.getDataLayer().setVisible(true);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.unlock(device);
        TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/simpleButton", true);
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_add_way), true, false));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.add_way_start_instruction)));
        TestUtils.clickAtCoordinates(device, map, 8.3886384, 47.3892752, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.add_way_node_instruction), 1000));

        TestUtils.clickAtCoordinates(device, map, 8.3887655, 47.3892752, true);
        TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/simpleButton", true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.tag_form_untagged_element)));
        TestUtils.clickHome(device, true);
        Way way = App.getLogic().getSelectedWay();
        assertNotNull(way);
        assertTrue(way.getOsmId() < 0);
        assertEquals(2, way.nodeCount());

        // start append
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        assertTrue(TestUtils.clickMenuButton(device, context.getString(R.string.menu_append), false, true));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.menu_append)));

        // append at end
        TestUtils.clickAtCoordinates(device, map, 8.3887655, 47.3892752, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.add_way_node_instruction)));
        TestUtils.clickAtCoordinates(device, map, 8.38877, 47.389202, true);
        TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/simpleButton", true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.tag_form_untagged_element)));
        TestUtils.clickHome(device, true);
        way = App.getLogic().getSelectedWay();
        assertNotNull(way);
        assertTrue(way.getOsmId() < 0);
        assertEquals(3, way.nodeCount());

        // undo
        assertTrue(TestUtils.clickMenuButton(device, context.getString(R.string.undo), false, false));
        TestUtils.findText(device, false, context.getString(R.string.undo));
        TestUtils.textGone(device, context.getString(R.string.undo), 5000);
        TestUtils.clickText(device, false, context.getString(R.string.okay), true); // in case we get a tip
        assertEquals(2, way.nodeCount());

        // start append
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        assertTrue(TestUtils.clickMenuButton(device, context.getString(R.string.menu_append), false, true));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.menu_append)));

        // append at start
        TestUtils.clickAtCoordinates(device, map, 8.3886384, 47.3892752, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.add_way_node_instruction)));
        TestUtils.clickAtCoordinates(device, map, 8.38877, 47.389202, true);
        TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/simpleButton", true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.tag_form_untagged_element)));
        TestUtils.clickHome(device, true);
        way = App.getLogic().getSelectedWay();
        assertNotNull(way);
        assertTrue(way.getOsmId() < 0);
        assertEquals(3, way.nodeCount());

        device.waitForIdle(1000);
        TestUtils.clickUp(device);
    }

    /**
     * Append to an existing way and then undo
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void appendUndo() {
        map.getDataLayer().setVisible(true);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.unlock(device);

        TestUtils.clickAtCoordinates(device, map, 8.3881245, 47.3901113, true);

        // start append
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        Way way = App.getLogic().getSelectedWay();
        assertEquals(8, way.nodeCount());
        assertTrue(TestUtils.clickMenuButton(device, context.getString(R.string.menu_append), false, true));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.menu_append)));
        TestUtils.clickAtCoordinates(device, map, 8.3881607, 47.3901758, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.add_way_node_instruction)));
        TestUtils.clickAtCoordinates(device, map, 8.3882792, 47.3901457, true);
        TestUtils.sleep();
        assertNotNull(App.getDelegator().getApiStorage().getWay(way.getOsmId()));

        // undo should terminate
        assertTrue(TestUtils.clickMenuButton(device, context.getString(R.string.undo), false, false));
        TestUtils.clickText(device, false, context.getString(R.string.okay), true); // in case we get a tip

        TestUtils.textGone(device, context.getString(R.string.menu_append), 5000);
        assertEquals(8, way.nodeCount());
        assertNull(App.getDelegator().getApiStorage().getWay(way.getOsmId()));
    }

    /**
     * Select way, try to split, download missing ways
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void splitRouteMember() {
        MockWebServerPlus mockServer = new MockWebServerPlus();
        HttpUrl mockBaseUrl = mockServer.server().url("/api/0.6/");
        AdvancedPrefDatabase prefDB = new AdvancedPrefDatabase(context);
        try {
            prefDB.deleteAPI("Test");
            prefDB.addAPI("Test", "Test", mockBaseUrl.toString(), null, null, new AuthParams(API.Auth.BASIC, "user", "pass", null, null), false);
            prefDB.selectAPI("Test");
            Preferences prefs = new Preferences(context);
            LayerUtils.removeImageryLayers(context);
            main.getMap().setPrefs(main, prefs);
            System.out.println("mock api url " + mockBaseUrl.toString()); // NOSONAR
            mockServer.enqueue("ways-119104093-45180812");
            mockServer.enqueue("way-45180812-nodes");
            mockServer.enqueue("way-119104093-nodes");
            map.getDataLayer().setVisible(true);
            TestUtils.unlock(device);
            TestUtils.zoomToLevel(device, main, 21);
            TestUtils.clickAtCoordinates(device, map, 8.3884403, 47.3884988, true);
            TestUtils.clickText(device, true, context.getString(R.string.okay), true, false); // Tip
            assertTrue(TestUtils.clickText(device, false, "↑ Bergstrasse", false, false));
            Way way = App.getLogic().getSelectedWay();
            assertNotNull(way);
            assertEquals(119104094L, way.getOsmId());
            List<Relation> parents = way.getParentRelations();
            assertNotNull(parents);
            Relation route = parents.get(0);
            assertTrue(route.hasTag(Tags.KEY_TYPE, Tags.VALUE_ROUTE));
            int memberCount = route.getMembers().size();
            //
            assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));

            assertTrue(TestUtils.clickMenuButton(device, context.getString(R.string.menu_split), false, true));
            TestUtils.clickText(device, false, context.getString(R.string.okay), true, false); // TIP

            assertTrue(TestUtils.findText(device, false, context.getString(R.string.menu_split)));

            // split at node 633468409
            Node via = (Node) App.getDelegator().getOsmElement(Node.NAME, 633468409L);
            assertNotNull(via);
            TestUtils.clickAtCoordinates(device, map, via.getLon(), via.getLat(), true);
            TestUtils.sleep();
            assertNull(App.getDelegator().getOsmElement(Way.NAME, 119104093L));
            assertNull(App.getDelegator().getOsmElement(Way.NAME, 45180812L));
            int apiWayCount = App.getDelegator().getApiWayCount();
            // download requires mocking
            assertTrue(TestUtils.clickText(device, false, context.getString(R.string.download), true, false));
            TestUtils.sleep();
            assertNotNull(App.getDelegator().getOsmElement(Way.NAME, 119104093L));
            assertNotNull(App.getDelegator().getOsmElement(Way.NAME, 45180812L));
            assertEquals(apiWayCount + 2, App.getDelegator().getApiWayCount());
            assertEquals(memberCount + 1, route.getMembers().size());
        } finally {
            try {
                mockServer.server().shutdown();
            } catch (IOException ioex) {
                System.out.println("Stopping mock webserver exception " + ioex); // NOSONAR
            }
            prefDB.selectAPI(AdvancedPrefDatabase.ID_DEFAULT);
            prefDB.close();
        }
    }

    /**
     * Select way, try to split, download missing ways
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void splitRestrictionMember() {
        TestUtils.loadTestData(main, "incomplete-restriction.osm");
        MockWebServerPlus mockServer = new MockWebServerPlus();
        HttpUrl mockBaseUrl = mockServer.server().url("/api/0.6/");
        AdvancedPrefDatabase prefDB = new AdvancedPrefDatabase(context);
        try {
            prefDB.deleteAPI("Test");
            prefDB.addAPI("Test", "Test", mockBaseUrl.toString(), null, null, new AuthParams(API.Auth.BASIC, "user", "pass", null, null), false);
            prefDB.selectAPI("Test");
            Preferences prefs = new Preferences(context);
            LayerUtils.removeImageryLayers(context);
            main.getMap().setPrefs(main, prefs);
            System.out.println("mock api url " + mockBaseUrl.toString()); // NOSONAR
            mockServer.enqueue("ways-4306402129-4306402128");
            mockServer.enqueue("way-4306402128-nodes");
            mockServer.enqueue("way-4306402129-nodes");
            map.getDataLayer().setVisible(true);
            TestUtils.unlock(device);
            TestUtils.zoomToLevel(device, main, 21);
            TestUtils.clickAtCoordinates(device, map, 8.3999683, 47.4002093, true);
            TestUtils.clickText(device, true, context.getString(R.string.okay), true, false); // Tip
            assertTrue(TestUtils.clickText(device, false, "↗ Primary", false, false));
            assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect), 2000));
            Way way = App.getLogic().getSelectedWay();
            assertNotNull(way);
            assertEquals(4306402131L, way.getOsmId());
            List<Relation> parents = way.getParentRelations();
            assertNotNull(parents);
            Relation restriction = parents.get(0);
            assertTrue(restriction.hasTag(Tags.KEY_TYPE, Tags.VALUE_RESTRICTION));
            //
            assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));

            assertTrue(TestUtils.clickMenuButton(device, context.getString(R.string.menu_split), false, true));
            TestUtils.clickText(device, false, context.getString(R.string.okay), true, false); // TIP

            assertTrue(TestUtils.findText(device, false, context.getString(R.string.menu_split)));

            // split at node 633468409
            Node via = (Node) App.getDelegator().getOsmElement(Node.NAME, 4345573842L);
            assertNotNull(via);
            TestUtils.clickAtCoordinates(device, map, via.getLon(), via.getLat(), true);
            TestUtils.sleep();
            assertNull(App.getDelegator().getOsmElement(Way.NAME, 4306402128L));
            assertNull(App.getDelegator().getOsmElement(Way.NAME, 4306402129L));
            // download requires mocking
            assertTrue(TestUtils.clickText(device, false, context.getString(R.string.download), true, false));
            TestUtils.sleep();
            assertNotNull(App.getDelegator().getOsmElement(Way.NAME, 4306402128L));
            assertNotNull(App.getDelegator().getOsmElement(Way.NAME, 4306402129L));
        } finally {
            try {
                mockServer.server().shutdown();
            } catch (IOException ioex) {
                System.out.println("Stopping mock webserver exception " + ioex); // NOSONAR
            }
            prefDB.selectAPI(AdvancedPrefDatabase.ID_DEFAULT);
            prefDB.close();
        }
    }

    /**
     * Rotate a building
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void rotate() {
        map.getDataLayer().setVisible(true);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.unlock(device);
        TestUtils.clickAtCoordinates(device, map, 8.38655, 47.38972, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        Way way = App.getLogic().getSelectedWay();
        Node n0 = way.getFirstNode();
        assertEquals(n0.getLon(), 83864605);
        assertEquals(n0.getLat(), 473896527);
        if (!TestUtils.clickMenuButton(device, context.getString(R.string.menu_rotate), false, false)) {
            TestUtils.clickOverflowButton(device);
            TestUtils.clickText(device, false, context.getString(R.string.menu_rotate), false, false);
        }
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_rotate)));
        TestUtils.drag(device, map, 8.386446, 47.38959, 8.386628, 47.38959, false, 30);

        TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/simpleButton", true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        assertEquals(n0.getLon(), 83865662, 1000);
        assertEquals(n0.getLat(), 473896285, 1000);
    }

    /**
     * Go to the start or end of a way
     */
    @Test
    public void startEndWay() {
        map.getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.clickAtCoordinates(device, map, 8.3884403, 47.3884988, true);
        TestUtils.clickText(device, true, context.getString(R.string.okay), true, false); // Tip
        assertTrue(TestUtils.clickText(device, false, "↑ Bergstrasse", false, false));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect), 2000));
        Way way = App.getLogic().getSelectedWay();
        assertNotNull(way);
        assertEquals(119104094L, way.getOsmId());
        //

        assertTrue(TestUtils.clickOverflowButton(device));
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_start_end_way), true, false));
        if (TestUtils.clickText(device, false, context.getString(R.string.start), false, false)) {
            ViewBox vb = ((Main) main).getMap().getViewBox();
            assertEquals(47.3879811D, vb.getCenterLat(), 0.001D);
            assertEquals(8.3881322D, ((vb.getRight() - vb.getLeft()) / 2 + vb.getLeft()) / 1E7D, 0.001D);
        }
        assertTrue(TestUtils.clickOverflowButton(device));
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_start_end_way), true, false));
        if (TestUtils.clickText(device, false, context.getString(R.string.end), false, false)) {
            ViewBox vb = ((Main) main).getMap().getViewBox();
            assertEquals(47.3910674D, vb.getCenterLat(), 0.001D);
            assertEquals(8.3895455D, ((vb.getRight() - vb.getLeft()) / 2 + vb.getLeft()) / 1E7D, 0.001D);
        }
    }
}
