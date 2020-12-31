package de.blau.android.resources.eli;

import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mapbox.geojson.Feature;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.osm.BoundingBox;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.resources.TileLayerSource.Category;
import de.blau.android.resources.TileLayerSource.Provider;
import de.blau.android.util.DateFormatter;
import de.blau.android.util.GeoJson;

public final class Eli {
    private static final String DEBUG_TAG = "Eli";

    public static final String VERSION_120 = "1.2.0";

    /**
     * Private constructor to prevent instantiation
     */
    private Eli() {
        // private constructor
    }

    /**
     * Get a string with name from a JsonObject
     * 
     * @param jsonObject the JsonObject
     * @param name the name of the string we want to retrieve
     * @return the string or null if it couldb't be found
     */
    @Nullable
    static String getJsonString(@NonNull JsonObject jsonObject, @NonNull String name) {
        JsonElement field = jsonObject.get(name);
        if (field != null && field.isJsonPrimitive()) {
            return field.getAsString();
        }
        return null;
    }

    /**
     * Get a string array with name from a JsonObject
     * 
     * @param jsonObject the JsonObject
     * @param name the name of the string we want to retrieve
     * @return the string array or null if it couldb't be found or if the field wasn't an array
     */
    @Nullable
    static String[] getJsonStringArray(@NonNull JsonObject jsonObject, @NonNull String name) {
        JsonElement field = jsonObject.get(name);
        if (field != null && field.isJsonArray()) {
            JsonArray array = field.getAsJsonArray();
            int length = array.size();
            String[] result = new String[length];
            for (int i = 0; i < length; i++) {
                result[i] = array.get(i).getAsString();
            }
            return result;
        }
        return null;
    }

    /**
     * Get a string array with name from a JsonObject
     * 
     * @param jsonObject the JsonObject
     * @param name the name of the string we want to retrieve
     * @return the string array or null if it couldb't be found
     */
    @Nullable
    static JsonObject getJsonObject(@NonNull JsonObject jsonObject, @NonNull String name) {
        JsonElement field = jsonObject.get(name);
        if (field != null && field.isJsonObject()) {
            return (JsonObject) field;
        }
        return null;
    }

    /**
     * Get a boolean with name from a JsonObject
     * 
     * @param jsonObject the JsonObject
     * @param name the name of the boolean we want to retrieve
     * @return the value or false if it couldb't be found
     */
    static boolean getJsonBoolean(@NonNull JsonObject jsonObject, @NonNull String name) {
        JsonElement field = jsonObject.get(name);
        if (field != null && field.isJsonPrimitive()) {
            return field.getAsBoolean();
        }
        return false;
    }

    /**
     * Get an int with name from a JsonObject
     * 
     * @param jsonObject the JsonObject
     * @param name the name of the boolean we want to retrieve
     * @param defaultValue the value to use if the int couldn't be found
     * @return the value or defaltValue if it couldb't be found
     */
    static int getJsonInteger(@NonNull JsonObject jsonObject, @NonNull String name, final int defaultValue) {
        JsonElement field = jsonObject.get(name);
        if (field != null && field.isJsonPrimitive()) {
            return field.getAsInt();
        }
        return defaultValue;
    }

    /**
     * Create a TileLayerServer instance from a GeoJson Feature
     * 
     * @param ctx Android Context
     * @param f the GeoJosn Feature containing the config
     * @param async if true retrieve meta-info async
     * @param fakeMultiPolygon if true assume the polygon geometry is all outer rings
     * @return a TileLayerServer instance of null if it couldn't be created
     */
    @Nullable
    public static TileLayerSource geojsonToServer(@NonNull Context ctx, @NonNull Feature f, boolean async, boolean fakeMultiPolygon) {
        TileLayerSource osmts = null;

        try {

            int tileWidth = TileLayerSource.DEFAULT_TILE_SIZE;
            int tileHeight = TileLayerSource.DEFAULT_TILE_SIZE;

            JsonObject properties = f.properties();

            List<BoundingBox> boxes = GeoJson.getBoundingBoxes(f, fakeMultiPolygon);
            int minZoom = getJsonInteger(properties, "min_zoom", TileLayerSource.DEFAULT_MIN_ZOOM);
            int maxZoom = getJsonInteger(properties, "max_zoom", TileLayerSource.NO_MAX_ZOOM);

            String type = getJsonString(properties, "type");
            boolean isWMS = TileLayerSource.TYPE_WMS.equals(type);
            if (maxZoom == TileLayerSource.NO_MAX_ZOOM) {
                if (isWMS) {
                    maxZoom = TileLayerSource.DEFAULT_WMS_MAX_ZOOM;
                } else {
                    maxZoom = TileLayerSource.DEFAULT_MAX_ZOOM;
                }
            }

            Category category = getCategory(getJsonString(properties, "category"));

            Provider provider = new Provider();
            if (boxes.isEmpty()) {
                provider.addCoverageArea(new Provider.CoverageArea(minZoom, maxZoom, null));
            } else {
                for (BoundingBox box : boxes) {
                    provider.addCoverageArea(new Provider.CoverageArea(minZoom, maxZoom, box));
                }
            }

            String id = getJsonString(properties, "id");
            String url = getJsonString(properties, "url");
            String name = getJsonString(properties, "name");
            boolean overlay = getJsonBoolean(properties, "overlay");
            boolean defaultLayer = getJsonBoolean(properties, "default");
            int preference = getJsonBoolean(properties, "best") ? TileLayerSource.PREFERENCE_BEST : TileLayerSource.PREFERENCE_DEFAULT;

            String termsOfUseUrl = getJsonString(properties, "license_url");

            JsonObject attribution = (JsonObject) properties.get("attribution");
            if (attribution != null) {
                provider.setAttributionUrl(getJsonString(attribution, "url"));
                provider.setAttribution(getJsonString(attribution, "text"));
            }
            String icon = getJsonString(properties, "icon");
            long startDate = -1L;
            long endDate = Long.MAX_VALUE;
            String dateString = getJsonString(properties, "start_date");
            if (dateString != null) {
                startDate = dateStringToTime(dateString);
            }
            dateString = getJsonString(properties, "end_date");
            if (dateString != null) {
                endDate = dateStringToTime(dateString);
            }

            String description = getJsonString(properties, "description");
            String privacyPolicyUrl = getJsonString(properties, "privacy_policy_url");

            String noTileHeader = null;
            String[] noTileValues = null;
            JsonObject noTileHeaderObject = getJsonObject(properties, "no_tile_header");
            if (noTileHeaderObject != null) {
                Iterator<Entry<String, JsonElement>> it = noTileHeaderObject.entrySet().iterator();
                if (it.hasNext()) { // we only support one entry
                    Entry<String, JsonElement> entry = it.next();
                    noTileHeader = entry.getKey();
                    noTileValues = getJsonStringArray(noTileHeaderObject, noTileHeader);
                }
            }

            String proj = null;
            JsonArray projections = (JsonArray) properties.get("available_projections");
            if (projections != null) {
                for (JsonElement p : projections) {
                    String supportedProj = p.getAsString();
                    boolean latLon = TileLayerSource.EPSG_4326.equals(supportedProj);
                    if (TileLayerSource.EPSG_3857.equals(supportedProj) || TileLayerSource.EPSG_900913.equals(supportedProj) || latLon) {
                        proj = supportedProj;
                        if (latLon) {
                            // small tiles keep errors small since we don't actually reproject tiles
                            tileWidth = TileLayerSource.DEFAULT_TILE_SIZE;
                            tileHeight = TileLayerSource.DEFAULT_TILE_SIZE;
                            // continue on searching for web mercator
                        } else {
                            tileWidth = TileLayerSource.WMS_TILE_SIZE;
                            tileHeight = TileLayerSource.WMS_TILE_SIZE;
                            break; // found web mercator compatible projection
                        }
                    }
                }
            }

            if (type == null || url == null || (isWMS && proj == null)) {
                Log.w(DEBUG_TAG, "skipping name " + name + " id " + id + " type " + type + " url " + url);
                if (TileLayerSource.TYPE_WMS.equals(type)) {
                    Log.w(DEBUG_TAG, "projections: " + projections);
                }
                return null;
            }
            osmts = new TileLayerSource(ctx, id, name, url, type, category, overlay, defaultLayer, provider, termsOfUseUrl, icon, null, null, minZoom, maxZoom,
                    TileLayerSource.DEFAULT_MAX_OVERZOOM, tileWidth, tileHeight, proj, preference, startDate, endDate, noTileHeader, noTileValues, description,
                    privacyPolicyUrl, async);
        } catch (UnsupportedOperationException uoex) {
            Log.e(DEBUG_TAG, "Got " + uoex.getMessage());
        }
        return osmts;
    }

    /**
     * Get the Category from a String
     * 
     * @param categoryString String with a category value or null
     * @return the Category or Category.other if it can't be determined
     */
    @NonNull
    private static Category getCategory(@Nullable String categoryString) {
        if (categoryString != null) {
            try {
                return Category.valueOf(categoryString);
            } catch (IllegalArgumentException e) {
                Log.e(DEBUG_TAG, "Unknown category value " + categoryString);
            }
        }
        return Category.other;
    }

    /**
     * Parse a RFC3339 timestamp into a time value since epoch, ignores non date parts
     * 
     * @param timeStamp the date string to parse
     * @return the time value or -1 if parsing failed
     */
    @NonNull
    private static long dateStringToTime(@Nullable String timeStamp) {
        long result = -1L;
        if (timeStamp != null && !"".equals(timeStamp)) {
            String[] parts = timeStamp.split("T");
            String f = "yyyy-MM-dd";
            try {
                int l = parts[0].length();
                if (l == 4) { // slightly hackish way of determining which format to use
                    f = "yyyy";
                } else if (l < 8) {
                    f = "yyyy-MM";
                }
                Date d = DateFormatter.getUtcFormat(f).parse(parts[0]);
                result = d.getTime();
            } catch (ParseException e) {
                Log.e(DEBUG_TAG, "Invalid RFC3339 value (" + f + ") " + timeStamp + " " + e.getMessage());
            }
        }
        return result;
    }
}
