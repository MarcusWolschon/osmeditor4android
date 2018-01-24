package de.blau.android.imageryoffset;

import java.util.Locale;

import android.net.Uri;

/**
 * Object to hold the output from the imagery DB see https://wiki.openstreetmap.org/wiki/Imagery_Offset_Database/API
 * 
 * @author Simon Poole
 *
 */
public class ImageryOffset {
    @SuppressWarnings("unused")
    long            id;
    String          imageryId;
    private double   lon        = 0;
    private double   lat        = 0;
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
        Uri uriBuilder = offsetServerUri.buildUpon().appendPath("store").appendQueryParameter("lat", String.format(Locale.US, "%.7f", lat))
                .appendQueryParameter("lon", String.format(Locale.US, "%.7f", lon)).appendQueryParameter("author", author)
                .appendQueryParameter("description", description).appendQueryParameter("imagery", imageryId)
                .appendQueryParameter("imlat", String.format(Locale.US, "%.7f", getImageryLat()))
                .appendQueryParameter("imlon", String.format(Locale.US, "%.7f", getImageryLon())).build();
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
