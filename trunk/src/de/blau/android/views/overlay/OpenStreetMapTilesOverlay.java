package de.blau.android.views.overlay;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import de.blau.android.Application;
import de.blau.android.DialogFactory;
import de.blau.android.Map;
import de.blau.android.resources.Profile;
import de.blau.android.services.util.OpenStreetMapTile;
import de.blau.android.util.GeoMath;
import de.blau.android.util.Offset;
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
	private static Rect tapArea = null;
	
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
	protected Paint textPaint = new Paint();
	

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
			mTileProvider = new OpenStreetMapTileProvider(myView.getContext().getApplicationContext(), new SimpleInvalidationHandler(myView));
		} else {
			mTileProvider = aTileProvider;
		}
		// 
		textPaint = Profile.getCurrent(Profile.ATTRIBUTION_TEXT).getPaint();
		// mPaint.setAlpha(aRendererInfo.getDefaultAlpha());
		Log.d("OpenStreetMapTilesOverlay","provider " + aRendererInfo.getId());
	}
	
	@Override
	public boolean isReadyToDraw() {
		return myRendererInfo.isMetadataLoaded();
	}
	
	public void flushTileCache() {
		new AsyncTask<Void,Void,Void>() {
			
			@Override
			protected void onPreExecute() {
				Application.mainActivity.showDialog(DialogFactory.PROGRESS_DELETING);
			}
			
			@Override
			protected Void doInBackground(Void... params) {
				mTileProvider.flushCache(myRendererInfo.getId());
				return null;
			}
			
			@Override
			protected void onPostExecute(Void result) {
				try {
					Application.mainActivity.dismissDialog(DialogFactory.PROGRESS_DELETING);
				} catch (IllegalArgumentException e) {
					 // Avoid crash if dialog is already dismissed
				}
			}
			
		}.execute();

	}
	
	@Override
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
		if (myRendererInfo != aRendererInfo) {
			// ...
		}
		myRendererInfo = aRendererInfo;
	}

	public OpenStreetMapTileProvider getTileProvider() {
		return mTileProvider;
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
		final int zoomLevel = osmv.getZoomLevel();
//		if (zoomLevel < myRendererInfo.getMinZoomLevel()) {
//			Log.d("OpenStreetMapTilesOverlay","Tiles for " + myRendererInfo.getId() + " are not available for zoom " + zoomLevel);
//			return;
//		}
		double lonOffset = 0d;
		double latOffset = 0d;
		Offset offset = myRendererInfo.getOffset(zoomLevel);
		if (offset != null) {
			lonOffset = offset.lon;
			latOffset = offset.lat;
		}

		final OpenStreetMapTile tile = new OpenStreetMapTile(myRendererInfo.getId(), 0, 0, 0); // reused instance of OpenStreetMapTile
		// 
		final double lonLeft   =  osmv.getViewBox().getLeft() / 1E7d - (lonOffset > 0 ? lonOffset : 0d); 	
		final double lonRight  =  osmv.getViewBox().getRight() / 1E7d - (lonOffset < 0 ? lonOffset : 0d);	
		final double latTop    =  Math.toRadians(osmv.getViewBox().getTop() / 1E7d - (latOffset < 0 ? latOffset : 0d));	
		final double latBottom =  Math.toRadians(osmv.getViewBox().getBottom() / 1E7d - (latOffset > 0 ? latOffset : 0d));	
		
		// pseudo-code for lon/lat to tile numbers
		//n = 2 ^ zoom
		//xtile = ((lon_deg + 180) / 360) * n
		//ytile = (1 - (log(tan(lat_rad) + sec(lat_rad)) / PI)) / 2 * n
		final double n = Math.pow(2d, zoomLevel);
		final int xTileLeft   = (int) Math.floor(((lonLeft  + 180d) / 360d) * n);
		final int xTileRight  = (int) Math.floor(((lonRight + 180d) / 360d) * n);

		final int yTileTop    = (int) Math.floor((1d - Math.log(Math.tan(latTop) + 1d / Math.cos(latTop)) / Math.PI) * n / 2d);
		final int yTileBottom = (int) Math.floor((1d - Math.log(Math.tan(latBottom) + 1d / Math.cos(latBottom)) / Math.PI) * n / 2d);

		final int tileNeededLeft   = Math.min(xTileLeft, xTileRight);
		final int tileNeededRight  = Math.max(xTileLeft, xTileRight);
		final int tileNeededTop    = Math.min(yTileTop, yTileBottom);
		final int tileNeededBottom = Math.max(yTileTop, yTileBottom);
		
//		Log.d("OpenStreetMapTileOverlay","zoom " + zoomLevel + " tile left " + xTileLeft + " right " + xTileRight + " top " +yTileTop + " bottom " + yTileBottom);
//		Log.d("OpenStreetMapTileOverlay"," top " + tileNeededTop + " bottom " + tileNeededBottom);
//		Log.d("OpenStreetMapTileOverlay","lonLeft " + lonLeft + " lonRight " + lonRight + " latTop " + Math.toDegrees(latTop)+ " latBottom " + Math.toDegrees(latBottom));
		
		final int mapTileMask = (1 << zoomLevel) - 1;
		
		Rect destRect = null; // destination rect for bit map
		int destIncX = 0, destIncY = 0;
		int xPos = 0, yPos = 0;
		
		boolean squareTiles = myRendererInfo.getTileWidth() == myRendererInfo.getTileHeight();
		// Draw all the MapTiles that intersect with the screen
		// y = y tile number (latitude)
		for (int y = tileNeededTop; y <= tileNeededBottom; y++) {
			// x = x tile number (longitude)
			for (int x = tileNeededLeft; x <= tileNeededRight; x++) {
				// Set the specifications for the required tile
				tile.zoomLevel = zoomLevel;
				tile.x = x & mapTileMask;
				tile.y = y & mapTileMask;
				
				// destination rect
				if (destRect == null) { // avoid recalculating this for every tile
					destRect = getScreenRectForTile(c, osmv, zoomLevel, y, x, squareTiles, lonOffset, latOffset);
					destIncX = destRect.width();
					destIncY = destRect.height();
					// Log.d("OpenStreetMapTileOverlay","tile width " + destIncX + " height " + destIncY);
				}
				
				// Set the size and top left corner on the source bitmap
				int sw = myRendererInfo.getTileWidth();
				int sh = myRendererInfo.getTileHeight();
				int tx = 0;
				int ty = 0;
				Bitmap tileBitmap = mTileProvider.getMapTile(tile);
				if (tileBitmap == null) {
					// Log.d("OpenStreetMapTileOverlay","tile not available trying larger");
					// OVERZOOM
					// Preferred tile is not available - request it
					// mTileProvider.preCacheTile(tile); already done in getMapTile
					// See if there are any alternative tiles available - try
					// using larger tiles
					while ((tileBitmap == null) && tile.zoomLevel > myRendererInfo.getMinZoomLevel()) {
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
						tileBitmap = mTileProvider.getMapTile(tile);
						// mTileProvider.preCacheTile(tile);
					}
				}

				if (tileBitmap != null) {
					c.drawBitmap(
						tileBitmap,
						new Rect(tx, ty, tx + sw, ty + sh),
						new Rect(destRect.left + xPos, destRect.top + yPos, destRect.right + xPos,  destRect.bottom + yPos),
						mPaint);
				} else {
					// Still no tile available - try smaller scale tiles
					drawTile(c, osmv, 0, zoomLevel + 2, zoomLevel, x & mapTileMask, y & mapTileMask, squareTiles, lonOffset, latOffset);
				}
				// Log.d("OpenStreetMapTileOverlay","Dest rect " + (destRect.left + xPos) + " " + (destRect.right + xPos) + " " + (destRect.top + yPos) +  " " + (destRect.bottom + yPos));
				xPos += destIncX;
			}
			xPos = 0;
			yPos += destIncY;
		}
		
		// Draw the tile layer branding logo (if it exists)
		if (tapArea == null) {
			resetAttributionArea(viewPort);
		}
		Drawable brandLogo = myRendererInfo.getBrandLogo();
		if (brandLogo != null) {
			tapArea.top -= brandLogo.getIntrinsicHeight();
			tapArea.right += brandLogo.getIntrinsicWidth();
			brandLogo.setBounds(tapArea);
			brandLogo.draw(c);
		}
		// Draw the attributions (if any)
		for (String attr : myRendererInfo.getAttributions(zoomLevel, osmv.getViewBox())) {
			c.drawText(attr, tapArea.left, tapArea.top, textPaint);
			tapArea.top -= textPaint.getTextSize();
			tapArea.right = Math.max(tapArea.right, tapArea.left + (int)textPaint.measureText(attr));
		}
		// Impose a minimum tap area
		if (tapArea.width() < TAPAREA_MIN_WIDTH) {
			tapArea.right = tapArea.left + TAPAREA_MIN_WIDTH;
		}
//TODO fix, causes problems with multiple layers
//		if (tapArea.height() < TAPAREA_MIN_HEIGHT) {
//			tapArea.top = tapArea.bottom - TAPAREA_MIN_HEIGHT;
//		}
	}

	public static void resetAttributionArea(Rect viewPort) {
		if (tapArea == null) {
			tapArea = new Rect();
		}
		tapArea.left = 0;
		tapArea.right = 0;
		tapArea.top = viewPort.bottom;
		tapArea.bottom = viewPort.bottom;
	}
	
	/** Recursively search the cache for smaller tiles to fill in the required
	 * space.
	 * @param c Canvas to draw on.
	 * @param osmv Map view area.
	 * @param minz Minimum zoom level.
	 * @param maxz Maximum zoom level to attempt - don't take too long searching.
	 * @param z Zoom level to draw.
	 * @param x Tile X to draw.
	 * @param y Tile Y to draw.
	 * @param lonOffset TODO
	 * @param latOffset TODO
	 */
	private void drawTile(Canvas c, IMapView osmv, int minz, int maxz, int z, int x, int y, boolean squareTiles, double lonOffset, double latOffset) {
		final OpenStreetMapTile tile = new OpenStreetMapTile(myRendererInfo.getId(), z, x, y);
		if (mTileProvider.isTileAvailable(tile)) {
			c.drawBitmap(
				mTileProvider.getMapTile(tile),
				new Rect(0, 0, myRendererInfo.getTileWidth(), myRendererInfo.getTileHeight()),
				getScreenRectForTile(c, osmv, z, y, x, squareTiles, lonOffset, latOffset),
				mPaint);
		} else {
			if (z < maxz && z < myRendererInfo.getMaxZoomLevel()) {
				// try smaller scale tiles
				x <<= 1;
				y <<= 1;
				++z;
				drawTile(c, osmv, z, maxz, z, x    , y    , squareTiles, lonOffset, latOffset);
				drawTile(c, osmv, z, maxz, z, x + 1, y    , squareTiles, lonOffset, latOffset);
				drawTile(c, osmv, z, maxz, z, x    , y + 1, squareTiles, lonOffset, latOffset);
				drawTile(c, osmv, z, maxz, z, x + 1, y + 1, squareTiles, lonOffset, latOffset);
			}
		}
	}
	
	/**
	 * NOTE: currently assumes square tiles
	 * @param c the canvas we draw to (we need its clip-bound's width and height)
	 * @param osmv the view with its viewBox
	 * @param zoomLevel the zoom-level of the tile
	 * @param y the y-number of the tile
	 * @param x the x-number of the tile
	 * @param lonOffset TODO
	 * @param latOffset TODO
	 * @return the rectangle of screen-coordinates it consumes.
	 */
	private Rect getScreenRectForTile(Canvas c, IMapView osmv,
			final int zoomLevel, int y, int x, boolean squareTiles, double lonOffset, double latOffset) {

		double north = tile2lat(y    , zoomLevel);
		// double south = tile2lat(y + 1, zoomLevel); only calculate when needed (aka non square tiles)
		double west  = tile2lon(x    , zoomLevel);
		double east  = tile2lon(x + 1, zoomLevel);
		int w = c.getClipBounds().width();
		int h = c.getClipBounds().height();
		int screenLeft   = (int) GeoMath.lonE7ToX(w , osmv.getViewBox(), (int) ((west + lonOffset) * 1E7));
		
		int tileWidth = (int)((double)(east - west) * 1E7 * w / osmv.getViewBox().getWidth()); // calculate here to avoid rounding differences

		int screenTop    = (int) GeoMath.latE7ToY(h, w, osmv.getViewBox(), (int) ((north + latOffset)* 1E7));
		int screenBottom = squareTiles ? screenTop + tileWidth : (int) GeoMath.latE7ToY(h, w, osmv.getViewBox(), (int) ((tile2lat(y + 1, zoomLevel) + latOffset)* 1E7));
		// Log.d("OpenStreeMapTileOverlay", "Dest Rect " + screenLeft + " " + screenTop + " " +  screenRight + " " + screenBottom);
		return new Rect(screenLeft, screenTop, screenLeft+tileWidth, screenBottom);
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
			if (done && !moved && (tapArea != null) && tapArea.contains((int)event.getX(), (int)event.getY())) {
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
	private static class SimpleInvalidationHandler extends Handler {
		private View v;
		private int viewInvalidates = 0;
		
		public SimpleInvalidationHandler(View v) {
			super();
			this.v = v;
		}
		@Override
		public void handleMessage(final Message msg) {
			switch (msg.what) {
				case OpenStreetMapTile.MAPTILE_SUCCESS_ID:
				    if (viewInvalidates == 0) { // try to suppress inordinate number of invalidates
						Handler handler = new Handler(); 
					    handler.postDelayed(new Runnable() { 
					         @Override
							public void run() { 
					        	 // Log.d("OpenStreetMapOverlay", "SimpleInvalidationHandler #viewInvalidates " + viewInvalidates);
					        	 viewInvalidates = 0;
					             v.invalidate();
					         } 
					    }, 100); // wait 1/10th of a second
					    viewInvalidates++;
				    } else
				    	viewInvalidates++;
					break;
			}
		}
	}
}
