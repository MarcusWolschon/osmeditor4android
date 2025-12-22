package de.blau.android.osm;

import static de.blau.android.osm.DelegatorUtil.toE7;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.xml.sax.SAXException;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.UnitTestUtils;
import de.blau.android.exception.DataConflictException;
import de.blau.android.exception.OsmException;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.DataStyle;
import de.blau.android.util.Coordinates;
import de.blau.android.util.Geometry;
import de.blau.android.util.Util;
import de.blau.android.util.collections.MultiHashMap;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
@LargeTest
public class StorageDelegatorTest {

    /**
     * Test way rotation
     */
    @Test
    public void rotate() {
        StorageDelegator d = new StorageDelegator();
        Way w = DelegatorUtil.addWayToStorage(d, true);
        Node n0 = w.getNodes().get(0);

        try {
            d.getUndo().createCheckpoint("rotate test", null);
            ViewBox v = new ViewBox(0D, 51.476D, 0.003D, 51.478D);
            Coordinates[] coords = Coordinates.nodeListToCoordinateArray(1000, 2000, v, new ArrayList<>(w.getNodes()));
            Coordinates center = Geometry.centroidXY(coords, true);
            d.rotateNodes(w.getNodes(), (float) Math.toRadians(180), -1, (float) center.x, (float) center.y, 1000, 2000, v);
            assertEquals(514760000, n0.getLat());
            assertEquals(30000, n0.getLon());
        } catch (OsmException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Test copy
     */
    @Test
    public void copy() {
        StorageDelegator d = new StorageDelegator();
        Way w = DelegatorUtil.addWayToStorage(d, true);
        int count = d.getApiElementCount();
        Way temp = (Way) d.getOsmElement(Way.NAME, w.getOsmId());
        assertNotNull(temp);
        double[] centroid = Geometry.centroidLonLat(w);
        d.copyToClipboard(Util.wrapInList(w), toE7(centroid[1]), toE7(centroid[0]));
        assertNotNull((Way) d.getOsmElement(Way.NAME, w.getOsmId()));
        assertEquals(count, d.getApiElementCount()); // should not change
        List<OsmElement> pasted = d.pasteFromClipboard(0, toE7(centroid[1] + 1), toE7(centroid[0] + 1));
        assertEquals(1, pasted.size());
        temp = (Way) d.getOsmElement(Way.NAME, pasted.get(0).getOsmId());
        assertNotNull(temp);
        assertFalse(d.clipboardIsEmpty());
        double[] centroid2 = Geometry.centroidLonLat(temp);
        assertEquals(centroid[1] + 1, centroid2[1], 0.001);
        assertEquals(centroid[0] + 1, centroid2[0], 0.001);
    }

    /**
     * Test cut
     */
    @Test
    public void cut() {
        StorageDelegator d = new StorageDelegator();
        Way w = DelegatorUtil.addWayToStorage(d, true);

        Way temp = (Way) d.getOsmElement(Way.NAME, w.getOsmId());
        assertNotNull(temp);
        double[] centroid = Geometry.centroidLonLat(w);
        d.cutToClipboard(Util.wrapInList(w), toE7(centroid[1]), toE7(centroid[0]));
        assertNull(d.getOsmElement(Way.NAME, w.getOsmId()));
        d.pasteFromClipboard(0, toE7(centroid[1] + 1), toE7(centroid[0] + 1));
        temp = (Way) d.getOsmElement(Way.NAME, w.getOsmId());
        assertNotNull(temp);
        assertTrue(!d.clipboardIsEmpty());
        List<ClipboardStorage> clipboards = d.getClipboards();
        assertNotNull(clipboards);
        assertFalse(clipboards.get(0).contentsWasCut()); // after paste switch to copy
        double[] centroid2 = Geometry.centroidLonLat(temp);
        assertEquals(centroid[1] + 1, centroid2[1], 0.001);
        assertEquals(centroid[0] + 1, centroid2[0], 0.001);
    }

    /**
     * Test cut and then undo
     */
    @Test
    public void cutUndo() {
        StorageDelegator d = App.getDelegator();
        Logic logic = App.newLogic();
        logic.setMap(new de.blau.android.Map(ApplicationProvider.getApplicationContext()), true);
        Way w = DelegatorUtil.addWayToStorage(d, true);
        Way temp = (Way) d.getOsmElement(Way.NAME, w.getOsmId());
        assertNotNull(temp);
        logic.cutToClipboard(null, Util.wrapInList(w));
        assertNull(d.getOsmElement(Way.NAME, w.getOsmId()));
        assertFalse(d.clipboardIsEmpty());
        List<ClipboardStorage> clipboards = d.getClipboards();
        assertNotNull(clipboards);
        assertTrue(clipboards.get(0).contentsWasCut());
        // undo should empty clipboard
        logic.undo();
        assertTrue(d.clipboardIsEmpty());
    }

    /**
     * Test duplication
     */
    @Test
    public void duplicateWay() {
        StorageDelegator d = new StorageDelegator();
        Way w = DelegatorUtil.addWayToStorage(d, true);

        List<OsmElement> duplicates = d.duplicate(Util.wrapInList(w), true);
        assertNotNull(d.getOsmElement(Way.NAME, w.getOsmId()));
        assertEquals(1, duplicates.size());
        assertTrue(duplicates.get(0) instanceof Way);
        Way dup = (Way) duplicates.get(0);
        assertNotEquals(w, dup);
        assertEquals(w.nodeCount(), dup.nodeCount());
        List<Node> originalNodes = w.getNodes();
        List<Node> dupNodes = dup.getNodes();
        for (int i = 0; i < w.nodeCount(); i++) {
            assertNotEquals(originalNodes.get(i), dupNodes.get(i));
            assertEquals(originalNodes.get(i).getLat(), dupNodes.get(i).getLat());
            assertEquals(originalNodes.get(i).getLon(), dupNodes.get(i).getLon());
        }
    }

    /**
     * Test shallow duplication
     */
    @Test
    public void shallowDuplicateWay() {
        StorageDelegator d = new StorageDelegator();
        Way w = DelegatorUtil.addWayToStorage(d, true);

        List<OsmElement> duplicates = d.duplicate(Util.wrapInList(w), false);
        assertNotNull(d.getOsmElement(Way.NAME, w.getOsmId()));
        assertEquals(1, duplicates.size());
        assertTrue(duplicates.get(0) instanceof Way);
        Way dup = (Way) duplicates.get(0);
        assertNotEquals(w, dup);
        assertEquals(w.nodeCount(), dup.nodeCount());
        List<Node> originalNodes = w.getNodes();
        List<Node> dupNodes = dup.getNodes();
        for (int i = 0; i < w.nodeCount(); i++) {
            assertEquals(originalNodes.get(i), dupNodes.get(i));
        }
    }

    /**
     * Test that just changing the role adds the parent relation to api storage
     */
    @Test
    public void updateParentRelation() {
        StorageDelegator d = new StorageDelegator();
        Way w = DelegatorUtil.addWayToStorage(d, true);

        final Relation r = w.getParentRelations().get(0);
        d.getApiStorage().removeElement(r);
        assertFalse(d.getApiStorage().contains(r));
        assertTrue(r.getMembersWithRole("test2").isEmpty());
        List<RelationMemberPosition> members = r.getAllMembersWithPosition(w);

        assertEquals(1, members.size());
        RelationMemberPosition newMember = RelationMemberPosition.copyWithoutElement(members.get(0));
        newMember.setRole("test2");
        MultiHashMap<Long, RelationMemberPosition> parents = new MultiHashMap<>();
        parents.add(r.getOsmId(), newMember);
        d.updateParentRelations(w, parents);

        assertFalse(r.getMembersWithRole("test2").isEmpty());
        assertTrue(d.getApiStorage().contains(r));
        assertEquals(OsmElement.STATE_CREATED, r.getState());
    }

    /**
     * Test duplication
     */
    @Test
    public void duplicateRelation() {
        StorageDelegator d = new StorageDelegator();
        Way w = DelegatorUtil.addWayToStorage(d, true);

        final Relation r = w.getParentRelations().get(0);
        List<OsmElement> duplicates = d.duplicate(Util.wrapInList(r), true);
        assertNotNull(d.getOsmElement(Way.NAME, w.getOsmId()));
        assertEquals(1, duplicates.size());
        assertTrue(duplicates.get(0) instanceof Relation);
        Relation dup = (Relation) duplicates.get(0);
        assertNotEquals(r, dup);
        assertEquals(r.getMemberCount(), dup.getMemberCount());
        List<RelationMember> originalMembers = r.getMembers();
        List<RelationMember> dupMembers = dup.getMembers();
        final OsmElement dupElement = dupMembers.get(0).getElement();
        assertNotEquals(originalMembers.get(0).getElement(), dupElement);
        assertTrue(dupElement instanceof Way);
        List<Node> originalNodes = w.getNodes();
        List<Node> dupNodes = ((Way) dupElement).getNodes();
        for (int i = 0; i < w.nodeCount(); i++) {
            assertNotEquals(originalNodes.get(i), dupNodes.get(i));
            assertEquals(originalNodes.get(i).getLat(), dupNodes.get(i).getLat());
            assertEquals(originalNodes.get(i).getLon(), dupNodes.get(i).getLon());
        }
    }

    /**
     * Test duplication with missing member, should throw an exception
     */
    @Test
    public void duplicateRelation2() {
        StorageDelegator d = new StorageDelegator();
        Way w = DelegatorUtil.addWayToStorage(d, true);
        final Relation r = w.getParentRelations().get(0);
        r.addMember(new RelationMember(Way.NAME, -11111, ""));
        d.insertElementSafe(r);
        try {
            d.duplicate(Util.wrapInList(r), true);
        } catch (OsmIllegalOperationException ex) {
            // expected
            return;
        }
        fail("should have thrown an exception");
    }

    /**
     * Test shallow duplication
     */
    @Test
    public void shallowDuplicateRelation() {
        StorageDelegator d = new StorageDelegator();
        Way w = DelegatorUtil.addWayToStorage(d, true);

        final Relation r = w.getParentRelations().get(0);
        List<OsmElement> duplicates = d.duplicate(Util.wrapInList(r), false);
        assertNotNull(d.getOsmElement(Way.NAME, w.getOsmId()));
        assertEquals(1, duplicates.size());
        assertTrue(duplicates.get(0) instanceof Relation);
        Relation dup = (Relation) duplicates.get(0);
        assertNotEquals(r, dup);
        assertEquals(r.getMemberCount(), dup.getMemberCount());
        List<RelationMember> originalMembers = r.getMembers();
        List<RelationMember> dupMembers = dup.getMembers();
        for (int i = 0; i < r.getMemberCount(); i++) {
            assertEquals(originalMembers.get(i).getElement(), dupMembers.get(i).getElement());
        }
    }

    /**
     * Load some data modify a way and a node, then prune
     */
    @Test
    public void pruneAll() {
        StorageDelegator d = new StorageDelegator();
        d.setCurrentStorage(PbfTest.read());
        assertEquals(0, d.getApiElementCount());
        assertEquals(258905, d.getCurrentStorage().getNodeCount());
        assertEquals(26454, d.getCurrentStorage().getWayCount());
        assertEquals(751, d.getCurrentStorage().getRelationCount());
        Way w = (Way) d.getOsmElement(Way.NAME, 571067343L);
        int wayNodeCount = w.nodeCount();
        assertNotNull(w);
        int parentCount = w.getParentRelations().size();
        SortedMap<String, String> tags = new TreeMap<>(w.getTags());
        tags.put("test", "pruneAll");
        d.getUndo().createCheckpoint("pruneAll", null);
        d.setTags(w, tags);
        assertEquals(1, d.getApiElementCount());
        Node n = (Node) d.getOsmElement(Node.NAME, 761534749L);
        assertNotNull(n);
        n.setLat(toE7(47.1187142));
        n.setLon(toE7(9.5430107));
        d.insertElementSafe(n);
        d.pruneAll();
        assertNotNull(d.getOsmElement(Way.NAME, 571067343L));
        assertNotNull(d.getOsmElement(Node.NAME, 761534749L));
        assertEquals(wayNodeCount + 1L, d.getCurrentStorage().getNodeCount());
        assertEquals(1, d.getCurrentStorage().getWayCount());
        assertEquals(32, d.getCurrentStorage().getRelationCount());
        assertEquals(parentCount, d.getOsmElement(Way.NAME, 571067343L).getParentRelations().size());
    }

    /**
     * Load some data modify a way and a node, then prune
     */
    @Test
    public void prune() {
        StorageDelegator d = new StorageDelegator();
        d.setCurrentStorage(PbfTest.read());
        assertEquals(0, d.getApiElementCount());
        assertEquals(258905, d.getCurrentStorage().getNodeCount());
        assertEquals(26454, d.getCurrentStorage().getWayCount());
        assertEquals(751, d.getCurrentStorage().getRelationCount());

        // change a tag on a way
        Way w = (Way) d.getOsmElement(Way.NAME, 571067343L);
        assertNotNull(w);
        int parentCount = w.getParentRelations().size();
        SortedMap<String, String> tags = new TreeMap<>(w.getTags());
        tags.put("test", "prunBox");
        d.getUndo().createCheckpoint("pruneBox", null);
        d.setTags(w, tags);
        assertEquals(1, d.getApiElementCount());

        // change node position
        Node n = (Node) d.getOsmElement(Node.NAME, 761534749L);
        assertNotNull(n);
        n.setLat(toE7(47.1187142));
        n.setLon(toE7(9.5430107));
        n.updateState(OsmElement.STATE_MODIFIED);
        d.insertElementSafe(n);

        // change way node position
        n = (Node) d.getOsmElement(Node.NAME, 3015985546L);
        assertNotNull(n);
        n.setLon(toE7(9.5426));
        n.updateState(OsmElement.STATE_MODIFIED);
        d.insertElementSafe(n);

        // now prune
        d.prune(null, new BoundingBox(9.52077, 47.13829, 9.52248, 47.14087));
        assertNotNull(d.getOsmElement(Way.NAME, 571067343L));
        assertNotNull(d.getOsmElement(Node.NAME, 761534749L));
        assertNotNull(d.getOsmElement(Relation.NAME, 30544L));
        assertNotNull(d.getOsmElement(Relation.NAME, 8130924L));
        assertNotNull(d.getOsmElement(Relation.NAME, 8134437L));
        assertNotNull(d.getOsmElement(Way.NAME, 297699554L));
        assertNull(d.getOsmElement(Relation.NAME, 6907800L));
        // check overall counts
        assertEquals(1999, d.getCurrentStorage().getNodeCount());
        assertEquals(104, d.getCurrentStorage().getWayCount());
        assertEquals(79, d.getCurrentStorage().getRelationCount());
        // prune elsewhere
        d.prune(null, new BoundingBox(9.52077, 47.13829, 9.52248, 47.14087));
        assertEquals(61, d.getCurrentStorage().getRelationCount());
        assertNotNull(d.getOsmElement(Relation.NAME, 30544L));
        assertNull(d.getOsmElement(Relation.NAME, 8130924L));
        assertNotNull(d.getOsmElement(Relation.NAME, 8134437L));
        assertEquals(parentCount, d.getOsmElement(Way.NAME, 571067343L).getParentRelations().size());
        List<OsmElement> downloaded = new ArrayList<>();
        for (RelationMember rm : ((Relation) d.getOsmElement(Relation.NAME, 8134437L)).getMembers()) {
            if (rm.getElement() != null) {
                downloaded.add(rm.getElement());
            }
        }
        // 5484325404 1107468615
        assertTrue(downloaded.contains(d.getOsmElement(Node.NAME, 5484325404L)));
        assertTrue(downloaded.contains(d.getOsmElement(Node.NAME, 1107468615L)));
        // 571067336 571067343 571452863 529794576 140309501
        assertTrue(downloaded.contains(d.getOsmElement(Way.NAME, 571067336L)));
        assertTrue(downloaded.contains(d.getOsmElement(Way.NAME, 571067343L)));
        assertTrue(downloaded.contains(d.getOsmElement(Way.NAME, 571452863L)));
        assertTrue(downloaded.contains(d.getOsmElement(Way.NAME, 529794576L)));
        assertTrue(downloaded.contains(d.getOsmElement(Way.NAME, 140309501L)));
    }

    /**
     * Load some data modify a way and a node, then merge some data
     */
    @Test
    public void mergeData() {
        StorageDelegator d = new StorageDelegator();
        d.setCurrentStorage(PbfTest.read());
        assertEquals(0, d.getApiElementCount());
        final int nodeCount = 258905;
        assertEquals(nodeCount, d.getCurrentStorage().getNodeCount());
        final int wayCount = 26454;
        assertEquals(wayCount, d.getCurrentStorage().getWayCount());
        assertEquals(751, d.getCurrentStorage().getRelationCount());
        Way w = (Way) d.getOsmElement(Way.NAME, 571067343L);
        assertNotNull(w);
        SortedMap<String, String> tags = new TreeMap<>(w.getTags());
        tags.put("test", "merge");
        d.getUndo().createCheckpoint("merge", null);
        d.setTags(w, tags);
        assertEquals(1, d.getApiElementCount());
        Node n = (Node) d.getOsmElement(Node.NAME, 761534749L);
        assertNotNull(n);
        n.setLat(toE7(47.1187142));
        n.setLon(toE7(9.5430107));
        d.insertElementSafe(n);

        StorageDelegator d2 = new StorageDelegator();
        Way w2 = DelegatorUtil.addWayToStorage(d2, true);

        try {
            d.mergeData(d2.getCurrentStorage(), null);
        } catch (DataConflictException e) {
            fail(e.getMessage());
        }

        assertNotNull(d.getOsmElement(Way.NAME, 571067343L));
        assertNotNull(d.getOsmElement(Way.NAME, w2.getOsmId()));
        assertNotNull(d.getOsmElement(Node.NAME, 761534749L));
        assertEquals(nodeCount + 4L, d.getCurrentStorage().getNodeCount());
        assertEquals(wayCount + 1L, d.getCurrentStorage().getWayCount());
    }

    /**
     * Load some data remove way from relation, prune data, merge again
     */
    @Test
    public void mergeData2() {
        StorageDelegator d = UnitTestUtils.loadTestData(getClass(), "lady_moon_lake.osm");
        InputStream input = getClass().getResourceAsStream("/lady_moon_lake.osm");
        OsmParser parser = new OsmParser();
        Storage orig = null;
        try {
            parser.start(input);
            orig = parser.getStorage();
        } catch (SAXException | IOException | ParserConfigurationException | IllegalArgumentException | IllegalStateException e) {
            fail(e.getMessage());
        }

        Way w = (Way) d.getOsmElement(Way.NAME, 353593072L);
        assertNotNull(w);
        Relation r = (Relation) d.getOsmElement(Relation.NAME, 19172024L);
        assertNotNull(r);
        RelationMember rm = r.getMember(w);
        assertNotNull(rm);

        d.getUndo().createCheckpoint("remove way from relation", null);
        d.removeRelationMembersFromRelation(r, Util.wrapInList(rm));

        assertNull(r.getMember(w));

        d.pruneAll();

        assertNotNull(d.getOsmElement(Relation.NAME, 19172024L));
        assertNull(d.getOsmElement(Way.NAME, 353593072L));
        assertEquals(1, d.getCurrentElementCount());

        assertNotNull(orig.getWay(353593072L));
        try {
            d.mergeData(orig, null);
        } catch (DataConflictException e) {
            fail(e.getMessage());
        }

        w = (Way) d.getOsmElement(Way.NAME, 353593072L);
        assertNotNull(w);
        assertNull(w.getParentRelations());
        rm = r.getMember(w);
        assertNull(rm);
    }

    /**
     * Merge some data into empty delegator to make sure that backlinks are re-created properly
     */
    @Test
    public void mergeData3() {
        StorageDelegator d = App.getDelegator();
        d.reset(true);
        InputStream input = getClass().getResourceAsStream("/lady_moon_lake.osm");
        OsmParser parser = new OsmParser();
        Storage orig = null;
        try {
            parser.start(input);
            orig = parser.getStorage();
        } catch (SAXException | IOException | ParserConfigurationException | IllegalArgumentException | IllegalStateException e) {
            fail(e.getMessage());
        }

        try {
            d.mergeData(orig, null);
        } catch (DataConflictException e) {
            fail(e.getMessage());
        }

        Way w = (Way) d.getOsmElement(Way.NAME, 353593072L);
        assertNotNull(w);
        Relation r = (Relation) d.getOsmElement(Relation.NAME, 19172024L);
        assertNotNull(r);
        RelationMember rm = r.getMember(w);
        assertNotNull(rm);
        assertTrue(w.getParentRelations().contains(r));
    }

    /**
     * Load some data try to merge a way with a higher version
     */
    @Test
    public void mergeDataConflict() {
        StorageDelegator d = new StorageDelegator();
        d.setCurrentStorage(PbfTest.read());
        Way w = (Way) d.getOsmElement(Way.NAME, 571067343L);
        assertNotNull(w);
        SortedMap<String, String> tags = new TreeMap<>(w.getTags());
        tags.put("test", "merge");
        d.getUndo().createCheckpoint("merge", null);
        d.setTags(w, tags);
        assertEquals(1, d.getApiElementCount());
        Node n = (Node) d.getOsmElement(Node.NAME, 761534749L);
        assertNotNull(n);
        n.setLat(toE7(47.1187142));
        n.setLon(toE7(9.5430107));
        d.insertElementSafe(n);

        StorageDelegator d2 = new StorageDelegator();
        Way w2 = DelegatorUtil.addWayToStorage(d2, true);
        w2.setOsmId(w.getOsmId());
        w2.setOsmVersion(w.getOsmVersion() + 1);
        d2.getCurrentStorage().rehash();
        d2.getApiStorage().rehash();
        try {
            d.mergeData(d2.getCurrentStorage(), null);
            fail("fail expected");
        } catch (DataConflictException e) {
            // nothing
        }
    }

    /**
     * Split way then merge in various ways
     */
    @Test
    public void merge() {
        StorageDelegator d = new StorageDelegator();
        Way w = DelegatorUtil.addWayToStorage(d, false);
        SortedMap<String, String> tags = new TreeMap<>(w.getTags());
        tags.put(Tags.KEY_HIGHWAY, "residential");
        w.setTags(tags);
        Way temp = (Way) d.getOsmElement(Way.NAME, w.getOsmId());
        assertNotNull(temp);
        Node n = w.getNodes().get(2);
        Node first = w.getFirstNode();
        Node last = w.getLastNode();
        List<Result> splitResult = d.splitAtNode(w, n, true);
        assertNotNull(splitResult);
        assertFalse(splitResult.isEmpty());
        Way newWay = (Way) splitResult.get(0).getElement();

        // all things the same the 1st way remains after merger
        MergeAction action = new MergeAction(d, w, newWay, null);
        List<Result> result = action.mergeWays();
        assertFalse(result.isEmpty());
        assertFalse(result.get(0).hasIssue());
        assertEquals(4, w.getNodes().size());
        assertNull(d.getOsmElement(Way.NAME, newWay.getOsmId()));
        splitResult = d.splitAtNode(w, n, true);
        assertNotNull(splitResult);
        assertFalse(splitResult.isEmpty());
        newWay = (Way) splitResult.get(0).getElement();
        action = new MergeAction(d, newWay, w, false, null);
        result = action.mergeWays();
        assertFalse(result.isEmpty());
        assertFalse(result.get(0).hasIssue());
        assertEquals(4, newWay.getNodes().size());
        assertNull(d.getOsmElement(Way.NAME, w.getOsmId()));
        //
        splitResult = d.splitAtNode(newWay, n, true);
        assertNotNull(splitResult);
        assertFalse(splitResult.isEmpty());
        w = (Way) splitResult.get(0).getElement();
        d.reverseWay(w);
        action = new MergeAction(d, w, newWay, false, null);
        result = action.mergeWays();
        assertFalse(result.isEmpty());
        assertFalse(result.get(0).hasIssue());
        assertEquals(4, w.getNodes().size());
        assertEquals(last, w.getFirstNode());
        assertEquals(first, w.getLastNode());
        //
        splitResult = d.splitAtNode(w, n, true);
        assertNotNull(splitResult);
        assertFalse(splitResult.isEmpty());
        newWay = (Way) splitResult.get(0).getElement();
        d.reverseWay(w);
        action = new MergeAction(d, w, newWay, false, null);
        result = action.mergeWays();
        assertFalse(result.isEmpty());
        assertFalse(result.get(0).hasIssue());
        assertEquals(4, w.getNodes().size());
        assertEquals(first, w.getFirstNode());
        assertEquals(last, w.getLastNode());

        // conflicting tags should allow merge but create non null result
        splitResult = d.splitAtNode(w, n, true);
        assertNotNull(splitResult);
        assertFalse(splitResult.isEmpty());
        newWay = (Way) splitResult.get(0).getElement();
        tags.clear();
        tags.put(Tags.KEY_HIGHWAY, "service");
        newWay.setTags(tags);
        action = new MergeAction(d, w, newWay, false, null);
        result = action.mergeWays();
        assertEquals(4, w.getNodes().size());
        assertFalse(result.isEmpty());
        assertEquals(1, result.get(0).getIssues().size());
        assertTrue(result.get(0).getIssues().contains(MergeIssue.MERGEDTAGS));
        tags.clear();
        w.setTags(tags);

        // metric tags should allow merge but create non null result
        splitResult = d.splitAtNode(w, n, true);
        assertNotNull(splitResult);
        assertFalse(splitResult.isEmpty());
        newWay = (Way) splitResult.get(0).getElement();
        tags.clear();
        tags.put(Tags.KEY_STEP_COUNT, "3");
        newWay.setTags(tags);
        tags.put(Tags.KEY_STEP_COUNT, "4");
        w.setTags(tags);
        action = new MergeAction(d, w, newWay, false, null);
        result = action.mergeWays();
        assertEquals(4, w.getNodes().size());
        assertFalse(result.isEmpty());
        assertEquals(1, result.get(0).getIssues().size());
        assertTrue(result.get(0).getIssues().contains(MergeIssue.MERGEDMETRIC));
        assertEquals("7", w.getTagWithKey(Tags.KEY_STEP_COUNT));
        tags.clear();
        w.setTags(tags);

        // metric tags should allow merge but create non null result
        // just one way with tag
        splitResult = d.splitAtNode(w, n, true);
        assertNotNull(splitResult);
        assertFalse(splitResult.isEmpty());
        newWay = (Way) splitResult.get(0).getElement();
        tags.clear();
        tags.put(Tags.KEY_STEP_COUNT, "3");
        newWay.setTags(tags);
        action = new MergeAction(d, w, newWay, false, null);
        result = action.mergeWays();
        assertEquals(4, w.getNodes().size());
        assertFalse(result.isEmpty());
        assertNotNull(result.get(0).getIssues());
        assertEquals(1, result.get(0).getIssues().size());
        assertTrue(result.get(0).getIssues().contains(MergeIssue.MERGEDMETRIC));
        assertEquals("3", w.getTagWithKey(Tags.KEY_STEP_COUNT));
        tags.clear();
        w.setTags(tags);

        // metric tags should allow merge but create non null result
        // invalid tag value
        splitResult = d.splitAtNode(w, n, true);
        assertNotNull(splitResult);
        assertFalse(splitResult.isEmpty());
        newWay = (Way) splitResult.get(0).getElement();
        tags.clear();
        tags.put(Tags.KEY_STEP_COUNT, "ABC");
        newWay.setTags(tags);
        action = new MergeAction(d, w, newWay, false, null);
        result = action.mergeWays();
        assertEquals(4, w.getNodes().size());
        assertFalse(result.isEmpty());
        assertEquals(1, result.get(0).getIssues().size());
        assertTrue(result.get(0).getIssues().contains(MergeIssue.MERGEDMETRIC));
        assertEquals("ABC", w.getTagWithKey(Tags.KEY_STEP_COUNT));
        tags.clear();
        w.setTags(tags);

        // duration tags should allow merge but create non null result
        splitResult = d.splitAtNode(w, n, true);
        assertNotNull(splitResult);
        assertFalse(splitResult.isEmpty());
        newWay = (Way) splitResult.get(0).getElement();
        tags.clear();
        tags.put(Tags.KEY_DURATION, "00:32");
        newWay.setTags(tags);
        tags.put(Tags.KEY_DURATION, "5");
        w.setTags(tags);
        action = new MergeAction(d, w, newWay, false, null);
        result = action.mergeWays();
        assertEquals(4, w.getNodes().size());
        assertFalse(result.isEmpty());
        assertEquals(1, result.get(0).getIssues().size());
        assertTrue(result.get(0).getIssues().contains(MergeIssue.MERGEDMETRIC));
        assertEquals("37", w.getTagWithKey(Tags.KEY_DURATION));
        tags.clear();
        w.setTags(tags);

        // conflicting roles should allow merge but create non null result
        splitResult = d.splitAtNode(w, n, true);
        assertNotNull(splitResult);
        assertFalse(splitResult.isEmpty());
        newWay = (Way) splitResult.get(0).getElement();
        assertNotNull(newWay.getParentRelations());
        Relation r = newWay.getParentRelations().get(0);
        r.getMember(newWay).setRole("test2");
        action = new MergeAction(d, w, newWay, false, null);
        result = action.mergeWays();
        assertEquals(4, w.getNodes().size());
        assertFalse(result.isEmpty());
        assertEquals(1, result.get(1).getIssues().size());
        assertTrue(result.get(1).getIssues().contains(MergeIssue.ROLECONFLICT));

        // way with pos id should remain
        splitResult = d.splitAtNode(w, n, true);
        assertNotNull(splitResult);
        assertFalse(splitResult.isEmpty());
        newWay = (Way) splitResult.get(0).getElement();
        newWay.setOsmId(1234L);
        action = new MergeAction(d, w, newWay, null);
        result = action.mergeWays();
        assertFalse(result.isEmpty());
        assertFalse(result.get(0).hasIssue());
        assertEquals(4, newWay.getNodes().size());
        assertNull(d.getOsmElement(Way.NAME, w.getOsmId()));
        assertNotNull(d.getOsmElement(Way.NAME, 1234L));

        // unjoin ways
        splitResult = d.splitAtNode(newWay, n, true);
        assertNotNull(splitResult);
        assertFalse(splitResult.isEmpty());
        w = (Way) splitResult.get(0).getElement();
        d.unjoinWays(n);
        action = new MergeAction(d, w, newWay, false, null);
        try {
            action.mergeWays();
            fail("Should have thrown an OsmIllegalOperationException");
        } catch (OsmIllegalOperationException ex) {
            // expected
        }
    }

    /**
     * Merge two nodes
     */
    @Test
    public void mergeNodes1() {
        StorageDelegator d = new StorageDelegator();
        OsmElementFactory factory = d.getFactory();
        Node n1 = factory.createNodeWithNewId(toE7(51.476), toE7(0.006));
        d.insertElementSafe(n1);
        Node n2 = factory.createNodeWithNewId(toE7(51.476), toE7(0.006));
        d.insertElementSafe(n2);
        MergeAction action = new MergeAction(d, n1, n2, null);
        List<Result> result = action.mergeNodes();
        assertFalse(result.isEmpty());
        assertFalse(result.get(0).hasIssue());
        assertNotNull(d.getOsmElement(Node.NAME, n1.getOsmId()));
        assertNull(d.getOsmElement(Node.NAME, n2.getOsmId()));
    }

    /**
     * Merge two nodes same as before args switched
     */
    @Test
    public void mergeNodes2() {
        StorageDelegator d = new StorageDelegator();
        OsmElementFactory factory = d.getFactory();
        Node n1 = factory.createNodeWithNewId(toE7(51.476), toE7(0.006));
        d.insertElementSafe(n1);
        Node n2 = factory.createNodeWithNewId(toE7(51.476), toE7(0.006));
        d.insertElementSafe(n2);
        MergeAction action = new MergeAction(d, n2, n1, null);
        List<Result> result = action.mergeNodes();
        assertFalse(result.isEmpty());
        assertFalse(result.get(0).hasIssue());
        assertNotNull(d.getOsmElement(Node.NAME, n2.getOsmId()));
        assertNull(d.getOsmElement(Node.NAME, n1.getOsmId()));
    }

    /**
     * Merge two nodes
     */
    @Test
    public void mergeNodes3() {
        StorageDelegator d = new StorageDelegator();
        OsmElementFactory factory = d.getFactory();
        Node n1 = factory.createNodeWithNewId(toE7(51.476), toE7(0.005));
        d.insertElementSafe(n1);
        Node n2 = factory.createNodeWithNewId(toE7(51.476), toE7(0.006));
        d.insertElementSafe(n2);
        n2.setOsmId(1234L);
        MergeAction action = new MergeAction(d, n1, n2, null);
        List<Result> result = action.mergeNodes();
        assertFalse(result.isEmpty());
        assertFalse(result.get(0).hasIssue());
        assertNull(d.getOsmElement(Node.NAME, n1.getOsmId()));
        assertNotNull(d.getOsmElement(Node.NAME, n2.getOsmId()));
        assertEquals(toE7(0.005), n2.lon);
    }

    /**
     * Merge two nodes
     */
    @Test
    public void mergeNodes3_1() {
        StorageDelegator d = new StorageDelegator();
        OsmElementFactory factory = d.getFactory();
        Node n1 = factory.createNodeWithNewId(toE7(51.476), toE7(0.005));
        d.insertElementSafe(n1);
        Node n2 = factory.createNodeWithNewId(toE7(51.476), toE7(0.006));
        d.insertElementSafe(n2);
        n2.setOsmId(1234L);
        MergeAction action = new MergeAction(d, n2, n1, null);
        List<Result> result = action.mergeNodes();
        assertFalse(result.isEmpty());
        assertFalse(result.get(0).hasIssue());
        assertNull(d.getOsmElement(Node.NAME, n1.getOsmId()));
        assertNotNull(d.getOsmElement(Node.NAME, n2.getOsmId()));
        assertEquals(toE7(0.006), n2.lon);
    }

    /**
     * Merge two nodes
     */
    @Test
    public void mergeNodes4() {
        StorageDelegator d = new StorageDelegator();
        OsmElementFactory factory = d.getFactory();
        Node n1 = factory.createNodeWithNewId(toE7(51.476), toE7(0.006));
        d.insertElementSafe(n1);
        MergeAction action = new MergeAction(d, n1, n1, null);
        List<Result> result = action.mergeNodes();
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).hasIssue());
        assertEquals(1, result.get(0).getIssues().size());
        assertTrue(result.get(0).getIssues().contains(MergeIssue.SAMEOBJECT));
    }

    /**
     * Merge two nodes
     */
    @Test
    public void mergeNodes5() {
        StorageDelegator d = new StorageDelegator();
        // create two ways with common node
        Way w = DelegatorUtil.addWayToStorage(d, false);
        Node n = w.getNodes().get(2);
        List<Result> splitResult = d.splitAtNode(w, n, true);
        assertNotNull(splitResult);
        assertFalse(splitResult.isEmpty());
        Way newWay = (Way) splitResult.get(0).getElement();
        d.unjoinWays(n);
        Node n1 = w.getLastNode();
        Node n2 = newWay.getFirstNode();
        assertNotEquals(n1, n2);
        MergeAction action = new MergeAction(d, n1, n2, false, null);
        List<Result> result = action.mergeNodes();
        assertFalse(result.isEmpty());
        assertFalse(result.get(0).hasIssue());
        assertTrue(w.hasNode(n1));
        assertTrue(newWay.hasNode(n1));
        assertFalse(newWay.hasNode(n2));
    }

    /**
     * Merge two nodes
     */
    @Test
    public void mergeNodes6() {
        // role conflict
        StorageDelegator d = new StorageDelegator();
        OsmElementFactory factory = d.getFactory();
        Node n1 = factory.createNodeWithNewId(toE7(51.476), toE7(0.006));
        d.insertElementSafe(n1);
        Node n2 = factory.createNodeWithNewId(toE7(51.476), toE7(0.006));
        d.insertElementSafe(n2);

        Relation r = factory.createRelationWithNewId();
        RelationMember member1 = new RelationMember("test1", n1);
        r.addMember(member1);
        d.insertElementSafe(r);
        n1.addParentRelation(r);
        RelationMember member2 = new RelationMember("test2", n2);
        r.addMember(member2);
        n2.addParentRelation(r);
        MergeAction action = new MergeAction(d, n1, n2, null);
        List<Result> result = action.mergeNodes();
        assertEquals(2, result.size());
        assertEquals(1, result.get(1).getIssues().size());
        assertTrue(result.get(1).getIssues().contains(MergeIssue.ROLECONFLICT));
    }

    /**
     * Way with doubled-backed end
     * 
     * Node numbering is relevant
     * 
     * See https://github.com/MarcusWolschon/osmeditor4android/issues/2907#issuecomment-3044160080
     */
    @Test
    public void mergeNodes7() {
        // StorageDelegator d = new StorageDelegator();
        // // create two ways with common node
        // Way w = DelegatorUtil.addWayToStorage(d, false);
        // List<Node> nodes = w.getNodes();
        // for (int i = 0; i < w.nodeCount(); i++) { // use proper ids!
        // nodes.get(i).setOsmId(i);
        // }
        // d.getCurrentStorage().rehash();

        StorageDelegator d = UnitTestUtils.loadTestData(getClass(), "node-merge-test.osm");
        Way w = (Way) d.getOsmElement(Way.NAME, 4307255231L);
        assertNotNull(w);
        assertEquals(4, w.nodeCount());
        List<Node> nodes = w.getNodes();
        Node lastNode = w.getLastNode();

        Node secondLastNode = nodes.get(w.nodeCount() - 2);

        // assertNotEquals(secondLastNode, lastNode);
        // w.addNode(secondLastNode);
        // d.insertElementSafe(w);
        // assertEquals(5, w.nodeCount());
        for (Node n : w.getNodes()) {
            System.out.println(n.getDescription(false));
        }

        MergeAction action = new MergeAction(d, lastNode, secondLastNode, true, null);
        List<Result> result = action.mergeNodes();
        for (Node n : w.getNodes()) {
            System.out.println(n.getDescription(false));
        }
        assertEquals(2, w.nodeCount());
        assertEquals(1, result.size());
        assertEquals(lastNode, w.getLastNode());
        assertNotEquals(lastNode, w.getNodes().get(w.nodeCount() - 2));
    }

    /**
     * Merge nodes that have tags, note that we should really do the same for ways
     */
    @Test
    public void mergeNodesWithTags1() {
        StorageDelegator d = new StorageDelegator();
        OsmElementFactory factory = d.getFactory();
        Node n1 = factory.createNodeWithNewId(toE7(51.476), toE7(0.006));
        d.insertElementSafe(n1);
        Node n2 = factory.createNodeWithNewId(toE7(51.476), toE7(0.006));
        d.insertElementSafe(n2);

        SortedMap<String, String> tags1 = new TreeMap<>();
        tags1.put(Tags.KEY_HIGHWAY, "a");
        n1.setTags(tags1);
        SortedMap<String, String> tags2 = new TreeMap<>();
        tags2.put(Tags.KEY_HIGHWAY, "b");
        n2.setTags(tags2);
        MergeAction action = new MergeAction(d, n1, n2, null);
        List<Result> result = action.mergeNodes();
        assertFalse(result.isEmpty());
        assertEquals(1, result.get(0).getIssues().size());
        assertTrue(result.get(0).getIssues().contains(MergeIssue.MERGEDTAGS));
    }

    /**
     * Merge nodes that have tags, note that we should really do the same for ways
     */
    @Test
    public void mergeNodesWithTags2() {
        StorageDelegator d = new StorageDelegator();
        OsmElementFactory factory = d.getFactory();
        Node n1 = factory.createNodeWithNewId(toE7(51.476), toE7(0.006));
        d.insertElementSafe(n1);
        Node n2 = factory.createNodeWithNewId(toE7(51.476), toE7(0.006));
        d.insertElementSafe(n2);

        SortedMap<String, String> tags1 = new TreeMap<>();
        tags1.put("test1", "a;b;c");
        n1.setTags(tags1);
        SortedMap<String, String> tags2 = new TreeMap<>();
        tags2.put("test2", "d;e;f");
        n2.setTags(tags2);
        MergeAction action = new MergeAction(d, n1, n2, null);
        List<Result> result = action.mergeNodes();
        assertNotNull(d.getOsmElement(Node.NAME, n1.getOsmId()));
        assertEquals("a;b;c", n1.getTagWithKey("test1"));
        assertEquals("d;e;f", n1.getTagWithKey("test2"));
        assertFalse(result.isEmpty());
        assertNull(result.get(0).getIssues());
    }

    /**
     * Merge nodes that have tags, note that we should really do the same for ways
     */
    @Test
    public void mergeNodesWithTags3() {
        StorageDelegator d = new StorageDelegator();
        OsmElementFactory factory = d.getFactory();
        Node n1 = factory.createNodeWithNewId(toE7(51.476), toE7(0.006));
        d.insertElementSafe(n1);
        Node n2 = factory.createNodeWithNewId(toE7(51.476), toE7(0.006));
        d.insertElementSafe(n2);

        SortedMap<String, String> tags1 = new TreeMap<>();
        tags1.put(Tags.KEY_HIGHWAY,
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        n1.setTags(tags1);
        SortedMap<String, String> tags2 = new TreeMap<>();
        tags2.put(Tags.KEY_HIGHWAY,
                "baaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        n2.setTags(tags2);
        MergeAction action = new MergeAction(d, n1, n2, null);
        try {
            List<Result> result = action.mergeNodes();
            fail("this should have failed");
        } catch (OsmIllegalOperationException e) {
            // expected
        }
    }

    /**
     * Merge nodes that have tags, note that we should really do the same for ways
     */
    @Test
    public void mergeNodesWithTags4() {
        StorageDelegator d = new StorageDelegator();
        OsmElementFactory factory = d.getFactory();
        Node n1 = factory.createNodeWithNewId(toE7(51.476), toE7(0.006));
        d.insertElementSafe(n1);
        Node n2 = factory.createNodeWithNewId(toE7(51.476), toE7(0.006));
        d.insertElementSafe(n2);

        SortedMap<String, String> tags1 = new TreeMap<>();
        tags1.put(Tags.KEY_HIGHWAY, "a;b;c");
        n1.setTags(tags1);
        SortedMap<String, String> tags2 = new TreeMap<>();
        tags2.put(Tags.KEY_HIGHWAY, "d;e;f");
        n2.setTags(tags2);
        MergeAction action = new MergeAction(d, n1, n2, null);
        List<Result> result = action.mergeNodes();
        assertNotNull(d.getOsmElement(Node.NAME, n1.getOsmId()));
        assertEquals("a;b;c;d;e;f", n1.getTagWithKey(Tags.KEY_HIGHWAY));
        assertFalse(result.isEmpty());
        assertEquals(1, result.get(0).getIssues().size());
        assertTrue(result.get(0).getIssues().contains(MergeIssue.MERGEDTAGS));
    }

    /**
     * Merge nodes that have tags, note that we should really do the same for ways
     */
    @Test
    public void mergeNodesWithTags5() {
        StorageDelegator d = new StorageDelegator();
        OsmElementFactory factory = d.getFactory();
        Node n1 = factory.createNodeWithNewId(toE7(51.476), toE7(0.006));
        d.insertElementSafe(n1);
        Node n2 = factory.createNodeWithNewId(toE7(51.476), toE7(0.006));
        d.insertElementSafe(n2);

        SortedMap<String, String> tags1 = new TreeMap<>();
        tags1.put("test" + Tags.KEY_CONDITIONAL_SUFFIX, "(a;b;c)");
        n1.setTags(tags1);
        SortedMap<String, String> tags2 = new TreeMap<>();
        tags2.put("test" + Tags.KEY_CONDITIONAL_SUFFIX, "(d;e;f)");
        n2.setTags(tags2);
        MergeAction action = new MergeAction(d, n1, n2, null);
        List<Result> result = action.mergeNodes();
        assertNotNull(d.getOsmElement(Node.NAME, n1.getOsmId()));
        assertEquals("(a;b;c);(d;e;f)", n1.getTagWithKey("test" + Tags.KEY_CONDITIONAL_SUFFIX));
        assertFalse(result.isEmpty());
        assertEquals(1, result.get(0).getIssues().size());
        assertTrue(result.get(0).getIssues().contains(MergeIssue.MERGEDTAGS));
    }

    /**
     * Replace a Node in Ways it is a member of
     */
    @Test
    public void replaceNodeInWays() {
        StorageDelegator d = new StorageDelegator();
        Way w = DelegatorUtil.addWayToStorage(d, false);
        Way temp = (Way) d.getOsmElement(Way.NAME, w.getOsmId());
        assertNotNull(temp);
        Node n = w.getNodes().get(2);
        d.splitAtNode(w, n, true);
        assertEquals(2, d.getCurrentStorage().getWays(n).size());
        Node newNode = d.replaceNode(n);
        assertEquals(2, d.getCurrentStorage().getWays(newNode).size());
        assertEquals(0, d.getCurrentStorage().getWays(n).size());
    }

    /**
     * Test removeWayNode method, by invoking removeNode on a node that is member of a way(s)
     */
    @Test
    public void removeWayNode() {
        StorageDelegator d = new StorageDelegator();
        Way w = DelegatorUtil.addWayToStorage(d, false);
        Way temp = (Way) d.getOsmElement(Way.NAME, w.getOsmId());
        assertNotNull(temp);
        Node n = w.getNodes().get(2);
        List<Result> splitResult = d.splitAtNode(w, n, true);
        assertNotNull(splitResult);
        assertFalse(splitResult.isEmpty());
        Way newWay = (Way) splitResult.get(0).getElement();
        assertEquals(n, w.getLastNode());
        assertEquals(n, newWay.getFirstNode());
        assertEquals(2, newWay.nodeCount());
        //
        d.removeNode(n);
        assertNotNull(d.getOsmElement(Way.NAME, w.getOsmId()));
        assertNull(d.getOsmElement(Way.NAME, newWay.getOsmId()));
        assertFalse(w.hasNode(n));

        // remove closing node of a closed way
        d = new StorageDelegator();
        w = DelegatorUtil.addWayToStorage(d, true);
        n = w.getFirstNode();
        d.removeNode(n);
        assertTrue(w.isClosed());
        assertFalse(w.hasNode(n));
    }

    /**
     * Test removeNodeFromWay closes Way is closing Node is removed
     */
    @Test
    public void removeNodeFromWay() {
        StorageDelegator d = new StorageDelegator();
        Way w = DelegatorUtil.addWayToStorage(d, true);
        Way temp = (Way) d.getOsmElement(Way.NAME, w.getOsmId());
        assertNotNull(temp);
        assertTrue(w.isClosed());
        Node lastNode = w.getLastNode();
        assertEquals(2, w.count(lastNode));
        d.removeNodeFromWay(w, lastNode);
        assertNotNull(d.getApiStorage().getWay(w.getOsmId()));
        assertFalse(w.hasNode(lastNode));
        assertTrue(w.isClosed());
    }

    /**
     * Test removeLastNodeFromWay method
     */
    @Test
    public void removeEndNodeFromWay() {
        StorageDelegator d = new StorageDelegator();
        Way w = DelegatorUtil.addWayToStorage(d, false);
        Way temp = (Way) d.getOsmElement(Way.NAME, w.getOsmId());
        assertNotNull(temp);
        Node tempNode = temp.getNodes().get(2);
        Map<String, String> tags = new TreeMap<>();
        tags.put("test", "test");
        tempNode.setTags(tags);

        int count = temp.nodeCount();
        assertEquals(4, count);
        final Storage apiStorage = d.getApiStorage();
        for (int i = 0; i < 2; i++) {
            Node n = temp.getLastNode();
            d.removeEndNodeFromWay(true, temp, !n.isTagged());
            assertFalse(temp.hasNode(n));
            assertEquals(count - (i + 1), temp.nodeCount());
            assertNotNull(apiStorage.getWay(temp.getOsmId()));
        }
        // removing the 2nd last node should delete the way
        Node n = temp.getLastNode();
        d.removeEndNodeFromWay(true, temp, !n.isTagged());
        assertEquals(OsmElement.STATE_DELETED, temp.getState());
        assertNull(apiStorage.getOsmElement(Way.NAME, temp.getOsmId()));

        // check that the tagged node is still here
        assertEquals(OsmElement.STATE_CREATED, tempNode.getState());
    }

    /**
     * As above but start at the beginning of the way
     */
    @Test
    public void removeEndNodeFromWay2() {
        StorageDelegator d = new StorageDelegator();
        Way w = DelegatorUtil.addWayToStorage(d, false);
        Way temp = (Way) d.getOsmElement(Way.NAME, w.getOsmId());
        assertNotNull(temp);
        Node tempNode = temp.getNodes().get(2);
        Map<String, String> tags = new TreeMap<>();
        tags.put("test", "test");
        tempNode.setTags(tags);

        int count = temp.nodeCount();
        assertEquals(4, count);
        final Storage apiStorage = d.getApiStorage();
        for (int i = 0; i < 2; i++) {
            Node n = temp.getFirstNode();
            d.removeEndNodeFromWay(false, temp, !n.isTagged());
            assertFalse(temp.hasNode(n));
            assertEquals(count - (i + 1), temp.nodeCount());
            assertNotNull(apiStorage.getWay(temp.getOsmId()));
        }
        // removing the 2nd last node should delete the way
        Node n = temp.getFirstNode();
        d.removeEndNodeFromWay(false, temp, !n.isTagged());
        assertEquals(OsmElement.STATE_DELETED, temp.getState());
        assertNull(apiStorage.getOsmElement(Way.NAME, temp.getOsmId()));

        // check that the tagged node is still here
        assertEquals(OsmElement.STATE_CREATED, tempNode.getState());
    }

    /**
     * Unjoin two ways
     */
    @Test
    public void unjoinWay() {
        StorageDelegator d = new StorageDelegator();
        Way w = DelegatorUtil.addWayToStorage(d, false);
        final Node n1 = w.getNodes().get(1);
        Way w2 = d.createAndInsertWay(n1);
        final Node n2 = w.getNodes().get(2);
        d.addNodeToWay(n2, w2);

        Relation r = d.getFactory().createRelationWithNewId();
        RelationMember member = new RelationMember("test", n2);
        r.addMember(member);
        d.insertElementSafe(r);
        n2.addParentRelation(r);

        assertEquals(2, d.getApiWayCount());
        assertEquals(w.nodeCount(), d.getApiNodeCount());
        d.unjoinWay(null, w2, null);
        assertNotEquals(n1, w2.getFirstNode());
        final Node lastNode = w2.getLastNode();
        assertNotEquals(n2, lastNode);
        assertEquals(2, d.getApiWayCount());
        assertEquals(w.nodeCount() + 2, d.getApiNodeCount());

        assertTrue(lastNode.hasParentRelation(r.getOsmId()));
    }

    /**
     * Unjoin two similar ways - nothing should happen - change tags unjoin should happen
     */
    @Test
    public void unjoinSimilarWay() {
        StorageDelegator d = new StorageDelegator();
        Way w = DelegatorUtil.addWayToStorage(d, false);
        final Node n1 = w.getNodes().get(1);
        Way w2 = d.createAndInsertWay(n1);
        final Node n2 = w.getNodes().get(2);
        d.addNodeToWay(n2, w2);

        assertEquals(2, d.getApiWayCount());
        assertEquals(w.nodeCount(), d.getApiNodeCount());
        Map<String, String> tags = new HashMap<>();
        tags.put(Tags.KEY_HIGHWAY, "service");
        w.setTags(tags);
        w2.setTags(tags);
        d.unjoinWay(null, w2, Tags.KEY_HIGHWAY);
        assertEquals(n1, w2.getFirstNode());
        final Node lastNode = w2.getLastNode();
        assertEquals(n2, lastNode);
        assertEquals(2, d.getApiWayCount());
        assertEquals(w.nodeCount(), d.getApiNodeCount());

        tags.clear();
        tags.put(Tags.KEY_WATERWAY, "wet");
        w2.setTags(tags);
        d.unjoinWay(null, w2, Tags.KEY_WATERWAY);
        assertNotEquals(n1, w2.getFirstNode());
        assertNotEquals(n2, w2.getLastNode());
        assertEquals(2, d.getApiWayCount());
        assertEquals(w.nodeCount() + 2, d.getApiNodeCount());
    }

    /**
     * Unjoin a closed way from another one
     */
    @Test
    public void unjoinClosedWays() {
        StorageDelegator d = new StorageDelegator();
        Way w = DelegatorUtil.addWayToStorage(d, false);
        final Node n1 = w.getNodes().get(1);
        Way w2 = d.createAndInsertWay(n1);
        final Node n2 = w.getNodes().get(2);
        d.addNodeToWay(n2, w2);
        final Node n3 = w.getNodes().get(3);
        d.addNodeToWay(n3, w2);
        d.addNodeToWay(n1, w2); // close
        assertEquals(2, d.getApiWayCount());
        assertEquals(w.nodeCount(), d.getApiNodeCount());
        assertTrue(w2.isClosed());

        d.unjoinWay(null, w2, null);
        assertNotEquals(n1, w2.getFirstNode());
        assertNotEquals(n2, w2.getNodes().get(1));
        assertNotEquals(n3, w2.getNodes().get(3));
        assertTrue(w2.isClosed());

        assertEquals(2, d.getApiWayCount());
        assertEquals(w.nodeCount() + 3, d.getApiNodeCount());
    }

    /**
     * Split way at node
     */
    @Test
    public void split() {
        StorageDelegator d = new StorageDelegator();
        Way w = DelegatorUtil.addWayToStorage(d, false);
        Way temp = (Way) d.getOsmElement(Way.NAME, w.getOsmId());
        assertNotNull(temp);
        Node n = w.getNodes().get(2);
        List<Result> splitResult = d.splitAtNode(w, n, true);
        assertNotNull(splitResult);
        assertFalse(splitResult.isEmpty());
        Way newWay = (Way) splitResult.get(0).getElement();
        assertEquals(n, w.getLastNode());
        assertEquals(n, newWay.getFirstNode());
        assertEquals(2, newWay.nodeCount());
    }

    /**
     * Split way at node, use earlier nodes in the way for the new way
     */
    @Test
    public void splitReverse() {
        StorageDelegator d = new StorageDelegator();
        Way w = DelegatorUtil.addWayToStorage(d, false);
        Way temp = (Way) d.getOsmElement(Way.NAME, w.getOsmId());
        assertNotNull(temp);
        Node n = w.getNodes().get(2);
        List<Result> splitResult = d.splitAtNode(w, n, false);
        assertNotNull(splitResult);
        assertFalse(splitResult.isEmpty());
        Way newWay = (Way) splitResult.get(0).getElement();
        assertEquals(n, w.getFirstNode());
        assertEquals(n, newWay.getLastNode());
        assertEquals(3, newWay.nodeCount());
    }

    /**
     * Split / merge way with metric tag
     */
    @Test
    public void splitMergeWithMetric() {
        StorageDelegator d = new StorageDelegator();
        Way w = DelegatorUtil.addWayToStorage(d, false);
        Way temp = (Way) d.getOsmElement(Way.NAME, w.getOsmId());
        assertNotNull(temp);
        Node n = w.getNodes().get(2);
        Map<String, String> tags = new TreeMap<>(w.getTags());
        tags.put(Tags.KEY_STEP_COUNT, "7");
        tags.put(Tags.KEY_DURATION, "15");
        w.setTags(tags);

        // split
        List<Result> splitResult = d.splitAtNode(w, n, true);
        assertNotNull(splitResult);
        assertFalse(splitResult.isEmpty());
        Result first = splitResult.get(0);
        assertNotNull(first.getIssues());
        assertEquals(1, first.getIssues().size());
        assertTrue(first.getIssues().contains(SplitIssue.SPLIT_METRIC));
        Way newWay = (Way) first.getElement();
        assertEquals("5", w.getTagWithKey(Tags.KEY_STEP_COUNT));
        assertEquals("2", newWay.getTagWithKey(Tags.KEY_STEP_COUNT));
        assertEquals("00:10:07", w.getTagWithKey(Tags.KEY_DURATION));
        assertEquals("00:04:53", newWay.getTagWithKey(Tags.KEY_DURATION));

        // merge
        MergeAction action = new MergeAction(d, newWay, w, null);
        List<Result> results = action.mergeWays();
        assertNotNull(results);
        assertEquals(1, results.size());
        assertNotNull(results.get(0).getIssues());
        assertTrue(results.get(0).getIssues().contains(MergeIssue.MERGEDMETRIC));
        w = (Way) results.get(0).getElement();
        assertEquals("7", w.getTagWithKey(Tags.KEY_STEP_COUNT));
        assertEquals("15", w.getTagWithKey(Tags.KEY_DURATION));
    }

    /**
     * Split closed way
     */
    @Test
    public void splitClosed1() {
        StorageDelegator d = new StorageDelegator();
        Way w = DelegatorUtil.addWayToStorage(d, true);
        assertTrue(w.isClosed());
        Way temp = (Way) d.getOsmElement(Way.NAME, w.getOsmId());
        assertNotNull(temp);
        Node n1 = w.getNodes().get(1);
        Node n2 = w.getNodes().get(2);
        List<Result> results = d.splitAtNodes(w, n1, n2, false);
        assertNotNull(results);
        assertEquals(2, results.size());
        Way newWay0 = (Way) results.get(0).getElement();
        assertNotNull(newWay0);
        assertTrue(newWay0.hasNode(n1));
        assertTrue(newWay0.hasNode(n2));
        Way newWay1 = (Way) results.get(1).getElement();
        assertNotNull(newWay1);
        assertTrue(newWay1.hasNode(n1));
        assertTrue(newWay1.hasNode(n2));
    }

    /**
     * Split closed way
     */
    @Test
    public void splitClosed2() {
        StorageDelegator d = new StorageDelegator();
        Way w = DelegatorUtil.addWayToStorage(d, true);
        assertTrue(w.isClosed());
        Way temp = (Way) d.getOsmElement(Way.NAME, w.getOsmId());
        assertNotNull(temp);
        Node n1 = w.getNodes().get(1);
        Node n2 = w.getNodes().get(3);
        List<Result> results = d.splitAtNodes(w, n1, n2, true);
        assertNotNull(results);
        assertEquals(2, results.size());
        Way newWay0 = (Way) results.get(0).getElement();
        assertNotNull(newWay0);
        Way newWay1 = (Way) results.get(1).getElement();
        assertNotNull(newWay1);
        assertTrue(newWay0.hasNode(n1));
        assertTrue(newWay0.hasNode(n2));
        assertTrue(newWay1.hasNode(n1));
        assertTrue(newWay1.hasNode(n2));
        assertTrue(newWay0.isClosed());
        assertTrue(newWay1.isClosed());
    }
    
    /**
     * Split way at node with incomplete route relation
     */
    @Test
    public void splitWithIncompleteRouteRelation() {
        StorageDelegator d = new StorageDelegator();
        Way w = DelegatorUtil.addWayToStorage(d, false);
        Way temp = (Way) d.getOsmElement(Way.NAME, w.getOsmId());
        assertNotNull(temp);
        Node n = w.getNodes().get(2);

        Relation r = w.getParentRelations().get(0);
        // add dummy members before and after the way we are splitting
        r.addMemberBefore(r.getMember(w), new RelationMember(Way.NAME, 12345, ""));
        r.addMemberAfter(r.getMember(w), new RelationMember(Way.NAME, 12346, ""));
        Map<String, String> routeTag = new HashMap<>();
        routeTag.put(Tags.KEY_TYPE, Tags.VALUE_ROUTE);
        r.setTags(routeTag);

        List<Result> splitResult = d.splitAtNode(w, n, true);
        assertNotNull(splitResult);
        assertEquals(2, splitResult.size());
        Result relationResult = splitResult.get(1);
        assertEquals(r, relationResult.getElement());
        assertTrue(relationResult.getIssues().contains(SplitIssue.SPLIT_ROUTE_ORDERING));
        Way newWay = (Way) splitResult.get(0).getElement();
        assertEquals(n, w.getLastNode());
        assertEquals(n, newWay.getFirstNode());
        assertEquals(2, newWay.nodeCount());
    }

    /**
     * Split closed way that is part of a not completely downloaded route
     */
    @Test
    public void splitClosedWithIncompleteRouteRelation() {
        StorageDelegator d = new StorageDelegator();
        Way w = DelegatorUtil.addWayToStorage(d, true);
        assertTrue(w.isClosed());

        Relation r = w.getParentRelations().get(0);
        // add dummy members before and after the way we are splitting
        r.addMemberBefore(r.getMember(w), new RelationMember(Way.NAME, 12345, ""));
        r.addMemberAfter(r.getMember(w), new RelationMember(Way.NAME, 12346, ""));
        Map<String, String> routeTag = new HashMap<>();
        routeTag.put(Tags.KEY_TYPE, Tags.VALUE_ROUTE);
        r.setTags(routeTag);
        
        Way temp = (Way) d.getOsmElement(Way.NAME, w.getOsmId());
        assertNotNull(temp);
        Node n1 = w.getNodes().get(1);
        Node n2 = w.getNodes().get(2);
        List<Result> results = d.splitAtNodes(w, n1, n2, false);
        assertNotNull(results);
        assertEquals(3, results.size());
        Result relationResult = results.get(2);
        assertEquals(r, relationResult.getElement());
        assertTrue(relationResult.getIssues().contains(SplitIssue.SPLIT_ROUTE_ORDERING));      
    }
    
    /**
     * Split way at node with route relation
     */
    @Test
    public void splitWithRouteRelation1() {
        StorageDelegator d = new StorageDelegator();
        Way w = DelegatorUtil.addWayToStorage(d, false);
        Way temp = (Way) d.getOsmElement(Way.NAME, w.getOsmId());
        assertNotNull(temp);
        Node n = w.getNodes().get(2);

        Relation r = w.getParentRelations().get(0);
        Map<String, String> routeTag = new HashMap<>();
        routeTag.put(Tags.KEY_TYPE, Tags.VALUE_ROUTE);
        r.setTags(routeTag);

        List<Result> splitResult = d.splitAtNode(w, n, true);
        assertNotNull(splitResult);
        assertEquals(1, splitResult.size());
    }

    /**
     * Split way at node with route relation
     */
    @Test
    public void splitWithRouteRelation2() {
        StorageDelegator d = new StorageDelegator();
        Way w = DelegatorUtil.addWayToStorage(d, false);
        Way temp = (Way) d.getOsmElement(Way.NAME, w.getOsmId());
        assertNotNull(temp);
        Node n = w.getNodes().get(2);

        Relation r = w.getParentRelations().get(0);
        Map<String, String> routeTag = new HashMap<>();
        routeTag.put(Tags.KEY_TYPE, Tags.VALUE_ROUTE);
        r.setTags(routeTag);

        List<Result> splitResult = d.splitAtNode(w, n, true);
        assertNotNull(splitResult);
        assertEquals(1, splitResult.size());

        // split again, this should not cause an error as we can determine where to put the split off part
        Way newWay = (Way) splitResult.get(0).getElement();
        if (newWay.nodeCount() > w.nodeCount()) {
            splitResult = d.splitAtNode(newWay, newWay.getNodes().get(1), true);
        } else {
            splitResult = d.splitAtNode(w, w.getNodes().get(1), true);
        }
        assertNotNull(splitResult);
        assertEquals(1, splitResult.size());
    }

    /**
     * Split way at node with route relation with low relation member limit
     */
    @Test
    public void splitWithRouteRelationLimit() {
        App.newLogic();
        Logic logic = App.getLogic();
        Preferences prefs = new Preferences(ApplicationProvider.getApplicationContext());
        Server server = prefs.getServer();
        DataStyle styles = App.getDataStyle(ApplicationProvider.getApplicationContext());
        styles.getStylesFromFiles(ApplicationProvider.getApplicationContext());
        try {
            server.getCachedCapabilities().setMaxRelationMembers(1);
            logic.setPrefs(prefs);
            StorageDelegator d = new StorageDelegator();
            Way w = DelegatorUtil.addWayToStorage(d, false);
            Way temp = (Way) d.getOsmElement(Way.NAME, w.getOsmId());
            assertNotNull(temp);
            Node n = w.getNodes().get(2);

            Relation r = w.getParentRelations().get(0);
            Map<String, String> routeTag = new HashMap<>();
            routeTag.put(Tags.KEY_TYPE, Tags.VALUE_ROUTE);
            r.setTags(routeTag);

            try {
                List<Result> splitResult = d.splitAtNode(w, n, true);
                fail("should have errored");
            } catch (OsmIllegalOperationException ex) {
                // expected
                assertEquals(PreconditionIssue.RELATION_MEMBER_COUNT, ex.getIssue());
            }
        } finally {
            server.getCachedCapabilities().setMaxRelationMembers(32000);
        }
    }

    /**
     * Split way at node with restriction relation
     */
    @Test
    public void splitWithRestrictionRelation() {
        StorageDelegator d = new StorageDelegator();
        Way w = DelegatorUtil.addWayToStorage(d, false);
        Way temp = (Way) d.getOsmElement(Way.NAME, w.getOsmId());
        assertNotNull(temp);
        Node n = w.getNodes().get(2);

        Relation r = w.getParentRelations().get(0);

        List<Result> splitResult = d.splitAtNode(w, n, true);
        assertNotNull(splitResult);
        assertEquals(1, splitResult.size());
        Way newWay = (Way) splitResult.get(0).getElement();

        Map<String, String> routeTag = new HashMap<>();
        routeTag.put(Tags.KEY_TYPE, Tags.VALUE_RESTRICTION);
        r.setTags(routeTag);
        RelationMember from = r.getMember(w);
        from.setRole(Tags.ROLE_FROM);
        RelationMember to = r.getMember(newWay);
        to.setRole(Tags.ROLE_TO);
        r.addMember(new RelationMember(Tags.ROLE_VIA, n));

        // split from
        splitResult = d.splitAtNode(w, w.getNodes().get(1), true);
        assertNotNull(splitResult);
        assertEquals(1, splitResult.size());

        List<RelationMember> froms = r.getMembersWithRole(Tags.ROLE_FROM);
        assertEquals(1, froms.size());
        Way fromWay = (Way) froms.get(0).getElement();
        assertTrue(fromWay.getLastNode().equals(n) || fromWay.getFirstNode().equals(n));
        assertTrue(fromWay.hasCommonNode(newWay));
    }

    /**
     * Split a lollipop with loop at the end of the way
     */
    @Test
    public void splitLollipop1() {
        StorageDelegator d = UnitTestUtils.loadTestData(getClass(), "lollipop.osm");
        Way lollipop = (Way) d.getOsmElement(Way.NAME, -1L);
        Node junction = (Node) d.getOsmElement(Node.NAME, -2L);
        List<Result> splitResult = d.splitAtNode(lollipop, junction, true);
        assertNotNull(splitResult);
        assertEquals(1, splitResult.size());
        Way closedBit = (Way) splitResult.get(0).getElement();
        assertTrue(closedBit.isClosed());
        assertEquals(2, Collections.frequency(closedBit.getNodes(), junction));
        assertFalse(lollipop.isClosed());
        assertEquals(1, Collections.frequency(lollipop.getNodes(), junction));
    }

    /**
     * Split a lollipop with loop at the end of the way
     */
    @Test
    public void splitLollipop2() {
        StorageDelegator d = UnitTestUtils.loadTestData(getClass(), "lollipop.osm");
        Way lollipop = (Way) d.getOsmElement(Way.NAME, -1L);
        Node junction = (Node) d.getOsmElement(Node.NAME, -2L);
        List<Result> splitResult = d.splitAtNode(lollipop, junction, false);
        assertNotNull(splitResult);
        assertEquals(1, splitResult.size());
        Way stick = (Way) splitResult.get(0).getElement();
        assertFalse(stick.isClosed());
        assertEquals(1, Collections.frequency(stick.getNodes(), junction));
        assertTrue(lollipop.isClosed());
        assertEquals(2, Collections.frequency(lollipop.getNodes(), junction));
    }

    /**
     * Split a lollipop with loop at the beginning of the way
     */
    @Test
    public void splitLollipop3() {
        StorageDelegator d = UnitTestUtils.loadTestData(getClass(), "lollipop.osm");
        Way lollipop = (Way) d.getOsmElement(Way.NAME, -2L);
        Node junction = (Node) d.getOsmElement(Node.NAME, -6L);
        List<Result> splitResult = d.splitAtNode(lollipop, junction, true);
        assertNotNull(splitResult);
        assertEquals(1, splitResult.size());
        Way stick = (Way) splitResult.get(0).getElement();
        assertFalse(stick.isClosed());
        assertEquals(1, Collections.frequency(stick.getNodes(), junction));
        assertTrue(lollipop.isClosed());
        assertEquals(2, Collections.frequency(lollipop.getNodes(), junction));
    }

    /**
     * Split a lollipop with loop at the beginning of the way
     */
    @Test
    public void splitLollipop4() {
        StorageDelegator d = UnitTestUtils.loadTestData(getClass(), "lollipop.osm");
        Way lollipop = (Way) d.getOsmElement(Way.NAME, -2L);
        Node junction = (Node) d.getOsmElement(Node.NAME, -6L);
        List<Result> splitResult = d.splitAtNode(lollipop, junction, false);
        assertNotNull(splitResult);
        assertEquals(1, splitResult.size());
        Way closedBit = (Way) splitResult.get(0).getElement();
        assertTrue(closedBit.isClosed());
        assertEquals(2, Collections.frequency(closedBit.getNodes(), junction));
        assertFalse(lollipop.isClosed());
        assertEquals(1, Collections.frequency(lollipop.getNodes(), junction));
    }

    /**
     * Add and remove some members to a Relation
     */
    @Test
    public void relationMembers() {
        StorageDelegator d = new StorageDelegator();
        OsmElementFactory factory = d.getFactory();
        Node n1 = factory.createNodeWithNewId(toE7(51.476), toE7(0.006));
        d.insertElementSafe(n1);
        Node n2 = factory.createNodeWithNewId(toE7(51.476), toE7(0.006));
        d.insertElementSafe(n2);

        Relation r = factory.createRelationWithNewId();
        RelationMember member1 = new RelationMember("test1", n1);
        RelationMember member2 = new RelationMember("test2", n2);
        RelationMember member3 = new RelationMember(Way.NAME, 1234567L, "test3");

        List<RelationMember> addMembers = new ArrayList<>();
        addMembers.add(member1);
        addMembers.add(member2);
        addMembers.add(member3);
        d.addRelationMembersToRelation(r, addMembers);

        assertTrue(d.getApiStorage().contains(r));
        assertEquals(member1, r.getMember(n1));
        assertEquals(member2, r.getMember(n2));
        assertEquals(member3, r.getMember(Way.NAME, 1234567L));

        List<RelationMember> removeMembers = new ArrayList<>();
        removeMembers.add(member2);
        removeMembers.add(member3);
        d.removeRelationMembersFromRelation(r, removeMembers);
        assertEquals(member1, r.getMember(n1));
        assertNull(r.getMember(n2));
        assertNull(r.getMember(Way.NAME, 1234567L));
    }

    /**
     * Bounding box related stuff
     */
    @Test
    public void boundingBoxes() {
        BoundingBox box = new BoundingBox(9.51947D, 47.13638D, 9.52300D, 47.14066D);
        StorageDelegator d = new StorageDelegator();
        d.addBoundingBox(box);
        List<BoundingBox> boxes = d.getBoundingBoxes();
        assertEquals(1, boxes.size());
        assertEquals(box, boxes.get(0));
        d.mergeBoundingBox(box);
        assertEquals(1, boxes.size());
        assertEquals(box, boxes.get(0));
        // a smaller bounding box shouldn't be added
        BoundingBox smallerBox = new BoundingBox(9.51950D, 47.13650D, 9.52250D, 47.140D);
        d.mergeBoundingBox(smallerBox);
        assertEquals(1, boxes.size());
        assertEquals(box, boxes.get(0));
        d.deleteBoundingBox(box);
        assertEquals(0, boxes.size());
        // smaller bounding box should be removed
        d.addBoundingBox(smallerBox);
        assertEquals(1, boxes.size());
        assertEquals(smallerBox, boxes.get(0));
        d.mergeBoundingBox(box);
        assertEquals(1, boxes.size());
        assertEquals(box, boxes.get(0));
    }

    /**
     * Add a member to a relation with low relation member limit
     */
    @Test
    public void addMemberToRelationLimit() {
        App.newLogic();
        Logic logic = App.getLogic();
        Preferences prefs = new Preferences(ApplicationProvider.getApplicationContext());
        Server server = prefs.getServer();
        DataStyle styles = App.getDataStyle(ApplicationProvider.getApplicationContext());
        styles.getStylesFromFiles(ApplicationProvider.getApplicationContext());
        try {
            server.getCachedCapabilities().setMaxRelationMembers(1);
            logic.setPrefs(prefs);
            StorageDelegator d = new StorageDelegator();
            Way w = DelegatorUtil.addWayToStorage(d, false);

            Relation r = w.getParentRelations().get(0);

            try { // simply add w again to exceed the limit
                d.addMemberToRelation(new RelationMember("", w), r);
                fail("should have errored");
            } catch (OsmIllegalOperationException ex) {
                // expected
                assertEquals(PreconditionIssue.RELATION_MEMBER_COUNT, ex.getIssue());
            }
        } finally {
            server.getCachedCapabilities().setMaxRelationMembers(32000);
        }
    }

    /**
     * Create a circle
     */
    @Test
    public void createCircle() {
        Logic logic = App.newLogic();
        logic.setMap(new de.blau.android.Map(ApplicationProvider.getApplicationContext()), true);
        StorageDelegator d = new StorageDelegator();
        OsmElementFactory factory = d.getFactory();
        Node n1 = factory.createNodeWithNewId(toE7(48.7779677), toE7(9.1812482));
        d.insertElementSafe(n1);
        List<Node> nodes = new ArrayList<>();
        nodes.add(n1);
        Node n2 = factory.createNodeWithNewId(toE7(48.7779849), toE7(9.1815418));
        d.insertElementSafe(n2);
        nodes.add(n2);
        Node n3 = factory.createNodeWithNewId(toE7(48.7778059), toE7(9.1812985));
        d.insertElementSafe(n3);
        nodes.add(n3);
        Way circle = d.createCircle(logic.getMap(), 6, 2.0, 0.5, nodes);
        assertTrue(circle.isClosed());
        assertEquals(n1, circle.getFirstNode());
        assertEquals(44, circle.nodeCount());
    }

    /**
     * Arrange way nodes in a circle
     */
    @Test
    public void arrangeInCircle() {
        Logic logic = App.newLogic();
        logic.setMap(new de.blau.android.Map(ApplicationProvider.getApplicationContext()), true);
        StorageDelegator d = new StorageDelegator();
        OsmElementFactory factory = d.getFactory();
        Node n1 = factory.createNodeWithNewId(toE7(48.7779677), toE7(9.1812482));
        d.insertElementSafe(n1);
        List<Node> nodes = new ArrayList<>();
        nodes.add(n1);
        Node n2 = factory.createNodeWithNewId(toE7(48.7779849), toE7(9.1815418));
        d.insertElementSafe(n2);
        nodes.add(n2);
        Node n3 = factory.createNodeWithNewId(toE7(48.7778059), toE7(9.1812985));
        d.insertElementSafe(n3);
        nodes.add(n3);
        Way circle = factory.createWayWithNewId();
        d.insertElementSafe(circle);
        circle.addNodes(nodes, false);
        d.circulizeWay(logic.getMap(), 6, 2.0, 0.5, circle);
        assertTrue(circle.isClosed());
        assertEquals(n1, circle.getFirstNode());
        assertEquals(44, circle.nodeCount());
    }

    /**
     * Replace a node relation memeber with a way
     */
    @Test
    public void replaceRelationMemberElement() {
        StorageDelegator d = UnitTestUtils.loadTestData(getClass(), "replace_geometry4.osm");
        Relation r = (Relation) d.getOsmElement(Relation.NAME, -3L);
        Way w = (Way) d.getOsmElement(Way.NAME, -1L);
        Node n = (Node) d.getOsmElement(Node.NAME, -14L);
        List<Relation> parents = n.getParentRelations();
        assertNotNull(parents);
        assertEquals(1, parents.size());
        assertEquals(r, parents.get(0));
        List<RelationMember> members = r.getMembers();
        assertNotNull(members);
        assertEquals(1, members.size());
        assertEquals(n, members.get(0).getElement());
        d.replaceRelationMemberElement(r, n, w);
        parents = n.getParentRelations();
        assertTrue(parents == null || parents.isEmpty());
        parents = w.getParentRelations();
        assertNotNull(parents);
        assertEquals(1, parents.size());
        assertEquals(r, parents.get(0));
        members = r.getMembers();
        assertNotNull(members);
        assertEquals(1, members.size());
        assertEquals(w, members.get(0).getElement());
    }

    @Test
    public void updateDataFromChangesTest1() {
        updateDataFromChanges("update-test-data.osm", "/update-test-changes.xml", 801);
    }

    @Test
    public void updateDataFromChangesTest2() {
        updateDataFromChanges("update-test-data-2.osm", "/update-test-changes-2.xml", 12);
    }

    @Test
    public void updateDataFromChangesTest3() {
        updateDataFromChanges("update-test-data-3.osm", "/update-test-changes-3.xml", 16);
    }

    /**
     * Update data from osmChanges file
     * 
     * @param dataFile the complete data with unsaved changes
     * @param changesFile the changeset osmChanges file
     * @param count number of changes
     * 
     */
    public void updateDataFromChanges(String dataFile, String changesFile, int count) {
        StorageDelegator d = UnitTestUtils.loadTestData(getClass(), dataFile);
        try (InputStream input = getClass().getResourceAsStream(changesFile)) {
            OsmChangeParser oscParser = new OsmChangeParser();
            oscParser.clearBoundingBoxes(); // this removes the default bounding box
            oscParser.start(input);
            assertEquals(count, d.getApiElementCount());
            assertTrue(UpdateFromChanges.update(d, oscParser.getStorage()));
            assertEquals(0, d.getApiElementCount());
        } catch (IOException | SAXException | ParserConfigurationException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Newly created way node upload should force upload of way
     */
    @Test
    public void uploadNewWayNodeTest() {
        StorageDelegator sd = UnitTestUtils.loadTestData(getClass(), "selective-upload1.osm");
        Node newNode = (Node) sd.getOsmElement(Node.NAME, -1L);
        assertNotNull(newNode);
        List<OsmElement> toUpload = new ArrayList<>();
        toUpload.add(newNode);
        sd.addRequiredElements(ApplicationProvider.getApplicationContext(), toUpload);
        assertTrue(toUpload.contains(newNode));
        assertTrue(toUpload.contains(sd.getOsmElement(Way.NAME, 28075087L)));
    }
    
    /**
     * Newly created way node upload should force upload of way, check that way isn't duplicated if already present
     */
    @Test
    public void uploadNewWayNodeTest2() {
        StorageDelegator sd = UnitTestUtils.loadTestData(getClass(), "selective-upload1.osm");
        Node newNode = (Node) sd.getOsmElement(Node.NAME, -1L);
        assertNotNull(newNode);
        List<OsmElement> toUpload = new ArrayList<>();
        toUpload.add(newNode);
        toUpload.add(sd.getOsmElement(Way.NAME, 28075087L));
        sd.addRequiredElements(ApplicationProvider.getApplicationContext(), toUpload);
        assertEquals(2, toUpload.size());
        assertTrue(toUpload.contains(newNode));
        assertTrue(toUpload.contains(sd.getOsmElement(Way.NAME, 28075087L)));
    }

    /**
     * Upload of modified way needs to include newly created node
     */
    @Test
    public void uploadModifiedWayTest() {
        StorageDelegator sd = UnitTestUtils.loadTestData(getClass(), "selective-upload1.osm");
        Way modifiedWay = (Way) sd.getOsmElement(Way.NAME, 28075087L);
        assertNotNull(modifiedWay);
        List<OsmElement> toUpload = new ArrayList<>();
        toUpload.add(modifiedWay);
        sd.addRequiredElements(ApplicationProvider.getApplicationContext(), toUpload);
        assertTrue(toUpload.contains(modifiedWay));
        assertTrue(toUpload.contains(sd.getOsmElement(Node.NAME, -1L)));
    }

    /**
     * Upload of modified relation needs to include newly created way
     */
    @Test
    public void uploadModifiedRelationTest() {
        StorageDelegator sd = UnitTestUtils.loadTestData(getClass(), "selective-upload2.osm");
        Relation modifiedRelation = (Relation) sd.getOsmElement(Relation.NAME, 29169L);
        assertNotNull(modifiedRelation);
        List<OsmElement> toUpload = new ArrayList<>();
        toUpload.add(modifiedRelation);
        sd.addRequiredElements(ApplicationProvider.getApplicationContext(), toUpload);
        assertTrue(toUpload.contains(modifiedRelation));
        assertTrue(toUpload.contains(sd.getOsmElement(Way.NAME, -1L)));
        assertTrue(toUpload.contains(sd.getOsmElement(Node.NAME, -2L)));
        assertTrue(toUpload.contains(sd.getOsmElement(Node.NAME, -3L)));
    }
    
    /**
     * As above but check that we protect against relation loops
     */
    @Test
    public void uploadModifiedRelationTest2() {
        StorageDelegator sd = UnitTestUtils.loadTestData(getClass(), "selective-upload2.osm");
        Relation modifiedRelation = (Relation) sd.getOsmElement(Relation.NAME, 29169L);
        assertNotNull(modifiedRelation);
        Relation loop = sd.createAndInsertRelation(Util.wrapInList(modifiedRelation));
        loop.addMember(new RelationMember("", loop));
        modifiedRelation.addMember(new RelationMember("", loop));
        
        List<OsmElement> toUpload = new ArrayList<>();
        toUpload.add(modifiedRelation);
        sd.addRequiredElements(ApplicationProvider.getApplicationContext(), toUpload);
        assertTrue(toUpload.contains(modifiedRelation));
        assertTrue(toUpload.contains(loop));
        assertTrue(toUpload.contains(sd.getOsmElement(Way.NAME, -1L)));
        assertTrue(toUpload.contains(sd.getOsmElement(Node.NAME, -2L)));
        assertTrue(toUpload.contains(sd.getOsmElement(Node.NAME, -3L)));
    }

    /**
     * Reverse a way
     */
    @Test
    public void reverseWay1() {
        StorageDelegator d = new StorageDelegator();
        Way w = DelegatorUtil.addWayToStorage(d, false);
        Node firstNode = w.getFirstNode();
        Node lastNode = w.getLastNode();
        SortedMap<String, String> tags = new TreeMap<>(w.getTags());
        tags.put(Tags.KEY_HIGHWAY, "residential");
        tags.put(Tags.KEY_ONEWAY, Tags.VALUE_YES);
        w.setTags(tags);
        List<Result> result = d.reverseWay(w);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getIssues().size());
        assertEquals(ReverseIssue.ONEWAY_DIRECTION_REVERSED, new ArrayList<Issue>(result.get(0).getIssues()).get(0));
        assertTrue(w.hasTag(Tags.KEY_ONEWAY, Tags.VALUE_YES)); // shouldn't change
        assertEquals(lastNode, w.getFirstNode());
        assertEquals(firstNode, w.getLastNode());
    }
    
    /**
     * Reverse a way
     */
    @Test
    public void reverseWay2() {
        StorageDelegator d = new StorageDelegator();
        Way w = DelegatorUtil.addWayToStorage(d, false);
        Node firstNode = w.getFirstNode();
        Node lastNode = w.getLastNode();
        SortedMap<String, String> tags = new TreeMap<>(w.getTags());
        tags.put(Tags.KEY_HIGHWAY, "residential");
        tags.put(Tags.KEY_SIDEWALK + ":left", Tags.VALUE_YES);
        w.setTags(tags);
        List<Result> result = d.reverseWay(w);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getIssues().size());
        assertEquals(ReverseIssue.TAGS_REVERSED, new ArrayList<Issue>(result.get(0).getIssues()).get(0));
        w.hasTag(Tags.KEY_SIDEWALK + ":right", Tags.VALUE_YES);
        assertEquals(lastNode, w.getFirstNode());
        assertEquals(firstNode, w.getLastNode());
    }
    
    /**
     * Reverse a way
     */
    @Test
    public void reverseWay3() {
        StorageDelegator d = new StorageDelegator();
        Way w = DelegatorUtil.addWayToStorage(d, false);
        Node firstNode = w.getFirstNode();
        Node lastNode = w.getLastNode();
        SortedMap<String, String> tags = new TreeMap<>(w.getTags());
        tags.put(Tags.KEY_HIGHWAY, "residential");
        tags.put(Tags.KEY_ONEWAY, Tags.VALUE_NO);
        w.setTags(tags);
        List<Result> result = d.reverseWay(w);
        assertEquals(0, result.size());
        assertEquals(lastNode, w.getFirstNode());
        assertEquals(firstNode, w.getLastNode());
    }
}