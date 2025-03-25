package io.vespucci.exception;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.osm.Issue;
import io.vespucci.osm.OsmElement;

public class OsmIllegalOperationException extends RuntimeException {

    /**
     * Thrown when we are trying to do something that is not allowed from an OSM data perspective
     */
    private static final long serialVersionUID = 2L;

    private final Issue      issue;
    private final OsmElement element;

    /**
     * Construct a new exception
     * 
     * @param message the error message
     */
    public OsmIllegalOperationException(String message) {
        super(message);
        issue = null;
        element = null;
    }

    /**
     * Construct a new exception
     * 
     * @param issue the issue that caused the exception
     * @param element the relevant OsmElement
     * @param message the error message
     */
    public OsmIllegalOperationException(@Nullable Issue issue, @Nullable OsmElement element, String message) {
        super(message);
        this.issue = issue;
        this.element = element;
    }

    /**
     * Construct a new exception from an existing one
     * 
     * @param original the original exception
     */
    public OsmIllegalOperationException(@NonNull OsmIllegalOperationException original) {
        super(original.getMessage());
        this.issue = original.issue;
        this.element = original.element;
    }

    /**
     * Get the issue that caused this if known
     * 
     * @return the issue
     */
    @Nullable
    public Issue getIssue() {
        return issue;
    }

    /**
     * Check if an Issue is set
     * 
     * @return true if there is an Issue value available
     */
    public boolean hasIssue() {
        return issue != null;
    }

    /**
     * Get the OsmElement if any
     * 
     * @return the element
     */
    @Nullable
    public OsmElement getElement() {
        return element;
    }
}
