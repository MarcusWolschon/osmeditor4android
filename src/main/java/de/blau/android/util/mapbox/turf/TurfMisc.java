package de.blau.android.util.mapbox.turf;

import java.util.ArrayList;
import java.util.List;

import de.blau.android.util.mapbox.geojson.Feature;
import de.blau.android.util.mapbox.geojson.LineString;
import de.blau.android.util.mapbox.geojson.Point;
import de.blau.android.util.mapbox.models.Position;
import de.blau.android.util.mapbox.turf.models.LineIntersectsResult;

/**
 * Class contains all the miscellaneous methods that Turf can perform.
 *
 * @see <a href="http://turfjs.org/docs/">Turf documentation</a>
 * @since 1.2.0
 */
class TurfMisc {

  /**
   * Takes a line, a start {@link Point}, and a stop point and returns the line in between those
   * points.
   *
   * @param startPt Starting point.
   * @param stopPt  Stopping point.
   * @param line    Line to slice.
   * @return Sliced line.
   * @throws TurfException signals that a Turf exception of some sort has occurred.
   * @see <a href="http://turfjs.org/docs/#lineslice">Turf Line slice documentation</a>
   * @since 1.2.0
   */
  public static LineString lineSlice(Point startPt, Point stopPt, Feature line) throws TurfException {
    if (!line.getGeometry().getType().equals("LineString")) {
      throw new TurfException("input must be a LineString Feature or Geometry");
    }

    return lineSlice(startPt, stopPt, (LineString) line.getGeometry());
  }

  /**
   * Takes a line, a start {@link Point}, and a stop point and returns the line in between those
   * points.
   *
   * @param startPt Starting point.
   * @param stopPt  Stopping point.
   * @param line    Line to slice.
   * @return Sliced line.
   * @throws TurfException signals that a Turf exception of some sort has occurred.
   * @see <a href="http://turfjs.org/docs/#lineslice">Turf Line slice documentation</a>
   * @since 1.2.0
   */
  private static LineString lineSlice(Point startPt, Point stopPt, LineString line) throws TurfException {
    List<Position> coords = line.getCoordinates();

    Feature startVertex = pointOnLine(startPt, coords);
    Feature stopVertex = pointOnLine(stopPt, coords);
    List<Feature> ends = new ArrayList<>();
    if ((int) startVertex.getNumberProperty("index") <= (int) stopVertex.getNumberProperty("index")) {
      ends.add(startVertex);
      ends.add(stopVertex);
    } else {
      ends.add(stopVertex);
      ends.add(startVertex);
    }
    List<Position> positions = new ArrayList<>();
    positions.add(((Point) ends.get(0).getGeometry()).getCoordinates());
    LineString clipLine = LineString.fromCoordinates(positions);
    for (int i = (int) ends.get(0).getNumberProperty("index") + 1;
         i < (int) ends.get(1).getNumberProperty("index") + 1; i++) {
      List<Position> coordinates = clipLine.getCoordinates();
      coordinates.add(coords.get(i));
      clipLine.setCoordinates(coordinates);
    }
    List<Position> coordinates = clipLine.getCoordinates();
    coordinates.add(((Point) ends.get(1).getGeometry()).getCoordinates());
    clipLine.setCoordinates(coordinates);
    return clipLine;
  }

  /**
   * Takes a {@link Point} and a {@link LineString} and calculates the closest Point on the
   * LineString.
   *
   * @param pt     point to snap from
   * @param coords line to snap to
   * @return closest point on the line to point
   * @throws TurfException signals that a Turf exception of some sort has occurred.
   * @since 1.3.0
   */
  private static Feature pointOnLine(Point pt, List<Position> coords) throws TurfException {
    String units = TurfConstants.UNIT_MILES;

    Feature closestPt = Feature.fromGeometry(
      Point.fromCoordinates(
        Position.fromCoordinates(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)));
    closestPt.addNumberProperty("dist", Double.POSITIVE_INFINITY);

    for (int i = 0; i < coords.size() - 1; i++) {
      Feature start = Feature.fromGeometry(Point.fromCoordinates(coords.get(i)));
      Feature stop = Feature.fromGeometry(Point.fromCoordinates(coords.get(i + 1)));
      //start
      start.addNumberProperty("dist", TurfMeasurement.distance(pt, (Point) start.getGeometry(), units));
      //stop
      stop.addNumberProperty("dist", TurfMeasurement.distance(pt, (Point) stop.getGeometry(), units));
      //perpendicular
      double direction = TurfMeasurement.bearing((Point) start.getGeometry(), (Point) stop.getGeometry());
      Feature perpendicularPt = Feature.fromGeometry(
        TurfMeasurement.destination(pt, 1000, direction + 90, units)); // 1000 = gross
      LineIntersectsResult intersect = lineIntersects(
        pt.getCoordinates().getLongitude(),
        pt.getCoordinates().getLatitude(),
        ((Point) perpendicularPt.getGeometry()).getCoordinates().getLongitude(),
        ((Point) perpendicularPt.getGeometry()).getCoordinates().getLatitude(),
        ((Point) start.getGeometry()).getCoordinates().getLongitude(),
        ((Point) start.getGeometry()).getCoordinates().getLatitude(),
        ((Point) stop.getGeometry()).getCoordinates().getLongitude(),
        ((Point) stop.getGeometry()).getCoordinates().getLatitude()
      );
      if (intersect == null) {
        perpendicularPt = Feature.fromGeometry(
          TurfMeasurement.destination(pt, 1000, direction - 90, units)); // 1000 = gross
        intersect = lineIntersects(
          pt.getCoordinates().getLongitude(),
          pt.getCoordinates().getLatitude(),
          ((Point) perpendicularPt.getGeometry()).getCoordinates().getLongitude(),
          ((Point) perpendicularPt.getGeometry()).getCoordinates().getLatitude(),
          ((Point) start.getGeometry()).getCoordinates().getLongitude(),
          ((Point) start.getGeometry()).getCoordinates().getLatitude(),
          ((Point) stop.getGeometry()).getCoordinates().getLongitude(),
          ((Point) stop.getGeometry()).getCoordinates().getLatitude()
        );
      }
      perpendicularPt.addNumberProperty("dist", Double.POSITIVE_INFINITY);
      Feature intersectPt = null;
      if (intersect != null) {
        intersectPt = Feature.fromGeometry(
          Point.fromCoordinates(Position.fromCoordinates(intersect.getY(), intersect.getX())));
        intersectPt.addNumberProperty("dist", TurfMeasurement.distance(pt, (Point) intersectPt.getGeometry(), units));
      }

      if ((double) start.getNumberProperty("dist") < (double) closestPt.getNumberProperty("dist")) {
        closestPt = start;
        closestPt.addNumberProperty("index", i);
      }
      if ((double) stop.getNumberProperty("dist") < (double) closestPt.getNumberProperty("dist")) {
        closestPt = stop;
        closestPt.addNumberProperty("index", i);
      }
      if (intersectPt != null
        && (double) intersectPt.getNumberProperty("dist") < (double) closestPt.getNumberProperty("dist")) {
        closestPt = intersectPt;
        closestPt.addNumberProperty("index", i);
      }
    }

    return closestPt;
  }

  private static LineIntersectsResult lineIntersects(double line1StartX, double line1StartY,
                                                     double line1EndX, double line1EndY,
                                                     double line2StartX, double line2StartY,
                                                     double line2EndX, double line2EndY) {
    // If the lines intersect, the result contains the x and y of the intersection
    // (treating the lines as infinite) and booleans for whether line segment 1 or line
    // segment 2 contain the point
    double denominator;
    double a;
    double b;
    double numerator1;
    double numerator2;
    LineIntersectsResult result = new LineIntersectsResult();

    denominator = ((line2EndY - line2StartY) * (line1EndX - line1StartX))
      - ((line2EndX - line2StartX) * (line1EndY - line1StartY));
    if (denominator == 0) {
      if (result.getX() != null && result.getY() != null) {
        return result;
      } else {
        return null;
      }
    }
    a = line1StartY - line2StartY;
    b = line1StartX - line2StartX;
    numerator1 = ((line2EndX - line2StartX) * a) - ((line2EndY - line2StartY) * b);
    numerator2 = ((line1EndX - line1StartX) * a) - ((line1EndY - line1StartY) * b);
    a = numerator1 / denominator;
    b = numerator2 / denominator;

    // if we cast these lines infinitely in both directions, they intersect here:
    result.setX(line1StartX + (a * (line1EndX - line1StartX)));
    result.setY(line1StartY + (a * (line1EndY - line1StartY)));

    // if line1 is a segment and line2 is infinite, they intersect if:
    if (a > 0 && a < 1) {
      result.setOnLine1(true);
    }
    // if line2 is a segment and line1 is infinite, they intersect if:
    if (b > 0 && b < 1) {
      result.setOnLine2(true);
    }
    // if line1 and line2 are segments, they intersect if both of the above are true
    if (result.isOnLine1() && result.isOnLine2()) {
      return result;
    } else {
      return null;
    }
  }
}
