package io.vespucci.gpx;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Locale;

import org.xmlpull.v1.XmlSerializer;

import android.location.Location;
import androidx.annotation.NonNull;
import io.vespucci.osm.GeoPoint.InterruptibleGeoPoint;
import io.vespucci.services.util.ExtendedLocation;

/**
 * This is a class to store location points and provide storing/serialization for them. Everything considered less
 * relevant is commented out to save space. If you chose that this should be included in the GPX, uncomment it,
 * increment {@link #FORMAT_VERSION}, set the correct {@link #RECORD_SIZE} and rewrite
 * {@link #fromStream(DataInputStream)}, {@link #toStream(DataOutputStream)} and {@link #getGPXString()}.
 * 
 * @author Jan
 */
public class TrackPoint implements InterruptibleGeoPoint, Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    static final String ELE_ELEMENT   = "ele";
    static final String TIME_ELEMENT  = "time";
    static final String TRKPT_ELEMENT = "trkpt";
    static final String LON_ATTR      = "lon";
    static final String LAT_ATTR      = "lat";

    public static final int FORMAT_VERSION = 2;
    public static final int RECORD_SIZE    = 1 + 4 * 8;

    public static final byte FLAG_NEWSEGMENT = 1;
    public final byte        flags;
    public final double      latitude;
    public final double      longitude;
    public final double      altitude;
    public final long        time;
    // public final Float accuracy;
    // public final Float bearing;
    // public final Float speed;

    /**
     * Construct a TrackPoint from a location
     * 
     * @param loc the Location we want to use
     * @param isNewSegment true id we are starting a new segment
     */
    public TrackPoint(@NonNull Location loc, boolean isNewSegment) {
        flags = encodeFlags(isNewSegment);
        latitude = loc.getLatitude();
        longitude = loc.getLongitude();
        if (loc instanceof ExtendedLocation) {
            ExtendedLocation eLoc = (ExtendedLocation) loc;
            if (eLoc.hasBarometricHeight() && eLoc.useBarometricHeight()) {
                altitude = eLoc.getBarometricHeight();
            } else if (eLoc.hasGeoidHeight()) {
                altitude = eLoc.getGeoidHeight();
            } else {
                altitude = Double.NaN;
            }
        } else {
            altitude = loc.hasAltitude() ? loc.getAltitude() : Double.NaN;
        }
        time = loc.getTime();
        // accuracy = original.hasAccuracy() ? original.getAccuracy() : null;
        // bearing = original.hasBearing() ? original.getBearing() : null;
        // speed = original.hasSpeed() ? original.getSpeed() : null;
    }

    /**
     * Construct a TrackPoint
     * 
     * @param flags flags (new segment)
     * @param latitude the latitude (WGS84)
     * @param longitude the longitude (WSG84)
     * @param altitude altitude in meters
     * @param time time (ms since the epoch)
     */
    public TrackPoint(byte flags, double latitude, double longitude, double altitude, long time) {
        this.flags = flags;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.time = time;
    }

    /**
     * Construct a TrackPoint
     * 
     * @param flags flags (new segment)
     * @param latitude the latitude (WGS84)
     * @param longitude the longitude (WSG84)
     * @param time time (ms since the epoch)
     */
    public TrackPoint(byte flags, double latitude, double longitude, long time) {
        this(flags, latitude, longitude, Double.NaN, time);
    }

    /**
     * Loads a track point from a {@link DataInputStream}
     * 
     * @param stream the stream from which to load
     * @return the loaded data point
     * @throws IOException if anything goes wrong
     */
    public static TrackPoint fromStream(@NonNull DataInputStream stream) throws IOException {
        return new TrackPoint(stream.readByte(), // flags
                stream.readDouble(), // lat
                stream.readDouble(), // lon
                stream.readDouble(), // alt
                stream.readLong() // time
        );
    }

    /**
     * Writes the current track point to the data output stream
     * 
     * @param stream target stream
     * @throws IOException if writing to the stream fails
     */
    public void toStream(@NonNull DataOutputStream stream) throws IOException {
        stream.writeByte(flags);
        stream.writeDouble(latitude);
        stream.writeDouble(longitude);
        stream.writeDouble(altitude);
        stream.writeLong(time);
    }

    @Override
    public int getLat() {
        return (int) (latitude * 1E7);
    }

    @Override
    public int getLon() {
        return (int) (longitude * 1E7);
    }

    /**
     * Get the latitude
     * 
     * @return the latitude in WGS84 coordinates
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Get the longitude
     * 
     * @return the longitude in WGS84 coordinates
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Return the time as milliseconds since the epoch
     * 
     * @return time in milliseconds
     */
    public long getTime() {
        return time;
    }

    /**
     * Check if this object has an altitude value
     * 
     * @return true if we have a valid altitude
     */
    public boolean hasAltitude() {
        return !Double.isNaN(altitude);
    }
    // public boolean hasAccuracy() { return accuracy != null; }
    // public boolean hasBearing() { return bearing != null; }
    // public boolean hasSpeed() { return speed != null; }

    /**
     * Get the alitude
     * 
     * @return the altitude
     */
    public double getAltitude() {
        return !Double.isNaN(altitude) ? altitude : 0d;
    }
    // public float getAccuracy() { return accuracy != null ? accuracy : 0f; }
    // public float getBearing() { return bearing != null ? bearing : 0f; }
    // public float getSpeed() { return speed != null ? speed : 0f; }

    /**
     * Encode flag values in to a byte
     * 
     * @param isNewSegment if true this starts a new segment
     * @return the byte value holding the flags
     */
    private byte encodeFlags(boolean isNewSegment) {
        byte result = 0;
        if (isNewSegment) {
            result += FLAG_NEWSEGMENT;
        }
        return result;
    }

    /**
     * Check if this object marks the beginning of a new segment
     * 
     * @return true if a new segment starts here
     */
    public boolean isNewSegment() {
        return (flags & FLAG_NEWSEGMENT) > 0;
    }

    @Override
    public boolean isInterrupted() {
        return isNewSegment();
    }

    /**
     * Fill a Location object with our values
     * 
     * @param loc the Location we want to set the vaules on
     */
    public void toLocation(@NonNull Location loc) {
        loc.reset();
        loc.setLongitude(longitude);
        loc.setLatitude(latitude);
        loc.setAltitude(altitude);
        loc.setTime(time);
    }

    /**
     * Adds a GPX trkpt (track point) tag to the given serializer (synchronized due to use of calendarInstance)
     * 
     * @param serializer the xml serializer to use for output
     * @param gtf class providing a formatter to GPX time data
     * @throws IOException if writing to the Serializer goes wrong
     */
    public synchronized void toXml(@NonNull XmlSerializer serializer, @NonNull GpxTimeFormater gtf) throws IOException {
        serializer.startTag(null, TRKPT_ELEMENT);
        serializer.attribute(null, LAT_ATTR, String.format(Locale.US, "%.8f", latitude));
        serializer.attribute(null, LON_ATTR, String.format(Locale.US, "%.8f", longitude));
        if (hasAltitude()) {
            serializer.startTag(null, ELE_ELEMENT).text(String.format(Locale.US, "%.3f", altitude)).endTag(null, ELE_ELEMENT);
        }
        serializer.startTag(null, TIME_ELEMENT).text(gtf.format(time)).endTag(null, TIME_ELEMENT);
        serializer.endTag(null, TRKPT_ELEMENT);
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%f, %f", latitude, longitude);
    }
}