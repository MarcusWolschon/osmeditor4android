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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import de.blau.android.Logic;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Way;
import de.westnordost.countryboundaries.CountryBoundaries;

/**
 * Class to determine certain general properties of the environment we are mapping in from the geographic location
 * 
 * @author simon
 *
 */
public class GeoContext {

    private static final String     DEBUG_TAG = "GeoContext";
    private final CountryBoundaries countryBoundaries;

    private class Properties {
        boolean imperialUnits   = false;
        boolean leftHandTraffic = false;
        int[]   speedLimits;
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
        countryBoundaries = getCountryBoundariesFromAssets(assetManager, "boundaries.ser");
        properties = getPropertiesMap(assetManager, "geocontext.json");
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
            e.printStackTrace();
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
    @Nullable
    Map<String, Properties> getPropertiesMap(@NonNull AssetManager assetManager, @NonNull String fileName) {
        Map<String, Properties> result = new HashMap<>();
        InputStream is = null;
        JsonReader reader = null;
        try {
            is = assetManager.open(fileName);
            reader = new JsonReader(new InputStreamReader(is, "UTF-8"));
            try {
                reader.beginObject();
                while (reader.hasNext()) {
                    String territory = reader.nextName();
                    Properties prop = new Properties();
                    reader.beginObject();
                    while (reader.hasNext()) {
                        String propName = reader.nextName();
                        switch (propName) {
                        case "distance":
                            prop.imperialUnits = "imperial".equals(reader.nextString());
                            break;
                        case "left-hand-traffic":
                            prop.leftHandTraffic = reader.nextBoolean();
                            break;
                        case "speed-limits":
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
            } catch (IOException e) {
                Log.d(DEBUG_TAG, "Reading " + fileName + " " + e.getMessage());
            }
        } catch (IOException e) {
            Log.d(DEBUG_TAG, "Opening " + fileName + " " + e.getMessage());
        } finally {
            SavingHelper.close(reader);
            SavingHelper.close(is);
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
    public List<String> getIsoCodes(double lon, double lat) {
        if (countryBoundaries == null) {
            return null;
        }
        List<String> territories = countryBoundaries.getIds(lon, lat);
        return territories;
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
            return false; // FIXME handle relations
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
        double[] coords = Logic.centroidLonLat(w);
        return imperial(coords[0], coords[1]);
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
        double[] coords = Logic.centroidLonLat(w);
        return driveLeft(coords[0], coords[1]);
    }
}
