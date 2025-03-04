package de.blau.android.osm;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.util.Log;
import androidx.annotation.NonNull;
import de.blau.android.util.collections.LongOsmElementMap;

public final class UpdateFromChanges {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, UpdateFromChanges.class.getSimpleName().length());
    private static final String DEBUG_TAG = UpdateFromChanges.class.getSimpleName().substring(0, TAG_LEN);

    /**
     * Private constructor
     */
    private UpdateFromChanges() {
        // nothing
    }

    /**
     * Update data with unsaved changes by processes an osmChanges file
     * 
     * This will do two passes on created relations if necessary
     * 
     * @param delegator the StorageDelegator instance
     * @param changes the parsed osmChanges file
     * @return true if things worked out
     */
    public static boolean update(@NonNull StorageDelegator delegator, @NonNull Storage changes) {

        try {
            delegator.lock();
            // make temp copy of current storage (we may have to abort
            Storage tempApi = new Storage(delegator.getApiStorage());
            Storage tempCurrent = new Storage(delegator.getCurrentStorage());

            LongOsmElementMap<Node> createdNodes = new LongOsmElementMap<>(Math.max(1, tempApi.getNodeCount()));
            addCreated(createdNodes, tempApi.getNodes());

            LongOsmElementMap<Way> createdWays = new LongOsmElementMap<>(Math.max(1, tempApi.getWayCount()));
            addCreated(createdWays, tempApi.getWays());

            LongOsmElementMap<Relation> createdRelations = new LongOsmElementMap<>(Math.max(1, tempApi.getRelationCount()));
            addCreated(createdRelations, tempApi.getRelations());

            for (Node changedNode : changes.getNodes()) {
                switch (changedNode.getState()) {
                case OsmElement.STATE_DELETED:
                    tempApi.removeElement(tempApi.getNode(changedNode.getOsmId()));
                    break;
                case OsmElement.STATE_CREATED:
                    Node ourNode = null;
                    for (Node n : createdNodes) {
                        if (changedNode.getTags().equals(n.getTags()) && changedNode.getLon() == n.getLon() && changedNode.getLat() == n.getLat()) {
                            ourNode = n;
                            break;
                        }
                    }
                    if (ourNode == null) {
                        Log.e(DEBUG_TAG, "Not found Node " + changedNode.getDescription());
                        return false;
                    }
                    createdNodes.remove(ourNode.getOsmId());
                    updateElement(tempApi, changedNode, ourNode);
                    break;
                case OsmElement.STATE_MODIFIED:
                    updateElement(tempApi, changedNode, tempApi.getNode(changedNode.getOsmId()));
                    break;
                default:
                    Log.w(DEBUG_TAG, "Unchanged node " + changedNode.getDescription());
                }
            }
            for (Way changedWay : changes.getWays()) {
                switch (changedWay.getState()) {
                case OsmElement.STATE_DELETED:
                    tempApi.removeElement(tempApi.getWay(changedWay.getOsmId()));
                    break;
                case OsmElement.STATE_CREATED:
                    Way ourWay = null;
                    for (Way w : createdWays) {
                        // all created nodes will already be updated
                        if (wayEquals(changedWay, w)) {
                            ourWay = w;
                            break;
                        }
                    }
                    if (ourWay == null) {
                        Log.e(DEBUG_TAG, "Not found Way " + changedWay.getDescription());
                        return false;
                    }
                    createdWays.remove(ourWay.getOsmId());
                    updateElement(tempApi, changedWay, ourWay);
                    break;
                case OsmElement.STATE_MODIFIED:
                    updateElement(tempApi, changedWay, tempApi.getWay(changedWay.getOsmId()));
                    break;
                default:
                    Log.w(DEBUG_TAG, "Unchanged way " + changedWay.getDescription());
                }
            }
            Set<Relation> unresolvedReferences = new HashSet<>();
            for (Relation changedRelation : changes.getRelations()) {
                switch (changedRelation.getState()) {
                case OsmElement.STATE_DELETED:
                    tempApi.removeElement(tempApi.getRelation(changedRelation.getOsmId()));
                    break;
                case OsmElement.STATE_CREATED:
                    try {
                        if (!updateCreatedRelation(tempApi, createdRelations, changedRelation)) {
                            return false;
                        }
                    } catch (UnresolvedReferenceException frex) {
                        unresolvedReferences.addAll(frex.getRelations());
                    }
                    break;
                case OsmElement.STATE_MODIFIED:
                    updateElement(tempApi, changedRelation, tempApi.getRelation(changedRelation.getOsmId()));
                    break;
                default:
                    Log.w(DEBUG_TAG, "Unchanged relation " + changedRelation.getDescription());
                }
            }
            if (!unresolvedReferences.isEmpty()) {
                // 2nd pass on relations
                Log.e(DEBUG_TAG, "Changes contain relations with forward references");
                for (Relation changedRelation : unresolvedReferences) {
                    try {
                        if (!updateCreatedRelation(tempApi, createdRelations, changedRelation)) {
                            return false;
                        }
                    } catch (UnresolvedReferenceException frex) {
                        Log.e(DEBUG_TAG, "Relations continue to have unresolved references on 2nd pass");
                        return false;
                    }
                }
            }
            tempCurrent.rehash();
            tempApi.rehash();
            delegator.setStorage(tempCurrent, tempApi);

            delegator.dirty();

        } finally {
            delegator.unlock();
        }
        return true;
    }

    /**
     * Try to update the Relation corresponding to changedRelation
     * 
     * @param tempApi the storage for our elements
     * @param createdRelations all the create relations remaining
     * @param changedRelation the remote change relation
     * @throws UnresolvedReferenceException if there is a potentially matching relation with a unresolved reference
     */
    public static boolean updateCreatedRelation(Storage tempApi, LongOsmElementMap<Relation> createdRelations, Relation changedRelation)
            throws UnresolvedReferenceException {
        List<Relation> withUnresolvedReferences = new ArrayList<>();
        for (Relation r : createdRelations) {
            try {
                if (relationEquals(changedRelation, r)) {
                    createdRelations.remove(r.getOsmId());
                    updateElement(tempApi, changedRelation, r);
                    return true;
                }
            } catch (UnresolvedReferenceException frex) {
                withUnresolvedReferences.addAll(frex.getRelations());
            }
        }
        // we don't want to abort before all possible matches have been checked
        if (!withUnresolvedReferences.isEmpty()) {
            throw new UnresolvedReferenceException(withUnresolvedReferences);
        }
        Log.e(DEBUG_TAG, "Not found Relation " + changedRelation.getDescription());
        return false;
    }

    /**
     * Throw this if we find an non-updated reference in the relation
     */
    private static class UnresolvedReferenceException extends Exception {
        private final List<Relation> relations;

        private static final long serialVersionUID = 1L;

        public UnresolvedReferenceException(@NonNull Relation relation) {
            relations = new ArrayList<>();
            getRelations().add(relation);
        }

        public UnresolvedReferenceException(@NonNull List<Relation> relations) {
            this.relations = relations;
        }

        /**
         * @return the relations
         */
        List<Relation> getRelations() {
            return relations;
        }
    }

    /**
     * Add OsmElements with state created to a list
     * 
     * @param <E>
     * @param map map holding all elements
     * @param elements output list
     */
    private static <E extends OsmElement> void addCreated(LongOsmElementMap<E> map, List<E> elements) {
        for (E e : elements) {
            if (e.getState() == OsmElement.STATE_CREATED) {
                map.put(e.getOsmId(), e);
            }
        }
    }

    /**
     * Check if a way equals another without using the ids
     * 
     * @param way1 first way
     * @param way2 2nd way
     * @return true if equal
     */
    private static boolean wayEquals(@NonNull Way way1, @NonNull Way way2) {
        int nodeCountChanged = way1.nodeCount();
        int nodeCountCreated = way2.nodeCount();
        if (nodeCountChanged != nodeCountCreated) {
            return false;
        }
        if (!way1.getTags().equals(way2.getTags())) {
            return false;
        }
        List<Node> changedWayNodes = way1.getNodes();
        List<Node> createdWayNodes = way2.getNodes();
        for (int i = 0; i < nodeCountChanged; i++) {
            if (changedWayNodes.get(i).getOsmId() != createdWayNodes.get(i).getOsmId()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Update local element from element from changes
     * 
     * @param apiStorage our local store of changes elements
     * @param changedElement the changedElement from remote
     * @param ourElement out OsmElement
     */
    private static <E extends OsmElement> void updateElement(@NonNull Storage apiStorage, @NonNull E changedElement, @NonNull E ourElement) {
        apiStorage.removeElement(ourElement);
        if (ourElement.getState() == OsmElement.STATE_CREATED) {
            ourElement.setOsmId(changedElement.getOsmId());
        }
        ourElement.setOsmVersion(changedElement.getOsmVersion());
        ourElement.setState(OsmElement.STATE_UNCHANGED);
    }

    /**
     * Check if a relation equals our local copy based on tags and members (not id)
     * 
     * All members of changedRelation will have proper ids
     * 
     * @param changedRelation the remote/changed relation
     * @param ourRelation our relation
     * @return true if equal, false if not, null if reference to a not yet updated relation
     * @throws UnresolvedReferenceException if ourRelation has a member that hasn't been updated
     */
    private static boolean relationEquals(@NonNull Relation changedRelation, @NonNull Relation ourRelation) throws UnresolvedReferenceException {
        int memberCountChanged = changedRelation.getMemberCount();
        int memberCountOurs = ourRelation.getMemberCount();
        if (memberCountChanged != memberCountOurs) {
            return false;
        }
        if (!changedRelation.getTags().equals(ourRelation.getTags())) {
            return false;
        }

        List<RelationMember> membersChanged = changedRelation.getMembers();
        List<RelationMember> membersOurs = ourRelation.getMembers();
        for (int i = 0; i < memberCountChanged; i++) {
            RelationMember createdMember = membersOurs.get(i);
            RelationMember changedMember = membersChanged.get(i);
            if (createdMember.getType().equals(changedMember.getType()) && changedMember.getRole().equals(createdMember.getRole())) {
                long createdMemberId = createdMember.getRef();
                if (createdMemberId != changedMember.getRef()) {
                    if (createdMemberId < 0 && createdMember.getType().equals(Relation.NAME)) {
                        Log.w(DEBUG_TAG, "Relation " + ourRelation + " has reference to relation " + createdMemberId);
                        throw new UnresolvedReferenceException(ourRelation);
                    }
                    return false;
                }
            }
        }
        return true;
    }
}
