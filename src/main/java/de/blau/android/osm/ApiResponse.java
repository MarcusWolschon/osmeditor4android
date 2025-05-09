package de.blau.android.osm;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.Serializable;
import java.net.HttpURLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;
import androidx.annotation.NonNull;

/**
 * Parse responses from the API that cause the upload to abort
 * 
 */
public final class ApiResponse {
    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, ApiResponse.class.getSimpleName().length());
    private static final String DEBUG_TAG = ApiResponse.class.getSimpleName().substring(0, TAG_LEN);

    /**
     * These patterns are fairly, to very, unforgiving, hopefully API 0.7 will give the error codes back in a more
     * structured way
     */
    private static final Pattern ERROR_MESSAGE_VERSION_CONFLICT                       = Pattern
            .compile("(?i)Version mismatch: Provided ([0-9]+), server had: ([0-9]+) of (Node|Way|Relation) ([0-9]+)");
    private static final Pattern ERROR_MESSAGE_ALREADY_DELETED                        = Pattern
            .compile("(?i)The (node|way|relation) with the id ([0-9]+) has already been deleted");
    private static final Pattern ERROR_MESSAGE_PRECONDITION_STILL_USED                = Pattern
            .compile("(?i)(?:Precondition failed: )?(Node|Way) ([0-9]+) is still used by (way|relation)[s]? (([0-9]+,?)+).*");
    private static final Pattern ERROR_MESSAGE_PRECONDITION_REQUIRED_WAY_NODES        = Pattern
            .compile("(?i)(?:Precondition failed: )?Way (-?[0-9]+) requires the nodes with id in (([0-9]+,?)+) which either do not exist, or are not visible.");
    private static final Pattern ERROR_MESSAGE_PRECONDITION_REQUIRED_RELATION_MEMBERS = Pattern.compile(
            "(?i)(?:Precondition failed: )?Relation (-?[0-9]+) requires the (nodes|ways|relations) with id in (([0-9]+,?)+) which either do not exist, or are not visible.");
    private static final Pattern ERROR_MESSAGE_PRECONDITION_RELATION_RELATION         = Pattern
            .compile("(?i)(?:Precondition failed: )?The relation ([0-9]+) is used in relation[s]? ([0-9]+).");
    private static final Pattern ERROR_MESSAGE_CLOSED_CHANGESET                       = Pattern.compile("(?i)The changeset ([0-9]+) was closed at.*");
    private static final Pattern ERROR_MESSAGE_CHANGESET_LOCKED                       = Pattern
            .compile("(?i)Changeset ([0-9]+) is currently locked by another process.");
    private static final Pattern ERROR_MESSAGE_BOUNDING_BOX_TOO_LARGE                 = Pattern.compile("(?i)Changeset bounding box size limit exceeded.");
    private static final Pattern ERROR_MESSAGE_WAY_NEEDS_ONE_NODE                     = Pattern
            .compile("(?i)(?:Precondition failed: )?Way (-?[0-9]+) must have at least one node");

    public abstract static class Conflict implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String elementType;
        private final long   elementId;

        /**
         * Construct a new Conflict object
         * 
         * @param type object type
         * @param id object id
         */
        protected Conflict(@NonNull String type, long id) {
            elementType = type;
            elementId = id;
        }

        /**
         * @return the elementType
         */
        @NonNull
        public String getElementType() {
            return elementType;
        }

        /**
         * @return the elementId
         */
        public long getElementId() {
            return elementId;
        }
    }

    public static class VersionConflict extends Conflict implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * Construct a new instance for a local - server version conflict
         * 
         * @param type element type
         * @param id element id
         */
        public VersionConflict(@NonNull String type, long id) {
            super(type, id);
        }
    }

    public static class AlreadyDeletedConflict extends Conflict implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * Construct a new instance for a conflict when the server element has already been deleted
         * 
         * @param type element type
         * @param id element id
         */
        public AlreadyDeletedConflict(@NonNull String type, long id) {
            super(type, id);
        }
    }

    public static class StillUsedConflict extends Conflict implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String usedByType;
        private final long[] usedByIds;

        /**
         * Construct a new instance for a conflict when we are trying to delete an element that is used by an element on
         * the server
         * 
         * @param type element type
         * @param id element id
         * @param usedByType element type that is using this element
         * @param usedByIds element ids of the elements that are using this element
         */
        public StillUsedConflict(@NonNull String type, long id, @NonNull String usedByType, @NonNull long[] usedByIds) {
            super(type, id);
            this.usedByType = usedByType;
            this.usedByIds = usedByIds;
        }

        /**
         * @return the elementType of the element still using it
         */
        @NonNull
        public String getUsedByElementType() {
            return usedByType;
        }

        /**
         * @return the elementIds of the elements still using it
         */
        public long[] getUsedByElementIds() {
            return usedByIds;
        }
    }

    public abstract static class RequiredElementsConflict extends Conflict implements Serializable {

        private static final long serialVersionUID = 1L;
        private final String      memberType;
        private final long[]      ids;

        /**
         * Generic conflict when we are creating an element that uses server side deleted elements
         * 
         * @param type element type
         * @param id element id
         * @param memberType type of the deleted elements
         * @param ids ids of the deleted elements
         */
        protected RequiredElementsConflict(@NonNull String type, long id, @NonNull String memberType, long[] ids) {
            super(type, id);
            this.memberType = memberType;
            this.ids = ids;
        }

        /**
         * @return the memberType
         */
        public String getRequriedElementType() {
            return memberType;
        }

        /**
         * @return the ids
         */
        public long[] getRequiredElementsIds() {
            return ids;
        }
    }

    public static class RequiredWayNodesConflict extends RequiredElementsConflict implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * Conflict when we are creating a way that uses server side deleted nodes
         * 
         * @param id the way id
         * @param nodeIds the node ids
         */
        public RequiredWayNodesConflict(long id, long[] nodeIds) {
            super(Way.NAME, id, Node.NAME, nodeIds);
        }
    }

    public static class RequiredRelationMembersConflict extends RequiredElementsConflict implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * Conflict when we are creating a relation that uses server side deleted members
         * 
         * @param id the relation id
         * @param memberType the member type
         * @param ids the member ids
         */
        public RequiredRelationMembersConflict(long id, @NonNull String memberType, long[] ids) {
            super(Relation.NAME, id, memberType, ids);
        }
    }

    private static final String CHANGESET = "CHANGESET";

    public static class ClosedChangesetConflict extends Conflict implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * Closed changeset on upload conflict
         * 
         * @param id the changeset id
         */
        public ClosedChangesetConflict(long id) {
            super(CHANGESET, id);
        }
    }

    public static class ChangesetLocked extends Conflict implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * Changeset locked
         * 
         * This could only occur with parallel uploads/
         * 
         * @param id the changeset id
         */
        public ChangesetLocked(long id) {
            super(CHANGESET, id);
        }
    }

    public static class BoundingBoxTooLargeError extends Conflict implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * Bounding box too large error
         */
        public BoundingBoxTooLargeError() {
            super(CHANGESET, -1);
        }
    }

    public static class NoNodesWayError extends Conflict implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * Degenerate way, it shouldn't be possible for us to generate these
         * 
         * @param id the way id
         */
        public NoNodesWayError(long id) {
            super(Way.NAME, id);
        }
    }

    /**
     * Private constructor
     */
    private ApiResponse() {
        // empty
    }

    /**
     * Parse an OSM API 0.6 error message
     * 
     * @param code HTTP error code
     * @param message message provided by the API
     * @return a Conflict object
     */
    @NonNull
    public static Conflict parseConflictResponse(int code, @NonNull String message) {
        // got conflict , possible messages see
        // http://wiki.openstreetmap.org/wiki/API_v0.6#Diff_upload:_POST_.2Fapi.2F0.6.2Fchangeset.2F.23id.2Fupload
        switch (code) {
        case HttpURLConnection.HTTP_CONFLICT:
            Matcher m = ERROR_MESSAGE_VERSION_CONFLICT.matcher(message);
            if (m.matches()) {
                return new VersionConflict(mapResponseToType(m.group(3)), Long.parseLong(m.group(4)));
            }
            m = ERROR_MESSAGE_CLOSED_CHANGESET.matcher(message);
            if (m.matches()) {
                // note this should never happen, since we check if the changeset is still open before upload
                return new ClosedChangesetConflict(Long.parseLong(m.group(1)));
            }
            m = ERROR_MESSAGE_CHANGESET_LOCKED.matcher(message);
            if (m.matches()) {
                // note this should never happen, we wait for uploads to complete
                return new ChangesetLocked(Long.parseLong(m.group(1)));
            }
            break;
        case HttpURLConnection.HTTP_GONE:
            m = ERROR_MESSAGE_ALREADY_DELETED.matcher(message);
            if (m.matches()) {
                return new AlreadyDeletedConflict(mapResponseToType(m.group(1)), Long.parseLong(m.group(2)));
            }
            break;
        case HttpURLConnection.HTTP_PRECON_FAILED:
            // Besides the messages parsed here, theoretically the following message could be returned:
            // Relation with id #{id} cannot be saved due to #{element} with id #{element.id} // NOSONAR
            // however it shouldn't be possible to create such situations with vespucci
            m = ERROR_MESSAGE_PRECONDITION_STILL_USED.matcher(message);
            if (m.matches()) {
                return new StillUsedConflict(mapResponseToType(m.group(1)), Long.parseLong(m.group(2)), mapResponseToType(m.group(3)), parseIdList(m.group(4)));
            }
            m = ERROR_MESSAGE_PRECONDITION_REQUIRED_WAY_NODES.matcher(message);
            if (m.matches()) {
                return new RequiredWayNodesConflict(Long.parseLong(m.group(1)), parseIdList(m.group(2)));
            }
            m = ERROR_MESSAGE_PRECONDITION_REQUIRED_RELATION_MEMBERS.matcher(message);
            if (m.matches()) {
                return new RequiredRelationMembersConflict(Long.parseLong(m.group(1)), mapResponseToType(m.group(2)), parseIdList(m.group(3)));
            }
            m = ERROR_MESSAGE_PRECONDITION_RELATION_RELATION.matcher(message);
            if (m.matches()) {
                return new StillUsedConflict(Relation.NAME, Long.parseLong(m.group(1)), Relation.NAME, parseIdList(m.group(2)));
            }
            m = ERROR_MESSAGE_WAY_NEEDS_ONE_NODE.matcher(message);
            if (m.matches()) {
                return new NoNodesWayError(Long.parseLong(m.group(1)));
            }
            break;
        case HttpURLConnection.HTTP_ENTITY_TOO_LARGE:
            m = ERROR_MESSAGE_BOUNDING_BOX_TOO_LARGE.matcher(message);
            if (m.matches()) {
                // BoundingBox is too large
                return new BoundingBoxTooLargeError();
            }
            break;
        default:
            // fall through
        }
        Log.e(DEBUG_TAG, "Code: " + code + " unknown error message: " + message);
        throw new IllegalArgumentException("Code: " + code + " unknown error message: " + message);
    }

    /**
     * Normalize OSM element name from a response
     * 
     * @param responseType the raw response
     * @return a OsmElement type
     */
    @NonNull
    private static String mapResponseToType(@NonNull String responseType) {
        switch (responseType.toLowerCase()) {
        case "node":
        case "nodes":
            return Node.NAME;
        case "way":
        case "ways":
            return Way.NAME;
        case "relation":
        case "relations":
            return Relation.NAME;
        default:
            return "";
        }
    }

    /**
     * Parse a , delimited list of long values
     * 
     * @param idList the String
     * @return an array of the parsed longs
     */
    @NonNull
    private static long[] parseIdList(@NonNull String idList) {
        String[] idStr = idList.split(",");
        long[] ids = new long[idStr.length];
        for (int i = 0; i < idStr.length; i++) {
            ids[i] = Long.parseLong(idStr[i]);
        }
        return ids;
    }
}
