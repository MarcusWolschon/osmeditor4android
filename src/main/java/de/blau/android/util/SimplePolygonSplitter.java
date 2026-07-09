package de.blau.android.util;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

import android.util.Log;
import androidx.annotation.NonNull;

/**
 * This splits a simple polygon either vertically or horizontally
 * 
 * Potentially this could be replaced by a Clipper2 based implementation
 */
public class SimplePolygonSplitter {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, SimplePolygonSplitter.class.getSimpleName().length());
    private static final String DEBUG_TAG = SimplePolygonSplitter.class.getSimpleName().substring(0, TAG_LEN);

    private static final double EPSILON = 1e-9;

    private SimplePolygonSplitter() {
        /* Private */
    }

    /**
     * Splits a hole-free (no inners) Polygon along a vertical or horizontal coordinate line.
     * 
     * @param polygon the input Polygon
     * @param splitPosition position to split at
     * @param isVertical true if we are splitting vertically
     * @return a List of Polygons
     */
    @NonNull
    public static List<Polygon> split(@NonNull Polygon polygon, double splitPosition, boolean isVertical) {
        List<Polygon> output = new ArrayList<>();
        List<List<Point>> rings = polygon.coordinates();
        if (rings == null || rings.isEmpty()) {
            Log.e(DEBUG_TAG, "No rings");
            return output;
        }

        List<Point> poly = rings.get(0);
        if (poly.size() < 4) {
            Log.e(DEBUG_TAG, "Degenerate ring");
            return output;
        }

        // Ensure the loop is open during calculations for clean iteration
        List<Point> openPoly = new ArrayList<>(poly);
        if (arePointsEqual(openPoly.get(0), openPoly.get(openPoly.size() - 1))) {
            openPoly.remove(openPoly.size() - 1);
        }

        // if the split position is exactly on the relevant coordinate things will break
        // we move the position a bit to avoid this, the change has be larger than
        // our equality epsilon to have any effect though
        for (Point vertex : openPoly) {
            double val = isVertical ? vertex.longitude() : vertex.latitude();
            if (Math.abs(val - splitPosition) < EPSILON) { // Exact match threshold
                splitPosition += EPSILON * 10;
                break;
            }
        }

        // intersect edges that cross the cut line
        List<Point[]> segments = new ArrayList<>();
        Set<Point> cutLinePoints = new HashSet<>();

        for (int i = 0; i < openPoly.size(); i++) {
            Point p1 = openPoly.get(i);
            Point p2 = openPoly.get((i + 1) % openPoly.size());

            Point inter = getIntersection(p1, p2, splitPosition, isVertical);
            if (inter != null && !arePointsEqual(inter, p1) && !arePointsEqual(inter, p2)) {
                segments.add(new Point[] { p1, inter });
                segments.add(new Point[] { inter, p2 });
            } else {
                segments.add(new Point[] { p1, p2 });
            }
        }

        // separate edges segments by side
        List<Point[]> sideAEdges = new ArrayList<>();
        List<Point[]> sideBEdges = new ArrayList<>();

        for (Point[] segment : segments) {
            Point s1 = segment[0];
            Point s2 = segment[1];

            double midLng = (s1.longitude() + s2.longitude()) / 2.0;
            double midLat = (s1.latitude() + s2.latitude()) / 2.0;
            double midVal = isVertical ? midLng : midLat;

            if (Math.abs(midVal - splitPosition) < EPSILON) {
                // Completely collinear with the cut line: belongs to both boundaries
                sideAEdges.add(segment);
                sideBEdges.add(segment);
            } else if (midVal < splitPosition) {
                sideAEdges.add(segment);
            } else {
                sideBEdges.add(segment);
            }

            if (Math.abs((isVertical ? s1.longitude() : s1.latitude()) - splitPosition) < EPSILON) {
                cutLinePoints.add(s1);
            }
            if (Math.abs((isVertical ? s2.longitude() : s2.latitude()) - splitPosition) < EPSILON) {
                cutLinePoints.add(s2);
            }
        }

        // close the unclosed polygons along the cut line
        List<Point> sortedCutPoints = new ArrayList<>(cutLinePoints);
        Collections.sort(sortedCutPoints,
                (p1, p2) -> isVertical ? Double.compare(p1.latitude(), p2.latitude()) : Double.compare(p1.longitude(), p2.longitude()));

        for (int i = 0; i < sortedCutPoints.size() - 1; i++) {
            Point pt1 = sortedCutPoints.get(i);
            Point pt2 = sortedCutPoints.get(i + 1);

            double midLng = (pt1.longitude() + pt2.longitude()) / 2.0;
            double midLat = (pt1.latitude() + pt2.latitude()) / 2.0;
            Point midPoint = Point.fromLngLat(midLng, midLat);

            if (isPointInsidePolygon(midPoint, openPoly)) {
                // Bidirectional injection handles loop continuity on both independent cut faces
                sideAEdges.add(new Point[] { pt1, pt2 });
                sideAEdges.add(new Point[] { pt2, pt1 });

                sideBEdges.add(new Point[] { pt1, pt2 });
                sideBEdges.add(new Point[] { pt2, pt1 });
            }
        }

        // assemble Polygons from the edges
        output.addAll(assemblePolygonsFromDirectedEdges(sideAEdges));
        output.addAll(assemblePolygonsFromDirectedEdges(sideBEdges));

        return output;
    }

    /**
     * Find the intersection of the segment between p1 and p2 with either the vertical or horizontal line at val
     * 
     * @param p1 1st Point
     * @param p2 2nd Point
     * @param val coordinate of the vertical or horizontal line
     * @param isVertical if we are intersecting with a vertical or horizontal line
     * @return the intersection Point or null
     */
    private static Point getIntersection(@NonNull Point p1, @NonNull Point p2, double val, boolean isVertical) {
        final double lon1 = p1.longitude();
        final double lon2 = p2.longitude();
        final double lat1 = p1.latitude();
        final double lat2 = p2.latitude();
        if (isVertical) {
            if (Math.min(lon1, lon2) <= val && val <= Math.max(lon1, lon2) && lon1 != lon2) {
                double t = (val - lon1) / (lon2 - lon1);
                return Point.fromLngLat(val, lat1 + t * (lat2 - lat1));
            }
        } else {
            if (Math.min(lat1, lat2) <= val && val <= Math.max(lat1, lat2) && lat1 != lat2) {
                double t = (val - lat1) / (lat2 - lat1);
                return Point.fromLngLat(lon1 + t * (lon2 - lon1), val);
            }
        }
        return null;
    }

    /**
     * Determine if p is inside an (unclosed) polygon defined by a list of points
     * 
     * Arguably this could be replaced with TurfJoin.inside at the expense of building a Polygon from the Points.
     * 
     * @param p the Point
     * @param poly List of Points defining the Polygon
     * @return true if the point is inside
     */
    private static boolean isPointInsidePolygon(@NonNull Point p, @NonNull List<Point> poly) {
        boolean inside = false;
        int n = poly.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            Point pi = poly.get(i);
            Point pj = poly.get(j);
            if (((pi.latitude() > p.latitude()) != (pj.latitude() > p.latitude()))
                    && (p.longitude() < (pj.longitude() - pi.longitude()) * (p.latitude() - pi.latitude()) / (pj.latitude() - pi.latitude() + EPSILON)
                            + pi.longitude())) {
                inside = !inside;
            }
        }
        return inside;
    }

    /**
     * Topologically traces and builds valid closed rings from directed edge pools.
     * 
     * @param edges a List of edges
     * @return a List of Polygons
     */
    @NonNull
    private static List<Polygon> assemblePolygonsFromDirectedEdges(@NonNull List<Point[]> edges) {
        List<Polygon> polygons = new ArrayList<>();
        Map<String, List<Point>> adj = new HashMap<>();
        Set<String> visitedEdges = new HashSet<>();

        for (Point[] edge : edges) {
            if (arePointsEqual(edge[0], edge[1])) {
                continue;
            }
            String fromKey = makePointKey(edge[0]);

            List<Point> temp = adj.get(fromKey);
            if (temp == null) {
                temp = new ArrayList<>();
                adj.put(fromKey, temp);
            }
            temp.add(edge[1]);
        }

        for (Point[] edge : edges) {
            String edgeKey = makeEdgeKey(edge[0], edge[1]);
            if (visitedEdges.contains(edgeKey)) {
                continue;
            }
            List<Point> currentLoop = new ArrayList<>();
            Point startNode = edge[0];
            Point currentNode = edge[1];

            currentLoop.add(startNode);
            visitedEdges.add(edgeKey);

            boolean loopClosed = false;
            while (currentNode != null) {
                currentLoop.add(currentNode);
                if (arePointsEqual(currentNode, startNode)) {
                    loopClosed = true;
                    break;
                }

                String currentKey = makePointKey(currentNode);
                List<Point> neighbors = adj.containsKey(currentKey) ? adj.get(currentKey) : Collections.emptyList();
                Point nextNode = null;

                for (Point neighbor : neighbors) {
                    String candidateEdgeKey = makeEdgeKey(currentNode, neighbor);
                    if (!visitedEdges.contains(candidateEdgeKey)) {
                        nextNode = neighbor;
                        visitedEdges.add(candidateEdgeKey);
                        break;
                    }
                }
                currentNode = nextNode;
            }

            // A valid closed loop needs at least 3 distinct vertices (4 points total with closure)
            if (loopClosed && currentLoop.size() >= 4) {
                polygons.add(Polygon.fromLngLats(Collections.singletonList(currentLoop)));
            }
        }
        return polygons;
    }

    @NonNull
    private static String makePointKey(@NonNull Point p) {
        return String.format(Locale.US, "%.9f,%.9f", p.longitude(), p.latitude());
    }

    @NonNull
    private static String makeEdgeKey(@NonNull Point p1, @NonNull Point p2) {
        return makePointKey(p1) + "->" + makePointKey(p2);
    }

    /**
     * Check if two points are equals within EPSILON
     * 
     * @param p1 first Point
     * @param p2 second Point
     * @return true if equals
     */
    private static boolean arePointsEqual(@NonNull Point p1, @NonNull Point p2) {
        return Math.abs(p1.longitude() - p2.longitude()) < EPSILON && Math.abs(p1.latitude() - p2.latitude()) < EPSILON;
    }
}
