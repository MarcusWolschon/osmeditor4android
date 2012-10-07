package de.blau.android.prefs;

import java.io.File;

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
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.osm.Server;
import de.blau.android.presets.Preset;

/**
 * This class provides access to complex settings like OSM APIs which consist of complex/relational data.
 * WARNING: It has nothing to do with the "Advanced preferences" the user sees in the menu;
 * those are just a separate PreferenceScreen defined in the preferences.xml and handled like normal prefs!
 * @author Jan
 */
public class AdvancedPrefDatabase extends SQLiteOpenHelper {

	private final Resources r;
	private final SharedPreferences prefs;
	private final String PREF_SELECTED_API;

	private final static int DATA_VERSION = 2;
	private final static String LOGTAG = "AdvancedPrefDB";
	
	/** The ID string for the default API and the default Preset */
	public final static String ID_DEFAULT = "default";
	
	/** The ID of the currently active API */
	private String currentAPI;
	
	private Context context;


	public AdvancedPrefDatabase(Context context) {
		super(context, "AdvancedPrefs", null, DATA_VERSION);
		this.context = context;
		r = context.getResources();
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
		PREF_SELECTED_API = r.getString(R.string.config_selected_api);
		currentAPI = prefs.getString(PREF_SELECTED_API, null);
		if (currentAPI == null) migrateAPI();
		if (getPreset(ID_DEFAULT) == null) addPreset(ID_DEFAULT, "OpenStreetMap", "");
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
	 * @param id the ID of the API to be set as active
	 */
	public void selectAPI(String id) {
		Log.d("AdvancedPrefDB", "Selecting API with ID: " + id);
		if (getAPIs(id).length == 0) throw new RuntimeException("Non-existant API selected");
		prefs.edit().putString(PREF_SELECTED_API, id).commit();
		currentAPI = id;
		Main.prepareRedownload();
		Main.resetPreset();
	}
	
	/**
	 * @return a list of API objects containing all available APIs
	 */
	public API[] getAPIs() {
		return getAPIs(null);
	}
	
	/** @return the API object representing the currently selected API */ 
	public API getCurrentAPI() {
		API[] apis = getAPIs(currentAPI);
		if (apis.length == 0) return null;
		return apis[0];
	}
	
	/** @return a Server object matching the current API */
	public Server getServerObject() {
		API api = getCurrentAPI();
		if (api == null) return null;
		String version = r.getString(R.string.app_name) + " " + r.getString(R.string.app_version);
		return new Server(api.url, api.user, api.pass, version);
	}
	
	/**
	 * Sets name and URL of the current API entry
	 * @param id
	 * @param name
	 * @param url
	 */
	public synchronized void setAPIDescriptors(String id, String name, String url) {
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("name", name);
		values.put("url", url);
		db.update("apis", values, "id = ?", new String[] {id});
		db.close();
	}

	/** Sets login data (user, password) for the current API */
	public synchronized void setCurrentAPILogin(String user, String pass) {
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("user", user);
		values.put("pass", pass);
		db.update("apis", values, "id = ?", new String[] {currentAPI});
		db.close();
	}
	
	/** Changes the preset (by name) applying to the current API */ 
	public synchronized void setCurrentAPIPreset(String preset) {
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("preset", preset);
		db.update("apis", values, "id = ?", new String[] {currentAPI});
		db.close();
		Main.resetPreset();
	}
	
	/** Changes the "show node icons" settings for the current API */
	public synchronized void setCurrentAPIShowIcons(boolean show) {
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("showicon", show ? 1 : 0);
		db.update("apis", values, "id = ?", new String[] {currentAPI});
		db.close();		
	}
	
	/** adds a new API with the given values to the API database */
	public synchronized void addAPI(String id, String name, String url, String user, String pass, String preset, boolean showicon) {
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("id", id);
		values.put("name", name);
		values.put("url", url);
		values.put("user", user);
		values.put("pass", pass);
		values.put("preset", preset);		
		values.put("showicon", showicon? 1 : 0);		
		db.insert("apis", null, values);
		db.close();
	}
	
	/** removes an API from the API database */
	public synchronized void deleteAPI(final String id) {
		if (id.equals(ID_DEFAULT)) throw new RuntimeException("Cannot delete default");
		if (id.equals(currentAPI)) selectAPI(ID_DEFAULT);
		SQLiteDatabase db = getWritableDatabase();
		db.delete("apis", "id = ?", new String[] { id });
		db.close();
	}
	
	/**
	 * Fetches all APIs matching the given ID, or all APIs if id is null
	 * @param id null to fetch all APIs, or API-ID to fetch a specific one
	 * @return API[]
	 */
	private synchronized API[] getAPIs(String id) {
		SQLiteDatabase db = getReadableDatabase();
		Cursor dbresult = db.query(
								"apis",
								new String[] {"id", "name", "url", "user", "pass", "preset", "showicon"},
								id == null ? null : "id = ?",
								id == null ? null : new String[] {id},
								null, null, null);
		API[] result = new API[dbresult.getCount()];
		dbresult.moveToFirst();
		for (int i = 0; i < result.length; i++) {
			result[i] = new API(dbresult.getString(0),
									dbresult.getString(1),
									dbresult.getString(2),
									dbresult.getString(3),
									dbresult.getString(4),
									dbresult.getString(5),
									dbresult.getInt(6));
			dbresult.moveToNext();
		}
		dbresult.close();
		db.close();
		return result;
	}

	/**
	 * Data structure class for API data
	 * @author Jan
	 */
	public class API {
		public final String id;
		public final String name;
		public final String url;
		public final String user;
		public final String pass;
		public final String preset;
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

	/**
	 * Creates an object for the currently selected preset
	 * @return a corresponding preset object, or null if no valid preset is selected or the preset cannot be created
	 */
	public Preset getCurrentPresetObject() {
		API api = getCurrentAPI();
		if (api == null || api.preset == null || api.preset.equals("")) return null;
		PresetInfo presetinfo = getPreset(api.preset);
		try {
			if (presetinfo.url.startsWith(Preset.APKPRESET_URLPREFIX)) {
				return new Preset(context, getPresetDirectory(presetinfo.id),
						presetinfo.url.substring(Preset.APKPRESET_URLPREFIX.length()));
			} else {
				return new Preset(context, getPresetDirectory(presetinfo.id), null);
			}
		} catch (Exception e) {
			Log.e(LOGTAG, "Failed to create preset", e);
			return null;
		}
	}
	
	/** returns an array of PresetInfos for all currently known presets */
	public PresetInfo[] getPresets() {
		return getPresets(null, false);
	}
	
	/** gets a preset by ID (will return null if no preset with this ID exists) */
	public PresetInfo getPreset(String id) {
		PresetInfo[] found = getPresets(id, false);
		if (found.length == 0) return null;
		return found[0];
	}

	/** gets a preset by URL (will return null if no preset with this URL exists) */
	public PresetInfo getPresetByURL(String url) {
		PresetInfo[] found = getPresets(url, true);
		if (found.length == 0) return null;
		return found[0];
	}
	
	
	/**
	 * Fetches all Presets matching the given ID, or all Presets if id is null
	 * @param value null to fetch all Presets, or Preset-ID/URL to fetch a specific one
	 * @param byURL if false, value represents an ID, if true, value represents an URL 
	 * @return PresetInfo[]
	 */
	private synchronized PresetInfo[] getPresets(String value, boolean byURL) {
		SQLiteDatabase db = getReadableDatabase();
		Cursor dbresult = db.query(
								"presets",
								new String[] {"id", "name", "url", "lastupdate"},
								value == null ? null : (byURL ? "url = ?" : "id = ?"),
								value == null ? null : new String[] {value},
								null, null, null);
		PresetInfo[] result = new PresetInfo[dbresult.getCount()];
		dbresult.moveToFirst();
		for (int i = 0; i < result.length; i++) {
			result[i] = new PresetInfo(dbresult.getString(0),
									dbresult.getString(1),
									dbresult.getString(2),
									dbresult.getString(3));
			dbresult.moveToNext();
		}
		dbresult.close();
		db.close();
		return result;
	}
	

	/** adds a new Preset with the given values to the Preset database */
	public synchronized void addPreset(String id, String name, String url) {
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("id", id);
		values.put("name", name);
		values.put("url", url);
		db.insert("presets", null, values);
		db.close();
	}
	
	
	/** Updates the information (name & URL) about a Preset  */
	public synchronized void setPresetInfo(String id, String name, String url) {
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("name", name);
		values.put("url", url);
		db.update("presets", values, "id = ?", new String[] {id});
		db.close();
	}

	/** 
	 * Sets the lastupdate value of the givenpreset to now
	 * @param id the ID of the preset to update
	 * */
	public synchronized void setPresetLastupdateNow(String id) {
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("lastupdate", ((Long)System.currentTimeMillis()).toString());
		db.update("presets", values, "id = ?", new String[] {id});
		db.close();
	}

	
	/**
	 * Deletes a preset including the corresponding preset data directory
	 * @param id id of the preset to delete
	 */
	public synchronized void deletePreset(String id) {
		if (id.equals(ID_DEFAULT)) throw new RuntimeException("Cannot delete default");
		SQLiteDatabase db = getWritableDatabase();
		db.delete("presets", "id = ?", new String[] { id });
		db.close();
		removePresetDirectory(id);
		if (id.equals(getCurrentAPI().preset)) Main.resetPreset();
	}

	/**
	 * Data structure class for Preset data
	 * @author Jan
	 */
	public class PresetInfo {
		public final String id;
		public final String name;
		public final String url;
		/** Timestamp (long, millis since epoch) when this preset was last downloaded*/
		public final long lastupdate;
		
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

	/**
	 * Gets the preset data path for a preset with the given ID
	 * @param id
	 * @return
	 */
	public File getPresetDirectory(String id) {
		if (id == null || id.equals("")) {
			throw new RuntimeException("Attempted to get folder for null or empty id!");
		}
		File rootDir = context.getFilesDir();
		return new File(rootDir, id);
	}
	
	/**
	 * Removes the data directory belonging to a preset
	 * @param id the preset ID of the preset whose directory is going to be deleted
	 */
	public void removePresetDirectory(String id) {
		File presetDir = getPresetDirectory(id);
		if (presetDir.isDirectory()) {
			killDirectory(presetDir);
		}
	}

	/**
	 * Deletes all files inside a directory, then the directory itself (one level only, no recursion)
	 * @param dir the directory to empty and delete
	 */
	private void killDirectory(File dir) {
		if (!dir.isDirectory()) throw new RuntimeException("This function only deletes directories");
		File[] files = dir.listFiles();
		for (File f : files) {
			if (!f.delete()) Log.e(LOGTAG, "Could not delete "+f.getAbsolutePath());
		}
		if (!dir.delete()) Log.e(LOGTAG, "Could not delete "+dir.getAbsolutePath());
	}
	
}
