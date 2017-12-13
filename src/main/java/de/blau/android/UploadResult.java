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
    public int                error            = ErrorCodes.OK;
    public int                httpError        = 0;
    public String             elementType;
    public long               osmId;
    public String             message;
}
