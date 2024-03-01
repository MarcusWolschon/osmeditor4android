package de.blau.android.prefs;

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

    public final String id;
    public final String name;
    public final String url;
    public final String readonlyurl;
    public final String notesurl;
    public final String user;
    public final String pass;
    public final Auth   auth;
    public final String accesstoken;
    public final String accesstokensecret;

    /**
     * Construct an new API instance
     * 
     * @param id the unique id of the API
     * @param name the name of the API
     * @param url the read and write URL
     * @param readonlyurl the read only URL
     * @param notesurl the URL for notes
     * @param user the user name (deprecated)
     * @param pass the pass word (deprecated)
     * @param auth if == 1 use OAuth
     * @param accesstoken the OAuth access token
     * @param accesstokensecret the OAuth access secret
     */
    public API(@NonNull String id, @NonNull String name, @NonNull String url, @Nullable String readonlyurl, @Nullable String notesurl, @Nullable String user,
            @Nullable String pass, @NonNull Auth auth, @Nullable String accesstoken, @Nullable String accesstokensecret) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.readonlyurl = readonlyurl;
        this.notesurl = notesurl;
        this.user = user;
        this.pass = pass;
        this.auth = auth;
        this.accesstoken = accesstoken;
        this.accesstokensecret = accesstokensecret;
    }
}
