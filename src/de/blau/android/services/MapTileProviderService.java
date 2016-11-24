package de.blau.android.services;

import java.io.File;

import org.acra.ACRA;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
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
import de.blau.android.services.util.MapTile;
import de.blau.android.services.util.MapTileFilesystemProvider;
import de.blau.android.services.util.MapTileProviderDataBase;

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
public class MapTileProviderService extends Service {

	private static final String DEBUG_TAG = MapTileProviderService.class.getSimpleName();
	private MapTileFilesystemProvider mFileSystemProvider;
	private boolean mountPointWriteable = false;

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
			MapTileProviderDataBase.delete(getBaseContext());
		}

		File[] storageDirectories = ContextCompat.getExternalFilesDirs(getBaseContext(), null);
		for (File dir : storageDirectories) { // iterate over the directories preferring a removable one if possible
			if (dir==null) {
				Log.d(DEBUG_TAG,"storage dir null");
				continue;
			}
			Log.d(DEBUG_TAG, "candidate storage directory " + dir.getPath());
			if (MapTileProviderDataBase.exists(dir)) { // existing tile cache, use
				mountPointWriteable = dir.canWrite();
				mountPoint = dir;
				if (mountPoint != null && mountPointWriteable) {
					break;
				}
			} else if (dir.canWrite()) {
				mountPointWriteable = true;
				mountPoint = dir;
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					try {
						if (Environment.isExternalStorageRemovable(dir)) {
							// prefer removeable storage
							Log.d(DEBUG_TAG, "isExternalStorageRemovable claims dir is removeable");
							break;
						}
					} catch (IllegalArgumentException iae) {
						// we've seen this on some devices even if it doesn0t make sense
						Log.d(DEBUG_TAG, "isExternalStorageRemovable didn't like that");
					}
				} 
			} else {
				Log.d(DEBUG_TAG,  dir.getPath() + " not writeable");
			}
		}

		if (mountPoint != null && mountPointWriteable) {
			Log.d(DEBUG_TAG,
					"Setting cache size to " + tileCacheSize + " on " + mountPoint.getPath());
			try {
				mFileSystemProvider = new MapTileFilesystemProvider(getBaseContext(), mountPoint,
						tileCacheSize * 1024 * 1024); // FSCache
				return;
			} catch (SQLiteException slex) {
				Log.d(DEBUG_TAG, "Opening DB hit " + slex);
				ACRA.getErrorReporter().putCustomData("STATUS", "NOCRASH");
				ACRA.getErrorReporter().handleException(slex);
			}
		} else {
			Toast.makeText(this, R.string.toast_no_suitable_storage, Toast.LENGTH_LONG).show();
			return;
		}
		Toast.makeText(this, getString(R.string.toast_storage_error, mountPoint), Toast.LENGTH_LONG).show();
		// FIXME potentially we should set both background and overlay
		// preferences to NONE here or simply zap what we are currently are
		// using.
		// don't terminate, simply ignore requests
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
	private final IMapTileProviderService.Stub mBinder = new IMapTileProviderService.Stub() {
		// @Override
		public String[] getTileProviders() throws RemoteException {
			return TileLayerServer.getIds(null,false);
		}

		// @Override
		public void getMapTile(String rendererID, int zoomLevel, int tileX, int tileY,
				IMapTileProviderCallback callback) throws RemoteException {
			if (!mountPointWriteable) { // fail silently
				return;
			}
			MapTile tile = new MapTile(rendererID, zoomLevel, tileX, tileY);
			mFileSystemProvider.loadMapTileAsync(tile, callback);
		}

		public void flushCache(String rendererId) {
			mFileSystemProvider.flushCache(rendererId);
		}
	};
}
