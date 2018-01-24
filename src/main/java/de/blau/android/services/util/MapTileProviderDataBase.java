package de.blau.android.services.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.acra.ACRA;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteFullException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pools;
import android.util.Log;
import de.blau.android.prefs.Preferences;
import de.blau.android.services.exceptions.EmptyCacheException;
import de.blau.android.views.util.MapViewConstants;

/**
 * The OpenStreetMapTileProviderDataBase contains a table with info for the available renderers and one for the
 * available tiles in the file system cache.<br/>
 * This class was taken from OpenStreetMapViewer (original package org.andnav.osm) in 2010 by Marcus Wolschon to be
 * integrated into the de.blau.androin OSMEditor.
 * 
 * @author Nicolas Gramlich
 * @author Marcus Wolschon <Marcus@Wolschon.biz
 * @author Simon Poole
 */
public class MapTileProviderDataBase implements MapViewConstants {

    private static final String DEBUG_TAG = "MapTilePro...DataBase";

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

    private static final String T_FSCACHE_SELECT_OLDEST     = "SELECT " + T_FSCACHE_RENDERER_ID + "," + T_FSCACHE_ZOOM_LEVEL + "," + T_FSCACHE_TILE_X + ","
            + T_FSCACHE_TILE_Y + "," + T_FSCACHE_FILESIZE + " FROM " + T_FSCACHE + " WHERE " + T_FSCACHE_FILESIZE + " > 0 ORDER BY " + T_FSCACHE_TIMESTAMP
            + " ASC";

    private static final String T_FSCACHE_GET = "SELECT " + T_FSCACHE_DATA + " FROM " + T_FSCACHE + " WHERE " + T_FSCACHE_WHERE;
    // ===========================================================
    // Fields
    // ===========================================================
    
    private final SQLiteDatabase  mDatabase;
 
    private static Pools.SynchronizedPool<SQLiteStatement> getStatements;

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
        mDatabase = new DatabaseHelper(context).getWritableDatabase();
        Preferences prefs = new Preferences(context);
        int maxThreads = prefs.getMaxTileDownloadThreads();
        getStatements = new Pools.SynchronizedPool<SQLiteStatement>(maxThreads);
        Log.i(DEBUG_TAG, "Allocating " + maxThreads + " prepared statements");
        for (int i = 0; i < maxThreads; i++) {
            getStatements.release(mDatabase.compileStatement(T_FSCACHE_GET));
        }
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
     * Check if a tile is invalid
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
     * Save tile data to the database, checks if it exists beforehand
     * 
     * @param aTile tile meta data
     * @param tile_data the tile image data
     * @return the size of the tile if successfully added
     * @throws IOException
     */
    public int addTile(@NonNull final MapTile aTile, @Nullable final byte[] tile_data) throws IOException {
        if (DEBUGMODE) {
            Log.d(MapTileFilesystemProvider.DEBUG_TAG, "adding " + aTile);
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
                long result = mDatabase.insertOrThrow(T_FSCACHE, null, cv);
                if (DEBUGMODE) {
                    Log.d(MapTileFilesystemProvider.DEBUG_TAG, "Inserting new tile result " + result);
                }
                return tile_data != null ? tile_data.length : 0;
            }
            return 0;
        } catch (SQLiteFullException | SQLiteDiskIOException sex) { // handle these the same
            throw new IOException(sex.getMessage());
        } catch (SQLiteConstraintException scex) {
            if (tile_data != null && isInvalid(aTile)) {
                Log.w(DEBUG_TAG, "Formerly invalid tile has become available " + aTile);
                // try to update tile with current data now that it has become available
                final ContentValues cv = new ContentValues();
                cv.put(T_FSCACHE_TIMESTAMP, System.currentTimeMillis());
                cv.put(T_FSCACHE_FILESIZE, tile_data.length);
                cv.put(T_FSCACHE_DATA, tile_data);
                long result = mDatabase.update(T_FSCACHE, cv, T_FSCACHE_WHERE, tileToWhereArgs(aTile));
                if (DEBUGMODE) {
                    Log.d(MapTileFilesystemProvider.DEBUG_TAG, "Inserting tile for invalid one result " + result);
                }
                return tile_data.length;
            } else {
                Log.w(DEBUG_TAG, "Constraint violated inserting tile " + aTile);
                return 0; // file was already inserted
            }
        }
    }

    /**
     * Get a SQLite argument array for a WHERE clause
     * 
     * @param aTile the tile meta-data
     * @return an array of strings with the args
     */
    private static String[] tileToWhereArgs(@NonNull MapTile aTile) {
        return new String[] { aTile.rendererID, Integer.toString(aTile.zoomLevel), Integer.toString(aTile.x), Integer.toString(aTile.y) };
    }

    /**
     * Returns requested tile and increases use count and date
     * 
     * @param aTile the tile meta data
     * @return the contents of the tile or null on failure to retrieve
     * @throws IOException
     */
    public byte[] getTile(@NonNull final MapTile aTile) throws IOException {
        if (DEBUGMODE) {
            Log.d(MapTileFilesystemProvider.DEBUG_TAG, "Trying to retrieve " + aTile + " from file");
        }
        try {
            if (mDatabase.isOpen()) {
                SQLiteStatement get = null;
                ParcelFileDescriptor pfd = null;
                try {
                    get = getStatements.acquire();
                    if (get == null) {
                        Log.e(DEBUG_TAG, "statement null");
                        return null;
                    }
                    get.bindString(1, aTile.rendererID);
                    get.bindLong(2, aTile.zoomLevel);
                    get.bindLong(3, aTile.x);
                    get.bindLong(4, aTile.y);
                    pfd = get.simpleQueryForBlobFileDescriptor();

                    ParcelFileDescriptor.AutoCloseInputStream acis = new ParcelFileDescriptor.AutoCloseInputStream(pfd);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = acis.read(buffer)) != -1) {
                        bos.write(buffer, 0, bytesRead);
                    }
                    acis.close();
                    return bos.toByteArray();
                } catch (SQLiteDoneException sde) {
                    // nothing found
                    return null;
                } finally {
                    getStatements.release(get);
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
        Log.d(DEBUG_TAG, "deleteOldest size needed " + pSizeNeeded);
        if (!mDatabase.isOpen()) { // this seems to happen, protect against crashing
            Log.e(MapTileFilesystemProvider.DEBUG_TAG, "deleteOldest called on closed DB");
            return 0;
        }
        final Cursor c = mDatabase.rawQuery(T_FSCACHE_SELECT_OLDEST, null);

        final List<MapTile> deleteFromDB = new ArrayList<>();
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
                        } while (c.moveToNext() && sizeGained < pSizeNeeded);
                    } else {
                        throw new EmptyCacheException("Cache seems to be empty.");
                    }

                    if (mDatabase.isOpen()) {
                        try {
                            mDatabase.beginTransaction();
                            for (MapTile t : deleteFromDB) {
                                mDatabase.delete(T_FSCACHE, T_FSCACHE_WHERE, tileToWhereArgs(t));
                            }
                            mDatabase.setTransactionSuccessful();
                        } finally {
                            mDatabase.endTransaction();
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
        Log.d(DEBUG_TAG, "deleteOldest size gained " + sizeGained);
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