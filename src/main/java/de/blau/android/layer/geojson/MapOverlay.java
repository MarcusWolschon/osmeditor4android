package de.blau.android.layer.geojson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;

import com.google.gson.GsonBuilder;
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
import com.mapbox.geojson.gson.BoundingBoxDeserializer;
import com.mapbox.geojson.gson.GeometryDeserializer;
import com.mapbox.geojson.gson.PointDeserializer;
import com.mapbox.turf.TurfException;
import com.mapbox.turf.TurfJoins;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Path;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import de.blau.android.Logic;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.dialogs.FeatureInfo;
import de.blau.android.layer.ClickableInterface;
import de.blau.android.layer.DiscardInterface;
import de.blau.android.layer.ExtentInterface;
import de.blau.android.layer.StyleableLayer;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.OsmXml;
import de.blau.android.osm.ViewBox;
import de.blau.android.resources.DataStyle;
import de.blau.android.resources.DataStyle.FeatureStyle;
import de.blau.android.util.GeoJSONConstants;
import de.blau.android.util.GeoMath;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.Snack;
import de.blau.android.util.rtree.BoundedObject;
import de.blau.android.util.rtree.RTree;
import de.blau.android.views.IMapView;

public class MapOverlay extends StyleableLayer implements Serializable, ExtentInterface, DiscardInterface, ClickableInterface<Feature> {

    /**
     * 
     */
    private static final long serialVersionUID = 2L;

    private static final String DEBUG_TAG = MapOverlay.class.getName();

    /**
     * when reading state lockout writing/reading
     */
    private transient ReentrantLock readingLock = new ReentrantLock();

    public static final String FILENAME = "geojson.res";

    private transient SavingHelper<MapOverlay> savingHelper = new SavingHelper<>();
    private transient boolean                  saved        = false;

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
                    box = getBounds(feature.geometry());
                } else { // the geojson contains a bbox, use that
                    box = new BoundingBox(bbox.get(0).getAsDouble(), bbox.get(1).getAsDouble(), bbox.get(2).getAsDouble(), bbox.get(3).getAsDouble());
                }
            }
            return box;
        }

        /**
         * Determine the bounding box for GeoJSON geometries
         * 
         * @param g the GeoJSON Geometry
         * @return the bounding box
         */
        public BoundingBox getBounds(@NonNull Geometry g) {
            BoundingBox result = null;
            String type = g.type();
            switch (type) {
            case GeoJSONConstants.POINT:
                Point p = (Point) g;
                result = new BoundingBox(p.longitude(), p.latitude());
                break;
            case GeoJSONConstants.LINESTRING:
            case GeoJSONConstants.MULTIPOINT:
                @SuppressWarnings("unchecked")
                List<Point> coordinates = ((CoordinateContainer<List<Point>>) g).coordinates();
                for (Point q : coordinates) {
                    if (result == null) {
                        result = new BoundingBox(q.longitude(), q.latitude());
                    } else {
                        result.union(q.longitude(), q.latitude());
                    }
                }
                break;
            case GeoJSONConstants.MULTIPOLYGON:
                @SuppressWarnings("unchecked")
                List<List<List<Point>>> polygons = ((CoordinateContainer<List<List<List<Point>>>>) g).coordinates();
                for (List<List<Point>> polygon : polygons) {
                    for (List<Point> l : polygon) {
                        for (Point r : l) {
                            if (result == null) {
                                result = new BoundingBox(r.longitude(), r.latitude());
                            } else {
                                result.union(r.longitude(), r.latitude());
                            }
                        }
                    }
                }
                break;
            case GeoJSONConstants.GEOMETRYCOLLECTION:
                List<Geometry> geometries = ((GeometryCollection) g).geometries();
                for (Geometry geometry : geometries) {
                    if (result == null) {
                        result = getBounds(geometry);
                    } else {
                        result.union(getBounds(geometry));
                    }
                }
                break;
            case GeoJSONConstants.MULTILINESTRING:
            case GeoJSONConstants.POLYGON:
                @SuppressWarnings("unchecked")
                List<List<Point>> linesOrRings = ((CoordinateContainer<List<List<Point>>>) g).coordinates();
                for (List<Point> l : linesOrRings) {
                    for (Point s : l) {
                        if (result == null) {
                            result = new BoundingBox(s.longitude(), s.latitude());
                        } else {
                            result.union(s.longitude(), s.latitude());
                        }
                    }
                }
                break;
            default:
                Log.e(DEBUG_TAG, "getBounds unknown GeoJSON geometry " + g.type());
            }
            return result;
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
         * @throws IOException
         */
        private void writeObject(java.io.ObjectOutputStream out) throws IOException {
            out.writeUTF(feature.toJson());
            out.writeObject(box);
        }

        /**
         * Recreate the object for serialized state
         * 
         * @param in ObjectInputStream to write from
         * @throws IOException
         * @throws ClassNotFoundException
         */
        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
            String jsonString = in.readUTF();
            feature = Feature.fromJson(jsonString);
            box = (BoundingBox) in.readObject();
        }
    }

    private RTree                data;
    private final transient Path path = new Path();
    private transient Paint      paint;

    /** Map this is an overlay of. */
    private final transient Map map;

    /**
     * Styling parameters
     */
    private int    iconRadius;
    private int    color;
    private float  strokeWidth;
    private String labelKey;

    transient Paint        labelPaint;
    transient Paint        labelBackground;
    transient float        labelStrokeWidth;
    transient FeatureStyle labelFs;

    /**
     * Name for this layer (typically the file name)
     */
    private String name;

    /**
     * Construct this layer
     * 
     * @param map the Map object we are displayed on
     */
    public MapOverlay(final Map map) {
        this.map = map;
        resetStyling();
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
        labelFs = DataStyle.getInternal(DataStyle.LABELTEXT_NORMAL);
        labelPaint = labelFs.getPaint();
        labelBackground = DataStyle.getInternal(DataStyle.LABELTEXT_BACKGROUND).getPaint();
        labelStrokeWidth = labelPaint.getStrokeWidth();

        Collection<BoundedObject> queryResult = new ArrayList<>();
        data.query(queryResult, bb);
        Log.d(DEBUG_TAG, "features result count " + queryResult.size());
        for (BoundedObject bo : queryResult) {
            Feature f = ((BoundedFeature) bo).getFeature();
            drawGeometry(canvas, bb, width, height, zoomLevel, f);
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
        if (zoomLevel > Map.SHOW_LABEL_LIMIT) {
            label = getLabel(f);
        }
        switch (g.type()) {
        case GeoJSONConstants.POINT:
            drawPoint(canvas, bb, width, height, (Point) g, paint, label);
            break;
        case GeoJSONConstants.MULTIPOINT:
            @SuppressWarnings("unchecked")
            List<Point> points = ((CoordinateContainer<List<Point>>) g).coordinates();
            for (Point q : points) {
                drawPoint(canvas, bb, width, height, q, paint, label);
            }
            break;
        case GeoJSONConstants.LINESTRING:
            @SuppressWarnings("unchecked")
            List<Point> line = ((CoordinateContainer<List<Point>>) g).coordinates();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.STROKE);
            drawLine(canvas, bb, width, height, line, paint);
            canvas.drawPath(path, paint);
            break;
        case GeoJSONConstants.MULTILINESTRING:
            @SuppressWarnings("unchecked")
            List<List<Point>> lines = ((CoordinateContainer<List<List<Point>>>) g).coordinates();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.STROKE);
            for (List<Point> l : lines) {
                drawLine(canvas, bb, width, height, l, paint);
            }
            break;
        case GeoJSONConstants.POLYGON:
            @SuppressWarnings("unchecked")
            List<List<Point>> rings = ((CoordinateContainer<List<List<Point>>>) g).coordinates();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            drawPolygon(canvas, bb, width, height, rings, paint);
            break;
        case GeoJSONConstants.MULTIPOLYGON:
            @SuppressWarnings("unchecked")
            List<List<List<Point>>> polygons = ((CoordinateContainer<List<List<List<Point>>>>) g).coordinates();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            for (List<List<Point>> polygon : polygons) {
                drawPolygon(canvas, bb, width, height, polygon, paint);
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
        float x = GeoMath.lonToX(width, bb, p.longitude());
        float y = GeoMath.latToY(height, width, bb, p.latitude());
        canvas.save();
        canvas.translate(x, y);
        canvas.drawPath(DataStyle.getCurrent().getWaypointPath(), paint);
        canvas.restore();
        if (label != null) {
            float yOffset = 2 * labelStrokeWidth + iconRadius;
            float halfTextWidth = labelPaint.measureText(label) / 2;
            FontMetrics fm = labelFs.getFontMetrics();
            canvas.drawRect(x - halfTextWidth, y + yOffset + fm.bottom, x + halfTextWidth, y + yOffset - labelPaint.getTextSize() + fm.bottom, labelBackground);
            canvas.drawText(label, x - halfTextWidth, y + yOffset, labelPaint);
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
     */
    public void drawLine(@NonNull Canvas canvas, @NonNull ViewBox bb, int width, int height, @NonNull List<Point> line, @NonNull Paint paint) {
        path.reset();
        int size = line.size();
        for (int i = 0; i < size; i++) {
            Point p = line.get(i);
            float x = GeoMath.lonToX(width, bb, p.longitude());
            float y = GeoMath.latToY(height, width, bb, p.latitude());
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        canvas.drawPath(path, paint);
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
     */
    public void drawPolygon(@NonNull Canvas canvas, @NonNull ViewBox bb, int width, int height, @NonNull List<List<Point>> polygon, @NonNull Paint paint) {
        path.reset();
        for (List<Point> ring : polygon) {
            int size = ring.size();
            for (int i = 0; i < size; i++) {
                Point p = ring.get(i);
                float x = GeoMath.lonToX(width, bb, p.longitude());
                float y = GeoMath.latToY(height, width, bb, p.latitude());
                if (i == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
                if (i == size - 1) {
                    path.close();
                }
            }
        }
        path.setFillType(Path.FillType.EVEN_ODD);
        canvas.drawPath(path, paint);
    }

    @Override
    protected void onDrawFinished(Canvas c, IMapView osmv) {
        // do nothing
    }

    @Override
    public void onDestroy() {
        data = null;
    }

    /**
     * Read a file containing GeoJSON in to the layer, replacing any existing data
     * 
     * @param ctx Android Context
     * @param uri an URI for the file
     * @return true if successful
     * @throws IOException
     */
    public boolean loadGeoJsonFile(@NonNull Context ctx, @NonNull Uri uri) throws IOException {
        InputStream is = null;
        if (uri.getScheme().equals("file")) {
            is = new FileInputStream(new File(uri.getPath()));
        } else {
            ContentResolver cr = ctx.getContentResolver();
            is = cr.openInputStream(uri);
        }
        name = uri.getLastPathSegment();
        return loadGeoJsonFile(ctx, is);
    }

    /**
     * Read an InputStream containing GeoJSON data in to the layer, replacing any existing data
     * 
     * @param ctx Android Context
     * @param is the InputStream to read from
     * @return true if successful
     * @throws IOException
     */
    public boolean loadGeoJsonFile(@NonNull Context ctx, @NonNull InputStream is) throws IOException {
        boolean successful = false;
        // don't draw while we are loading
        setVisible(false);
        BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName(OsmXml.UTF_8)));
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }

        try {
            data = new RTree(2, 12);
            String json = sb.toString();
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
                    GsonBuilder gson = new GsonBuilder();
                    gson.registerTypeAdapter(Geometry.class, new GeometryDeserializer());
                    gson.registerTypeAdapter(Point.class, new PointDeserializer());
                    gson.registerTypeAdapter(BoundingBox.class, new BoundingBoxDeserializer());
                    g = gson.create().fromJson(json, Geometry.class);
                    Log.d(DEBUG_TAG, "Geometry " + g.type());
                    if (g.type() != null) {
                        data.insert(new BoundedFeature(Feature.fromGeometry(g)));
                    }
                }
            }
            setVisible(true); // enable too
            successful = true;
        } catch (com.google.gson.JsonSyntaxException jsex) {
            data = null;
            Snack.toastTopError(ctx, jsex.getLocalizedMessage());
            Log.e(DEBUG_TAG, "Syntax error " + jsex.getMessage());
        } catch (Exception e) {
            // never crash
            data = null;
            Snack.toastTopError(ctx, e.getLocalizedMessage());
            Log.e(DEBUG_TAG, "Exception " + e.getMessage());
        }
        saved = false;
        // re-enable drawing
        setVisible(true);
        return successful;
    }

    /**
     * @param features a List of Feature
     */
    private void loadFeatures(List<Feature> features) {
        for (Feature f : features) {
            if (GeoJSONConstants.FEATURE.equals(f.type()) && f.geometry() != null) {
                data.insert(new BoundedFeature(f));
            } else {
                Log.e(DEBUG_TAG, "Type of object " + f.type() + " geometry " + f.geometry());
            }
        }
    }

    /**
     * Stores the current state to the default storage file
     * 
     * @param context Android Context
     * @throws IOException on errors writing the file
     */
    public synchronized void onSaveState(@NonNull Context context) throws IOException {
        super.onSaveState(context);
        if (saved) {
            Log.i(DEBUG_TAG, "state not dirty, skipping save");
            return;
        }
        if (readingLock.tryLock()) {
            try {
                // TODO this doesn't really help with error conditions need to throw exception
                if (savingHelper.save(context, FILENAME, this, true)) {
                    saved = true;
                } else {
                    // this is essentially catastrophic and can only happen if something went really wrong
                    // running out of memory or disk, or HW failure
                    if (context instanceof Activity) {
                        Snack.barError((Activity) context, R.string.toast_statesave_failed);
                    }
                }
            } finally {
                readingLock.unlock();
            }
        } else {
            Log.i(DEBUG_TAG, "bug state being read, skipping save");
        }
    }

    /**
     * Loads any saved state from the default storage file
     * 
     * 
     * @param context Android context
     * @return true if the saved state was successfully read
     */
    public synchronized boolean onRestoreState(@NonNull Context context) {
        super.onRestoreState(context);
        try {
            readingLock.lock();
            if (data != null && data.count() > 0) {
                // don't restore over existing data
                return true;
            }
            // disable drawing
            setVisible(false);
            MapOverlay restoredOverlay = savingHelper.load(context, FILENAME, true);
            if (restoredOverlay != null) {
                Log.d(DEBUG_TAG, "read saved state");
                data = restoredOverlay.data;
                iconRadius = restoredOverlay.iconRadius;
                color = restoredOverlay.color;
                paint.setColor(color);
                strokeWidth = restoredOverlay.strokeWidth;
                paint.setStrokeWidth(strokeWidth);
                labelKey = restoredOverlay.labelKey;
                name = restoredOverlay.name;
                return true;
            } else {
                Log.d(DEBUG_TAG, "saved state null");
                return false;
            }
        } finally {
            // re-enable drawing
            setVisible(true);
            readingLock.unlock();
        }
    }

    /**
     * Given screen coordinates, find all nearby elements.
     *
     * @param x Screen X-coordinate.
     * @param y Screen Y-coordinate.
     * @param viewBox Map view box.
     * @return List of photos close to given location.
     */
    @Override
    public List<Feature> getClicked(final float x, final float y, final ViewBox viewBox) {
        List<Feature> result = new ArrayList<>();
        Log.d(DEBUG_TAG, "getClicked");
        if (data != null) {
            final float tolerance = DataStyle.getCurrent().getNodeToleranceValue();
            Collection<BoundedObject> queryResult = new ArrayList<>();
            data.query(queryResult, viewBox);
            Log.d(DEBUG_TAG, "features result count " + queryResult.size());
            if (queryResult != null) {
                for (BoundedObject bo : queryResult) {
                    Feature f = ((BoundedFeature) bo).getFeature();
                    Geometry g = f.geometry();
                    if (g == null) {
                        continue;
                    }
                    switch (g.type()) {
                    case GeoJSONConstants.POINT:
                        if (inToleranceArea(viewBox, tolerance, (Point) g, x, y)) {
                            result.add(f);
                        }
                        break;
                    case GeoJSONConstants.MULTIPOINT:
                        @SuppressWarnings("unchecked")
                        List<Point> positions = ((CoordinateContainer<List<Point>>) g).coordinates();
                        for (Point q : positions) {
                            if (inToleranceArea(viewBox, tolerance, q, x, y)) {
                                result.add(f);
                                break;
                            }
                        }
                        break;
                    case GeoJSONConstants.LINESTRING:
                        float p1X = Float.MAX_VALUE;
                        float p1Y = Float.MAX_VALUE;
                        int width = map.getWidth();
                        int height = map.getHeight();
                        // Iterate over all WayNodes, but not the last one.
                        @SuppressWarnings("unchecked")
                        List<Point> vertices = ((CoordinateContainer<List<Point>>) g).coordinates();
                        for (int k = 0, verticesSize = vertices.size(); k < verticesSize - 1; ++k) {
                            Point p1 = vertices.get(k);
                            Point p2 = vertices.get(k + 1);
                            if (p1X == Float.MAX_VALUE) {
                                p1X = GeoMath.lonToX(width, viewBox, p1.longitude());
                                p1Y = GeoMath.latToY(height, width, viewBox, p1.latitude());
                            }
                            float node2X = GeoMath.lonToX(width, viewBox, p2.longitude());
                            float node2Y = GeoMath.latToY(height, width, viewBox, p2.latitude());
                            double distance = Logic.isPositionOnLine(x, y, p1X, p1Y, node2X, node2Y);
                            if (distance >= 0) {
                                result.add(f);
                                break;
                            }
                        }
                        break;
                    case GeoJSONConstants.POLYGON:
                        try {
                            if (TurfJoins.inside(pointFromScreenCoords(x, y, viewBox), (Polygon) g)) {
                                result.add(f);
                            }
                        } catch (TurfException e) {
                            Log.e(DEBUG_TAG, "Exception in getClicked " + e);
                        }
                        break;
                    case GeoJSONConstants.MULTIPOLYGON:
                        try {
                            if (TurfJoins.inside(pointFromScreenCoords(x, y, viewBox), (MultiPolygon) g)) {
                                result.add(f);
                            }
                        } catch (TurfException e) {
                            Log.e(DEBUG_TAG, "Exception in getClicked " + e);
                        }
                        break;
                    default:
                    }
                }
            }
        }
        Log.d(DEBUG_TAG, "getClicked found " + result.size());
        return result;
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
    private Point pointFromScreenCoords(final float x, final float y, final ViewBox viewBox) {
        Point point = Point.fromLngLat(GeoMath.xToLonE7(map.getWidth(), viewBox, x) / 1E7D,
                GeoMath.yToLatE7(map.getHeight(), map.getWidth(), viewBox, y) / 1E7D);
        return point;
    }

    /**
     * Return a List of all loaded Features
     * 
     * @return a List of Feature objects
     */
    public List<Feature> getFeatures() {
        Collection<BoundedObject> queryResult = new ArrayList<>();
        data.query(queryResult);
        List<Feature> result = new ArrayList<>();
        for (BoundedObject bo : queryResult) {
            result.add(((BoundedFeature) bo).getFeature());
        }
        return result;
    }

    @Override
    public int getColor() {
        return paint.getColor();
    }

    @Override
    public void setColor(int color) {
        paint.setColor(color);
        this.color = color;
    }

    @Override
    public float getStrokeWidth() {
        return paint.getStrokeWidth();
    }

    @Override
    public void setStrokeWidth(float width) {
        paint.setStrokeWidth(width);
        strokeWidth = width;
    }

    @Override
    public Path getPointSymbol() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setPointSymbol(Path symbol) {
        // TODO Auto-generated method stub
    }

    @Override
    public void resetStyling() {
        paint = new Paint(DataStyle.getInternal(DataStyle.GEOJSON_DEFAULT).getPaint());
        color = paint.getColor();
        strokeWidth = paint.getStrokeWidth();
        labelKey = "";
        iconRadius = map.getIconRadius();
    }

    @Override
    public List<String> getLabelList() {
        if (data != null) {
            Collection<BoundedObject> queryResult = new ArrayList<>();
            data.query(queryResult);
            Set<String> result = new TreeSet<>();
            for (BoundedObject bo : queryResult) {
                BoundedFeature bf = (BoundedFeature) bo;
                Feature feature = bf.getFeature();
                if (feature != null) {
                    JsonObject properties = feature.properties();
                    if (properties != null) {
                        for (String key : properties.keySet()) {
                            JsonElement e = properties.get(key);
                            if (e != null && e.isJsonPrimitive()) {
                                result.add(key);
                            }
                        }
                    }
                }
            }
            return new ArrayList<>(result);
        }
        return null;
    }

    @Override
    public void setLabel(String key) {
        labelKey = key;
    }

    @Override
    public String getLabel() {
        return labelKey;
    }

    /**
     * Get the label value for this Feature
     * 
     * @param f the Feature we want the label for
     * @return the label or null if not found
     */
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
            Collection<BoundedObject> queryResult = new ArrayList<>();
            data.query(queryResult);
            BoundingBox extent = null;
            for (BoundedObject bo : queryResult) {
                if (extent == null) {
                    extent = bo.getBounds();
                } else {
                    extent.union(bo.getBounds());
                }
            }
            return extent;
        }
        return null;
    }

    @Override
    public boolean isEnabled() {
        return data != null && data.count() > 0;
    }

    @Override
    public void discard(Context context) {
        if (readingLock.tryLock()) {
            try {
                data = null;
                File originalFile = context.getFileStreamPath(FILENAME);
                if (!originalFile.delete()) {
                    Log.e(DEBUG_TAG, "Failed to delete state file " + FILENAME);
                }
            } finally {
                readingLock.unlock();
            }
        }
    }

    @Override
    public void onSelected(FragmentActivity activity, Feature f) {
        FeatureInfo.showDialog(activity, f);
    }

    @Override
    public String getDescription(Feature f) {
        String label = getLabel(f);
        if (label == null || "".equals(label)) {
            Geometry g = f.geometry();
            if (g != null) {
                label = g.type();
            }
        }
        return map.getContext().getString(R.string.geojson_object, label, getName());
    }

    @Override
    public Feature getSelected() {
        return null;
    }

    @Override
    public void deselectObjects() {
        // not used
    }
}
