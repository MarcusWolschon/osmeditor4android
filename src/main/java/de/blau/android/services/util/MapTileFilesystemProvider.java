package de.blau.android.services.util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import android.app.NotificationManager;
import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import de.blau.android.R;
import de.blau.android.exception.InvalidTileException;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.services.IMapTileProviderCallback;
import de.blau.android.services.exceptions.EmptyCacheException;
import de.blau.android.util.CustomDatabaseContext;
import de.blau.android.util.Notifications;
import de.blau.android.util.Snack;

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
    protected Runnable getTileLoader(MapTile aTile, IMapTileProviderCallback aCallback) {
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
        public TileLoader(@NonNull final MapTile aTile, @NonNull final IMapTileProviderCallback aCallback) {
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
                        byte[] data = MapTileFilesystemProvider.this.mDatabase.getTile(mTile);
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
            } catch (IOException | RemoteException | NullPointerException | IllegalStateException e) {
                if (Log.isLoggable(DEBUG_TAG, Log.DEBUG)) {
                    Log.d(DEBUG_TAG, "Tile loading failed", e);
                }
                if (!download && !errorDisplayed) {
                    // something is seriously wrong with the database, show a toast once
                    final String message = mCtx.getString(R.string.toast_tile_database_issue, e.getLocalizedMessage());
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(() -> Snack.toastTopError(mCtx, message));
                    NotificationCompat.Builder builder = Notifications.builder(mCtx).setSmallIcon(R.drawable.logo_simplified)
                            .setContentTitle(mCtx.getString(R.string.toast_tile_database_issue_short));
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        builder.setContentText(e.getLocalizedMessage());
                    } else {
                        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(message)).setPriority(NotificationCompat.PRIORITY_MAX);
                    }

                    NotificationManager nManager = (NotificationManager) mCtx.getSystemService(Context.NOTIFICATION_SERVICE);
                    nManager.notify(random.nextInt(), builder.build());
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
         * @throws RemoteException Binder remote-invocation error
         */
        private void failed(@NonNull MapTile tile, int code) throws RemoteException {
            mCallback.mapTileFailed(tile.rendererID, tile.zoomLevel, tile.x, tile.y, code);
        }

        IMapTileProviderCallback passedOnCallback = new IMapTileProviderCallback() {

            @Override
            public IBinder asBinder() {
                return mCallback.asBinder();
            }

            @Override
            public void mapTileLoaded(String rendererID, int zoomLevel, int tileX, int tileY, byte[] aImage) throws RemoteException {
                mCallback.mapTileLoaded(rendererID, zoomLevel, tileX, tileY, aImage);
                finished();
            }

            @Override
            public void mapTileFailed(String rendererID, int zoomLevel, int tileX, int tileY, int reason) throws RemoteException {
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
}