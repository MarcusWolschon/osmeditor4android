package de.blau.android.osm;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.HttpURLConnection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.test.filters.LargeTest;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
@LargeTest
public class ApiResponseTest {

    private static final String ERROR_MESSAGE_VERSION_CONFLICT_NODE     = "Version mismatch: Provided 1, server had: 3 of Node 12345";
    private static final String ERROR_MESSAGE_VERSION_CONFLICT_WAY      = "Version mismatch: Provided 1, server had: 3 of Way 12345";
    private static final String ERROR_MESSAGE_VERSION_CONFLICT_RELATION = "Version mismatch: Provided 1, server had: 3 of Relation 12345";

    private static final String ERROR_MESSAGE_ALREADY_DELETED_NODE     = "The node with the id 12345 has already been deleted";
    private static final String ERROR_MESSAGE_ALREADY_DELETED_WAY      = "The way with the id 12345 has already been deleted";
    private static final String ERROR_MESSAGE_ALREADY_DELETED_RELATION = "The relation with the id 12345 has already been deleted";

    private static final String ERROR_MESSAGE_PRECONDITION_STILL_USED_NODE_WAY       = "Precondition failed: Node 12345 is still used by way 56789.";
    private static final String ERROR_MESSAGE_PRECONDITION_STILL_USED_NODE_WAYS      = "Precondition failed: Node 12345 is still used by ways 56789,54321.";
    private static final String ERROR_MESSAGE_PRECONDITION_STILL_USED_NODE_RELATION  = "Precondition failed: Node 12345 is still used by relation 56789";
    private static final String ERROR_MESSAGE_PRECONDITION_STILL_USED_NODE_RELATIONS = "Precondition failed: Node 12345 is still used by relations 56789,54321";
    private static final String ERROR_MESSAGE_PRECONDITION_STILL_USED_WAY_RELATION   = "Precondition failed: Way 12345 is still used by relation 56789";
    private static final String ERROR_MESSAGE_PRECONDITION_STILL_USED_WAY_RELATIONS  = "Precondition failed: Way 12345 is still used by relations 56789,54321";

    private static final String ERROR_MESSAGE_PRECONDITION_REQUIRED_WAY_NODE_NEG  = "Precondition failed: Way -2 requires the nodes with id in 56789 which either do not exist, or are not visible.";
    private static final String ERROR_MESSAGE_PRECONDITION_REQUIRED_WAY_NODES_NEG = "Precondition failed: Way -2 requires the nodes with id in 56789,54321 which either do not exist, or are not visible.";
    private static final String ERROR_MESSAGE_PRECONDITION_REQUIRED_WAY_NODE      = "Precondition failed: Way 12345 requires the nodes with id in 56789 which either do not exist, or are not visible.";
    private static final String ERROR_MESSAGE_PRECONDITION_REQUIRED_WAY_NODES     = "Precondition failed: Way 12345 requires the nodes with id in 56789,54321 which either do not exist, or are not visible.";

    private static final String ERROR_MESSAGE_PRECONDITION_REQUIRED_ONE_WAY_NODE = "Precondition failed: Way -522 must have at least one node";

    private static final String ERROR_MESSAGE_PRECONDITION_REQUIRED_RELATION_MEMBERS_NODE_NEG  = "Precondition failed: Relation -2 requires the nodes with id in 56789 which either do not exist, or are not visible.";
    private static final String ERROR_MESSAGE_PRECONDITION_REQUIRED_RELATION_MEMBERS_NODES_NEG = "Precondition failed: Relation -2 requires the nodes with id in 56789,54321 which either do not exist, or are not visible.";
    private static final String ERROR_MESSAGE_PRECONDITION_REQUIRED_RELATION_MEMBERS_NODE      = "Precondition failed: Relation 12345 requires the nodes with id in 56789 which either do not exist, or are not visible.";
    private static final String ERROR_MESSAGE_PRECONDITION_REQUIRED_RELATION_MEMBERS_NODES     = "Precondition failed: Relation 12345 requires the nodes with id in 56789,54321 which either do not exist, or are not visible.";
    private static final String ERROR_MESSAGE_PRECONDITION_REQUIRED_RELATION_MEMBERS_WAY       = "Precondition failed: Relation 12345 requires the ways with id in 56789 which either do not exist, or are not visible.";
    private static final String ERROR_MESSAGE_PRECONDITION_REQUIRED_RELATION_MEMBERS_WAYS      = "Precondition failed: Relation 12345 requires the ways with id in 56789,54321 which either do not exist, or are not visible.";
    private static final String ERROR_MESSAGE_PRECONDITION_REQUIRED_RELATION_MEMBERS_RELATION  = "Precondition failed: Relation 12345 requires the relations with id in 56789 which either do not exist, or are not visible.";
    private static final String ERROR_MESSAGE_PRECONDITION_REQUIRED_RELATION_MEMBERS_RELATIONS = "Precondition failed: Relation 12345 requires the relations with id in 56789,54321 which either do not exist, or are not visible.";

    private static final String ERROR_MESSAGE_PRECONDITION_RELATION_RELATION        = "Precondition failed: The relation 12345 is used in relation 6789.";
    private static final String ERROR_MESSAGE_PRECONDITION_RELATION_RELATION_CGIMAP = "Precondition failed: The relation 12345 is used in relations 6789.";

    private static final String ERROR_MESSAGE_CLOSED_CHANGESET = "The changeset 123456 was closed at 2022-01-12T06:06:08.";

    private static final String ERROR_MESSAGE_BOUNDING_BOX_TOO_LARGE = "Changeset bounding box size limit exceeded.";

    private static final String ERROR_MESSAGE_CHANGESET_LOCKED = "Changeset 123456 is currently locked by another process.";

    /**
     */
    @Test
    public void versionConflict() {
        ApiResponse.Conflict conflict = ApiResponse.parseConflictResponse(HttpURLConnection.HTTP_CONFLICT, ERROR_MESSAGE_VERSION_CONFLICT_NODE);
        assertTrue(conflict instanceof ApiResponse.VersionConflict);
        assertEquals(12345L, conflict.getElementId());
        assertEquals(Node.NAME, conflict.getElementType());

        conflict = ApiResponse.parseConflictResponse(HttpURLConnection.HTTP_CONFLICT, ERROR_MESSAGE_VERSION_CONFLICT_WAY);
        assertTrue(conflict instanceof ApiResponse.VersionConflict);
        assertEquals(12345L, conflict.getElementId());
        assertEquals(Way.NAME, conflict.getElementType());

        conflict = ApiResponse.parseConflictResponse(HttpURLConnection.HTTP_CONFLICT, ERROR_MESSAGE_VERSION_CONFLICT_RELATION);
        assertTrue(conflict instanceof ApiResponse.VersionConflict);
        assertEquals(12345L, conflict.getElementId());
        assertEquals(Relation.NAME, conflict.getElementType());
    }

    /**
     */
    @Test
    public void alreadyDeleted() {
        ApiResponse.Conflict conflict = ApiResponse.parseConflictResponse(HttpURLConnection.HTTP_GONE, ERROR_MESSAGE_ALREADY_DELETED_NODE);
        assertTrue(conflict instanceof ApiResponse.AlreadyDeletedConflict);
        assertEquals(12345L, conflict.getElementId());
        assertEquals(Node.NAME, conflict.getElementType());

        conflict = ApiResponse.parseConflictResponse(HttpURLConnection.HTTP_GONE, ERROR_MESSAGE_ALREADY_DELETED_WAY);
        assertTrue(conflict instanceof ApiResponse.AlreadyDeletedConflict);
        assertEquals(12345L, conflict.getElementId());
        assertEquals(Way.NAME, conflict.getElementType());

        conflict = ApiResponse.parseConflictResponse(HttpURLConnection.HTTP_GONE, ERROR_MESSAGE_ALREADY_DELETED_RELATION);
        assertTrue(conflict instanceof ApiResponse.AlreadyDeletedConflict);
        assertEquals(12345L, conflict.getElementId());
        assertEquals(Relation.NAME, conflict.getElementType());
    }

    /**
     */
    @Test
    public void stillUsed() {
        ApiResponse.Conflict conflict = ApiResponse.parseConflictResponse(HttpURLConnection.HTTP_PRECON_FAILED, ERROR_MESSAGE_PRECONDITION_STILL_USED_NODE_WAY);
        assertTrue(conflict instanceof ApiResponse.StillUsedConflict);
        assertEquals(Node.NAME, conflict.getElementType());
        assertEquals(12345L, conflict.getElementId());
        assertEquals(Way.NAME, ((ApiResponse.StillUsedConflict) conflict).getUsedByElementType());
        assertArrayEquals(new long[] { 56789 }, ((ApiResponse.StillUsedConflict) conflict).getUsedByElementIds());

        conflict = ApiResponse.parseConflictResponse(HttpURLConnection.HTTP_PRECON_FAILED, ERROR_MESSAGE_PRECONDITION_STILL_USED_NODE_WAYS);
        assertTrue(conflict instanceof ApiResponse.StillUsedConflict);
        assertEquals(Node.NAME, conflict.getElementType());
        assertEquals(12345L, conflict.getElementId());
        assertEquals(Way.NAME, ((ApiResponse.StillUsedConflict) conflict).getUsedByElementType());
        assertArrayEquals(new long[] { 56789, 54321 }, ((ApiResponse.StillUsedConflict) conflict).getUsedByElementIds());

        conflict = ApiResponse.parseConflictResponse(HttpURLConnection.HTTP_PRECON_FAILED, ERROR_MESSAGE_PRECONDITION_STILL_USED_NODE_RELATION);
        assertTrue(conflict instanceof ApiResponse.StillUsedConflict);
        assertEquals(Node.NAME, conflict.getElementType());
        assertEquals(12345L, conflict.getElementId());
        assertEquals(Relation.NAME, ((ApiResponse.StillUsedConflict) conflict).getUsedByElementType());
        assertArrayEquals(new long[] { 56789 }, ((ApiResponse.StillUsedConflict) conflict).getUsedByElementIds());

        conflict = ApiResponse.parseConflictResponse(HttpURLConnection.HTTP_PRECON_FAILED, ERROR_MESSAGE_PRECONDITION_STILL_USED_NODE_RELATIONS);
        assertTrue(conflict instanceof ApiResponse.StillUsedConflict);
        assertEquals(Node.NAME, conflict.getElementType());
        assertEquals(12345L, conflict.getElementId());
        assertEquals(Relation.NAME, ((ApiResponse.StillUsedConflict) conflict).getUsedByElementType());
        assertArrayEquals(new long[] { 56789, 54321 }, ((ApiResponse.StillUsedConflict) conflict).getUsedByElementIds());

        conflict = ApiResponse.parseConflictResponse(HttpURLConnection.HTTP_PRECON_FAILED, ERROR_MESSAGE_PRECONDITION_STILL_USED_WAY_RELATION);
        assertTrue(conflict instanceof ApiResponse.StillUsedConflict);
        assertEquals(Way.NAME, conflict.getElementType());
        assertEquals(12345L, conflict.getElementId());
        assertEquals(Relation.NAME, ((ApiResponse.StillUsedConflict) conflict).getUsedByElementType());
        assertArrayEquals(new long[] { 56789 }, ((ApiResponse.StillUsedConflict) conflict).getUsedByElementIds());

        conflict = ApiResponse.parseConflictResponse(HttpURLConnection.HTTP_PRECON_FAILED, ERROR_MESSAGE_PRECONDITION_STILL_USED_WAY_RELATIONS);
        assertTrue(conflict instanceof ApiResponse.StillUsedConflict);
        assertEquals(Way.NAME, conflict.getElementType());
        assertEquals(12345L, conflict.getElementId());
        assertEquals(Relation.NAME, ((ApiResponse.StillUsedConflict) conflict).getUsedByElementType());
        assertArrayEquals(new long[] { 56789, 54321 }, ((ApiResponse.StillUsedConflict) conflict).getUsedByElementIds());
    }

    /**
     */
    @Test
    public void wayNodeRequired() {
        ApiResponse.Conflict conflict = ApiResponse.parseConflictResponse(HttpURLConnection.HTTP_PRECON_FAILED,
                ERROR_MESSAGE_PRECONDITION_REQUIRED_WAY_NODE_NEG);
        assertTrue(conflict instanceof ApiResponse.RequiredWayNodesConflict);
        assertEquals(Way.NAME, conflict.getElementType());
        assertEquals(-2L, conflict.getElementId());
        assertEquals(Node.NAME, ((ApiResponse.RequiredWayNodesConflict) conflict).getRequriedElementType());
        assertArrayEquals(new long[] { 56789 }, ((ApiResponse.RequiredWayNodesConflict) conflict).getRequiredElementsIds());

        conflict = ApiResponse.parseConflictResponse(HttpURLConnection.HTTP_PRECON_FAILED, ERROR_MESSAGE_PRECONDITION_REQUIRED_WAY_NODES_NEG);
        assertTrue(conflict instanceof ApiResponse.RequiredWayNodesConflict);
        assertEquals(Way.NAME, conflict.getElementType());
        assertEquals(-2L, conflict.getElementId());
        assertEquals(Node.NAME, ((ApiResponse.RequiredWayNodesConflict) conflict).getRequriedElementType());
        assertArrayEquals(new long[] { 56789, 54321 }, ((ApiResponse.RequiredWayNodesConflict) conflict).getRequiredElementsIds());

        conflict = ApiResponse.parseConflictResponse(HttpURLConnection.HTTP_PRECON_FAILED, ERROR_MESSAGE_PRECONDITION_REQUIRED_WAY_NODE);
        assertTrue(conflict instanceof ApiResponse.RequiredWayNodesConflict);
        assertEquals(Way.NAME, conflict.getElementType());
        assertEquals(12345L, conflict.getElementId());
        assertEquals(Node.NAME, ((ApiResponse.RequiredWayNodesConflict) conflict).getRequriedElementType());
        assertArrayEquals(new long[] { 56789 }, ((ApiResponse.RequiredWayNodesConflict) conflict).getRequiredElementsIds());

        conflict = ApiResponse.parseConflictResponse(HttpURLConnection.HTTP_PRECON_FAILED, ERROR_MESSAGE_PRECONDITION_REQUIRED_WAY_NODES);
        assertTrue(conflict instanceof ApiResponse.RequiredWayNodesConflict);
        assertEquals(Way.NAME, conflict.getElementType());
        assertEquals(12345L, conflict.getElementId());
        assertEquals(Node.NAME, ((ApiResponse.RequiredWayNodesConflict) conflict).getRequriedElementType());
        assertArrayEquals(new long[] { 56789, 54321 }, ((ApiResponse.RequiredWayNodesConflict) conflict).getRequiredElementsIds());
    }

    /**
     */
    @Test
    public void oneWayNodeRequired() {
        ApiResponse.Conflict conflict = ApiResponse.parseConflictResponse(HttpURLConnection.HTTP_PRECON_FAILED,
                ERROR_MESSAGE_PRECONDITION_REQUIRED_ONE_WAY_NODE);
        assertTrue(conflict instanceof ApiResponse.NoNodesWayError);
        assertEquals(Way.NAME, conflict.getElementType());
        assertEquals(-522L, conflict.getElementId());
    }
    
    /**
     */
    @Test
    public void relationMemberRequired() {
        ApiResponse.Conflict conflict = ApiResponse.parseConflictResponse(HttpURLConnection.HTTP_PRECON_FAILED,
                ERROR_MESSAGE_PRECONDITION_REQUIRED_RELATION_MEMBERS_NODE_NEG);
        assertTrue(conflict instanceof ApiResponse.RequiredRelationMembersConflict);
        assertEquals(Relation.NAME, conflict.getElementType());
        assertEquals(-2L, conflict.getElementId());
        assertEquals(Node.NAME, ((ApiResponse.RequiredRelationMembersConflict) conflict).getRequriedElementType());
        assertArrayEquals(new long[] { 56789 }, ((ApiResponse.RequiredRelationMembersConflict) conflict).getRequiredElementsIds());

        conflict = ApiResponse.parseConflictResponse(HttpURLConnection.HTTP_PRECON_FAILED, ERROR_MESSAGE_PRECONDITION_REQUIRED_RELATION_MEMBERS_NODES_NEG);
        assertTrue(conflict instanceof ApiResponse.RequiredRelationMembersConflict);
        assertEquals(Relation.NAME, conflict.getElementType());
        assertEquals(-2L, conflict.getElementId());
        assertEquals(Node.NAME, ((ApiResponse.RequiredRelationMembersConflict) conflict).getRequriedElementType());
        assertArrayEquals(new long[] { 56789, 54321 }, ((ApiResponse.RequiredRelationMembersConflict) conflict).getRequiredElementsIds());

        conflict = ApiResponse.parseConflictResponse(HttpURLConnection.HTTP_PRECON_FAILED, ERROR_MESSAGE_PRECONDITION_REQUIRED_RELATION_MEMBERS_NODE);
        assertTrue(conflict instanceof ApiResponse.RequiredRelationMembersConflict);
        assertEquals(Relation.NAME, conflict.getElementType());
        assertEquals(12345L, conflict.getElementId());
        assertEquals(Node.NAME, ((ApiResponse.RequiredRelationMembersConflict) conflict).getRequriedElementType());
        assertArrayEquals(new long[] { 56789 }, ((ApiResponse.RequiredRelationMembersConflict) conflict).getRequiredElementsIds());

        conflict = ApiResponse.parseConflictResponse(HttpURLConnection.HTTP_PRECON_FAILED, ERROR_MESSAGE_PRECONDITION_REQUIRED_RELATION_MEMBERS_NODES);
        assertTrue(conflict instanceof ApiResponse.RequiredRelationMembersConflict);
        assertEquals(Relation.NAME, conflict.getElementType());
        assertEquals(12345L, conflict.getElementId());
        assertEquals(Node.NAME, ((ApiResponse.RequiredRelationMembersConflict) conflict).getRequriedElementType());
        assertArrayEquals(new long[] { 56789, 54321 }, ((ApiResponse.RequiredRelationMembersConflict) conflict).getRequiredElementsIds());

        conflict = ApiResponse.parseConflictResponse(HttpURLConnection.HTTP_PRECON_FAILED, ERROR_MESSAGE_PRECONDITION_REQUIRED_RELATION_MEMBERS_WAY);
        assertTrue(conflict instanceof ApiResponse.RequiredRelationMembersConflict);
        assertEquals(Relation.NAME, conflict.getElementType());
        assertEquals(12345L, conflict.getElementId());
        assertEquals(Way.NAME, ((ApiResponse.RequiredRelationMembersConflict) conflict).getRequriedElementType());
        assertArrayEquals(new long[] { 56789 }, ((ApiResponse.RequiredRelationMembersConflict) conflict).getRequiredElementsIds());

        conflict = ApiResponse.parseConflictResponse(HttpURLConnection.HTTP_PRECON_FAILED, ERROR_MESSAGE_PRECONDITION_REQUIRED_RELATION_MEMBERS_WAYS);
        assertTrue(conflict instanceof ApiResponse.RequiredRelationMembersConflict);
        assertEquals(Relation.NAME, conflict.getElementType());
        assertEquals(12345L, conflict.getElementId());
        assertEquals(Way.NAME, ((ApiResponse.RequiredRelationMembersConflict) conflict).getRequriedElementType());
        assertArrayEquals(new long[] { 56789, 54321 }, ((ApiResponse.RequiredRelationMembersConflict) conflict).getRequiredElementsIds());

        conflict = ApiResponse.parseConflictResponse(HttpURLConnection.HTTP_PRECON_FAILED, ERROR_MESSAGE_PRECONDITION_REQUIRED_RELATION_MEMBERS_RELATION);
        assertTrue(conflict instanceof ApiResponse.RequiredRelationMembersConflict);
        assertEquals(Relation.NAME, conflict.getElementType());
        assertEquals(12345L, conflict.getElementId());
        assertEquals(Relation.NAME, ((ApiResponse.RequiredRelationMembersConflict) conflict).getRequriedElementType());
        assertArrayEquals(new long[] { 56789 }, ((ApiResponse.RequiredRelationMembersConflict) conflict).getRequiredElementsIds());

        conflict = ApiResponse.parseConflictResponse(HttpURLConnection.HTTP_PRECON_FAILED, ERROR_MESSAGE_PRECONDITION_REQUIRED_RELATION_MEMBERS_RELATIONS);
        assertTrue(conflict instanceof ApiResponse.RequiredRelationMembersConflict);
        assertEquals(Relation.NAME, conflict.getElementType());
        assertEquals(12345L, conflict.getElementId());
        assertEquals(Relation.NAME, ((ApiResponse.RequiredRelationMembersConflict) conflict).getRequriedElementType());
        assertArrayEquals(new long[] { 56789, 54321 }, ((ApiResponse.RequiredRelationMembersConflict) conflict).getRequiredElementsIds());
    }

    /**
     */
    @Test
    public void relationRelationMemberStillUsed() {
        ApiResponse.Conflict conflict = ApiResponse.parseConflictResponse(HttpURLConnection.HTTP_PRECON_FAILED, ERROR_MESSAGE_PRECONDITION_RELATION_RELATION);
        assertTrue(conflict instanceof ApiResponse.StillUsedConflict);
        assertEquals(Relation.NAME, conflict.getElementType());
        assertEquals(12345L, conflict.getElementId());
        assertEquals(Relation.NAME, ((ApiResponse.StillUsedConflict) conflict).getUsedByElementType());
        assertArrayEquals(new long[] { 6789 }, ((ApiResponse.StillUsedConflict) conflict).getUsedByElementIds());
    }

    /**
     */
    @Test
    public void relationRelationMemberStillUsedCgiMap() {
        ApiResponse.Conflict conflict = ApiResponse.parseConflictResponse(HttpURLConnection.HTTP_PRECON_FAILED,
                ERROR_MESSAGE_PRECONDITION_RELATION_RELATION_CGIMAP);
        assertTrue(conflict instanceof ApiResponse.StillUsedConflict);
        assertEquals(Relation.NAME, conflict.getElementType());
        assertEquals(12345L, conflict.getElementId());
        assertEquals(Relation.NAME, ((ApiResponse.StillUsedConflict) conflict).getUsedByElementType());
        assertArrayEquals(new long[] { 6789 }, ((ApiResponse.StillUsedConflict) conflict).getUsedByElementIds());
    }

    /**
     */
    @Test
    public void closedChangeset() {
        ApiResponse.Conflict conflict = ApiResponse.parseConflictResponse(HttpURLConnection.HTTP_CONFLICT, ERROR_MESSAGE_CLOSED_CHANGESET);
        assertTrue(conflict instanceof ApiResponse.ClosedChangesetConflict);
        assertEquals(123456L, conflict.getElementId());
    }

    /**
     */
    @Test
    public void changesetLocked() {
        ApiResponse.Conflict conflict = ApiResponse.parseConflictResponse(HttpURLConnection.HTTP_CONFLICT, ERROR_MESSAGE_CHANGESET_LOCKED);
        assertTrue(conflict instanceof ApiResponse.ChangesetLocked);
        assertEquals(123456L, conflict.getElementId());
    }

    /**
     */
    @Test
    public void boundingBoxTooLarge() {
        ApiResponse.Conflict conflict = ApiResponse.parseConflictResponse(HttpURLConnection.HTTP_ENTITY_TOO_LARGE, ERROR_MESSAGE_BOUNDING_BOX_TOO_LARGE);
        assertTrue(conflict instanceof ApiResponse.BoundingBoxTooLargeError);
    }
}
