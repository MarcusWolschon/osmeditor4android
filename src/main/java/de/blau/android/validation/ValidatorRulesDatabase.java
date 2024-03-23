package de.blau.android.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.blau.android.bookmarks.BookmarkStorage;
import de.blau.android.util.collections.MultiHashMap;

/**
 * Access methods for the validator rules database
 * 
 * @author simon
 *
 */
public final class ValidatorRulesDatabase {
    /**
     * Table: rulesets (id INTEGER, name TEXT) Table: resurvey (ruleset INTEGER, key TEXT, value TEXT DEFAULT NULL, days
     * INTEGER DEFAULT 365, enabled INTEGER DEFAULT 1, FOREIGN KEY(ruleset) REFERENCES rulesets(id)) Table: check (ruleset INTEGER, key TEXT,
     * optional INTEGER DEFAULT 0, FOREIGN KEY(ruleset) REFERENCES rulesets(id))
     */
    private static final String DEBUG_TAG = ValidatorRulesDatabase.class.getSimpleName().substring(0, Math.min(23, ValidatorRulesDatabase.class.getSimpleName().length()));
    static final String         DEFAULT_RULESET_NAME = "Default";
    static final int            DEFAULT_RULESET      = 0;
    private static final String RULESET_TABLE        = "rulesets";
    static final String         ID_FIELD             = "id";
    static final String         NAME_FIELD           = "name";
    private static final String RESURVEY_TABLE       = "resurveytags";
    static final String         DAYS_FIELD           = "days";
    static final String         VALUE_FIELD          = "value";
    static final String         ISREGEXP_FIELD       = "is_regexp";
    static final String         KEY_FIELD            = "key";
    static final String         RULESET_FIELD        = "ruleset";
    private static final String CHECK_TABLE          = "checktags";
    static final String         OPTIONAL_FIELD       = "optional";
    static final String         ENABLED_FIELD       = "enabled";

    static final String QUERY_RESURVEY_DEFAULT  = "SELECT resurveytags.rowid as _id, key, value, is_regexp, days, enabled FROM resurveytags WHERE ruleset = "
            + DEFAULT_RULESET + " ORDER BY key, value";
    static final String QUERY_RESURVEY_ALL_ID  = "SELECT resurveytags.rowid as _id FROM resurveytags";
    static final String QUERY_RESURVEY_BY_ROWID = "SELECT key, value, is_regexp, days, enabled FROM resurveytags WHERE rowid=?";
    static final String QUERY_RESURVEY_BY_NAME  = "SELECT resurveytags.rowid as _id, key, value, is_regexp, days, enabled FROM resurveytags, rulesets WHERE ruleset = rulesets.id and rulesets.name = ? ORDER BY key, value";

    static final String QUERY_CHECK_DEFAULT  = "SELECT checktags.rowid as _id, key, optional FROM checktags WHERE ruleset = " + DEFAULT_RULESET
            + " ORDER BY key";
    static final String QUERY_CHECK_BY_ROWID = "SELECT key, optional FROM checktags WHERE rowid=?";
    static final String QUERY_CHECK_BY_NAME  = "SELECT checktags.rowid as _id, key, optional FROM checktags, rulesets WHERE ruleset = rulesets.id and rulesets.name = ? ORDER BY key";

    /**
     * Private constructor to stop instantiation
     */
    private ValidatorRulesDatabase() {
        // private
    }

    /**
     * Add an entry to the ruleset table
     * 
     * @param db writable ruleset database
     * @param id ruleset id
     * @param name name of the ruleset
     */
    public static void addRuleset(SQLiteDatabase db, int id, String name) {
        ContentValues values = new ContentValues();
        values.put(ID_FIELD, id);
        values.put(NAME_FIELD, name);
        db.insert(RULESET_TABLE, null, values);
    }

    /**
     * Return the default resurvey entries if any
     * 
     * @param database readable database
     * @return a Map of the key-value tupels
     */
    @Nullable
    public static MultiHashMap<String, PatternAndAge> getDefaultResurvey(@NonNull SQLiteDatabase database) {
        MultiHashMap<String, PatternAndAge> result = null;
        Cursor dbresult = database.query(RESURVEY_TABLE, new String[] { KEY_FIELD, VALUE_FIELD, ISREGEXP_FIELD, DAYS_FIELD, ENABLED_FIELD },
                RULESET_FIELD + " = " + DEFAULT_RULESET, null, null, null, KEY_FIELD + "," + VALUE_FIELD);

        if (dbresult.getCount() >= 1) {
            result = new MultiHashMap<>();
            boolean haveEntry = dbresult.moveToFirst();
            while (haveEntry) {
                if (dbresult.getInt(dbresult.getColumnIndexOrThrow(ValidatorRulesDatabase.ENABLED_FIELD)) == 1) {
                    PatternAndAge v = new PatternAndAge();
                    v.setValue(dbresult.getString(1));
                    v.setIsRegexp(dbresult.getInt(2) == 1);
                    v.setAge(dbresult.getLong(3) * 24 * 3600); // days -> secs
                    result.add(dbresult.getString(0), v);
                    haveEntry = dbresult.moveToNext();
                }
            }
        }
        dbresult.close();
        return result;
    }

    /**
     * Get all entries for a specific ruleset
     * 
     * @param database the database to query
     * @param name the ruleset name or null if we want the default entries
     * @return a Cursor pointing to the first entry
     */
    public static Cursor queryResurveyByName(@NonNull SQLiteDatabase database, @Nullable String name) {
        if (name == null) {
            return database.rawQuery(ValidatorRulesDatabase.QUERY_RESURVEY_DEFAULT, null);
        } else {
            return database.rawQuery(ValidatorRulesDatabase.QUERY_RESURVEY_BY_NAME, new String[] { name });
        }
    }
    /**
     * Return all the resurvey ids at the time a new instance is created
     *
     * @param database readable database
     * @return a List of resurvey ids
     */
    @Nullable
    public static List<Integer> getAllResurveyIds(@NonNull SQLiteDatabase database) {
        List<Integer> result = new ArrayList<>();
        Cursor dbresult = database.rawQuery(ValidatorRulesDatabase.QUERY_RESURVEY_ALL_ID, null);
        int counter = 0;
        if (dbresult.getCount() >= 1) {
            boolean haveEntry = dbresult.moveToFirst();
            while (haveEntry) {
                result.add(dbresult.getInt(dbresult.getColumnIndexOrThrow("_id")));
                haveEntry = dbresult.moveToNext();
                counter++;
            }
        }
        dbresult.close();
        Log.d(DEBUG_TAG, "Retrieved" + counter +"ids from validator database");
        return result;
    }

    /**
     * Add an entry to the resurvey table
     * 
     * @param db writable ruleset database
     * @param ruleSetId id of the rule set we are using
     * @param key key of objects that should be age checked
     * @param value value of objects that should be age checked
     * @param isRegexp if true the value is a regexp
     * @param days how man days old the object should max be
     */
    public static void addResurvey(@NonNull SQLiteDatabase db, int ruleSetId, @NonNull String key, @Nullable String value, boolean isRegexp, int days, boolean enabled) {
        ContentValues values = new ContentValues();
        values.put(RULESET_FIELD, ruleSetId);
        values.put(KEY_FIELD, key);
        values.put(VALUE_FIELD, value);
        values.put(ISREGEXP_FIELD, isRegexp ? 1 : 0);
        values.put(DAYS_FIELD, days);
        values.put(ENABLED_FIELD, enabled ? 1 : 0);
        db.insert(RESURVEY_TABLE, null, values);
    }

    /**
     * Update an existing resurvex entry
     * 
     * @param db writable database
     * @param id rowid of the resurvey entry
     * @param key key of objects that should be age checked
     * @param value value of objects that should be age checked
     * @param isRegexp if true the value is a regexp
     * @param days how man days old the object should max be
     */
    public static void updateResurvey(@NonNull SQLiteDatabase db, int id, @NonNull String key, @Nullable String value, boolean isRegexp, int days) {
        ContentValues values = new ContentValues();
        values.put(KEY_FIELD, key);
        values.put(VALUE_FIELD, value);
        values.put(ISREGEXP_FIELD, isRegexp ? 1 : 0);
        values.put(DAYS_FIELD, days);
        db.update(RESURVEY_TABLE, values, "rowid=" + id, null);
    }

    /**
     * Delete an entry in the resurvey table
     * 
     * @param db writable database
     * @param id rowid of the entry
     */
    static void deleteResurvey(final SQLiteDatabase db, final int id) {
        db.delete(RESURVEY_TABLE, "rowid=?", new String[] { Integer.toString(id) });
    }

    /**
     * Change the enabled row of the resurvey table.
     *
     * @param db writable database
     * @param idIsEnabledMap rowid and their checkbox state
     */
    public static void enableResurvey(SQLiteDatabase db, Map<Integer, Boolean> idIsEnabledMap) {
        List<ContentValues> contentValuesList = new ArrayList<>();
        for (Map.Entry<Integer, Boolean> entry : idIsEnabledMap.entrySet()) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(ENABLED_FIELD, entry.getValue() ? 1 : 0);
            contentValuesList.add(contentValues);
        }
        StringBuilder selectionBuilder = new StringBuilder("rowid IN (");
        List<String> selectionArgs = new ArrayList<>();
        for (int key : idIsEnabledMap.keySet()) {
            selectionBuilder.append("?,");
            selectionArgs.add(String.valueOf(key));
        }
        selectionBuilder.replace(selectionBuilder.length() - 1, selectionBuilder.length(), ")");
        String selection = selectionBuilder.toString();

        int rowsUpdated = db.update(RESURVEY_TABLE, contentValuesList.get(0), selection, selectionArgs.toArray(new String[0]));
        Log.d(DEBUG_TAG, "Rulesets updated: " + rowsUpdated);

    }

    /**
     * Return the default resurvey entries if any
     * 
     * @param database readable database
     * @return a Map of the key-value tupels
     */
    @Nullable
    public static Map<String, Boolean> getDefaultCheck(@NonNull SQLiteDatabase database) {
        Map<String, Boolean> result = null;
        Cursor dbresult = database.query(CHECK_TABLE, new String[] { KEY_FIELD, OPTIONAL_FIELD }, RULESET_FIELD + " = " + DEFAULT_RULESET, null, null, null,
                KEY_FIELD);

        if (dbresult.getCount() >= 1) {
            result = new HashMap<>();
            boolean haveEntry = dbresult.moveToFirst();
            while (haveEntry) {
                result.put(dbresult.getString(0), dbresult.getInt(1) == 1);
                haveEntry = dbresult.moveToNext();
            }
        }
        dbresult.close();
        return result;
    }

    /**
     * Get all entries for a specific ruleset
     * 
     * @param database the database to query
     * @param name the ruleset name or null if we want the default entries
     * @return a Cursor pointing to the first entry
     */
    public static Cursor queryCheckByName(@NonNull SQLiteDatabase database, @Nullable String name) {
        if (name == null) {
            return database.rawQuery(ValidatorRulesDatabase.QUERY_CHECK_DEFAULT, null);
        } else {
            return database.rawQuery(ValidatorRulesDatabase.QUERY_CHECK_BY_NAME, new String[] { name });
        }
    }

    /**
     * Add an entry to the check table
     * 
     * @param db writable ruleset database
     * @param ruleSetId id of the rule set we are using
     * @param key key of objects that should be age checked
     * @param optional if true check against optional tags too
     */
    public static void addCheck(@NonNull SQLiteDatabase db, int ruleSetId, @NonNull String key, boolean optional) {
        ContentValues values = new ContentValues();
        values.put(RULESET_FIELD, ruleSetId);
        values.put(KEY_FIELD, key);
        values.put(OPTIONAL_FIELD, optional ? 1 : 0);
        db.insert(CHECK_TABLE, null, values);
    }

    /**
     * Update an existing check entry
     * 
     * @param db writable database
     * @param id rowid of the check entry
     * @param key key of objects that should be age checked
     * @param optional if true check against optional tags too
     */
    public static void updateCheck(@NonNull SQLiteDatabase db, int id, @NonNull String key, boolean optional) {
        ContentValues values = new ContentValues();
        values.put(KEY_FIELD, key);
        values.put(OPTIONAL_FIELD, optional ? 1 : 0);
        db.update(CHECK_TABLE, values, "rowid=" + id, null);
    }

    /**
     * Delete an entry in the check table
     * 
     * @param db writable database
     * @param id rowid of the entry
     */
    static void deleteCheck(final SQLiteDatabase db, final int id) {
        db.delete(CHECK_TABLE, "rowid=?", new String[] { Integer.toString(id) });
    }
}
