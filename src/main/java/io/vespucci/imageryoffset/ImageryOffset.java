package io.vespucci.imageryoffset;

import java.util.Locale;

import android.net.Uri;

/**
 * Object to hold the output from the imagery DB see https://wiki.openstreetmap.org/wiki/Imagery_Offset_Database/API
 * 
 * @author Simon Poole
 *
 */
public class ImageryOffset {

    private static final String STORE             = "store";
    static final String         IMLON_PARAM       = "imlon";
    static final String         IMLAT_PARAM       = "imlat";
    static final String         IMAGERY_PARAM     = "imagery";
    static final String         DESCRIPTION_PARAM = "description";
    static final String         AUTHOR_PARAM      = "author";
    static final String         LAT_PARAM         = "lat";
    static final String         LON_PARAM         = "lon";

    @SuppressWarnings("unused")
    long            id;
    String          imageryId;
    private double  lon        = 0;
    private double  lat        = 0;
    private int     minZoom    = 0;
    private int     maxZoom    = 18;
    private double  imageryLat = 0;
    private double  imageryLon = 0;
    String          author;
    String          description;
    String          date;
    DeprecationNote deprecated = null;

    /**
     * Date pattern used to describe when the imagery offset was created.
     */
    static final String DATE_PATTERN_IMAGERY_OFFSET_CREATED_AT = "yyyy-MM-dd";

    /**
     * Create an URL for saving to the imagery offset DB
     * 
     * @param offsetServerUri the base url
     * @return an url with the parameters filled in
     */
    public String toSaveUrl(final Uri offsetServerUri) {
        Uri uriBuilder = offsetServerUri.buildUpon().appendPath(STORE).appendQueryParameter(LAT_PARAM, String.format(Locale.US, "%.7f", lat))
                .appendQueryParameter(LON_PARAM, String.format(Locale.US, "%.7f", lon)).appendQueryParameter(AUTHOR_PARAM, author)
                .appendQueryParameter(DESCRIPTION_PARAM, description).appendQueryParameter(IMAGERY_PARAM, imageryId)
                .appendQueryParameter(IMLAT_PARAM, String.format(Locale.US, "%.7f", getImageryLat()))
                .appendQueryParameter(IMLON_PARAM, String.format(Locale.US, "%.7f", getImageryLon())).build();
        return uriBuilder.toString();
    }

    /**
     * @return the lon
     */
    public double getLon() {
        return lon;
    }

    /**
     * @param lon the lon to set
     */
    public void setLon(double lon) {
        this.lon = lon;
    }

    /**
     * @return the lat
     */
    public double getLat() {
        return lat;
    }

    /**
     * @param lat the lat to set
     */
    public void setLat(double lat) {
        this.lat = lat;
    }

    /**
     * @return the imageryLon
     */
    public double getImageryLon() {
        return imageryLon;
    }

    /**
     * @param imageryLon the imageryLon to set
     */
    public void setImageryLon(double imageryLon) {
        this.imageryLon = imageryLon;
    }

    /**
     * @return the imageryLat
     */
    public double getImageryLat() {
        return imageryLat;
    }

    /**
     * @param imageryLat the imageryLat to set
     */
    public void setImageryLat(double imageryLat) {
        this.imageryLat = imageryLat;
    }

    /**
     * @return the minZoom
     */
    public int getMinZoom() {
        return minZoom;
    }

    /**
     * @param minZoom the minZoom to set
     */
    public void setMinZoom(int minZoom) {
        this.minZoom = minZoom;
    }

    /**
     * @return the maxZoom
     */
    public int getMaxZoom() {
        return maxZoom;
    }

    /**
     * @param maxZoom the maxZoom to set
     */
    public void setMaxZoom(int maxZoom) {
        this.maxZoom = maxZoom;
    }

    /**
     * Object to hold the output from the imagery DB see https://wiki.openstreetmap.org/wiki/Imagery_Offset_Database/API
     * Currently we don't actually display the contents anywhere
     * 
     * @author Simon Poole
     *
     */
    static class DeprecationNote {
        @SuppressWarnings("unused")
        String author;
        @SuppressWarnings("unused")
        String date;
        @SuppressWarnings("unused")
        String reason;
    }

    @Override
    public String toString() {
        return imageryId + " min " + minZoom + " max " + maxZoom + " lon " + (lon - imageryLon) + " lat " + (lat - imageryLat);
    }
}
