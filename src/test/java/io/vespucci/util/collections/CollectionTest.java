package io.vespucci.util.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import io.vespucci.osm.BoundingBox;
import io.vespucci.osm.Node;
import io.vespucci.osm.OsmElement;
import io.vespucci.osm.OsmElementFactory;
import io.vespucci.util.GeoMath;
import io.vespucci.util.collections.LongHashSet;
import io.vespucci.util.collections.LongOsmElementMap;
import io.vespucci.util.collections.MRUHashMap;
import io.vespucci.util.collections.MRUList;
import io.vespucci.util.collections.MultiHashMap;
import io.vespucci.util.collections.UnsignedSparseBitSet;
import io.vespucci.util.rtree.RTree;

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
        Collection<Node> result = new ArrayList<>();
        BoundingBox b = new BoundingBox();
        start = System.currentTimeMillis();
        for (int i = 0; i < NODES; i++) {
            // create a small bounding box around the Node and query that
            b.set(temp[i].getLon() - 1, temp[i].getLat() - 1, temp[i].getLon() + 1, temp[i].getLat() + 1);
            result.clear();
            tree.query(result, b);
            assertTrue(result.contains(temp[i]));
        }
        System.out.println("Query " + (System.currentTimeMillis() - start)); // NOSONAR
        assertEquals(NODES, tree.count());
        result.clear();
        start = System.currentTimeMillis();
        tree.query(result);
        assertEquals(NODES, result.size());
        System.out.println("Query all " + (System.currentTimeMillis() - start)); // NOSONAR
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

    /**
     * Test our extension of SparseBitSet
     */
    @Test
    public void unsignedSparseBitSet() {
        UnsignedSparseBitSet bs = new UnsignedSparseBitSet();
        // bottom half
        assertEquals(-1, bs.nextSetBit(0));
        bs.set(25034);
        assertTrue(bs.get(25034));
        assertEquals(25034, bs.nextSetBit(0));
        assertEquals(1, bs.cardinality());
        // top half
        bs.set(0x80150A00);
        assertTrue(bs.get(0x80150A00));
        assertEquals(0x80150A00, bs.nextSetBit(0x80000000));
        assertEquals(0x80150A00, bs.nextSetBit(25035));
        assertEquals(2, bs.cardinality());
        // undo
        bs.clear(0x80150A00);
        assertFalse(bs.get(0x80150A00));
        assertEquals(1, bs.cardinality());
        bs.clear(25034);
        assertFalse(bs.get(25034));
        assertEquals(0, bs.cardinality());
        // misc
        assertEquals(25035, UnsignedSparseBitSet.inc(25034));
        assertEquals(0x80150A01, UnsignedSparseBitSet.inc(0x80150A00));
    }

    /**
     * Test Most-Recently-Used list implementation
     */
    @Test
    public void mruList() {
        MRUList<String> mru = new MRUList<>(10);
        mru.push("bottom");
        mru.push("top");
        assertEquals(2, mru.size());
        assertEquals("top", mru.last());
        mru.push("bottom");
        assertEquals(2, mru.size());
        assertEquals("bottom", mru.last());

        assertTrue(mru.equals(new MRUList<String>(mru))); // capacity should be the same
        assertFalse(mru.equals(new MRUList<String>(11)));

        mru.pushAll(Arrays.asList("1", "2"));
        assertEquals(4, mru.size());
        assertEquals("2", mru.last());
        mru.push("top");
        assertEquals(4, mru.size());
        assertTrue(mru.remove("top"));
        mru.clear();
        assertEquals(0, mru.size());
        assertTrue(mru.isEmpty());
    }

    @Test
    public void mruHashMap() {
        MRUHashMap<String, String> mruMap = new MRUHashMap<>(10);
        for (int i = 0; i < 9; i++) {
            assertNull(mruMap.put("key" + i, "value" + i));
        }

        MRUHashMap<String, String> mruMap2 = new MRUHashMap<>(10);
        for (int i = 0; i < 9; i++) {
            assertNull(mruMap2.put("key" + i, "value" + i));
        }
        assertTrue(mruMap.equals(mruMap2));
        assertFalse(mruMap.equals(new MRUHashMap<String, String>(11)));

        assertEquals(9, mruMap.size());
        assertEquals("value8", mruMap.get("key8"));
        assertNull(mruMap.put("key9", "value9"));
        assertEquals(10, mruMap.size());
        assertEquals("value0", mruMap.put("key10", "value10"));
        assertEquals(10, mruMap.size());
        assertEquals("value8", mruMap.remove("key8"));
        mruMap.clear();
        assertEquals(0, mruMap.size());
        assertTrue(mruMap.isEmpty());
    }
}