package de.blau.android.util.geojson;

/**
 * Interface implemented by all GeoJSON objects, contains common fields.
 *
 * @since 1.0.0
 */
public interface GeoJSON {

  String getType();

  String toJson();

}
