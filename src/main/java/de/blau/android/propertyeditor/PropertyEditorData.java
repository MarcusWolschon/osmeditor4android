package de.blau.android.propertyeditor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.RelationMemberDescription;
import de.blau.android.osm.RelationMemberPosition;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.collections.MultiHashMap;

/**
 * Holds data sent in intents. Directly serializing a TreeMap in an intent does not work, as it comes out as a HashMap
 * (?!?)
 * 
 * @author Jan
 */
public class PropertyEditorData implements Serializable {
    private static final long serialVersionUID = 5L;

    private static final String DEBUG_TAG = PropertyEditorData.class.getSimpleName();

    public final long                                       osmId;
    public final String                                     type;
    public final LinkedHashMap<String, String>              tags;
    public final LinkedHashMap<String, String>              originalTags;
    public final MultiHashMap<Long, RelationMemberPosition> parents;
    public final MultiHashMap<Long, RelationMemberPosition> originalParents;
    public final ArrayList<RelationMemberDescription>       members;
    public final ArrayList<RelationMemberDescription>       originalMembers;
    public final String                                     focusOnKey;

    /**
     * Construct a new PropertyEditorData instance
     * 
     * @param osmId the id of the edited object
     * @param type the type of the object
     * @param tags the edited tags
     * @param originalTags the previous tags
     * @param parents the edited parent Relations
     * @param originalParents the previous paren Relations
     * @param members the edited relation members
     * @param originalMembers the previous relation members
     */
    public PropertyEditorData(long osmId, @NonNull String type, @Nullable Map<String, String> tags, Map<String, String> originalTags,
            @Nullable MultiHashMap<Long, RelationMemberPosition> parents, @Nullable MultiHashMap<Long, RelationMemberPosition> originalParents,
            @Nullable ArrayList<RelationMemberDescription> members, @Nullable ArrayList<RelationMemberDescription> originalMembers) {
        this.osmId = osmId;
        this.type = type;
        this.tags = tags != null ? new LinkedHashMap<>(tags) : null;
        this.originalTags = originalTags != null ? new LinkedHashMap<>(originalTags) : null;
        this.parents = parents;
        this.originalParents = originalParents;
        this.members = members;
        this.originalMembers = originalMembers;
        this.focusOnKey = null;
    }

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
        MultiHashMap<Long, RelationMemberPosition> tempParents = new MultiHashMap<>(false, true);
        if (selectedElement.getParentRelations() != null) {
            Set<Relation> uniqueRelations = new HashSet<>(selectedElement.getParentRelations());
            for (Relation r : uniqueRelations) {
                List<RelationMember> members = r.getAllMembers(selectedElement);
                for (RelationMember rm : members) {
                    if (rm != null) {
                        // we don't need to actually reference the member
                        RelationMemberPosition rmp = new RelationMemberPosition(new RelationMember(rm.getType(), rm.getRef(), rm.getRole()), r.getPosition(rm));
                        tempParents.add(r.getOsmId(), rmp);
                    } else {
                        Log.e(DEBUG_TAG, "inconsistency in relation membership");
                        ACRAHelper.nocrashReport(null, "inconsistency in relation membership");
                    }
                }
            }
            parents = tempParents;
            originalParents = parents;
        } else {
            parents = null;
            originalParents = null;
        }
        ArrayList<RelationMemberDescription> tempMembers = new ArrayList<>();
        if (selectedElement.getName().equals(Relation.NAME)) {
            for (RelationMember rm : ((Relation) selectedElement).getMembers()) {
                RelationMemberDescription newRm = new RelationMemberDescription(rm);
                tempMembers.add(newRm);
            }
            members = tempMembers;
            originalMembers = members;
        } else {
            members = null;
            originalMembers = null;
        }

        this.focusOnKey = focusOnKey;
    }

    /**
     * Deserialize an array
     * 
     * @param s the Serializable
     * @return an array of the elements
     */
    @Nullable
    public static PropertyEditorData[] deserializeArray(Serializable s) {
        Object[] a = (Object[]) s;
        if (a == null) {
            return null;
        }
        PropertyEditorData[] r = new PropertyEditorData[a.length];
        for (int i = 0; i < a.length; i++) {
            r[i] = (PropertyEditorData) a[i];
        }
        return r;
    }
}
