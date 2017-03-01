package de.blau.android.util.mapbox.turf;

import java.util.ArrayList;

import de.blau.android.util.mapbox.geojson.Feature;
import de.blau.android.util.mapbox.geojson.FeatureCollection;
import de.blau.android.util.mapbox.geojson.Point;
import de.blau.android.util.mapbox.geojson.Polygon;

/**
 * Methods found in this Turf class typically return a set of {@link Point}, {@link Polygon}, or
 * other geometry in a grid.
 *
 * @see <a href="http://turfjs.org/docs/">Turfjs documentation</a>
 * @since 1.3.0
 */
class TurfGrids {

  /**
   * Takes a {@link FeatureCollection} of {@link Point} and a {@link FeatureCollection} of
   * {@link Polygon} and returns the points that fall within the polygons.
   *
   * @param points   input points.
   * @param polygons input polygons.
   * @return points that land within at least one polygon.
   * @throws TurfException Signals that a Turf exception of some sort has occurred.
   * @since 1.3.0
   */
  public static FeatureCollection within(FeatureCollection points, FeatureCollection polygons) throws TurfException {
    ArrayList<Feature> features = new ArrayList<>();
    for (int i = 0; i < polygons.getFeatures().size(); i++) {
      for (int j = 0; j < points.getFeatures().size(); j++) {
        Point point = (Point) points.getFeatures().get(j).getGeometry();
        boolean isInside = TurfJoins.inside(point, (Polygon) polygons.getFeatures().get(i).getGeometry());
        if (isInside) {
          features.add(Feature.fromGeometry(point));
        }
      }
    }
    return FeatureCollection.fromFeatures(features);
  }
}
