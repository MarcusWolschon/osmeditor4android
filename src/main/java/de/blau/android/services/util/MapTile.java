package de.blau.android.services.util;

/**
 * This class merely holds the coordinates embedded in the url of a tile.<br/>
 * This class was taken from OpenStreetMapViewer (original package org.andnav.osm) in 2010-06
 * by Marcus Wolschon to be integrated into the de.blau.androin
 * OSMEditor. 
 * @author Nicolas Gramlich
 * @author Marcus Wolschon <Marcus@Wolschon.biz>
 */
public class MapTile {
	
	final public String rendererID;
	
	public int x;
	public int y;
	
	public int zoomLevel;
	
	private String id = null;

	public static final int MAPTILE_SUCCESS_ID = 0;
	public static final int MAPTILE_FAIL_ID = MAPTILE_SUCCESS_ID + 1;
	
	public MapTile(String rendererID, int zoomLevel, int tileX, int tileY) {
		this.rendererID = rendererID;
		this.zoomLevel = zoomLevel;
		x = tileX;
		y = tileY;
	}
	
	public MapTile(MapTile tile) {
		this.rendererID = tile.rendererID;
		this.zoomLevel = tile.zoomLevel;
		this.x = tile.x;
		this.y = tile.y;
	}

	@Override
	public String toString() {
		// Log.d("OpenStreetMapTile","Tile " + rendererID + "/" + zoomLevel + "/" + x + "/" + y);
		return rendererID + "/" + zoomLevel + "/" + x + "/" + y; 
	}
	
	/**
	 * Generate an unique id for this tile
	 * 
	 * @return the id, generate new if not in cache
	 */
	public String toId() {
		if (id == null) {
			// attempt to reduce the number of times StringBuilder.append is called 
			id = zoomLevel + rendererID + x + "/" + y; 
		}
		return id; 
	}

	/**
	 * Reset anything important so that the instance can be reused
	 */
	public void reinit() {
		id = null;
	}
}
