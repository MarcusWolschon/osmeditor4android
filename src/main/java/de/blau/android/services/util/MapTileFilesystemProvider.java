package de.blau.android.services.util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import de.blau.android.R;
import de.blau.android.exception.InvalidTileException;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.services.exceptions.EmptyCacheException;
import de.blau.android.util.CustomDatabaseContext;
import de.blau.android.util.Notifications;
import de.blau.android.util.Snack;
import de.blau.android.views.util.MapTileProviderCallback;

/**
 * 
 * This class was taken from OpenStreetMapViewer (original package org.andnav.osm) in 2010-06 by Marcus Wolschon to be
 * integrated into the de.blau.androin OSMEditor.
 * 
 * @author Created by plusminus on 21:46:41 - 25.09.2008
 * @author Nicolas Gramlich
 * @author Marcus Wolschon &lt;Marcus@Wolschon.biz&gt;
 * @author Simon Poole
 *
 */
public class MapTileFilesystemProvider extends MapAsyncTileProvider {
    // ===========================================================
    // Constants
    // ===========================================================

    static final String DEBUG_TAG = "MapTileFile...Provider";

    // ===========================================================
    // Fields
    // ===========================================================

    private final Context                 mCtx;
    private final MapTileProviderDataBase mDatabase;
    private final int                     mMaxFSCacheByteSize;
    private int                           mCurrentCacheByteSize;
    private boolean                       errorDisplayed = false;

    private final Map<String, MBTileProviderDataBase> mbTileDatabases = new HashMap<>();
    private final Random                              random          = new Random();

    /** online provider */
    private MapTileDownloader mTileDownloader;

    // ===========================================================
    // Constructors
    // ===========================================================

    /**
     * Construct a new tile cache on a local device
     * 
     * @param ctx Android Context
     * @param mountPoint where the cache should be creates
     * @param aMaxFSCacheByteSize the size of the cached MapTiles will not exceed this size.
     */
    public MapTileFilesystemProvider(@NonNull final Context ctx, @NonNull File mountPoint, final int aMaxFSCacheByteSize) {
        mCtx = ctx;
        mMaxFSCacheByteSize = aMaxFSCacheByteSize;
        mDatabase = new MapTileProviderDataBase(new CustomDatabaseContext(ctx, mountPoint.getAbsolutePath()));
        mCurrentCacheByteSize = mDatabase.getCurrentFSCacheByteSize();
        Preferences prefs = new Preferences(ctx);
        int maxThreads = prefs.getMaxTileDownloadThreads();
        mThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(maxThreads);

        mTileDownloader = new MapTileDownloader(ctx, this);
        Log.d(DEBUG_TAG, "Currently used cache-size is: " + mCurrentCacheByteSize + " of " + mMaxFSCacheByteSize + " Bytes");
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    /**
     * Get the current size of the caches contents
     * 
     * @return size in bytes
     */
    public int getCurrentCacheByteSize() {
        return mCurrentCacheByteSize;
    }

    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================

    @Override
    public Runnable getTileLoader(MapTile aTile, MapTileProviderCallback aCallback) {
        return new TileLoader(aTile, aCallback);
    }

    // ===========================================================
    // Methods
    // ===========================================================

    /**
     * Save the image data for a tile to the database, making space if necessary
     * 
     * @param tile tile meta-data
     * @param data the tile image data
     * @throws IOException if saving the file goes wrong
     */
    public void saveFile(final MapTile tile, final byte[] data) throws IOException {
        try {
            final int bytesGrown = mDatabase.addTile(tile, data);
            mCurrentCacheByteSize += bytesGrown;

            if (Log.isLoggable(DEBUG_TAG, Log.DEBUG)) {
                Log.d(DEBUG_TAG, "FSCache Size is now: " + mCurrentCacheByteSize + " Bytes");
            }
            /* If Cache is full... */
            if (mCurrentCacheByteSize > mMaxFSCacheByteSize) {
                if (Log.isLoggable(DEBUG_TAG, Log.DEBUG)) {
                    Log.d(DEBUG_TAG, "Freeing FS cache...");
                }
                // Free 5% of cache
                mCurrentCacheByteSize -= mDatabase.deleteOldest((int) (mMaxFSCacheByteSize * 0.05f));
            }
            if (Log.isLoggable(DEBUG_TAG, Log.DEBUG)) {
                Log.d(DEBUG_TAG, "Tile saved");
            }
        } catch (IllegalStateException e) {
            if (Log.isLoggable(DEBUG_TAG, Log.DEBUG)) {
                Log.d(DEBUG_TAG, "Tile saving failed", e);
            }
        }
    }

    /**
     * Remove all tiles from cache
     */
    public void clearCurrentCache() {
        cutCurrentCacheBy(Integer.MAX_VALUE); // Delete all
    }

    /**
     * Reduce size of cached tiles by a specified amount
     * 
     * @param bytesToCut how much we want to make free
     */
    private void cutCurrentCacheBy(final int bytesToCut) {
        synchronized (this) {
            mDatabase.deleteOldest(bytesToCut);
        }
        mCurrentCacheByteSize = mDatabase.getCurrentFSCacheByteSize();
    }

    /**
     * delete tiles for specific provider or all
     * 
     * @param rendererID the provider or null for all
     */
    public void flushCache(@Nullable String rendererID) {
        try {
            mDatabase.flushCache(rendererID);
            mCurrentCacheByteSize = mDatabase.getCurrentFSCacheByteSize();
        } catch (EmptyCacheException e) {
            if (Log.isLoggable(DEBUG_TAG, Log.DEBUG)) {
                Log.d(DEBUG_TAG, "Flushing tile cache failed", e);
            }
        }
    }

    @Override
    public void flushQueue(String rendererId, int zoom) {
        // don't bother flushing our queue
        // just do it for downloads
        mTileDownloader.flushQueue(rendererId, zoom);
    }

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================

    private class TileLoader extends MapAsyncTileProvider.TileLoader {

        /**
         * Construct a new TileLoader for a Tile
         * 
         * @param aTile the tile descriptor
         * @param aCallback the callback to the provider
         */
        public TileLoader(@NonNull final MapTile aTile, @NonNull final MapTileProviderCallback aCallback) {
            super(aTile, aCallback);
        }

        @Override
        public void run() {
            /**
             * If we don't have the tile in the database and need to download we have to keep the tile in our queue
             * until the download attempt is finished
             */
            boolean download = false;
            try {
                TileLayerSource renderer = TileLayerSource.get(mCtx, mTile.rendererID, false);
                if (renderer == null || !renderer.isMetadataLoaded()) {
                    failed(mTile, RETRY);
                    return;
                }
                if (mTile.zoomLevel < renderer.getMinZoomLevel() || !mTile.rendererID.equals(renderer.getId())) {
                    failed(mTile, DOESNOTEXIST);
                    return;
                }
                if (renderer.isReadOnly()) {
                    MBTileProviderDataBase mbTileDatabase = mbTileDatabases.get(renderer.getId());
                    if (mbTileDatabase == null) {
                        if (!mbTileDatabases.containsKey(renderer.getId())) {
                            synchronized (mbTileDatabases) {
                                mbTileDatabase = mbTileDatabases.get(renderer.getId());
                                if (mbTileDatabase == null) { // re-test
                                    try {
                                        mbTileDatabase = new MBTileProviderDataBase(mCtx, renderer.getOriginalTileUrl());
                                        mbTileDatabases.put(renderer.getId(), mbTileDatabase);
                                    } catch (SQLiteException sqlex) {
                                        Log.e(DEBUG_TAG, "Unable to open db " + renderer.getOriginalTileUrl());
                                        mbTileDatabases.put(renderer.getId(), null);
                                    }
                                }
                            }
                        } else {
                            failed(mTile, DOESNOTEXIST);
                        }
                    }
                    byte[] data = mbTileDatabase.getTile(mTile);
                    if (data == null) {
                        if (Log.isLoggable(DEBUG_TAG, Log.DEBUG)) {
                            Log.d(DEBUG_TAG, "FS failed " + mTile + " " + mTile.toId());
                        }
                        failed(mTile, DOESNOTEXIST);
                    } else { // success!
                        mCallback.mapTileLoaded(mTile.rendererID, mTile.zoomLevel, mTile.x, mTile.y, data);
                    }
                } else {
                    try {
                        byte[] data = mDatabase.getTile(mTile);
                        if (data == null) {
                            if (Log.isLoggable(DEBUG_TAG, Log.DEBUG)) {
                                Log.d(DEBUG_TAG, "FS failed, request for download " + mTile + " " + mTile.toId());
                            }
                            download = true;
                            mTileDownloader.loadMapTileAsync(mTile, passedOnCallback);
                        } else { // success!
                            mCallback.mapTileLoaded(mTile.rendererID, mTile.zoomLevel, mTile.x, mTile.y, data);
                        }
                    } catch (InvalidTileException itex) {
                        failed(mTile, DOESNOTEXIST);
                    }
                }
                if (Log.isLoggable(DEBUG_TAG, Log.DEBUG)) {
                    Log.d(DEBUG_TAG, "Loaded: " + mTile.toString());
                }
            } catch (IOException | NullPointerException | IllegalStateException e) {
                if (Log.isLoggable(DEBUG_TAG, Log.DEBUG)) {
                    Log.d(DEBUG_TAG, "Tile loading failed", e);
                }
                if (!download && !errorDisplayed) {
                    // something is seriously wrong with the database, show a toast once
                    final String localizedMessage = e.getLocalizedMessage();
                    final String message = mCtx.getString(R.string.toast_tile_database_issue, localizedMessage);
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(() -> Snack.toastTopError(mCtx, message));
                    Notifications.error(mCtx, R.string.toast_tile_database_issue_short,
                            Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ? localizedMessage : message, random.nextInt());
                    errorDisplayed = true;
                }
            } finally {
                if (!download) {
                    finished();
                }
            }
        }

        /**
         * Tell the caller that a tile failed
         * 
         * @param tile the tile
         * @param code a code indicating the issue
         */
        private void failed(@NonNull MapTile tile, int code) {
            try {
                mCallback.mapTileFailed(tile.rendererID, tile.zoomLevel, tile.x, tile.y, code);
            } catch (IOException e) {
                Log.e(DEBUG_TAG, "mapTileFailed failed with " + e.getMessage());
            }
        }

        MapTileProviderCallback passedOnCallback = new MapTileProviderCallback() {

            @Override
            public void mapTileLoaded(String rendererID, int zoomLevel, int tileX, int tileY, byte[] aImage) throws IOException {
                mCallback.mapTileLoaded(rendererID, zoomLevel, tileX, tileY, aImage);
                finished();
            }

            @Override
            public void mapTileFailed(String rendererID, int zoomLevel, int tileX, int tileY, int reason) throws IOException {
                mCallback.mapTileFailed(rendererID, zoomLevel, tileX, tileY, reason);
                finished();
            }
        };
    }

    /**
     * Call when the object is no longer needed to close the database
     */
    public void destroy() {
        Log.d(DEBUG_TAG, "Closing tile databases");
        mDatabase.close();
        synchronized (mbTileDatabases) {
            for (MBTileProviderDataBase mb : mbTileDatabases.values()) {
                mb.close();
            }
            mbTileDatabases.clear();
        }
    }

    /**
     * Mark a tile as invalid (really doesn't exist)
     * 
     * @param mTile tile meta-data
     * @throws IOException if writing to the database fails
     */
    public void markAsInvalid(@NonNull MapTile mTile) throws IOException {
        mDatabase.addTile(mTile, null);
    }

    /**
     * Get an instance of the MapTileProvider that uses a sqlite DB for caching
     * 
     * @param ctx an Android Context
     * @return the provider or null if the DB cannot be created
     */
    @Nullable
    public static MapTileFilesystemProvider getInstance(@NonNull Context ctx) {
        Preferences prefs = new Preferences(ctx);
        int tileCacheSize = prefs.getTileCacheSize();
        boolean preferRemovableStorage = prefs.preferRemovableStorage();

        File mountPoint = null;

        for (File dir : ContextCompat.getExternalFilesDirs(ctx, null)) { // iterate over the directories
                                                                         // preferring a removable one if
                                                                         // required
            if (dir == null) {
                Log.d(DEBUG_TAG, "storage dir null");
                continue;
            }
            Log.d(DEBUG_TAG, "candidate storage directory " + dir.getPath());
            if (MapTileProviderDataBase.exists(dir)) { // existing tile cache, only use if we can write
                if (dir.canWrite()) {
                    mountPoint = dir;
                    break;
                }
            } else if (dir.canWrite()) {
                mountPoint = dir;
                if (preferRemovableStorage && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    try {
                        if (Environment.isExternalStorageRemovable(dir)) {
                            // prefer removable storage
                            Log.d(DEBUG_TAG, "isExternalStorageRemovable claims dir is removable");
                            break;
                        }
                    } catch (IllegalArgumentException iae) {
                        // we've seen this on some devices even if it doesn't make sense
                        Log.d(DEBUG_TAG, "isExternalStorageRemovable didn't like " + dir);
                    }
                } else {
                    break; // just use the first writable directory
                }
            } else {
                Log.d(DEBUG_TAG, dir.getPath() + " not writable");
            }
        }

        MapTileFilesystemProvider result = null;
        if (mountPoint != null) {
            Log.d(DEBUG_TAG, "Setting cache size to " + tileCacheSize + " on " + mountPoint.getPath());
            try {
                result = new MapTileFilesystemProvider(ctx, mountPoint, tileCacheSize * 1024 * 1024); // FSCache
                // try to get BING layer early so the meta-data is already loaded
                TileLayerSource.get(ctx, TileLayerSource.LAYER_BING, false);
            } catch (SQLiteException slex) {
                Log.d(DEBUG_TAG, "Opening DB hit " + slex);
            }
        } else {
            Snack.toastTopError(ctx, R.string.toast_no_suitable_storage);
        }
        return result;
    }
}