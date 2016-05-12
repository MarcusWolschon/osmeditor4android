// Created by plusminus on 21:46:41 - 25.09.2008
package de.blau.android.services.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Executors;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;
import de.blau.android.contract.Paths;
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
		
		String tileCacheNomedia = mountPoint.getPath() + Paths.DIRECTORY_PATH_TILE_CACHE + ".nomedia"; 
		File file = new File(tileCacheNomedia);
		File parent = file.getParentFile();
		if (!parent.isDirectory()) {
			synchronized (this) {
				file.mkdirs();
			}
		}	
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				Log.e(DEBUGTAG, "Unable to create " + file.getAbsolutePath() + "/" + file.getName());
			}
		}

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
		final OutputStream bos = getOutput(tile);
		bos.write(someData);
		bos.flush();
		bos.close();

		synchronized (this) {
			try {
				final int bytesGrown = mDatabase.addTileOrIncrement(tile, someData.length);
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
	
	public String buildPath(final OpenStreetMapTile tile) {
		TileLayerServer renderer = TileLayerServer.get(mCtx, tile.rendererID, false);
		String ext = renderer.getImageExtension();
		return (ext == null) ? null :
				mountPoint.getPath()
				+ Paths.DIRECTORY_PATH_TILE_CACHE + renderer.getId() + "/" + tile.zoomLevel + "/"
				+ tile.x + "/" + tile.y + ext + ".andnav"; 
	}
	
	private InputStream getInput(final OpenStreetMapTile tile) throws FileNotFoundException {
		String path = buildPath(tile);
		if (path == null) {
			throw new FileNotFoundException("null tile path");
		}
		return new BufferedInputStream(new FileInputStream(path), StreamUtils.IO_BUFFER_SIZE);
	}
	
	private OutputStream getOutput(final OpenStreetMapTile tile) throws IOException {
		String path = buildPath(tile);
		if (path == null) {
			throw new FileNotFoundException("null tile path");
		}
		File file = new File(path);
		File parent = file.getParentFile();
		if (!parent.isDirectory()) {
			synchronized (this) {
				// Multiple threads creating directories simultaneously
				// can result in this call failing, the directories not
				// being created, and the subsequent FileOutputStream
				// issuing an IOException.
				parent.mkdirs();
			}
			if (!parent.isDirectory()) {
				throw new IOException("unable to create directory " + parent.getPath());
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
		@Override
		public void run() {
			DataInputStream dataIs = null;
			try {
				synchronized (OpenStreetMapTileFilesystemProvider.this) {
					if (OpenStreetMapTileFilesystemProvider.this.mDatabase.hasTile(mTile)) {
						OpenStreetMapTileFilesystemProvider.this.mDatabase.incrementUse(mTile);
						if (OpenStreetMapTileFilesystemProvider.this.mDatabase.isInvalid(mTile)) {
							// Log.i(DEBUGTAG, "TileLoader " + mTile.toString() + " is invalid, skipping");
							return; // the finally clause will remove the tile from the pending list
						}
					}
				}
	
				TileLayerServer renderer = TileLayerServer.get(mCtx, mTile.rendererID, false);
				if (mTile.zoomLevel < renderer.getMinZoomLevel()) { // the tile doesn't exist no point in trying to get it
					mCallback.mapTileFailed(mTile.rendererID, mTile.zoomLevel, mTile.x, mTile.y, DOESNOTEXIST);
					return;
				}
				String path = buildPath(mTile);
				if (path == null) {
					throw new FileNotFoundException("null tile path");
				}
				File tileFile = new File(path);
				byte[] data = new byte[(int)tileFile.length()];
				dataIs = new DataInputStream(new FileInputStream(tileFile));
				dataIs.readFully(data);
				mCallback.mapTileLoaded(mTile.rendererID, mTile.zoomLevel, mTile.x, mTile.y, data);
				// the following will add back tiles to the DB if the DB was deleted
				OpenStreetMapTileFilesystemProvider.this.mDatabase.addTileOrIncrement(mTile, (int)tileFile.length());
				if (Log.isLoggable(DEBUGTAG, Log.DEBUG))
					Log.d(DEBUGTAG, "Loaded: " + mTile.toString());
			} catch (FileNotFoundException e) {
				if (Log.isLoggable(DEBUGTAG, Log.DEBUG))
					Log.i(DEBUGTAG, "FS failed, request for download.");
				mTileDownloader.loadMapTileAsync(mTile, mCallback);
			} catch (IOException e) {
				try {
					mCallback.mapTileFailed(mTile.rendererID, mTile.zoomLevel, mTile.x, mTile.y, IOERR);
				} catch (RemoteException e1) {
					if (Log.isLoggable(DEBUGTAG, Log.DEBUG)) {
						Log.e(DEBUGTAG, "Error marking tile as failed", e);
					}
				}
				if (Log.isLoggable(DEBUGTAG, Log.DEBUG)) {	// only log in debug mode, though it's an error message
					Log.e(DEBUGTAG, "Error Loading MapTile from FS.");
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
				try {
					if (dataIs != null)
						dataIs.close();
				} catch (IOException e) {
					// ignore
				}
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
		mDatabase.addTileOrIncrement(mTile, 0);	
	}
}