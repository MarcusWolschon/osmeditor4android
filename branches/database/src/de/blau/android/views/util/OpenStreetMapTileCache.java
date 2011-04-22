// Created by plusminus on 17:58:57 - 25.09.2008
package  de.blau.android.views.util;

import java.util.HashMap;

import de.blau.android.services.util.OpenStreetMapTile;
import de.blau.android.views.util.OpenStreetMapViewConstants;

import android.graphics.Bitmap;

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
	
	protected HashMap<String, Bitmap> mCachedTiles;

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
	
	public static long defaultCacheBytes() {
		// Default to using half the available memory
		return Runtime.getRuntime().maxMemory() / 2;
	}
	
	public void clear() {
		mCachedTiles.clear();
	}
	
	public boolean containsTile(final OpenStreetMapTile aTile) {
		return mCachedTiles.containsKey(aTile.toString());
	}
	
	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
