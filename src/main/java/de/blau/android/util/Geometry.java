package de.blau.android.util;

import java.util.List;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import de.blau.android.osm.Node;
import de.blau.android.osm.ViewBox;
import de.blau.android.osm.Way;
import de.blau.android.resources.DataStyle;

public final class Geometry {

    /**
     * Private constructor
     */
    private Geometry() {
        // don't instantiate
    }

    /**
     * calculate the centroid of a way
     * 
     * @param v current display bounding box
     * @param w screen width
     * @param h screen height
     * @param way the Way
     * @return WS84*17E coordinates of the centroid or null if they could not be determined
     */
    @Nullable
    public static int[] centroid(int w, int h, @NonNull ViewBox v, @NonNull final Way way) {
        Coordinates c = centroidXY(w, h, v, way);
        if (c == null) {
            return null;
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
        Coordinates[] coords = Coordinates.nodeListToCooardinateArray(w, h, v, way.getNodes());
        return centroidXY(coords, false);
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
     * @param way way to calculate the centroid of
     * @return WGS84 coordinates of centroid, null if the way has problems and if the way has length or area zero return
     *         the coordinates of the first node
     */
    @Nullable
    public static double[] centroidLonLat(@Nullable final Way way) {
        if (way == null || way.getNodes().isEmpty()) {
            return null;
        }
        List<Node> nodes = way.getNodes();
        Coordinates[] points = new Coordinates[nodes.size()];
        // loop over all nodes
        for (int i = 0; i < nodes.size(); i++) {
            points[i] = new Coordinates(0.0f, 0.0f);
            points[i].x = nodes.get(i).getLon() / 1E7D;
            points[i].y = GeoMath.latE7ToMercator(nodes.get(i).getLat());
        }
        //
        Coordinates centroid = centroidXY(points, false);
        return new double[] { centroid.x, GeoMath.mercatorToLat(centroid.y) };
    }

    /**
     * Checks if the x,y-position plus the tolerance is on a line between node1(x,y) and node2(x,y).
     * 
     * To avoid the typical two time calculation of the distance we actually return it
     * 
     * @param x screen X coordinate of the position
     * @param y screen Y coordinate of the position
     * @param node1X screen X coordinate of node1
     * @param node1Y screen Y coordinate of node1
     * @param node2X screen X coordinate of node2
     * @param node2Y screen Y coordinate of node2
     * @return distance &gt;= 0, when x,y plus way-tolerance lays on the line between node1 and node2.
     */
    public static double isPositionOnLine(final float x, final float y, final float node1X, final float node1Y, final float node2X, final float node2Y) {
        return isPositionOnLine(DataStyle.getCurrent().getWayToleranceValue() / 2f, x, y, node1X, node1Y, node2X, node2Y);
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
}
