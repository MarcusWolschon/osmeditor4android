package de.blau.android.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.Test;

import androidx.annotation.NonNull;
import de.blau.android.exception.OsmException;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.osm.MergeResult.Issue;
import de.blau.android.util.Coordinates;
import de.blau.android.util.Geometry;
import de.blau.android.util.Util;

public class StorageDelegatorTest {

    /**
     * Test way rotation
     */
    @Test
    public void rotate() {
        StorageDelegator d = new StorageDelegator();
        Way w = addWayToStorage(d, true);
        Node n0 = w.getNodes().get(0);

        try {
            d.getUndo().createCheckpoint("rotate test");
            ViewBox v = new ViewBox(0D, 51.476D, 0.003D, 51.478D);
            Coordinates[] coords = Coordinates.nodeListToCooardinateArray(1000, 2000, v, new ArrayList<>(w.getNodes()));
            Coordinates center = Geometry.centroidXY(coords, true);
            d.rotateWay(w, (float) Math.toRadians(180), -1, (float) center.x, (float) center.y, 1000, 2000, v);
            assertEquals(514760000, n0.getLat());
            assertEquals(30000, n0.getLon());
        } catch (OsmException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Test copy
     */
    @Test
    public void copy() {
        StorageDelegator d = new StorageDelegator();
        Way w = addWayToStorage(d, true);

        Way temp = (Way) d.getOsmElement(Way.NAME, w.getOsmId());
        assertNotNull(temp);
        double[] centroid = Geometry.centroidLonLat(w);
        d.copyToClipboard(Util.wrapInList(w), toE7(centroid[1]), toE7(centroid[0]));
        assertNotNull((Way) d.getOsmElement(Way.NAME, w.getOsmId()));
        List<OsmElement> pasted = d.pasteFromClipboard(toE7(centroid[1] + 1), toE7(centroid[0] + 1));
        assertEquals(1, pasted.size());
        temp = (Way) d.getOsmElement(Way.NAME, pasted.get(0).getOsmId());
        assertNotNull(temp);
        assertFalse(d.clipboardIsEmpty());
        double[] centroid2 = Geometry.centroidLonLat(temp);
        assertEquals(centroid[1] + 1, centroid2[1], 0.001);
        assertEquals(centroid[0] + 1, centroid2[0], 0.001);
    }

    /**
     * Test cut
     */
    @Test
    public void cut() {
        StorageDelegator d = new StorageDelegator();
        Way w = addWayToStorage(d, true);

        Way temp = (Way) d.getOsmElement(Way.NAME, w.getOsmId());
        assertNotNull(temp);
        double[] centroid = Geometry.centroidLonLat(w);
        d.cutToClipboard(Util.wrapInList(w), toE7(centroid[1]), toE7(centroid[0]));
        assertNull((Way) d.getOsmElement(Way.NAME, w.getOsmId()));
        d.pasteFromClipboard(toE7(centroid[1] + 1), toE7(centroid[0] + 1));
        temp = (Way) d.getOsmElement(Way.NAME, w.getOsmId());
        assertNotNull(temp);
        assertTrue(d.clipboardIsEmpty());
        double[] centroid2 = Geometry.centroidLonLat(temp);
        assertEquals(centroid[1] + 1, centroid2[1], 0.001);
        assertEquals(centroid[0] + 1, centroid2[0], 0.001);
    }

    /**
     * Add a test way to storage and return it
     * 
     * @param d the StorageDeleagot instance
     * @param close if true close the way
     * @return the way
     */
    static Way addWayToStorage(@NonNull StorageDelegator d, boolean close) {
        d.getUndo().createCheckpoint("add test way");
        OsmElementFactory factory = d.getFactory();
        Way w = factory.createWayWithNewId();
        Node n0 = factory.createNodeWithNewId(toE7(51.478), toE7(0));
        d.insertElementSafe(n0);
        w.addNode(n0);
        Node n1 = factory.createNodeWithNewId(toE7(51.478), toE7(0.003));
        d.insertElementSafe(n1);
        w.addNode(n1);
        Node n2 = factory.createNodeWithNewId(toE7(51.476), toE7(0.003));
        d.insertElementSafe(n2);
        w.addNode(n2);
        Node n3 = factory.createNodeWithNewId(toE7(51.476), toE7(0));
        d.insertElementSafe(n3);
        w.addNode(n3);
        if (close) {
            w.addNode(n0); // close
        }
        d.insertElementSafe(w);
        Relation r = factory.createRelationWithNewId();
        RelationMember member = new RelationMember("test", w);
        r.addMember(member);
        d.insertElementSafe(r);
        w.addParentRelation(r);
        return w;
    }

    /**
     * Load some data modify a way and a node, then prune
     */
    @Test
    public void pruneAll() {
        StorageDelegator d = new StorageDelegator();
        d.setCurrentStorage(PbfTest.read());
        assertEquals(0, d.getApiElementCount());
        assertEquals(258905, d.getCurrentStorage().getNodeCount());
        assertEquals(26454, d.getCurrentStorage().getWayCount());
        assertEquals(751, d.getCurrentStorage().getRelationCount());
        Way w = (Way) d.getOsmElement(Way.NAME, 571067343L);
        int wayNodeCount = w.nodeCount();
        assertNotNull(w);
        int parentCount = w.getParentRelations().size();
        SortedMap<String, String> tags = new TreeMap<>(w.getTags());
        tags.put("test", "pruneAll");
        d.getUndo().createCheckpoint("pruneAll");
        d.setTags(w, tags);
        assertEquals(1, d.getApiElementCount());
        Node n = (Node) d.getOsmElement(Node.NAME, 761534749L);
        assertNotNull(n);
        n.setLat(toE7(47.1187142));
        n.setLon(toE7(9.5430107));
        d.insertElementSafe(n);
        d.pruneAll();
        assertNotNull(d.getOsmElement(Way.NAME, 571067343L));
        assertNotNull(d.getOsmElement(Node.NAME, 761534749L));
        assertEquals(wayNodeCount + 1L, d.getCurrentStorage().getNodeCount());
        assertEquals(1, d.getCurrentStorage().getWayCount());
        assertEquals(32, d.getCurrentStorage().getRelationCount());
        assertEquals(parentCount, d.getOsmElement(Way.NAME, 571067343L).getParentRelations().size());
    }

    /**
     * Load some data modify a way and a node, then prune
     */
    @Test
    public void prune() {
        StorageDelegator d = new StorageDelegator();
        d.setCurrentStorage(PbfTest.read());
        assertEquals(0, d.getApiElementCount());
        assertEquals(258905, d.getCurrentStorage().getNodeCount());
        assertEquals(26454, d.getCurrentStorage().getWayCount());
        assertEquals(751, d.getCurrentStorage().getRelationCount());
        Way w = (Way) d.getOsmElement(Way.NAME, 571067343L);

        assertNotNull(w);
        int parentCount = w.getParentRelations().size();
        SortedMap<String, String> tags = new TreeMap<>(w.getTags());
        tags.put("test", "prunBox");
        d.getUndo().createCheckpoint("pruneBox");
        d.setTags(w, tags);
        assertEquals(1, d.getApiElementCount());
        Node n = (Node) d.getOsmElement(Node.NAME, 761534749L);
        assertNotNull(n);
        n.setLat(toE7(47.1187142));
        n.setLon(toE7(9.5430107));
        d.insertElementSafe(n);
        d.prune(null, new BoundingBox(9.52077, 47.13829, 9.52248, 47.14087));
        assertNotNull(d.getOsmElement(Way.NAME, 571067343L));
        assertNotNull(d.getOsmElement(Node.NAME, 761534749L));
        assertNotNull(d.getOsmElement(Relation.NAME, 30544L));
        assertNotNull(d.getOsmElement(Relation.NAME, 8130924L));
        assertNotNull(d.getOsmElement(Relation.NAME, 8134437L));
        assertNull(d.getOsmElement(Relation.NAME, 6907800L));
        assertEquals(1989, d.getCurrentStorage().getNodeCount());
        assertEquals(103, d.getCurrentStorage().getWayCount());
        assertEquals(74, d.getCurrentStorage().getRelationCount());
        d.prune(null, new BoundingBox(9.52077, 47.13829, 9.52248, 47.14087));
        assertEquals(56, d.getCurrentStorage().getRelationCount());
        assertNotNull(d.getOsmElement(Relation.NAME, 30544L));
        assertNull(d.getOsmElement(Relation.NAME, 8130924L));
        assertNotNull(d.getOsmElement(Relation.NAME, 8134437L));
        assertEquals(parentCount, d.getOsmElement(Way.NAME, 571067343L).getParentRelations().size());
        List<OsmElement> downloaded = new ArrayList<>();
        for (RelationMember rm : ((Relation) d.getOsmElement(Relation.NAME, 8134437L)).getMembers()) {
            if (rm.getElement() != null) {
                downloaded.add(rm.getElement());
            }
        }
        // 5484325404 1107468615
        assertTrue(downloaded.contains(d.getOsmElement(Node.NAME, 5484325404L)));
        assertTrue(downloaded.contains(d.getOsmElement(Node.NAME, 1107468615L)));
        // 571067336 571067343 571452863 529794576 140309501
        assertTrue(downloaded.contains(d.getOsmElement(Way.NAME, 571067336L)));
        assertTrue(downloaded.contains(d.getOsmElement(Way.NAME, 571067343L)));
        assertTrue(downloaded.contains(d.getOsmElement(Way.NAME, 571452863L)));
        assertTrue(downloaded.contains(d.getOsmElement(Way.NAME, 529794576L)));
        assertTrue(downloaded.contains(d.getOsmElement(Way.NAME, 140309501L)));
    }

    /**
     * Load some data modify a way and a node, then merge some data
     */
    @Test
    public void mergeData() {
        StorageDelegator d = new StorageDelegator();
        d.setCurrentStorage(PbfTest.read());
        assertEquals(0, d.getApiElementCount());
        final int nodeCount = 258905;
        assertEquals(nodeCount, d.getCurrentStorage().getNodeCount());
        final int wayCount = 26454;
        assertEquals(wayCount, d.getCurrentStorage().getWayCount());
        assertEquals(751, d.getCurrentStorage().getRelationCount());
        Way w = (Way) d.getOsmElement(Way.NAME, 571067343L);
        assertNotNull(w);
        SortedMap<String, String> tags = new TreeMap<>(w.getTags());
        tags.put("test", "merge");
        d.getUndo().createCheckpoint("merge");
        d.setTags(w, tags);
        assertEquals(1, d.getApiElementCount());
        Node n = (Node) d.getOsmElement(Node.NAME, 761534749L);
        assertNotNull(n);
        n.setLat(toE7(47.1187142));
        n.setLon(toE7(9.5430107));
        d.insertElementSafe(n);

        StorageDelegator d2 = new StorageDelegator();
        Way w2 = addWayToStorage(d2, true);

        d.mergeData(d2.getCurrentStorage(), null);

        assertNotNull(d.getOsmElement(Way.NAME, 571067343L));
        assertNotNull(d.getOsmElement(Way.NAME, w2.getOsmId()));
        assertNotNull(d.getOsmElement(Node.NAME, 761534749L));
        assertEquals(nodeCount + 4L, d.getCurrentStorage().getNodeCount());
        assertEquals(wayCount + 1L, d.getCurrentStorage().getWayCount());
    }

    /**
     * Split way then merge in various ways
     */
    @Test
    public void merge() {
        StorageDelegator d = new StorageDelegator();
        Way w = addWayToStorage(d, false);
        SortedMap<String, String> tags = new TreeMap<>(w.getTags());
        tags.put(Tags.KEY_HIGHWAY, "residential");
        w.setTags(tags);
        Way temp = (Way) d.getOsmElement(Way.NAME, w.getOsmId());
        assertNotNull(temp);
        Node n = w.getNodes().get(2);
        Node first = w.getFirstNode();
        Node last = w.getLastNode();
        Way newWay = d.splitAtNode(w, n);
        // all things the same the 1st way remains after merger
        MergeResult result = d.mergeWays(w, newWay);
        assertNull(result.getIssues());
        assertEquals(4, w.getNodes().size());
        assertNull(d.getOsmElement(Way.NAME, newWay.getOsmId()));
        newWay = d.splitAtNode(w, n);
        result = d.mergeWays(newWay, w);
        assertNull(result.getIssues());
        assertEquals(4, newWay.getNodes().size());
        assertNull(d.getOsmElement(Way.NAME, w.getOsmId()));
        //
        w = d.splitAtNode(newWay, n);
        d.reverseWay(w);
        result = d.mergeWays(w, newWay);
        assertNull(result.getIssues());
        assertEquals(4, w.getNodes().size());
        assertEquals(last, w.getFirstNode());
        assertEquals(first, w.getLastNode());
        //
        newWay = d.splitAtNode(w, n);
        d.reverseWay(w);
        result = d.mergeWays(w, newWay);
        assertNull(result.getIssues());
        assertEquals(4, w.getNodes().size());
        assertEquals(first, w.getFirstNode());
        assertEquals(last, w.getLastNode());
        // conflicting tags should allow merge but create non null result
        newWay = d.splitAtNode(w, n);
        tags.clear();
        tags.put(Tags.KEY_HIGHWAY, "service");
        newWay.setTags(tags);
        result = d.mergeWays(w, newWay);
        assertEquals(4, w.getNodes().size());
        assertNotNull(result.getIssues());
        assertEquals(1, result.getIssues().size());
        assertTrue(result.getIssues().contains(Issue.MERGEDTAGS));
        tags.clear();
        w.setTags(tags);
        // conflicting roles should allow merge but create non null result
        newWay = d.splitAtNode(w, n);
        assertNotNull(newWay.getParentRelations());
        Relation r = newWay.getParentRelations().get(0);
        r.getMember(newWay).setRole("test2");
        result = d.mergeWays(w, newWay);
        assertEquals(4, w.getNodes().size());
        assertNotNull(result.getIssues());
        assertEquals(1, result.getIssues().size());
        assertTrue(result.getIssues().contains(Issue.ROLECONFLICT));
        // way with pos id should remain
        newWay = d.splitAtNode(w, n);
        newWay.setOsmId(1234L);
        result = d.mergeWays(w, newWay);
        assertNull(result.getIssues());
        assertEquals(4, newWay.getNodes().size());
        assertNull(d.getOsmElement(Way.NAME, w.getOsmId()));
        assertNotNull(d.getOsmElement(Way.NAME, 1234L));
        // unjoin ways
        w = d.splitAtNode(newWay, n);
        d.unjoinWays(n);
        try {
            d.mergeWays(w, newWay);
            fail("Should have thrown an OsmIllegalOperationException");
        } catch (OsmIllegalOperationException ex) {
            // expected
        }
    }

    /**
     * Merge two nodes
     */
    @Test
    public void mergeNodes() {
        StorageDelegator d = new StorageDelegator();
        OsmElementFactory factory = d.getFactory();
        Node n1 = factory.createNodeWithNewId(StorageDelegatorTest.toE7(51.476), StorageDelegatorTest.toE7(0.006));
        d.insertElementSafe(n1);
        Node n2 = factory.createNodeWithNewId(StorageDelegatorTest.toE7(51.476), StorageDelegatorTest.toE7(0.006));
        d.insertElementSafe(n2);
        MergeResult result = d.mergeNodes(n1, n2);
        assertNull(result.getIssues());
        assertNotNull(d.getOsmElement(Node.NAME, n1.getOsmId()));
        assertNull(d.getOsmElement(Node.NAME, n2.getOsmId()));

        d = new StorageDelegator();
        factory = d.getFactory();
        n1 = factory.createNodeWithNewId(StorageDelegatorTest.toE7(51.476), StorageDelegatorTest.toE7(0.006));
        d.insertElementSafe(n1);
        n2 = factory.createNodeWithNewId(StorageDelegatorTest.toE7(51.476), StorageDelegatorTest.toE7(0.006));
        d.insertElementSafe(n2);
        n2.setOsmId(1234L);
        result = d.mergeNodes(n1, n2);
        assertNull(result.getIssues());
        assertNull(d.getOsmElement(Node.NAME, n1.getOsmId()));
        assertNotNull(d.getOsmElement(Node.NAME, n2.getOsmId()));

        d = new StorageDelegator();
        factory = d.getFactory();
        n1 = factory.createNodeWithNewId(StorageDelegatorTest.toE7(51.476), StorageDelegatorTest.toE7(0.006));
        d.insertElementSafe(n1);
        result = d.mergeNodes(n1, n1);
        assertNotNull(result.getIssues());
        assertEquals(1, result.getIssues().size());
        assertTrue(result.getIssues().contains(Issue.SAMEOBJECT));

        d = new StorageDelegator();
        factory = d.getFactory();
        n1 = factory.createNodeWithNewId(StorageDelegatorTest.toE7(51.476), StorageDelegatorTest.toE7(0.006));
        d.insertElementSafe(n1);
        n2 = factory.createNodeWithNewId(StorageDelegatorTest.toE7(51.476), StorageDelegatorTest.toE7(0.006));
        d.insertElementSafe(n2);

        SortedMap<String, String> tags1 = new TreeMap<>();
        tags1.put(Tags.KEY_HIGHWAY, "a");
        n1.setTags(tags1);
        SortedMap<String, String> tags2 = new TreeMap<>();
        tags2.put(Tags.KEY_HIGHWAY, "b");
        n2.setTags(tags2);
        result = d.mergeNodes(n1, n2);
        assertNotNull(result.getIssues());
        assertEquals(1, result.getIssues().size());
        assertTrue(result.getIssues().contains(Issue.MERGEDTAGS));

        // create two ways with common node
        Way w = addWayToStorage(d, false);
        Node n = w.getNodes().get(2);
        Way newWay = d.splitAtNode(w, n);
        d.unjoinWays(n);
        n1 = w.getLastNode();
        n2 = newWay.getFirstNode();
        assertNotEquals(n1, n2);
        result = d.mergeNodes(n1, n2);
        assertNull(result.getIssues());
        assertTrue(w.hasNode(n1));
        assertTrue(newWay.hasNode(n1));
        assertFalse(newWay.hasNode(n2));

        // role conflict
        d = new StorageDelegator();
        factory = d.getFactory();
        n1 = factory.createNodeWithNewId(StorageDelegatorTest.toE7(51.476), StorageDelegatorTest.toE7(0.006));
        d.insertElementSafe(n1);
        n2 = factory.createNodeWithNewId(StorageDelegatorTest.toE7(51.476), StorageDelegatorTest.toE7(0.006));
        d.insertElementSafe(n2);

        Relation r = factory.createRelationWithNewId();
        RelationMember member1 = new RelationMember("test1", n1);
        r.addMember(member1);
        d.insertElementSafe(r);
        n1.addParentRelation(r);
        RelationMember member2 = new RelationMember("test2", n2);
        r.addMember(member2);
        n2.addParentRelation(r);
        result = d.mergeNodes(n1, n2);
        assertNotNull(result.getIssues());
        assertEquals(1, result.getIssues().size());
        assertTrue(result.getIssues().contains(Issue.ROLECONFLICT));
    }

    /**
     * Replace a Node in Ways it is a member of
     */
    @Test
    public void repplaceNodeInWays() {
        StorageDelegator d = new StorageDelegator();
        Way w = addWayToStorage(d, false);
        Way temp = (Way) d.getOsmElement(Way.NAME, w.getOsmId());
        assertNotNull(temp);
        Node n = w.getNodes().get(2);
        d.splitAtNode(w, n);
        assertEquals(2, d.getCurrentStorage().getWays(n).size());
        Node newNode = d.replaceNode(n);
        assertEquals(2, d.getCurrentStorage().getWays(newNode).size());
        assertEquals(0, d.getCurrentStorage().getWays(n).size());
    }

    /**
     * Test removeWayNode method, by invoking removeNode on a node that is member of a way(s)
     */
    @Test
    public void removeWayNode() {
        StorageDelegator d = new StorageDelegator();
        Way w = addWayToStorage(d, false);
        Way temp = (Way) d.getOsmElement(Way.NAME, w.getOsmId());
        assertNotNull(temp);
        Node n = w.getNodes().get(2);
        Way newWay = d.splitAtNode(w, n);
        assertEquals(n, w.getLastNode());
        assertEquals(n, newWay.getFirstNode());
        assertEquals(2, newWay.nodeCount());
        //
        d.removeNode(n);
        assertNotNull(d.getOsmElement(Way.NAME, w.getOsmId()));
        assertNull(d.getOsmElement(Way.NAME, newWay.getOsmId()));
        assertFalse(w.hasNode(n));

        // remove closing node of a closed way
        d = new StorageDelegator();
        w = addWayToStorage(d, true);
        n = w.getFirstNode();
        d.removeNode(n);
        assertTrue(w.isClosed());
        assertFalse(w.hasNode(n));
    }

    /**
     * Split way at node
     */
    @Test
    public void split() {
        StorageDelegator d = new StorageDelegator();
        Way w = addWayToStorage(d, false);
        Way temp = (Way) d.getOsmElement(Way.NAME, w.getOsmId());
        assertNotNull(temp);
        Node n = w.getNodes().get(2);
        Way newWay = d.splitAtNode(w, n);
        assertEquals(n, w.getLastNode());
        assertEquals(n, newWay.getFirstNode());
        assertEquals(2, newWay.nodeCount());
    }

    /**
     * Split closed way
     */
    @Test
    public void splitClosed1() {
        StorageDelegator d = new StorageDelegator();
        Way w = addWayToStorage(d, true);
        assertTrue(w.isClosed());
        Way temp = (Way) d.getOsmElement(Way.NAME, w.getOsmId());
        assertNotNull(temp);
        Node n1 = w.getNodes().get(1);
        Node n2 = w.getNodes().get(2);
        Way[] newWays = d.splitAtNodes(w, n1, n2, false);
        assertNotNull(newWays);
        assertEquals(2, newWays.length);
        assertTrue(newWays[0].hasNode(n1));
        assertTrue(newWays[0].hasNode(n2));
        assertTrue(newWays[1].hasNode(n1));
        assertTrue(newWays[1].hasNode(n2));
    }

    /**
     * Split closed way
     */
    @Test
    public void splitClosed2() {
        StorageDelegator d = new StorageDelegator();
        Way w = addWayToStorage(d, true);
        assertTrue(w.isClosed());
        Way temp = (Way) d.getOsmElement(Way.NAME, w.getOsmId());
        assertNotNull(temp);
        Node n1 = w.getNodes().get(1);
        Node n2 = w.getNodes().get(3);
        Way[] newWays = d.splitAtNodes(w, n1, n2, true);
        assertNotNull(newWays);
        assertEquals(2, newWays.length);
        assertTrue(newWays[0].hasNode(n1));
        assertTrue(newWays[0].hasNode(n2));
        assertTrue(newWays[1].hasNode(n1));
        assertTrue(newWays[1].hasNode(n2));
        assertTrue(newWays[0].isClosed());
        assertTrue(newWays[1].isClosed());
    }

    /**
     * Convert to scaled int representation
     * 
     * @param d double coordinate value
     * @return a scaled int
     */
    static int toE7(double d) {
        return (int) (d * 1E7);
    }
}