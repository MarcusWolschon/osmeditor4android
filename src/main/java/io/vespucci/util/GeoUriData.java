package io.vespucci.util;

import static io.vespucci.contract.Constants.LOG_TAG_LEN;

import java.io.Serializable;
import java.util.Locale;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.contract.Schemes;

/**
 * Container for data from a geo url
 * 
 * @author simon
 *
 */
public class GeoUriData implements Serializable {

    private static final long serialVersionUID = 3L;

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, GeoUriData.class.getSimpleName().length());
    private static final String DEBUG_TAG = GeoUriData.class.getSimpleName().substring(0, TAG_LEN);

    private static final String ZOOM_PARAMETER = "z";

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
     * Get the coordinates as a LatLon object
     * 
     * @return a LatLon object
     */
    public LatLon getLatLon() {
        return new LatLon(lat, lon);
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
        return Schemes.GEO + ":" + lat + "," + lon + (hasZoom() ? "?z=" + zoom : "");
    }

    /**
     * Get a GeoUrlData object from an Intent
     * 
     * @param schemeSpecificPart the scheme specific part of the url
     * @return a GeoUrlData object or null
     */
    @Nullable
    public static GeoUriData parse(@NonNull String schemeSpecificPart) {
        GeoUriData geoData = new GeoUriData();
        String[] query = schemeSpecificPart.split("[\\?\\&]"); // used by osmand likely not standard conform
        if (query == null || query.length == 0) {
            Log.e(DEBUG_TAG, "no query found in " + schemeSpecificPart);
            return null;
        }
        String[] params = query[0].split(";");
        if (params != null && params.length >= 1) {
            String[] coords = params[0].split(",");
            boolean wgs84 = true; // for now the only supported datum
            if (params.length > 1) {
                for (String p : params) {
                    if (p.toLowerCase(Locale.US).matches("crs=.*")) {
                        wgs84 = p.toLowerCase(Locale.US).matches("crs=wgs84");
                        Log.i(DEBUG_TAG, "crs found " + p + ", is wgs84 is " + wgs84);
                    }
                }
            }
            if (coords != null && coords.length >= 2 && wgs84) {
                try {
                    double lat = Double.parseDouble(coords[0]);
                    double lon = Double.parseDouble(coords[1]);
                    if (GeoMath.coordinatesInCompatibleRange(lon, lat)) {
                        geoData.setLat(lat);
                        geoData.setLon(lon);
                    }
                } catch (NumberFormatException e) {
                    Log.e(DEBUG_TAG, "Coordinates " + coords[0] + "/" + coords[1] + " not parseable");
                }
            }
        }
        if (query.length > 1) {
            for (int i = 1; i < query.length; i++) {
                params = query[i].split("=", 2);
                if (params.length == 2 && ZOOM_PARAMETER.equals(params[0])) {
                    try {
                        geoData.setZoom(Integer.parseInt(params[1]));
                    } catch (NumberFormatException e) {
                        Log.e(DEBUG_TAG, "Illegal zoom value " + params[1] + " trying to recover");
                        try {
                            geoData.setZoom((int) Math.round(Double.parseDouble(params[1])));
                        } catch (NumberFormatException e1) {
                            Log.e(DEBUG_TAG, "Illegal zoom value parsing as double failed");
                        }
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
        return lat >= -GeoMath.MAX_LAT && lat <= GeoMath.MAX_LAT && lon >= -GeoMath.MAX_LON && lon <= GeoMath.MAX_LON;
    }
}
