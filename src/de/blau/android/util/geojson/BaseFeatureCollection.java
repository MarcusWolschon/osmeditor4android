package de.blau.android.util.geojson;

import com.google.gson.GsonBuilder;
import de.blau.android.util.geojson.custom.GeometryDeserializer;
import de.blau.android.util.geojson.custom.PositionDeserializer;
import de.blau.android.util.geojson.custom.PositionSerializer;
import de.blau.android.util.models.Position;

/**
 * Shared by FeatureCollection and CarmenFeatureCollection
 *
 * @since 1.0.0
 */
public class BaseFeatureCollection implements GeoJSON {

  private final String type = "FeatureCollection";

  /**
   * Should always be "FeatureCollection".
   *
   * @return String "FeatureCollection".
   * @since 1.0.0
   */
  @Override
  public String getType() {
    return type;
  }

  /**
   * Create a GeoJSON feature collection object from JSON.
   *
   * @param json String of JSON making up a feature collection.
   * @return {@link FeatureCollection} GeoJSON object.
   * @since 1.0.0
   */
  public static FeatureCollection fromJson(String json) {
    GsonBuilder gson = new GsonBuilder();
    gson.registerTypeAdapter(Position.class, new PositionDeserializer());
    gson.registerTypeAdapter(Geometry.class, new GeometryDeserializer());
    return gson.create().fromJson(json, FeatureCollection.class);
  }

  /**
   * Convert feature collection into JSON.
   *
   * @return String containing feature collection JSON.
   * @since 1.0.0
   */
  @Override
  public String toJson() {
    GsonBuilder gson = new GsonBuilder();
    gson.registerTypeAdapter(Position.class, new PositionSerializer());
    return gson.create().toJson(this);
  }

}
