// Created by plusminus on 21:46:41 - 25.09.2008
package de.blau.android.services.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Executors;


import de.blau.android.services.exceptions.EmptyCacheException;
import de.blau.android.services.IOpenStreetMapTileProviderCallback;
import de.blau.android.views.util.OpenStreetMapTileServer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.RemoteException;
import android.util.Log;

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
	protected final int mMaxFSCacheByteSize;
	protected int mCurrentFSCacheByteSize;

	/** online provider */
	protected OpenStreetMapTileDownloader mTileDownloader;

	// ===========================================================
	// Constructors
	// ===========================================================

	/**
	 * @param ctx
	 * @param aMaxFSCacheByteSize the size of the cached MapTiles will not exceed this size.
	 * @param aCache to load fs-tiles to.
	 */
	public OpenStreetMapTileFilesystemProvider(final Context ctx, final int aMaxFSCacheByteSize) {
		this.mCtx = ctx;
		this.mMaxFSCacheByteSize = aMaxFSCacheByteSize;
		this.mDatabase = new OpenStreetMapTileProviderDataBase(ctx);
		this.mCurrentFSCacheByteSize = this.mDatabase.getCurrentFSCacheByteSize();
		this.mThreadPool = Executors.newFixedThreadPool(2);

		this.mTileDownloader = new OpenStreetMapTileDownloader(ctx, this);

//		File f = new File("/sdcard/andnav2");
//		f.mkdirs();
		
		if(Log.isLoggable(DEBUGTAG, Log.INFO))
			Log.i(DEBUGTAG, "Currently used cache-size is: " + this.mCurrentFSCacheByteSize + " of " + this.mMaxFSCacheByteSize + " Bytes");
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================
	
	public int getCurrentFSCacheByteSize() {
		return this.mCurrentFSCacheByteSize;
	}

	public void saveFile(final OpenStreetMapTile tile, final byte[] someData) throws IOException{
		final OutputStream bos = getOutput(tile);
		bos.write(someData);
		bos.flush();
		bos.close();

		synchronized (this) {
			final int bytesGrown = this.mDatabase.addTileOrIncrement(tile, someData.length);
			this.mCurrentFSCacheByteSize += bytesGrown;

			if(Log.isLoggable(DEBUGTAG, Log.DEBUG))
				Log.i(DEBUGTAG, "FSCache Size is now: " + this.mCurrentFSCacheByteSize + " Bytes");

			/* If Cache is full... */
			try {

				if(this.mCurrentFSCacheByteSize > this.mMaxFSCacheByteSize){
					if(Log.isLoggable(DEBUGTAG, Log.DEBUG))
						Log.d(DEBUGTAG, "Freeing FS cache...");
					this.mCurrentFSCacheByteSize -= this.mDatabase.deleteOldest((int)(this.mMaxFSCacheByteSize * 0.05f)); // Free 5% of cache
				}
			} catch (EmptyCacheException e) {
				if(Log.isLoggable(DEBUGTAG, Log.DEBUG))
					Log.e(DEBUGTAG, "Cache empty", e);
			}
		}
	}

	public void clearCurrentFSCache(){
		cutCurrentFSCacheBy(Integer.MAX_VALUE); // Delete all
	}
	
	public void cutCurrentFSCacheBy(final int bytesToCut){
		try {
			this.mDatabase.deleteOldest(Integer.MAX_VALUE); // Delete all
			this.mCurrentFSCacheByteSize = 0;
		} catch (EmptyCacheException e) {
			if(Log.isLoggable(DEBUGTAG, Log.DEBUG))
				Log.e(DEBUGTAG, "Cache empty", e);
		}
	}

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	protected Runnable getTileLoader(OpenStreetMapTile aTile, IOpenStreetMapTileProviderCallback aCallback) {
		return new TileLoader(aTile, aCallback);
	};
	
	// ===========================================================
	// Methods
	// ===========================================================

	private String buildPath(final OpenStreetMapTile tile) {
		OpenStreetMapTileServer renderer = OpenStreetMapTileServer.get(mCtx.getResources(), tile.rendererID);
		return Environment.getExternalStorageDirectory().getPath()
					+ "/andnav2/tiles/" + renderer.ID + "/" + tile.zoomLevel + "/"
					+ tile.x + "/" + tile.y + renderer.IMAGE_FILENAMEENDING + ".andnav"; 
	}
	
	private InputStream getInput(final OpenStreetMapTile tile) throws FileNotFoundException {
		return new BufferedInputStream(new FileInputStream(buildPath(tile)), StreamUtils.IO_BUFFER_SIZE);
//		return new BufferedInputStream(this.mCtx.openFileInput(path), StreamUtils.IO_BUFFER_SIZE);
	}
	
	private OutputStream getOutput(final OpenStreetMapTile tile) throws IOException {
		File file = new File(buildPath(tile));
		if (!file.exists()) {
			synchronized (this) {
				// Multiple threads creating directories simultaneously
				// can result in this call failing, the directories not
				// being created, and the subsequent FileOutputStream
				// issuing an IOException.
				file.getParentFile().mkdirs();
			}
		}
		return new BufferedOutputStream(new FileOutputStream(file, false), StreamUtils.IO_BUFFER_SIZE);		
	}
	
	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	private class TileLoader extends OpenStreetMapAsyncTileProvider.TileLoader {

		public TileLoader(final OpenStreetMapTile aTile, final IOpenStreetMapTileProviderCallback aCallback) {
			super(aTile, aCallback);
		}

		//@Override
		public void run() {
			OpenStreetMapTileFilesystemProvider.this.mDatabase.incrementUse(mTile);
			InputStream in = null;
			try {
				in = getInput(mTile);
				final Bitmap bmp = BitmapFactory.decodeStream(in);
				if (bmp != null) {
					mCallback.mapTileLoaded(mTile.rendererID, mTile.zoomLevel, mTile.x, mTile.y, bmp);

					if (Log.isLoggable(DEBUGTAG, Log.DEBUG))
						Log.d(DEBUGTAG, "Loaded: " + mTile.toString());
				} else {
					mCallback.mapTileFailed(mTile.rendererID, mTile.zoomLevel, mTile.x, mTile.y);
					if (Log.isLoggable(DEBUGTAG, Log.DEBUG))	// only log in debug mode, though it's an error message
						Log.e(DEBUGTAG, "Error Loading MapTile from FS.");
				}
			} catch (FileNotFoundException e) {
				if (Log.isLoggable(DEBUGTAG, Log.DEBUG))
					Log.i(DEBUGTAG, "FS failed, request for download.");
				mTileDownloader.loadMapTileAsync(mTile, mCallback);
			} catch (RemoteException e) {
				if (Log.isLoggable(DEBUGTAG, Log.DEBUG))
					Log.e(DEBUGTAG, "Service failed", e);
			} finally {
				StreamUtils.closeStream(in);
				finished();
			}
		}
	};
	
}
