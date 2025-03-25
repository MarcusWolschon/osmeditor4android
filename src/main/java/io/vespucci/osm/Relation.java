package io.vespucci.osm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.presets.Preset;
import io.vespucci.presets.PresetItem;
import io.vespucci.util.Util;
import io.vespucci.util.rtree.BoundedObject;
import io.vespucci.validation.Validator;

/**
 * Relation represents an OSM relation element which essentially is a collection of other OSM elements.
 * 
 * @author simon
 *
 */
public class Relation extends StyledOsmElement implements RelationInterface, BoundedObject {
    /**
     * 
     */
    private static final long serialVersionUID = 1104911642016294268L;

    private final List<RelationMember> members;

    public static final String NAME = "relation";

    /**
     * Abbreviation
     */
    public static final String ABBREV = "r";

    static final String MEMBER_ATTR      = "member";
    static final String MEMBER_ROLE_ATTR = "role";
    static final String MEMBER_REF_ATTR  = "ref";
    static final String MEMBER_TYPE_ATTR = "type";

    static final int MAX_DEPTH = 3;

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

    @Override
    @Nullable
    public List<RelationMember> getMembers() {
        return members;
    }

    /**
     * Get the current number of members
     * 
     * @return the member count
     */
    public int getMemberCount() {
        return members.size();
    }

    /**
     * Return complete list of relation members of a certain type
     * 
     * @param type the RelationMember type
     * @return list of members
     */
    @NonNull
    public List<RelationMember> getMembers(@NonNull String type) {
        List<RelationMember> result = new ArrayList<>();
        for (RelationMember member : getMembers()) {
            if (type.equals(member.getType())) {
                result.add(member);
            }
        }
        return result;
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

    @Override
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
     * Return all relation member elements for this OSM element wrapped in a RelationMemberPosition with the position
     * set
     * 
     * @param e OsmElement to search for
     * @return the list of corresponding RelationMembers, empty if non found
     */
    @NonNull
    public List<RelationMemberPosition> getAllMembersWithPosition(@NonNull OsmElement e) {
        List<RelationMemberPosition> result = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            RelationMember member = members.get(i);
            if (member.getElement() == e) {
                RelationMemberPosition rmp = new RelationMemberPosition(member, i);
                result.add(rmp);
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
     * @return the RelationMember or null if the position was out of bounds
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
    public String toString() {
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
     * @throws IllegalArgumentException if the serializer encountered an illegal argument
     * @throws IllegalStateException if the serializer detects an illegal state
     * @throws IOException if writing to the serializer fails
     */
    private void toXml(@NonNull final XmlSerializer s, @Nullable Long changeSetId, boolean josm)
            throws IllegalArgumentException, IllegalStateException, IOException {
        s.startTag("", NAME);
        attributesToXml(s, changeSetId, josm);
        for (RelationMember member : members) {
            s.startTag("", MEMBER_ATTR);
            s.attribute("", MEMBER_TYPE_ATTR, member.getType());
            s.attribute("", MEMBER_REF_ATTR, Long.toString(member.getRef()));
            s.attribute("", MEMBER_ROLE_ATTR, member.getRole());
            s.endTag("", MEMBER_ATTR);
        }
        tagsToXml(s);
        s.endTag("", NAME);
    }

    /**
     * Completely remove member from relation (even if present more than once) Does not update backlink
     * 
     * @param member the RelationMember to remove
     */
    void removeMember(@NonNull final RelationMember member) {
        while (members.remove(member)) {
            // LOOP
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
    protected void addMembers(@NonNull List<RelationMember> newMembers, boolean atBeginning) {
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
    @NonNull
    public List<RelationMember> getMembersWithRole(@NonNull String role) {
        List<RelationMember> rl = new ArrayList<>();
        for (RelationMember rm : members) {
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
    void replaceMember(@NonNull RelationMember existing, @NonNull RelationMember newMember) {
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
    void replaceMembers(@NonNull Collection<RelationMember> newMembers) {
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
        return getDescription(ctx, true);
    }

    @Override
    public String getDescription(Context ctx, boolean withType) {
        String name = getTagWithKey(Tags.KEY_NAME);
        String type = getTagWithKey(Tags.KEY_TYPE);
        if (!Util.notEmpty(type)) {
            return addId(ctx, (name != null ? name + " " : "") + App.resources().getString(R.string.unset_relation_type), withType);
        }

        PresetItem p = null;
        if (ctx != null) {
            p = Preset.findBestMatch(App.getCurrentPresets(ctx), tags, null, null);
        }
        if (p != null) {
            String templateName = nameFromTemplate(ctx, p);
            if (Util.notEmpty(templateName)) {
                return templateName;
            }
            String description = p.getTranslatedName();
            if (Tags.VALUE_RESTRICTION.equals(type)) {
                String restriction = getTagWithKey(Tags.VALUE_RESTRICTION);
                if (restriction != null) {
                    String d = p.getDescriptionForValue(Tags.VALUE_RESTRICTION, restriction);
                    if (d != null) { // the names of turn restrictions are clear enough
                        description = d;
                    }
                }
                return addId(ctx, description, true);
            }
            if (Tags.VALUE_MULTIPOLYGON.equals(type)) {
                Map<String, String> tagsCopy = new TreeMap<>(tags);
                tagsCopy.remove(Tags.KEY_TYPE);
                return getDescription(ctx, tagsCopy, withType);
            }

            return addId(ctx, name != null ? name + " " + description : description, withType);
        }
        String description = type;
        switch (type) {
        case Tags.VALUE_RESTRICTION:
            String restriction = getTagWithKey(Tags.VALUE_RESTRICTION);
            if (restriction != null) {
                description = restriction + " " + description;
            }
            break;
        case Tags.VALUE_ROUTE:
            String route = getTagWithKey(Tags.VALUE_ROUTE);
            if (route != null) {
                description = route + " " + description;
            }
            break;

        case Tags.VALUE_BOUNDARY:
            String b = getTagWithKey(Tags.KEY_BOUNDARY);
            if (b != null) {
                description = b + " " + Tags.KEY_BOUNDARY + " " + description;
            }
            break;
        case Tags.VALUE_MULTIPOLYGON:
            String l = getTagWithKey(Tags.KEY_LANDUSE);
            if (l != null) {
                description = Tags.KEY_LANDUSE + " " + l + " " + description;
            } else {
                String n = getTagWithKey(Tags.KEY_NATURAL);
                if (n != null) {
                    description = Tags.KEY_NATURAL + " " + n + " " + description;
                }
            }
            break;
        default:
            // nothing
        }

        return addId(ctx, name != null ? name + " " + description : description, withType);
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

    /**
     * Check if the Relation has any downloaded members
     * 
     * @return true if any members are present
     */
    public boolean hasDownloadedMembers() {
        for (RelationMember rm : getMembers()) {
            if (rm.getElement() != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public BoundingBox getBounds() {
        return getBounds(1);
    }

    /**
     * Return a bounding box covering the element with loop protection
     * 
     * This will stop when depth > MAX_DEPTH
     * 
     * @param depth current depth in the tree we are at
     * @return the BoundingBox or null if it cannot be determined
     */
    @Nullable
    private BoundingBox getBounds(int depth) {
        // NOTE this will only return a bb covering the downloaded elements
        BoundingBox result = null;
        if (depth > MAX_DEPTH) {
            Log.e(NAME, "getBounds relation nested too deep " + getOsmId());
            return result;
        }
        for (RelationMember rm : members) {
            OsmElement e = rm.getElement();
            if (e != null) {
                BoundingBox box = e instanceof Relation ? ((Relation) e).getBounds(depth + 1) : e.getBounds();
                if (result == null) {
                    result = box;
                } else {
                    if (box != null) {
                        result.union(box);
                    }
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

    @Override
    public double getMinDistance(int[] location) {
        return getMinDistance(1, location);
    }

    /**
     * Loop protected version of getMinDistance
     * 
     * This will stop when depth > MAX_DEPTH
     * 
     * @param depth current depth in a Relation "tree"
     * @param location a coordinate tupel in WGS84*1E7 degrees
     * @return the planar geom distance in degrees
     */
    private double getMinDistance(int depth, @NonNull int[] location) {
        double distance = Double.MAX_VALUE;
        if (depth <= MAX_DEPTH) {
            for (RelationMember rm : members) {
                OsmElement e = rm.getElement();
                if (e != null) {
                    if (e instanceof Relation) {
                        distance = Math.min(distance, ((Relation) e).getMinDistance(depth + 1, location));
                    } else {
                        distance = Math.min(distance, e.getMinDistance(location));
                    }
                }
            }
        } else {
            Log.e(NAME, "getMinDistance relation nested too deep " + getOsmId());
        }
        return distance;
    }
}
