package de.blau.android.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.view.ViewGroup.LayoutParams;
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.App;
import de.blau.android.Map;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=33)
@LargeTest
public class StorageDelegatorRoboelectricTest {

    /**
     * Pre test setup
     */
    @Before
    public void setup() {
        App.getDelegator().reset(true);
    }

    /**
     * Merge two slightly complicated polygons
     */
    @Test
    public void polgonsToMultiPolygonMergeTest1() {
        StorageDelegator d = RelationUtilTest.loadTestData(getClass());
        Way w9 = RelationUtilTest.getWay(d, -9L);
        Way w8 = RelationUtilTest.getWay(d, -8L);
        MergeAction action = new MergeAction(d, w9, w8);
        List<Result> results = action.mergeSimplePolygons(getMap(d));
        assertEquals(1, results.size());
        Result r = results.get(0);
        assertTrue(r.getElement() instanceof Relation);
        Relation mp = (Relation) r.getElement();
        List<RelationMember> outers = mp.getMembersWithRole(Tags.ROLE_OUTER);
        assertEquals(1, outers.size());
        final Way outer = (Way) outers.get(0).getElement();
        assertTrue(outer.isClosed());
        assertEquals(7, outer.getNodes().size());
        List<RelationMember> inners = mp.getMembersWithRole(Tags.ROLE_INNER);
        assertEquals(2, inners.size());
        final Way inner1 = (Way) inners.get(0).getElement();
        assertTrue(inner1.isClosed());
        assertEquals(5, inner1.getNodes().size());
        final Way inner2 = (Way) inners.get(1).getElement();
        assertTrue(inner2.isClosed());
        assertEquals(5, inner2.getNodes().size());
        assertEquals(-9, outer.getOsmId());
    }

    /**
     * As above but different order
     */
    @Test
    public void polgonsToMultiPolygonMergeTest2() {
        StorageDelegator d = RelationUtilTest.loadTestData(getClass());
        Way w9 = RelationUtilTest.getWay(d, -9L);
        Way w8 = RelationUtilTest.getWay(d, -8L);
        MergeAction action = new MergeAction(d, w8, w9);
        List<Result> results = action.mergeSimplePolygons(getMap(d));
        assertEquals(1, results.size());
        Result r = results.get(0);
        assertTrue(r.getElement() instanceof Relation);
        Relation mp = (Relation) r.getElement();
        List<RelationMember> outers = mp.getMembersWithRole(Tags.ROLE_OUTER);
        assertEquals(1, outers.size());
        final Way outer = (Way) outers.get(0).getElement();
        assertTrue(outer.isClosed());
        assertEquals(7, outer.getNodes().size());
        assertEquals(-8, outer.getOsmId());
    }

    /**
     * As above but reverse 1st way
     */
    @Test
    public void polgonsToMultiPolygonMergeTest3() {
        StorageDelegator d = RelationUtilTest.loadTestData(getClass());
        Way w9 = RelationUtilTest.getWay(d, -9L);
        Way w8 = RelationUtilTest.getWay(d, -8L);
        d.reverseWay(w8);
        MergeAction action = new MergeAction(d, w8, w9);
        List<Result> results = action.mergeSimplePolygons(getMap(d));
        assertEquals(1, results.size());
        Result r = results.get(0);
        assertTrue(r.getElement() instanceof Relation);
        Relation mp = (Relation) r.getElement();
        List<RelationMember> outers = mp.getMembersWithRole(Tags.ROLE_OUTER);
        assertEquals(1, outers.size());
        final Way outer = (Way) outers.get(0).getElement();
        assertTrue(outer.isClosed());
        assertEquals(7, outer.getNodes().size());
        assertEquals(-8, outer.getOsmId());
    }

    /**
     * As above but reverse 2nd way
     */
    @Test
    public void polgonsToMultiPolygonMergeTest4() {
        StorageDelegator d = RelationUtilTest.loadTestData(getClass());
        Way w9 = RelationUtilTest.getWay(d, -9L);
        Way w8 = RelationUtilTest.getWay(d, -8L);
        d.reverseWay(w9);
        MergeAction action = new MergeAction(d, w8, w9);
        List<Result> results = action.mergeSimplePolygons(getMap(d));
        assertEquals(1, results.size());
        Result r = results.get(0);
        assertTrue(r.getElement() instanceof Relation);
        Relation mp = (Relation) r.getElement();
        List<RelationMember> outers = mp.getMembersWithRole(Tags.ROLE_OUTER);
        assertEquals(1, outers.size());
        final Way outer = (Way) outers.get(0).getElement();
        assertTrue(outer.isClosed());
        assertEquals(7, outer.getNodes().size());
        assertEquals(-8, outer.getOsmId());
    }

    /**
     * Two disjunct polygons to MP
     */
    @Test
    public void polgonsToMultiPolygonMergeTest5() {
        StorageDelegator d = RelationUtilTest.loadTestData(getClass());
        Way w1 = RelationUtilTest.getWay(d, -1L);
        Way w3 = RelationUtilTest.getWay(d, -3L);
        MergeAction action = new MergeAction(d, w1, w3);
        List<Result> results = action.mergeSimplePolygons(getMap(d));
        assertEquals(1, results.size());
        Result r = results.get(0);
        assertTrue(r.getElement() instanceof Relation);
        Relation mp = (Relation) r.getElement();
        List<RelationMember> outers = mp.getMembersWithRole(Tags.ROLE_OUTER);
        assertEquals(2, outers.size());
        final Way outer1 = (Way) outers.get(0).getElement();
        assertTrue(outer1.isClosed());
        assertEquals(5, outer1.getNodes().size());
        final Way outer2 = (Way) outers.get(0).getElement();
        assertTrue(outer2.isClosed());
        assertEquals(5, outer2.getNodes().size());
    }

    /**
     * Simple polygons to one polygon
     */
    @Test
    public void polgonsToPolygonMergeTest() {
        StorageDelegator d = RelationUtilTest.loadTestData(getClass());
        Way w7 = RelationUtilTest.getWay(d, -7L);
        Way w6 = RelationUtilTest.getWay(d, -6L);
        MergeAction action = new MergeAction(d, w6, w7);
        List<Result> results = action.mergeSimplePolygons(getMap(d));
        assertEquals(1, results.size());
        Result r = results.get(0);
        assertTrue(r.getElement() instanceof Way);
    }

    /**
     * Get a Map instance
     * 
     * @param d the StorageDelegator
     * @return a Map instance
     */
    private Map getMap(@NonNull StorageDelegator d) {
        Map map = new Map(ApplicationProvider.getApplicationContext());
        LayoutParams lp = new LayoutParams(1024, 1920);
        map.setLayoutParams(lp);
        map.setViewBox(new ViewBox(d.getLastBox()));
        map.layout(0, 0, 1024, 1920);
        return map;
    }
}
