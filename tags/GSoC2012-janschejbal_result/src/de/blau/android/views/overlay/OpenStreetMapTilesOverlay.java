package de.blau.android.views.overlay;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import de.blau.android.Map;
import de.blau.android.services.util.OpenStreetMapTile;
import de.blau.android.util.GeoMath;
import de.blau.android.views.IMapView;
import de.blau.android.views.util.OpenStreetMapTileProvider;
import de.blau.android.views.util.OpenStreetMapTileServer;

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
	
	/** Define a minimum active area for taps on the tile attribution data. */
	private static final int TAPAREA_MIN_WIDTH = 40;
	private static final int TAPAREA_MIN_HEIGHT = 40;
	
	/** Tap tracking */
	private float downX, downY;
	private boolean moved;
	private Rect tapArea = new Rect();
	
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
	protected final Paint textPaint = new Paint();

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
		textPaint.setColor(Color.WHITE);
		textPaint.setTypeface(Typeface.SANS_SERIF);
		textPaint.setTextSize(12);
		textPaint.setShadowLayer(1, 0, 0, Color.BLACK);
	}
	
	public void onDestroy() {
		super.onDestroy();
		mTileProvider.clear();
	}
	
	/**
	 * Try to reduce memory use.
	 */
	@Override
	public void onLowMemory() {
		super.onLowMemory();
		// The tile provider with its cache consumes the most memory.
		mTileProvider.onLowMemory();
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
	 * {@inheritDoc}.
	 */
	@Override
	public void onDraw(Canvas c, IMapView osmv) {
		// Do some calculations and drag attributes to local variables to save
		//some performance.
		final Rect viewPort = c.getClipBounds();
		final int zoomLevel = osmv.getZoomLevel(viewPort);
		final OpenStreetMapTile tile = new OpenStreetMapTile(myRendererInfo.getId(), 0, 0, 0); // reused instance of OpenStreetMapTile
		final double lonLeft   = GeoMath.xToLonE7(viewPort.width() , osmv.getViewBox(), viewPort.left  ) / 1E7d;
		final double lonRight  = GeoMath.xToLonE7(viewPort.width() , osmv.getViewBox(), viewPort.right ) / 1E7d;
		final double latTop    = GeoMath.yToLatE7(viewPort.height(), osmv.getViewBox(), viewPort.top   ) / 1E7d;
		final double latBottom = GeoMath.yToLatE7(viewPort.height(), osmv.getViewBox(), viewPort.bottom) / 1E7d;
		
		// pseudo-code for lon/lat to tile numbers
		//n = 2 ^ zoom
		//xtile = ((lon_deg + 180) / 360) * n
		//ytile = (1 - (log(tan(lat_rad) + sec(lat_rad)) / PI)) / 2 * n
		final int xTileLeft   = (int) Math.floor(((lonLeft  + 180d) / 360d) * Math.pow(2d, zoomLevel));
		final int xTileRight  = (int) Math.floor(((lonRight + 180d) / 360d) * Math.pow(2d, zoomLevel));
		final int yTileTop    = (int) Math.floor((1d - Math.log(Math.tan(Math.toRadians(latTop   )) + 1d / Math.cos(Math.toRadians(latTop   ))) / Math.PI) / 2d * Math.pow(2d, zoomLevel));
		final int yTileBottom = (int) Math.floor((1d - Math.log(Math.tan(Math.toRadians(latBottom)) + 1d / Math.cos(Math.toRadians(latBottom))) / Math.PI) / 2d * Math.pow(2d, zoomLevel));
		
		final int tileNeededLeft   = Math.min(xTileLeft, xTileRight);
		final int tileNeededRight  = Math.max(xTileLeft, xTileRight);
		final int tileNeededTop    = Math.min(yTileTop, yTileBottom);
		final int tileNeededBottom = Math.max(yTileTop, yTileBottom);
		
		final int mapTileMask = (1 << zoomLevel) - 1;
		
		// Draw all the MapTiles that intersect with the screen
		// y = y tile number (latitude)
		for (int y = tileNeededTop; y <= tileNeededBottom; y++) {
			// x = x tile number (longitude)
			for (int x = tileNeededLeft; x <= tileNeededRight; x++) {
				// Set the specifications for the required tile
				tile.zoomLevel = zoomLevel;
				tile.x = x & mapTileMask;
				tile.y = y & mapTileMask;
				
				// Set the size and top left corner on the source bitmap
				int sw = myRendererInfo.getTileWidth();
				int sh = myRendererInfo.getTileHeight();
				int tx = 0;
				int ty = 0;
				
				if (!mTileProvider.isTileAvailable(tile)) {
					// Preferred tile is not available - request it
					mTileProvider.preCacheTile(tile);
					// See if there are any alternative tiles available - try
					// using larger tiles
					while (!mTileProvider.isTileAvailable(tile) && tile.zoomLevel > myRendererInfo.getMinZoomLevel()) {
						// As we zoom out to larger-scale tiles, we want to
						// draw smaller and smaller sections of them
						sw >>= 1; // smaller size
						sh >>= 1;
						tx >>= 1; // smaller offsets
						ty >>= 1;
						// select the correct quarter
						if ((tile.x & 1) != 0) tx += (myRendererInfo.getTileWidth() >> 1);
						if ((tile.y & 1) != 0) ty += (myRendererInfo.getTileHeight() >> 1);
						// zoom out to next level
						tile.x >>= 1;
						tile.y >>= 1;
						--tile.zoomLevel;
					}
				}
				if (mTileProvider.isTileAvailable(tile)) {
					c.drawBitmap(
						mTileProvider.getMapTile(tile),
						new Rect(tx, ty, tx + sw, ty + sh),
						getScreenRectForTile(c, osmv, zoomLevel, y, x),
						mPaint);
				} else {
					// Still no tile available - try smaller scale tiles
					drawTile(c, osmv, zoomLevel + 2, zoomLevel, x & mapTileMask, y & mapTileMask);
				}
			}
		}
		
		// Draw the tile layer branding logo (if it exists)
		tapArea.left = 0;
		tapArea.right = 0;
		tapArea.top = viewPort.bottom;
		tapArea.bottom = viewPort.bottom;
		Drawable brandLogo = myRendererInfo.getBrandLogo();
		if (brandLogo != null) {
			tapArea.top -= brandLogo.getIntrinsicHeight();
			tapArea.right += brandLogo.getIntrinsicWidth();
			brandLogo.setBounds(tapArea);
			brandLogo.draw(c);
		}
		// Draw the attributions (if any)
		for (String attr : myRendererInfo.getAttributions(zoomLevel, new RectF((float)lonLeft, (float)latBottom, (float)lonRight, (float)latTop))) {
			c.drawText(attr, tapArea.left, tapArea.top, textPaint);
			tapArea.top -= textPaint.getTextSize();
			tapArea.right = Math.max(tapArea.right, tapArea.left + (int)textPaint.measureText(attr));
		}
		// Impose a minimum tap area
		if (tapArea.width() < TAPAREA_MIN_WIDTH) {
			tapArea.right = tapArea.left + TAPAREA_MIN_WIDTH;
		}
		if (tapArea.height() < TAPAREA_MIN_HEIGHT) {
			tapArea.top = tapArea.bottom - TAPAREA_MIN_HEIGHT;
		}
	}
	
	/** Recursively search the cache for smaller tiles to fill in the required
	 * space.
	 * @param c Canvas to draw on.
	 * @param osmv Map view area.
	 * @param maxz Maximum zoom level to attempt - don't take too long searching.
	 * @param z Zoom level to draw.
	 * @param x Tile X to draw.
	 * @param y Tile Y to draw.
	 */
	private void drawTile(Canvas c, IMapView osmv, int maxz, int z, int x, int y) {
		final OpenStreetMapTile tile = new OpenStreetMapTile(myRendererInfo.getId(), z, x, y);
		if (mTileProvider.isTileAvailable(tile)) {
			c.drawBitmap(
				mTileProvider.getMapTile(tile),
				new Rect(0, 0, myRendererInfo.getTileWidth(), myRendererInfo.getTileHeight()),
				getScreenRectForTile(c, osmv, z, y, x),
				mPaint);
		} else if (z < maxz && z < myRendererInfo.getMaxZoomLevel()){
			// Still no tile available - try smaller scale tiles
			x <<= 1;
			y <<= 1;
			++z;
			drawTile(c, osmv, maxz, z, x    , y    );
			drawTile(c, osmv, maxz, z, x + 1, y    );
			drawTile(c, osmv, maxz, z, x    , y + 1);
			drawTile(c, osmv, maxz, z, x + 1, y + 1);
		}
	}
	
	/**
	 * @param c the canvas we draw to (we need its clip-bound's width and height)
	 * @param osmv the view with its viewBox
	 * @param zoomLevel the zoom-level of the tile
	 * @param y the y-number of the tile
	 * @param x the x-number of the tile
	 * @return the rectangle of screen-coordinates it consumes.
	 */
	private Rect getScreenRectForTile(Canvas c, IMapView osmv,
			final int zoomLevel, int y, int x) {
		final Rect viewPort = c.getClipBounds();
		double north = tile2lat(y    , zoomLevel);
		double south = tile2lat(y + 1, zoomLevel);
		double west  = tile2lon(x    , zoomLevel);
		double east  = tile2lon(x + 1, zoomLevel);

		int screenLeft   = (int) Math.round(GeoMath.lonE7ToX(viewPort.width() , osmv.getViewBox(), (int) (west  * 1E7)));
		int screenRight  = (int) Math.round(GeoMath.lonE7ToX(viewPort.width() , osmv.getViewBox(), (int) (east  * 1E7)));
		int screenTop    = (int) Math.round(GeoMath.latE7ToY(viewPort.height(), osmv.getViewBox(), (int) (north * 1E7)));
		int screenBottom = (int) Math.round(GeoMath.latE7ToY(viewPort.height(), osmv.getViewBox(), (int) (south * 1E7)));

		return new Rect(screenLeft, screenTop, screenRight, screenBottom);
	}

	/**
	 * {@inheritDoc}.
	 */
	@Override
	public void onDrawFinished(Canvas c, IMapView osmv) {
	}

	/**
	 * Handle touch events in order to display tile layer End User Terms Of Use
	 * when the tile provider branding logo or attributions are tapped.
	 * @param event The touch event information.
	 * @param mapView The parent map view of this overlay.
	 * @return true if the event was handled.
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event, IMapView mapView) {
		boolean done = false;
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			downX = event.getX();
			downY = event.getY();
			moved = false;
			break;
		case MotionEvent.ACTION_UP:
			done = true;
			// FALL THROUGH
		case MotionEvent.ACTION_MOVE:
			moved |= (Math.abs(event.getX() - downX) > 20 ||
					  Math.abs(event.getY() - downY) > 20);
			if (done && !moved && tapArea.contains((int)event.getX(), (int)event.getY())) {
				String touUri = myRendererInfo.getTouUri();
				if (touUri != null) {
					// Display the End User Terms Of Use (in the browser)
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse(touUri));
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					myView.getContext().startActivity(intent);
					return true;
				}
			}
			break;
		}
		return false;
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
