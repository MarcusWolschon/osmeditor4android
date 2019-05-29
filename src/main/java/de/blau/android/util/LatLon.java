package de.blau.android.util;

public class LatLon {
    private double lat;
    private double lon;

    /**
     * Construct a new container for a coordinate tuple
     * 
     * @param lat the latitude
     * @param lon the longitude
     */
    public LatLon(double lat, double lon) {
        this.setLat(lat);
        this.setLon(lon);
    }

    /**
     * @return the latitude
     */
    public double getLat() {
        return lat;
    }

    /**
     * @param lat the latitude to set
     */
    public void setLat(double lat) {
        this.lat = lat;
    }

    /**
     * @return the longitude
     */
    public double getLon() {
        return lon;
    }

    /**
     * @param lon the longitude to set
     */
    public void setLon(double lon) {
        this.lon = lon;
    }

    @Override
    public String toString() {
        return "lat: " + getLat() + " lon " + getLon();
    }
}
