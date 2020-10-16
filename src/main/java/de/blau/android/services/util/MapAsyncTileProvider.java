package de.blau.android.services.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

import androidx.annotation.NonNull;
import de.blau.android.services.IMapTileProviderCallback;

/**
 * This class was taken from OpenStreetMapViewer (original package org.andnav.osm) in 2010-06 by Marcus Wolschon to be
 * integrated into the de.blau.androin OSMEditor.
 * 
 * @author Nicolas Gramlich
 * @author Marcus Wolschon &lt;Marcus@Wolschon.biz&gt;
 * @author Simon Poole
 *
 */
public abstract class MapAsyncTileProvider {

    public static final int IOERR        = 1;
    public static final int DOESNOTEXIST = 2;
    public static final int NONETWORK    = 3;
    public static final int RETRY        = 4;

    public static final int ALLZOOMS = -1;

    ThreadPoolExecutor                  mThreadPool;
    private final Map<String, Runnable> mPending = Collections.synchronizedMap(new HashMap<String, Runnable>());

    /**
     * Queue a tile for loading, if it is already in the queue this returns without doing anything
     * 
     * @param aTile the tile descriptor
     * @param aCallback the call back for when the tile has been loaded
     */
    public synchronized void loadMapTileAsync(@NonNull final MapTile aTile, final IMapTileProviderCallback aCallback) {
        final String tileId = aTile.toId();

        if (mPending.containsKey(tileId)) {
            return;
        }

        Runnable r = getTileLoader(aTile, aCallback);
        mPending.put(tileId, r);
        mThreadPool.execute(r);
    }

    /**
     * Remove a specific request from the Executors queue
     * 
     * @param tileId id of the request
     * @return true if successful
     */
    private boolean removeRequest(@NonNull final String tileId) {
        Runnable r = mPending.get(tileId);
        if (mThreadPool.remove(r)) {
            mPending.remove(tileId);
            return true;
        }
        return false;
    }

    /**
     * Remove requests from the Executor queue for a specific renderer and zoom level
     * 
     * @param rendererId the renderer we want to remove tiles for
     * @param zoom the zoom level we want to remove tiles for, if ALLZOOMS remove all requests for the renderer
     */
    public void flushQueue(@NonNull String rendererId, int zoom) {
        Set<Entry<String, Runnable>> entries;
        synchronized (mPending) {
            entries = new HashSet<>(mPending.entrySet());
        }
        if (zoom != ALLZOOMS) {
            String id = Integer.toString(zoom) + rendererId; // see MapTile.toId()
            for (Entry<String, Runnable> e : entries) {
                if (e.getKey().startsWith(id)) {
                    removeRequest(e.getKey());
                }
            }
        } else {
            for (Entry<String, Runnable> e : entries) {
                if (e.getKey().contains(rendererId)) {
                    removeRequest(e.getKey());
                }
            }
        }
    }

    /**
     * Get the TileLoader for a tile
     * 
     * @param aTile the tile descriptor
     * @param aCallback callback to the TileProvider
     * @return a TileLoader
     */
    protected abstract Runnable getTileLoader(@NonNull final MapTile aTile, @NonNull final IMapTileProviderCallback aCallback);

    abstract class TileLoader implements Runnable {
        final MapTile                  mTile;
        final IMapTileProviderCallback mCallback;

        /**
         * Construct a new TileLoader
         * 
         * @param aTile the tile descriptor
         * @param aCallback callback to the TileProvider
         */
        protected TileLoader(@NonNull final MapTile aTile, @NonNull final IMapTileProviderCallback aCallback) {
            mTile = aTile;
            mCallback = aCallback;
        }

        /**
         * Finished loading, remove tile from pending
         */
        void finished() {
            mPending.remove(mTile.toId());
        }
    }
}
