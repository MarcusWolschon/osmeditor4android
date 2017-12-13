package de.blau.android.exception;

public class OsmIllegalOperationException extends RuntimeException {

    /**
     * Thrown when we are trying to do something that is not allowed from an OSM data perspective
     */
    private static final long serialVersionUID = 1L;

    public OsmIllegalOperationException(String string) {
        super(string);
    }

    public OsmIllegalOperationException(OsmIllegalOperationException e) {
        super(e.getMessage());
    }

}
