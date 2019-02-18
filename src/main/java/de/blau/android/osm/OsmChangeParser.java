package de.blau.android.osm;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import android.support.annotation.NonNull;
import android.util.Log;
import de.blau.android.exception.OsmParseException;
import de.blau.android.exception.StorageException;

/**
 * Parses a XML (as InputStream), provided by XmlRetriever, and pushes generated OsmElements to the given Storage.
 * 
 * Supports osmChange files
 * 
 * @author simon
 */
public class OsmChangeParser extends OsmParser {

    private static final String DEBUG_TAG = OsmChangeParser.class.getSimpleName();

    private static final String OSM_CHANGE_CREATE = "create";

    private static final String OSM_CHANGE = "osmChange";
    private static final String OSM        = "osm";

    /**
     * Place holder for a node referenced by a way, but not in the OsmChange file
     */
    public class MissingNode extends Node {
        private static final long serialVersionUID = 1L;

        /**
         * Create a new instance
         * 
         * @param osmId the OSM id of the node we are standing in for
         */
        MissingNode(long osmId) {
            super(osmId, -1L, -1, (byte) -1, -1, -1);
        }
    }

    private boolean isOsmInput;

    private boolean isOsmChangeInput;

    private byte currentStatus = OsmElement.STATE_UNCHANGED;

    /**
     * Construct a new instance of the parser
     */
    public OsmChangeParser() {
        super();
    }

    /**
     * Reset the parser to its initial state but with the existing Storage
     */
    public void reinit() {
        super.reinit();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(final String uri, final String name, final String qName, final Attributes atts) {
        try {
            switch (name) {
            case OSM:
                isOsmInput = true;
                break;
            case OSM_CHANGE:
                isOsmChangeInput = true;
                break;
            case Way.NAME:
            case Node.NAME:
            case Relation.NAME:
                parseOsmElement(name, atts, currentStatus);
                break;
            case Way.NODE:
                parseWayNode(atts);
                break;
            case OSM_CHANGE_CREATE:
            case OSM_CHANGE_MODIFY:
            case OSM_CHANGE_DELETE:
                if (!isOsmInput && isOsmChangeInput) {
                    currentStatus = action2state(name);
                } else {
                    throw new OsmParseException("Unexpected osmChange xml node " + name);
                }
                break;
            default:
                super.startElement(uri, name, qName, atts);
            }
        } catch (OsmParseException e) {
            Log.e(DEBUG_TAG, "OsmParseException", e);
            getExceptions().add(e);
        }
    }

    /**
     * Map osmChange "action" nodes to element states
     * 
     * @param action the action
     * @return the OsmElement state
     * @throws OsmParseException if the action is unknown
     */
    private byte action2state(@NonNull String action) throws OsmParseException {
        switch (action) {
        case OSM_CHANGE_CREATE:
            return OsmElement.STATE_CREATED;
        case OSM_CHANGE_MODIFY:
            return OsmElement.STATE_MODIFIED;
        case OSM_CHANGE_DELETE:
            return OsmElement.STATE_DELETED;
        default:
            throw new OsmParseException("Unexpected osmChange action " + action);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endElement(final String uri, final String name, final String qName) throws SAXException {
        try {
            switch (name) {
            case OSM_CHANGE_CREATE:
            case OSM_CHANGE_MODIFY:
            case OSM_CHANGE_DELETE:
                currentStatus = OsmElement.STATE_UNCHANGED;
                break;
            default:
                super.endElement(uri, name, qName);
            }
        } catch (StorageException sex) {
            throw new SAXException(sex);
        }
    }

    /**
     * Parses a nd entry in a Way
     * 
     * This is a near duplicate of the code in OsmParser
     * 
     * @param atts XML attributes for the current element
     * @throws OsmParseException if parsing fails
     */
    @Override
    protected void parseWayNode(final Attributes atts) throws OsmParseException {
        try {
            if (currentWay == null) {
                Log.e(DEBUG_TAG, "No currentWay set!");
            } else {
                long nodeOsmId = Long.parseLong(atts.getValue("ref"));
                Node node = nodeIndex.get(nodeOsmId);
                if (node == null) {
                    if (isOsmChangeInput) {
                        currentWay.addNode(new MissingNode(nodeOsmId));
                    } else {
                        throw new OsmParseException("parseWayNode node " + nodeOsmId + " not in storage");
                    }
                } else {
                    currentWay.addNode(node);
                }
            }
        } catch (NumberFormatException e) {
            throw new OsmParseException("WayNode unparsable");
        }
    }
}
