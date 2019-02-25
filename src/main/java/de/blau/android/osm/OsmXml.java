package de.blau.android.osm;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import de.blau.android.util.Util;

/**
 * Provide reading and writing data files in OSM and JOSM format
 * 
 * @author simon
 *
 */
public final class OsmXml {
    private static final String DEBUG_TAG = "OsmXml";


    public static final String UTF_8 = "UTF-8";

    public static final String TAG       = "tag";
    public static final String CHANGESET = "changeset";

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
        serializer.startTag(null, "osmChange");
        serializer.attribute(null, "generator", generator);
        serializer.attribute(null, "version", "0.6");

        ArrayList<OsmElement> createdNodes = new ArrayList<>();
        ArrayList<OsmElement> modifiedNodes = new ArrayList<>();
        ArrayList<OsmElement> deletedNodes = new ArrayList<>();
        ArrayList<OsmElement> createdWays = new ArrayList<>();
        ArrayList<OsmElement> modifiedWays = new ArrayList<>();
        ArrayList<OsmElement> deletedWays = new ArrayList<>();
        ArrayList<Relation> createdRelations = new ArrayList<>();
        ArrayList<Relation> modifiedRelations = new ArrayList<>();
        ArrayList<Relation> deletedRelations = new ArrayList<>();

        for (OsmElement elem : storage.getNodes()) {
            Log.d(DEBUG_TAG, "node added to list for upload, id " + elem.osmId);
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
                Log.d(DEBUG_TAG, "node id " + elem.osmId + " not modified");
                continue;
            }
            count++;
            if (count >= maxChanges) {
                break;
            }
        }
        if (count < maxChanges) {
            for (OsmElement elem : storage.getWays()) {
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
                    Log.d(DEBUG_TAG, "way id " + elem.osmId + " not modified");
                    continue;
                }
                count++;
                if (count >= maxChanges) {
                    break;
                }
            }
        }
        if (count < maxChanges) {
            for (OsmElement elem : storage.getRelations()) {
                Log.d(DEBUG_TAG, "relation added to list for upload, id " + elem.osmId);
                switch (elem.state) {
                case OsmElement.STATE_CREATED:
                    createdRelations.add((Relation) elem);
                    break;
                case OsmElement.STATE_MODIFIED:
                    modifiedRelations.add((Relation) elem);
                    break;
                case OsmElement.STATE_DELETED:
                    deletedRelations.add((Relation) elem);
                    break;
                default:
                    Log.d(DEBUG_TAG, "relation id " + elem.osmId + " not modified");
                    continue;
                }
                count++;
                if (count >= maxChanges) {
                    break;
                }
            }
        }
        Comparator<Relation> relationOrder = new Comparator<Relation>() {
            @Override
            public int compare(Relation r1, Relation r2) {
                if (r1.hasParentRelation(r2)) {
                    return -1;
                }
                if (r2.hasParentRelation(r1)) {
                    return 1;
                }
                return 0;
            }
        };
        if (!createdRelations.isEmpty()) {
            // sort the relations so that childs come first, will not handle loops and similar brokenness
            Collections.sort(createdRelations, relationOrder);
        }
        if (!modifiedRelations.isEmpty()) {
            // sort the relations so that childs come first, will not handle loops and similar brokenness
            Collections.sort(modifiedRelations, relationOrder);
        }
        if (!deletedRelations.isEmpty()) {
            // sort the relations so that parents come first, will not handle loops and similar brokenness
            Collections.sort(deletedRelations, new Comparator<Relation>() {
                @Override
                public int compare(Relation r1, Relation r2) {
                    if (r1.hasParentRelation(r2)) {
                        return 1;
                    }
                    if (r2.hasParentRelation(r1)) {
                        return -1;
                    }
                    return 0;
                }
            });
        }

        if (!createdNodes.isEmpty() || !createdWays.isEmpty() || !createdRelations.isEmpty()) {
            serializer.startTag(null, "create");
            for (OsmElement elem : createdNodes) {
                elem.toXml(serializer, changeSetId);
            }
            for (OsmElement elem : createdWays) {
                elem.toXml(serializer, changeSetId);
            }
            for (OsmElement elem : createdRelations) {
                elem.toXml(serializer, changeSetId);
            }
            serializer.endTag(null, "create");
        }

        if (!modifiedNodes.isEmpty() || !modifiedWays.isEmpty() || !modifiedRelations.isEmpty()) {
            serializer.startTag(null, "modify");
            for (OsmElement elem : modifiedNodes) {
                elem.toXml(serializer, changeSetId);
            }
            for (OsmElement elem : modifiedWays) {
                elem.toXml(serializer, changeSetId);
            }
            for (OsmElement elem : modifiedRelations) {
                elem.toXml(serializer, changeSetId);
            }
            serializer.endTag(null, "modify");
        }

        // delete in opposite order
        if (!deletedNodes.isEmpty() || !deletedWays.isEmpty() || !deletedRelations.isEmpty()) {
            serializer.startTag(null, "delete");
            for (OsmElement elem : deletedRelations) {
                elem.toXml(serializer, changeSetId);
            }
            for (OsmElement elem : deletedWays) {
                elem.toXml(serializer, changeSetId);
            }
            for (OsmElement elem : deletedNodes) {
                elem.toXml(serializer, changeSetId);
            }
            serializer.endTag(null, "delete");
        }

        serializer.endTag(null, "osmChange");
        serializer.endDocument();
    }

    /**
     * Writes currentStorage + deleted objects to an outputstream in JOSM format.
     * 
     * Output is sorted as suggested by Jochen Topf
     * 
     * @param current a Storage object with the undeleted elements
     * @param api a Storage object with the changed and deleted elements, if null deleted objects will not be written
     * @param outputStream the stream we are writing to
     * @param generator a String for the generator attribute
     * @throws XmlPullParserException
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     * @throws IOException
     */
    public static void write(@NonNull Storage current, @Nullable Storage api, @NonNull OutputStream outputStream, @NonNull String generator)
            throws XmlPullParserException, IllegalArgumentException, IllegalStateException, IOException {
        XmlSerializer serializer = XmlPullParserFactory.newInstance().newSerializer();
        serializer.setOutput(outputStream, UTF_8);
        serializer.startDocument(UTF_8, null);
        serializer.startTag(null, "osm");
        serializer.attribute(null, "generator", generator);
        serializer.attribute(null, "version", "0.6");
        serializer.attribute(null, "upload", "true");

        /**
         * Comparator to avoid unpleasant surprises when processing unsorted OSM data with osmium and tools based on it
         */
        final Comparator<OsmElement> sortItLikeJochen = new Comparator<OsmElement>() {
            @Override
            public int compare(OsmElement e1, OsmElement e2) {
                long id1 = e1.getOsmId();
                long id2 = e2.getOsmId();
                if ((id1 < 0 && id2 > 0) || (id1 > 0 && id2 < 0)) { // signs different
                    return Util.longCompare(id1, id2);
                }
                return Util.longCompare(Math.abs(id1), Math.abs(id2));
            }
        };

        ArrayList<Node> saveNodes = new ArrayList<>(current.getNodes());
        ArrayList<Way> saveWays = new ArrayList<>(current.getWays());
        ArrayList<Relation> saveRelations = new ArrayList<>(current.getRelations());

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
            for (Way elem : api.getWays()) {
                if (elem.state == OsmElement.STATE_DELETED) {
                    saveWays.add(elem);
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

        serializer.endTag(null, "osm");
        serializer.endDocument();
    }
}
