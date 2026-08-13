package de.blau.android.osm;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import androidx.test.filters.LargeTest;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for the WaySegment class
 */
@RunWith(RobolectricTestRunner.class)
@LargeTest
public class WaySegmentTest {

    private Way way;
    private Node node1, node2, node3, node4, node5;

    @Before
    public void setUp() {
        // Create test nodes with WGS84 coordinates * 1E7
        node1 = OsmElementFactory.createNode(1, 1, System.currentTimeMillis() / 1000, OsmElement.STATE_CREATED, 525000000, 134000000);
        node2 = OsmElementFactory.createNode(2, 1, System.currentTimeMillis() / 1000, OsmElement.STATE_CREATED, 525000000, 135000000);
        node3 = OsmElementFactory.createNode(3, 1, System.currentTimeMillis() / 1000, OsmElement.STATE_CREATED, 526000000, 135000000);
        node4 = OsmElementFactory.createNode(4, 1, System.currentTimeMillis() / 1000, OsmElement.STATE_CREATED, 526000000, 134000000);
        node5 = OsmElementFactory.createNode(5, 1, System.currentTimeMillis() / 1000, OsmElement.STATE_CREATED, 525000000, 133000000);

        // Create a way with 5 nodes
        way = OsmElementFactory.createWay(1, 1, System.currentTimeMillis() / 1000, OsmElement.STATE_CREATED);
        way.addNode(node1);
        way.addNode(node2);
        way.addNode(node3);
        way.addNode(node4);
        way.addNode(node5);
    }

    /**
     * Test basic segment creation and getNodes for a forward segment
     */
    @Test
    public void testGetNodesForwardSegment() {
        WaySegment segment = new WaySegment(way, 1, 3);
        List<Node> nodes = segment.getNodes();

        assertEquals("Segment should contain 3 nodes (indices 1, 2, 3)", 3, nodes.size());
        assertEquals("First node should be node2", node2, nodes.get(0));
        assertEquals("Second node should be node3", node3, nodes.get(1));
        assertEquals("Third node should be node4", node4, nodes.get(2));
    }

    /**
     * Test segment with wrap-around (when end < start)
     */
    @Test
    public void testGetNodesWrappedSegment() {
        // Create a closed way for proper wrap-around testing
        Node closingNode = node1; // closed way has first and last node the same
        way.addNode(closingNode);

        WaySegment segment = new WaySegment(way, 4, 1);
        List<Node> nodes = segment.getNodes();

        // Should include nodes 4, 5, 0 (wrap), 1
        assertEquals("Segment should contain 4 nodes", 4, nodes.size());
        assertEquals("Should start with node5", node5, nodes.get(0));
        assertTrue("Should contain node1", nodes.contains(node1));
        assertTrue("Should contain node2", nodes.contains(node2));
    }

    /**
     * Test nodeCount for forward segment
     */
    @Test
    public void testNodeCountForwardSegment() {
        WaySegment segment = new WaySegment(way, 0, 2);
        assertEquals("Forward segment node count", 3, segment.nodeCount());
    }

    /**
     * Test nodeCount for single node segment
     */
    @Test
    public void testNodeCountSingleNode() {
        WaySegment segment = new WaySegment(way, 2, 2);
        assertEquals("Single node segment should have count 1", 1, segment.nodeCount());
    }

    /**
     * Test nodeCount for wrapped segment
     */
    @Test
    public void testNodeCountWrappedSegment() {
        way.addNode(node1); // make it closed
        WaySegment segment = new WaySegment(way, 4, 1);
        // nodeCount = way.nodeCount() - start + end = 6 - 4 + 1 = 3
        assertEquals("Wrapped segment node count", 3, segment.nodeCount());
    }

    /**
     * Test isClosed for non-closed segment
     */
    @Test
    public void testIsClosedFalse() {
        WaySegment segment = new WaySegment(way, 0, 3);
        assertFalse("Segment should not be closed", segment.isClosed());
    }

    /**
     * Test isClosed for closed segment
     */
    @Test
    public void testIsClosedTrue() {
        // Make way closed
        way.addNode(node1);
        WaySegment segment = new WaySegment(way, 0, 5); // starts and ends at node1
        assertTrue("Segment should be closed", segment.isClosed());
    }

    /**
     * Test length calculation
     */
    @Test
    public void testLength() {
        WaySegment segment = new WaySegment(way, 0, 2);
        double length = segment.length();
        assertTrue("Length should be positive", length > 0);
    }

    /**
     * Test that getNodes returns consistent results on multiple calls
     */
    @Test
    public void testGetNodesConsistency() {
        WaySegment segment = new WaySegment(way, 1, 3);
        List<Node> nodes1 = segment.getNodes();
        List<Node> nodes2 = segment.getNodes();

        assertEquals("Multiple calls to getNodes should return same content", nodes1, nodes2);
    }

    /**
     * Test segment at the beginning of way
     */
    @Test
    public void testSegmentAtBeginning() {
        WaySegment segment = new WaySegment(way, 0, 1);
        List<Node> nodes = segment.getNodes();

        assertEquals("Should contain 2 nodes", 2, nodes.size());
        assertEquals("First node should be node1", node1, nodes.get(0));
        assertEquals("Second node should be node2", node2, nodes.get(1));
    }

    /**
     * Test segment at the end of way
     */
    @Test
    public void testSegmentAtEnd() {
        WaySegment segment = new WaySegment(way, 3, 4);
        List<Node> nodes = segment.getNodes();

        assertEquals("Should contain 2 nodes", 2, nodes.size());
        assertEquals("First node should be node4", node4, nodes.get(0));
        assertEquals("Second node should be node5", node5, nodes.get(1));
    }

    /**
     * Test WaySegment implements WayInterface
     */
    @Test
    public void testImplementsWayInterface() {
        WaySegment segment = new WaySegment(way, 0, 2);
        assertTrue("WaySegment should implement WayInterface", segment instanceof WayInterface);
    }

    /**
     * Test WaySegment is Serializable
     */
    @Test
    public void testIsSerializable() {
        WaySegment segment = new WaySegment(way, 0, 2);
        assertTrue("WaySegment should implement Serializable", segment instanceof java.io.Serializable);
    }
}

