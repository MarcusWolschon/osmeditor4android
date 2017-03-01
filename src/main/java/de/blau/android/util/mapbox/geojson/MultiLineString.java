package de.blau.android.util.mapbox.geojson;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.GsonBuilder;

import de.blau.android.util.mapbox.geojson.custom.PositionDeserializer;
import de.blau.android.util.mapbox.geojson.custom.PositionSerializer;
import de.blau.android.util.mapbox.models.Position;

/**
 * A MultiLineString is a type of {@link Geometry}.
 *
 * @see <a href='http://geojson.org/geojson-spec.html#multilinestring'>Official GeoJSON MultiLineString Specifications</a>
 * @since 1.0.0
 */
public class MultiLineString implements Geometry<List<List<Position>>> {

  private final String type = "MultiLineString";
  private List<List<Position>> coordinates;

  /**
   * Private constructor.
   *
   * @param coordinates List of {@link Position} making up the MultiLineString.
   * @since 1.0.0
   */
  private MultiLineString(List<List<Position>> coordinates) {
    this.coordinates = coordinates;
  }

  /**
   * Should always be "MultiLineString".
   *
   * @return String "MultiLineString".
   * @since 1.0.0
   */
  @Override
  public String getType() {
    return type;
  }

  /**
   * Get the list of {@link Position} making up the MultiLineString.
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
   * Creates a {@link MultiLineString} from a list of coordinates.
   *
   * @param coordinates List of {@link Position} coordinates.
   * @return {@link MultiLineString}.
   * @since 1.0.0
   */
  private static MultiLineString fromCoordinates(List<List<Position>> coordinates) {
    return new MultiLineString(coordinates);
  }

  public static MultiLineString fromCoordinates(double[][][] coordinates) {
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
   * Create a GeoJSON MultiLineString object from JSON.
   *
   * @param json String of JSON making up a MultiLineString.
   * @return {@link MultiLineString} GeoJSON object.
   * @since 1.0.0
   */
  public static MultiLineString fromJson(String json) {
    GsonBuilder gson = new GsonBuilder();
    gson.registerTypeAdapter(Position.class, new PositionDeserializer());
    return gson.create().fromJson(json, MultiLineString.class);
  }

  /**
   * Convert feature into JSON.
   *
   * @return String containing MultiLineString JSON.
   * @since 1.0.0
   */
  @Override
  public String toJson() {
    GsonBuilder gson = new GsonBuilder();
    gson.registerTypeAdapter(Position.class, new PositionSerializer());
    return gson.create().toJson(this);
  }
}
