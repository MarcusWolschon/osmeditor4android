package de.blau.android;

/**
 * A small class to store the result returned from the OSM server after trying to upload changes. The response includes
 * things like the HTTP response code, conflict information, etc.
 * 
 * Return not only the error code, but the element involved
 * 
 * @author simon
 */
public class UploadResult extends AbstractUploadResult {

    private static final long serialVersionUID = 2L;

    private String elementType;
    private long   osmId;

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
}
