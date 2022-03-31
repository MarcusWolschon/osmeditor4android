package de.blau.android.presets;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import ch.poole.osm.josmfilterparser.JosmFilterParser;
import de.blau.android.R;
import de.blau.android.contract.Urls;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.search.Wrapper;
import de.blau.android.util.SearchIndexUtils;
import de.blau.android.util.StringWithDescription;

/** Represents a preset item (e.g. "footpath", "grocery store") */
public class PresetItem extends PresetElement {

    private static final long serialVersionUID = 17L;

    private static final String HTTP = "http";

    private static final String DEBUG_TAG = null;

    /**
     * All fields in the order they are in the Preset file
     */
    LinkedHashMap<String, PresetField> fields = new LinkedHashMap<>();

    /** "fixed" tags, i.e. the ones that have a fixed key-value pair */
    Map<String, PresetFixedField> fixedTags = new HashMap<>();

    /**
     * Roles
     */
    LinkedList<PresetRole> roles = null;

    /**
     * Linked names of presets
     */
    LinkedList<PresetItemLink> linkedPresetItems = null;

    /**
     * Linked names of alternative presets
     */
    LinkedList<PresetItemLink> alternativePresetItems = null;

    /**
     * true if a chunk
     */
    private boolean chunk = false;

    /**
     * If true the item is suitable to be autoapplied
     */
    private boolean autoapply = true;

    /**
     * Minimum match value so that a match will be considered, normally the number of fixed keys
     */
    short minMatch = -1;

    private final int itemIndex;

    private transient int recommendedKeyCount = -1;

    /**
     * Construct a new PresetItem
     * 
     * @param preset the Preset this belongs to
     * @param parent parent group (or null if this is the root group)
     * @param name name of the element or null
     * @param iconpath the icon path (either "http://" URL or "presets/" local image reference) or null
     * @param types comma separated list of types of OSM elements this applies to or null for all
     */
    public PresetItem(@NonNull Preset preset, @Nullable PresetGroup parent, @Nullable String name, @Nullable String iconpath, @Nullable String types) {
        super(preset, parent, name, iconpath);
        if (types == null) {
            // Type not specified, assume all types
            setAppliesToNode();
            setAppliesToWay();
            setAppliesToClosedway();
            setAppliesToRelation();
            setAppliesToArea();
        } else {
            String[] typesArray = types.split(",");
            for (String type : typesArray) {
                switch (type.trim()) {
                case Node.NAME:
                    setAppliesToNode();
                    break;
                case Way.NAME:
                    setAppliesToWay();
                    break;
                case Preset.CLOSEDWAY:
                    setAppliesToClosedway();
                    break;
                case Preset.MULTIPOLYGON:
                    setAppliesToArea();
                    break;
                case Preset.AREA:
                    setAppliesToArea(); //
                    break;
                case Relation.NAME:
                    setAppliesToRelation();
                    break;
                default:
                    // do nothing
                    Log.e(DEBUG_TAG, "Unknown type " + type);
                }
            }
        }
        itemIndex = preset.allItems.size();
        preset.allItems.add(this);
    }

    /**
     * Construct a new PresetItem in this preset from an existing one adding the necessary bits to the indices
     * 
     * @param preset the Preset this belongs to
     * @param group PresetGroup this should be added, null if none
     * @param item the PresetItem to copy
     */
    public PresetItem(@NonNull Preset preset, @Nullable PresetGroup group, @NonNull PresetItem item) {
        super(preset, group, item);
        this.fields = item.fields;
        this.fixedTags = item.fixedTags;
        this.roles = item.roles;
        this.linkedPresetItems = item.linkedPresetItems;
        this.minMatch = item.minMatch;

        if (!chunk) {
            for (Entry<String, PresetFixedField> e : getFixedTags().entrySet()) {
                StringWithDescription v = e.getValue().getValue();
                String key = e.getKey();
                String value = "";
                if (v != null && v.getValue() != null) {
                    value = v.getValue();
                }
                preset.tagItems.add(key + "\t" + value, this);
                addToAutosuggest(key, v);
            }
            for (Entry<String, PresetField> e : getFields().entrySet()) {
                PresetField field = e.getValue();
                if (field instanceof PresetCheckGroupField) {
                    for (PresetCheckField check : ((PresetCheckGroupField) field).getCheckFields()) {
                        preset.tagItems.add(check.getKey() + "\t", this);
                    }
                } else if (!(field instanceof PresetFixedField)) {
                    String key = e.getKey();
                    preset.tagItems.add(key + "\t", this);
                    if (field instanceof PresetComboField) {
                        StringWithDescription[] values = ((PresetComboField) field).getValues();
                        for (StringWithDescription swd : values) {
                            preset.tagItems.add(e.getKey() + "\t" + swd.getValue(), this);
                        }
                        addToAutosuggest(key, values);
                    }
                }
            }
        }

        itemIndex = preset.allItems.size();
        preset.allItems.add(this);
    }

    /**
     * Add the values to the autosuggest maps for the key
     * 
     * @param key the key
     * @param values array of the values
     */
    void addToAutosuggest(String key, StringWithDescription[] values) {
        if (appliesTo(ElementType.NODE)) {
            preset.autosuggestNodes.add(key, values);
        }
        if (appliesTo(ElementType.WAY)) {
            preset.autosuggestWays.add(key, values);
        }
        if (appliesTo(ElementType.CLOSEDWAY)) {
            preset.autosuggestClosedways.add(key, values);
        }
        if (appliesTo(ElementType.RELATION)) {
            preset.autosuggestRelations.add(key, values);
        }
        if (appliesTo(ElementType.AREA)) {
            preset.autosuggestAreas.add(key, values);
        }
    }

    /**
     * Add the value to the autosuggest maps for the key
     * 
     * @param key the key
     * @param value the value
     */
    void addToAutosuggest(String key, StringWithDescription value) {
        if (appliesTo(ElementType.NODE)) {
            preset.autosuggestNodes.add(key, value);
        }
        if (appliesTo(ElementType.WAY)) {
            preset.autosuggestWays.add(key, value);
        }
        if (appliesTo(ElementType.CLOSEDWAY)) {
            preset.autosuggestClosedways.add(key, value);
        }
        if (appliesTo(ElementType.RELATION)) {
            preset.autosuggestRelations.add(key, value);
        }
        if (appliesTo(ElementType.AREA)) {
            preset.autosuggestAreas.add(key, value);
        }
    }

    /**
     * build the search index
     */
    synchronized void buildSearchIndex() {
        addToSearchIndex(name, nameContext);
        if (parent != null) {
            String parentName = parent.getName();
            if (parentName != null && parentName.length() > 0) {
                addToSearchIndex(parentName, parent.nameContext);
            }
        }
        for (Entry<String, PresetFixedField> entry : fixedTags.entrySet()) {
            PresetFixedField fixedField = entry.getValue();
            StringWithDescription v = fixedField.getValue();
            String textContext = fixedField.getTextContext();
            addToSearchIndex(fixedField.getKey(), textContext);
            String hint = fixedField.getHint();
            if (hint != null) {
                addToSearchIndex(hint, textContext);
            }
            String value = v.getValue();
            String valueContext = fixedField.getValueContext();
            addToSearchIndex(value, valueContext);
            addToSearchIndex(v.getDescription(), valueContext);
            // support subtypes
            PresetField subTypeField = fields.get(value);
            if (subTypeField instanceof PresetComboField) {
                PresetComboField presetComboField = (PresetComboField) subTypeField;
                StringWithDescription[] subtypes = presetComboField.getValues();
                if (subtypes != null) {
                    String valuesContext = presetComboField.getValuesContext();
                    for (StringWithDescription subtype : subtypes) {
                        addToSearchIndex(subtype.getValue(), valuesContext);
                        addToSearchIndex(subtype.getDescription(), valuesContext);
                    }
                    presetComboField.setValuesSearchable(false);
                }
            }
        }
        for (Entry<String, PresetField> entry : fields.entrySet()) {
            PresetField field = entry.getValue();
            if (field.getValueType() == ValueType.INTEGER) {
                continue;
            }
            String textContext = field.getTextContext();
            if (!(field instanceof PresetCheckGroupField)) {
                addToSearchIndex(field.getKey(), textContext);
                String hint = field.getHint();
                if (hint != null) {
                    addToSearchIndex(hint, textContext);
                }
                if (field instanceof PresetComboField) {
                    PresetComboField presetComboField = (PresetComboField) field;
                    if (presetComboField.getValuesSearchable() && presetComboField.getValues() != null) {
                        String valuesContext = presetComboField.getValuesContext();
                        for (StringWithDescription value : presetComboField.getValues()) {
                            addToSearchIndex(value.getValue(), valuesContext);
                            addToSearchIndex(value.getDescription(), valuesContext);
                        }
                    }
                }
            } else {
                for (PresetCheckField check : ((PresetCheckGroupField) field).getCheckFields()) {
                    addToSearchIndex(check.getKey(), textContext);
                    String hint = field.getHint();
                    if (hint != null) {
                        addToSearchIndex(hint, textContext);
                    }
                    StringWithDescription value = check.getOnValue();
                    String valueContext = check.getValueContext();
                    addToSearchIndex(value.getValue(), valueContext);
                    addToSearchIndex(value.getDescription(), valueContext);
                    value = check.getOffValue();
                    if (value != null && !"".equals(value.getValue())) {
                        addToSearchIndex(value.getValue(), valueContext);
                        addToSearchIndex(value.getDescription(), valueContext);
                    }
                }
            }
        }
    }

    /**
     * Add a name, any translation and the individual words to the index. Currently we assume that all words are
     * significant
     * 
     * @param term search key to add
     * @param translationContext the translation context if any
     */
    void addToSearchIndex(@Nullable String term, @Nullable String translationContext) {
        // search support
        if (term != null) {
            String normalizedName = SearchIndexUtils.normalize(term);
            preset.searchIndex.add(normalizedName, this);
            String[] words = normalizedName.split(" ");
            if (words.length > 1) {
                for (String w : words) {
                    preset.searchIndex.add(w, this);
                }
            }
            if (preset.po != null) { // and any translation
                String normalizedTranslatedName = SearchIndexUtils.normalize(preset.po.t(translationContext, term));
                preset.translatedSearchIndex.add(normalizedTranslatedName, this);
                String[] translastedWords = normalizedName.split(" ");
                if (translastedWords.length > 1) {
                    for (String w : translastedWords) {
                        preset.translatedSearchIndex.add(w, this);
                    }
                }
            }
        }
    }

    /**
     * Adds a fixed tag to the item, registers the item in the tagItems map and populates autosuggest.
     * 
     * @param key key name of the tag
     * @param type PresetType
     * @param value value of the tag
     * @param text description of the tag return the allocated PresetField
     * @return the allocated PresetField
     */
    @NonNull
    public PresetField addTag(final String key, final PresetKeyType type, @Nullable String value, @Nullable String text) {
        if (key == null) {
            throw new NullPointerException("null key not supported");
        }
        if (value == null) {
            value = "";
        }
        if (text != null && preset.po != null) {
            text = preset.po.t(text);
        }
        PresetFixedField field = new PresetFixedField(key, new StringWithDescription(value, text));

        fixedTags.put(key, field);
        fields.put(key, field);
        if (!chunk) {
            preset.tagItems.add(key + "\t" + value, this);
            addToAutosuggest(key, value.length() > 0 ? new StringWithDescription(value, text) : null);
        }
        return field;
    }

    /**
     * Adds a recommended or optional tag to the item and populates autosuggest.
     * 
     * @param optional true if optional, false if recommended
     * @param key key name of the tag
     * @param type type of preset field
     * @param value value string from the XML (comma-separated list if more than one possible values)
     * @param matchType the applicable MatchType
     * @return the allocated PresetField
     */
    @NonNull
    public PresetField addTag(boolean optional, @NonNull String key, PresetKeyType type, String value, MatchType matchType) {
        return addTag(optional, key, type, value, null, null, Preset.COMBO_DELIMITER, matchType);
    }

    /**
     * Adds a recommended or optional tag to the item and populates autosuggest
     * 
     * @param optional true if optional, false if recommended
     * @param key key name of the tag
     * @param type type of preset field
     * @param value value string from the XML (delimiter-separated list if more than one possible values)
     * @param displayValue matching display value for value (same format for more than one)
     * @param shortDescriptions matching short description for value (same format for more than one)
     * @param delimiter the delimiter if more than one value is present
     * @param matchType the applicable MatchType
     * @return the allocated PresetField
     */
    @NonNull
    public PresetField addTag(boolean optional, @NonNull String key, PresetKeyType type, @Nullable String value, @Nullable String displayValue,
            @Nullable String shortDescriptions, final String delimiter, MatchType matchType) {
        String[] valueArray = (value == null) ? new String[0] : value.split(Pattern.quote(delimiter));
        String[] displayValueArray = (displayValue == null) ? new String[0] : displayValue.split(Pattern.quote(delimiter));
        String[] shortDescriptionArray = (shortDescriptions == null) ? new String[0] : shortDescriptions.split(Pattern.quote(delimiter));
        StringWithDescription[] valuesWithDesc = new StringWithDescription[valueArray.length];
        boolean useDisplayValues = valueArray.length == displayValueArray.length;
        boolean useShortDescriptions = !useDisplayValues && valueArray.length == shortDescriptionArray.length;
        for (int i = 0; i < valueArray.length; i++) {
            String valueDescription = null;
            if (useDisplayValues) {
                valueDescription = displayValueArray[i];
            } else if (useShortDescriptions) {
                valueDescription = shortDescriptionArray[i];
            }
            valuesWithDesc[i] = new StringWithDescription(valueArray[i], valueDescription);
        }
        return addTag(optional, key, type, valuesWithDesc, delimiter, matchType);
    }

    /**
     * Adds a recommended or optional tag to the item and populates autosuggest
     * 
     * @param optional true if optional, false if recommended
     * @param key key name of the tag
     * @param type type of preset field
     * @param valueCollection Collection with the values
     * @param delimiter the delimiter if more than one value is present
     * @param matchType the applicable MatchType
     * @return the allocated PresetField
     */
    @NonNull
    public PresetField addTag(boolean optional, @NonNull String key, PresetKeyType type, Collection<StringWithDescription> valueCollection,
            final String delimiter, MatchType matchType) {
        return addTag(optional, key, type, valueCollection.toArray(new StringWithDescription[valueCollection.size()]), delimiter, matchType);
    }

    /**
     * Adds a recommended or optional tag to the item and populates autosuggest
     * 
     * @param optional true if optional, false if recommended
     * @param key key name of the tag
     * @param type type of preset field
     * @param valueArray array with the values
     * @param delimiter the delimiter if more than one value is present
     * @param matchType the applicable MatchType
     * @return the allocated PresetField
     */
    @NonNull
    public PresetField addTag(boolean optional, @NonNull String key, PresetKeyType type, StringWithDescription[] valueArray, final String delimiter,
            MatchType matchType) { // NOSONAR
        addValues(key, valueArray, matchType);
        PresetField field = null;
        switch (type) {
        case COMBO:
        case MULTISELECT:
            field = new PresetComboField(key, valueArray);
            ((PresetComboField) field).setMultiSelect(type == PresetKeyType.MULTISELECT);
            if (!Preset.MULTISELECT_DELIMITER.equals(delimiter) || !Preset.COMBO_DELIMITER.equals(delimiter)) {
                ((PresetComboField) field).delimiter = delimiter;
            }
            break;
        case TEXT:
            field = new PresetTextField(key);
            break;
        case CHECK:
            Log.e(DEBUG_TAG, "check fields should not be handled here");
            throw new IllegalArgumentException("check fields should not be handled here");
        }
        field.setMatchType(matchType); // NOSONAR field can't be null here
        field.setOptional(optional); // NOSONAR field can't be null here
        fields.put(key, field);
        return field;
    }

    /**
     * Add key and values to tagItems and autosuggest
     * 
     * @param key the key
     * @param valueArray the suggested values
     * @param matchType type of match
     */
    synchronized void addValues(String key, StringWithDescription[] valueArray, @Nullable MatchType matchType) {
        if (!chunk) {
            if (MatchType.KEY == matchType) {
                preset.tagItems.add(key + "\t", this);
            }
            if (valueArray != null && valueArray.length > 0) {
                for (StringWithDescription v : valueArray) {
                    preset.tagItems.add(key + "\t" + v.getValue(), this);
                }
                addToAutosuggest(key, valueArray);
            } else {
                addToAutosuggest(key, new StringWithDescription(""));
            }
        }
    }

    /**
     * Remove this PresetItem as far as possible
     */
    public void delete() {
        for (String key : preset.searchIndex.getKeys()) {
            preset.searchIndex.removeItem(key, this);
        }
        for (String key : preset.translatedSearchIndex.getKeys()) {
            preset.searchIndex.removeItem(key, this);
        }
        for (String key : preset.tagItems.getKeys()) {
            preset.tagItems.removeItem(key, this);
        }
        preset.removeRecentlyUsed(this);
        getParent().removeElement(this);
        setParent(null);
    }

    /**
     * Add a PresetField to the PresetItem
     * 
     * @param field the PresetField
     */
    public void addField(@NonNull PresetField field) {
        fields.put(field.key, field);
        if (field instanceof PresetFixedField) {
            fixedTags.put(field.key, (PresetFixedField) field);
        }
    }

    /**
     * Get the PresetField associated with a key
     * 
     * @param key the key
     * @return a PresetField or null if none found
     */
    @Nullable
    public PresetField getField(@NonNull String key) {
        PresetField field = fields.get(key);
        if (field == null) { // check PresetGroupFields, not very efficient
            for (PresetField f : fields.values()) {
                if (f instanceof PresetCheckGroupField) {
                    field = ((PresetCheckGroupField) f).getCheckField(key);
                    if (field != null) {
                        return f;
                    }
                }
            }
        }
        return field;
    }

    /**
     * Add a PresetRole to this PresetItem
     * 
     * @param role the role to add
     */
    public void addRole(@NonNull final PresetRole role) {
        if (roles == null) {
            roles = new LinkedList<>();
        }
        roles.add(role);
    }

    /**
     * Add a LinkedList of PresetRoles to the item
     * 
     * @param newRoles the PresetRoles to add
     */
    public void addAllRoles(@Nullable LinkedList<PresetRole> newRoles) {
        if (roles == null) {
            roles = newRoles; // doesn't matter if newRoles is null
        } else if (newRoles != null) {
            for (PresetRole role : newRoles) {
                if (!roles.contains(role)) {
                    roles.add(role);
                }
            }
        }
    }

    /**
     * Get any applicable roles for this PresetItem
     * 
     * @return a List of PresetRoles or null if none
     */
    @Nullable
    public List<PresetRole> getRoles() {
        return roles != null ? Collections.unmodifiableList(roles) : null;
    }

    /**
     * Get any applicable roles for this PresetItem
     * 
     * @param type the OsmElement type as a string (NODE, WAY, RELATION)
     * @return a List of PresetRoles or null if none
     */
    @Nullable
    public List<PresetRole> getRoles(@Nullable String type) {
        List<PresetRole> result = null;
        if (roles != null) {
            result = new ArrayList<>();
            for (PresetRole role : roles) {
                if (role.appliesTo(type)) {
                    result.add(role);
                }
            }
        }
        return result;
    }

    /**
     * Get any applicable roles for this PresetItem, considers object type and member_expression
     * 
     * @param context an Android Context
     * @param element the OsmElement that is the relation member
     * @param tags alternative Map of tags to use
     * @return a List of PresetRoles or null if none
     */
    @Nullable
    public List<PresetRole> getRoles(@NonNull Context context, @NonNull OsmElement element, @Nullable Map<String, String> tags) {
        List<PresetRole> result = null;
        if (roles != null) {
            result = new ArrayList<>();
            Wrapper wrapper = new Wrapper(context);
            wrapper.setElement(element);
            ElementType type = element.getType();
            Map<String, String> tagsToUse = tags != null ? tags : element.getTags();
            for (PresetRole role : roles) {
                if (role.appliesTo(type)) {
                    String memberExpression = role.getMemberExpression();
                    if (memberExpression != null) {
                        JosmFilterParser parser = new JosmFilterParser(new ByteArrayInputStream(memberExpression.getBytes()));
                        try { // test if this matches the member expression
                            if (!parser.condition().eval(Wrapper.toJosmFilterType(element), wrapper, tagsToUse)) {
                                continue;
                            }
                        } catch (ch.poole.osm.josmfilterparser.ParseException | IllegalArgumentException e) {
                            Log.e(DEBUG_TAG, "member_expression " + memberExpression + " caused " + e.getMessage());
                        }
                    }
                    result.add(role);
                }
            }
        }
        return result;
    }

    /**
     * Save hint for the tag
     * 
     * @param key tag key this should be set for
     * @param hint hint value
     */
    public void setHint(@NonNull String key, @Nullable String hint) {
        PresetField field = fields.get(key);
        if (field != null) {
            field.setHint(hint);
        }
    }

    /**
     * Return, potentially translated, "text" field from preset
     * 
     * @param key tag key we want the hint for
     * @return the hint for this field or null
     */
    @Nullable
    public String getHint(@NonNull String key) {
        PresetField field = fields.get(key);
        if (field == null) {
            field = getCheckFieldFromGroup(key);
        }
        if (field != null) {
            return field.getHint();
        }
        return null;
    }

    /**
     * Get a default value for the key or null
     * 
     * @param key key this default value is used for
     * @return the default value of null if none
     */
    @Nullable
    public String getDefault(@NonNull String key) {
        PresetField field = fields.get(key);
        if (field == null) {
            field = getCheckFieldFromGroup(key);
        }
        return field != null ? field.getDefaultValue() : null;
    }

    /**
     * Get a non-standard delimiter character for a combo or multiselect
     * 
     * @param key the tag key this delimiter is for
     * @return the delimiter
     */
    @NonNull
    public char getDelimiter(@NonNull String key) {
        PresetField field = fields.get(key);
        if (field instanceof PresetComboField) {
            PresetComboField combo = (PresetComboField) field;
            return (combo.delimiter != null ? combo.delimiter : (combo.isMultiSelect() ? Preset.MULTISELECT_DELIMITER : Preset.COMBO_DELIMITER)).charAt(0);
        } else {
            Log.e(DEBUG_TAG,
                    "Trying to get delimiter from non-combo field, item " + name + " key " + key + " " + (field != null ? field.getClass().getName() : "null"));
            return Preset.COMBO_DELIMITER.charAt(0);
        }
    }

    /**
     * Get the match type for a key
     * 
     * @param key tag key we want the match type for
     * @return the MatchType for this key or null
     */
    @Nullable
    public MatchType getMatchType(@NonNull String key) {
        PresetField field = fields.get(key);
        if (field == null) {
            field = getCheckFieldFromGroup(key);
        }
        return field != null ? field.matchType : null;
    }

    /**
     * See if a key matches a PresetCheckField in PresetCheckGroupField and return it
     * 
     * @param key the key
     * @return a PresetCheckField or null if not found
     */
    @Nullable
    public PresetField getCheckFieldFromGroup(String key) {
        for (PresetField f : fields.values()) {
            if (f instanceof PresetCheckGroupField) {
                PresetCheckField check = ((PresetCheckGroupField) f).getCheckField(key);
                if (check != null) {
                    return check;
                }
            }
        }
        return null;
    }

    /**
     * Get the ValueType for this key
     * 
     * @param key the key to check
     * @return the ValueType of null if none set
     */
    @Nullable
    public ValueType getValueType(@NonNull String key) {
        PresetField field = fields.get(key);
        if (field != null) {
            return field.getValueType();
        }
        return null;
    }

    /**
     * Add a linked preset to the PresetItem
     * 
     * @param presetLink the PresetLink
     */
    public void addLinkedPresetItem(@NonNull PresetItemLink presetLink) {
        if (linkedPresetItems == null) {
            linkedPresetItems = new LinkedList<>();
        }
        linkedPresetItems.add(presetLink);
    }

    /**
     * Add a linked alternative preset to the PresetItem
     * 
     * @param presetLink the PresetLink
     */
    public void addAlternativePresetItem(@NonNull PresetItemLink presetLink) {
        if (alternativePresetItems == null) {
            alternativePresetItems = new LinkedList<>();
        }
        alternativePresetItems.add(presetLink);
    }

    /**
     * Add a LinkedList containing linked PresetItems to this PresetItem
     * 
     * @param newLinkedPresetItems the LinkedList of PresetLinks
     */
    public void addAllLinkedPresetItems(@Nullable LinkedList<PresetItemLink> newLinkedPresetItems) { // NOSONAR
        if (linkedPresetItems == null) {
            linkedPresetItems = newLinkedPresetItems; // doesn't matter if newLinkedPresetNames is null
        } else if (newLinkedPresetItems != null) {
            for (PresetItemLink linkedPreset : newLinkedPresetItems) {
                if (!linkedPresetItems.contains(linkedPreset)) {
                    linkedPresetItems.add(linkedPreset);
                }
            }
        }
    }

    /**
     * Add a LinkedList containing alternative PresetItems to this PresetItem
     * 
     * @param newalternativePresetItems the LinkedList of PresetLinks
     */
    public void addAllAlternativePresetItems(@Nullable LinkedList<PresetItemLink> newalternativePresetItems) { // NOSONAR
        if (alternativePresetItems == null) {
            alternativePresetItems = newalternativePresetItems; // doesn't matter if newLinkedPresetNames is null
        } else if (newalternativePresetItems != null) {
            for (PresetItemLink linkedPreset : newalternativePresetItems) {
                if (!linkedPresetItems.contains(linkedPreset)) {
                    linkedPresetItems.add(linkedPreset);
                }
            }
        }
    }

    /**
     * Get all linked PresetItems
     * 
     * @return a list of all linked PresetItems or null if none
     */
    @Nullable
    public List<PresetItemLink> getLinkedPresetItems() {
        return linkedPresetItems;
    }

    /**
     * Get all alternative PresetItems
     * 
     * @return a list of all alternative PresetItems or null if none
     */
    @Nullable
    public List<PresetItemLink> getAlternativePresetItems() {
        return alternativePresetItems;
    }

    /**
     * Returns a list of linked preset items
     * 
     * @param noPrimary if true only items will be returned that doen't correspond to primary OSM objects
     * @param otherPresets other Presets beside this one to search in
     * @return list of PresetItems
     */
    @NonNull
    public List<PresetItem> getLinkedPresets(boolean noPrimary, @Nullable Preset[] otherPresets) {
        List<PresetItem> result = new ArrayList<>();
        List<Preset> presets = new ArrayList<>();
        if (otherPresets != null) {
            presets.addAll(Arrays.asList(otherPresets));
            presets.remove(preset);
        }
        presets.add(0, preset); // move this Preset to front
        if (linkedPresetItems != null) {
            for (PresetItemLink pl : linkedPresetItems) {
                for (Preset preset : presets) {
                    if (preset != null) {
                        Integer index = preset.getItemIndexByName(pl.getPresetName()); // FIXME this involves a
                                                                                       // sequential search
                        if (index != null) {
                            PresetItem candidateItem = preset.allItems.get(index);
                            if (!noPrimary || !candidateItem.isObject(preset)) { // remove primary objects
                                result.add(candidateItem);
                            }
                            break;
                        } else {
                            Log.e(DEBUG_TAG, "Couldn't find linked preset " + pl.getPresetName());
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Check if this PresetItem represents an irl object
     * 
     * @param preset the Preset to
     * @return true if a irl object
     */
    private boolean isObject(@NonNull Preset preset) {
        Set<String> tags = getFixedTags().keySet();
        if (tags.isEmpty()) {
            tags = getFields().keySet();
        }
        for (String k : tags) {
            if (Tags.IMPORTANT_TAGS.contains(k) || preset.isObjectKey(k)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if values should be sorted
     * 
     * @param key combo/multiselect key
     * @return true if the values should be alphabetically sorted
     */
    public boolean sortValues(@NonNull String key) {
        PresetField field = fields.get(key);
        if (field instanceof PresetComboField) {
            return ((PresetComboField) field).getSortValues();
        }
        return false;
    }

    /**
     * Get all keys in the item that support i18n
     * 
     * @return a Set with the keys or null
     */
    @NonNull
    public Set<String> getI18nKeys() {
        Set<String> result = new HashSet<>();
        for (PresetField field : fields.values()) {
            if (field instanceof PresetTextField && ((PresetTextField) field).isI18n()) {
                result.add(((PresetTextField) field).key);
            }
        }
        return result;
    }

    /**
     * Indicate that this PresetITem is a chunk
     */
    void setChunk() {
        chunk = true;
    }

    /**
     * Check if this PresetItem is a chunk
     * 
     * @return true if this PresetItem is a chunk
     */
    boolean isChunk() {
        return chunk;
    }

    /**
     * Determine if this preset can be autoapplied or not
     * 
     * @return true if this preset can be autoapplied
     */
    public boolean autoapply() {
        return autoapply;
    }

    /**
     * Set the autoapply value
     * 
     * @param autoapply if true the preset is suitable for autoapplying
     */
    void setAutoapply(boolean autoapply) {
        this.autoapply = autoapply;
    }

    /**
     * Get the minimum number of fixed tags that need to match
     * 
     * @return the minimum number of fixed tags that need to match if &lt;= 0 the number of fixed tags in the preset
     *         item will be used
     */
    public short getMinMatch() {
        return minMatch;
    }

    /**
     * @return the fixed tags belonging to this item (unmodifiable)
     */
    public Map<String, PresetFixedField> getFixedTags() {
        return Collections.unmodifiableMap(fixedTags);
    }

    /**
     * Return the number of keys with fixed values
     * 
     * @return number of fixed tags
     */
    public int getFixedTagCount() {
        return fixedTags.size();
    }

    /**
     * Check if the tag has a fixed value
     * 
     * @param key key to check
     * @return true if this is a fixed key - value combination
     */
    public boolean isFixedTag(String key) {
        return fixedTags.containsKey(key);
    }

    /**
     * Test if the key is optional for this PresetITem
     * 
     * @param key the key to check
     * @return true if the key is optional
     */
    public boolean isOptionalTag(String key) {
        PresetField field = fields.get(key);
        return field != null && field.isOptional();
    }

    /**
     * Get the number of "recommended" keys aka non-fixed and non-optional Note: this only calculates the value once and
     * then uses a cached version
     * 
     * @return the number of "recommended" keys
     */
    int getRecommendedKeyCount() {
        if (recommendedKeyCount >= 0) {
            return recommendedKeyCount;
        }
        int count = 0;
        for (PresetField field : fields.values()) {
            if (!field.isOptional() && !(field instanceof PresetFixedField)) {
                count++;
            }
        }
        recommendedKeyCount = count;
        return count;
    }

    /**
     * Get an (ordered and unmodifiable) Map of the PresetFields
     * 
     * @return an unmodifiable Map
     */
    public Map<String, PresetField> getFields() {
        return Collections.unmodifiableMap(fields);
    }

    /**
     * Return a ist of the values suitable for autocomplete, note values for fixed tags are not returned
     * 
     * @param key key to get values for
     * @return Collection of StringWithDescription objects
     */
    @NonNull
    public Collection<StringWithDescription> getAutocompleteValues(@NonNull String key) {
        PresetField field = fields.get(key);
        if (field == null) {
            field = getCheckFieldFromGroup(key);
        }
        return getAutocompleteValues(field);
    }

    /**
     * Return a ist of the values suitable for autocomplete, note values for fixed tags are not returned
     * 
     * @param field the PresetField to get values for
     * @return Collection of StringWithDescription objects
     */
    @NonNull
    public Collection<StringWithDescription> getAutocompleteValues(@NonNull PresetField field) {
        Collection<StringWithDescription> result = new LinkedHashSet<>();
        if (field instanceof PresetComboField) {
            result.addAll(Arrays.asList(((PresetComboField) field).getValues()));
        } else if (field instanceof PresetCheckField) {
            result.add(((PresetCheckField) field).getOnValue());
            StringWithDescription offValue = ((PresetCheckField) field).getOffValue();
            if (offValue != null) {
                result.add(offValue);
            }
        }
        return result;
    }

    /**
     * Get the description for a specific value of a tag
     * 
     * @param key the key
     * @param value the value which we want the description for
     * @return the description or null if not found
     */
    @Nullable
    public String getDescriptionForValue(@NonNull String key, @NonNull String value) {
        Collection<StringWithDescription> presetValues = getAutocompleteValues(key);
        for (StringWithDescription swd : presetValues) {
            if (swd.getValue().equals(value)) {
                return swd.getDescription();
            }
        }
        return null;
    }

    /**
     * Return what kind of selection applies to the values of this key
     * 
     * @param key the key
     * @return the selection type for this key, null key doesn't exist
     */
    @Nullable
    public PresetKeyType getKeyType(@NonNull String key) {
        PresetField field = fields.get(key);
        if (field == null) {
            field = getCheckFieldFromGroup(key);
        }
        if (field instanceof PresetFixedField || field instanceof PresetTextField) {
            return PresetKeyType.TEXT;
        } else if (field instanceof PresetCheckField) {
            return PresetKeyType.CHECK;
        } else if (field instanceof PresetComboField) {
            if (((PresetComboField) field).isMultiSelect()) {
                return PresetKeyType.MULTISELECT;
            } else {
                return PresetKeyType.COMBO;
            }
        }
        return null;
    }

    /**
     * Checks the fixed tags belonging to this item exist in the given tags
     * 
     * Fields with MatchType.NONE will be ignored
     * 
     * @param tagSet Map containing tags to compare against this preset item
     * @return true if the tagSet matches (all the fixed fields need to be present)
     */
    public boolean matches(@NonNull Map<String, String> tagSet) {
        int matchCount = 0;
        int fixedTagsCount = fixedTags.size();
        for (Entry<String, PresetFixedField> tag : fixedTags.entrySet()) { // for each own tag
            PresetFixedField field = tag.getValue();
            if (field.matchType == MatchType.NONE) {
                fixedTagsCount--;
                continue;
            }
            String key = tag.getKey();
            String value = tagSet.get(key);
            if (value != null && field.getValue().equals(value)) { // key and value match
                matchCount++;
            }
        }
        return minMatch > 0 ? matchCount >= minMatch : matchCount == fixedTagsCount;
    }

    /**
     * Returns the number of matches between the list of non-optional tags and the provided tags
     * 
     * Uses the match value to control actual behavior
     * 
     * @param tagMap Map containing the tags
     * @return number of matches
     */
    int matchesRecommended(@NonNull Map<String, String> tagMap) {
        int matches = 0;

        List<PresetField> allFields = new ArrayList<>();
        for (PresetField field : fields.values()) {
            if (field instanceof PresetCheckGroupField) {
                allFields.addAll(((PresetCheckGroupField) field).getCheckFields());
            } else {
                allFields.add(field);
            }
        }

        for (PresetField field : allFields) { // for each own tag
            String key = field.getKey();
            if (field.isOptional() || field instanceof PresetFixedField) {
                continue;
            }
            MatchType type = field.matchType;
            if (tagMap.containsKey(key)) { // key could have null value in the set
                // value not empty
                if (type == MatchType.NONE) {
                    // don't count this
                    continue;
                }
                if (type == MatchType.KEY || type == MatchType.KEY_NEG) {
                    matches++;
                    continue;
                }
                String otherTagValue = tagMap.get(key);
                if (field instanceof PresetComboField && ((PresetComboField) field).getValues() != null) {
                    boolean matched = false;
                    for (StringWithDescription v : ((PresetComboField) field).getValues()) {
                        if (v.equals(otherTagValue)) {
                            matched = true;
                            break;
                        }
                    }
                    if (matched) {
                        matches++;
                    } else if (type == MatchType.KEY_VALUE_NEG) {
                        matches--;
                    }
                } else if (field instanceof PresetCheckField) {
                    String onValue = ((PresetCheckField) field).getOnValue().getValue();
                    String offValue = ((PresetCheckField) field).getOnValue() != null ? ((PresetCheckField) field).getOnValue().getValue() : null;
                    if (otherTagValue.equals(onValue) || otherTagValue.equals(offValue)) {
                        matches++;
                    } else if (type == MatchType.KEY_VALUE_NEG) {
                        matches--;
                    }
                }
            } else {
                if (type == MatchType.KEY_NEG || type == MatchType.KEY_VALUE_NEG) {
                    matches--;
                }
            }
        }
        return matches;
    }

    @Override
    public View getView(Context ctx, final PresetClickHandler handler, boolean selected) {
        View v = super.getBaseView(ctx, selected);
        if (handler != null) {
            v.setOnClickListener(view -> handler.onItemClick(PresetItem.this));
            v.setOnLongClickListener(view -> handler.onItemLongClick(PresetItem.this));
        }
        v.setBackgroundColor(ContextCompat.getColor(ctx, selected ? R.color.material_deep_teal_500 : R.color.preset_bg));
        v.setTag(Integer.toString(this.getItemIndex()));
        return v;
    }

    /**
     * Return true if the key is contained in this preset
     * 
     * @param key key to look for
     * @return true if the key is present in any category (fixed, recommended, optional)
     */
    public boolean hasKey(@NonNull String key) {
        return hasKey(key, true);
    }

    /**
     * Return true if the key is contained in this preset
     * 
     * @param key key to look for
     * @param checkOptional check in optional tags too
     * @return true if the key is present in any category (fixed, recommended, and optional if checkOptional is true)
     */
    public boolean hasKey(@NonNull String key, boolean checkOptional) {
        PresetField field = fields.get(key);
        return field != null && (!field.isOptional() || (checkOptional && field.isOptional()));
    }

    /**
     * Return true if the key and value is contained in this preset taking match attribute in to account
     * 
     * Note match="none" is handled the same as "key" in this method
     * 
     * @param key key to look for
     * @param value value to look for
     * @return true if the key- value combination is present in any category (fixed, recommended, and optional)
     */
    public boolean hasKeyValue(@NonNull String key, @Nullable String value) {

        PresetField field = fields.get(key);
        return Preset.hasKeyValue(field, value);
    }

    /**
     * Get the index of this item
     * 
     * @return the index
     */
    public int getItemIndex() {
        return itemIndex;
    }

    @Override
    public String toString() {
        StringBuilder tagStrings = new StringBuilder(" ");
        for (Entry<String, PresetField> entry : fields.entrySet()) {
            PresetField field = entry.getValue();
            tagStrings.append(" ");
            tagStrings.append(field.toString());
        }
        return super.toString() + tagStrings.toString();
    }

    /**
     * Create a JSON representation of this item
     * 
     * @return JSON format string
     */
    public String toJSON() {
        StringBuilder presetNameBuilder = new StringBuilder(name);
        PresetElement p = getParent();
        while (p != null && p != preset.rootGroup && !"".equals(p.getName())) {
            presetNameBuilder.insert(0, '/');
            presetNameBuilder.insert(0, p.getName());
            p = p.getParent();
        }
        String presetName = presetNameBuilder.toString();
        StringBuilder jsonString = new StringBuilder();
        for (Entry<String, PresetFixedField> entry : fixedTags.entrySet()) {
            if (jsonString.length() != 0) {
                jsonString.append(",\n");
            }
            jsonString.append(tagToJSON(presetName, entry.getKey(), entry.getValue().getValue()));
        }
        for (Entry<String, PresetField> entry : fields.entrySet()) {
            PresetField field = entry.getValue();
            if (field instanceof PresetFixedField) {
                continue;
            }
            // check match attribute
            String k = entry.getKey();
            MatchType match = getMatchType(k);
            boolean editable = field instanceof PresetComboField && ((PresetComboField) field).isEditable();
            if (editable || field instanceof PresetTextField || field instanceof PresetCheckField
                    || (match != null && match != MatchType.KEY_VALUE && match != MatchType.KEY)) {
                if (jsonString.length() != 0) {
                    jsonString.append(",\n");
                }
                jsonString.append(tagToJSON(presetName, k, null));
            }
            if (field instanceof PresetComboField && !editable
                    && (match == null || match == MatchType.KEY_VALUE || match == MatchType.KEY || match == MatchType.KEY_VALUE_NEG)) {
                for (StringWithDescription v : ((PresetComboField) entry.getValue()).getValues()) {
                    if (jsonString.length() != 0) {
                        jsonString.append(",\n");
                    }
                    jsonString.append(tagToJSON(presetName, k, v));
                }
            }
        }
        return jsonString.toString();
    }

    /**
     * For taginfo.openstreetmap.org Projects
     * 
     * @param presetName the name of the PresetItem
     * @param key tag key
     * @param value tag value
     * @return JSON representation of a single tag
     */
    @NonNull
    private String tagToJSON(@NonNull String presetName, @NonNull String key, @Nullable StringWithDescription value) {
        StringBuilder result = new StringBuilder(
                "{\"description\":\"" + presetName + "\",\"key\": \"" + key + "\"" + (value == null ? "" : ",\"value\": \"" + value.getValue() + "\""));
        result.append(",\"object_types\": [");
        boolean first = true;
        if (appliesToNode) {
            result.append("\"node\"");
            first = false;
        }
        if (appliesToWay) {
            if (!first) {
                result.append(",");
            }
            result.append("\"way\"");
            first = false;
        }
        if (appliesToRelation) {
            if (!first) {
                result.append(",");
            }
            result.append("\"relation\"");
            first = false;
        }
        if (appliesToClosedway || appliesToArea) {
            if (!first) {
                result.append(",");
            }
            result.append("\"area\"");
        }
        return result.append("]}").toString();
    }

    /**
     * Arrange any i18n keys that have dynamically been added to this preset
     * 
     * @param i18nKeys List of candidate i18n keys
     */
    public void groupI18nKeys(List<String> i18nKeys) {
        LinkedHashMap<String, PresetField> temp = new LinkedHashMap<>();
        ArrayList<String> keys = new ArrayList<>(fields.keySet());
        while (!keys.isEmpty()) {
            String key = keys.get(0);
            keys.remove(0);
            if (i18nKeys.contains(key)) {
                temp.put(key, fields.get(key));
                int i = 0;
                while (!keys.isEmpty() && i < keys.size()) {
                    String i18nKey = keys.get(i);
                    if (i18nKey.startsWith(key + ":")) {
                        temp.put(i18nKey, fields.get(i18nKey));
                        keys.remove(i);
                    } else {
                        i++;
                    }
                }
            } else {
                temp.put(key, fields.get(key));
            }
        }
        fields.clear();
        fields.putAll(temp);
    }

    @Override
    public void toXml(XmlSerializer s) throws IllegalArgumentException, IllegalStateException, IOException {
        s.startTag("", chunk ? Preset.CHUNK : Preset.ITEM);
        s.attribute("", Preset.NAME, name);
        String iconPath = getIconpath();
        if (iconPath != null) {
            s.attribute("", Preset.ICON, getIconpath());
        }
        StringBuilder builder = new StringBuilder();
        if (appliesTo(ElementType.NODE)) {
            builder.append(Node.NAME);
        }
        if (appliesTo(ElementType.WAY)) {
            if (builder.length() != 0) {
                builder.append(',');
            }
            builder.append(Way.NAME);
        }
        if (appliesTo(ElementType.CLOSEDWAY)) {
            if (builder.length() != 0) {
                builder.append(',');
            }
            builder.append(Preset.CLOSEDWAY);
        }
        if (appliesTo(ElementType.RELATION)) {
            if (builder.length() != 0) {
                builder.append(',');
            }
            builder.append(Relation.NAME);
        }
        if (appliesTo(ElementType.AREA)) {
            if (builder.length() != 0) {
                builder.append(',');
            }
            builder.append(Preset.MULTIPOLYGON);
        }
        s.attribute("", Preset.TYPE, builder.toString());
        String mapFeatures = getMapFeatures();
        if (mapFeatures != null) {
            s.startTag("", Preset.LINK);
            if (mapFeatures.startsWith(Urls.OSM_WIKI) || !mapFeatures.startsWith(HTTP)) {
                // wiki might or might not be present
                mapFeatures = mapFeatures.replace(Urls.OSM_WIKI, "").replace("wiki/", "");
                s.attribute("", Preset.WIKI, mapFeatures);
            } else {
                s.attribute("", Preset.HREF, mapFeatures);
            }
            s.endTag("", Preset.LINK);
        }
        for (PresetFixedField field : fixedTags.values()) {
            field.toXml(s);
        }
        fieldsToXml(s, fields);
        if (linkedPresetItems != null) {
            for (PresetItemLink linkedPreset : linkedPresetItems) {
                s.startTag("", Preset.PRESET_LINK);
                s.attribute("", Preset.PRESET_NAME, linkedPreset.getPresetName());
                if (linkedPreset.getText() != null) {
                    s.attribute("", Preset.TEXT, linkedPreset.getText());
                }
                if (linkedPreset.getTextContext() != null) {
                    s.attribute("", Preset.TEXT_CONTEXT, linkedPreset.getTextContext());
                }
                s.endTag("", Preset.PRESET_LINK);
            }
        }
        s.endTag("", chunk ? Preset.CHUNK : Preset.ITEM);
    }

    /**
     * Output the preset fields to XML Will add optional tags where necessary
     * 
     * @param fields a map containing the fields
     * @param s the serializer
     * @throws IOException
     */
    private void fieldsToXml(@NonNull XmlSerializer s, @NonNull Map<String, PresetField> fields) throws IOException {
        boolean inOptional = false;
        for (Entry<String, PresetField> entry : fields.entrySet()) {
            PresetField field = entry.getValue();
            if (field instanceof PresetFixedField) {
                continue;
            }
            if (!inOptional && field.isOptional()) {
                s.startTag("", Preset.OPTIONAL);
                inOptional = true;
            }
            if (inOptional && !field.isOptional()) {
                s.endTag("", Preset.OPTIONAL);
                inOptional = false;
            }
            field.toXml(s);
        }
        if (inOptional) {
            s.endTag("", Preset.OPTIONAL);
        }
    }
}