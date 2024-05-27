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
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Way;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = { ShadowWorkManager.class })
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
        assertEquals(3, s.getAll().size());
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
        assertEquals(2, s.getAll().size());
    }
}