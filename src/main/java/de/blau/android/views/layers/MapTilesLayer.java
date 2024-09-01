package de.blau.android.views.layers;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.util.BitSet;
import java.util.Collection;
import java.util.Random;

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
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.dialogs.LayerInfo;
import de.blau.android.dialogs.Progress;
import de.blau.android.imageryoffset.Offset;
import de.blau.android.layer.AttributionInterface;
import de.blau.android.layer.DiscardInterface;
import de.blau.android.layer.ExtentInterface;
import de.blau.android.layer.LayerInfoInterface;
import de.blau.android.layer.LayerType;
import de.blau.android.layer.MapViewLayer;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.ViewBox;
import de.blau.android.resources.DataStyle;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.resources.TileLayerSource.TileType;
import de.blau.android.services.util.MapAsyncTileProvider;
import de.blau.android.services.util.MapTile;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.GeoMath;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.collections.MRUList;
import de.blau.android.views.IMapView;
import de.blau.android.views.util.MapTileProvider;
import de.blau.android.views.util.MapTileProvider.TileDecoder;

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
public class MapTilesLayer<T> extends MapViewLayer implements ExtentInterface, LayerInfoInterface, DiscardInterface, AttributionInterface {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, MapTilesLayer.class.getSimpleName().length());
    private static final String DEBUG_TAG = MapTilesLayer.class.getSimpleName().substring(0, TAG_LEN);

    /** Define a minimum active area for taps on the tile attribution data. */
    private static final int TAPAREA_MIN_WIDTH  = 40;
    private static final int TAPAREA_MIN_HEIGHT = 60;

    /**
     * 
     */
    private boolean coverageWarningDisplayed = false;
    private String  coverageWarningMessage;

    /** Tap tracking */
    private float      downX;
    private float      downY;
    private boolean    moved;
    private final Rect tapArea  = new Rect();
    private final Rect logoRect = new Rect();

    /**
     * The view we are a part of.
     */
    private final Map         map;
    /**
     * The tile-server to load a rendered map from.
     */
    protected TileLayerSource layerSource;

    /** Current renderer */
    protected final MapTileProvider<T> mTileProvider;
    private final Paint                mPaint    = new Paint();
    private Paint                      textPaint = new Paint();

    /**
     * MRU of last servers
     */
    protected static final int                  MRU_SIZE        = 5;
    private static final MRUList<String>        lastServers     = new MRUList<>(MRU_SIZE);
    private final SavingHelper<MRUList<String>> mruSavingHelper = new SavingHelper<>();

    private int prevZoomLevel = -1; // zoom level from previous draw

    private static final long TILE_ERROR_LIMIT = 50;
    private boolean           tileErrorShown   = false;
    private long              tileErrorCount   = 0;

    private final Context ctx;

    private final ViewBox viewBox = new ViewBox();

    private final Random random = new Random();

    private final TileRenderer<T> mTileRenderer;

    // avoid creating new Rects in onDraw
    private Rect       destRect = new Rect(); // destination rect for bit map
    private final Rect srcRect  = new Rect();
    private final Rect tempRect = new Rect();

    private final BitSet rendered = new BitSet();

    public interface TileRenderer<B> {

        /**
         * Render a tile
         * 
         * @param c the Canvas to render on to
         * @param tileBlob the tile
         * @param z current zoom level
         * @param fromRect source rect in the tile
         * @param screenRect destination rect on screen
         * @param paint a Paint object to use for rendering
         */
        void render(@NonNull Canvas c, @NonNull B tileBlob, int z, @Nullable Rect fromRect, @NonNull Rect screenRect, @NonNull Paint paint);

        /**
         * Get the tile decoder for this renderer
         * 
         * @param map the current map instance
         * @return a TileDecoder
         */
        @NonNull
        MapTileProvider.TileDecoder<B> decoder(@NonNull Map map);

        /**
         * Prepare for a rendering pass
         * 
         * @param c Canvas
         * @param z actual zoom level
         */
        default void preRender(@NonNull Canvas c, int z) {
            // do nothing
        }

        /**
         * Finalize a rendering pass
         * 
         * @param c Canvas
         * @param z actual zoom level
         */
        default void postRender(@NonNull Canvas c, int z) {
            // do nothing
        }
    }

    public static class BitmapTileRenderer implements TileRenderer<Bitmap> {
        private final boolean hardwareRenderer;

        /**
         * Construct a new renderer
         * 
         * @param hardwareRenderer decode bitmap for hardware rendering if true
         */
        public BitmapTileRenderer(boolean hardwareRenderer) {
            this.hardwareRenderer = hardwareRenderer;
        }

        @Override
        public void render(@NonNull Canvas c, @NonNull Bitmap tileBlob, int z, @Nullable Rect fromRect, @NonNull Rect screenRect, @NonNull Paint paint) {
            c.drawBitmap(tileBlob, fromRect, screenRect, paint);
        }

        @Override
        @NonNull
        public TileDecoder<Bitmap> decoder(Map map) {
            return new MapTileProvider.BitmapDecoder(hardwareRenderer);
        }
    }

    /**
     * Construct a new tile layer
     * 
     * @param map The view we are a part of.
     * @param aRendererInfo The tile-server to load a rendered map from.
     * @param aTileProvider the MapTileProvider if null a new one will be allocated
     * @param aTileRenderer the TileRender for this layer
     */
    public MapTilesLayer(@NonNull final Map map, @Nullable final TileLayerSource aRendererInfo, @Nullable final MapTileProvider<T> aTileProvider,
            @NonNull TileRenderer<T> aTileRenderer) {
        this.map = map;
        ctx = map.getContext();
        setRendererInfo(aRendererInfo);
        if (aTileProvider == null) {
            mTileProvider = new MapTileProvider<>(ctx, aTileRenderer.decoder(map), new SimpleInvalidationHandler(map));
        } else {
            mTileProvider = aTileProvider;
        }
        mTileRenderer = aTileRenderer;
        //
        textPaint = map.getDataStyle().getInternal(DataStyle.ATTRIBUTION_TEXT).getPaint();

        Log.d(DEBUG_TAG,
                aRendererInfo != null ? (aRendererInfo.isMetadataLoaded()
                        ? "provider " + aRendererInfo.getId() + " min zoom " + aRendererInfo.getMinZoomLevel() + " max " + aRendererInfo.getMaxZoomLevel()
                        : aRendererInfo.getId() + " is not ready yet") : "renderer is null");
    }

    @Override
    public boolean isReadyToDraw() {
        return layerSource != null && layerSource.isMetadataLoaded() && !TileLayerSource.LAYER_NONE.equals(layerSource.getId());
    }

    /**
     * Empty the cache for this layer
     * 
     * @param activity activity this was called from, if null don't display progress
     * @param all if true flush the on device cache too
     */
    public void flushTileCache(@Nullable final FragmentActivity activity, boolean all) {
        flushTileCache(activity, layerSource.getId(), all);
    }

    /**
     * Empty the cache
     * 
     * @param activity activity this was called from, if null don't display progress
     * @param renderer the renderer to delete the cache for or null for all
     * @param all if true flush the on device cache too
     */
    public void flushTileCache(@Nullable final FragmentActivity activity, final String renderer, boolean all) {
        Logic logic = App.getLogic();
        new ExecutorTask<Void, Void, Void>(logic.getExecutorService(), logic.getHandler()) {

            @Override
            protected void onPreExecute() {
                if (activity != null) {
                    Progress.showDialog(activity, Progress.PROGRESS_DELETING);
                }
            }

            @Override
            protected Void doInBackground(Void param) {
                if (mTileProvider != null) {
                    mTileProvider.flushCache(renderer, all);
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
     * @return a TileLayerSource object
     */
    @Nullable
    public TileLayerSource getTileLayerConfiguration() {
        return layerSource;
    }

    /**
     * Set the tile layer to display
     * 
     * Updates warning message if we are outside of coverage and flushes request queue
     * 
     * @param tileLayer layer to use
     */
    public void setRendererInfo(@Nullable final TileLayerSource tileLayer) {
        if (tileLayer == null) {
            Log.w(DEBUG_TAG, "Trying to set null layer");
            return;
        }
        Log.d(DEBUG_TAG, "setRendererInfo " + tileLayer.getId());
        if (layerSource != tileLayer) {
            try {
                coverageWarningMessage = map.getResources().getString(R.string.toast_no_coverage, tileLayer.getName());
            } catch (Exception ex) {
                coverageWarningMessage = "";
            }
            coverageWarningDisplayed = false;
            if (layerSource != null) { // 1st invocation this is null
                mTileProvider.flushQueue(layerSource.getId(), MapAsyncTileProvider.ALLZOOMS);
                synchronized (getLastServers()) {
                    getLastServers().push(layerSource.getId());
                }
            }
        }
        layerSource = tileLayer;
        // reset error counter
        tileErrorCount = 0;
        tileErrorShown = false;
    }

    /**
     * Get the current MapTileProvider
     * 
     * @return the current MapTileProvider
     */
    @NonNull
    public MapTileProvider<T> getTileProvider() {
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

        viewBox.set(osmv.getViewBox());
        if (!layerSource.covers(viewBox)) {
            if (!coverageWarningDisplayed) {
                coverageWarningDisplayed = true;
                ScreenMessage.toastTopWarning(ctx, coverageWarningMessage);
            }
            return; // no point, return immediately
        }
        coverageWarningDisplayed = false;

        if (tileErrorCount > TILE_ERROR_LIMIT && !tileErrorShown) {
            tileErrorShown = true;
            ScreenMessage.toastTopWarning(ctx, ctx.getString(R.string.toast_tile_layer_errors, layerSource.getName()));
        }

        long owner = random.nextLong(); // unique values so that we can track in the cache which
                                        // invocation of onDraw the tile belongs too

        int maxZoom = layerSource.getMaxZoomLevel();
        int minZoom = layerSource.getMinZoomLevel();
        int maxOverZoom = layerSource.getMaxOverZoom();

        // Do some calculations and drag attributes to local variables to save
        // some performance.
        final int actualZoomLevel = osmv.getZoomLevel();
        final int zoomLevel = Math.min(actualZoomLevel, maxZoom); // clamp to max zoom here
        if (zoomLevel != prevZoomLevel && prevZoomLevel != -1) {
            mTileProvider.flushQueue(layerSource.getId(), prevZoomLevel);
        }
        prevZoomLevel = zoomLevel;

        // any pre render pass setup
        mTileRenderer.preRender(c, actualZoomLevel);

        double lonOffset = 0d;
        double latOffset = 0d;
        Offset offset = layerSource.getOffset(zoomLevel);
        if (offset != null) {
            lonOffset = offset.getDeltaLon();
            latOffset = offset.getDeltaLat();
        }

        final MapTile tile = new MapTile(layerSource.getId(), 0, 0, 0);
        final MapTile originalTile = new MapTile(tile);

        // pseudo-code for lon/lat to tile numbers
        // n = 2 ^ zoom
        // xtile = ((lon_deg + 180) / 360) * n
        // ytile = (1 - (log(tan(lat_rad) + sec(lat_rad)) / PI)) / 2 * n
        final int n = 1 << zoomLevel;
        final int xTileLeft = tileLeft(viewBox.getLeft(), lonOffset, n);
        final int xTileRight = tileRight(viewBox.getRight(), lonOffset, n);
        final int yTileTop = tileTop(viewBox.getTop(), latOffset, n);
        final int yTileBottom = tileBottom(viewBox.getBottom(), latOffset, n);

        final int tileNeededLeft = Math.min(xTileLeft, xTileRight);
        final int tileNeededRight = Math.max(xTileLeft, xTileRight);
        final int tileNeededTop = Math.min(yTileTop, yTileBottom);
        final int tileNeededBottom = Math.max(yTileTop, yTileBottom);

        final int mapTileMask = (n) - 1;

        boolean firstIteration = true;
        int destIncX = 0;
        int destIncY = 0;
        int xPos = 0;
        int yPos = 0;

        final int width = c.getClipBounds().width();
        final int height = c.getClipBounds().height();
        boolean squareTiles = layerSource.getTileWidth() == layerSource.getTileHeight();
        // Draw all the MapTiles that intersect with the screen
        // y = y tile number (latitude)
        // requiredTiles = (tileNeededBottom - tileNeededTop + 1) * (tileNeededRight - tileNeededLeft + 1)
        int row = tileNeededRight - tileNeededLeft + 1;
        rendered.clear();
        for (int y = tileNeededTop; y <= tileNeededBottom; y++) {
            // x = x tile number (longitude)
            for (int x = tileNeededLeft; x <= tileNeededRight; x++) {
                if (rendered.get((y - tileNeededTop) * row + (x - tileNeededLeft))) {
                    continue;
                }
                tile.reinit();
                // Set the specifications for the required tile
                tile.zoomLevel = zoomLevel;
                tile.x = x & mapTileMask;
                tile.y = y & mapTileMask;
                originalTile.zoomLevel = tile.zoomLevel;
                originalTile.x = tile.x;
                originalTile.y = tile.y;

                // destination rect
                if (firstIteration) { // avoid recalculating this for every tile
                    destRect = getScreenRectForTile(destRect, width, height, osmv, zoomLevel, y, x, squareTiles, lonOffset, latOffset);
                    destIncX = destRect.width();
                    destIncY = destRect.height();
                    firstIteration = false;
                }

                // Set the size and top left corner on the source bitmap
                int sw = layerSource.getTileWidth();
                int sh = layerSource.getTileHeight();
                int tx = 0;
                int ty = 0;
                T tileBlob = null;
                // only actually try to get tile if in range
                if (tile.zoomLevel >= minZoom && tile.zoomLevel <= maxZoom) {
                    tileBlob = mTileProvider.getMapTile(tile, owner);
                }

                final boolean bitmapRenderer = layerSource.getTileType() == TileType.BITMAP;

                // OVERZOOM
                // Preferred tile is not available - request it
                // mTileProvider.preCacheTile(tile); already done in getMapTile
                // See if there are any alternative tiles available - try using larger tiles up to
                // maximum maxOverZoom zoom levels up, with standard tiles this reduces the width to 64 bits
                while (tileBlob == null && ((!bitmapRenderer && tile.zoomLevel > maxZoom) || (bitmapRenderer && (zoomLevel - tile.zoomLevel) <= maxOverZoom))
                        && tile.zoomLevel > minZoom) {
                    tile.reinit();
                    // As we zoom out to larger-scale tiles, we want to
                    // draw smaller and smaller sections of them
                    sw >>= 1; // smaller size
                    sh >>= 1;
                    tx >>= 1; // smaller offsets
                    ty >>= 1;
                    // select the correct quarter
                    if ((tile.x & 1) != 0) {
                        tx += (layerSource.getTileWidth() >> 1);
                    }
                    if ((tile.y & 1) != 0) {
                        ty += (layerSource.getTileHeight() >> 1);
                    }
                    // zoom out to next level
                    tile.x >>= 1;
                    tile.y >>= 1;
                    --tile.zoomLevel;
                    tileBlob = mTileProvider.getMapTile(tile, owner);
                }

                if (tileBlob != null) {
                    if (bitmapRenderer) {
                        srcRect.set(tx, ty, tx + sw, ty + sh);
                        tempRect.set(destRect.left + xPos, destRect.top + yPos, destRect.right + xPos, destRect.bottom + yPos);
                        mTileRenderer.render(c, tileBlob, zoomLevel, srcRect, tempRect, mPaint);
                    } else {
                        int zoomDiff = originalTile.zoomLevel - tile.zoomLevel;
                        mTileRenderer.render(c, tileBlob, actualZoomLevel, null,
                                getScreenRectForTile(tempRect, width, height, osmv, tile.zoomLevel, tile.y, tile.x, squareTiles, 0, 0), mPaint);
                        // mark tiles we've just rendered as done
                        for (int i = y; i <= Math.min(((tile.y + 1) << zoomDiff) - 1, tileNeededBottom); i++) {
                            for (int j = x; j <= Math.min(((tile.x + 1) << zoomDiff) - 1, tileNeededRight); j++) {
                                rendered.set((i - tileNeededTop) * row + (j - tileNeededLeft));
                            }
                        }
                    }
                } else if (bitmapRenderer) {
                    tile.reinit();
                    // Still no tile available - try smaller scale tiles
                    drawTile(c, osmv, Math.min(zoomLevel + 2, layerSource.getMaxZoomLevel()), zoomLevel, x & mapTileMask, y & mapTileMask, squareTiles,
                            lonOffset, latOffset);
                }
                xPos += destIncX;
            }
            xPos = 0;
            yPos += destIncY;
        }
        // any post render pass finalisation
        mTileRenderer.postRender(c, actualZoomLevel);
    }

    /**
     * Get the bottom most tile y coordinate
     * 
     * @param bottom bottom coordinate in WGS84*1E7
     * @param latOffset latitude offset in WGS84
     * @param n 1 << zoom
     * @return the bottom tile y coordinate
     */
    public static int tileBottom(int bottom, double latOffset, final int n) {
        return yTileNumber(Math.toRadians(bottom / 1E7d - (latOffset > 0 ? latOffset : 0d)), n);
    }

    /**
     * Get the top most tile y coordinate
     * 
     * @param top top coordinate in WGS84*1E7
     * @param latOffset latitude offset in WGS84
     * @param n 1 << zoom
     * @return the top tile y coordinate
     */
    public static int tileTop(int top, double latOffset, final int n) {
        return yTileNumber(Math.toRadians(top / 1E7d - (latOffset < 0 ? latOffset : 0d)), n);
    }

    /**
     * Get the right most tile x coordinate
     * 
     * @param right right coordinate in WGS84*1E7
     * @param lonOffset longitude offset in WGS84
     * @param n 1 << zoom
     * @return the right tile x coordinate
     */
    public static int tileRight(int right, double lonOffset, final int n) {
        return xTileNumber(right / 1E7d - (lonOffset < 0 ? lonOffset : 0d), n);
    }

    /**
     * Get the left most tile x coordinate
     * 
     * @param left left coordinate in WGS84*1E7
     * @param lonOffset longitude offset in WGS84
     * @param n 1 << zoom
     * @return the left tile x coordinate
     */
    public static int tileLeft(int left, double lonOffset, final int n) {
        return xTileNumber(left / 1E7d - (lonOffset > 0 ? lonOffset : 0d), n);
    }

    /**
     * Get the y tile number
     * 
     * @param lat the latitude
     * @param n 2^zoom
     * @return the tile number for the vertical coordinate
     */
    protected static int yTileNumber(final double lat, final int n) {
        return (int) Math.floor((1d - Math.log(Math.tan(lat) + 1d / Math.cos(lat)) / Math.PI) * n / 2d);
    }

    /**
     * Get the x tile number
     * 
     * @param lon the longitude
     * @param n 2^zoom
     * @return the tile number for the horizontal coordinate
     */
    protected static int xTileNumber(final double lon, final int n) {
        return (int) Math.floor(((lon + 180d) / 360d) * n);
    }

    @Override
    public int onDrawAttribution(@NonNull Canvas c, @NonNull IMapView osmv, int offset) {
        return layerSource.isMetadataLoaded() ? drawAttribution(c, osmv.getViewBox(), osmv.getZoomLevel(), offset) : offset;
    }

    /**
     * Display any necessary logos and attribution strings
     * 
     * @param c the Canvas we are drawing on
     * @param viewBox the ViewBox with the currently displayed area
     * @param zoomLevel the current tile zoom level
     * @param offset starting offset
     * @return next offset
     */
    private int drawAttribution(Canvas c, ViewBox viewBox, final int zoomLevel, final int offset) {
        final Rect viewPort = c.getClipBounds();
        Collection<String> attributions = layerSource.getAttributions(zoomLevel, viewBox);
        // Draw the tile layer branding logo (if it exists)
        resetAttributionArea(viewPort, offset);
        Drawable brandLogo = layerSource.getLogoDrawable();
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
        } else if (attributions.isEmpty()) {
            return offset; // nothing to display
        }
        // Draw the attributions (if any)
        for (String attr : attributions) {
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
        return viewPort.bottom - tapArea.top;
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
        tapArea.bottom = tapArea.top;
    }

    /**
     * Recursively search the cache for smaller tiles to fill in the required space.
     * 
     * @param c Canvas to draw on.
     * @param osmv Map view area.
     * @param maxz Maximum zoom level to attempt - don't take too long searching.
     * @param z Zoom level to draw.
     * @param x Tile X to draw.
     * @param y Tile Y to draw.
     * @param squareTiles true if the tiles are square
     * @param lonOffset imagery longitude offset correction in WGS84
     * @param latOffset imagery latitude offset correction in WGS84
     * @return true if the space could be filled with tiles
     */
    private boolean drawTile(@NonNull Canvas c, @NonNull IMapView osmv, int maxz, int z, int x, int y, boolean squareTiles, double lonOffset,
            double latOffset) {
        final MapTile tile = new MapTile(layerSource.getId(), z, x, y);
        T bitmap = mTileProvider.getMapTileFromCache(tile);
        if (bitmap != null) {
            mTileRenderer.render(c, bitmap, 0, new Rect(0, 0, layerSource.getTileWidth(), layerSource.getTileHeight()),
                    getScreenRectForTile(new Rect(), c.getClipBounds().width(), c.getClipBounds().height(), osmv, z, y, x, squareTiles, lonOffset, latOffset),
                    mPaint);
            return true;
        } else {
            if (z < maxz) {
                // try smaller scale tiles
                x <<= 1;
                y <<= 1;
                ++z;
                boolean result = drawTile(c, osmv, maxz, z, x, y, squareTiles, lonOffset, latOffset);
                result = drawTile(c, osmv, maxz, z, x + 1, y, squareTiles, lonOffset, latOffset) && result;
                result = drawTile(c, osmv, maxz, z, x, y + 1, squareTiles, lonOffset, latOffset) && result;
                result = drawTile(c, osmv, maxz, z, x + 1, y + 1, squareTiles, lonOffset, latOffset) && result;
                return result;
            }
            // final fail
            return false;
        }
    }

    /**
     * Gets a Rect for the area we are going to draw the tile into
     * 
     * @param rect the Rect to use
     * @param w width
     * @param h height
     * @param osmv the view with its viewBox
     * @param zoomLevel the zoom-level of the tile
     * @param y the y-number of the tile
     * @param x the x-number of the tile
     * @param squareTiles true if the tiles are square - ignored
     * @param lonOffset imagery longitude offset correction in WGS84
     * @param latOffset imagery latitude offset correction in WGS84
     * @return rect for convenience
     */
    protected Rect getScreenRectForTile(@NonNull Rect rect, int w, int h, @NonNull IMapView osmv, final int zoomLevel, int y, int x, boolean squareTiles,
            double lonOffset, double latOffset) {
        double north = tile2lat(y, zoomLevel);
        // double south = tile2lat(y + 1, zoomLevel); only calculate when needed (aka non square tiles)
        double west = tile2lon(x, zoomLevel);
        double east = tile2lon(x + 1, zoomLevel);
        int screenLeft = (int) GeoMath.lonE7ToX(w, viewBox, (int) ((west + lonOffset) * 1E7));
        int screenRight = (int) GeoMath.lonE7ToX(w, viewBox, (int) ((east + lonOffset) * 1E7));
        int screenTop = (int) GeoMath.latE7ToY(h, w, viewBox, (int) ((north + latOffset) * 1E7));
        int screenBottom = (int) GeoMath.latE7ToY(h, w, viewBox, (int) ((tile2lat(y + 1, zoomLevel) + latOffset) * 1E7));
        rect.set(screenLeft, screenTop, screenRight, screenBottom);
        return rect;
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
        float x = event.getX();
        float y = event.getY();
        if (tapArea.contains((int) x, (int) y)) {
            switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = x;
                downY = y;
                moved = false;
                return true;
            case MotionEvent.ACTION_UP:
                done = true;
                // FALL THROUGH
            case MotionEvent.ACTION_MOVE:
                moved |= (Math.abs(x - downX) > 20 || Math.abs(y - downY) > 20);
                if (done && !moved && displayAttribution()) {
                    return true;
                }
                break;
            default:
                // do nothing
            }
        }
        return false;
    }

    /**
     * Display attribution
     * 
     * @return true if an attribution uri was found
     */
    private boolean displayAttribution() {
        String attributionUri = layerSource.getTouUri();
        if (attributionUri == null) {
            attributionUri = layerSource.getAttributionUrl();
        }
        if (attributionUri != null) {
            // Display the End User Terms Of Use (in the browser)
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(attributionUri));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                ctx.startActivity(intent);
            } catch (ActivityNotFoundException anfe) {
                Log.e(DEBUG_TAG, "Activity not found " + anfe.getMessage());
                ScreenMessage.toastTopError(ctx, anfe.getLocalizedMessage() != null ? anfe.getLocalizedMessage() : anfe.getMessage());
            }
            return true;
        }
        return false;
    }

    /**
     * Invalidate myView when a new tile got downloaded.
     */
    private class SimpleInvalidationHandler extends Handler {
        private static final String DEBUG_TAG_1 = "SimpleInvalidationH...";

        private final View        v;
        private int               viewInvalidates = 0;
        private final Handler     handler         = new Handler(Looper.getMainLooper());
        private final Invalidator invalidator     = new Invalidator();

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
                return;
            } else if (msg.what == MapTile.MAPTILE_FAIL_ID) {
                switch (msg.arg1) {
                case MapAsyncTileProvider.RETRY:
                case MapAsyncTileProvider.IOERR:
                    MapTilesLayer.this.incErrorCount();
                    return;
                case MapAsyncTileProvider.NONETWORK:
                case MapAsyncTileProvider.DOESNOTEXIST:
                    return; // ignore
                default: // fall though to log
                }
            }
            Log.e(DEBUG_TAG_1, "Unknown message " + msg);
        }
    }

    @Override
    @NonNull
    public String getName() {
        return layerSource.getName();
    }

    /**
     * Increment the tile error count
     */
    public void incErrorCount() {
        tileErrorCount++;
    }

    @Override
    @Nullable
    public String getContentId() {
        if (layerSource != null) {
            return layerSource.getId();
        }
        return null;
    }

    @Override
    public void invalidate() {
        map.invalidate();
    }

    @Override
    @Nullable
    public BoundingBox getExtent() {
        if (layerSource != null) {
            return layerSource.getOverallCoverage();
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
        synchronized (getLastServers()) {
            return getLastServers().toArray(new String[getLastServers().size()]);
        }
    }

    /**
     * Remove an entry from the MRU
     * 
     * @param id the id of the layer
     */
    public void removeServerFromMRU(@NonNull String id) {
        synchronized (getLastServers()) {
            getLastServers().remove(id);
        }
    }

    @Override
    public void onSaveState(@NonNull Context ctx) throws IOException {
        Log.d(DEBUG_TAG, "Saving MRU size " + getLastServers().size());
        super.onSaveState(ctx);
        synchronized (getLastServers()) {
            mruSavingHelper.save(ctx, getClass().getName() + "lastServers", getLastServers(), true);
        }
    }

    @Override
    public boolean onRestoreState(@NonNull Context ctx) {
        Log.d(DEBUG_TAG, "Restoring MRU");
        super.onRestoreState(ctx);
        synchronized (getLastServers()) {
            MRUList<String> tempLastServers = mruSavingHelper.load(ctx, getClass().getName() + "lastServers", true);
            if (tempLastServers == null) {
                Log.w(DEBUG_TAG, "No MRU");
                return false;
            }
            Log.d(DEBUG_TAG, "read MRU size " + tempLastServers.size());
            getLastServers().ensureCapacity(MRU_SIZE);
            getLastServers().pushAll(tempLastServers);
        }
        return true;
    }

    @Override
    public void showInfo(@NonNull FragmentActivity activity) {
        LayerInfo f = new ImageryLayerInfo();
        f.setShowsDialog(true);
        Bundle args = new Bundle();
        args.putSerializable(ImageryLayerInfo.LAYER_KEY, layerSource);
        args.putSerializable(ImageryLayerInfo.ERROR_COUNT_KEY, tileErrorCount);
        f.setArguments(args);
        LayerInfo.showDialog(activity, f);
    }

    @Override
    @NonNull
    public LayerType getType() {
        return LayerType.IMAGERY;
    }

    @Override
    public void discard(@NonNull Context context) {
        onDestroy();
    }

    /**
     * Hack so that we can have two different MRUList singletons
     * 
     * @return the MRUList with the last used sources
     */
    MRUList<String> getLastServers() {
        return lastServers;
    }
}
