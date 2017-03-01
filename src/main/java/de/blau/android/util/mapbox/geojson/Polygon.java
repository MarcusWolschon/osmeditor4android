package de.blau.android.util.mapbox.geojson;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.GsonBuilder;

import de.blau.android.util.mapbox.geojson.custom.PositionDeserializer;
import de.blau.android.util.mapbox.geojson.custom.PositionSerializer;
import de.blau.android.util.mapbox.models.Position;

/**
 * A Polygon is a type of {@link Geometry}.
 *
 * @see <a href='http://geojson.org/geojson-spec.html#polygon'>Official GeoJSON Polygon Specifications</a>
 * @since 1.0.0
 */
public class Polygon implements Geometry<List<List<Position>>> {

  private final String type = "Polygon";
  private List<List<Position>> coordinates;

  /**
   * Private constructor.
   *
   * @param coordinates List of {@link Position} making up the Polygon.
   * @since 1.0.0
   */
  private Polygon(List<List<Position>> coordinates) {
    this.coordinates = coordinates;
  }

  /**
   * Should always be "Polygon".
   *
   * @return String "Polygon".
   * @since 1.0.0
   */
  @Override
  public String getType() {
    return type;
  }

  /**
   * Get the list of {@link Position} making up the Polygon.
   *
   * @return List of {@link Position}.
   * @since 1.0.0
   */
  @Override
  public List<List<Position>> getCoordinates() {
    return coordinates;
  }

  @Override
  public void setCoordinates(List<List<Position>> coordinates) {
    this.coordinates = coordinates;
  }

  /**
   * Creates a {@link Polygon} from a list of coordinates.
   *
   * @param coordinates List of {@link Position} coordinates.
   * @return {@link Polygon}.
   * @since 1.0.0
   */
  public static Polygon fromCoordinates(List<List<Position>> coordinates) {
    return new Polygon(coordinates);
  }

  public static Polygon fromCoordinates(double[][][] coordinates) {
    List<List<Position>> converted = new ArrayList<>(coordinates.length);
    for (int i = 0; i < coordinates.length; i++) {
      List<Position> innerList = new ArrayList<>(coordinates[i].length);
      for (int j = 0; j < coordinates[i].length; j++) {
        innerList.add(Position.fromCoordinates(coordinates[i][j]));
      }
      converted.add(innerList);
    }

    return fromCoordinates(converted);
  }

  /**
   * Create a GeoJSON Polygon object from JSON.
   *
   * @param json String of JSON making up a Polygon.
   * @return {@link Polygon} GeoJSON object.
   * @since 1.0.0
   */
  public static Polygon fromJson(String json) {
    GsonBuilder gson = new GsonBuilder();
    gson.registerTypeAdapter(Position.class, new PositionDeserializer());
    return gson.create().fromJson(json, Polygon.class);
  }

  /**
   * Convert feature into JSON.
   *
   * @return String containing Polygon JSON.
   * @since 1.0.0
   */
  @Override
  public String toJson() {
    GsonBuilder gson = new GsonBuilder();
    gson.registerTypeAdapter(Position.class, new PositionSerializer());
    return gson.create().toJson(this);
  }
}
