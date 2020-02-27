package de.blau.android.util;

import java.util.ArrayList;
import java.util.List;

import com.mapbox.geojson.CoordinateContainer;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.GeometryCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.ViewBox;
import de.blau.android.util.collections.FloatPrimitiveList;

/**
 * GeoJosn utilities
 * 
 * @author Simon Poole
 *
 */
public final class GeoJson {

    private static final String DEBUG_TAG = "GeoJson";

    /**
     * Private constructor to stop instantiation
     */
    private GeoJson() {
        // private
    }

    /**
     * Calculate the bounding boxes of a GeoJson Polygon feature
     * 
     * @param f The GeoJson feature
     * @return a List of BoundingBoxes, empty in no Polygons were found
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public static List<BoundingBox> getBoundingBoxes(@NonNull Feature f) {
        List<BoundingBox> result = new ArrayList<>();
        Geometry g = f.geometry();
        if (g instanceof Polygon) {
            for (List<Point> l : ((CoordinateContainer<List<List<Point>>>) g).coordinates()) {
                BoundingBox box = null;
                for (Point p : l) {
                    if (box == null) {
                        box = new BoundingBox(p.longitude(), p.latitude());
                    } else {
                        box.union(p.longitude(), p.latitude());
                    }
                }
                if (box != null) {
                    result.add(box);
                }
            }
        }
        return result;
    }

    /**
     * Determine the bounding box for GeoJSON geometries
     * 
     * @param g the GeoJSON Geometry
     * @return the bounding box
     */
    @Nullable
    public static BoundingBox getBounds(@NonNull Geometry g) {
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
     * Converts a geographical way/path/track to a list of screen-coordinate points for drawing.
     * 
     * Only segments that are inside the ViewBox are included. This duplicates the logic in Map for OSM objects for
     * GeoJSON (can't really be avoided)
     * 
     * @param box the current ViewBox
     * @param w screen width
     * @param h screen height
     * @param points list to (re-)use for projected points in the format expected by
     *            {@link Canvas#drawLines(float[], Paint)}
     * @param nodes A List of the Points to be drawn
     */
    public static void pointListToLinePointsArray(@NonNull ViewBox box, int w, int h, @NonNull final FloatPrimitiveList points,
            @NonNull final List<Point> nodes) {
        points.clear(); // reset
        // loop over all nodes
        Point prevNode = null;
        Point lastDrawnNode = null;
        int lastDrawnNodeLonE7 = 0;
        int lastDrawnNodeLatE7 = 0;
        float prevX = 0f;
        float prevY = 0f;
        boolean thisIntersects = false;
        boolean nextIntersects = false;
        int nodesSize = nodes.size();
        if (nodesSize > 0) {
            Point nextNode = nodes.get(0);
            double nextNodeLat = nextNode.latitude();
            double nextNodeLon = nextNode.longitude();
            int nextNodeLatE7 = (int) (nextNodeLat * 1E7);
            int nextNodeLonE7 = (int) (nextNodeLon * 1E7);
            float X = -Float.MAX_VALUE;
            float Y = -Float.MAX_VALUE;
            for (int i = 0; i < nodesSize; i++) {
                Point node = nextNode;
                double nodeLon = nextNodeLon;
                double nodeLat = nextNodeLat;
                int nodeLonE7 = nextNodeLonE7;
                int nodeLatE7 = nextNodeLatE7;
                nextIntersects = true;
                if (i < nodesSize - 1) {
                    nextNode = nodes.get(i + 1);
                    nextNodeLat = nextNode.latitude();
                    nextNodeLon = nextNode.longitude();
                    nextNodeLatE7 = (int) (nextNodeLat * 1E7);
                    nextNodeLonE7 = (int) (nextNodeLon * 1E7);
                    nextIntersects = box.isIntersectionPossible(nextNodeLonE7, nextNodeLatE7, nodeLonE7, nodeLatE7);
                } else {
                    nextNode = null;
                }
                X = -Float.MAX_VALUE; // misuse this as a flag
                if (prevNode != null) {
                    if (thisIntersects || nextIntersects || (!(nextNode != null && lastDrawnNode != null)
                            || box.isIntersectionPossible(nextNodeLonE7, nextNodeLatE7, lastDrawnNodeLonE7, lastDrawnNodeLatE7))) {
                        X = GeoMath.lonToX(w, box, nodeLon);
                        Y = GeoMath.latToY(h, w, box, nodeLat);
                        if (prevX == -Float.MAX_VALUE) { // last segment didn't intersect
                            prevX = GeoMath.lonToX(w, box, prevNode.longitude());
                            prevY = GeoMath.latToY(h, w, box, prevNode.latitude());
                        }
                        // Line segment needs to be drawn
                        points.add(prevX);
                        points.add(prevY);
                        points.add(X);
                        points.add(Y);
                        lastDrawnNode = node;
                        lastDrawnNodeLatE7 = nodeLatE7;
                        lastDrawnNodeLonE7 = nodeLonE7;
                    }
                }
                prevNode = node;
                prevX = X;
                prevY = Y;
                thisIntersects = nextIntersects;
            }
        }
    }
}
