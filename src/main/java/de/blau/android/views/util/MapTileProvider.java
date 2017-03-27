// Created by plusminus on 21:46:22 - 25.09.2008
package  de.blau.android.views.util;

import java.util.HashMap;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import de.blau.android.R;
import de.blau.android.exception.StorageException;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.services.IMapTileProviderCallback;
import de.blau.android.services.IMapTileProviderService;
import de.blau.android.services.util.MapAsyncTileProvider;
import de.blau.android.services.util.MapTile;

/**
 * 
 * <br/>
 * This class was taken from OpenStreetMapViewer (original package org.andnav.osm) in 2010
 * by Marcus Wolschon to be integrated into the de.blau.androin
 * OSMEditor. 
 * @author Nicolas Gramlich
 * @author Marcus Wolschon <Marcus@Wolschon.biz>
 * 
 */
public class MapTileProvider implements ServiceConnection,
		MapViewConstants {
	// ===========================================================
	// Constants
	// ===========================================================

	/**
	 * Tag used in debug log-entries.
	 */
	private static final String DEBUG_TAG = MapTileProvider.class.getSimpleName();

	// ===========================================================
	// Fields
	// ===========================================================

	/**
	 * place holder if tile not available
	 */
	Object staticTilesLock = new Object();
	private static Bitmap mLoadingMapTile;
	private static Bitmap mNoTilesTile;

	private Context mCtx;
	/**
	 * cache provider
	 */
	private MapTileCache mTileCache;
	private HashMap<String,Long> pending = new HashMap<String,Long>();

	private IMapTileProviderService mTileService;
	private Handler mDownloadFinishedHandler;
	
	/**
	 * Set to true if we have less than 64 MB heap or have other caching issues
	 */
	private boolean smallHeap = false;
	
	// ===========================================================
	// Constructors
	// ===========================================================

	public MapTileProvider(final Context ctx,
			final Handler aDownloadFinishedListener) {
		mCtx = ctx;
		Resources r = ctx.getResources();
		synchronized(staticTilesLock) {
			if (mNoTilesTile == null) {
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inPreferredConfig =  Bitmap.Config.RGB_565;
				mNoTilesTile = BitmapFactory.decodeResource(r,R.drawable.no_tiles, options);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
					Log.d(DEBUG_TAG,"Notiles tile uses " + mNoTilesTile.getByteCount());
				}
				// mLoadingMapTile = BitmapFactory.decodeResource(r,R.drawable.no_tiles);
			}
		}
		mTileCache = new MapTileCache();
		
		smallHeap = Runtime.getRuntime().maxMemory() <= 32L*1024L*1024L; // less than 32MB
	
		Intent explicitIntent = (new Intent(IMapTileProviderService.class.getName())).setPackage(ctx.getPackageName());
		if(explicitIntent == null || !ctx.bindService(explicitIntent, this, Context.BIND_AUTO_CREATE)) {
			Log.e(DEBUG_TAG, "Could not bind to " + IMapTileProviderService.class.getName() + " in package " + ctx.getPackageName());
		}
		
		mDownloadFinishedHandler = aDownloadFinishedListener;
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	@Override
	public void onServiceConnected(android.content.ComponentName name, android.os.IBinder service) {
		mTileService = IMapTileProviderService.Stub.asInterface(service);
		mDownloadFinishedHandler.sendEmptyMessage(MapTile.MAPTILE_SUCCESS_ID);
		Log.d(DEBUG_TAG, "connected");
	}
	
	//@Override
	@Override
	public void onServiceDisconnected(ComponentName name) {
		mTileService = null;
		Log.d(DEBUG_TAG, "disconnected");
	}
	
	// ===========================================================
	// Methods
	// ===========================================================
	
	/**
	 * Clear out memory related to tracking map tiles.
	 */
	public void clear() {
		pending.clear();
		mTileCache.clear();
		mCtx.unbindService(this);
	}
	
	/**
	 * Try to reduce memory use.
	 */
	public void onLowMemory() {
		mTileCache.onLowMemory();
	}
	
	/**
	 * Determine if the specified tile is available from local storage.
	 * @param aTile The tile to find.
	 * @return true if the tile is in local storage.
	 */
	public boolean isTileAvailable(final MapTile aTile) {
		return mTileCache.containsTile(aTile);
	}

	/**
	 * Attempt to return a tile from cache otherwise ask for it from remote
	 * 
	 * @param aTile tile spec
	 * @param owner
	 * @return the tile or null if it wasn't in cache
	 */
	@Nullable
	public Bitmap getMapTile(@NonNull final MapTile aTile, long owner) {
		Bitmap tile = mTileCache.getMapTile(aTile); 
		if (tile != null) {
			// from cache
			//if (DEBUGMODE)
			//	Log.i(DEBUGTAG, "MapTileCache succeeded for: " + aTile.toString());
			return tile;
		} else {
			// from service
			if (DEBUGMODE) {
				Log.i(DEBUG_TAG, "Memory MapTileCache failed for: " + aTile.toString());
			}
			preCacheTile(aTile, owner);
		}
		return null;
	}
	
	/**
	 * Attempt to return a tile from cache otherwise ask for it from remote
	 * 
	 * @param aTile tile spec
	 * @param owner
	 * @return the tile or null if it wasn't in cache
	 */
	public Bitmap getMapTileFromCache(final MapTile aTile, long owner) {
		return mTileCache.getMapTile(aTile);
	}

	private void preCacheTile(final MapTile aTile, long owner) {
		if (mTileService != null && !pending.containsKey(aTile.toId())) {
			try {
				pending.put(aTile.toId(), Long.valueOf(owner));
				mTileService.getMapTile(aTile.rendererID, aTile.zoomLevel, aTile.x, aTile.y, mServiceCallback);
			} catch (RemoteException e) {
				Log.e(DEBUG_TAG, "RemoteException in preCacheTile()", e);
			} catch (Exception e) {
				Log.e(DEBUG_TAG, "Exception in preCacheTile()", e);
			}
		}
	}
	
	public void flushCache(String rendererId) { 
		try {
			mTileService.flushCache(rendererId);
		} catch (RemoteException e) {
			Log.e(DEBUG_TAG, "RemoteException in flushCache()", e);
		} catch (Exception e) {
			Log.e(DEBUG_TAG, "Exception in flushCache()", e);
		}
		mTileCache.clear(); // zap everything in in memory cache
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
	
	/**
	 * Callback for the {@link IOpenStreetMapTileProviderService} we are using.
	 */
	private IMapTileProviderCallback mServiceCallback = new IMapTileProviderCallback.Stub() {
		
		//@Override
		public void mapTileLoaded(final String rendererID, final int zoomLevel, final int tileX, final int tileY, final byte[] data) throws RemoteException {
			BitmapFactory.Options options = new BitmapFactory.Options();
			if (smallHeap) {
				 options.inPreferredConfig =  Bitmap.Config.RGB_565;
			} else {
				options.inPreferredConfig = Bitmap.Config.ARGB_8888; // Bitmap.Config.RGB_565;
			}
	        
			MapTile t = new MapTile(rendererID, zoomLevel, tileX, tileY);
			
			try {
				//long start = System.currentTimeMillis();
				Bitmap tileBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
				// long duration = System.currentTimeMillis() - start;
				if (tileBitmap == null) {
					Log.d(DEBUG_TAG, "decoded tile is null");
					throw new RemoteException();
				}
				// Log.d(DEBUGTAG, "raw data size " + data.length + " decoded bitmap size " + aTile.getRowBytes()*aTile.getHeight());
				String id = t.toId();
				Long l = pending.get(t.toId());
				if (l != null) {
					mTileCache.putTile(t, tileBitmap,l);
					pending.remove(id);
				} // else wasn't in pending queue just ignore
				mDownloadFinishedHandler.sendEmptyMessage(MapTile.MAPTILE_SUCCESS_ID);
				// Log.d(DEBUGTAG, "Sending tile success message");
			} catch (StorageException e) {
				// unable to cache tile
				if (!smallHeap) { // reduce tile size to half
					smallHeap = true;
					mTileCache.clear();
					// should toast this
				} else {
					// FIXME this should show a toast ... or a special tile
				}
			} catch (NullPointerException npe) {
				Log.d(DEBUG_TAG, "Exception in mapTileLoaded callback " + npe);
				npe.printStackTrace();
				throw new RemoteException();
			}
			if (DEBUGMODE)
				Log.i(DEBUG_TAG, "MapTile download success."+t.toString());
		}
		
		//@Override
		public void mapTileFailed(final String rendererID, final int zoomLevel, final int tileX, final int tileY, final int reason) throws RemoteException {
			MapTile t = new MapTile(rendererID, zoomLevel, tileX, tileY);
			if (reason == MapAsyncTileProvider.DOESNOTEXIST) {// only show error tile if we have no chance of getting the proper one
				TileLayerServer osmts = TileLayerServer.get(mCtx, rendererID, false);
				if (zoomLevel < Math.max(0,osmts.getMinZoomLevel()-1)) {
					try {
						mTileCache.putTile(t, mNoTilesTile, false, 0);
					} catch (StorageException e) {
						// TODO Auto-generated catch block
						// e.printStackTrace();
					}
				}
			}
			pending.remove(t.toString());
			//if (DEBUGMODE) {
			//	Log.e(DEBUGTAG, "MapTile download error " + t.toString());
			//}
			// don't send when we fail mDownloadFinishedHandler.sendEmptyMessage(OpenStreetMapTile.MAPTILE_SUCCESS_ID);
		}
	};

	public String getCacheUsageInfo() {
		return mTileCache.getCacheUsageInfo();
	}
}
