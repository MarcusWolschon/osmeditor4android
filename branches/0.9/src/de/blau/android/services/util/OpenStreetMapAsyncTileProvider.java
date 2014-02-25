package de.blau.android.services.util;

import java.util.HashSet;
import java.util.concurrent.ExecutorService;

import de.blau.android.services.IOpenStreetMapTileProviderCallback;

/**
 * 
 * <br/>
 * This class was taken from OpenStreetMapViewer (original package org.andnav.osm) in 2010-06
 * by Marcus Wolschon  to be integrated into the de.blau.androin
 * OSMEditor. 
 * @author Nicolas Gramlich
 * @author Marcus Wolschon <Marcus@Wolschon.biz>
 *
 */
public abstract class OpenStreetMapAsyncTileProvider {

	public static final int IOERR = 1;
	public static final int DOESNOTEXIST = 2;
	
	protected ExecutorService mThreadPool;
	private final HashSet<String> mPending = new HashSet<String>();
	
	public void loadMapTileAsync(final OpenStreetMapTile aTile,
			final IOpenStreetMapTileProviderCallback aCallback) {
		final String tileID = aTile.toString();
		
		if(mPending.contains(tileID))
			return;
		
		mPending.add(tileID);

		mThreadPool.execute(getTileLoader(aTile, aCallback));
	}
	
	protected abstract Runnable getTileLoader(final OpenStreetMapTile aTile,
			final IOpenStreetMapTileProviderCallback aCallback);

	protected abstract class TileLoader implements Runnable {
		final OpenStreetMapTile mTile;
		final IOpenStreetMapTileProviderCallback mCallback;
		
		public TileLoader(final OpenStreetMapTile aTile, final IOpenStreetMapTileProviderCallback aCallback) {
			mTile = aTile;
			mCallback = aCallback;
		}
		
		protected void finished() {
			mPending.remove(mTile.toString());
		}
	}
	
}
