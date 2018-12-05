package de.blau.android.util;

import java.io.Serializable;
import java.util.Locale;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Container for data from a geo url
 * 
 * @author simon
 *
 */
public class GeoUrlData implements Serializable {
    private static final long serialVersionUID = 3L;

    private static final String DEBUG_TAG = "GeoUrlData";

    private double lat  = -Double.MAX_VALUE;
    private double lon  = -Double.MAX_VALUE;
    private int    zoom = -1;

    /**
     * @return the lat
     */
    public double getLat() {
        return lat;
    }

    /**
     * @return latitude in WGS*1E7 coords
     */
    public int getLatE7() {
        return (int) (lat * 1E7D);
    }

    /**
     * @param lat the lat to set
     */
    public void setLat(double lat) {
        this.lat = lat;
    }

    /**
     * @return the lon
     */
    public double getLon() {
        return lon;
    }

    /**
     * @return longitude in WGS*1E7 coords
     */
    public int getLonE7() {
        return (int) (lon * 1E7D);
    }

    /**
     * @param lon the lon to set
     */
    public void setLon(double lon) {
        this.lon = lon;
    }

    /**
     * @return the zoom
     */
    public int getZoom() {
        return zoom;
    }

    /**
     * @param zoom the zoom to set
     */
    public void setZoom(int zoom) {
        this.zoom = zoom;
    }

    /**
     * Check if we have a valid zoom value
     * 
     * @return true if zoom is present
     */
    public boolean hasZoom() {
        return zoom >= 0;
    }

    @Override
    public String toString() {
        return "geo:" + lat + "," + lon + (hasZoom() ? "?z=" + zoom : "");
    }

    /**
     * Get a GeoUrlData object from an Intent
     * 
     * @param schemeSpecificPart the scheme specific part of the url
     * @return a GeoUrlData object or null
     */
    @Nullable
    public static GeoUrlData parse(@NonNull String schemeSpecificPart) {
        GeoUrlData geoData = new GeoUrlData();
        String[] query = schemeSpecificPart.split("[\\?\\&]"); // used by osmand likely not standard conform
        if (query != null && query.length >= 1) {
            String[] params = query[0].split(";");
            if (params != null && params.length >= 1) {
                String[] coords = params[0].split(",");
                boolean wgs84 = true; // for now the only supported datum
                if (params.length > 1) {
                    for (String p : params) {
                        if (p.toLowerCase(Locale.US).matches("crs=.*")) {
                            wgs84 = p.toLowerCase(Locale.US).matches("crs=wgs84");
                            Log.d(DEBUG_TAG, "crs found " + p + ", is wgs84 is " + wgs84);
                        }
                    }
                }
                if (coords != null && coords.length >= 2 && wgs84) {
                    try {
                        double lat = Double.parseDouble(coords[0]);
                        double lon = Double.parseDouble(coords[1]);
                        if (lon >= -180 && lon <= 180 && lat >= -GeoMath.MAX_LAT && lat <= GeoMath.MAX_LAT) {

                            geoData.setLat(lat);
                            geoData.setLon(lon);

                        }
                    } catch (NumberFormatException e) {
                        Log.d(DEBUG_TAG, "Coordinates " + coords[0] + "/" + coords[1] + " not parseable");
                    }
                }
            }
            if (query.length > 1) {
                for (int i = 1; i < query.length; i++) {
                    params = query[i].split("=", 2);
                    if (params.length == 2 && "z".equals(params[0])) {
                        geoData.setZoom(Integer.parseInt(params[1]));
                    }
                }
            }
        }
        return geoData.isValid() ? geoData : null;
    }

    /**
     * Check if received both a latitude and a longitude
     * 
     * @return true if the object contains both a latitude and a longitude
     */
    boolean isValid() {
        return lat > -Double.MAX_VALUE && lon > -Double.MAX_VALUE;
    }
}
