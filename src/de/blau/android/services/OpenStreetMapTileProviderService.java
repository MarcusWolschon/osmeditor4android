package de.blau.android.services;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;
import de.blau.android.R;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.services.util.OpenStreetMapTile;
import de.blau.android.services.util.OpenStreetMapTileFilesystemProvider;

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
	private boolean mountPointWiteable = false;
	
	@Override
	public void onCreate() {
		super.onCreate();
		Preferences prefs = new Preferences(getApplicationContext());
		int tileCacheSize = 100; // just in case we can't read the prefs
		if (prefs != null) {
			tileCacheSize = prefs.getTileCacheSize();
		}
		if (Environment.getExternalStorageDirectory().canWrite()) {
			Log.d("OpenStreetMapTilePRoviderService", "Setting cache size to " + tileCacheSize + " on " + Environment.getExternalStorageDirectory().getPath());
			mountPointWiteable = true;
			mFileSystemProvider = new OpenStreetMapTileFilesystemProvider(
				getBaseContext(),Environment.getExternalStorageDirectory(), tileCacheSize * 1024 * 1024); //  FSCache
		} else {
			Toast.makeText(this,R.string.toast_storage_error, Toast.LENGTH_LONG).show();
			// FIXME potentially we should set both background and overlay preferences to NONE here or simply zap what we are currently are using.
			// don't terminate, simply igonre requests
		}
		
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
			return TileLayerServer.getIds(false);
		}
		//@Override
		public void getMapTile(String rendererID, int zoomLevel, int tileX,
				int tileY, IOpenStreetMapTileProviderCallback callback)
				throws RemoteException {
			if (!mountPointWiteable) { // fail silently
				return;
			}
			OpenStreetMapTile tile = new OpenStreetMapTile(rendererID, zoomLevel, tileX, tileY);
			mFileSystemProvider.loadMapTileAsync(tile, callback);
		}
		
		public void flushCache(String rendererId) {
			mFileSystemProvider.flushCache(rendererId);
		}
	};
}
