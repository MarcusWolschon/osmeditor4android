package de.blau.android.util;

import de.blau.android.exception.OsmException;
import de.blau.android.osm.BoundingBox;

/**
 * GeoMath provides some calculating functions for mercator projection conversions and other math-utils.
 * 
 * @author mb
 */
public class GeoMath {

    private static final double _180_PI = 180d / Math.PI;

    public static final double _360_PI = 360d / Math.PI;

    private static final double PI_360 = Math.PI / 360d;

    private static final double PI_180 = Math.PI / 180d;

    private static final double PI_4 = Math.PI / 4d;

    private static final double PI_2 = Math.PI / 2d;

    public static final double MAX_LAT = Math.toDegrees(Math.atan(Math.sinh(Math.PI)));

    public static final double MAX_LON = 180;

    public static final int    MAX_MLAT_E7 = GeoMath.latE7ToMercatorE7((int) (MAX_LAT * 1E7d));
    public static final double MAX_MLAT    = GeoMath.latE7ToMercator((int) (MAX_LAT * 1E7d));

    public static final int  EARTH_RADIUS_EQUATOR = 6378137;
    public static final int  EARTH_RADIUS_POLAR   = 6356752;
    /**
     * The arithmetic middle of the two WGS84 reference-ellipsoids.
     */
    private static final int EARTH_RADIUS         = (EARTH_RADIUS_EQUATOR + EARTH_RADIUS_POLAR) / 2;

    /**
     * Checks if x is between a and b (or equals a or b).
     * 
     * @param x
     * @param a
     * @param b
     * @return true, if x is between a or b, or equals a or b.
     */
    public static boolean isBetween(final float x, final float a, final float b) {
        return (a > b) ? x <= a && x >= b : x <= b && x >= a;
    }

    public static boolean isBetween(final int x, final int a, final int b) {
        return (a > b) ? x <= a && x >= b : x <= b && x >= a;
    }

    /**
     * Checks if x is between a and b plus the given offset.
     * 
     * @param x
     * @param a
     * @param b
     * @param offset
     * @return
     */
    public static boolean isBetween(final float x, final float a, final float b, final float offset) {
        return (a > b) ? x <= a + offset && x >= b - offset : x <= b + offset && x >= a - offset;
    }

    /**
     * Checks if x is between a and b plus the given offset.
     * 
     * @param x
     * @param a
     * @param b
     * @param offset
     * @return
     */
    public static boolean isBetween(final double x, final double a, final double b, final double offset) {
        return (a > b) ? x <= a + offset && x >= b - offset : x <= b + offset && x >= a - offset;
    }

    /**
     * Calculates a projected coordinate for a given latitude value. When lat is bigger than MAX_LAT, it will be clamped
     * to MAX_LAT.
     * 
     * @see {@link http://en.wikipedia.org/wiki/Mercator_projection#Mathematics_of_the_projection}
     * @param lat the latitude as double value
     * @return the mercator-projected y-coordinate for a cartesian coordinate system in degrees.
     */
    // TODO clamping is likely to lead to issues for objects extending past +-MAX_LAT given that they will never be in a
    // drawing box
    // this should simple throw an exception and let the drawing code handle it
    public static double latToMercator(double lat) {
        lat = Math.min(MAX_LAT, lat);
        lat = Math.max(-MAX_LAT, lat);
        return _180_PI * Math.log(Math.tan(lat * PI_360 + PI_4));
    }

    /**
     * @see latToMercator(double)
     * @param latE7 the latitude multiplied by 1E7
     * @return
     */
    public static double latE7ToMercator(final int latE7) {
        return latToMercator(latE7 / 1E7);
    }

    /**
     * @see latToMercator(double)
     * @param latE7 the latitude multiplied by 1E7
     * @return the mercator-projected y-coordinate for a cartesian coordinate system, multiplied by 1E7.
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
     * @see mercatorToLat(double)
     * @param mer the mercator projected coordinate, multiplied by 1E7
     * @return
     */
    public static double mercatorE7ToLat(final int mer) {
        return mercatorToLat(mer / 1E7d);
    }

    /**
     * @see mercatorToLat(double)
     * @param mer
     * @return the latitude value, multiplied by 1E7
     */
    public static int mercatorToLatE7(final double mer) {
        return (int) Math.round(mercatorToLat(mer) * 1E7d);
    }

    /**
     * @see mercatorToLat(double)
     * @param mer the mercator projected coordinate, multiplied by 1E7
     * @return the latitude value, multiplied by 1E7
     */
    public static int mercatorE7ToLatE7(final int mer) {
        return (int) Math.round(mercatorToLat(mer / 1E7d) * 1E7d);
    }

    /**
     * Calculate the smallest bounding box that contains a circle of the given radius in metres centered at the given
     * lat/lon.
     * 
     * @param lat Latitude of box centre [-90.0,+90.0].
     * @param lon Longitude of box centre [-180.0,+180.0].
     * @param radius Radius in metres to be contained in the box.
     * @param checkSize check that boundingbox would be a legal bb for the OSM api
     * @return The BoundingBox that contains the specified area.
     * @throws OsmException If any of the calculated latitudes are outside [-90.0,+90.0] or longitudes are outside
     *             [-180.0,+180.0].
     */
    public static BoundingBox createBoundingBoxForCoordinates(final double lat, final double lon, final double radius, boolean checkSize) throws OsmException {
        double horizontalRadiusDegree = convertMetersToGeoDistance(radius);
        if (checkSize && horizontalRadiusDegree > BoundingBox.API_MAX_DEGREE_DIFFERENCE / 1E7D / 2D) {
            horizontalRadiusDegree = BoundingBox.API_MAX_DEGREE_DIFFERENCE / 1E7D / 2D;
        }
        // Log.d("GeoMath","horizontalRadiusDegree " + horizontalRadiusDegree);
        double mercatorLat = latToMercator(lat);
        // Log.d("GeoMath","mercatorLat " + mercatorLat);
        double verticalRadiusDegree = horizontalRadiusDegree; //
        double left = lon - horizontalRadiusDegree;
        double right = lon + horizontalRadiusDegree;
        double bottom = mercatorToLat(mercatorLat - verticalRadiusDegree);
        double top = mercatorToLat(mercatorLat + verticalRadiusDegree);
        // Log.d("GeoMath","bottom " + bottom + " top " + top);
        if (left < -MAX_LON) {
            left = -MAX_LON;
            right = left + horizontalRadiusDegree * 2d;
        }
        if (right > MAX_LON) {
            right = BoundingBox.MAX_LON_E7;
            left = right - horizontalRadiusDegree * 2d;
        }
        if (bottom < -MAX_LAT) {
            bottom = -MAX_LAT;
            top = bottom + verticalRadiusDegree * 2d;
        }
        if (top > MAX_LAT) {
            top = MAX_LAT;
            bottom = top - verticalRadiusDegree * 2d;
        }
        // Log.d("GeoMath","left " + left + " right " + right + " bottom " + bottom + " top " + top);
        return new BoundingBox(left, bottom, right, top);
    }

    public static double convertMetersToGeoDistance(final double meters) {
        return (_180_PI * meters) / (double) EARTH_RADIUS;
    }

    public static int convertMetersToGeoDistanceE7(final double meters) {
        return (int) ((_180_PI * meters * 1E7d) / (double) EARTH_RADIUS);
    }

    /**
     * Calculates the screen-coordinate to the given latitude.
     * 
     * @param latE7 latitude, multiplied by 1E7.
     * @return the y screen-coordinate for this latitude value.
     */
    public static float latE7ToY(final int screenHeight, int screenWidth, final BoundingBox viewBox, final int latE7) {
        // note the last term should be pre-calculated too
        double pixelRadius = (double) screenWidth / (viewBox.getWidth() / 1E7d);
        // Log.d("GeoMath","screen width " + screenWidth + " width " + viewBox.getWidth() + " height " + screenHeight +
        // " mercator " + latE7ToMercator(latE7) );
        return (float) (screenHeight - (latE7ToMercator(latE7) - viewBox.getBottomMercator()) * pixelRadius);
    }

    /**
     * Non scaled version. Calculates the screen-coordinate to the given latitude.
     * 
     * @param screenHeight
     * @param screenWidth
     * @param viewBox
     * @param lat
     * @return
     */
    public static float latToY(final int screenHeight, int screenWidth, final BoundingBox viewBox, final double lat) {
        // note the last term should be pre-calculated too
        double pixelRadius = (double) screenWidth / (viewBox.getWidth() / 1E7d);
        // Log.d("GeoMath","screen width " + screenWidth + " width " + viewBox.getWidth() + " height " + screenHeight +
        // " mercator " + latE7ToMercator(latE7) );
        return (float) (screenHeight - (latToMercator(lat) - viewBox.getBottomMercator()) * pixelRadius);
    }

    /**
     * Non-scaled version. Calculates the screen-coordinate to the given longitude.
     * 
     * @param lonE7 the longitude, multiplied by 1E7.
     * @return the x screen-coordinate for this longitude value.
     */
    public static float lonE7ToX(final int screenWidth, final BoundingBox viewBox, final int lonE7) {
        return (float) ((double) (lonE7 - viewBox.getLeft()) / (double) viewBox.getWidth()) * screenWidth;
    }

    /**
     * Calculates the latitude value for the given y screen coordinate.
     * 
     * @param y the y-coordinate from the screen
     * @return latitude representing by the given y-value, multiplied by 1E7
     */
    public static int yToLatE7(final int screenHeight, int screenWidth, final BoundingBox viewBox, final float y) {
        double pixelRadius = screenWidth / (viewBox.getWidth() / 1E7d);
        double lat = mercatorToLatE7(viewBox.getBottomMercator() + ((double) screenHeight - y) / pixelRadius);
        return (int) lat;
    }

    /**
     * Calculates the longitude value for the given x screen coordinate.
     * 
     * @param x the x-coordinate from the screen
     * @return longitude representing by the given x-value, multiplied by 1E7
     */
    public static int xToLonE7(final int screenWidth, final BoundingBox viewBox, final float x) {
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
    public static double getLineDistance(float x, float y, float node1X, float node1Y, float node2X, float node2Y) {
        // http://stackoverflow.com/questions/849211/shortest-distance-between-a-point-and-a-line-segment
        // (adaptation of Ben Gotow's post of 23-Jun-2012, originally Joshua's post of 28-Jul-2011)
        double a, b, c, d, dot, len2, t, xx, yy;
        a = x - node1X;
        b = y - node1Y;
        c = node2X - node1X;
        d = node2Y - node1Y;
        dot = a * c + b * d;
        len2 = c * c + d * d;
        // find the closest point (xx,yy) on the line segment (node1..node2) to the point (x,y)
        t = (len2 == 0.0) ? -1.0 : dot / len2;
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
     * @param x
     * @param y
     * @param node1X
     * @param node1Y
     * @param node2X
     * @param node2Y
     * @return
     */
    public static float[] closestPoint(float x, float y, float node1X, float node1Y, float node2X, float node2Y) {
        // http://paulbourke.net/geometry/pointline/
        float dx = node2X - node1X;
        float dy = node2Y - node1Y;
        float cx, cy;
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
     * Caculate the haversine distance between two points
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
     * @param lon1
     * @param lat1
     * @param lon2
     * @param lat2
     * @return
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

}
