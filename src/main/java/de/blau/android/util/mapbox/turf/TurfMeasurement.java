package de.blau.android.util.mapbox.turf;

import de.blau.android.util.mapbox.geojson.Feature;
import de.blau.android.util.mapbox.geojson.FeatureCollection;
import de.blau.android.util.mapbox.geojson.Geometry;
import de.blau.android.util.mapbox.geojson.LineString;
import de.blau.android.util.mapbox.geojson.Point;
import de.blau.android.util.mapbox.models.Position;

import java.util.List;

/**
 * Class contains an assortment of methods used to calculate measurments such as bearing,
 * destination, midpoint, etc.
 *
 * @see <a href="http://turfjs.org/docs/">Turf documentation</a>
 * @since 1.2.0
 */
class TurfMeasurement {

  /**
   * Takes two positions and finds the geographic bearing between them.
   *
   * @param p1 Starting {@link Position}.
   * @param p2 Ending {@link Position}.
   * @return bearing in decimal degrees.
   * @see <a href="http://turfjs.org/docs/#bearing">Turf Bearing documentation</a>
   * @since 1.3.0
   */
  private static double bearing(Position p1, Position p2) {
    return bearing(Point.fromCoordinates(p1), Point.fromCoordinates(p2));
  }

  /**
   * Takes two points and finds the geographic bearing between them.
   *
   * @param p1 Starting {@link Point}.
   * @param p2 Ending {@link Point}.
   * @return bearing in decimal degrees.
   * @see <a href="http://turfjs.org/docs/#bearing">Turf Bearing documentation</a>
   * @since 1.2.0
   */
  public static double bearing(Point p1, Point p2) {
    double degrees2radians = Math.PI / 180;
    double radians2degrees = 180 / Math.PI;
    Position coordinates1 = p1.getCoordinates();
    Position coordinates2 = p2.getCoordinates();

    double lon1 = degrees2radians * coordinates1.getLongitude();
    double lon2 = degrees2radians * coordinates2.getLongitude();
    double lat1 = degrees2radians * coordinates1.getLatitude();
    double lat2 = degrees2radians * coordinates2.getLatitude();
    double a = Math.sin(lon2 - lon1) * Math.cos(lat2);
    double b = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(lon2 - lon1);

    return radians2degrees * Math.atan2(a, b);
  }

  /**
   * Takes a Position and calculates the location of a destination point given a distance in
   * degrees, radians, miles, or kilometers; and bearing in degrees. This uses the Haversine
   * formula to account for global curvature.
   *
   * @param p1       Starting point.
   * @param distance Distance from the starting point.
   * @param bearing  Ranging from -180 to 180.
   * @param units    Miles, kilometers, degrees, or radians (defaults kilometers).
   * @return destination {@link Point}
   * @throws TurfException TurfException Signals that a Turf exception of some sort has occurred.
   * @see <a href="http://turfjs.org/docs/#destination">Turf Destination documetation</a>
   * @since 1.3.0
   */
  private static Position destination(Position p1, double distance, double bearing, String units) throws TurfException {
    return destination(Point.fromCoordinates(p1), distance, bearing, units).getCoordinates();
  }

  /**
   * Takes a Point and calculates the location of a destination point given a distance in
   * degrees, radians, miles, or kilometers; and bearing in degrees. This uses the Haversine
   * formula to account for global curvature.
   *
   * @param point1   Starting point.
   * @param distance Distance from the starting point.
   * @param bearing  Ranging from -180 to 180.
   * @param units    Miles, kilometers, degrees, or radians (defaults kilometers).
   * @return destination {@link Point}
   * @throws TurfException TurfException Signals that a Turf exception of some sort has occurred.
   * @see <a href="http://turfjs.org/docs/#destination">Turf Destination documetation</a>
   * @since 1.2.0
   */
  public static Point destination(Point point1, double distance, double bearing, String units) throws TurfException {
    double degrees2radians = Math.PI / 180;
    double radians2degrees = 180 / Math.PI;
    Position coordinates1 = point1.getCoordinates();
    double longitude1 = degrees2radians * coordinates1.getLongitude();
    double latitude1 = degrees2radians * coordinates1.getLatitude();
    double bearingRad = degrees2radians * bearing;

    double radians = TurfHelpers.distanceToRadians(distance, units);

    double latitude2 = Math.asin(Math.sin(latitude1) * Math.cos(radians)
      + Math.cos(latitude1) * Math.sin(radians) * Math.cos(bearingRad));
    double longitude2 = longitude1 + Math.atan2(Math.sin(bearingRad)
      * Math.sin(radians) * Math.cos(latitude1),
      Math.cos(radians) - Math.sin(latitude1) * Math.sin(latitude2));

    return Point.fromCoordinates(
      Position.fromCoordinates(radians2degrees * longitude2, radians2degrees * latitude2));
  }

  /**
   * Calculates the distance between two positions in degress, radians, miles, or kilometers. This
   * uses the Haversine formula to account for global curvature.
   *
   * @param point1 Origin position.
   * @param point2 Destination position.
   * @param units  Miles, kilometers, degrees, or radians (defaults kilometers).
   * @return Distance between the two positions.
   * @throws TurfException TurfException Signals that a Turf exception of some sort has occurred.
   * @see <a href="http://turfjs.org/docs/#distance">Turf distance documentation</a>
   * @since 1.3.0
   */
  private static double distance(Position point1, Position point2, String units) throws TurfException {
    return distance(Point.fromCoordinates(point1), Point.fromCoordinates(point2), units);
  }

  /**
   * Calculates the distance between two points in kilometers. This uses the Haversine formula to
   * account for global curvature.
   *
   * @param point1 Origin point.
   * @param point2 Destination point.
   * @return Distance between the two points.
   * @throws TurfException TurfException Signals that a Turf exception of some sort has occurred.
   * @see <a href="http://turfjs.org/docs/#distance">Turf distance documentation</a>
   * @since 1.2.0
   */
  public static double distance(Point point1, Point point2) throws TurfException {
    return distance(point1, point2, TurfConstants.UNIT_DEFAULT);
  }

  /**
   * Calculates the distance between two points in degress, radians, miles, or kilometers. This
   * uses the Haversine formula to account for global curvature.
   *
   * @param point1 Origin point.
   * @param point2 Destination point.
   * @param units  Can be degrees, radians, miles, or kilometers (defaults kilometers).
   * @return Distance between the two points.
   * @throws TurfException TurfException Signals that a Turf exception of some sort has occurred.
   * @see <a href="http://turfjs.org/docs/#distance">Turf distance documentation</a>
   * @since 1.2.0
   */
  public static double distance(Point point1, Point point2, String units) throws TurfException {
    double degrees2radians = Math.PI / 180;
    Position coordinates1 = point1.getCoordinates();
    Position coordinates2 = point2.getCoordinates();
    double dLat = degrees2radians * (coordinates2.getLatitude() - coordinates1.getLatitude());
    double dLon = degrees2radians * (coordinates2.getLongitude() - coordinates1.getLongitude());
    double lat1 = degrees2radians * coordinates1.getLatitude();
    double lat2 = degrees2radians * coordinates2.getLatitude();

    double a = Math.pow(Math.sin(dLat / 2), 2) + Math.pow(Math.sin(dLon / 2), 2) * Math.cos(lat1) * Math.cos(lat2);

    return TurfHelpers.radiansToDistance(2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)), units);
  }

  /**
   * Takes a line and measures its length in the specified units.
   *
   * @param line  Line to measure.
   * @param units Can be degrees, radians, miles, or kilometers (defaults kilometers).
   * @return Length of the input line.
   * @throws TurfException TurfException Signals that a Turf exception of some sort has occurred.
   * @see <a href="http://turfjs.org/docs/#linedistance">Turf Line Distance documentation</a>
   * @since 1.2.0
   */
  public static double lineDistance(FeatureCollection line, String units) throws TurfException {
    double d = 0;
    for (int i = 0; i < line.getFeatures().size(); i++) {
      d += lineDistance(line.getFeatures().get(i), units);
    }
    return d;
  }

  /**
   * Takes a line and measures its length in the specified units.
   *
   * @param line  Line to measure.
   * @param units Can be degrees, radians, miles, or kilometers (defaults kilometers).
   * @return Length of the input line.
   * @throws TurfException TurfException Signals that a Turf exception of some sort has occurred.
   * @see <a href="http://turfjs.org/docs/#linedistance">Turf Line Distance documentation</a>
   * @since 1.2.0
   */
  private static double lineDistance(Feature line, String units) throws TurfException {
    return lineDistance(line.getGeometry(), units);
  }

  /**
   * Takes a line and measures its length in the specified units.
   *
   * @param line  Line to measure.
   * @param units Can be degrees, radians, miles, or kilometers (defaults kilometers).
   * @return Length of the input line.
   * @throws TurfException TurfException Signals that a Turf exception of some sort has occurred.
   * @see <a href="http://turfjs.org/docs/#linedistance">Turf Line Distance documentation</a>
   * @since 1.2.0
   */
  private static double lineDistance(Geometry line, String units) throws TurfException {
    double d;

    if (line.getType().equals("LineString")) {
      List<Position> coordinates = (List<Position>) line.getCoordinates();
      return length(coordinates, units);
    } else if (line.getType().equals("Polygon") || line.getType().equals("MultiLineString")) {
      List<List<Position>> coordinates = (List<List<Position>>) line.getCoordinates();
      d = 0;
      for (int i = 0; i < coordinates.size(); i++) {
        d += length(coordinates.get(i), units);
      }
      return d;
    } else if (line.getType().equals("MultiPolygon")) {
      List<List<List<Position>>> coordinates = (List<List<List<Position>>>) line.getCoordinates();
      d = 0;
      for (int i = 0; i < coordinates.size(); i++) {
        for (int j = 0; j < coordinates.get(i).size(); j++) {
          d += length(coordinates.get(i).get(j), units);
        }
      }
      return d;
    } else {
      throw new TurfException("Input must be a LineString, MultiLineString, "
        + "Polygon, or MultiPolygon Feature or Geometry (or a FeatureCollection "
        + "containing only those types)");
    }
  }

  private static double length(List<Position> coords, String units) throws TurfException {
    double travelled = 0;
    Point prevCoords = Point.fromCoordinates(coords.get(0));
    Point curCoords = Point.fromCoordinates(coords.get(0));
    Point temp;
    for (int i = 1; i < coords.size(); i++) {
      curCoords.setCoordinates(coords.get(i));
      travelled += distance(prevCoords, curCoords, units);
      temp = prevCoords;
      prevCoords = curCoords;
      curCoords = temp;
    }
    return travelled;
  }

  /**
   * Takes two {@link Position} and returns a position midway between them. The midpoint is
   * calculated geodesically, meaning the curvature of the earth is taken into account.
   *
   * @param from First point.
   * @param to   Second point.
   * @return A {@link Position} midway between pt1 and pt2.
   * @throws TurfException TurfException Signals that a Turf exception of some sort has occurred.
   * @see <a href="http://turfjs.org/docs/#midpoint">Turf Midpoint documentation</a>
   * @since 1.3.0
   */
  public static Position midpoint(Position from, Position to) throws TurfException {
    Point midpointResult = midpoint(Point.fromCoordinates(from), Point.fromCoordinates(to));
    return Position.fromCoordinates(midpointResult.getCoordinates().getLongitude(),
      midpointResult.getCoordinates().getLatitude());
  }

  /**
   * Takes two {@link Point} and returns a point midway between them. The midpoint is calculated
   * geodesically, meaning the curvature of the earth is taken into account.
   *
   * @param from First point.
   * @param to   Second point.
   * @return A {@link Point} midway between pt1 and pt2.
   * @throws TurfException TurfException Signals that a Turf exception of some sort has occurred.
   * @see <a href="http://turfjs.org/docs/#midpoint">Turf Midpoint documentation</a>
   * @since 1.3.0
   */
  private static Point midpoint(Point from, Point to) throws TurfException {
    double dist = distance(from, to, TurfConstants.UNIT_MILES);
    double heading = bearing(from, to);
    return destination(from, dist / 2, heading, TurfConstants.UNIT_MILES);
  }

  public static Point along(LineString line, double distance, String units) throws TurfException {
    List<Position> coords = line.getCoordinates();

    double travelled = 0;
    for (int i = 0; i < coords.size(); i++) {
      if (distance >= travelled && i == coords.size() - 1) {
        break;
      } else if (travelled >= distance) {
        double overshot = distance - travelled;
        if (overshot == 0) {
          return Point.fromCoordinates(coords.get(i));
        } else {
          double direction = bearing(coords.get(i), coords.get(i - 1)) - 180;
          return Point.fromCoordinates(destination(coords.get(i), overshot, direction, units));
        }
      } else {
        travelled += distance(coords.get(i), coords.get(i + 1), units);
      }
    }

    return Point.fromCoordinates(coords.get(coords.size() - 1));
  }
}
