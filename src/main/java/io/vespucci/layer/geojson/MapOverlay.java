package io.vespucci.layer.geojson;

import static io.vespucci.contract.Constants.LOG_TAG_LEN;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mapbox.geojson.CoordinateContainer;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.GeometryCollection;
import com.mapbox.geojson.MultiPolygon;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.turf.TurfException;
import com.mapbox.turf.TurfJoins;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Path;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.Logic;
import io.vespucci.Map;
import io.vespucci.contract.FileExtensions;
import io.vespucci.dialogs.FeatureInfo;
import io.vespucci.dialogs.LayerInfo;
import io.vespucci.layer.ClickableInterface;
import io.vespucci.layer.ExtentInterface;
import io.vespucci.layer.LabelMinZoomInterface;
import io.vespucci.layer.LayerInfoInterface;
import io.vespucci.layer.LayerType;
import io.vespucci.layer.StyleableFileLayer;
import io.vespucci.layer.StyleableLayer;
import io.vespucci.osm.BoundingBox;
import io.vespucci.osm.Server;
import io.vespucci.osm.ViewBox;
import io.vespucci.prefs.Preferences;
import io.vespucci.resources.DataStyle;
import io.vespucci.resources.DataStyle.FeatureStyle;
import io.vespucci.resources.symbols.TriangleDown;
import io.vespucci.util.ColorUtil;
import io.vespucci.util.ContentResolverUtil;
import io.vespucci.util.Coordinates;
import io.vespucci.util.ExecutorTask;
import io.vespucci.util.FileUtil;
import io.vespucci.util.GeoJSONConstants;
import io.vespucci.util.GeoJson;
import io.vespucci.util.GeoMath;
import io.vespucci.util.SavingHelper;
import io.vespucci.util.ScreenMessage;
import io.vespucci.util.SerializableTextPaint;
import io.vespucci.util.Util;
import io.vespucci.util.collections.FloatPrimitiveList;
import io.vespucci.util.rtree.BoundedObject;
import io.vespucci.util.rtree.RTree;
import io.vespucci.views.IMapView;

public class MapOverlay extends StyleableFileLayer
        implements Serializable, ExtentInterface, ClickableInterface<Feature>, LayerInfoInterface, LabelMinZoomInterface {

    private static final long serialVersionUID = 5L;

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, MapOverlay.class.getSimpleName().length());
    private static final String DEBUG_TAG = MapOverlay.class.getSimpleName().substring(0, TAG_LEN);

    public static final String FILENAME = "geojson" + "." + FileExtensions.RES;

    private transient SavingHelper<MapOverlay> savingHelper = new SavingHelper<>();

    /**
     * Wrapper around mapboxes Feature class makes the object serializable and usable in an RTree
     * 
     * @author Simon Poole
     *
     */
    class BoundedFeature implements BoundedObject, Serializable {
        private static final long serialVersionUID = 1;

        Feature     feature = null;
        BoundingBox box     = null;

        /**
         * Constructor
         * 
         * @param f the Feature to wrap
         */
        public BoundedFeature(@Nullable Feature f) {
            this.feature = f;
        }

        @Override
        public BoundingBox getBounds() {
            if (box == null) {
                JsonArray bbox = (JsonArray) feature.properties().get(GeoJSONConstants.BBOX);
                if (bbox == null || bbox.size() != 4) {
                    box = GeoJson.getBounds(feature.geometry());
                } else { // the geojson contains a bbox, use that
                    box = new BoundingBox(bbox.get(0).getAsDouble(), bbox.get(1).getAsDouble(), bbox.get(2).getAsDouble(), bbox.get(3).getAsDouble());
                }
            }
            return box;
        }

        /**
         * Get the wrapped Feature object
         * 
         * @return the Feature
         */
        @Nullable
        public Feature getFeature() {
            return feature;
        }

        /**
         * Serialize this object
         * 
         * @param out ObjectOutputStream to write to
         * @throws IOException if writing failes
         */
        private void writeObject(java.io.ObjectOutputStream out) throws IOException {
            out.writeUTF(feature.toJson());
            out.writeObject(box);
        }

        /**
         * Recreate the object for serialized state
         * 
         * @param in ObjectInputStream to write from
         * @throws IOException if reading failes
         * @throws ClassNotFoundException the target Class isn't defined
         */
        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
            String jsonString = in.readUTF();
            feature = Feature.fromJson(jsonString);
            box = (BoundingBox) in.readObject();
        }
    }

    private RTree<BoundedFeature>                data;
    private final transient Path                 path                  = new Path();
    private transient FloatPrimitiveList         points                = new FloatPrimitiveList(FloatPrimitiveList.MEDIUM_DEFAULT);
    private transient Collection<BoundedFeature> queryForDisplayResult = new ArrayList<>();
    private final transient Coordinates          centroid              = new Coordinates(0, 0);

    /**
     * Styling parameters
     */
    private String labelKey;
    private int    labelMinZoom;

    transient Paint        labelPaint;
    transient Paint        labelBackground;
    transient float        labelStrokeWidth;
    transient FeatureStyle labelFs;

    /**
     * The uri for the layer source
     */
    private String uri;

    /**
     * Construct this layer
     * 
     * @param map the Map object we are displayed on
     * @param contentId the id for the current contents
     */
    public MapOverlay(@NonNull final Map map, @NonNull String contentId) {
        super(contentId, FILENAME);
        this.map = map;
        final Preferences prefs = map.getPrefs();
        initStyling(!hasStateFile(map.getContext()), prefs.getGeoJsonStrokeWidth(), prefs.getGeoJsonLabelSource(), prefs.getGeoJsonLabelMinZoom(),
                prefs.getGeoJsonSynbol());
        paint.setColor(ColorUtil.generateColor(map.getLayerTypeCount(LayerType.GEOJSON), 9,
                map.getDataStyle().getInternal(DataStyle.GEOJSON_DEFAULT).getPaint().getColor()));
    }

    @Override
    public boolean isReadyToDraw() {
        return data != null;
    }

    @Override
    protected void onDraw(Canvas canvas, IMapView osmv) {
        if (!isVisible || data == null) {
            return;
        }
        ViewBox bb = osmv.getViewBox();
        int width = map.getWidth();
        int height = map.getHeight();
        int zoomLevel = map.getZoomLevel();
        DataStyle styles = map.getDataStyle();
        labelFs = styles.getInternal(DataStyle.LABELTEXT_NORMAL);
        labelPaint = labelFs.getPaint();
        labelBackground = styles.getInternal(DataStyle.LABELTEXT_BACKGROUND).getPaint();
        labelStrokeWidth = labelPaint.getStrokeWidth();

        queryForDisplayResult.clear();
        data.query(queryForDisplayResult, bb);
        Log.d(DEBUG_TAG, "features result count " + queryForDisplayResult.size());
        for (BoundedFeature bf : queryForDisplayResult) {
            drawGeometry(canvas, bb, width, height, zoomLevel, bf.getFeature());
        }
    }

    /**
     * Draw a GeoJSON geometry
     * 
     * @param canvas Canvas object we are drawing on
     * @param bb the current ViewBox
     * @param width screen width in screen coordinates
     * @param height screen height in screen coordinates
     * @param zoomLevel current zoom level
     * @param f the Feature object to draw
     */
    public void drawGeometry(@NonNull Canvas canvas, @NonNull ViewBox bb, int width, int height, int zoomLevel, @NonNull Feature f) {
        Geometry g = f.geometry();
        if (g == null) {
            return;
        }
        String label = null;
        if (zoomLevel >= labelMinZoom) {
            label = getLabel(f);
        }
        switch (g.type()) {
        case GeoJSONConstants.POINT:
            paint.setStyle(Paint.Style.STROKE);
            drawPoint(canvas, bb, width, height, (Point) g, paint, label);
            break;
        case GeoJSONConstants.MULTIPOINT:
            @SuppressWarnings("unchecked")
            List<Point> pointList = ((CoordinateContainer<List<Point>>) g).coordinates();
            paint.setStyle(Paint.Style.STROKE);
            for (Point q : pointList) {
                drawPoint(canvas, bb, width, height, q, paint, label);
            }
            break;
        case GeoJSONConstants.LINESTRING:
            @SuppressWarnings("unchecked")
            List<Point> line = ((CoordinateContainer<List<Point>>) g).coordinates();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.STROKE);
            drawLine(canvas, bb, width, height, line, paint, label);
            break;
        case GeoJSONConstants.MULTILINESTRING:
            @SuppressWarnings("unchecked")
            List<List<Point>> lines = ((CoordinateContainer<List<List<Point>>>) g).coordinates();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.STROKE);
            for (List<Point> l : lines) {
                drawLine(canvas, bb, width, height, l, paint, label);
            }
            break;
        case GeoJSONConstants.POLYGON:
            @SuppressWarnings("unchecked")
            List<List<Point>> rings = ((CoordinateContainer<List<List<Point>>>) g).coordinates();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            drawPolygon(canvas, bb, width, height, rings, paint, label);
            break;
        case GeoJSONConstants.MULTIPOLYGON:
            @SuppressWarnings("unchecked")
            List<List<List<Point>>> polygons = ((CoordinateContainer<List<List<List<Point>>>>) g).coordinates();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            for (List<List<Point>> polygon : polygons) {
                drawPolygon(canvas, bb, width, height, polygon, paint, label);
            }
            break;
        case GeoJSONConstants.GEOMETRYCOLLECTION:
            List<Geometry> geometries = ((GeometryCollection) g).geometries();
            for (Geometry geometry : geometries) {
                drawGeometry(canvas, bb, width, height, zoomLevel, Feature.fromGeometry(geometry));
            }
            break;
        default:
            Log.e(DEBUG_TAG, "drawGeometry unknown GeoJSON geometry " + g.type());
        }
    }

    /**
     * Draw a marker
     * 
     * @param canvas Canvas object we are drawing on
     * @param bb the current ViewBox
     * @param width screen width in screen coordinates
     * @param height screen height in screen coordinates
     * @param p the Position of the marker
     * @param paint Paint object for drawing
     * @param label label to display, null if none
     */
    public void drawPoint(@NonNull Canvas canvas, @NonNull ViewBox bb, int width, int height, @NonNull Point p, @NonNull Paint paint, @Nullable String label) {
        double lon = p.longitude();
        double lat = p.latitude();
        if (bb.contains(lon, lat)) {
            float x = GeoMath.lonToX(width, bb, p.longitude());
            float y = GeoMath.latToY(height, width, bb, p.latitude());
            canvas.save();
            canvas.translate(x, y);
            canvas.drawPath(symbolPath, paint);
            canvas.restore();
            if (label != null) {
                float yOffset = 2 * labelStrokeWidth + iconRadius;
                float halfTextWidth = labelPaint.measureText(label) / 2;
                FontMetrics fm = labelFs.getFontMetrics();
                canvas.drawRect(x - halfTextWidth, y + yOffset + fm.bottom, x + halfTextWidth, y + yOffset - labelPaint.getTextSize() + fm.bottom,
                        labelBackground);
                canvas.drawText(label, x - halfTextWidth, y + yOffset, labelPaint);
            }
        }
    }

    /**
     * Draw a line
     * 
     * @param canvas Canvas object we are drawing on
     * @param bb the current ViewBox
     * @param width screen width in screen coordinates
     * @param height screen height in screen coordinates
     * @param line List of Position objects defining the line to draw
     * @param paint Paint object for drawing
     * @param label a label for the line or null
     */
    public void drawLine(@NonNull Canvas canvas, @NonNull ViewBox bb, int width, int height, @NonNull List<Point> line, @NonNull Paint paint,
            @Nullable String label) {
        GeoJson.pointListToLinePointsArray(bb, width, height, points, line);
        float[] linePoints = points.getArray();
        int pointsSize = points.size();
        if (pointsSize > 1) {
            path.reset();
            path.moveTo(linePoints[0], linePoints[1]);
            for (int i = 0; i < pointsSize; i = i + 4) {
                path.lineTo(linePoints[i + 2], linePoints[i + 3]);
            }
            canvas.drawPath(path, paint);
            if (label != null) {
                Coordinates m = io.vespucci.util.Geometry.midpointFromPointlist(linePoints, pointsSize, centroid);
                if (m != null) {
                    paintLabel(canvas, label, m);
                }
            }
        }
    }

    /**
     * Draw a polygon
     * 
     * @param canvas Canvas object we are drawing on
     * @param bb the current ViewBox
     * @param width screen width in screen coordinates
     * @param height screen height in screen coordinates
     * @param polygon List of List of Position objects defining the polygon rings
     * @param paint Paint object for drawing
     * @param label a label for the polygon or null
     */
    public void drawPolygon(@NonNull Canvas canvas, @NonNull ViewBox bb, int width, int height, @NonNull List<List<Point>> polygon, @NonNull Paint paint,
            @Nullable String label) {
        Coordinates c = null;
        path.reset();
        for (List<Point> ring : polygon) {
            GeoJson.pointListToLinePointsArray(bb, width, height, points, ring);
            float[] linePoints = points.getArray();
            int pointsSize = points.size();
            if (pointsSize > 2) {
                if (c == null && label != null) {
                    c = io.vespucci.util.Geometry.centroidFromPointlist(linePoints, pointsSize, centroid);
                }
                path.moveTo(linePoints[0], linePoints[1]);
                for (int i = 0; i < pointsSize; i = i + 4) {
                    path.lineTo(linePoints[i + 2], linePoints[i + 3]);
                }
                path.close();
            }
        }
        path.setFillType(Path.FillType.EVEN_ODD);
        canvas.drawPath(path, paint);
        if (c != null) {
            paintLabel(canvas, label, c);
        }
    }

    /**
     * Draw a label at the screen coordinates in c
     * 
     * @param canvas the Canvas
     * @param label the label
     * @param c the Coordinates
     */
    private void paintLabel(@NonNull Canvas canvas, @NonNull String label, @NonNull Coordinates c) {
        float halfTextWidth = labelPaint.measureText(label) / 2;
        FontMetrics fm = labelFs.getFontMetrics();
        final float x = (float) c.x;
        final float y = (float) c.y;
        canvas.drawRect(x - halfTextWidth, y, x + halfTextWidth, y - labelPaint.getTextSize() + fm.bottom, labelBackground);
        canvas.drawText(label, x - halfTextWidth, y, labelPaint);
    }

    @Override
    protected void onDrawFinished(Canvas c, IMapView osmv) {
        // do nothing
    }

    @Override
    public void onDestroy() {
        data = null;
    }

    @Override
    public String getContentId() {
        return uri;
    }

    /**
     * Read a file containing GeoJSON in to the layer, replacing any existing data
     * 
     * @param ctx Android Context
     * @param uri an URI for the file
     * @param fromState reading from saved state
     * @return true if successful
     */
    public boolean loadGeoJsonFile(@NonNull Context ctx, @NonNull Uri uri, boolean fromState) {
        try {
            Logic logic = App.getLogic();
            return new ExecutorTask<Void, Void, Boolean>(logic.getExecutorService(), logic.getHandler()) {
                @Override
                protected Boolean doInBackground(Void arg) {
                    try (InputStream is = ctx.getContentResolver().openInputStream(uri)) {
                        readingLock.lock();
                        name = ContentResolverUtil.getDisplaynameColumn(ctx, uri);
                        if (name == null) {
                            name = uri.getLastPathSegment();
                        }
                        setStateFileName(uri.getEncodedPath());
                        MapOverlay.this.uri = uri.toString();
                        return loadGeoJsonFile(ctx, is, fromState);
                    } catch (SecurityException sex) {
                        Log.e(DEBUG_TAG, sex.getMessage());
                        // note need a context here that is on the ui thread
                        ScreenMessage.toastTopError(map.getContext(), ctx.getString(R.string.toast_permission_denied, uri.toString()));
                        return false;
                    } catch (IOException iex) {
                        ScreenMessage.toastTopError(map.getContext(), ctx.getString(R.string.toast_error_reading, uri.toString()));
                        return false;
                    } finally {
                        if (readingLock.isLocked()) {
                            readingLock.unlock();
                        }
                    }
                }
            }.execute().get(Server.DEFAULT_TIMEOUT, TimeUnit.SECONDS); // result is not going to be null
        } catch (InterruptedException | ExecutionException | TimeoutException e) { // NOSONAR
            Log.e(DEBUG_TAG, e.getMessage());
            return false;
        }
    }

    /**
     * Read an InputStream containing GeoJSON data in to the layer, replacing any existing data
     * 
     * @param ctx Android Context
     * @param is the InputStream to read from
     * @param fromState reading from saved state
     * @return true if successful
     * @throws IOException if reading the InputStream fails
     */
    @SuppressLint("NewApi") // StandardCharsets is desugared for APIs < 19.
    public boolean loadGeoJsonFile(@NonNull Context ctx, @NonNull InputStream is, boolean fromState) throws IOException {
        boolean successful = false;
        // don't draw while we are loading
        setVisible(false);
        BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        try {
            String json = FileUtil.readToString(rd);
            data = new RTree<>(2, 12);
            FeatureCollection fc = FeatureCollection.fromJson(json);
            List<Feature> features = fc.features();
            if (features != null) {
                loadFeatures(features);
            } else {
                Log.d(DEBUG_TAG, "Retrying as Feature");
                Feature f = Feature.fromJson(json);
                Geometry g = f.geometry();
                if (g != null) {
                    data.insert(new BoundedFeature(f));
                } else {
                    g = GeoJson.geometryFromJson(json);
                    Log.d(DEBUG_TAG, "Geometry " + g.type());
                    if (g.type() != null) {
                        data.insert(new BoundedFeature(Feature.fromGeometry(g)));
                    }
                }
            }
            setVisible(true); // enable too
            successful = true;
            if (!fromState) {
                dirty();
            }
        } catch (OutOfMemoryError oom) {
            data = null;
            Util.runOnUiThread(ctx, () -> ScreenMessage.toastTopError(ctx, R.string.out_of_memory_title));
            Log.e(DEBUG_TAG, "Out of memory error " + oom.getMessage());
        } catch (com.google.gson.JsonSyntaxException jsex) {
            Util.runOnUiThread(ctx, () -> ScreenMessage.toastTopError(ctx, jsex.getLocalizedMessage()));
            Log.e(DEBUG_TAG, "Syntax error " + jsex.getMessage());
        } catch (Exception e) {
            // never crash
            data = null;
            Util.runOnUiThread(ctx, () -> ScreenMessage.toastTopError(ctx, e.getLocalizedMessage()));
            Log.e(DEBUG_TAG, "Exception " + e.getMessage());
        }
        // re-enable drawing
        setVisible(true);
        return successful;
    }

    /**
     * @param features a List of Feature
     */
    private void loadFeatures(@NonNull List<Feature> features) {
        for (Feature f : features) {
            if (f == null) {
                Log.e(DEBUG_TAG, "loadFeatures: null feature");
                continue;
            }
            if (GeoJSONConstants.FEATURE.equals(f.type()) && f.geometry() != null) {
                data.insert(new BoundedFeature(f));
            } else {
                Log.e(DEBUG_TAG, "Type of object " + f.type() + " geometry " + f.geometry());
            }
        }
    }

    @Override
    protected synchronized boolean save(@NonNull Context context) throws IOException {
        Log.d(DEBUG_TAG, "Saving state to " + stateFileName);
        return savingHelper.save(context, stateFileName, this, true);
    }

    @Override
    protected synchronized StyleableLayer load(@NonNull Context context) {
        Log.d(DEBUG_TAG, "Loading state from " + stateFileName);
        MapOverlay restoredOverlay = savingHelper.load(context, stateFileName, true, true, false);
        if (restoredOverlay != null) {
            labelKey = restoredOverlay.labelKey;
            labelMinZoom = restoredOverlay.labelMinZoom;
            stateFileName = restoredOverlay.stateFileName;
        }
        return restoredOverlay;
    }

    /**
     * Given screen coordinates, find all nearby elements.
     *
     * @param x Screen X-coordinate.
     * @param y Screen Y-coordinate.
     * @param viewBox Map view box.
     * @return List of Features close to given location.
     */
    @Override
    public List<Feature> getClicked(final float x, final float y, final ViewBox viewBox) {
        List<Feature> result = new ArrayList<>();
        Log.d(DEBUG_TAG, "getClicked");
        if (data != null) {
            final float tolerance = map.getDataStyle().getCurrent().getNodeToleranceValue();
            Collection<BoundedFeature> queryResult = new ArrayList<>();
            data.query(queryResult, viewBox);
            Log.d(DEBUG_TAG, "features result count " + queryResult.size());
            for (BoundedFeature bf : queryResult) {
                Feature f = bf.getFeature();
                Geometry g = f.geometry();
                if (g == null) {
                    continue;
                }
                if (geometryClicked(x, y, viewBox, tolerance, g)) {
                    result.add(f);
                }
            }
        }
        Log.d(DEBUG_TAG, "getClicked found " + result.size());
        return result;
    }

    /**
     * Check if a geometry has been clicked
     * 
     * @param x Screen X-coordinate.
     * @param y Screen Y-coordinate.
     * @param viewBox Map view box.
     * @param tolerance the tolerance value to use
     * @param g the Geometry
     * @return true if clicked
     */
    @SuppressWarnings("unchecked")
    boolean geometryClicked(final float x, final float y, @NonNull final ViewBox viewBox, final float tolerance, @NonNull Geometry g) {
        switch (g.type()) {
        case GeoJSONConstants.POINT:
            return inToleranceArea(viewBox, tolerance, (Point) g, x, y);
        case GeoJSONConstants.MULTIPOINT:
            for (Point q : ((CoordinateContainer<List<Point>>) g).coordinates()) {
                if (inToleranceArea(viewBox, tolerance, q, x, y)) {
                    return true;
                }
            }
            break;
        case GeoJSONConstants.LINESTRING:
            return distanceToLineString(x, y, map, viewBox, ((CoordinateContainer<List<Point>>) g).coordinates()) >= 0;
        case GeoJSONConstants.MULTILINESTRING:
            for (List<Point> l : ((CoordinateContainer<List<List<Point>>>) g).coordinates()) {
                if (distanceToLineString(x, y, map, viewBox, l) >= 0) {
                    return true;
                }
            }
            break;
        case GeoJSONConstants.POLYGON:
        case GeoJSONConstants.MULTIPOLYGON:
            try {
                final Point point = pointFromScreenCoords(x, y, viewBox);
                return GeoJSONConstants.POLYGON.equals(g.type()) ? TurfJoins.inside(point, (Polygon) g) : TurfJoins.inside(point, (MultiPolygon) g);
            } catch (TurfException e) {
                Log.e(DEBUG_TAG, "Exception in getClicked " + e);
            }
            break;
        case GeoJSONConstants.GEOMETRYCOLLECTION:
            for (Geometry geometry : ((GeometryCollection) g).geometries()) {
                if (geometryClicked(x, y, viewBox, tolerance, geometry)) {
                    return true;
                }
            }
            break;
        default:
            Log.e(DEBUG_TAG, "Unsupported geometry " + g.type());
        }
        return false;
    }

    /**
     * Determine if screen coords are within the tolerance for a geojson linestring
     * 
     * @param x x screen coord
     * @param y y screen coord
     * @param map map object
     * @param viewBox the current ViewBox
     * @param vertices the list of lineString vertices
     * @return if the returned value is > 0 then the coords are in the tolerance
     */
    private double distanceToLineString(final float x, final float y, final Map map, final ViewBox viewBox, List<Point> vertices) {
        final float tolerance = map.getDataStyle().getCurrent().getWayToleranceValue();
        float p1X = Float.MAX_VALUE;
        float p1Y = Float.MAX_VALUE;
        int width = map.getWidth();
        int height = map.getHeight();
        // Iterate over all WayNodes, but not the last one.
        for (int k = 0, verticesSize = vertices.size(); k < verticesSize - 1; ++k) {
            Point p1 = vertices.get(k);
            Point p2 = vertices.get(k + 1);
            if (k == 0) {
                p1X = GeoMath.lonToX(width, viewBox, p1.longitude());
                p1Y = GeoMath.latToY(height, width, viewBox, p1.latitude());
            }
            float p2X = GeoMath.lonToX(width, viewBox, p2.longitude());
            float p2Y = GeoMath.latToY(height, width, viewBox, p2.latitude());
            double distance = io.vespucci.util.Geometry.isPositionOnLine(tolerance, x, y, p1X, p1Y, p2X, p2Y);
            if (distance >= 0) {
                return distance;
            }
            p1X = p2X;
            p1Y = p2Y;
        }
        return -1;
    }

    /**
     * Check if the current touch position is in the tolerance area around a Position
     * 
     * @param viewBox the current screen ViewBox
     * @param tolerance the tolerance value
     * @param p the Position
     * @param x screen x coordinate of touch location
     * @param y screen y coordinate of touch location
     * @return true if touch position is in tolerance
     */
    private boolean inToleranceArea(@NonNull ViewBox viewBox, float tolerance, @NonNull Point p, float x, float y) {
        float differenceX = Math.abs(GeoMath.lonToX(map.getWidth(), viewBox, p.longitude()) - x);
        float differenceY = Math.abs(GeoMath.latToY(map.getHeight(), map.getWidth(), viewBox, p.latitude()) - y);
        return differenceX <= tolerance && differenceY <= tolerance && Math.hypot(differenceX, differenceY) <= tolerance;
    }

    /**
     * Create a Point object from a screen coordinate tupel
     * 
     * @param x x screen coordinate
     * @param y y screen coordinate
     * @param viewBox the current ViewBox
     * @return a Point object
     */
    private Point pointFromScreenCoords(final float x, final float y, @NonNull final ViewBox viewBox) {
        return Point.fromLngLat(GeoMath.xToLonE7(map.getWidth(), viewBox, x) / 1E7D, GeoMath.yToLatE7(map.getHeight(), map.getWidth(), viewBox, y) / 1E7D);
    }

    /**
     * Return a List of all loaded Features
     * 
     * @return a List of Feature objects
     */
    public List<Feature> getFeatures() {
        Collection<BoundedFeature> queryResult = new ArrayList<>();
        data.query(queryResult);
        List<Feature> result = new ArrayList<>();
        for (BoundedFeature bf : queryResult) {
            result.add(bf.getFeature());
        }
        return result;
    }

    @Override
    public void resetStyling() {
        initStyling(true, DataStyle.DEFAULT_GEOJSON_STROKE_WIDTH, "", Map.SHOW_LABEL_LIMIT, TriangleDown.NAME);
    }

    /**
     * Init the styling to the provided values
     * 
     * @param style if true set styling
     * @param strokeWidth the stroke width
     * @param labelKey the source of the label
     * @param labelMinZoom min. zoom from on we show the label
     * @param symbolName the name of the point symbol
     */
    private void initStyling(boolean style, float strokeWidth, @NonNull String labelKey, int labelMinZoom, String symbolName) {
        paint = new SerializableTextPaint(map.getDataStyle().getInternal(DataStyle.GEOJSON_DEFAULT).getPaint());
        iconRadius = map.getIconRadius();
        if (style) {
            setStrokeWidth(strokeWidth);
            setLabel(labelKey);
            setLabelMinZoom(labelMinZoom);
            setPointSymbol(symbolName);
        }
    }

    @Override
    public List<String> getLabelList() {
        if (data == null) {
            return super.getLabelList();
        }
        Collection<BoundedFeature> queryResult = new ArrayList<>();
        data.query(queryResult);
        Set<String> result = new TreeSet<>();
        for (BoundedFeature bf : queryResult) {
            Feature feature = bf.getFeature();
            JsonObject properties = feature != null ? feature.properties() : null;
            if (properties == null) {
                continue;
            }
            for (String key : properties.keySet()) {
                JsonElement e = properties.get(key);
                if (e != null && e.isJsonPrimitive()) {
                    result.add(key);
                }
            }

        }
        return new ArrayList<>(result);
    }

    @Override
    public void setStrokeWidth(float width) {
        super.setStrokeWidth(width);
        map.getPrefs().setGeoJsonStrokeWidth(width);
    }

    @Override
    public void setLabel(String key) {
        labelKey = key;
        map.getPrefs().setGeoJsonLabelSource(key);
    }

    @Override
    public String getLabel() {
        return labelKey;
    }

    @Override
    public void setLabelMinZoom(int minZoom) {
        labelMinZoom = minZoom;
        map.getPrefs().setGeoJsonLabelMinZoom(minZoom);
    }

    @Override
    public int getLabelMinZoom() {
        return labelMinZoom;
    }

    @Override
    public void setPointSymbol(@Nullable String symbol) {
        super.setPointSymbol(symbol);
        if (symbol != null) {
            map.getPrefs().setGeoJsonSymbol(symbol);
        }
    }

    /**
     * Get the label value for this Feature
     * 
     * @param f the Feature we want the label for
     * @return the label or null if not found
     */
    @Nullable
    public String getLabel(Feature f) {
        if (labelKey != null) {
            JsonObject properties = f.properties();
            if (properties != null) {
                JsonElement e = properties.get(labelKey);
                if (e != null && e.isJsonPrimitive()) {
                    return e.getAsString();
                }
            }
        }
        return null;
    }

    @Override
    public String getName() {
        if (name != null) {
            return name;
        }
        return map.getContext().getString(R.string.layer_geojson);
    }

    @Override
    public void invalidate() {
        map.invalidate();
    }

    @Override
    public BoundingBox getExtent() {
        if (data != null) {
            Collection<BoundedFeature> queryResult = new ArrayList<>();
            data.query(queryResult);
            BoundingBox extent = null;
            for (BoundedFeature bf : queryResult) {
                if (extent == null) {
                    extent = bf.getBounds();
                } else {
                    extent.union(bf.getBounds());
                }
            }
            return extent;
        }
        return null;
    }

    @Override
    public void discardLayer(Context context) {
        data = null;
        name = null;
        File originalFile = context.getFileStreamPath(stateFileName);
        if (!originalFile.delete()) { // NOSONAR requires API 26
            Log.e(DEBUG_TAG, "Failed to delete state file " + stateFileName);
        }
        map.invalidate();
    }

    @Override
    public void onSelected(FragmentActivity activity, Feature f) {
        FeatureInfo.showDialog(activity, f);
    }

    @Override
    public SpannableString getDescription(Feature f) {
        return getDescription(map.getContext(), f);
    }

    @Override
    public SpannableString getDescription(Context context, Feature f) {
        String label = getLabel(f);
        if (label == null || "".equals(label)) {
            Geometry g = f.geometry();
            if (g != null) {
                label = g.type();
            }
        }
        return new SpannableString(context.getString(R.string.geojson_object, label, getName()));
    }

    @Override
    public Feature getSelected() {
        return null;
    }

    @Override
    public void deselectObjects() {
        // not used
    }

    @Override
    public void setSelected(Feature o) {
        // not used
    }

    static class Info implements Serializable {
        private static final long serialVersionUID = 1L;

        String name;
        String path;
        int    pointCount              = 0;
        int    multiPointCount         = 0;
        int    linestringCount         = 0;
        int    multiLinestringCount    = 0;
        int    polygonCount            = 0;
        int    multiPolygonCount       = 0;
        int    geometrycollectionCount = 0;
    }

    @Override
    public void showInfo(FragmentActivity activity) {
        LayerInfo f = new GeoJsonLayerInfo();
        f.setShowsDialog(true);
        Bundle args = new Bundle();
        Info info = new Info();
        info.name = getName();
        info.path = uri;
        Collection<BoundedFeature> queryResult = new ArrayList<>();
        data.query(queryResult);
        for (BoundedFeature bf : queryResult) {
            switch (bf.getFeature().geometry().type()) {
            case GeoJSONConstants.POINT:
                info.pointCount++;
                break;
            case GeoJSONConstants.MULTIPOINT:
                info.multiPointCount++;
                break;
            case GeoJSONConstants.LINESTRING:
                info.linestringCount++;
                break;
            case GeoJSONConstants.MULTILINESTRING:
                info.multiLinestringCount++;
                break;
            case GeoJSONConstants.POLYGON:
                info.polygonCount++;
                break;
            case GeoJSONConstants.MULTIPOLYGON:
                info.multiPolygonCount++;
                break;
            case GeoJSONConstants.GEOMETRYCOLLECTION:
                info.geometrycollectionCount++;
                break;
            default:
                // ignore
            }
        }
        args.putSerializable(GeoJsonLayerInfo.LAYER_INFO_KEY, info);
        f.setArguments(args);
        LayerInfo.showDialog(activity, f);
    }

    @Override
    public LayerType getType() {
        return LayerType.GEOJSON;
    }
}
