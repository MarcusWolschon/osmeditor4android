package de.blau.android.osm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstreetmap.osmosis.osmbinary.BinaryParser;
import org.openstreetmap.osmosis.osmbinary.Osmformat;
import org.openstreetmap.osmosis.osmbinary.Osmformat.DenseInfo;
import org.openstreetmap.osmosis.osmbinary.Osmformat.DenseNodes;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import de.blau.android.exception.UnsupportedFormatException;

/**
 * Parse OSM data in PBF format
 */
public class OsmPbfParser extends BinaryParser {
    private static final String DEBUG_TAG = "OsmPbfParser";

    private static final String NO_VERSION_INFORMATION_AVAILABLE = "no version information available";

    final Storage     storage;
    final BoundingBox box;

    /**
     * Construct a new parser
     * 
     * If storage already has elements with the same id they will not be overwritten
     * 
     * @param storage the Storage object to hold the OsmElements
     */
    public OsmPbfParser(@NonNull Storage storage) {
        this.storage = storage;
        box = null;
    }

    /**
     * /** Construct a new parser
     * 
     * If storage already has elements with the same id they will not be overwritten
     * 
     * @param storage the Storage object to hold the OsmElements
     * @param box if not null trim contents as far as possible to this bounding box (Relations are never trimmed)
     */
    public OsmPbfParser(@NonNull Storage storage, @Nullable BoundingBox box) {
        this.storage = storage;
        this.box = box;
    }

    @Override
    protected void parseRelations(List<Osmformat.Relation> relations) {

        class UnprocessedRelationMember {
            Relation       parent;
            RelationMember member;

            /**
             * Construct a container for unprocessed RelationMembers
             * 
             * @param parent the parent Relation
             * @param member the member
             */
            UnprocessedRelationMember(@NonNull Relation parent, @NonNull RelationMember member) {
                this.parent = parent;
                this.member = member;
            }
        }

        int timeStampToSeconds = date_granularity / 1000; // mostly one
        List<UnprocessedRelationMember> relFor2ndPass = new ArrayList<>();
        for (Osmformat.Relation r : relations) {
            if (!r.hasInfo()) {
                // we require version so we fail here
                throw new UnsupportedFormatException(NO_VERSION_INFORMATION_AVAILABLE);
            }
            Relation relation = OsmElementFactory.createRelation(r.getId(), (long) r.getInfo().getVersion(), r.getInfo().getTimestamp() / timeStampToSeconds,
                    OsmElement.STATE_UNCHANGED);
            long ref = 0; // Delta coded!
            int mcount = r.getMemidsCount();
            for (int i = 0; i < mcount; i++) {
                ref += r.getMemids(i);
                String role = getStringById(r.getRolesSid(i));
                String type = null;
                switch (r.getTypes(i)) {
                case NODE:
                    type = Node.NAME;
                    break;
                case WAY:
                    type = Way.NAME;
                    break;
                case RELATION:
                    type = Relation.NAME;
                    break;
                default:
                    throw new UnsupportedFormatException("Unknown relation member type " + r.getTypes(i));
                }
                RelationMember rm = new RelationMember(type, ref, role);
                OsmElement element = storage.getOsmElement(type, ref);
                if (element != null) {
                    rm.setElement(element);
                    element.addParentRelation(relation);
                } else if (Relation.NAME.equals(type)) {
                    relFor2ndPass.add(new UnprocessedRelationMember(relation, rm));
                }
                relation.addMember(rm);
            }

            int tagCount = r.getKeysCount();
            if (tagCount > 0) {
                Map<String, String> tags = new HashMap<>();
                for (int i = 0; i < tagCount; i++) {
                    tags.put(getStringById(r.getKeys(i)), (getStringById(r.getVals(i))));
                }
                if (tags != null && !tags.isEmpty()) {
                    relation.setTags(tags);
                }
            }
            storage.insertElementSafe(relation);
        }
        // 2nd pass for Relations that hadn't been parsed yet
        for (UnprocessedRelationMember urm : relFor2ndPass) {
            OsmElement element = storage.getOsmElement(urm.member.getType(), urm.member.getRef());
            if (element != null) {
                urm.member.setElement(element);
                element.addParentRelation(urm.parent);
            }
        }
    }

    @Override
    protected void parseDense(DenseNodes nodes) {
        DenseInfo denseInfo = nodes.getDenseinfo();
        if (denseInfo == null || denseInfo.getVersionCount() == 0 || denseInfo.getTimestampCount() == 0) {
            // we require version so we fail here
            throw new UnsupportedFormatException(NO_VERSION_INFORMATION_AVAILABLE);
        }
        int timeStampToSeconds = date_granularity / 1000; // mostly one
        long lastId = 0;
        long lastLat = 0;
        long lastLon = 0;
        int lastVersion = 0;
        long lastTimestamp = 0;
        int tagPointer = 0;
        for (int i = 0; i < nodes.getIdCount(); i++) {
            // delta encoded
            lastId += nodes.getId(i);
            lastLat += nodes.getLat(i);
            lastLon += nodes.getLon(i);
            lastTimestamp += denseInfo.getTimestamp(i);
            // not delta encoded
            lastVersion = denseInfo.getVersion(i);
            Node node = OsmElementFactory.createNode(lastId, (long) lastVersion, lastTimestamp / timeStampToSeconds, OsmElement.STATE_UNCHANGED,
                    parseToLatE7(lastLat), parseToLonE7(lastLon));
            Map<String, String> tags = null;
            if (nodes.getKeysValsCount() > 0) {
                while (nodes.getKeysVals(tagPointer) != 0) {
                    String key = getStringById(nodes.getKeysVals(tagPointer++));
                    String value = getStringById(nodes.getKeysVals(tagPointer++));
                    if (key != null) {
                        if (tags == null) {
                            tags = new HashMap<>();
                        }
                        tags.put(key, value);
                    }
                }
                tagPointer++;
            }
            if (tags != null && !tags.isEmpty()) {
                node.setTags(tags);
            }
            storage.insertElementSafe(node);
        }
    }

    @Override
    protected void parseNodes(List<Osmformat.Node> nodes) {
        int timeStampToSeconds = date_granularity / 1000; // mostly one
        for (Osmformat.Node n : nodes) {
            if (!n.hasInfo()) {
                // we require version so we fail here
                throw new UnsupportedFormatException(NO_VERSION_INFORMATION_AVAILABLE);
            }
            Node node = OsmElementFactory.createNode(n.getId(), (long) n.getInfo().getVersion(), n.getInfo().getTimestamp() / timeStampToSeconds,
                    OsmElement.STATE_UNCHANGED, parseToLatE7(n.getLat()), parseToLonE7(n.getLon()));
            int tagCount = n.getKeysCount();
            if (tagCount > 0) {
                Map<String, String> tags = new HashMap<>();
                for (int i = 0; i < tagCount; i++) {
                    tags.put(getStringById(n.getKeys(i)), (getStringById(n.getVals(i))));
                }
                if (!tags.isEmpty()) {
                    node.setTags(tags);
                }
            }
            storage.insertElementSafe(node);
        }
    }

    @Override
    protected void parseWays(List<Osmformat.Way> ways) {
        int timeStampToSeconds = date_granularity / 1000; // mostly one
        for (Osmformat.Way w : ways) {
            if (!w.hasInfo()) {
                // we require version so we fail here
                throw new UnsupportedFormatException(NO_VERSION_INFORMATION_AVAILABLE);
            }
            Way way = OsmElementFactory.createWay(w.getId(), (long) w.getInfo().getVersion(), w.getInfo().getTimestamp() / timeStampToSeconds,
                    OsmElement.STATE_UNCHANGED);
            long lastRef = 0;
            for (long ref : w.getRefsList()) {
                lastRef += ref;
                Node nd = storage.getNode(lastRef);
                if (nd == null) {
                    // input is referentially broken, complain rather than fixing it up
                    Log.e(DEBUG_TAG, "Way node " + lastRef + " missing, not adding way " + w.getId());
                    throw new UnsupportedFormatException("Way node " + lastRef + " missing, not adding way " + w.getId());
                }
                way.addNode(nd);
            }
            if (box != null) {
                if (storage.contains(way)) {
                    continue; // no point in doing anything
                }
                if (!way.getBounds().intersects(box)) {
                    continue; // trim before we add tags
                }
                // flag the Node as referenced for the ways we keep
                // unreferenced nodes will be removed in a later step
                // once all data has been loaded into the Storage object
                for (Node nd : way.getNodes()) {
                    storage.addNodeRef(nd.getOsmId());
                }
            }
            int tagCount = w.getKeysCount();
            if (tagCount > 0) {
                Map<String, String> tags = new HashMap<>();
                for (int i = 0; i < tagCount; i++) {
                    tags.put(getStringById(w.getKeys(i)), (getStringById(w.getVals(i))));
                }
                if (tags != null && !tags.isEmpty()) {
                    way.setTags(tags);
                }
            }
            storage.insertElementSafe(way);
        }
    }

    @Override
    protected void parse(Osmformat.HeaderBlock block) {
        if (block.hasBbox()) {
            double multiplier = .01;
            int right = (int) (block.getBbox().getRight() * multiplier);
            int left = (int) (block.getBbox().getLeft() * multiplier);
            int top = (int) (block.getBbox().getTop() * multiplier);
            int bottom = (int) (block.getBbox().getBottom() * multiplier);

            BoundingBox bounds = new BoundingBox(left, bottom, right, top);
            if (storage.isEmpty()) {
                storage.setBoundingBox(bounds);
            } else {
                storage.addBoundingBox(bounds);
            }
        }
    }

    @Override
    public void complete() {
        // do nothing
    }

    /**
     * Convert latitude to our scaled format
     * 
     * Very silly as we essentially undo scaling in the other direction
     * 
     * @param degree degree value from pbf
     * @return latitude in WGS84*1E7
     */
    private int parseToLatE7(long degree) {
        return (int) (parseLat(degree) * 1E7D);
    }

    /**
     * Convert longitude to our scaled format
     * 
     * Very silly as we essentially undo scaling in the other direction
     * 
     * @param degree degree value from pbf
     * @return longitude in WGS84*1E7
     */
    private int parseToLonE7(long degree) {
        return (int) (parseLon(degree) * 1E7D);
    }
}
