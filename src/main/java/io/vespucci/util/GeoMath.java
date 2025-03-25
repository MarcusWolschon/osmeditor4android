package io.vespucci.util;

import java.util.Collections;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Paint;
import androidx.annotation.NonNull;
import io.vespucci.exception.OsmException;
import io.vespucci.osm.BoundingBox;
import io.vespucci.osm.GeoPoint;
import io.vespucci.osm.ViewBox;
import io.vespucci.util.collections.FloatPrimitiveList;

/**
 * GeoMath provides some calculating functions for mercator projection conversions and other math-utils.
 * 
 * @author mb
 */
public final class GeoMath {

    protected static final String DEBUG_TAG = GeoMath.class.getSimpleName().substring(0, Math.min(23, GeoMath.class.getSimpleName().length()));

    private static final double _180_PI = 180d / Math.PI;

    public static final double _360_PI = 360d / Math.PI;

    private static final double PI_360 = Math.PI / 360d;

    private static final double PI_180 = Math.PI / 180d;

    private static final double PI_4 = Math.PI / 4d;

    private static final double PI_2 = Math.PI / 2d;

    public static final double MAX_LAT           = 90;
    public static final int    MAX_LAT_E7        = (int) (MAX_LAT * 1E7);
    /**
     * Maximum latitude value that can still be projected to web mercator
     */
    public static final double MAX_COMPAT_LAT    = Math.toDegrees(Math.atan(Math.sinh(Math.PI)));
    public static final int    MAX_COMPAT_LAT_E7 = (int) (MAX_COMPAT_LAT * 1E7);

    public static final double MAX_LON    = 180;
    public static final int    MAX_LON_E7 = (int) (MAX_LON * 1E7);

    public static final double MAX_MLAT    = GeoMath.latE7ToMercator(MAX_COMPAT_LAT_E7);
    public static final int    MAX_MLAT_E7 = GeoMath.latE7ToMercatorE7(MAX_COMPAT_LAT_E7);

    public static final int EARTH_RADIUS_EQUATOR = 6378137;

    public static final double EARTH_RADIUS_PI    = Math.PI * EARTH_RADIUS_EQUATOR;
    public static final int    EARTH_RADIUS_POLAR = 6356752;
    /**
     * The arithmetic middle of the two WGS84 reference-ellipsoids.
     */
    public static final int    EARTH_RADIUS       = (EARTH_RADIUS_EQUATOR + EARTH_RADIUS_POLAR) / 2;

    /**
     * Private constructor to stop instantiation
     */
    private GeoMath() {
        // private
    }

    /**
     * Check if a pair of coordinates have Mercartor compatible WGS84 values
     * 
     * @param lon the longitude
     * @param lat the latitude
     * @return true if in range
     */
    public static boolean coordinatesInCompatibleRange(double lon, double lat) {
        return lon >= -GeoMath.MAX_LON && lon <= GeoMath.MAX_LON && lat >= -GeoMath.MAX_COMPAT_LAT && lat <= GeoMath.MAX_COMPAT_LAT;
    }

    /**
     * Checks if x is between a and b (or equals a or b).
     * 
     * @param x value to check
     * @param a boundary a
     * @param b boundary b
     * @return true, if x is between a or b, or equals a or b.
     */
    public static boolean isBetween(final float x, final float a, final float b) {
        return (a > b) ? x <= a && x >= b : x <= b && x >= a;
    }

    /**
     * Checks if x is between a and b (or equals a or b).
     * 
     * @param x value to check
     * @param a boundary a
     * @param b boundary b
     * @return true, if x is between a or b, or equals a or b.
     */
    public static boolean isBetween(final int x, final int a, final int b) {
        return (a > b) ? x <= a && x >= b : x <= b && x >= a;
    }

    /**
     * Checks if x is between a and b plus the given offset.
     * 
     * @param x value to check
     * @param a boundary a
     * @param b boundary b
     * @param offset an offset to add/subtract to the boundaries
     * @return true, if x is between a or b, or equals a or b, + or - the offset
     */
    public static boolean isBetween(final float x, final float a, final float b, final float offset) {
        return (a > b) ? x <= a + offset && x >= b - offset : x <= b + offset && x >= a - offset;
    }

    /**
     * Checks if x is between a and b plus the given offset.
     * 
     * @param x value to check
     * @param a boundary a
     * @param b boundary b
     * @param offset an offset to add/subtract to the boundaries
     * @return true, if x is between a or b, or equals a or b, + or - the offset
     */
    public static boolean isBetween(final double x, final double a, final double b, final double offset) {
        return (a > b) ? x <= a + offset && x >= b - offset : x <= b + offset && x >= a - offset;
    }

    /**
     * Calculates a projected coordinate for a given latitude value. When lat is bigger than MAX_LAT, it will be clamped
     * to MAX_LAT.
     * 
     * NOTE: this is a degree, not a meter value
     * 
     * @param lat the latitude as double value
     * @return the mercator-projected y-coordinate for a cartesian coordinate system in degrees.
     * 
     * @see <a href="http://en.wikipedia.org/wiki/Mercator_projection#Mathematics_of_the_projection">Mathematics of the
     *      projection</a>
     */
    // TODO clamping is likely to lead to issues for objects extending past +-MAX_LAT given that they will never be in a
    // drawing box this should simple throw an exception and let the drawing code handle it
    public static double latToMercator(double lat) {
        lat = Math.min(MAX_COMPAT_LAT, lat);
        lat = Math.max(-MAX_COMPAT_LAT, lat);
        return _180_PI * Math.log(Math.tan(lat * PI_360 + PI_4));
    }

    /**
     * Convert a WGS84*1E7 latitude value to a mercator projected one
     * 
     * @param latE7 the latitude multiplied by 1E7
     * @return the mercator projected value
     * 
     * @see #latToMercator(double)
     */
    public static double latE7ToMercator(final int latE7) {
        return latToMercator(latE7 / 1E7D);
    }

    /**
     * @param latE7 the latitude multiplied by 1E7
     * @return the mercator-projected y-coordinate for a cartesian coordinate system, multiplied by 1E7.
     *
     * @see #latToMercator(double)
     */
    public static int latE7ToMercatorE7(final int latE7) {
        return (int) Math.round((latToMercator(latE7 / 1E7d) * 1E7d));
    }

    /**
     * Converts a projected mercator coordinate to a geo-latitude value. This is the inverse function to
     * latToMercator(double).
     * 
     * @param mer the projected mercator coordinate
     * @return the latitude value.
     */
    public static double mercatorToLat(final double mer) {
        return _180_PI * (2d * Math.atan(Math.exp(mer * PI_180)) - PI_2);
    }

    /**
     * Converts a scaled mercator latitude to a WGS84 value
     * 
     * @param mer the mercator projected latitude, multiplied by 1E7
     * @return a WGS84 latitude value
     * 
     * @see #mercatorToLat(double)
     */
    public static double mercatorE7ToLat(final int mer) {
        return mercatorToLat(mer / 1E7d);
    }

    /**
     * Converts a mercator latitude to a scaled WGS84 value
     *
     * @param mer the mercator projected latitude
     * @return the latitude value, multiplied by 1E7
     * 
     *         #mercatorToLat(double)
     */
    public static int mercatorToLatE7(final double mer) {
        return (int) Math.round(mercatorToLat(mer) * 1E7d);
    }

    /**
     * Converts a scaled mercator latitude to a scaled WGS84 value
     *
     * @param mer the mercator projected coordinate, multiplied by 1E7
     * @return the latitude value, multiplied by 1E7
     * 
     *         #mercatorToLat(double)
     */
    public static int mercatorE7ToLatE7(final int mer) {
        return (int) Math.round(mercatorToLat(mer / 1E7d) * 1E7d);
    }

    /**
     * Calculate the smallest bounding box that contains a circle of the given radius around center
     * 
     * @param center center coordinates
     * @param radius Radius in metres to be contained in the box.
     * @return The BoundingBox that contains the specified area, values clamped to not cross the dateline)
     * @throws OsmException if the bounding box cannot be calculated
     */
    public static BoundingBox createBoundingBoxForCoordinates(IntCoordinates center, final double radius) throws OsmException {
        return createBoundingBoxForCoordinates(center.lat / 1E7D, center.lon / 1E7D, radius);
    }

    /**
     * Calculate the smallest bounding box that contains a circle of the given radius in metres centered at the given
     * lat/lon.
     * 
     * @param lat Latitude of box center (within maximum values that we can convert to mercator)
     * @param lon Longitude of box center
     * @param radius Radius in metres to be contained in the box.
     * @return The BoundingBox that contains the specified area, values clamped to not cross the dateline)
     * @throws OsmException if the bounding box cannot be calculated
     */
    public static BoundingBox createBoundingBoxForCoordinates(final double lat, final double lon, final double radius) throws OsmException {
        double radiusDegree = convertMetersToGeoDistance(radius);

        if (lat + radiusDegree > MAX_COMPAT_LAT || lat - radiusDegree < -MAX_COMPAT_LAT) {
            throw new OsmException("Latitude outside of range that can be projected to mercator coordinates");
        }

        double horizontalRadiusDegree = radiusDegree / Math.cos(Math.toRadians(lat)); //

        double left = lon - horizontalRadiusDegree;
        double right = lon + horizontalRadiusDegree;
        double bottom = lat - radiusDegree;
        double top = lat + radiusDegree;
        if (left < -MAX_LON) {
            left = -MAX_LON;
            right = left + radiusDegree * 2d;
        }
        if (right > MAX_LON) {
            right = MAX_LON_E7;
            left = right - radiusDegree * 2d;
        }
        if (bottom < -MAX_COMPAT_LAT) {
            bottom = -MAX_COMPAT_LAT;
            top = bottom + horizontalRadiusDegree * 2d;
        }
        if (top > MAX_COMPAT_LAT) {
            top = MAX_COMPAT_LAT;
            bottom = top - horizontalRadiusDegree * 2d;
        }
        return new BoundingBox(left, bottom, right, top);
    }

    /**
     * Converts a distance to a degree value
     * 
     * @param meters distance in meters
     * @return a value in degrees
     */
    public static double convertMetersToGeoDistance(final double meters) {
        return (_180_PI * meters) / EARTH_RADIUS;
    }

    /**
     * Converts a distance to a degrees * 1E7 value
     * 
     * @param meters distance in meters
     * @return a value in degrees * 1E7
     */
    public static int convertMetersToGeoDistanceE7(final double meters) {
        return (int) ((_180_PI * meters * 1E7d) / EARTH_RADIUS);
    }

    /**
     * Calculates the screen-coordinate to the given latitude.
     * 
     * @param screenHeight the height of the screen in px
     * @param screenWidth the width of the screen in px
     * @param viewBox the current ViewBox
     * @param latE7 latitude, WGS84 multiplied by 1E7.
     * @return the y screen-coordinate for this latitude value.
     */
    public static float latE7ToY(final int screenHeight, int screenWidth, final ViewBox viewBox, final int latE7) {
        return (float) (screenHeight - (latE7ToMercator(latE7) - viewBox.getBottomMercator()) * viewBox.getPixelRadius(screenWidth));
    }

    /**
     * Non scaled version. Calculates the screen-coordinate to the given latitude.
     * 
     * @param screenHeight the height of the screen in px
     * @param screenWidth the width of the screen in px
     * @param viewBox the current ViewBox
     * @param lat latitude, WGS84
     * @return the y screen-coordinate for this latitude value.
     */
    public static float latToY(final int screenHeight, int screenWidth, final ViewBox viewBox, final double lat) {
        return (float) (screenHeight - (latToMercator(lat) - viewBox.getBottomMercator()) * viewBox.getPixelRadius(screenWidth));
    }

    /**
     * Calculates the screen-coordinate to the given latitude.in mercator
     * 
     * @param screenHeight the height of the screen in px
     * @param screenWidth the width of the screen in px
     * @param viewBox the current ViewBox
     * @param lat latitude
     * @return the y screen-coordinate for this latitude value.
     */
    public static float latMercatorToY(final int screenHeight, int screenWidth, final ViewBox viewBox, final double lat) {
        return (float) (screenHeight - (lat - viewBox.getBottomMercator()) * viewBox.getPixelRadius(screenWidth));
    }

    /**
     * Calculates the screen-coordinate to the given latitude.in mercator
     * 
     * @param screenHeight the height of the screen in px
     * @param screenWidth the width of the screen in px
     * @param viewBox the current ViewBox
     * @param latE7 latitude, multiplied by 1E7.
     * @return the y screen-coordinate for this latitude value.
     */
    public static float latMercatorE7ToY(final int screenHeight, int screenWidth, final ViewBox viewBox, final int latE7) {
        return (float) (screenHeight - (latE7 / 1E7D - viewBox.getBottomMercator()) * viewBox.getPixelRadius(screenWidth));
    }

    /**
     * Non-scaled version. Calculates the screen-coordinate to the given longitude.
     * 
     * @param screenWidth the width of the screen in px
     * @param viewBox the current ViewBox
     * @param lonE7 the longitude, multiplied by 1E7.
     * @return the x screen-coordinate for this longitude value.
     */
    public static float lonE7ToX(final int screenWidth, final BoundingBox viewBox, final int lonE7) {
        return (float) ((double) (lonE7 - viewBox.getLeft()) / (double) viewBox.getWidth()) * screenWidth;
    }

    /**
     * Non-scaled version. Calculates the screen-coordinate to the given longitude.
     * 
     * @param screenWidth the width of the screen in px
     * @param viewBox the current ViewBox
     * @param lon the longitude
     * @return the x screen-coordinate for this longitude value.
     */
    public static float lonToX(final int screenWidth, final BoundingBox viewBox, final double lon) {
        return (float) ((lon * 1E7D - viewBox.getLeft()) / viewBox.getWidth()) * screenWidth;
    }

    /**
     * Calculates the latitude value for the given y screen coordinate.
     * 
     * @param screenHeight the height of the screen in px
     * @param screenWidth the width of the screen in px
     * @param viewBox the current ViewBox
     * @param y the y-coordinate from the screen
     * @return latitude representing by the given y-value, multiplied by 1E7
     */
    public static int yToLatE7(final int screenHeight, int screenWidth, final ViewBox viewBox, final float y) {
        double pixelRadius = screenWidth / (viewBox.getWidth() / 1E7d);
        double lat = mercatorToLatE7(viewBox.getBottomMercator() + ((double) screenHeight - y) / pixelRadius);
        return (int) lat;
    }

    /**
     * Calculates the longitude value for the given x screen coordinate.
     *
     * @param screenWidth current screen width in screen coordinates
     * @param viewBox the current VieeBox
     * @param x the x-coordinate from the screen
     * @return longitude representing by the given x-value, multiplied by 1E7
     */
    public static int xToLonE7(final int screenWidth, @NonNull final BoundingBox viewBox, final float x) {
        return (int) Math.round(((double) x / (double) screenWidth * viewBox.getWidth()) + viewBox.getLeft());
    }

    /**
     * Calculates the distance of a point from a line
     * 
     * @param x the x coordinate of the point
     * @param y the y coordinate of the point
     * @param node1X the x coordinate of node1 (start point of the line)
     * @param node1Y the y coordinate of node1 (start point of the line)
     * @param node2X the x coordinate of node2 (end point of the line)
     * @param node2Y the y coordinate of node2 (end point of the line)
     * @return the distance of the point from the line specified by node1 and node2
     */
    public static double getLineDistance(double x, double y, double node1X, double node1Y, double node2X, double node2Y) {
        // http://stackoverflow.com/questions/849211/shortest-distance-between-a-point-and-a-line-segment
        // (adaptation of Ben Gotow's post of 23-Jun-2012, originally Joshua's post of 28-Jul-2011)
        double xx;
        double yy;
        double a = x - node1X;
        double b = y - node1Y;
        double c = node2X - node1X;
        double d = node2Y - node1Y;
        double dot = a * c + b * d;
        double len2 = c * c + d * d;
        // find the closest point (xx,yy) on the line segment (node1..node2) to the point (x,y)
        double t = (len2 == 0.0) ? -1.0 : dot / len2;
        if (t < 0.0) {
            // closest point on infinite line is past node1 end
            xx = node1X;
            yy = node1Y;
        } else if (t > 1.0) {
            // closest point on infinite line is past node2 end
            xx = node2X;
            yy = node2Y;
        } else {
            // closest point is between node1 and node2
            xx = node1X + t * c;
            yy = node1Y + t * d;
        }
        return Math.hypot(x - xx, y - yy);
    }

    /**
     * Calculates the point on the line (node1X,node1Y)-(node2X,node2Y) that is closest to the point (x,y).
     * 
     * @param x point x-coordinate
     * @param y point y-coordinate
     * @param node1X line node 1 x-coordinate
     * @param node1Y line node 1 y-coordinate
     * @param node2X line node 2 x-coordinate
     * @param node2Y line node 2 y-coordinate
     * @return an array holing the x, y coordinates of the point on the line
     */
    public static float[] closestPoint(float x, float y, float node1X, float node1Y, float node2X, float node2Y) {
        // http://paulbourke.net/geometry/pointline/
        float dx = node2X - node1X;
        float dy = node2Y - node1Y;
        float cx;
        float cy;
        if (dx == 0.0d && dy == 0.0d) {
            // node1 and node2 are the same
            cx = node1X;
            cy = node1Y;
        } else {
            final double u = ((x - node1X) * dx + (y - node1Y) * dy) / (dx * dx + dy * dy);
            if (u < 0.0d) {
                cx = node1X;
                cy = node1Y;
            } else if (u > 1.0d) {
                cx = node2X;
                cy = node2Y;
            } else {
                cx = (float) (node1X + u * dx);
                cy = (float) (node1Y + u * dy);
            }
        }
        return new float[] { cx, cy };
    }

    /**
     * Calculate the haversine distance between two points
     * 
     * @param lon1 longitude of the first point in degrees
     * @param lat1 latitude of the first point in degree
     * @param lon2 longitude of the second point in degrees
     * @param lat2 latitude of the second point in degree
     * @return distance between the two point in meters
     */
    public static double haversineDistance(double lon1, double lat1, double lon2, double lat2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    /**
     * Get the bearing in degrees from point 1 to point 2
     * 
     * @param lon1 longitude of point 1 in WGS84 coordinates
     * @param lat1 latitude of point 1 in WGS84 coordinates
     * @param lon2 longitude of point 2 in WGS84 coordinates
     * @param lat2 latitude of point 1 in WGS84 coordinates
     * @return a bearing in degrees
     */
    public static long bearing(double lon1, double lat1, double lon2, double lat2) {
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);
        double dLon = Math.toRadians(lon2 - lon1);
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);

        double bearing = Math.toDegrees(Math.atan2(y, x));
        if (bearing < 0) {
            bearing = bearing + 360;
        }
        return Math.round(bearing);
    }

    /**
     * Return the longitude value for a tile number (google convention)
     * 
     * @param x x number of the tile
     * @param z z zoom level
     * @return WGD84 coordinate in degrees
     */
    public static double tile2lon(int x, int z) {
        return x / Math.pow(2.0, z) * 360.0 - 180;
    }

    /**
     * Return the latitude value for a tile number (google convention)
     * 
     * @param y y number of the tile
     * @param z z zoom level
     * @return WGD84 coordinate in degrees
     */
    public static double tile2lat(int y, int z) {
        double n = Math.pow(2.0, z);
        return Math.toDegrees(Math.atan(Math.sinh(Math.PI * (1 - 2 * y / n))));
    }

    /**
     * Covert a tiles x number to a web mercator coordinate
     * 
     * @param tileWidth width of a tile in pixels
     * @param x x number of the tils
     * @param z z zoom level
     * @return coordinate in Web Mercator meters
     */
    public static double tile2lonMerc(int tileWidth, int x, int z) {
        return pixelsToMeters(tileWidth, x * tileWidth, z);
    }

    /**
     * Covert a tiles y number (TMS convention) to a web mercator coordinate
     * 
     * @param tileHeight height of a tile in pixels
     * @param y y number of the tiles
     * @param z z zoom level
     * @return coordinate in Web Mercator meters
     */
    public static double tile2latMerc(int tileHeight, int y, int z) {
        return pixelsToMeters(tileHeight, y * tileHeight, z);
    }

    /**
     * Converts pixel coordinates at given zoom level to EPSG:3857
     * 
     * @param tileSize the size of the tile (one side)
     * @param px the "pixel" coordinate
     * @param zoom zoom level
     * @return coordinate in Web Mercator meters
     */
    public static double pixelsToMeters(int tileSize, int px, int zoom) {
        double res = resolution(tileSize, zoom);
        return px * res - EARTH_RADIUS_PI;
    }

    /**
     * Resolution (meters/pixel) for given zoom level (measured at Equator)
     * 
     * @param tileSize the size of the tile (one side)
     * @param zoom zoom level
     * @return the meters/pixel value
     */
    private static double resolution(int tileSize, int zoom) {
        return (2 * EARTH_RADIUS_PI) / (tileSize * Math.pow(2.0, zoom));
    }

    /**
     * Calculate the best zoom level based on an images nominal resolution
     * 
     * @param resolution in meters/pixel
     * @param lat latitude in degrees
     * @return the zoom level
     */
    public static int resolutionToZoom(double resolution, double lat) {
        if (Util.notZero(resolution)) {
            return (int) (Math.log(2 * EARTH_RADIUS_PI * (Math.cos(Math.toRadians(lat)) / resolution)) / Math.log(2) - 8); // NOSONAR
            // nonZero tests for zero
        }
        throw new IllegalArgumentException("Resolution can't be zero");
    }

    /**
     * Squash adjacent lines shorter than 'maxLen' that anyway would hardly be visible
     *
     * @param points in/out list in the format expected by {@link Canvas#drawLines(float[], Paint)}
     * @param maxLen maximal length of adjacent lines to be squashed
     */
    public static void squashPointsArray(@NonNull final FloatPrimitiveList points, float maxLen) {
        if (points.size() <= 4) {
            return;
        }
        int i; // current input line index
        int i0; // first squashable input line index
        int o; // current output line index
        float dx;
        float dy;
        float len;
        for (i = 0, i0 = 0, o = 0; i < points.size(); i += 4) {
            dx = Math.abs(points.get(i) - points.get(i + 2));
            dy = Math.abs(points.get(i + 1) - points.get(i + 3));
            len = (dx > dy) ? (dx + dy / 2) : (dy + dx / 2); // quick upper bound of line length: is 1.5 for diagonal
                                                             // line instead of sqrt(2)~=1.41
            if (len > maxLen) { // single line length
                if (i != i0) {
                    points.set(o, points.get(i0));
                    points.set(o + 1, points.get(i0 + 1));
                    points.set(o + 2, points.get(i - 2));
                    points.set(o + 3, points.get(i - 1));
                    o += 4;
                }
                points.set(o, points.get(i));
                points.set(o + 1, points.get(i + 1));
                points.set(o + 2, points.get(i + 2));
                points.set(o + 3, points.get(i + 3));
                o += 4;
                i0 = i + 4;
            } else if (i != i0) {
                boolean squash = true;
                boolean adjacent = points.get(i - 2) == points.get(i) && points.get(i - 1) == points.get(i + 1);
                if (!adjacent) {
                    squash = false;
                } else {
                    dx = Math.abs(points.get(i0) - points.get(i + 2));
                    dy = Math.abs(points.get(i0 + 1) - points.get(i + 3));
                    len = (dx > dy) ? (dx + dy / 2) : (dy + dx / 2);
                    if (len > maxLen) { // multi line length
                        squash = false;
                    }
                }
                if (!squash) {
                    points.set(o, points.get(i0));
                    points.set(o + 1, points.get(i0 + 1));
                    points.set(o + 2, points.get(i - 2));
                    points.set(o + 3, points.get(i - 1));
                    o += 4;
                    i0 = i;
                }
            }
        }
        if (i != i0) {
            points.set(o, points.get(i0));
            points.set(o + 1, points.get(i0 + 1));
            points.set(o + 2, points.get(i - 2));
            points.set(o + 3, points.get(i - 1));
            o += 4;
        }
        points.truncate(o);
    }

    /**
     * Sort a list of photos by distance from a specific one
     * 
     * @param point the specific photo
     * @param list the List
     * @param viewBox current view box
     * @param w current screen width in WGS84*1E7 degrees
     * @param h current screen height in WGS84*1E7 degrees
     */
    public static <P extends GeoPoint> void sortGeoPoint(@NonNull P point, @NonNull List<P> list, @NonNull ViewBox viewBox, int w, int h) {
        float pX = GeoMath.lonE7ToX(w, viewBox, point.getLon());
        float pY = GeoMath.latE7ToY(h, w, viewBox, point.getLat());

        Collections.sort(list, (p1, p2) -> {
            double d1 = distance(p1, viewBox, pX, pY, w, h);
            double d2 = distance(p2, viewBox, pX, pY, w, h);
            if (d1 > d2) {
                return 1;
            }
            if (d2 > d1) {
                return -1;
            }
            return 0;
        });
    }

    /**
     * Calc planar geom. distance between Photo p and a location on the screen
     * 
     * @param p the Photo
     * @param bb current view box
     * @param x screen x
     * @param y screen y
     * @param w current screen width in WGS84*1E7 degrees
     * @param h current screen height in WGS84*1E7 degrees
     * @return distance in screen pixel units
     */
    private static double distance(@NonNull GeoPoint p, @NonNull ViewBox bb, float x, float y, int w, int h) {
        return Math.hypot(GeoMath.lonE7ToX(w, bb, p.getLon()) - x, GeoMath.latE7ToY(h, w, bb, p.getLat()) - y);
    }
}
