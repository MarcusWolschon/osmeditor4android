/*
 */

package de.blau.android.services;

import de.blau.android.services.IMapTileProviderCallback;

interface IMapTileProviderService {

	String[] getTileProviders();

	void getMapTile(in String rendererID, in int zoomLevel, in int tileX, in int tileY, in IMapTileProviderCallback callback);
	
	void flushCache(in String rendererID);
}
