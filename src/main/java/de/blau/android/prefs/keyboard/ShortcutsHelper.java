package de.blau.android.prefs.keyboard;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import androidx.annotation.NonNull;
import de.blau.android.R;
import de.blau.android.prefs.keyboard.Shortcuts.Modifier;
import de.blau.android.prefs.keyboard.Shortcuts.Shortcut;
import de.blau.android.util.Util;

public class ShortcutsHelper extends SQLiteOpenHelper {
    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, ShortcutsHelper.class.getSimpleName().length());
    private static final String DEBUG_TAG = ShortcutsHelper.class.getSimpleName().substring(0, TAG_LEN);

    public static final String DATABASE_NAME    = "shortcuts";
    private static final int   DATABASE_VERSION = 1;

    private static final String SHORTCUT_ENTRIES_TABLE = "shortcutentries";
    private static final String MODIFIER_COL           = "modifier";
    private static final String CHARACTER_COL          = "character";
    private static final String ACTION_COL             = "action";
    private static final String WHERE                  = "metakey = ? AND character = ?";
    private static final String WHERE_ACTION           = "action = ?";

    private final Context ctx;

    /**
     * Create a SQLiteOpenHelper for the shortcut DB
     */
    public ShortcutsHelper(@NonNull Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.ctx = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(
                    "CREATE TABLE shortcutentries (modifier INTEGER NOT NULL, character INTEGER NOT NULL, action TEXT NOT NULL , PRIMARY KEY (modifier, character, action))");

            addShortcut(db, Modifier.NONE, Util.getShortCut(ctx, R.string.shortcut_zoom_in), ctx.getString(R.string.ACTION_ZOOM_IN));
            addShortcut(db, Modifier.NONE, Util.getShortCut(ctx, R.string.shortcut_zoom_out), ctx.getString(R.string.ACTION_ZOOM_OUT));

            addShortcut(db, Modifier.CTRL, Util.getShortCut(ctx, R.string.shortcut_help), ctx.getString(R.string.ACTION_HELP));
            addShortcut(db, Modifier.CTRL, Util.getShortCut(ctx, R.string.shortcut_undo), ctx.getString(R.string.ACTION_UNDO));
            addShortcut(db, Modifier.CTRL, Util.getShortCut(ctx, R.string.shortcut_gps_follow), ctx.getString(R.string.ACTION_GPS_FOLLOW));
            addShortcut(db, Modifier.CTRL, Util.getShortCut(ctx, R.string.shortcut_gps_goto), ctx.getString(R.string.ACTION_GPS_GOTO));
            addShortcut(db, Modifier.CTRL, Util.getShortCut(ctx, R.string.shortcut_download), ctx.getString(R.string.ACTION_DOWNLOAD));
            addShortcut(db, Modifier.CTRL, Util.getShortCut(ctx, R.string.shortcut_bugs_download), ctx.getString(R.string.ACTION_BUG_DOWNLOAD));
            addShortcut(db, Modifier.CTRL, Util.getShortCut(ctx, R.string.shortcut_paste), ctx.getString(R.string.ACTION_ELEMENT_PASTE));
            addShortcut(db, Modifier.CTRL, Util.getShortCut(ctx, R.string.shortcut_paste_tags), ctx.getString(R.string.ACTION_PASTE_TAGS));
            addShortcut(db, Modifier.CTRL, Util.getShortCut(ctx, R.string.shortcut_tagedit), ctx.getString(R.string.ACTION_TAGEDIT));
            addShortcut(db, Modifier.CTRL, Util.getShortCut(ctx, R.string.shortcut_info), ctx.getString(R.string.ACTION_INFO));
            addShortcut(db, Modifier.CTRL, Util.getShortCut(ctx, R.string.shortcut_copy), ctx.getString(R.string.ACTION_COPY));
            addShortcut(db, Modifier.CTRL, Util.getShortCut(ctx, R.string.shortcut_cut), ctx.getString(R.string.ACTION_CUT));
            addShortcut(db, Modifier.CTRL, Util.getShortCut(ctx, R.string.shortcut_square), ctx.getString(R.string.ACTION_SQUARE));
            addShortcut(db, Modifier.CTRL, Util.getShortCut(ctx, R.string.shortcut_follow), ctx.getString(R.string.ACTION_FOLLOW));
            addShortcut(db, Modifier.CTRL, Util.getShortCut(ctx, R.string.shortcut_address), ctx.getString(R.string.ACTION_ADDRESS));
            addShortcut(db, Modifier.CTRL, Util.getShortCut(ctx, R.string.shortcut_remove), ctx.getString(R.string.ACTION_DELETE));
            addShortcut(db, Modifier.CTRL, Util.getShortCut(ctx, R.string.shortcut_merge), ctx.getString(R.string.ACTION_MERGE));
            addShortcut(db, Modifier.CTRL, Util.getShortCut(ctx, R.string.shortcut_keep), ctx.getString(R.string.ACTION_KEEP));
        } catch (SQLException e) {
            Log.w(DEBUG_TAG, "Problem creating database", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // initial version
    }

    /**
     * Add a shortcut
     * 
     * @param modifier the Modifier
     * @param character the shortcut char
     * @param action the action to execute
     */
    public synchronized void addShortcut(@NonNull Modifier modifier, @NonNull char character, @NonNull String action) {
        SQLiteDatabase db = getWritableDatabase();
        addShortcut(db, modifier, character, action);
        db.close();
    }

    /**
     * Add a shortcut
     * 
     * @param db a writable DB
     * @param modifier the Modifier
     * @param character the shortcut char
     * @param action the action to execute
     */
    private synchronized void addShortcut(@NonNull SQLiteDatabase db, @NonNull Modifier modifier, @NonNull char character, @NonNull String action) {
        ContentValues values = setShortcutValues(modifier, character, action);
        db.insert(SHORTCUT_ENTRIES_TABLE, null, values);
    }

    /**
     * Update a shortcut
     * 
     * @param shortcut shortcut object with the new values
     */
    public void updateShortcut(@NonNull Shortcut shortcut) {
        updateShortcut(shortcut.getModifier(), shortcut.getCharacter(), shortcut.getActionRef());
    }

    /**
     * Update a shortcut
     * 
     * @param modifier the Modifier
     * @param character the shortcut char
     * @param action the action to execute
     */
    public synchronized void updateShortcut(@NonNull Modifier modifier, @NonNull char character, @NonNull String action) {
        Log.d(DEBUG_TAG, "Updating " + action + " to " + modifier + " " + character); // NOSONAR
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = setShortcutValues(modifier, character, action);
        db.update(SHORTCUT_ENTRIES_TABLE, values, WHERE_ACTION, new String[] { action });
        db.close();
    }

    /**
     * Create sqlite args for a where condition
     * 
     * @param modifier the Metakey
     * @param character the char for the short cut
     * @return an array with the args
     */
    private String[] whereArgs(Modifier modifier, char character) {
        return new String[] { Integer.toString(modifier.ordinal()), Integer.toString(character) };
    }

    /**
     * Allocate a ContentValues object and set the values
     *
     * @param modifier the Modifier
     * @param character the shortcut char
     * @param action the action to execute
     * @return a ContentValues object
     */
    private ContentValues setShortcutValues(@NonNull Modifier modifier, @NonNull char character, @NonNull String action) {
        ContentValues values = new ContentValues();
        values.put(MODIFIER_COL, modifier.ordinal());
        values.put(CHARACTER_COL, (int) character);
        values.put(ACTION_COL, action);
        return values;
    }

    /**
     * Get all shortcuts
     * 
     * @param db the database
     * @return an array of Shortcut objects
     */
    @NonNull
    public synchronized Shortcut[] getShortcuts(@NonNull SQLiteDatabase db) {
        Cursor dbresult = db.query(SHORTCUT_ENTRIES_TABLE, new String[] { MODIFIER_COL, CHARACTER_COL, ACTION_COL }, null, null, null, null, null, null);
        Shortcut[] result = new Shortcut[dbresult.getCount()];
        dbresult.moveToFirst();
        for (int i = 0; i < result.length; i++) {
            result[i] = new Shortcut(Modifier.values()[dbresult.getInt(0)], dbresult.getInt(1), dbresult.getString(2));
            dbresult.moveToNext();
        }
        dbresult.close();
        return result;
    }

    /**
     * Delete a shortcut
     * 
     * @param modifier the Modifier
     * @param character the shortcut char
     */
    public synchronized void deleteShortcut(@NonNull Modifier modifier, @NonNull char character) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(SHORTCUT_ENTRIES_TABLE, WHERE, whereArgs(modifier, character));
        db.close();
    }
}
