package de.blau.android.validation;

import java.util.HashMap;
import java.util.Map;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import de.blau.android.util.collections.MultiHashMap;

/**
 * Access methods for the validator rules database
 * 
 * @author simon
 *
 */
public class ValidatorRulesDatabase {
    /**
     * Table: rulesets (id INTEGER, name TEXT)
     * Table: resurvey (ruleset INTEGER, key TEXT, value TEXT DEFAULT NULL, days INTEGER DEFAULT 365, FOREIGN KEY(ruleset) REFERENCES rulesets(id))
     * Table: check (ruleset INTEGER, key TEXT, optional INTEGER DEFAULT 0, FOREIGN KEY(ruleset) REFERENCES rulesets(id)) 
     */
    
    static final String DEFAULT_RULESET_NAME = "Default";
    static final int DEFAULT_RULESET = 0;
    private static final String RULESET_TABLE = "rulesets";
    static final String ID_FIELD = "id";
    static final String NAME_FIELD = "name";
    private static final String RESURVEY_TABLE = "resurveytags";
    static final String DAYS_FIELD = "days";
    static final String VALUE_FIELD = "value";
    static final String ISREGEXP_FIELD = "is_regexp";
    static final String KEY_FIELD = "key";
    static final String RULESET_FIELD = "ruleset";
    private static final String CHECK_TABLE = "checktags";
    static final String OPTIONAL_FIELD = "optional";

    static final String QUERY_RESURVEY_DEFAULT = "SELECT resurveytags.rowid as _id, key, value, is_regexp, days FROM resurveytags WHERE ruleset = " + DEFAULT_RULESET + " ORDER BY key, value";
    static final String QUERY_RESURVEY_BY_ROWID = "SELECT key, value, is_regexp, days FROM resurveytags WHERE rowid=?";
    static final String QUERY_RESURVEY_BY_NAME = "SELECT resurveytags.rowid as _id, key, value, is_regexp, days FROM resurveytags, rulesets WHERE ruleset = rulesets.id and rulesets.name = ? ORDER BY key, value";
   
    static final String QUERY_CHECK_DEFAULT = "SELECT checktags.rowid as _id, key, optional FROM checktags WHERE ruleset = " + DEFAULT_RULESET + " ORDER BY key";
    static final String QUERY_CHECK_BY_ROWID = "SELECT key, optional FROM checktags WHERE rowid=?";
    static final String QUERY_CHECK_BY_NAME = "SELECT checktags.rowid as _id, key, optional FROM checktags, rulesets WHERE ruleset = rulesets.id and rulesets.name = ? ORDER BY key";

    /**
     * Add an entry to the ruleset table
     * 
     * @param db    writable ruleset database
     * @param id    ruleset id
     * @param name  name of the ruleset
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
    public static MultiHashMap<String,PatternAndAge> getDefaultResurvey(@NonNull SQLiteDatabase database) {
        MultiHashMap<String,PatternAndAge> result = null;
        Cursor dbresult = database.query(RESURVEY_TABLE, new String[] { KEY_FIELD, VALUE_FIELD, ISREGEXP_FIELD, DAYS_FIELD },
                RULESET_FIELD + " = " +  DEFAULT_RULESET, null, null, null, KEY_FIELD + "," +  VALUE_FIELD);

        if (dbresult.getCount() >= 1) {
            result = new MultiHashMap<>();
            boolean haveEntry = dbresult.moveToFirst();
            while (haveEntry) {
                PatternAndAge v = new PatternAndAge();
                v.setValue(dbresult.getString(1));
                v.setIsRegexp(dbresult.getInt(2) == 1 ? true : false);
                v.setAge(dbresult.getLong(3)*24*3600); // days -> secs
                result.add(dbresult.getString(0), v);
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
    public static Cursor queryResurveyByName(@NonNull SQLiteDatabase database, @Nullable String name) {
        if (name == null) {
            return database.rawQuery(ValidatorRulesDatabase.QUERY_RESURVEY_DEFAULT, null);
        } else {
            return database.rawQuery(ValidatorRulesDatabase.QUERY_RESURVEY_BY_NAME, new String[] { name });
        }
    }
    
    /**
     * Add an entry to the resurvey table
     * 
     * @param db        writable ruleset database
     * @param ruleSetId id of the rule set we are using
     * @param key       key of objects that should be age checked
     * @param value     value of objects that should be age checked
     * @param isRegexp TODO
     * @param days      how man days old the object should max be
     */
    public static void addResurvey(@NonNull SQLiteDatabase db, int ruleSetId, @NonNull String key, @Nullable String value, boolean isRegexp, int days) {
        ContentValues values = new ContentValues();
        values.put(RULESET_FIELD, ruleSetId);
        values.put(KEY_FIELD, key);
        values.put(VALUE_FIELD, value);
        values.put(ISREGEXP_FIELD, isRegexp ? 1 : 0);
        values.put(DAYS_FIELD, days);
        db.insert(RESURVEY_TABLE, null, values);       
    }

    /**
     * Update an existing resurvex entry
     * 
     * @param db    writable database
     * @param id    rowid of the resurvey entry
     * @param key   key of objects that should be age checked
     * @param value value of objects that should be age checked
     * @param isRegexp TODO
     * @param days  how man days old the object should max be
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
     * Return the default resurvey entries if any
     * 
     * @param database readable database
     * @return a Map of the key-value tupels
     */
    @Nullable
    public static Map<String,Boolean> getDefaultCheck(@NonNull SQLiteDatabase database) {
        Map<String,Boolean> result = null;
        Cursor dbresult = database.query(CHECK_TABLE, new String[] { KEY_FIELD, OPTIONAL_FIELD},
                RULESET_FIELD + " = " +  DEFAULT_RULESET, null, null, null, KEY_FIELD);

        if (dbresult.getCount() >= 1) {
            result = new HashMap<>();
            boolean haveEntry = dbresult.moveToFirst();
            while (haveEntry) {
                result.put(dbresult.getString(0), dbresult.getInt(1)==1);
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
     * @param db        writable ruleset database
     * @param ruleSetId id of the rule set we are using
     * @param key       key of objects that should be age checked
     * @param optional  if true check against optional tags too
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
     * @param db    writable database
     * @param id    rowid of the check entry
     * @param key   key of objects that should be age checked
     * @param optional  if true check against optional tags too
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
