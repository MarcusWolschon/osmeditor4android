package de.blau.android.views.overlay;

import de.blau.android.services.util.OpenStreetMapTile;
import de.blau.android.Map;
import de.blau.android.views.IMapView;
import de.blau.android.views.util.OpenStreetMapTileServer;
import de.blau.android.views.util.OpenStreetMapTileProvider;
import de.blau.android.util.GeoMath;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.view.View;

/**
 * Overlay that draws downloaded tiles which may be displayed on top of an
 * {@link IMapView}. To add an overlay, subclass this class, create an
 * instance, and add it to the list obtained from getOverlays() of
 * {@link Map}.
 * <br/>
 * This class was taken from OpenStreetMapViewer (original package org.andnav.osm) in 2010-06
 * and changed significantly by Marcus Wolschon to be integrated into the de.blau.androin
 * OSMEditor. 
 * @author Nicolas Gramlich
 * @author Marcus Wolschon <Marcus@Wolschon.biz>
 */
public class OpenStreetMapTilesOverlay extends OpenStreetMapViewOverlay {

	/**
	 * The view we are a part of.
	 */
	protected View myView;
	/**
	 * The tile-server to load a rendered map from.
	 */
	protected OpenStreetMapTileServer myRendererInfo;

	/** Current renderer */
	protected final OpenStreetMapTileProvider mTileProvider;
	protected final Paint mPaint = new Paint();

	/**
	 * 
	 * @param aView The view we are a part of.
	 * @param aRendererInfo The tile-server to load a rendered map from.
	 * @param aTileProvider (may be null)
	 */
	public OpenStreetMapTilesOverlay(final View aView,
			final OpenStreetMapTileServer aRendererInfo,
			final OpenStreetMapTileProvider aTileProvider) {
		myView = aView;
		myRendererInfo = aRendererInfo;
		if(aTileProvider == null) {
			mTileProvider = new OpenStreetMapTileProvider(myView.getContext().getApplicationContext(), new SimpleInvalidationHandler());
		} else {
			mTileProvider = aTileProvider;
		}
	}
	
	public void onDestroy() {
		mTileProvider.clear();
	}
	
	public OpenStreetMapTileServer getRendererInfo() {
		return myRendererInfo;
	}
	
	public void setRendererInfo(final OpenStreetMapTileServer aRendererInfo) {
		myRendererInfo = aRendererInfo;
	}

	public void setAlpha(int a) {
		mPaint.setAlpha(a);
	}

	/**
	 * @param x a x tile -number
	 * @param aZoomLevel a zoom-level of a tile
	 * @return the longitude of the tile
	 */
	static double tile2lon(int x, int aZoomLevel) {
	     return x / Math.pow(2.0, aZoomLevel) * 360.0 - 180;
	  }

	/**
	 * @param y a y tile -number
	 * @param aZoomLevel a zoom-level of a tile
	 * @return the latitude of the tile
	 */
	static double tile2lat(int y, int aZoomLevel) {
		double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, aZoomLevel);
		return Math.toDegrees(Math.atan(Math.sinh(n)));
	}

	/**
	 * Helper-Method
	 * @param number a number to calculate the modulo for
	 * @param modulus what modulo to calculate
	 * @return always > 0
	 */
	private static int mod(int number, final int modulus){
		if(number > 0) {
			return number % modulus;
		}

		while(number < 0) {
			number += modulus;
		}

		return number;
	}

	/**
	 * {@inheritDoc}.
	 */
	@Override
	public void onDraw(Canvas c, IMapView osmv) {
		// Do some calculations and drag attributes to local variables to save
		//some performance.
		final Rect viewPort = c.getClipBounds();
		final int zoomLevel = osmv.getZoomLevel(viewPort);
		final OpenStreetMapTile tile = new OpenStreetMapTile(0, 0, 0, 0); // reused instance of OpenStreetMapTile
		tile.rendererID = myRendererInfo.ordinal();	// TODO get from service
		double latLeftUpper = GeoMath.yToLatE7(c.getHeight(), osmv.getViewBox(), viewPort.top) / 1E7d;
		double lonLeftUpper = GeoMath.xToLonE7(c.getWidth(), osmv.getViewBox(), viewPort.left) / 1E7d;
		double latRightLower = GeoMath.yToLatE7(c.getHeight(), osmv.getViewBox(), viewPort.bottom) / 1E7d;
		double lonRightLower = GeoMath.xToLonE7(c.getWidth(), osmv.getViewBox(), viewPort.right) / 1E7d;
		
		
		// pseudo-code for lon/lat to tile numbers
		//n = 2 ^ zoom
		//xtile = ((lon_deg + 180) / 360) * n
		//ytile = (1 - (log(tan(lat_rad) + sec(lat_rad)) / π)) / 2 * n
		int xTileLeftUpper = (int) Math.floor(((lonLeftUpper + 180) / 360d) * Math.pow(2, zoomLevel));
		int yTileLeftUpper = (int) Math.floor((1 - Math.log(Math.tan(Math.toRadians(latLeftUpper)) + 1 / Math.cos(Math.toRadians(latLeftUpper))) / Math.PI) /2 * Math.pow(2, zoomLevel));
		int xTileRightLower = (int) Math.floor(((lonRightLower + 180) / 360d) * Math.pow(2, zoomLevel));
		int yTileRightLower = (int) Math.floor((1 - Math.log(Math.tan(Math.toRadians(latRightLower)) + 1 / Math.cos(Math.toRadians(latRightLower))) / Math.PI) /2 * Math.pow(2, zoomLevel));
		
		final int tileNeededToLeftOfCenter   = Math.min(xTileLeftUpper, xTileRightLower);
		final int tileNeededToRightOfCenter  = Math.max(xTileLeftUpper, xTileRightLower);
		final int tileNeededToTopOfCenter    = Math.min(yTileLeftUpper, yTileRightLower);
		final int tileNeededToBottomOfCenter = Math.max(yTileLeftUpper, yTileRightLower);
		
		final int mapTileUpperBound = 1 << zoomLevel;
		
		// Draw all the MapTiles that intersect with the screen
		// y = y tile number (latitude)
		for (int y = tileNeededToTopOfCenter; y <= tileNeededToBottomOfCenter && y >= tileNeededToTopOfCenter; y++) {
			// x = x tile number (longitude)
			for (int x = tileNeededToLeftOfCenter; x <= tileNeededToRightOfCenter && x >= tileNeededToLeftOfCenter; x++) {
				// Construct a URLString, which represents the MapTile
				tile.zoomLevel = zoomLevel;
				tile.y = mod(y, mapTileUpperBound);
				tile.x = mod(x, mapTileUpperBound);

				if (mTileProvider.isTileAvailable(tile)) {
					final Bitmap currentMapTile = mTileProvider.getMapTile(tile);
					final Rect src = new Rect(0, 0, currentMapTile.getWidth(), currentMapTile.getHeight()); 
					final Rect dst = getScreenRectForTile(c, osmv, zoomLevel, y, x);
					c.drawBitmap(currentMapTile, src, dst, mPaint);
				} else {
					mTileProvider.preCacheTile(tile);
					if (zoomLevel > 0) {
						tile.zoomLevel = zoomLevel - 1;
						tile.x >>= 1;
						tile.y >>= 1;
						if (mTileProvider.isTileAvailable(tile)) {
							final Bitmap currentMapTile = mTileProvider.getMapTile(tile);
							if (currentMapTile != null) {
								final Rect src = new Rect(0, 0, currentMapTile.getWidth(), currentMapTile.getHeight()); 
								final Rect dst = getScreenRectForTile(c, osmv, zoomLevel, y, x);
								c.drawBitmap(currentMapTile, src, dst, mPaint);
							}
						}
					}
				}

			}
		}
	}

	/**
	 * @param c the canvas we draw to (we need it´s clpi-bound´s width and height)
	 * @param osmv the view with it´s viewBox
	 * @param zoomLevel the zoom-level of the tile
	 * @param y the y-number of the tile
	 * @param x the x-number of the tile
	 * @return the rectangle of screen-coordinates it consumes.
	 */
	private Rect getScreenRectForTile(Canvas c, IMapView osmv,
			final int zoomLevel, int y, int x) {
		double north = tile2lat(y,     zoomLevel);
		double south = tile2lat(y + 1, zoomLevel);
		double west  = tile2lon(x,     zoomLevel);
		double east  = tile2lon(x + 1, zoomLevel);

		int screenLeft   = (int) Math.round(GeoMath.lonE7ToX(c.getClipBounds().width(), osmv.getViewBox(), (int) (west * 1E7)));
		int screenRight  = (int) Math.round(GeoMath.lonE7ToX(c.getClipBounds().width(), osmv.getViewBox(), (int) (east * 1E7)));
		int screenTop    = (int) Math.round(GeoMath.latE7ToY(c.getClipBounds().height(), osmv.getViewBox(), (int) (north * 1E7)));
		int screenBottom = (int) Math.round(GeoMath.latE7ToY(c.getClipBounds().height(), osmv.getViewBox(), (int) (south * 1E7)));

		final Rect dst = new Rect(screenLeft, screenTop, screenRight, screenBottom);
		return dst;
	}

	/**
	 * {@inheritDoc}.
	 */
	@Override
	public void onDrawFinished(Canvas c, IMapView osmv) {
	}

	/**
	 * Invalidate myView when a new tile got downloaded.
	 */
	private class SimpleInvalidationHandler extends Handler {

		@Override
		public void handleMessage(final Message msg) {
			switch (msg.what) {
				case OpenStreetMapTile.MAPTILE_SUCCESS_ID:
					myView.invalidate();
					break;
			}
		}
	}
}
