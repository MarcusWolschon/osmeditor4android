// Created by plusminus on 21:46:41 - 25.09.2008
package de.blau.android.services.util;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.services.IMapTileProviderCallback;
import de.blau.android.services.exceptions.EmptyCacheException;
import de.blau.android.util.CustomDatabaseContext;

/**
 * 
 * <br/>
 * This class was taken from OpenStreetMapViewer (original package org.andnav.osm) in 2010-06 by Marcus Wolschon to be
 * integrated into the de.blau.androin OSMEditor.
 * 
 * @author Nicolas Gramlich
 * @author Marcus Wolschon <Marcus@Wolschon.biz>
 *
 */
public class MapTileFilesystemProvider extends MapAsyncTileProvider {
    // ===========================================================
    // Constants
    // ===========================================================

    final static String DEBUGTAG = "OSM_FS_PROVIDER";

    // ===========================================================
    // Fields
    // ===========================================================

    private final Context                 mCtx;
    private final MapTileProviderDataBase mDatabase;
    private final File                    mountPoint;
    private final int                     mMaxFSCacheByteSize;
    private int                           mCurrentFSCacheByteSize;

    /** online provider */
    private MapTileDownloader mTileDownloader;

    // ===========================================================
    // Constructors
    // ===========================================================

    /**
     * @param ctx
     * @param mountPoint TODO
     * @param aMaxFSCacheByteSize the size of the cached MapTiles will not exceed this size.
     */
    public MapTileFilesystemProvider(final Context ctx, File mountPoint, final int aMaxFSCacheByteSize) {
        mCtx = ctx;
        this.mountPoint = mountPoint;
        mMaxFSCacheByteSize = aMaxFSCacheByteSize;
        mDatabase = new MapTileProviderDataBase(new CustomDatabaseContext(ctx, mountPoint.getAbsolutePath()));
        mCurrentFSCacheByteSize = mDatabase.getCurrentFSCacheByteSize();
        mThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);

        mTileDownloader = new MapTileDownloader(ctx, this);
        Log.d(DEBUGTAG, "Currently used cache-size is: " + mCurrentFSCacheByteSize + " of " + mMaxFSCacheByteSize + " Bytes");
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    public int getCurrentFSCacheByteSize() {
        return mCurrentFSCacheByteSize;
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

    public void saveFile(final MapTile tile, final byte[] someData) throws IOException {
        try {
            final int bytesGrown = mDatabase.addTile(tile, someData);
            mCurrentFSCacheByteSize += bytesGrown;

            if (Log.isLoggable(DEBUGTAG, Log.DEBUG)) {
                Log.d(DEBUGTAG, "FSCache Size is now: " + mCurrentFSCacheByteSize + " Bytes");
            }
            /* If Cache is full... */
            try {

                if (mCurrentFSCacheByteSize > mMaxFSCacheByteSize) {
                    if (Log.isLoggable(DEBUGTAG, Log.DEBUG)) {
                        Log.d(DEBUGTAG, "Freeing FS cache...");
                    }
                    // Free 5% of cache
                    mCurrentFSCacheByteSize -= mDatabase.deleteOldest((int) (mMaxFSCacheByteSize * 0.05f));
                }
            } catch (EmptyCacheException e) {
                if (Log.isLoggable(DEBUGTAG, Log.DEBUG)) {
                    Log.d(DEBUGTAG, "Cache empty", e);
                }
            }
        } catch (IllegalStateException e) {
            if (Log.isLoggable(DEBUGTAG, Log.DEBUG)) {
                Log.d(DEBUGTAG, "Tile saving failed", e);
            }
        }
    }

    public void clearCurrentFSCache() {
        cutCurrentFSCacheBy(Integer.MAX_VALUE); // Delete all
    }

    private void cutCurrentFSCacheBy(final int bytesToCut) {
        try {
            synchronized (this) {
                mDatabase.deleteOldest(Integer.MAX_VALUE); // Delete all
            }
            mCurrentFSCacheByteSize = 0;
        } catch (EmptyCacheException e) {
            if (Log.isLoggable(DEBUGTAG, Log.DEBUG)) {
                Log.d(DEBUGTAG, "Cache empty", e);
            }
        }
    }

    /**
     * delete tiles for specific provider or all
     * 
     * @param rendererID the provider or null for all
     */
    public void flushCache(@Nullable String rendererID) {
        try {
            mDatabase.flushCache(rendererID);
        } catch (EmptyCacheException e) {
            if (Log.isLoggable(DEBUGTAG, Log.DEBUG)) {
                Log.d(DEBUGTAG, "Flushing tile cache failed", e);
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

        public TileLoader(final MapTile aTile, final IMapTileProviderCallback aCallback) {
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
                TileLayerServer renderer = TileLayerServer.get(mCtx, mTile.rendererID, false);
                if (renderer == null || mTile.zoomLevel < renderer.getMinZoomLevel() || !mTile.rendererID.equals(renderer.getId())) {
                    // the tile doesn't exist no point in trying to get it
                    mCallback.mapTileFailed(mTile.rendererID, mTile.zoomLevel, mTile.x, mTile.y, DOESNOTEXIST);
                    return;
                }
                byte[] data = MapTileFilesystemProvider.this.mDatabase.getTile(mTile);
                if (data == null) {
                    if (Log.isLoggable(DEBUGTAG, Log.DEBUG)) {
                        Log.d(DEBUGTAG, "FS failed, request for download " + mTile + " " + mTile.toId());
                    }
                    download = true;
                    mTileDownloader.loadMapTileAsync(mTile, passedOnCallback);
                } else { // success!
                    mCallback.mapTileLoaded(mTile.rendererID, mTile.zoomLevel, mTile.x, mTile.y, data);
                }
                if (Log.isLoggable(DEBUGTAG, Log.DEBUG)) {
                    Log.d(DEBUGTAG, "Loaded: " + mTile.toString());
                }
            } catch (IOException e) {
                if (Log.isLoggable(DEBUGTAG, Log.DEBUG)) {
                    Log.d(DEBUGTAG, "Invalid tile: " + mTile.toString());
                }
            } catch (RemoteException e) {
                if (Log.isLoggable(DEBUGTAG, Log.DEBUG)) {
                    Log.d(DEBUGTAG, "Service failed", e);
                }
            } catch (NullPointerException e) {
                if (Log.isLoggable(DEBUGTAG, Log.DEBUG)) {
                    Log.d(DEBUGTAG, "Service failed", e);
                }
            } catch (IllegalStateException e) {
                if (Log.isLoggable(DEBUGTAG, Log.DEBUG)) {
                    Log.d(DEBUGTAG, "Tile loading failed", e);
                }
            } finally {
                if (!download) {
                    finished();
                }
            }
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
        Log.d(DEBUGTAG, "Closing tile database");
        mDatabase.close();
    }

    public void markAsInvalid(MapTile mTile) throws IOException {
        mDatabase.addTile(mTile, null);
    }
}