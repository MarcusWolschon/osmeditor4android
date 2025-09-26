package de.blau.android.imagestorage;

import java.io.Serializable;

import de.blau.android.ErrorCodes;

/**
 * A small class to store the result returned from a image store. The response includes things like the HTTP response
 * code etc.
 * 
 * Return not only the error code, but the element involved
 * 
 * @author simon
 */
public class UploadResult implements Serializable {

    private static final long serialVersionUID = 2L;
    private int               error            = ErrorCodes.OK;
    private int               httpError        = 0;
    private String            url;
    private String            message;

    /**
     * Default constructor
     */
    public UploadResult() {
        // empty
    }

    /**
     * Constructor that sets the error code
     * 
     * @param error the code
     */
    public UploadResult(int error) {
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

    /**
     * @return the url for the image
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param message set the url for the image
     */
    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "" + error + " " + httpError + " " + url + " " + message;
    }
}
