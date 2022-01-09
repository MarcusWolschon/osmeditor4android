package de.blau.android.resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.contract.Files;
import de.blau.android.net.OAuthHelper.OAuthConfiguration;

/**
 * Database helper for managing private keys
 * 
 * @author Simon Poole
 *
 */
public class KeyDatabaseHelper extends SQLiteOpenHelper {
    private static final String DEBUG_TAG = "KeyDatabase";

    private static final String DATABASE_NAME    = "keys";
    private static final int    DATABASE_VERSION = 3;
    private static final int    FIELD_COUNT      = 4;
    private static final String AND              = " AND ";

    private static final String KEYS_TABLE   = "keys";
    private static final String NAME_FIELD   = "name";
    private static final String TYPE_FIELD   = "type";
    private static final String KEY_FIELD    = "key";
    private static final String CUSTOM_FIELD = "custom";
    private static final String ADD1_FIELD   = "add1";
    private static final String ADD2_FIELD   = "add2";

    public enum EntryType {
        IMAGERY, API_KEY, API_OAUTH1_KEY
    }

    /**
     * Create a new DatabaseHelper for the key db
     * 
     * @param context an Android Context
     */
    public KeyDatabaseHelper(final Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(DEBUG_TAG, "Creating database");
        try {
            db.execSQL(
                    "CREATE TABLE keys (name TEXT, type TEXT, key TEXT DEFAULT NULL, add1 TEXT DEFAULT NULL, add2 TEXT DEFAULT NULL, custom INTEGER DEFAULT 0)");
            db.execSQL("CREATE UNIQUE INDEX idx_keys ON keys (name)");
        } catch (SQLException e) {
            Log.w(DEBUG_TAG, "Problem creating database", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(DEBUG_TAG, "Upgrade from " + oldVersion + " to " + newVersion);
        if (oldVersion <= 2 && newVersion >= 3) {
            db.execSQL("DROP TABLE " + KEYS_TABLE);
            onCreate(db);
        }
    }

    /**
     * Add or replace an entry in the keys table
     * 
     * @param db writable database
     * @param name the key name
     * @param type type of the entry
     * @param key the key value
     * @param custom if set this is a custom entry that will be protected against overwriting
     * @param overwrite overwrite entry even if custom flag is set
     * @param add1 1st additional value to store
     * @param add2 2nd additional value to store
     */
    public static void replaceKey(@NonNull SQLiteDatabase db, @NonNull String name, @NonNull EntryType type, @Nullable String key, boolean custom,
            boolean overwrite, @Nullable String add1, @Nullable String add2) {
        ContentValues values = new ContentValues();
        values.put(NAME_FIELD, name);
        values.put(TYPE_FIELD, type.toString());
        values.put(KEY_FIELD, key);
        values.put(ADD1_FIELD, add1);
        values.put(ADD2_FIELD, add2);
        values.put(CUSTOM_FIELD, custom ? 1 : 0);
        try {
            int count = db.update(KEYS_TABLE, values, NAME_FIELD + "=? AND " + TYPE_FIELD + "=?" + (!overwrite ? AND + CUSTOM_FIELD + "=0" : ""),
                    new String[] { name, type.toString() });
            if (count == 0) {
                db.insert(KEYS_TABLE, null, values);
            }
        } catch (SQLException e) {
            Log.e(DEBUG_TAG, "replaceKey " + e.getMessage());
        }
    }

    /**
     * Delete a specific key
     * 
     * @param db writable database
     * @param name name of the entry
     * @param type type of the entry
     */
    public static void deleteKey(@NonNull final SQLiteDatabase db, @NonNull String name, @NonNull EntryType type) {
        db.delete(KEYS_TABLE, NAME_FIELD + "=? AND " + TYPE_FIELD + "=?", new String[] { name, type.toString() });
    }

    /**
     * Retrieve a single key identified by its name
     * 
     * @param db readable SQLiteDatabase
     * @param name the key name
     * @param type type of the entry
     * @return the key value or null if none could be found
     */
    @Nullable
    public static String getKey(@NonNull SQLiteDatabase db, @NonNull String name, @NonNull EntryType type) {
        try (Cursor dbresult = db.query(KEYS_TABLE, new String[] { KEY_FIELD },
                NAME_FIELD + "='" + name + "'" + AND + TYPE_FIELD + "='" + type.toString() + "'", null, null, null, null)) {
            if (dbresult.getCount() >= 1) {
                boolean haveEntry = dbresult.moveToFirst();
                if (haveEntry) {
                    return dbresult.getString(0);
                }
            }
            return null;
        }
    }

    /**
     * Retrieve the OAuth configuration for an API
     * 
     * @param db readable SQLiteDatabase
     * @param name the API name
     * @return a configuration or null if none found
     */
    @Nullable
    public static OAuthConfiguration getOAuthConfiguration(@NonNull SQLiteDatabase db, @NonNull String name) {
        try (Cursor dbresult = db.query(KEYS_TABLE, new String[] { KEY_FIELD, ADD1_FIELD, ADD2_FIELD },
                NAME_FIELD + "='" + name + "'" + AND + TYPE_FIELD + "='" + EntryType.API_OAUTH1_KEY + "'", null, null, null, null)) {
            if (dbresult.getCount() == 1) {
                OAuthConfiguration result = new OAuthConfiguration();
                boolean haveEntry = dbresult.moveToFirst();
                if (haveEntry) {
                    try {
                        result.setKey(dbresult.getString(dbresult.getColumnIndexOrThrow(KEY_FIELD)));
                        result.setSecret(dbresult.getString(dbresult.getColumnIndexOrThrow(ADD1_FIELD)));
                        result.setOauthUrl(dbresult.getString(dbresult.getColumnIndexOrThrow(ADD2_FIELD)));
                        return result;
                    } catch (IllegalArgumentException iaex) {
                        Log.e(DEBUG_TAG, "error in entry for " + name);
                    }
                }
            }
            return null;
        }
    }

    /**
     * Read keys from an InputStream in the format
     * name&lt;tab&gt;type&lt;tab&gt;key&lt;tab&gt;overwrite&lt;tab&gt;add1&lt;tab&gt;add2
     * 
     * @param is the InputStream
     */
    public void keysFromStream(@NonNull InputStream is) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is)); SQLiteDatabase db = getWritableDatabase()) {
            String line = null;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue; // skip comments
                }
                String[] k = line.split("\\t");
                for (int i = 0; i < k.length; i++) {
                    k[i] = k[i].trim();
                }
                boolean overwrite = "true".equalsIgnoreCase(k[3]);
                EntryType type = EntryType.valueOf(k[1].toUpperCase(Locale.US).trim());
                if (type == EntryType.IMAGERY) { // backwards compatibility
                    k[0] = k[0].toUpperCase(Locale.US);
                }
                if (k.length == FIELD_COUNT) {
                    replaceKey(db, k[0], type, k[2], false, overwrite, null, null);
                } else if (k.length == FIELD_COUNT + 2) {
                    replaceKey(db, k[0], type, k[2], false, overwrite, k[4], k[5]);
                } else {
                    Log.e(DEBUG_TAG, "invalid entry " + line);
                }
            }
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "exception reading stream  " + e.getMessage());
        }
    }

    /**
     * Read keys from assets
     * 
     * Assumes they are in a file named Files.FILE_NAME_KEYS_V2
     * 
     * @param ctx an Android Context
     */
    public static void readKeysFromAssets(@NonNull Context ctx) {
        AssetManager assetManager = ctx.getAssets();
        try (KeyDatabaseHelper keys = new KeyDatabaseHelper(ctx)) {
            // these will be overwritten if Files.FILE_NAME_KEYS_V2 exists
            keys.keysFromStream(assetManager.open(Files.FILE_NAME_KEYS_V2_DEFAULT));
            keys.keysFromStream(assetManager.open(Files.FILE_NAME_KEYS_V2));
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "Error reading keys file " + e.getMessage());
        }
    }
}
