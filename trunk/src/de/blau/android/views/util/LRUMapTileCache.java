// Created by plusminus on 22:13:10 - 28.09.2008
package de.blau.android.views.util;

import java.util.HashMap;
import java.util.LinkedList;

import android.graphics.Bitmap;

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
	
	private static final long serialVersionUID = 3345124753192560741L;

	// ===========================================================
	// Fields
	// ===========================================================
	
	/** Maximum cache size. */
	private final long maxCacheSize;
	/** LRU list. */
	private final LinkedList<String> list;

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
		this.maxCacheSize = maxCacheSize;
		this.list = new LinkedList<String>();
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
		list.clear();
		for (Bitmap b : values()) {
			if (b != null) {
				b.recycle();
			}
		}
		super.clear();
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
	private long cacheSizeBytes() {
		long result = 0;
		for (Bitmap b : values()) {
			if (b != null && !b.isRecycled()) {
				result += b.getRowBytes() * b.getHeight();
			}
		}
		return result;
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
	public synchronized Bitmap put(final String key, final Bitmap value) {
		if (maxCacheSize == 0 || value == null){
			return null;
		}

		// if the key isn't in the cache and the cache is full...
		if (!containsKey(key)) {
			while (cacheSizeBytes() >= maxCacheSize) {
				remove(list.getLast()).recycle();
			}
		}

		updateKey(key);
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
		if (value != null) {
			updateKey(key);
		}
		return value;
	}

	public synchronized Bitmap remove(final String key) {
		list.remove(key);
		return super.remove(key);
	}

	/**
	 * Moves the specified value to the top of the LRU list (the bottom of the
	 * list is where least recently used items live).
	 * 
	 * @param key of the value to move to the top of the list
	 */
	private void updateKey(final String key) {
		list.remove(key);
		list.addFirst(key);
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
