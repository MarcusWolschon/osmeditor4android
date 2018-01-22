package de.blau.android.services.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

import de.blau.android.services.IMapTileProviderCallback;

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
public abstract class MapAsyncTileProvider {

    public static final int IOERR        = 1;
    public static final int DOESNOTEXIST = 2;
    public static final int NONETWORK    = 3;
    public static final int RETRY        = 4;

    public static final int ALLZOOMS = -1;

    ThreadPoolExecutor                  mThreadPool;
    private final Map<String, Runnable> mPending = Collections.synchronizedMap(new HashMap<String, Runnable>());

    public synchronized void loadMapTileAsync(final MapTile aTile, final IMapTileProviderCallback aCallback) {
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
    private boolean removeRequest(final String tileId) {
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
    public void flushQueue(String rendererId, int zoom) {
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

    protected abstract Runnable getTileLoader(final MapTile aTile, final IMapTileProviderCallback aCallback);

    abstract class TileLoader implements Runnable {
        final MapTile                  mTile;
        final IMapTileProviderCallback mCallback;

        public TileLoader(final MapTile aTile, final IMapTileProviderCallback aCallback) {
            mTile = aTile;
            mCallback = aCallback;
        }

        void finished() {
            mPending.remove(mTile.toId());
        }
    }
}
