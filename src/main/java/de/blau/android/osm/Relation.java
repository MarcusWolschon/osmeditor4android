package de.blau.android.osm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.resources.DataStyle.FeatureStyle;
import de.blau.android.util.rtree.BoundedObject;
import de.blau.android.validation.Validator;

/**
 * Relation represents an OSM relation element which essentially is a collection of other OSM elements.
 * 
 * @author simon
 *
 */
public class Relation extends OsmElement implements BoundedObject, StyleableFeature {

    /**
     * 
     */
    private static final long serialVersionUID = 1104911642016294266L;

    final ArrayList<RelationMember> members;
    
    private transient FeatureStyle style = null; // FeatureProfile is currently not serializable

    public static final String NAME = "relation";

    public static final String MEMBER = "member";

    /**
     * Construct a new Relation
     * 
     * @param osmId the OSM id
     * @param osmVersion the version
     * @param timestamp timestamp in ms since the epoch
     * @param status the status of the Relation
     */
    Relation(final long osmId, final long osmVersion, final long timestamp, final byte status) {
        super(osmId, osmVersion, timestamp, status);
        members = new ArrayList<>();
    }

    /**
     * Append a RelationMember to the Relation
     * 
     * @param member the RelationMember to append
     */
    void addMember(@NonNull final RelationMember member) {
        members.add(member);
    }

    /**
     * Return complete list of relation members
     * 
     * @return list of members, or null if there are none
     */
    @Nullable
    public List<RelationMember> getMembers() {
        return members;
    }

    /**
     * Return first relation member element for this OSM element
     * 
     * Note: if the element is present more than once you will only get one
     * 
     * @param e OsmElement to search for
     * @return the corresponding RelationMember or null if not found
     */
    @Nullable
    public RelationMember getMember(@NonNull OsmElement e) {
        for (int i = 0; i < members.size(); i++) {
            RelationMember member = members.get(i);
            if (member.getElement() == e) {
                return member;
            }
        }
        return null;
    }

    /**
     * Return all relation member elements for this OSM element
     * 
     * @param e OsmElement to search for
     * @return the list of corresponding RelationMembers, empty if non found
     */
    @NonNull
    public List<RelationMember> getAllMembers(@NonNull OsmElement e) {
        List<RelationMember> result = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            RelationMember member = members.get(i);
            if (member.getElement() == e) {
                result.add(member);
            }
        }
        return result;
    }

    /**
     * Return first relation member element for this OSM element
     * 
     * Note: if the element is present more than once you will only get ont
     * 
     * @param type type of OsmElement
     * @param id OSM id of the element
     * @return the corresponding RelationMember or null if not found
     */
    @Nullable
    public RelationMember getMember(@NonNull String type, long id) {
        for (int i = 0; i < members.size(); i++) {
            RelationMember member = members.get(i);
            if (member.getRef() == id && member.getType().equals(type)) {
                return member;
            }
        }
        return null;
    }

    /**
     * Get the RelationMember at a specific position
     * 
     * @param pos the position
     * @return the RelationMember of null if the position was out of bounds
     */
    @Nullable
    public RelationMember getMemberAt(int pos) {
        if (pos >= 0 && pos < members.size()) {
            return members.get(pos);
        }
        return null;
    }

    /**
     * Get the position (0 based) of the RelationMember in the list of members
     * 
     * @param rm the RelationMember
     * @return the position or -1 if not found
     */
    public int getPosition(@NonNull RelationMember rm) {
        return members.indexOf(rm);
    }

    /**
     * 
     * @return list of members allowing {@link Iterator#remove()}.
     */
    Iterator<RelationMember> getRemovableMembers() {
        return members.iterator();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    void updateState(final byte newState) {
        style = null; // force recalc of style
        super.updateState(newState);
    }

    @Override
    void setState(final byte newState) {
        style = null; // force recalc of style
        super.setState(newState);
    }

    @Override
    public FeatureStyle getStyle() {
        return style;
    }

    @Override
    public void setStyle(@Nullable FeatureStyle fp) {
        style = fp;
    }
    
    @Override
    public String toString() {
        // String res = super.toString();
        // for (Map.Entry<String, String> tag : tags.entrySet()) {
        // res += "\t" + tag.getKey() + "=" + tag.getValue();
        // }
        // for (RelationMember m:members) {
        // res += "\t" + m.toString();
        // }
        // return res;
        return getDescription();
    }

    @Override
    public void toXml(final XmlSerializer s, final Long changeSetId) throws IllegalArgumentException, IllegalStateException, IOException {
        toXml(s, changeSetId, false);
    }

    @Override
    public void toJosmXml(final XmlSerializer s) throws IllegalArgumentException, IllegalStateException, IOException {
        toXml(s, null, true);
    }

    /**
     * Generate XML format OSM files
     * 
     * @param s the XML serializer
     * @param changeSetId the current changeset id or null
     * @param josm if true use JOSM format
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     * @throws IOException
     */
    private void toXml(@NonNull final XmlSerializer s, @Nullable Long changeSetId, boolean josm)
            throws IllegalArgumentException, IllegalStateException, IOException {
        s.startTag("", NAME);
        attributesToXml(s, changeSetId, josm);
        for (RelationMember member : members) {
            s.startTag("", "member");
            s.attribute("", "type", member.getType());
            s.attribute("", "ref", Long.toString(member.getRef()));
            s.attribute("", "role", member.getRole());
            s.endTag("", "member");
        }
        tagsToXml(s);
        s.endTag("", NAME);
    }

    /**
     * Completely remove member from relation (even if present more than once) Does not update backlink
     * 
     * @param member the RelationMember to remove
     */
    void removeMember(final RelationMember member) {
        while (members.remove(member)) {
        }
    }

    /**
     * Add/insert a member after an already existing member
     * 
     * @param memberBefore the existing RelationMember
     * @param newMember the new RelationMember
     */
    void addMemberAfter(@NonNull final RelationMember memberBefore, @NonNull final RelationMember newMember) {
        members.add(members.indexOf(memberBefore) + 1, newMember);
    }

    /**
     * Add/insert a member after an already existing member
     * 
     * @param memberAfter the existing RelationMember
     * @param newMember the new RelationMember
     */
    void addMemberBefore(@NonNull final RelationMember memberAfter, @NonNull final RelationMember newMember) {
        members.add(members.indexOf(memberAfter), newMember);
    }

    /**
     * Add a member at a specific position
     * 
     * @param pos position to ad the member at, if out of bounds the member will be added
     * @param newMember the new RelationMember
     */
    void addMember(int pos, @NonNull final RelationMember newMember) {
        if (pos < 0 || pos > members.size()) {
            pos = members.size(); // append
        }
        members.add(pos, newMember);
    }

    /**
     * Adds multiple elements to the relation in the order in which they appear in the list. They can be either
     * prepended or appended to the existing nodes.
     * 
     * @param newMembers a list of new members
     * @param atBeginning if true, nodes are prepended, otherwise, they are appended
     */
    protected void addMembers(List<RelationMember> newMembers, boolean atBeginning) {
        if (atBeginning) {
            members.addAll(0, newMembers);
        } else {
            members.addAll(newMembers);
        }
    }

    /**
     * Return a List of all RelationMembers with a specific role
     * 
     * @param role the role we are looking for
     * @return a List of the RelationMembers
     */
    public List<RelationMember> getMembersWithRole(@NonNull String role) {
        List<RelationMember> rl = new ArrayList<>();
        for (RelationMember rm : members) {
            Log.d("Relation", "getMembersWithRole " + rm.getRole());
            if (role.equals(rm.getRole())) {
                rl.add(rm);
            }
        }
        return rl;
    }

    /**
     * Replace an existing member in a relation with a different member.
     * 
     * @param existing The existing member to be replaced.
     * @param newMember The new member.
     */
    void replaceMember(RelationMember existing, RelationMember newMember) {
        int idx;
        while ((idx = members.indexOf(existing)) != -1) {
            members.set(idx, newMember);
        }
    }

    /**
     * Replace all existing members in a relation.
     *
     * @param newMembers The new member.
     */
    void replaceMembers(Collection<RelationMember> newMembers) {
        members.clear();
        members.addAll(newMembers);
    }

    /**
     * rough implementation for now
     */
    @Override
    public String getDescription() {
        return getDescription(null);
    }

    @Override
    public String getDescription(Context ctx) {
        String description = "";
        String type = getTagWithKey(Tags.KEY_TYPE);
        if (type != null && !"".equals(type)) {
            PresetItem p = null;
            if (ctx != null) {
                p = Preset.findBestMatch(App.getCurrentPresets(ctx), tags);
            }
            if (p != null) {
                description = p.getTranslatedName();
                if (Tags.VALUE_RESTRICTION.equals(type)) {
                    String restriction = getTagWithKey(Tags.VALUE_RESTRICTION);
                    if (restriction != null) {
                        String d = p.getDescriptionForValue(Tags.VALUE_RESTRICTION, restriction);
                        if (d != null) { // the names of turn restrictions are clear enouhg
                            description = d;
                        }
                    }
                } else {
                    TreeMap<String, String> tagsCopy = new TreeMap<>(tags);
                    if (tagsCopy.remove(Tags.KEY_TYPE) != null) {
                        p = Preset.findBestMatch(App.getCurrentPresets(ctx), tagsCopy);
                        if (p != null) {
                            description = description + " " + p.getTranslatedName();
                        }
                    }
                }
            } else {
                description = type;
                if (Tags.VALUE_RESTRICTION.equals(type)) {
                    String restriction = getTagWithKey(Tags.VALUE_RESTRICTION);
                    if (restriction != null) {
                        description = restriction + " " + description;
                    }
                } else if (Tags.VALUE_ROUTE.equals(type)) {
                    String route = getTagWithKey(Tags.VALUE_ROUTE);
                    if (route != null) {
                        description = route + " " + description;
                    }
                } else if (Tags.VALUE_MULTIPOLYGON.equals(type)) {
                    String b = getTagWithKey(Tags.KEY_BOUNDARY);
                    if (b != null) {
                        description = b + " " + Tags.KEY_BOUNDARY + " " + description;
                    } else {
                        String l = getTagWithKey(Tags.KEY_LANDUSE);
                        if (l != null) {
                            description = Tags.KEY_LANDUSE + " " + l + " " + description;
                        } else {
                            String n = getTagWithKey(Tags.KEY_NATURAL);
                            if (n != null) {
                                description = Tags.KEY_NATURAL + " " + n + " " + description;
                            }
                        }
                    }
                }
            }
        } else {
            description = App.resources().getString(R.string.unset_relation_type);
        }

        String name = getTagWithKey(Tags.KEY_NAME);
        if (name != null) {
            description = description + " " + name;
        } else {
            description = description + " #" + osmId;
        }
        return description;
    }

    @Override
    public ElementType getType() {
        return getType(tags);
    }

    @Override
    public ElementType getType(Map<String, String> tags) {
        if (hasTag(tags, Tags.KEY_TYPE, Tags.VALUE_MULTIPOLYGON) || hasTag(tags, Tags.KEY_TYPE, Tags.VALUE_BOUNDARY)) {
            return ElementType.AREA;
        }
        return ElementType.RELATION;
    }

    /**
     * Return a list of the downloaded elements
     * 
     * @return List of OsmElement
     */
    public List<OsmElement> getMemberElements() {
        List<OsmElement> result = new ArrayList<>();
        for (RelationMember rm : getMembers()) {
            if (rm.getElement() != null) {
                result.add(rm.getElement());
            }
        }
        return result;
    }

    /**
     * Check if all member elements are downloaded
     * 
     * @return true if all elements are present
     */
    public boolean allDownloaded() {
        for (RelationMember rm : getMembers()) {
            if (rm.getElement() == null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public BoundingBox getBounds() {
        // NOTE this will only return a bb covering the downloaded elements
        BoundingBox result = null;
        for (RelationMember rm : members) {
            OsmElement e = rm.getElement();
            if (e != null) {
                if (result == null) {
                    result = e.getBounds();
                } else {
                    result.union(e.getBounds());
                }
            }
        }
        return result;
    }

    @Override
    protected int validate(Validator validator) {
        return validator.validate(this);
    }

    @Override
    <T extends OsmElement> void updateFrom(T e) {
        if (!(e instanceof Relation)) {
            throw new IllegalArgumentException("e is not a Relation");
        }
        if (e.getOsmId() != getOsmId()) {
            throw new IllegalArgumentException("Different ids " + e.getOsmId() + " != " + getOsmId());
        }
        setTags(e.getTags());
        setState(e.getState());
        replaceMembers(((Relation) e).getMembers());
    }
}
