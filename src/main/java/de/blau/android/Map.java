package de.blau.android;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import de.blau.android.exception.OsmException;
import de.blau.android.imageryoffset.ImageryOffsetUtils;
import de.blau.android.imageryoffset.Offset;
import de.blau.android.layer.MapViewLayer;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.GeoPoint;
import de.blau.android.osm.GeoPoint.InterruptibleGeoPoint;
import de.blau.android.osm.Node;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.ViewBox;
import de.blau.android.osm.Way;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.DataStyle;
import de.blau.android.resources.DataStyle.FeatureStyle;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.services.TrackerService;
import de.blau.android.util.Density;
import de.blau.android.util.GeoMath;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.collections.FloatPrimitiveList;
import de.blau.android.views.IMapView;
import de.blau.android.views.layers.MapTilesLayer;
import de.blau.android.views.layers.MapTilesOverlayLayer;

/**
 * Paints all data provided previously by {@link Logic}.<br/>
 * As well as a number of overlays. There is a default overlay that fetches rendered tiles from an OpenStreetMap-server.
 * 
 * @author mb
 * @author Marcus Wolschon <Marcus@Wolschon.biz>
 * @author Simon Poole
 */

public class Map extends View implements IMapView {

    private static final String DEBUG_TAG = Map.class.getSimpleName();

    public static final int ICON_SIZE_DP = 20;

    /** Use reflection to access Canvas method only available in API11. */
    private static final Method mIsHardwareAccelerated;

    /**
     * zoom level from which on we display icons and house numbers
     */
    private static final int SHOW_ICONS_LIMIT = 15;

    public static final int SHOW_LABEL_LIMIT = SHOW_ICONS_LIMIT + 5;

    /** half the width/height of a node icon in px */
    private final int iconRadius;

    private final ArrayList<BoundingBox> boundingBoxes = new ArrayList<>();

    private Preferences prefs;

    /** Direction we're pointing. 0-359 is valid, anything else is invalid. */
    private float orientation = -1f;

    /**
     * List of layers we are showing.
     * 
     * @see #getLayers()
     */
    private final List<MapViewLayer> mLayers      = new ArrayList<>();
    /**
     * Copy of the above for use during onDraw
     */
    private final List<MapViewLayer> renderLayers = new ArrayList<>();
    /* currently the layers setup is very static and we provide methods to access them */
    private MapTilesLayer                            backgroundLayer = null;
    private MapTilesOverlayLayer                     overlayLayer    = null;
    private de.blau.android.layer.photos.MapOverlay  photoLayer      = null;
    private de.blau.android.layer.tasks.MapOverlay   taskLayer       = null;
    private de.blau.android.layer.gpx.MapOverlay     gpxLayer        = null;
    private de.blau.android.layer.geojson.MapOverlay geojsonLayer    = null;
    private de.blau.android.layer.data.MapOverlay    dataLayer       = null;

    /**
     * The visible area in decimal-degree (WGS84) -space.
     */
    private ViewBox myViewBox;

    private StorageDelegator delegator;

    /**
     * Always darken non-downloaded areas
     */
    private boolean alwaysDrawBoundingBoxes = false;

    /**
     * Locked or not
     */
    private boolean tmpLocked;

    /** cached zoom level, calculated once per onDraw pass **/
    private int zoomLevel = 0;

    /**
     * We just need one path object
     */
    private Path path = new Path();

    private Location displayLocation = null;
    private boolean  isFollowingGPS  = false;

    private Paint textPaint;

    /**
     * support for display a crosshairs at a position
     */
    private boolean showCrosshairs = false;
    private int     crosshairsLat  = 0;
    private int     crosshairsLon  = 0;

    static {
        Method m;
        try {
            m = Canvas.class.getMethod("isHardwareAccelerated", (Class[]) null);
        } catch (NoSuchMethodException e) {
            m = null;
        }
        mIsHardwareAccelerated = m;
    }

    private Context context;

    private Rect canvasBounds;

    private Paint labelBackground;

    private TrackerService tracker = null;

    @SuppressLint("NewApi")
    public Map(final Context context) {
        super(context);
        this.context = context;

        canvasBounds = new Rect();

        setFocusable(true);
        setFocusableInTouchMode(true);

        // Style me
        setBackgroundColor(ContextCompat.getColor(context, R.color.ccc_white));
        setDrawingCacheEnabled(false);
        //
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        iconRadius = Density.dpToPx(ICON_SIZE_DP / 2);
    }

    /**
     * Setup layers from preferences
     * 
     * @param ctx Android context
     */
    public void setUpLayers(Context ctx) {
        List<MapViewLayer> tempLayers = new ArrayList<>();
        TileLayerServer backgroundTS = TileLayerServer.get(ctx, prefs.backgroundLayer(), true);
        if (backgroundTS == null) {
            backgroundTS = TileLayerServer.getDefault(ctx, true);
        }
        if (backgroundTS != null) {
            if (backgroundLayer != null) {
                backgroundLayer.setRendererInfo(backgroundTS);
                tempLayers.add(backgroundLayer);
                backgroundLayer.setIndex(tempLayers.size() - 1);
                ImageryOffsetUtils.applyImageryOffsets(ctx, backgroundTS, getViewBox());
            } else if (activeOverlay(backgroundTS.getId())) {
                backgroundLayer = new MapTilesLayer(this, backgroundTS, null);
                tempLayers.add(backgroundLayer);
                backgroundLayer.setIndex(tempLayers.size() - 1);
                ImageryOffsetUtils.applyImageryOffsets(ctx, backgroundTS, getViewBox());
                backgroundLayer.setContrast(prefs.getContrastValue());
            }
        }

        final TileLayerServer overlayTS = TileLayerServer.get(ctx, prefs.overlayLayer(), true);
        if (overlayTS != null) {
            if (overlayLayer != null) {
                if (activeOverlay(overlayTS.getId())) {
                    overlayLayer.setRendererInfo(overlayTS);
                    tempLayers.add(overlayLayer);
                    overlayLayer.setIndex(tempLayers.size() - 1);
                    ImageryOffsetUtils.applyImageryOffsets(ctx, overlayTS, getViewBox());
                } else {
                    overlayLayer = null; // entry removed in setUpLayers
                }
            } else if (activeOverlay(overlayTS.getId())) {
                overlayLayer = new MapTilesOverlayLayer(this);
                overlayLayer.setRendererInfo(overlayTS);
                tempLayers.add(overlayLayer);
                overlayLayer.setIndex(tempLayers.size() - 1);
                ImageryOffsetUtils.applyImageryOffsets(ctx, overlayTS, getViewBox());
            }
        }
        if (prefs.isPhotoLayerEnabled()) {
            if (photoLayer == null) {
                photoLayer = new de.blau.android.layer.photos.MapOverlay(this);
            }
            tempLayers.add(photoLayer);
            photoLayer.setIndex(tempLayers.size() - 1);
        } else {
            saveLayerState(ctx, photoLayer);
            photoLayer = null;
        }
        String[] scaleValues = ctx.getResources().getStringArray(R.array.scale_values);
        if (scaleValues != null && scaleValues.length > 0 && !scaleValues[0].contentEquals(prefs.scaleLayer())) {
            de.blau.android.layer.grid.MapOverlay grid = new de.blau.android.layer.grid.MapOverlay(this);
            tempLayers.add(grid);
            grid.setIndex(tempLayers.size() - 1);
        }
        if (dataLayer == null) {
            dataLayer = new de.blau.android.layer.data.MapOverlay(this);
        }
        tempLayers.add(dataLayer);
        dataLayer.setIndex(tempLayers.size() - 1);
        if (gpxLayer == null) {
            gpxLayer = new de.blau.android.layer.gpx.MapOverlay(this);
        }
        tempLayers.add(gpxLayer);
        gpxLayer.setIndex(tempLayers.size() - 1);
        if (prefs.areBugsEnabled()) {
            if (taskLayer == null) {
                taskLayer = new de.blau.android.layer.tasks.MapOverlay(this);
            }
            tempLayers.add(taskLayer);
            taskLayer.setIndex(mLayers.size() - 1);
        } else {
            saveLayerState(ctx, taskLayer);
            taskLayer = null;
        }
        if (geojsonLayer == null) {
            geojsonLayer = new de.blau.android.layer.geojson.MapOverlay(this);
        }
        tempLayers.add(geojsonLayer);
        geojsonLayer.setIndex(tempLayers.size() - 1);
        synchronized (mLayers) {
            mLayers.clear();
            mLayers.addAll(tempLayers);
        }
    }

    /**
     * Save the state of a single layer Used to save the state of layers that are being removed completely
     * 
     * @param ctx Android context
     * @param layer the layer to save the state of
     */
    private void saveLayerState(Context ctx, MapViewLayer layer) {
        Log.d(DEBUG_TAG, "saveLayerState");
        if (layer != null) {
            try {
                layer.onSaveState(ctx);
            } catch (IOException e) {
            }
        }
    }

    /**
     * Loop over all layers saving their state
     * 
     * @param ctx Android context
     */
    public void saveLayerState(Context ctx) {
        Log.d(DEBUG_TAG, "saveLayerState (all)");
        for (MapViewLayer layer : getLayers()) {
            if (layer != null) {
                try {
                    Log.d(DEBUG_TAG, "saving " + layer.getName());
                    layer.onSaveState(ctx);
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Get the list of configured layers
     * 
     * @return a shallow copy of the List of MapViewLayers
     */
    @NonNull
    public List<MapViewLayer> getLayers() {
        synchronized (mLayers) {
            return new ArrayList<>(mLayers);
        }
    }

    /**
     * Get a layer via its index
     * 
     * @param index the position of the layer
     * @return a MapViewLayer or null if index not valid
     */
    @Nullable
    public MapViewLayer getLayer(int index) {
        synchronized (mLayers) {
            if (index >= 0 && index < mLayers.size()) {
                return mLayers.get(index);
            }
            Log.e(DEBUG_TAG, "Layer for index " + index + " is null");
            return null;
        }
    }

    /**
     * Return the current background layer
     * 
     * @return the current background layer or null if none is configured
     */
    @Nullable
    public MapTilesLayer getBackgroundLayer() {
        return backgroundLayer;
    }

    /**
     * Return the current overlay layer
     * 
     * @return the current overlay layer or null if none is configured
     */
    @Nullable
    public MapTilesOverlayLayer getOverlayLayer() {
        return overlayLayer;
    }

    @Nullable
    public de.blau.android.layer.tasks.MapOverlay getTaskLayer() {
        return taskLayer;
    }

    @Nullable
    public de.blau.android.layer.geojson.MapOverlay getGeojsonLayer() {
        return geojsonLayer;
    }

    @Nullable
    public de.blau.android.layer.data.MapOverlay getDataLayer() {
        return dataLayer;
    }

    public void onDestroy() {
        for (MapViewLayer osmvo : getLayers()) {
            if (osmvo != null) {
                osmvo.onDestroy();
            }
        }
    }

    public void onLowMemory() {
        for (MapViewLayer osmvo : getLayers()) {
            if (osmvo != null) {
                osmvo.onLowMemory();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);
        long time = System.currentTimeMillis();

        zoomLevel = calcZoomLevel(canvas);

        // set in paintOsmData now tmpDrawingInEditRange = Main.logic.isInEditZoomRange();
        final Logic logic = App.getLogic();
        final Mode tmpDrawingEditMode = logic.getMode();
        tmpLocked = logic.isLocked();

        // Draw our Overlays.
        canvas.getClipBounds(canvasBounds);

        int attributionOffset = 2;

        synchronized (mLayers) {
            // use a copy here, avoids locking over a longer time
            // using a temp List avoids creating a new object
            renderLayers.clear();
            renderLayers.addAll(mLayers);
        }
        for (MapViewLayer osmvo : renderLayers) {
            osmvo.setAttributionOffset(attributionOffset);
            osmvo.onManagedDraw(canvas, this);
            attributionOffset = osmvo.getAttributionOffset();
        }

        if (zoomLevel > 10) {
            if (tmpDrawingEditMode != Mode.MODE_ALIGN_BACKGROUND) {
                // shallow copy to avoid modification issues
                boundingBoxes.clear();
                boundingBoxes.addAll(delegator.getBoundingBoxes());
                paintStorageBox(canvas, boundingBoxes);
            }
        }
        paintGpsPos(canvas);
        if (App.getLogic().isInEditZoomRange()) {
            paintCrosshairs(canvas);
        }

        if (tmpDrawingEditMode == Mode.MODE_ALIGN_BACKGROUND) {
            paintZoomAndOffset(canvas);
        }

        if (prefs.isStatsVisible()) {
            time = System.currentTimeMillis() - time;
            paintStats(canvas, (int) (1 / (time / 1000f)));
        }
    }

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        try {
            myViewBox.setRatio(this, (float) w / h, true);
        } catch (OsmException e) {
            Log.d(DEBUG_TAG, "onSizeChanged got " + e.getMessage());
        }
    }

    /* Overlay Event Forwarders */

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        for (MapViewLayer osmvo : getLayers()) {
            if (osmvo != null && osmvo.isVisible() && osmvo.onTouchEvent(event, this)) {
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        for (MapViewLayer osmvo : getLayers()) {
            if (osmvo != null && osmvo.isVisible() && osmvo.onKeyDown(keyCode, event, this)) {
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        for (MapViewLayer osmvo : getLayers()) {
            if (osmvo != null && osmvo.isVisible() && osmvo.onKeyUp(keyCode, event, this)) {
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        for (MapViewLayer osmvo : getLayers()) {
            if (osmvo != null && osmvo.isVisible() && osmvo.onTrackballEvent(event, this)) {
                return true;
            }
        }
        return super.onTrackballEvent(event);
    }

    /**
     * As of Android 4.0.4, clipping with Op.DIFFERENCE is not supported if hardware acceleration is used. (see
     * http://android-developers.blogspot.de/2011/03/android-30-hardware-acceleration.html) Op.DIFFERENCE and clipPath
     * supported as of 18
     * 
     * !!! FIXME Disable using HW clipping completely for now, see bug
     * https://github.com/MarcusWolschon/osmeditor4android/issues/307
     * 
     * @param c Canvas to check
     * @return true if the canvas supports proper clipping with Op.DIFFERENCE
     */
    private boolean hasFullClippingSupport(Canvas c) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB /* && Build.VERSION.SDK_INT < 18 */ && mIsHardwareAccelerated != null) {
            try {
                return !(Boolean) mIsHardwareAccelerated.invoke(c, (Object[]) null);
            } catch (IllegalArgumentException e) {
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
            }
        }
        // Older versions do not use hardware acceleration
        return true;
    }

    /**
     * Check if the canvas is hardware accelerated
     * 
     * Works on pre-API 11 devices too
     * 
     * @param c the Canvas
     * @return true is accelerated
     */
    public static boolean myIsHardwareAccelerated(Canvas c) {
        if (mIsHardwareAccelerated != null) {
            try {
                return (Boolean) mIsHardwareAccelerated.invoke(c, (Object[]) null);
            } catch (IllegalArgumentException e) {
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
            }
        }
        // Older versions do not use hardware acceleration
        return false;
    }

    /**
     * Paint the position marker for example for creating new objects
     * 
     * Draws the marker twice with different Paaint objects to create a halo effect
     * 
     * @param canvas the Canvas to draw on
     */
    private void paintCrosshairs(Canvas canvas) {
        //
        if (showCrosshairs) {
            float x = GeoMath.lonE7ToX(getWidth(), getViewBox(), crosshairsLon);
            float y = GeoMath.latE7ToY(getHeight(), getWidth(), getViewBox(), crosshairsLat);
            Paint paint = DataStyle.getCurrent(DataStyle.CROSSHAIRS_HALO).getPaint();
            drawCrosshairs(canvas, x, y, paint);
            paint = DataStyle.getCurrent(DataStyle.CROSSHAIRS).getPaint();
            drawCrosshairs(canvas, x, y, paint);
        }
    }

    /**
     * Draw a cross hair marker
     * 
     * @param canvas the Canvas to draw on
     * @param x screen x coordinate
     * @param y screen y coordinate
     * @param paint Paint object to use
     */
    private void drawCrosshairs(Canvas canvas, float x, float y, Paint paint) {
        canvas.save();
        canvas.translate(x, y);
        canvas.drawPath(DataStyle.getCurrent().getCrosshairsPath(), paint);
        canvas.restore();
    }

    /**
     * Show a marker for the current GPS position
     * 
     * @param canvas canvas to draw on
     */
    private void paintGpsPos(final Canvas canvas) {
        if (displayLocation == null) {
            return;
        }
        ViewBox viewBox = getViewBox();
        float x = GeoMath.lonE7ToX(getWidth(), viewBox, (int) (displayLocation.getLongitude() * 1E7));
        float y = GeoMath.latE7ToY(getHeight(), getWidth(), viewBox, (int) (displayLocation.getLatitude() * 1E7));

        float o = -1f;
        if (displayLocation.hasBearing() && displayLocation.hasSpeed() && displayLocation.getSpeed() > 1.4f) {
            // 1.4m/s ~= 5km/h ~= walking pace
            // faster than walking pace - use the GPS bearing
            o = displayLocation.getBearing();
        } else {
            // slower than walking pace - use the compass orientation (if available)
            if (orientation >= 0) {
                o = orientation;
            }
        }
        Paint paint = null;
        if (isFollowingGPS) {
            paint = DataStyle.getCurrent(DataStyle.GPS_POS_FOLLOW).getPaint();
        } else {
            paint = DataStyle.getCurrent(DataStyle.GPS_POS).getPaint();
        }

        if (o < 0) {
            // no orientation data available
            canvas.drawCircle(x, y, paint.getStrokeWidth(), paint);
        } else {
            // show the orientation using a pointy indicator
            canvas.save();
            canvas.translate(x, y);
            canvas.rotate(o);
            canvas.drawPath(DataStyle.getCurrent().getOrientationPath(), paint);
            canvas.restore();
        }
        if (displayLocation.hasAccuracy()) {
            // FIXME this assumes square pixels
            float accuracyInPixels = (float) (GeoMath.convertMetersToGeoDistance(displayLocation.getAccuracy())
                    * ((double) getWidth() / (viewBox.getWidth() / 1E7D)));
            RectF accuracyRect = new RectF(x - accuracyInPixels, y + accuracyInPixels, x + accuracyInPixels, y - accuracyInPixels);
            canvas.drawOval(accuracyRect, DataStyle.getCurrent(DataStyle.GPS_ACCURACY).getPaint());
        }
    }

    /**
     * Show some statistics for depugging purposes
     * 
     * @param canvas canvas to draw on
     * @param fps frames per second
     */
    private void paintStats(final Canvas canvas, final int fps) {
        int pos = 1;
        String text = "";
        Paint infotextPaint = DataStyle.getCurrent(DataStyle.INFOTEXT).getPaint();
        float textSize = infotextPaint.getTextSize();

        BoundingBox viewBox = getViewBox();

        text = "viewBox: " + viewBox.toString();
        canvas.drawText(text, 5, getHeight() - textSize * pos++, infotextPaint);
        text = "Relations (current/API) :" + delegator.getCurrentStorage().getRelations().size() + "/" + delegator.getApiRelationCount();
        canvas.drawText(text, 5, getHeight() - textSize * pos++, infotextPaint);
        text = "Ways (current/API) :" + delegator.getCurrentStorage().getWays().size() + "/" + delegator.getApiWayCount();
        canvas.drawText(text, 5, getHeight() - textSize * pos++, infotextPaint);
        text = "Nodes (current/Waynodes/API) :" + delegator.getCurrentStorage().getNodes().size() + "/" + delegator.getCurrentStorage().getWaynodes().size()
                + "/" + delegator.getApiNodeCount();
        canvas.drawText(text, 5, getHeight() - textSize * pos++, infotextPaint);
        text = "fps: " + fps;
        canvas.drawText(text, 5, getHeight() - textSize * pos++, infotextPaint);
        text = "hardware acceleration: " + (myIsHardwareAccelerated(canvas) ? "on" : "off");
        canvas.drawText(text, 5, getHeight() - textSize * pos++, infotextPaint);
        text = "zoom level: " + zoomLevel;
        canvas.drawText(text, 5, getHeight() - textSize * pos, infotextPaint);
    }

    /**
     * Paint the current tile zoom level and offset ... very ugly used when adjusting the offset
     * 
     * @param canvas canvas to draw on
     */
    private void paintZoomAndOffset(final Canvas canvas) {
        int pos = ThemeUtils.getActionBarHeight(context) + 5 + (int) de.blau.android.layer.grid.MapOverlay.LONGTICKS_DP * 3;
        Offset o = getBackgroundLayer().getTileLayerConfiguration().getOffset(zoomLevel);
        String text = context.getString(R.string.zoom_and_offset, zoomLevel, o != null ? String.format(Locale.US, "%.5f", o.getDeltaLon()) : "0.00000",
                o != null ? String.format(Locale.US, "%.5f", o.getDeltaLat()) : "0.00000");
        float textSize = textPaint.getTextSize();
        float textWidth = textPaint.measureText(text);
        FontMetrics fm = textPaint.getFontMetrics();
        float yOffset = pos + textSize;
        canvas.drawRect(5, yOffset + fm.bottom, 5 + textWidth, yOffset - textSize, labelBackground);
        canvas.drawText(text, 5, pos + textSize, textPaint);
    }

    /**
     * Dim everything that hasn't been downloaded
     * 
     * @param canvas the canvas we are drawing on
     * @param list list of bounding boxes that we've downloaded
     */
    private void paintStorageBox(final Canvas canvas, List<BoundingBox> list) {
        if (!tmpLocked || alwaysDrawBoundingBoxes) {
            Canvas c = canvas;
            Bitmap b = null;
            // Clipping with Op.DIFFERENCE is not supported when a device uses hardware acceleration
            // drawing to a bitmap however will currently not be accelerated
            if (!hasFullClippingSupport(canvas)) {
                b = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
                c = new Canvas(b);
            } else {
                c.save();
            }
            int screenWidth = getWidth();
            int screenHeight = getHeight();
            ViewBox viewBox = getViewBox();
            path.reset();
            RectF screen = new RectF(0, 0, getWidth(), getHeight());
            for (BoundingBox bb : list) {
                if (viewBox.intersects(bb)) { // only need to do this if we are on screen
                    float left = GeoMath.lonE7ToX(screenWidth, viewBox, bb.getLeft());
                    float right = GeoMath.lonE7ToX(screenWidth, viewBox, bb.getRight());
                    float bottom = GeoMath.latE7ToY(screenHeight, screenWidth, viewBox, bb.getBottom());
                    float top = GeoMath.latE7ToY(screenHeight, screenWidth, viewBox, bb.getTop());
                    RectF rect = new RectF(left, top, right, bottom);
                    rect.intersect(screen);
                    path.addRect(rect, Path.Direction.CW);
                }
            }

            Paint boxpaint = DataStyle.getCurrent(DataStyle.VIEWBOX).getPaint();
            c.clipPath(path, Region.Op.DIFFERENCE);
            c.drawRect(screen, boxpaint);

            if (!hasFullClippingSupport(canvas)) {
                canvas.drawBitmap(b, 0, 0, null);
            } else {
                c.restore();
            }
        }
    }

    static final Bitmap NOICON = Bitmap.createBitmap(2, 2, Config.ARGB_8888);

    /**
     * Converts a geographical way/path/track to a list of screen-coordinate points for drawing.
     * 
     * Only segments that are inside the ViewBox are included.
     * 
     * @param points list to (re-)use for projected points in the format expected by
     *            {@link Canvas#drawLines(float[], Paint)
     * @param nodes An iterable (e.g. List or array) with GeoPoints of the line that should be drawn (e.g. a Way or a
     *            GPS track)
     */
    public void pointListToLinePointsArray(@NonNull final FloatPrimitiveList points, @NonNull final List<? extends GeoPoint> nodes) {
        points.clear(); // reset
        boolean testInterrupted = false;
        // loop over all nodes
        GeoPoint prevNode = null;
        GeoPoint lastDrawnNode = null;
        int lastDrawnNodeLon = 0;
        int lastDrawnNodeLat = 0;
        float prevX = 0f;
        float prevY = 0f;
        ViewBox box = getViewBox();
        int w = getWidth();
        int h = getHeight();
        boolean thisIntersects = false;
        boolean nextIntersects = false;
        int nodesSize = nodes.size();
        if (nodesSize > 0) {
            GeoPoint nextNode = nodes.get(0);
            int nextNodeLat = nextNode.getLat();
            int nextNodeLon = nextNode.getLon();
            float X = -Float.MAX_VALUE;
            float Y = -Float.MAX_VALUE;
            for (int i = 0; i < nodesSize; i++) {
                GeoPoint node = nextNode;
                int nodeLon = nextNodeLon;
                int nodeLat = nextNodeLat;
                boolean interrupted = false;
                if (i == 0) { // just do this once
                    testInterrupted = node instanceof InterruptibleGeoPoint;
                }
                if (testInterrupted && node != null) {
                    interrupted = ((InterruptibleGeoPoint) node).isInterrupted();
                }
                nextIntersects = true;
                if (i < nodesSize - 1) {
                    nextNode = nodes.get(i + 1);
                    nextNodeLat = nextNode.getLat();
                    nextNodeLon = nextNode.getLon();
                    nextIntersects = box.isIntersectionPossible(nextNodeLon, nextNodeLat, nodeLon, nodeLat);
                } else {
                    nextNode = null;
                }
                X = -Float.MAX_VALUE; // misuse this as a flag
                if (!interrupted && prevNode != null) {
                    if (thisIntersects || nextIntersects || (!(nextNode != null && lastDrawnNode != null)
                            || box.isIntersectionPossible(nextNodeLon, nextNodeLat, lastDrawnNodeLon, lastDrawnNodeLat))) {
                        X = GeoMath.lonE7ToX(w, box, nodeLon);
                        Y = GeoMath.latE7ToY(h, w, box, nodeLat);
                        if (prevX == -Float.MAX_VALUE) { // last segment didn't intersect
                            prevX = GeoMath.lonE7ToX(w, box, prevNode.getLon());
                            prevY = GeoMath.latE7ToY(h, w, box, prevNode.getLat());
                        }
                        // Line segment needs to be drawn
                        points.add(prevX);
                        points.add(prevY);
                        points.add(X);
                        points.add(Y);
                        lastDrawnNode = node;
                        lastDrawnNodeLat = nodeLat;
                        lastDrawnNodeLon = nodeLon;
                    }
                }
                prevNode = node;
                prevX = X;
                prevY = Y;
                thisIntersects = nextIntersects;
            }
        }
    }

    /**
     * ${@inheritDoc}.
     */
    @Override
    public ViewBox getViewBox() {
        return myViewBox;
    }

    /**
     * @param aSelectedNodes the currently selected nodes to edit.
     */
    void setSelectedNodes(final List<Node> aSelectedNodes) {
        if (dataLayer != null) {
            dataLayer.setSelectedNodes(aSelectedNodes);
        }
    }

    /**
     * 
     * @param aSelectedWays the currently selected ways to edit.
     */
    void setSelectedWays(final List<Way> aSelectedWays) {
        if (dataLayer != null) {
            dataLayer.setSelectedWays(aSelectedWays);
        }
    }

    /**
     * Get our current Preferences object
     * 
     * @return a Preferences instance
     */
    public Preferences getPrefs() {
        return prefs;
    }

    /**
     * Set the current Preferences object and layers for this Map and any thing that needs changing
     * 
     * @param ctx Android Context
     * @param aPreference the new Preferences
     */
    public void setPrefs(Context ctx, final Preferences aPreference) {
        prefs = aPreference;
        TileLayerServer.setBlacklist(prefs.getServer().getCachedCapabilities().getImageryBlacklist());
        setUpLayers(ctx);
        alwaysDrawBoundingBoxes = prefs.getAlwaysDrawBoundingBoxes();
        if (dataLayer != null) {
            dataLayer.setPrefs(ctx, aPreference);
        }
    }

    /**
     * Check for a overlay that we actually have to display
     * 
     * @param layerId the layer id
     * @return true if we should allocate a layer
     */
    public static boolean activeOverlay(String layerId) {
        return !(TileLayerServer.LAYER_NONE.equals(layerId) || TileLayerServer.LAYER_NOOVERLAY.equals(layerId));
    }

    public void updateStyle() {
        // changes when profile changes
        labelBackground = DataStyle.getCurrent(DataStyle.LABELTEXT_BACKGROUND).getPaint();
        FeatureStyle fs = DataStyle.getCurrent(DataStyle.LABELTEXT_NORMAL);
        textPaint = fs.getPaint();
        if (dataLayer != null) {
            dataLayer.updateStyle();
        }
    }

    void setOrientation(final float orientation) {
        this.orientation = orientation;
    }

    void setLocation(Location location) {
        displayLocation = location;
    }

    void setDelegator(final StorageDelegator delegator) {
        this.delegator = delegator;
    }

    public void setViewBox(final ViewBox viewBox) {
        myViewBox = viewBox;
        try {
            myViewBox.setRatio(this, (float) getWidth() / getHeight(), false);
        } catch (OsmException e) {
            Log.d(DEBUG_TAG, "setViewBox got " + e.getMessage());
        }
    }

    public void showCrosshairs(float x, float y) {
        showCrosshairs = true;
        // store as lat lon for redraws on translation and zooming
        crosshairsLat = GeoMath.yToLatE7(getHeight(), getWidth(), getViewBox(), y);
        crosshairsLon = GeoMath.xToLonE7(getWidth(), getViewBox(), x);
    }

    public void hideCrosshairs() {
        showCrosshairs = false;
    }

    /**
     * ${@inheritDoc}.
     */
    @Override
    public int getZoomLevel() {
        return zoomLevel;
    }

    /**
     * This calculates the best tile zoom level to use (not the actual zoom level of the map!)
     * 
     * @param canvas Canvas we are drawing on
     * @return the tile zoom level
     */
    private int calcZoomLevel(Canvas canvas) {
        int tileWidth = TileLayerServer.DEFAULT_TILE_SIZE;
        int tileHeight = TileLayerServer.DEFAULT_TILE_SIZE;
        MapTilesLayer tileLayer = getBackgroundLayer();
        if (tileLayer == null) {
            tileLayer = getOverlayLayer();
        }
        if (tileLayer != null) {
            TileLayerServer s = tileLayer.getTileLayerConfiguration();
            if (s == null || !s.isMetadataLoaded()) {// protection on startup
                return 0;
            } else {
                tileWidth = s.getTileWidth();
                tileHeight = s.getTileHeight();
            }
        }

        // Calculate lat/lon of view extents
        final double latBottom = getViewBox().getBottom() / 1E7; // GeoMath.yToLatE7(viewPort.height(), getViewBox(),
                                                                 // viewPort.bottom) / 1E7d;
        final double lonRight = getViewBox().getRight() / 1E7; // GeoMath.xToLonE7(viewPort.width() , getViewBox(),
                                                               // viewPort.right ) / 1E7d;
        final double latTop = getViewBox().getTop() / 1E7; // GeoMath.yToLatE7(viewPort.height(), getViewBox(),
                                                           // viewPort.top ) / 1E7d;
        final double lonLeft = getViewBox().getLeft() / 1E7; // GeoMath.xToLonE7(viewPort.width() , getViewBox(),
                                                             // viewPort.left ) / 1E7d;

        // Calculate tile x/y scaled 0.0 to 1.0
        final double xTileRight = (lonRight + 180d) / 360d;
        final double xTileLeft = (lonLeft + 180d) / 360d;
        final double yTileBottom = (1d - Math.log(Math.tan(Math.toRadians(latBottom)) + 1d / Math.cos(Math.toRadians(latBottom))) / Math.PI) / 2d;
        final double yTileTop = (1d - Math.log(Math.tan(Math.toRadians(latTop)) + 1d / Math.cos(Math.toRadians(latTop))) / Math.PI) / 2d;

        // Calculate the ideal zoom to fit into the view
        final double xTiles = (canvas.getWidth() / (xTileRight - xTileLeft)) / tileWidth;
        final double yTiles = (canvas.getHeight() / (yTileBottom - yTileTop)) / tileHeight;
        final double xZoom = Math.log(xTiles) / Math.log(2d);
        final double yZoom = Math.log(yTiles) / Math.log(2d);

        // Zoom out to the next integer step
        int zoom = (int) Math.floor(Math.max(0, Math.min(xZoom, yZoom)));
        // zoom = Math.min(zoom, s.getMaxZoomLevel());

        return zoom;
    }

    public Location getLocation() {
        return displayLocation;
    }

    /**
     * Set the flag that determines if the arror is just an outline or not
     * 
     * @param follow if true follow the GPS/Location position
     */
    public void setFollowGPS(boolean follow) {
        isFollowingGPS = follow;
    }

    /**
     * Return a list of the names of the currently used layers
     * 
     * @return a List containg the currently in use imagery names
     */
    public List<String> getImageryNames() {
        List<String> result = new ArrayList<>();
        for (MapViewLayer osmvo : getLayers()) {
            if (osmvo instanceof MapTilesLayer) {
                result.add(((MapTilesLayer) osmvo).getTileLayerConfiguration().getName());
            }
        }
        return result;
    }

    /**
     * @return the iconRadius
     */
    public int getIconRadius() {
        return iconRadius;
    }

    void setTracker(TrackerService tracker) {
        this.tracker = tracker;
    }

    public TrackerService getTracker() {
        return this.tracker;
    }
}
