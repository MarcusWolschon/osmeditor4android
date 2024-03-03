package de.blau.android.osm;

import static de.blau.android.util.Winding.COUNTERCLOCKWISE;
import static de.blau.android.util.Winding.winding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.R;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.Coordinates;
import de.blau.android.util.Util;

/**
 * This class contains the code for merging OsmElements
 * 
 * @author simon
 *
 */
public class MergeAction {

    private static final String DEBUG_TAG = MergeAction.class.getSimpleName();

    private final StorageDelegator delegator;
    private final OsmElement       mergeInto;
    private final OsmElement       mergeFrom;
    private final List<Result>     overallResult;

    /**
     * Initialize a new MergeAction
     * 
     * Note depending on id and version mergeInto and mergeFrom may switch
     * 
     * @param delegator the StorageDelegator to use
     * @param mergeInto the OsmElement that we are going to merge into
     * @param mergeFrom the OsmElement that is going to be removed
     */
    public MergeAction(final @NonNull StorageDelegator delegator, @NonNull OsmElement mergeInto, @NonNull OsmElement mergeFrom) {
        this.delegator = delegator;
        // first determine if one of the elements already has a valid id, if it is not and other node has valid id swap
        // else check version numbers, the point of this is to preserve as much history as possible
        if (((mergeInto.getOsmId() < 0) && (mergeFrom.getOsmId() > 0)) || mergeInto.getOsmVersion() < mergeFrom.getOsmVersion()) {
            // swap
            Log.d(DEBUG_TAG, "swap into #" + mergeInto.getOsmId() + " with from #" + mergeFrom.getOsmId());
            OsmElement tmpElement = mergeInto;
            mergeInto = mergeFrom;
            mergeFrom = tmpElement;
            Log.d(DEBUG_TAG, "now into #" + mergeInto.getOsmId() + " from #" + mergeFrom.getOsmId());
        }
        this.mergeInto = mergeInto;
        this.mergeFrom = mergeFrom;
        overallResult = roleConflict(mergeInto, mergeFrom);
    }

    /**
     * Merge two nodes into one.
     * 
     * Updates ways and relations the node is a member of.
     * 
     * @return a MergeResult object with a reference to the resulting object and any issues
     * @throws OsmIllegalOperationException if merged tags are too long to be merged
     */
    @NonNull
    public List<Result> mergeNodes() throws OsmIllegalOperationException {
        Result result = new Result();
        if (mergeInto.equals(mergeFrom)) {
            result.addIssue(MergeIssue.SAMEOBJECT);
            result.setElement(mergeFrom);
            return Util.wrapInList(result);
        }
        delegator.dirty();

        // merge tags
        final Map<String, String> mergedTags = mergeTags(mergeInto, mergeFrom);
        checkForMergedTags(mergeInto.getTags(), mergeFrom.getTags(), mergedTags, result);
        delegator.setTags(mergeInto, mergedTags); // this calls onElementChange for the node

        // replace references to mergeFrom node in ways with mergeInto
        synchronized (delegator) {
            Storage currentStorage = delegator.getCurrentStorage();
            for (Way way : currentStorage.getWays((Node) mergeFrom)) {
                delegator.replaceNodeInWay((Node) mergeFrom, (Node) mergeInto, way);
            }
        }
        mergeElementsRelations(mergeInto, mergeFrom);
        // delete mergeFrom node
        delegator.removeNode((Node) mergeFrom);
        delegator.onElementChanged(null, mergeInto);
        result.setElement(mergeInto);

        overallResult.add(0, result);
        return overallResult;
    }

    /**
     * Check the merged tags for a new, that is not present in the original elements, tag value
     * 
     * @param into tags of the 1st element
     * @param from tags of the 2nd element
     * @param merged the merged tags
     * @return the merge result
     */
    @NonNull
    public static Result checkForMergedTags(@NonNull Map<String, String> into, @NonNull Map<String, String> from, @NonNull Map<String, String> merged) {
        Result result = new Result();
        checkForMergedTags(into, from, merged, result);
        return result;
    }

    /**
     * Check the merged tags for a new, that is not present in the original elements, tag value
     * 
     * @param into tags of the 1st element
     * @param from tags of the 2nd element
     * @param merged the merged tags
     * @param result the merge result
     */
    private static void checkForMergedTags(@NonNull Map<String, String> into, @NonNull Map<String, String> from, @NonNull Map<String, String> merged,
            @NonNull Result result) {
        // if merging the tags creates a new tag for a key report it
        for (Entry<String, String> m : merged.entrySet()) {
            final String key = m.getKey();
            final String intoValue = into.get(key);
            final String fromValue = from.get(key);
            // note a metric tag will have already been flagged
            if (intoValue != null && fromValue != null && !intoValue.equals(m.getValue()) && !Tags.isWayMetric(key)) {
                result.addIssue(MergeIssue.MERGEDTAGS);
                break;
            }
        }
    }

    /**
     * Merges two ways by prepending/appending all nodes from the second way to the first one, then deleting the second
     * one.
     * 
     * Updated for relation support if roles are not the same the merge will fail.
     * 
     * @return a List of MergeResult objects with a reference to the resulting object in the first one and any issues
     * @throws OsmIllegalOperationException if the ways cannot be merged
     */
    @NonNull
    public List<Result> mergeWays() throws OsmIllegalOperationException {
        Result mergeResult = new Result();

        Way w1 = (Way) mergeInto;
        Way w2 = (Way) mergeFrom;

        delegator.validateWayNodeCount(w1.nodeCount() + w2.nodeCount());

        // undo - w1 way saved here, w2 way will not be changed directly and will be saved in removeWay
        delegator.dirty();
        delegator.getUndo().save(w1);
        delegator.removeWay(w2); // have to do this here because otherwise the way will be saved with potentially
                                 // reversed tags

        List<Node> newNodes = new ArrayList<>(w2.getNodes());
        boolean atBeginning;
        List<Result> reverseResults = null;
        if (w1.getFirstNode().equals(w2.getFirstNode())) {
            // Result: f3 f2 f1 (f0=)i0 i1 i2 i3 (f0 = 0th node of w2, i1 = 1st node of w1)
            atBeginning = true;
            // check for direction dependent tags
            Map<String, String> dirTags = Reverse.getDirectionDependentTags(w2);
            if (!dirTags.isEmpty()) {
                Reverse.reverseDirectionDependentTags(w2, dirTags, true);
                mergeResult.addIssue(ReverseIssue.TAGSREVERSED);
            }
            if (w2.notReversable()) {
                mergeResult.addIssue(MergeIssue.NOTREVERSABLE);
            }
            Collections.reverse(newNodes);
            newNodes.remove(newNodes.size() - 1); // remove "last" (originally first) node after reversing
            reverseResults = delegator.reverseWayNodeTags(newNodes); // needs to happen after end node removal
        } else if (w1.getLastNode().equals(w2.getFirstNode())) {
            // Result: i0 i1 i2 i3(=f0) f1 f2 f3
            atBeginning = false;
            newNodes.remove(0);
        } else if (w1.getFirstNode().equals(w2.getLastNode())) {
            // Result: f0 f1 f2 (f3=)i0 i1 i2 i3
            atBeginning = true;
            newNodes.remove(newNodes.size() - 1);
        } else if (w1.getLastNode().equals(w2.getLastNode())) {
            // Result: i0 i1 i2 i3(=f3) f2 f1 f0
            atBeginning = false;
            // check for direction dependent tags
            Map<String, String> dirTags = Reverse.getDirectionDependentTags(w2);
            if (!dirTags.isEmpty()) {
                Reverse.reverseDirectionDependentTags(w2, dirTags, true);
                mergeResult.addIssue(ReverseIssue.TAGSREVERSED);
            }
            if (w2.notReversable()) {
                mergeResult.addIssue(MergeIssue.NOTREVERSABLE);
            }
            newNodes.remove(newNodes.size() - 1); // remove last node before reversing
            reverseResults = delegator.reverseWayNodeTags(newNodes); // needs to happen after end node removal
            Collections.reverse(newNodes);
        } else {
            throw new OsmIllegalOperationException("attempted to merge non-mergeable nodes. this is a bug.");
        }

        // merge tags (after any reversal has been done)
        Map<String, String> mergedTags = mergeTags(w1, w2);
        // special handling for metric tags
        for (Entry<String, String> entry : mergedTags.entrySet()) {
            String k = entry.getKey();
            if (Tags.isWayMetric(k)) {
                mergeResult.addIssue(MergeIssue.MERGEDMETRIC);
                String[] s = splitValue(entry.getValue());
                if (s.length >= 2) {
                    try {
                        mergedTags.put(k, Tags.KEY_DURATION.equals(k) ? Duration.toString(Duration.parse(s[0]) + Duration.parse(s[1]))
                                : Integer.toString(Integer.parseInt(s[0]) + Integer.parseInt(s[1])));
                    } catch (NumberFormatException nfe) {
                        // ignore issue will be set below
                    }
                }
            }
        }
        checkForMergedTags(w1.getTags(), w2.getTags(), mergedTags, mergeResult);
        delegator.setTags(w1, mergedTags);

        w1.addNodes(newNodes, atBeginning);
        w1.updateState(OsmElement.STATE_MODIFIED);
        synchronized (delegator) {
            delegator.getApiStorage().insertElementSafe(w1);
        }
        delegator.onElementChanged(null, w1);
        mergeElementsRelations(w1, w2);

        mergeResult.setElement(w1);

        overallResult.add(0, mergeResult);
        if (reverseResults != null) {
            overallResult.addAll(reverseResults);
        }
        return overallResult;
    }

    /**
     * Check if two elements have different roles in the same relation
     * 
     * @param o1 the first OsmElement
     * @param o2 the second OsmElement
     * @return a List (potentially empty) of issues if elements have different roles in the same relation
     */
    @NonNull
    private List<Result> roleConflict(@NonNull OsmElement o1, @NonNull OsmElement o2) {
        List<Result> result = new ArrayList<>();
        List<Relation> r1 = o1.getParentRelations() != null ? o1.getParentRelations() : new ArrayList<>();
        List<Relation> r2 = o2.getParentRelations() != null ? o2.getParentRelations() : new ArrayList<>();
        for (Relation r : r1) {
            if (!r2.contains(r)) {
                continue;
            }
            RelationMember rm1 = r.getMember(o1);
            RelationMember rm2 = r.getMember(o2);
            if (rm1 != null && rm2 != null) { // if either of these are null something is broken
                String role1 = rm1.getRole();
                String role2 = rm2.getRole();
                // noinspection StringEquality
                if ((role1 != null && role2 == null) || (role1 == null && role2 != null) || (role1 != role2 && !role1.equals(role2))) { // NOSONAR
                    Log.d(DEBUG_TAG, "role conflict between " + o1.getDescription() + " role " + role1 + " and " + o2.getDescription() + " role " + role2);
                    addRoleConflictIssue(result, r);
                }
            } else {
                String msg = "inconsistent relation membership in " + r.getOsmId() + " for " + o1.getOsmId() + " and " + o2.getOsmId();
                Log.e(DEBUG_TAG, msg);
                ACRAHelper.nocrashReport(null, msg);
                addRoleConflictIssue(result, r);
            }
        }
        return result;
    }

    /**
     * Add a role conflict issue for a specific relation to a list of Results
     * 
     * @param results the List of Result
     * @param r the Relation
     */
    private void addRoleConflictIssue(@NonNull List<Result> results, @NonNull Relation r) {
        Result roleIssue = new Result();
        roleIssue.setElement(r);
        roleIssue.addIssue(MergeIssue.ROLECONFLICT);
        results.add(roleIssue);
    }

    /**
     * Merge two closed ways
     * 
     * This will merge two closed ways in to a single polygon or multipolygon if necessary
     * 
     * Note: does not reverse direction dependent tags on nodes if the ways were reversed
     * 
     * @param map the current Map instance
     * @return a List of Result objects, the 1st one containing the merged object
     * @throws OsmIllegalOperationException if we can't complete the merge for reasons that shouldn't occur
     */
    @NonNull
    public List<Result> mergeSimplePolygons(@NonNull de.blau.android.Map map) throws OsmIllegalOperationException {
        Result mergeResult = new Result();

        Way p1 = (Way) mergeInto;
        Way p2 = (Way) mergeFrom;
        // determine max number of nodes we will end up with
        // while this double counts common nodes in degenerate cases
        // they may remain twice in the output polygon
        int nodeTotal = p1.getNodes().size() + p2.getNodes().size();
        delegator.validateWayNodeCount(nodeTotal);

        // undo - mergeInto way saved here, mergeFrom way will not be changed directly and will be saved in removeWay
        delegator.dirty();
        synchronized (delegator) {
            UndoStorage undo = delegator.getUndo();
            undo.save(p1);
            undo.save(p2);
        }

        List<List<Node>> outputRings = new ArrayList<>();
        List<Node> outputRing = new ArrayList<>();
        List<Node> currentInputRing = new ArrayList<>(p1.getNodes());
        List<Node> otherInputRing = new ArrayList<>(p2.getNodes());

        // in the 1st pass we create the outer merged polygon and handle disjunct polygons
        // after that we try to convert any left over nodes in to inners
        boolean firstpass = true;

        boolean reversed = false;

        // make winding clockwise for both rings
        if (!currentInputRing.isEmpty() && winding(currentInputRing) == COUNTERCLOCKWISE) {
            reversed = true; // so that we can undo this later
            Collections.reverse(currentInputRing);
        }
        if (!otherInputRing.isEmpty() && winding(otherInputRing) == COUNTERCLOCKWISE) {
            // check for direction dependent tags
            Map<String, String> dirTags = Reverse.getDirectionDependentTags(p2);
            if (!dirTags.isEmpty()) {
                Reverse.reverseDirectionDependentTags(p2, dirTags, true);
                mergeResult.addIssue(ReverseIssue.TAGSREVERSED);
            }
            if (p1.notReversable()) {
                mergeResult.addIssue(MergeIssue.NOTREVERSABLE);
            }
            Collections.reverse(otherInputRing);
        }

        int currentSize = currentInputRing.size();
        int otherSize = otherInputRing.size();
        while (currentSize >= 2 || otherSize >= 2) {
            // find a node to start that isn't a member of both
            Node startNode = firstpass ? findInitalStartNode(map, currentInputRing, otherInputRing) : findStartNode(currentInputRing, otherInputRing);
            if (startNode == null) {
                // switch rings and retry if that fails the rings are identical (shouldn't happen) or disjunct
                startNode = firstpass ? findInitalStartNode(map, otherInputRing, currentInputRing) : findStartNode(otherInputRing, currentInputRing);
                if (startNode != null) {
                    List<Node> tempRing = currentInputRing;
                    currentInputRing = otherInputRing;
                    otherInputRing = tempRing;
                } else {
                    Log.w(DEBUG_TAG, "Disjunct rings");
                    if (isClosedRing(currentInputRing)) {
                        outputRings.add(new ArrayList<>(currentInputRing));
                        currentInputRing.clear();
                        otherInputRing.clear();
                    }
                    break; // finished
                }
            }

            List<Node> keep = new ArrayList<>();
            if (currentInputRing.size() < 3) {
                // switch
                List<Node> tempRing = currentInputRing;
                currentInputRing = otherInputRing;
                otherInputRing = tempRing;
            }

            int i = 0;
            i = currentInputRing.indexOf(startNode);
            outputRing.add(startNode);

            Node currentNode = null;
            Node previousNode = startNode;

            Log.d(DEBUG_TAG, "startNode " + startNode + " index " + i + " currentInputRing size " + currentInputRing.size() + " otherInputRing size "
                    + otherInputRing.size());
            i = (i + 1) % currentInputRing.size();
            while (startNode != currentNode && (outputRing.size() <= nodeTotal + 1)) { // safety catch
                currentNode = currentInputRing.get(i);
                if (currentNode != previousNode) {
                    outputRing.add(currentNode);
                }
                if (otherInputRing.contains(currentNode)) {
                    Node nextNodeCurrent = getNextNode(currentInputRing, i);
                    int j = otherInputRing.indexOf(currentNode);
                    Node nextNodeOther = getNextNode(otherInputRing, j);
                    if (nextNodeOther == null) {
                        Collections.reverse(otherInputRing);
                        j = otherInputRing.indexOf(currentNode);
                        nextNodeOther = getNextNode(otherInputRing, j);
                    }
                    Log.d(DEBUG_TAG, " next current " + nextNodeCurrent + " other " + nextNodeOther);
                    if (nextNodeCurrent == null && nextNodeOther == null) {
                        Log.e(DEBUG_TAG, "inconsistent state");
                        break;
                    }
                    // any inners from non-overlapping polygons should have counter clockwise winding so the same
                    // criteria should work
                    if (nextNodeOther != null && (nextNodeCurrent == null || compareAngles(map, previousNode, currentNode, nextNodeCurrent, nextNodeOther))) {
                        Log.d(DEBUG_TAG, "switch rings");
                        List<Node> tempRing = currentInputRing;
                        currentInputRing = otherInputRing;
                        otherInputRing = tempRing;
                        i = j;
                    }
                }
                previousNode = currentNode;
                i = (i + 1) % currentInputRing.size();
            }

            if (startNode.equals(currentNode)) {
                // finished ring
                outputRings.add(new ArrayList<>(outputRing)); // store shallow copy
            } else {
                Log.w(DEBUG_TAG, "Incomplete ring discarded");
            }

            // remove all accounted for nodes from the two rings
            for (Node n : outputRing) {
                if (keep.contains(n)) {
                    continue;
                }
                while (currentInputRing.remove(n)) {
                    // empty
                }
                while (otherInputRing.remove(n)) {
                    // empty
                }
            }

            currentSize = currentInputRing.size();
            otherSize = otherInputRing.size();

            if (currentSize == 0 || otherSize == 0) {
                if (currentSize > 2 && isClosedRing(currentInputRing)) {
                    outputRings.add(new ArrayList<>(currentInputRing));
                }
                if (otherSize > 2 && isClosedRing(otherInputRing)) {
                    outputRings.add(new ArrayList<>(otherInputRing));
                }
                currentInputRing.clear();
                otherInputRing.clear();
            }

            outputRing.clear(); // restart
            firstpass = false;
        }

        int ringCount = outputRings.size();
        Log.d(DEBUG_TAG, "ring count " + ringCount);
        OsmElement result = null;
        if (ringCount >= 1) {
            List<Node> ring = outputRings.get(0);
            if (reversed) { // undo reverse
                Collections.reverse(ring);
            }
            p1.getNodes().clear();
            p1.getNodes().addAll(ring);
            p1.updateState(OsmElement.STATE_MODIFIED);
            delegator.insertElementSafe(p1);
            if (ringCount == 1) {
                result = p1;
                final Map<String, String> mergedTags = mergeTags(p1, p2);
                checkForMergedTags(p1.getTags(), p2.getTags(), mergedTags, mergeResult);
                delegator.setTags(result, mergedTags);
                mergeElementsRelations(p1, p2);
                delegator.removeWay(p2);
            } else {
                // its a MP
                List<RelationMember> members = new ArrayList<>();
                members.add(new RelationMember("", p1));
                // reuse p2
                ring = outputRings.get(1);
                p2.getNodes().clear();
                p2.getNodes().addAll(ring);
                p2.updateState(OsmElement.STATE_MODIFIED);
                delegator.insertElementSafe(p2);
                members.add(new RelationMember("", p2));
                // any further rings
                for (int i = 2; i < ringCount; i++) {
                    Way newWay = delegator.getFactory().createWayWithNewId();
                    newWay.getNodes().addAll(outputRings.get(i));
                    delegator.insertElementSafe(newWay);
                    members.add(new RelationMember("", newWay));
                }
                RelationUtils.setMultipolygonRoles(null, members, true);
                result = delegator.createAndInsertRelationFromMembers(members);
                Map<String, String> tags = RelationUtils.addTypeTag(Tags.VALUE_MULTIPOLYGON, result.getTags());
                delegator.setTags(result, tags);
                synchronized (delegator) {
                    delegator.getUndo().createCheckpoint(map.getContext().getString(R.string.undo_action_move_tags));
                }
                RelationUtils.moveOuterTags(delegator, (Relation) result);
            }
        } else {
            // something went really wrong
            Log.d(DEBUG_TAG, "ring count " + ringCount);
            throw new OsmIllegalOperationException("attempted to merge non-mergeable polygon ways. this is a bug.");
        }

        // remove any left over nodes
        removeUntaggedNodes(currentInputRing);
        removeUntaggedNodes(otherInputRing);

        mergeResult.setElement(result);

        overallResult.add(0, mergeResult);
        return overallResult;
    }

    /**
     * Delete nodes, if they have neither tags nor are way nodes
     * 
     * @param list the List of Nodes
     */
    private void removeUntaggedNodes(@NonNull List<Node> list) {
        synchronized (delegator) {
            Storage currentStorage = delegator.getCurrentStorage();
            for (Node n : list) {
                if (!n.hasTags() && currentStorage.getWays(n).isEmpty()) {
                    delegator.removeNode(n);
                }
            }
        }
    }

    /**
     * Get the next node from a ring, taking into account if the ring is closed or not
     * 
     * @param ring the ring
     * @param i the current index
     * @return the next Node of null
     */
    @Nullable
    private Node getNextNode(@NonNull List<Node> ring, int i) {
        final int size = ring.size();
        if (isClosedRing(ring)) { // closed
            int newI = (i + 1) % size;
            newI = newI == 0 ? 1 : newI; // skip dup node
            return ring.get(newI);
        } else {
            if (i < size - 1) {
                return ring.get(i + 1);
            }
            return null;
        }
    }

    /**
     * Compares two angles defined by four nodes
     * 
     * @param map the current map instance
     * @param a Node a (start)
     * @param b Node b (start)
     * @param c1 Node c1 (end 1)
     * @param c2 Node c2 (end 2)
     * @return true if the first angle is larger than the 2nd
     */
    private boolean compareAngles(@NonNull de.blau.android.Map map, @NonNull Node a, @NonNull Node b, @NonNull Node c1, @NonNull Node c2) {
        Coordinates[] c = Coordinates.nodeListToCoordinateArray(map.getWidth(), map.getHeight(), map.getViewBox(), Arrays.asList(a, b, c1, c2));
        Coordinates p = c[0].subtract(c[1]);
        double angle1 = Coordinates.angle(p, c[2].subtract(c[1]));
        angle1 = angle1 < 0 ? angle1 + 2 * Math.PI : angle1;
        double angle2 = Coordinates.angle(p, c[3].subtract(c[1]));
        angle2 = angle2 < 0 ? angle2 + 2 * Math.PI : angle2;
        return angle2 < angle1;
    }

    /**
     * Check if first Node and last Node of a list are the same
     * 
     * @param list the List
     * @return true if the condition is true
     */
    private boolean isClosedRing(@NonNull List<Node> list) {
        return list.size() > 3 && (list.get(0) == list.get(list.size() - 1));
    }

    /**
     * Find a Node in list1 that is not in list2
     * 
     * @param list1 the 1st List of Node
     * @param list2 the 2nd List of Node
     * @return the Node or null if there is none
     */
    @Nullable
    private Node findStartNode(@NonNull List<Node> list1, @NonNull List<Node> list2) {
        Node startNode = null;
        for (Node n : list1) {
            if (!list2.contains(n)) {
                startNode = n;
                break;
            }
        }
        return startNode;
    }

    /**
     * Find a Node in ring1 that is on the outer part of the ring
     * 
     * @param map the current Map instance
     * @param ring1 the 1st List of Node
     * @param ring2 the 2nd List of Node
     * @return the Node or null if there is none
     */
    @Nullable
    private Node findInitalStartNode(@NonNull de.blau.android.Map map, @NonNull List<Node> ring1, @NonNull List<Node> ring2) {
        List<Node> allNodes = new ArrayList<>(ring1);
        allNodes.addAll(ring2);
        Node maxX = allNodes.get(0);
        for (Node n : allNodes) {
            if (n.getLon() > maxX.getLon()) {
                maxX = n;
            }
        }
        if (ring1.contains(maxX) && !ring2.contains(maxX)) {
            return maxX;
        }

        // walk around the outer till we find a suitable Node
        List<Node> currentRing = ring1.contains(maxX) ? ring1 : ring2;
        List<Node> otherRing = currentRing == ring1 ? ring2 : ring1;
        int i = currentRing.indexOf(maxX);
        Node current = maxX;
        // use the previous node in this ring
        Node previous = currentRing.get((i + currentRing.size() - 1) % currentRing.size());
        do {
            if (otherRing.contains(current)) {
                Node nextNodeCurrent = getNextNode(currentRing, i);
                int j = otherRing.indexOf(current);
                Node nextNodeOther = getNextNode(otherRing, j);
                if (nextNodeOther != null && (nextNodeCurrent == null || compareAngles(map, previous, current, nextNodeCurrent, nextNodeOther))) {
                    List<Node> tmp = currentRing;
                    currentRing = otherRing;
                    otherRing = tmp;
                    i = j;
                }
            }
            previous = current;
            i = (i + 1) % currentRing.size();
            current = currentRing.get(i);
            if (ring1.contains(current) && !ring2.contains(current)) {
                return current;
            }
        } while (current != maxX);
        return null;
    }

    /**
     * Assumes mergeFrom will deleted by caller and doesn't update back refs
     * 
     * @param mergeInto OsmElement to merge the parent Relations into
     * @param mergeFrom OsmElement with potentially new parent Relations
     */
    private void mergeElementsRelations(@NonNull final OsmElement mergeInto, @NonNull final OsmElement mergeFrom) {
        // copy just to be safe, use Set to ensure uniqueness
        Set<Relation> fromRelations = mergeFrom.getParentRelations() != null ? new HashSet<>(mergeFrom.getParentRelations()) : new HashSet<>();
        List<Relation> toRelations = mergeInto.getParentRelations() != null ? mergeInto.getParentRelations() : new ArrayList<>();
        Set<OsmElement> changedElements = new HashSet<>();
        synchronized (delegator) {
            UndoStorage undo = delegator.getUndo();
            Storage apiStorage = delegator.getApiStorage();
            for (Relation r : fromRelations) {
                if (!toRelations.contains(r)) {
                    delegator.dirty();
                    undo.save(r);
                    List<RelationMember> members = r.getAllMembers(mergeFrom);
                    for (RelationMember rm : members) {
                        // create new member with same role
                        RelationMember newRm = new RelationMember(rm.getRole(), mergeInto);
                        // insert at same place
                        r.replaceMember(rm, newRm);
                        mergeInto.addParentRelation(r);
                    }
                    r.updateState(OsmElement.STATE_MODIFIED);
                    apiStorage.insertElementSafe(r);
                    changedElements.add(r);
                    mergeInto.updateState(OsmElement.STATE_MODIFIED);
                    apiStorage.insertElementSafe(mergeInto);
                    changedElements.add(mergeInto);
                }
            }
        }
        delegator.onElementChanged(null, new ArrayList<>(changedElements));
    }

    /**
     * Merge the tags from two OsmElements into one set.
     * 
     * Note: while this does try to merge simple OSM lists correctly, and avoid known issues, there are no guarantees
     * that this will work for conflicting values
     * 
     * @param e1 first element
     * @param e2 second element
     * @return Map containing the merged tags
     * @throws OsmIllegalOperationException if the merged tag is too long
     */
    @NonNull
    public static Map<String, String> mergeTags(@NonNull OsmElement e1, @NonNull OsmElement e2) throws OsmIllegalOperationException {
        Map<String, String> merged = new TreeMap<>(e1.getTags());
        for (Entry<String, String> entry : e2.getTags().entrySet()) {
            final String key = entry.getKey();
            String value = entry.getValue();
            final String mergedValue = merged.get(key); // NOSONAR
            if (mergedValue == null) {
                merged.put(key, value);
                continue;
            }
            if (!mergedValue.equals(value)) { // identical tags do not need to be merged
                if (Tags.hasNestedLists(key)) {
                    value = mergedValue + Tags.OSM_VALUE_SEPARATOR + value; // no expectation that this is valid
                } else {
                    Set<String> values = new LinkedHashSet<>(Arrays.asList(splitValue(mergedValue)));
                    values.addAll(Arrays.asList(splitValue(value)));
                    value = Util.toOsmList(values);
                }
                if (value.length() > Capabilities.DEFAULT_MAX_STRING_LENGTH) {
                    // can't merge without losing information
                    throw new OsmIllegalOperationException("Merged tags too long for key " + key);
                }
                merged.put(key, value);
            }
        }
        return merged;
    }

    /**
     * Split value with the default value separator
     * 
     * @param value the value to split
     * @return an array holding the split values
     */
    @NonNull
    private static String[] splitValue(@NonNull final String value) {
        return value.split("\\" + Tags.OSM_VALUE_SEPARATOR);
    }
}
