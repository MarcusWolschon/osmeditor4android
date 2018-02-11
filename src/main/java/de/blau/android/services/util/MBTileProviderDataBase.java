package de.blau.android.services.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteFullException;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pools;
import android.util.Log;
import de.blau.android.prefs.Preferences;

/**
 * @author Simon Poole
 */
public class MBTileProviderDataBase {

    private static final String  DEBUG_TAG = "MBTilePro...DataBase";
    private static final boolean DEBUGMODE = false;

    private static final String T_MBTILES            = "tiles";
    private static final String T_MBTILES_ZOOM_LEVEL = "zoom_level";
    private static final String T_MBTILES_TILE_X     = "tile_column";
    private static final String T_MBTILES_TILE_Y     = "tile_row";
    private static final String T_MBTILES_DATA       = "tile_data";

    private static final String T_METADATA       = "metadata";
    private static final String T_METADATA_NAME  = "name";
    private static final String T_METADATA_VALUE = "value";

    private static final String SQL_ARG = "=?";
    private static final String AND     = " AND ";

    private static final String T_MBTILES_WHERE = T_MBTILES_ZOOM_LEVEL + SQL_ARG + AND + T_MBTILES_TILE_X + SQL_ARG + AND + T_MBTILES_TILE_Y + SQL_ARG;

    private static final String T_MBTILES_GET = "SELECT " + T_MBTILES_DATA + " FROM " + T_MBTILES + " WHERE " + T_MBTILES_WHERE;

    private static final String T_MBTILES_GET_ZOOMS = "SELECT DISTINCT " + T_MBTILES_ZOOM_LEVEL + " FROM " + T_MBTILES + " ORDER BY " + T_MBTILES_ZOOM_LEVEL;

    // ===========================================================
    // Fields
    // ===========================================================

    private final SQLiteDatabase mDatabase;

    private static Pools.SynchronizedPool<SQLiteStatement> getStatements;

    // ===========================================================
    // Constructors
    // ===========================================================
    /**
     * Construct a new database tile storage provider
     * 
     * @param context Android Context
     * @param mbTilesUri Uri for the mbtiles file
     */
    public MBTileProviderDataBase(@NonNull final Context context, @NonNull Uri mbTilesUri) {
        Log.i(DEBUG_TAG, "Creating database instance for " + mbTilesUri.getPath());
        String path = mbTilesUri.getPath();
        mDatabase = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY);
        Preferences prefs = new Preferences(context);
        int maxThreads = prefs.getMaxTileDownloadThreads();
        getStatements = new Pools.SynchronizedPool<SQLiteStatement>(maxThreads);
        Log.i(DEBUG_TAG, "Allocating " + maxThreads + " prepared statements");
        for (int i = 0; i < maxThreads; i++) {
            getStatements.release(mDatabase.compileStatement(T_MBTILES_GET));
        }
    }

    /**
     * Construct a new database tile storage provider
     * 
     * @param context Android Context
     * @param mbTilesUri Uri for the mbtiles file as a String
     */
    public MBTileProviderDataBase(@NonNull final Context context, @NonNull String mbTilesUri) {
        this(context, Uri.parse(mbTilesUri));
    }

    /**
     * Returns requested tile
     * 
     * @param aTile the tile meta data
     * @return the contents of the tile or null on failure to retrieve
     * @throws IOException
     */
    public byte[] getTile(@NonNull final MapTile aTile) throws IOException {
        if (DEBUGMODE) {
            Log.d(MapTileFilesystemProvider.DEBUG_TAG, "Trying to retrieve " + aTile + " from db");
        }
        try {
            if (mDatabase.isOpen()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    SQLiteStatement get = null;
                    ParcelFileDescriptor pfd = null;
                    try {
                        get = getStatements.acquire();
                        if (get == null) {
                            Log.e(DEBUG_TAG, "statement null");
                            return null;
                        }
                        int ymax = 1 << aTile.zoomLevel; // TMS scheme
                        int y = ymax - aTile.y - 1;
                        get.bindLong(1, aTile.zoomLevel);
                        get.bindLong(2, aTile.x);
                        get.bindLong(3, y);
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
                } else { // old and slow
                    final Cursor c = mDatabase.query(T_MBTILES, new String[] { T_MBTILES_DATA }, T_MBTILES_WHERE,
                            new String[] { aTile.rendererID, Integer.toString(aTile.zoomLevel), Integer.toString(aTile.x), Integer.toString(aTile.y) }, null,
                            null, null);
                    try {
                        if (c.moveToFirst()) {
                            byte[] tile_data = c.getBlob(c.getColumnIndexOrThrow(T_MBTILES_DATA));
                            return tile_data;
                        }
                    } finally {
                        c.close();
                    }
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
     * Get meta data from the file
     * 
     * @return a Map containing all key-value pairs from the meta data of null if none
     */
    @Nullable
    public Map<String, String> getMetadata() {
        Map<String, String> result = null;
        if (mDatabase.isOpen()) {
            Cursor dbresult = mDatabase.query(T_METADATA, null, null, null, null, null, null);
            if (dbresult.getCount() >= 1) {
                result = new HashMap<>();
                boolean haveEntry = dbresult.moveToFirst();
                while (haveEntry) {
                    String name = dbresult.getString(dbresult.getColumnIndex(T_METADATA_NAME));
                    String value = dbresult.getString(dbresult.getColumnIndex(T_METADATA_VALUE));
                    result.put(name, value);
                    haveEntry = dbresult.moveToNext();
                }
            }
            dbresult.close();
            return result;
        }
        if (DEBUGMODE) {
            Log.d(MapTileFilesystemProvider.DEBUG_TAG, "Tile not found in DB");
        }
        return null;
    }

    /**
     * Get min and max zoom from the existing tiles
     * 
     * This is likely rather expensive for a large number of tiles
     * @return an int array holding the min zoom in the first, max zoom in the second element or null
     */
    @Nullable
    public int[] getMinMaxZoom() {
        int[] result = null;
        if (mDatabase.isOpen()) {
            Cursor dbresult = mDatabase.rawQuery(T_MBTILES_GET_ZOOMS, null);
            if (dbresult.getCount() >= 1) {
                boolean haveEntry = dbresult.moveToFirst();
                if (haveEntry) {
                    result = new int[2];
                    result[0] = dbresult.getInt(dbresult.getColumnIndex(T_MBTILES_ZOOM_LEVEL));
                    haveEntry = dbresult.moveToLast();
                    result[1] = dbresult.getInt(dbresult.getColumnIndex(T_MBTILES_ZOOM_LEVEL));
                }
            }
            dbresult.close();
            return result;
        }
        if (DEBUGMODE) {
            Log.d(MapTileFilesystemProvider.DEBUG_TAG, "Tile not found in DB");
        }
        return null;
    }

    /**
     * Close the DB handle
     */
    public void close() {
        mDatabase.close();
    }
}