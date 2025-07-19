package de.blau.android.osm;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.osm.UndoStorage.UndoElement;
import de.blau.android.osm.UndoStorage.UndoNode;
import de.blau.android.osm.UndoStorage.UndoRelation;
import de.blau.android.osm.UndoStorage.UndoWay;

/**
 * Provide reading and writing data files in OSM and JOSM format
 * 
 * @author simon
 *
 */
public final class OsmXml {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, OsmXml.class.getSimpleName().length());
    private static final String DEBUG_TAG = OsmXml.class.getSimpleName().substring(0, TAG_LEN);

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
    // augmented diff constants
    private static final String OSM_AUGMENTED_DIFF = "osmAugmentedDiff";
    private static final String NEW                = "new";
    private static final String OLD                = "old";
    private static final String TYPE               = "type";
    private static final String ACTION             = "action";

    /**
     * Try to order relations so that parent relations come later
     */
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
     * Comparator to avoid unpleasant surprises when processing unsorted OSM data with osmium and tools based on it
     */
    private static final Comparator<OsmElement> sortItLikeJochen = (e1, e2) -> {
        long id1 = e1.getOsmId();
        long id2 = e2.getOsmId();
        if ((id1 < 0 && id2 > 0) || (id1 > 0 && id2 < 0)) { // signs different
            return Long.compare(id1, id2);
        }
        return Long.compare(Math.abs(id1), Math.abs(id2));
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

        count = fillLists(storage.getNodes(), maxChanges, count, createdNodes, modifiedNodes, deletedNodes);
        if (count < maxChanges) {
            count += fillLists(storage.getWays(), maxChanges, count, createdWays, modifiedWays, deletedWays);
        }
        if (count < maxChanges) {
            fillLists(storage.getRelations(), maxChanges, count, createdRelations, modifiedRelations, deletedRelations);
        }

        sortRelations(createdRelations, modifiedRelations, deletedRelations);

        // NOTE as deleted elements cannot be referenced we need to undelete them in MODIFY elements before we reference
        // them, this will not always work for relations, see below
        serializeElements(serializer, CREATE, changeSetId, createdNodes);
        serializeElements(serializer, MODIFY, changeSetId, modifiedNodes);

        serializeElements(serializer, CREATE, changeSetId, createdWays);
        serializeElements(serializer, MODIFY, changeSetId, modifiedWays);

        // if a newly created relation references deleted relations, they would need to be undeleted in a separate pass
        serializeElements(serializer, CREATE, changeSetId, createdRelations);
        serializeElements(serializer, MODIFY, changeSetId, modifiedRelations);

        // delete in opposite order
        serializeElements(serializer, DELETE, changeSetId, deletedRelations);
        serializeElements(serializer, DELETE, changeSetId, deletedWays);
        serializeElements(serializer, DELETE, changeSetId, deletedNodes);

        serializer.endTag(null, OSM_CHANGE);
        serializer.endDocument();
    }

    /**
     * Add elements with changes to the appropriate lists
     * 
     * @param storage the element Storage
     * @param maxChanges the max number of changes allowed
     * @param count the current count
     * @param created the list for created elements
     * @param modified the list for modified elements
     * @param deleted the list for deleted elements
     * @return the number of elements added
     */
    private static <E extends OsmElement> int fillLists(@NonNull final List<E> elements, int maxChanges, int count, @NonNull final List<E> created,
            @NonNull final List<E> modified, @NonNull final List<E> deleted) {
        for (E elem : elements) {
            Log.d(DEBUG_TAG, "node added to list for upload, id " + elem.getOsmId());
            switch (elem.state) {
            case OsmElement.STATE_CREATED:
                created.add(elem);
                break;
            case OsmElement.STATE_MODIFIED:
                modified.add(elem);
                break;
            case OsmElement.STATE_DELETED:
                deleted.add(elem);
                break;
            default:
                logNotModified(elem);
                continue;
            }
            count++;
            if (count >= maxChanges) {
                return count;
            }
        }
        return count;
    }

    /**
     * Serialize elements in an action section
     * 
     * @param <T> type of element to serialize
     * @param serializer the serializer
     * @param action the action (CREATE, MODIFY, DELETE)
     * @param changeSetId the changeset id
     * @param elements the list of elements
     * @throws IOException if serializing fails
     */
    private static <T extends OsmElement> void serializeElements(@NonNull XmlSerializer serializer, @NonNull String action, @Nullable Long changeSetId,
            @NonNull List<T> elements) throws IOException {
        if (elements.isEmpty()) {
            return;
        }
        serializer.startTag(null, action);
        for (OsmElement elem : elements) {
            elem.toXml(serializer, changeSetId);
        }
        serializer.endTag(null, action);
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
     * Writes currentStorage + deleted objects to an OutputStream in (J)OSM format.
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

        List<Node> saveNodes = new ArrayList<>(current.getNodes());
        List<Way> saveWays = new ArrayList<>(current.getWays());
        List<Relation> saveRelations = new ArrayList<>(current.getRelations());

        if (api != null) {
            addDeleted(api.getNodes(), saveNodes);
            addDeleted(api.getWays(), saveWays);
            addDeleted(api.getRelations(), saveRelations);
        }

        josmSerializer(serializer, current.getBoundingBoxes());

        Collections.sort(saveNodes, sortItLikeJochen);
        Collections.sort(saveWays, sortItLikeJochen);
        Collections.sort(saveRelations, sortItLikeJochen);

        josmSerializer(serializer, saveNodes);
        josmSerializer(serializer, saveWays);
        josmSerializer(serializer, saveRelations);

        serializer.endTag(null, OSM);
        serializer.endDocument();
    }

    /**
     * Add deleted OsmElements to a list
     * 
     * @param <E> the type of element
     * @param apiElements all the elements of one type from the api storage
     * @param elements the list to add to
     */
    private static <E extends OsmElement> void addDeleted(@NonNull List<E> apiElements, @NonNull List<E> elements) {
        for (E elem : apiElements) {
            if (elem.state == OsmElement.STATE_DELETED) {
                elements.add(elem);
            }
        }
    }

    /**
     * Serialize a list of OsmElements in (J)OSM format
     * 
     * @param serializer the Serializer
     * @param elements the List of elements
     * @throws IOException if serializing fails
     */
    private static <E extends JosmXmlSerializable> void josmSerializer(XmlSerializer serializer, List<E> elements) throws IOException {
        if (!elements.isEmpty()) {
            for (E elem : elements) {
                elem.toJosmXml(serializer);
            }
        }
    }

    /**
     * Writes created/changed/deleted data to outputStream in Augmented Diff format
     * https://wiki.openstreetmap.org/wiki/Overpass_API/Augmented_Diffs
     * 
     * @param storage a Storage object with the changes
     * @param undo current UndoStorage
     * @param outputStream stream to write to
     * @param generator a String for the generator attribute
     * @throws XmlPullParserException on a parser error
     * @throws IllegalArgumentException on a parser error
     * @throws IllegalStateException on a parser error
     * @throws IOException if writing to the OutputStream fails
     */
    public static void writeAugmentedDiff(@NonNull Storage storage, @NonNull UndoStorage undo, @NonNull OutputStream outputStream, @NonNull String generator)
            throws IllegalArgumentException, IllegalStateException, IOException, XmlPullParserException {
        XmlSerializer serializer = XmlPullParserFactory.newInstance().newSerializer();
        serializer.setOutput(outputStream, UTF_8);
        serializer.startDocument(UTF_8, null);
        serializer.startTag(null, OSM_AUGMENTED_DIFF);
        serializer.attribute(null, GENERATOR, generator);

        List<Node> createdNodes = new ArrayList<>();
        Map<Long, UndoNode> oldModifiedNodes = new HashMap<>();
        List<Node> modifiedNodes = new ArrayList<>();
        Map<Long, UndoNode> oldDeletedNodes = new HashMap<>();
        List<Node> deletedNodes = new ArrayList<>();
        List<Way> createdWays = new ArrayList<>();
        Map<Long, UndoWay> oldModifiedWays = new HashMap<>();
        List<Way> modifiedWays = new ArrayList<>();
        Map<Long, UndoWay> oldDeletedWays = new HashMap<>();
        List<Way> deletedWays = new ArrayList<>();
        List<Relation> createdRelations = new ArrayList<>();
        Map<Long, UndoRelation> oldModifiedRelations = new HashMap<>();
        List<Relation> modifiedRelations = new ArrayList<>();
        Map<Long, UndoRelation> oldDeletedRelations = new HashMap<>();
        List<Relation> deletedRelations = new ArrayList<>();

        fillAugmentedLists(storage.getNodes(), undo, createdNodes, oldModifiedNodes, modifiedNodes, oldDeletedNodes, deletedNodes);
        // if we want to propagate changes upwards to parents, do that here
        fillAugmentedLists(storage.getWays(), undo, createdWays, oldModifiedWays, modifiedWays, oldDeletedWays, deletedWays);
        // if we want to propagate changes upwards to parents, do that here
        fillAugmentedLists(storage.getRelations(), undo, createdRelations, oldModifiedRelations, modifiedRelations, oldDeletedRelations, deletedRelations);
        // if we want to propagate changes upwards to parents, do that here
        
        sortRelations(createdRelations, modifiedRelations, deletedRelations);

        augmentedSerializeElements(serializer, CREATE, null, createdNodes);
        augmentedSerializeElements(serializer, MODIFY, oldModifiedNodes, modifiedNodes);

        augmentedSerializeElements(serializer, CREATE, null, createdWays);
        augmentedSerializeElements(serializer, MODIFY, oldModifiedWays, modifiedWays);

        // if a newly created relation references deleted relations, they would need to be undeleted in a separate pass
        augmentedSerializeElements(serializer, CREATE, null, createdRelations);
        augmentedSerializeElements(serializer, MODIFY, oldModifiedRelations, modifiedRelations);

        // delete in opposite order
        augmentedSerializeElements(serializer, DELETE, oldDeletedRelations, deletedRelations);
        augmentedSerializeElements(serializer, DELETE, oldDeletedWays, deletedWays);
        augmentedSerializeElements(serializer, DELETE, oldDeletedNodes, deletedNodes);

        serializer.endTag(null, OSM_AUGMENTED_DIFF);
        serializer.endDocument();
    }

    /**
     * Sort relations lists
     * 
     * @param createdRelations list with created relations
     * @param modifiedRelations list with modified relations
     * @param deletedRelations list with deleted relations
     */
    private static void sortRelations(@NonNull List<Relation> createdRelations, @NonNull List<Relation> modifiedRelations,
            @NonNull List<Relation> deletedRelations) {
        // sort the relations so that children come first, will not handle loops and similar brokenness
        if (!createdRelations.isEmpty()) {
            Collections.sort(createdRelations, relationOrder);
        }
        if (!modifiedRelations.isEmpty()) {
            Collections.sort(modifiedRelations, relationOrder);
        }
        if (!deletedRelations.isEmpty()) {
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
    }

    /**
     * Files lists and maps for producing augmented diffs
     * 
     * @param storage the OsmElement Storage
     * @param undo the current UndoStorage
     * @param created list for create element
     * @param oldModified map for initial version of modified elements
     * @param modified list for modified elements
     * @param oldDeleted map for initial version of deleted elements
     * @param deleted list for deleted elements
     */
    @SuppressWarnings("unchecked")
    private static <E extends OsmElement, U extends UndoElement> void fillAugmentedLists(@NonNull List<E> elements, @NonNull UndoStorage undo,
            @NonNull List<E> created, @NonNull Map<Long, U> oldModified, @NonNull List<E> modified, @NonNull Map<Long, U> oldDeleted,
            @NonNull List<E> deleted) {
        for (E elem : elements) {
            Log.d(DEBUG_TAG, "node added to list for upload, id " + elem.getOsmId());
            switch (elem.state) {
            case OsmElement.STATE_CREATED:
                created.add(elem);
                break;
            case OsmElement.STATE_MODIFIED:
                oldModified.put(elem.getOsmId(), (U) undo.getOriginal(elem));
                modified.add(elem);
                break;
            case OsmElement.STATE_DELETED:
                oldDeleted.put(elem.getOsmId(), (U) undo.getOriginal(elem));
                deleted.add(elem);
                break;
            default:
                logNotModified(elem);
                continue;
            }
        }
    }

    /**
     * Serialize a section for a specific action (CREATE, MODIFY, DELETE)
     * 
     * @param <T> type of element to serialize
     * @param <U> type of undo element to serialize
     * @param serializer the Serializer
     * @param action the action
     * @param oldElements map of old elements
     * @param elements the list of elements
     * @throws IOException if serializing fails
     */
    private static <T extends OsmElement, U extends UndoElement> void augmentedSerializeElements(@NonNull XmlSerializer serializer, @NonNull String action,
            @Nullable Map<Long, U> oldElements, @NonNull List<T> elements) throws IOException {
        if (elements.isEmpty()) {
            return;
        }
        for (OsmElement elem : elements) {
            serializer.startTag(null, ACTION);
            serializer.attribute(null, TYPE, action);
            if (oldElements != null) {
                U undoElement = oldElements.get(elem.getOsmId());
                if (undoElement != null) {
                    serializer.startTag(null, OLD);
                    undoElement.toAugmentedXml(serializer);
                    serializer.endTag(null, OLD);
                } else { // error

                }
            }
            serializer.startTag(null, NEW);
            elem.toAugmentedXml(serializer);
            serializer.endTag(null, NEW);
            serializer.endTag(null, ACTION);
        }
    }
}
