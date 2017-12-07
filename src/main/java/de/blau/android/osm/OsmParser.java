package de.blau.android.osm;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;
import de.blau.android.exception.OsmException;
import de.blau.android.exception.OsmParseException;
import de.blau.android.exception.StorageException;
import de.blau.android.util.collections.LongOsmElementMap;

/**
 * Parses a XML (as InputStream), provided by XmlRetriever, and pushes generated OsmElements to the given Storage.
 * 
 * @author mb
 */
public class OsmParser extends DefaultHandler {

	public static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

	private static final String DEBUG_TAG = OsmParser.class.getSimpleName();

	/** The storage, where the data will be stored (e.g. as JavaStorage or SqliteStorage). */
	private final Storage storage;

	/**
	 * Current node (node of OsmElement), where the parser is actually in. Will be used when children of this element
	 * have to been assigned to their parent.
	 */
	private Node currentNode = null;

	/** Same as {@link currentNode}. */
	private Way currentWay = null;
	
	/** Same as {@link currentNode}. */
	private Relation currentRelation = null;
	
	private TreeMap<String, String> currentTags;

	private final ArrayList<Exception> exceptions = new ArrayList<>();

	/**
	 * Helper class to store missing relation information for post processing
	 */
	private class MissingRelation {
		Relation parent;
		RelationMember member;
		
		public MissingRelation(RelationMember member, Relation parent) {
			this.member = member;
			this.parent = parent;
		}
	}
	private ArrayList<MissingRelation> missingRelations = new ArrayList<>();
	
	private LongOsmElementMap<Node>nodeIndex = null;
	private LongOsmElementMap<Way>wayIndex = null;

	public OsmParser() {
		super();
		storage = new Storage();
	}

	/**
	 * Reset the parser to its initial state but with the existing Storage
	 */
	public void reinit() {
		currentNode = null;
		currentWay = null;
		currentRelation = null;
		exceptions.clear();
		missingRelations.clear();
	}

	public Storage getStorage() {
		return storage;
	}

	/**
	 * Triggers the beginning of parsing.
	 * @throws SAXException 
	 * 
	 * @throws SAXException {@see SAXException}
	 * @throws IOException when the xmlRetriever could not provide any data.
	 * @throws ParserConfigurationException 
	 */
	public void start(final InputStream in) throws SAXException, IOException, ParserConfigurationException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();
		saxParser.parse(in, this);
	}

	/**
	 * needed for post processing of relations
	 */
	@Override
	public void endDocument() {
		Log.d(DEBUG_TAG, "Post processing relations.");
		for (MissingRelation mr : missingRelations)
		{
			RelationMember rm = mr.member;
			Relation r = storage.getRelation(rm.ref);
			if (r != null) {
				rm.setElement(r);
				r.addParentRelation(mr.parent);
				Log.d(DEBUG_TAG, "Added relation " + rm.ref);
			}
		}
		Log.d(DEBUG_TAG, "Finished parsing input.");
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
			} else if (isRelationMember(name)) {
				parseRelationMember(atts);
			} else if (isTag(name)) {
				parseTag(atts);
			} else if (isBounds(name)) {
				parseBounds(atts);
			}
		} catch (OsmParseException e) {
			Log.e(DEBUG_TAG, "OsmParseException", e);
			exceptions.add(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endElement(final String uri, final String name, final String qName) throws SAXException {
		try {
			if (isNode(name)) {
				addTags(currentNode);
				storage.insertNodeUnsafe(currentNode);
				currentNode = null;
			} else if (isWay(name)) {
				addTags(currentWay);
				if (currentWay.getNodes() != null && !currentWay.getNodes().isEmpty()) {
					storage.insertWayUnsafe(currentWay);
				} else {
					Log.e(DEBUG_TAG,"Way " + currentWay.getOsmId() + " has no nodes! Ignored.");
				}
				currentWay = null;
			} else if (isRelation(name)) {
				addTags(currentRelation);
				storage.insertRelationUnsafe(currentRelation);
				currentRelation = null;
			}
		} catch (StorageException sex) {
			throw new SAXException(sex);
		}
	}

	/**
	 * Add accumulated tags to element
	 * @param e element to add the tags to
	 */
	void addTags(OsmElement e) {
		if (currentTags != null) {
			e.setTags(currentTags);
			currentTags = null;
		}
	}
	
	
	/**
	 * parse API 0.6 output and JOSM OSM files
	 * @param name
	 * @param atts
	 * @throws OsmParseException
	 */
	private void parseOsmElement(final String name, final Attributes atts) throws OsmParseException {
		try {
			long osmId = Long.parseLong(atts.getValue("id"));
			String version = atts.getValue("version");
			long osmVersion = version == null ? 0 : Long.parseLong(atts.getValue("version")); // hack for JOSM file format support
			String action = atts.getValue("action");
			
			String timestampStr = atts.getValue("timestamp");
			long timestamp = -1L;
			if (timestampStr != null) {
				try {
					timestamp = new SimpleDateFormat(TIMESTAMP_FORMAT).parse(timestampStr).getTime()/1000;
				} catch (ParseException e) {
					Log.d(DEBUG_TAG, "Invalid timestamp " + timestampStr);
				}
			}
			
			byte status = OsmElement.STATE_UNCHANGED;
			if (action != null) {
				if (action.equalsIgnoreCase("modify")) {
					status = OsmElement.STATE_MODIFIED;
					if (osmId < 0) {
						status = OsmElement.STATE_CREATED;
					}
				} else if (action.equalsIgnoreCase("delete")) {
					status = OsmElement.STATE_DELETED;
				}
			}
			
			if (isNode(name)) {
				int lat = (int) (Double.valueOf(atts.getValue("lat")) * 1E7);
				int lon = (int) (Double.valueOf(atts.getValue("lon")) * 1E7);
				currentNode = OsmElementFactory.createNode(osmId, osmVersion, timestamp, status, lat, lon);
				// Log.d(DEBUG_TAG, "Creating node " + osmId);
			} else if (isWay(name)) {
				currentWay = OsmElementFactory.createWay(osmId, osmVersion, timestamp, status);
				if (nodeIndex==null) {
					nodeIndex = storage.getNodeIndex(); // !!!!! this will fail if input is not ordered
				}
				// Log.d(DEBUG_TAG, "Creating way " + osmId);
			}
			else if (isRelation(name)) {
				currentRelation = OsmElementFactory.createRelation(osmId, osmVersion, timestamp, status);
				if (nodeIndex==null) {
					nodeIndex = storage.getNodeIndex(); // !!!!! this will fail if input is not ordered
				}
				if (wayIndex==null) {
					wayIndex = storage.getWayIndex(); // !!!!! this will fail if input is not ordered
				}
			}
			else {
				throw new OsmParseException("Unknown element " + name);
			}
		} catch (NumberFormatException e) {
			throw new OsmParseException("Element unparsable");
		}
	}

	/**
	 * Parse tags and accumulate them in a collection for later use
	 * @param atts current set of xml attribute
	 */
	private void parseTag(final Attributes atts) {
		if (currentTags == null) {
			currentTags = new TreeMap<>();
		}
		String k = atts.getValue("k");
		String v = atts.getValue("v");
		currentTags.put(k, v);
	}

	/**
	 * @param atts
	 * @throws OsmParseException
	 */
	private void parseBounds(final Attributes atts) throws OsmParseException {
		//<bounds minlat="53.56465" minlon="9.95893" maxlat="53.56579" maxlon="9.96022"/>
		try {
			double minlat = Double.parseDouble(atts.getValue("minlat"));
			double maxlat = Double.parseDouble(atts.getValue("maxlat"));
			double minlon = Double.parseDouble(atts.getValue("minlon"));
			double maxlon = Double.parseDouble(atts.getValue("maxlon"));
			try {
				if (storage.getBoundingBoxes() == null) {
					storage.setBoundingBox(new BoundingBox(minlon, minlat, maxlon, maxlat));
				} else {
					storage.addBoundingBox(new BoundingBox(minlon, minlat, maxlon, maxlat));
				}
				Log.d(DEBUG_TAG, "Creating bounding box " + minlon + " " + minlat + " " + maxlon + " " + maxlat);
			} catch (OsmException e) {
				throw new OsmParseException("Bounds are not correct");
			}
		} catch (NumberFormatException e) {
			throw new OsmParseException("Bounds unparsable");
		}
	}

	/**
	 * @param atts	XML attributes for the current element
	 * @throws OsmParseException
	 */
	private void parseWayNode(final Attributes atts) throws OsmParseException {
		try {
			if (currentWay == null) {
				Log.e(DEBUG_TAG, "No currentWay set!");
			} else {
				long nodeOsmId = Long.parseLong(atts.getValue("ref"));
				// Log.d("OsmParser","parseWayNode " + nodeOsmId);
				// Node node = storage.getNode(nodeOsmId);
				Node node = nodeIndex.get(nodeOsmId);
				if (node==null) {
					throw new OsmParseException("parseWayNode node " + nodeOsmId + " not in storage");
				}
				currentWay.addNode(node);
			}
		} catch (NumberFormatException e) {
			throw new OsmParseException("WayNode unparsable");
		}
	}
	
	/**
	 * Parse relation members, storing information on relations that we haven't seen yet for post processing
	 * 
	 * @param atts	XML attributes for the current element
	 * @throws OsmParseException
	 */
	private void parseRelationMember(final Attributes atts) throws OsmParseException {
		try {
			if (currentRelation == null) {
				Log.e(DEBUG_TAG, "No currentRelation set!");
			} else {
				long ref = Long.parseLong(atts.getValue("ref"));
				String type = atts.getValue("type");
				String role = atts.getValue("role");
				RelationMember member = null;
				
				if (isNode(type)) {
					// Node n = storage.getNode(ref);
					Node n = nodeIndex.get(ref);
					if (n != null) {
						n.addParentRelation(currentRelation);
						member = new RelationMember(role, n);
					} else {
						member = new RelationMember(type, ref, role);
					}
					// Log.d(DEBUG_TAG, "Added node member");
				} else if (isWay(type)) {
					// Way w = storage.getWay(ref);
					Way w = wayIndex.get(ref);
					if (w != null) {
						w.addParentRelation(currentRelation);
						member = new RelationMember(role, w);
					} else {
						member = new RelationMember(type, ref, role);
					}
					// Log.d(DEBUG_TAG, "Added way member");
				} else if (isRelation(type)) {
					Relation r = storage.getRelation(ref);
					if (r != null) {
						r.addParentRelation(currentRelation);
						member = new RelationMember(role, r);
					} else {
						// these need to be saved and reprocessed
						member = new RelationMember(type, ref, role);
						MissingRelation mr = new MissingRelation(member, currentRelation);
						missingRelations.add(mr);
						// Log.d(DEBUG_TAG, "Parent relation not available yet or downloaded");
					}
					// Log.d(DEBUG_TAG, "Added relation member");
				}
					
				currentRelation.addMember(member);
				// Log.d(DEBUG_TAG, "Adding relation member " + ref + " " + type);
			}
		} catch (NumberFormatException e) {
			throw new OsmParseException("RelationMember unparsable");
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
	
	/**
	 * @see isNode()
	 */
	private static boolean isRelation(final String name) {
		return Relation.NAME.equalsIgnoreCase(name);
	}
	
	/**
	 * @see isNode()
	 */
	private static boolean isRelationMember(final String name) {
		return Relation.MEMBER.equalsIgnoreCase(name);
	}

	/**
	 * Clear the list of bounding boxes
	 */
	public void clearBoundingBoxes() {
		List<BoundingBox> boundingBoxes = getStorage().getBoundingBoxes();
		if (boundingBoxes != null) {
			boundingBoxes.clear();
		}
	}
}
