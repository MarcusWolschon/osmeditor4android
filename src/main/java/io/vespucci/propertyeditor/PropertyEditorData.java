package io.vespucci.propertyeditor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.osm.OsmElement;
import io.vespucci.osm.Relation;
import io.vespucci.osm.RelationMember;
import io.vespucci.osm.RelationMemberDescription;
import io.vespucci.osm.RelationMemberPosition;
import io.vespucci.util.ACRAHelper;
import io.vespucci.util.collections.MultiHashMap;

/**
 * Holds data sent in intents. Directly serializing a TreeMap in an intent does not work, as it comes out as a HashMap
 * (?!?)
 * 
 * @author Jan
 */
public class PropertyEditorData implements Serializable {
    private static final long serialVersionUID = 5L;

    private static final String DEBUG_TAG = PropertyEditorData.class.getSimpleName().substring(0,
            Math.min(23, PropertyEditorData.class.getSimpleName().length()));

    public final long                                       osmId;
    public final String                                     type;
    public final LinkedHashMap<String, String>              tags;            // NOSONAR
    public final LinkedHashMap<String, String>              originalTags;    // NOSONAR
    public final MultiHashMap<Long, RelationMemberPosition> parents;
    public final MultiHashMap<Long, RelationMemberPosition> originalParents;
    public final ArrayList<RelationMemberDescription>       members;         // NOSONAR
    public final ArrayList<RelationMemberDescription>       originalMembers; // NOSONAR
    public final String                                     focusOnKey;

    /**
     * Construct a new PropertyEditorData instance
     * 
     * @param selectedElement the OsmElement
     * @param focusOnKey a key if not null we want the PropertyEditor to focus on
     */
    public PropertyEditorData(@NonNull OsmElement selectedElement, @Nullable String focusOnKey) {
        osmId = selectedElement.getOsmId();
        type = selectedElement.getName();
        tags = new LinkedHashMap<>(selectedElement.getTags());
        originalTags = tags;
        if (selectedElement.getParentRelations() != null) {
            parents = getParentMap(selectedElement, new MultiHashMap<>(false, true));
            originalParents = parents;
        } else {
            parents = null;
            originalParents = null;
        }
        if (Relation.NAME.equals(selectedElement.getName())) {
            members = getRelationMemberDescriptions((Relation) selectedElement, new ArrayList<>());
            originalMembers = members;
        } else {
            members = null;
            originalMembers = null;
        }

        this.focusOnKey = focusOnKey;
    }

    /**
     * Get a map of parent relations and the elements position in them
     * 
     * @param element the OsmElement
     * @param parents the map of parent relations
     * @return the map for convenience
     */
    static MultiHashMap<Long, RelationMemberPosition> getParentMap(@NonNull OsmElement element, @NonNull MultiHashMap<Long, RelationMemberPosition> parents) {
        final List<Relation> parentRelations = element.getParentRelations();
        if (parentRelations != null) {
            for (Relation r : new HashSet<>(parentRelations)) {
                for (RelationMemberPosition rmp : r.getAllMembersWithPosition(element)) {
                    if (rmp != null) {
                        // we don't need to actually reference the member element, so we create a new RelationMember
                        parents.add(r.getOsmId(), RelationMemberPosition.copyWithoutElement(rmp));
                    } else {
                        Log.e(DEBUG_TAG, "inconsistency in relation membership");
                        ACRAHelper.nocrashReport(null, "inconsistency in relation membership");
                    }
                }
            }
        }
        return parents;
    }

    /**
     * Fill a List of RelationMemberDescription from the Relation members
     * 
     * @param relation the Relation
     * @param members the List
     * @return the List
     */
    static <T extends List<RelationMemberDescription>> T getRelationMemberDescriptions(@NonNull Relation relation, @NonNull T members) {
        int position = 0;
        for (RelationMember rm : relation.getMembers()) {
            RelationMemberDescription newRm = new RelationMemberDescription(rm);
            newRm.setPosition(position);
            members.add(newRm);
            position++;
        }
        return members;
    }

    /**
     * Deserialize an array
     * 
     * @param s the Serializable
     * @return an array of the elements, empty is s is null
     */
    @NonNull
    public static PropertyEditorData[] deserializeArray(@Nullable Serializable s) {
        Object[] a = (Object[]) s;
        if (a == null) {
            return new PropertyEditorData[0];
        }
        PropertyEditorData[] r = new PropertyEditorData[a.length];
        for (int i = 0; i < a.length; i++) {
            r[i] = (PropertyEditorData) a[i];
        }
        return r;
    }
}
