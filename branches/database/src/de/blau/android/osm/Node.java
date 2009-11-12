package de.blau.android.osm;

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

/**
 * Node represents a Node in the OSM-data-structure. It stores the lat/lon-pair
 * and provides some package-internal manipulating-methods.
 * 
 * @author mb
 */
public class Node extends OsmElement {
	/**
	 * 
	 */
	private static final long serialVersionUID = 152395243648348266L;

	/**
	 * WGS84 decimal Latitude-Coordinate times 1E7.
	 */
	private int lat;

	/**
	 * WGS84 decimal Longitude-Coordinate times 1E7.
	 */
	private int lon;

	/**
	 * It's name in the OSM-XML-scheme.
	 */
	public static final String NAME = "node";

	/**
	 * Constructor. Call it solely in {@link OsmElementFactory}!
	 * 
	 * @param osmId
	 *            the OSM-ID. When not yet transmitted to the API it is
	 *            negative.
	 * @param osmVersion
	 *            the version of the element
	 * @param status
	 *            see {@link OsmElement#state}
	 * @param lat
	 *            WGS84 decimal Latitude-Coordinate times 1E7.
	 * @param lon
	 *            WGS84 decimal Longitude-Coordinate times 1E7.
	 */
	Node(final long osmId, final long osmVersion, final byte status,
			final int lat, final int lon) {
		super(osmId, osmVersion, status);
		this.lat = lat;
		this.lon = lon;
	}

	public int getLat() {
		return lat;
	}

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

	@Override
	public byte getType() {
		return OsmElement.TYPE_NODE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return super.toString() + "\tlat: " + lat + "; lon: " + lon;
	}

	@Override
	public void toXml(XmlSerializer s, long changeSetId) {
		try {
			s.startTag("", "node");
			s.attribute("", "id", Long.toString(osmId));
			s.attribute("", "changeset", Long.toString(changeSetId));
			s.attribute("", "version", Long.toString(osmVersion));
			s.attribute("", "lat", Long.toString((long) (lat / 1E7)));
			s.attribute("", "lon", Long.toString((long) (lon / 1E7)));
			tagsToXml(s);
			s.endTag("", "node");
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
