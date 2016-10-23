package de.blau.android.util.geojson;

/**
 * Interface implemented by all Geometry objects, contains common fields.
 *
 * @param <T> the type of the coordinates, normally a list interface of positions.
 * @since 1.0.0
 */
public interface Geometry<T> extends GeoJSON {

  T getCoordinates();

  void setCoordinates(T coordinates);

}
