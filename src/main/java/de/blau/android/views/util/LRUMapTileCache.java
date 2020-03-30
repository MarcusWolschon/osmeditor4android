// Created by plusminus on 22:13:10 - 28.09.2008
package de.blau.android.views.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import de.blau.android.exception.StorageException;

/**
 * Simple LRU cache for any type of object. Implemented as an extended <code>HashMap</code> with a maximum size and an
 * aggregated <code>List</code> as LRU queue.
 * 
 * This class was taken from OpenStreetMapViewer (original package org.andnav.osm) in 2010-06 by Marcus Wolschon to be
 * integrated into the de.blau.androin OSMEditor.
 * 
 * @author Nicolas Gramlich
 * @author Marcus Wolschon &lt;Marcus@Wolschon.biz&gt;
 * @author Simon Poole
 *
 */
public class LRUMapTileCache {

    private static final String DEBUG_TAG = "LRUMapTileCache";

    // ===========================================================
    // Constants
    // ===========================================================

    // ===========================================================
    // Fields
    // ===========================================================

    Map<String, CacheElement> cache;

    /** Maximum cache size. */
    private long                     maxCacheSize;
    /** Current cache size **/
    private long                     cacheSize = 0;
    /** LRU list. */
    private final List<CacheElement> list;
    private final List<CacheElement> reuseList;

    private class CacheElement {
        boolean recycleable = true;
        String  key;
        Bitmap  bitmap;
        long    owner;

        /**
         * Container for a cached Bitmap
         * 
         * @param key the key in to the cache
         * @param bitmap the Bitmap
         * @param recycleable if true the Bitmap can be recycled
         * @param owner owner reference
         */
        public CacheElement(@NonNull String key, @NonNull Bitmap bitmap, boolean recycleable, long owner) {
            init(key, bitmap, recycleable, owner);
        }

        /**
         * Initialize the container
         * 
         * @param key the key in to the cache
         * @param bitmap the Bitmap
         * @param recycleable if true the Bitmap can be recycled
         * @param owner owner reference
         */
        void init(@Nullable String key, @Nullable Bitmap bitmap, boolean recycleable, long owner) {
            if (key == null) {
                throw new IllegalArgumentException("key cannot be null");
            }
            if (bitmap == null) {
                throw new IllegalArgumentException("bitmap cannot be null");
            }
            this.recycleable = recycleable;
            this.key = key;
            this.bitmap = bitmap;
            this.owner = owner;
        }
    }

    // ===========================================================
    // Constructors
    // ===========================================================

    /**
     * Constructs a new LRU cache instance.
     * 
     * @param maxCacheSize the maximum number of entries in this cache before entries are aged off.
     */
    public LRUMapTileCache(final long maxCacheSize) {
        super();
        // Log.d("LRUMapTileCache","created");
        this.maxCacheSize = maxCacheSize;
        cache = new HashMap<>();
        list = new ArrayList<>(); // using a LinkedList doesn't have any real advantages
        reuseList = new ArrayList<>();
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================

    /**
     * Overrides clear() to also clear the LRU list.
     */
    public synchronized void clear() {
        for (CacheElement ce : cache.values()) {
            Bitmap b = ce.bitmap;
            if (b != null && ce.recycleable) {
                b.recycle();
            }
        }
        cache.clear();
        list.clear();
    }

    /**
     * Ensure the cache is less than its limit, less some extra.
     * 
     * @param extra Extra space to take away from the cache size. Used to make room for new items before adding them so
     *            that the total cache never exceeds the limit.
     * @param owner a long indicating who added the element to the cache
     * @return true if the limit was successfully applied
     */
    private synchronized boolean applyCacheLimit(long extra, long owner) {
        long limit = maxCacheSize - extra;
        if (limit < 0) {
            limit = 0;
        }
        while (cacheSize > limit && !list.isEmpty()) {
            // Log.d(DEBUG_TAG,"removing bitmap from in memory cache " + cacheSize);
            CacheElement ce = list.remove(list.size() - 1);
            if (ce.owner == owner && owner != 0) {
                // cache is being thrashed because it is too small, fail
                Log.d(DEBUG_TAG, "cache too small, failing");
                return false;
            }
            if (cache.remove(ce.key) == null) {
                throw new IllegalStateException("can't remove " + ce.key + " from cache");
            }
            reuseList.add(ce);
            Bitmap b = ce.bitmap;
            if (b != null && !b.isRecycled()) {
                cacheSize -= b.getRowBytes() * b.getHeight();
                if (ce.recycleable) {
                    b.recycle();
                }
            }
        }
        return true; // success
    }

    /**
     * Current number of entries
     * 
     * @return count
     */
    public int size() {
        return cache.size();
    }

    /**
     * Reduces memory use by halving the cache size.
     */
    public void onLowMemory() {
        maxCacheSize /= 2;
        applyCacheLimit(0, 0);
    }

    /**
     * Check if the cache contains an element for a key
     * 
     * @param key the key
     * @return true if present
     */
    public synchronized boolean containsKey(@NonNull String key) {
        return cache.containsKey(key);
    }

    /**
     * Calculate the amount of memory used by the cache.
     * 
     * @return The number of bytes used by the cache.
     */
    public long cacheSizeBytes() {
        return cacheSize;
    }

    /**
     * Get the current maximum cache size
     * 
     * @return a long indicating the current maximum cache size in bytes
     */
    public long getMaxCacheSize() {
        return maxCacheSize;
    }

    /**
     * Overrides <code>put()</code> so that it also updates the LRU list. Interesting enough the slight change in
     * signature does work
     * 
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the key
     * @param recycleable true if the element can be recycled
     * @param owner a long indicating what is putting the element in the cache
     * @return previous value associated with key or <code>null</code> if there was no mapping for key; a
     *         <code>null</code> return can also indicate that the cache previously associated <code>null</code> with
     *         the specified key
     * @throws StorageException if we can't expand the cache anymore
     */
    public synchronized Bitmap put(@NonNull final String key, @NonNull final Bitmap value, boolean recycleable, long owner) throws StorageException {
        // Log.d("LRUMapTileCache","put " + key + " " + recycleable);
        if (maxCacheSize == 0 || value == null) {
            return null;
        }

        CacheElement prev = cache.get(key);
        // if the key isn't in the cache and the cache is full...
        if (prev == null) {
            long bitmapSize = (long) value.getRowBytes() * value.getHeight();
            if (!applyCacheLimit(bitmapSize * 2, owner)) {
                // failed: cache is to small to handle all tiles necessary for one draw cycle
                // see if we can expand by 50%
                if (maxCacheSize < (Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory()) && (maxCacheSize / 2 > bitmapSize)) {
                    Log.w(DEBUG_TAG, "expanding memory tile cache from " + maxCacheSize + " to " + (maxCacheSize + maxCacheSize / 2));
                    maxCacheSize = maxCacheSize + maxCacheSize / 2;
                } else {
                    throw new StorageException(StorageException.OOM); // can't expand any more
                }
            }
            // avoid creating new objects
            CacheElement ce = null;
            if (!reuseList.isEmpty()) {
                ce = reuseList.remove(0);
                ce.init(key, value, recycleable, owner);
            } else {
                ce = new CacheElement(key, value, recycleable, owner);
            }
            list.add(0, ce);
            cache.put(key, ce);
            cacheSize += bitmapSize;
        } else {
            update(prev);
        }

        // Log.d("LRUMapTileCache","put done");
        return value;
    }

    /**
     * Overrides <code>get()</code> so that it also updates the LRU list.
     * 
     * @param key key with which the expected value is associated
     * @return the value to which the cache maps the specified key, or <code>null</code> if the map contains no mapping
     *         for this key
     */
    public synchronized Bitmap get(final String key) {
        final CacheElement value = cache.get(key);
        // Log.d("LRUMapTileCache","get " + key);
        if (value != null) {
            update(value);
            return value.bitmap;
        }
        // Log.d("LRUMapTileCache","get done");
        return null;
    }

    /**
     * Moves the specified value to the top of the LRU list (the bottom of the list is where least recently used items
     * live).
     * 
     * @param value to move to the top of the list
     */
    private synchronized void update(final CacheElement value) {
        list.remove(value);
        list.add(0, value);
    }

    // ===========================================================
    // Methods
    // ===========================================================

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================
}
