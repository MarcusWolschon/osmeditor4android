package de.blau.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmElementFactory;
import de.blau.android.util.GeoMath;
import de.blau.android.util.collections.LongHashSet;
import de.blau.android.util.collections.LongOsmElementMap;
import de.blau.android.util.collections.MultiHashMap;
import de.blau.android.util.rtree.RTree;

public class CollectionTest {

    /**
     * Test our Long to OsmElement hash map implementation
     */
    @Test
    public void hashmap() {
        LongOsmElementMap<Node> map = new LongOsmElementMap<>(10000);

        ArrayList<Node> elements = new ArrayList<>(100000);
        for (int i = 0; i < 100000; i++) {
            elements.add(OsmElementFactory.createNode((long) (Math.random() * Long.MAX_VALUE), 1L, System.currentTimeMillis() / 1000, OsmElement.STATE_CREATED,
                    0, 0));
        }
        for (int i = 0; i < 100000; i++) {
            Node n = elements.get(i);
            map.put(n.getOsmId(), n);
        }

        for (int i = 0; i < 100000; i++) {
            assertTrue(map.containsKey(elements.get(i).getOsmId()));
        }

        List<Node> values = map.values();
        assertEquals(100000, values.size());
        int j = 0;
        for (Node n : map) {
            assertEquals(values.get(j), n); // both should be in the same internal order
            j++;
        }

        for (int i = 0; i < 100000; i++) {
            assertNotNull(map.remove(elements.get(i).getOsmId()));
        }
        assertTrue(map.isEmpty());

        for (int i = 0; i < 100000; i++) {
            assertFalse(map.containsKey(elements.get(i).getOsmId()));
        }
    }

    /**
     * Test our OsmElement hash set implementation
     */
    @Test
    public void hashset() {
        LongHashSet set = new LongHashSet(10000);

        long[] l = new long[100000];
        for (int i = 0; i < 100000; i++) {
            l[i] = (long) ((Math.random() - 0.5D) * 2 * Long.MAX_VALUE);
        }
        for (int i = 0; i < 100000; i++) {
            set.put(l[i]);
        }

        for (int i = 0; i < 100000; i++) {
            assertTrue(set.contains(l[i]));
        }

        for (int i = 0; i < 100000; i++) {
            assertTrue(set.remove(l[i]));
        }
        assertTrue(set.isEmpty());
    }

    /**
     * Some minimal tests for our RTree implementation
     */
    @Test
    public void rtree() {
        final double MAX = GeoMath.MAX_LAT_E7;
        RTree<Node> tree = new RTree<>(2, 100);
        final int NODES = 10000;
        Node[] temp = new Node[NODES];
        for (long i = 0; i < NODES; i++) {
            temp[(int) i] = OsmElementFactory.createNode(i, 1L, System.currentTimeMillis() / 1000, OsmElement.STATE_CREATED, (int) (Math.random() * MAX),
                    (int) (Math.random() * MAX));
        }
        long start = System.currentTimeMillis();
        for (long i = 0; i < NODES; i++) {
            tree.insert(temp[(int) i]);
        }
        System.out.println("Node insertion " + (System.currentTimeMillis() - start)); // NOSONAR
        start = System.currentTimeMillis();
        for (int i = 0; i < NODES; i++) {
            Collection<Node> result = new ArrayList<>();
            BoundingBox b = null;
            // create a small bounding box around the Node and query that
            b = new BoundingBox(temp[i].getLon() - 1, temp[i].getLat() - 1, temp[i].getLon() + 1, temp[i].getLat() + 1);
            tree.query(result, b);
            assertTrue(result.contains(temp[i]));
        }
        System.out.println("Query " + (System.currentTimeMillis() - start)); // NOSONAR
        assertEquals(NODES, tree.count());
        // currently contains and remove doesn't work for nodes
        start = System.currentTimeMillis();
        for (long i = 0; i < NODES; i++) {
            assertTrue(tree.contains(temp[(int) i]));
        }
        System.out.println("Contains " + (System.currentTimeMillis() - start)); // NOSONAR
        start = System.currentTimeMillis();
        for (long i = 0; i < NODES; i++) {
            assertTrue(tree.remove(temp[(int) i]));
        }
        System.out.println("Remove " + (System.currentTimeMillis() - start)); // NOSONAR
        assertEquals(0, tree.count());
    }

    /**
     * Test our MultiHashMap
     */
    @Test
    public void multihashmap() {
        MultiHashMap<String, String> map = new MultiHashMap<>();
        map.add("A", "1");
        map.add("A", "2");
        map.add("M", "3");
        Set<String> r = map.get("A");
        assertEquals(2, r.size());
        assertTrue(r.contains("1"));
        assertTrue(r.contains("2"));
        r = map.get("M");
        assertEquals(1, r.size());
        assertTrue(r.contains("3"));
    }
}