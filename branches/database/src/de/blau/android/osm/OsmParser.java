package de.blau.android.osm;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;
import de.blau.android.exception.OsmException;
import de.blau.android.exception.OsmParseException;

/**
 * Parses a XML (as InputStream), provided by XmlRetriever, and stores generated
 * OsmElements in the given Storage.
 * 
 * @author mb
 */
public class OsmParser extends DefaultHandler {

	private static final String DEBUG_TAG = OsmParser.class.getSimpleName();

	/**
	 * Current node (node of OsmElement), where the parser is actually in. Will
	 * be used when children of this element have to been assigned to their
	 * parent.
	 */
	private Node currentNode;

	/** Same as {@link currentNode}. */
	private Way currentWay;

	private final ArrayList<Exception> exceptions;

	private StorageDelegator storageDelegator;

	public OsmParser(StorageDelegator storageDelegator) {
		super();
		this.storageDelegator = storageDelegator;
		currentNode = null;
		currentWay = null;
		exceptions = new ArrayList<Exception>();
	}

	/**
	 * Triggers the beginning of parsing.
	 * 
	 * @throws SAXException
	 *             {@see SAXException}
	 * @throws IOException
	 *             when the xmlRetriever could not provide any data.
	 */
	public void start(final InputStream in) {
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(in, this);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} finally {
			Server.close(in);
		}
	}

	public List<Exception> getExceptions() {
		return exceptions;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void startElement(final String uri, final String name,
			final String qName, final Attributes atts) {
		try {
			if (isOsmElement(name)) {
				parseOsmElement(name, atts);
			} else if (isWayNode(name)) {
				parseWayNode(atts);
			} else if (isTag(name)) {
				parseTag(atts);
			} else if (isBounds(name)) {
				parseBounds(atts);
			}
		} catch (OsmParseException e) {
			Log.e(DEBUG_TAG, e.getStackTrace().toString());
			exceptions.add(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endElement(final String uri, final String name,
			final String qName) {
		try {
			if (isNode(name)) {
				storageDelegator.storeNode(currentNode);
				currentNode = null;
			} else if (isWay(name)) {
				storageDelegator.storeWay(currentWay);
				currentWay = null;
			}
		} catch (OsmException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param name
	 * @param atts
	 * @throws OsmParseException
	 */
	private void parseOsmElement(final String name, final Attributes atts)
			throws OsmParseException {
		long osmId = Integer.parseInt(atts.getValue("id"));
		long osmVersion = Integer.parseInt(atts.getValue("version"));
		byte status = 0;

		if (isNode(name)) {
			int lat = (int) (Double.parseDouble(atts.getValue("lat")) * 1E7);
			int lon = (int) (Double.parseDouble(atts.getValue("lon")) * 1E7);
			currentNode = OsmElementFactory.createNode(osmId, osmVersion,
					status, lat, lon);
		} else if (isWay(name)) {
			currentWay = OsmElementFactory.createWay(osmId, osmVersion, status);
		}
	}

	/**
	 * @param atts
	 */
	private void parseTag(final Attributes atts) {
		OsmElement currentOsmElement = getCurrentOsmElement();
		if (currentOsmElement == null) {
			Log.e(DEBUG_TAG, "Parsing Error: no currentOsmElement set!");
		} else {
			String k = atts.getValue("k");
			String v = atts.getValue("v");
			currentOsmElement.addOrUpdateTag(k, v);
		}
	}

	/**
	 * @param atts
	 * @throws OsmParseException
	 */
	private void parseBounds(final Attributes atts) throws OsmParseException {
		// <bounds minlat="53.56465" minlon="9.95893" maxlat="53.56579"
		// maxlon="9.96022"/>
		float minlat = Float.parseFloat(atts.getValue("minlat"));
		float maxlat = Float.parseFloat(atts.getValue("maxlat"));
		float minlon = Float.parseFloat(atts.getValue("minlon"));
		float maxlon = Float.parseFloat(atts.getValue("maxlon"));
		try {
			storageDelegator.setBoundingBox(new BoundingBox(minlon, minlat,
					maxlon, maxlat));
		} catch (OsmException e) {
			throw new OsmParseException("Bounds are not correct");
		}
	}

	/**
	 * @param atts
	 */
	private void parseWayNode(final Attributes atts) {
		if (currentWay == null) {
			Log.e(DEBUG_TAG, "No currentWay set!");
		} else {
			long nodeOsmId = Integer.parseInt(atts.getValue("ref"));
			Node node = storageDelegator.getNode(nodeOsmId);
			currentWay.addNode(node);
		}
	}

	/**
	 * @return the element in which we're in. When we're not in any element, it
	 *         returns null.
	 */
	private OsmElement getCurrentOsmElement() {
		if (currentNode != null) {
			return currentNode;
		}
		if (currentWay != null) {
			return currentWay;
		}
		return null;
	}

	/**
	 * Checks if the element "name" is an OsmElement (either a node, way or
	 * relation).
	 * 
	 * @param name
	 *            the name of the XML element.
	 * @return true if element "name" is a node, way or relation, otherwise
	 *         false.
	 */
	private static boolean isOsmElement(final String name) {
		return isNode(name) || isWay(name);
	}

	/**
	 * Checks if the element "name" is a Node
	 * 
	 * @param name
	 *            the name of the XML element.
	 * @return true if element "name" is a node, otherwise false.
	 */
	private static boolean isNode(final String name) {
		return Node.NAME.equalsIgnoreCase(name);
	}

	/**
	 * @see isNode()
	 */
	private static boolean isWay(final String name) {
		return Way.NAME.equalsIgnoreCase(name);
	}

	/**
	 * @see isNode()
	 */
	private static boolean isTag(final String name) {
		return "tag".equalsIgnoreCase(name);
	}

	/**
	 * @see isNode()
	 */
	private static boolean isWayNode(final String name) {
		return Way.NODE.equalsIgnoreCase(name);
	}

	/**
	 * @see isNode()
	 */
	private static boolean isBounds(final String name) {
		return BoundingBox.NAME.equalsIgnoreCase(name);
	}

}
