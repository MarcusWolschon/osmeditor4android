package de.blau.android.util.mapbox.geojson.custom;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import de.blau.android.util.mapbox.models.Position;

import java.lang.reflect.Type;

/**
 * Required to handle the "Expected BEGIN_OBJECT but was BEGIN_ARRAY" error that Gson would show
 * otherwise.
 *
 * @since 1.0.0
 */
public class PositionDeserializer implements JsonDeserializer<Position> {

  /**
   * Required to handle the "Expected BEGIN_OBJECT but was BEGIN_ARRAY" error that Gson would show
   * otherwise.
   *
   * @param json    A class representing an element of Json.
   * @param typeOfT Common superinterface for all types in the Java.
   * @param context Context for deserialization that is passed to a custom deserializer during
   *                invocation of its {@link JsonDeserializer#deserialize(JsonElement, Type,
   *                JsonDeserializationContext)} method.
   * @return Either {@link Position} with an altitude or one that doesn't.
   * @throws JsonParseException This exception is raised if there is a serious issue that occurs
   *                            during parsing of a Json string.
   * @since 1.0.0
   */
  @Override
  public Position deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
    throws JsonParseException {
    JsonArray rawCoordinates = json.getAsJsonArray();
    double longitude = rawCoordinates.get(0).getAsDouble();
    double latitude = rawCoordinates.get(1).getAsDouble();

    // Includes altitude
    if (rawCoordinates.size() > 2) {
      double altitude = rawCoordinates.get(2).getAsDouble();
      return Position.fromCoordinates(longitude, latitude, altitude);
    }

    // It doesn't have altitude
    return Position.fromCoordinates(longitude, latitude);
  }

}
