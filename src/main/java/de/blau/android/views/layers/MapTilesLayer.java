package de.blau.android.views.layers;

import java.io.IOException;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import de.blau.android.Map;
import de.blau.android.dialogs.LayerInfo;
import de.blau.android.dialogs.Progress;
import de.blau.android.imageryoffset.Offset;
import de.blau.android.layer.ExtentInterface;
import de.blau.android.layer.LayerInfoInterface;
import de.blau.android.layer.MapViewLayer;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.ViewBox;
import de.blau.android.resources.DataStyle;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.services.util.MapAsyncTileProvider;
import de.blau.android.services.util.MapTile;
import de.blau.android.util.GeoMath;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.Snack;
import de.blau.android.util.collections.MRUList;
import de.blau.android.views.IMapView;
import de.blau.android.views.util.MapTileProvider;

/**
 * Overlay that draws downloaded tiles which may be displayed on top of an {@link IMapView}. To add an overlay, subclass
 * this class, create an instance, and add it to the list obtained from getOverlays() of {@link Map}.
 * 
 * This class was taken from OpenStreetMapViewer (original package org.andnav.osm) in 2010-06 and changed significantly
 * by Marcus Wolschon to be integrated into the de.blau.androin OSMEditor.
 * 
 * @author Nicolas Gramlich
 * @author Marcus Wolschon &lt;Marcus@Wolschon.biz&gt;
 * @author Simon Poole
 */
public class MapTilesLayer extends MapViewLayer implements ExtentInterface, LayerInfoInterface {

    private static final String DEBUG_TAG          = MapTilesLayer.class.getSimpleName();
    /** Define a minimum active area for taps on the tile attribution data. */
    private static final int    TAPAREA_MIN_WIDTH  = 40;
    private static final int    TAPAREA_MIN_HEIGHT = 60;

    /**
     * 
     */
    private boolean coverageWarningDisplayed = false;
    private String  coverageWarningMessage;

    /** Tap tracking */
    private float   downX;
    private float   downY;
    private boolean moved;
    private Rect    tapArea  = new Rect();
    private Rect    logoRect = new Rect();

    /**
     * The view we are a part of.
     */
    private View            myView;
    /**
     * The tile-server to load a rendered map from.
     */
    private TileLayerServer myRendererInfo;

    /** Current renderer */
    private final MapTileProvider mTileProvider;
    private final Paint           mPaint    = new Paint();
    private Paint                 textPaint = new Paint();

    /**
     * MRU of last servers
     */
    private static final int                        MRU_SIZE        = 5;
    private final MRUList<String>                   lastServers     = new MRUList<>(MRU_SIZE);
    private transient SavingHelper<MRUList<String>> mruSavingHelper = new SavingHelper<>();

    private int     prevZoomLevel = -1;  // zoom level from previous draw
    private boolean saved         = true;

    /**
     * Construct a new tile layer
     * 
     * @param aView The view we are a part of.
     * @param aRendererInfo The tile-server to load a rendered map from.
     * @param aTileProvider the MapTileProvider if null a new one will be allocated
     */
    public MapTilesLayer(final View aView, final TileLayerServer aRendererInfo, final MapTileProvider aTileProvider) {
        myView = aView;
        Context ctx = myView.getContext();
        setRendererInfo(aRendererInfo);
        if (aTileProvider == null) {
            mTileProvider = new MapTileProvider(ctx, new SimpleInvalidationHandler(myView));
        } else {
            mTileProvider = aTileProvider;
        }
        //
        textPaint = DataStyle.getInternal(DataStyle.ATTRIBUTION_TEXT).getPaint();
        // mPaint.setAlpha(aRendererInfo.getDefaultAlpha());

        Log.d(DEBUG_TAG,
                aRendererInfo != null ? (aRendererInfo.isMetadataLoaded()
                        ? "provider " + aRendererInfo.getId() + " min zoom " + aRendererInfo.getMinZoomLevel() + " max " + aRendererInfo.getMaxZoomLevel()
                        : aRendererInfo.getId() + " is not ready yet") : "renderer is null");
    }

    @Override
    public boolean isReadyToDraw() {
        if (myRendererInfo != null) {
            return myRendererInfo.isMetadataLoaded();
        }
        return false;
    }

    /**
     * Empty the cache
     * 
     * @param activity activity this was called from, if null don't display progress
     */
    public void flushTileCache(@Nullable final FragmentActivity activity) {
        flushTileCache(activity, myRendererInfo.getId());
    }

    /**
     * Empty the cache
     * 
     * @param activity activity this was called from, if null don't display progress
     * @param renderer the renderer to delete the cache for or null for all
     */
    public void flushTileCache(@Nullable final FragmentActivity activity, final String renderer) {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected void onPreExecute() {
                if (activity != null) {
                    Progress.showDialog(activity, Progress.PROGRESS_DELETING);
                }
            }

            @Override
            protected Void doInBackground(Void... params) {
                if (mTileProvider != null) {
                    mTileProvider.flushCache(renderer);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                if (activity != null) {
                    Progress.dismissDialog(activity, Progress.PROGRESS_DELETING);
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

    /**
     * Get the current displayed tile layer configuration
     * 
     * @return a TileLayerServer object
     */
    @Nullable
    public TileLayerServer getTileLayerConfiguration() {
        return myRendererInfo;
    }

    /**
     * Set the tile layer to display
     * 
     * Updates warning message if we are outside of coverage and flushes request queue
     * 
     * @param tileLayer layer to use
     */
    public void setRendererInfo(@Nullable final TileLayerServer tileLayer) {
        if (tileLayer == null) {
            Log.w(DEBUG_TAG, "Trying to set null layer");
            return;
        }
        if (myRendererInfo != tileLayer) {
            try {
                coverageWarningMessage = myView.getResources().getString(de.blau.android.R.string.toast_no_coverage, tileLayer.getName());
            } catch (Exception ex) {
                coverageWarningMessage = "";
            }
            coverageWarningDisplayed = false;
            if (myRendererInfo != null) { // 1st invocation this is null
                mTileProvider.flushQueue(myRendererInfo.getId(), MapAsyncTileProvider.ALLZOOMS);
                saved = false;
                lastServers.push(myRendererInfo.getId());
            }
        }
        myRendererInfo = tileLayer;
    }

    /**
     * Get the current MapTileProvider
     * 
     * @return the current MapTileProvider
     */
    @NonNull
    public MapTileProvider getTileProvider() {
        return mTileProvider;
    }

    /**
     * Set the contrast for this layer
     * 
     * @param a the new value
     */
    public void setContrast(float a) {
        float scale = a + 1.f;
        float translate = (-.5f * scale + .5f) * 255.f;
        ColorMatrix cm = new ColorMatrix();
        cm.set(new float[] { scale, 0, 0, 0, translate, 0, scale, 0, 0, translate, 0, 0, scale, 0, translate, 0, 0, 0, 1, 0 });
        mPaint.setColorFilter(new ColorMatrixColorFilter(cm));
    }

    /**
     * @param x a x tile -number
     * @param aZoomLevel a zoom-level of a tile
     * @return the longitude of the tile
     */
    private static double tile2lon(int x, int aZoomLevel) {
        return x / Math.pow(2.0, aZoomLevel) * 360.0 - 180;
    }

    /**
     * @param y a y tile -number
     * @param aZoomLevel a zoom-level of a tile
     * @return the latitude of the tile
     */
    private static double tile2lat(int y, int aZoomLevel) {
        double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, aZoomLevel);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    protected void onDraw(Canvas c, IMapView osmv) {
        if (!isVisible) {
            return;
        }
        BoundingBox viewBox = osmv.getViewBox();
        if (!myRendererInfo.covers(viewBox)) {
            if (!coverageWarningDisplayed) {
                coverageWarningDisplayed = true;
                Snack.toastTopWarning(myView.getContext(), coverageWarningMessage);
            }
            return; // no point, return immediately
        }
        coverageWarningDisplayed = false;

        long owner = (long) (Math.random() * Long.MAX_VALUE); // unique values so that we can track in the cache which
                                                              // invocation of onDraw the tile belongs too

        int maxZoom = myRendererInfo.getMaxZoomLevel();
        int minZoom = myRendererInfo.getMinZoomLevel();
        int maxOverZoom = myRendererInfo.getMaxOverZoom();

        // Do some calculations and drag attributes to local variables to save
        // some performance.
        final Rect viewPort = c.getClipBounds();
        final int zoomLevel = Math.min(osmv.getZoomLevel(), maxZoom); // clamp to max zoom here
        if (zoomLevel != prevZoomLevel && prevZoomLevel != -1) {
            mTileProvider.flushQueue(myRendererInfo.getId(), prevZoomLevel);
        }
        prevZoomLevel = zoomLevel;

        double lonOffset = 0d;
        double latOffset = 0d;
        Offset offset = myRendererInfo.getOffset(zoomLevel);
        if (offset != null) {
            lonOffset = offset.getDeltaLon();
            latOffset = offset.getDeltaLat();
        }

        final MapTile tile = new MapTile(myRendererInfo.getId(), 0, 0, 0); // reused instance of OpenStreetMapTile
        final MapTile originalTile = new MapTile(tile);
        //
        final double lonLeft = viewBox.getLeft() / 1E7d - (lonOffset > 0 ? lonOffset : 0d);
        final double lonRight = viewBox.getRight() / 1E7d - (lonOffset < 0 ? lonOffset : 0d);
        final double latTop = Math.toRadians(viewBox.getTop() / 1E7d - (latOffset < 0 ? latOffset : 0d));
        final double latBottom = Math.toRadians(viewBox.getBottom() / 1E7d - (latOffset > 0 ? latOffset : 0d));

        // pseudo-code for lon/lat to tile numbers
        // n = 2 ^ zoom
        // xtile = ((lon_deg + 180) / 360) * n
        // ytile = (1 - (log(tan(lat_rad) + sec(lat_rad)) / PI)) / 2 * n
        final double n = Math.pow(2d, zoomLevel);
        final int xTileLeft = (int) Math.floor(((lonLeft + 180d) / 360d) * n);
        final int xTileRight = (int) Math.floor(((lonRight + 180d) / 360d) * n);
        final int yTileTop = (int) Math.floor((1d - Math.log(Math.tan(latTop) + 1d / Math.cos(latTop)) / Math.PI) * n / 2d);
        final int yTileBottom = (int) Math.floor((1d - Math.log(Math.tan(latBottom) + 1d / Math.cos(latBottom)) / Math.PI) * n / 2d);

        final int tileNeededLeft = Math.min(xTileLeft, xTileRight);
        final int tileNeededRight = Math.max(xTileLeft, xTileRight);
        final int tileNeededTop = Math.min(yTileTop, yTileBottom);
        final int tileNeededBottom = Math.max(yTileTop, yTileBottom);

        final int mapTileMask = (1 << zoomLevel) - 1;

        Rect destRect = null; // destination rect for bit map
        int destIncX = 0;
        int destIncY = 0;
        int xPos = 0;
        int yPos = 0;

        boolean squareTiles = myRendererInfo.getTileWidth() == myRendererInfo.getTileHeight();
        // Draw all the MapTiles that intersect with the screen
        // y = y tile number (latitude)
        // int requiredTiles = (tileNeededBottom - tileNeededTop + 1) * (tileNeededRight - tileNeededLeft + 1);
        for (int y = tileNeededTop; y <= tileNeededBottom; y++) {
            // x = x tile number (longitude)
            for (int x = tileNeededLeft; x <= tileNeededRight; x++) {
                tile.reinit();
                // Set the specifications for the required tile
                tile.zoomLevel = zoomLevel;
                tile.x = x & mapTileMask;
                tile.y = y & mapTileMask;
                originalTile.zoomLevel = tile.zoomLevel;
                originalTile.x = tile.x;
                originalTile.y = tile.y;

                // destination rect
                if (destRect == null) { // avoid recalculating this for every tile
                    destRect = getScreenRectForTile(c, osmv, zoomLevel, y, x, squareTiles, lonOffset, latOffset);
                    destIncX = destRect.width();
                    destIncY = destRect.height();
                }

                // Set the size and top left corner on the source bitmap
                int sw = myRendererInfo.getTileWidth();
                int sh = myRendererInfo.getTileHeight();
                int tx = 0;
                int ty = 0;
                Bitmap tileBitmap = null;
                // only actually try to get tile if in range
                if (tile.zoomLevel >= minZoom && tile.zoomLevel <= maxZoom) {
                    tileBitmap = mTileProvider.getMapTile(tile, owner);
                }
                if (tileBitmap == null) {
                    // OVERZOOM
                    // Preferred tile is not available - request it
                    // mTileProvider.preCacheTile(tile); already done in getMapTile
                    // See if there are any alternative tiles available - try
                    // using larger tiles
                    // maximum maxOverZoom zoom levels up, with standard tiles this reduces the width to 64 bits
                    while ((tileBitmap == null) && (zoomLevel - tile.zoomLevel) <= maxOverZoom && tile.zoomLevel > minZoom) {
                        tile.reinit();
                        // As we zoom out to larger-scale tiles, we want to
                        // draw smaller and smaller sections of them
                        sw >>= 1; // smaller size
                        sh >>= 1;
                        tx >>= 1; // smaller offsets
                        ty >>= 1;
                        // select the correct quarter
                        if ((tile.x & 1) != 0) {
                            tx += (myRendererInfo.getTileWidth() >> 1);
                        }
                        if ((tile.y & 1) != 0) {
                            ty += (myRendererInfo.getTileHeight() >> 1);
                        }
                        // zoom out to next level
                        tile.x >>= 1;
                        tile.y >>= 1;
                        --tile.zoomLevel;
                        tileBitmap = mTileProvider.getMapTile(tile, owner);
                    }
                }

                if (tileBitmap != null) {
                    c.drawBitmap(tileBitmap, new Rect(tx, ty, tx + sw, ty + sh),
                            new Rect(destRect.left + xPos, destRect.top + yPos, destRect.right + xPos, destRect.bottom + yPos), mPaint);
                } else {
                    tile.reinit();
                    // Still no tile available - try smaller scale tiles
                    drawTile(c, osmv, 0, zoomLevel + 2, zoomLevel, x & mapTileMask, y & mapTileMask, squareTiles, lonOffset, latOffset);
                }
                xPos += destIncX;
            }
            xPos = 0;
            yPos += destIncY;
        }

        drawAttribution(c, osmv.getViewBox(), viewPort, zoomLevel);
    }

    /**
     * Display any necessary logos and attribution strings
     * 
     * @param c the Canvas we are drawing on
     * @param viewBox the ViewBox with the currently displayed area
     * @param viewPort the screen size in screen coordinates
     * @param zoomLevel the current tile zoom level
     */
    private void drawAttribution(Canvas c, ViewBox viewBox, final Rect viewPort, final int zoomLevel) {
        // Draw the tile layer branding logo (if it exists)
        resetAttributionArea(viewPort, attributionOffset);
        Drawable brandLogo = myRendererInfo.getLogoDrawable();
        int logoWidth = -1; // misuse this a flag
        int logoHeight = -1;
        if (brandLogo != null) {
            logoHeight = brandLogo.getIntrinsicHeight();
            logoWidth = brandLogo.getIntrinsicWidth();
            tapArea.top -= logoHeight;
            tapArea.right = Math.max(tapArea.right, tapArea.left + logoWidth);
            logoRect.left = 0;
            logoRect.top = tapArea.top;
            logoRect.right = logoWidth;
            logoRect.bottom = tapArea.top + logoHeight;
            brandLogo.setBounds(logoRect);
            brandLogo.draw(c);
        }
        // Draw the attributions (if any)
        for (String attr : myRendererInfo.getAttributions(zoomLevel, viewBox)) {
            float textSize = textPaint.getTextSize();
            if (logoWidth != -1) {
                c.drawText(attr, tapArea.left + (float) logoWidth, tapArea.top + (logoHeight + textSize) / 2F, textPaint);
                tapArea.right = Math.max(tapArea.right, tapArea.left + (int) textPaint.measureText(attr) + logoWidth);
                logoWidth = -1; // reset flag
            } else {
                c.drawText(attr, tapArea.left + (float) logoWidth, tapArea.top, textPaint);
                tapArea.top -= textSize;
                tapArea.right = Math.max(tapArea.right, tapArea.left + (int) textPaint.measureText(attr));
            }
        }
        // Impose a minimum tap area
        if (tapArea.width() < TAPAREA_MIN_WIDTH) {
            tapArea.right = tapArea.left + TAPAREA_MIN_WIDTH;
        }
        if (tapArea.height() < TAPAREA_MIN_HEIGHT) {
            tapArea.top = tapArea.bottom - TAPAREA_MIN_HEIGHT;
        }

        // set offset for potential next layer
        attributionOffset = viewPort.bottom - tapArea.top;
    }

    /**
     * Reset the attribution area for this layer to its initial values
     * 
     * @param viewPort the screen size in screen coordinates
     * @param bottomOffset the initial offset relative to the bottom of the screen
     */
    private void resetAttributionArea(Rect viewPort, int bottomOffset) {
        tapArea.left = 0;
        tapArea.right = 0;
        tapArea.top = viewPort.bottom - bottomOffset;
        tapArea.bottom = viewPort.bottom - bottomOffset;
    }

    /**
     * Recursively search the cache for smaller tiles to fill in the required space.
     * 
     * @param c Canvas to draw on.
     * @param osmv Map view area.
     * @param minz Minimum zoom level.
     * @param maxz Maximum zoom level to attempt - don't take too long searching.
     * @param z Zoom level to draw.
     * @param x Tile X to draw.
     * @param y Tile Y to draw.
     * @param squareTiles true if the tiles are square
     * @param lonOffset imagery longitude offset correction in WGS84
     * @param latOffset imagery latitude offset correction in WGS84
     * @return true if the space could be filled with tiles
     */
    private boolean drawTile(Canvas c, IMapView osmv, int minz, int maxz, int z, int x, int y, boolean squareTiles, double lonOffset, double latOffset) {
        final MapTile tile = new MapTile(myRendererInfo.getId(), z, x, y);
        Bitmap bitmap = mTileProvider.getMapTileFromCache(tile);
        if (bitmap != null) {
            c.drawBitmap(bitmap, new Rect(0, 0, myRendererInfo.getTileWidth(), myRendererInfo.getTileHeight()),
                    getScreenRectForTile(c, osmv, z, y, x, squareTiles, lonOffset, latOffset), mPaint);
            return true;
        } else {
            if (z < maxz && z < myRendererInfo.getMaxZoomLevel()) {
                // try smaller scale tiles
                x <<= 1;
                y <<= 1;
                ++z;
                boolean result = drawTile(c, osmv, z, maxz, z, x, y, squareTiles, lonOffset, latOffset);
                result = drawTile(c, osmv, z, maxz, z, x + 1, y, squareTiles, lonOffset, latOffset) && result;
                result = drawTile(c, osmv, z, maxz, z, x, y + 1, squareTiles, lonOffset, latOffset) && result;
                result = drawTile(c, osmv, z, maxz, z, x + 1, y + 1, squareTiles, lonOffset, latOffset) && result;
                return result;
            } else {
                // final fail
                return false;
            }
        }
    }

    /**
     * Gets a Rect for the area we are going to draw the tile into
     * 
     * FIXME don't allocate a new Rect
     * 
     * @param c the canvas we draw to (we need its clip-bound's width and height)
     * @param osmv the view with its viewBox
     * @param zoomLevel the zoom-level of the tile
     * @param y the y-number of the tile
     * @param x the x-number of the tile
     * @param squareTiles true if the tiles are square
     * @param lonOffset imagery longitude offset correction in WGS84
     * @param latOffset imagery latitude offset correction in WGS84
     * @return the rectangle of screen-coordinates it consumes.
     */
    private Rect getScreenRectForTile(Canvas c, IMapView osmv, final int zoomLevel, int y, int x, boolean squareTiles, double lonOffset, double latOffset) {

        double north = tile2lat(y, zoomLevel);
        // double south = tile2lat(y + 1, zoomLevel); only calculate when needed (aka non square tiles)
        double west = tile2lon(x, zoomLevel);
        double east = tile2lon(x + 1, zoomLevel);
        int w = c.getClipBounds().width();
        int h = c.getClipBounds().height();
        int screenLeft = (int) GeoMath.lonE7ToX(w, osmv.getViewBox(), (int) ((west + lonOffset) * 1E7));

        // calculate here to avoid rounding differences
        int tileWidth = 1 + (int) Math.floor((east - west) * 1E7D * w / osmv.getViewBox().getWidth());

        int screenTop = (int) GeoMath.latE7ToY(h, w, osmv.getViewBox(), (int) ((north + latOffset) * 1E7));
        int screenBottom = squareTiles ? screenTop + tileWidth
                : (int) GeoMath.latE7ToY(h, w, osmv.getViewBox(), (int) ((tile2lat(y + 1, zoomLevel) + latOffset) * 1E7));
        return new Rect(screenLeft, screenTop, screenLeft + tileWidth, screenBottom);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void onDrawFinished(Canvas c, IMapView osmv) {
        // unused
    }

    /**
     * Handle touch events in order to display tile layer End User Terms Of Use when the tile provider branding logo or
     * attributions are tapped.
     * 
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
            moved |= (Math.abs(event.getX() - downX) > 20 || Math.abs(event.getY() - downY) > 20);
            if (done && !moved && (tapArea != null) && tapArea.contains((int) event.getX(), (int) event.getY())) {
                String touUri = myRendererInfo.getTouUri();
                if (touUri != null) {
                    // Display the End User Terms Of Use (in the browser)
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(touUri));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    Context context = myView.getContext();
                    try {
                        context.startActivity(intent);
                    } catch (ActivityNotFoundException anfe) {
                        Log.e(DEBUG_TAG, "Activity not found " + anfe.getMessage());
                        Snack.toastTopError(context, anfe.getLocalizedMessage());
                    }
                    return true;
                }
            }
            break;
        default:
            // do nothing
        }
        return false;
    }

    /**
     * Invalidate myView when a new tile got downloaded.
     */
    private static class SimpleInvalidationHandler extends Handler {
        private View        v;
        private int         viewInvalidates = 0;
        private Handler     handler         = new Handler(Looper.getMainLooper());
        private Invalidator invalidator     = new Invalidator();

        /**
         * Handle success messages from downloaded tiles, and invalidate View v if necessary
         * 
         * @param v View to invalidate
         */
        public SimpleInvalidationHandler(@NonNull View v) {
            super(Looper.getMainLooper());
            this.v = v;
        }

        class Invalidator implements Runnable {
            @Override
            public void run() {
                viewInvalidates = 0;
                v.invalidate();
            }
        }

        @Override
        public void handleMessage(final Message msg) {
            if (msg.what == MapTile.MAPTILE_SUCCESS_ID) {
                if (viewInvalidates == 0) { // try to suppress inordinate number of invalidates
                    handler.postDelayed(invalidator, 100); // wait 1/10th of a second
                }
                viewInvalidates++;
            }
        }
    }

    @Override
    public String getName() {
        return myRendererInfo.getName();
    }

    @Override
    public void invalidate() {
        myView.invalidate();
    }

    @Override
    public BoundingBox getExtent() {
        if (myRendererInfo != null) {
            return myRendererInfo.getOverallCoverage();
        }
        return null;
    }

    /**
     * Get the list of most recently used tile server ids
     * 
     * @return a String array
     */
    @NonNull
    public String[] getMRU() {
        return lastServers.toArray(new String[lastServers.size()]);
    }

    /**
     * Remove an entry from the MRU
     * 
     * @param id the id of the layer
     */
    public void removeServerFromMRU(@NonNull String id) {
        lastServers.remove(id);
    }

    @Override
    public void onSaveState(@NonNull Context ctx) throws IOException {
        Log.d(DEBUG_TAG, "Saving MRU size " + lastServers.size());
        super.onSaveState(ctx);
        saved = true;
        mruSavingHelper.save(ctx, getClass().getName() + "lastServers", lastServers, true);
    }

    @Override
    public boolean onRestoreState(@NonNull Context ctx) {
        Log.d(DEBUG_TAG, "Restoring MRU");
        super.onRestoreState(ctx);
        if (saved) {
            MRUList<String> tempLastServers = mruSavingHelper.load(ctx, getClass().getName() + "lastServers", true);
            Log.d(DEBUG_TAG, "MRU size " + tempLastServers.size());
            lastServers.clear();
            lastServers.addAll(tempLastServers);
            lastServers.ensureCapacity(MRU_SIZE);
        }
        return true;
    }

    @Override
    public void showInfo(FragmentActivity activity) {
        LayerInfo f = new ImageryLayerInfo();
        f.setShowsDialog(true);
        Bundle args = new Bundle();
        args.putSerializable(ImageryLayerInfo.LAYER_KEY, myRendererInfo);
        f.setArguments(args);
        LayerInfo.showDialog(activity, f);
    }
}
