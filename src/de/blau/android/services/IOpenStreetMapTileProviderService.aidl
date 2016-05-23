/*
 */

package de.blau.android.services;

import de.blau.android.services.IOpenStreetMapTileProviderCallback;

interface IOpenStreetMapTileProviderService {

	String[] getTileProviders();

	void getMapTile(in String rendererID, in int zoomLevel, in int tileX, in int tileY, in IOpenStreetMapTileProviderCallback callback);
	
	void flushCache(in String rendererID);
}
