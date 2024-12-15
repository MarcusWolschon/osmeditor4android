package de.blau.android.prefs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

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
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import de.blau.android.R;
import de.blau.android.contract.Files;
import de.blau.android.contract.Paths;
import de.blau.android.contract.Urls;
import de.blau.android.exception.IllegalOperationException;
import de.blau.android.layer.LayerConfig;
import de.blau.android.layer.LayerType;
import de.blau.android.osm.Server;
import de.blau.android.prefs.API.Auth;
import de.blau.android.presets.AutoPreset;
import de.blau.android.presets.Preset;
import de.blau.android.propertyeditor.CustomPreset;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.util.FileUtil;
import de.blau.android.util.ScreenMessage;

/**
 * This class provides access to complex settings like OSM APIs which consist of complex/relational data. WARNING: It
 * has nothing to do with the "Advanced preferences" the user sees in the menu; those are just a separate
 * PreferenceScreen defined in the preferences.xml and handled like normal prefs!
 * 
 * @author Jan
 * @author Simon Poole
 */
public class AdvancedPrefDatabase extends SQLiteOpenHelper implements AutoCloseable {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, AdvancedPrefDatabase.class.getSimpleName().length());
    private static final String DEBUG_TAG = AdvancedPrefDatabase.class.getSimpleName().substring(0, TAG_LEN);

    private final Resources         r;
    private final SharedPreferences sharedPrefs;
    private final String            selectedApi;

    public static final String DATABASE_NAME = "AdvancedPrefs";

    private static final int DATA_VERSION = 17;

    /** The ID string for the default API and the default Preset */
    public static final String ID_DEFAULT = "default";
    public static final String ID_SANDBOX = "sandbox";

    private static final String GEOCODERS_TABLE               = "geocoders";
    private static final String ID_DEFAULT_GEOCODER_NOMINATIM = "Nominatim";
    private static final String ID_DEFAULT_GEOCODER_PHOTON    = "Photon";
    private static final String VERSION_COL                   = "version";

    private static final String PRESETS_TABLE        = "presets";
    private static final String ID_COL               = "id";
    private static final String POSITION_COL         = "position";
    private static final String NAME_COL             = "name";
    private static final String URL_COL              = "url";
    private static final String USETRANSLATIONS_COL  = "usetranslations";
    private static final String LASTUPDATE_COL       = "lastupdate";
    private static final String ACTIVE_COL           = "active";
    private static final String SHORTDESCRIPTION_COL = "shortdescription";
    private static final String DESCRIPTION_COL      = "description";

    private static final String APIS_TABLE            = "apis";
    private static final String ACCESSTOKENSECRET_COL = "accesstokensecret";
    private static final String ACCESSTOKEN_COL       = "accesstoken";
    private static final String AUTH_COL              = "oauth";
    private static final String NOTESURL_COL          = "notesurl";
    private static final String READONLYURL_COL       = "readonlyurl";
    private static final String PASS_COL              = "pass";
    private static final String USER_COL              = "user";

    private static final String LAYERS_TABLE   = "layers";
    private static final String VISIBLE_COL    = "visible";
    private static final String TYPE_COL       = "type";
    private static final String CONTENT_ID_COL = "content_id";

    private static final String ROWID = "rowid";

    private static final String WHERE_ID    = "id = ?";
    private static final String WHERE_ROWID = "rowid = ?";

    private static final String CANNOT_DELETE_DEFAULT = "Cannot delete default";

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
        super(context.getApplicationContext(), DATABASE_NAME, null, DATA_VERSION); // always use the application
                                                                                   // context
        this.context = context;
        r = context.getResources();
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        selectedApi = r.getString(R.string.config_selected_api);
        currentAPI = sharedPrefs.getString(selectedApi, null);
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
                "CREATE TABLE presets (id TEXT, name TEXT, url TEXT, version TEXT DEFAULT NULL, shortdescription TEXT DEFAULT NULL, description TEXT DEFAULT NULL, lastupdate TEXT, data TEXT, position INTEGER DEFAULT 0, active INTEGER DEFAULT 0, usetranslations INTEGER DEFAULT 1)");
        db.execSQL("CREATE TABLE geocoders (id TEXT, type TEXT, version INTEGER DEFAULT 0, name TEXT, url TEXT, active INTEGER DEFAULT 0)");
        addGeocoder(db, ID_DEFAULT_GEOCODER_NOMINATIM, ID_DEFAULT_GEOCODER_NOMINATIM, GeocoderType.NOMINATIM, 0, Urls.DEFAULT_NOMINATIM_SERVER, true);
        addGeocoder(db, ID_DEFAULT_GEOCODER_PHOTON, ID_DEFAULT_GEOCODER_PHOTON, GeocoderType.PHOTON, 0, Urls.DEFAULT_PHOTON_SERVER, true);
        db.execSQL("CREATE TABLE layers (type TEXT, position INTEGER DEFAULT -1, visible INTEGER DEFAULT 1, content_id TEXT)");
        addLayer(db, 0, LayerType.IMAGERY, true, TileLayerSource.LAYER_MAPNIK);
        addLayer(db, 1, LayerType.SCALE);
        addLayer(db, 2, LayerType.OSMDATA);
        addLayer(db, 3, LayerType.TASKS);
    }

    @Override
    public synchronized void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(DEBUG_TAG, "Upgrading Preferences DB");
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
            addAPI(db, ID_SANDBOX, Urls.DEFAULT_SANDBOX_API_NAME, Urls.DEFAULT_SANDBOX_API, null, "", "", ID_SANDBOX, Auth.OAUTH1A);
        }
        if (oldVersion <= 9 && newVersion >= 10) {
            db.execSQL("ALTER TABLE presets ADD COLUMN position INTEGER DEFAULT 0");
            renumberPresets(db);
        }
        if (oldVersion <= 10 && newVersion >= 11) {
            db.execSQL("ALTER TABLE presets ADD COLUMN usetranslations INTEGER DEFAULT 1");
        }
        if (oldVersion <= 11 && newVersion >= 12) {
            try {
                FileUtil.copyFileFromAssets(context, "images/" + CustomPreset.ICON,
                        FileUtil.getPublicDirectory(FileUtil.getPublicDirectory(), Paths.DIRECTORY_PATH_AUTOPRESET), CustomPreset.ICON);
            } catch (IOException e) {
                Log.e(DEBUG_TAG, "Unable to copy custom preset icon");
            }
        }
        if (oldVersion <= 12 && newVersion >= 13) {
            db.execSQL("CREATE TABLE layers (type TEXT, position INTEGER DEFAULT -1, visible INTEGER DEFAULT 1, content_id TEXT)");
            int position = 0;
            String backgroundLayer = sharedPrefs.getString(r.getString(R.string.config_backgroundLayer_key), TileLayerSource.LAYER_NONE);
            if (!TileLayerSource.LAYER_NONE.equals(backgroundLayer)) {
                addLayer(db, position++, LayerType.IMAGERY, true, backgroundLayer);
            }
            String overlayLayer = sharedPrefs.getString(r.getString(R.string.config_overlayLayer_key), TileLayerSource.LAYER_NOOVERLAY);
            if (!TileLayerSource.LAYER_NOOVERLAY.equals(overlayLayer)) {
                addLayer(db, position++, LayerType.OVERLAYIMAGERY, true, overlayLayer);
            }
            final String scaleNone = r.getString(R.string.scale_none);
            String scaleLayer = sharedPrefs.getString(r.getString(R.string.config_scale_key), scaleNone);
            if (!scaleNone.equals(scaleLayer)) {
                addLayer(db, position++, LayerType.SCALE);
            }
            addLayer(db, position++, LayerType.OSMDATA);
            // can't automatically add an existing geojson layer
            if (sharedPrefs.getBoolean(r.getString(R.string.config_enableOpenStreetBugs_key), true)) {
                addLayer(db, position, LayerType.TASKS);
            }
        }
        if (oldVersion <= 13 && newVersion >= 14) {
            db.execSQL("UPDATE geocoders SET url='" + Urls.DEFAULT_PHOTON_SERVER + "' WHERE url='https://photon.komoot.de/'");
        }
        if (oldVersion <= 14 && newVersion >= 15) {
            // hack to fix offset server preference
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String offsetServerKey = r.getString(R.string.config_offsetServer_key);
            if ("https://offsets.textual.ru/".equals(prefs.getString(offsetServerKey, Urls.DEFAULT_OFFSET_SERVER))) {
                Log.w(DEBUG_TAG, "fixing up offset server url");
                prefs.edit().putString(offsetServerKey, Urls.DEFAULT_OFFSET_SERVER).commit();
            }
        }
        if (oldVersion <= 15 && newVersion >= 16) {
            db.execSQL("ALTER TABLE presets ADD COLUMN version TEXT DEFAULT NULL");
            db.execSQL("ALTER TABLE presets ADD COLUMN shortdescription TEXT DEFAULT NULL");
            db.execSQL("ALTER TABLE presets ADD COLUMN description TEXT DEFAULT NULL");
        }
        if (oldVersion <= 16 && newVersion >= 17) {
            // force migrate the default API entries as now the old ones will definitely not work any more
            final int oauth2 = Auth.OAUTH2.ordinal();
            db.execSQL("UPDATE apis SET oauth=" + oauth2 + ", accesstokensecret=NULL, accesstoken=NULL WHERE id='" + ID_DEFAULT + "' AND NOT oauth=" + oauth2);
            db.execSQL("UPDATE apis SET oauth=" + oauth2 + ", accesstokensecret=NULL, accesstoken=NULL WHERE id='" + ID_SANDBOX + "' AND NOT oauth=" + oauth2);
        }
    }

    @Override
    public synchronized void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(DEBUG_TAG, "Downgrading API DB loosing all settings");
        db.execSQL("DROP TABLE apis");
        db.execSQL("DROP TABLE presets");
        db.execSQL("DROP TABLE geocoders");
        db.execSQL("DROP TABLE layers");
        onCreate(db);
        migrateAPI(db);
    }

    /**
     * Creates the default API entry using the old-style username/password
     * 
     * @param db an instance of the pref database
     */
    private synchronized void migrateAPI(@NonNull SQLiteDatabase db) {
        Log.d(DEBUG_TAG, "Migrating API");
        String user = sharedPrefs.getString(r.getString(R.string.config_username_key), "");
        String pass = sharedPrefs.getString(r.getString(R.string.config_password_key), "");
        Log.d(DEBUG_TAG, "Adding default URL with user '" + user + "'");
        addAPI(db, ID_DEFAULT, Urls.DEFAULT_API_NAME, Urls.DEFAULT_API, null, user, pass, ID_DEFAULT, Auth.OAUTH2);
        Log.d(DEBUG_TAG, "Adding default dev URL");
        addAPI(db, ID_SANDBOX, Urls.DEFAULT_SANDBOX_API_NAME, Urls.DEFAULT_SANDBOX_API, null, "", "", ID_SANDBOX, Auth.OAUTH2);
        Log.d(DEBUG_TAG, "Selecting default API");
        selectAPI(db, ID_DEFAULT);
        Log.d(DEBUG_TAG, "Deleting old user/pass settings");
        Editor editor = sharedPrefs.edit();
        editor.remove(r.getString(R.string.config_username_key));
        editor.remove(r.getString(R.string.config_password_key));
        editor.commit();
        Log.d(DEBUG_TAG, "Migration finished");
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
        Log.d(DEBUG_TAG, "Selecting API with ID: " + id);
        if (getAPIs(db, id).length == 0) {
            throw new IllegalOperationException("Non-existant API selected");
        }
        sharedPrefs.edit().putString(selectedApi, id).commit();
        currentAPI = id;
        resetCurrentServer();
    }

    /**
     * Rest the current Server object, closing any MapSplit source first
     */
    public static void resetCurrentServer() {
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
            Log.e(DEBUG_TAG, "Current API was null, selecting default");
            selectAPI(ID_DEFAULT);
            api = getCurrentAPI();
            if (api == null) {
                Log.e(DEBUG_TAG, "Couldn't find default server api, fatal error");
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
     * @param auth Authentication method
     */
    public synchronized void setAPIDescriptors(@NonNull String id, @NonNull String name, @NonNull String url, @Nullable String readonlyurl,
            @Nullable String notesurl, @NonNull Auth auth) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(NAME_COL, name);
        values.put(URL_COL, url);
        values.put(READONLYURL_COL, readonlyurl);
        values.put(NOTESURL_COL, notesurl);
        values.put(AUTH_COL, auth.ordinal());
        db.update(APIS_TABLE, values, WHERE_ID, new String[] { id });
        if (auth == Auth.BASIC) { // zap any key and secret
            values = new ContentValues();
            values.put(ACCESSTOKEN_COL, (String) null);
            values.put(ACCESSTOKENSECRET_COL, (String) null);
            db.update(APIS_TABLE, values, WHERE_ID, new String[] { id });
        }
        db.close();
        resetCurrentServer();
    }

    /**
     * Sets OAuth access token and secret of the API entries with the same URL and authentication method as the current
     * entry
     * 
     * @param token the OAuth token
     * @param secret the OAuth secret
     */
    public synchronized void setAPIAccessToken(@Nullable String token, @Nullable String secret) {
        API api = getCurrentAPI();
        if (api == null) {
            throw new IllegalStateException("Couldn't find current server api, fatal error");
        }
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ACCESSTOKEN_COL, token);
        values.put(ACCESSTOKENSECRET_COL, secret);
        db.update(APIS_TABLE, values, "url= ? AND oauth= ?", new String[] { api.url, Integer.toString(api.auth.ordinal()) });
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
        values.put(USER_COL, user);
        values.put(PASS_COL, pass);
        db.update(APIS_TABLE, values, WHERE_ID, new String[] { currentAPI });
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
     * @param auth authentication method
     */
    public synchronized void addAPI(@NonNull String id, @NonNull String name, @NonNull String url, @Nullable String readonlyurl, @Nullable String notesurl,
            @Nullable String user, @Nullable String pass, @NonNull Auth auth) {
        SQLiteDatabase db = getWritableDatabase();
        addAPI(db, id, name, url, readonlyurl, notesurl, user, pass, auth);
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
     * @param auth authentication method
     */
    private synchronized void addAPI(@NonNull SQLiteDatabase db, @NonNull String id, @NonNull String name, @NonNull String url, @Nullable String readonlyurl,
            @Nullable String notesurl, @Nullable String user, @Nullable String pass, @NonNull Auth auth) {
        ContentValues values = new ContentValues();
        values.put(ID_COL, id);
        values.put(NAME_COL, name);
        values.put(URL_COL, url);
        values.put(READONLYURL_COL, readonlyurl);
        values.put(NOTESURL_COL, notesurl);
        values.put(USER_COL, user);
        values.put(PASS_COL, pass);
        values.put(AUTH_COL, auth.ordinal());
        db.insert(APIS_TABLE, null, values);
    }

    /**
     * Removes an API from the API database
     * 
     * @param id id of the API we want to delete
     */
    public synchronized void deleteAPI(@NonNull final String id) {
        if (id.equals(ID_DEFAULT)) {
            throw new IllegalOperationException(CANNOT_DELETE_DEFAULT);
        }
        if (id.equals(currentAPI)) {
            selectAPI(ID_DEFAULT);
        }
        SQLiteDatabase db = getWritableDatabase();
        db.delete(APIS_TABLE, WHERE_ID, new String[] { id });
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
        Cursor dbresult = db.query(
                APIS_TABLE, new String[] { ID_COL, NAME_COL, URL_COL, READONLYURL_COL, NOTESURL_COL, USER_COL, PASS_COL, "preset", "showicon", AUTH_COL,
                        ACCESSTOKEN_COL, ACCESSTOKENSECRET_COL },
                id == null ? null : WHERE_ID, id == null ? null : new String[] { id }, null, null, null, null);
        API[] result = new API[dbresult.getCount()];
        dbresult.moveToFirst();
        for (int i = 0; i < result.length; i++) {
            Auth auth = Auth.BASIC;
            try {
                auth = API.Auth.values()[dbresult.getInt(9)];
            } catch (IndexOutOfBoundsException ex) {
                Log.e(DEBUG_TAG, "No auth method for " + dbresult.getInt(9));
            }
            result[i] = new API(dbresult.getString(0), dbresult.getString(1), dbresult.getString(2), dbresult.getString(3), dbresult.getString(4),
                    dbresult.getString(5), dbresult.getString(6), auth, dbresult.getString(10), dbresult.getString(11));
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
            try (Cursor dbresult = readableDb.query(APIS_TABLE, new String[] { ID_COL, READONLYURL_COL }, "readonlyurl LIKE ?", new String[] { queryUri }, null,
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
    @NonNull
    public Preset[] getCurrentPresetObject() {
        long start = System.currentTimeMillis();
        PresetInfo[] presetInfos = getActivePresets();

        Preset[] activePresets = new Preset[presetInfos.length + 1];
        for (int i = 0; i < presetInfos.length; i++) {
            PresetInfo pi = presetInfos[i];
            try {
                Log.d(DEBUG_TAG, "Adding preset " + pi.name);
                activePresets[i] = new Preset(context, getPresetDirectory(pi.id), pi.useTranslations);
                Preset preset = activePresets[i];
                if (preset != null) {
                    setAdditionalFieldsFromPreset(pi, preset);
                }
            } catch (Exception e) {
                Log.e(DEBUG_TAG, "Failed to create preset", e);
                ScreenMessage.toastTopError(context, context.getString(R.string.toast_preset_failed, pi.name, e.getLocalizedMessage()));
                activePresets[i] = null;
            }
        }
        int autopresetPosition = activePresets.length - 1;
        try {
            AutoPreset.readAutoPreset(context, activePresets, autopresetPosition);
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Failed to find auto-preset, creating", e);
            createEmptyAutoPreset(context, activePresets, autopresetPosition);
        }
        Log.d(DEBUG_TAG, "Elapsed time to read presets " + (System.currentTimeMillis() - start) / 1000);
        return activePresets;
    }

    /**
     * Set the additional preset fields from a parsed preset (if they have changed)
     * 
     * @param pi a PresetInfo object with the current DB values
     * @param preset a parsed Preset
     */
    private void setAdditionalFieldsFromPreset(@NonNull PresetInfo pi, @NonNull Preset preset) {
        boolean versionChanged = preset.getVersion() != null && !preset.getVersion().equals(pi.version);
        boolean shortDescriptionChanged = preset.getShortDescription() != null && !preset.getShortDescription().equals(pi.description);
        boolean descriptionChanged = preset.getDescription() != null && !preset.getDescription().equals(pi.description);
        if (versionChanged || shortDescriptionChanged || descriptionChanged) {
            setPresetAdditionalFields(pi.id, preset.getVersion(), preset.getShortDescription(), preset.getDescription());
        }
    }

    /**
     * Create an empty AutoPreset from template
     * 
     * @param context an Android Context
     * @param activePresets an array holding the current active presets
     * @param autopresetPosition the position where the new preset should go
     */
    public static void createEmptyAutoPreset(@NonNull Context context, @NonNull Preset[] activePresets, int autopresetPosition) {
        try {
            FileUtil.copyFileFromAssets(context, Files.FILE_NAME_AUTOPRESET_TEMPLATE,
                    FileUtil.getPublicDirectory(FileUtil.getPublicDirectory(), Paths.DIRECTORY_PATH_AUTOPRESET), Files.FILE_NAME_AUTOPRESET);
            AutoPreset.readAutoPreset(context, activePresets, autopresetPosition);
        } catch (Exception e1) {
            Log.e(DEBUG_TAG, "Failed to create auto-preset", e1);
            activePresets[autopresetPosition] = null;
        }
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
        Cursor dbresult = db.query(PRESETS_TABLE,
                new String[] { ID_COL, NAME_COL, VERSION_COL, SHORTDESCRIPTION_COL, DESCRIPTION_COL, URL_COL, LASTUPDATE_COL, ACTIVE_COL, USETRANSLATIONS_COL },
                "active=1", null, null, null, POSITION_COL);
        PresetInfo[] result = new PresetInfo[dbresult.getCount()];
        Log.d(DEBUG_TAG, "#prefs " + result.length);
        dbresult.moveToFirst();
        for (int i = 0; i < result.length; i++) {
            Log.d(DEBUG_TAG, "Reading pref " + i + " " + dbresult.getString(1));
            result[i] = new PresetInfo(dbresult.getString(0), dbresult.getString(1), dbresult.getString(2), dbresult.getString(3), dbresult.getString(4),
                    dbresult.getString(5), dbresult.getString(6), dbresult.getInt(7) == 1, dbresult.getInt(8) == 1);
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
        Cursor dbresult = db.query(PRESETS_TABLE,
                new String[] { ID_COL, NAME_COL, VERSION_COL, SHORTDESCRIPTION_COL, DESCRIPTION_COL, URL_COL, LASTUPDATE_COL, ACTIVE_COL, USETRANSLATIONS_COL },
                value == null ? null : (byURL ? "url = ?" : WHERE_ID), value == null ? null : new String[] { value }, null, null, POSITION_COL);
        PresetInfo[] result = new PresetInfo[dbresult.getCount()];
        dbresult.moveToFirst();
        for (int i = 0; i < result.length; i++) {
            result[i] = new PresetInfo(dbresult.getString(0), dbresult.getString(1), dbresult.getString(2), dbresult.getString(3), dbresult.getString(4),
                    dbresult.getString(5), dbresult.getString(6), dbresult.getInt(7) == 1, dbresult.getInt(8) == 1);
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
        values.put(ID_COL, id);
        values.put(NAME_COL, name);
        values.put(URL_COL, url);
        values.put(ACTIVE_COL, active ? 1 : 0);
        long count = DatabaseUtils.queryNumEntries(db, PRESETS_TABLE);
        values.put(POSITION_COL, count);
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
        values.put(NAME_COL, name);
        values.put(URL_COL, url);
        values.put(USETRANSLATIONS_COL, useTranslations ? 1 : 0);
        db.update(PRESETS_TABLE, values, WHERE_ID, new String[] { id });
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
        values.put(LASTUPDATE_COL, ((Long) System.currentTimeMillis()).toString());
        db.update(PRESETS_TABLE, values, WHERE_ID, new String[] { id });
        db.close();
    }

    /**
     * Sets information that requires parsing the preset first
     * 
     * @param id the ID of the preset to update
     * @param version the version if null this will not be updated
     * @param shortDescription the short description if null this will not be updated
     * @param description the description if null this will not be updated
     */
    public synchronized void setPresetAdditionalFields(@NonNull String id, @Nullable String version, @Nullable String shortDescription,
            @Nullable String description) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        if (version != null) {
            values.put(VERSION_COL, version);
        }
        if (shortDescription != null) {
            values.put(SHORTDESCRIPTION_COL, shortDescription);
        }
        if (description != null) {
            values.put(DESCRIPTION_COL, description);
        }
        if (values.size() != 0) { // isEmpty was added in API 30
            db.update(PRESETS_TABLE, values, WHERE_ID, new String[] { id });
        }
        db.close();
    }

    /**
     * Sets the active value of the given preset
     * 
     * @param id the ID of the preset to update
     * @param active state to set, active if true
     */
    public synchronized void setPresetState(@NonNull String id, boolean active) {
        Log.d(DEBUG_TAG, "Setting preset " + id + " active state to " + active);
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ACTIVE_COL, active ? 1 : 0);
        db.update(PRESETS_TABLE, values, WHERE_ID, new String[] { id });
        db.close();
    }

    /**
     * Deletes a preset including the corresponding preset data directory
     * 
     * @param id id of the preset to delete
     */
    public synchronized void deletePreset(@NonNull String id) {
        if (ID_DEFAULT.equals(id)) {
            throw new IllegalOperationException(CANNOT_DELETE_DEFAULT);
        }
        SQLiteDatabase db = getWritableDatabase();
        db.delete(PRESETS_TABLE, WHERE_ID, new String[] { id });
        // need to renumber after deleting
        renumberPresets(db);
        db.close();
        removePresetDirectory(id);
    }

    /**
     * Renumber the preset table
     * 
     * @param db the database
     */
    private void renumberPresets(@NonNull SQLiteDatabase db) {
        Cursor dbresult = db.query(PRESETS_TABLE, new String[] { ID_COL }, null, null, null, null, POSITION_COL);
        dbresult.moveToFirst();
        int count = dbresult.getCount();
        for (int i = 0; i < count; i++) {
            ContentValues values = new ContentValues();
            values.put(POSITION_COL, i);
            db.update(PRESETS_TABLE, values, WHERE_ID, new String[] { dbresult.getString(0) });
            dbresult.moveToNext();
        }
        dbresult.close();
    }

    /**
     * Move a preset to a new position
     * 
     * @param oldPos index of old position
     * @param newPos index of new position
     */
    public synchronized void movePreset(int oldPos, int newPos) {
        moveRow(PRESETS_TABLE, ID_COL, WHERE_ID, oldPos, newPos);
    }

    /**
     * Data structure class for Preset data
     * 
     * @author Jan
     */
    public class PresetInfo {
        public final String  id;
        public final String  name;
        public final String  version;
        public final String  shortDescription;
        public final String  description;
        public final String  url;
        /** Timestamp (long, millis since epoch) when this preset was last downloaded */
        public final long    lastupdate;
        public final boolean active;
        public final boolean useTranslations;

        /**
         * Construct a new configuration for a Preset
         * 
         * @param id the Preset id
         * @param name the Preset name
         * @param version the preset version
         * @param shortDescription the name the author gave the preset
         * @param description a description of its contents
         * @param url an url or an empty string
         * @param lastUpdate time and date of last update in milliseconds since the epoch
         * @param active true if the Preset is active
         * @param useTranslations if true translations included with the preset will be used
         */
        public PresetInfo(@NonNull String id, @NonNull String name, @Nullable String version, @Nullable String shortDescription, @Nullable String description,
                @NonNull String url, @NonNull String lastUpdate, boolean active, boolean useTranslations) {
            this.id = id;
            this.name = name;
            this.version = version;
            this.shortDescription = shortDescription;
            this.description = description;
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
        if (id == null || "".equals(id)) {
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
                if (!f.delete()) { // NOSONAR requires API 26
                    Log.e(DEBUG_TAG, "Could not delete " + f.getAbsolutePath());
                }
            }
        }
        if (!dir.delete()) { // NOSONAR requires API 26
            Log.e(DEBUG_TAG, "Could not delete " + dir.getAbsolutePath());
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
        Cursor dbresult = db.query(GEOCODERS_TABLE, new String[] { ID_COL, NAME_COL, TYPE_COL, VERSION_COL, URL_COL, ACTIVE_COL }, id == null ? null : WHERE_ID,
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
        Cursor dbresult = db.query(GEOCODERS_TABLE, new String[] { ID_COL, NAME_COL, TYPE_COL, VERSION_COL, URL_COL, ACTIVE_COL }, "active = 1", null, null,
                null, null);
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
        values.put(ID_COL, id);
        values.put(NAME_COL, name);
        values.put(TYPE_COL, type.name());
        values.put(VERSION_COL, version);
        values.put(URL_COL, url);
        values.put(ACTIVE_COL, active ? 1 : 0);
        db.insert(GEOCODERS_TABLE, null, values);
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
        Log.d(DEBUG_TAG, "Setting geocoder " + id + " active to " + active);
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(NAME_COL, name);
        values.put(TYPE_COL, type.name());
        values.put(VERSION_COL, version);
        values.put(URL_COL, url);
        values.put(ACTIVE_COL, active ? 1 : 0);
        db.update(GEOCODERS_TABLE, values, WHERE_ID, new String[] { id });
        db.close();
    }

    /**
     * Sets the active value of the given geocoder
     * 
     * @param id the ID of the geocoder to update
     * @param active use this geocoder if true
     */
    public synchronized void setGeocoderState(@NonNull String id, boolean active) {
        Log.d(DEBUG_TAG, "Setting pref " + id + " active to " + active);
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ACTIVE_COL, active ? 1 : 0);
        db.update(GEOCODERS_TABLE, values, WHERE_ID, new String[] { id });
        db.close();
    }

    /**
     * Deletes a geocoder entry
     * 
     * @param id id of the geocoder to delete
     */
    public synchronized void deleteGeocoder(@NonNull String id) {
        if (id.equals(ID_DEFAULT_GEOCODER_NOMINATIM)) {
            throw new IllegalOperationException(CANNOT_DELETE_DEFAULT);
        }
        SQLiteDatabase db = getWritableDatabase();
        db.delete(GEOCODERS_TABLE, WHERE_ID, new String[] { id });
        db.close();
    }

    /**
     * Get the current layer configuration
     * 
     * @return an array of LayerConfig objects sorted by their position
     */
    public synchronized LayerConfig[] getLayers() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor dbresult = db.query(LAYERS_TABLE, new String[] { POSITION_COL, TYPE_COL, VISIBLE_COL, CONTENT_ID_COL }, null, null, null, null, POSITION_COL);
        LayerConfig[] result = new LayerConfig[dbresult.getCount()];
        dbresult.moveToFirst();
        for (int i = 0; i < result.length; i++) {
            result[i] = new LayerConfig(dbresult.getInt(0), LayerType.valueOf(dbresult.getString(1)), dbresult.getInt(2) == 1, dbresult.getString(3));
            dbresult.moveToNext();
        }
        dbresult.close();
        db.close();
        return result;
    }

    /**
     * Check if a specific layer is configured
     * 
     * @param type the type of the layer
     * @param contentId the content id
     * @return true if the layer exists
     */
    public synchronized boolean hasLayer(@NonNull LayerType type, @NonNull String contentId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor dbresult = db.query(LAYERS_TABLE, new String[] { CONTENT_ID_COL }, TYPE_COL + "= ? and " + CONTENT_ID_COL + "= ?",
                new String[] { type.name(), contentId }, null, null, POSITION_COL);
        boolean result = dbresult.getCount() > 0;
        dbresult.close();
        db.close();
        return result;
    }

    /**
     * Add a layer to the layer list
     * 
     * @param db a writable DB
     * @param position the position of the layer
     * @param type the layer type
     */
    private synchronized void addLayer(@NonNull SQLiteDatabase db, int position, @NonNull LayerType type) {
        addLayer(db, position, type, true, null);
    }

    /**
     * Add a layer to the layer list
     * 
     * @param db a writable DB
     * @param position the position of the layer
     * @param type the layer type
     * @param visible if the layer is visible
     * @param contentId if of any content in the layer
     */
    private synchronized void addLayer(@NonNull SQLiteDatabase db, int position, @NonNull LayerType type, boolean visible, @Nullable String contentId) {
        ContentValues values = new ContentValues();
        values.put(TYPE_COL, type.name());
        values.put(POSITION_COL, position);
        values.put(VISIBLE_COL, visible ? 1 : 0);
        if (contentId != null) {
            values.put(CONTENT_ID_COL, contentId);
        }
        db.insert(LAYERS_TABLE, null, values);
    }

    /**
     * Deletes a layer entry
     * 
     * @param position of the layer
     * @param type the layer type
     */
    public synchronized void deleteLayer(int position, @NonNull LayerType type) {
        checkLayerDeletion(type);
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.delete(LAYERS_TABLE, "position = ? AND type = ?", new String[] { Integer.toString(position), type.name() });
            renumber(db);
        }
    }

    /**
     * Check if we can actually delete this kind of layer
     * 
     * @param type the LayerType
     */
    private void checkLayerDeletion(@Nullable LayerType type) {
        if (LayerType.OSMDATA.equals(type)) {
            throw new IllegalOperationException("Cannot delete osm data layer");
        }
    }

    /**
     * Delete a layer entry / entries
     * 
     * @param type the layer type
     * @param contentId the content id, or null for all of the same type
     */
    public synchronized void deleteLayer(@NonNull LayerType type, @Nullable String contentId) {
        checkLayerDeletion(type);
        try (SQLiteDatabase db = getWritableDatabase()) {
            if (contentId != null) {
                db.delete(LAYERS_TABLE, "content_id = ? AND type = ?", new String[] { contentId, type.name() });
            } else {
                db.delete(LAYERS_TABLE, "type = ?", new String[] { type.name() });
            }
            renumber(db);
        }
    }

    /**
     * Delete a layer entry with NULL content_id
     * 
     * @param type the layer type
     */
    public synchronized void deleteLayer(@NonNull LayerType type) {
        checkLayerDeletion(type);
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.delete(LAYERS_TABLE, "content_id is NULL AND type = ?", new String[] { type.name() });
            renumber(db);
        }
    }

    /**
     * Renumber layer position are deletions
     * 
     * @param db a writable DB
     */
    private void renumber(@NonNull SQLiteDatabase db) {
        // need to renumber after deleting
        try (Cursor dbresult = db.query(LAYERS_TABLE, new String[] { ROWID }, null, null, null, null, POSITION_COL)) {
            dbresult.moveToFirst();
            int count = dbresult.getCount();
            for (int i = 0; i < count; i++) {
                ContentValues values = new ContentValues();
                values.put(POSITION_COL, i);
                db.update(LAYERS_TABLE, values, WHERE_ROWID, new String[] { dbresult.getString(0) });
                dbresult.moveToNext();
            }
        }
    }

    /**
     * Set visibility of a layer
     * 
     * @param position the layer position
     * @param visible true if visible
     */
    public synchronized void setLayerVisibility(@NonNull int position, boolean visible) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(VISIBLE_COL, visible ? 1 : 0);
        db.update(LAYERS_TABLE, values, "position = ?", new String[] { Integer.toString(position) });
        db.close();
    }

    /**
     * Set layer content id
     * 
     * @param position the layer position
     * @param contentId the id
     */
    public synchronized void setLayerContentId(@NonNull int position, @NonNull String contentId) {
        try (SQLiteDatabase db = getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(CONTENT_ID_COL, contentId);
            db.update(LAYERS_TABLE, values, "position = ?", new String[] { Integer.toString(position) });
        }
    }

    /**
     * Insert a layer at position
     * 
     * @param position the position of the layer
     * @param type the layer type
     * @param visible if the layer is visible
     * @param contentId if of any content in the layer
     */
    public synchronized void insertLayer(int position, @NonNull LayerType type, boolean visible, @Nullable String contentId) {
        int tempPos = layerCount();
        try (SQLiteDatabase db = getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(TYPE_COL, type.name());
            values.put(POSITION_COL, tempPos);
            values.put(VISIBLE_COL, visible ? 1 : 0);
            if (contentId != null) {
                values.put(CONTENT_ID_COL, contentId);
            }
            db.insert(LAYERS_TABLE, null, values);
        }
        moveLayer(tempPos, position);
    }

    /**
     * Get the current number of layers
     * 
     * @return the current number of layers
     */
    public int layerCount() {
        try (SQLiteDatabase db = getReadableDatabase(); Cursor dbresult = db.query(LAYERS_TABLE, new String[] { ROWID }, null, null, null, null, null)) {
            return dbresult.getCount();
        }
    }

    /**
     * Move a layer to a new position
     * 
     * @param oldPos index of old position
     * @param newPos index of new position
     */
    public synchronized void moveLayer(int oldPos, int newPos) {
        moveRow(LAYERS_TABLE, ROWID, WHERE_ROWID, oldPos, newPos);
    }

    /**
     * Move a table row to a new position, renumbering at the same time
     * 
     * @param table the table holding the row
     * @param rowId the column holding the row id
     * @param whereId the query for the above
     * @param oldPos index of old position
     * @param newPos index of new position
     */
    private void moveRow(@NonNull String table, @NonNull String rowId, @NonNull String whereId, int oldPos, int newPos) {
        if (oldPos == newPos) {
            return;
        }
        try (SQLiteDatabase db = getWritableDatabase(); Cursor dbresult = db.query(table, new String[] { rowId }, null, null, null, null, POSITION_COL)) {
            dbresult.moveToFirst();
            int count = dbresult.getCount();
            for (int i = 0; i < count; i++) {
                ContentValues values = new ContentValues();
                if (i == oldPos) {
                    values.put(POSITION_COL, newPos);
                } else if (oldPos < newPos) { // moving down
                    if (i < oldPos || i > newPos) {
                        dbresult.moveToNext();
                        continue;
                    }
                    values.put(POSITION_COL, i - 1); // move everything in between up
                } else {
                    if (i > oldPos || i < newPos) {
                        dbresult.moveToNext();
                        continue;
                    }
                    values.put(POSITION_COL, i + 1); // move everything in between down
                }
                db.update(table, values, whereId, new String[] { dbresult.getString(0) });
                dbresult.moveToNext();
            }
        }
    }

}
