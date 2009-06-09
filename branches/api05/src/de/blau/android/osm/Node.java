package de.blau.android.osm;


/**
 * Node represents a Node in the OSM-data-structure. It stores the lat/lon-pair and provides some package-internal
 * manipulating-methods.
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
	 * @param osmId the OSM-ID. When not yet transmitted to the API it is negative.
	 * @param user the last user made changes to this element.
	 * @param dateChanged date of last change.
	 * @param status see {@link OsmElement#state}
	 * @param lat WGS84 decimal Latitude-Coordinate times 1E7.
	 * @param lon WGS84 decimal Longitude-Coordinate times 1E7.
	 */
	Node(final long osmId, final byte status, final int lat, final int lon) {
		super(osmId, status);
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return super.toString() + "\tlat: " + lat + "; lon: " + lon;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toXml() {
		String xml = "";
		xml += "<node id=\"" + osmId + "\" lat=\"" + lat / 1E7 + "\" lon=\"" + lon / 1E7 + "\" ";
		if (tags.size() > 0) {
			xml += ">\n";
			xml += tagsToXml();
			xml += "</node>\n";
		} else {
			xml += "/>\n";
		}
		return xml;
	}
}
