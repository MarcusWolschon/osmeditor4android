package de.blau.android.util;

import android.util.Pair;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.BoundingBox;

/**
 * GeoMath provides some calculating functions for mercator projection conversions and other math-utils.
 * 
 * @author mb
 */
public class GeoMath {
	
	public static final double _180_PI = 180 / Math.PI;
	
	public static final double _360_PI = 360 / Math.PI;
	
	public static final double PI_360 = Math.PI / 360;
	
	public static final double PI_180 = Math.PI / 180;
	
	public static final double PI_4 = Math.PI / 4;
	
	public static final double PI_2 = Math.PI / 2;
	
	public static final double MAX_LAT = -(PI_360 * Math.atan(Math.pow(Math.E, Math.PI)) - 90);
	
	/**
	 * The arithmetic middle of the two WGS84 reference-ellipsoids.
	 */
	public static final int EARTH_RADIUS = (6378137 + 6356752) / 2;
	
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
	 * @return the mercator-projected y-coordinate for a cartesian coordinate system.
	 */
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
		return (int) (latToMercator(latE7 / 1E7) * 1E7);
	}
	
	/**
	 * Calculates a projected mercator coordinate to a geo-latitude value. This is the inverse function to
	 * latToMercator(double).
	 * 
	 * @param mer the projected mercator coordinate
	 * @return the latitude value.
	 */
	public static double mercatorToLat(final double mer) {
		return _180_PI * (2 * Math.atan(Math.exp(mer * PI_180)) - PI_2);
	}
	
	/**
	 * @see mercatorToLat(double)
	 * @param mer the mercator projected coordinate, multiplied by 1E7
	 * @return
	 */
	public static double mercartorE7ToLat(final int mer) {
		return mercatorToLat(mer / 1E7);
	}
	
	/**
	 * @see mercatorToLat(double)
	 * @param mer
	 * @return the latitude value, multiplied by 1E7
	 */
	public static int mercartorToLatE7(final double mer) {
		return (int) (mercatorToLat(mer) * 1E7);
	}
	
	/**
	 * @see mercatorToLat(double)
	 * @param mer the mercator projected coordinate, multiplied by 1E7
	 * @return the latitude value, multiplied by 1E7
	 */
	public static int mercartorE7ToLatE7(final int mer) {
		return (int) (mercatorToLat(mer / 1E7) * 1E7);
	}
	
	/**
	 * Calculate the smallest bounding box that contains a circle of the given
	 * radius in metres centered at the given lat/lon.
	 * @param lat Latitude of box centre [-90.0,+90.0].
	 * @param lon Longitude of box centre [-180.0,+180.0].
	 * @param radius Radius in metres to be contained in the box.
	 * @return The BoundingBox that contains the specified area.
	 * @throws OsmException If any of the calculated latitudes are outside [-90.0,+90.0]
	 * or longitudes are outside [-180.0,+180.0].
	 */
	public static BoundingBox createBoundingBoxForCoordinates(final double lat, final double lon, final float radius)
			throws OsmException {
		double horizontalRadiusDegree = convertMetersToGeoDistance(radius);
		double verticalRadiusDegree = horizontalRadiusDegree / getMercartorFactorPow3(lat);
		double left = lon - horizontalRadiusDegree;
		double right = lon + horizontalRadiusDegree;
		double bottom = lat - verticalRadiusDegree;
		double top = lat + verticalRadiusDegree;
		
		return new BoundingBox(left, bottom, right, top);
	}
	
	public static double convertMetersToGeoDistance(final float meters) {
		return _180_PI * meters / EARTH_RADIUS;
	}
	
	public static int convertMetersToGeoDistanceE7(final float meters) {
		return (int) (_180_PI * meters * 1E7 / EARTH_RADIUS);
	}
	
	public static double getMercartorFactorPow3(final double lat) {
		if (lat == 0.0) {
			return 1;
		}
		return Math.pow(latToMercator(lat) / lat, 3.0);
	}
	
	/**
	 * Calculates the screen-coordinate to the given latitude.
	 * 
	 * @param latE7 latitude, multiplied by 1E7.
	 * @return the y screen-coordinate for this latitude value.
	 */
	public static float latE7ToY(final int screenHeight, final BoundingBox viewBox, final int latE7) {
		double ratio = viewBox.getMercatorFactorPow3() * (latE7 - viewBox.getBottom()) / viewBox.getHeight();
		return (float) ((screenHeight - ratio * screenHeight));
	}
	
	/**
	 * Calculates the screen-coordinate to the given longitude.
	 * 
	 * @param lonE7 the longitude, multiplied by 1E7.
	 * @return the x screen-coordinate for this longitude value.
	 */
	public static float lonE7ToX(final int screenWidth, final BoundingBox viewBox, final int lonE7) {
		return (float) (lonE7 - viewBox.getLeft()) / viewBox.getWidth() * screenWidth;
	}
	
	/**
	 * Calculates the latitude value for the given y screen coordinate.
	 * 
	 * @param y the y-coordinate from the screen
	 * @return latitude representing by the given y-value, multiplied by 1E7
	 */
	public static int yToLatE7(final int screenHeight, final BoundingBox viewBox, final float y) {
		final double ratio = ((screenHeight - y) / screenHeight) / viewBox.getMercatorFactorPow3();
		final double lat = ratio * viewBox.getHeight() + viewBox.getBottom();
		return (int) Math.round(lat);
	}
	
	/**
	 * Calculates the longitude value for the given x screen coordinate.
	 * 
	 * @param x the x-coordinate from the screen
	 * @return longitude representing by the given x-value, multiplied by 1E7
	 */
	public static int xToLonE7(final int screenWidth, final BoundingBox viewBox, final float x) {
		return (int) (x / screenWidth * viewBox.getWidth()) + viewBox.getLeft();
	}

	/**
	 * Calculates the distance of a point from a line
	 * @param x the x coordinate of the point
	 * @param y the y coordinate of the point
	 * @param node1X the x coordinate of node1 (start point of the line)
	 * @param node1Y the y coordinate of node1 (start point of the line)
	 * @param node2X the x coordinate of node2 (end point of the line)
	 * @param node2Y the y coordinate of node2 (end point of the line)
	 * @return the distance of the point from the line specified by node1 and node2
	 */
	public static double getLineDistance(float x, float y, float node1X, float node1Y, float node2X, float node2Y) {
		// equation (14) on http://mathworld.wolfram.com/Point-LineDistance2-Dimensional.html
		return (Math.abs((node2X - node1X) * (node1Y - y) - (node1X - x) * (node2Y - node1Y)) /
		                 Math.hypot(node2X - node1X, node2Y - node1Y));
	}
	
	/**
	 * Calculates the point on the line (node1X,node1Y)-(node2X,node2Y) that is
	 * closest to the point (x,y).
	 * @param x
	 * @param y
	 * @param node1X
	 * @param node1Y
	 * @param node2X
	 * @param node2Y
	 * @return
	 */
	public static Pair<Float, Float> closestPoint(float x, float y, float node1X, float node1Y, float node2X, float node2Y) {
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
				cx = (float)(node1X + u * dx);
				cy = (float)(node1Y + u * dy);
			}
		}
		return new Pair<Float, Float>(cx, cy);
	}
}
