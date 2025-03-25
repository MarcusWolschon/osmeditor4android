package io.vespucci.prefs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Data structure class for API data
 * 
 * @author Jan
 * @author Simon
 */
public class API {

    public enum Auth {
        BASIC, OAUTH1A, OAUTH2
    }

    public static class AuthParams {
        public final Auth   auth;
        public final String user;
        public final String pass;
        public final String accesstoken;
        public final String accesstokensecret;

        /**
         * Container for authentication relation parameters
         * 
         * @param auth authorization method
         * @param user the user name (deprecated)
         * @param pass the pass word (deprecated)
         * @param accesstoken the OAuth access token
         * @param accesstokensecret the OAuth access secret
         */
        public AuthParams(@NonNull Auth auth, @Nullable String user, @Nullable String pass, @Nullable String accesstoken, @Nullable String accesstokensecret) {
            this.auth = auth;
            this.user = user;
            this.pass = pass;
            this.accesstoken = accesstoken;
            this.accesstokensecret = accesstokensecret;
        }
    }

    public final String  id;
    public final String  name;
    public final String  url;
    public final String  readonlyurl;
    public final String  notesurl;
    public final Auth    auth;
    public final String  user;
    public final String  pass;
    public final String  accesstoken;
    public final String  accesstokensecret;
    public final int     timeout;
    public final boolean compressedUploads;

    /**
     * Construct an new API instance
     * 
     * @param id the unique id of the API
     * @param name the name of the API
     * @param url the read and write URL
     * @param readonlyurl the read only URL
     * @param notesurl the URL for notes
     * @param authParams authentication params
     * @param timeout timeout in seconds
     * @param compressedUploads if true compress uploads
     */
    public API(@NonNull String id, @NonNull String name, @NonNull String url, @Nullable String readonlyurl, @Nullable String notesurl,
            @NonNull AuthParams authParams, int timeout, boolean compressedUploads) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.readonlyurl = readonlyurl;
        this.notesurl = notesurl;
        this.auth = authParams.auth;
        this.user = authParams.user;
        this.pass = authParams.pass;
        this.accesstoken = authParams.accesstoken;
        this.accesstokensecret = authParams.accesstokensecret;
        this.timeout = timeout;
        this.compressedUploads = compressedUploads;
    }
}
