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
		this(CACHE_MAPTILECOUNT_DEFAULT);
	}
	
	/**
	 * @param aMaximumCacheSize Maximum amount of MapTiles to be hold within.
	 */
	public OpenStreetMapTileCache(final int aMaximumCacheSize){
		mCachedTiles = new LRUMapTileCache(aMaximumCacheSize);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================
	
	public synchronized Bitmap getMapTile(final OpenStreetMapTile aTile) {
		return this.mCachedTiles.get(aTile.toString());
	}

	public synchronized void putTile(final OpenStreetMapTile aTile, final Bitmap aImage) {
		this.mCachedTiles.put(aTile.toString(), aImage);
	}

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================
	
	public void clear() {
		mCachedTiles.clear();
	}
	
	public boolean containsTile(final OpenStreetMapTile aTile) {
		return this.mCachedTiles.containsKey(aTile.toString());
	}
	
	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
