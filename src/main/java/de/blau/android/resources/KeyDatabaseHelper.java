package de.blau.android.resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
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
import de.blau.android.prefs.API.Auth;
import de.blau.android.util.ScreenMessage;

/**
 * Database helper for managing private keys
 * 
 * @author Simon Poole
 *
 */
public class KeyDatabaseHelper extends SQLiteOpenHelper {

    private static final int    TAG_LEN   = Math.min(23, KeyDatabaseHelper.class.getSimpleName().length());
    private static final String DEBUG_TAG = KeyDatabaseHelper.class.getSimpleName().substring(0, TAG_LEN);

    static final String         DATABASE_NAME    = "keys";
    private static final int    DATABASE_VERSION = 4;
    private static final int    FIELD_COUNT      = 4;
    private static final String AND              = " AND ";
    private static final String NAME_AND         = "=? AND ";

    private static final String KEYS_TABLE   = "keys";
    private static final String NAME_FIELD   = "name";
    private static final String TYPE_FIELD   = "type";
    private static final String KEY_FIELD    = "key";
    private static final String CUSTOM_FIELD = "custom";
    private static final String ADD1_FIELD   = "add1";
    private static final String ADD2_FIELD   = "add2";
    private static final String TRUE         = "true";

    public enum EntryType {
        IMAGERY, API_KEY, API_OAUTH1_KEY, API_OAUTH2_KEY, PANORAMAX_KEY, WIKIMEDIA_COMMONS_KEY
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
            db.execSQL("CREATE UNIQUE INDEX idx_keys ON keys (name, type)");
        } catch (SQLException e) {
            Log.w(DEBUG_TAG, "Problem creating database", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(DEBUG_TAG, "Upgrade from " + oldVersion + " to " + newVersion);
        if (oldVersion <= 2) {
            db.execSQL("DROP TABLE " + KEYS_TABLE);
            onCreate(db);
        }
        if (oldVersion <= 3) {
            db.execSQL("DROP INDEX idx_keys");
            db.execSQL("CREATE UNIQUE INDEX idx_keys ON keys (name, type)");
        }
    }

    /**
     * Add, replace or delete an entry in the keys table
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
    public static void replaceOrDeleteKey(@NonNull SQLiteDatabase db, @NonNull String name, @NonNull EntryType type, @Nullable String key, boolean custom,
            boolean overwrite, @Nullable String add1, @Nullable String add2) {
        if ("".equals(key) && overwrite) {
            Log.i(DEBUG_TAG, "Deleting key " + name);
            deleteKey(db, name, type);
        } else {
            Log.i(DEBUG_TAG, "Updating key " + name);
            ContentValues values = new ContentValues();
            values.put(NAME_FIELD, name);
            values.put(TYPE_FIELD, type.toString());
            values.put(KEY_FIELD, key);
            values.put(ADD1_FIELD, add1);
            values.put(ADD2_FIELD, add2);
            values.put(CUSTOM_FIELD, custom ? 1 : 0);
            try {
                int count = db.update(KEYS_TABLE, values, NAME_FIELD + NAME_AND + TYPE_FIELD + "=?" + (!overwrite ? AND + CUSTOM_FIELD + "=0" : ""),
                        new String[] { name, type.toString() });
                if (count == 0) {
                    db.insert(KEYS_TABLE, null, values);
                }
            } catch (SQLException e) {
                Log.e(DEBUG_TAG, "replaceOrDeleteKey " + e.getMessage());
            }
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
        db.delete(KEYS_TABLE, NAME_FIELD + NAME_AND + TYPE_FIELD + "=?", new String[] { name, type.toString() });
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
        try (Cursor dbresult = db.query(KEYS_TABLE, new String[] { KEY_FIELD }, NAME_FIELD + "=?  AND " + TYPE_FIELD + "=?",
                new String[] { name, type.toString() }, null, null, null)) {
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
     * Duplicate key identified by its name
     * 
     * @param db readable SQLiteDatabase
     * @param name the key name
     * @param type type of the entry
     * @param newName the name of the duplicated entry
     */
    @Nullable
    public static void copyKey(@NonNull SQLiteDatabase db, @NonNull String name, @NonNull EntryType type, @NonNull String newName) {
        try (Cursor dbresult = db.query(KEYS_TABLE, new String[] { KEY_FIELD, ADD1_FIELD, ADD2_FIELD }, NAME_FIELD + "=?  AND " + TYPE_FIELD + "=?",
                new String[] { name, type.toString() }, null, null, null)) {
            if (dbresult.getCount() == 1) {
                boolean haveEntry = dbresult.moveToFirst();
                if (haveEntry) {
                    replaceOrDeleteKey(db, newName, type, dbresult.getString(0), true, false, dbresult.getString(1), dbresult.getString(2));
                    return;
                }
            }
        }
        Log.d(DEBUG_TAG, "copying key " + name + " " + type + " to " + newName + " failed");
    }

    /**
     * Retrieve the OAuth configuration for an API
     * 
     * @param db readable SQLiteDatabase
     * @param name the API name
     * @param auth current Authentication type
     * @return a configuration or null if none found
     */
    @Nullable
    public static OAuthConfiguration getOAuthConfiguration(@NonNull SQLiteDatabase db, @NonNull String name, @NonNull Auth auth) {
        final boolean oAuth1a = auth == Auth.OAUTH1A;
        try (Cursor dbresult = db.query(KEYS_TABLE, new String[] { KEY_FIELD, ADD1_FIELD, ADD2_FIELD },
                NAME_FIELD + NAME_AND + TYPE_FIELD + "='" + (oAuth1a ? EntryType.API_OAUTH1_KEY : EntryType.API_OAUTH2_KEY) + "'", new String[] { name }, null,
                null, null)) {
            if (dbresult.getCount() == 1) {
                boolean haveEntry = dbresult.moveToFirst();
                if (haveEntry) {
                    try {
                        return getOAuthConfigurationFromCursor(oAuth1a, dbresult, name);
                    } catch (IllegalArgumentException iaex) {
                        Log.e(DEBUG_TAG, "error in entry for " + name);
                    }
                }
            }
            return null;
        }
    }

    /**
     * Retrieve the OAuth configurations for a specific OAuth variant
     * 
     * @param db readable SQLiteDatabase
     * @param auth current Authentication type
     * @return a List of configurations (potentially empty)
     */
    @NonNull
    public static List<OAuthConfiguration> getOAuthConfigurations(@NonNull SQLiteDatabase db, @NonNull Auth auth) {
        final boolean oAuth1a = auth == Auth.OAUTH1A;
        List<OAuthConfiguration> configurations = new ArrayList<>();
        try (Cursor dbresult = db.query(KEYS_TABLE, new String[] { NAME_FIELD, KEY_FIELD, ADD1_FIELD, ADD2_FIELD },
                TYPE_FIELD + "='" + (oAuth1a ? EntryType.API_OAUTH1_KEY : EntryType.API_OAUTH2_KEY) + "'", null, null, null, null)) {
            if (dbresult.getCount() >= 1) {
                boolean haveEntry = dbresult.moveToFirst();
                while (haveEntry) {
                    configurations.add(getOAuthConfigurationFromCursor(oAuth1a, dbresult, dbresult.getString(dbresult.getColumnIndexOrThrow(NAME_FIELD))));
                    haveEntry = dbresult.moveToNext();
                }
            }
        }
        return configurations;
    }

    /**
     * Get an OAuthConfiguration from a Cursor
     * 
     * @param oAuth1a true if it should be an OAuth1a config
     * @param dbresult the Cursor
     * @param name the name to use
     * @return the config
     */
    @NonNull
    private static OAuthConfiguration getOAuthConfigurationFromCursor(final boolean oAuth1a, @NonNull Cursor dbresult, @NonNull String name) {
        OAuthConfiguration result = new OAuthConfiguration(name);
        result.setKey(dbresult.getString(dbresult.getColumnIndexOrThrow(KEY_FIELD)));
        if (oAuth1a) {
            result.setSecret(dbresult.getString(dbresult.getColumnIndexOrThrow(ADD1_FIELD)));
        }
        result.setOauthUrl(dbresult.getString(dbresult.getColumnIndexOrThrow(ADD2_FIELD)));
        return result;
    }

    /**
     * Read keys from an InputStream in the format
     * name&lt;tab&gt;type&lt;tab&gt;key&lt;tab&gt;overwrite&lt;tab&gt;add1&lt;tab&gt;add2
     * 
     * @param context Android content if not null error messages will be toasted
     * @param is the InputStream
     */
    public void keysFromStream(@Nullable Context context, @NonNull InputStream is) {
        int lineNumber = 0;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is)); SQLiteDatabase db = getWritableDatabase()) {
            String line = null;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                if (line.startsWith("#")) {
                    continue; // skip comments
                }
                String[] k = line.split("\\t");
                for (int i = 0; i < k.length; i++) {
                    k[i] = k[i].trim();
                }
                if (k.length < FIELD_COUNT) {
                    Log.e(DEBUG_TAG, "short key DB entry " + line);
                } else {
                    processLine(db, k);
                }
            }
        } catch (IOException | ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
            final String msg = "Exception reading keys file  " + e.getMessage() + " on line " + lineNumber;
            Log.e(DEBUG_TAG, msg);
            if (context != null) {
                ScreenMessage.toastTopError(context, msg);
            }
        }
    }

    /**
     * Process the values from one line
     * 
     * @param db the target database
     * @param k an array of strings to use
     */
    private void processLine(@NonNull SQLiteDatabase db, @NonNull String[] k) {
        boolean overwrite = TRUE.equalsIgnoreCase(k[3]);
        EntryType type = EntryType.valueOf(k[1].toUpperCase(Locale.US));
        if (k.length == FIELD_COUNT) {
            replaceOrDeleteKey(db, k[0], type, k[2], false, overwrite, null, null);
        } else if (k.length == FIELD_COUNT + 2) {
            replaceOrDeleteKey(db, k[0], type, k[2], false, overwrite, k[4], k[5]);
        } else {
            throw new IllegalArgumentException("invalid entry");
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
            keys.keysFromStream(null, assetManager.open(Files.FILE_NAME_KEYS_V2_DEFAULT));
            keys.keysFromStream(null, assetManager.open(Files.FILE_NAME_KEYS_V2));
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "Error reading keys file " + e.getMessage());
        }
    }

    /**
     * Get the contents of the DB
     * 
     * @param db a readable DB
     * @return a list of strings with tab separated columns
     */
    public static List<String> getAll(@NonNull SQLiteDatabase db) {
        List<String> result = new ArrayList<>();
        try (Cursor dbresult = db.query(KEYS_TABLE, new String[] { NAME_FIELD, KEY_FIELD, TYPE_FIELD, ADD1_FIELD, ADD2_FIELD }, null, null, null, null, null)) {
            boolean haveEntry = dbresult.moveToFirst();
            while (haveEntry) {
                // @formatter:off
                result.add(dbresult.getString(dbresult.getColumnIndexOrThrow(NAME_FIELD)) 
                        + "\t" + dbresult.getString(dbresult.getColumnIndexOrThrow(KEY_FIELD))
                        + "\t" + dbresult.getString(dbresult.getColumnIndexOrThrow(TYPE_FIELD)) 
                        + "\t" + dbresult.getString(dbresult.getColumnIndexOrThrow(ADD1_FIELD)) 
                        + "\t" + dbresult.getString(dbresult.getColumnIndexOrThrow(ADD2_FIELD)));
                // @formatter:on
                haveEntry = dbresult.moveToNext();
            }
        }
        return result;
    }
}
