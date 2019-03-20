package de.blau.android.filter;

import java.util.List;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.Way;
import de.blau.android.validation.Validator;

/**
 * Filter plus UI for filtering on presets NOTE: the relevant ways should be processed before nodes
 * 
 * @author simon
 *
 */
public class CorrectFilter extends Filter {

    /**
     * 
     */
    private static final long   serialVersionUID = 1L;
    private static final String DEBUG_TAG        = "CompleteFilter";

    private boolean             enabled         = true;
    private transient Context   context;
    private boolean             includeWayNodes = false;
    private boolean             inverted        = false;
    private transient Validator validator;

    /**
     * Construct a new filter
     */
    public CorrectFilter() {
        this(null);
    }

    /**
     * Construct a new filter
     * 
     * @param context an Android Context
     */
    public CorrectFilter(@Nullable Context context) {
        super();
        Log.d(DEBUG_TAG, "Constructor");
        init(context);
        //
    }

    @Override
    public void init(Context context) {
        Log.d(DEBUG_TAG, "init");
        this.context = context;
        validator = App.getDefaultValidator(context);
        clear();
    }

    /**
     * @return if way nodes are incldued
     */
    public boolean includeWayNodes() {
        return includeWayNodes;
    }

    /**
     * Include way nodes when ways are included
     * 
     * @param on if true include way nodes
     */
    public void setIncludeWayNodes(boolean on) {
        Log.d(DEBUG_TAG, "set include way nodes " + on);
        this.includeWayNodes = on;
    }

    /**
     * @return is the filter inverted?
     */
    public boolean isInverted() {
        return inverted;
    }

    /**
     * Invert the filter
     * 
     * @param inverted invert the filter if true
     */
    public void setInverted(boolean inverted) {
        this.inverted = inverted;
    }

    /**
     * Check if an OsmElement should be shown or not
     * 
     * @param e the OsmElement
     * @return an indication if an element should be schown or not
     */
    private Include filter(@NonNull OsmElement e) {
        Include include = Include.DONT;
        if (e.hasProblem(context, validator) != Validator.OK) {
            return Include.INCLUDE;
        }
        return include;
    }

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

    Include testRelation(Relation relation, boolean selected) {
        if (!enabled || selected) {
            return Include.INCLUDE_WITH_WAYNODES;
        }
        Include include = cachedRelations.get(relation);
        if (include != null) {
            return include;
        }

        include = filter(relation);

        cachedRelations.put(relation, include);
        List<RelationMember> members = relation.getMembers();
        if (members != null) {
            for (RelationMember rm : members) {
                OsmElement element = rm.getElement();
                if (element != null) {
                    if (element instanceof Way) {
                        Way w = (Way) element;
                        Include includeWay = cachedWays.get(w);
                        if (includeWay == null || (include != Include.DONT && includeWay == Include.DONT)) {
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
                        if (includeNode == null || (include != Include.DONT && includeNode == Include.DONT)) {
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
