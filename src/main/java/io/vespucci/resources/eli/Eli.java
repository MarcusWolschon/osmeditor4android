package io.vespucci.resources.eli;

import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mapbox.geojson.Feature;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.osm.BoundingBox;
import io.vespucci.resources.TileLayerSource;
import io.vespucci.resources.TileLayerSource.Category;
import io.vespucci.resources.TileLayerSource.Provider;
import io.vespucci.resources.TileLayerSource.TileType;
import io.vespucci.util.DateFormatter;
import io.vespucci.util.GeoJson;

public final class Eli {
    private static final String HEADER_VALUE_KEY = "header-value";

    private static final String HEADER_NAME_KEY = "header-name";

    private static final String DEBUG_TAG = Eli.class.getSimpleName().substring(0, Math.min(23, Eli.class.getSimpleName().length()));

    public static final String VERSION_1_1 = "1.1";

    private static final String MIN_ZOOM_KEY              = "min_zoom";
    private static final String MAX_ZOOM_KEY              = "max_zoom";
    private static final String TYPE_KEY                  = "type";
    private static final String CATEGORY_KEY              = "category";
    private static final String ID_KEY                    = "id";
    private static final String NAME_KEY                  = "name";
    private static final String OVERLAY_KEY               = "overlay";
    private static final String DEFAULT_KEY               = "default";
    private static final String BEST_KEY                  = "best";
    private static final String LICENSE_URL_KEY           = "license_url";
    private static final String ATTRIBUTION_KEY           = "attribution";
    private static final String URL_KEY                   = "url";
    private static final String TEXT_KEY                  = "text";
    private static final String ICON_KEY                  = "icon";
    private static final String START_DATE_KEY            = "start_date";
    private static final String END_DATE_KEY              = "end_date";
    private static final String DESCRIPTION_KEY           = "description";
    private static final String PRIVACY_POLICY_URL_KEY    = "privacy_policy_url";
    private static final String NO_TILE_HEADER_KEY        = "no_tile_header";
    private static final String NO_TILE_TILE_KEY          = "no_tile_tile";
    private static final String AVAILABLE_PROJECTIONS_KEY = "available_projections";
    private static final String TILE_SIZE_KEY             = "tile-size";
    private static final String TILE_TYPE_KEY             = "tile_type";            // extension
    private static final String MVT_VALUE                 = "mvt";
    private static final String CUSTOM_HTTP_HEADERS_KEY   = "custom-http-headers";

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
     * @return the string or null if it couldn't be found
     */
    @Nullable
    private static String getJsonString(@NonNull JsonObject jsonObject, @NonNull String name) {
        JsonElement field = jsonObject.get(name);
        if (field != null && field.isJsonPrimitive() && ((JsonPrimitive) field).isString()) {
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
    private static String[] getJsonStringArray(@NonNull JsonObject jsonObject, @NonNull String name) {
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
    private static JsonObject getJsonObject(@NonNull JsonObject jsonObject, @NonNull String name) {
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
    private static boolean getJsonBoolean(@NonNull JsonObject jsonObject, @NonNull String name) {
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
    private static int getJsonInteger(@NonNull JsonObject jsonObject, @NonNull String name, final int defaultValue) {
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
            int minZoom = getJsonInteger(properties, MIN_ZOOM_KEY, TileLayerSource.DEFAULT_MIN_ZOOM);
            int maxZoom = getJsonInteger(properties, MAX_ZOOM_KEY, TileLayerSource.NO_MAX_ZOOM);

            String type = getJsonString(properties, TYPE_KEY);
            boolean isWMS = TileLayerSource.TYPE_WMS.equals(type);
            if (maxZoom == TileLayerSource.NO_MAX_ZOOM) {
                if (isWMS) {
                    maxZoom = TileLayerSource.DEFAULT_WMS_MAX_ZOOM;
                } else {
                    maxZoom = TileLayerSource.DEFAULT_MAX_ZOOM;
                }
            }

            Category category = getCategory(getJsonString(properties, CATEGORY_KEY));

            Provider provider = new Provider();
            if (boxes.isEmpty()) {
                provider.addCoverageArea(new Provider.CoverageArea(minZoom, maxZoom, null));
            } else {
                for (BoundingBox box : boxes) {
                    provider.addCoverageArea(new Provider.CoverageArea(minZoom, maxZoom, box));
                }
            }

            String id = getJsonString(properties, ID_KEY);
            String url = getJsonString(properties, URL_KEY);
            String name = getJsonString(properties, NAME_KEY);
            boolean overlay = getJsonBoolean(properties, OVERLAY_KEY);
            boolean defaultLayer = getJsonBoolean(properties, DEFAULT_KEY);
            int preference = getJsonBoolean(properties, BEST_KEY) ? TileLayerSource.PREFERENCE_BEST : TileLayerSource.PREFERENCE_DEFAULT;

            String termsOfUseUrl = getJsonString(properties, LICENSE_URL_KEY);

            JsonObject attribution = (JsonObject) properties.get(ATTRIBUTION_KEY);
            if (attribution != null) {
                provider.setAttributionUrl(getJsonString(attribution, URL_KEY));
                provider.setAttribution(getJsonString(attribution, TEXT_KEY));
            }
            String icon = getJsonString(properties, ICON_KEY);
            long startDate = -1L;
            long endDate = Long.MAX_VALUE;
            String dateString = getJsonString(properties, START_DATE_KEY);
            if (dateString != null) {
                startDate = dateStringToTime(dateString);
            }
            dateString = getJsonString(properties, END_DATE_KEY);
            if (dateString != null) {
                endDate = dateStringToTime(dateString);
            }

            String description = getJsonString(properties, DESCRIPTION_KEY);
            String privacyPolicyUrl = getJsonString(properties, PRIVACY_POLICY_URL_KEY);

            String noTileHeader = null;
            String[] noTileValues = null;
            JsonObject noTileHeaderObject = getJsonObject(properties, NO_TILE_HEADER_KEY);
            if (noTileHeaderObject != null) {
                Iterator<Entry<String, JsonElement>> it = noTileHeaderObject.entrySet().iterator();
                if (it.hasNext()) { // we only support one entry
                    Entry<String, JsonElement> entry = it.next();
                    noTileHeader = entry.getKey();
                    noTileValues = getJsonStringArray(noTileHeaderObject, noTileHeader);
                }
            }

            byte[] noTileTile = null;
            String noTileTileString = getJsonString(properties, NO_TILE_TILE_KEY);
            if (noTileTileString != null) {
                noTileTile = hexStringToByteArray(noTileTileString);
            }

            String proj = null;
            JsonArray projections = (JsonArray) properties.get(AVAILABLE_PROJECTIONS_KEY);
            if (projections != null) {
                for (JsonElement p : projections) {
                    String supportedProj = p.getAsString();
                    boolean latLon = TileLayerSource.isLatLon(supportedProj);
                    if (TileLayerSource.is3857compatible(supportedProj) || latLon) {
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
            // tile-size will override anything determined automatically
            JsonElement tileSize = properties.get(TILE_SIZE_KEY);
            if (tileSize != null && tileSize.isJsonPrimitive()) {
                tileWidth = tileSize.getAsInt();
                tileHeight = tileWidth;
            }

            if (type == null || url == null || (isWMS && proj == null)) {
                Log.w(DEBUG_TAG, "skipping name " + name + " id " + id + " type " + type + " url " + url);
                if (TileLayerSource.TYPE_WMS.equals(type)) {
                    Log.w(DEBUG_TAG, "projections: " + projections);
                }
                return null;
            }
            osmts = new TileLayerSource(ctx, id, name, url, type, category, overlay, defaultLayer, provider, termsOfUseUrl, icon, null, null, minZoom, maxZoom,
                    TileLayerSource.DEFAULT_MAX_OVERZOOM, tileWidth, tileHeight, proj, preference, startDate, endDate, noTileHeader, noTileValues,
                    privacyPolicyUrl, async);
            osmts.setDescription(description);
            osmts.setNoTileTile(noTileTile);
            if (TileLayerSource.TYPE_TMS.equals(osmts.getType()) || TileLayerSource.TYPE_PMT_3.equals(osmts.getType())) {
                osmts.setTileType(MVT_VALUE.equals(getJsonString(properties, TILE_TYPE_KEY)) ? TileType.MVT : TileType.BITMAP);
            }
            // we currently only support a single header object
            JsonObject headers = getJsonObject(properties, CUSTOM_HTTP_HEADERS_KEY);
            if (headers != null) {
                String headerName = headers.getAsJsonPrimitive(HEADER_NAME_KEY).getAsString();
                String headerValue = headers.getAsJsonPrimitive(HEADER_VALUE_KEY).getAsString();
                if (headerName != null && headerValue != null) {
                    osmts.setHeaders(io.vespucci.util.Util.wrapInList(new TileLayerSource.Header(headerName, headerValue)));
                }
            }
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

    /**
     * Convert hex string to a byte array
     * 
     * See https://stackoverflow.com/a/140861
     * 
     * @param s the String
     * @return a array of byte
     */
    @NonNull
    private static byte[] hexStringToByteArray(@NonNull String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
