package de.blau.android.osm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Instrumentation;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.annotation.UiThreadTest;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.UiDevice;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.TestUtils;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.osm.MergeResult.Issue;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.DataStyle;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.util.GeoMath;
import de.blau.android.util.Util;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class GeometryEditsTest {

    Context context = null;
    Main    main    = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getTargetContext();
        UiDevice device = UiDevice.getInstance(instrumentation);
        main = mActivityRule.getActivity();
        Preferences prefs = new Preferences(context);
        prefs.setBackGroundLayer(TileLayerServer.LAYER_NONE); // try to avoid downloading tiles
        prefs.setOverlayLayer(TileLayerServer.LAYER_NOOVERLAY);
        main.getMap().setPrefs(main, prefs);
        App.getDelegator().reset(false);
        App.getDelegator().setOriginalBox(ViewBox.getMaxMercatorExtent());
        Logic logic = App.getLogic();
        Map map = logic.getMap();
        logic.setZoom(map, 18);
        map.getViewBox().moveTo(map, 0, 0);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, context);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
    }

    /**
     * Create a way, split it, create a turn restriction on it, split one of the ways, merge the two resulting ways back
     * in to one, add the way to a normal relation, split it again
     */
    @UiThreadTest
    @Test
    public void mergeSplit() {
        try {
            // setup some stuff to test relations
            Logic logic = App.getLogic();
            setupWaysForSplit(logic);
            Way w1 = logic.getSelectedWay();
            Assert.assertNotNull(w1);
            List<Node> nList1 = w1.getNodes();
            Assert.assertEquals(4, nList1.size());
            final Node n2 = nList1.get(1);
            final Node n3 = nList1.get(2);

            // split at n2
            logic.performSplit(main, n2);

            List<Way> wList1 = logic.getWaysForNode(n3);
            Assert.assertEquals(1, wList1.size());
            Way w2 = wList1.get(0);

            Relation r1 = logic.createRestriction(main, w1, n2, w2, "test rest");
            List<OsmElement> mList1 = r1.getMemberElements();
            Assert.assertEquals(3, mList1.size());
            Assert.assertEquals(w1, mList1.get(0));
            Assert.assertEquals(n2, mList1.get(1));
            Assert.assertEquals(w2, mList1.get(2));
            Assert.assertEquals(1, w1.getParentRelations().size());
            Assert.assertEquals(r1, w1.getParentRelations().get(0));
            Assert.assertEquals(1, n2.getParentRelations().size());
            Assert.assertEquals(r1, n2.getParentRelations().get(0));
            Assert.assertEquals(1, w2.getParentRelations().size());
            Assert.assertEquals(r1, w2.getParentRelations().get(0));
            Assert.assertEquals(3, r1.getMemberElements().size());

            // split way 2
            logic.performSplit(main, n3);

            List<Way> wList2 = logic.getWaysForNode(n3);
            // this assumes wList2 contains the way in chronological order
            Assert.assertEquals(2, wList2.size());
            Assert.assertEquals(wList2.get(0), w2);
            Assert.assertEquals(wList2.get(0).getParentRelations().get(0), r1);
            Assert.assertNull(wList2.get(1).getParentRelations()); // special case for restrictions
            Assert.assertEquals(3, r1.getMemberElements().size());

            // merge former way 2
            logic.performMerge(main, wList2.get(0), wList2.get(1));
            Assert.assertEquals(3, w2.getNodes().size());
            Assert.assertEquals(w2.getParentRelations().get(0), r1);
            Assert.assertEquals(3, r1.getMemberElements().size());

            // add w2 to a normal relation
            List<OsmElement> mList2 = new ArrayList<OsmElement>();
            mList2.add(w2);
            Relation r2 = logic.createRelation(main, "test", mList2);
            Assert.assertEquals(2, w2.getParentRelations().size());
            // r2 should have 1 member
            Assert.assertEquals(1, r2.getMemberElements().size());

            // add w2 to a normal relation and then w1
            mList2 = new ArrayList<OsmElement>();
            mList2.add(w2);
            mList2.add(w1);
            Relation r3 = logic.createRelation(main, "test", mList2);
            Assert.assertEquals(3, w2.getParentRelations().size());
            // r3 should have 2 members
            Assert.assertEquals(2, r3.getMemberElements().size());

            // resplit at n3
            logic.performSplit(main, n3);

            wList2 = (ArrayList<Way>) logic.getWaysForNode(n3);
            // this assumes wList2 contains the way in chronological order
            Way w3 = wList2.get(0);
            Way w4 = wList2.get(1);
            Assert.assertEquals(3, wList2.get(0).getParentRelations().size()); // should contain both rels
            Assert.assertEquals(2, wList2.get(1).getParentRelations().size()); // just the 2nd one
            // default ordering in r2
            Assert.assertEquals(0, r2.getPosition(r2.getMember(w3))); // first position in r2
            Assert.assertEquals(1, r2.getPosition(r2.getMember(w4))); // second position in r2
            // r2 should have 2 members now
            Assert.assertEquals(2, r2.getMemberElements().size());
            // since w1 has a common node with w2, w3 should be after w4 in r3
            Assert.assertEquals(1, r3.getPosition(r3.getMember(w3))); // second position in r3
            Assert.assertEquals(0, r3.getPosition(r3.getMember(w4))); // fist position in r3
            Assert.assertEquals(2, r3.getPosition(r3.getMember(w1))); // third position in r3
            // r3 should have 3 members now
            Assert.assertEquals(3, r3.getMemberElements().size());
        } catch (Exception igit) {
            Assert.fail(igit.getMessage());
        }
    }

    /**
     * Create a way, split it, create a destination_sign relation on it, split one of the ways
     */
    @UiThreadTest
    @Test
    public void splitDestinationSign() {
        try {
            // setup some stuff to test relations
            Logic logic = App.getLogic();
            setupWaysForSplit(logic);
            Way w1 = logic.getSelectedWay();
            Assert.assertNotNull(w1);
            List<Node> nList1 = w1.getNodes();
            Assert.assertEquals(4, nList1.size());
            final Node n2 = nList1.get(1);
            final Node n3 = nList1.get(2);

            // split at n2
            logic.performSplit(main, n2);

            List<Way> wList1 = logic.getWaysForNode(n3);
            Assert.assertEquals(1, wList1.size());
            Way w2 = wList1.get(0);

            // create destination_sign relation from a turn restriction for convenience
            Relation r1 = logic.createRestriction(main, w1, n2, w2, Tags.VALUE_DESTINATION_SIGN);
            java.util.Map<String, String> tags = new TreeMap<>(r1.getTags());
            tags.put(Tags.KEY_TYPE, Tags.VALUE_DESTINATION_SIGN);
            logic.setTags(null, r1, tags);
            List<RelationMember> vias = r1.getMembersWithRole(Tags.ROLE_VIA);
            Assert.assertEquals(1, vias.size());
            vias.get(0).setRole(Tags.ROLE_INTERSECTION);

            // split way 2
            logic.performSplit(main, n3);

            List<Way> wList2 = logic.getWaysForNode(n3);
            // this assumes wList2 contains the way in chronological order
            Assert.assertEquals(2, wList2.size());
            Assert.assertEquals(wList2.get(0), w2);
            Assert.assertEquals(wList2.get(0).getParentRelations().get(0), r1);
            Assert.assertNull(wList2.get(1).getParentRelations()); // special case for restrictions
            Assert.assertEquals(3, r1.getMemberElements().size());
        } catch (Exception igit) {
            Assert.fail(igit.getMessage());
        }
    }

    /**
     * Create a way for further use
     * 
     * @param logic a logic instance
     */
    private void setupWaysForSplit(@NonNull Logic logic) {
        logic.setSelectedWay(null);
        logic.setSelectedNode(null);
        logic.setSelectedRelation(null);
        logic.performAdd(main, 100.0f, 100.0f);
        Assert.assertNotNull(logic.getSelectedNode());
        Assert.assertEquals(1, App.getDelegator().getApiNodeCount());
        logic.performAdd(main, 150.0f, 150.0f);
        logic.performAdd(main, 200.0f, 200.0f);
        logic.performAdd(main, 250.0f, 250.0f);
    }

    /**
     * Split a way that is present in a relation twice
     */
    @UiThreadTest
    @Test
    public void splitDupRelationWay() {
        try {
            // setup some stuff to test relations
            Logic logic = App.getLogic();
            setupWaysForSplit(logic);
            Way w1 = logic.getSelectedWay();
            Assert.assertNotNull(w1);
            ArrayList<Node> nList1 = (ArrayList<Node>) w1.getNodes();
            Assert.assertEquals(4, nList1.size());
            final Node n1 = nList1.get(0);
            final Node n2 = nList1.get(1);
            final Node n3 = nList1.get(2);
            final Node n4 = nList1.get(3);

            // add w1 twice to a normal relation
            List<OsmElement> mList = new ArrayList<OsmElement>();
            mList.add(w1);
            mList.add(w1);
            Relation r = logic.createRelation(main, "test", mList);
            Assert.assertEquals(2, w1.getParentRelations().size());
            // r should have 2 members
            Assert.assertEquals(2, r.getMemberElements().size());

            // split at n2
            logic.performSplit(main, n2);

            // get the the resulting ways
            List<Way> ways = logic.getWaysForNode(n4);
            Assert.assertEquals(1, ways.size());

            if (ways.get(0).equals(w1)) {
                ways = logic.getWaysForNode(n1);
                Assert.assertEquals(1, ways.size());
            }
            Way w2 = ways.get(0);
            // both should have two parents
            Assert.assertEquals(2, w1.getParentRelations().size());
            Assert.assertEquals(2, w2.getParentRelations().size());

            // r should have 4 members now
            Assert.assertEquals(4, r.getMemberElements().size());

            // split at n3
            logic.performSplit(main, n3);

            // r should have 6 members now
            Assert.assertEquals(6, r.getMemberElements().size());
            // w1 and w2 should be unchanged
            Assert.assertEquals(2, w1.getParentRelations().size());
            Assert.assertEquals(2, w2.getParentRelations().size());
        } catch (Exception igit) {
            Assert.fail(igit.getMessage());
        }
    }

    /**
     * Create a closed way Split it
     */
    @UiThreadTest
    @Test
    public void closedWaySplit() {
        try {
            // setup some stuff to test relations
            Logic logic = App.getLogic();
            logic.setSelectedWay(null);
            logic.setSelectedNode(null);
            logic.setSelectedRelation(null);
            logic.performAdd(main, 200.0f, 200.0f);
            Assert.assertNotNull(logic.getSelectedNode());
            Assert.assertEquals(1, App.getDelegator().getApiNodeCount());
            logic.performAdd(main, 200.0f, 400.0f);
            logic.performAdd(main, 400.0f, 400.0f);
            logic.performAdd(main, 400.0f, 200.0f);
            logic.performAdd(main, 200.0f, 200.0f);
            Way w1 = logic.getSelectedWay();
            Assert.assertNotNull(w1);
            ArrayList<Node> nList1 = (ArrayList<Node>) w1.getNodes();
            Assert.assertEquals(5, nList1.size());
            final Node n1 = nList1.get(0);
            final Node n2 = nList1.get(1);
            final Node n4 = nList1.get(3);
            final Node n5 = nList1.get(4);
            Assert.assertEquals(n1, n5);
            Assert.assertTrue(w1.isClosed());
            Way[] ways = logic.performClosedWaySplit(main, w1, n2, n4, false);
            Assert.assertEquals(2, ways.length);
            Assert.assertEquals(3, ways[0].getNodes().size());
            Assert.assertEquals(3, ways[1].getNodes().size());
        } catch (Exception igit) {
            Assert.fail(igit.getMessage());
        }
    }

    /**
     * Create a closed way Split it in to two polygons (closed ways)
     */
    @UiThreadTest
    @Test
    public void closedWaySplitToPolygons() {
        try {
            // setup some stuff to test closed way splitting
            Logic logic = App.getLogic();
            logic.setSelectedWay(null);
            logic.setSelectedNode(null);
            logic.setSelectedRelation(null);
            logic.performAdd(main, 200.0f, 200.0f);
            Assert.assertNotNull(logic.getSelectedNode());
            Assert.assertEquals(1, App.getDelegator().getApiNodeCount());
            logic.performAdd(main, 200.0f, 400.0f);
            logic.performAdd(main, 400.0f, 400.0f);
            logic.performAdd(main, 400.0f, 200.0f);
            logic.performAdd(main, 200.0f, 200.0f);
            Way w1 = logic.getSelectedWay();
            Assert.assertNotNull(w1);
            ArrayList<Node> nList1 = (ArrayList<Node>) w1.getNodes();
            Assert.assertEquals(5, nList1.size());
            final Node n1 = nList1.get(0);
            final Node n3 = nList1.get(2);
            final Node n5 = nList1.get(4);
            Assert.assertEquals(n1, n5);
            Assert.assertTrue(w1.isClosed());
            Way[] ways = logic.performClosedWaySplit(main, w1, n1, n3, true);
            Assert.assertEquals(2, ways.length);
            Assert.assertEquals(4, ways[0].getNodes().size());
            Assert.assertTrue(ways[0].isClosed());
            Assert.assertEquals(4, ways[1].getNodes().size());
            Assert.assertTrue(ways[1].isClosed());
        } catch (Exception igit) {
            Assert.fail(igit.getMessage());
        }
    }

    /**
     * Test bounding box calculation and intersection
     */
    @UiThreadTest
    @Test
    public void wayBoundingBox() {
        try {
            // setup some stuff
            Logic logic = App.getLogic();
            logic.setSelectedWay(null);
            logic.setSelectedNode(null);
            logic.setSelectedRelation(null);
            logic.performAdd(main, 200.0f, 200.0f);
            Assert.assertNotNull(logic.getSelectedNode());
            System.out.println(logic.getSelectedNode());
            Assert.assertEquals(1, App.getDelegator().getApiNodeCount());
            logic.performAdd(main, 200.0f, 400.0f);
            logic.performAdd(main, 400.0f, 400.0f);
            logic.performAdd(main, 400.0f, 200.0f);
            logic.performAdd(main, 200.0f, 200.0f);
            Way w1 = logic.getSelectedWay();
            Assert.assertNotNull(w1);
            Assert.assertTrue(w1.isClosed());
            System.out.println("ApplicationTest created way " + w1.getOsmId());
            logic.setSelectedWay(null);
            logic.setSelectedNode(null);
            logic.setSelectedRelation(null);
            logic.performAdd(main, 400.0f, 400.0f);
            logic.performAdd(main, 200.0f, 600.0f);
            logic.performAdd(main, 800.0f, 800.0f);
            logic.performAdd(main, 600.0f, 600.0f);
            logic.performAdd(main, 400.0f, 400.0f);
            Way w2 = logic.getSelectedWay();
            Assert.assertNotNull(w2);
            Assert.assertTrue(w2.isClosed());
            System.out.println("ApplicationTest created way " + w2.getOsmId());
            ArrayList<Node> nList2 = (ArrayList<Node>) w2.getNodes();
            Assert.assertEquals(5, nList2.size());
            final Node n1 = nList2.get(0);
            List<Way> wayList = logic.getWaysForNode(n1);
            Assert.assertEquals(2, wayList.size());
            Assert.assertTrue(wayList.contains(w1));
            Assert.assertTrue(wayList.contains(w2));
            w1.invalidateBoundingBox();
            BoundingBox box1 = w1.getBounds();
            System.out.println("ApplicationTest bb way1 " + box1.toApiString());
            BoundingBox nodeBox = n1.getBounds();
            System.out.println("ApplicationTest bb node " + nodeBox.toApiString());
            // get box for w1 again it should be the same
            BoundingBox box1cache = w1.getBounds();
            box1cache.makeValidForApi();
            System.out.println("ApplicationTest bb way1 from cache " + box1cache.toApiString());
            // Assert.assertEquals(box1, box1cache);
            BoundingBox box2 = w2.getBounds();
            System.out.println("ApplicationTest bb way2 " + box2.toApiString());
            Assert.assertTrue(box2.intersects(nodeBox));
            List<Way> fromBB = logic.getWays(box1);
            System.out.println("ApplicationTest ways from BB " + fromBB.size());
            Assert.assertEquals(2, fromBB.size());
            System.out.println("ApplicationTest ways from BB " + fromBB.size());
            Assert.assertEquals(2, fromBB.size());
            fromBB = logic.getWays(box2);
        } catch (Exception igit) {
            Assert.fail(igit.getMessage());
        }
    }

    /**
     * This tries to test adding nodes to existing ways taking the tolerance area in to account
     */
    @UiThreadTest
    @Test
    public void addNodeToWay() {
        try {
            App.getDelegator().setOriginalBox(new BoundingBox(-1, -1, 1, 1)); // force ops to be outside box
            Logic logic = App.getLogic();
            Map map = main.getMap();
            logic.setZoom(map, 20);
            float tolerance = DataStyle.getCurrent().getWayToleranceValue();
            System.out.println("Tolerance " + tolerance);

            logic.setSelectedWay(null);
            logic.setSelectedNode(null);
            logic.setSelectedRelation(null);
            logic.performAdd(main, 1000.0f, 0.0f);
            Node wn = logic.getSelectedNode();
            ViewBox box = new ViewBox(wn.getLon(), wn.getLat());
            float wnY = getY(logic, wn);
            float wnX = getX(logic, wn);
            System.out.println("WN1 X " + wnX + " Y " + wnY);
            logic.performAdd(main, 1000.0f, 1000.0f);
            wn = logic.getSelectedNode();
            box.union(wn.getLon(), wn.getLat());
            wnY = getY(logic, wn);
            wnX = getX(logic, wn);
            System.out.println("WN2 X " + wnX + " Y " + wnY);
            Way w1 = logic.getSelectedWay();
            Assert.assertEquals(2, w1.getNodes().size());
            logic.setSelectedWay(null);
            logic.setSelectedNode(null);
            // 2nd way tolerance + 1 pixels away

            float X = 1000.0f + tolerance + 1.0f;
            logic.performAdd(main, X, -tolerance);
            wn = logic.getSelectedNode();
            wnY = getY(logic, wn);
            wnX = getX(logic, wn);
            box.union(wn.getLon(), wn.getLat());
            map.setViewBox(box);
            System.out.println("WN3 X " + wnX + " Y " + wnY);
            logic.performAdd(main, X, 1000.0f + tolerance);
            wn = logic.getSelectedNode();
            wnY = getY(logic, wn);
            wnX = getX(logic, wn);
            box.union(wn.getLon(), wn.getLat());

            System.out.println("WN4 X " + wnX + " Y " + wnY);
            Way w2 = logic.getSelectedWay();
            Assert.assertEquals(2, w2.getNodes().size());
            logic.setSelectedWay(null);
            logic.setSelectedNode(null);

            Node tempNode = logic.performAddOnWay(main, null, X, 500.0f, false);
            Node n1 = logic.getSelectedNode();
            box.union(n1.getLon(), n1.getLat());
            Assert.assertEquals(n1, tempNode);
            Assert.assertEquals(1, logic.getWaysForNode(n1).size());
            logic.setSelectedWay(null);
            logic.setSelectedNode(null);
            Assert.assertTrue(map.getViewBox().contains(n1.getLon(), n1.getLat()));

            Assert.assertEquals(2, w1.getNodes().size()); // should be unchanged
            Assert.assertEquals(3, w2.getNodes().size());
            // add again, shouldn't change anything

            float n1Y = getY(logic, n1);
            float n1X = getX(logic, n1);
            logic.performAdd(main, n1X, n1Y);
            Node n3 = logic.getSelectedNode();
            Assert.assertEquals(n1, n3);
            Assert.assertEquals(2, w1.getNodes().size()); // should be unchanged
            Assert.assertEquals(3, w2.getNodes().size());
        } catch (Exception igit) {
            Assert.fail(igit.getMessage());
        }
    }

    /**
     * Add a node to an existing way
     */
    @UiThreadTest
    @Test
    public void joinToWay() {
        try {
            Logic logic = App.getLogic();
            logic.setSelectedWay(null);
            logic.setSelectedNode(null);
            logic.setSelectedRelation(null);
            logic.performAdd(main, 1000.0f, 0.0f);
            logic.performAdd(main, 1000.0f, 1000.0f);
            Way w1 = logic.getSelectedWay();
            logic.setSelectedWay(null);
            logic.setSelectedNode(null);
            int lon = GeoMath.xToLonE7(logic.getMap().getWidth(), logic.getViewBox(), 1001.0f);
            int lat = GeoMath.yToLatE7(logic.getMap().getHeight(), logic.getMap().getWidth(), logic.getViewBox(), 500.0f);
            Node n1 = logic.performAddNode(main, lon, lat);
            Assert.assertEquals(0, logic.getWaysForNode(n1).size());
            MergeResult result = logic.performJoinNodeToWays(main, Util.wrapInList(w1), n1);
            Assert.assertTrue(w1.hasNode(n1));
            Assert.assertFalse(result.hasIssue());
        } catch (Exception igit) {
            Assert.fail(igit.getMessage());
        }
    }

    /**
     * Merge two nodes, undo, add conflicting tags re-merge
     */
    @UiThreadTest
    @Test
    public void joinToNode() {
        try {
            Logic logic = App.getLogic();
            logic.setSelectedWay(null);
            logic.setSelectedNode(null);
            logic.setSelectedRelation(null);
            logic.performAdd(main, 1000.0f, 1000.0f);
            Node n1 = logic.getSelectedNode();
            logic.setSelectedWay(null);
            logic.setSelectedNode(null);
            int lon = GeoMath.xToLonE7(logic.getMap().getWidth(), logic.getViewBox(), 1001.0f);
            int lat = GeoMath.yToLatE7(logic.getMap().getHeight(), logic.getMap().getWidth(), logic.getViewBox(), 1001.0f);
            Node n2 = logic.performAddNode(main, lon, lat);
            Assert.assertEquals(2, App.getDelegator().getApiNodeCount());
            MergeResult result = logic.performMergeNodes(main, Util.wrapInList(n1), n2);
            Assert.assertEquals(1, App.getDelegator().getApiNodeCount());
            Assert.assertFalse(result.hasIssue());
            logic.undo();
            Assert.assertEquals(2, App.getDelegator().getApiNodeCount());
            java.util.Map<String, String> tags = new HashMap<>();
            tags.put("highway", "residential");
            logic.setTags(main, n1, tags);
            tags.clear();
            tags.put("highway", "unclassified");
            logic.setTags(main, n2, tags);
            result = logic.performMergeNodes(main, Util.wrapInList(n1), n2);
            Assert.assertTrue(result.hasIssue());
            Assert.assertEquals(1, result.getIssues().size());
            Assert.assertTrue(result.getIssues().contains(Issue.MERGEDTAGS));
        } catch (Exception igit) {
            Assert.fail(igit.getMessage());
        }
    }

    /**
     * Unjoin ways with common nodes, first in normal mode, aka no connections should remain, then in unjoin dissimilar
     * which should maintain connection to similar ways
     */
    @UiThreadTest
    @Test
    public void unjoinWay() {
        try {
            Logic logic = App.getLogic();
            logic.setSelectedWay(null);
            logic.setSelectedNode(null);
            logic.setSelectedRelation(null);
            logic.performAdd(main, 200.0f, 200.0f);
            Assert.assertNotNull(logic.getSelectedNode());
            logic.performAdd(main, 200.0f, 400.0f);
            logic.performAdd(main, 400.0f, 400.0f);
            logic.performAdd(main, 400.0f, 200.0f);
            Way w1 = logic.getSelectedWay();
            TreeMap<String, String> tags = new TreeMap<>();
            tags.put(Tags.KEY_HIGHWAY, Tags.VALUE_MOTORWAY);
            w1.setTags(tags);
            logic.setSelectedWay(null);
            logic.setSelectedNode(null);
            logic.performAdd(main, 200.0f, 200.0f);
            Assert.assertNotNull(logic.getSelectedNode());
            logic.performAdd(main, 200.0f, 400.0f);
            logic.performAdd(main, 400.0f, 400.0f);
            Way w2 = logic.getSelectedWay();
            logic.setSelectedWay(null);
            logic.setSelectedNode(null);
            logic.performAdd(main, 400.0f, 400.0f);
            logic.performAdd(main, 600.0f, 600.0f);
            Way w3 = logic.getSelectedWay();
            logic.setSelectedWay(null);
            logic.setSelectedNode(null);
            w3.setTags(tags);
            Assert.assertTrue(w1.hasCommonNode(w2));
            Assert.assertTrue(w2.hasCommonNode(w3));
            Assert.assertTrue(w1.hasCommonNode(w3));
            logic.performUnjoinWay(main, w1, false);
            Assert.assertFalse(w1.hasCommonNode(w2));
            Assert.assertFalse(w1.hasCommonNode(w3));
            logic.undo();
            Assert.assertTrue(w1.hasCommonNode(w2));
            Assert.assertTrue(w2.hasCommonNode(w3));
            logic.performUnjoinWay(main, w1, false);
            Assert.assertFalse(w1.hasCommonNode(w2));
            Assert.assertTrue(w2.hasCommonNode(w3));
        } catch (Exception igit) {
            Assert.fail(igit.getMessage());
        }
    }

    /**
     * Cut and then paste a way
     */
    @UiThreadTest
    @Test
    public void cutPasteWay() {
        try {
            Logic logic = App.getLogic();
            logic.setSelectedWay(null);
            logic.setSelectedNode(null);
            logic.setSelectedRelation(null);
            logic.performAdd(main, 1000.0f, 0.0f);
            logic.performAdd(main, 1000.0f, 1000.0f);
            Way w1 = logic.getSelectedWay();
            logic.setSelectedWay(null);
            logic.setSelectedNode(null);
            logic.cutToClipboard(null, w1);
            Assert.assertTrue(w1.getState() == OsmElement.STATE_DELETED);
            logic.pasteFromClipboard(null, 500.0f, 500.0f);
            Assert.assertTrue(w1.getState() == OsmElement.STATE_CREATED);

            // w1 should now have both nodes in the same place
            App.getDelegator().moveNode(w1.getLastNode(), w1.getFirstNode().getLat(), w1.getFirstNode().getLon());

            logic.cutToClipboard(null, w1);
            Assert.assertTrue(w1.getState() == OsmElement.STATE_DELETED);
            logic.pasteFromClipboard(null, 0.0f, 0.0f);
            Assert.assertTrue(w1.getState() == OsmElement.STATE_CREATED);

        } catch (Exception igit) {
            Assert.fail(igit.getMessage());
        }
    }

    /**
     * Cut and then paste a closed way
     */
    @UiThreadTest
    @Test
    public void cutPasteClosedWay() {
        try {
            Logic logic = App.getLogic();
            logic.setSelectedWay(null);
            logic.setSelectedNode(null);
            logic.setSelectedRelation(null); // this is fairly fragile
            logic.performAdd(main, 300.0f, 300.0f);
            logic.performAdd(main, 300.0f, 500.0f);
            logic.performAdd(main, 100.0f, 500.0f);
            logic.performAdd(main, 300.0f, 300.0f);
            Way w1 = logic.getSelectedWay();
            Assert.assertTrue(w1.isClosed());
            logic.setSelectedWay(null);
            logic.setSelectedNode(null);
            logic.cutToClipboard(null, w1);
            Assert.assertTrue(w1.getState() == OsmElement.STATE_DELETED);
            logic.pasteFromClipboard(null, 500.0f, 500.0f);
            Assert.assertTrue(w1.getState() == OsmElement.STATE_CREATED);

            // collapse the area
            int lat = w1.getFirstNode().getLat();
            int lon = w1.getFirstNode().getLon();
            for (Node n : w1.getNodes()) {
                App.getDelegator().moveNode(n, lat, lon);
            }

            logic.cutToClipboard(null, w1);
            Assert.assertTrue(w1.getState() == OsmElement.STATE_DELETED);
            logic.pasteFromClipboard(null, 0.0f, 0.0f);
            Assert.assertTrue(w1.getState() == OsmElement.STATE_CREATED);

        } catch (Exception igit) {
            Assert.fail(igit.getMessage());
        }
    }

    /**
     * Remove way nodes one by one from a Way
     */
    @UiThreadTest
    @Test
    public void wayNodeDelete() {
        try {
            Logic logic = App.getLogic();
            logic.setSelectedWay(null);
            logic.setSelectedNode(null);
            logic.setSelectedRelation(null); // this is fairly fragile
            logic.performAdd(main, 300.0f, 300.0f);
            logic.performAdd(main, 300.0f, 500.0f);
            logic.performAdd(main, 100.0f, 500.0f);
            Way w1 = logic.getSelectedWay();
            BoundingBox origBox = w1.getBounds();
            List<Node> nodes = new ArrayList<>(w1.getNodes());
            for (Node n : nodes) {
                logic.performEraseNode(main, n, true);
            }
            Assert.assertTrue(w1.getState() == OsmElement.STATE_DELETED);
            logic.undo(); // way should still be deleted
            Assert.assertTrue(w1.getState() == OsmElement.STATE_DELETED);
            logic.undo();
            Assert.assertTrue(w1.getState() == OsmElement.STATE_CREATED);
            logic.undo();
            Assert.assertEquals(origBox.getLeft(), w1.getBounds().getLeft());
            Assert.assertEquals(origBox.getBottom(), w1.getBounds().getBottom());
            Assert.assertEquals(origBox.getRight(), w1.getBounds().getRight());
            Assert.assertEquals(origBox.getTop(), w1.getBounds().getTop());
        } catch (Exception igit) {
            Assert.fail(igit.getMessage());
        }
    }

    /**
     * Return the screen X coordinate for a node
     * 
     * @param logic the current logic instance
     * @param n the node
     * @return the screen X coordinate
     */
    private float getX(Logic logic, Node n) {
        return GeoMath.lonE7ToX(logic.getMap().getWidth(), logic.getViewBox(), n.getLon());
    }

    /**
     * Return the screen Y coordinate for a node
     * 
     * @param logic the current logic instance
     * @param n the node
     * @return the screen Y coordinate
     */
    private float getY(Logic logic, Node n) {
        return GeoMath.latE7ToY(logic.getMap().getHeight(), logic.getMap().getWidth(), logic.getViewBox(), n.getLat());
    }

    /**
     * REverse a way with a lot of direction dependent tags
     */
    @UiThreadTest
    @Test
    public void reverseWay() {
        try {
            Logic logic = App.getLogic();
            logic.setSelectedWay(null);
            logic.setSelectedNode(null);
            logic.setSelectedRelation(null); // this is fairly fragile
            logic.performAdd(main, 300.0f, 300.0f);
            logic.performAdd(main, 300.0f, 500.0f);
            Way w = logic.getSelectedWay();

            List<OsmElement> members = new ArrayList<OsmElement>();
            members.add(w);
            Relation route = logic.createRelation(main, Tags.VALUE_ROUTE, null);
            App.getDelegator().addMemberToRelation(w, Tags.ROLE_FORWARD, route);

            HashMap<String, String> tags = new HashMap<String, String>();
            // first lot of tags
            tags.put(Tags.KEY_ONEWAY, Tags.VALUE_YES);
            tags.put(Tags.KEY_DIRECTION, Tags.VALUE_UP);
            tags.put(Tags.KEY_INCLINE, "10%");
            tags.put(Tags.KEY_SIDEWALK, Tags.VALUE_RIGHT);
            tags.put(Tags.KEY_TURN_LANES + ":forward", Tags.VALUE_THROUGH + "|" + Tags.VALUE_RIGHT);
            tags.put(Tags.KEY_TURN_LANES + ":backward", Tags.VALUE_LEFT + "|" + Tags.VALUE_RIGHT);
            logic.setTags(main, w, tags);
            Assert.assertTrue(logic.performReverse(main, w)); // suppose we could press the button here
            Assert.assertTrue(Tags.ROLE_BACKWARD.equals(route.getMember(w).getRole()));
            // when we are interactively reserving the way, we assume that it is because we
            // we actually want to change the oneway direction and not correct by reversing the tags
            Assert.assertEquals(Tags.VALUE_YES, w.getTagWithKey(Tags.KEY_ONEWAY));
            Assert.assertEquals(Tags.VALUE_DOWN, w.getTagWithKey(Tags.KEY_DIRECTION));
            Assert.assertEquals("-10%", w.getTagWithKey(Tags.KEY_INCLINE));
            Assert.assertEquals(Tags.VALUE_LEFT, w.getTagWithKey(Tags.KEY_SIDEWALK));
            Assert.assertEquals(Tags.VALUE_THROUGH + "|" + Tags.VALUE_RIGHT, w.getTagWithKey(Tags.KEY_TURN_LANES + ":backward"));
            Assert.assertEquals(Tags.VALUE_LEFT + "|" + Tags.VALUE_RIGHT, w.getTagWithKey(Tags.KEY_TURN_LANES + ":forward"));
            Assert.assertTrue(logic.performReverse(main, w));
            Assert.assertTrue(Tags.ROLE_FORWARD.equals(route.getMember(w).getRole()));
            Assert.assertEquals(Tags.VALUE_YES, w.getTagWithKey(Tags.KEY_ONEWAY));
            Assert.assertEquals(Tags.VALUE_UP, w.getTagWithKey(Tags.KEY_DIRECTION));
            Assert.assertEquals("10%", w.getTagWithKey(Tags.KEY_INCLINE));
            Assert.assertEquals(Tags.VALUE_RIGHT, w.getTagWithKey(Tags.KEY_SIDEWALK));
            Assert.assertEquals(Tags.VALUE_THROUGH + "|" + Tags.VALUE_RIGHT, w.getTagWithKey(Tags.KEY_TURN_LANES + ":forward"));
            Assert.assertEquals(Tags.VALUE_LEFT + "|" + Tags.VALUE_RIGHT, w.getTagWithKey(Tags.KEY_TURN_LANES + ":backward"));

            route.removeMember(route.getMember(w));
            tags.clear();
            // 2nd lot of tags
            tags.put(Tags.KEY_DIRECTION, String.valueOf(Tags.VALUE_NORTH));
            tags.put(Tags.KEY_INCLINE, Tags.VALUE_UP);
            logic.setTags(main, w, tags);
            Assert.assertTrue(logic.performReverse(main, w));
            Assert.assertEquals(String.valueOf(Tags.VALUE_SOUTH), w.getTagWithKey(Tags.KEY_DIRECTION));
            Assert.assertEquals(Tags.VALUE_DOWN, w.getTagWithKey(Tags.KEY_INCLINE));
            Assert.assertTrue(logic.performReverse(main, w));
            Assert.assertEquals(String.valueOf(Tags.VALUE_NORTH), w.getTagWithKey(Tags.KEY_DIRECTION));
            Assert.assertEquals(Tags.VALUE_UP, w.getTagWithKey(Tags.KEY_INCLINE));

            tags.clear();
            // 3rd lot of tags
            tags.put(Tags.KEY_DIRECTION, "200°");
            tags.put(Tags.KEY_INCLINE, "10°");
            logic.setTags(main, w, tags);
            Assert.assertTrue(logic.performReverse(main, w));
            Assert.assertEquals("20°", w.getTagWithKey(Tags.KEY_DIRECTION));
            Assert.assertEquals("-10°", w.getTagWithKey(Tags.KEY_INCLINE));
            Assert.assertTrue(logic.performReverse(main, w));
            Assert.assertEquals("200°", w.getTagWithKey(Tags.KEY_DIRECTION));
            Assert.assertEquals("10°", w.getTagWithKey(Tags.KEY_INCLINE));

            tags.clear();
            // 4th lot of tags
            tags.put(Tags.KEY_DIRECTION, "200");
            tags.put(Tags.KEY_INCLINE, "10");
            logic.setTags(main, w, tags);
            Assert.assertTrue(logic.performReverse(main, w));
            Assert.assertEquals("20", w.getTagWithKey(Tags.KEY_DIRECTION));
            Assert.assertEquals("-10", w.getTagWithKey(Tags.KEY_INCLINE));
            Assert.assertTrue(logic.performReverse(main, w));
            Assert.assertEquals("200", w.getTagWithKey(Tags.KEY_DIRECTION));
            Assert.assertEquals("10", w.getTagWithKey(Tags.KEY_INCLINE));

            tags.clear();
            // 5th lot of tags
            tags.put(Tags.KEY_HIGHWAY, Tags.VALUE_MOTORWAY);
            tags.put(Tags.KEY_DIRECTION, String.valueOf(Tags.VALUE_EAST));

            logic.setTags(main, w, tags);
            Assert.assertTrue(logic.performReverse(main, w));
            Assert.assertFalse(w.hasTagKey(Tags.KEY_ONEWAY));
            Assert.assertEquals(String.valueOf(Tags.VALUE_WEST), w.getTagWithKey(Tags.KEY_DIRECTION));
            Assert.assertTrue(logic.performReverse(main, w));
            Assert.assertFalse(w.hasTagKey(Tags.KEY_ONEWAY));
            Assert.assertEquals(String.valueOf(Tags.VALUE_EAST), w.getTagWithKey(Tags.KEY_DIRECTION));

            // reverse "one way" too now
            java.util.Map<String, String> dirTags = Reverse.getDirectionDependentTags(w);
            if (dirTags != null) {
                Reverse.reverseDirectionDependentTags(w, dirTags, true);
            } else {
                Assert.fail("no direction dependent tags found");
            }
            Assert.assertEquals("-1", w.getTagWithKey(Tags.KEY_ONEWAY));

            dirTags = Reverse.getDirectionDependentTags(w);
            if (dirTags != null) {
                Reverse.reverseDirectionDependentTags(w, dirTags, true);
            } else {
                Assert.fail("no direction dependent tags found");
            }
            Assert.assertEquals(Tags.VALUE_YES, w.getTagWithKey(Tags.KEY_ONEWAY));

        } catch (Exception igit) {
            Assert.fail(igit.getMessage());
        }
    }

    /**
     * Move a way and then try to move it out of bounds
     */
    @UiThreadTest
    @Test
    public void moveWay() {
        try {
            // setup some stuff to test relations
            Logic logic = App.getLogic();
            setupWaysForSplit(logic);
            Way w1 = logic.getSelectedWay();
            Assert.assertNotNull(w1);
            StorageDelegator delegator = App.getDelegator();
            List<Node> oldNodes = w1.getNodes();
            int nodeCount = oldNodes.size();
            int[] oldLat = new int[nodeCount];
            int[] oldLon = new int[nodeCount];
            for (int i = 0; i < nodeCount; i++) {
                oldLat[i] = oldNodes.get(i).getLat();
                oldLon[i] = oldNodes.get(i).getLon();
            }
            delegator.moveWay(w1, (int) (10 * 1E7), (int) (5 * 1E7));
            List<Node> newNodes = w1.getNodes();
            Assert.assertEquals(nodeCount, newNodes.size());
            for (int i = 0; i < nodeCount; i++) {
                Node newNode = newNodes.get(i);
                Assert.assertEquals(oldLat[i] + (int) (10 * 1E7), newNode.getLat());
                Assert.assertEquals(oldLon[i] + (int) (5 * 1E7), newNode.getLon());
            }
            try {
                delegator.moveWay(w1, (int) (90 * 1E7), (int) (5 * 1E7));
                Assert.fail("should have got an exception here");
            } catch (OsmIllegalOperationException ex) {
                // good
            }
            // the way should be unchanged
            Assert.assertEquals(nodeCount, newNodes.size());
            for (int i = 0; i < nodeCount; i++) {
                Node newNode = newNodes.get(i);
                Assert.assertEquals(oldLat[i] + (int) (10 * 1E7), newNode.getLat());
                Assert.assertEquals(oldLon[i] + (int) (5 * 1E7), newNode.getLon());
            }
        } catch (Exception igit) {
            igit.printStackTrace();
            Assert.fail(igit.getMessage());
        }
    }

    /**
     * Move a node and then try to move it out of bounds
     */
    @UiThreadTest
    @Test
    public void moveNode() {
        try {
            // setup some stuff to test relations
            Logic logic = App.getLogic();
            logic.setSelectedWay(null);
            logic.setSelectedNode(null);
            logic.setSelectedRelation(null);
            logic.performAdd(main, 100.0f, 100.0f);
            Assert.assertNotNull(logic.getSelectedNode());
            System.out.println(logic.getSelectedNode());
            Assert.assertEquals(1, App.getDelegator().getApiNodeCount());
            Node node = logic.getSelectedNode();
            StorageDelegator delegator = App.getDelegator();
            int oldLat = node.getLat();
            int oldLon = node.getLon();
            delegator.moveNode(node, oldLat + (int) (10 * 1E7), oldLon + (int) (5 * 1E7));
            Assert.assertEquals(oldLat + (int) (10 * 1E7), node.getLat());
            Assert.assertEquals(oldLon + (int) (5 * 1E7), node.getLon());
            try {
                delegator.moveNode(node, (int) (90 * 1E7), (int) (5 * 1E7));
                Assert.fail("should have got an exception here");
            } catch (OsmIllegalOperationException ex) {
                // good
            }
            // the node should be unchanged
            Assert.assertEquals(oldLat + (int) (10 * 1E7), node.getLat());
            Assert.assertEquals(oldLon + (int) (5 * 1E7), node.getLon());
        } catch (Exception igit) {
            igit.printStackTrace();
            Assert.fail(igit.getMessage());
        }
    }

    /**
     * Create a two node way and then merge the two nodes, this should delete the way and one of the nodes
     */
    @UiThreadTest
    @Test
    public void mergeWayNodes() {
        try {
            Logic logic = App.getLogic();
            logic.setSelectedWay(null);
            logic.setSelectedNode(null);
            logic.setSelectedRelation(null);
            logic.performAdd(main, 100.0f, 100.0f);
            Assert.assertNotNull(logic.getSelectedNode());
            System.out.println(logic.getSelectedNode());
            Assert.assertEquals(1, App.getDelegator().getApiNodeCount());
            logic.performAdd(main, 150.0f, 150.0f);
            Way w1 = logic.getSelectedWay();
            Assert.assertNotNull(w1);
            StorageDelegator delegator = App.getDelegator();
            List<Node> nodes = w1.getNodes();
            Assert.assertEquals(2, nodes.size());
            logic.performMergeNodes(main, Util.wrapInList(nodes.get(0)), nodes.get(1));
            Assert.assertNull(delegator.getCurrentStorage().getWay(w1.getOsmId()));
            Assert.assertNull(delegator.getApiStorage().getWay(w1.getOsmId()));
        } catch (Exception igit) {
            igit.printStackTrace();
            Assert.fail(igit.getMessage());
        }
    }
}
