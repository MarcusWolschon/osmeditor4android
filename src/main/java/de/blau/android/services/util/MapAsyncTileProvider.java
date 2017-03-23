package de.blau.android.services.util;

import java.util.HashSet;
import java.util.concurrent.ExecutorService;

import de.blau.android.services.IMapTileProviderCallback;

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
public abstract class MapAsyncTileProvider {

	public static final int IOERR = 1;
	public static final int DOESNOTEXIST = 2;
	public static final int NONETWORK = 3;
	
	ExecutorService mThreadPool;
	private final HashSet<String> mPending = new HashSet<String>();
	
	public void loadMapTileAsync(final MapTile aTile,
			final IMapTileProviderCallback aCallback) {
		final String tileID = aTile.toId();
		
		if(mPending.contains(tileID))
			return;
		
		mPending.add(tileID);

		mThreadPool.execute(getTileLoader(aTile, aCallback));
	}
	
	protected abstract Runnable getTileLoader(final MapTile aTile,
			final IMapTileProviderCallback aCallback);

	abstract class TileLoader implements Runnable {
		final MapTile mTile;
		final IMapTileProviderCallback mCallback;
		
		public TileLoader(final MapTile aTile, final IMapTileProviderCallback aCallback) {
			mTile = aTile;
			mCallback = aCallback;
		}
		
		void finished() {
			mPending.remove(mTile.toId());
		}
	}
	
}
