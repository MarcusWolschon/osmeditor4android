package de.blau.android;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import de.blau.android.Selection.Ids;
import de.blau.android.osm.Node;
import de.blau.android.osm.Relation;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Way;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = { ShadowWorkManager.class }, sdk=33)
public class SelectionTest {
    Context  context;

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        context = ApplicationProvider.getApplicationContext();
    }

    /**
     * Get elements by their ids
     */
    @Test
    public void fromIds() {
        StorageDelegator d = UnitTestUtils.loadTestData(getClass(), "test2.osm");
        Selection s = new Selection();
        s.fromIds(context, d, new Ids(new long[] {101792984L}, new long[] {437198585L}, new long[] {6490362L}));
        assertEquals(1, s.nodeCount());
        assertEquals(1, s.wayCount());
        Way w = s.getWay();
        assertEquals(1, s.relationCount());
        assertEquals(3, s.count());
        //
        s.reset();
        assertEquals(0, s.getAll().size());
        w.removeAllNodes();
        assertEquals(0, w.nodeCount());
        //
        // we should now not add the way as it is degenerate
        //
        s.fromIds(context, d, new Ids(new long[] {101792984L}, new long[] {437198585L}, new long[] {6490362L}));
        assertEquals(1, s.nodeCount());
        assertEquals(0, s.wayCount());
        assertEquals(1, s.relationCount());
        assertEquals(2, s.count());
    }
    
    /**
     * Add and remove all three element types
     */
    @Test
    public void addRemoveSelection() {
        StorageDelegator d = UnitTestUtils.loadTestData(getClass(), "test2.osm");
        Selection s = new Selection();
        
        Node n = (Node) d.getOsmElement(Node.NAME, 101792984L);
        s.add(n);
        assertEquals(1, s.nodeCount());
        assertEquals(0, s.wayCount());
        assertEquals(0, s.relationCount());
        assertEquals(1, s.count());
        s.remove(n);
        assertEquals(0, s.nodeCount());
        assertEquals(0, s.wayCount());
        assertEquals(0, s.relationCount());
        assertEquals(0, s.count());
        
        Way w = (Way) d.getOsmElement(Way.NAME, 437198585L);
        s.add(w);
        assertEquals(0, s.nodeCount());
        assertEquals(1, s.wayCount());
        assertEquals(0, s.relationCount());
        assertEquals(1, s.count());
        s.remove(w);
        assertEquals(0, s.nodeCount());
        assertEquals(0, s.wayCount());
        assertEquals(0, s.relationCount());
        assertEquals(0, s.count());
        
        Relation r = (Relation) d.getOsmElement(Relation.NAME, 6490362L);
        s.add(r);
        assertEquals(0, s.nodeCount());
        assertEquals(0, s.wayCount());
        assertEquals(1, s.relationCount());
        assertEquals(1, s.count());
        s.remove(r);
        assertEquals(0, s.nodeCount());
        assertEquals(0, s.wayCount());
        assertEquals(0, s.relationCount());
        assertEquals(0, s.count());
    }
}