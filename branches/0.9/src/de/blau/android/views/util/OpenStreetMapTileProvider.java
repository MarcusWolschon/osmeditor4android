// Created by plusminus on 21:46:22 - 25.09.2008
package  de.blau.android.views.util;

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
import de.blau.android.services.IOpenStreetMapTileProviderCallback;
import de.blau.android.services.IOpenStreetMapTileProviderService;
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

	protected Context mCtx;
	/**
	 * cache provider
	 */
	protected OpenStreetMapTileCache mTileCache;
	private Set<String> pending = new HashSet<String>();

	private IOpenStreetMapTileProviderService mTileService;
	private Handler mDownloadFinishedHandler;

	// ===========================================================
	// Constructors
	// ===========================================================

	public OpenStreetMapTileProvider(final Context ctx,
			final Handler aDownloadFinishedListener) {
		mCtx = ctx;
		mLoadingMapTile = BitmapFactory.decodeResource(ctx.getResources(),
				R.drawable.maptile_loading);
		mTileCache = new OpenStreetMapTileCache();
		
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

	public void onServiceConnected(android.content.ComponentName name, android.os.IBinder service) {
		mTileService = IOpenStreetMapTileProviderService.Stub.asInterface(service);
		mDownloadFinishedHandler.sendEmptyMessage(OpenStreetMapTile.MAPTILE_SUCCESS_ID);
		Log.d("MapTileProviderService", "connected");
	};
	
	//@Override
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

	public Bitmap getMapTile(final OpenStreetMapTile aTile) {
		Bitmap tile = mTileCache.getMapTile(aTile); 
		if (tile != null) {
			// from cache
			//if (DEBUGMODE)
			//	Log.i(DEBUGTAG, "MapTileCache succeded for: " + aTile.toString());
			return tile;
		} else {
			// from service
			if (DEBUGMODE)
				Log.i(DEBUGTAG, "MapTileCache failed for: " + aTile.toString());
			preCacheTile(aTile);
		}
		return null;
	}

	public void preCacheTile(final OpenStreetMapTile aTile) {
		if (!isTileAvailable(aTile) && mTileService != null && !pending.contains(aTile.toString())) {
			try {
				pending.add(aTile.toString());
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
	        options.inPreferredConfig =  Bitmap.Config.RGB_565;
	        
			OpenStreetMapTile t = new OpenStreetMapTile(rendererID, zoomLevel, tileX, tileY);
			long start = System.currentTimeMillis();
			Bitmap aTile = BitmapFactory.decodeByteArray(data, 0, data.length, options);
			long duration = System.currentTimeMillis() - start;
			if (aTile == null) {
				throw new RemoteException("decodeByteArray returned null");
			}
			// Log.d("OpenStreetMapTileProvider", "raw data size " + data.length + " decoded bitmap size " + aTile.getRowBytes()*aTile.getHeight() + " time to decode " + duration);
			mTileCache.putTile(t, aTile);
			pending.remove(t.toString());
			mDownloadFinishedHandler.sendEmptyMessage(OpenStreetMapTile.MAPTILE_SUCCESS_ID);
			if (DEBUGMODE)
				Log.i(DEBUGTAG, "MapTile download success."+t.toString());
		}
		
		//@Override
		public void mapTileFailed(final String rendererID, final int zoomLevel, final int tileX, final int tileY) throws RemoteException {
			OpenStreetMapTile t = new OpenStreetMapTile(rendererID, zoomLevel, tileX, tileY);
			pending.remove(t.toString());
			if (DEBUGMODE) {
				Log.e(DEBUGTAG, "MapTile download error.");
			}
			//mTileService.getMapTile(rendererID, zoomLevel, tileX, tileY, this);
		}
	};

	public String getCacheUsageInfo() {
		return mTileCache.getCacheUsageInfo();
	}
	
}
