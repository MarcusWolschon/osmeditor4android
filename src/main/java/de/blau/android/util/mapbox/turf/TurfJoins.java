package de.blau.android.util.mapbox.turf;

import de.blau.android.util.mapbox.geojson.MultiPolygon;
import de.blau.android.util.mapbox.geojson.Point;
import de.blau.android.util.mapbox.geojson.Polygon;
import de.blau.android.util.mapbox.models.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * Class contains methods that can determine if points lie within a polygon or not.
 *
 * @see <a href="http://turfjs.org/docs/">Turf documentation</a>
 * @since 1.3.0
 */
class TurfJoins {

  /**
   * Takes a {@link Position} and a List of Positions making up a polygon and determines if the
   * point resides inside the polygon. The polygon can be convex or concave. The function accounts
   * for holes.
   *
   * @param point   input position.
   * @param polygon input list of positions making up the polygon.
   * @return True if the Point is inside the Polygon; false if the Point is not inside the Polygon.
   * @throws TurfException Signals that a Turf exception of some sort has occurred.
   * @see <a href="http://turfjs.org/docs/#inside">Turf Inside documentation</a>
   * @since 1.3.0
   */
  public static boolean inside(Position point, List<Position> polygon) throws TurfException {
    List<List<Position>> polygons = new ArrayList<>();
    polygons.add(polygon);

    return inside(Point.fromCoordinates(point), Polygon.fromCoordinates(polygons));
  }

  /**
   * Takes a {@link Point} and a {@link Polygon} and determines if the point resides inside the
   * polygon. The polygon can be convex or concave. The function accounts for holes.
   *
   * @param point   input point.
   * @param polygon input polygon.
   * @return True if the Point is inside the Polygon; false if the Point is not inside the Polygon.
   * @throws TurfException Signals that a Turf exception of some sort has occurred.
   * @see <a href="http://turfjs.org/docs/#inside">Turf Inside documentation</a>
   * @since 1.3.0
   */
  public static boolean inside(Point point, Polygon polygon) throws TurfException {
    // This API needs to get better
    List<List<Position>> coordinates = polygon.getCoordinates();
    ArrayList<List<List<Position>>> multiCoordinates = new ArrayList<>();
    multiCoordinates.add(coordinates);
    return inside(point, MultiPolygon.fromCoordinates(multiCoordinates));
  }

  /**
   * Takes a {@link Point} and a {@link MultiPolygon} and determines if the point resides inside
   * the polygon. The polygon can be convex or concave. The function accounts for holes.
   *
   * @param point   input point.
   * @param polygon input multipolygon.
   * @return True if the Point is inside the MultiPolygon; false if the Point is not inside the MultiPolygon.
   * @throws TurfException Signals that a Turf exception of some sort has occurred.
   * @see <a href="http://turfjs.org/docs/#inside">Turf Inside documentation</a>
   * @since 1.3.0
   */
  private static boolean inside(Point point, MultiPolygon polygon) throws TurfException {
    Position pt = TurfInvariant.getCoord(point);
    List<List<List<Position>>> polys = polygon.getCoordinates();

    boolean insidePoly = false;
    for (int i = 0; i < polys.size() && !insidePoly; i++) {
      // check if it is in the outer ring first
      if (inRing(pt, polys.get(i).get(0))) {
        boolean inHole = false;
        int k = 1;
        // check for the point in any of the holes
        while (k < polys.get(i).size() && !inHole) {
          if (inRing(pt, polys.get(i).get(k))) {
            inHole = true;
          }
          k++;
        }
        if (!inHole) {
          insidePoly = true;
        }
      }
    }

    return insidePoly;
  }

  // pt is [x,y] and ring is [[x,y], [x,y],..]
  private static boolean inRing(Position pt, List<Position> ring) {
    boolean isInside = false;

    for (int i = 0, j = ring.size() - 1; i < ring.size(); j = i++) {
      double xi = ring.get(i).getLongitude();
      double yi = ring.get(i).getLatitude();
      double xj = ring.get(j).getLongitude();
      double yj = ring.get(j).getLatitude();
      boolean intersect = (
        (yi > pt.getLatitude()) != (yj > pt.getLatitude()))
        && (pt.getLongitude() < (xj - xi) * (pt.getLatitude() - yi) / (yj - yi) + xi);
      if (intersect) {
        isInside = !isInside;
      }
    }

    return isInside;
  }

}
