package de.blau.android.util.mapbox.geojson.custom;

import java.lang.reflect.Type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import de.blau.android.util.mapbox.models.Position;

/**
 * Required to handle the special case where the altitude might be a Double.NaN, which isn't a valid double value as per
 * JSON specification.
 *
 * @since 1.0.0
 */
public class PositionSerializer implements JsonSerializer<Position> {

    /**
     * Required to handle the special case where the altitude might be a Double.NaN, which isn't a valid double value as
     * per JSON specification.
     *
     * @param src A {@link Position} defined by a longitude, latitude, and optionally, an altitude.
     * @param typeOfSrc Common superinterface for all types in the Java.
     * @param context Context for deserialization that is passed to a custom deserializer during invocation of its
     *            {@link com.google.gson.JsonDeserializer#deserialize(JsonElement, Type, com.google.gson.JsonDeserializationContext)}
     *            method.
     * @return a JsonArray containing the raw coordinates.
     * @since 1.0.0
     */
    @Override
    public JsonElement serialize(Position src, Type typeOfSrc, JsonSerializationContext context) {
        JsonArray rawCoordinates = new JsonArray();
        rawCoordinates.add(new JsonPrimitive(src.getLongitude()));
        rawCoordinates.add(new JsonPrimitive(src.getLatitude()));

        // Includes altitude
        if (src.hasAltitude()) {
            rawCoordinates.add(new JsonPrimitive(src.getAltitude()));
        }

        return rawCoordinates;
    }

}
