package de.blau.android.osm;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

import org.xmlpull.v1.XmlSerializer;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
     * Scaling between floating point coordinates and internal representation as an int
     */
    public static final int COORDINATE_SCALE = 7;

    /**
     * Constructor. Call it solely in {@link OsmElementFactory}!
     * 
     * @param osmId the OSM-ID. When not yet transmitted to the API it is negative.
     * @param osmVersion the version of the element
     * @param timestamp seconds since the epoch
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

    /**
     * Set the latitude
     * 
     * @param lat latitude in WGS84*1E7
     */
    void setLat(final int lat) {
        this.lat = lat;
    }

    /**
     * Set the longitude
     * 
     * @param lon longitude in WGS84*1E7
     */
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
        toXml(s, changeSetId, false);
    }

    @Override
    public void toJosmXml(final XmlSerializer s) throws IllegalArgumentException, IllegalStateException, IOException {
        toXml(s, null, true);
    }

    /**
     * Generate XML format OSM files
     * 
     * @param s the XML serializer
     * @param changeSetId the current changeset id or null
     * @param josm if true use JOSM format
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     * @throws IOException
     */
    private void toXml(@NonNull final XmlSerializer s, @Nullable Long changeSetId, boolean josm)
            throws IllegalArgumentException, IllegalStateException, IOException {
        s.startTag("", NAME);
        attributesToXml(s, changeSetId, josm);
        s.attribute("", "lat", BigDecimal.valueOf(lat).scaleByPowerOfTen(-COORDINATE_SCALE).stripTrailingZeros().toPlainString());
        s.attribute("", "lon", BigDecimal.valueOf(lon).scaleByPowerOfTen(-COORDINATE_SCALE).stripTrailingZeros().toPlainString());
        tagsToXml(s);
        s.endTag("", NAME);
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
        return Math.hypot((double) location[0] - getLat(), (double) location[1] - getLon());
    }

    @Override
    public BoundingBox getBounds() {
        return new BoundingBox(lon, lat);
    }

    @Override
    protected int validate(Validator validator) {
        return validator.validate(this);
    }

    @Override
    <T extends OsmElement> void updateFrom(T e) {
        if (!(e instanceof Node)) {
            throw new IllegalArgumentException("e is not a Node");
        }
        if (e.getOsmId() != getOsmId()) {
            throw new IllegalArgumentException("Different ids " + e.getOsmId() + " != " + getOsmId());
        }
        setTags(e.getTags());
        setState(e.getState());
        setLon(((Node) e).getLon());
        setLat(((Node) e).getLat());
    }
}
