package de.blau.android.views.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.exception.StorageException;
import de.blau.android.services.IMapTileProviderCallback;
import de.blau.android.services.IMapTileProviderService;
import de.blau.android.services.util.MapAsyncTileProvider;
import de.blau.android.services.util.MapTile;
import de.blau.android.util.Util;

/**
 * This class was taken from OpenStreetMapViewer (original package org.andnav.osm) in 2010 by Marcus Wolschon to be
 * integrated into the de.blau.androin OSMEditor.
 * 
 * Created by plusminus on 21:46:22 - 25.09.2008
 * 
 * @author Nicolas Gramlich
 * @author Marcus Wolschon &lt;Marcus@Wolschon.biz&gt;
 * @author Simon Poole
 * 
 */
public class MapTileProvider<T> implements ServiceConnection {
    // ===========================================================
    // Constants
    // ===========================================================

    /**
     * Tag used in debug log-entries.
     */
    private static final String DEBUG_TAG = MapTileProvider.class.getSimpleName();

    // ===========================================================
    // Fields
    // ===========================================================

    private Context                 mCtx;
    /**
     * cache provider
     */
    private MapTileCache<T>         mTileCache;
    private final Map<String, Long> pending = Collections.synchronizedMap(new HashMap<String, Long>());

    private IMapTileProviderService mTileService;
    private final Handler           mDownloadFinishedHandler;
    private final TileDecoder<T>    decoder;

    /**
     * Set to true if we have less than 64 MB heap or have other caching issues
     */
    private boolean smallHeap = false;

    public interface TileDecoder<D> {
        /**
         * Decode a tile
         * 
         * @param data the original tile data
         * @param small use a little memory as possible
         * @return the tile in the target format
         */
        D decode(@NonNull byte[] data, boolean small);
    }

    public static class BitmapDecoder implements TileDecoder<Bitmap> {

        @Override
        public Bitmap decode(byte[] data, boolean small) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            if (small) {
                options.inPreferredConfig = Bitmap.Config.RGB_565;
            } else {
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            }
            return BitmapFactory.decodeByteArray(data, 0, data.length, options);
        }

    }
    // ===========================================================
    // Constructors
    // ===========================================================

    /**
     * Create a new MapTileProvider instance
     * 
     * @param ctx Android Context
     * @param aDownloadFinishedListener handler to call when a tile download is complete
     */
    public MapTileProvider(@NonNull final Context ctx, @NonNull TileDecoder<T> decoder, @NonNull final Handler aDownloadFinishedListener) {
        mCtx = ctx;
        mTileCache = new MapTileCache<>();

        smallHeap = Util.smallHeap();
        this.decoder = decoder;
        mDownloadFinishedHandler = aDownloadFinishedListener;

        Intent explicitIntent = (new Intent(IMapTileProviderService.class.getName())).setPackage(ctx.getPackageName());
        if (!ctx.bindService(explicitIntent, this, Context.BIND_AUTO_CREATE)) {
            Log.e(DEBUG_TAG, "Could not bind to " + IMapTileProviderService.class.getName() + " in package " + ctx.getPackageName());
        }
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================

    @Override
    public void onServiceConnected(android.content.ComponentName name, android.os.IBinder service) {
        mTileService = IMapTileProviderService.Stub.asInterface(service);
        mDownloadFinishedHandler.sendEmptyMessage(MapTile.MAPTILE_SUCCESS_ID);
        Log.d(DEBUG_TAG, "connected");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mTileService = null;
        Log.d(DEBUG_TAG, "disconnected");
    }

    // ===========================================================
    // Methods
    // ===========================================================

    /**
     * Check if we are connected to the service
     * 
     * @return true if connected
     */
    public boolean connected() {
        return mTileService != null;
    }

    /**
     * Clear out memory related to tracking map tiles.
     */
    public void clear() {
        pending.clear();
        mTileCache.clear();
        mCtx.unbindService(this);
    }

    /**
     * Try to reduce memory use.
     */
    public void onLowMemory() {
        mTileCache.onLowMemory();
    }

    /**
     * Attempt to return a tile from cache otherwise ask for it from remote
     * 
     * @param aTile tile spec
     * @param owner id for the current owner
     * @return the tile or null if it wasn't in cache
     */
    @Nullable
    public T getMapTile(@NonNull final MapTile aTile, long owner) {
        T tile = mTileCache.getMapTile(aTile);
        if (tile != null) {
            return tile;
        } else {
            // from service
            if (MapViewConstants.DEBUGMODE) {
                Log.i(DEBUG_TAG, "Memory MapTileCache failed for: " + aTile.toString());
            }
            preCacheTile(aTile, owner);
        }
        return null;
    }

    /**
     * Attempt to return a tile from in memory cache
     * 
     * @param aTile tile spec
     * @return the tile or null if it wasn't in cache
     */
    @Nullable
    public T getMapTileFromCache(@NonNull final MapTile aTile) {
        return mTileCache.getMapTile(aTile);
    }

    /**
     * Request a tile from the tile service
     * 
     * @param aTile the tile parameters
     * @param owner if for the current owner
     */
    private void preCacheTile(@NonNull final MapTile aTile, long owner) {
        if (mTileService != null && !pending.containsKey(aTile.toId())) {
            try {
                pending.put(aTile.toId(), owner);
                mTileService.getMapTile(aTile.rendererID, aTile.zoomLevel, aTile.x, aTile.y, mServiceCallback);
            } catch (RemoteException e) {
                Log.e(DEBUG_TAG, "RemoteException in preCacheTile()", e);
            } catch (Exception e) {
                Log.e(DEBUG_TAG, "Exception in preCacheTile()", e);
            }
        }
    }

    /**
     * Remove requests for a specific renderer and zoom level from the queues
     * 
     * @param rendererId the renderer we want to delete the requests for
     * @param zoomLevel the zoom level we want to delete the requests for, if MapAsyncTileProvider.ALLZOOMS remove for
     *            all zooms
     */
    public void flushQueue(String rendererId, int zoomLevel) {
        if (mTileService != null) {
            try {
                mTileService.flushQueue(rendererId, zoomLevel);
                // remove the same from pending

                Set<String> keys;
                synchronized (pending) {
                    keys = new HashSet<>(pending.keySet());
                }
                if (zoomLevel != MapAsyncTileProvider.ALLZOOMS) {
                    String id = Integer.toString(zoomLevel) + rendererId;
                    for (String key : keys) {
                        if (key.startsWith(id)) {
                            pending.remove(key);
                        }
                    }
                } else {
                    for (String key : keys) {
                        if (key.contains(rendererId)) {
                            pending.remove(key);
                        }
                    }
                }
            } catch (RemoteException e) {
                Log.e(DEBUG_TAG, "RemoteException in flushQueue()", e);
            } catch (Exception e) {
                Log.e(DEBUG_TAG, "Exception in flushQueue()", e);
            }
        }
    }

    /**
     * Flush the tile cache for a specific provider
     * 
     * @param rendererId the provider to flush or if null all
     */
    public void flushCache(@Nullable String rendererId) {
        if (mTileService == null) {
            Log.e(DEBUG_TAG, "tile service is disconnected");
            return;
        }
        try {
            mTileService.flushCache(rendererId);
        } catch (RemoteException e) {
            Log.e(DEBUG_TAG, "RemoteException in flushCache()", e);
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Exception in flushCache()", e);
        }
        mTileCache.clear(); // zap everything in in memory cache
    }

    /**
     * Tell the tile provider service to reread the database of TileLayerServers
     */
    public void update() {
        if (mTileService == null) {
            Log.e(DEBUG_TAG, "tile service is disconnected");
            return;
        }
        try {
            mTileService.update();
        } catch (RemoteException e) {
            Log.e(DEBUG_TAG, "RemoteException in update()", e);
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Exception in in update()", e);
        }
    }

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================

    /**
     * Callback for the {@link IOpenStreetMapTileProviderService} we are using.
     */
    private IMapTileProviderCallback mServiceCallback = new IMapTileProviderCallback.Stub() {

        /**
         * Called after a tile has been loaded, copies the tile to the in memory cache
         * 
         * @param rendererID the tile renderer id
         * @param zoomLevel the zoom level
         * @param tileX tile x
         * @param tileY tile y
         * @param data tile image data
         * @throws RemoteException if something goes wrong receiving the tile from the service
         */
        public void mapTileLoaded(@NonNull final String rendererID, final int zoomLevel, final int tileX, final int tileY, @NonNull final byte[] data)
                throws RemoteException {

            MapTile t = new MapTile(rendererID, zoomLevel, tileX, tileY);
            String id = t.toId();
            try {
                T tileBitmap = decoder.decode(data, smallHeap);
                if (tileBitmap == null) {
                    Log.d(DEBUG_TAG, "decoded tile is null");
                    throw new RemoteException();
                }
                Long l = pending.get(t.toId());
                if (l != null) {
                    mTileCache.putTile(t, tileBitmap, l);
                } // else wasn't in pending queue just ignore
                mDownloadFinishedHandler.sendEmptyMessage(MapTile.MAPTILE_SUCCESS_ID);
            } catch (StorageException | OutOfMemoryError e) {
                // unable to cache tile
                Log.w(DEBUG_TAG, "mapTileLoaded got " + e.getMessage());
                setSmallHeapMode();
            } catch (NullPointerException npe) {
                Log.d(DEBUG_TAG, "Exception in mapTileLoaded callback " + npe);
                throw new RemoteException();
            } finally {
                pending.remove(id);
            }
            if (MapViewConstants.DEBUGMODE) {
                Log.i(DEBUG_TAG, "MapTile download success." + t.toString());
            }
        }

        /**
         * Switch to "small heap mode" which uses tiles with slightly less quality
         */
        public void setSmallHeapMode() {
            if (!smallHeap) { // reduce tile size to half
                smallHeap = true;
                mTileCache.clear();
                // should toast this
            } else {
                Log.e(DEBUG_TAG, "already in small heap mode");
            }
        }

        /**
         * Called after a tile has failed
         * 
         * @param rendererID the tile renderer id
         * @param zoomLevel the zoom level
         * @param tileX tile x
         * @param tileY tile y
         * @param reason error code
         * @throws RemoteException if something goes wrong receiving the tile from the service
         */
        public void mapTileFailed(@NonNull final String rendererID, final int zoomLevel, final int tileX, final int tileY, final int reason)
                throws RemoteException {
            MapTile t = new MapTile(rendererID, zoomLevel, tileX, tileY);
            pending.remove(t.toId());
            mDownloadFinishedHandler.sendMessage(Message.obtain(mDownloadFinishedHandler, MapTile.MAPTILE_FAIL_ID, reason, 0));
        }
    };

    /**
     * Get some information on cache usage
     * 
     * @return a String with cache usage information suitable for display
     */
    @NonNull
    public String getCacheUsageInfo() {
        return mTileCache.getCacheUsageInfo();
    }
}
