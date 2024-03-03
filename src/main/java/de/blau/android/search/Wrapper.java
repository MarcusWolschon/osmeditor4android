package de.blau.android.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.jetbrains.annotations.NotNull;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ch.poole.osm.josmfilterparser.Condition;
import ch.poole.osm.josmfilterparser.ElementState.State;
import ch.poole.osm.josmfilterparser.Meta;
import ch.poole.osm.josmfilterparser.Type;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.R;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.ViewBox;
import de.blau.android.osm.Way;
import de.blau.android.presets.Preset;
import de.blau.android.presets.PresetElement;
import de.blau.android.presets.PresetElementPath;
import de.blau.android.presets.PresetGroup;
import de.blau.android.presets.PresetItem;

/**
 * Wrapper around an OsmElement to provide the Meta interface without impacting serialization
 * 
 * @author simon
 *
 */
public class Wrapper implements Meta {

    private OsmElement element;

    final Context context;
    final Logic   logic;

    /**
     * Create a new wrapper object
     * 
     */
    public Wrapper() {
        this(null);
    }

    /**
     * Create a new wrapper object
     * 
     * @param context an Android Context
     */
    public Wrapper(@Nullable Context context) {
        this.context = context;
        this.logic = App.getLogic();
    }

    /**
     * @param element the element to set
     */
    public void setElement(@NonNull OsmElement element) {
        this.element = element;
    }

    /**
     * Get the current element
     * 
     * @return the current element
     */
    @Nullable
    public OsmElement getElement() {
        return element;
    }

    @Override
    public Type getType() {
        return toJosmFilterType(element);
    }

    @Override
    public Map<String, String> getTags() {
        return element.getTags();
    }

    /**
     * Determine the type of the element for purposes of the filter language
     * 
     * @param element the OsmElement
     * @return the corresponding Type
     */
    public static Type toJosmFilterType(@NonNull OsmElement element) {
        if (element instanceof Node) {
            return Type.NODE;
        }
        if (element instanceof Way) {
            return Type.WAY;
        }
        return Type.RELATION;
    }

    @Override
    public String getUser() {
        throw unsupported("user");
    }

    /**
     * Construct an IllegalArgumentException
     * 
     * @param expression the unsupported expression
     * @return an IllegalArgumentException
     */
    private IllegalArgumentException unsupported(@NonNull String expression) {
        return new IllegalArgumentException(
                context != null ? context.getString(R.string.search_objects_unsupported, expression) : "Unsupported expression \"" + expression + "\"");
    }

    @Override
    public long getId() {
        return element.getOsmId();
    }

    @Override
    public long getVersion() {
        return element.getOsmVersion();
    }

    @Override
    public long getChangeset() {
        throw unsupported("changeset");
    }

    @Override
    public long getTimestamp() {
        return element.getTimestamp();
    }

    @Override
    public State getState() {
        switch (element.getState()) {
        case OsmElement.STATE_CREATED:
            return State.NEW;
        case OsmElement.STATE_MODIFIED:
            return State.MODIFIED;
        case OsmElement.STATE_DELETED:
            return State.DELETED;
        default:
            return State.UNCHANGED;
        }
    }

    @Override
    public boolean isClosed() {
        return (element instanceof Way) && ((Way) element).isClosed();
    }

    @Override
    public int getNodeCount() {
        if (element instanceof Way) {
            return ((Way) element).nodeCount();
        } else if (element instanceof Relation) {
            return getMemberTypeCount(Node.NAME);
        }
        return 0;
    }

    @Override
    public int getWayCount() {
        if (element instanceof Node) {
            return App.getLogic().getWaysForNode((Node) element).size();
        } else if (element instanceof Relation) {
            return getMemberTypeCount(Way.NAME);
        }
        return 0;
    }

    /**
     * Count the number of members of the supplied type
     * 
     * @param type the element type to count
     * @return a count
     */
    private int getMemberTypeCount(@NonNull String type) {
        List<RelationMember> members = ((Relation) element).getMembers();
        int count = 0;
        if (members != null) {
            for (RelationMember rm : members) {
                if (type.equals(rm.getType())) {
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    public int getAreaSize() {
        throw unsupported("areasize");
    }

    @Override
    public int getWayLength() {
        if (element instanceof Way) {
            return (int) ((Way) element).length();
        }
        return 0;
    }

    @Override
    public @NonNull Collection<String> getRoles() {
        Set<String> result = new HashSet<>();
        List<Relation> parents = element.getParentRelations();
        if (parents != null) {
            for (Relation parent : parents) {
                List<RelationMember> members = parent.getAllMembers(element);
                for (RelationMember member : members) {
                    String role = member.getRole();
                    if (role != null && !"".equals(role)) {
                        result.add(role);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public boolean isSelected() {
        return App.getLogic().isSelected(element);
    }

    @Override
    public boolean hasRole(@NonNull String role) {
        if (element instanceof Relation) {
            List<RelationMember> members = ((Relation) element).getMembers();
            if (members != null) {
                for (RelationMember member : members) {
                    if (role.equals(member.getRole())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public Object getPreset(@NonNull String presetPath) {
        if (context == null) {
            throw unsupported("preset:");
        }
        if (presetPath.endsWith("*")) {
            presetPath = presetPath.substring(0, presetPath.length() - 1);
        }
        String[] segments = presetPath.split("\\|");
        if (segments.length > 0) {
            PresetElementPath path = new PresetElementPath(Arrays.asList(segments));
            return Preset.getElementByPath(App.getCurrentRootPreset(context).getRootGroup(), path);
        }
        return null;
    }

    @Override
    public boolean matchesPreset(@NonNull Object preset) {
        SortedMap<String, String> tags = element.getTags();
        ElementType type = element.getType();
        if (preset instanceof PresetItem && matches((PresetItem) preset, type, tags)) {
            return true;
        } else if (preset instanceof PresetGroup) {
            for (PresetElement pe2 : ((PresetGroup) preset).getElements()) {
                if (pe2 instanceof PresetItem && matches((PresetItem) pe2, type, tags)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if an element matches a preset (only taking fixed tags in to account)
     * 
     * @param preset the preset
     * @param type the element type
     * @param tags any tags
     * @return true if it matches
     */
    private boolean matches(@NonNull PresetItem preset, @NonNull ElementType type, @NonNull SortedMap<String, String> tags) {
        return preset.getFixedTagCount() > 0 && preset.appliesTo(type) && preset.matches(tags);
    }

    @Override
    public boolean isIncomplete() {
        if (element instanceof Relation) {
            return !((Relation) element).allDownloaded();
        }
        return false;
    }

    @Override
    public boolean isInDownloadedArea() {
        for (BoundingBox box : App.getDelegator().getBoundingBoxes()) {
            if (inBox(box)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isAllInDownloadedArea() {
        BoundingBox bounds = element.getBounds();
        if (!(element instanceof Relation) || ((Relation) element).allDownloaded()) {
            for (BoundingBox box : App.getDelegator().getBoundingBoxes()) {
                if (box.contains(bounds)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isInview() {
        ViewBox box = logic.getViewBox();
        return inBox(box);
    }

    /**
     * Check is the OSM element is in a bounding box
     * 
     * @param box the BoundingBox
     * @return true if part of the element is in the box
     */
    boolean inBox(@NonNull BoundingBox box) {
        if (element instanceof Relation) {
            List<RelationMember> members = ((Relation) element).getMembers();
            if (members != null) {
                for (RelationMember rm : members) {
                    if (rm.downloaded() && !Relation.NAME.equals(rm.getType()) && inBox(box, rm.getElement())) {
                        return true;
                    }
                }
            }
            return false;
        }
        return inBox(box, element);
    }

    /**
     * Check if an OSM Way or Node is at least partially in a BoundingBox
     * 
     * Note intersection of bounding boxes doesn't work for this case so we need to check all nodes
     * 
     * @param box the BoundingBox
     * @param e the OsmElement
     * @return true if part of the element is in the box
     */
    boolean inBox(@NonNull BoundingBox box, @NonNull OsmElement e) {
        if (e instanceof Node) {
            return box.contains(((Node) e).getLon(), ((Node) e).getLat());
        }
        if (e instanceof Way) {
            for (Node n : ((Way) e).getNodes()) {
                if (box.contains(n.getLon(), n.getLat())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isAllInview() {
        return logic.getViewBox().contains(element.getBounds()) && (!(element instanceof Relation) || ((Relation) element).allDownloaded());
    }

    @Override
    public boolean isChild(@NonNull Type type, @NonNull Meta meta, @NonNull List<Object> parents) {
        for (Object o : parents) {
            if (o instanceof Relation) {
                if (element.hasParentRelation((Relation) o)) {
                    return true;
                }
            } else if (o instanceof Way && element instanceof Node && ((Way) o).hasNode((Node) element)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isParent(@NonNull Type type, @NonNull Meta meta, @NonNull List<Object> children) {
        for (Object o : children) {
            if (element instanceof Relation) {
                if (((OsmElement) o).hasParentRelation((Relation) element)) {
                    return true;
                }
            } else if (element instanceof Way && o instanceof Node && ((Way) element).hasNode((Node) o)) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    @Override
    public List<Object> getMatchingElements(@NonNull Condition c) {
        List<Object> result = new ArrayList<>();
        SearchResult sr = getMatchingElementsInternal(c);
        result.addAll(sr.nodes);
        result.addAll(sr.ways);
        result.addAll(sr.relations);
        return result;
    }

    class SearchResult {
        List<Node>     nodes     = new ArrayList<>();
        List<Way>      ways      = new ArrayList<>();
        List<Relation> relations = new ArrayList<>();

        /**
         * Check if the result is empty
         * 
         * @return true if empty
         */
        public boolean isEmpty() {
            return nodes.isEmpty() && ways.isEmpty() && relations.isEmpty();
        }
    }

    /**
     * Eval the condition on all objects in memory
     * 
     * @param c the Condition to check
     * @return a SearchResult object
     */
    @NonNull
    SearchResult getMatchingElementsInternal(@NonNull Condition c) {
        OsmElement savedElement = element; // save this instead of instantating a new wrapper
        StorageDelegator delegator = App.getDelegator();
        SearchResult result = new SearchResult();
        for (Node n : delegator.getCurrentStorage().getNodes()) {
            element = n;
            if (c.eval(Type.NODE, this, n.getTags())) {
                result.nodes.add(n);
            }
        }
        for (Way w : delegator.getCurrentStorage().getWays()) {
            element = w;
            if (c.eval(Type.WAY, this, w.getTags())) {
                result.ways.add(w);
            }
        }
        for (Relation r : delegator.getCurrentStorage().getRelations()) {
            element = r;
            if (c.eval(Type.RELATION, this, r.getTags())) {
                result.relations.add(r);
            }
        }
        element = savedElement;
        return result;
    }

    @Override
    public @NotNull Meta wrap(Object arg0) {
        if (context == null) {
            throw unsupported("unknown");
        }
        Wrapper wrapper = new Wrapper(context);
        wrapper.setElement((OsmElement) arg0);
        return wrapper;
    }
}
