package de.blau.android.filter;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import androidx.annotation.NonNull;

public class TagFilterDatabaseHelper extends SQLiteOpenHelper {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, TagFilterDatabaseHelper.class.getSimpleName().length());
    private static final String DEBUG_TAG = TagFilterDatabaseHelper.class.getSimpleName().substring(0, TAG_LEN);

    public static final String DATABASE_NAME    = "tagfilters";
    private static final int   DATABASE_VERSION = 3;

    public static final String FILTERENTRIES_TABLE = "filterentries";
    static final String        FILTER_COLUMN       = "filter";

    public static final String  FILTER_NAME_TABLE = "filters";
    private static final String NAME_QUERY        = "SELECT rowid as _id,name FROM filters";
    private static final String CURRENT_QUERY     = "SELECT name FROM filters where current = 1";
    static final String         NAME_COLUMN       = "name";
    static final String         CURRENT_COLUMN    = "current";

    /**
     * Create a new DatabaseFelber for the tag filter
     * 
     * @param context an Android Context
     */
    public TagFilterDatabaseHelper(final Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE TABLE filters (name TEXT, current INTEGER DEFAULT 0)");
            db.execSQL("INSERT INTO filters VALUES ('" + TagFilter.DEFAULT_FILTER + "', 1)");
            db.execSQL("CREATE TABLE filterentries (filter TEXT, include INTEGER DEFAULT 0,"
                    + "type TEXT DEFAULT '*', key TEXT DEFAULT '*', value TEXT DEFAULT '*',"
                    + "active INTEGER DEFAULT 0, FOREIGN KEY(filter) REFERENCES filters(name))");
        } catch (SQLException e) {
            Log.w(DEBUG_TAG, "Problem creating database", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion <= 2) {
            db.execSQL("ALTER TABLE filters ADD COLUMN current INTEGER DEFAULT 0");
        }
    }

    /**
     * Get a Cursor for filter names
     * 
     * @return a new cursor
     */
    @NonNull
    static String[] getFilterNames(@NonNull Context context, @NonNull SQLiteDatabase db) {
        List<String> names = new ArrayList<>();
        try (Cursor cursor = db.rawQuery(NAME_QUERY, null)) {
            if (cursor.moveToFirst()) {
                do {
                    names.add(TagFilterActivity.getFilterName(context, cursor.getString(cursor.getColumnIndexOrThrow(NAME_COLUMN))));
                } while (cursor.moveToNext());
            }
        }
        return names.toArray(new String[1]);
    }

    static void addFilterName(@NonNull SQLiteDatabase db, @NonNull String name) {
        ContentValues values = new ContentValues();
        values.put(NAME_COLUMN, name);
        db.insert(FILTER_NAME_TABLE, null, values);
    }

    public static void setCurrent(@NonNull SQLiteDatabase db, @NonNull String filter) {
        ContentValues values = new ContentValues();
        values.put(CURRENT_COLUMN, 0);
        db.update(FILTER_NAME_TABLE, values, null, null);
        values.put(CURRENT_COLUMN, 1);
        db.update(FILTER_NAME_TABLE, values, NAME_COLUMN + " =?", new String[] { filter });
    }

    @NonNull
    public static String getCurrent(@NonNull SQLiteDatabase db) {
        try (Cursor cursor = db.rawQuery(CURRENT_QUERY, null)) {
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(NAME_COLUMN));
            }
        }
        return TagFilter.DEFAULT_FILTER;
    }
}
