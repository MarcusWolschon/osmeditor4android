package de.blau.android.layer.tiles.util;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.zip.GZIPInputStream;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.App;
import de.blau.android.exception.StorageException;
import de.blau.android.services.util.MapAsyncTileProvider;
import de.blau.android.services.util.MapTile;
import de.blau.android.services.util.MapTileFilesystemProvider;
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
public class MapTileProvider<T> {
    // ===========================================================
    // Constants
    // ===========================================================

    private static final int    UNZIP_BUFFER_SIZE = 4096;
    /**
     * Tag used in debug log-entries.
     */
    private static final int    TAG_LEN           = Math.min(LOG_TAG_LEN, MapTileProvider.class.getSimpleName().length());
    private static final String DEBUG_TAG         = MapTileProvider.class.getSimpleName().substring(0, TAG_LEN);

    private static final int MVT_CACHE_SIZE = 128;

    // ===========================================================
    // Fields
    // ===========================================================

    /**
     * cache provider
     */
    private final MapTileCache<T>   mTileCache;
    private final Map<String, Long> pending = new HashMap<>();

    private final Handler                   mDownloadFinishedHandler;
    private final TileDecoder<T>            decoder;
    private final ThreadPoolExecutor        mThreadPool;
    private final MapTileFilesystemProvider mapTileFilesystemProvider;

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
        @Nullable
        D decode(@NonNull byte[] data, boolean small);
    }

    public static class BitmapDecoder implements TileDecoder<Bitmap> {
        private BitmapFactory.Options options = new BitmapFactory.Options();

        private boolean hardwareRendering;

        /**
         * Construct a new decoder
         * 
         * @param hardwareRendering if true decode for hardware rendering
         */
        public BitmapDecoder(boolean hardwareRendering) {
            this.hardwareRendering = hardwareRendering;
        }

        @TargetApi(26)
        @Override
        public Bitmap decode(@NonNull byte[] data, boolean small) {
            if (hardwareRendering) {
                options.inPreferredConfig = Bitmap.Config.HARDWARE;
            } else if (small) {
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
     * @param decoder the TileDecoder to use
     * @param aDownloadFinishedListener handler to call when a tile download is complete
     */
    public MapTileProvider(@NonNull final Context ctx, @NonNull TileDecoder<T> decoder, @NonNull final Handler aDownloadFinishedListener) {
        mTileCache = decoder instanceof BitmapDecoder ? new MapTileCache<>() : new MapTileCache<>(MVT_CACHE_SIZE);

        smallHeap = Util.smallHeap();
        this.decoder = decoder;
        mDownloadFinishedHandler = aDownloadFinishedListener;

        mThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(App.getPreferences(ctx).getMaxTileDownloadThreads());

        mapTileFilesystemProvider = App.getMapTileFilesystemProvider(ctx);
    }

    /**
     * Clear out memory related to tracking map tiles.
     */
    public void clear() {
        synchronized (pending) {
            pending.clear();
        }
        mTileCache.clear();
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
        String id = aTile.toId();
        synchronized (pending) {
            if (!pending.containsKey(id)) {
                try {
                    pending.put(id, owner);
                    if (mapTileFilesystemProvider != null) {
                        // note aTile will be reused and needs to be copied
                        mapTileFilesystemProvider.loadMapTileAsync(new MapTile(aTile), mCallback);
                    }
                } catch (Exception e) {
                    Log.e(DEBUG_TAG, "Exception in preCacheTile()", e);
                }
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
        if (mapTileFilesystemProvider == null) {
            return;
        }
        try {
            mThreadPool.execute(() -> {
                mapTileFilesystemProvider.flushQueue(rendererId, zoomLevel);
                // remove the same from pending
                synchronized (pending) {
                    Set<String> keys = new HashSet<>(pending.keySet());
                    if (zoomLevel != MapAsyncTileProvider.ALLZOOMS) {
                        String id = Integer.toString(zoomLevel) + rendererId;
                        for (String key : keys) {
                            if (key.startsWith(id)) {
                                pending.remove(key);
                            }
                        }
                        return;
                    }
                    for (String key : keys) {
                        if (key.contains(rendererId)) {
                            pending.remove(key);
                        }
                    }
                }
            });
        } catch (RejectedExecutionException rjee) {
            Log.e(DEBUG_TAG, "Execution rejected " + rjee.getMessage());
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Exception in flushQueue()", e);
        }
    }

    /**
     * Flush the tile cache for a specific provider
     * 
     * @param rendererId the provider to flush or if null all
     * @param all if true flush the on device cache too
     */
    public void flushCache(@Nullable String rendererId, boolean all) {
        if (all && mapTileFilesystemProvider != null) {
            try {
                mapTileFilesystemProvider.flushCache(rendererId);
            } catch (Exception e) {
                Log.e(DEBUG_TAG, "Exception in flushCache()", e);
            }
        }
        mTileCache.clear(); // zap everything in in memory cache
    }

    /**
     * Tell the tile provider service to reread the database of TileLayerServers
     */
    public void update() {
        // no longer in use
    }

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================

    /**
     * Callback from the loading thread
     */
    private final MapTileProviderCallback mCallback = new MapTileProviderCallback() {

        @Override
        public void mapTileLoaded(@NonNull final String rendererID, final int zoomLevel, final int tileX, final int tileY, @NonNull final byte[] data)
                throws IOException {
            MapTile t = new MapTile(rendererID, zoomLevel, tileX, tileY);
            String id = t.toId();
            try {
                T tileBlob = decoder.decode(unGZip(data), smallHeap);
                if (tileBlob == null) {
                    Log.d(DEBUG_TAG, "decoded tile is null");
                    throw new IOException("decoded tile is null");
                }
                synchronized (pending) {
                    Long l = pending.get(id);
                    if (l != null) {
                        mTileCache.putTile(t, tileBlob, l);
                    } // else wasn't in pending queue just ignore
                }
                mDownloadFinishedHandler.sendEmptyMessage(MapTile.MAPTILE_SUCCESS_ID);
            } catch (StorageException | OutOfMemoryError e) {
                // unable to cache tile
                Log.w(DEBUG_TAG, "mapTileLoaded got " + e.getMessage());
                setSmallHeapMode();
            } catch (NullPointerException | NoClassDefFoundError npe) {
                Log.d(DEBUG_TAG, "Exception in mapTileLoaded callback " + npe);
                throw new IOException("Exception in mapTileLoaded callback " + npe);
            } finally {
                synchronized (pending) {
                    pending.remove(id);
                }
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

        @Override
        public void mapTileFailed(@NonNull final String rendererID, final int zoomLevel, final int tileX, final int tileY, final int reason, String message)
                throws IOException {
            MapTile t = new MapTile(rendererID, zoomLevel, tileX, tileY);
            synchronized (pending) {
                pending.remove(t.toId());
            }
            mDownloadFinishedHandler.sendMessage(Message.obtain(mDownloadFinishedHandler, MapTile.MAPTILE_FAIL_ID, reason, 0));
        }
    };

    /**
     * Unzip the data if it is zipped
     * 
     * @param data the potentially gzipped data
     * @return the unzipped data
     */
    @NonNull
    public static byte[] unGZip(@NonNull byte[] data) {
        // check magic number
        if (data.length > 3 && data[0] == (byte) 0x1F && data[1] == (byte) 0x8B && data[2] == (byte) 0x08) {
            // nearly all the objects allocated here could be reused
            try (ByteArrayInputStream in = new ByteArrayInputStream(data);
                    GZIPInputStream gis = new GZIPInputStream(in);
                    ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[UNZIP_BUFFER_SIZE];
                int len;
                while ((len = gis.read(buffer)) != -1) {
                    os.write(buffer, 0, len);
                }
                return os.toByteArray();
            } catch (IOException e) {
                Log.d(DEBUG_TAG, "Exception in unGZip " + e.getMessage());
            }
        }
        return data;
    }

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
