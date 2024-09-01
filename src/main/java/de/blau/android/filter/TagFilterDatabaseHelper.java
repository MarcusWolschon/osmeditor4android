package de.blau.android.filter;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class TagFilterDatabaseHelper extends SQLiteOpenHelper {
    private static final int    TAG_LEN          = Math.min(LOG_TAG_LEN, TagFilterDatabaseHelper.class.getSimpleName().length());
    private static final String DEBUG_TAG        = TagFilterDatabaseHelper.class.getSimpleName().substring(0, TAG_LEN);
    
    static final String         DATABASE_NAME    = "tagfilters";
    private static final int    DATABASE_VERSION = 2;

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
            db.execSQL("CREATE TABLE filters (name TEXT)");
            db.execSQL("INSERT INTO filters VALUES ('Default')");
            db.execSQL(
                    "CREATE TABLE filterentries (filter TEXT, include INTEGER DEFAULT 0, type TEXT DEFAULT '*', key TEXT DEFAULT '*', value TEXT DEFAULT '*', active INTEGER DEFAULT 0, FOREIGN KEY(filter) REFERENCES filters(name))");
        } catch (SQLException e) {
            Log.w(DEBUG_TAG, "Problem creating database", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(DEBUG_TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
    }
}
