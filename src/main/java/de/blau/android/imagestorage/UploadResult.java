package de.blau.android.imagestorage;

import de.blau.android.AbstractUploadResult;

/**
 * A small class to store the result returned from a image store. The response includes things like the HTTP response
 * code etc.
 * 
 * Return not only the error code, but the element involved
 * 
 * @author simon
 */
public class UploadResult extends AbstractUploadResult {

    private static final long serialVersionUID = 2L;
    
    private String url;

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
