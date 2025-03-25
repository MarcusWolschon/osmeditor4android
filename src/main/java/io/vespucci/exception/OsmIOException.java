package io.vespucci.exception;

public class OsmIOException extends OsmException {

    /**
     * 
     */
    private static final long serialVersionUID = 3180633569202678654L;

    /**
     * Construct a new exception
     * 
     * @param message the error message
     */
    public OsmIOException(final String message) {
        super(message);
    }

    /**
     * Construct a new exception
     * 
     * @param message the error message
     * @param cause the original Exception
     */
    public OsmIOException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
