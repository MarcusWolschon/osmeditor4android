package de.blau.android.contract;

/**
 * Constants for Github repositories and related stuff
 */
public final class Github {
    public static final String CODE_REPO_USER   = "MarcusWolschon";
    public static final String CODE_REPO_NAME   = "osmeditor4android";
    public static final String PRESET_REPO_USER = "simonpoole";
    public static final String PRESET_REPO_NAME = "beautified-JOSM-preset";

    public static final String APP = "com.github.android";

    public static final String DEVICE_CODE_URL = "https://github.com/login/device/code";
    public static final String ACCESS_TOKEN_URL = "https://github.com/login/oauth/access_token";
    public static final String DEVICE_AUTH_URL = "https://github.com/login/device";

    /**
     * GitHub OAuth App client_id for Device Flow.
     * Replace this with your own registered Client ID if needed.
     */
    public static final String OAUTH_CLIENT_ID = "Ov23liRkzNU5Ez3PvNLv";

    public static final String MIME_TYPE_JSON = "application/json; charset=utf-8";
    public static final String GITHUB_API_BASE_URL = "https://api.github.com/";
    public static final String GITHUB_HOST = "github.com";
    public static final String FORGEJO_GITEA_API_PATH = "api/v1/";
    public static final String AUTH_HEADER_PREFIX = "token ";
    public static final String ACCEPT_HEADER_JSON = "application/json";
    public static final String ACCEPT_HEADER_GITHUB_V3 = "application/vnd.github.v3+json";
    public static final String GRANT_TYPE_DEVICE_CODE = "urn:ietf:params:oauth:grant-type:device_code";

    /**
     * Resolves the API base URL for a given host
     * 
     * @param host the repository host (e.g. github.com, codeberg.org)
     * @return the API base URL
     */
    public static String getApiBaseUrl(String host) {
        if (GITHUB_HOST.equalsIgnoreCase(host)) {
            return "https://api." + host + "/";
        }
        // Gitea/Forgejo typically use /api/v1/ on the same host
        return "https://" + host + "/" + FORGEJO_GITEA_API_PATH;
    }

    /**
     * Resolves the Device Flow code endpoint
     * 
     * @param host the repository host
     * @return the device code URL
     */
    public static String getDeviceCodeUrl(String host) {
        if (GITHUB_HOST.equalsIgnoreCase(host)) {
            return "https://" + host + "/login/device/code";
        }
        return "https://" + host + "/login/oauth/device/code";
    }

    /**
     * Resolves the OAuth token endpoint
     * 
     * @param host the repository host
     * @return the access token URL
     */
    public static String getAccessTokenUrl(String host) {
        if (GITHUB_HOST.equalsIgnoreCase(host)) {
            return "https://" + host + "/login/oauth/access_token";
        }
        return "https://" + host + "/login/oauth/access_token";
    }

    /**
     * Resolves the User Authorization URL
     * 
     * @param host the repository host
     * @return the user auth URL
     */
    public static String getUserAuthUrl(String host) {
        if (GITHUB_HOST.equalsIgnoreCase(host)) {
            return "https://" + host + "/login/device";
        }
        return "https://" + host + "/login/oauth/device";
    }

    /**
     * Resolves the OAuth Client ID for a given host.
     * 
     * @param host the repository host
     * @return the client_id, or an empty string if not configured
     */
    public static String getOAuthClientId(String host) {
        if (GITHUB_HOST.equalsIgnoreCase(host)) {
            return OAUTH_CLIENT_ID;
        }
        return "";
    }

    public static final String KEY_CLIENT_ID = "client_id";
    public static final String KEY_SCOPE = "scope";
    public static final String KEY_DEVICE_CODE = "device_code";
    public static final String KEY_USER_CODE = "user_code";
    public static final String KEY_INTERVAL = "interval";
    public static final String KEY_GRANT_TYPE = "grant_type";
    public static final String KEY_ACCESS_TOKEN = "access_token";
    public static final String KEY_ERROR = "error";

    public static final String SCOPE_PUBLIC_REPO = "public_repo";
    public static final String ERROR_ACCESS_DENIED = "access_denied";
    public static final String ERROR_EXPIRED_TOKEN = "expired_token";
    public static final String ERROR_SLOW_DOWN = "slow_down";
    public static final String ERROR_AUTHORIZATION_PENDING = "authorization_pending";
    public static final String ERROR_TIMEOUT = "timeout";
    public static final String ERROR_FAILED_DEVICE_CODE = "failed_to_get_device_code";

    public static final String TOKEN_PREFIX = "token:";
    public static final String ERROR_PREFIX = "error:";

    /**
     * Private constructor
     */
    private Github() {
        // don't instantiate
    }
}
