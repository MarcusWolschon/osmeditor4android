package de.blau.android.osm;

import static de.blau.android.util.Geometry.isInside;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.R;
import de.blau.android.util.Snack;
import de.blau.android.util.Util;

public final class RelationUtils {
    private static final String DEBUG_TAG = RelationUtils.class.getSimpleName();

    /**
     * Private constructor to inhibit instantation
     */
    private RelationUtils() {
        // empty
    }

    /**
     * Try to determine if rings are outer or inner rings
     * 
     * This simply tests one node from a ring if it is inside another ring, this can go wrong in multiple ways. This
     * method doesn't save the relation nor create an undo checkpoint
     * 
     * @param context (optional) Android Context for error messages
     * @param origMembers List of RelationMembers
     * @param force overwrite existing roles
     * 
     * @return a List of RelationMembers with inner / outer role set as far as could be determined
     */
    public static List<RelationMember> setMultipolygonRoles(@Nullable Context context, @NonNull List<RelationMember> origMembers, boolean force) {
        List<RelationMember> sortedMembers = Util.sortRelationMembers(origMembers);
        List<RelationMember> other = new ArrayList<>();
        List<List<RelationMember>> rings = new ArrayList<>();
        List<List<RelationMember>> partialRings = new ArrayList<>();
        List<RelationMember> currentRing = null;
        Way previousRingSegment = null;
        for (RelationMember rm : sortedMembers) {
            if (rm.downloaded() && Way.NAME.equals(rm.getType())) {
                Way currentRingSegment = ((Way) rm.getElement());
                boolean closed = currentRingSegment.isClosed();
                if (currentRing == null) { // start ring
                    currentRing = new ArrayList<>();
                    currentRing.add(rm);
                    if (closed) {
                        rings.add(currentRing);
                        currentRing = null;
                    }
                } else if (closed) {
                    // incomplete ring
                    partialRings.add(currentRing);
                    currentRing = new ArrayList<>();
                    currentRing.add(rm);
                    rings.add(currentRing);
                    currentRing = null;
                } else {
                    final Node currentFirstNode = currentRingSegment.getFirstNode();
                    final Node currentLastNode = currentRingSegment.getLastNode();
                    final Node previousFirstNode = previousRingSegment.getFirstNode();
                    final Node previousLastNode = previousRingSegment.getLastNode();
                    if (currentFirstNode.equals(previousFirstNode) || currentFirstNode.equals(previousLastNode) || currentLastNode.equals(previousFirstNode)
                            || currentLastNode.equals(previousLastNode)) {
                        currentRing.add(rm);
                        final Way firstSegment = (Way) currentRing.get(0).getElement();
                        final Node firstFirstNode = firstSegment.getFirstNode();
                        final Node firstLastNode = firstSegment.getLastNode();
                        if (firstFirstNode.equals(currentFirstNode) || firstFirstNode.equals(currentLastNode) || firstLastNode.equals(currentFirstNode)
                                || firstLastNode.equals(currentLastNode)) {
                            rings.add(currentRing);
                            currentRing = null;
                        }
                    } else { // incomplete ring, restart
                        partialRings.add(currentRing);
                        currentRing = new ArrayList<>();
                        currentRing.add(rm);
                    }
                }
                previousRingSegment = currentRingSegment;
            } else {
                other.add(rm);
            }
        }
        final int ringCount = rings.size();
        List<List<RelationMember>> rings2 = new ArrayList<>(rings);
        for (int i = 0; i < ringCount; i++) {
            List<RelationMember> ring = rings.get(i);
            // this avoids iterating over rings we have already processed at the price of creating a shallow copy of
            // rings2
            for (List<RelationMember> ring2 : new ArrayList<>(rings2)) {
                if (ring2.equals(ring)) {
                    continue;
                }
                Node ring2Node = ((Way) ring2.get(0).getElement()).getFirstNode();
                try {
                    if (isInside(getNodesForRing(ring).toArray(new Node[ring.size()]), ring2Node)) {
                        setRole(context, Tags.ROLE_OUTER, ring, force);
                        setRole(context, Tags.ROLE_INNER, ring2, force);
                        rings2.remove(ring);
                        rings2.remove(ring2);
                    } else {
                        Node ringNode = ((Way) ring.get(0).getElement()).getFirstNode();
                        if (isInside(getNodesForRing(ring2).toArray(new Node[ring2.size()]), ringNode)) {
                            setRole(context, Tags.ROLE_INNER, ring, force);
                            setRole(context, Tags.ROLE_OUTER, ring2, force);
                            rings2.remove(ring);
                            rings2.remove(ring2);
                        }
                    }
                } catch (IllegalArgumentException iae) {
                    Log.e(DEBUG_TAG, "Ring not well formed");
                }
            }
            final String role = ring.get(0).getRole();
            if (role == null || "".equals(role)) {
                setRole(context, Tags.ROLE_OUTER, ring, true);
                rings2.remove(ring);
            }
        }
        List<RelationMember> result = new ArrayList<>();
        for (List<RelationMember> ring : rings) {
            result.addAll(ring);
        }
        for (List<RelationMember> ring : partialRings) {
            result.addAll(ring);
        }
        if (!partialRings.isEmpty()) {
            if (context != null) {
                Snack.toastTopWarning(context, R.string.toast_multipolygon_has_incomplete_rings);
            }
            Log.w(DEBUG_TAG, "Incomplete multi-polgon rings");
        }
        result.addAll(other);
        return result;
    }

    /**
     * Set a role value for all members in a List
     * 
     * @param context an Android Context or null
     * @param role the role value to set
     * @param members the List
     * @param force if true overwrite existing roles
     */
    private static void setRole(@Nullable Context context, @NonNull String role, @NonNull List<RelationMember> members, boolean force) {
        boolean warningShown = false;
        for (RelationMember member : members) {
            String current = member.getRole();
            if (current != null && !"".equals(current) && !role.equals(current)) {
                if (force) {
                    Log.w(DEBUG_TAG, "Changing role from " + current + " to " + role);
                } else {
                    if (context != null && !warningShown) {
                        warningShown = true;
                        Snack.toastTopWarning(context, R.string.toast_multipolygon_inconsistent_roles);
                    }
                    continue; // skip this one
                }
            }
            member.setRole(role);
        }
    }

    /**
     * Create a list of Nodes for the ring ordered correctly
     * 
     * @param ring the input ring
     * @return a List of Nodes
     */
    private static List<Node> getNodesForRing(@NonNull List<RelationMember> ring) {
        final Way firstRingWay = (Way) ring.get(0).getElement();
        if (firstRingWay.isClosed()) {
            List<Node> nodes = firstRingWay.getNodes();
            return nodes.subList(0, nodes.size() - 1); // de-dup last node
        }
        List<Node> result = new ArrayList<>();
        List<Node> nodes = new ArrayList<>(firstRingWay.getNodes());
        final int ringNodeCount = ring.size();
        for (int i = 0; i < ringNodeCount; i++) {
            List<Node> nextNodes = new ArrayList<>(((Way) ring.get((i + 1) % ringNodeCount).getElement()).getNodes());
            // order of nodes is important
            Node firstNode = nodes.get(0);
            if (firstNode.equals(nextNodes.get(0)) || firstNode.equals(nextNodes.get(nextNodes.size() - 1))) {
                Collections.reverse(nodes);
            }
            result.addAll(nodes.subList(0, nodes.size() - 1)); // de-dup last node
            nodes = nextNodes;
        }
        return result;
    }

    /**
     * Move tags from outer rings to Relation
     * 
     * This moves all tags common to the outers (untagged outers are not considered) to the Relation
     * 
     * Shoule be wrapped in an undo checkpoint
     * 
     * @param delegator the relevant StorageDelegator instance
     * @param relation the MP Relation
     */
    public static void moveOuterTags(@NonNull StorageDelegator delegator, @NonNull Relation relation) {
        Log.d(DEBUG_TAG, "moveOuterTags");
        List<RelationMember> outers = relation.getMembersWithRole(Tags.ROLE_OUTER);

        // count the occurrences of the tags on the outers
        int targetCount = outers.size();
        Map<String, Integer> tagsCount = new HashMap<>();
        for (RelationMember outer : outers) {
            if (outer.downloaded()) {
                if (outer.getElement().hasTags()) {
                    Log.d(DEBUG_TAG, "Processing tags for " + outer.getElement());
                    for (Entry<String, String> tag : outer.getElement().getTags().entrySet()) {
                        String key = tag.getKey() + ">" + tag.getValue(); // > should not appear literally in any tag
                        Log.d(DEBUG_TAG, "Tag " + key);
                        Integer count = tagsCount.get(key);
                        if (count == null) {
                            count = 1;
                        } else {
                            count++;
                        }
                        tagsCount.put(key, count);
                    }
                } else {
                    targetCount--;
                }
            } else {
                targetCount--;
            }
        }
        Log.d(DEBUG_TAG, "target count " + targetCount);
        // remove anything that isn't common to all outers with tags
        for (Entry<String, Integer> c : new HashSet<>(tagsCount.entrySet())) {
            Integer count = c.getValue();
            Log.d(DEBUG_TAG, "tag " + c.getKey() + " count " + count);
            if (count == null || count != targetCount) {
                tagsCount.remove(c.getKey());
            }
        }
        Log.d(DEBUG_TAG, "tags to move " + tagsCount.size());

        // create the final set of tags that is moved to the relation
        Map<String, String> outerTags = new TreeMap<>();
        for (String key : tagsCount.keySet()) {
            String[] tag = key.split("\\>");
            if (tag.length == 2) {
                outerTags.put(tag[0], tag[1]);
            } else {
                Log.e(DEBUG_TAG, "Couldn't split " + key);
            }
        }

        // now remove tags from outers
        for (RelationMember outer : outers) {
            if (outer.downloaded()) {
                Map<String, String> tags = new TreeMap<>(outer.getElement().getTags());
                for (Entry<String, String> ot : outerTags.entrySet()) {
                    if (ot.getValue().equals(tags.get(ot.getKey()))) {
                        tags.remove(ot.getKey());
                    }
                }
                // set anything that is left
                delegator.setTags(outer.getElement(), tags);
            }
        }
        // add existing relation tags
        outerTags.putAll(relation.getTags());
        delegator.setTags(relation, outerTags);
    }

    /**
     * Add a type tag to existing tags
     * 
     * @param value the type to set
     * @param original the existing tags
     * @return a new Map of the tags
     */
    public static Map<String, String> addTypeTag(@NonNull String value, @NonNull Map<String, String> original) {
        Map<String, String> tags = new TreeMap<>(original);
        tags.put(Tags.KEY_TYPE, value);
        return tags;
    }
}
