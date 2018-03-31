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
import com.mapbox.services.api.utils.turf.TurfException;
import com.mapbox.services.api.utils.turf.TurfJoins;
import com.mapbox.services.commons.geojson.Feature;
import com.mapbox.services.commons.geojson.FeatureCollection;
import com.mapbox.services.commons.geojson.Geometry;
import com.mapbox.services.commons.geojson.GeometryCollection;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.geojson.MultiLineString;
import com.mapbox.services.commons.geojson.MultiPoint;
import com.mapbox.services.commons.geojson.MultiPolygon;
import com.mapbox.services.commons.geojson.Point;
import com.mapbox.services.commons.geojson.Polygon;
import com.mapbox.services.commons.geojson.custom.GeometryDeserializer;
import com.mapbox.services.commons.geojson.custom.PositionDeserializer;
import com.mapbox.services.commons.models.Position;

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
import android.util.Log;
import de.blau.android.Logic;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.osm.BoundingBox;
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
import de.blau.android.views.layers.StyleableLayer;

public class MapOverlay extends StyleableLayer implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 2L;

    private static final String DEBUG_TAG = MapOverlay.class.getName();

    /**
     * when reading state lockout writing/reading
     */
    private transient ReentrantLock readingLock = new ReentrantLock();

    public final static String FILENAME = "geojson.res";

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
                JsonArray bbox = (JsonArray) feature.getProperties().get(GeoJSONConstants.BBOX);
                if (bbox == null || bbox.size() != 4) {
                    box = getBounds(feature.getGeometry());
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
        public BoundingBox getBounds(Geometry<?> g) {
            BoundingBox result = null;
            String type = g.getType();
            switch (type) {
            case GeoJSONConstants.POINT:
                Position p = (Position) g.getCoordinates();
                result = new BoundingBox(p.getLongitude(), p.getLatitude());
                break;
            case GeoJSONConstants.LINESTRING:
            case GeoJSONConstants.MULTIPOINT:
                @SuppressWarnings("unchecked")
                List<Position> coordinates = (List<Position>) g.getCoordinates();
                for (Position q : coordinates) {
                    if (result == null) {
                        result = new BoundingBox(q.getLongitude(), q.getLatitude());
                    } else {
                        result.union(q.getLongitude(), q.getLatitude());
                    }
                }
                break;
            case GeoJSONConstants.MULTIPOLYGON:
                List<List<List<Position>>> polygons = ((MultiPolygon) g).getCoordinates();
                for (List<List<Position>> polygon : polygons) {
                    for (List<Position> l : polygon) {
                        for (Position r : l) {
                            if (result == null) {
                                result = new BoundingBox(r.getLongitude(), r.getLatitude());
                            } else {
                                result.union(r.getLongitude(), r.getLatitude());
                            }
                        }
                    }
                }
                break;
            case GeoJSONConstants.GEOMETRYCOLLECTION:
                @SuppressWarnings("rawtypes")
                List<Geometry> geometries = ((GeometryCollection) g).getGeometries();
                for (Geometry<?> geometry : geometries) {
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
                List<List<Position>> linesOrRings = (List<List<Position>>) g.getCoordinates();
                for (List<Position> l : linesOrRings) {
                    for (Position s : l) {
                        if (result == null) {
                            result = new BoundingBox(s.getLongitude(), s.getLatitude());
                        } else {
                            result.union(s.getLongitude(), s.getLatitude());
                        }
                    }
                }
                break;
            default:
                Log.e(DEBUG_TAG, "getBounds unknown GeoJSON geometry " + g.getType());
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
    private transient final Path path = new Path();
    private transient Paint      paint;

    /** Map this is an overlay of. */
    private transient final Map map;

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
        return data != null && map.getBackgroundLayer().isReadyToDraw();
    }

    @Override
    protected void onDraw(Canvas canvas, IMapView osmv) {
        if (data == null) {
            return;
        }
        ViewBox bb = osmv.getViewBox();
        int width = map.getWidth();
        int height = map.getHeight();
        int zoomLevel = map.getZoomLevel();
        labelFs = DataStyle.getCurrent(DataStyle.LABELTEXT_NORMAL);
        labelPaint = labelFs.getPaint();
        labelBackground = DataStyle.getCurrent(DataStyle.LABELTEXT_BACKGROUND).getPaint();
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
    public void drawGeometry(Canvas canvas, ViewBox bb, int width, int height, int zoomLevel, Feature f) {
        Geometry<?> g = f.getGeometry();
        String label = null;
        if (zoomLevel > Map.SHOW_LABEL_LIMIT) {
            label = getLabel(f);
        }
        switch (g.getType()) {
        case GeoJSONConstants.POINT:
            Position p = (Position) g.getCoordinates();
            drawPoint(canvas, bb, width, height, p, paint, label);
            break;
        case GeoJSONConstants.MULTIPOINT:
            List<Position> points = ((MultiPoint) g).getCoordinates();
            for (Position q : points) {
                drawPoint(canvas, bb, width, height, q, paint, label);
            }
            break;
        case GeoJSONConstants.LINESTRING:
            List<Position> line = ((LineString) g).getCoordinates();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.STROKE);
            drawLine(canvas, bb, width, height, line, paint);
            canvas.drawPath(path, paint);
            break;
        case GeoJSONConstants.MULTILINESTRING:
            List<List<Position>> lines = ((MultiLineString) g).getCoordinates();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.STROKE);
            for (List<Position> l : lines) {
                drawLine(canvas, bb, width, height, l, paint);
            }
            break;
        case GeoJSONConstants.POLYGON:
            List<List<Position>> rings = ((Polygon) g).getCoordinates();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            drawPolygon(canvas, bb, width, height, rings, paint);
            break;
        case GeoJSONConstants.MULTIPOLYGON:
            List<List<List<Position>>> polygons = ((MultiPolygon) g).getCoordinates();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            for (List<List<Position>> polygon : polygons) {
                drawPolygon(canvas, bb, width, height, polygon, paint);
            }
            break;
        case GeoJSONConstants.GEOMETRYCOLLECTION:
            @SuppressWarnings("rawtypes")
            List<Geometry> geometries = ((GeometryCollection) g).getGeometries();
            for (Geometry<?> geometry : geometries) {
                drawGeometry(canvas, bb, width, height, zoomLevel, Feature.fromGeometry(geometry));
            }
            break;
        default:
            Log.e(DEBUG_TAG, "drawGeometry unknown GeoJSON geometry " + g.getType());
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
    public void drawPoint(@NonNull Canvas canvas, @NonNull ViewBox bb, int width, int height, @NonNull Position p, @NonNull Paint paint,
            @Nullable String label) {
        float x = GeoMath.lonToX(width, bb, p.getLongitude());
        float y = GeoMath.latToY(height, width, bb, p.getLatitude());
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
    public void drawLine(@NonNull Canvas canvas, @NonNull ViewBox bb, int width, int height, @NonNull List<Position> line, @NonNull Paint paint) {
        path.reset();
        int size = line.size();
        for (int i = 0; i < size; i++) {
            Position p = line.get(i);
            float x = GeoMath.lonToX(width, bb, p.getLongitude());
            float y = GeoMath.latToY(height, width, bb, p.getLatitude());
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
    public void drawPolygon(@NonNull Canvas canvas, @NonNull ViewBox bb, int width, int height, @NonNull List<List<Position>> polygon, @NonNull Paint paint) {
        path.reset();
        for (List<Position> ring : polygon) {
            int size = ring.size();
            for (int i = 0; i < size; i++) {
                Position p = ring.get(i);
                float x = GeoMath.lonToX(width, bb, p.getLongitude());
                float y = GeoMath.latToY(height, width, bb, p.getLatitude());
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
     * @throws IOException
     */
    public void loadGeoJsonFile(@NonNull Context ctx, @NonNull Uri uri) throws IOException {
        InputStream is = null;
        if (uri.getScheme().equals("file")) {
            is = new FileInputStream(new File(uri.getPath()));
        } else {
            ContentResolver cr = ctx.getContentResolver();
            is = cr.openInputStream(uri);
        }
        name = uri.getLastPathSegment();
        loadGeoJsonFile(ctx, is);
    }

    /**
     * Read an InputStream containing GeoJSON data in to the layer, replacing any existing data
     * 
     * @param ctx Android Context
     * @param is the InputStream to read from
     * @throws IOException
     */
    public void loadGeoJsonFile(@NonNull Context ctx, @NonNull InputStream is) throws IOException {
        BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }

        try {
            data = new RTree(2, 12);
            String json = sb.toString();
            FeatureCollection fc = FeatureCollection.fromJson(json);
            List<Feature> features = fc.getFeatures();
            if (features != null) {
                loadFeatures(features);
            } else {
                Log.d(DEBUG_TAG, "Retrying as Feature");
                Feature f = Feature.fromJson(json);
                Geometry<?> g = f.getGeometry();
                if (g != null) {
                    data.insert(new BoundedFeature(f));
                } else {
                    GsonBuilder gson = new GsonBuilder();
                    gson.registerTypeAdapter(Position.class, new PositionDeserializer());
                    gson.registerTypeAdapter(Geometry.class, new GeometryDeserializer());
                    g = gson.create().fromJson(json, Geometry.class);
                    Log.d(DEBUG_TAG, "Geometry " + g.getType());
                    if (g.getType() != null) {
                        data.insert(new BoundedFeature(Feature.fromGeometry(g)));
                    }
                }
            }
        } catch (com.google.gson.JsonSyntaxException jsex) {
            data = null;
            Snack.toastTopError(ctx, jsex.getLocalizedMessage());
        } catch (Exception e) {
            // never crash
            data = null;
            Snack.toastTopError(ctx, e.getLocalizedMessage());
            e.printStackTrace();
        }
        saved = false;
    }

    /**
     * @param fc
     */
    public void loadFeatures(List<Feature> features) {
        for (Feature f : features) {
            if (GeoJSONConstants.FEATURE.equals(f.getType()) && f.getGeometry() != null) {
                data.insert(new BoundedFeature(f));
            } else {
                Log.e(DEBUG_TAG, "Type of object " + f.getType() + " geometry " + f.getGeometry());
            }
        }
    }

    /**
     * Stores the current state to the default storage file
     * 
     * @param ctx Android Context
     * @throws IOException on errors writing the file
     */
    public synchronized void onSaveState(@NonNull Context ctx) throws IOException {
        if (saved) {
            Log.i(DEBUG_TAG, "state not dirty, skipping save");
            return;
        }
        if (readingLock.tryLock()) {
            try {
                // TODO this doesn't really help with error conditions need to throw exception
                if (savingHelper.save(ctx, FILENAME, this, true)) {
                    saved = true;
                } else {
                    // this is essentially catastrophic and can only happen if something went really wrong
                    // running out of memory or disk, or HW failure
                    if (ctx != null && ctx instanceof Activity) {
                        Snack.barError((Activity) ctx, R.string.toast_statesave_failed);
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
        try {
            readingLock.lock();
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
                return true;
            } else {
                Log.d(DEBUG_TAG, "saved state null");
                return false;
            }
        } finally {
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
                    Geometry<?> g = f.getGeometry();
                    switch (g.getType()) {
                    case GeoJSONConstants.POINT:
                        Position p = (Position) g.getCoordinates();
                        if (inToleranceArea(viewBox, tolerance, p, x, y)) {
                            result.add(f);
                        }
                        break;
                    case GeoJSONConstants.MULTIPOINT:
                        @SuppressWarnings("unchecked")
                        List<Position> positions = (List<Position>) g.getCoordinates();
                        for (Position q : positions) {
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
                        List<Position> vertices = (List<Position>) g.getCoordinates();
                        for (int k = 0, verticesSize = vertices.size(); k < verticesSize - 1; ++k) {
                            Position p1 = vertices.get(k);
                            Position p2 = vertices.get(k + 1);
                            if (p1X == Float.MAX_VALUE) {
                                p1X = GeoMath.lonToX(width, viewBox, p1.getLongitude());
                                p1Y = GeoMath.latToY(height, width, viewBox, p1.getLatitude());
                            }
                            float node2X = GeoMath.lonToX(width, viewBox, p2.getLongitude());
                            float node2Y = GeoMath.latToY(height, width, viewBox, p2.getLatitude());
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
    private boolean inToleranceArea(@NonNull ViewBox viewBox, float tolerance, Position p, float x, float y) {
        float differenceX = Math.abs(GeoMath.lonToX(map.getWidth(), viewBox, p.getLongitude()) - x);
        float differenceY = Math.abs(GeoMath.latToY(map.getHeight(), map.getWidth(), viewBox, p.getLatitude()) - y);
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
        Point point = Point.fromCoordinates(Position.fromCoordinates(GeoMath.xToLonE7(map.getWidth(), viewBox, x) / 1E7D,
                GeoMath.yToLatE7(map.getHeight(), map.getWidth(), viewBox, y) / 1E7D));
        return point;
    }

    /**
     * Return the bounding box for all of the GeoJSON objects in storage
     * 
     * @return a BoundingBox covering all objects
     */
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

    /**
     * Get the current color for this layer
     * 
     * @return the color as an int
     */
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
        paint = new Paint(DataStyle.getCurrent(DataStyle.GEOJSON_DEFAULT).getPaint());
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
            Set<String> result = new TreeSet<String>();
            for (BoundedObject bo : queryResult) {
                BoundedFeature bf = (BoundedFeature) bo;
                JsonObject properties = bf.getFeature().getProperties();
                if (properties != null) {
                    for (String key : properties.keySet()) {
                        JsonElement e = properties.get(key);
                        if (e != null && e.isJsonPrimitive()) {
                            result.add(key);
                        }
                    }
                }
            }
            return new ArrayList<String>(result);
        }
        return null;
    }

    @Override
    public void setLabel(String key) {
        labelKey = key;
    }

    /**
     * Get the label value for this Feature
     * 
     * @param f the Feature we want the label for
     * @return the label or null if not found
     */
    public String getLabel(Feature f) {
        if (labelKey != null) {
            JsonObject properties = f.getProperties();
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
}
