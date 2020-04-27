package de.blau.android.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
    Way addWayToStorage(@NonNull StorageDelegator d, boolean close) {
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
        assertEquals(wayNodeCount + 1, d.getCurrentStorage().getNodeCount());
        assertEquals(1, d.getCurrentStorage().getWayCount());
        assertEquals(32, d.getCurrentStorage().getRelationCount());
        assertEquals(parentCount, d.getOsmElement(Way.NAME, 571067343L).getParentRelations().size());
    }

    /**
     * Load some data modify a way and a node, then merge some data
     */
    @Test
    public void merge() {
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
        assertEquals(nodeCount + 4, d.getCurrentStorage().getNodeCount());
        assertEquals(wayCount + 1, d.getCurrentStorage().getWayCount());
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
    int toE7(double d) {
        return (int) (d * 1E7);
    }
}