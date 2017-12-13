package de.blau.android.util.mapbox.geojson;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.blau.android.util.mapbox.geojson.custom.GeometryDeserializer;
import de.blau.android.util.mapbox.geojson.custom.PositionDeserializer;
import de.blau.android.util.mapbox.geojson.custom.PositionSerializer;
import de.blau.android.util.mapbox.models.Position;

/**
 * A GeoJSON object with the type "Feature" is a feature object.
 *
 * @see <a href='geojson.org/geojson-spec.html#feature-objects'>Official GeoJSON Feature Specifications</a>
 * @since 1.0.0
 */
public class Feature implements GeoJSON {

    private final String type = "Feature";
    private Geometry     geometry;
    private JsonObject   properties;
    private String       id;

    /**
     * Private constructor.
     *
     * @param geometry {@link Geometry} object.
     * @param properties of this feature as JSON.
     * @param id common identifier of this feature.
     * @since 1.0.0
     */
    private Feature(Geometry geometry, JsonObject properties, String id) {
        this.geometry = geometry;
        this.properties = properties;
        this.id = id;
    }

    /**
     * Should always be "Feature".
     *
     * @return String "Feature".
     * @since 1.0.0
     */
    @Override
    public String getType() {
        return type;
    }

    /**
     * Get the features {@link Geometry}.
     *
     * @return {@link Geometry} of the feature or null if not set.
     * @since 1.0.0
     */
    public Geometry getGeometry() {
        return geometry;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    /**
     * Returns the optional properties of this feature as JSON.
     *
     * @return the properties of this feature
     * @since 1.0.0
     */
    public JsonObject getProperties() {
        if (properties == null) {
            properties = new JsonObject();
        }
        return properties;
    }

    public void setProperties(JsonObject properties) {
        this.properties = properties;
    }

    /**
     * The optional, common identifier of this feature.
     *
     * @return The common identifier of this feature, if set.
     * @since 1.0.0
     */
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Create a feature from geometry.
     *
     * @param geometry {@link Geometry} object.
     * @return {@link Feature}
     * @since 1.0.0
     */
    public static Feature fromGeometry(Geometry geometry) {
        return new Feature(geometry, new JsonObject(), null);
    }

    /**
     * Create a feature from geometry.
     *
     * @param geometry {@link Geometry} object.
     * @param properties of this feature as JSON.
     * @return {@link Feature}
     * @since 1.0.0
     */
    public static Feature fromGeometry(Geometry geometry, JsonObject properties) {
        return new Feature(geometry, properties, null);
    }

    /**
     * Create a feature from geometry.
     *
     * @param geometry {@link Geometry} object.
     * @param properties of this feature as JSON.
     * @param id common identifier of this feature.
     * @return {@link Feature}
     * @since 1.0.0
     */
    public static Feature fromGeometry(Geometry geometry, JsonObject properties, String id) {
        return new Feature(geometry, properties, id);
    }

    /**
     * Create a GeoJSON feature object from JSON.
     *
     * @param json String of JSON making up a feature.
     * @return {@link Feature} GeoJSON object.
     * @since 1.0.0
     */
    public static Feature fromJson(String json) {
        GsonBuilder gson = new GsonBuilder();
        gson.registerTypeAdapter(Position.class, new PositionDeserializer());
        gson.registerTypeAdapter(Geometry.class, new GeometryDeserializer());
        return gson.create().fromJson(json, Feature.class);
    }

    /**
     * Convert feature into JSON.
     *
     * @return String containing feature JSON.
     * @since 1.0.0
     */
    @Override
    public String toJson() {
        GsonBuilder gson = new GsonBuilder();
        gson.registerTypeAdapter(Position.class, new PositionSerializer());
        return gson.create().toJson(this);
    }

    /**
     * Convenience method to add a String member.
     *
     * @param key name of the member
     * @param value the String value associated with the member
     * @since 1.0.0
     */
    public void addStringProperty(String key, String value) {
        getProperties().addProperty(key, value);
    }

    /**
     * Convenience method to add a Number member.
     *
     * @param key name of the member
     * @param value the Number value associated with the member
     * @since 1.0.0
     */
    public void addNumberProperty(String key, Number value) {
        getProperties().addProperty(key, value);
    }

    /**
     * Convenience method to add a Boolean member.
     *
     * @param key name of the member
     * @param value the Boolean value associated with the member
     * @since 1.0.0
     */
    public void addBooleanProperty(String key, Boolean value) {
        getProperties().addProperty(key, value);
    }

    /**
     * Convenience method to add a Character member.
     *
     * @param key name of the member
     * @param value the Character value associated with the member
     * @since 1.0.0
     */
    public void addCharacterProperty(String key, Character value) {
        getProperties().addProperty(key, value);
    }

    /**
     * Convenience method to add a JsonElement member.
     *
     * @param key name of the member
     * @param value the JsonElement value associated with the member
     * @since 1.0.0
     */
    public void addProperty(String key, JsonElement value) {
        getProperties().add(key, value);
    }

    /**
     * Convenience method to get a String member.
     *
     * @param key name of the member
     * @return the value of the member, null if it doesn't exist
     * @since 1.0.0
     */
    public String getStringProperty(String key) {
        return getProperties().get(key).getAsString();
    }

    /**
     * Convenience method to get a Number member.
     *
     * @param key name of the member
     * @return the value of the member, null if it doesn't exist
     * @since 1.0.0
     */
    public Number getNumberProperty(String key) {
        return getProperties().get(key).getAsNumber();
    }

    /**
     * Convenience method to get a Boolean member.
     *
     * @param key name of the member
     * @return the value of the member, null if it doesn't exist
     * @since 1.0.0
     */
    public Boolean getBooleanProperty(String key) {
        return getProperties().get(key).getAsBoolean();
    }

    /**
     * Convenience method to get a Character member.
     *
     * @param key name of the member
     * @return the value of the member, null if it doesn't exist
     * @since 1.0.0
     */
    public Character getCharacterProperty(String key) {
        return getProperties().get(key).getAsCharacter();
    }

    /**
     * Convenience method to get a JsonElement member.
     *
     * @param key name of the member
     * @return the value of the member, null if it doesn't exist
     * @since 1.0.0
     */
    private JsonElement getProperty(String key) {
        return getProperties().get(key);
    }

    /**
     * Removes the property from the object properties
     *
     * @param key name of the member
     * @return Removed {@code property} from the key string passed in through the parameter.
     * @since 1.0.0
     */
    public JsonElement removeProperty(String key) {
        return getProperties().remove(key);
    }

    /**
     * Convenience method to check if a member with the specified name is present in this object.
     *
     * @param key name of the member
     * @return true if there is the member has the specified name, false otherwise.
     * @since 1.0.0
     */
    private boolean hasProperty(String key) {
        return getProperties().has(key);
    }

    /**
     * Convenience method to check for a member by name as well as non-null value.
     *
     * @param key name of the member
     * @return true if member is present with non-null value, false otherwise.
     * @since 1.3.0
     */
    public boolean hasNonNullValueForProperty(String key) {
        return hasProperty(key) && !getProperty(key).isJsonNull();
    }
}
