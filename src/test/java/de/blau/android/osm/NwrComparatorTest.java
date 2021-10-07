package de.blau.android.osm;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NwrComparatorTest {

    /**
     * Test that comparator gives back expected results
     */
    @Test
    public void comparator() {
        Node n = new Node(0L, 0L, 0L, (byte) 0, 0, 0);
        Way w = new Way(0L, 0L, 0L, (byte) 0);
        Relation r = new Relation(0L, 0L, 0L, (byte) 0);

        NwrComparator nwr = new NwrComparator();

        assertEquals(0, nwr.compare(n, n));
        assertEquals(-1, nwr.compare(n, w));
        assertEquals(-1, nwr.compare(n, r));

        assertEquals(1, nwr.compare(w, n));
        assertEquals(0, nwr.compare(w, w));
        assertEquals(-1, nwr.compare(w, r));

        assertEquals(1, nwr.compare(r, n));
        assertEquals(1, nwr.compare(r, w));
        assertEquals(0, nwr.compare(r, r));
    }
}
