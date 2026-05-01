package de.blau.android.easyedit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import androidx.test.filters.LargeTest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmElementFactory;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Way;

import static org.junit.Assert.*;

/**
 * Unit tests for EasyEditActionModeCallback
 */
@RunWith(RobolectricTestRunner.class)
@LargeTest
public class SquarableNodesTest {

    private Node             node1, node2, node3, node4;
    private Logic            testLogic;
    private StorageDelegator delegator;

    @Before
    public void setUp() {
        testLogic = App.newLogic();
        delegator = App.getDelegator();

        // Create test nodes with WGS84 coordinates * 1E7
        node1 = OsmElementFactory.createNode(1, 1, System.currentTimeMillis() / 1000, OsmElement.STATE_CREATED, 525000000, 134000000);
        delegator.insertElementSafe(node1);
        node2 = OsmElementFactory.createNode(2, 1, System.currentTimeMillis() / 1000, OsmElement.STATE_CREATED, 525000000, 135000000);
        delegator.insertElementSafe(node2);
        node3 = OsmElementFactory.createNode(3, 1, System.currentTimeMillis() / 1000, OsmElement.STATE_CREATED, 526000000, 135000000);
        delegator.insertElementSafe(node3);
        node4 = OsmElementFactory.createNode(4, 1, System.currentTimeMillis() / 1000, OsmElement.STATE_CREATED, 526000000, 134000000);
        delegator.insertElementSafe(node4);
    }

    /**
     * 
     */
    private Way createTestWay1() {
        // Create test ways
        Way way1 = OsmElementFactory.createWay(1, 1, System.currentTimeMillis() / 1000, OsmElement.STATE_CREATED);
        delegator.insertElementSafe(way1);
        delegator.addNodeToWay(node1, way1);
        delegator.addNodeToWay(node2, way1);
        delegator.addNodeToWay(node3, way1);
        return way1;
    }

    private Way createTestWay2() {
        Way way2 = OsmElementFactory.createWay(2, 1, System.currentTimeMillis() / 1000, OsmElement.STATE_CREATED);
        delegator.insertElementSafe(way2);
        delegator.addNodeToWay(node2, way2);
        delegator.addNodeToWay(node3, way2);
        delegator.addNodeToWay(node4, way2);
        return way2;
    }

    /**
     * 
     */
    private Way createClosedWay() {
        // Create a closed way
        Way closedWay = OsmElementFactory.createWay(3, 1, System.currentTimeMillis() / 1000, OsmElement.STATE_CREATED);
        delegator.insertElementSafe(closedWay);
        delegator.addNodeToWay(node1, closedWay);
        delegator.addNodeToWay(node2, closedWay);
        delegator.addNodeToWay(node3, closedWay);
        delegator.addNodeToWay(node1, closedWay); // Close the way
        return closedWay;
    }

    @After
    public void cleanUp() {
        delegator.reset(true);
    }

    /**
     * Test that a single squarable node (not an end node) is correctly identified
     */
    @Test
    public void testGetSquarableNodesSingleNode() {
        createTestWay1();
        List<Node> candidateNodes = new ArrayList<>();
        candidateNodes.add(node2); // node2 is in middle of way1

        Set<Node> squarableNodes = new HashSet<>();
        EasyEditActionModeCallback.getSquarableNodes(testLogic, candidateNodes, null, squarableNodes);

        assertEquals("Should identify node2 as squarable", 1, squarableNodes.size());
        assertTrue("node2 should be in squarable nodes", squarableNodes.contains(node2));
    }

    /**
     * Test that end nodes on non-closed ways are not squarable
     */
    @Test
    public void testGetSquarableNodesEndNodeNonClosed() {
        createTestWay1();
        List<Node> candidateNodes = new ArrayList<>();
        candidateNodes.add(node1); // node1 is an end node of way1

        Set<Node> squarableNodes = new HashSet<>();
        EasyEditActionModeCallback.getSquarableNodes(testLogic, candidateNodes, null, squarableNodes);

        assertEquals("End node on non-closed way should not be squarable", 0, squarableNodes.size());
    }

    /**
     * Test that end nodes on closed ways ARE squarable
     */
    @Test
    public void testGetSquarableNodesEndNodeClosed() {
        createClosedWay();

        List<Node> candidateNodes = new ArrayList<>();
        candidateNodes.add(node1);

        Set<Node> squarableNodes = new HashSet<>();
        EasyEditActionModeCallback.getSquarableNodes(testLogic, candidateNodes, null, squarableNodes);

        assertEquals("End node on closed way should be squarable", 1, squarableNodes.size());
        assertTrue("node1 should be in squarable nodes", squarableNodes.contains(node1));
    }

    /**
     * Test with empty candidate nodes list
     */
    @Test
    public void testGetSquarableNodesEmptyList() {
        List<Node> candidateNodes = new ArrayList<>();
        Set<Node> squarableNodes = new HashSet<>();

        EasyEditActionModeCallback.getSquarableNodes(testLogic, candidateNodes, null, squarableNodes);

        assertEquals("Empty candidate list should result in empty squarable nodes", 0, squarableNodes.size());
    }

    /**
     * Test with null candidate nodes list
     */
    @Test
    public void testGetSquarableNodesNullList() {
        Set<Node> squarableNodes = new HashSet<>();

        EasyEditActionModeCallback.getSquarableNodes(testLogic, null, null, squarableNodes);

        assertEquals("Null candidate list should result in empty squarable nodes", 0, squarableNodes.size());
    }

    /**
     * Test with multiple candidate nodes
     */
    @Test
    public void testGetSquarableNodesMultipleNodes() {
        Way way1 = createTestWay1();
        Way way2 = createTestWay2();
        List<Node> candidateNodes = new ArrayList<>();
        candidateNodes.add(node2);
        candidateNodes.add(node3);

        Set<Node> squarableNodes = new HashSet<>();
        Set<Way> waysSet = new HashSet<>();

        EasyEditActionModeCallback.getSquarableNodes(testLogic, candidateNodes, waysSet, squarableNodes);

        assertEquals("Both nodes should be squarable", 2, squarableNodes.size());
        assertTrue("node2 should be in squarable nodes", squarableNodes.contains(node2));
        assertTrue("node3 should be in squarable nodes", squarableNodes.contains(node3));
        assertTrue("way1 should be collected", waysSet.contains(way1));
        assertTrue("way2 should be collected", waysSet.contains(way2));
    }

    /**
     * Test that ways are collected in the provided set
     */
    @Test
    public void testGetSquarableNodesCollectsWays() {
        Way way1 = createTestWay1();
        Way closedWay = createClosedWay();

        List<Node> candidateNodes = new ArrayList<>();
        candidateNodes.add(node2); // node2 is in both way1 and closedWay

        Set<Way> waysSet = new HashSet<>();
        Set<Node> squarableNodes = new HashSet<>();

        EasyEditActionModeCallback.getSquarableNodes(testLogic, candidateNodes, waysSet, squarableNodes);

        assertEquals("Both ways should be collected", 2, waysSet.size());
        assertTrue("way1 should be in ways set", waysSet.contains(way1));
        assertTrue("closedWay should be in ways set", waysSet.contains(closedWay));
    }

    /**
     * Test that ways parameter is optional (can be null)
     */
    @Test
    public void testGetSquarableNodesNullWaysSet() {
        createTestWay1();
        List<Node> candidateNodes = new ArrayList<>();
        candidateNodes.add(node2);

        Set<Node> squarableNodes = new HashSet<>();

        // Should not throw exception when ways is null
        EasyEditActionModeCallback.getSquarableNodes(testLogic, candidateNodes, null, squarableNodes);

        assertEquals("Should identify squarable nodes even with null ways set", 1, squarableNodes.size());
    }

    /**
     * Test node that is an orphan (not in any way)
     */
    @Test
    public void testGetSquarableNodesOrphanNode() {
        Node orphanNode = OsmElementFactory.createNode(99, 1, System.currentTimeMillis() / 1000, OsmElement.STATE_CREATED, 527000000, 136000000);
        delegator.insertElementSafe(orphanNode);

        List<Node> candidateNodes = new ArrayList<>();
        candidateNodes.add(orphanNode);

        Set<Node> squarableNodes = new HashSet<>();
        EasyEditActionModeCallback.getSquarableNodes(testLogic, candidateNodes, null, squarableNodes);

        assertEquals("Orphan node should not be squarable", 0, squarableNodes.size());
    }

    /**
     * Test with duplicate candidates
     */
    @Test
    public void testGetSquarableNodesDuplicateCandidates() {
        createTestWay1();
        List<Node> candidateNodes = new ArrayList<>();
        candidateNodes.add(node2);
        candidateNodes.add(node2); // Add same node twice

        Set<Node> squarableNodes = new HashSet<>();
        EasyEditActionModeCallback.getSquarableNodes(testLogic, candidateNodes, null, squarableNodes);

        assertEquals("Duplicates in candidates may result in duplicates in squarable nodes", 1, squarableNodes.size());
    }

    /**
     * Test node shared between multiple ways
     */
    @Test
    public void testGetSquarableNodesSharedBetweenWays() {
        createTestWay2();
        createClosedWay();
        List<Node> candidateNodes = new ArrayList<>();
        candidateNodes.add(node3); // node3 is in both way2 and closedWay, but not at end of either

        Set<Node> squarableNodes = new HashSet<>();
        Set<Way> waysSet = new HashSet<>();

        EasyEditActionModeCallback.getSquarableNodes(testLogic, candidateNodes, waysSet, squarableNodes);

        assertEquals("Shared node should be squarable", 1, squarableNodes.size());
        assertEquals("Both ways containing the node should be collected", 2, waysSet.size());
    }
}