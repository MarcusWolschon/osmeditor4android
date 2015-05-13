// Created by plusminus on 21:46:22 - 25.09.2008
package  de.blau.android.views.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import de.blau.android.R;
import de.blau.android.exception.StorageException;
import de.blau.android.services.IOpenStreetMapTileProviderCallback;
import de.blau.android.services.IOpenStreetMapTileProviderService;
import de.blau.android.services.util.OpenStreetMapAsyncTileProvider;
import de.blau.android.services.util.OpenStreetMapTile;

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
public class OpenStreetMapTileProvider implements ServiceConnection,
		OpenStreetMapViewConstants {
	// ===========================================================
	// Constants
	// ===========================================================

	/**
	 * Tag used in debug log-entries.
	 */
	public static final String DEBUGTAG = "OpenStreetMapTileProvider";

	// ===========================================================
	// Fields
	// ===========================================================

	/**
	 * place holder if tile not available
	 */
	protected final Bitmap mLoadingMapTile;
	protected final Bitmap mNoTilesTile;

	protected Context mCtx;
	/**
	 * cache provider
	 */
	protected OpenStreetMapTileCache mTileCache;
	private HashMap<String,Long> pending = new HashMap<String,Long>();

	private IOpenStreetMapTileProviderService mTileService;
	private Handler mDownloadFinishedHandler;
	
	/**
	 * Set to true if we have less than 64 MB heep or have other caching issues
	 */
	private boolean smallHeap = false;
	
	// ===========================================================
	// Constructors
	// ===========================================================

	public OpenStreetMapTileProvider(final Context ctx,
			final Handler aDownloadFinishedListener) {
		mCtx = ctx;
		mNoTilesTile = BitmapFactory.decodeResource(ctx.getResources(),
				R.drawable.no_tiles);
		mLoadingMapTile = BitmapFactory.decodeResource(ctx.getResources(),
				R.drawable.no_tiles);
		mTileCache = new OpenStreetMapTileCache();
		
		smallHeap = Runtime.getRuntime().maxMemory() <= 32L*1024L*1024L; // less than 32MB
	
		if(!ctx.bindService(new Intent(IOpenStreetMapTileProviderService.class.getName()), this, Context.BIND_AUTO_CREATE)) {
			Log.e(DEBUGTAG, "Could not bind to " + IOpenStreetMapTileProviderService.class.getName());
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
		mTileService = IOpenStreetMapTileProviderService.Stub.asInterface(service);
		mDownloadFinishedHandler.sendEmptyMessage(OpenStreetMapTile.MAPTILE_SUCCESS_ID);
		Log.d("MapTileProviderService", "connected");
	};
	
	//@Override
	@Override
	public void onServiceDisconnected(ComponentName name) {
		mTileService = null;
		Log.d("MapTileProviderService", "disconnected");
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
	public boolean isTileAvailable(final OpenStreetMapTile aTile) {
		return mTileCache.containsTile(aTile);
	}

	public Bitmap getMapTile(final OpenStreetMapTile aTile, long owner) {
		Bitmap tile = mTileCache.getMapTile(aTile); 
		if (tile != null) {
			// from cache
			//if (DEBUGMODE)
			//	Log.i(DEBUGTAG, "MapTileCache succeeded for: " + aTile.toString());
			return tile;
		} else {
			// from service
			if (DEBUGMODE)
				Log.i(DEBUGTAG, "MapTileCache failed for: " + aTile.toString());
			preCacheTile(aTile, owner);
		}
		return null;
	}

	public void preCacheTile(final OpenStreetMapTile aTile, long owner) {
		if (!isTileAvailable(aTile) && mTileService != null && !pending.containsKey(aTile.toString())) {
			try {
				pending.put(aTile.toString(), Long.valueOf(owner));
				mTileService.getMapTile(aTile.rendererID, aTile.zoomLevel, aTile.x, aTile.y, mServiceCallback);
			} catch (RemoteException e) {
				Log.e("OpenStreetMapTileProvider", "RemoteException in preCacheTile()", e);
			} catch (Exception e) {
				Log.e("OpenStreetMapTileProvider", "Exception in preCacheTile()", e);
			}
		}
	}
	
	public void flushCache(String rendererId) { 
		try {
			mTileService.flushCache(rendererId);
		} catch (RemoteException e) {
			Log.e("OpenStreetMapTileProvider", "RemoteException in flushCache()", e);
		} catch (Exception e) {
			Log.e("OpenStreetMapTileProvider", "Exception in flushCache()", e);
		}
		mTileCache.clear(); // zap everything in in memory cache
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
	
	/**
	 * Callback for the {@link IOpenStreetMapTileProviderService} we are using.
	 */
	private IOpenStreetMapTileProviderCallback mServiceCallback = new IOpenStreetMapTileProviderCallback.Stub() {
		
		//@Override
		public void mapTileLoaded(final String rendererID, final int zoomLevel, final int tileX, final int tileY, final byte[] data) throws RemoteException {
			BitmapFactory.Options options = new BitmapFactory.Options();
			if (smallHeap) {
				 options.inPreferredConfig =  Bitmap.Config.RGB_565;
			} else {
				options.inPreferredConfig = Bitmap.Config.ARGB_8888; // Bitmap.Config.RGB_565;
			}
	        
			OpenStreetMapTile t = new OpenStreetMapTile(rendererID, zoomLevel, tileX, tileY);
			
			try {
				//long start = System.currentTimeMillis();
				Bitmap aTile = BitmapFactory.decodeByteArray(data, 0, data.length, options);
				// long duration = System.currentTimeMillis() - start;
				if (aTile == null) {
					Log.d("OpenStreetMapTileProvider", "decoded tile is null");
					throw new RemoteException();
				}
				// Log.d("OpenStreetMapTileProvider", "raw data size " + data.length + " decoded bitmap size " + aTile.getRowBytes()*aTile.getHeight());

				mTileCache.putTile(t, aTile,pending.get(t.toString()).longValue());
				pending.remove(t.toString());
				mDownloadFinishedHandler.sendEmptyMessage(OpenStreetMapTile.MAPTILE_SUCCESS_ID);
				// Log.d("OpenStreetMapTileProvider", "Sending tile success message");
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
				Log.d("OpenStreetMapTileProvider", "Exception in mapTileLoaded callback " + npe);
				throw new RemoteException();
			}
			if (DEBUGMODE)
				Log.i(DEBUGTAG, "MapTile download success."+t.toString());
		}
		
		//@Override
		public void mapTileFailed(final String rendererID, final int zoomLevel, final int tileX, final int tileY, final int reason) throws RemoteException {
			OpenStreetMapTile t = new OpenStreetMapTile(rendererID, zoomLevel, tileX, tileY);
			if (reason == OpenStreetMapAsyncTileProvider.DOESNOTEXIST) {// only show error tile if we have no chance of getting the proper one
				OpenStreetMapTileServer osmts = OpenStreetMapTileServer.get(mCtx, rendererID, false);
				//TODO check if we are inside the providers bounding box
				if (zoomLevel < Math.max(0,osmts.getMinZoomLevel()-1)) {
					try {
						mTileCache.putTile(t, mNoTilesTile, false,0);
					} catch (StorageException e) {
						// TODO Auto-generated catch block
						// e.printStackTrace();
					}
				}
			}
			pending.remove(t.toString());
			//if (DEBUGMODE) {
				Log.e(DEBUGTAG, "MapTile download error " + t.toString());
			//}
			// don't send when we fail mDownloadFinishedHandler.sendEmptyMessage(OpenStreetMapTile.MAPTILE_SUCCESS_ID);
		}
	};

	public String getCacheUsageInfo() {
		return mTileCache.getCacheUsageInfo();
	}
}
