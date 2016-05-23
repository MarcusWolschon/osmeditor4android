// Created by plusminus on 21:46:41 - 25.09.2008
package de.blau.android.services.util;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.services.IOpenStreetMapTileProviderCallback;
import de.blau.android.services.exceptions.EmptyCacheException;

/**
 * 
 * <br/>
 * This class was taken from OpenStreetMapViewer (original package org.andnav.osm) in 2010-06
 * by Marcus Wolschon to be integrated into the de.blau.androin
 * OSMEditor. 
 * @author Nicolas Gramlich
 * @author Marcus Wolschon <Marcus@Wolschon.biz>
 *
 */
public class OpenStreetMapTileFilesystemProvider extends OpenStreetMapAsyncTileProvider {
	// ===========================================================
	// Constants
	// ===========================================================

	final static String DEBUGTAG = "OSM_FS_PROVIDER";

	// ===========================================================
	// Fields
	// ===========================================================

	protected final Context mCtx;
	protected final OpenStreetMapTileProviderDataBase mDatabase;
	protected final File mountPoint;
	protected final int mMaxFSCacheByteSize;
	protected int mCurrentFSCacheByteSize;

	/** online provider */
	protected OpenStreetMapTileDownloader mTileDownloader;

	// ===========================================================
	// Constructors
	// ===========================================================

	/**
	 * @param ctx
	 * @param mountPoint TODO
	 * @param aMaxFSCacheByteSize the size of the cached MapTiles will not exceed this size.
	 * @param aCache to load fs-tiles to.
	 */
	public OpenStreetMapTileFilesystemProvider(final Context ctx, File mountPoint, final int aMaxFSCacheByteSize) {
		mCtx = ctx;
		this.mountPoint = mountPoint;
		mMaxFSCacheByteSize = aMaxFSCacheByteSize;
		mDatabase = new OpenStreetMapTileProviderDataBase(ctx, this);
		mCurrentFSCacheByteSize = mDatabase.getCurrentFSCacheByteSize();
		mThreadPool = Executors.newFixedThreadPool(2);

		mTileDownloader = new OpenStreetMapTileDownloader(ctx, this);

		if(Log.isLoggable(DEBUGTAG, Log.INFO))
			Log.i(DEBUGTAG, "Currently used cache-size is: " + mCurrentFSCacheByteSize + " of " + mMaxFSCacheByteSize + " Bytes");
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================
	
	public int getCurrentFSCacheByteSize() {
		return mCurrentFSCacheByteSize;
	}


	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	@Override
	protected Runnable getTileLoader(OpenStreetMapTile aTile, IOpenStreetMapTileProviderCallback aCallback) {
		return new TileLoader(aTile, aCallback);
	}
	
	// ===========================================================
	// Methods
	// ===========================================================

	public void saveFile(final OpenStreetMapTile tile, final byte[] someData) throws IOException{
		synchronized (this) {
			try {
				final int bytesGrown = mDatabase.addTileOrIncrement(tile, someData);
				mCurrentFSCacheByteSize += bytesGrown;

				if (Log.isLoggable(DEBUGTAG, Log.DEBUG))
					Log.i(DEBUGTAG, "FSCache Size is now: " + mCurrentFSCacheByteSize + " Bytes");

				/* If Cache is full... */
				try {

					if (mCurrentFSCacheByteSize > mMaxFSCacheByteSize){
						if (Log.isLoggable(DEBUGTAG, Log.DEBUG))
							Log.d(DEBUGTAG, "Freeing FS cache...");
						mCurrentFSCacheByteSize -= mDatabase.deleteOldest((int)(mMaxFSCacheByteSize * 0.05f)); // Free 5% of cache
					}
				} catch (EmptyCacheException e) {
					if(Log.isLoggable(DEBUGTAG, Log.DEBUG))
						Log.e(DEBUGTAG, "Cache empty", e);
				}
			} catch (IllegalStateException e) {
				if (Log.isLoggable(DEBUGTAG, Log.DEBUG)) {
					Log.e(DEBUGTAG, "Tile saving failed", e);
				}
			}
		}
	}

	public void clearCurrentFSCache(){
		cutCurrentFSCacheBy(Integer.MAX_VALUE); // Delete all
	}
	
	public void cutCurrentFSCacheBy(final int bytesToCut){
		try {
			synchronized (this) {
				mDatabase.deleteOldest(Integer.MAX_VALUE); // Delete all
			}
			mCurrentFSCacheByteSize = 0;
		} catch (EmptyCacheException e) {
			if (Log.isLoggable(DEBUGTAG, Log.DEBUG))
				Log.e(DEBUGTAG, "Cache empty", e);
		}
	}

	/**
	 * delete tiles for specific provider
	 * @param rendererID
	 */
	public void flushCache(String rendererID) {
		try {
			mDatabase.flushCache(rendererID);
		} catch (EmptyCacheException e) {
			if (Log.isLoggable(DEBUGTAG, Log.DEBUG)) {
				Log.e(DEBUGTAG, "Flushing tile cache failed", e);
			}
		}
	}
	
	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	private class TileLoader extends OpenStreetMapAsyncTileProvider.TileLoader {

		public TileLoader(final OpenStreetMapTile aTile, final IOpenStreetMapTileProviderCallback aCallback) {
			super(aTile, aCallback);
		}

		@Override
		public void run() {
			try {
				TileLayerServer renderer = TileLayerServer.get(mCtx, mTile.rendererID, false);
				if (mTile.zoomLevel < renderer.getMinZoomLevel()) { // the tile doesn't exist no point in trying to get it
					mCallback.mapTileFailed(mTile.rendererID, mTile.zoomLevel, mTile.x, mTile.y, DOESNOTEXIST);
					return;
				}
				synchronized (OpenStreetMapTileFilesystemProvider.this) {
					byte[] data = OpenStreetMapTileFilesystemProvider.this.mDatabase.getTile(mTile);
					if (data == null) {
						if (Log.isLoggable(DEBUGTAG, Log.DEBUG))
							Log.i(DEBUGTAG, "FS failed, request for download.");
						mTileDownloader.loadMapTileAsync(mTile, mCallback);
					} else { // success!
						mCallback.mapTileLoaded(mTile.rendererID, mTile.zoomLevel, mTile.x, mTile.y, data);
					}
				}
				if (Log.isLoggable(DEBUGTAG, Log.DEBUG))
					Log.d(DEBUGTAG, "Loaded: " + mTile.toString());	
			} catch (IOException e) { 
				if (Log.isLoggable(DEBUGTAG, Log.DEBUG)) {
					Log.d(DEBUGTAG, "Invalid tile: " + mTile.toString());	
				}
			} catch (RemoteException e) {
				if (Log.isLoggable(DEBUGTAG, Log.DEBUG)) {
					Log.e(DEBUGTAG, "Service failed", e);
				}
			} catch (NullPointerException e) {
				if (Log.isLoggable(DEBUGTAG, Log.DEBUG)) {
					Log.e(DEBUGTAG, "Service failed", e);
				}
			} catch (IllegalStateException e) {
				if (Log.isLoggable(DEBUGTAG, Log.DEBUG)) {
					Log.e(DEBUGTAG, "Tile loading failed", e);
				}
			} finally {
				finished();
			}
		}
	}

	/**
	 * Call when the object is no longer needed to close the database
	 */
	public void destroy() {
		Log.d(DEBUGTAG, "Closing tile database");
		mDatabase.close();
	}

	public void markAsInvalid(OpenStreetMapTile mTile) {
		mDatabase.addTileOrIncrement(mTile, null);	
	}
}