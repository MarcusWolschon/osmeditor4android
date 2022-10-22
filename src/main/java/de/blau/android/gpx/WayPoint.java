package de.blau.android.gpx;

import java.io.IOException;
import java.util.Locale;

import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.R;

public class WayPoint extends TrackPoint {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    static final String TYPE_ELEMENT = "type";
    static final String DESC_ELEMENT = "desc";
    static final String NAME_ELEMENT = "name";
    static final String WPT_ELEMENT  = "wpt";
    static final String SYM_ELEMENT  = "sym";

    private String name;
    private String description;
    private String type;
    private String symbol;

    /**
     * Construct a new WayPoint
     * 
     * @param latitude the latitude (WGS84)
     * @param longitude the longitude (WSG84)
     * @param altitude altitude in meters
     * @param time time (ms since the epoch)
     * @param name optional name value
     * @param description optional description
     * @param type optional type value
     * @param symbol optional symbol
     */
    public WayPoint(double latitude, double longitude, double altitude, long time, @Nullable String name, @Nullable String description, @Nullable String type,
            @Nullable String symbol) {
        super((byte) 0, latitude, longitude, altitude, time);
        this.name = name;
        this.description = description;
        this.type = type;
        this.symbol = symbol;
    }

    @Override
    public synchronized void toXml(XmlSerializer serializer, GpxTimeFormater gtf) throws IOException {
        serializer.startTag(null, WPT_ELEMENT);
        serializer.attribute(null, LAT_ATTR, String.format(Locale.US, "%f", latitude));
        serializer.attribute(null, LON_ATTR, String.format(Locale.US, "%f", longitude));
        if (hasAltitude()) {
            serializer.startTag(null, ELE_ELEMENT).text(String.format(Locale.US, "%f", altitude)).endTag(null, ELE_ELEMENT);
        }
        serializer.startTag(null, TIME_ELEMENT).text(gtf.format(time)).endTag(null, TIME_ELEMENT);
        if (name != null) {
            serializer.startTag(null, NAME_ELEMENT).text(name).endTag(null, NAME_ELEMENT);
        }
        if (description != null) {
            serializer.startTag(null, DESC_ELEMENT).text(description).endTag(null, DESC_ELEMENT);
        }
        if (type != null) {
            serializer.startTag(null, TYPE_ELEMENT).text(type).endTag(null, TYPE_ELEMENT);
        }
        if (symbol != null) {
            serializer.startTag(null, SYM_ELEMENT).text(symbol).endTag(null, SYM_ELEMENT);
        }
        serializer.endTag(null, WPT_ELEMENT);
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%f, %f", latitude, longitude);
    }

    @Override
    public boolean isInterrupted() {
        return isNewSegment();
    }

    /**
     * Get the symbol value
     * 
     * @return the symbol value
     */
    @Nullable
    public String getSymbol() {
        return symbol;
    }

    /**
     * Get the name if any
     * 
     * @return the name
     */
    @Nullable
    public String getName() {
        return name;
    }

    /**
     * Get a description if any
     * 
     * @return the description
     */
    @Nullable
    public String getDescription() {
        return description;
    }

    /**
     * Get a String suitable for labeling the point
     * 
     * @return a label
     */
    @Nullable
    public String getLabel() {
        return name != null ? name : description;
    }

    /**
     * @return the type
     */
    @Nullable
    public String getType() {
        return type;
    }

    /**
     * Generate a short description suitable for a menu
     * 
     * @param ctx Android Context
     * @return the description
     */
    public String getShortDescription(@NonNull Context ctx) {
        if (getName() != null) {
            return ctx.getString(R.string.waypoint_description, getName());
        }
        if (getType() != null) {
            return ctx.getString(R.string.waypoint_description, getType());
        }
        return ctx.getString(R.string.waypoint_description, toString());
    }
}
