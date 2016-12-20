package de.blau.android.util.mapbox.turf;

import de.blau.android.util.mapbox.geojson.Feature;
import de.blau.android.util.mapbox.geojson.FeatureCollection;
import de.blau.android.util.mapbox.geojson.GeoJSON;
import de.blau.android.util.mapbox.geojson.Point;
import de.blau.android.util.mapbox.models.Position;
import de.blau.android.util.mapbox.utils.TextUtils;

/**
 * Also called Assertions, these methods enforce expectations of a certain type or calculate various
 * shapes from given points.
 *
 * @see <a href="http://turfjs.org/docs/">Turf documentation</a>
 * @since 1.2.0
 */
public class TurfInvariant {

  /**
   * Unwrap a coordinate from a Feature with a Point geometry, a Point geometry, or a single
   * coordinate.
   *
   * @param obj any value
   * @return A coordinate
   * @throws TurfException Signals that a Turf exception of some sort has occurred.
   * @see <a href="http://turfjs.org/docs/#getcoord">Turf getCoord documentation</a>
   * @since 1.2.0
   */
  public static Position getCoord(Feature obj) throws TurfException {
    if (obj.getGeometry().getClass().equals(Point.class)) {
      return getCoord((Point) obj.getGeometry());
    }
    throw new TurfException("A coordinate, feature, or point geometry is required");
  }

  public static Position getCoord(Point obj) throws TurfException {
    if (obj != null) {
      return obj.getCoordinates();
    }
    throw new TurfException("A coordinate, feature, or point geometry is required");
  }

  /**
   * Enforce expectations about types of GeoJSON objects for Turf.
   *
   * @param value Any GeoJSON object.
   * @param type  Type expected GeoJSON type.
   * @param name  Name of calling function.
   * @throws TurfException Signals that a Turf exception of some sort has occurred.
   * @see <a href="http://turfjs.org/docs/#geojsontype">Turf geojsonType documentation</a>
   * @since 1.2.0
   */
  public static void geojsonType(GeoJSON value, String type, String name) throws TurfException {
    if (TextUtils.isEmpty(type) || TextUtils.isEmpty(name)) {
      throw new TurfException("Type and name required");
    }

    if (value == null || !value.getType().equals(type)) {
      throw new TurfException("Invalid input to " + name + ": must be a " + type
        + ", given " + value.getType());
    }
  }

  /**
   * Enforce expectations about types of {@link Feature} inputs for Turf. Internally this uses
   * {@link Feature#getType()} to judge geometry types.
   *
   * @param feature A feature with an expected geometry type.
   * @param type    Type expected GeoJSON type.
   * @param name    Name of calling function.
   * @throws TurfException Signals that a Turf exception of some sort has occurred.
   * @see <a href="http://turfjs.org/docs/#featureof">Turf featureOf documentation</a>
   * @since 1.2.0
   */
  public static void featureOf(Feature feature, String type, String name) throws TurfException {
    if (TextUtils.isEmpty(name)) {
      throw new TurfException(".featureOf() requires a name");
    }

    if (feature == null || !feature.getType().equals("Feature") || feature.getGeometry() == null) {
      throw new TurfException("Invalid input to " + name + ", Feature with geometry required");
    }

    if (feature.getGeometry() == null || !feature.getGeometry().getType().equals(type)) {
      throw new TurfException("Invalid input to " + name + ": must be a " + type
        + ", given " + feature.getGeometry().getType());
    }
  }

  /**
   * Enforce expectations about types of {@link FeatureCollection} inputs for Turf. Internally
   * this uses {@link Feature#getType()}} to judge geometry types.
   *
   * @param featurecollection A {@link FeatureCollection} for which features will be judged
   * @param type              Expected GeoJSON type.
   * @param name              Name of calling function.
   * @throws TurfException Signals that a Turf exception of some sort has occurred.
   * @see <a href="http://turfjs.org/docs/#collectionof">Turf collectionOf documentation</a>
   * @since 1.2.0
   */
  public static void collectionOf(FeatureCollection featurecollection, String type, String name) throws TurfException {
    if (TextUtils.isEmpty(name)) {
      throw new TurfException("collectionOf() requires a name");
    }

    if (featurecollection == null || !featurecollection.getType().equals("FeatureCollection")
      || featurecollection.getFeatures() == null) {
      throw new TurfException("Invalid input to " + name + ", FeatureCollection required");
    }

    for (Feature feature : featurecollection.getFeatures()) {
      if (feature == null || !feature.getType().equals("Feature") || feature.getGeometry() == null) {
        throw new TurfException("Invalid input to " + name + ", Feature with geometry required");
      }

      if (feature.getGeometry() == null || !feature.getGeometry().getType().equals(type)) {
        throw new TurfException("Invalid input to " + name + ": must be a " + type
          + ", given " + feature.getGeometry().getType());
      }
    }
  }
}
