package de.blau.android.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import android.util.Log;

public class StorageTest {

    private static final String DEBUG_TAG = "StorageTest";
    private Storage             storage;

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        storage = PbfTest.read();
        Log.d(DEBUG_TAG, "Loaded " + storage.getNodeCount() + " Nodes " + storage.getWayCount() + " Ways " + storage.getRelationCount() + " Relations");
    }

    /**
     * Ways for node
     */
    @Test
    public void waysForNode() {
        Node node = (Node) storage.getOsmElement(Node.NAME, 300852915L);
        assertNotNull(node);
        long start = System.currentTimeMillis();
        List<Way> ways = storage.getWays(node);
        long execution = System.currentTimeMillis() - start;
        Log.d(DEBUG_TAG, "getWays(Node) took " + execution + " ms");
        assertEquals(2, ways.size());
    }

    /**
     * Nodes for BoundingBox
     */
    @Test
    public void nodesForBoundingBox() {
        BoundingBox box = new BoundingBox(9.51947D, 47.13638D, 9.52300D, 47.14066D);
        long start = System.currentTimeMillis();
        List<Node> nodes = storage.getNodes(box);
        long execution = System.currentTimeMillis() - start;
        Log.d(DEBUG_TAG, "getNodes(Boundingbox) took " + execution + " ms");
        assertEquals(1260, nodes.size());
    }

    /**
     * Ways for BoundingBox
     */
    @Test
    public void waysForBoundingBox() {
        BoundingBox box = new BoundingBox(9.51947D, 47.13638D, 9.52300D, 47.14066D);
        long start = System.currentTimeMillis();
        List<Way> ways = storage.getWays(box);
        long execution = System.currentTimeMillis() - start;
        Log.d(DEBUG_TAG, "getWays(Boundingbox) took " + execution + " ms");
        assertEquals(217, ways.size());
    }
}
