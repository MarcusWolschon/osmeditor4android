package de.blau.android.util.mapbox.geojson;

import com.google.gson.GsonBuilder;

import de.blau.android.util.mapbox.geojson.custom.PositionDeserializer;
import de.blau.android.util.mapbox.geojson.custom.PositionSerializer;
import de.blau.android.util.mapbox.models.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * A MultiPolygon is a type of {@link Geometry}.
 *
 * @see <a href='http://geojson.org/geojson-spec.html#multipolygon'>Official GeoJSON MultiPolygon Specifications</a>
 * @since 1.0.0
 */
public class MultiPolygon implements Geometry<List<List<List<Position>>>> {

  private final String type = "MultiPolygon";
  private List<List<List<Position>>> coordinates;

  /**
   * Private constructor.
   *
   * @param coordinates List of {@link Position} making up the MultiPolygon.
   * @since 1.0.0
   */
  private MultiPolygon(List<List<List<Position>>> coordinates) {
    this.coordinates = coordinates;
  }

  /**
   * Should always be "MultiPolygon".
   *
   * @return String "MultiPolygon".
   * @since 1.0.0
   */
  @Override
  public String getType() {
    return type;
  }

  /**
   * Get the list of {@link Position} making up the MultiPolygon.
   *
   * @return List of {@link Position}.
   * @since 1.0.0
   */
  @Override
  public List<List<List<Position>>> getCoordinates() {
    return coordinates;
  }

  @Override
  public void setCoordinates(List<List<List<Position>>> coordinates) {
    this.coordinates = coordinates;
  }

  /**
   * Creates a {@link MultiPolygon} from a list of coordinates.
   *
   * @param coordinates List of {@link Position} coordinates.
   * @return {@link MultiPolygon}.
   * @since 1.0.0
   */
  public static MultiPolygon fromCoordinates(List<List<List<Position>>> coordinates) {
    return new MultiPolygon(coordinates);
  }

  public static MultiPolygon fromCoordinates(double[][][][] coordinates) {
    List<List<List<Position>>> converted = new ArrayList<>(coordinates.length);
    for (int i = 0; i < coordinates.length; i++) {
      List<List<Position>> innerOneList = new ArrayList<>(coordinates[i].length);
      for (int j = 0; j < coordinates[i].length; j++) {
        List<Position> innerTwoList = new ArrayList<>(coordinates[i][j].length);
        for (int k = 0; k < coordinates[i][j].length; k++) {
          innerTwoList.add(Position.fromCoordinates(coordinates[i][j][k]));
        }
        innerOneList.add(innerTwoList);
      }
      converted.add(innerOneList);
    }

    return fromCoordinates(converted);
  }

  /**
   * Create a GeoJSON MultiPolygon object from JSON.
   *
   * @param json String of JSON making up a MultiPolygon.
   * @return {@link MultiPolygon} GeoJSON object.
   * @since 1.0.0
   */
  public static MultiPolygon fromJson(String json) {
    GsonBuilder gson = new GsonBuilder();
    gson.registerTypeAdapter(Position.class, new PositionDeserializer());
    return gson.create().fromJson(json, MultiPolygon.class);
  }

  /**
   * Convert feature into JSON.
   *
   * @return String containing MultiPolygon JSON.
   * @since 1.0.0
   */
  @Override
  public String toJson() {
    GsonBuilder gson = new GsonBuilder();
    gson.registerTypeAdapter(Position.class, new PositionSerializer());
    return gson.create().toJson(this);
  }
}
