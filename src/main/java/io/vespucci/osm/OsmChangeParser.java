package io.vespucci.osm;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import android.util.Log;
import androidx.annotation.NonNull;
import io.vespucci.exception.OsmParseException;
import io.vespucci.exception.StorageException;
import io.vespucci.tasks.Note;
import io.vespucci.tasks.OsnParser;

/**
 * Parses OSM OCS/osmChange XML
 * 
 * Including an OsmAnd extension that includes OSM Notes
 * 
 * @author Simon Poole
 */
public class OsmChangeParser extends OsmParser {

    private static final String DEBUG_TAG = OsmChangeParser.class.getSimpleName().substring(0, Math.min(23, OsmChangeParser.class.getSimpleName().length()));

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

    private OsnParser noteHandler;

    /**
     * Construct a new instance of the parser
     */
    public OsmChangeParser() {
        super(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(final String uri, final String name, final String qName, final Attributes atts) {
        try {
            switch (name) {
            case OsmXml.OSM:
                isOsmInput = true;
                break;
            case OsmXml.OSM_CHANGE:
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
            case OsmXml.CREATE:
            case OsmXml.MODIFY:
            case OsmXml.DELETE:
                if (!isOsmInput && isOsmChangeInput) {
                    currentStatus = action2state(name);
                } else {
                    throw new OsmParseException("Unexpected osmChange xml node " + name);
                }
                break;
            case Note.NOTE_ELEMENT:
            case Note.COMMENT_ELEMENT:
                if (noteHandler == null) {
                    noteHandler = new OsnParser();
                }
                noteHandler.startElement(uri, name, qName, atts);
                break;
            default:
                super.startElement(uri, name, qName, atts);
            }
        } catch (OsmParseException e) {
            handleException(e);
        }
    }

    /**
     * Handle a OsmParseException
     * 
     * @param e the exception
     */
    private void handleException(@NonNull OsmParseException e) {
        Log.e(DEBUG_TAG, "OsmParseException", e);
        getExceptions().add(e);
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
        case OsmXml.CREATE:
            return OsmElement.STATE_CREATED;
        case OsmXml.MODIFY:
            return OsmElement.STATE_MODIFIED;
        case OsmXml.DELETE:
            return OsmElement.STATE_DELETED;
        default:
            throw new OsmParseException("Unexpected osmChange action " + action);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (noteHandler != null) {
            noteHandler.characters(ch, start, length);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endElement(final String uri, final String name, final String qName) throws SAXException {
        try {
            switch (name) {
            case OsmXml.CREATE:
            case OsmXml.MODIFY:
            case OsmXml.DELETE:
                currentStatus = OsmElement.STATE_UNCHANGED;
                break;
            case Note.NOTE_ELEMENT:
            case Note.COMMENT_ELEMENT:
                if (noteHandler == null) {
                    throw new OsmParseException("Unexpected note element " + name);
                }
                noteHandler.endElement(uri, name, qName);
                break;
            default:
                super.endElement(uri, name, qName);
            }
        } catch (StorageException sex) {
            throw new SAXException(sex);
        } catch (OsmParseException e) {
            handleException(e);
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
                long nodeOsmId = Long.parseLong(atts.getValue(Way.REF));
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

    /**
     * Get any notes included in the input (OsmAnd extension)
     * 
     * @return the a List of Notes
     */
    @NonNull
    public List<Note> getNotes() {
        return noteHandler != null ? noteHandler.getNotes() : new ArrayList<>();
    }
}
