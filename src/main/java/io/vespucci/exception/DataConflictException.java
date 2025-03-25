package io.vespucci.exception;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.osm.OsmElement;

public class DataConflictException extends OsmException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    // local element causing the issue
    private final String type;
    private final long   osmId;

    /**
     * Construct a new exception
     * 
     * @param errorCode the error code
     * @param type the type of the affected OSM element
     * @param osmId the id of the affected OSM element
     * @param message the error message
     */
    public DataConflictException(@NonNull final String type, final long osmId, @Nullable final String message) {
        super(message);
        this.type = type;
        this.osmId = osmId;
    }

    public DataConflictException(@NonNull OsmElement element, @Nullable final String message) {
        this(element.getName(), element.getOsmId(), message);
    }

    /**
     * @return a string with element type and id
     */
    public String getElementDescription() {
        return type + " #" + osmId;
    }

    /**
     * Return the element id
     * 
     * @return the id as a long
     */
    public long getElementId() {
        return osmId;
    }

    /**
     * Return the element type
     * 
     * @return one of "node", "way", "relation"
     */
    public String getElementType() {
        return type;
    }
}
