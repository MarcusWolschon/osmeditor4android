package de.blau.android.exception;

public class OsmParseException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 5410676963227425404L;

    /**
     * Construct a new exception
     * 
     * @param msg the error message
     */
    public OsmParseException(final String msg) {
        super(msg);
    }
}
