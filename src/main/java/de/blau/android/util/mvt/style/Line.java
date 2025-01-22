package de.blau.android.util.mvt.style;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;

import com.mapbox.geojson.CoordinateContainer;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;

import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.util.GeoJSONConstants;
import de.blau.android.util.SerializableTextPaint;
import de.blau.android.util.collections.FloatPrimitiveList;
import de.blau.android.util.mvt.VectorTileDecoder;
import de.blau.android.util.mvt.VectorTileDecoder.Feature;

public class Line extends Layer {

    private static final long serialVersionUID = 7L;

    public static final float DEFAULT_LINE_WIDTH = 1f;

    private transient FloatPrimitiveList points = new FloatPrimitiveList(1000);

    FloatStyleAttribute lineWidth = new FloatStyleAttribute(true) {
        private static final long serialVersionUID = 1L;

        @Override
        protected void set(float value) {
            paint.setStrokeWidth(value);
        }
    };

    private float[] dashArray;

    /**
     * Default constructor
     * 
     * @param sourceLayer the source (data) layer
     */
    public Line(@NonNull String sourceLayer) {
        super(sourceLayer);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
    }

    /**
     * Copy constructor
     * 
     * @param other another Style
     */
    public Line(@NonNull Line other) {
        this(other.getSourceLayer());
        lineWidth.set(other.getStrokeWidth());
    }

    /**
     * Create a rudimentary Layer from Paint objects for the geometries
     * 
     * @param layer source layer
     * @param paint the Paint to use for the geometries
     * @return a Line Layer
     */
    @NonNull
    public static Line fromPaint(@NonNull String layer, @NonNull Paint paint) {
        Line style = new Line(layer);
        style.paint = new SerializableTextPaint(paint);
        style.paint.setAntiAlias(true);
        style.paint.setStyle(Paint.Style.STROKE);
        return style;
    }

    @Override
    public void onZoomChange(Style style, VectorTileDecoder.Feature feature, int z) {
        super.onZoomChange(style, feature, z);
        lineWidth.eval(feature, z);
        setDashArrayOnPaint();
    }

    @Override
    public void render(Canvas c, Style style, Feature feature, int z, Rect screenRect, Rect destinationRect, float scaleX, float scaleY) {
        super.render(c, style, feature, z, screenRect, destinationRect, scaleX, scaleY);
        this.destinationRect = destinationRect;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        Geometry g = feature.getGeometry();
        switch (g.type()) {
        case GeoJSONConstants.LINESTRING:
            @SuppressWarnings("unchecked")
            List<Point> line = ((CoordinateContainer<List<Point>>) g).coordinates();
            drawLine(screenRect, c, line);
            break;
        case GeoJSONConstants.MULTILINESTRING:
            @SuppressWarnings("unchecked")
            List<List<Point>> lines = ((CoordinateContainer<List<List<Point>>>) g).coordinates();
            for (List<Point> l : lines) {
                drawLine(screenRect, c, l);
            }
            break;
        default:
            // log?
        }
    }

    /**
     * Draw a line
     * 
     * @param screenRect a REct with the screen bounds
     * @param canvas Canvas object we are drawing on
     * @param line a List of Points making up the line
     */
    public void drawLine(@NonNull Rect screenRect, @NonNull Canvas canvas, @NonNull List<Point> line) {
        pointListToLinePointsArray(screenRect, destinationRect.left, scaleX, destinationRect.top, scaleY, points, line);
        float[] linePoints = points.getArray();
        int pointsSize = points.size();
        if (pointsSize > 1) {
            path.reset();
            path.moveTo(linePoints[0], linePoints[1]);
            for (int i = 0; i < pointsSize; i = i + 4) {
                path.lineTo(linePoints[i + 2], linePoints[i + 3]);
            }
            canvas.drawPath(path, paint);
        }
    }

    /**
     * Converts linestring nodes to a list of screen-coordinate points for drawing.
     * 
     * Only segments that are inside the rect are included. This duplicates the logic in Map for OSM objects
     * 
     * @param rect the current screen rect
     * @param left left coordinate of the destiation rect
     * @param scaleX scale factor X
     * @param top top coordindate of the destiation rect
     * @param scaleY scale factoY
     * @param points list to (re-)use for projected points in the format expected by
     *            {@link Canvas#drawLines(float[], Paint)}
     * @param nodes A List of the Points to be drawn
     */
    static void pointListToLinePointsArray(@NonNull final Rect rect, float left, float scaleX, float top, float scaleY,
            @NonNull final FloatPrimitiveList points, @NonNull final List<Point> nodes) {
        points.clear(); // reset
        // loop over all nodes
        Point prevNode = null;
        Point lastDrawnNode = null;
        float lastDrawnNodeX = 0;
        float lastDrawnNodeY = 0;
        float prevX = 0f;
        float prevY = 0f;
        boolean thisIntersects = false;
        boolean nextIntersects = false;
        int nodesSize = nodes.size();
        if (nodesSize == 0) {
            return;
        }
        Point nextNode = nodes.get(0);
        float nextNodeX = left + ((float) nextNode.longitude() * scaleX);
        float nextNodeY = top + ((float) nextNode.latitude() * scaleY);

        float nodeX;
        float nodeY;
        boolean didntIntersect = false;
        boolean lastDidntIntersect = false;
        for (int i = 0; i < nodesSize; i++) {
            Point node = nextNode;
            nodeX = nextNodeX;
            nodeY = nextNodeY;
            nextIntersects = true;
            if (i < nodesSize - 1) {
                nextNode = nodes.get(i + 1);
                nextNodeX = left + ((float) nextNode.longitude() * scaleX);
                nextNodeY = top + ((float) nextNode.latitude() * scaleY);
                nextIntersects = isIntersectionPossible(rect, nextNodeX, nextNodeY, nodeX, nodeY);
            } else {
                nextNode = null;
            }
            didntIntersect = true;
            if (prevNode != null && (thisIntersects || nextIntersects
                    || (!(nextNode != null && lastDrawnNode != null) || isIntersectionPossible(rect, nextNodeX, nextNodeY, lastDrawnNodeX, lastDrawnNodeY)))) { // NOSONAR
                if (lastDidntIntersect) { // last segment didn't intersect
                    prevX = (float) (left + prevNode.longitude() * scaleX);
                    prevY = (float) (top + prevNode.latitude() * scaleY);
                }
                // Line segment needs to be drawn
                points.add(prevX);
                points.add(prevY);
                points.add(nodeX);
                points.add(nodeY);
                lastDrawnNode = node;
                lastDrawnNodeY = nodeY;
                lastDrawnNodeX = nodeX;
                didntIntersect = false;
            }
            lastDidntIntersect = didntIntersect;
            prevNode = node;
            prevX = nodeX;
            prevY = nodeY;
            thisIntersects = nextIntersects;
        }
    }

    /**
     * Checks if an intersection with a line between lat/lon and lat2/lon2 is possible. If two coordinates are outside
     * of a border, no intersection is possible.
     * 
     * @param rect the rect to test against
     * @param x x coordinate of 1st node
     * @param y y coordinate of 1st node
     * @param x2 x coordinate of 2nd node
     * @param y2 y coordinate of 2nd node
     * @return true, when an intersection is possible.
     */
    private static boolean isIntersectionPossible(final Rect rect, final float x, final float y, final float x2, final float y2) {
        return !(y < rect.top && y2 < rect.top || y > rect.bottom && y2 > rect.bottom || x > rect.right && x2 > rect.right || x < rect.left && x2 < rect.left);
    }

    /**
     * Set a dash pattern
     * 
     * @param dashArray an array of floats indicating relative lengths of on/off segments
     */
    public void setDashArray(@Nullable List<Float> dashArray) {
        if (dashArray != null) {
            this.dashArray = new float[dashArray.size()];
            for (int i = 0; i < dashArray.size(); i++) {
                this.dashArray[i] = dashArray.get(i);
            }
            setDashArrayOnPaint();
        } else {
            this.dashArray = null;
            paint.setPathEffect(null);
        }
    }

    /**
     * Set up the dash array in the paint object
     * 
     * This multiplies the dash lengths with the stroke width
     */
    private void setDashArrayOnPaint() {
        if (dashArray != null && dashArray.length > 0) {
            float[] temp = new float[dashArray.length];
            float width = Math.max(1, paint.getStrokeWidth());
            for (int i = 0; i < dashArray.length; i++) {
                temp[i] = dashArray[i] * width;
            }
            DashPathEffect dp = new DashPathEffect(temp, 0);
            paint.setPathEffect(dp);
        }
    }

    @Override
    @NonNull
    public String toString() {
        return super.toString() + " " + getClass().getSimpleName() + " " + Integer.toHexString(paint.getColor()) + " " + paint.getStrokeWidth();
    }

    /**
     * Read serialized object
     * 
     * @param stream the input stream
     * @throws IOException if reading fails
     * @throws ClassNotFoundException if the Class to deserialize can't be found
     */
    private void readObject(@NonNull ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        points = new FloatPrimitiveList(1000);
    }
}
