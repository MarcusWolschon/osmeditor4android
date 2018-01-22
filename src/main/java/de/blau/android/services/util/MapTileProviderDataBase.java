package de.blau.android.services.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.acra.ACRA;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteFullException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import de.blau.android.R;
import de.blau.android.services.exceptions.EmptyCacheException;
import de.blau.android.util.Snack;
import de.blau.android.views.util.MapViewConstants;

/**
 * The OpenStreetMapTileProviderDataBase contains a table with info for the available renderers and one for the
 * available tiles in the file system cache.<br/>
 * This class was taken from OpenStreetMapViewer (original package org.andnav.osm) in 2010 by Marcus Wolschon to be
 * integrated into the de.blau.androin OSMEditor.
 * 
 * @author Nicolas Gramlich
 * @author Marcus Wolschon <Marcus@Wolschon.biz
 */
public class MapTileProviderDataBase implements MapViewConstants {

    private static final String DEBUG_TAG = "MapTileProviderDataBase";

    private static final String DATABASE_NAME    = "osmaptilefscache_db";
    private static final int    DATABASE_VERSION = 8;

    private static final String T_FSCACHE             = "tiles";
    private static final String T_FSCACHE_RENDERER_ID = "rendererID";
    private static final String T_FSCACHE_ZOOM_LEVEL  = "zoom_level";
    private static final String T_FSCACHE_TILE_X      = "tile_column";
    private static final String T_FSCACHE_TILE_Y      = "tile_row";
    // private static final String T_FSCACHE_LINK = "link"; // TODO store link (multiple use for similar tiles)
    private static final String T_FSCACHE_TIMESTAMP  = "timestamp";
    private static final String T_FSCACHE_USAGECOUNT = "countused";
    private static final String T_FSCACHE_FILESIZE   = "filesize";
    private static final String T_FSCACHE_DATA       = "tile_data";

    private static final String T_RENDERER               = "t_renderer";
    private static final String T_RENDERER_ID            = "id";
    private static final String T_RENDERER_NAME          = "name";
    private static final String T_RENDERER_BASE_URL      = "base_url";
    private static final String T_RENDERER_ZOOM_MIN      = "zoom_min";
    private static final String T_RENDERER_ZOOM_MAX      = "zoom_max";
    private static final String T_RENDERER_TILE_SIZE_LOG = "tile_size_log";

    private static final String T_FSCACHE_CREATE_COMMAND = "CREATE TABLE IF NOT EXISTS " + T_FSCACHE + " (" + T_FSCACHE_RENDERER_ID + " VARCHAR(255) NOT NULL,"
            + T_FSCACHE_ZOOM_LEVEL + " INTEGER NOT NULL," + T_FSCACHE_TILE_X + " INTEGER NOT NULL," + T_FSCACHE_TILE_Y + " INTEGER NOT NULL,"
            + T_FSCACHE_TIMESTAMP + " INTEGER NOT NULL," + T_FSCACHE_USAGECOUNT + " INTEGER NOT NULL DEFAULT 1," + T_FSCACHE_FILESIZE + " INTEGER NOT NULL,"
            + T_FSCACHE_DATA + " BLOB," + " PRIMARY KEY(" + T_FSCACHE_RENDERER_ID + "," + T_FSCACHE_ZOOM_LEVEL + "," + T_FSCACHE_TILE_X + "," + T_FSCACHE_TILE_Y
            + ")" + ");";

    private static final String T_RENDERER_CREATE_COMMAND = "CREATE TABLE IF NOT EXISTS " + T_RENDERER + " (" + T_RENDERER_ID + " VARCHAR(255) PRIMARY KEY,"
            + T_RENDERER_NAME + " VARCHAR(255)," + T_RENDERER_BASE_URL + " VARCHAR(255)," + T_RENDERER_ZOOM_MIN + " INTEGER NOT NULL," + T_RENDERER_ZOOM_MAX
            + " INTEGER NOT NULL," + T_RENDERER_TILE_SIZE_LOG + " INTEGER NOT NULL" + ");";

    private static final String SQL_ARG = "=?";
    private static final String AND     = " AND ";

    private static final String T_FSCACHE_WHERE = T_FSCACHE_RENDERER_ID + SQL_ARG + AND + T_FSCACHE_ZOOM_LEVEL + SQL_ARG + AND + T_FSCACHE_TILE_X + SQL_ARG
            + AND + T_FSCACHE_TILE_Y + SQL_ARG;

    private static final String T_FSCACHE_WHERE_INVALID = T_FSCACHE_RENDERER_ID + SQL_ARG + AND + T_FSCACHE_ZOOM_LEVEL + SQL_ARG + AND + T_FSCACHE_TILE_X
            + SQL_ARG + AND + T_FSCACHE_TILE_Y + SQL_ARG + AND + T_FSCACHE_FILESIZE + "=0";

    private static final String T_FSCACHE_WHERE_NOT_INVALID = T_FSCACHE_RENDERER_ID + SQL_ARG + AND + T_FSCACHE_ZOOM_LEVEL + SQL_ARG + AND + T_FSCACHE_TILE_X
            + SQL_ARG + AND + T_FSCACHE_TILE_Y + SQL_ARG + AND + T_FSCACHE_FILESIZE + ">0";

    private static final String T_FSCACHE_SELECT_LEAST_USED = "SELECT " + T_FSCACHE_RENDERER_ID + "," + T_FSCACHE_ZOOM_LEVEL + "," + T_FSCACHE_TILE_X + ","
            + T_FSCACHE_TILE_Y + "," + T_FSCACHE_FILESIZE + " FROM " + T_FSCACHE + " WHERE " + T_FSCACHE_USAGECOUNT + " = (SELECT MIN(" + T_FSCACHE_USAGECOUNT
            + ") FROM " + T_FSCACHE + ")";
    private static final String T_FSCACHE_SELECT_OLDEST     = "SELECT " + T_FSCACHE_RENDERER_ID + "," + T_FSCACHE_ZOOM_LEVEL + "," + T_FSCACHE_TILE_X + ","
            + T_FSCACHE_TILE_Y + "," + T_FSCACHE_FILESIZE + " FROM " + T_FSCACHE + " WHERE " + T_FSCACHE_FILESIZE + " > 0 ORDER BY " + T_FSCACHE_TIMESTAMP
            + " ASC";

    private static final String T_FSCACHE_INCREMENT_USE = "UPDATE " + T_FSCACHE + " SET " + T_FSCACHE_USAGECOUNT + "=" + T_FSCACHE_USAGECOUNT + "+1, "
            + T_FSCACHE_TIMESTAMP + "=" + SQL_ARG + " WHERE " + T_FSCACHE_WHERE;

    // ===========================================================
    // Fields
    // ===========================================================

    private final Context         mCtx;
    private final SQLiteDatabase  mDatabase;
    private final SQLiteStatement incrementUse;

    // ===========================================================
    // Constructors
    // ===========================================================
    /**
     * Construct a new database tile storage provider
     * 
     * @param context Android Context
     */
    public MapTileProviderDataBase(@NonNull final Context context) {
        Log.i(DEBUG_TAG, "creating database instance");
        mCtx = context;
        mDatabase = new DatabaseHelper(context).getWritableDatabase();

        incrementUse = mDatabase.compileStatement(T_FSCACHE_INCREMENT_USE);
    }

    /**
     * Check if a tile is present in the database
     * 
     * @param aTile the tile meta data
     * @return true if the tile exists in the database
     */
    public boolean hasTile(@NonNull final MapTile aTile) {
        boolean existed = false;
        if (mDatabase.isOpen()) {
            final String[] args = new String[] { aTile.rendererID, Integer.toString(aTile.zoomLevel), Integer.toString(aTile.x), Integer.toString(aTile.y) };
            final Cursor c = mDatabase.query(T_FSCACHE, new String[] { T_FSCACHE_RENDERER_ID }, T_FSCACHE_WHERE, args, null, null, null);
            existed = c.getCount() > 0;
            c.close();
        }
        return existed;
    }

    /**
     * CHekc if a tile is invalid
     * 
     * @param aTile the tile meta data
     * @return true if this is an invalid tile
     */
    public boolean isInvalid(@NonNull final MapTile aTile) {
        boolean existed = false;
        if (mDatabase.isOpen()) {
            final String[] args = new String[] { aTile.rendererID, Integer.toString(aTile.zoomLevel), Integer.toString(aTile.x), Integer.toString(aTile.y) };
            final Cursor c = mDatabase.query(T_FSCACHE, new String[] { T_FSCACHE_RENDERER_ID }, T_FSCACHE_WHERE_INVALID, args, null, null, null);
            existed = c.getCount() > 0;
            c.close();
        }
        return existed;
    }

    /**
     * Increment use counter for a specific tile
     * 
     * @param aTile the tile meta data
     * @return true if the count was updated
     * @throws SQLiteFullException
     * @throws SQLiteDiskIOException
     */
    private boolean incrementUse(@NonNull final MapTile aTile) throws SQLiteFullException, SQLiteDiskIOException {
        boolean ret = false;
        if (mDatabase.isOpen()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                try {
                    incrementUse.bindLong(1, System.currentTimeMillis());
                    incrementUse.bindString(2, aTile.rendererID);
                    incrementUse.bindLong(3, aTile.zoomLevel);
                    incrementUse.bindLong(4, aTile.x);
                    incrementUse.bindLong(5, aTile.y);
                    return incrementUse.executeUpdateDelete() >= 1; // > 1 is naturally an error, but safe to return
                                                                    // true here
                } catch (Exception e) {
                    if (e instanceof SQLiteFullException) {
                        // database/disk is full
                        Log.e(MapTileFilesystemProvider.DEBUG_TAG, "Tile database full");
                        Snack.toastTopError(mCtx, R.string.toast_tile_database_full);
                        throw new SQLiteFullException(e.getMessage());
                    } else if (e instanceof SQLiteDiskIOException) {
                        throw new SQLiteDiskIOException(e.getMessage());
                    }
                    ACRA.getErrorReporter().putCustomData("STATUS", "NOCRASH");
                    ACRA.getErrorReporter().handleException(e);
                    return true; // this will indicate that the tile is in the DB which is erring on the safe side
                }
            } else {
                final String[] args = new String[] { aTile.rendererID, Integer.toString(aTile.zoomLevel), Integer.toString(aTile.x),
                        Integer.toString(aTile.y) };
                Cursor c = mDatabase.query(T_FSCACHE, new String[] { T_FSCACHE_USAGECOUNT }, T_FSCACHE_WHERE, args, null, null, null);
                try {
                    if (DEBUGMODE) {
                        Log.d(MapTileFilesystemProvider.DEBUG_TAG, "incrementUse found " + c.getCount() + " entries");
                    }
                    if (c.getCount() == 1) {
                        try {
                            c.moveToFirst();
                            int usageCount = c.getInt(c.getColumnIndexOrThrow(T_FSCACHE_USAGECOUNT));
                            ContentValues cv = new ContentValues();
                            if (DEBUGMODE) {
                                Log.d(MapTileFilesystemProvider.DEBUG_TAG, "incrementUse count " + usageCount);
                            }
                            cv.put(T_FSCACHE_USAGECOUNT, usageCount + 1);
                            cv.put(T_FSCACHE_TIMESTAMP, System.currentTimeMillis());
                            ret = mDatabase.update(T_FSCACHE, cv, T_FSCACHE_WHERE, args) > 0;
                            if (DEBUGMODE) {
                                Log.d(MapTileFilesystemProvider.DEBUG_TAG, "incrementUse count " + usageCount + " update sucessful " + ret);
                            }
                        } catch (Exception e) {
                            if (e instanceof NullPointerException) {
                                // just log ... likely these are really spurious
                                Log.e(MapTileFilesystemProvider.DEBUG_TAG, "NPE in incrementUse");
                            } else if (e instanceof SQLiteFullException) {
                                // database/disk is full
                                Log.e(MapTileFilesystemProvider.DEBUG_TAG, "Tile database full");
                                Snack.toastTopError(mCtx, R.string.toast_tile_database_full);
                                throw new SQLiteFullException(e.getMessage());
                            } else if (e instanceof SQLiteDiskIOException) {
                                throw new SQLiteDiskIOException(e.getMessage());
                            } else {
                                ACRA.getErrorReporter().putCustomData("STATUS", "NOCRASH");
                                ACRA.getErrorReporter().handleException(e);
                            }
                        }
                    }
                } finally {
                    c.close();
                }
            }
        } else if (DEBUGMODE) {
            Log.d(MapTileFilesystemProvider.DEBUG_TAG, "incrementUse database not open");
        }
        return ret;
    }

    /**
     * Save tile data to the database, checks if it exists beforehand
     * 
     * @param aTile tile meta data
     * @param tile_data the tile image data
     * @return the size of the tile if successfully added
     * @throws IOException
     */
    public int addTile(@NonNull final MapTile aTile, @Nullable final byte[] tile_data) throws IOException {
        if (DEBUGMODE) {
            Log.d(MapTileFilesystemProvider.DEBUG_TAG, "adding or incrementing use " + aTile);
        }
        try {
            if (mDatabase.isOpen()) {
                final ContentValues cv = new ContentValues();
                cv.put(T_FSCACHE_RENDERER_ID, aTile.rendererID);
                cv.put(T_FSCACHE_ZOOM_LEVEL, aTile.zoomLevel);
                cv.put(T_FSCACHE_TILE_X, aTile.x);
                cv.put(T_FSCACHE_TILE_Y, aTile.y);
                cv.put(T_FSCACHE_TIMESTAMP, System.currentTimeMillis());
                cv.put(T_FSCACHE_FILESIZE, tile_data != null ? tile_data.length : 0); // 0 == invalid
                cv.put(T_FSCACHE_DATA, tile_data);
                long result = mDatabase.insert(T_FSCACHE, null, cv);
                if (DEBUGMODE) {
                    Log.d(MapTileFilesystemProvider.DEBUG_TAG, "Inserting new tile result " + result);
                }
                return tile_data != null ? tile_data.length : 0;
            }
            return 0;
        } catch (SQLiteFullException | SQLiteDiskIOException sex) { // handle these the same
            throw new IOException(sex.getMessage());
        } catch (SQLiteConstraintException scex) {
            Log.w(DEBUG_TAG, "Constraint violated inserting tile " + scex.getMessage());
            return 0; // file was already inserted
        }
    }

    /**
     * Returns requested tile and increases use count and date
     * 
     * @param aTile the tile meta data
     * @return the contents of the tile or null on failure to retrieve
     * @throws IOException
     */
    public byte[] getTile(@NonNull final MapTile aTile) throws IOException {
        // there seems to be danger for a race condition here
        if (DEBUGMODE) {
            Log.d(MapTileFilesystemProvider.DEBUG_TAG, "Trying to retrieve " + aTile + " from file");
        }
        try {
            if (incrementUse(aTile)) { // checks if DB is open
                final String[] args = new String[] { aTile.rendererID, Integer.toString(aTile.zoomLevel), Integer.toString(aTile.x),
                        Integer.toString(aTile.y) };
                final Cursor c = mDatabase.query(T_FSCACHE, new String[] { T_FSCACHE_DATA }, T_FSCACHE_WHERE_NOT_INVALID, args, null, null, null);
                try {
                    if (c.moveToFirst()) {
                        byte[] tile_data = c.getBlob(c.getColumnIndexOrThrow(T_FSCACHE_DATA));
                        if (DEBUGMODE) {
                            Log.d(MapTileFilesystemProvider.DEBUG_TAG, "Sucessfully retrieved " + aTile + " from file");
                        }
                        return tile_data;
                    } else if (DEBUGMODE) {
                        Log.d(MapTileFilesystemProvider.DEBUG_TAG, "Tile not found but should be 2");
                    }
                } finally {
                    c.close();
                }
            }
        } catch (SQLiteDiskIOException sioex) { // handle these exceptions the same
            throw new IOException(sioex.getMessage());
        } catch (SQLiteFullException sdfex) {
            throw new IOException(sdfex.getMessage());
        }
        if (DEBUGMODE) {
            Log.d(MapTileFilesystemProvider.DEBUG_TAG, "Tile not found in DB");
        }
        return null;
    }

    /**
     * Remove old tiles until enough space is present
     * 
     * @param pSizeNeeded the extra size we need
     * @return the size we actually gained
     */
    synchronized long deleteOldest(final int pSizeNeeded) {
        if (!mDatabase.isOpen()) { // this seems to happen, protect against crashing
            Log.e(MapTileFilesystemProvider.DEBUG_TAG, "deleteOldest called on closed DB");
            return 0;
        }
        final Cursor c = mDatabase.rawQuery(T_FSCACHE_SELECT_OLDEST, null);

        final ArrayList<MapTile> deleteFromDB = new ArrayList<>();
        long sizeGained = 0;
        if (c != null) {
            try {
                MapTile tileToBeDeleted;
                try {
                    if (c.moveToFirst()) {
                        do {
                            final int sizeItem = c.getInt(c.getColumnIndexOrThrow(T_FSCACHE_FILESIZE));
                            sizeGained += sizeItem;

                            tileToBeDeleted = new MapTile(c.getString(c.getColumnIndexOrThrow(T_FSCACHE_RENDERER_ID)),
                                    c.getInt(c.getColumnIndexOrThrow(T_FSCACHE_ZOOM_LEVEL)), c.getInt(c.getColumnIndexOrThrow(T_FSCACHE_TILE_X)),
                                    c.getInt(c.getColumnIndexOrThrow(T_FSCACHE_TILE_Y)));

                            deleteFromDB.add(tileToBeDeleted);
                            Log.d(DEBUG_TAG, "deleteOldest " + tileToBeDeleted.toString());

                        } while (c.moveToNext() && sizeGained < pSizeNeeded);
                    } else {
                        throw new EmptyCacheException("Cache seems to be empty.");
                    }

                    for (MapTile t : deleteFromDB) {
                        if (mDatabase.isOpen()) {
                            final String[] args = new String[] { t.rendererID, Integer.toString(t.zoomLevel), Integer.toString(t.x), Integer.toString(t.y) };
                            mDatabase.delete(T_FSCACHE, T_FSCACHE_WHERE, args);
                        } else {
                            break; // if the database is closed abort
                        }
                    }
                } catch (Exception e) {
                    if (e instanceof NullPointerException) {
                        // just log ... likely these are really spurious
                        Log.e(MapTileFilesystemProvider.DEBUG_TAG, "NPE in deleteOldest " + e);
                    } else if (e instanceof SQLiteFullException || e instanceof SQLiteDiskIOException || e instanceof java.lang.IllegalStateException) {
                        Log.e(MapTileFilesystemProvider.DEBUG_TAG, "Exception in deleteOldest " + e);
                    } else if (e instanceof EmptyCacheException) {
                        Log.e(MapTileFilesystemProvider.DEBUG_TAG, "Exception in deleteOldest cache empty " + e);
                    } else {
                        ACRA.getErrorReporter().putCustomData("STATUS", "NOCRASH");
                        ACRA.getErrorReporter().handleException(e);
                    }
                }
            } finally {
                c.close();
            }
        }
        return sizeGained;
    }

    /**
     * Delete all tiles from cache for a specific renderer
     * 
     * @param rendererID the tile server for which to remove the tiles or null to remove all tiles
     * @throws EmptyCacheException
     */
    synchronized public void flushCache(@Nullable String rendererID) throws EmptyCacheException {
        mDatabase.beginTransaction();
        try {
            if (rendererID == null) {
                Log.d(MapTileFilesystemProvider.DEBUG_TAG, "Flushing all caches");
                mDatabase.execSQL("DELETE FROM " + T_FSCACHE);
            } else {
                Log.d(MapTileFilesystemProvider.DEBUG_TAG, "Flushing cache for " + rendererID);
                final Cursor c = mDatabase
                        .rawQuery(
                                "SELECT " + T_FSCACHE_ZOOM_LEVEL + "," + T_FSCACHE_TILE_X + "," + T_FSCACHE_TILE_Y + "," + T_FSCACHE_FILESIZE + " FROM "
                                        + T_FSCACHE + " WHERE " + T_FSCACHE_RENDERER_ID + "='" + rendererID + "' ORDER BY " + T_FSCACHE_TIMESTAMP + " ASC",
                                null);
                final ArrayList<MapTile> deleteFromDB = new ArrayList<>();
                long sizeGained = 0;
                if (c != null) {
                    try {
                        MapTile tileToBeDeleted;
                        if (c.moveToFirst()) {
                            do {
                                final int sizeItem = c.getInt(c.getColumnIndexOrThrow(T_FSCACHE_FILESIZE));
                                sizeGained += sizeItem;

                                tileToBeDeleted = new MapTile(rendererID, c.getInt(c.getColumnIndexOrThrow(T_FSCACHE_ZOOM_LEVEL)),
                                        c.getInt(c.getColumnIndexOrThrow(T_FSCACHE_TILE_X)), c.getInt(c.getColumnIndexOrThrow(T_FSCACHE_TILE_Y)));

                                deleteFromDB.add(tileToBeDeleted);
                                // Log.d(DEBUG_TAG,"flushCache " + tileToBeDeleted.toString());
                            } while (c.moveToNext());
                        } else {
                            throw new EmptyCacheException("Cache seems to be empty.");
                        }
                        Log.d(DEBUG_TAG, "flushCache freed " + sizeGained);
                    } finally {
                        c.close();
                    }

                    for (MapTile t : deleteFromDB) {
                        final String[] args = new String[] { t.rendererID, Integer.toString(t.zoomLevel), Integer.toString(t.x), Integer.toString(t.y) };
                        mDatabase.delete(T_FSCACHE, T_FSCACHE_WHERE, args);
                    }
                }
            }
            mDatabase.setTransactionSuccessful();
        } finally {
            mDatabase.endTransaction();
        }
    }

    // ===========================================================
    // Methods
    // ===========================================================
    private String TMP_COLUMN = "tmp";

    /**
     * Get the current size of the cache in bytes
     * 
     * @return the current cache size
     */
    public int getCurrentFSCacheByteSize() {
        int ret = 0;
        if (mDatabase.isOpen()) {
            final Cursor c = mDatabase.rawQuery("SELECT SUM(" + T_FSCACHE_FILESIZE + ") AS " + TMP_COLUMN + " FROM " + T_FSCACHE, null);
            if (c != null) {
                if (c.moveToFirst()) {
                    ret = c.getInt(c.getColumnIndexOrThrow(TMP_COLUMN));
                }
                c.close();
            }
        }
        return ret;
    }

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================

    private class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(final Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            try {
                db.execSQL(T_RENDERER_CREATE_COMMAND);
                db.execSQL(T_FSCACHE_CREATE_COMMAND);
            } catch (SQLException e) {
                Log.w(MapTileFilesystemProvider.DEBUG_TAG, "Problem creating database", e);
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (DEBUGMODE)
                Log.w(MapTileFilesystemProvider.DEBUG_TAG,
                        "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");

            db.execSQL("DROP TABLE IF EXISTS " + T_FSCACHE);

            onCreate(db);
        }
    }

    /**
     * Close the DB handle
     */
    public void close() {
        mDatabase.close();
    }

    /**
     * Deletes the database
     * 
     * @param context Android Context
     */
    public static void delete(@NonNull final Context context) {
        Log.w(MapTileFilesystemProvider.DEBUG_TAG, "Deleting database " + DATABASE_NAME);
        context.deleteDatabase(DATABASE_NAME);
    }

    /**
     * Check if the database exists and can be read.
     * 
     * @param dir directory path
     * @return true if it exists and can be read and written, false if it doesn't
     */
    public static boolean exists(@NonNull File dir) {
        SQLiteDatabase checkDB = null;
        try {
            String path = dir.getAbsolutePath() + "/databases/" + DATABASE_NAME + ".db";
            checkDB = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READWRITE);
            checkDB.close();
        } catch (Exception e) {
            // database doesn't exist yet.
            // NOTE this originally caught just SQLiteException however this seems to cause issues with some Android
            // versions
        }
        return checkDB != null;
    }
}