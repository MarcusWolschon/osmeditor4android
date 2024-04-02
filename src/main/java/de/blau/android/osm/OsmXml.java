package de.blau.android.osm;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Provide reading and writing data files in OSM and JOSM format
 * 
 * @author simon
 *
 */
public final class OsmXml {
    private static final String DEBUG_TAG = OsmXml.class.getSimpleName().substring(0, Math.min(23, OsmXml.class.getSimpleName().length()));

    public static final String UTF_8 = "UTF-8";

    public static final String OSM         = "osm";
    public static final String OSM_CHANGE  = "osmChange";
    public static final String CHANGESET   = "changeset";
    public static final String JOSM_UPLOAD = "upload";
    public static final String TRUE        = "true";
    public static final String DELETE      = "delete";
    public static final String MODIFY      = "modify";
    public static final String CREATE      = "create";
    public static final String VERSION_0_6 = "0.6";
    public static final String VERSION     = "version";
    public static final String GENERATOR   = "generator";

    private static final Comparator<Relation> relationOrder = (r1, r2) -> {
        if (r1.hasParentRelation(r2)) {
            return -1;
        }
        if (r2.hasParentRelation(r1)) {
            return 1;
        }
        return 0;
    };

    /**
     * Empty private constructor to prevent instantiation
     */
    private OsmXml() {
        // empty
    }

    /**
     * Writes created/changed/deleted data to outputStream in OsmChange format
     * http://wiki.openstreetmap.org/wiki/OsmChange
     * 
     * @param storage a Storage object with the changes
     * @param outputStream stream to write to
     * @param changeSetId the allocated changeset id or null if non
     * @param maxChanges maximum number of changes to write
     * @param generator a String for the generator attribute
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     * @throws IOException
     * @throws XmlPullParserException
     */
    public static void writeOsmChange(@NonNull Storage storage, @NonNull OutputStream outputStream, @Nullable Long changeSetId, int maxChanges,
            @NonNull String generator) throws IllegalArgumentException, IllegalStateException, IOException, XmlPullParserException {
        int count = 0;
        Log.d(DEBUG_TAG, "writing osm change with changesetid " + changeSetId);
        XmlSerializer serializer = XmlPullParserFactory.newInstance().newSerializer();
        serializer.setOutput(outputStream, UTF_8);
        serializer.startDocument(UTF_8, null);
        serializer.startTag(null, OSM_CHANGE);
        serializer.attribute(null, GENERATOR, generator);
        serializer.attribute(null, VERSION, VERSION_0_6);

        List<Node> createdNodes = new ArrayList<>();
        List<Node> modifiedNodes = new ArrayList<>();
        List<Node> deletedNodes = new ArrayList<>();
        List<Way> createdWays = new ArrayList<>();
        List<Way> modifiedWays = new ArrayList<>();
        List<Way> deletedWays = new ArrayList<>();
        List<Relation> createdRelations = new ArrayList<>();
        List<Relation> modifiedRelations = new ArrayList<>();
        List<Relation> deletedRelations = new ArrayList<>();

        for (Node elem : storage.getNodes()) {
            Log.d(DEBUG_TAG, "node added to list for upload, id " + elem.getOsmId());
            switch (elem.state) {
            case OsmElement.STATE_CREATED:
                createdNodes.add(elem);
                break;
            case OsmElement.STATE_MODIFIED:
                modifiedNodes.add(elem);
                break;
            case OsmElement.STATE_DELETED:
                deletedNodes.add(elem);
                break;
            default:
                logNotModified(elem);
                continue;
            }
            count++;
            if (count >= maxChanges) {
                break;
            }
        }
        if (count < maxChanges) {
            for (Way elem : storage.getWays()) {
                Log.d(DEBUG_TAG, "way added to list for upload, id " + elem.osmId);
                switch (elem.state) {
                case OsmElement.STATE_CREATED:
                    createdWays.add(elem);
                    break;
                case OsmElement.STATE_MODIFIED:
                    modifiedWays.add(elem);
                    break;
                case OsmElement.STATE_DELETED:
                    deletedWays.add(elem);
                    break;
                default:
                    logNotModified(elem);
                    continue;
                }
                count++;
                if (count >= maxChanges) {
                    break;
                }
            }
        }
        if (count < maxChanges) {
            for (Relation elem : storage.getRelations()) {
                Log.d(DEBUG_TAG, "relation added to list for upload, id " + elem.osmId);
                switch (elem.state) {
                case OsmElement.STATE_CREATED:
                    createdRelations.add(elem);
                    break;
                case OsmElement.STATE_MODIFIED:
                    modifiedRelations.add(elem);
                    break;
                case OsmElement.STATE_DELETED:
                    deletedRelations.add(elem);
                    break;
                default:
                    logNotModified(elem);
                    continue;
                }
                count++;
                if (count >= maxChanges) {
                    break;
                }
            }
        }
        if (!createdRelations.isEmpty()) {
            // sort the relations so that children come first, will not handle loops and similar brokenness
            Collections.sort(createdRelations, relationOrder);
        }
        if (!modifiedRelations.isEmpty()) {
            // sort the relations so that children come first, will not handle loops and similar brokenness
            Collections.sort(modifiedRelations, relationOrder);
        }
        if (!deletedRelations.isEmpty()) {
            // sort the relations so that parents come first, will not handle loops and similar brokenness
            Collections.sort(deletedRelations, (r1, r2) -> {
                if (r1.hasParentRelation(r2)) {
                    return 1;
                }
                if (r2.hasParentRelation(r1)) {
                    return -1;
                }
                return 0;
            });
        }

        // NOTE as deleted elements cannot be referenced we need to undelete them in MODIFY elements before we reference
        // them, this will not always work for relations, see below
        serializeCreatedElements(serializer, changeSetId, createdNodes);
        serializeModifiedElements(serializer, changeSetId, modifiedNodes);

        serializeCreatedElements(serializer, changeSetId, createdWays);
        serializeModifiedElements(serializer, changeSetId, modifiedWays);

        // if a newly created relation references deleted relations, they would need to be undeleted in a separate pass
        serializeCreatedElements(serializer, changeSetId, createdRelations);
        serializeModifiedElements(serializer, changeSetId, modifiedRelations);

        // delete in opposite order
        if (!deletedNodes.isEmpty() || !deletedWays.isEmpty() || !deletedRelations.isEmpty()) {
            serializer.startTag(null, DELETE);
            for (OsmElement elem : deletedRelations) {
                elem.toXml(serializer, changeSetId);
            }
            for (OsmElement elem : deletedWays) {
                elem.toXml(serializer, changeSetId);
            }
            for (OsmElement elem : deletedNodes) {
                elem.toXml(serializer, changeSetId);
            }
            serializer.endTag(null, DELETE);
        }

        serializer.endTag(null, OSM_CHANGE);
        serializer.endDocument();
    }

    /**
     * Serialize a MODIFY section
     * 
     * @param <T> type of element to serialize
     * @param serializer the serializer
     * @param changeSetId the changeset id
     * @param modifiedElements the list of elements
     * @throws IOException if serializing fails
     */
    private static <T extends OsmElement> void serializeModifiedElements(@NonNull XmlSerializer serializer, @NonNull Long changeSetId,
            @NonNull List<T> modifiedElements) throws IOException {
        if (!modifiedElements.isEmpty()) {
            serializer.startTag(null, MODIFY);
            for (OsmElement elem : modifiedElements) {
                elem.toXml(serializer, changeSetId);
            }
            serializer.endTag(null, MODIFY);
        }
    }

    /**
     * Serialize a CREATE section
     * 
     * @param <T> type of element to serialize
     * @param serializer the serializer
     * @param changeSetId the changeset id
     * @param createdElements the list of elements
     * @throws IOException if serializing fails
     */
    private static <T extends OsmElement> void serializeCreatedElements(@NonNull XmlSerializer serializer, @NonNull Long changeSetId,
            @NonNull List<T> createdElements) throws IOException {
        if (!createdElements.isEmpty()) {
            serializer.startTag(null, CREATE);
            for (OsmElement elem : createdElements) {
                elem.toXml(serializer, changeSetId);
            }
            serializer.endTag(null, CREATE);
        }
    }

    /**
     * Log that the element hasn't been modified
     * 
     * @param elem the OsmElement
     */
    private static void logNotModified(@NonNull OsmElement elem) {
        Log.d(DEBUG_TAG, elem.getName() + " id " + elem.getOsmId() + " not modified");
    }

    /**
     * Writes currentStorage + deleted objects to an OutputStream in JOSM format.
     * 
     * Output is sorted as suggested by Jochen Topf
     * 
     * @param current a Storage object with the undeleted elements
     * @param api a Storage object with the changed and deleted elements, if null deleted objects will not be written
     * @param outputStream the stream we are writing to
     * @param generator a String for the generator attribute
     * @throws XmlPullParserException on a parser error
     * @throws IllegalArgumentException on a parser error
     * @throws IllegalStateException on a parser error
     * @throws IOException if writing to the OutputStream fails
     */
    public static void write(@NonNull Storage current, @Nullable Storage api, @NonNull OutputStream outputStream, @NonNull String generator)
            throws XmlPullParserException, IllegalArgumentException, IllegalStateException, IOException {
        XmlSerializer serializer = XmlPullParserFactory.newInstance().newSerializer();
        serializer.setOutput(outputStream, UTF_8);
        serializer.startDocument(UTF_8, null);
        serializer.startTag(null, OSM);
        serializer.attribute(null, GENERATOR, generator);
        serializer.attribute(null, VERSION, VERSION_0_6);
        serializer.attribute(null, JOSM_UPLOAD, TRUE);

        /**
         * Comparator to avoid unpleasant surprises when processing unsorted OSM data with osmium and tools based on it
         */
        final Comparator<OsmElement> sortItLikeJochen = (e1, e2) -> {
            long id1 = e1.getOsmId();
            long id2 = e2.getOsmId();
            if ((id1 < 0 && id2 > 0) || (id1 > 0 && id2 < 0)) { // signs different
                return Long.compare(id1, id2);
            }
            return Long.compare(Math.abs(id1), Math.abs(id2));
        };

        List<Node> saveNodes = new ArrayList<>(current.getNodes());
        List<Way> saveWays = new ArrayList<>(current.getWays());
        List<Relation> saveRelations = new ArrayList<>(current.getRelations());

        if (api != null) {
            for (Node elem : api.getNodes()) {
                if (elem.state == OsmElement.STATE_DELETED) {
                    saveNodes.add(elem);
                }
            }
            for (Way elem : api.getWays()) {
                if (elem.state == OsmElement.STATE_DELETED) {
                    saveWays.add(elem);
                }
            }
            for (Relation elem : api.getRelations()) {
                if (elem.state == OsmElement.STATE_DELETED) {
                    saveRelations.add(elem);
                }
            }
        }

        //
        for (BoundingBox b : current.getBoundingBoxes()) {
            b.toJosmXml(serializer);
        }

        Collections.sort(saveNodes, sortItLikeJochen);
        Collections.sort(saveWays, sortItLikeJochen);
        Collections.sort(saveRelations, sortItLikeJochen);

        if (!saveNodes.isEmpty()) {
            for (OsmElement elem : saveNodes) {
                elem.toJosmXml(serializer);
            }
        }
        if (!saveWays.isEmpty()) {
            for (OsmElement elem : saveWays) {
                elem.toJosmXml(serializer);
            }
        }
        if (!saveRelations.isEmpty()) {
            for (OsmElement elem : saveRelations) {
                elem.toJosmXml(serializer);
            }
        }

        serializer.endTag(null, OSM);
        serializer.endDocument();
    }
}
