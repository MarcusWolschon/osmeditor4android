package de.blau.android.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.test.filters.LargeTest;
import de.blau.android.App;
import de.blau.android.UnitTestUtils;
import de.blau.android.util.Util;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=33)
@LargeTest
public class RelationUtilTest {

    /**
     * Pre test setup
     */
    @Before
    public void setup() {
        App.getDelegator().reset(true);
    }

    /**
     * Set roles on MP rings
     */
    @Test
    public void setMultiPolygonRolesTest() {
        StorageDelegator d = loadTestData(getClass());
        Way w1 = getWay(d, -1L);
        Way w5 = getWay(d, -5L);
        Way w4 = getWay(d, -4L);
        Way w10 = getWay(d, -10L);
        Relation mp = d.createAndInsertRelation(Arrays.asList(w1, w5, w4, w10));
        assertNotNull(mp);
        Way[] newWays = d.splitAtNodes(w5, w5.getFirstNode(), w5.getNodes().get(2), false);
        assertEquals(2, newWays.length);
        RelationUtils.setMultipolygonRoles(null, mp.getMembers(), false);
        assertEquals(Tags.ROLE_OUTER, mp.getMember(w1).getRole());
        assertEquals(Tags.ROLE_OUTER, mp.getMember(newWays[0]).getRole());
        assertEquals(Tags.ROLE_OUTER, mp.getMember(newWays[1]).getRole());
        assertEquals(Tags.ROLE_INNER, mp.getMember(w4).getRole());
        assertEquals("", mp.getMember(w10).getRole());
    }

    /**
     * Set roles on MP rings odd number of segments
     */
    @Test
    public void setMultiPolygonRolesTest2() {
        StorageDelegator d = UnitTestUtils.loadTestData(getClass(), "rings2.osm");
        Way w1 = getWay(d, -1L);
        Way w2 = getWay(d, -2L);
        Way w12 = getWay(d, -12L);
        Relation mp = d.createAndInsertRelation(Arrays.asList(w1, w2, w12));
        assertNotNull(mp);
        RelationUtils.setMultipolygonRoles(null, mp.getMembers(), false);
        assertEquals(Tags.ROLE_OUTER, mp.getMember(w1).getRole());
        assertEquals(Tags.ROLE_OUTER, mp.getMember(w2).getRole());
        assertEquals(Tags.ROLE_OUTER, mp.getMember(w12).getRole());
    }

    /**
     * Move tags from outer rings to relation test
     */
    @Test
    public void moveOuterTagsTest() {
        StorageDelegator d = loadTestData(getClass());
        Way w1 = getWay(d, -1L);
        addTag(d, w1, Tags.KEY_BUILDING, Tags.VALUE_YES);
        Way w5 = getWay(d, -5L);
        addTag(d, w5, Tags.KEY_BUILDING, Tags.VALUE_YES);
        addTag(d, w5, Tags.KEY_BUILDING_PART, Tags.VALUE_YES);
        Way w4 = getWay(d, -4L);
        addTag(d, w4, Tags.KEY_LANDUSE, "grass");
        Way w10 = getWay(d, -10L);
        Relation mp = d.createAndInsertRelation(Arrays.asList(w1, w5, w4, w10));
        assertNotNull(mp);
        RelationUtils.setMultipolygonRoles(null, mp.getMembers(), false);
        assertFalse(mp.hasTags());
        d.setTags(mp, RelationUtils.addTypeTag(Tags.VALUE_MULTIPOLYGON, mp.getTags()));
        RelationUtils.moveOuterTags(d, mp);
        assertTrue(mp.hasTag(Tags.KEY_TYPE, Tags.VALUE_MULTIPOLYGON));
        assertTrue(mp.hasTag(Tags.KEY_BUILDING, Tags.VALUE_YES));
        assertFalse(w1.hasTags());
        assertTrue(w5.hasTag(Tags.KEY_BUILDING_PART, Tags.VALUE_YES));
        assertTrue(w4.hasTag(Tags.KEY_LANDUSE, "grass"));
    }

    /**
     * Sort a large relation
     * 
     * The relation was pre-sorted (and fixed) in JOSM, so this test doesn't really show much more than that it doesn't
     * crash and is stable (aka doesn't change sorted data)
     */
    @Test
    public void sortTest() {
        StorageDelegator d = UnitTestUtils.loadTestData(getClass(), "500_member_relation_sorted.osm");
        Relation large = (Relation) d.getOsmElement(Relation.NAME, 49094);
        assertNotNull(large);
        List<RelationMember> members = new ArrayList<>(large.getMembers());
        long start = System.currentTimeMillis();
        List<RelationMember> presorted = RelationUtils.sortRelationMembers(members);
        Log.d("sortTest", "Sorting time " + (System.currentTimeMillis() - start));
        assertEquals(large.getMembers().size(), presorted.size());
        assertEquals(large.getMembers(), presorted);
    }

    /**
     * Sort relations by distance from a way
     */
    @Test
    public void sortRelationListByDistance() {
        StorageDelegator d = UnitTestUtils.loadTestData(getClass(), "test2.osm");
        Way w = (Way) d.getOsmElement(Way.NAME, 437198585L);
        assertNotNull(w);
        List<Relation> relations = new ArrayList<>(d.getCurrentStorage().getRelations());
        assertNotNull(relations);
        assertEquals(5, relations.size());
        RelationUtils.sortRelationListByDistance(Util.wrapInList(w), relations);
        assertEquals(5, relations.size());
        assertEquals(6490362L, relations.get(0).getOsmId());
    }

    /**
     * Add tag to element
     * 
     * @param d current StorageDelegator
     * @param e the tag
     * @param key tag key
     * @param value tag value
     */
    private void addTag(@NonNull StorageDelegator d, @NonNull OsmElement e, @NonNull String key, @NonNull String value) {
        Map<String, String> tags = new TreeMap<>(e.getTags());
        tags.put(key, value);
        d.setTags(e, tags);
    }

    /**
     * Get a specific Way from storage
     * 
     * @param d a StorageDelegator
     * @param id the OSM id
     * @return a Way
     */
    static Way getWay(@NonNull StorageDelegator d, long id) {
        Way w = (Way) d.getOsmElement(Way.NAME, id);
        assertNotNull(w);
        return w;
    }

    /**
     * Load some test data in to storage
     * 
     * @param c Class
     * @return the StorageDelegator
     * 
     */
    @SuppressWarnings("rawtypes")
    @NonNull
    public static StorageDelegator loadTestData(@NonNull Class c) {
        return UnitTestUtils.loadTestData(c, "rings.osm");
    }
}
