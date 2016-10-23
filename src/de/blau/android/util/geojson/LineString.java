package de.blau.android.util.geojson;

import com.google.gson.GsonBuilder;
import de.blau.android.util.geojson.custom.PositionDeserializer;
import de.blau.android.util.geojson.custom.PositionSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * A LineString is a type of {@link Geometry}.
 *
 * @see <a href='geojson.org/geojson-spec.html#linestring'>Official GeoJSON LineString Specifications</a>
 * @since 1.0.0
 */
public class LineString implements Geometry<List<Position>> {

  private final String type = "LineString";
  private List<Position> coordinates;

  /**
   * Private constructor.
   *
   * @param coordinates List of {@link Position} making up the LineString.
   * @since 1.0.0
   */
  private LineString(List<Position> coordinates) {
    this.coordinates = coordinates;
  }

  /**
   * Should always be "LineString".
   *
   * @return String "LineString".
   * @since 1.0.0
   */
  @Override
  public String getType() {
    return type;
  }

  /**
   * Get the list of {@link Position} making up the LineString.
   *
   * @return List of {@link Position}.
   * @since 1.0.0
   */
  @Override
  public List<Position> getCoordinates() {
    return coordinates;
  }

  @Override
  public void setCoordinates(List<Position> coordinates) {
    this.coordinates = coordinates;
  }

  /**
   * creates a {@link LineString} from a list of coordinates.
   *
   * @param coordinates List of {@link Position} coordinates.
   * @return {@link LineString}.
   * @since 1.0.0
   */
  public static LineString fromCoordinates(List<Position> coordinates) {
    return new LineString(coordinates);
  }

  public static LineString fromCoordinates(double[][] coordinates) {
    ArrayList<Position> converted = new ArrayList<>(coordinates.length);
    for (int i = 0; i < coordinates.length; i++) {
      converted.add(Position.fromCoordinates(coordinates[i]));
    }

    return fromCoordinates(converted);
  }

  /**
   * Create a GeoJSON LineString object from JSON.
   *
   * @param json String of JSON making up a LineString.
   * @return {@link LineString} GeoJSON object.
   * @since 1.0.0
   */
  public static LineString fromJson(String json) {
    GsonBuilder gson = new GsonBuilder();
    gson.registerTypeAdapter(Position.class, new PositionDeserializer());
    return gson.create().fromJson(json, LineString.class);
  }

  /**
   * Convert feature into JSON.
   *
   * @return String containing LineString JSON.
   * @since 1.0.0
   */
  @Override
  public String toJson() {
    GsonBuilder gson = new GsonBuilder();
    gson.registerTypeAdapter(Position.class, new PositionSerializer());
    return gson.create().toJson(this);
  }

  /**
   * Convert a polyline into a LineString.
   *
   * @param polyline  String describing a polyline.
   * @param precision The encoded precision, for example Constants.OSRM_PRECISION_V4.
   * @return {@link LineString} containing the geometric structure of our polyline.
   * @since 1.0.0
   */
  public static LineString fromPolyline(String polyline, int precision) {
    return new LineString(PolylineUtils.decode(polyline, precision));
  }

  /**
   * Convert the sequence of coordinates into an encoded path string.
   *
   * @param precision The encoded precision, for example Constants.OSRM_PRECISION_V4.
   * @return a string describing the geometry of polyline.
   * @since 1.0.0
   */
  public String toPolyline(int precision) {
    return PolylineUtils.encode(getCoordinates(), precision);
  }
}
