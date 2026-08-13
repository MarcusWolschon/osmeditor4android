package de.blau.android.contract;

import androidx.annotation.NonNull;

/**
 * Constants for Github repositories and related stuff
 */
public final class Github {
    public static final String CODE_REPO_USER = "MarcusWolschon";
    public static final String CODE_REPO_NAME = "osmeditor4android";

    public static final String PRESET_REPO_USER = "simonpoole";
    public static final String PRESET_REPO_NAME = "beautified-JOSM-preset";

    public static final String APP = "com.github.android";

    /** OAuth 2 Web Application Flow redirect URI, github only accepts "URLs" so we have to fake a host here */
    public static final String WEB_FLOW_REDIRECT_URI = "vespucci://local/oauth_github/";
    public static final String OAUTH_GITHUB_PATH     = "oauth_github";

    public static final String MIME_TYPE_JSON          = "application/json; charset=utf-8";
    public static final String GITHUB_API_BASE_URL     = "https://api.github.com/";
    public static final String GITHUB_HOST             = "github.com";
    public static final String FORGEJO_GITEA_API_PATH  = "api/v1/";
    public static final String AUTH_HEADER_PREFIX      = "token ";
    public static final String ACCEPT_HEADER_GITHUB_V3 = "application/vnd.github.v3+json";

    // instead of hardwiring these we could extract them from
    // https://www.openstreetmap.org/.well-known/oauth-authorization-server
    public static final String AUTHORIZE_PATH    = "authorize";
    public static final String ACCESS_TOKEN_PATH = "access_token";

    // OAuth Scopes
    public static final String SCOPE_PUBLIC_REPO = "public_repo";

    // Response prefixes for internal communication
    public static final String TOKEN_PREFIX = "token:";
    public static final String ERROR_PREFIX = "error:";

    // field in the JSON API response
    public static final String ISSUE_URL = "url";

    /**
     * Resolves the API base URL for a given host. GitHub uses api.github.com while Forgejo/Gitea use /api/v1/ on the
     * same host.
     * 
     * @param host the repository host (e.g. github.com, codeberg.org)
     * @return the API base URL
     */
    public static String getApiBaseUrl(@NonNull String host) {
        if (GITHUB_HOST.equalsIgnoreCase(host)) {
            return "https://api." + host + "/";
        }
        // Gitea/Forgejo typically use /api/v1/ on the same host
        return "https://" + host + "/" + FORGEJO_GITEA_API_PATH;
    }

    /**
     * Get the URL for an issue in the github UI
     * 
     * @param repoUser owner of the repo
     * @param repoName the repo name
     * @param issue the (numeric) issue
     * @return the correct URL
     */
    public static String getIssueUrl(@NonNull String repoUser, @NonNull String repoName, @NonNull String issue) {
        return "https://" + GITHUB_HOST + "/" + repoUser + "/" + repoName + "/issues/" + issue;
    }

    /**
     * Private constructor
     */
    private Github() {
        // don't instantiate
    }
}