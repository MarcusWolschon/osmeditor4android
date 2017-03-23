// Created by plusminus on 17:58:57 - 25.09.2008
package  de.blau.android.views.util;

import android.graphics.Bitmap;
import android.util.Log;
import de.blau.android.exception.StorageException;
import de.blau.android.services.util.MapTile;

/**
 * 
 * <br/>
 * This class was taken from OpenStreetMapViewer (original package org.andnav.osm) in 2010
 * by Marcus Wolschon to be integrated into the de.blau.android.OSMEditor. 
 * @author Nicolas Gramlich
 * @author Marcus Wolschon <Marcus@Wolschon.biz>
 *
 */
public class MapTileCache implements MapViewConstants{
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================
	
	private LRUMapTileCache mCachedTiles;

	// ===========================================================
	// Constructors
	// ===========================================================
	
	public MapTileCache(){
		this(defaultCacheBytes());
	}
	
	/**
	 * @param aMaximumCacheBytes Maximum cache size in bytes.
	 */
	private MapTileCache(final long aMaximumCacheBytes){
		Log.d("OpenStreetMapTileCache","Created new in memory tile cache with " + aMaximumCacheBytes + " bytes");
		mCachedTiles = new LRUMapTileCache(aMaximumCacheBytes);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================
	
	public synchronized Bitmap getMapTile(final MapTile aTile) {
		return mCachedTiles.get(aTile.toId());
	}

	public synchronized boolean putTile(final MapTile aTile, final Bitmap aImage, final long owner) throws StorageException {
		return mCachedTiles.put(aTile.toId(), aImage, true, owner) != null;
	}
	
	public synchronized boolean putTile(final MapTile aTile, final Bitmap aImage, final boolean recycleable, final long owner) throws StorageException {
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
	 * @param aTile The tile to check for.
	 * @return true if the tile is in the cache.
	 */
	public boolean containsTile(final MapTile aTile) {
		return mCachedTiles.containsKey(aTile.toId());
	}
	
	/**
	 * Try to reduce memory use.
	 */
	public void onLowMemory() {
		mCachedTiles.onLowMemory();
	}

	public String getCacheUsageInfo() {
		return "Size " + mCachedTiles.cacheSizeBytes() + " of maximum " + mCachedTiles.getMaxCacheSize() + " #entries " + mCachedTiles.size();
	}
	
	
	
	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
