package de.blau.android.services.util;

/**
 * This class merely holds the coordinates embedded in the url of a tile.<br/>
 * This class was taken from OpenStreetMapViewer (original package org.andnav.osm) in 2010-06
 * by Marcus Wolschon to be integrated into the de.blau.androin
 * OSMEditor. 
 * @author Nicolas Gramlich
 * @author Marcus Wolschon <Marcus@Wolschon.biz>
 */
public class OpenStreetMapTile {
	
	public String rendererID;
	
	public int x;
	public int y;
	
	public int zoomLevel;

	public static final int MAPTILE_SUCCESS_ID = 0;
	public static final int MAPTILE_FAIL_ID = MAPTILE_SUCCESS_ID + 1;
	
	public OpenStreetMapTile(String rendererID, int zoomLevel, int tileX, int tileY) {
		this.rendererID = rendererID;
		this.zoomLevel = zoomLevel;
		x = tileX;
		y = tileY;
	}
	
	@Override
	public String toString() {
		return rendererID + "/" + zoomLevel + "/" + x + "/" + y;
	}

}
