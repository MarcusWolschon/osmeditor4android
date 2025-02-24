// Created by plusminus on 17:58:57 - 25.09.2008
package de.blau.android.layer.tiles.util;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.exception.StorageException;
import de.blau.android.services.util.MapTile;

/**
 * This class was taken from OpenStreetMapViewer (original package org.andnav.osm) in 2010 by Marcus Wolschon to be
 * integrated into the de.blau.android.OSMEditor.
 * 
 * @author Nicolas Gramlich
 * @author Marcus Wolschon &lt;Marcus@Wolschon.biz&gt;
 * @author Simon Poole
 *
 */
public class MapTileCache<T> {
    // ===========================================================
    // Constants
    // ===========================================================

    // ===========================================================
    // Fields
    // ===========================================================

    private static final String DEBUG_TAG = MapTileCache.class.getSimpleName().substring(0, Math.min(23, MapTileCache.class.getSimpleName().length()));
    private final LRUMapTileCache<T>  mCachedTiles;

    // ===========================================================
    // Constructors
    // ===========================================================

    /**
     * Construct a new cache of default size
     */
    public MapTileCache() {
        this(defaultCacheBytes());
    }

    /**
     * Construct a new cache
     * 
     * @param aMaximumCacheBytes Maximum cache size in bytes.
     */
    public MapTileCache(final long aMaximumCacheBytes) {
        Log.d(DEBUG_TAG, "Created new in memory tile cache with " + aMaximumCacheBytes + " bytes");
        mCachedTiles = new LRUMapTileCache<>(aMaximumCacheBytes);
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    /**
     * Get a tile from the cache
     * 
     * @param aTile the tile specification
     * @return the tile or null if not found
     */
    @Nullable
    public synchronized T getMapTile(@NonNull final MapTile aTile) {
        return mCachedTiles.get(aTile.toId());
    }

    /**
     * Put a tile in to the cache
     * 
     * @param aTile the tile spec
     * @param aImage the tile Bitmap
     * @param owner a ref to the owner
     * @return true if there was no previous mapping for this tile
     * @throws StorageException if we coudn't store the tile
     */
    public synchronized boolean putTile(@NonNull final MapTile aTile, @NonNull final T aImage, final long owner) throws StorageException {
        return mCachedTiles.put(aTile.toId(), aImage, true, owner) != null;
    }

    /**
     * Put a tile in to the cache
     * 
     * @param aTile the tile spec
     * @param aImage the tile Bitmap
     * @param owner a ref to the owner
     * @param recycleable treue if the Bitmap can be recycled
     * @return true if there was no previous mapping for this tile
     * @throws StorageException if we coudn't store the tile
     */
    public synchronized boolean putTile(@NonNull final MapTile aTile, @NonNull final T aImage, final boolean recycleable, final long owner)
            throws StorageException {
        return mCachedTiles.put(aTile.toId(), aImage, recycleable, owner) != null;
    }

    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================

    // ===========================================================
    // Methods
    // ===========================================================

    /**
     * Returns a suitable default for the cache size.
     * 
     * @return The default cache size.
     */
    private static long defaultCacheBytes() {
        // Default to using half the available memory
        return Runtime.getRuntime().maxMemory() / 8;
    }

    /**
     * Clear the tile cache.
     */
    public void clear() {
        mCachedTiles.clear();
    }

    /**
     * Test if the cache contains the specified tile.
     * 
     * @param aTile The tile to check for.
     * @return true if the tile is in the cache.
     */
    public boolean containsTile(@NonNull final MapTile aTile) {
        return mCachedTiles.containsKey(aTile.toId());
    }

    /**
     * Try to reduce memory use.
     */
    public void onLowMemory() {
        mCachedTiles.onLowMemory();
    }

    /**
     * Get some stats on cache usage
     * 
     * @return a string with some stats
     */
    @NonNull
    public String getCacheUsageInfo() {
        return "Size " + mCachedTiles.cacheSizeBytes() + " of maximum " + mCachedTiles.getMaxCacheSize() + " #entries " + mCachedTiles.size();
    }

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================
}
