package de.blau.android.osm;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.support.annotation.NonNull;
import android.util.Log;
import de.blau.android.exception.OsmParseException;
import de.blau.android.exception.StorageException;
import de.blau.android.util.DateFormatter;
import de.blau.android.util.collections.LongOsmElementMap;

/**
 * Parses a XML (as InputStream), provided by XmlRetriever, and pushes generated OsmElements to the given Storage.
 * 
 * Supports API 0.6 output and JOSM OSM files, assumes Node, Ways, Relations ordering of input
 * 
 * @author mb
 * @author simon
 */
public class OsmParser extends DefaultHandler {

    private static final String JOSM_ACTION = "action";

    public static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private static final String DEBUG_TAG = OsmParser.class.getSimpleName();

    private static final String TAG = "tag";

    private static final String OSM = "osm";

    protected static final String OSM_CHANGE_DELETE = "delete";
    protected static final String OSM_CHANGE_MODIFY = "modify";

    protected static final String OVERPASS_NOTE = "note";
    protected static final String OVERPASS_META = "meta";

    /** The storage, where the data will be stored (e.g. as JavaStorage or SqliteStorage). */
    private final Storage storage;

    /**
     * Current node (node of OsmElement), where the parser is actually in. Will be used when children of this element
     * have to been assigned to their parent.
     */
    private Node currentNode = null;

    /** Same as {@link currentNode}. */
    protected Way currentWay = null;

    /** Same as {@link currentNode}. */
    private Relation currentRelation = null;

    private TreeMap<String, String> currentTags;

    private final List<Exception> exceptions = new ArrayList<>();

    /**
     * Helper class to store missing relation information for post processing
     */
    private class MissingRelation {
        Relation       parent;
        RelationMember member;

        /**
         * Construct a new temporary container for missing Relations
         * 
         * @param member the RelationMember
         * @param parent the parent Relation
         */
        public MissingRelation(@NonNull RelationMember member, @NonNull Relation parent) {
            this.member = member;
            this.parent = parent;
        }
    }

    private ArrayList<MissingRelation> missingRelations = new ArrayList<>();

    protected LongOsmElementMap<Node> nodeIndex = null;
    private LongOsmElementMap<Way>    wayIndex  = null;

    /**
     * Construct a new instance of the parser
     */
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

    /**
     * Get the Storage instane associated with the parser
     * 
     * @return an instance of Storage
     */
    @NonNull
    public Storage getStorage() {
        return storage;
    }

    /**
     * Triggers the beginning of parsing.
     * 
     * @param in the InputStream
     * @throws SAXException {@see SAXException}
     * @throws IOException when the xmlRetriever could not provide any data.
     * @throws ParserConfigurationException if a parser feature is used that is not supported
     */
    public void start(@NonNull final InputStream in) throws SAXException, IOException, ParserConfigurationException {
        SAXParserFactory factory = SAXParserFactory.newInstance(); // NOSONAR
        SAXParser saxParser = factory.newSAXParser();
        saxParser.parse(in, this);
    }

    /**
     * needed for post processing of relations
     */
    @Override
    public void endDocument() {
        Log.d(DEBUG_TAG, "Post processing relations.");
        for (MissingRelation mr : missingRelations) {
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

    /**
     * Get the List of exceptions that have occurred, if any
     * 
     * @return a List of Exceptions
     */
    @NonNull
    public List<Exception> getExceptions() {
        return exceptions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(final String uri, final String name, final String qName, final Attributes atts) {
        try {
            switch (name) {
            case Way.NAME:
            case Node.NAME:
            case Relation.NAME:
                parseOsmElement(name, atts, OsmElement.STATE_UNCHANGED);
                break;
            case Way.NODE:
                parseWayNode(atts);
                break;
            case Relation.MEMBER:
                parseRelationMember(atts);
                break;
            case TAG:
                parseTag(atts);
                break;
            case BoundingBox.NAME:
                parseBounds(atts);
                break;
            case OSM:
            case OVERPASS_NOTE:
            case OVERPASS_META:
                // we don't do anything with these
                break;
            default:
                throw new OsmParseException("Unknown element " + name);
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
            switch (name) {
            case Node.NAME:
                if (currentNode != null) {
                    addTags(currentNode);
                    storage.insertNodeUnsafe(currentNode);
                    currentNode = null;
                } else {
                    throw new SAXException("State error, null Node");
                }
                break;
            case Way.NAME:
                if (currentWay != null) {
                    addTags(currentWay);
                    if (currentWay.getNodes() != null && !currentWay.getNodes().isEmpty()) {
                        storage.insertWayUnsafe(currentWay);
                    } else {
                        Log.e(DEBUG_TAG, "Way " + currentWay.getOsmId() + " has no nodes! Ignored.");
                    }
                    currentWay = null;
                } else {
                    throw new SAXException("State error, null Way");
                }
                break;
            case Relation.NAME:
                if (currentRelation != null) {
                    addTags(currentRelation);
                    storage.insertRelationUnsafe(currentRelation);
                    currentRelation = null;
                } else {
                    throw new SAXException("State error, null Relation");
                }
                break;
            default:
                // ignore everything else
            }
        } catch (StorageException sex) {
            throw new SAXException(sex);
        }
    }

    /**
     * Add accumulated tags to element
     * 
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
     * 
     * @param name the OsmElement type ("node", "way", "relation")
     * @param atts the attributes of the current XML start tag
     * @param status the status the element should be set too
     * @throws OsmParseException if parsing fails
     */
    protected void parseOsmElement(@NonNull final String name, @NonNull final Attributes atts, byte status) throws OsmParseException {
        try {
            long osmId = Long.parseLong(atts.getValue("id"));
            String version = atts.getValue("version");
            long osmVersion = version == null ? 0 : Long.parseLong(version); // hack for JOSM file
                                                                             // format support
            String action = atts.getValue(JOSM_ACTION);

            String timestampStr = atts.getValue("timestamp");
            long timestamp = -1L;
            if (timestampStr != null) {
                try {
                    timestamp = DateFormatter.getUtcFormat(OsmParser.TIMESTAMP_FORMAT).parse(timestampStr).getTime() / 1000;
                } catch (ParseException e) {
                    Log.d(DEBUG_TAG, "Invalid timestamp " + timestampStr);
                }
            }

            if (action != null) {
                if (action.equalsIgnoreCase(OSM_CHANGE_MODIFY)) {
                    status = OsmElement.STATE_MODIFIED;
                    if (osmId < 0) {
                        status = OsmElement.STATE_CREATED;
                    }
                } else if (action.equalsIgnoreCase(OSM_CHANGE_DELETE)) {
                    status = OsmElement.STATE_DELETED;
                }
            }

            switch (name) {
            case Node.NAME:
                int lat = (new BigDecimal(atts.getValue("lat")).scaleByPowerOfTen(Node.COORDINATE_SCALE)).intValue();
                int lon = (new BigDecimal(atts.getValue("lon")).scaleByPowerOfTen(Node.COORDINATE_SCALE)).intValue();
                currentNode = OsmElementFactory.createNode(osmId, osmVersion, timestamp, status, lat, lon);
                break;
            case Way.NAME:
                currentWay = OsmElementFactory.createWay(osmId, osmVersion, timestamp, status);
                if (nodeIndex == null) {
                    nodeIndex = storage.getNodeIndex(); // !!!!! this will fail if input is not ordered
                }
                break;
            case Relation.NAME:
                currentRelation = OsmElementFactory.createRelation(osmId, osmVersion, timestamp, status);
                if (nodeIndex == null) {
                    nodeIndex = storage.getNodeIndex(); // !!!!! this will fail if input is not ordered
                }
                if (wayIndex == null) {
                    wayIndex = storage.getWayIndex(); // !!!!! this will fail if input is not ordered
                }
                break;
            default:
                throw new OsmParseException("Unknown element " + name);
            }
        } catch (NumberFormatException e) {
            throw new OsmParseException("Element unparsable");
        }
    }

    /**
     * Parse tags and accumulate them in a collection for later use
     * 
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
     * Parse a bounding box
     * 
     * @param atts an Attributes object holding the current attributes
     * @throws OsmParseException if parsing fails
     */
    private void parseBounds(final Attributes atts) throws OsmParseException {
        // <bounds minlat="53.56465" minlon="9.95893" maxlat="53.56579" maxlon="9.96022"/>
        try {
            double minlat = Double.parseDouble(atts.getValue("minlat"));
            double maxlat = Double.parseDouble(atts.getValue("maxlat"));
            double minlon = Double.parseDouble(atts.getValue("minlon"));
            double maxlon = Double.parseDouble(atts.getValue("maxlon"));
            storage.addBoundingBox(new BoundingBox(minlon, minlat, maxlon, maxlat));
            Log.d(DEBUG_TAG, "Creating bounding box " + minlon + " " + minlat + " " + maxlon + " " + maxlat);
        } catch (NumberFormatException e) {
            throw new OsmParseException("Bounds unparsable");
        }
    }

    /**
     * Parse a nd entry in a Way
     * 
     * @param atts XML attributes for the current element
     * @throws OsmParseException if parsing fails
     */
    protected void parseWayNode(final Attributes atts) throws OsmParseException {
        try {
            if (currentWay == null) {
                Log.e(DEBUG_TAG, "No currentWay set!");
            } else {
                long nodeOsmId = Long.parseLong(atts.getValue("ref"));
                Node node = nodeIndex.get(nodeOsmId);
                if (node == null) {
                    throw new OsmParseException("parseWayNode node " + nodeOsmId + " not in storage");
                } else {
                    currentWay.addNode(node);
                }
            }
        } catch (NumberFormatException e) {
            throw new OsmParseException("WayNode unparsable");
        }
    }

    /**
     * Parse relation members, storing information on relations that we haven't seen yet for post processing
     * 
     * @param atts XML attributes for the current element
     * @throws OsmParseException if parsing fails
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
                switch (type) {
                case Node.NAME:
                    Node n = nodeIndex.get(ref);
                    if (n != null) {
                        n.addParentRelation(currentRelation);
                        member = new RelationMember(role, n);
                    } else {
                        member = new RelationMember(type, ref, role);
                    }
                    break;
                case Way.NAME:
                    Way w = wayIndex.get(ref);
                    if (w != null) {
                        w.addParentRelation(currentRelation);
                        member = new RelationMember(role, w);
                    } else {
                        member = new RelationMember(type, ref, role);
                    }
                    break;
                case Relation.NAME:
                    Relation r = storage.getRelation(ref);
                    if (r != null) {
                        r.addParentRelation(currentRelation);
                        member = new RelationMember(role, r);
                    } else {
                        // these need to be saved and reprocessed
                        member = new RelationMember(type, ref, role);
                        MissingRelation mr = new MissingRelation(member, currentRelation);
                        missingRelations.add(mr);
                    }
                    break;
                default:
                    throw new OsmParseException("Unknown OSM object type " + type);
                }
                currentRelation.addMember(member);
            }
        } catch (NumberFormatException e) {
            throw new OsmParseException("RelationMember unparsable");
        }
    }

    /**
     * Clear the list of bounding boxes
     */
    public void clearBoundingBoxes() {
        getStorage().getBoundingBoxes().clear();
    }
}
