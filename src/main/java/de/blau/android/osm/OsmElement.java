package de.blau.android.osm;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.content.res.Resources;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ch.poole.osm.josmtemplateparser.Formatter;
import ch.poole.osm.josmtemplateparser.JosmTemplateParseException;
import ch.poole.osm.josmtemplateparser.JosmTemplateParser;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.presets.Preset;
import de.blau.android.presets.PresetItem;
import de.blau.android.search.Wrapper;
import de.blau.android.util.DateFormatter;
import de.blau.android.util.IssueAlert;
import de.blau.android.util.Util;
import de.blau.android.validation.Validator;

public abstract class OsmElement implements OsmElementInterface, Serializable, XmlSerializable, JosmXmlSerializable {

    /**
     * 
     */
    private static final long serialVersionUID = 7711945069147743675L;

    public static final long NEW_OSM_ID = -1;

    public static final byte STATE_UNCHANGED = 0;
    public static final byte STATE_CREATED   = 1;
    public static final byte STATE_MODIFIED  = 2;
    public static final byte STATE_DELETED   = 3;

    static final String ID_ATTR        = "id";
    static final String VERSION_ATTR   = "version";
    static final String TIMESTAMP_ATTR = "timestamp";
    static final String VISIBLE_ATTR   = "visible";
    static final String TRUE_VALUE     = "true";
    static final String FALSE_VALUE    = "false";

    static final String TAG            = "tag";
    static final String TAG_KEY_ATTR   = "k";
    static final String TAG_VALUE_ATTR = "v";

    static final String JOSM_ACTION = "action";
    static final String JOSM_MODIFY = "modify";
    static final String JOSM_DELETE = "delete";

    public static final long EPOCH = 1104537600L; // 2005-01-01 00:00:00

    private static final String ADDRESS_DESC = "address ";

    protected long osmId;

    protected long osmVersion;

    protected TreeMap<String, String> tags;

    protected byte state;

    private List<Relation> parentRelations;

    // seconds since EPOCH, negative == not set, using an int here limits dates up to 2073
    private int timestamp = -1;

    /**
     * hasProblem() is an expensive test, so the results are cached. old version used a Boolean object which was silly
     * we could naturally encode these as bits
     */
    private int cachedProblems = Validator.NOT_VALIDATED;

    /**
     * Construct a new base osm element
     * 
     * @param osmId the id
     * @param osmVersion version
     * @param timestamp timestamp
     * @param state state
     */
    OsmElement(final long osmId, final long osmVersion, final long timestamp, final byte state) {
        this.osmId = osmId;
        this.osmVersion = osmVersion;
        setTimestamp(timestamp);
        this.tags = null;
        this.state = state;
        this.parentRelations = null;
    }

    @Override
    public long getOsmId() {
        return osmId;
    }

    @Override
    public long getOsmVersion() {
        return osmVersion;
    }

    /**
     * Set the version for this element
     * 
     * @param osmVersion the version as a long
     */
    void setOsmVersion(final long osmVersion) {
        this.osmVersion = osmVersion;
    }

    /**
     * Set the OSM id for this element
     * 
     * This updates any relation member object that refer to this element
     * 
     * @param osmId the id as a long
     */
    void setOsmId(final long osmId) {
        this.osmId = osmId;
        if (parentRelations != null) { // update all references
            for (Relation r : parentRelations) {
                for (RelationMember member : r.getAllMembers(this)) {
                    member.ref = osmId;
                }
            }
        }
    }

    @Override
    @NonNull
    public SortedMap<String, String> getTags() {
        if (tags == null) {
            return Collections.unmodifiableSortedMap(new TreeMap<String, String>()); // for backwards compatibility
        }
        return Collections.unmodifiableSortedMap(tags);
    }

    /**
     * @return true if the element has at least one tag
     */
    public boolean hasTags() {
        return tags != null && tags.size() > 0;
    }

    /**
     * Get an object of type T from the cache using our tags as the key
     * 
     * @param <T> the object type
     * @param cache the cache
     * @return an object of type T or null
     */
    @Nullable
    public <T> T getFromCache(@NonNull Map<Map<String, String>, T> cache) {
        return cache.get(tags);
    }

    /**
     * Add an object of type T to the cache using our tags as the key
     * 
     * @param <T> the object type
     * @param cache the cache
     * @param o an object of type T
     */
    public <T> void addToCache(@NonNull Map<Map<String, String>, T> cache, @Nullable T o) {
        cache.put(tags, o);
    }

    /**
     * Check if the cache contains an object of type T to the cache using our tags as the key
     * 
     * @param <T> the object type
     * @param cache the cache
     * @return true if there is a mapping
     */
    public <T> boolean isInCache(@NonNull Map<Map<String, String>, T> cache) {
        return cache.containsKey(tags);
    }

    @Override
    public byte getState() {
        return state;
    }

    /**
     * gives a string description of the element type (e.g. 'node', 'way' or 'relation') - see also {@link #getType()}
     * is rather confusingly named
     * 
     * @return the type of the element
     */
    public abstract String getName();

    /**
     * Update state if MODIFIED or DELETED.
     * 
     * @param newState new state to set
     */
    void updateState(final byte newState) {
        if (state != STATE_CREATED || newState == STATE_DELETED) {
            state = newState;
        }
    }

    /**
     * Unconditionally set the state
     * 
     * @param newState new state to set
     */
    void setState(final byte newState) {
        state = newState;
    }

    /**
     * Add the tags of the element, replacing any existing tags.
     * 
     * @param tags New tags to add or to replace existing tags.
     */
    void addTags(final Map<String, String> tags) {
        if (tags != null) {
            if (this.tags == null) {
                this.tags = new TreeMap<>();
            }
            this.tags.putAll(tags);
        }
    }

    /**
     * Set the tags of the element, replacing all existing tags.
     * 
     * @param tags New tags to replace existing tags.
     * @return Flag indicating if the tags have actually changed.
     */
    boolean setTags(@Nullable final Map<String, String> tags) {
        if (this.tags == null) {
            addTags(tags);
            return true;
        } else if (!this.tags.equals(tags)) {
            this.tags.clear();
            addTags(tags);
            return true;
        }
        return false;
    }

    /**
     * Check if we have a tag with a specific key-value combination
     * 
     * @param key the key to search for (case sensitive)
     * @param value the value to search for (case sensitive)
     * @return true if the element has a tag with this key and value.
     */
    public boolean hasTag(@NonNull final String key, @Nullable final String value) {
        if (tags == null) {
            return false;
        }
        String keyValue = tags.get(key);
        return keyValue != null && keyValue.equals(value);
    }

    /**
     * Check if we have a tag with a specific key-value combination
     * 
     * Note TreeMap doesn't implement Map
     * 
     * @param tags tags to use instead of the standard ones
     * @param key the key to search for (case sensitive)
     * @param value the value to search for (case sensitive)
     * @return true if the element has a tag with this key and value.
     */
    static boolean hasTag(@Nullable final Map<String, String> tags, @NonNull final String key, @Nullable final String value) {
        if (tags == null) {
            return false;
        }
        String keyValue = tags.get(key);
        return keyValue != null && keyValue.equals(value);
    }

    /**
     * Check if we have a tag with a specific key-value combination
     * 
     * Note: the value is compared case insensitive
     * 
     * @param tagKey the key of the tag
     * @param value the value that we are checking
     * @return true if the key - value combination is present
     */
    public boolean hasTagWithValue(@NonNull String tagKey, @NonNull String value) {
        String tagValue = getTagWithKey(tagKey);
        return tagValue != null && tagValue.equalsIgnoreCase(value);
    }

    /**
     * Get the value of a tag with key
     * 
     * @param key the key to search for (case sensitive)
     * @return the value of this key.
     */
    @Nullable
    public String getTagWithKey(@NonNull final String key) {
        return getTagWithKey(tags, key);
    }

    /**
     * Get the value of a tag with key
     * 
     * @param tags a Map holding the tags
     * @param key the key to search for (case sensitive)
     * @return the value of this key.
     */
    @Nullable
    public static String getTagWithKey(Map<String, String> tags, @NonNull final String key) {
        return tags != null ? tags.get(key) : null;
    }

    /**
     * Check if the tags contain an entry for key
     * 
     * @param key the key to search for (case sensitive)
     * @return true if the element has a tag with this key.
     */
    public boolean hasTagKey(@NonNull final String key) {
        return hasTagKey(tags, key);
    }

    /**
     * Check if the tags contain an entry for key
     * 
     * @param tags a Map holding the tags
     * @param key the key to search for (case sensitive)
     * @return true if the element has a tag with this key.
     */
    public static boolean hasTagKey(@Nullable Map<String, String> tags, @NonNull final String key) {
        return tags != null && tags.containsKey(key);
    }

    /**
     * Check if the tags contain any of the keys
     * 
     * @param keys the keys to search for (case sensitive)
     * @return true if the element has a tag with one of the keys.
     */
    public boolean hasTagKey(@NonNull final String... keys) {
        if (tags == null) {
            return false;
        }
        for (String key : keys) {
            if (tags.containsKey(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * check if this element has tags of any kind
     * 
     * @return true if this elements has at least one tag
     */
    public boolean isTagged() {
        return (tags != null) && (tags.size() > 0);
    }

    @Override
    public String toString() {
        return getName() + " " + osmId;
    }

    /**
     * Write the tags in XML format
     * 
     * @param s the Serializer
     * @throws IllegalArgumentException if the serializer encountered an illegal argument
     * @throws IllegalStateException if the serializer detects an illegal state
     * @throws IOException if writing to the serializer fails
     */
    protected void tagsToXml(@NonNull final XmlSerializer s) throws IllegalArgumentException, IllegalStateException, IOException {
        if (tags != null) {
            for (Entry<String, String> tag : tags.entrySet()) {
                s.startTag("", TAG);
                s.attribute("", TAG_KEY_ATTR, tag.getKey());
                s.attribute("", TAG_VALUE_ATTR, tag.getValue());
                s.endTag("", TAG);
            }
        }
    }

    /**
     * Write the element attributes in XML format
     * 
     * @param s the Serializer
     * @param changeSetId the current changeset id or null
     * @param josm if true use josm format
     * @throws IOException if writing to the serializer fails
     */
    protected void attributesToXml(@NonNull final XmlSerializer s, @Nullable Long changeSetId, boolean josm) throws IOException {
        s.attribute("", ID_ATTR, Long.toString(osmId));
        if (changeSetId != null) {
            s.attribute("", OsmXml.CHANGESET, Long.toString(changeSetId));
        }
        if (josm) {
            if (state == OsmElement.STATE_DELETED) {
                s.attribute("", JOSM_ACTION, JOSM_DELETE);
            } else if (state == OsmElement.STATE_CREATED || state == OsmElement.STATE_MODIFIED) {
                s.attribute("", JOSM_ACTION, JOSM_MODIFY);
            }
        }
        s.attribute("", VERSION_ATTR, Long.toString(osmVersion));
        if (timestamp >= 0) {
            s.attribute("", TIMESTAMP_ATTR, DateFormatter.getUtcFormat(OsmParser.TIMESTAMP_FORMAT).format(getTimestamp() * 1000));
        }
        s.attribute("", VISIBLE_ATTR, TRUE_VALUE); // it could be argued that this should follow the state too
    }

    /**
     * Check if this element is unchanged
     * 
     * @return true if the element hasn't been changed
     */
    public boolean isUnchanged() {
        return state == STATE_UNCHANGED;
    }

    /**
     * Add reference to parent relation Does not check id to avoid dupes!
     * 
     * @param relation we want to add a ref to
     */
    public void addParentRelation(@NonNull Relation relation) {
        if (parentRelations == null) {
            parentRelations = new ArrayList<>();
        }
        parentRelations.add(relation);
    }

    /**
     * Check for parent relation
     * 
     * @param relation relation to check for
     * @return true if the relation was found
     */
    public boolean hasParentRelation(@NonNull Relation relation) {
        return (parentRelations != null && parentRelations.contains(relation));
    }

    /**
     * Check for parent relation based on id
     * 
     * @param osmId id of the parent relation
     * @return true if the relation was found
     */
    public boolean hasParentRelation(long osmId) {
        if (parentRelations == null) {
            return false;
        }
        for (Relation r : parentRelations) {
            if (osmId == r.getOsmId()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add all parent relations, avoids dups
     * 
     * @param relations List of Relations to add
     */
    public void addParentRelations(@NonNull List<Relation> relations) {
        if (parentRelations == null) {
            parentRelations = new ArrayList<>();
        }
        // dedup
        for (Relation r : relations) {
            if (!parentRelations.contains(r)) {
                addParentRelation(r);
            }
        }
    }

    @Override
    @Nullable
    public List<Relation> getParentRelations() {
        return parentRelations;
    }

    /**
     * Check if this element is a member of a relation
     * 
     * @return true if if this element is a member of a relation
     */
    public boolean hasParentRelations() {
        return parentRelations != null && !parentRelations.isEmpty();
    }

    /**
     * Remove all references to parent relation does not check for id
     *
     * @param relation relation from which we want to remove this element
     */
    public void removeParentRelation(@NonNull Relation relation) {
        if (parentRelations != null) {
            while (parentRelations.remove(relation)) {
                // empty
            }
        }
    }

    /**
     * Remove reference to parent relation
     * 
     * @param osmId id of the relation from which we want to remove this element
     */
    public void removeParentRelation(long osmId) {
        if (parentRelations != null) {
            for (Relation r : new ArrayList<>(parentRelations)) {
                if (osmId == r.getOsmId()) {
                    parentRelations.remove(r);
                }
            }
        }
    }

    /**
     * Remove all back links
     */
    public void clearParentRelations() {
        parentRelations = null;
    }

    /**
     * Generate a human-readable description/summary of the element.
     * 
     * @return a description of the element
     */
    @NonNull
    public String getDescription() {
        return getDescription(true);
    }

    /**
     * Generate a human-readable description/summary of the element.
     * 
     * @param ctx Android context
     * @return a description of the element
     */
    @NonNull
    public String getDescription(@Nullable Context ctx) {
        return getDescription(ctx, true);
    }

    /**
     * Return a concise description of the element
     * 
     * @param withType include an indication of the object type (node, way, relation)
     * @return a description of the element
     */
    @NonNull
    public String getDescription(boolean withType) {
        return getDescription(null, withType);
    }

    /**
     * Return a concise description of the element
     * 
     * @param ctx Android context
     * @param withType include an indication of the object type (node, way, relation)
     * @return a string containing the description
     */
    @NonNull
    public String getDescription(@Nullable Context ctx, boolean withType) {
        return getDescription(ctx, tags, withType);
    }

    /**
     * Return a concise description of the element
     * 
     * @param ctx Android context
     * @param tags the elements tags
     * @param withType include an indication of the object type (node, way, relation)
     * @return a string containing the description
     */
    @NonNull
    protected String getDescription(Context ctx, @Nullable Map<String, String> tags, boolean withType) {
        // Use the name if it exists
        String name = getTagWithKey(tags, Tags.KEY_NAME);
        if (Util.notEmpty(name)) {
            return addId(ctx, name, withType);
        }

        // Then the address
        String address = getAddressString(ctx, tags);
        if (Util.notEmpty(address) && (hasTagKey(tags, Tags.KEY_BUILDING) || hasTagKey(tags, Tags.KEY_ENTRANCE))) {
            return addId(ctx, address, withType);
        }

        // try to match with a preset
        if (ctx != null) {
            PresetItem p = Preset.findBestMatch(App.getCurrentPresets(ctx), tags, null, null);
            if (p != null) {
                String templateName = nameFromTemplate(ctx, p);
                if (Util.notEmpty(templateName)) {
                    return templateName;
                }
                String ref = getTagWithKey(tags, Tags.KEY_REF);
                return addId(ctx, p.getDisplayName(ctx) + (ref != null ? " " + ref : ""), withType);
            }
        }

        // Then the value of the most 'important' tag the element has
        String tag = getPrimaryTag(ctx);
        if (tag != null) {
            return addId(ctx, tag, withType);
        }

        return addId(ctx, null, withType);
    }

    /**
     * Add the element id to a description
     * 
     * @param ctx an ANdroid Context
     * @param description the descriptin
     * @param withType if the type should be included
     * @return a formatted String
     */
    @NonNull
    protected String addId(@Nullable Context ctx, @Nullable String description, boolean withType) {
        final String idString = Long.toString(getOsmId());
        if (Util.notEmpty(description)) {
            if (ctx != null) {
                return withType ? ctx.getString(R.string.description_type_id, description, getName(), idString)
                        : ctx.getString(R.string.description_id, description, idString);
            } else {
                return (withType ? description + " " + getName() : description) + " #" + idString;
            }
        }
        if (ctx != null) {
            return withType ? ctx.getString(R.string.type_id, getName(), idString) : ctx.getString(R.string.only_id, idString);
        }
        return (withType ? getName() : "") + " #" + idString;
    }

    /**
     * Get a String from address tags (if any)
     * 
     * @param ctx an Android Context
     * @param tags the tags
     * @return a String or null
     */
    @Nullable
    static String getAddressString(@Nullable Context ctx, @Nullable Map<String, String> tags) {
        final boolean haveCtx = ctx != null;
        String housenumber = getTagWithKey(tags, Tags.KEY_ADDR_HOUSENUMBER);
        if (Util.notEmpty(housenumber)) {
            try {
                String street = getTagWithKey(tags, Tags.KEY_ADDR_STREET);
                if (Util.notEmpty(street)) {
                    if (haveCtx) {
                        return ctx.getResources().getString(R.string.address_housenumber_street, street, housenumber);
                    }
                    return ADDRESS_DESC + housenumber + " " + street;
                }
                if (haveCtx) {
                    return ctx.getResources().getString(R.string.address_housenumber, housenumber);
                }
                return ADDRESS_DESC + housenumber;
            } catch (Exception ex) {
                // protect against translation errors
            }
        }
        return null;
    }

    /**
     * Get a name from a preset name template
     * 
     * @param ctx an Android Context
     * @param p the matching PresetItem
     * @return a name or null
     */
    @Nullable
    public String nameFromTemplate(@NonNull Context ctx, @NonNull PresetItem p) {
        String nameTemplate = p.getNameTemplate();
        if (nameTemplate != null) {
            JosmTemplateParser parser = new JosmTemplateParser(new ByteArrayInputStream(nameTemplate.getBytes()));
            try {
                List<Formatter> rs = parser.formatters();
                Wrapper wrapper = new Wrapper(ctx);
                wrapper.setElement(this);
                return ch.poole.osm.josmtemplateparser.Util.listFormat(rs, wrapper.getType(), wrapper, getTags());
            } catch (JosmTemplateParseException e) {
                // ignore
            }
        }
        return null;
    }

    /**
     * Get the first "important" tag
     * 
     * @param ctx Android context
     * @return the first key=value of any important tags or null if none found
     */
    @Nullable
    public String getPrimaryTag(@Nullable Context ctx) {
        String result = null;
        for (String tag : Tags.IMPORTANT_TAGS) {
            result = getTagValueString(tag);
            if (result != null) {
                return result;
            }
        }
        if (ctx != null) {
            Preset[] presets = App.getCurrentPresets(ctx);
            for (Preset preset : presets) {
                if (preset != null) {
                    for (String key : preset.getObjectKeys()) {
                        result = getTagValueString(key);
                        if (result != null) {
                            return result;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Get a string in the form key=value, if tag has a value
     * 
     * @param tag the key to generate the string for
     * @return a string in the form key=value or null
     */
    @Nullable
    private String getTagValueString(@NonNull String tag) {
        String value = getTagWithKey(tag);
        if (value != null && value.length() > 0) {
            return tag + "=" + value;
        }
        return null;
    }

    /**
     * Generate a description of the element that also includes state information.
     * 
     * @param aResources Application resources.
     * @return A human readable description of the element that includes state information.
     */
    @NonNull
    public String getStateDescription(@NonNull final Resources aResources) {
        int resid = getStateStringResource();
        String result = getDescription();
        if (resid != 0) {
            result = aResources.getString(resid, result);
        }
        return result;
    }

    /**
     * Get the string resource for the current state
     * 
     * @return A string resource for the current state
     */
    public int getStateStringResource() {
        switch (getState()) {
        case STATE_CREATED:
            return R.string.changes_created;
        case STATE_MODIFIED:
            return R.string.changes_changed;
        case STATE_DELETED:
            return R.string.changes_deleted;
        default:
            return 0;
        }
    }

    /**
     * Update tags and anything necessary from e
     * 
     * @param <T> the subclass of OsmElement
     * @param e the OsmELement to update from
     */
    abstract <T extends OsmElement> void updateFrom(@NonNull T e);

    /**
     * Validate this element
     * 
     * @param validator the Validator to use
     * @return the validation result
     */
    protected abstract int validate(@NonNull Validator validator);

    /**
     * Test if the element has a noted problem.
     * 
     * A noted problem is where someone has tagged the element with a "fixme" or "todo" key/value. //NOSONAR
     * 
     * @param context Android context, if non-null used for generating alerts
     * @param validator the Validator to use
     * @return true if the element has a noted problem, false if it doesn't.
     */
    public int hasProblem(@Nullable Context context, @NonNull Validator validator) {
        // This implementation assumes that calcProblem() may be expensive, and
        // caches the calculation.
        if (cachedProblems == Validator.NOT_VALIDATED) {
            cachedProblems = validate(validator);
            if (cachedProblems != Validator.OK && context != null) {
                IssueAlert.alert(context, this);
            }
        }
        return cachedProblems;
    }

    /**
     * Get any cached problems for this element
     * 
     * @return and int containing the issue values as individual set bits or Validator.NOT_VALIDATED
     */
    public int getCachedProblems() {
        return cachedProblems;
    }

    /**
     * Call if you have made a change that potentially changes the problem state of the element
     */
    public void resetHasProblem() {
        cachedProblems = Validator.NOT_VALIDATED;
    }

    /**
     * Call if you want to disable validation call {@link #resetHasProblem()} to re-enable
     */
    public void dontValidate() {
        cachedProblems = Validator.OK;
    }

    /**
     * Set the problem value
     * 
     * @param problem the int to set
     */
    public void setProblem(int problem) {
        cachedProblems = problem;
    }

    /**
     * (see also {@link #getName()} - this returns the full type, differentiating between open and closed ways)
     * 
     * @return the {@link ElementType} of the element
     */
    public abstract ElementType getType();

    /**
     * Version of above that uses a potential different set of tags
     * 
     * @param tags tags to use
     * @return the ElementType
     */
    public abstract ElementType getType(Map<String, String> tags);

    /** Enum for element types (Node, Way, Closed Ways, Relations, Areas (MPs) */
    public enum ElementType {
        NODE, WAY, CLOSEDWAY, RELATION, AREA
    }

    /**
     * Get the minimum distance of this element from the location
     * 
     * Note this is only useful for sorting given that the result is returned in WGS84 °*1E7 or so
     * 
     * @param location a coordinate tupel - lat / lon - in WGS84*1E7 degrees
     * @return the planar geom distance in degrees
     */
    public abstract double getMinDistance(final int[] location);

    /**
     * Return a bounding box covering the element
     * 
     * @return the BoundingBox or null if it cannot be determined
     */
    @Nullable
    public abstract BoundingBox getBounds();

    /**
     * Set the timestamp
     * 
     * @param secsSinceUnixEpoch seconds since the Unix Epoch
     */
    public void setTimestamp(long secsSinceUnixEpoch) {
        timestamp = (int) (secsSinceUnixEpoch - EPOCH);
    }

    /**
     * Set the timestamp to the current time
     */
    public void stamp() {
        setTimestamp(System.currentTimeMillis() / 1000);
    }

    @Override
    public long getTimestamp() {
        if (timestamp >= 0) {
            return EPOCH + timestamp;
        }
        return -1L;
    }
}
