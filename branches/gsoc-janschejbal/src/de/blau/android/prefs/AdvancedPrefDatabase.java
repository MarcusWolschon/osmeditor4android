package de.blau.android.prefs;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;
import android.util.Log;
import de.blau.android.R;
import de.blau.android.osm.Server;

public class AdvancedPrefDatabase extends SQLiteOpenHelper {

	private final Resources r;
	private final SharedPreferences prefs;
	private final String PREF_SELECTED_API;

	private final static int DATA_VERSION = 1;
	private final static String LOGTAG = "AdvancedPrefDB";
	
	public final static String ID_DEFAULT = "default";
	
	private String currentAPI;


	public AdvancedPrefDatabase(Context context) {
		super(context, "AdvancedPrefs", null, DATA_VERSION);
		this.r = context.getResources();
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
		PREF_SELECTED_API = r.getString(R.string.config_selected_api);
		currentAPI = prefs.getString(PREF_SELECTED_API, null);
		if (currentAPI == null) migrateAPI();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE apis (id TEXT, name TEXT, url TEXT, user TEXT, pass TEXT, preset TEXT)");
		db.execSQL("CREATE TABLE presets (id TEXT, name TEXT, url TEXT, lastupdate TEXT, data TEXT)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// nothing yet
	}
	
	
	private void migrateAPI() {
		Log.d(LOGTAG, "Migrating API");
		String user = prefs.getString(r.getString(R.string.config_username_key), "");
		String pass = prefs.getString(r.getString(R.string.config_password_key), "");
		String name = "OpenStreetMap";
		Log.d(LOGTAG, "Adding default URL with user '" + user + "'");
		addAPI(ID_DEFAULT, name, "", user, pass, ID_DEFAULT); // empty API URL => default API URL
		Log.d(LOGTAG, "Selecting default API");
		selectAPI(ID_DEFAULT);
		Log.d(LOGTAG, "Migration finished");
	}
	
	/**
	 * Set the currently active API
	 * @param id the ID of the API to be set as active
	 */
	public void selectAPI(String id) {
		Log.d("AdvancedPrefDB", "Selecting API with ID: " + id);
		if (getAPIs(id).length == 0) throw new RuntimeException("Non-existant API selected");
		prefs.edit().putString(PREF_SELECTED_API, id).apply();
		currentAPI = id;
	}
	
	public API[] getAPIs() {
		return getAPIs(null);
	}
	
	public API getCurrentAPI() {
		API[] apis = getAPIs(currentAPI);
		if (apis.length == 0) return null;
		return apis[0];
	}
	
	public Server getServerObject() {
		API api = getCurrentAPI();
		if (api == null) return null;
		String version = r.getString(R.string.app_name) + " " + r.getString(R.string.app_version);
		return new Server(api.url, api.user, api.pass, version);
	}
	
	/**
	 * Sets name and URL of the current API entry
	 * @param name
	 * @param url
	 * @param value 
	 */
	public void setAPIDescriptors(String id, String name, String url) {
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("name", name);
		values.put("url", url);
		db.update("apis", values, "id = ?", new String[] {id});
		db.close();
	}

	public void setCurrentAPILogin(String user, String pass) {
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("user", user);
		values.put("pass", pass);
		db.update("apis", values, "id = ?", new String[] {currentAPI});
		db.close();
	}
	
	public void setCurrentAPIPreset(String preset) {
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("preset", preset);
		db.update("apis", values, "id = ?", new String[] {currentAPI});
		db.close();
	}
	
	public void addAPI(String id, String name, String url, String user, String pass, String preset) {
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("id", id);
		values.put("name", name);
		values.put("url", url);
		values.put("user", user);
		values.put("pass", pass);
		values.put("preset", preset);		
		db.insert("apis", null, values);
		db.close();
	}
	
	public void deleteAPI(final String id) {
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
	private API[] getAPIs(String id) {
		SQLiteDatabase db = getReadableDatabase();
		Cursor dbresult = db.query(
								"apis",
								new String[] {"id", "name", "url", "user", "pass", "preset"},
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
									dbresult.getString(5));
			dbresult.moveToNext();
		}
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
		
		public API(String id, String name, String url, String user, String pass, String preset) {
			this.id = id;
			this.name = name;
			this.url = url;
			this.user = user;
			this.pass = pass;
			this.preset = preset;
		}
	}
	
}
