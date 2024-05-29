package de.blau.android.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.gson.stream.JsonReader;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.contract.Files;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmXml;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Tags;
import de.blau.android.osm.ViewBox;
import de.blau.android.osm.Way;
import de.westnordost.countryboundaries.CountryBoundaries;

/**
 * Class to determine certain general properties of the environment we are mapping in from the geographic location
 * 
 * @author simon
 *
 */
public class GeoContext {
    private static final String DEBUG_TAG = GeoContext.class.getSimpleName().substring(0, Math.min(23, GeoContext.class.getSimpleName().length()));

    private static final String SPEED_LIMITS      = "speed-limits";
    private static final String LEFT_HAND_TRAFFIC = "left-hand-traffic";
    private static final String IMPERIAL          = "imperial";
    private static final String DISTANCE          = "distance";
    private static final String LANGUAGES         = "languages";
    private static final String ADDRESS_KEYS      = "address-keys";

    private final CountryBoundaries countryBoundaries;

    /**
     * Wrapper to return country and state values for a location
     * 
     * @author simon
     *
     */
    public class CountryAndStateIso {
        String country;
        String state;

        /**
         * Construct a new instance
         * 
         * @param country the country ISO code
         * @param state the state ISO code or null
         */
        CountryAndStateIso(@NonNull String country, @Nullable String state) {
            this.country = country;
            this.state = state;
        }

        /**
         * @return the country
         */
        public String getCountry() {
            return country;
        }

        /**
         * @return the state
         */
        public String getState() {
            return state;
        }
    }

    public class Properties {
        boolean          imperialUnits   = false;
        boolean          leftHandTraffic = false;
        private int[]    speedLimits;
        private String[] languages;
        private String[] addressKeys;

        /**
         * Get an array of common speed limits, add mph if imperialUnits is true
         * 
         * @return the speedLimits
         */
        @Nullable
        public String[] getSpeedLimits() {
            String[] result = null;
            if (speedLimits != null) {
                int length = speedLimits.length;
                result = new String[length];
                for (int i = 0; i < length; i++) {
                    result[i] = Integer.toString(speedLimits[i]) + (imperialUnits ? Tags.MPH : "");
                }
            }
            return result;
        }

        /**
         * @return true if imperial units are in use
         */
        public boolean imperialUnits() {
            return imperialUnits;
        }

        /**
         * Get the languages for the territory
         * 
         * @return an array with language codes or null
         */
        @Nullable
        public String[] getLanguages() {
            return languages;
        }

        /**
         * Get OSM address keys for the territory
         * 
         * @return an array with keys or null
         */
        @Nullable
        public String[] getAddressKeys() {
            return addressKeys;
        }
    }

    private final Map<String, Properties> properties;

    /**
     * Implicit assumption that the data will be short and that it is OK to read in synchronously which may not be true
     * any longer
     * 
     * @param context Android Context
     */
    public GeoContext(@NonNull Context context) {
        Log.d(DEBUG_TAG, "Initalizing");
        AssetManager assetManager = context.getAssets();
        countryBoundaries = getCountryBoundariesFromAssets(assetManager, Files.FILE_NAME_BOUNDARIES);
        properties = getPropertiesMap(assetManager, Files.FILE_NAME_GEOCONTEXT);
    }

    /**
     * Load the country boundaries data from file
     * 
     * @param assetManager a AssetMangager instance
     * @param fileName the filename
     * @return a COuntryBoundaries instance or null
     */
    @Nullable
    CountryBoundaries getCountryBoundariesFromAssets(@NonNull AssetManager assetManager, @NonNull String fileName) {
        try {
            return CountryBoundaries.load(assetManager.open(fileName));
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "Reading boundaries failed with " + e.getMessage());
            return null;
        }
    }

    /**
     * Read a GeoJson file from assets
     * 
     * @param assetManager an AssetManager
     * @param fileName the name of the file
     * @return a GeoJson FeatureCollection
     */
    @NonNull
    private Map<String, Properties> getPropertiesMap(@NonNull AssetManager assetManager, @NonNull String fileName) {
        Map<String, Properties> result = new HashMap<>();
        try (InputStream is = assetManager.open(fileName); JsonReader reader = new JsonReader(new InputStreamReader(is, OsmXml.UTF_8))) {
            reader.beginObject();
            while (reader.hasNext()) {
                String territory = reader.nextName();
                Properties prop = new Properties();
                reader.beginObject();
                while (reader.hasNext()) {
                    String propName = reader.nextName();
                    switch (propName) {
                    case DISTANCE:
                        prop.imperialUnits = IMPERIAL.equals(reader.nextString());
                        break;
                    case LEFT_HAND_TRAFFIC:
                        prop.leftHandTraffic = reader.nextBoolean();
                        break;
                    case SPEED_LIMITS:
                        reader.beginArray();
                        List<Integer> speedLimits = new ArrayList<>();
                        while (reader.hasNext()) {
                            speedLimits.add(reader.nextInt());
                        }
                        reader.endArray();
                        int size = speedLimits.size();
                        prop.speedLimits = new int[speedLimits.size()];
                        for (int i = 0; i < size; i++) {
                            prop.speedLimits[i] = speedLimits.get(i);
                        }
                        break;
                    case LANGUAGES:
                        reader.beginArray();
                        List<String> languages = new ArrayList<>();
                        while (reader.hasNext()) {
                            languages.add(reader.nextString());
                        }
                        reader.endArray();
                        prop.languages = languages.toArray(new String[0]);
                        break;
                    case ADDRESS_KEYS:
                        reader.beginArray();
                        List<String> addressKeys = new ArrayList<>();
                        while (reader.hasNext()) {
                            addressKeys.add(reader.nextString());
                        }
                        reader.endArray();
                        prop.addressKeys = addressKeys.toArray(new String[0]);
                        break;
                    default:
                        Log.e(DEBUG_TAG, "Unknown property " + propName);
                        reader.skipValue();
                    }
                }
                reader.endObject();
                result.put(territory, prop);
            }
            reader.endObject();
            Log.d(DEBUG_TAG, "Found " + result.size() + " entries.");
        } catch (IOException | NumberFormatException e) {
            Log.d(DEBUG_TAG, "Reading " + fileName + " " + e.getMessage());
        }
        return result;
    }

    /**
     * Check if a coordinate is in a territory that uses imperial units
     * 
     * @param lon longitude
     * @param lat latitude
     * @return true if the territory uses imperial units
     */
    public boolean imperial(double lon, double lat) {
        Properties result = getProperties(lon, lat);
        if (result == null) {
            return false;
        }
        return result.imperialUnits;
    }

    /**
     * Get the properties for a specific territory
     * 
     * @param lon WGS84 longitude of the location
     * @param lat WGS84 latitude of the location
     * @return a Properties instance or null if not found
     */
    @Nullable
    private Properties getProperties(double lon, double lat) {
        List<String> territories = getIsoCodes(lon, lat);
        return getProperties(territories);
    }

    /**
     * Get the properties for a specific territory
     * 
     * @param territories List of ISO codes
     * @return a Properties instance or null if not found
     */
    @Nullable
    public Properties getProperties(@Nullable List<String> territories) {
        Properties result = null;
        if (territories != null) {
            for (String territory : territories) {
                result = properties.get(territory.toLowerCase(Locale.US));
                if (result != null) {
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Get the ISO codes of the territory the supplied location is in
     * 
     * @param lon WGS84 longitude of the location
     * @param lat WGS84 latitude of the location
     * @return a list of ISO codes for the territory
     */
    @Nullable
    public List<String> getIsoCodes(double lon, double lat) {
        if (countryBoundaries == null) {
            return null;
        }
        return countryBoundaries.getIds(lon, lat);
    }

    /**
     * Get a list of ISO country codes that this element is in
     * 
     * Currently this uses a centroid of the object which is probably a bad idea
     * 
     * @param e the OsmElement in question
     * @return a List of ISO country codes as Strings, or null if nothing found
     */
    @Nullable
    public List<String> getIsoCodes(@NonNull OsmElement e) {
        try {
            if (countryBoundaries == null) {
                throw new IllegalArgumentException("countryBoundaries null");
            }
            double lon;
            double lat;
            if (e instanceof Node) {
                lon = ((Node) e).getLon() / 1E7D;
                lat = ((Node) e).getLat() / 1E7D;
            } else if (e instanceof Way) {
                double[] coords = Geometry.centroidLonLat((Way) e);
                if (coords.length != 2) {
                    throw new IllegalArgumentException("way " + e.getOsmId() + " no coords");
                }
                lon = coords[0];
                lat = coords[1];
            } else {
                BoundingBox bbox = e.getBounds();
                if (bbox != null) {
                    ViewBox vbox = new ViewBox(bbox);
                    lon = (vbox.getLeft() + (vbox.getRight() - vbox.getLeft()) / 2D) / 1E7D;
                    lat = vbox.getCenterLat();
                } else {
                    throw new IllegalArgumentException("way " + e.getOsmId() + " no coords");
                }
            }
            return countryBoundaries.getIds(lon, lat);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            Log.e(DEBUG_TAG, ex.getMessage() + " " + e);
            return null;
        }
    }

    /**
     * Get the ISO codes for the country and state the location is in
     * 
     * @param lon WGS84 longitude of the location
     * @param lat WGS84 latitude of the location
     * @return a CountryAndStateIso object or null
     */
    @Nullable
    public CountryAndStateIso getCountryAndStateIso(double lon, double lat) {
        List<String> codes = getIsoCodes(lon, lat);
        String country = getCountryIsoCode(codes);
        if (country != null) {
            String state = null;
            for (String code : codes) {
                if (code.startsWith(country) && code.indexOf('-') == country.length()) {
                    String[] temp = code.split("-");
                    if (temp.length == 2) {
                        state = temp[1];
                        break;
                    }
                }
            }
            Log.d(DEBUG_TAG, "Found country " + country + " state " + state);
            return new CountryAndStateIso(country, state);
        }
        Log.e(DEBUG_TAG, "No country found for lon " + lon + " / lat" + lat);
        return null;
    }

    /**
     * Check if an OsmElement is in a territory that uses imperial units
     * 
     * @param e the OsmElement
     * @return true if the territory uses imperial units
     */
    public boolean imperial(@NonNull OsmElement e) {
        if (e instanceof Node) {
            return imperial((Node) e);
        } else if (e instanceof Way) {
            return imperial((Way) e);
        } else {
            return imperial((Relation) e);
        }
    }

    /**
     * Check if a Node is in a territory that uses imperial units
     * 
     * @param n the Node
     * @return true if the territory uses imperial units
     */
    public boolean imperial(@NonNull Node n) {
        return imperial(n.getLon() / 1E7D, n.getLat() / 1E7D);
    }

    /**
     * Check if a Way is in a territory that uses imperial units
     * 
     * Note the check uses the centroid of the way, this might not be the best choice
     * 
     * @param w the Way
     * @return true if the territory uses imperial units
     */
    public boolean imperial(@NonNull Way w) {
        double[] coords = Geometry.centroidLonLat(w);
        return coords.length == 2 && imperial(coords[0], coords[1]);
    }

    /**
     * Check if a Relation is in a territory that uses imperial units
     * 
     * Note the check uses the center of the bounding box, this might not be the best choice
     * 
     * @param r the Relation
     * @return true if the territory uses imperial units
     */
    public boolean imperial(@NonNull Relation r) {
        BoundingBox bbox = r.getBounds();
        if (bbox != null) {
            ViewBox vbox = new ViewBox(bbox);
            return imperial(vbox.getLeft() + (vbox.getRight() - vbox.getLeft()) / 2D, vbox.getCenterLat());
        }
        return false;
    }

    /**
     * Check if a coordinate is in a territory that drives on the left hand sides
     * 
     * @param lon longitude
     * @param lat latitude
     * @return true if the territory that drives on the left hand side
     */
    public boolean driveLeft(double lon, double lat) {
        Properties result = getProperties(lon, lat);
        if (result == null) {
            return false;
        }
        return result.leftHandTraffic;
    }

    /**
     * Check if a Node is in a territory that drives on the left hand side
     * 
     * @param n the Node
     * @return true if the territory that drives on the left hand sides
     */
    public boolean driveLeft(@NonNull Node n) {
        return driveLeft(n.getLon() / 1E7D, n.getLat() / 1E7D);
    }

    /**
     * Check if a Way is in a territory that drives on the left hand side
     * 
     * Note the check uses the centroid of the way, this might not be the best choice
     * 
     * @param w the Way
     * @return true if the territory that drives on the left hand side
     */
    public boolean driveLeft(@NonNull Way w) {
        double[] coords = Geometry.centroidLonLat(w);
        return coords.length == 2 && driveLeft(coords[0], coords[1]);
    }

    /**
     * Utility to return the country code from a list of codes
     * 
     * @param codes a List of ISO codes
     * @return null or the country code
     */
    @Nullable
    public static String getCountryIsoCode(@Nullable List<String> codes) {
        if (codes != null) {
            for (String c : codes) {
                if (c.indexOf('-') == -1) {
                    return c;
                }
            }
        }
        return null;
    }

    /**
     * Get the country code for an element
     * 
     * @param geoContext a GeoContext instance
     * @param element the OsmEeement
     * @return a country code or null
     */
    @Nullable
    public static String getCountryIsoCode(@Nullable GeoContext geoContext, @NonNull OsmElement element) {
        try {
            if (geoContext != null) {
                List<String> isoCodes = geoContext.getIsoCodes(element);
                if (isoCodes != null) {
                    return GeoContext.getCountryIsoCode(isoCodes);
                }
            }
        } catch (IllegalArgumentException iaex) {
            Log.e(DEBUG_TAG, "getIsoCodes " + iaex + " for " + element + " " + element.getOsmId());
        }
        return null;
    }
}
