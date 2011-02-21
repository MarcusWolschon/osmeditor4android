// Created by plusminus on 18:23:16 - 25.09.2008
package  de.blau.android.views.util;

import android.content.res.Resources;
import de.blau.android.R;
import de.blau.android.services.util.OpenStreetMapTile;

/**
 * The OpenStreetMapRendererInfo stores information about available tile servers.
 * <br/>
 * This class was taken from OpenStreetMapViewer (original package org.andnav.osm) in 2010-06
 * by Marcus Wolschon to be integrated into the de.blau.androin
 * OSMEditor. 
 * @author Nicolas Gramlich
 * @author Marcus Wolschon <Marcus@Wolschon.biz>
 *
 */
public class OpenStreetMapTileServer {
	
	// ===========================================================
	// Fields
	// ===========================================================
	
	public final String ID, BASEURL, IMAGE_FILENAMEENDING;
	public final int ZOOM_MINLEVEL, ZOOM_MAXLEVEL, MAPTILE_ZOOM, MAPTILE_SIZEPX;
	
	// ===========================================================
	// Constructors
	// ===========================================================
	
	private OpenStreetMapTileServer(final String id, final String config) {
		String[] cfgItems = config.split("\\s+");
		ID = id;
		BASEURL = cfgItems[0];
		IMAGE_FILENAMEENDING = cfgItems[1];
		ZOOM_MINLEVEL = Integer.parseInt(cfgItems[2]);
		ZOOM_MAXLEVEL = Integer.parseInt(cfgItems[3]);
		MAPTILE_ZOOM = Integer.parseInt(cfgItems[4]);
		MAPTILE_SIZEPX = 1 << MAPTILE_ZOOM;
	}
	
	public static OpenStreetMapTileServer getDefault(final Resources r) {
		// ask for an invalid renderer, so we'll get the fallback default
		return get(r, "");
	}
	
	/**
	 * Get the tile server information for a specified tile server ID. If the given
	 * ID cannot be found, a default renderer is selected.
	 * @param r The application resources.
	 * @param id The internal ID of the tile layer, eg "MAPNIK"
	 * @return
	 */
	public static OpenStreetMapTileServer get(final Resources r, final String id) {
		String ids[] = r.getStringArray(R.array.renderer_ids);
		String cfgs[] = r.getStringArray(R.array.renderer_configs);
		OpenStreetMapTileServer result = null;
		for (int i = 0; i < ids.length; ++i) {
			if (ids[i].equals(id) ||
				// check for default renderer MAPNIK here
				(result == null && ids[i].equals("MAPNIK"))) {
				result = new OpenStreetMapTileServer(ids[i], cfgs[i]);
			}
		}
		return result;
	}
	
	// ===========================================================
	// Methods
	// ===========================================================
	
	public static String[] getIds(final Resources r) {
		return r.getStringArray(R.array.renderer_ids);
	}
	
	public String getTileURLString(final OpenStreetMapTile aTile) {
		String result = BASEURL;
		result = result.replaceFirst("\\!" , Integer.toString(aTile.zoomLevel));
		result = result.replaceFirst("\\!" , Integer.toString(aTile.x));
		result = result.replaceFirst("\\!" , Integer.toString(aTile.y));
		result = result.replaceFirst("\\$z", Integer.toString(aTile.zoomLevel));
		result = result.replaceFirst("\\$x", Integer.toString(aTile.x));
		result = result.replaceFirst("\\$y", Integer.toString(aTile.y));
		result = result.replaceFirst("\\$quadkey", quadTree(aTile));
		return result;
	}
	
	/**
	 * Converts TMS tile coordinates to QuadTree
	 * @param aTile The tile coordinates to convert
	 * @return The QuadTree as String.
	 */
	private String quadTree(final OpenStreetMapTile aTile) {
		StringBuilder quadKey = new StringBuilder();
		for (int i = aTile.zoomLevel; i > 0; i--) {
			int digit = 0;
			int mask = 1 << (i - 1);
			if ((aTile.x & mask) != 0)
				digit += 1;
			if ((aTile.y & mask) != 0)
				digit += 2;
			quadKey.append(digit);
		}
		return quadKey.toString();
	}
	
}
