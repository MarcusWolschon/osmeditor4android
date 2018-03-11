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
public class UploadResult implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private int               error            = ErrorCodes.OK;
    private int               httpError        = 0;
    private String            elementType;
    private long              osmId;
    private String            message;

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
     * @return the elementType
     */
    public String getElementType() {
        return elementType;
    }

    /**
     * @param elementType the elementType to set
     */
    public void setElementType(String elementType) {
        this.elementType = elementType;
    }

    /**
     * @return the osmId
     */
    public long getOsmId() {
        return osmId;
    }

    /**
     * @param osmId the osmId to set
     */
    public void setOsmId(long osmId) {
        this.osmId = osmId;
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
