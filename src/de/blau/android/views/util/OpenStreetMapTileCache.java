// Created by plusminus on 17:58:57 - 25.09.2008
package  de.blau.android.views.util;

import android.graphics.Bitmap;
import de.blau.android.services.util.OpenStreetMapTile;

/**
 * 
 * <br/>
 * This class was taken from OpenStreetMapViewer (original package org.andnav.osm) in 2010
 * by Marcus Wolschon to be integrated into the de.blau.android.OSMEditor. 
 * @author Nicolas Gramlich
 * @author Marcus Wolschon <Marcus@Wolschon.biz>
 *
 */
public class OpenStreetMapTileCache implements OpenStreetMapViewConstants{
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================
	
	protected LRUMapTileCache mCachedTiles;

	// ===========================================================
	// Constructors
	// ===========================================================
	
	public OpenStreetMapTileCache(){
		this(defaultCacheBytes());
	}
	
	/**
	 * @param aMaximumCacheBytes Maximum cache size in bytes.
	 */
	public OpenStreetMapTileCache(final long aMaximumCacheBytes){
		mCachedTiles = new LRUMapTileCache(aMaximumCacheBytes);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================
	
	public synchronized Bitmap getMapTile(final OpenStreetMapTile aTile) {
		return mCachedTiles.get(aTile.toString());
	}

	public synchronized void putTile(final OpenStreetMapTile aTile, final Bitmap aImage) {
		mCachedTiles.put(aTile.toString(), aImage);
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
	public static long defaultCacheBytes() {
		// Default to using half the available memory
		return Runtime.getRuntime().maxMemory() / 4;
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
	public boolean containsTile(final OpenStreetMapTile aTile) {
		return mCachedTiles.containsKey(aTile.toString());
	}
	
	/**
	 * Try to reduce memory use.
	 */
	public void onLowMemory() {
		mCachedTiles.onLowMemory();
	}
	
	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
