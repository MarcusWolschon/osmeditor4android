package io.vespucci.util;

import static io.vespucci.contract.Constants.LOG_TAG_LEN;
import static io.vespucci.util.Winding.COLINEAR;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.App;
import io.vespucci.exception.OsmIllegalOperationException;
import io.vespucci.osm.BoundingBox;
import io.vespucci.osm.Node;
import io.vespucci.osm.OsmElement;
import io.vespucci.osm.Relation;
import io.vespucci.osm.RelationMember;
import io.vespucci.osm.ViewBox;
import io.vespucci.osm.Way;

public final class Geometry {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, Geometry.class.getSimpleName().length());
    private static final String DEBUG_TAG = Geometry.class.getSimpleName().substring(0, TAG_LEN);

    public static final double PI_2 = 2 * Math.PI;

    /**
     * Private constructor
     */
    private Geometry() {
        // don't instantiate
    }

    /**
     * Calculate the centroid of a way
     * 
     * @param v current display bounding box
     * @param w screen width
     * @param h screen height
     * @param way the Way
     * @return WS84*17E coordinates [lat/lon] of the centroid or an empty array if they could not be determined
     */
    @NonNull
    public static int[] centroid(int w, int h, @NonNull ViewBox v, @NonNull final Way way) {
        Coordinates c = centroidXY(w, h, v, way);
        if (c == null) {
            return new int[0];
        }
        int lat = GeoMath.yToLatE7(h, w, v, (float) c.y);
        int lon = GeoMath.xToLonE7(w, v, (float) c.x);
        return new int[] { lat, lon };
    }

    /**
     * Calculate the centroid of a way
     * 
     * @param w screen width
     * @param h screen height
     * @param v current display bounding box
     * @param way way to caculate the centroid of
     * @return screen coordinates of centroid, null if the way has problems and if the way has length or area zero
     *         return the coordinates of the first node
     */
    @Nullable
    public static Coordinates centroidXY(int w, int h, @NonNull ViewBox v, @Nullable final Way way) {
        if (way == null || way.nodeCount() == 0) {
            return null;
        }
        Coordinates[] coords = Coordinates.nodeListToCoordinateArray(w, h, v, way.getNodes());
        return centroidXY(coords, false);
    }

    /**
     * Calculate the centroid of a list of RelationMembers arranged in a ring
     * 
     * @param v current display bounding box
     * @param w screen width
     * @param h screen height
     * @param ring list of RelationMembers
     * @return WS84*17E coordinates [lat/lon] of the centroid or an empty array if they could not be determined
     */
    @NonNull
    public static int[] centroid(int w, int h, @NonNull ViewBox v, @NonNull final List<RelationMember> ring) {
        Coordinates c = centroidXY(w, h, v, ring);
        if (c == null) {
            return new int[0];
        }
        int lat = GeoMath.yToLatE7(h, w, v, (float) c.y);
        int lon = GeoMath.xToLonE7(w, v, (float) c.x);
        return new int[] { lat, lon };
    }

    /**
     * Calculate the centroid of a list of RelationMembers arranged in a ring
     * 
     * @param w screen width
     * @param h screen height
     * @param v current display bounding box
     * @param ring list of RelationMembers
     * @return screen coordinates of centroid, null if the ring has problems return the coordinates of the first node
     */
    @Nullable
    public static Coordinates centroidXY(int w, int h, @NonNull ViewBox v, @NonNull final List<RelationMember> ring) {
        LinkedHashSet<Node> nodes = new LinkedHashSet<>();
        for (RelationMember rm : ring) {
            OsmElement e = rm.getElement();
            if (e instanceof Way) {
                nodes.addAll(((Way) e).getNodes());
            }
        }
        Coordinates[] coords = Coordinates.nodeListToCoordinateArray(w, h, v, new ArrayList<>(nodes));
        return centroidXY(coords, true);
    }

    /**
     * Calculate the centroid of a List of Coordinates representing a polygon or a line
     * 
     * The polygon or way that is defined by the Nodes should be non-self-intersecting and for polygons the last Node
     * should be equal to the first (OSM semantics)
     * 
     * @param coords array of screen coordinates
     * @param close close way (non-OSM semantics)
     * @return screen coordinates of centroid, null if the way has problems and if the way has length or area zero
     *         return the coordinates of the first node
     */
    @Nullable
    public static Coordinates centroidXY(@NonNull final Coordinates[] coords, boolean close) {
        int vs = coords.length;

        double y = 0;
        double x = 0;
        double x1 = coords[0].x;
        double y1 = coords[0].y;
        if (close || coords[0].equals(coords[vs - 1])) { // closed as per OSM definition
            // see http://paulbourke.net/geometry/polygonmesh/
            double a = 0;
            for (int i = 0; i < (vs - 1); i++) {
                double x2 = coords[i + 1].x;
                double y2 = coords[i + 1].y;
                double d = x1 * y2 - x2 * y1;
                a += d;
                x += (x1 + x2) * d;
                y += (y1 + y2) * d;
                x1 = x2;
                y1 = y2;
            }
            if (close) {
                double d = x1 * coords[0].y - coords[0].x * y1;
                a += d;
                x += (x1 + coords[0].x) * d;
                y += (y1 + coords[0].y) * d;
            }
            if (Util.notZero(a)) {
                y = y / (3 * a); // NOSONAR nonZero tests for zero
                x = x / (3 * a); // NOSONAR nonZero tests for zero
                return new Coordinates(x, y);
            } else {
                return coords[0];
            }
        } else { //
            double l = 0;
            for (int i = 0; i < (vs - 1); i++) {
                double x2 = coords[i + 1].x;
                double y2 = coords[i + 1].y;
                double len = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
                l += len;
                x += len * (x1 + x2) / 2;
                y += len * (y1 + y2) / 2;
                x1 = x2;
                y1 = y2;
            }
            if (Util.notZero(l)) {
                y = y / l; // NOSONAR nonZero tests for zero
                x = x / l; // NOSONAR nonZero tests for zero
                return new Coordinates(x, y);
            } else {
                return coords[0];
            }
        }
    }

    /**
     * Calculate the centroid of a way
     * 
     * Note this translates the way to originate at 0,0 and back to avoid rounding issues
     * 
     * @param way way to calculate the centroid of
     * @return WGS84 coordinates of centroid, null if the way has problems and if the way has length or area zero return
     *         the coordinates of the first node
     */
    @NonNull
    public static double[] centroidLonLat(@Nullable final Way way) {
        if (way == null || way.getNodes().isEmpty()) {
            return new double[0];
        }
        List<Node> nodes = way.getNodes();
        int size = nodes.size();
        boolean closed = way.isClosed() && size != 1; // handle 1 node ways
        size = closed ? size - 1 : size;
        Coordinates[] points = new Coordinates[size];
        Coordinates start = new Coordinates(nodes.get(0).getLon() / 1E7D, GeoMath.latE7ToMercator(nodes.get(0).getLat()));
        points[0] = new Coordinates(0, 0);
        // loop over all nodes
        for (int i = 1; i < size; i++) {
            points[i] = new Coordinates(nodes.get(i).getLon() / 1E7D - start.x, GeoMath.latE7ToMercator(nodes.get(i).getLat()) - start.y);
        }
        //
        Coordinates centroid = centroidXY(points, closed);
        return new double[] { centroid.x + start.x, GeoMath.mercatorToLat(centroid.y + start.y) };
    }

    /**
     * Get the centroid for an OsmElement
     * 
     * @param e the OsmElement
     * @return a double array containing lon - lat in WGS84 coords
     */
    @NonNull
    public static double[] centroid(@NonNull OsmElement e) {
        switch (e.getName()) {
        case Node.NAME:
            return new double[] { ((Node) e).getLon() / 1E7D, ((Node) e).getLat() / 1E7D };
        case Way.NAME:
            double[] result = Geometry.centroidLonLat((Way) e);
            if (result.length == 2) {
                return new double[] { result[0], result[1] };
            }
            break;
        case Relation.NAME:
            BoundingBox bbox = e.getBounds();
            if (bbox != null) {
                ViewBox box = new ViewBox(bbox);
                result = box.getCenter();
                return new double[] { result[0], result[1] };
            }
            break;
        default:
        }
        Log.d(DEBUG_TAG, "couldn't determine centroid for " + e);
        return new double[0];
    }

    /**
     * Checks if the x,y-position plus the tolerance is on a line between node1(x,y) and node2(x,y).
     * 
     * To avoid the typical two time calculation of the distance we actually return it
     * 
     * @param tolerance tolerance in screen pixel units
     * @param x screen X coordinate of the position
     * @param y screen Y coordinate of the position
     * @param node1X screen X coordinate of node1
     * @param node1Y screen Y coordinate of node1
     * @param node2X screen X coordinate of node2
     * @param node2Y screen Y coordinate of node2
     * @return distance &gt;= 0, when x,y plus way-tolerance lays on the line between node1 and node2.
     */
    public static double isPositionOnLine(final float tolerance, final float x, final float y, final float node1X, final float node1Y, final float node2X,
            final float node2Y) {
        // noinspection SuspiciousNameCombination
        if (GeoMath.isBetween(x, node1X, node2X, tolerance) && GeoMath.isBetween(y, node1Y, node2Y, tolerance)) {
            double distance = GeoMath.getLineDistance(x, y, node1X, node1Y, node2X, node2Y);
            return distance < tolerance ? distance : -1D;
        }
        return -1D;
    }

    /**
     * Get the winding direction of three Nodes
     * 
     * @param n1 1st Node
     * @param n2 2nd Node
     * @param n3 3rd Node
     * @return an int indication winding
     */
    private static int winding(@NonNull Node n1, @NonNull Node n2, @NonNull Node n3) {
        List<Node> list = new ArrayList<>();
        list.add(n1);
        list.add(n2);
        list.add(n3);
        return Winding.winding(list);
    }

    /**
     * Given three colinear Nodes start, intermediate, end, the function checks if the intermediate Node lies on line
     * segment
     * 
     * Note assumes planar geometry
     * 
     * @param start segment start Node
     * @param intermediate intermediate Node
     * @param end segment end Node
     * @return true if q lies on the segment
     */
    private static boolean onSegment(@NonNull Node start, @NonNull Node intermediate, @NonNull Node end) {
        return intermediate.getLon() <= Math.max(start.getLon(), end.getLon()) && intermediate.getLon() >= Math.min(start.getLon(), end.getLon())
                && intermediate.getLat() <= Math.max(start.getLat(), end.getLat()) && intermediate.getLat() >= Math.min(start.getLat(), end.getLat());
    }

    /**
     * Check if the segments/lines between p1-q1 and p2-q2 intersect
     * 
     * Note assumes planar geometry
     * 
     * @param start1 start Node segment 1
     * @param end1 end Node segment 1
     * @param start2 start Node segment 2
     * @param end2 end Node segment 2
     * @return true if the lines intersect
     */
    private static boolean intersect(@NonNull Node start1, @NonNull Node end1, @NonNull Node start2, @NonNull Node end2) {
        int w1 = winding(start1, end1, start2);
        int w2 = winding(start1, end1, end2);
        int w3 = winding(start2, end2, start1);
        int w4 = winding(start2, end2, end1);

        // General case
        if (w1 != w2 && w3 != w4) {
            return true;
        }

        // Handle cases in which the intersection is on one of the ways
        if (w1 == COLINEAR && onSegment(start1, start2, end1)) {
            return true;
        }

        if (w2 == COLINEAR && onSegment(start1, end2, end1)) {
            return true;
        }

        if (w3 == COLINEAR && onSegment(start2, start1, end2)) {
            return true;
        }

        return w4 == COLINEAR && onSegment(start2, end1, end2);
    }

    /**
     * Check if Node lies inside a polygon
     * 
     * Based on the code and algorithm from
     * https://www.geeksforgeeks.org/how-to-check-if-a-given-point-lies-inside-a-polygon/
     *
     * Note: assumes planar geometry
     * 
     * @param polygon a Array of Node defining the polygon
     * @param node the Node
     * @return true if the Node is in the polygon
     */
    public static boolean isInside(@NonNull Node[] polygon, @NonNull Node node) {
        int n = polygon.length;
        if (n < 3) {
            throw new IllegalArgumentException("There must be at least 3 vertices in a polygon");
        }

        Node extreme = App.getDelegator().getFactory().createNodeWithNewId(node.getLat(), (int) (200 * 1E7));
        int intersections = 0;
        int i = 0;
        do {
            int next = (i + 1) % n;
            if (intersect(polygon[i], polygon[next], node, extreme)) {
                if (winding(polygon[i], node, polygon[next]) == COLINEAR) {
                    return onSegment(polygon[i], node, polygon[next]);
                }
                intersections++;
            }
            i = next;
        } while (i != 0);
        return (intersections % 2 == 1);
    }

    /**
     * Offset the line defined by the coordinates
     * 
     * x0 y0, x1 y1, x1 y1, x2 y2, ... , x5 y5 , x5 y5, x6, y6
     * 
     * A positive offset with offset to the right in line direction, negative to the left.
     * 
     * See https://stackoverflow.com/a/68109283/16623964 (note it is missing a "2")
     * 
     * @param input array of coordinates in "drawLine" format
     * @param output output array, this can be the same as input, if the values do not need to be preserved
     * @param length in use number of elements in the array
     * @param closed true if closed
     * @param offset the offset
     */
    public static void offset(@NonNull float[] input, @NonNull float[] output, int length, boolean closed, float offset) {
        if (length < 4) {
            throw new IllegalArgumentException("There must be at least 2 vertices in a line");
        }

        final double limit = 2 * Math.abs(offset);

        float d1x = input[2] - input[0];
        float d1y = input[3] - input[1];
        double len1 = Math.hypot(d1x, d1y);
        double prevNy = -d1x / len1;
        double prevNx = d1y / len1;
        double startnx = prevNx;
        double startny = prevNy;
        if (!closed) {
            // 1st vertex
            output[0] = (float) (input[0] + offset * prevNx);
            output[1] = (float) (input[1] + offset * prevNy);
        }

        double n2y = 0;
        double n2x = 0;
        for (int i = 3; i < length; i += 4) {

            final int nextX = (i + 1) % length;
            final int nextY = (i + 2) % length;

            if (i == length - 1) { // last iteration
                if (closed) {
                    n2y = startny;
                    n2x = startnx;
                } else {
                    // last vertex
                    output[i - 1] = (float) (input[i - 1] + offset * prevNx);
                    output[i] = (float) (input[i] + offset * prevNy);
                    break;
                }
            } else {
                float d2x = input[(i + 3) % length] - input[nextX];
                float d2y = input[(i + 4) % length] - input[nextY];
                double len2 = Math.hypot(d2x, d2y);
                n2y = -d2x / len2;
                n2x = d2y / len2;
            }

            double bisx = prevNx + n2x;
            double bisy = prevNy + n2y;

            double lenbis = Math.hypot(bisx, bisy);
            bisx = bisx / lenbis;
            bisy = bisy / lenbis;

            double l = offset / Math.sqrt((1 + prevNx * n2x + prevNy * n2y) / 2);

            if (Math.abs(l) > limit) {
                l = Math.signum(l) * limit;
            }

            float newX = (float) (input[i - 1] + l * bisx);
            float newY = (float) (input[i] + l * bisy);

            output[i - 1] = newX;
            output[i] = newY;
            output[nextX] = newX;
            output[nextY] = newY;

            prevNx = n2x;
            prevNy = n2y;
        }
    }

    public static class Circle {
        public final Coordinates center;
        public final double      radius;

        public Circle(@NonNull Coordinates c, double r) {
            center = c;
            radius = r;
        }
    }

    /**
     * From a list of coordinates calculate the best fitting center and radius of a circle
     * 
     * See https://www.scribd.com/document/14819165/Regressions-coniques-quadriques-circulaire-spherique
     * 
     * @param coords a list of non-colinear coordinates
     * @return a Circle object
     */
    @NonNull
    public static Circle calculateCircle(@NonNull Coordinates[] coords) {
        Coordinates[] translated = new Coordinates[coords.length];

        for (int i = 0; i < coords.length; i++) {
            translated[i] = new Coordinates(coords[i].x - coords[0].x, coords[i].y - coords[0].y);
        }
        double sumX = sum(translated, c -> c.x);
        double sumX2 = sum(translated, c -> c.x * c.x);
        double sumY = sum(translated, c -> c.y);
        double sumY2 = sum(translated, c -> c.y * c.y);

        int n = translated.length;
        double s11 = n * sum(translated, c -> c.x * c.y) - sumX * sumY;
        double s20 = n * sumX2 - sumX * sumX;
        double s02 = n * sumY2 - sumY * sumY;
        double s30 = n * sum(translated, c -> c.x * c.x * c.x) - sumX2 * sumX;
        double s03 = n * sum(translated, c -> c.y * c.y * c.y) - sumY2 * sumY;
        double s21 = n * sum(translated, c -> c.x * c.x * c.y) - sumX2 * sumY;
        double s12 = n * sum(translated, c -> c.x * c.y * c.y) - sumY2 * sumX;

        double d = 2 * (s20 * s02 - s11 * s11);
        if (!Util.notZero(d)) {
            throw new OsmIllegalOperationException("calculateCircle called with colinear nodes");
        }
        double x = ((s30 + s12) * s02 - (s03 + s21) * s11) / d; // NOSONAR
        double y = ((s03 + s21) * s20 - (s30 + s12) * s11) / d; // NOSONAR

        double c = (sumX2 + sumY2 - 2 * x * sumX - 2 * y * sumY) / n;

        double r = Math.sqrt(c + x * x + y * y);

        return new Circle(new Coordinates(x + coords[0].x, y + coords[0].y), r);
    }

    /**
     * Calculate on screen centroid from a list of coordinates of a polygon
     * 
     * While this duplicates code from above, this makes sense in situations in which the coordinates are in the float
     * array format.
     * 
     * @param linePoints an array holding the coordinates
     * @param length the length of the usable data in the array
     * @param centroid a preallocated Coordinates object
     * @return the provided Coordinates object or null
     */
    @Nullable
    public static Coordinates centroidFromPointlist(@NonNull float[] linePoints, int length, @NonNull Coordinates centroid) {
        double area = 0;
        double y = 0;
        double x = 0;
        double x1 = linePoints[0];
        double y1 = linePoints[1];
        for (int i = 0; i < length; i = i + 2) {
            double x2 = linePoints[(i + 2) % length];
            double y2 = linePoints[(i + 3) % length];
            double d = x1 * y2 - x2 * y1;
            area = area + d;
            x = x + (x1 + x2) * d;
            y = y + (y1 + y2) * d;
            x1 = x2;
            y1 = y2;
        }
        if (Util.notZero(area)) {
            centroid.set(x / (3 * area), y / (3 * area)); // NOSONAR nonZero tests for zero
            return centroid;
        }
        return null;
    }

    /**
     * Calculate on screen midpoint from a list of coordinates of a line
     * 
     * While this duplicates code from above, this makes sense in situations in which the coordinates are in the float
     * array format.
     * 
     * @param linePoints an array holding the coordinates
     * @param length the length of the usable data in the array
     * @param midpoint a preallocated Coordinates object
     * @return the provided Coordinates object or null
     */
    @Nullable
    public static Coordinates midpointFromPointlist(@NonNull float[] linePoints, int length, @NonNull Coordinates midpoint) {
        double y = 0;
        double x = 0;
        double x1 = linePoints[0];
        double y1 = linePoints[1];
        double l = 0;
        for (int i = 0; i < length - 2; i = i + 2) {
            double x2 = linePoints[i + 2];
            double y2 = linePoints[i + 3];
            double len = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
            l += len;
            x += len * (x1 + x2) / 2;
            y += len * (y1 + y2) / 2;
            x1 = x2;
            y1 = y2;
        }
        if (Util.notZero(l)) {
            midpoint.set(x / l, y / l); // NOSONAR nonZero tests for zero
            return midpoint;
        }
        return null;
    }

    interface Op {
        double calc(Coordinates c);
    }

    private static double sum(Coordinates[] coords, Op op) {
        double result = 0;
        for (Coordinates c : coords) {
            result += op.calc(c);
        }
        return result;
    }
}
