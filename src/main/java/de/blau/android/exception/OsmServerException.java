package de.blau.android.exception;

import java.net.HttpURLConnection;

public class OsmServerException extends OsmException {

    private static final String ELEMENT_TYPE_NONE = "none";

    /**
     * 
     */
    private static final long serialVersionUID = 2767654576083786633L;

    private final int errorCode;
    private String    type  = ELEMENT_TYPE_NONE;
    private long      osmId = -1;

    public OsmServerException(final int errorCode, final String string) {
        super(string);
        this.errorCode = errorCode;
    }

    public OsmServerException(final int errorCode, final String type, final long osmId, final String string) {
        super(string);
        this.errorCode = errorCode;
        this.type = type;
        this.osmId = osmId;
    }

    /**
     * Return the error message with a description of the error code
     * 
     * @return a string containing error plus description
     */
    public String getMessageWithDescription() {
        String errorText = errorCodeToMeaning(getErrorCode());
        return super.getMessage() + (errorText != null ? "\nError description: " + errorText : "");
    }

    /**
     * @return the HTTP response-code
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * @return a string with element type and id
     */
    public String getElementDescription() {
        return type + " #" + osmId;
    }

    /**
     * Return the element id
     * 
     * @return the id as a long
     */
    public long getElementId() {
        return osmId;
    }

    /**
     * Return the element type
     * 
     * @return one of "node", "way", "relation"
     */
    public String getElementType() {
        return type;
    }

    /**
     * Check if we have an element
     * 
     * @return true if information on an element is present
     */
    public boolean hasElement() {
        return !ELEMENT_TYPE_NONE.equals(type);
    }

    /**
     * 
     * @param errorCode the HTTP response-code
     * @return the meaning of that code in english
     */
    private static String errorCodeToMeaning(final int errorCode) {
        switch (errorCode) {
        case HttpURLConnection.HTTP_BAD_REQUEST:
            return "The payload did not match the request. This happens for example when an \"update\" request is made and the object id given in the XML does not match the object id in the URL, or if the requested bounding box is too big.";
        case HttpURLConnection.HTTP_UNAUTHORIZED:
            return "A write request was attempted without (valid) HTTP Basic Authorization. ";
        case HttpURLConnection.HTTP_NOT_FOUND:
            return "The object requested to be retrieved/modified/deleted does not exist, and did never. ";
        case HttpURLConnection.HTTP_BAD_METHOD:
            return "The keyword \"create\" was passed on the URL but the request was not a PUT request. ";
        case HttpURLConnection.HTTP_GONE:
            return "The object requested to be retrieved/modified/deleted existed once but has been deleted meanwhile. ";
        case HttpURLConnection.HTTP_PRECON_FAILED:
            return "The operation requested would break referential integrity (e.g. when requesting to delete a node that is used in a way, or when modifying a way to refer to a non-existing node). This error code is also used when the XML payload contains an object id in the context of a \"create\" request (where the server is expected to assign a fresh id). ";
        case 417:
            return "The API no longer returns a 417 error code, however if you are using curl you might come across it when curl sends an Expect header and lighttpd rejects it. See the curl page for more information ";
        case HttpURLConnection.HTTP_INTERNAL_ERROR:
            return "An internal error occurred. This is usually an uncaught Ruby exception and should be reported as a bug. There have been cases where such errors were caused by timeouts, i.e. a retry after a short waiting period could succeed. ";
        case HttpURLConnection.HTTP_UNAVAILABLE:
            return "The database has been taken offline for maintenance. ";
        }
        return null;
    }
}
