package de.blau.android.osm;

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

/**
 * Node represents a Node in the OSM-data-structure. It stores the lat/lon-pair and provides some package-internal
 * manipulating-methods.
 * 
 * @author mb
 */
public class Node extends OsmElement implements GeoPoint {
	/**
	 * 
	 */
	private static final long serialVersionUID = 152395243648348266L;

	/**
	 * WGS84 decimal Latitude-Coordinate times 1E7.
	 */
	protected int lat;

	/**
	 * WGS84 decimal Longitude-Coordinate times 1E7.
	 */
	protected int lon;

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
	Node(final long osmId, final long osmVersion, final byte status, final int lat, final int lon) {
		super(osmId, osmVersion, status);
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
	public void toXml(final XmlSerializer s, final Long changeSetId)
			throws IllegalArgumentException, IllegalStateException, IOException {
		s.startTag("", "node");
		s.attribute("", "id", Long.toString(osmId));
		if (changeSetId != null) s.attribute("", "changeset", Long.toString(changeSetId));
		s.attribute("", "version", Long.toString(osmVersion));
		s.attribute("", "lat", Double.toString((lat / 1E7)));
		s.attribute("", "lon", Double.toString((lon / 1E7)));
		tagsToXml(s);
		s.endTag("", "node");
	}
	
	@Override
	public void toJosmXml(final XmlSerializer s)
			throws IllegalArgumentException, IllegalStateException, IOException {
		s.startTag("", "node");
		s.attribute("", "id", Long.toString(osmId));
		if (state == OsmElement.STATE_DELETED) {
			s.attribute("", "action", "delete");
		} else if (state == OsmElement.STATE_CREATED || state == OsmElement.STATE_MODIFIED) {
			s.attribute("", "action", "modify");
		}
		s.attribute("", "version", Long.toString(osmVersion));
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
	
	public double getDistance(final int[] location) {
		return Math.hypot(location[0] - getLat(),location[1] - getLon());
	}

	@Override
	public BoundingBox getBounds() {
		return new BoundingBox(lon, lat);
	}
}
