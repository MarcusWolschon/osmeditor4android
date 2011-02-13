// Created by plusminus on 21:46:22 - 25.09.2008
package  de.blau.android.views.util;

import de.blau.android.R;
import de.blau.android.services.IOpenStreetMapTileProviderCallback;
import de.blau.android.services.IOpenStreetMapTileProviderService;
import de.blau.android.services.util.OpenStreetMapTile;
import de.blau.android.views.util.OpenStreetMapViewConstants;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;

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
		Log.d("Service", "connected");
	};
	
	//@Override
	public void onServiceDisconnected(ComponentName name) {
		mTileService = null;
		Log.d("Service", "disconnected");
	}
	
	// ===========================================================
	// Methods
	// ===========================================================

	public void clear() {
		mTileCache.clear();
		mCtx.unbindService(this);
	}
	
	public boolean isTileAvailable(final OpenStreetMapTile aTile) {
		return mTileCache.containsTile(aTile);
	}

	public Bitmap getMapTile(final OpenStreetMapTile aTile) {
		if (mTileCache.containsTile(aTile)) {							// from cache
			if (DEBUGMODE)
				Log.i(DEBUGTAG, "MapTileCache succeded for: " + aTile.toString());
			return mTileCache.getMapTile(aTile);
			
		} else {																	// from service
			if (DEBUGMODE)
				Log.i(DEBUGTAG, "Cache failed, trying from FS.");
			try {
				mTileService.getMapTile(aTile.rendererID, aTile.zoomLevel, aTile.x, aTile.y, this.mServiceCallback);
			} catch (RemoteException e) {
				Log.e("OpenStreetMapTileProvider", "RemoteException in getMapTile()", e);
			} catch (Exception e) {
				Log.e("OpenStreetMapTileProvider", "Exception in getMapTile()", e);
			}
		}
		return null;
	}

	public void preCacheTile(final OpenStreetMapTile aTile) {
		if (!mTileCache.containsTile(aTile) && mTileService != null) {
			try {
				mTileService.getMapTile(aTile.rendererID, aTile.zoomLevel, aTile.x, aTile.y, this.mServiceCallback);
			} catch (RemoteException e) {
				Log.e("OpenStreetMapTileProvider", "RemoteException in preCacheTile()", e);
			} catch (Exception e) {
				Log.e("OpenStreetMapTileProvider", "Exception in preCacheTile()", e);
			}
		}
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
	
	/**
	 * Callback for the {@link IOpenStreetMapTileProviderService} we are using.
	 */
	private IOpenStreetMapTileProviderCallback mServiceCallback = new IOpenStreetMapTileProviderCallback.Stub() {
		
		//@Override
		public void mapTileLoaded(final int rendererID, final int zoomLevel, final int tileX, final int tileY, final Bitmap aTile) throws RemoteException {
			mTileCache.putTile(new OpenStreetMapTile(rendererID, zoomLevel, tileX, tileY), aTile);
			mDownloadFinishedHandler
					.sendEmptyMessage(OpenStreetMapTile.MAPTILE_SUCCESS_ID);
			if (DEBUGMODE)
				Log.i(DEBUGTAG, "MapTile download success.");
		}
		
		//@Override
		public void mapTileFailed(final int rendererID, final int zoomLevel, final int tileX, final int tileY) throws RemoteException {
			if (DEBUGMODE) {
				Log.e(DEBUGTAG, "MapTile download error.");
			}
			mTileService.getMapTile(rendererID, zoomLevel, tileX, tileY, this);
		}
	};

}
