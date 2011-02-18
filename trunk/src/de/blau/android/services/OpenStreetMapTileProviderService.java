package de.blau.android.services;

import de.blau.android.services.IOpenStreetMapTileProviderCallback;
import de.blau.android.services.IOpenStreetMapTileProviderService;
import de.blau.android.services.util.OpenStreetMapTile;
import de.blau.android.services.util.OpenStreetMapTileFilesystemProvider;
import de.blau.android.views.util.OpenStreetMapTileServer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

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
		mFileSystemProvider = new OpenStreetMapTileFilesystemProvider(
				this.getBaseContext(), 4 * 1024 * 1024); // 4MB FSCache
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
			return OpenStreetMapTileServer.getIds(getApplicationContext().getResources());
		}
		//@Override
		public void getMapTile(String rendererID, int zoomLevel, int tileX,
				int tileY, IOpenStreetMapTileProviderCallback callback)
				throws RemoteException {

			OpenStreetMapTile tile = new OpenStreetMapTile(rendererID, zoomLevel, tileX, tileY);
			mFileSystemProvider.loadMapTileAsync(tile, callback);
		}
	};

}
