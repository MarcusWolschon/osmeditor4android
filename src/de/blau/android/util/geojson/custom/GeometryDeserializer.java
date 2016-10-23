package de.blau.android.util.geojson.custom;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import de.blau.android.util.geojson.Geometry;

import java.lang.reflect.Type;

/**
 * Required to handle the "Unable to invoke no-args constructor for interface {@link Geometry} error
 * that Gson shows when trying to deserialize a list of {@link Geometry}.
 *
 * @since 1.0.0
 */
public class GeometryDeserializer implements JsonDeserializer<Geometry> {

  /**
   * Required to handle the "Unable to invoke no-args constructor for interface {@link Geometry}
   * error that Gson shows when trying to deserialize a list of {@link Geometry}.
   *
   * @param json    A class representing an element of Json.
   * @param typeOfT Common superinterface for all types in the Java.
   * @param context Context for deserialization that is passed to a custom deserializer during
   *                invocation of its {@link JsonDeserializer#deserialize(JsonElement, Type,
   *                JsonDeserializationContext)} method.
   * @return either default deserialization on the specified object or JsonParseException.
   * @throws JsonParseException This exception is raised if there is a serious issue that occurs
   *                            during parsing of a Json string.
   * @since 1.0.0
   */
  @Override
  public Geometry deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
    throws JsonParseException {
    // Find the actual class name from the type property in the JSON.
    String geometryType = json.getAsJsonObject().get("type").getAsString();
    try {
      // Use the current context to deserialize it
      Type classType = Class.forName("de.blau.android.util.geojson." + geometryType);
      return context.deserialize(json, classType);
    } catch (ClassNotFoundException classNotFoundException) {
      // Unknown geometry
      throw new JsonParseException(classNotFoundException);
    }
  }

}
