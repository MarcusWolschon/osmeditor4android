package de.blau.android.services;

import java.io.File;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import android.util.Log;
import de.blau.android.R;
import de.blau.android.contract.Paths;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerDatabase;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.services.util.MapTile;
import de.blau.android.services.util.MapTileFilesystemProvider;
import de.blau.android.services.util.MapTileProviderDataBase;
import de.blau.android.util.Snack;

/**
 * The OpenStreetMapTileProviderService can download map tiles from a server and stores them in a file system cache.
 * <br/>
 * This class was taken from OpenStreetMapViewer (original package org.andnav.osm) in 2010-06 by Marcus Wolschon to be
 * integrated into the de.blau.androin OSMEditor.
 * 
 * @author Marcus Wolschon &lt;Marcus@Wolschon.biz&gt;
 * @author Manuel Stahl
 * @author Simon Poole
 */
public class MapTileProviderService extends Service {

    private static final String       DEBUG_TAG           = MapTileProviderService.class.getSimpleName();
    private MapTileFilesystemProvider mFileSystemProvider;
    private boolean                   mountPointWriteable = false;

    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    /**
     * Tries to put the tile cache on a removable sd card if present and we haven't already created the cache
     */
    @SuppressLint("NewApi")
    private void init() {
        Preferences prefs = new Preferences(this);
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
            if (dir == null) {
                Log.d(DEBUG_TAG, "storage dir null");
                continue;
            }
            Log.d(DEBUG_TAG, "candidate storage directory " + dir.getPath());
            if (MapTileProviderDataBase.exists(dir)) { // existing tile cache, only use if we can write
                if (dir.canWrite()) {
                    mountPointWriteable = true;
                    mountPoint = dir;
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
                        Log.d(DEBUG_TAG, "isExternalStorageRemovable didn't like " + dir);
                    }
                } else {
                    break; // as we can't determine if this is external we may as well use it
                }
            } else {
                Log.d(DEBUG_TAG, dir.getPath() + " not writeable");
            }
        }

        if (mountPoint != null && mountPointWriteable) {
            Log.d(DEBUG_TAG, "Setting cache size to " + tileCacheSize + " on " + mountPoint.getPath());
            try {
                mFileSystemProvider = new MapTileFilesystemProvider(this, mountPoint, tileCacheSize * 1024 * 1024); // FSCache
                // try to get BING layer early so the meta-data is already loaded
                TileLayerServer.get(this, TileLayerServer.LAYER_BING, false);
                return;
            } catch (SQLiteException slex) {
                Log.d(DEBUG_TAG, "Opening DB hit " + slex);
            }
        } else {
            Snack.toastTopError(this, R.string.toast_no_suitable_storage);
            return;
        }
        Snack.toastTopError(this, getString(R.string.toast_storage_error, mountPoint));
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
        /**
         * Get a tile
         * 
         * @param renderId the tile rendered
         * @param zoomLevel the zoom level
         * @param tile X
         * @param tile Y
         * @param callback callback to the TileProvider
         * @throws RemoteException if something goes wrong with the service
         */
        public void getMapTile(@NonNull String rendererID, int zoomLevel, int tileX, int tileY, @NonNull IMapTileProviderCallback callback)
                throws RemoteException {
            if (!mountPointWriteable) { // fail silently
                return;
            }
            MapTile tile = new MapTile(rendererID, zoomLevel, tileX, tileY);
            mFileSystemProvider.loadMapTileAsync(tile, callback);
        }

        /**
         * Flush the on device cache
         * 
         * @param rendererId the tile renderer, if null all caches will be flushed
         */
        public void flushCache(@Nullable String rendererId) {
            mFileSystemProvider.flushCache(rendererId);
        }

        /**
         * Flush the queue of pending tile requests
         * 
         * @param rendererId the tile renderer
         * @param zoomLevel the zoom level if -1 then requests will be flushed for all zoom levels
         */
        public void flushQueue(@NonNull String rendererId, int zoomLevel) {
            mFileSystemProvider.flushQueue(rendererId, zoomLevel);
        }

        /**
         * Update the configuration
         */
        public void update() {
            try (TileLayerDatabase tlDb = new TileLayerDatabase(MapTileProviderService.this); SQLiteDatabase db = tlDb.getReadableDatabase()) {
                TileLayerServer.getListsLocked(MapTileProviderService.this, db, false);
            }
        }
    };
}
