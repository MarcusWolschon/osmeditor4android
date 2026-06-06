package de.blau.android.contract;

/**
 * Constants for OpenStreetMap instances
 */
public final class OpenStreetMap {

    public static final String OSM_REDIRECT_URI = "vespucci:/oauth2/";
    public static final String OAUTH1A_PATH     = "oauth";
    public static final String OAUTH2_PATH      = "oauth2";
    // instead of hardwiring these we could extract them from
    // https://www.openstreetmap.org/.well-known/oauth-authorization-server
    public static final String AUTHORIZE_PATH    = "authorize";
    public static final String ACCESS_TOKEN_PATH = "token";

    /**
     * Private constructor
     */
    private OpenStreetMap() {
        // don't instantiate
    }
}