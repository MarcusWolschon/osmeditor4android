package de.blau.android.imageryoffset;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.util.Log;

public class ImageryOffsetDatabase extends SQLiteOpenHelper {
    private static final String DEBUG_TAG        = "OffsetDatabase";
    public static final String  DATABASE_NAME    = "offsets";
    private static final int    DATABASE_VERSION = 1;

    private static final String OFFSETS_TABLE     = "offsets";
    private static final String IMAGERY_ID_FIELD  = "imagery_id";
    private static final String LON_FIELD         = "lon";
    private static final String LAT_FIELD         = "lat";
    private static final String MIN_ZOOM_FIELD    = "min_zoom";
    private static final String MAX_ZOOM_FIELD    = "max_zoom";
    private static final String IMAGERY_LON_FIELD = "imagery_lon";
    private static final String IMAGERY_LAT_FIELD = "imagery_lat";

    static final String QUERY_LAYER_BY_ROWID = "SELECT * FROM layers WHERE rowid=?";

    /**
     * Create a new instance of OffsetDatabase creating the underlying DB is necessary
     * 
     * @param context Android Context
     */
    public ImageryOffsetDatabase(final Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {

            db.execSQL("CREATE TABLE offsets (imagery_id TEXT NOT NULL, lon NUMBER NOT NULL, lat NUMBER NOT NULL, "
                    + " min_zoom INTEGER NOT NULL, max_zoom INTEGER NOT NULL, imagery_lon NUMBER NOT NULL, imagery_lat NUMBER NOT NULL)");
            db.execSQL("CREATE INDEX imagery_idx ON offsets(imagery_id)");
        } catch (SQLException e) {
            Log.w(DEBUG_TAG, "Problem creating database", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(DEBUG_TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
    }

    /**
     * Add a layer, will add coverage areas to the coverage table
     * 
     * @param db writable database
     * @param offset a Offset object
     */
    public static void addOffset(@NonNull SQLiteDatabase db, @NonNull ImageryOffset offset) {
        ContentValues values = getContentValuesForOffset(offset);
        try {
            db.insert(OFFSETS_TABLE, null, values);
        } catch (SQLiteConstraintException e) {
            Log.e(DEBUG_TAG, "Constraint exception " + e.getMessage());
        }
    }

    /**
     * Get an ContentValues object suitable for insertion or an update of a layer
     * 
     * @param offset a Offset object
     * @return a ContentValues object
     */
    private static ContentValues getContentValuesForOffset(@NonNull ImageryOffset offset) {
        ContentValues values = new ContentValues();
        values.put(IMAGERY_ID_FIELD, offset.imageryId);
        values.put(LON_FIELD, offset.getLon());
        values.put(LAT_FIELD, offset.getLat());
        values.put(MIN_ZOOM_FIELD, offset.getMinZoom());
        values.put(MAX_ZOOM_FIELD, offset.getMaxZoom());
        values.put(IMAGERY_LON_FIELD, offset.getImageryLon());
        values.put(IMAGERY_LAT_FIELD, offset.getImageryLat());
        return values;
    }

    /**
     * Retrieve offsets for specific imagery identified by its id
     * 
     * @param context Android Context
     * @param db readable SQLiteDatabase
     * @param id imagery offset db id
     * @return a List of Offsets instances
     */
    public static List<ImageryOffset> getOffsets(@NonNull Context context, @NonNull SQLiteDatabase db, @NonNull String id) {
        return getOffsets(db, id);
    }

    /**
     * Retrieve offsets for specific imagery identified by its id
     * @param db readable SQLiteDatabase
     * @param id imagery offset db id
     * 
     * @return a List of Offsets instances
     */
    public static List<ImageryOffset> getOffsets(@NonNull SQLiteDatabase db, @NonNull String id) {
        List<ImageryOffset>result = new ArrayList<>();
        Cursor dbresult = db.query(OFFSETS_TABLE, null, IMAGERY_ID_FIELD + "='" + id + "'", null, null, null, null);

        if (dbresult.getCount() >= 1) {
            boolean haveEntry = dbresult.moveToFirst();
            while (haveEntry) {
                ImageryOffset offset = getOffsetFromCursor(dbresult);
                result.add(offset);
                haveEntry = dbresult.moveToNext();
            }
        }
        dbresult.close();
        return result;
    }
    
    /**
     * Create an Offset from a database entry
     * 
     * @param cursor the Cursor
     * @return an Offset instance
     */
    private static ImageryOffset getOffsetFromCursor(Cursor cursor) {
        ImageryOffset offset = new ImageryOffset();
        offset.imageryId = cursor.getString(cursor.getColumnIndex(IMAGERY_ID_FIELD));
        offset.setLon(cursor.getDouble(cursor.getColumnIndex(LON_FIELD)));
        offset.setLat(cursor.getDouble(cursor.getColumnIndex(LAT_FIELD)));
        offset.setMinZoom(cursor.getInt(cursor.getColumnIndex(MIN_ZOOM_FIELD)));
        offset.setMaxZoom(cursor.getInt(cursor.getColumnIndex(MAX_ZOOM_FIELD)));
        offset.setImageryLon(cursor.getDouble(cursor.getColumnIndex(IMAGERY_LON_FIELD)));
        offset.setImageryLat(cursor.getDouble(cursor.getColumnIndex(IMAGERY_LAT_FIELD)));
 
        return offset;
    }
    
    /**
     * Delete a specific source which will delete all layers from that source
     * 
     * @param db writable database
     * @param id imagery offset db id
     */
    public static void deleteOffset(final SQLiteDatabase db, @NonNull String id) {
        db.delete(OFFSETS_TABLE, IMAGERY_ID_FIELD + "=?", new String[] { id });
    }

}
