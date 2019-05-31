package de.blau.android.prefs;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Data structure class for API data
 * 
 * @author Jan
 */
public class API {
    public final String  id;
    public final String  name;
    public final String  url;
    public final String  readonlyurl;
    public final String  notesurl;
    public final String  user;
    public final String  pass;
    public final String  preset;           // no longer used
    public final boolean showicon;         // no longer used
    public final boolean oauth;
    public final String  accesstoken;
    public final String  accesstokensecret;

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
     * @param oauth if == 1 use OAuth
     * @param accesstoken the OAuth access token
     * @param accesstokensecret the OAuth access secret
     */
    public API(@NonNull String id, @NonNull String name, @NonNull String url, @Nullable String readonlyurl, @Nullable String notesurl, @Nullable String user,
            @Nullable String pass, int oauth, @Nullable String accesstoken, @Nullable String accesstokensecret) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.readonlyurl = readonlyurl;
        this.notesurl = notesurl;
        this.user = user;
        this.pass = pass;
        this.preset = null;
        this.showicon = false;
        this.oauth = (oauth == 1);
        this.accesstoken = accesstoken;
        this.accesstokensecret = accesstokensecret;
    }
}
