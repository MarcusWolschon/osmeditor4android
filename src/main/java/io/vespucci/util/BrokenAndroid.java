package io.vespucci.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.stream.JsonReader;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Class to determine certain device specific brokeness
 * 
 * @author simon
 *
 */
public class BrokenAndroid {

    private static final String DEBUG_TAG = BrokenAndroid.class.getSimpleName().substring(0, Math.min(23, BrokenAndroid.class.getSimpleName().length()));

    private static final String FULLSCREEN = "fullscreen";

    public class Properties {
        boolean fullScreen = false;
    }

    private final Map<String, Properties> properties;

    /**
     * Implicit assumption that the data will be short and that it is OK to read in synchronously which may not be true
     * 
     * @param context Android Context
     */
    public BrokenAndroid(@NonNull Context context) {
        Log.d(DEBUG_TAG, "Initalizing for " + Build.MANUFACTURER + "|" + Build.HARDWARE);
        AssetManager assetManager = context.getAssets();
        properties = getPropertiesMap(assetManager, "devices.json");
    }

    /**
     * Check if full screen mode is broken on this device
     * 
     * @return true if broken
     */
    public boolean isFullScreenBroken() {
        Properties prop = getProperties(Build.MANUFACTURER, Build.HARDWARE);
        return prop != null && prop.fullScreen;
    }

    /**
     * Read a GeoJson file from assets
     * 
     * @param assetManager an AssetManager
     * @param fileName the name of the file
     * @return a GeoJson FeatureCollection
     */
    @Nullable
    private Map<String, Properties> getPropertiesMap(@NonNull AssetManager assetManager, @NonNull String fileName) {
        Map<String, Properties> result = new HashMap<>();
        try (InputStream is = assetManager.open(fileName); JsonReader reader = new JsonReader(new InputStreamReader(is, "UTF-8"))) {
            reader.beginObject();
            while (reader.hasNext()) {
                String manufacturerDevice = reader.nextName();
                Properties prop = new Properties();
                reader.beginObject();
                while (reader.hasNext()) {
                    String propName = reader.nextName();
                    if (FULLSCREEN.equals(propName)) {
                        prop.fullScreen = reader.nextBoolean();
                    } else {
                        Log.e(DEBUG_TAG, "Unknown property " + propName);
                        reader.skipValue();
                    }
                }
                reader.endObject();
                result.put(manufacturerDevice, prop);
            }
            reader.endObject();
            Log.d(DEBUG_TAG, "Found " + result.size() + " entries.");
        } catch (IOException | NumberFormatException e) {
            Log.d(DEBUG_TAG, "Opening/reading " + fileName + " " + e.getMessage());
        }
        return result;
    }

    /**
     * Get the properties for a specific device or all devices of the manufacturer
     * 
     * @param manufacturer the manufacturer
     * @param device the device name
     * @return a Properties element or null if not found
     */
    @Nullable
    public Properties getProperties(@NonNull String manufacturer, @NonNull String device) {
        Properties allDevices = properties.get(manufacturer + "|*"); // wildcard entry for manufacturer
        Properties justThisDevice = properties.get(manufacturer + "|" + device);
        return justThisDevice != null ? justThisDevice : allDevices;
    }
}
