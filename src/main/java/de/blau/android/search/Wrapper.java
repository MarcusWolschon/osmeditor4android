package de.blau.android.search;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import android.content.Context;
import android.support.annotation.NonNull;
import ch.poole.osm.josmfilterparser.ElementState.State;
import ch.poole.osm.josmfilterparser.Meta;
import ch.poole.osm.josmfilterparser.Type;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.Way;

/**
 * Wrapper around an OsmElement to provide the Meta interface without impacting serialization
 * 
 * @author simon
 *
 */
public class Wrapper implements Meta {

    private OsmElement element;

    final Context context;

    /**
     * Create a new wrapper object
     * 
     * @param context an Android Context
     */
    public Wrapper(@NonNull Context context) {
        this.context = context;
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
        } else if (element instanceof Node) {
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
    public boolean matchesPreset(@NotNull String preset) {
        throw new IllegalArgumentException(context.getString(R.string.search_objects_unsupported, "preset"));
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
}
