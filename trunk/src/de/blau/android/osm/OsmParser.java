package de.blau.android.osm;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

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
 * Parses a XML (as InputStream), provided by XmlRetriever, and pushes generated OsmElements to the given Storage.
 * 
 * @author mb
 */
public class OsmParser extends DefaultHandler {

	private static final String DEBUG_TAG = OsmParser.class.getSimpleName();

	/** The storage, where the data will be stored (e.g. as JavaStorage or SqliteStorage). */
	private final Storage storage;

	/**
	 * Current node (node of OsmElement), where the parser is actually in. Will be used when children of this element
	 * have to been assigned to their parent.
	 */
	private Node currentNode;

	/** Same as {@link currentNode}. */
	private Way currentWay;

	/** Same as {@link currentNode}. */
	private Relation currentRelation;

	private final ArrayList<Exception> exceptions;

	public OsmParser() {
		super();
		storage = new Storage();
		currentNode = null;
		currentWay = null;
		currentRelation = null;
		exceptions = new ArrayList<Exception>();
	}

	public Storage getStorage() {
		return storage;
	}

	/**
	 * Triggers the beginning of parsing.
	 * 
	 * @throws SAXException {@see SAXException}
	 * @throws IOException when the xmlRetriever could not provide any data.
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
	public void startElement(final String uri, final String name, final String qName, final Attributes atts) {
		try {
			if (isOsmElement(name)) {
				parseOsmElement(name, atts);
			} else if (isWayNode(name)) {
				parseWayNode(atts);
			} else if (isTag(name)) {
				parseTag(atts);
			} else if (isMember(name)) {
				//parseMember(atts);
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
	public void endElement(final String uri, final String name, final String qName) {
		if (isNode(name)) {
			storage.insertNodeUnsafe(currentNode);
			currentNode = null;
		} else if (isWay(name)) {
			storage.insertWayUnsafe(currentWay);
			currentWay = null;
		} else if (isRelation(name)) {
			//storage.insertRelationUnsafe(currentRelation);
			currentRelation = null;
		}
	}

	/**
	 * @param name
	 * @param atts
	 * @throws OsmParseException
	 */
	private void parseOsmElement(final String name, final Attributes atts) throws OsmParseException {
		long osmId = Integer.parseInt(atts.getValue("id"));
		Date timestamp = parseDate(atts.getValue("timestamp"));
		String user = atts.getValue("user");
		byte status = 0;

		if (isNode(name)) {
			int lat = (int) (Double.parseDouble(atts.getValue("lat")) * 1E7);
			int lon = (int) (Double.parseDouble(atts.getValue("lon")) * 1E7);
			currentNode = OsmElementFactory.createNode(osmId, user, timestamp, status, lat, lon);
		} else if (isWay(name)) {
			currentWay = OsmElementFactory.createWay(osmId, user, timestamp, status);
		} else if (isRelation(name)) {
			//currentRelation = OsmElementFactory.createRelation(osmId, user, timestamp, status);
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
			currentOsmElement.addTag(new Tag(k, v));
		}
	}

	/**
	 * @param atts
	 * @throws OsmParseException
	 */
	private void parseBounds(final Attributes atts) throws OsmParseException {
		//<bounds minlat="53.56465" minlon="9.95893" maxlat="53.56579" maxlon="9.96022"/>
		float minlat = Float.parseFloat(atts.getValue("minlat"));
		float maxlat = Float.parseFloat(atts.getValue("maxlat"));
		float minlon = Float.parseFloat(atts.getValue("minlon"));
		float maxlon = Float.parseFloat(atts.getValue("maxlon"));
		try {
			storage.setBoundingBox(new BoundingBox(minlon, minlat, maxlon, maxlat));
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
			Node node = storage.getNode(nodeOsmId);
			currentWay.addNode(node);
		}
	}

	/**
	 * @return the element in which we're in. When we're not in any element, it returns null.
	 */
	private OsmElement getCurrentOsmElement() {
		if (currentNode != null) {
			return currentNode;
		}
		if (currentWay != null) {
			return currentWay;
		}
		if (currentRelation != null) {
			return currentRelation;
		}
		return null;
	}

	/**
	 * Parses a date. The format of the str is "yyyy-MM-ddTHH:mm:ss+01:00", where 'T' is a literal and "+01:00" can be
	 * any other timezone offset. This method is needed, because the Java build-in {@link SimpleDate.parse()} can't
	 * handle the OSM specific timezoneformat.
	 * 
	 * @param str the date represented by a String.
	 * @return
	 * @throws IllegalArgumentException when str is null or smaller than 25 characters.
	 * @throws OsmParseException
	 */
	private static Date parseDate(final String str) throws IllegalArgumentException, OsmParseException {
		if (str == null || str.length() < 25) {
			throw new OsmParseException("Date-string " + str + " is not valid!");
		}
		Calendar cal = Calendar.getInstance();
		//yyyy-MM-ddTHH:mm:ss+01:00
		int year = Integer.parseInt(str.substring(0, 4));
		int month = Integer.parseInt(str.substring(5, 7));
		int day = Integer.parseInt(str.substring(8, 10));
		int hour = Integer.parseInt(str.substring(11, 13));
		int min = Integer.parseInt(str.substring(14, 16));
		int sec = Integer.parseInt(str.substring(17, 19));
		TimeZone tz = TimeZone.getTimeZone("GMT" + str.substring(19, 25));

		cal.set(year, month, day, hour, min, sec);
		cal.setTimeZone(tz);
		return cal.getTime();
	}

	/**
	 * Checks if the element "name" is an OsmElement (either a node, way or relation).
	 * 
	 * @param name the name of the XML element.
	 * @return true if element "name" is a node, way or relation, otherwise false.
	 */
	private static boolean isOsmElement(final String name) {
		return isNode(name) || isWay(name) || isRelation(name);
	}

	/**
	 * Checks if the element "name" is a Node
	 * 
	 * @param name the name of the XML element.
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
	private static boolean isRelation(final String name) {
		//return Relation.NAME.equalsIgnoreCase(name);
		return false;
	}

	/**
	 * @see isNode()
	 */
	private static boolean isTag(final String name) {
		return Tag.NAME.equalsIgnoreCase(name);
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
	private static boolean isMember(final String name) {
		//return Member.NAME.equalsIgnoreCase(name);
		return false;
	}

	/**
	 * @see isNode()
	 */
	private static boolean isBounds(final String name) {
		return BoundingBox.NAME.equalsIgnoreCase(name);
	}

}
