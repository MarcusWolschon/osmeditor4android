/*
 */

package de.blau.android.services;

import de.blau.android.services.IMapTileProviderCallback;

interface IMapTileProviderService {

	void getMapTile(in String rendererID, in int zoomLevel, in int tileX, in int tileY, in IMapTileProviderCallback callback);
	
	void flushCache(in String rendererID);
	
	void flushQueue(in String rendererID, in int zoomLevel);
	
	void update();
}
