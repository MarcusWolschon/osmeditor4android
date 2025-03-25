package io.vespucci.filter;

import java.util.List;

import android.util.Log;
import androidx.annotation.NonNull;
import io.vespucci.osm.Node;
import io.vespucci.osm.OsmElement;
import io.vespucci.osm.Relation;
import io.vespucci.osm.RelationMember;
import io.vespucci.osm.Way;
import io.vespucci.util.collections.LongHashSet;

/**
 * Common methods used by multiple Filters
 * 
 * @author simon
 *
 */
public abstract class CommonFilter extends InvertableFilter {

    /**
     * 
     */
    private static final long serialVersionUID = 2L;

    private static final String DEBUG_TAG = CommonFilter.class.getSimpleName().substring(0, Math.min(23, CommonFilter.class.getSimpleName().length()));

    protected boolean enabled = true;

    transient LongHashSet rels = null;

    /**
     * Check if an OsmElement should be shown or not
     * 
     * @param e the OsmElement
     * @return an indication if an element should be shown or not
     */
    protected abstract Include filter(@NonNull OsmElement e);

    @Override
    public boolean include(Node node, boolean selected) {
        if (!enabled || selected) {
            return true;
        }
        Include include = cachedNodes.get(node);
        if (include != null) {
            return include != Include.DONT;
        }

        include = filter(node);
        cachedNodes.put(node, include);
        return include != Include.DONT;
    }

    @Override
    public boolean include(Way way, boolean selected) {
        if (!enabled) {
            return true;
        }
        Include include = cachedWays.get(way);
        if (include != null) {
            return include != Include.DONT;
        }

        include = filter(way);

        if (include == Include.INCLUDE_WITH_WAYNODES) {
            for (Node n : way.getNodes()) {
                Include includeNode = cachedNodes.get(n);
                if (includeNode == null || (include != Include.DONT && includeNode == Include.DONT)) {
                    // if not originally included overwrite now
                    if (include == Include.DONT && (n.hasTags() || n.hasParentRelations())) { // no entry yet so we have
                                                                                              // to check tags and
                                                                                              // relations
                        include(n, false);
                        continue;
                    }
                    cachedNodes.put(n, include);
                }
            }
        }
        cachedWays.put(way, include);

        return include != Include.DONT || selected;
    }

    @Override
    public boolean include(Relation relation, boolean selected) {
        return testRelation(relation, selected) != Include.DONT;
    }

    /**
     * Check if a relation should be included
     * 
     * @param relation the Relation
     * @param selected true if the Relation is selected
     * @return an Include value
     */
    Include testRelation(@NonNull Relation relation, boolean selected) {
        if (!enabled || selected) {
            return Include.INCLUDE_WITH_WAYNODES;
        }
        Include include = cachedRelations.get(relation);
        if (include != null) {
            return include;
        }
        if (cachedRelations.containsKey(relation)) { // relation loop
            include = Include.DONT;
            Log.e(DEBUG_TAG, "Relation " + relation.getOsmId() + " has a loop");
        } else {
            cachedRelations.put(relation, null);
            include = filter(relation);
        }
        cachedRelations.put(relation, include);
        List<RelationMember> members = relation.getMembers();
        if (members != null) {
            for (RelationMember rm : members) {
                OsmElement element = rm.getElement();
                if (element != null) {
                    if (element instanceof Way) {
                        Way w = (Way) element;
                        Include includeWay = cachedWays.get(w);
                        if (includeWay != null && (include != Include.DONT && includeWay == Include.DONT)) {
                            // if not originally included overwrite now
                            if (include == Include.INCLUDE_WITH_WAYNODES) {
                                for (Node n : w.getNodes()) {
                                    cachedNodes.put(n, include);
                                }
                            }
                            cachedWays.put(w, include);
                        }
                    } else if (element instanceof Node) {
                        Node n = (Node) element;
                        Include includeNode = cachedNodes.get(n);
                        if (includeNode != null && (include != Include.DONT && includeNode == Include.DONT)) {
                            // if not originally included overwrite now
                            cachedNodes.put(n, include);
                        }
                    } else if (element instanceof Relation) {
                        // FIXME not clear if we really want to do this
                    }
                }
            }
        }
        return include;
    }
}
