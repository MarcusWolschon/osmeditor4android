package de.blau.android.resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

public class KeyDatabaseHelper extends SQLiteOpenHelper {
    private static final String DEBUG_TAG        = "KeyDatabase";
    private static final String DATABASE_NAME    = "keys";
    private static final int    DATABASE_VERSION = 2;

    private static final String KEYS_TABLE = "keys";
    private static final String NAME_FIELD = "name";
    private static final String KEY_FIELD  = "key";

    /**
     * Create a new DatabaseFelber for the tag filter
     * 
     * @param context an Android Context
     */
    public KeyDatabaseHelper(final Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE TABLE keys (name TEXT, key TEXT DEFAULT NULL)");
            db.execSQL("CREATE UNIQUE INDEX idx_keys ON keys (name)");
        } catch (SQLException e) {
            Log.w(DEBUG_TAG, "Problem creating database", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(DEBUG_TAG, "Upgrade from " + oldVersion + " to " + newVersion);
    }

    /**
     * Add or replace an entry in the keys table
     * 
     * @param db writable database
     * @param name the key name
     * @param key the key value
     */
    public static void replaceKey(@NonNull SQLiteDatabase db, @NonNull String name, @Nullable String key) {
        ContentValues values = new ContentValues();
        values.put(NAME_FIELD, name);
        values.put(KEY_FIELD, key);
        try {
            db.replaceOrThrow(KEYS_TABLE, null, values);
        } catch (SQLException e) {
            Log.e(DEBUG_TAG, "replaceKey " + e.getMessage());
        }
    }

    /**
     * Delete a specific key
     * 
     * @param db writable database
     * @param name name of the entry
     */
    public static void deleteKey(@NonNull final SQLiteDatabase db, @NonNull String name) {
        db.delete(KEYS_TABLE, NAME_FIELD + "=?", new String[] { name });
    }

    /**
     * Retrieve a single key identified by its name
     * 
     * @param db readable SQLiteDatabase
     * @param name the layer name
     * @return the key value or null if none could be found
     */
    @Nullable
    public static String getKey(@NonNull SQLiteDatabase db, @NonNull String name) {
        Cursor dbresult = db.query(KEYS_TABLE, new String[] { KEY_FIELD }, NAME_FIELD + "='" + name + "'", null, null, null, null);
        try {
            if (dbresult.getCount() >= 1) {
                boolean haveEntry = dbresult.moveToFirst();
                if (haveEntry) {
                    return dbresult.getString(0);
                }
            }
            return null;
        } finally {
            dbresult.close();
        }
    }

    /**
     * Read keys from an InputStream in the format name<tab>key
     * 
     * @param is the InputStream
     */
    public void keysFromStream(@NonNull InputStream is) {
        SQLiteDatabase db = getWritableDatabase();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line = null;
            while ((line = br.readLine()) != null) {
                String[] k = line.split("\\t", 2);
                if (k.length == 2) {
                    replaceKey(db, k[0].toUpperCase(Locale.US), k[1]);
                } else {
                    Log.e(DEBUG_TAG, "invalid entry " + line);
                }
            }
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "exception reading stream  " + e.getMessage());
        } finally {
            db.close();
        }
    }
}
