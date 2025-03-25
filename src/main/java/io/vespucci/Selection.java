package io.vespucci;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.R;
import io.vespucci.osm.Node;
import io.vespucci.osm.OsmElement;
import io.vespucci.osm.OsmElementList;
import io.vespucci.osm.Relation;
import io.vespucci.osm.StorageDelegator;
import io.vespucci.osm.Way;
import io.vespucci.util.ScreenMessage;

public class Selection {

    public static final String SELECTION_KEY = "SELECTION";

    public static class Ids implements Serializable {
        private static final long serialVersionUID = 1L;

        private final long[] nodes;
        private final long[] ways;
        private final long[] relations;

        /**
         * Construct a new instance
         * 
         * @param nodes a long array of node ids
         * @param ways a long array of way ids
         * @param relations a long array of relation ids
         */
        public Ids(@NonNull long[] nodes, @NonNull long[] ways, @NonNull long[] relations) {
            this.nodes = nodes;
            this.ways = ways;
            this.relations = relations;
        }

        /**
         * @return the nodes
         */
        public long[] getNodes() {
            return nodes;
        }

        /**
         * @return the ways
         */
        public long[] getWays() {
            return ways;
        }

        /**
         * @return the relations
         */
        public long[] getRelations() {
            return relations;
        }
    }

    /**
     * The user-selected nodes.
     */
    private final OsmElementList<Node> nodes = new OsmElementList<>();

    /**
     * The user-selected ways.
     */
    private final OsmElementList<Way> ways = new OsmElementList<>();

    /**
     * The user-selected relations.
     */
    private final OsmElementList<Relation> relations = new OsmElementList<>();

    /**
     * Get the number of selected Nodes
     * 
     * @return the node count
     */
    public int nodeCount() {
        return nodes.count();
    }

    /**
     * Get the number of selected Ways
     * 
     * @return the way count
     */
    public int wayCount() {
        return ways.count();
    }

    /**
     * Get the number of selected Relations
     * 
     * @return the relation count
     */
    public int relationCount() {
        return relations.count();
    }

    /**
     * Get the 1st selected Node
     * 
     * @return the 1st selected Node or null
     */
    @Nullable
    public Node getNode() {
        return nodes.get();
    }

    /**
     * Get the list of selected Nodes
     * 
     * @return the selected Nodes or null
     */
    @Nullable
    public List<Node> getNodes() {
        return nodes.getElements();
    }

    /**
     * Get the 1st selected Way
     * 
     * @return the 1st selected Way or null
     */
    @Nullable
    public Way getWay() {
        return ways.get();
    }

    /**
     * Get the list of selected Ways
     * 
     * @return the selected Ways or null
     */
    @Nullable
    public List<Way> getWays() {
        return ways.getElements();
    }

    /**
     * Get the 1st selected Relation
     * 
     * @return the 1st selected Relation or null
     */
    @Nullable
    public Relation getRelation() {
        return relations.get();
    }

    /**
     * Get the list of selected Relations
     * 
     * @return the selected Relations or null
     */
    @Nullable
    public List<Relation> getRelations() {
        return relations.getElements();
    }

    /**
     * Set the selected Node (this removes any previously selected Nodes)
     * 
     * @param element the Node to set
     */
    public void setNode(@Nullable Node element) {
        nodes.set(element);
    }

    /**
     * Set the selected Way (this removes any previously selected Ways)
     * 
     * @param element the Way to set
     */
    public void setWay(@Nullable Way element) {
        ways.set(element);
    }

    /**
     * Set the selected Relation (this removes any previously selected Relations)
     * 
     * @param element the Relation to set
     */
    public void setRelation(@Nullable Relation element) {
        relations.set(element);
    }

    /**
     * Reset all selections
     */
    public void reset() {
        nodes.set(null);
        ways.set(null);
        relations.set(null);
    }

    /**
     * Add an OsmElement to the selection
     * 
     * @param element the element
     */
    public void add(@NonNull OsmElement element) {
        if (element instanceof Node) {
            nodes.add((Node) element);
            return;
        }
        if (element instanceof Way) {
            ways.add((Way) element);
            return;
        }
        if (element instanceof Relation) {
            relations.add((Relation) element);
            return;
        }
        throw new IllegalArgumentException("Unknown element " + element.getClass().getCanonicalName());
    }

    /**
     * Remove an OsmElement from the selection
     * 
     * @param element the element
     * @return true if sucessful (aka the element was in the selection)
     */
    public boolean remove(@NonNull OsmElement element) {
        if (element instanceof Node) {
            return nodes.remove((Node) element);
        }
        if (element instanceof Way) {
            return ways.remove((Way) element);
        }
        if (element instanceof Relation) {
            return relations.remove((Relation) element);
        }
        throw new IllegalArgumentException("Unknown element " + element.getClass().getCanonicalName());
    }

    /**
     * Check if the selection contains an element
     * 
     * @param element the element
     * @return true if the selection contains the element
     */
    public boolean contains(@NonNull OsmElement element) {
        if (element instanceof Node) {
            return nodes.contains((Node) element);
        }
        if (element instanceof Way) {
            return ways.contains((Way) element);
        }
        return relations.contains((Relation) element);
    }

    /**
     * Get all elements
     * 
     * @return a List of OsmElement
     */
    @NonNull
    public List<OsmElement> getAll() {
        List<OsmElement> result = new ArrayList<>();
        if (nodes.count() > 0) {
            result.addAll(nodes.getElements());
        }
        if (ways.count() > 0) {
            result.addAll(ways.getElements());
        }
        if (relations.count() > 0) {
            result.addAll(relations.getElements());
        }
        return result;
    }

    /**
     * Get a count of all selected elements
     * 
     * @return the number of selected elements
     */
    public int count() {
        return nodes.count() + ways.count() + relations.count();
    }

    /**
     * Get the ids of the current contents
     * 
     * @return an Ids object
     */
    @NonNull
    public Ids getIds() {
        return new Ids(nodes.getIdArray(), ways.getIdArray(), relations.getIdArray());
    }

    /**
     * Set the contents from the ids
     * 
     * Checks for empty ways as these will cause crashes if selected
     * 
     * @param ctx optional Context for error messages
     * @param delegator a StorageDelegator object containing the OsmElements
     * @param ids the ids
     */
    public void fromIds(@Nullable Context ctx, @NonNull StorageDelegator delegator, @NonNull Ids ids) {
        nodes.fromIds(delegator, Node.NAME, ids.getNodes());
        ways.fromIds(delegator, Way.NAME, ids.getWays());
        // remove any degenerate ways and display a toast
        if (ways.count() > 0) {
            for (Way w : new ArrayList<>(ways.getElements())) {
                if (w.nodeCount() == 0) {
                    if (ctx != null) {
                        ScreenMessage.toastTopError(ctx, ctx.getString(R.string.toast_degenerate_way_with_info, w.getDescription(ctx)), true);
                    }
                    ways.remove(w);
                }
            }
        }
        relations.fromIds(delegator, Relation.NAME, ids.getRelations());
    }
}
