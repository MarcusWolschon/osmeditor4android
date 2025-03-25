package io.vespucci.contract;

/**
 * Constants for codes missing from HttpURLConnection.
 * 
 * @author simon
 *
 */
public final class HttpStatusCodes {

    /**
     * Private constructor
     */
    private HttpStatusCodes() {
        // don't instantiate
    }

    public static final int HTTP_TOO_MANY_REQUESTS        = 429;
    public static final int HTTP_BANDWIDTH_LIMIT_EXCEEDED = 509;
}
