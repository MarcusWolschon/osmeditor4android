package de.blau.android;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.dialogs.Tip;
import de.blau.android.exception.OsmException;
import de.blau.android.imageryoffset.ImageryOffsetUtils;
import de.blau.android.imageryoffset.Offset;
import de.blau.android.layer.AttributionInterface;
import de.blau.android.layer.ClickableInterface;
import de.blau.android.layer.LayerConfig;
import de.blau.android.layer.LayerType;
import de.blau.android.layer.MapViewLayer;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.GeoPoint;
import de.blau.android.osm.GeoPoint.InterruptibleGeoPoint;
import de.blau.android.osm.Node;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.ViewBox;
import de.blau.android.osm.Way;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.DataStyle;
import de.blau.android.resources.DataStyle.FeatureStyle;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.resources.TileLayerSource.TileType;
import de.blau.android.services.TrackerService;
import de.blau.android.util.Density;
import de.blau.android.util.GeoMath;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.collections.FloatPrimitiveList;
import de.blau.android.util.mvt.VectorTileRenderer;
import de.blau.android.views.IMapView;
import de.blau.android.views.layers.MapTilesLayer;
import de.blau.android.views.layers.MapTilesOverlayLayer;

/**
 * Orchestrate layer drawing, configuration and associated rendering
 * 
 * @author mb
 * @author Marcus Wolschon &lt;Marcus@Wolschon.biz&gt;
 * @author Simon Poole
 */

public class Map extends View implements IMapView {

    private static final String DEBUG_TAG = Map.class.getSimpleName();

    public static final int ICON_SIZE_DP = 20;

    /**
     * zoom level from which on we display icons and house numbers
     */
    private static final int SHOW_ICONS_LIMIT = 15;

    public static final int SHOW_LABEL_LIMIT = SHOW_ICONS_LIMIT + 5;

    private static final int STORAGE_BOX_LIMIT = 10;

    private static final int INITIAL_ATTRIBUTION_OFFSET = 2;

    private static final long ONE_SECOND_IN_NS = 1000000000L;

    /** half the width/height of a node icon in px */
    private final int iconRadius;

    private final List<BoundingBox> boundingBoxes = new ArrayList<>();

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

    /**
     * The visible area in decimal-degree (WGS84) -space.
     */
    private ViewBox myViewBox;

    private ViewBox clipBox = new ViewBox(); // used for clipping

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

    private Context context;

    private Rect canvasBounds;

    /** Cached Paint objects */
    private Paint labelBackground;
    private Paint gpsPosFollowPaint;
    private Paint gpsPosPaint;
    private Paint gpsPosFollowPaintStale;
    private Paint gpsPosPaintStale;

    private Paint gpsAccuracyPaint;
    private Paint boxPaint;

    private long timeToStale = 60 * ONE_SECOND_IN_NS;

    private TrackerService tracker = null;

    /**
     * Construct a new Map object that orchestrates the layer drawing and related rendering
     * 
     * @param context an Android Context
     */
    @SuppressLint("NewApi")
    public Map(@NonNull final Context context) {
        super(context);
        this.context = context;

        canvasBounds = new Rect();

        setFocusable(true);
        setFocusableInTouchMode(true);

        // Style me
        setBackgroundColor(ContextCompat.getColor(context, R.color.ccc_white));
        setDrawingCacheEnabled(false);

        iconRadius = Density.dpToPx(context, ICON_SIZE_DP / 2);

        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    /**
     * Setup layers from layer db
     * 
     * @param ctx Android context
     */
    public void setUpLayers(@NonNull Context ctx) {
        List<MapViewLayer> tempLayers = new ArrayList<>();
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(ctx)) {
            final LayerConfig[] layerConfigs = db.getLayers();
            for (LayerConfig config : layerConfigs) {
                MapViewLayer layer = null;
                final String contentId = config.getContentId();
                final LayerType type = config.getType();
                // GPX and GEOJSON layers require a non-null content id
                if (contentId == null && (type == LayerType.GPX || type == LayerType.GEOJSON)) {
                    db.deleteLayer(type);
                    Log.w(DEBUG_TAG, "Removed " + type + " layer with null content id");
                    continue;
                }
                List<MapViewLayer> existingLayers = getLayers(type, contentId);
                if (existingLayers.isEmpty()) {
                    try {
                        switch (type) { // NOSONAR
                        case IMAGERY:
                            TileLayerSource backgroundSource = TileLayerSource.get(ctx, contentId, false);
                            if (backgroundSource != null) {
                                if (backgroundSource.getTileType() == TileType.MVT) {
                                    layer = new de.blau.android.layer.mvt.MapOverlay(this, new VectorTileRenderer(), false);
                                    ((MapTilesOverlayLayer<?>) layer).setRendererInfo(backgroundSource);
                                } else {
                                    layer = new MapTilesLayer<Bitmap>(this, backgroundSource, null, new MapTilesLayer.BitmapTileRenderer());
                                }
                            }
                            break;
                        case OVERLAYIMAGERY:
                            TileLayerSource overlaySource = TileLayerSource.get(ctx, contentId, false);
                            if (overlaySource != null) {
                                if (overlaySource.getTileType() == TileType.MVT) {
                                    layer = new de.blau.android.layer.mvt.MapOverlay(this, new VectorTileRenderer(), true);
                                } else {
                                    layer = new MapTilesOverlayLayer<Bitmap>(this, new MapTilesLayer.BitmapTileRenderer());
                                }
                                ((MapTilesOverlayLayer<?>) layer).setRendererInfo(overlaySource);
                            }
                            break;
                        case MVT:
                            // unused for now
                            break;
                        case PHOTO:
                            layer = new de.blau.android.layer.photos.MapOverlay(this);
                            break;
                        case SCALE:
                            layer = new de.blau.android.layer.grid.MapOverlay(this);
                            break;
                        case OSMDATA:
                            layer = new de.blau.android.layer.data.MapOverlay(this);
                            break;
                        case GPX:
                            layer = new de.blau.android.layer.gpx.MapOverlay(this, contentId); // NOSONAR
                            if (ctx.getString(R.string.layer_gpx_recording).equals(contentId)) {
                                ((de.blau.android.layer.gpx.MapOverlay) layer).setName(contentId);
                                if (getTracker() != null) {
                                    // if the tracker isn't running we can't do this, but the tracker will when
                                    // connected
                                    ((de.blau.android.layer.gpx.MapOverlay) layer).setTrack(getTracker().getTrack());
                                }
                            } else if (!((de.blau.android.layer.gpx.MapOverlay) layer).fromFile(ctx, Uri.parse(contentId))) {
                                db.deleteLayer(LayerType.GPX, contentId);
                                Log.w(DEBUG_TAG, "Deleted GPX layer for " + contentId);
                                continue; // skip
                            }
                            break;
                        case TASKS:
                            layer = new de.blau.android.layer.tasks.MapOverlay(this);
                            break;
                        case GEOJSON:
                            layer = new de.blau.android.layer.geojson.MapOverlay(this);
                            if (!((de.blau.android.layer.geojson.MapOverlay) layer).loadGeoJsonFile(ctx, Uri.parse(contentId), true)) {
                                // other error, has already been toasted
                                db.deleteLayer(LayerType.GEOJSON, contentId);
                                continue;
                            }
                            break;
                        case MAPILLARY:
                            layer = new de.blau.android.layer.mapillary.MapOverlay(this);
                            break;
                        }
                    } catch (SecurityException ex) {
                        Log.e(DEBUG_TAG, "SecurityException creating layer, content " + contentId + " " + ex.getMessage());
                    }
                } else {
                    layer = existingLayers.get(0);
                }
                if (layer != null) {
                    tempLayers.add(layer);
                    layer.setIndex(tempLayers.size() - 1);
                    layer.setVisible(config.isVisible());
                    if (LayerType.IMAGERY.equals(type) || LayerType.OVERLAYIMAGERY.equals(type)) {
                        ImageryOffsetUtils.applyImageryOffsets(ctx, prefs, ((MapTilesLayer<Bitmap>) layer).getTileLayerConfiguration(), getViewBox());
                    }
                }
            }
        }
        synchronized (mLayers) {
            // save, then destroy any unused layers
            for (MapViewLayer oldLayer : mLayers) {
                if (!tempLayers.contains(oldLayer)) {
                    saveLayerState(ctx, oldLayer);
                    oldLayer.onDestroy();
                }
            }
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
    private void saveLayerState(@NonNull Context ctx, @Nullable MapViewLayer layer) {
        Log.d(DEBUG_TAG, "saveLayerState");
        if (layer != null) {
            try {
                layer.onSaveState(ctx);
            } catch (IOException e) {
                Log.e(DEBUG_TAG, "Saving layer " + layer.getName() + " " + e.getMessage());
            }
        }
    }

    /**
     * Loop over all layers saving their state
     * 
     * @param ctx Android context
     */
    public void saveLayerState(@NonNull Context ctx) {
        Log.d(DEBUG_TAG, "saveLayerState (all)");
        for (MapViewLayer layer : getLayers()) {
            if (layer != null) {
                try {
                    Log.d(DEBUG_TAG, "saving " + layer.getName());
                    layer.onSaveState(ctx);
                } catch (IOException e) {
                    Log.e(DEBUG_TAG, "Saving layers " + e.getMessage());
                }
            }
        }
    }

    /**
     * Get layers of a specific type and potentially with a specific content
     * 
     * @param type the type
     * @param contentId the content id
     * @return a List of layers
     */
    @NonNull
    private List<MapViewLayer> getLayers(@NonNull LayerType type, @Nullable String contentId) {
        List<MapViewLayer> result = new ArrayList<>();
        synchronized (mLayers) {
            for (MapViewLayer l : mLayers) {
                if (l.getType().equals(type) && (contentId == null || contentId.equals(l.getContentId()))) {
                    result.add(l);
                }
            }
        }
        return result;
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
     * Get the top visible imagery layer for a type
     * 
     * @param type the type (typically LayerType.BACKGROUNDIMAGERY or OVERLAYIMAGERY)
     * @return the layer or null
     */
    @Nullable
    public MapTilesLayer<?> getTopImageryLayer(@NonNull LayerType type) {
        List<MapViewLayer> imageryLayers = getLayers(type, null);
        Collections.reverse(imageryLayers);
        for (MapViewLayer layer : imageryLayers) {
            if (layer.isVisible()) {
                return (MapTilesLayer<?>) layer;
            }
        }
        return null;
    }

    /**
     * Return the current (that is top visible) background layer
     * 
     * @return the current background layer or null
     */
    @Nullable
    public MapTilesLayer<?> getBackgroundLayer() {
        return getTopImageryLayer(LayerType.IMAGERY);
    }

    /**
     * Return the current overlay layer
     * 
     * @return the current overlay layer or null if none is configured
     */
    @Nullable
    public MapTilesOverlayLayer<?> getOverlayLayer() {
        return (MapTilesOverlayLayer<?>) getTopImageryLayer(LayerType.OVERLAYIMAGERY);
    }

    /**
     * Get a layer of a specific type
     * 
     * Assumes there is only one
     * 
     * @param type the LayerType
     * @return the layer or null
     */
    @Nullable
    public MapViewLayer getLayer(@NonNull LayerType type) {
        return getLayer(type, null);
    }

    /**
     * Get a layer of a specific type
     * 
     * Assumes there is only one
     * 
     * @param type the LayerType
     * @param contentId the id or null for any layer of the type
     * @return the layer or null
     */
    @Nullable
    public MapViewLayer getLayer(@NonNull LayerType type, @Nullable String contentId) {
        List<MapViewLayer> layers = getLayers(type, contentId);
        return layers.isEmpty() ? null : layers.get(0);
    }

    /**
     * Get a layer by id
     * 
     * @param contentId the id or null for any layer of the type
     * @return the layer or null
     */
    @Nullable
    public MapViewLayer getLayer(@NonNull String contentId) {
        List<MapViewLayer> layers = getLayers();
        for (MapViewLayer l : layers) {
            if (contentId.equals(l.getContentId())) {
                return l;
            }
        }
        return null;
    }

    /**
     * Return the current task layer
     * 
     * @return the current task layer or null if none is configured
     */
    @Nullable
    public de.blau.android.layer.tasks.MapOverlay getTaskLayer() {
        return (de.blau.android.layer.tasks.MapOverlay) getLayer(LayerType.TASKS);
    }

    /**
     * Return the current photo layer
     * 
     * @return the current photo layer or null if none is configured
     */
    @Nullable
    public de.blau.android.layer.photos.MapOverlay getPhotoLayer() {
        return (de.blau.android.layer.photos.MapOverlay) getLayer(LayerType.PHOTO);
    }

    /**
     * Return the current geojson layer
     * 
     * @return the current geojson layer or null if none is configured
     */
    @Nullable
    public de.blau.android.layer.geojson.MapOverlay getGeojsonLayer() {
        return (de.blau.android.layer.geojson.MapOverlay) getLayer(LayerType.GEOJSON);
    }

    /**
     * Return the current data layer
     * 
     * @return the current data layer or null if none is configured
     */
    @Nullable
    public de.blau.android.layer.data.MapOverlay getDataLayer() {
        return (de.blau.android.layer.data.MapOverlay) getLayer(LayerType.OSMDATA);
    }

    /**
     * Loop over the layers de-selecting any objects
     */
    public void deselectObjects() {
        for (MapViewLayer layer : getLayers()) {
            if (layer instanceof ClickableInterface) {
                ((ClickableInterface<?>) layer).deselectObjects();
            }
        }
    }

    /**
     * Call onDestroy on each active layer
     */
    public void onDestroy() {
        for (MapViewLayer osmvo : getLayers()) {
            if (osmvo != null) {
                osmvo.onDestroy();
            }
        }
    }

    /**
     * Call onLowMemory on each active layer
     */
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

        clipBox.set(myViewBox);
        clipBox.scale(1.1);

        final Logic logic = App.getLogic();
        final Mode tmpDrawingEditMode = logic.getMode();
        tmpLocked = logic.isLocked();

        // Draw our Overlays.
        canvas.getClipBounds(canvasBounds);

        synchronized (mLayers) {
            // use a copy here, avoids locking over a longer time
            // using a temp List avoids creating a new object
            renderLayers.clear();
            for (int i = mLayers.size() - 1; i >= 0; i--) {
                MapViewLayer layer = mLayers.get(i);
                renderLayers.add(layer);
                if (layer.isVisible() && LayerType.IMAGERY == layer.getType()) {
                    break; // nothing below this is visible
                }
            }
            Collections.reverse(renderLayers);
        }
        for (MapViewLayer osmvo : renderLayers) {
            osmvo.onManagedDraw(canvas, this);
        }
        int attributionOffset = INITIAL_ATTRIBUTION_OFFSET;
        for (MapViewLayer osmvo : renderLayers) {
            if (osmvo instanceof AttributionInterface && osmvo.isVisible()) {
                attributionOffset = ((AttributionInterface) osmvo).onDrawAttribution(canvas, this, attributionOffset);
            }
        }

        final boolean backgroundAlignMode = tmpDrawingEditMode == Mode.MODE_ALIGN_BACKGROUND;
        if (zoomLevel > STORAGE_BOX_LIMIT && !backgroundAlignMode) {
            // shallow copy to avoid modification issues
            boundingBoxes.clear();
            boundingBoxes.addAll(delegator.getBoundingBoxes());
            paintStorageBox(canvas, boundingBoxes);
        }
        paintGpsPos(canvas);
        if (App.getLogic().isInEditZoomRange()) {
            paintCrosshairs(canvas);
        }

        if (backgroundAlignMode) {
            paintZoomAndOffset(canvas);
        }

        if (prefs != null && prefs.isStatsVisible()) {
            time = System.currentTimeMillis() - time;
            paintStats(canvas, 1 / (time / 1000f));
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
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        for (MapViewLayer osmvo : getLayers()) {
            if (osmvo != null && osmvo.isVisible() && osmvo.onKeyDown(keyCode, event, this)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        for (MapViewLayer osmvo : getLayers()) {
            if (osmvo != null && osmvo.isVisible() && osmvo.onKeyUp(keyCode, event, this)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        for (MapViewLayer osmvo : getLayers()) {
            if (osmvo != null && osmvo.isVisible() && osmvo.onTrackballEvent(event, this)) {
                return true;
            }
        }
        return false;
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
    private static boolean hasFullClippingSupport(@NonNull Canvas c) {
        return !c.isHardwareAccelerated();
    }

    /**
     * Paint the position marker for example for creating new objects
     * 
     * Draws the marker twice with different Paaint objects to create a halo effect
     * 
     * @param canvas the Canvas to draw on
     */
    private void paintCrosshairs(@NonNull Canvas canvas) {
        //
        if (showCrosshairs) {
            float x = GeoMath.lonE7ToX(getWidth(), getViewBox(), crosshairsLon);
            float y = GeoMath.latE7ToY(getHeight(), getWidth(), getViewBox(), crosshairsLat);
            Paint paint = DataStyle.getInternal(DataStyle.CROSSHAIRS_HALO).getPaint();
            drawCrosshairs(canvas, x, y, paint);
            paint = DataStyle.getInternal(DataStyle.CROSSHAIRS).getPaint();
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
    private void drawCrosshairs(@NonNull Canvas canvas, float x, float y, @NonNull Paint paint) {
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
    private void paintGpsPos(@NonNull final Canvas canvas) {
        if (displayLocation == null) {
            return;
        }
        ViewBox viewBox = getViewBox();
        float x = GeoMath.lonE7ToX(getWidth(), viewBox, (int) (displayLocation.getLongitude() * 1E7));
        float y = GeoMath.latE7ToY(getHeight(), getWidth(), viewBox, (int) (displayLocation.getLatitude() * 1E7));

        float o = -1f;
        if (displayLocation.hasBearing() && displayLocation.getSpeed() > 1.4f) {
            // 1.4m/s ~= 5km/h ~= walking pace
            // faster than walking pace - use the GPS bearing
            o = displayLocation.getBearing();
        } else if (orientation >= 0) {
            // slower than walking pace - use the compass orientation (if available)
            o = orientation;
        }
        Paint paint = gpsPosFollowPaint;
        boolean stale = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1
                ? SystemClock.elapsedRealtimeNanos() - displayLocation.getElapsedRealtimeNanos()
                : 0) > timeToStale;
        if (isFollowingGPS) {
            if (stale) {
                paint = gpsPosFollowPaintStale;
            }
        } else {
            if (stale) {
                paint = gpsPosPaintStale;
            } else {
                paint = gpsPosPaint;
            }
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
            // note this assumes square pixels
            float accuracyInPixels = (float) (GeoMath.convertMetersToGeoDistance(displayLocation.getAccuracy()) * (getWidth() / (viewBox.getWidth() / 1E7D)));
            canvas.drawCircle(x, y, accuracyInPixels, gpsAccuracyPaint);
        }
    }

    /**
     * Show some statistics for debugging purposes
     * 
     * @param canvas canvas to draw on
     * @param fps frames per second
     */
    private void paintStats(@NonNull final Canvas canvas, final float fps) {
        int pos = 1;
        String text = "";
        Paint infotextPaint = DataStyle.getInternal(DataStyle.INFOTEXT).getPaint();
        float textSize = infotextPaint.getTextSize();

        BoundingBox viewBox = getViewBox();

        text = "viewBox: " + viewBox.toString();
        canvas.drawText(text, 5, getHeight() - textSize * pos++, infotextPaint);
        text = "Relations (current/API) :" + delegator.getCurrentStorage().getRelations().size() + "/" + delegator.getApiRelationCount();
        canvas.drawText(text, 5, getHeight() - textSize * pos++, infotextPaint);
        text = "Ways (current/API) :" + delegator.getCurrentStorage().getWays().size() + "/" + delegator.getApiWayCount();
        canvas.drawText(text, 5, getHeight() - textSize * pos++, infotextPaint);
        text = "Nodes (current/Waynodes/API) :" + delegator.getCurrentStorage().getNodes().size() + "/" + delegator.getCurrentStorage().getWayNodes().size()
                + "/" + delegator.getApiNodeCount();
        canvas.drawText(text, 5, getHeight() - textSize * pos++, infotextPaint);
        if (fps < 10) {
            text = "fps: " + String.format(Locale.US, "%.1f", fps);
        } else {
            text = "fps: " + (int) (fps);
        }
        canvas.drawText(text, 5, getHeight() - textSize * pos++, infotextPaint);
        text = "hardware acceleration: " + (canvas.isHardwareAccelerated() ? "on" : "off");
        canvas.drawText(text, 5, getHeight() - textSize * pos++, infotextPaint);
        text = "zoom level: " + zoomLevel;
        canvas.drawText(text, 5, getHeight() - textSize * pos, infotextPaint);
    }

    /**
     * Paint the current tile zoom level and offset ... very ugly used when adjusting the offset
     * 
     * @param canvas canvas to draw on
     */
    private void paintZoomAndOffset(@NonNull final Canvas canvas) {
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
    private void paintStorageBox(@NonNull final Canvas canvas, @NonNull List<BoundingBox> list) {
        if (!tmpLocked || alwaysDrawBoundingBoxes) {
            Canvas c = canvas;
            Bitmap b = null;
            // Clipping with Op.DIFFERENCE is not supported when a device uses hardware acceleration
            // drawing to a bitmap however will currently not be accelerated
            final boolean noFullClipping = !hasFullClippingSupport(canvas);
            if (noFullClipping) {
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
                if (bb != null && viewBox.intersects(bb)) { // only need to do this if we are on screen
                    float left = GeoMath.lonE7ToX(screenWidth, viewBox, bb.getLeft());
                    float right = GeoMath.lonE7ToX(screenWidth, viewBox, bb.getRight());
                    float bottom = GeoMath.latE7ToY(screenHeight, screenWidth, viewBox, bb.getBottom());
                    float top = GeoMath.latE7ToY(screenHeight, screenWidth, viewBox, bb.getTop());
                    RectF rect = new RectF(left, top, right, bottom);
                    rect.intersect(screen);
                    path.addRect(rect, Path.Direction.CW);
                }
            }

            c.clipPath(path, Region.Op.DIFFERENCE);
            c.drawRect(screen, boxPaint);

            if (noFullClipping) {
                canvas.drawBitmap(b, 0, 0, null); // NOSONAR
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
     *            {@link Canvas#drawLines(float[], Paint)}
     * @param nodes An iterable (e.g. List or array) with GeoPoints of the line that should be drawn (e.g. a Way or a
     *            GPS track)
     */
    public void pointListToLinePointsArray(@NonNull final FloatPrimitiveList points, @NonNull final List<? extends GeoPoint> nodes) {
        pointListToLinePointsArray(points, nodes, 0, nodes.size());
    }

    /**
     * Converts a geographical way/path/track to a list of screen-coordinate points for drawing.
     *
     * Only segments that are inside or overlap the ViewBox are included.
     *
     * @param points list to (re-)use for projected points in the format expected by
     *            {@link Canvas#drawLines(float[], Paint)}
     * @param nodes An iterable (e.g. List or array) with GeoPoints of the line that should be drawn (e.g. a Way or a
     *            GPS track)
     * @param nodesOffset begin in {@param nodes} list
     * @param nodesLength end in {@param nodes} list
     */
    public void pointListToLinePointsArray(@NonNull final FloatPrimitiveList points, @NonNull final List<? extends GeoPoint> nodes, int nodesOffset,
            int nodesLength) {
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
        if (nodesLength > 0) {
            int nodesSize = nodesOffset + nodesLength;
            GeoPoint nextNode = nodes.get(nodesOffset);
            int nextNodeLat = nextNode.getLat();
            int nextNodeLon = nextNode.getLon();
            float x;
            float y = -Float.MAX_VALUE;
            for (int i = nodesOffset; i < nodesSize; i++) {
                GeoPoint node = nextNode;
                int nodeLon = nextNodeLon;
                int nodeLat = nextNodeLat;
                boolean interrupted = false;
                if (i == nodesOffset) { // just do this once
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
                    nextIntersects = clipBox.isIntersectionPossible(nextNodeLon, nextNodeLat, nodeLon, nodeLat);
                } else {
                    nextNode = null;
                }
                x = -Float.MAX_VALUE; // misuse this as a flag
                if (!interrupted && prevNode != null) {
                    if (thisIntersects || nextIntersects || (!(nextNode != null && lastDrawnNode != null)
                            || clipBox.isIntersectionPossible(nextNodeLon, nextNodeLat, lastDrawnNodeLon, lastDrawnNodeLat))) {
                        x = GeoMath.lonE7ToX(w, box, nodeLon);
                        y = GeoMath.latE7ToY(h, w, box, nodeLat);
                        if (prevX == -Float.MAX_VALUE) { // last segment didn't intersect
                            prevX = GeoMath.lonE7ToX(w, box, prevNode.getLon());
                            prevY = GeoMath.latE7ToY(h, w, box, prevNode.getLat());
                        }
                        // Line segment needs to be drawn
                        points.add(prevX);
                        points.add(prevY);
                        points.add(x);
                        points.add(y);
                        lastDrawnNode = node;
                        lastDrawnNodeLat = nodeLat;
                        lastDrawnNodeLon = nodeLon;
                    }
                }
                prevNode = node;
                prevX = x;
                prevY = y;
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
    void setSelectedNodes(@Nullable final List<Node> aSelectedNodes) {
        de.blau.android.layer.data.MapOverlay dataLayer = getDataLayer();
        if (dataLayer != null) {
            dataLayer.setSelectedNodes(aSelectedNodes);
        }
    }

    /**
     * 
     * @param aSelectedWays the currently selected ways to edit.
     */
    void setSelectedWays(@Nullable final List<Way> aSelectedWays) {
        de.blau.android.layer.data.MapOverlay dataLayer = getDataLayer();
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
    public void setPrefs(@NonNull Context ctx, @NonNull final Preferences aPreference) {
        prefs = aPreference;
        TileLayerSource.setBlacklist(prefs.getServer().getCachedCapabilities().getImageryBlacklist());
        setUpLayers(ctx);
        alwaysDrawBoundingBoxes = prefs.getAlwaysDrawBoundingBoxes();
        timeToStale = prefs.getGnssTimeToStale() * ONE_SECOND_IN_NS;
        synchronized (mLayers) {
            for (MapViewLayer osmvo : mLayers) {
                if (osmvo != null) {
                    osmvo.setPrefs(aPreference);
                }
            }
        }
    }

    /**
     * Check for a overlay that we actually have to display
     * 
     * @param layerId the layer id
     * @return true if we should allocate a layer
     */
    public static boolean activeOverlay(@NonNull String layerId) {
        return !(TileLayerSource.LAYER_NONE.equals(layerId) || TileLayerSource.LAYER_NOOVERLAY.equals(layerId));
    }

    /**
     * Update some stylable attributes
     */
    public void updateStyle() {
        // changes when profile changes
        labelBackground = DataStyle.getInternal(DataStyle.LABELTEXT_BACKGROUND).getPaint();
        FeatureStyle fs = DataStyle.getInternal(DataStyle.LABELTEXT_NORMAL);
        textPaint = fs.getPaint();
        gpsPosFollowPaint = DataStyle.getInternal(DataStyle.GPS_POS_FOLLOW).getPaint();
        gpsPosPaint = DataStyle.getInternal(DataStyle.GPS_POS).getPaint();
        gpsPosFollowPaintStale = DataStyle.getInternal(DataStyle.GPS_POS_FOLLOW_STALE).getPaint();
        gpsPosPaintStale = DataStyle.getInternal(DataStyle.GPS_POS_STALE).getPaint();
        gpsAccuracyPaint = DataStyle.getInternal(DataStyle.GPS_ACCURACY).getPaint();
        boxPaint = DataStyle.getInternal(DataStyle.VIEWBOX).getPaint();
        for (MapViewLayer layer : getLayers(LayerType.OSMDATA, null)) {
            ((de.blau.android.layer.data.MapOverlay) layer).updateStyle();
        }
    }

    /**
     * Set the current orientation
     * 
     * @param orientation the orientation in degrees
     */
    void setOrientation(final float orientation) {
        this.orientation = orientation;
    }

    /**
     * Set the Location to display
     * 
     * @param location a Location object
     */
    void setLocation(@Nullable Location location) {
        displayLocation = location;
    }

    /**
     * Get the current Location displayed
     * 
     * @return a Location object or null if none set
     */
    @Nullable
    public Location getLocation() {
        return displayLocation;
    }

    /**
     * Set the current StorageDelegator instance
     * 
     * @param delegator the StorageDelegator
     */
    void setDelegator(@NonNull final StorageDelegator delegator) {
        this.delegator = delegator;
    }

    /**
     * Set the current ViewBox
     * 
     * @param viewBox the ViewBox the map currently displays
     */
    public void setViewBox(@NonNull final ViewBox viewBox) {
        myViewBox = viewBox;
        try {
            myViewBox.setRatio(this, (float) getWidth() / getHeight(), true);
        } catch (OsmException e) {
            Log.d(DEBUG_TAG, "setViewBox got " + e.getMessage());
        }
    }

    /**
     * Show a small crosshairs marker
     * 
     * @param x screen x coordinate
     * @param y screen y coordinate
     */
    public void showCrosshairs(float x, float y) {
        showCrosshairs = true;
        // store as lat lon for redraws on translation and zooming
        crosshairsLat = GeoMath.yToLatE7(getHeight(), getWidth(), getViewBox(), y);
        crosshairsLon = GeoMath.xToLonE7(getWidth(), getViewBox(), x);
    }

    /**
     * Stop showing a crosshairs marker
     */
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
    private int calcZoomLevel(@NonNull Canvas canvas) { // NOSONAR
        int tileWidth = TileLayerSource.DEFAULT_TILE_SIZE;
        int tileHeight = TileLayerSource.DEFAULT_TILE_SIZE;
        MapTilesLayer<?> tileLayer = getBackgroundLayer();
        if (tileLayer == null) {
            tileLayer = getOverlayLayer();
        }
        if (tileLayer != null) {
            TileLayerSource s = tileLayer.getTileLayerConfiguration();
            if (s != null && s.isMetadataLoaded()) {// protection on startup
                tileWidth = s.getTileWidth();
                tileHeight = s.getTileHeight();
            }
        }

        // Calculate lat/lon of view extents
        final double latBottom = getViewBox().getBottom() / 1E7D;
        final double lonRight = getViewBox().getRight() / 1E7D;
        final double latTop = getViewBox().getTop() / 1E7D;
        final double lonLeft = getViewBox().getLeft() / 1E7D;

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
        return (int) Math.floor(Math.max(0, Math.min(xZoom, yZoom)));
    }

    /**
     * Set the flag that determines if the arrow is just an outline or not
     * 
     * @param follow if true follow the GPS/Location position
     */
    public void setFollowGPS(boolean follow) {
        isFollowingGPS = follow;
    }

    /**
     * Return a list of the names of the currently used (and visible) layers
     * 
     * @return a List containing the currently in use imagery names
     */
    @NonNull
    public List<String> getImageryNames() {
        List<String> result = new ArrayList<>();
        List<MapViewLayer> imageryLayers = getLayers();
        Collections.reverse(imageryLayers);
        for (MapViewLayer osmvo : imageryLayers) {
            if (osmvo instanceof MapTilesLayer && osmvo.isVisible()) {
                TileLayerSource tileLayerConfiguration = ((MapTilesLayer<?>) osmvo).getTileLayerConfiguration();
                if (tileLayerConfiguration != null) {
                    result.add(tileLayerConfiguration.getName());
                    if (!tileLayerConfiguration.isOverlay()) {
                        // not an overlay -> not transparent so nothing below it is visible
                        break;
                    }
                }
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

    /**
     * Set the current TrackerService
     * 
     * @param tracker the current TrackerService or null
     */
    void setTracker(@Nullable TrackerService tracker) {
        this.tracker = tracker;
    }

    /**
     * Get the current TrackerSerice
     * 
     * The is only used by the GPS layer
     * 
     * @return an instance of TrackerService or null
     */
    @Nullable
    public TrackerService getTracker() {
        return this.tracker;
    }

    @Override
    public boolean showContextMenu() {
        if (context instanceof FragmentActivity) {
            Tip.showDialog((FragmentActivity) context, R.string.tip_disambiguation_menu_key, R.string.tip_disambiguation_menu);
        }
        return super.showContextMenu();
    }
}