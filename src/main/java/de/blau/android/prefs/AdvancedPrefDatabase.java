package de.blau.android.prefs;

import java.io.File;
import java.io.IOException;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.contract.Files;
import de.blau.android.contract.Paths;
import de.blau.android.contract.Urls;
import de.blau.android.exception.IllegalOperationException;
import de.blau.android.osm.Server;
import de.blau.android.presets.AutoPreset;
import de.blau.android.presets.Preset;
import de.blau.android.propertyeditor.CustomPreset;
import de.blau.android.util.FileUtil;

/**
 * This class provides access to complex settings like OSM APIs which consist of complex/relational data. WARNING: It
 * has nothing to do with the "Advanced preferences" the user sees in the menu; those are just a separate
 * PreferenceScreen defined in the preferences.xml and handled like normal prefs!
 * 
 * @author Jan
 * @author Simon Poole
 */
public class AdvancedPrefDatabase extends SQLiteOpenHelper {

    private final Resources         r;
    private final SharedPreferences prefs;
    private final String            PREF_SELECTED_API;

    private static final int    DATA_VERSION = 12;
    private static final String LOGTAG       = "AdvancedPrefDB";

    /** The ID string for the default API and the default Preset */
    public static final String ID_DEFAULT = "default";
    public static final String ID_SANDBOX = "sandbox";

    private static final String ID_DEFAULT_GEOCODER_NOMINATIM = "Nominatim";
    private static final String ID_DEFAULT_GEOCODER_PHOTON    = "Photon";

    private static final String PRESETS_TABLE  = "presets";
    private static final String ID_FIELD       = "id";
    private static final String POSITION_FIELD = "position";

    private static final String APIS_TABLE = "apis";

    /** The ID of the currently active API */
    private String currentAPI;

    /** The ID of the currently active API */
    private static Server currentServer = null;

    private Context context;

    /**
     * Construct a new database instance
     * 
     * @param context an Android Context
     */
    public AdvancedPrefDatabase(@NonNull Context context) {
        super(context, "AdvancedPrefs", null, DATA_VERSION);
        this.context = context;
        r = context.getResources();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        PREF_SELECTED_API = r.getString(R.string.config_selected_api);
        currentAPI = prefs.getString(PREF_SELECTED_API, null);
        if (currentAPI == null) {
            migrateAPI(getWritableDatabase());
        }
        if (getPreset(ID_DEFAULT) == null) {
            addPreset(ID_DEFAULT, r.getString(R.string.config_built_in_preset), "", true);
        }
    }

    @Override
    public synchronized void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE apis (id TEXT, name TEXT, url TEXT, readonlyurl TEXT, notesurl TEXT, user TEXT, pass TEXT, preset TEXT, showicon INTEGER DEFAULT 1, oauth INTEGER DEFAULT 0, accesstoken TEXT, accesstokensecret TEXT)");
        db.execSQL(
                "CREATE TABLE presets (id TEXT, name TEXT, url TEXT, lastupdate TEXT, data TEXT, position INTEGER DEFAULT 0, active INTEGER DEFAULT 0, usetranslations INTEGER DEFAULT 1)");
        db.execSQL("CREATE TABLE geocoders (id TEXT, type TEXT, version INTEGER DEFAULT 0, name TEXT, url TEXT, active INTEGER DEFAULT 0)");
        addGeocoder(db, ID_DEFAULT_GEOCODER_NOMINATIM, ID_DEFAULT_GEOCODER_NOMINATIM, GeocoderType.NOMINATIM, 0, Urls.DEFAULT_NOMINATIM_SERVER, true);
        addGeocoder(db, ID_DEFAULT_GEOCODER_PHOTON, ID_DEFAULT_GEOCODER_PHOTON, GeocoderType.PHOTON, 0, Urls.DEFAULT_PHOTON_SERVER, true);
    }

    @Override
    public synchronized void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(LOGTAG, "Upgrading API DB");
        if (oldVersion <= 1 && newVersion >= 2) {
            db.execSQL("ALTER TABLE apis ADD COLUMN showicon INTEGER DEFAULT 0");
        }
        if (oldVersion <= 2 && newVersion >= 3) {
            db.execSQL("ALTER TABLE apis ADD COLUMN oauth INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE apis ADD COLUMN accesstoken TEXT DEFAULT NULL");
            db.execSQL("ALTER TABLE apis ADD COLUMN accesstokensecret TEXT DEFAULT NULL");
            db.execSQL("UPDATE apis SET url='" + Urls.DEFAULT_API + "' WHERE id='" + ID_DEFAULT + "'");
        }
        if (oldVersion <= 3 && newVersion >= 4) {
            db.execSQL("ALTER TABLE presets ADD COLUMN active INTEGER DEFAULT 0");
            db.execSQL("UPDATE presets SET active=1 WHERE id='default'");
        }
        if (oldVersion <= 4 && newVersion >= 5) {
            db.execSQL("UPDATE apis SET url='" + Urls.DEFAULT_API + "' WHERE id='" + ID_DEFAULT + "'");
        }
        if (oldVersion <= 5 && newVersion >= 6) {
            db.execSQL("ALTER TABLE apis ADD COLUMN readonlyurl TEXT DEFAULT NULL");
            db.execSQL("ALTER TABLE apis ADD COLUMN notesurl TEXT DEFAULT NULL");
        }
        if (oldVersion <= 6 && newVersion >= 7) {
            // this used to add a non-https version of the API, however this no longer works
        }
        if (oldVersion <= 7 && newVersion >= 8) {
            db.execSQL("CREATE TABLE geocoders (id TEXT, type TEXT, version INTEGER DEFAULT 0, name TEXT, url TEXT, active INTEGER DEFAULT 0)");
            addGeocoder(db, ID_DEFAULT_GEOCODER_NOMINATIM, ID_DEFAULT_GEOCODER_NOMINATIM, GeocoderType.NOMINATIM, 0, Urls.DEFAULT_NOMINATIM_SERVER, true);
            addGeocoder(db, ID_DEFAULT_GEOCODER_PHOTON, ID_DEFAULT_GEOCODER_PHOTON, GeocoderType.PHOTON, 0, Urls.DEFAULT_PHOTON_SERVER, true);
        }
        if (oldVersion <= 8 && newVersion >= 9) {
            addAPI(db, ID_SANDBOX, Urls.DEFAULT_SANDBOX_API_NAME, Urls.DEFAULT_SANDBOX_API, null, "", "", ID_SANDBOX, true);
        }
        if (oldVersion <= 9 && newVersion >= 10) {
            db.execSQL("ALTER TABLE presets ADD COLUMN position INTEGER DEFAULT 0");
            Cursor dbresult = db.query(PRESETS_TABLE, new String[] { ID_FIELD }, null, null, null, null, null);
            dbresult.moveToFirst();
            int count = dbresult.getCount();
            for (int i = 0; i < count; i++) {
                ContentValues values = new ContentValues();
                values.put(POSITION_FIELD, i);
                db.update(PRESETS_TABLE, values, "id = ?", new String[] { dbresult.getString(0) });
                dbresult.moveToNext();
            }
            dbresult.close();
        }
        if (oldVersion <= 10 && newVersion >= 11) {
            db.execSQL("ALTER TABLE presets ADD COLUMN usetranslations INTEGER DEFAULT 1");
        }
        if (oldVersion <= 11 && newVersion >= 12) {
            try {
                FileUtil.copyFileFromAssets(context, "images/" + CustomPreset.ICON,
                        FileUtil.getPublicDirectory(FileUtil.getPublicDirectory(), Paths.DIRECTORY_PATH_AUTOPRESET), CustomPreset.ICON);
            } catch (IOException e) {
                Log.e(LOGTAG, "Unable to copy custom preset icon");
            }
        }
    }

    @Override
    public synchronized void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(LOGTAG, "Downgrading API DB loosing all settings");
        db.execSQL("DROP TABLE apis");
        db.execSQL("DROP TABLE presets");
        db.execSQL("DROP TABLE geocoders");
        onCreate(db);
        migrateAPI(db);
    }

    /**
     * Creates the default API entry using the old-style username/password
     * 
     * @param db an instance of the pref database
     */
    private synchronized void migrateAPI(@NonNull SQLiteDatabase db) {
        Log.d(LOGTAG, "Migrating API");
        String user = prefs.getString(r.getString(R.string.config_username_key), "");
        String pass = prefs.getString(r.getString(R.string.config_password_key), "");
        Log.d(LOGTAG, "Adding default URL with user '" + user + "'");
        addAPI(db, ID_DEFAULT, Urls.DEFAULT_API_NAME, Urls.DEFAULT_API, null, user, pass, ID_DEFAULT, true);
        Log.d(LOGTAG, "Adding default dev URL");
        addAPI(db, ID_SANDBOX, Urls.DEFAULT_SANDBOX_API_NAME, Urls.DEFAULT_SANDBOX_API, null, "", "", ID_SANDBOX, true);
        Log.d(LOGTAG, "Selecting default API");
        selectAPI(db, ID_DEFAULT);
        Log.d(LOGTAG, "Deleting old user/pass settings");
        Editor editor = prefs.edit();
        editor.remove(r.getString(R.string.config_username_key));
        editor.remove(r.getString(R.string.config_password_key));
        editor.commit();
        Log.d(LOGTAG, "Migration finished");
    }

    /**
     * Set the currently active API
     * 
     * @param id the ID of the API to be set as active
     */
    public synchronized void selectAPI(@NonNull String id) {
        SQLiteDatabase db = getReadableDatabase();
        selectAPI(db, id);
        db.close();
    }

    /**
     * Set the currently active API
     * 
     * @param db an instance of the pref database
     * @param id the ID of the API to be set as active
     */
    private synchronized void selectAPI(@NonNull SQLiteDatabase db, @NonNull String id) {
        Log.d(LOGTAG, "Selecting API with ID: " + id);
        if (getAPIs(db, id).length == 0) {
            throw new IllegalOperationException("Non-existant API selected");
        }
        prefs.edit().putString(PREF_SELECTED_API, id).commit();
        currentAPI = id;
        Main.prepareRedownload();
        resetCurrentServer();
    }

    /**
     * Rest the current Server object, closing any MapSplit source first
     */
    public void resetCurrentServer() {
        if (currentServer != null) {
            currentServer.closeMapSplitSource();
        }
        currentServer = null; // force recreation of Server object
    }

    /**
     * @return a list of API objects containing all available APIs
     */
    @NonNull
    public API[] getAPIs() {
        return getAPIs(null);
    }

    /** @return the API object representing the currently selected API */
    @Nullable
    public API getCurrentAPI() {
        API[] apis = getAPIs(currentAPI);
        if (apis.length == 0) {
            return null;
        }
        return apis[0];
    }

    /**
     * If a Server object already exists return that, otherwise create one from the currently configured API
     * 
     * @return a Server object matching the current API
     */
    @NonNull
    public synchronized Server getServerObject() {
        API api = getCurrentAPI();
        if (api == null) {
            Log.e(LOGTAG, "Current API was null, selecting default");
            selectAPI(ID_DEFAULT);
            api = getCurrentAPI();
            if (api == null) {
                Log.e(LOGTAG, "Couldn't find default server api, fatal error");
                throw new IllegalStateException("Couldn't find default server api, fatal error");
            }
        }
        if (currentServer == null) { // only create when necessary
            String version = r.getString(R.string.app_name) + " " + r.getString(R.string.app_version);
            currentServer = new Server(context, api, version);
        }
        return currentServer;
    }

    /**
     * Sets name and URLs for a OSM API entry id
     * 
     * @param id the internal id for this entry
     * @param name the name of the entry
     * @param url the read / write url
     * @param readonlyurl a read only url or null
     * @param notesurl a note url or null
     * @param oauth set to true if OAuth should be used for this API
     */
    public synchronized void setAPIDescriptors(@NonNull String id, @NonNull String name, @NonNull String url, @Nullable String readonlyurl,
            @Nullable String notesurl, boolean oauth) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("url", url);
        values.put("readonlyurl", readonlyurl);
        values.put("notesurl", notesurl);
        values.put("oauth", oauth ? 1 : 0);
        db.update(APIS_TABLE, values, "id = ?", new String[] { id });
        if (!oauth) { // zap any key and secret
            values = new ContentValues();
            values.put("accesstoken", (String) null);
            values.put("accesstokensecret", (String) null);
            db.update(APIS_TABLE, values, "id = ?", new String[] { id });
        }
        db.close();
        resetCurrentServer();
    }

    /**
     * Sets OAuth access token and secret of the current API entry
     * 
     * @param token the OAuth token
     * @param secret the OAuth secret
     */
    public synchronized void setAPIAccessToken(@Nullable String token, @Nullable String secret) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("accesstoken", token);
        values.put("accesstokensecret", secret);
        db.update(APIS_TABLE, values, "id = ?", new String[] { currentAPI });
        Log.d(LOGTAG, "setAPIAccessToken " + token + " secret " + secret);
        db.close();
        resetCurrentServer();
    }

    /**
     * Sets login data (user, password) for the current API, normally ou would use OAuth
     * 
     * @param user login/user name
     * @param pass password
     */
    public synchronized void setCurrentAPILogin(@Nullable String user, @Nullable String pass) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user", user);
        values.put("pass", pass);
        db.update(APIS_TABLE, values, "id = ?", new String[] { currentAPI });
        db.close();
        resetCurrentServer();
    }

    /**
     * Add a new API with the given values to the API database
     * 
     * @param id the internal id for this entry
     * @param name the name of the entry
     * @param url the read / write url
     * @param readonlyurl a read only url or null
     * @param notesurl a note url or null
     * @param user OSM display name
     * @param pass OSM password
     * @param oauth set to true if OAuth should be used for this API
     */
    public synchronized void addAPI(@NonNull String id, @NonNull String name, @NonNull String url, @Nullable String readonlyurl, @Nullable String notesurl,
            @Nullable String user, @Nullable String pass, boolean oauth) {
        SQLiteDatabase db = getWritableDatabase();
        addAPI(db, id, name, url, readonlyurl, notesurl, user, pass, oauth);
        db.close();
    }

    /**
     * Adds a new API with the given values to the supplied database
     * 
     * @param db a writeable SQLiteDatabase
     * @param id the internal id for this entry
     * @param name the name of the entry
     * @param url the read / write url
     * @param readonlyurl a read only url or null
     * @param notesurl a note url or null
     * @param user OSM display name
     * @param pass OSM password
     * @param oauth set to true if OAuth should be used for this API
     */
    private synchronized void addAPI(@NonNull SQLiteDatabase db, @NonNull String id, @NonNull String name, @NonNull String url, @Nullable String readonlyurl,
            @Nullable String notesurl, @Nullable String user, @Nullable String pass, boolean oauth) {
        ContentValues values = new ContentValues();
        values.put(ID_FIELD, id);
        values.put("name", name);
        values.put("url", url);
        values.put("readonlyurl", readonlyurl);
        values.put("notesurl", notesurl);
        values.put("user", user);
        values.put("pass", pass);
        values.put("preset", (String) null); // no longer used
        values.put("showicon", 0); // no longer used
        values.put("oauth", oauth ? 1 : 0);
        db.insert(APIS_TABLE, null, values);
    }

    /**
     * Removes an API from the API database
     * 
     * @param id id of the API we want to delete
     */
    public synchronized void deleteAPI(@NonNull final String id) {
        if (id.equals(ID_DEFAULT)) {
            throw new IllegalOperationException("Cannot delete default");
        }
        if (id.equals(currentAPI)) {
            selectAPI(ID_DEFAULT);
        }
        SQLiteDatabase db = getWritableDatabase();
        db.delete(APIS_TABLE, "id = ?", new String[] { id });
        db.close();
    }

    /**
     * Fetches all APIs matching the given ID, or all APIs if id is null
     * 
     * @param id null to fetch all APIs, or API-ID to fetch a specific one
     * @return API[]
     */
    @NonNull
    public synchronized API[] getAPIs(@Nullable String id) {
        SQLiteDatabase db = getReadableDatabase();
        API[] result = getAPIs(db, id);
        db.close();
        return result;
    }

    /**
     * Get the all currently available APIs or just for id if present
     * 
     * @param db an open database
     * @param id the id of the API configuration or null for all
     * @return an array of API objects
     */
    @NonNull
    private synchronized API[] getAPIs(@NonNull SQLiteDatabase db, @Nullable String id) {
        Cursor dbresult = db.query(APIS_TABLE, new String[] { ID_FIELD, "name", "url", "readonlyurl", "notesurl", "user", "pass", "preset", "showicon", "oauth",
                "accesstoken", "accesstokensecret" }, id == null ? null : "id = ?", id == null ? null : new String[] { id }, null, null, null, null);
        API[] result = new API[dbresult.getCount()];
        dbresult.moveToFirst();
        for (int i = 0; i < result.length; i++) {
            result[i] = new API(dbresult.getString(0), dbresult.getString(1), dbresult.getString(2), dbresult.getString(3), dbresult.getString(4),
                    dbresult.getString(5), dbresult.getString(6), dbresult.getInt(9), dbresult.getString(10), dbresult.getString(11));
            Log.d(LOGTAG, "id " + dbresult.getString(0) + " name " + dbresult.getString(1) + " url " + dbresult.getString(2) + " readonly url "
                    + dbresult.getString(3) + " notes url " + dbresult.getString(4) + " " + dbresult.getString(10) + " " + dbresult.getString(11));
            dbresult.moveToNext();
        }
        dbresult.close();
        return result;
    }

    /**
     * Try to retrieve an API that uses the specified filename as a source
     * 
     * @param filename the filename
     * @return the API id or null if not found
     */
    @Nullable
    public String getReadOnlyApiId(@NonNull String filename) {
        try (SQLiteDatabase readableDb = getReadableDatabase()) {
            String queryUri = "file:%" + filename;
            try (Cursor dbresult = readableDb.query(APIS_TABLE, new String[] { ID_FIELD, "readonlyurl" }, "readonlyurl LIKE ?", new String[] { queryUri }, null,
                    null, null, null)) {
                if (dbresult.getCount() > 0) {
                    dbresult.moveToFirst();
                    return dbresult.getString(0);
                }
            }
        }
        return null;
    }

    /**
     * Creates an object for the currently selected presets
     * 
     * @return an array of preset objects, or null if no valid preset is selected or the preset cannot be created
     */
    @Nullable
    public Preset[] getCurrentPresetObject() {
        long start = System.currentTimeMillis();
        PresetInfo[] presetInfos = getActivePresets();
        if (presetInfos == null || presetInfos.length == 0) {
            return null;
        }

        Preset[] activePresets = new Preset[presetInfos.length + 1];
        for (int i = 0; i < presetInfos.length; i++) {
            PresetInfo pi = presetInfos[i];
            try {
                Log.d(LOGTAG, "Adding preset " + pi.name);
                if (pi.url.startsWith(Preset.APKPRESET_URLPREFIX)) {
                    activePresets[i] = new Preset(context, getPresetDirectory(pi.id), pi.url.substring(Preset.APKPRESET_URLPREFIX.length()),
                            pi.useTranslations);
                } else {
                    activePresets[i] = new Preset(context, getPresetDirectory(pi.id), null, pi.useTranslations);
                }
            } catch (Exception e) {
                Log.e(LOGTAG, "Failed to create preset", e);
                activePresets[i] = null;
            }
        }
        int autopresetPosition = activePresets.length - 1;
        try {
            AutoPreset.readAutoPreset(context, activePresets, autopresetPosition);
        } catch (Exception e) {
            Log.e(LOGTAG, "Failed to find auto-preset, creating", e);
            try {
                FileUtil.copyFileFromAssets(context, Files.FILE_NAME_AUTOPRESET_TEMPLATE,
                        FileUtil.getPublicDirectory(FileUtil.getPublicDirectory(), Paths.DIRECTORY_PATH_AUTOPRESET), Files.FILE_NAME_AUTOPRESET);
                AutoPreset.readAutoPreset(context, activePresets, autopresetPosition);
            } catch (Exception e1) {
                Log.e(LOGTAG, "Failed to create auto-preset", e1);
                activePresets[autopresetPosition] = null;
            }
        }
        Log.d(LOGTAG, "Elapsed time to read presets " + (System.currentTimeMillis() - start) / 1000);
        if (activePresets.length >= 1) {
            return activePresets;
        }
        return null;
    }

    /**
     * Get PresetInfos for all currently known presets
     * 
     * @return an array of PresetInfo
     */
    @Nullable
    public PresetInfo[] getPresets() {
        return getPresets(null, false);
    }

    /**
     * Gets a preset by ID (will return null if no preset with this ID exists)
     * 
     * @param id id of the preset
     * @return a PresetInfo object or null
     */
    @Nullable
    public PresetInfo getPreset(@NonNull String id) {
        PresetInfo[] found = getPresets(id, false);
        if (found.length == 0) {
            return null;
        }
        return found[0];
    }

    /**
     * Gets a preset by URL (will return null if no preset with this URL exists)
     * 
     * @param url the url, if null the first preset found will be returned
     * @return a PresetInfo object or null
     */
    @Nullable
    public PresetInfo getPresetByURL(@Nullable String url) {
        PresetInfo[] found = getPresets(url, true);
        if (found.length == 0) {
            return null;
        }
        return found[0];
    }

    /**
     * Gets an array of PresetInfos for all active presets
     * 
     * @return an array of PresetInfo
     */
    @NonNull
    public PresetInfo[] getActivePresets() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor dbresult = db.query(PRESETS_TABLE, new String[] { ID_FIELD, "name", "url", "lastupdate", "active", "usetranslations" }, "active=1", null, null,
                null, POSITION_FIELD);
        PresetInfo[] result = new PresetInfo[dbresult.getCount()];
        Log.d(LOGTAG, "#prefs " + result.length);
        dbresult.moveToFirst();
        for (int i = 0; i < result.length; i++) {
            Log.d(LOGTAG, "Reading pref " + i + " " + dbresult.getString(1));
            result[i] = new PresetInfo(dbresult.getString(0), dbresult.getString(1), dbresult.getString(2), dbresult.getString(3), dbresult.getInt(4) == 1,
                    dbresult.getInt(5) == 1);
            dbresult.moveToNext();
        }
        dbresult.close();
        db.close();
        return result;
    }

    /**
     * Fetches all Presets matching the given ID, or all Presets if id is null
     * 
     * @param value null to fetch all Presets, or Preset-ID/URL to fetch a specific one
     * @param byURL if false, value represents an ID, if true, value represents an URL
     * @return PresetInfo[]
     */
    @NonNull
    private synchronized PresetInfo[] getPresets(@Nullable String value, boolean byURL) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor dbresult = db.query(PRESETS_TABLE, new String[] { ID_FIELD, "name", "url", "lastupdate", "active", "usetranslations" },
                value == null ? null : (byURL ? "url = ?" : "id = ?"), value == null ? null : new String[] { value }, null, null, POSITION_FIELD);
        PresetInfo[] result = new PresetInfo[dbresult.getCount()];
        dbresult.moveToFirst();
        for (int i = 0; i < result.length; i++) {
            result[i] = new PresetInfo(dbresult.getString(0), dbresult.getString(1), dbresult.getString(2), dbresult.getString(3), dbresult.getInt(4) == 1,
                    dbresult.getInt(5) == 1);
            dbresult.moveToNext();
        }
        dbresult.close();
        db.close();
        return result;
    }

    /**
     * adds a new Preset with the given values to the Preset database
     * 
     * @param id the id
     * @param name the name of the new Preset
     * @param url any url for the preset
     * @param active if true this will be included in the currently avaliable presets
     */
    public synchronized void addPreset(@NonNull String id, @NonNull String name, @NonNull String url, boolean active) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ID_FIELD, id);
        values.put("name", name);
        values.put("url", url);
        values.put("active", active ? 1 : 0);
        long count = DatabaseUtils.queryNumEntries(db, PRESETS_TABLE);
        values.put(POSITION_FIELD, count);
        db.insert(PRESETS_TABLE, null, values);
        db.close();
    }

    /**
     * Updates the information (name &amp; URL) of a Preset
     * 
     * @param id the internal id
     * @param name the name
     * @param url the url
     * @param useTranslations if true use the incldued translations
     */
    public synchronized void setPresetInfo(@NonNull String id, @NonNull String name, @NonNull String url, boolean useTranslations) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("url", url);
        values.put("usetranslations", useTranslations ? 1 : 0);
        db.update(PRESETS_TABLE, values, "id = ?", new String[] { id });
        db.close();
    }

    /**
     * Sets the lastupdate value of the given preset to now
     * 
     * @param id the ID of the preset to update
     */
    public synchronized void setPresetLastupdateNow(@NonNull String id) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("lastupdate", ((Long) System.currentTimeMillis()).toString());
        db.update(PRESETS_TABLE, values, "id = ?", new String[] { id });
        db.close();
    }

    /**
     * Sets the active value of the given preset
     * 
     * @param id the ID of the preset to update
     * @param active state to set, active if true
     */
    public synchronized void setPresetState(@NonNull String id, boolean active) {
        Log.d(LOGTAG, "Setting pref " + id + " active to " + active);
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("active", active ? 1 : 0);
        db.update(PRESETS_TABLE, values, "id = ?", new String[] { id });
        db.close();
    }

    /**
     * Deletes a preset including the corresponding preset data directory
     * 
     * @param id id of the preset to delete
     */
    public synchronized void deletePreset(@NonNull String id) {
        if (ID_DEFAULT.equals(id)) {
            throw new IllegalOperationException("Cannot delete default");
        }
        SQLiteDatabase db = getWritableDatabase();
        db.delete(PRESETS_TABLE, "id = ?", new String[] { id });
        // need to renumber after deleting
        Cursor dbresult = db.query(PRESETS_TABLE, new String[] { ID_FIELD }, null, null, null, null, POSITION_FIELD);
        dbresult.moveToFirst();
        int count = dbresult.getCount();
        for (int i = 0; i < count; i++) {
            ContentValues values = new ContentValues();
            values.put(POSITION_FIELD, i);
            db.update(PRESETS_TABLE, values, "id = ?", new String[] { dbresult.getString(0) });
            dbresult.moveToNext();
        }
        dbresult.close();
        db.close();
        removePresetDirectory(id);
        if (id.equals(getCurrentAPI().preset)) {
            App.resetPresets();
        }
    }

    /**
     * Move a preset to a new position
     * 
     * @param oldPos index of old position
     * @param newPos index of new position
     */
    public synchronized void movePreset(int oldPos, int newPos) {
        if (oldPos == newPos) {
            return;
        }
        SQLiteDatabase db = getWritableDatabase();
        Cursor dbresult = db.query(PRESETS_TABLE, new String[] { ID_FIELD }, null, null, null, null, POSITION_FIELD);
        dbresult.moveToFirst();
        int count = dbresult.getCount();
        for (int i = 0; i < count; i++) {
            ContentValues values = new ContentValues();
            if (i == oldPos) {
                values.put(POSITION_FIELD, newPos);
            } else if (oldPos < newPos) { // moving down
                if (i < oldPos || i > newPos) {
                    dbresult.moveToNext();
                    continue;
                }
                values.put(POSITION_FIELD, i - 1); // move everything in between up
            } else {
                if (i > oldPos || i < newPos) {
                    dbresult.moveToNext();
                    continue;
                }
                values.put(POSITION_FIELD, i + 1); // move everything in between down
            }
            db.update(PRESETS_TABLE, values, "id = ?", new String[] { dbresult.getString(0) });
            dbresult.moveToNext();
        }
        dbresult.close();
        db.close();
    }

    /**
     * Data structure class for Preset data
     * 
     * @author Jan
     */
    public class PresetInfo {
        public final String  id;
        public final String  name;
        public final String  url;
        /** Timestamp (long, millis since epoch) when this preset was last downloaded */
        public final long    lastupdate;
        public final boolean active;
        public final boolean useTranslations;

        /**
         * Construnct a new configuration for a Preset
         * 
         * @param id the Preset id
         * @param name the Preset name
         * @param url an url or an empty string
         * @param lastUpdate time and date of last update in milliseconds since the epoch
         * @param active true if the Preset is active
         * @param useTranslations if true tranlsations included with the preset will be used
         */
        public PresetInfo(@NonNull String id, @NonNull String name, @NonNull String url, @NonNull String lastUpdate, boolean active, boolean useTranslations) {
            this.id = id;
            this.name = name;
            this.url = url;
            long tmpLastupdate;
            try {
                tmpLastupdate = Long.parseLong(lastUpdate);
            } catch (Exception e) {
                tmpLastupdate = 0;
            }
            this.lastupdate = tmpLastupdate;
            this.active = active;
            this.useTranslations = useTranslations;
        }
    }

    /**
     * Gets the preset data path for a preset with the given ID
     * 
     * @param id the id for the Preset
     * @return the preset data path
     */
    @NonNull
    public File getPresetDirectory(@Nullable String id) {
        if (id == null || id.equals("")) {
            throw new IllegalOperationException("Attempted to get folder for null or empty id!");
        }
        File rootDir = context.getFilesDir();
        return new File(rootDir, id);
    }

    /**
     * Removes the data directory belonging to a preset
     * 
     * @param id the preset ID of the preset whose directory is going to be deleted
     */
    public void removePresetDirectory(@NonNull String id) {
        File presetDir = getPresetDirectory(id);
        if (presetDir.isDirectory()) {
            killDirectory(presetDir);
        }
    }

    /**
     * Deletes all files inside a directory, then the directory itself (one level only, no recursion)
     * 
     * @param dir the directory to empty and delete
     */
    private void killDirectory(@NonNull File dir) {
        if (!dir.isDirectory()) {
            throw new IllegalOperationException("This function only deletes directories");
        }
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (!f.delete()) {
                    Log.e(LOGTAG, "Could not delete " + f.getAbsolutePath());
                }
            }
        }
        if (!dir.delete()) {
            Log.e(LOGTAG, "Could not delete " + dir.getAbsolutePath());
        }
    }

    public enum GeocoderType {
        NOMINATIM, PHOTON
    }

    /**
     * Data structure class for geocoders
     */
    public class Geocoder {
        public final String       id;
        public final String       name;
        public final GeocoderType type;
        public final int          version;
        public final String       url;
        public final boolean      active;

        /**
         * Construct a new class describing a Geocode
         * 
         * @param id internal id
         * @param name the name
         * @param type the type
         * @param version version of the API
         * @param url url for the API
         * @param active if true the entry is in use
         */
        public Geocoder(String id, String name, GeocoderType type, int version, String url, boolean active) {
            this.id = id;
            this.type = type;
            this.version = version;
            this.name = name;
            this.url = url;
            this.active = active;
        }
    }

    /**
     * Get all currently known Geocoders
     * 
     * @return an array of Geocoder objects
     */
    @NonNull
    public Geocoder[] getGeocoders() {
        return getGeocoders(null);
    }

    /**
     * Fetches all Geocoders matching the given ID, or all Geocoders if id is null
     * 
     * @param id null to fetch all Geocoders, or the id to fetch a specific one
     * @return an array of Geocoder objects
     */
    @NonNull
    private synchronized Geocoder[] getGeocoders(@Nullable String id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor dbresult = db.query("geocoders", new String[] { ID_FIELD, "name", "type", "version", "url", "active" }, id == null ? null : "id = ?",
                id == null ? null : new String[] { id }, null, null, null);
        Geocoder[] result = new Geocoder[dbresult.getCount()];
        dbresult.moveToFirst();
        for (int i = 0; i < result.length; i++) {
            result[i] = new Geocoder(dbresult.getString(0), dbresult.getString(1), GeocoderType.valueOf(dbresult.getString(2)), dbresult.getInt(3),
                    dbresult.getString(4), dbresult.getInt(5) == 1);
            dbresult.moveToNext();
        }
        dbresult.close();
        db.close();
        return result;
    }

    /**
     * Fetches all active Geocoders
     * 
     * @return Geocoder[]
     */
    public synchronized Geocoder[] getActiveGeocoders() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor dbresult = db.query("geocoders", new String[] { ID_FIELD, "name", "type", "version", "url", "active" }, "active = 1", null, null, null, null);
        Geocoder[] result = new Geocoder[dbresult.getCount()];
        dbresult.moveToFirst();
        for (int i = 0; i < result.length; i++) {
            result[i] = new Geocoder(dbresult.getString(0), dbresult.getString(1), GeocoderType.valueOf(dbresult.getString(2)), dbresult.getInt(3),
                    dbresult.getString(4), dbresult.getInt(5) == 1);
            dbresult.moveToNext();
        }
        dbresult.close();
        db.close();
        return result;
    }

    /**
     * Add a new Geocoder with the given values to the database
     * 
     * Opens the existing or creates the database
     * 
     * @param id id of the geocoder
     * @param name name used for display purposes
     * @param type type (Nominatim, Photon)
     * @param version version of the geocoder
     * @param url geocoder API url
     * @param active use this geocoder
     */
    public synchronized void addGeocoder(@NonNull String id, String name, GeocoderType type, int version, String url, boolean active) {
        SQLiteDatabase db = getWritableDatabase();
        addGeocoder(db, id, name, type, version, url, active);
        db.close();
    }

    /**
     * Add a new Geocoder with the given values to the database
     * 
     * @param db database to use
     * @param id id of the geocoder
     * @param name name used for display purposes
     * @param type type (Nominatim, Photon)
     * @param version version of the geocoder
     * @param url geocoder API url
     * @param active use this geocoder if true
     */
    private synchronized void addGeocoder(@NonNull SQLiteDatabase db, @NonNull String id, String name, GeocoderType type, int version, String url,
            boolean active) {
        ContentValues values = new ContentValues();
        values.put(ID_FIELD, id);
        values.put("name", name);
        values.put("type", type.name());
        values.put("version", version);
        values.put("url", url);
        values.put("active", active ? 1 : 0);
        db.insert("geocoders", null, values);
    }

    /**
     * Update the specified geocoder
     * 
     * @param id the ID of the geocoder to update
     * @param name name used for display purposes
     * @param type type (Nominatim, Photon)
     * @param version version of the geocoder
     * @param url geocoder API url
     * @param active use this geocoder if true
     */
    public synchronized void updateGeocoder(@NonNull String id, String name, GeocoderType type, int version, String url, boolean active) {
        Log.d(LOGTAG, "Setting geocoder " + id + " active to " + active);
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("type", type.name());
        values.put("version", version);
        values.put("url", url);
        values.put("active", active ? 1 : 0);
        db.update("geocoders", values, "id = ?", new String[] { id });
        db.close();
    }

    /**
     * Sets the active value of the given geocoder
     * 
     * @param id the ID of the geocoder to update
     * @param active use this geocoder if true
     */
    public synchronized void setGeocoderState(@NonNull String id, boolean active) {
        Log.d(LOGTAG, "Setting pref " + id + " active to " + active);
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("active", active ? 1 : 0);
        db.update("geocoders", values, "id = ?", new String[] { id });
        db.close();
    }

    /**
     * Deletes a geocoder entry
     * 
     * @param id id of the geocoder to delete
     */
    public synchronized void deleteGeocoder(@NonNull String id) {
        if (id.equals(ID_DEFAULT_GEOCODER_NOMINATIM)) {
            throw new IllegalOperationException("Cannot delete default");
        }
        SQLiteDatabase db = getWritableDatabase();
        db.delete("geocoders", "id = ?", new String[] { id });
        db.close();
    }
}
