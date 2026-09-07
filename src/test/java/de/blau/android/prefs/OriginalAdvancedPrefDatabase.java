package de.blau.android.prefs;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;
import android.util.Log;
import de.blau.android.R;

/**
 * Code from the near original implementation with unsed code removed and methods made private where possible
 * 
 * 
 * 
 * This class provides access to complex settings like OSM APIs which consist of complex/relational data
 * 
 * @author Jan
 */
public class OriginalAdvancedPrefDatabase extends SQLiteOpenHelper {

    private final Resources         r;
    private final SharedPreferences prefs;
    private final String            PREF_SELECTED_API;

    final static int    DATA_VERSION = 2;
    final static String LOGTAG       = "AdvancedPrefDB";

    /** The ID string for the default API and the default Preset */
    private final static String ID_DEFAULT = "default";

    /** The ID of the currently active API */
    private String currentAPI;

    private Context context;

    public OriginalAdvancedPrefDatabase(Context context) {
        super(context, "AdvancedPrefs", null, DATA_VERSION);
        this.context = context;
        this.r = context.getResources();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        PREF_SELECTED_API = r.getString(R.string.config_selected_api);
        currentAPI = prefs.getString(PREF_SELECTED_API, null);
        if (currentAPI == null)
            migrateAPI();
        if (getPreset(ID_DEFAULT) == null)
            addPreset(ID_DEFAULT, "OpenStreetMap", "");
    }

    @Override
    public synchronized void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE apis (id TEXT, name TEXT, url TEXT, user TEXT, pass TEXT, preset TEXT, showicon INTEGER)");
        db.execSQL("CREATE TABLE presets (id TEXT, name TEXT, url TEXT, lastupdate TEXT, data TEXT)");
    }

    @Override
    public synchronized void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion <= 1 && newVersion >= 2) {
            db.execSQL("ALTER TABLE apis ADD COLUMN showicon INTEGER DEFAULT 0");
        }
    }

    /**
     * Creates the default API entry using the old-style username/password
     */
    private synchronized void migrateAPI() {
        Log.d(LOGTAG, "Migrating API");
        String user = prefs.getString(r.getString(R.string.config_username_key), "");
        String pass = prefs.getString(r.getString(R.string.config_password_key), "");
        String name = "OpenStreetMap";
        Log.d(LOGTAG, "Adding default URL with user '" + user + "'");
        addAPI(ID_DEFAULT, name, "", user, pass, ID_DEFAULT, false); // empty API URL => default API URL
        Log.d(LOGTAG, "Selecting default API");
        selectAPI(ID_DEFAULT);
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
    private void selectAPI(String id) {
        Log.d("AdvancedPrefDB", "Selecting API with ID: " + id);
        if (getAPIs(id).length == 0)
            throw new RuntimeException("Non-existant API selected");
        prefs.edit().putString(PREF_SELECTED_API, id).commit();
        currentAPI = id;
    }

    /** adds a new API with the given values to the API database */
    private synchronized void addAPI(String id, String name, String url, String user, String pass, String preset, boolean showicon) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", id);
        values.put("name", name);
        values.put("url", url);
        values.put("user", user);
        values.put("pass", pass);
        values.put("preset", preset);
        values.put("showicon", showicon ? 1 : 0);
        db.insert("apis", null, values);
        db.close();
    }

    /**
     * Fetches all APIs matching the given ID, or all APIs if id is null
     * 
     * @param id null to fetch all APIs, or API-ID to fetch a specific one
     * @return API[]
     */
    private synchronized API[] getAPIs(String id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor dbresult = db.query("apis", new String[] { "id", "name", "url", "user", "pass", "preset", "showicon" }, id == null ? null : "id = ?",
                id == null ? null : new String[] { id }, null, null, null);
        API[] result = new API[dbresult.getCount()];
        dbresult.moveToFirst();
        for (int i = 0; i < result.length; i++) {
            result[i] = new API(dbresult.getString(0), dbresult.getString(1), dbresult.getString(2), dbresult.getString(3), dbresult.getString(4),
                    dbresult.getString(5), dbresult.getInt(6));
            dbresult.moveToNext();
        }
        db.close();
        return result;
    }

    /**
     * Data structure class for API data
     * 
     * @author Jan
     */
    public class API {
        public final String  id;
        public final String  name;
        public final String  url;
        public final String  user;
        public final String  pass;
        public final String  preset;
        public final boolean showicon;

        public API(String id, String name, String url, String user, String pass, String preset, int showicon) {
            this.id = id;
            this.name = name;
            this.url = url;
            this.user = user;
            this.pass = pass;
            this.preset = preset;
            this.showicon = (showicon == 1);
        }
    }

    /** gets a preset by ID (will return null if no preset with this ID exists) */
    private PresetInfo getPreset(String id) {
        PresetInfo[] found = getPresets(id, false);
        if (found.length == 0)
            return null;
        return found[0];
    }

    /**
     * Fetches all Presets matching the given ID, or all Presets if id is null
     * 
     * @param value null to fetch all Presets, or Preset-ID/URL to fetch a specific one
     * @param byURL if false, value represents an ID, if true, value represents an URL
     * @return PresetInfo[]
     */
    private synchronized PresetInfo[] getPresets(String value, boolean byURL) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor dbresult = db.query("presets", new String[] { "id", "name", "url", "lastupdate" }, value == null ? null : (byURL ? "url = ?" : "id = ?"),
                value == null ? null : new String[] { value }, null, null, null);
        PresetInfo[] result = new PresetInfo[dbresult.getCount()];
        dbresult.moveToFirst();
        for (int i = 0; i < result.length; i++) {
            result[i] = new PresetInfo(dbresult.getString(0), dbresult.getString(1), dbresult.getString(2), dbresult.getString(3));
            dbresult.moveToNext();
        }
        db.close();
        return result;
    }

    /** adds a new Preset with the given values to the Preset database */
    private synchronized void addPreset(String id, String name, String url) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", id);
        values.put("name", name);
        values.put("url", url);
        db.insert("presets", null, values);
        db.close();
    }

    /**
     * Data structure class for Preset data
     * 
     * @author Jan
     */
    public class PresetInfo {
        public final String id;
        public final String name;
        public final String url;
        /** Timestamp (long, millis since epoch) when this preset was last downloaded */
        public final long   lastupdate;

        public PresetInfo(String id, String name, String url, String lastUpdate) {
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
        }
    }

}
