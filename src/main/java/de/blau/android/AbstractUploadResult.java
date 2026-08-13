package de.blau.android;

import java.io.Serializable;

/**
 * A small class to store the result returned from the OSM server after trying to upload changes. The response includes
 * things like the HTTP response code, conflict information, etc.
 * 
 * Return not only the error code, but the element involved
 * 
 * @author simon
 */
public class AbstractUploadResult implements Serializable {

    private static final long serialVersionUID = 2L;
    protected int             error            = ErrorCodes.OK;
    protected int             httpError        = 0;
    protected String          message;

    /**
     * Default constructor
     */
    public AbstractUploadResult() {
        // empty
    }

    /**
     * Constructor that sets the error code
     * 
     * @param error the code
     */
    public AbstractUploadResult(int error) {
        this.error = error;
    }

    /**
     * @return the error
     */
    public int getError() {
        return error;
    }

    /**
     * @param error the error to set
     */
    public void setError(int error) {
        this.error = error;
    }

    /**
     * @return the httpError
     */
    public int getHttpError() {
        return httpError;
    }

    /**
     * @param httpError the httpError to set
     */
    public void setHttpError(int httpError) {
        this.httpError = httpError;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }
}
