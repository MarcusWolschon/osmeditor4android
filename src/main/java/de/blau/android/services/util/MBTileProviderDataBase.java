package de.blau.android.services.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pools;
import de.blau.android.App;
import de.blau.android.osm.BoundingBox;
import de.blau.android.resources.MBTileConstants;
import de.blau.android.util.ContentResolverUtil;

/**
 * @author Simon Poole
 */
public class MBTileProviderDataBase implements LocalTileContainer {
    private static final String  DEBUG_TAG = "MBTilePro...DataBase";
    private static final boolean DEBUGMODE = false;

    private static final int BUFFER_SIZE = 4096;

    static final String         T_MBTILES            = "tiles";
    private static final String T_MBTILES_ZOOM_LEVEL = "zoom_level";
    private static final String T_MBTILES_TILE_X     = "tile_column";
    private static final String T_MBTILES_TILE_Y     = "tile_row";
    static final String         T_MBTILES_DATA       = "tile_data";

    private static final String T_METADATA       = "metadata";
    private static final String T_METADATA_NAME  = "name";
    private static final String T_METADATA_VALUE = "value";

    private static final String SQL_ARG = "=?";
    private static final String AND     = " AND ";

    static final String T_MBTILES_WHERE = T_MBTILES_ZOOM_LEVEL + SQL_ARG + AND + T_MBTILES_TILE_X + SQL_ARG + AND + T_MBTILES_TILE_Y + SQL_ARG;

    private static final String T_MBTILES_GET = "SELECT " + T_MBTILES_DATA + " FROM " + T_MBTILES + " WHERE " + T_MBTILES_WHERE;

    private static final String T_MBTILES_GET_ZOOMS = "SELECT DISTINCT " + T_MBTILES_ZOOM_LEVEL + " FROM " + T_MBTILES + " ORDER BY " + T_MBTILES_ZOOM_LEVEL;

    // ===========================================================
    // Fields
    // ===========================================================

    private final SQLiteDatabase mDatabase;

    private final Pools.SynchronizedPool<SQLiteStatement> getStatements;

    private static class BufferWrapper {
        byte[] buffer = new byte[BUFFER_SIZE];
    }

    private final Pools.SynchronizedPool<BufferWrapper> buffers;

    private final Pools.SynchronizedPool<ByteArrayOutputStream> streams;

    private Map<String, String> metadata = null;

    // ===========================================================
    // Constructors
    // ===========================================================
    /**
     * Construct a new MapBox tile storage provider
     * 
     * @param context Android Context
     * @param mbTilesUri Uri for the mbtiles file
     * @param maxThreads if larger than 0 this will be used for determining how many prepared statements to create
     */
    public MBTileProviderDataBase(@NonNull final Context context, @NonNull Uri mbTilesUri, int maxThreads) {
        Log.i(DEBUG_TAG, "Creating database instance for " + mbTilesUri.toString());
        String path = ContentResolverUtil.getPath(context, mbTilesUri);
        mDatabase = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY);
        if (maxThreads <= 0) {
            maxThreads = App.getPreferences(context).getMaxTileDownloadThreads();
        }
        getStatements = new Pools.SynchronizedPool<>(maxThreads);
        buffers = new Pools.SynchronizedPool<>(maxThreads);
        streams = new Pools.SynchronizedPool<>(maxThreads);
        Log.i(DEBUG_TAG, "Allocating " + maxThreads + " prepared statements");
        for (int i = 0; i < maxThreads; i++) {
            buffers.release(new BufferWrapper());
            streams.release(new ByteArrayOutputStream());
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
        this(context, Uri.parse(mbTilesUri), -1);
    }

    @Override
    public byte[] getTile(@NonNull final MapTile aTile) throws IOException {
        try (InputStream is = getTileStream(aTile)) {
            if (is != null) {
                ByteArrayOutputStream bos = streams.acquire();
                BufferWrapper b = buffers.acquire();
                if (bos != null && b != null) {
                    try {
                        bos.reset();
                        byte[] buffer = b.buffer;
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            bos.write(buffer, 0, bytesRead);
                        }
                    } finally {
                        streams.release(bos);
                        buffers.release(b);
                    }
                    return bos.toByteArray();
                }
                throw new IOException("Pools exhausted");
            }
        }
        return null;
    }

    /**
     * Returns requested tile as an InputStream
     * 
     * This avoids copying when we are going to be processing a stream in any case
     * 
     * @param aTile the tile meta data
     * @return the contents of the tile as an InputStream or null on failure to retrieve
     * @throws IOException if we had issues reading from the database
     */
    @Nullable
    public InputStream getTileStream(@NonNull final MapTile aTile) throws IOException {
        if (DEBUGMODE) {
            Log.d(MapTileFilesystemProvider.DEBUG_TAG, "Trying to retrieve " + aTile + " from db");
        }
        SQLiteStatement get = getStatements.acquire();
        if (get == null) {
            throw new IOException("Used all statements");
        }
        try { // NOSONAR
            if (mDatabase.isOpen()) {
                bindTile(aTile, get);
                final ParcelFileDescriptor pfd = get.simpleQueryForBlobFileDescriptor();
                if (pfd != null) {
                    return new ParcelFileDescriptor.AutoCloseInputStream(pfd);
                }
                return null;
            }
        } catch (SQLiteDoneException sde) {
            // nothing found
            if (DEBUGMODE) {
                Log.d(MapTileFilesystemProvider.DEBUG_TAG, "Tile not found in DB " + sde.getMessage());
            }
        } catch (SQLiteException sex) { // handle these exceptions the same
            throw new IOException(sex.getMessage());
        } finally {
            getStatements.release(get);
        }
        return null;
    }

    /**
     * Bind the tile values to the prepared statement
     * 
     * @param aTile the tile descriptor
     * @param get the prepared statement
     */
    private void bindTile(@NonNull final MapTile aTile, @NonNull SQLiteStatement get) {
        int ymax = 1 << aTile.zoomLevel; // TMS scheme
        int y = ymax - aTile.y - 1;
        get.bindLong(1, aTile.zoomLevel);
        get.bindLong(2, aTile.x);
        get.bindLong(3, y);
    }

    /**
     * Get meta data from the file
     * 
     * The result is cached and only retrieved once
     * 
     * @return a Map containing all key-value pairs from the meta data or null if none
     */
    @Nullable
    public Map<String, String> getMetadata() {
        if (metadata == null && mDatabase.isOpen()) {
            try (Cursor dbresult = mDatabase.query(T_METADATA, null, null, null, null, null, null)) {
                if (dbresult.getCount() >= 1) {
                    metadata = new HashMap<>();
                    boolean haveEntry = dbresult.moveToFirst();
                    while (haveEntry) {
                        String name = dbresult.getString(dbresult.getColumnIndexOrThrow(T_METADATA_NAME));
                        String value = dbresult.getString(dbresult.getColumnIndexOrThrow(T_METADATA_VALUE));
                        metadata.put(name, value);
                        haveEntry = dbresult.moveToNext();
                    }
                }
            } catch (IllegalArgumentException e) {
                Log.e(DEBUG_TAG, "missing columns " + e.getMessage());
            }
        }
        return metadata;
    }

    /**
     * Get min and max zoom from the existing tiles
     * 
     * This tries first MBTiles 1.3 metadata snd then checks the tiles, likely rather expensive for a large number of
     * tiles
     * 
     * @return an int array holding the min zoom in the first, max zoom in the second element or null
     */
    @NonNull
    public int[] getMinMaxZoom() {
        int[] result = null;
        Map<String, String> meta = getMetadata();
        if (meta == null) {
            return new int[0];
        }
        try {
            result = new int[2];
            result[0] = Integer.parseInt(meta.get(MBTileConstants.MINZOOM));
            result[1] = Integer.parseInt(meta.get(MBTileConstants.MAXZOOM));
            return result;
        } catch (NumberFormatException e) {
            Log.e(DEBUG_TAG, "Unparseable zoom value " + e.getMessage());
        }
        if (mDatabase.isOpen()) {
            try (Cursor dbresult = mDatabase.rawQuery(T_MBTILES_GET_ZOOMS, null)) {
                if (dbresult.getCount() >= 1) {
                    boolean haveEntry = dbresult.moveToFirst();
                    if (haveEntry) {
                        result = new int[2];
                        result[0] = dbresult.getInt(dbresult.getColumnIndexOrThrow(T_MBTILES_ZOOM_LEVEL));
                        haveEntry = dbresult.moveToLast();
                        result[1] = dbresult.getInt(dbresult.getColumnIndexOrThrow(T_MBTILES_ZOOM_LEVEL));
                        return result;
                    }
                }
            } catch (IllegalArgumentException e) {
                Log.e(DEBUG_TAG, "missing columns " + e.getMessage());
            }
        }
        if (DEBUGMODE) {
            Log.d(DEBUG_TAG, "Min max zoom not found");
        }
        return new int[0];
    }

    /**
     * Get the MBTiles bounds meta information
     * 
     * @return a BoundingBox or null
     */
    @Nullable
    public BoundingBox getBounds() {
        Map<String, String> meta = getMetadata();
        if (meta != null) {
            String boxString = meta.get(MBTileConstants.BOUNDS);
            if (boxString != null) {
                return BoundingBox.fromDoubleString(boxString);
            }
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