package de.blau.android.easyedit;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import androidx.test.filters.LargeTest;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmElementFactory;

@LargeTest
public class FollowWayTest {

    @Test
    public void followClosedWayTest() {
        Node node1 = OsmElementFactory.createNode(-1L, 1L, System.currentTimeMillis() / 1000, OsmElement.STATE_CREATED, 0, 0);
        final Node node2 = OsmElementFactory.createNode(-2L, 1L, System.currentTimeMillis() / 1000, OsmElement.STATE_CREATED, 0, 0);
        final Node node3 = OsmElementFactory.createNode(-3L, 1L, System.currentTimeMillis() / 1000, OsmElement.STATE_CREATED, 0, 0);
        final Node node4 = OsmElementFactory.createNode(-4L, 1L, System.currentTimeMillis() / 1000, OsmElement.STATE_CREATED, 0, 0);
        final Node node5 = OsmElementFactory.createNode(-5L, 1L, System.currentTimeMillis() / 1000, OsmElement.STATE_CREATED, 0, 0);
        List<Node> nodes = Arrays.asList(node1, node2, node3, node4, node5, node1);

        List<Node> result = PathCreationActionModeCallback.nodesFromFollow(nodes, node1, node2, node5, true);
        assertEquals(Arrays.asList(node3, node4, node5), result);
        
        result = PathCreationActionModeCallback.nodesFromFollow(nodes, node2, node3, node1, true);
        assertEquals(Arrays.asList(node4, node5, node1), result);
        
        result = PathCreationActionModeCallback.nodesFromFollow(nodes, node3, node4, node2, true);
        assertEquals(Arrays.asList(node5, node1, node2), result);
        
        result = PathCreationActionModeCallback.nodesFromFollow(nodes, node4, node5, node3, true);
        assertEquals(Arrays.asList(node1, node2, node3), result);
        
        result = PathCreationActionModeCallback.nodesFromFollow(nodes, node1, node5, node2, true);
        assertEquals(Arrays.asList(node4, node3, node2), result);
        
        result = PathCreationActionModeCallback.nodesFromFollow(nodes, node2, node1, node3, true);
        assertEquals(Arrays.asList(node5, node4, node3), result);
        
        result = PathCreationActionModeCallback.nodesFromFollow(nodes, node3, node2, node4, true);
        assertEquals(Arrays.asList(node1, node5, node4), result);
        
        result = PathCreationActionModeCallback.nodesFromFollow(nodes, node4, node3, node5, true);
        assertEquals(Arrays.asList(node2, node1, node5), result);
    }
    
    @Test
    public void followWayTest() {
        Node node1 = OsmElementFactory.createNode(-1L, 1L, System.currentTimeMillis() / 1000, OsmElement.STATE_CREATED, 0, 0);
        final Node node2 = OsmElementFactory.createNode(-2L, 1L, System.currentTimeMillis() / 1000, OsmElement.STATE_CREATED, 0, 0);
        final Node node3 = OsmElementFactory.createNode(-3L, 1L, System.currentTimeMillis() / 1000, OsmElement.STATE_CREATED, 0, 0);
        final Node node4 = OsmElementFactory.createNode(-4L, 1L, System.currentTimeMillis() / 1000, OsmElement.STATE_CREATED, 0, 0);
        final Node node5 = OsmElementFactory.createNode(-5L, 1L, System.currentTimeMillis() / 1000, OsmElement.STATE_CREATED, 0, 0);
        List<Node> nodes = Arrays.asList(node1, node2, node3, node4, node5);

        List<Node> result = PathCreationActionModeCallback.nodesFromFollow(nodes, node1, node2, node5, false);
        assertEquals(Arrays.asList(node3, node4, node5), result);
    }
}
