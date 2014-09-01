package de.blau.android.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import de.blau.android.prefs.Preferences;
import de.blau.android.services.util.OpenStreetMapTile;
import de.blau.android.services.util.OpenStreetMapTileFilesystemProvider;
import de.blau.android.views.util.OpenStreetMapTileServer;

/**
 * The OpenStreetMapTileProviderService can download map tiles from a server
 * and stores them in a file system cache. <br/>
 * This class was taken from OpenStreetMapViewer (original package org.andnav.osm) in 2010-06
 * by Marcus Wolschon to be integrated into the de.blau.androin
 * OSMEditor. 
 * @author Marcus Wolschon <Marcus@Wolschon.biz>
 * @author Manuel Stahl
 */
public class OpenStreetMapTileProviderService extends Service {

	private OpenStreetMapTileFilesystemProvider mFileSystemProvider;
	
	@Override
	public void onCreate() {
		super.onCreate();
		Preferences prefs = new Preferences(getApplicationContext());
		int tileCacheSize = 100; // just in case we can't read the prefs
		if (prefs != null) {
			tileCacheSize = prefs.getTileCacheSize();
		}
		Log.d("OpenStreetMapTilePRoviderService", "Setting cache size to " + tileCacheSize);
		mFileSystemProvider = new OpenStreetMapTileFilesystemProvider(
				getBaseContext(),tileCacheSize * 1024 * 1024); //  FSCache
	}
	
	@Override
	public void onDestroy() {
		mFileSystemProvider.destroy();
		super.onDestroy();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	/**
	 * The IRemoteInterface is defined through IDL
	 */
	private final IOpenStreetMapTileProviderService.Stub mBinder = new IOpenStreetMapTileProviderService.Stub() {
		//@Override
		public String[] getTileProviders() throws RemoteException {
			return OpenStreetMapTileServer.getIds(false);
		}
		//@Override
		public void getMapTile(String rendererID, int zoomLevel, int tileX,
				int tileY, IOpenStreetMapTileProviderCallback callback)
				throws RemoteException {
			OpenStreetMapTile tile = new OpenStreetMapTile(rendererID, zoomLevel, tileX, tileY);
			mFileSystemProvider.loadMapTileAsync(tile, callback);
		}
		
		public void flushCache(String rendererId) {
			mFileSystemProvider.flushCache(rendererId);
		}
	};
}
