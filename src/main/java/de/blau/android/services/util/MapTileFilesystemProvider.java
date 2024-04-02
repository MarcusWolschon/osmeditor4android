package de.blau.android.services.util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.exception.InvalidTileException;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.services.exceptions.EmptyCacheException;
import de.blau.android.util.CustomDatabaseContext;
import de.blau.android.util.Notifications;
import de.blau.android.util.ScreenMessage;
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
public class MapTileFilesystemProvider extends MapAsyncTileProvider implements MapTileSaver {

    static final String DEBUG_TAG = MapTileFilesystemProvider.class.getSimpleName().substring(0,
            Math.min(23, MapTileFilesystemProvider.class.getSimpleName().length()));

    private final Context                 mCtx;
    private final MapTileProviderDataBase tileCache;
    private final int                     mMaxFSCacheByteSize;
    private int                           mCurrentCacheByteSize;

    private final Map<String, LocalTileContainer> tileContainerCache = new HashMap<>();
    private final Set<String>                     errorDisplayed     = new HashSet<>(); // track error display

    private static final Random random = App.getRandom();

    /** online provider */
    private final MapTileDownloader mTileDownloader;

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
        tileCache = new MapTileProviderDataBase(new CustomDatabaseContext(ctx, mountPoint.getAbsolutePath()));

        int maxThreads = App.getPreferences(ctx).getMaxTileDownloadThreads();
        mThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(maxThreads);

        mTileDownloader = new MapTileDownloader(ctx, this);

        mThreadPool.execute(() -> {
            // mCurrentCacheByteSize will be zero till this is set which is harmless
            mCurrentCacheByteSize = tileCache.getCurrentFSCacheByteSize();
            Log.d(DEBUG_TAG, "Currently used cache-size is: " + mCurrentCacheByteSize + " of " + mMaxFSCacheByteSize + " Bytes");
        });
    }

    /**
     * Get the current size of the caches contents
     * 
     * @return size in bytes
     */
    public int getCurrentCacheByteSize() {
        return mCurrentCacheByteSize;
    }

    @Override
    public Runnable getTileLoader(@NonNull MapTile aTile, @NonNull MapTileProviderCallback aCallback) {
        return new TileLoader(aTile, aCallback);
    }

    @Override
    public void saveTile(final MapTile tile, final byte[] data) throws IOException {
        try {
            final int bytesGrown = tileCache.addTile(tile, data);
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
                mCurrentCacheByteSize -= tileCache.deleteOldest((int) (mMaxFSCacheByteSize * 0.05f));
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
            tileCache.deleteOldest(bytesToCut);
        }
        mCurrentCacheByteSize = tileCache.getCurrentFSCacheByteSize();
    }

    /**
     * delete tiles for specific provider or all
     * 
     * @param sourceId the provider or null for all
     */
    public void flushCache(@Nullable String sourceId) {
        try {
            tileCache.flushCache(sourceId);
            mCurrentCacheByteSize = tileCache.getCurrentFSCacheByteSize();
            mTileDownloader.flushDisabled(sourceId);
        } catch (EmptyCacheException e) {
            if (Log.isLoggable(DEBUG_TAG, Log.DEBUG)) {
                Log.d(DEBUG_TAG, "Flushing tile cache failed", e);
            }
        }
    }

    @Override
    public void flushQueue(@NonNull String rendererId, int zoom) {
        // don't bother flushing our queue
        // just do it for downloads
        mTileDownloader.flushQueue(rendererId, zoom);
    }

    /**
     * This will load a single tile from a local source or from the local tile cache, if necessary it will queue the
     * tile for download in the later case.
     * 
     * @author simon
     *
     */
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
            TileLayerSource source = TileLayerSource.get(mCtx, mTile.rendererID, false);
            if (source == null) {
                Log.e(DEBUG_TAG, "No source for " + mTile.rendererID);
                finished();
                return;
            }
            final String sourceId = source.getId();
            boolean download = false;
            final boolean isLocalFile = source.isLocalFile();
            try {
                if (!source.isMetadataLoaded()) {
                    failed(mTile, RETRY);
                    return;
                }
                if (mTile.zoomLevel < source.getMinZoomLevel()) {
                    failed(mTile, DOESNOTEXIST);
                    return;
                }
                byte[] data = null;
                if (isLocalFile) {
                    data = getTileContainer(source.getType(), sourceId, source.getOriginalTileUrl()).getTile(mTile);
                    if (data == null) {
                        failed(mTile, DOESNOTEXIST);
                        return;
                    }
                } else {
                    // retrieve from on device cache or download
                    data = tileCache.getTile(mTile);
                    if (data == null) {
                        download = true;
                        mTileDownloader.loadMapTileAsync(mTile, passedOnCallback);
                        return;
                    }
                }
                mCallback.mapTileLoaded(mTile.rendererID, mTile.zoomLevel, mTile.x, mTile.y, data);
            } catch (InvalidTileException itex) {
                failed(mTile, DOESNOTEXIST);
            } catch (IOException | NullPointerException | IllegalStateException e) {
                displayError(mCtx, errorDisplayed, isLocalFile ? sourceId : DEBUG_TAG, R.string.toast_tile_database_issue, e);
            } finally {
                /*
                 * If we don't have the tile in the database and need to download we have to keep the tile in our queue
                 * until the download attempt is finished
                 */
                if (!download) {
                    finished();
                }
            }
        }

        /**
         * Get a local tile container, if it isn't in the cache cache it
         * 
         * @param type the type currently everything except PMTiles are assumed to be MBT sqlite databases
         * @param sourceId the id of the tile source
         * @param uri an URI with the location of the container
         * @return a LocalTileContainer instance
         * @throws IOException if the container couldn't be opened
         */
        @NonNull
        private LocalTileContainer getTileContainer(@NonNull String type, @NonNull String sourceId, @NonNull String uri) throws IOException {
            LocalTileContainer tileContainer = tileContainerCache.get(sourceId);
            if (tileContainer == null) {
                synchronized (tileContainerCache) {
                    tileContainer = tileContainerCache.get(sourceId);
                    if (tileContainer != null) { // another thread may have created it
                        return tileContainer;
                    }
                    if (!tileContainerCache.containsKey(sourceId)) {
                        try {
                            tileContainer = TileLayerSource.TYPE_PMT_3.equals(type) ? new PMTilesContainer(new File(Uri.parse(uri).getPath()))
                                    : new MBTileProviderDataBase(mCtx, uri);
                            tileContainerCache.put(sourceId, tileContainer);
                            return tileContainer;
                        } catch (IOException | SQLiteException ex) {
                            Log.e(DEBUG_TAG, "Unable to open tile container " + uri + " " + ex.getMessage());
                            tileContainerCache.put(sourceId, null);
                        }
                    }
                    throw new IOException(mCtx.getString(R.string.toast_tile_container_issue, uri));
                }
            }
            return tileContainer;
        }

        /**
         * Tell the caller that a tile failed
         * 
         * @param tile the tile
         * @param code a code indicating the issue
         */
        private void failed(@NonNull MapTile tile, int code) {
            try {
                mCallback.mapTileFailed(tile.rendererID, tile.zoomLevel, tile.x, tile.y, code, null);
            } catch (IOException e) {
                Log.e(DEBUG_TAG, "mapTileFailed failed with " + e.getMessage());
            }
        }

        MapTileProviderCallback passedOnCallback = new MapTileProviderCallback() {

            @Override
            public void mapTileLoaded(@NonNull String rendererID, int zoomLevel, int tileX, int tileY, @NonNull byte[] aImage) throws IOException {
                mCallback.mapTileLoaded(rendererID, zoomLevel, tileX, tileY, aImage);
                finished();
            }

            @Override
            public void mapTileFailed(@NonNull String rendererID, int zoomLevel, int tileX, int tileY, int reason, String message) throws IOException {
                mCallback.mapTileFailed(rendererID, zoomLevel, tileX, tileY, reason, null);
                finished();
            }
        };
    }

    /**
     * Display an error notification once per "source"
     * 
     * @param ctx and Android Context
     * @param displayed a set to track if we have already messaged
     * @param sourceId the source id
     * @param msg a string resource with the message
     * @param e the Exception we caught
     */
    static void displayError(Context ctx, Set<String> displayed, final String sourceId, int msg, Exception e) {
        if (!displayed.contains(sourceId)) {
            // something is seriously wrong with the database or source file, show a toast once
            final String localizedMessage = e.getLocalizedMessage();
            final String message = ctx.getString(msg, localizedMessage);
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> ScreenMessage.toastTopError(ctx, message, false));
            Notifications.error(ctx, R.string.toast_tile_database_issue_short, message, random.nextInt());
            displayed.add(sourceId);
        }
    }

    /**
     * Call when the object is no longer needed to close the database
     */
    public void destroy() {
        Log.d(DEBUG_TAG, "Closing tile databases");
        tileCache.close();
        synchronized (tileContainerCache) {
            for (LocalTileContainer container : tileContainerCache.values()) {
                if (container != null) {
                    container.close();
                }
            }
            tileContainerCache.clear();
        }
    }

    @Override
    public void markAsInvalid(@NonNull MapTile mTile) throws IOException {
        tileCache.addTile(mTile, null);
    }

    /**
     * Get an instance of the MapTileProvider that uses a sqlite DB for caching
     * 
     * @param ctx an Android Context
     * @return the provider or null if the DB cannot be created
     */
    @Nullable
    public static MapTileFilesystemProvider getInstance(@NonNull Context ctx) {
        Preferences prefs = App.getPreferences(ctx);
        int tileCacheSize = prefs.getTileCacheSize();
        boolean preferRemovableStorage = prefs.preferRemovableStorage();

        File mountPoint = null;

        for (File dir : ContextCompat.getExternalFilesDirs(ctx, null)) { // iterate over the directories
                                                                         // preferring a removable one if
                                                                         // required
            if (dir == null || !dir.canWrite()) {
                Log.d(DEBUG_TAG, "storage dir null or not writable ");
                continue;
            }

            Log.d(DEBUG_TAG, "candidate storage directory " + dir.getPath());
            mountPoint = dir;
            // existing tile cache
            if (MapTileProviderDataBase.exists(dir)) {
                break;
            }
            if (!preferRemovableStorage) {
                break; // we are done
            }
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
        }
        if (mountPoint == null) {
            ScreenMessage.toastTopError(ctx, R.string.toast_no_suitable_storage);
            return null;
        }

        Log.d(DEBUG_TAG, "Setting cache size to " + tileCacheSize + " on " + mountPoint.getPath());
        try {
            return new MapTileFilesystemProvider(ctx, mountPoint, tileCacheSize * 1024 * 1024); // FSCache
        } catch (SQLiteException slex) {
            Log.d(DEBUG_TAG, "Opening DB hit " + slex);
            ScreenMessage.toastTopError(ctx, ctx.getString(R.string.toast_tile_database_issue, slex.getMessage()));
        }
        return null;
    }
}