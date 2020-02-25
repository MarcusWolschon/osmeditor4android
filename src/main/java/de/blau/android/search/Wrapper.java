package de.blau.android.search;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;

import org.jetbrains.annotations.NotNull;

import android.content.Context;
import android.support.annotation.NonNull;
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
import de.blau.android.osm.ViewBox;
import de.blau.android.osm.Way;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetElement;
import de.blau.android.presets.Preset.PresetGroup;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.presets.PresetElementPath;

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
     * @param context an Android Context
     */
    public Wrapper(@NonNull Context context) {
        this.context = context;
        this.logic = App.getLogic();
    }

    /**
     * @param element the element to set
     */
    public void setElement(OsmElement element) {
        this.element = element;
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
        throw new IllegalArgumentException(context.getString(R.string.search_objects_unsupported, "user"));
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
        throw new IllegalArgumentException(context.getString(R.string.search_objects_unsupported, "changeset"));
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
            return null;
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
        }
        return 0;
    }

    @Override
    public int getWayCount() {
        if (element instanceof Node) {
            return App.getLogic().getWaysForNode((Node) element).size();
        } else if (element instanceof Relation) {
            List<RelationMember> members = ((Relation) element).getMembers();
            int count = 0;
            for (RelationMember rm : members) {
                if (Way.NAME.equals(rm.getType())) {
                    count++;
                }
            }
            return count;
        }
        return 0;
    }

    @Override
    public int getAreaSize() {
        throw new IllegalArgumentException(context.getString(R.string.search_objects_unsupported, "areasize"));
    }

    @Override
    public int getWayLength() {
        if (element instanceof Way) {
            return (int) ((Way) element).length();
        }
        return 0;
    }

    @Override
    public @NotNull Collection<String> getRoles() {
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
    public boolean hasRole(@NotNull String role) {
        if (element instanceof Relation) {
            for (RelationMember member : ((Relation) element).getMembers()) {
                if (role.equals(member.getRole())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Object getPreset(@NotNull String presetPath) {
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
    public boolean matchesPreset(@NotNull Object preset) {
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
        for (BoundingBox box : App.getDelegator().getBoundingBoxes()) {
            if (box.contains(bounds)) {
                return true;
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
            for (RelationMember rm : ((Relation) element).getMembers()) {
                if (rm.downloaded() && !Relation.NAME.equals(rm.getType()) && inBox(box, rm.getElement())) {
                    return true;
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
        return logic.getViewBox().contains(element.getBounds());
    }
}
