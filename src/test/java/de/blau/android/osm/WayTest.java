package de.blau.android.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import androidx.test.filters.LargeTest;
import de.blau.android.osm.OsmElement.ElementType;

@RunWith(RobolectricTestRunner.class)
@LargeTest
public class WayTest {

    /**
     * Test the addNode method
     */
    @Test
    public void addNodeTest() {
        StorageDelegator d = new StorageDelegator();
        Way w = StorageDelegatorTest.addWayToStorage(d, false);
        OsmElementFactory factory = d.getFactory();
        Node n = factory.createNodeWithNewId(StorageDelegatorTest.toE7(51.476), StorageDelegatorTest.toE7(0.006));
        d.insertElementSafe(n);

        w.addNode(n);
        assertEquals(n, w.getLastNode());
    }

    /**
     * Test the addNode after method
     */
    @Test
    public void addNodeAfterTest() {
        StorageDelegator d = new StorageDelegator();
        Way w = StorageDelegatorTest.addWayToStorage(d, false);
        OsmElementFactory factory = d.getFactory();
        Node n = factory.createNodeWithNewId(StorageDelegatorTest.toE7(51.476), StorageDelegatorTest.toE7(0.006));
        d.insertElementSafe(n);

        w.addNodeAfter(w.getFirstNode(), n);
        assertEquals(n, w.getNodes().get(1));
    }

    /**
     * Test the appenNode method
     */
    @Test
    public void appendNodeTest() {
        StorageDelegator d = new StorageDelegator();
        Way w = StorageDelegatorTest.addWayToStorage(d, false);
        OsmElementFactory factory = d.getFactory();
        Node n = factory.createNodeWithNewId(StorageDelegatorTest.toE7(51.476), StorageDelegatorTest.toE7(0.006));
        d.insertElementSafe(n);

        w.appendNode(w.getLastNode(), n);
        assertEquals(n, w.getLastNode());
        w.removeNode(n);
        w.appendNode(w.getFirstNode(), n);
        assertEquals(n, w.getFirstNode());
    }

    /**
     * Test that closed ways work
     */
    @Test
    public void isClosedTest() {
        StorageDelegator d = new StorageDelegator();
        Way w = StorageDelegatorTest.addWayToStorage(d, true);
        assertTrue(w.isClosed());
        assertEquals(ElementType.CLOSEDWAY, w.getType());

        d = new StorageDelegator();
        w = StorageDelegatorTest.addWayToStorage(d, false);
        assertFalse(w.isClosed());
        assertEquals(ElementType.WAY, w.getType());
    }

    /**
     * Test the removeNode and removeAllNodes methods work
     */
    @Test
    public void removeNodeTest() {
        StorageDelegator d = new StorageDelegator();
        Way w = StorageDelegatorTest.addWayToStorage(d, true);
        assertTrue(w.isClosed());
        assertEquals(ElementType.CLOSEDWAY, w.getType());
        assertEquals(5, w.getNodes().size());
        w.removeNode(w.getNodes().get(1));
        assertEquals(4, w.getNodes().size());
        assertTrue(w.isClosed());
        assertEquals(ElementType.CLOSEDWAY, w.getType());

        // the same but this time the closing node
        d = new StorageDelegator();
        w = StorageDelegatorTest.addWayToStorage(d, true);
        assertTrue(w.isClosed());
        assertEquals(ElementType.CLOSEDWAY, w.getType());
        assertEquals(5, w.getNodes().size());
        w.removeNode(w.getNodes().get(1));
        assertEquals(4, w.getNodes().size());
        assertTrue(w.isClosed());
        assertEquals(ElementType.CLOSEDWAY, w.getType());

        d = new StorageDelegator();
        w = StorageDelegatorTest.addWayToStorage(d, false);
        w.removeNode(w.getNodes().get(1));
        assertEquals(3, w.getNodes().size());

        d = new StorageDelegator();
        w = StorageDelegatorTest.addWayToStorage(d, false);
        w.removeAllNodes();
        assertEquals(0, w.getNodes().size());
        assertFalse(w.isClosed());
    }

    /**
     * Test the hasCommonNode and getCommonNode methods
     */
    @Test
    public void commonNodesTest() {
        StorageDelegator d = new StorageDelegator();
        Way w = StorageDelegatorTest.addWayToStorage(d, false);
        Node n = w.getNodes().get(2);
        List<Result> splitResult = d.splitAtNode(w, n);
        assertNotNull(splitResult);
        assertFalse(splitResult.isEmpty());
        Way w2 = (Way) splitResult.get(0).getElement();

        assertTrue(w.hasCommonNode(w2));
        assertEquals(n, w.getCommonNode(w2));

        d.unjoinWays(n);

        assertFalse(w.hasCommonNode(w2));
        assertNull(w.getCommonNode(w2));
    }

    /**
     * Test getOneway behaviour
     */
    @Test
    public void onewayTest() {
        StorageDelegator d = new StorageDelegator();
        Way w = StorageDelegatorTest.addWayToStorage(d, false);
        assertEquals(0, w.getOneway());

        SortedMap<String, String> tags = new TreeMap<>(w.getTags());

        tags.clear();
        tags.put(Tags.KEY_HIGHWAY, "residential");
        tags.put(Tags.KEY_ONEWAY, Tags.VALUE_YES);
        w.setTags(tags);
        assertEquals(1, w.getOneway());

        tags.clear();
        tags.put(Tags.KEY_HIGHWAY, "residential");
        tags.put(Tags.KEY_ONEWAY, Tags.VALUE_TRUE);
        w.setTags(tags);
        assertEquals(1, w.getOneway());

        tags.clear();
        tags.put(Tags.KEY_HIGHWAY, "residential");
        tags.put(Tags.KEY_ONEWAY, "1");
        w.setTags(tags);
        assertEquals(1, w.getOneway());

        tags.clear();
        tags.put(Tags.KEY_HIGHWAY, "residential");
        tags.put(Tags.KEY_ONEWAY, Tags.VALUE_REVERSE);
        w.setTags(tags);
        assertEquals(-1, w.getOneway());

        tags.clear();
        tags.put(Tags.KEY_HIGHWAY, "residential");
        tags.put(Tags.KEY_ONEWAY, "-1");
        w.setTags(tags);
        assertEquals(-1, w.getOneway());
    }

    /**
     * Test the notReversible method
     */
    @Test
    public void reversibleTest() {
        StorageDelegator d = new StorageDelegator();
        Way w = StorageDelegatorTest.addWayToStorage(d, false);
        assertEquals(0, w.getOneway());

        SortedMap<String, String> tags = new TreeMap<>(w.getTags());

        tags.clear();
        tags.put(Tags.KEY_HIGHWAY, Tags.VALUE_MOTORWAY);
        w.setTags(tags);
        assertTrue(w.notReversable());

        tags.clear();
        tags.put(Tags.KEY_WATERWAY, "anything");
        w.setTags(tags);
        assertTrue(w.notReversable());

        tags.clear();
        tags.put(Tags.KEY_WATERWAY, Tags.VALUE_RIVERBANK);
        w.setTags(tags);
        assertFalse(w.notReversable());

        tags.clear();
        tags.put(Tags.KEY_NATURAL, "anything");
        w.setTags(tags);
        assertFalse(w.notReversable());

        tags.clear();
        tags.put(Tags.KEY_NATURAL, Tags.VALUE_CLIFF);
        w.setTags(tags);
        assertTrue(w.notReversable());

        tags.clear();
        tags.put(Tags.KEY_NATURAL, Tags.VALUE_COASTLINE);
        w.setTags(tags);
        assertTrue(w.notReversable());

        tags.clear();
        tags.put(Tags.KEY_MAN_MADE, "anything");
        w.setTags(tags);
        assertFalse(w.notReversable());

        tags.clear();
        tags.put(Tags.KEY_MAN_MADE, Tags.VALUE_EMBANKMENT);
        w.setTags(tags);
        assertTrue(w.notReversable());

        tags.clear();
        tags.put(Tags.KEY_BARRIER, "anything");
        w.setTags(tags);
        assertFalse(w.notReversable());

        tags.clear();
        tags.put(Tags.KEY_BARRIER, Tags.VALUE_RETAINING_WALL);
        w.setTags(tags);
        assertTrue(w.notReversable());

        tags.clear();
        tags.put(Tags.KEY_BARRIER, Tags.VALUE_KERB);
        w.setTags(tags);
        assertTrue(w.notReversable());

        tags.clear();
        tags.put(Tags.KEY_BARRIER, Tags.VALUE_GUARD_RAIL);
        w.setTags(tags);
        assertTrue(w.notReversable());

        tags.clear();
        tags.put(Tags.KEY_BARRIER, Tags.VALUE_CITY_WALL);
        w.setTags(tags);
        assertTrue(w.notReversable());

        tags.put(Tags.KEY_TWO_SIDED, Tags.VALUE_YES);
        w.setTags(tags);
        assertFalse(w.notReversable());
    }
}