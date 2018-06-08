package de.blau.android.osm;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Map;

import org.xmlpull.v1.XmlSerializer;

import de.blau.android.util.rtree.BoundedObject;
import de.blau.android.validation.Validator;

/**
 * Node represents a Node in the OSM-data-structure. It stores the lat/lon-pair and provides some package-internal
 * manipulating-methods.
 * 
 * @author mb
 */
public class Node extends OsmElement implements GeoPoint, BoundedObject {
    /**
     * 
     */
    private static final long serialVersionUID = 152395243648348266L;

    /**
     * WGS84 decimal Latitude-Coordinate times 1E7.
     */
    int lat;

    /**
     * WGS84 decimal Longitude-Coordinate times 1E7.
     */
    int lon;

    /**
     * It's name in the OSM-XML-scheme.
     */
    public static final String NAME = "node";

    /**
     * Constructor. Call it solely in {@link OsmElementFactory}!
     * 
     * @param osmId the OSM-ID. When not yet transmitted to the API it is negative.
     * @param osmVersion the version of the element
     * @param status see {@link OsmElement#state}
     * @param lat WGS84 decimal Latitude-Coordinate times 1E7.
     * @param lon WGS84 decimal Longitude-Coordinate times 1E7.
     */
    Node(final long osmId, final long osmVersion, final long timestamp, final byte status, final int lat, final int lon) {
        super(osmId, osmVersion, timestamp, status);
        this.lat = lat;
        this.lon = lon;
    }

    @Override
    public int getLat() {
        return lat;
    }

    @Override
    public int getLon() {
        return lon;
    }

    void setLat(final int lat) {
        this.lat = lat;
    }

    void setLon(final int lon) {
        this.lon = lon;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return super.toString() + "\tlat: " + lat + "; lon: " + lon;
    }

    @Override
    public void toXml(final XmlSerializer s, final Long changeSetId) throws IllegalArgumentException, IllegalStateException, IOException {
        s.startTag("", "node");
        s.attribute("", "id", Long.toString(osmId));
        if (changeSetId != null)
            s.attribute("", "changeset", Long.toString(changeSetId));
        s.attribute("", "version", Long.toString(osmVersion));
        if (timestamp >= 0) {
            s.attribute("", "timestamp", new SimpleDateFormat(OsmParser.TIMESTAMP_FORMAT).format(getTimestamp() * 1000));
        }
        s.attribute("", "lat", Double.toString((lat / 1E7)));
        s.attribute("", "lon", Double.toString((lon / 1E7)));
        tagsToXml(s);
        s.endTag("", "node");
    }

    @Override
    public void toJosmXml(final XmlSerializer s) throws IllegalArgumentException, IllegalStateException, IOException {
        s.startTag("", "node");
        s.attribute("", "id", Long.toString(osmId));
        if (state == OsmElement.STATE_DELETED) {
            s.attribute("", "action", "delete");
        } else if (state == OsmElement.STATE_CREATED || state == OsmElement.STATE_MODIFIED) {
            s.attribute("", "action", "modify");
        }
        s.attribute("", "version", Long.toString(osmVersion));
        if (timestamp >= 0) {
            s.attribute("", "timestamp", new SimpleDateFormat(OsmParser.TIMESTAMP_FORMAT).format(getTimestamp() * 1000));
        }
        s.attribute("", "visible", "true");
        s.attribute("", "lat", Double.toString((lat / 1E7)));
        s.attribute("", "lon", Double.toString((lon / 1E7)));
        tagsToXml(s);
        s.endTag("", "node");
    }

    @Override
    public ElementType getType() {
        return ElementType.NODE;
    }

    @Override
    public ElementType getType(Map<String, String> tags) {
        return getType();
    }

    /**
     * Get the "distance" of this Node from the location
     * 
     * @param location a coordinate tupel in WGS84*1E7 degrees
     * @return the planar geom distance in degrees
     */
    public double getDistance(final int[] location) {
        if (location == null) {
            return Double.MAX_VALUE;
        }
        return Math.hypot((double)location[0] - getLat(), (double)location[1] - getLon());
    }

    @Override
    public BoundingBox getBounds() {
        return new BoundingBox(lon, lat);
    }

    @Override
    protected int validate(Validator validator) {
        return validator.validate(this);
    }
}
