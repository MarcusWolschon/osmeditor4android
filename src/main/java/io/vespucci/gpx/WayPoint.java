package io.vespucci.gpx;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Locale;

import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.R;

public class WayPoint extends TrackPoint {
    /**
     * 
     */
    private static final long serialVersionUID = 2L;

    static final String TYPE_ELEMENT = "type";
    static final String DESC_ELEMENT = "desc";
    static final String NAME_ELEMENT = "name";
    static final String WPT_ELEMENT  = "wpt";
    static final String SYM_ELEMENT  = "sym";
    static final String LINK_ELEMENT = "link";

    private String     name;
    private String     description;
    private String     type;
    private String     symbol;
    private List<Link> links;

    public static class Link implements Serializable {

        private static final long serialVersionUID = 1L;

        static final String TEXT_ELEMENT = "text";
        static final String HREF_ATTR    = "href";

        private String url;
        private String description;

        /**
         * @return the description
         */
        public String getDescription() {
            return description;
        }

        /**
         * @param description the description to set
         */
        public void setDescription(String description) {
            this.description = description;
        }

        /**
         * @return the url
         */
        public String getUrl() {
            return url;
        }

        /**
         * @param url the url to set
         */
        public void setUrl(String url) {
            this.url = url;
        }
    }

    /**
     * Construct a new WayPoint
     * 
     * @param latitude the latitude (WGS84)
     * @param longitude the longitude (WSG84)
     * @param altitude altitude in meters
     * @param time time (ms since the epoch)
     */
    public WayPoint(double latitude, double longitude, double altitude, long time) {
        super((byte) 0, latitude, longitude, altitude, time);
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
        if (links != null) {
            for (Link link : links) {
                serializer.startTag(null, LINK_ELEMENT);
                if (link.getUrl() != null) {
                    serializer.attribute(null, Link.HREF_ATTR, link.getUrl());
                }
                if (link.getDescription() != null) {
                    serializer.startTag(null, Link.TEXT_ELEMENT).text(link.getDescription()).endTag(null, Link.TEXT_ELEMENT);
                }
                serializer.endTag(null, LINK_ELEMENT);
            }
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
     * Set an optional symbol value
     * 
     * @param symbol the symbol value
     */
    public void setSymbol(@Nullable String symbol) {
        this.symbol = symbol;
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
     * Set optional name value
     * 
     * @param name the name
     */
    public void setName(@Nullable String name) {
        this.name = name;
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
     * Set an optional description
     * 
     * @param description the description
     */
    public void setDescription(@Nullable String description) {
        this.description = description;
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
     * Set an optional type
     * 
     * @param type the type
     */
    public void setType(@Nullable String type) {
        this.type = type;
    }

    /**
     * @return the links
     */
    public List<Link> getLinks() {
        return links;
    }

    /**
     * @param links the links to set
     */
    public void setLinks(@Nullable List<Link> links) {
        this.links = links;
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
