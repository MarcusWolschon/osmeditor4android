// Created by plusminus on 22:13:10 - 28.09.2008
package de.blau.android.views.util;

import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.LinkedList;

import android.graphics.Bitmap;
import android.util.Log;

/**
 * Simple LRU cache for any type of object. Implemented as an extended
 * <code>HashMap</code> with a maximum size and an aggregated <code>List</code>
 * as LRU queue.
 * <br/>
 * This class was taken from OpenStreetMapViewer (original package org.andnav.osm) in 2010-06
 * by Marcus Wolschon to be integrated into the de.blau.androin
 * OSMEditor. 
 * @author Nicolas Gramlich
 * @author Marcus Wolschon <Marcus@Wolschon.biz>
 *
 */
public class LRUMapTileCache extends HashMap<String, Bitmap> {

	// ===========================================================
	// Constants
	// ===========================================================
	
	private static final long serialVersionUID = 3345124753192560743L;

	// ===========================================================
	// Fields
	// ===========================================================
	
	/** Maximum cache size. */
	private long maxCacheSize;
	/** LRU list. */
	private final LinkedList<CacheElement> list;
	private class CacheElement implements Serializable {
		private static final long serialVersionUID = 1;
		boolean recycleable = true;
		String key;
		
		public CacheElement(String key, boolean recycleable) {
			this.key = key;
			this.recycleable = recycleable;
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
		list = new LinkedList<CacheElement>();
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
	@Override
	public synchronized void clear() {
		for (CacheElement ce:list) {
			Bitmap b = super.get(ce.key);
			if (b != null && ce.recycleable) {
				b.recycle();
			}
		}
		list.clear();
		super.clear();
	}
	
	/**
	 * Ensure the cache is less than its limit, less some extra.
	 * @param extra Extra space to take away from the cache size. Used to make room
	 * for new items before adding them so that the total cache never exceeds the limit.
	 */
	private void applyCacheLimit(long extra) {
		long limit = maxCacheSize - extra;
		if (limit < 0) {
			limit = 0;
		}
		while (cacheSizeBytes() > limit && !list.isEmpty()) {
			CacheElement ce = list.getLast();
			Bitmap b = remove(ce.key);
			if (b != null && !b.isRecycled() && ce.recycleable) {
				b.recycle();
			}
		}
	}
	
	/**
	 * Reduces memory use by halving the cache size.
	 */
	public void onLowMemory() {
		maxCacheSize /= 2;
		applyCacheLimit(0);
	}
	
	@Override
	public boolean containsKey(Object key) {
		if (super.containsKey(key)) {
			Bitmap b = super.get(key);
			if (b != null && !b.isRecycled()) {
				return true;
			}
			remove(key);
		}
		return false;
	}
	
	/**
	 * Calculate the amount of memory used by the cache.
	 * @return The number of bytes used by the cache.
	 */
	public long cacheSizeBytes() {
		long result = 0;
		for (Bitmap b : values()) {
			if (b != null && !b.isRecycled()) {
				result += b.getRowBytes() * b.getHeight();
			}
		}
		return result;
	}
	
	public long getMaxCacheSize() {
		return maxCacheSize;
	}

	/**
	 * Overrides <code>put()</code> so that it also updates the LRU list.
	 * 
	 * @param key
	 *            key with which the specified value is to be associated
	 * @param value
	 *            value to be associated with the key
	 * @return previous value associated with key or <code>null</code> if there
	 *         was no mapping for key; a <code>null</code> return can also
	 *         indicate that the cache previously associated <code>null</code>
	 *         with the specified key
	 */
	public synchronized Bitmap put(final String key, final Bitmap value, boolean recycleable) {
		// Log.d("LRUMapTileCache","put " + key + " " + recycleable);
		if (maxCacheSize == 0 || value == null){
			return null;
		}

		// if the key isn't in the cache and the cache is full...
		if (!containsKey(key)) {
			applyCacheLimit(value.getRowBytes() * value.getHeight());
		}

		updateKey(key, recycleable);
		// Log.d("LRUMapTileCache","put done");
		return super.put(key, value);
	}

	/**
	 * Overrides <code>get()</code> so that it also updates the LRU list.
	 * 
	 * @param key
	 *            key with which the expected value is associated
	 * @return the value to which the cache maps the specified key, or
	 *         <code>null</code> if the map contains no mapping for this key
	 */
	public synchronized Bitmap get(final String key) {
		final Bitmap value = super.get(key);
		// Log.d("LRUMapTileCache","get " + key);
		CacheElement toUpdate = null;
		if (value != null) {
			for (CacheElement ce:list) {
				if (ce.key.equals(key))
					toUpdate = ce;
			}
		}
		if (toUpdate != null)
			updateKey(key,toUpdate.recycleable);
		// Log.d("LRUMapTileCache","get done");
		return value;
	}

	public synchronized Bitmap remove(final String key) {
		CacheElement toRemove = null;
		for (CacheElement ce:list) {
			if (ce.key.equals(key)) {
				toRemove = ce;
				break;
			}
		}
		if (toRemove != null)
			list.remove(toRemove);
		return super.remove(key);
	}

	/**
	 * Moves the specified value to the top of the LRU list (the bottom of the
	 * list is where least recently used items live).
	 * 
	 * @param key of the value to move to the top of the list
	 */
	private synchronized void updateKey(final String key, final boolean recycleable) {
		// Log.d("LRUMapTileCache","updateKey " + key);
		CacheElement toRemove = null;
		for (CacheElement ce:list) {
			if (ce.key.equals(key)) {
				toRemove = ce;
				break;
			}
		}
		if (toRemove != null)
			list.remove(toRemove);
		list.addFirst(new CacheElement(key, recycleable));
		// Log.d("LRUMapTileCache","updateKey done");
	}
	
	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
