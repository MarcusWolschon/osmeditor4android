package io.vespucci.osm;

import static io.vespucci.osm.DelegatorUtil.addWayToStorage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import io.vespucci.App;
import io.vespucci.osm.BoundingBox;
import io.vespucci.osm.Relation;
import io.vespucci.osm.StorageDelegator;
import io.vespucci.osm.UndoStorage;
import io.vespucci.osm.Way;
import io.vespucci.osm.UndoStorage.UndoElement;
import io.vespucci.osm.UndoStorage.UndoRelation;
import io.vespucci.osm.UndoStorage.UndoWay;
import io.vespucci.util.Util;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
@LargeTest
public class UndoStorageTest {

    /**
     * Pre test setup
     */
    @Before
    public void setup() {
        App.getDelegator().reset(true);
    }

    /**
     * Create a way - undo - redo
     */
    @Test
    public void createdWay() {
        StorageDelegator d = App.getDelegator();
        UndoStorage undo = d.getUndo();
        assertFalse(undo.canUndo());
        assertFalse(undo.canRedo());
        Way w = addWayToStorage(d, false);
        assertEquals(1, d.getCurrentStorage().getWayCount());
        assertEquals(1, d.getApiStorage().getWayCount());
        assertEquals(4, w.nodeCount());
        assertTrue(undo.canUndo());
        UndoElement ue = undo.getOriginal(w);
        assertTrue(ue instanceof UndoWay);
        assertEquals(0, ((UndoWay) ue).nodeCount());

        String[] undoActions = undo.getUndoActions(ApplicationProvider.getApplicationContext());
        assertEquals(1, undoActions.length);
        String[] redoActions = undo.getRedoActions(ApplicationProvider.getApplicationContext());
        assertEquals(0, redoActions.length);
        // undo
        assertNotNull(undo.undo(true));
        assertTrue(undo.canRedo());
        assertEquals(0, d.getCurrentStorage().getWayCount());
        assertEquals(0, d.getApiStorage().getWayCount());
        List<UndoElement> redoElements = undo.getRedoElements(w);
        assertNotNull(redoElements);
        assertEquals(1, redoElements.size());
        ue = redoElements.get(0);
        assertTrue(ue instanceof UndoWay);
        assertFalse(((UndoWay) ue).isClosed());
        assertEquals(4, ((UndoWay) ue).nodeCount());
        assertEquals(637.56, ((UndoWay) ue).length(), 0.01);
        assertEquals(new BoundingBox(0.0000000, 51.4760000, 0.0030000, 51.4780000), undo.getLastRedoBounds());
        // redo
        assertNotNull(undo.redo());
        assertTrue(undo.canUndo());
        assertFalse(undo.canRedo());
        assertEquals(1, d.getCurrentStorage().getWayCount());
        assertEquals(1, d.getApiStorage().getWayCount());
    }

    /**
     * Create a relation - undo - redo
     */
    @Test
    public void createdRelation() {
        StorageDelegator d = App.getDelegator(); // undoing will want this delegator instance
        UndoStorage undo = d.getUndo();
        assertFalse(undo.canUndo());
        assertFalse(undo.canRedo());
        Way w = addWayToStorage(d, false); // w is already a member here
        undo.createCheckpoint("add test relation", null);
        Relation r = d.createAndInsertRelation(Util.wrapInList(w));
        assertEquals(1, d.getCurrentStorage().getWayCount());
        assertEquals(1, d.getApiStorage().getWayCount());
        assertEquals(2, d.getCurrentStorage().getRelationCount());
        assertEquals(2, d.getApiStorage().getRelationCount());
        assertEquals(2, w.getParentRelations().size());
        assertTrue(w.getParentRelations().contains(r));
        assertTrue(undo.canUndo());
        UndoElement ue = undo.getOriginal(r);
        assertTrue(ue instanceof UndoRelation);

        String[] undoActions = undo.getUndoActions(ApplicationProvider.getApplicationContext());
        assertEquals(2, undoActions.length);
        String[] redoActions = undo.getRedoActions(ApplicationProvider.getApplicationContext());
        assertEquals(0, redoActions.length);
        // undo
        assertNotNull(undo.undo(true));
        assertTrue(undo.canRedo());
        assertEquals(1, d.getCurrentStorage().getWayCount());
        assertEquals(1, d.getApiStorage().getWayCount());
        assertEquals(1, d.getCurrentStorage().getRelationCount());
        assertEquals(1, d.getApiStorage().getRelationCount());
        assertEquals(1, w.getParentRelations().size());
        assertFalse(w.getParentRelations().contains(r));
        // redo
        assertNotNull(undo.redo());
        assertTrue(undo.canUndo());
        assertFalse(undo.canRedo());
        assertEquals(1, d.getCurrentStorage().getWayCount());
        assertEquals(1, d.getApiStorage().getWayCount());
        assertEquals(2, d.getCurrentStorage().getRelationCount());
        assertEquals(2, d.getApiStorage().getRelationCount());
        assertEquals(2, w.getParentRelations().size());
        assertTrue(w.getParentRelations().contains(r));
    }
}
