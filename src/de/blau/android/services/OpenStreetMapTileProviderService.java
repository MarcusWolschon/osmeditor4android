package de.blau.android.services;

import java.io.File;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;
import de.blau.android.R;
import de.blau.android.contract.Paths;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.services.util.OpenStreetMapTile;
import de.blau.android.services.util.OpenStreetMapTileFilesystemProvider;
import de.blau.android.services.util.OpenStreetMapTileProviderDataBase;

/**
 * The OpenStreetMapTileProviderService can download map tiles from a server and
 * stores them in a file system cache. <br/>
 * This class was taken from OpenStreetMapViewer (original package
 * org.andnav.osm) in 2010-06 by Marcus Wolschon to be integrated into the
 * de.blau.androin OSMEditor.
 * 
 * @author Marcus Wolschon <Marcus@Wolschon.biz>
 * @author Manuel Stahl
 */
public class OpenStreetMapTileProviderService extends Service {

	private OpenStreetMapTileFilesystemProvider mFileSystemProvider;
	private boolean mountPointWiteable = false;

	@Override
	public void onCreate() {
		super.onCreate();
		init();
	}

	@SuppressLint("NewApi")
	/**
	 * Tries to put the tile cache on a removable sd card if present and we
	 * haven't already created the cache
	 */
	private void init() {
		Preferences prefs = new Preferences(getApplicationContext());
		int tileCacheSize = 100; // just in case we can't read the prefs
		if (prefs != null) {
			tileCacheSize = prefs.getTileCacheSize();
		}
		File mountPoint = null;
		// check for classic location first
		File classicMountPoint = Environment.getExternalStorageDirectory();
		File classicTileDir = new File(classicMountPoint, Paths.DIRECTORY_PATH_TILE_CACHE_CLASSIC);
		if (classicTileDir.exists()) {
			// remove old database
			OpenStreetMapTileProviderDataBase.delete(getBaseContext());
		}

		File[] storageDirectories = ContextCompat.getExternalFilesDirs(getBaseContext(), null);
		for (File dir : storageDirectories) { // iterate over the directories preferring a removable one if possible
			if (dir==null) {
				Log.d("OpenStreetMapTileProviderService","storage dir null");
				continue;
			}
			Log.d("OpenStreetMapTileProviderService", "candidate storage directory " + dir.getPath());
			if (OpenStreetMapTileProviderDataBase.exists(dir)) { // existing tile cache, use
				mountPointWiteable = dir.canWrite();
				mountPoint = dir;
			} else if (dir.canWrite()) {
				mountPointWiteable = true;
				mountPoint = dir;
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					if (Environment.isExternalStorageRemovable(dir)) {
						break;
					}
				}
			}
		}

		if (mountPointWiteable) {
			Log.d("OpenStreetMapTileProviderService",
					"Setting cache size to " + tileCacheSize + " on " + mountPoint.getPath());
			mFileSystemProvider = new OpenStreetMapTileFilesystemProvider(getBaseContext(), mountPoint,
					tileCacheSize * 1024 * 1024); // FSCache
		} else {
			Toast.makeText(this, R.string.toast_storage_error, Toast.LENGTH_LONG).show();
			// FIXME potentially we should set both background and overlay
			// preferences to NONE here or simply zap what we are currently are
			// using.
			// don't terminate, simply ignore requests
		}
	}

	@Override
	public void onDestroy() {
		if (mFileSystemProvider != null) {
			mFileSystemProvider.destroy();
		}
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
		// @Override
		public String[] getTileProviders() throws RemoteException {
			return TileLayerServer.getIds(false);
		}

		// @Override
		public void getMapTile(String rendererID, int zoomLevel, int tileX, int tileY,
				IOpenStreetMapTileProviderCallback callback) throws RemoteException {
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
