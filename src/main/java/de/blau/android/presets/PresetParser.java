package de.blau.android.presets;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.contract.Schemes;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.ExtendedStringWithDescription;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.StringWithDescriptionAndIcon;

public class PresetParser {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, PresetParser.class.getSimpleName().length());
    private static final String DEBUG_TAG = PresetParser.class.getSimpleName().substring(0, TAG_LEN);

    private static final String ALTERNATIVE           = "alternative";
    private static final String USE_LAST_AS_DEFAULT   = "use_last_as_default";
    static final String         NO                    = "no";
    static final String         VALUE_TYPE            = "value_type";
    static final String         PRESET_NAME           = "preset_name";
    static final String         PRESET_LINK           = "preset_link";
    static final String         SHORT_DESCRIPTION     = "short_description";
    private static final String DISPLAY_VALUE         = "display_value";
    static final String         LIST_ENTRY            = "list_entry";
    private static final String REFERENCE             = "reference";
    private static final String ROLE                  = "role";
    private static final String ROLES                 = "roles";
    private static final String VALUES_SEARCHABLE     = "values_searchable";
    static final String         EDITABLE              = "editable";
    static final String         VALUES_SORT           = "values_sort";
    private static final String VALUES_CONTEXT        = "values_context";
    private static final String SHORT_DESCRIPTIONS    = "short_descriptions";
    private static final String DISPLAY_VALUES        = "display_values";
    private static final String VALUES                = "values";
    private static final String VALUES_FROM           = "values_from";
    static final String         DELIMITER             = "delimiter";
    static final String         COMBO_FIELD           = "combo";
    static final String         MULTISELECT_FIELD     = "multiselect";
    static final String         YES                   = "yes";
    static final String         DISABLE_OFF           = "disable_off";
    static final String         VALUE_OFF             = "value_off";
    static final String         VALUE_ON              = "value_on";
    static final String         CHECK_FIELD           = "check";
    static final String         CHECKGROUP            = "checkgroup";
    static final String         HREF                  = "href";
    static final String         WIKI                  = "wiki";
    static final String         LINK                  = "link";
    private static final String I18N                  = "i18n";
    private static final String JAVASCRIPT            = "javascript";
    static final String         DEFAULT               = "default";
    static final String         TEXT_CONTEXT          = "text_context";
    private static final String TEXT_FIELD            = "text";
    static final String         TEXT                  = "text";
    static final String         VALUE                 = "value";
    private static final String NONE                  = "none";
    static final String         MATCH                 = "match";
    static final String         CHUNK                 = "chunk";
    static final String         KEY_ATTR              = "key";
    static final String         OPTIONAL              = "optional";
    static final String         SEPARATOR             = "separator";
    static final String         ITEM_SEPARATOR        = "item_separator";
    private static final String ID                    = "id";
    private static final String DEPRECATED            = "deprecated";
    static final String         TRUE                  = "true";
    private static final String FALSE                 = "false";
    private static final String GTYPE                 = "gtype";
    static final String         TYPE                  = "type";
    static final String         ITEM                  = "item";
    private static final String NAME_CONTEXT          = "name_context";
    static final String         ICON                  = "icon";
    private static final String IMAGE                 = "image";
    static final String         NAME                  = "name";
    private static final String NAME_TEMPLATE         = "name_template";
    private static final String OBJECT_KEYS           = "object_keys";
    static final String         OBJECT                = "object";
    static final String         GROUP                 = "group";
    static final String         PRESETS               = "presets";
    static final String         AREA                  = "area";
    static final String         MULTIPOLYGON          = "multipolygon";
    static final String         CLOSEDWAY             = "closedway";
    static final String         LABEL                 = "label";
    private static final String ITEMS_SORT            = "items_sort";
    static final String         SPACE                 = "space";
    static final String         HEIGHT_ATTR           = "height";
    private static final String LENGTH                = "length";
    private static final String REGIONS               = "regions";
    private static final String EXCLUDE_REGIONS       = "exclude_regions";
    private static final String AUTOAPPLY             = "autoapply";
    private static final String MIN_MATCH             = "min_match";
    private static final String REGEXP                = "regexp";
    private static final String COUNT                 = "count";
    private static final String REQUISITE             = "requisite";
    private static final String MEMBER_EXPRESSION     = "member_expression";
    private static final String REF                   = "ref";
    private static final String VALUE_COUNT_KEY       = "value_count_key";
    private static final String ON                    = "on";
    private static final String DESCRIPTION_ATTR      = "description";
    private static final String SHORTDESCRIPTION_ATTR = "shortdescription";
    private static final String VERSION_ATTR          = "version";
    private static final String BACKGROUND            = "background";
    private static final String MATCH_EXPRESSION      = "match_expression";
    private static final String LINKED_KEYS_ELEMENT = "linked_keys";
    private static final String KEY_ELEMENT = "key";
    private static final String NAME_ATTR = "name";
    private enum PARSE_STATE {
        TOP, ITEM, CHUNK
    }

    /**
     * Parses the preset XML and add fields to the provided preset
     * 
     * @param preset the preset
     * @param input the input stream from which to read XML data
     * @param supportLabels support labels in presets if true, otherwise ignore them
     * @throws ParserConfigurationException
     * @throws SAXException on parsing issues
     * @throws IOException when reading the presets fails
     */
    static void parseXML(@NonNull Preset preset, @NonNull InputStream input, boolean supportLabels)
            throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance(); // NOSONAR
        SAXParser saxParser = factory.newSAXParser();

        saxParser.parse(input, new DefaultHandler() {
            private PARSE_STATE                 state             = PARSE_STATE.TOP;
            /** stack of group-subgroup-subsubgroup... where we currently are */
            private Deque<PresetGroup>          groupstack        = new ArrayDeque<>();
            /** item currently being processed */
            private PresetItem                  currentItem       = null;
            /** true if we are currently processing the optional section of an item */
            private boolean                     inOptionalSection = false;
            /** hold reference to chunks */
            private Map<String, PresetChunk>    chunks            = new HashMap<>();
            /** store current combo or multiselect key */
            private String                      listKey           = null;
            private List<StringWithDescription> listValues        = null;
            private int                         imageCount        = 0;
            /** check groups */
            private PresetCheckGroupField       checkGroup        = null;
            private int                         checkGroupCounter = 0;
            /** */
            private String                      currentLabel      = null;
            private boolean                     addedLabel        = false;
            private StringWithDescription       currentListEntrySwd = null;
            private boolean                     isInLinkedKeys    = false;
            private List<String>                collectedLinkedKeys = null;
            /**
             * ${@inheritDoc}.
             */
            @Override
            public void startElement(String uri, String localName, String name, Attributes attr) throws SAXException {
                switch (state) {
                case TOP:
                    parseTop(name, attr);
                    break;
                case ITEM:
                    parseItemOrNestedField(name, attr);
                    break;
                case CHUNK:
                    PresetChunk currentChunk = (PresetChunk) currentItem;
                    if (currentChunk.getFields().isEmpty() && currentChunk.getListValues() == null) {
                        if (LIST_ENTRY.equals(name)) {
                            Log.v(DEBUG_TAG, "Chunk '" + currentChunk.getName() + "' identified as LIST based.");
                            currentChunk.setListValues(new ArrayList<>());
                            startListEntry(attr, currentChunk.getListValues());
                        } else {
                            Log.v(DEBUG_TAG, "Chunk '" + currentChunk.getName() + "' identified as FIELD based.");
                            parseItemOrNestedField(name, attr);
                        }
                    }
                    else {
                        if (currentChunk.getListValues() != null) {
                            if (LIST_ENTRY.equals(name)) {
                                startListEntry(attr, currentChunk.getListValues());
                            } else {
                                throw new SAXException("Chunk '" + currentChunk.getName() + "' expects only <" + LIST_ENTRY + "> but found <" + name + ">.");
                            }
                        }
                        else {
                            parseItemOrNestedField(name, attr);
                        }
                    }
                    break;
                }
            }
            private void parseItemOrNestedField(String name, Attributes attr) throws SAXException {
                // Are we inside a list_entry (parsing its children like linked_keys)?
                if (currentListEntrySwd != null) {
                    parseListEntrySubElement(name, attr);
                }
                // Is this the start of a list_entry *within a combo/multi context*?
                else if (LIST_ENTRY.equals(name)) {
                    if (listValues != null) { // Is a combo/multi active?
                        startListEntry(attr, listValues); // adding to the active combo's list
                    } else {
                        throw new SAXException("<" + LIST_ENTRY + "> found outside of expected <combo>/<multiselect> context.");
                    }
                }
                // a standard field definition or container (<optional>, <text>, <combo> etc.)
                else {
                    parseItem(name, attr);
                }
            }
            private void parseListEntrySubElement(String name, Attributes attr) throws SAXException {
                if (LINKED_KEYS_ELEMENT.equals(name)) {
                    if (isInLinkedKeys) {
                        throw new SAXException("Nested <" + LINKED_KEYS_ELEMENT + "> elements are not allowed.");
                    }
                    isInLinkedKeys = true;
                    collectedLinkedKeys = new ArrayList<>();
                    Log.v(DEBUG_TAG, "Entered <" + LINKED_KEYS_ELEMENT + ">");
                } else if (KEY_ELEMENT.equals(name)) {
                    if (!isInLinkedKeys) {
                        Log.w(DEBUG_TAG, "<" + KEY_ELEMENT + "> found outside of <" + LINKED_KEYS_ELEMENT + "> context. Ignoring.");
                        return;
                    }
                    String keyName = attr.getValue(NAME_ATTR);
                    if (keyName != null && !keyName.trim().isEmpty()) {
                        if (collectedLinkedKeys == null) {
                            collectedLinkedKeys = new ArrayList<>();
                        }
                        collectedLinkedKeys.add(keyName.trim());
                        Log.v(DEBUG_TAG, "Added linked key: " + keyName.trim());
                    } else {
                        Log.w(DEBUG_TAG, "<" + KEY_ELEMENT + "> element is missing required '" + NAME_ATTR + "' attribute. Ignoring.");
                    }
                } else {
                    throw new SAXException("Unexpected element <" + name + "> inside <" + LIST_ENTRY + ">.");
                }
            }

            /** Handles start of a <list_entry> element. */
            private void startListEntry(@NonNull Attributes attr, @NonNull List<StringWithDescription> listValues) throws SAXException {
                if (listValues == null) {
                    throw new SAXException("<list_entry> encountered outside of a combo/multiselect or list chunk context.");
                }

                String value = attr.getValue(VALUE);
                if (value == null) {
                    currentListEntrySwd = null;
                    return;
                }
                Log.v(DEBUG_TAG, "Starting list_entry: value=" + value);
                String displayValue = attr.getValue(DISPLAY_VALUE);
                String listShortDescription = attr.getValue(SHORT_DESCRIPTION);
                String listDescription = displayValue != null ? displayValue : listShortDescription;
                String iconPath = attr.getValue(ICON);
                String imagePathAttr = attr.getValue(IMAGE);
                String imagePath = null;
                if (imagePathAttr != null) {
                    imagePath = getImagePath(preset, imagePathAttr);
                    imageCount++;
                }
                if (iconPath == null && imagePath == null) {
                    currentListEntrySwd = new ExtendedStringWithDescription(value, listDescription);
                } else {
                    currentListEntrySwd = new StringWithDescriptionAndIcon(value, listDescription, iconPath, imagePath);
                }
                boolean deprecated = TRUE.equals(attr.getValue(DEPRECATED));
                if (currentListEntrySwd instanceof ExtendedStringWithDescription) {
                    ((ExtendedStringWithDescription) currentListEntrySwd).setDeprecated(deprecated);
                    if (displayValue != null && listShortDescription != null) ((ExtendedStringWithDescription) currentListEntrySwd).setLongDescription(listShortDescription); // Store original short desc if display value used
                } else if (currentListEntrySwd instanceof StringWithDescriptionAndIcon) {
                    ((StringWithDescriptionAndIcon) currentListEntrySwd).setDeprecated(deprecated);
                }
                setRegions(attr, currentListEntrySwd);
                listValues.add(currentListEntrySwd);
                isInLinkedKeys = false;
                collectedLinkedKeys = null;

                Log.v(DEBUG_TAG, "Started list_entry: value=" + value + ", desc=" + listDescription);
            }

            /**
             * Parse everything that isn't in an ITEM or CHUNK
             * 
             * @param name tag name
             * @param attr attributes
             * @throws SAXException if there is a parsing error
             */
            private void parseTop(String name, Attributes attr) throws SAXException {
                switch (name) {
                case PRESETS:
                    String objectKeysTemp = attr.getValue(OBJECT_KEYS);
                    if (objectKeysTemp != null) {
                        String[] tempArray = objectKeysTemp.split("\\s*,\\s*");
                        if (tempArray.length > 0) {
                            preset.getObjectKeys().addAll(Arrays.asList(tempArray));
                        }
                    }
                    preset.setVersion(attr.getValue(VERSION_ATTR));
                    preset.setShortDescription(attr.getValue(SHORTDESCRIPTION_ATTR));
                    preset.setDescription(attr.getValue(DESCRIPTION_ATTR));
                    groupstack.push(preset.getRootGroup());
                    break;
                case GROUP:
                    PresetGroup parent = groupstack.peek();
                    PresetGroup g = new PresetGroup(preset, parent, attr.getValue(NAME), attr.getValue(ICON));
                    String imagePath = attr.getValue(IMAGE);
                    if (imagePath != null) {
                        g.setImage(getImagePath(preset, imagePath));
                    }
                    String context = attr.getValue(NAME_CONTEXT);
                    if (context != null) {
                        g.setNameContext(context);
                    }
                    String itemsSort = attr.getValue(ITEMS_SORT);
                    if (itemsSort != null) {
                        g.setItemSort(YES.equals(itemsSort));
                    }
                    g.setRegions(attr.getValue(REGIONS));
                    g.setExcludeRegions(TRUE.equals(attr.getValue(EXCLUDE_REGIONS)));
                    groupstack.push(g);
                    break;
                case ITEM:
                    if (currentItem != null) {
                        throw new SAXException("Nested items are not allowed");
                    }
                    if (inOptionalSection) {
                        final String msg = ITEM + " " + attr.getValue(NAME) + " optional must be nested";
                        Log.e(DEBUG_TAG, msg);
                        throw new SAXException(msg);
                    }
                    parent = groupstack.peek();
                    String type = attr.getValue(TYPE);
                    if (type == null) {
                        type = attr.getValue(GTYPE); // note gtype seems to be undocumented
                    }
                    currentItem = new PresetItem(preset, parent, attr.getValue(NAME), attr.getValue(ICON), type);
                    imagePath = attr.getValue(IMAGE);
                    if (imagePath != null) {
                        currentItem.setImage(getImagePath(preset, imagePath));
                    }
                    context = attr.getValue(NAME_CONTEXT);
                    if (context != null) {
                        currentItem.setNameContext(context);
                    }
                    String nameTemplate = attr.getValue(NAME_TEMPLATE);
                    if (nameTemplate != null) {
                        currentItem.setNameTemplate(nameTemplate);
                    }
                    currentItem.setDeprecated(TRUE.equals(attr.getValue(DEPRECATED)));
                    setRegions(attr, currentItem);
                    currentItem.setAutoapply(!FALSE.equals(attr.getValue(AUTOAPPLY)));
                    String minMatchStr = attr.getValue(MIN_MATCH);
                    if (minMatchStr != null) {
                        try {
                            currentItem.setMinMatch(Short.parseShort(minMatchStr));
                        } catch (NumberFormatException e) {
                            Log.e(DEBUG_TAG, "Illegal min_match value " + minMatchStr + " " + e.getMessage());
                        }
                    }
                    currentItem.setMatchExpression(attr.getValue(MATCH_EXPRESSION));
                    checkGroupCounter = 0;
                    state = PARSE_STATE.ITEM;
                    break;
                case CHUNK:
                    if (currentItem != null) {
                        throw new SAXException("Nested chunks are not allowed");
                    }
                    if (inOptionalSection) {
                        final String msg = "Chunk " + attr.getValue(ID) + " optional must be nested"; // NOSONAR
                        Log.e(DEBUG_TAG, msg);
                        throw new SAXException(msg);
                    }
                    type = attr.getValue(TYPE);
                    if (type == null) {
                        type = attr.getValue(GTYPE); // note gtype seems to be undocumented
                    }
                    currentItem = new PresetChunk(preset, null, attr.getValue(ID), attr.getValue(ICON), type);
                    checkGroupCounter = 0;
                    state = PARSE_STATE.CHUNK;
                    break;
                case SEPARATOR:
                    new PresetSeparator(preset, groupstack.peek());
                    break;
                default:
                    Log.w(DEBUG_TAG, "Unexpected top-level element: " + name);
                    throw new SAXException("Unexpected top-level element: " + name);
                }
            }

            /**
             * Parse a preset item
             * 
             * @param name tag name
             * @param attr attributes
             * @throws SAXException if there is a parsing error
             */
            private void parseItem(@NonNull String name, @NonNull Attributes attr) throws SAXException {
                switch (name) {
                case OPTIONAL:
                    inOptionalSection = true;
                    currentLabel = addLabelField(supportLabels, attr);
                    break;
                case KEY_ATTR:
                    String key = getKey(name, attr);
                    String value = attr.getValue(VALUE);
                    if (value == null) {
                        final String msg = ITEM + " " + attr.getValue(NAME) + " value must be present in key field";
                        Log.e(DEBUG_TAG, msg);
                        throw new SAXException(msg);
                    }
                    String match = attr.getValue(MATCH);
                    String textContext = attr.getValue(TEXT_CONTEXT);
                    String isObjectString = attr.getValue(OBJECT);
                    PresetTagField field = null;
                    if (!inOptionalSection) {
                        if (NONE.equals(match)) {// don't include in fixed tags if not used for matching
                            field = currentItem.addTag(false, key, PresetKeyType.TEXT, value, MatchType.fromString(match));
                        } else {
                            field = currentItem.addFixedTag(key, value, attr.getValue(TEXT), textContext);
                        }
                    } else {
                        // Optional fixed tags should not happen, their values will NOT be automatically inserted.
                        field = currentItem.addTag(true, key, PresetKeyType.TEXT, value, MatchType.fromString(match));
                        field.setDeprecated(TRUE.equals(attr.getValue(DEPRECATED))); // fixed fields can't be deprecated
                    }
                    if (match != null) {
                        field.setMatchType(match);
                    }
                    if (textContext != null) {
                        field.setTextContext(textContext);
                    }
                    if (field instanceof PresetFixedField && isObjectString != null) {
                        ((PresetFixedField) field).setIsObject(Boolean.parseBoolean(isObjectString));
                    }
                    setRegions(attr, field);
                    field.setMatchExpression(attr.getValue(MATCH_EXPRESSION));
                    break;
                case TEXT_FIELD:
                    key = getKey(name, attr);
                    match = attr.getValue(MATCH);
                    field = currentItem.addTag(inOptionalSection, key, PresetKeyType.TEXT, (String) null, match == null ? null : MatchType.fromString(match));
                    if (!(field instanceof PresetTextField)) {
                        break;
                    }
                    String defaultValue = attr.getValue(DEFAULT);
                    if (defaultValue != null) {
                        field.setDefaultValue(defaultValue);
                    }
                    String text = attr.getValue(TEXT);
                    if (text != null) {
                        field.setHint(text);
                    }
                    textContext = attr.getValue(TEXT_CONTEXT);
                    if (textContext != null) {
                        field.setTextContext(textContext);
                    }
                    String javaScript = attr.getValue(JAVASCRIPT);
                    if (javaScript != null) {
                        ((PresetTextField) field).setScript(javaScript);
                    }
                    field.setI18n(TRUE.equals(attr.getValue(I18N)));
                    String valueType = attr.getValue(VALUE_TYPE);
                    if (valueType != null) {
                        field.setValueType(valueType);
                    }
                    String useLastAsDefault = attr.getValue(USE_LAST_AS_DEFAULT);
                    if (useLastAsDefault != null) {
                        field.setUseLastAsDefault(useLastAsDefault);
                    }
                    String length = attr.getValue(LENGTH);
                    if (length != null) {
                        try {
                            ((PresetTextField) field).setLength(Integer.parseInt(length));
                        } catch (NumberFormatException e) {
                            Log.e(DEBUG_TAG, "Parsing of 'length' failed " + length + " " + e.getMessage());
                        }
                    }
                    field.setDeprecated(TRUE.equals(attr.getValue(DEPRECATED)));
                    setBackground(attr, field);
                    setRegions(attr, field);
                    field.setMatchExpression(attr.getValue(MATCH_EXPRESSION));
                    break;
                case LINK:
                    String language = Locale.getDefault().getLanguage();
                    String href = attr.getValue(language.toLowerCase(Locale.US) + "." + HREF);
                    if (href != null) { // lang specific urls have precedence
                        currentItem.setMapFeatures(href);
                    } else {
                        String wiki = attr.getValue(WIKI);
                        if (wiki != null) {
                            currentItem.setMapFeatures(wiki);
                        } else { // last try
                            href = attr.getValue(HREF);
                            if (href != null) {
                                currentItem.setMapFeatures(href);
                            }
                        }
                    }
                    break;
                case LABEL:
                    currentLabel = addLabelField(supportLabels, attr);
                    break;
                case CHECKGROUP:
                    checkGroup = new PresetCheckGroupField(currentItem.getName() + PresetCheckGroupField.class.getSimpleName() + checkGroupCounter);
                    text = attr.getValue(TEXT);
                    if (text != null) {
                        checkGroup.setHint(text);
                        removeDuplicatedLabel(text);
                    } else if (currentLabel != null && !supportLabels) {
                        checkGroup.setHint(currentLabel);
                    }
                    checkGroup.setOptional(inOptionalSection);
                    checkGroup.setDeprecated(TRUE.equals(attr.getValue(DEPRECATED)));
                    setBackground(attr, checkGroup);
                    setRegions(attr, checkGroup);
                    break;
                case CHECK_FIELD:
                    key = getKey(name, attr);
                    String valueOnAttr = attr.getValue(VALUE_ON) == null ? YES : attr.getValue(VALUE_ON);
                    String valueOffAttr = attr.getValue(VALUE_OFF) == null ? NO : attr.getValue(VALUE_OFF);
                    String disableOffAttr = attr.getValue(DISABLE_OFF);
                    StringWithDescription valueOn = new StringWithDescription(valueOnAttr, de.blau.android.util.Util.capitalize(valueOnAttr));
                    StringWithDescription valueOff = null;
                    PresetCheckField checkField = new PresetCheckField(key, valueOn);
                    if (disableOffAttr == null || !disableOffAttr.equals(TRUE)) {
                        valueOff = new StringWithDescription(valueOffAttr, de.blau.android.util.Util.capitalize(valueOffAttr));
                        checkField.setOffValue(valueOff);
                    }
                    defaultValue = attr.getValue(DEFAULT);
                    if (defaultValue != null) {
                        checkField.setDefaultValue(ON.equals(defaultValue) ? valueOnAttr : valueOffAttr);
                    }
                    text = attr.getValue(TEXT);
                    if (text != null) {
                        checkField.setHint(text);
                    }
                    textContext = attr.getValue(TEXT_CONTEXT);
                    if (textContext != null) {
                        checkField.setTextContext(textContext);
                    }
                    match = attr.getValue(MATCH);
                    if (match != null) {
                        checkField.setMatchType(match);
                    }
                    checkField.setOptional(inOptionalSection);
                    useLastAsDefault = attr.getValue(USE_LAST_AS_DEFAULT);
                    if (useLastAsDefault != null) {
                        checkField.setUseLastAsDefault(useLastAsDefault);
                    }
                    checkField.setDeprecated(TRUE.equals(attr.getValue(DEPRECATED)));
                    if (checkGroup != null) {
                        checkGroup.addCheckField(checkField);
                    } else {
                        currentItem.addField(checkField);
                    }
                    setBackground(attr, checkField);
                    setRegions(attr, checkField);
                    checkField.setMatchExpression(attr.getValue(MATCH_EXPRESSION));
                    break;
                case COMBO_FIELD:
                case MULTISELECT_FIELD:
                    boolean multiselect = MULTISELECT_FIELD.equals(name);
                    key = getKey(name, attr);
                    String delimiter = attr.getValue(DELIMITER);
                    if (delimiter == null) {
                        delimiter = multiselect ? Preset.MULTISELECT_DELIMITER : Preset.COMBO_DELIMITER;
                    }
                    String values = attr.getValue(VALUES);
                    String displayValues = attr.getValue(DISPLAY_VALUES);
                    String shortDescriptions = attr.getValue(SHORT_DESCRIPTIONS);
                    String valuesFrom = attr.getValue(VALUES_FROM);
                    match = attr.getValue(MATCH);
                    final PresetKeyType keyType = multiselect ? PresetKeyType.MULTISELECT : PresetKeyType.COMBO;
                    if (values != null) {
                        currentItem.addTag(inOptionalSection, key, keyType, values, displayValues, shortDescriptions, delimiter,
                                match == null ? null : MatchType.fromString(match));
                    } else if (valuesFrom != null) {
                        setValuesFromMethod(key, valuesFrom, keyType, currentItem, inOptionalSection, delimiter);
                    } else {
                        currentItem.addTag(inOptionalSection, key, keyType, (StringWithDescription[]) null, delimiter,
                                match == null ? null : MatchType.fromString(match));
                        listKey = key;
                        listValues = new ArrayList<>();
                        imageCount = 0;
                    }
                    field = currentItem.getField(key);
                    if (!(field instanceof PresetComboField)) {
                        break;
                    }

                    defaultValue = attr.getValue(DEFAULT);
                    if (defaultValue != null) {
                        field.setDefaultValue(defaultValue);
                    }
                    text = attr.getValue(TEXT);
                    if (text != null) {
                        field.setHint(text);
                        removeDuplicatedLabel(text);
                    }
                    textContext = attr.getValue(TEXT_CONTEXT);
                    if (textContext != null) {
                        field.setTextContext(textContext);
                    }

                    String valuesContext = attr.getValue(VALUES_CONTEXT);
                    ((PresetComboField) field).setValuesContext(valuesContext);
                    if (textContext == null && valuesContext != null) {
                        field.setTextContext(valuesContext);
                    }

                    String sort = attr.getValue(VALUES_SORT);
                    if (sort != null) {
                        // normally this will not be set because true is the default
                        ((PresetComboField) field).setSortValues(YES.equals(sort) || TRUE.equals(sort));
                    }
                    String editable = attr.getValue(EDITABLE);
                    if (editable != null) {
                        ((PresetComboField) field).setEditable(YES.equals(editable) || TRUE.equals(editable));
                    }
                    String searchable = attr.getValue(VALUES_SEARCHABLE);
                    if (searchable != null) {
                        ((PresetComboField) field).setValuesSearchable(YES.equals(searchable) || TRUE.equals(searchable));
                    }
                    valueType = attr.getValue(VALUE_TYPE);
                    if (valueType != null) {
                        field.setValueType(valueType);
                    }
                    useLastAsDefault = attr.getValue(USE_LAST_AS_DEFAULT);
                    if (useLastAsDefault != null) {
                        field.setUseLastAsDefault(useLastAsDefault);
                    }
                    javaScript = attr.getValue(JAVASCRIPT);
                    if (javaScript != null) {
                        ((PresetComboField) field).setScript(javaScript);
                    }
                    String valueCountKey = attr.getValue(VALUE_COUNT_KEY);
                    if (valueCountKey != null) {
                        ((PresetComboField) field).setValueCountKey(valueCountKey);
                    }
                    field.setDeprecated(TRUE.equals(attr.getValue(DEPRECATED)));
                    setBackground(attr, field);
                    setRegions(attr, field);
                    field.setMatchExpression(attr.getValue(MATCH_EXPRESSION));
                    break;
                case ROLES:
                    break;
                case ROLE:
                    String roleValue = attr.getValue(KEY_ATTR);
                    text = attr.getValue(TEXT);
                    textContext = attr.getValue(TEXT_CONTEXT);
                    PresetRole role = new PresetRole(roleValue, text == null ? null : preset.translate(text, textContext), attr.getValue(TYPE));
                    role.setMemberExpression(attr.getValue(MEMBER_EXPRESSION));
                    role.setRequisite(attr.getValue(REQUISITE));
                    role.setCount(attr.getValue(COUNT));
                    role.setRegexp(attr.getValue(REGEXP));
                    role.setDeprecated(TRUE.equals(attr.getValue(DEPRECATED)));
                    setRegions(attr, role);
                    currentItem.addRole(role);
                    break;
                case REFERENCE:
                    String chunkRef = attr.getValue(REF);
                    PresetChunk chunk = chunks.get(chunkRef); // note this assumes that there are no
                                                              // forward references
                    if (chunk == null) {
                        Log.e(DEBUG_TAG, "Chunk " + attr.getValue(REF) + " not found"); // NOSONAR
                        break;
                    }
                    if (chunk.getListValues() != null) {
                        if (listValues != null) {
                            listValues.addAll(chunk.getListValues());
                        } else {
                            String msg = "chunk " + chunkRef + " with LIST_ENTRY sequence referenced outside of COMBO/MULTISELECT";
                            Log.d(DEBUG_TAG, msg);
                            throw new SAXException(msg);
                        }
                    } else {
                        if (inOptionalSection) {
                            // fixed tags don't make sense in an optional section, and doesn't seem to happen in
                            // practice
                            if (chunk.getFixedTagCount() > 0) {
                                Log.e(DEBUG_TAG, "Chunk " + chunk.name + " has fixed tags but is used in an optional section"); // NOSONAR
                            }
                            for (PresetField f : chunk.getFields().values()) {
                                if (f instanceof PresetTagField) {
                                    key = ((PresetTagField) f).getKey();
                                    // don't overwrite exiting fields
                                    if (!currentItem.hasKey(key)) {
                                        addOptionalCopy(f);
                                    } else {
                                        Log.w(DEBUG_TAG, "PresetItem " + currentItem.getName() + " chunk " + attr.getValue(REF) + " field " + key
                                                + " overwrites existing field");
                                    }
                                } else {
                                    addOptionalCopy(f);
                                }
                            }
                        } else {
                            currentItem.addAllFixedFields(chunk.getFixedTags());
                            currentItem.addAllFields(chunk.getFields());
                        }
                        currentItem.addAllRoles(chunk.getRoles());
                        currentItem.addAllLinkedPresetItems(chunk.getLinkedPresetItems());
                        currentItem.addAllAlternativePresetItems(chunk.getAlternativePresetItems());
                    }
                    break;
                case PRESET_LINK:
                    String presetName = attr.getValue(PRESET_NAME);
                    if (presetName != null) {
                        PresetItemLink link = new PresetItemLink(presetName, attr.getValue(TEXT), attr.getValue(TEXT_CONTEXT));
                        if (TRUE.equals(attr.getValue(ALTERNATIVE))) {
                            currentItem.addAlternativePresetItem(link);
                        } else {
                            currentItem.addLinkedPresetItem(link);
                        }
                    }
                    break;
                case SPACE:
                case ITEM_SEPARATOR:
                    PresetFormattingField formattingField = SPACE.equals(name) ? new PresetSpaceField(getHeight(attr))
                            : new PresetItemSeparatorField(getHeight(attr));
                    currentItem.addField(formattingField);
                    formattingField.setOptional(inOptionalSection);
                    setBackground(attr, formattingField);
                    if (formattingField instanceof FieldHeight) {
                        String height = attr.getValue(HEIGHT_ATTR);
                        if (height != null) {
                            try {
                                ((FieldHeight) formattingField).setHeight(Integer.parseInt(height));
                            } catch (NumberFormatException e) {
                                Log.e(DEBUG_TAG, "Invalid int value " + height);
                            }
                        }
                    }
                    break;
                default:
                    Log.w(DEBUG_TAG, "Unknown start tag in preset item " + name);
                }
                // always zap label after next element
                if (!addedLabel) {
                    currentLabel = null;
                } else {
                    addedLabel = false;
                }
            }

            /**
             * Get the key from attributes
             * 
             * @param name current field name
             * @param attr the attribtues
             * @return the key
             * @throws SAXException if no key was found
             */
            @NonNull
            public String getKey(@NonNull String name, @NonNull Attributes attr) throws SAXException {
                String key = attr.getValue(KEY_ATTR);
                if (key == null) {
                    final String msg = ITEM + " " + attr.getValue(NAME) + " key must be present in " + name + " field";
                    Log.e(DEBUG_TAG, msg);
                    throw new SAXException(msg);
                }
                return key;
            }

            /**
             * Add a copy of a PresetFiled to the current item with the optional flag set
             * 
             * @param f the PresetField to copy
             */
            private void addOptionalCopy(@NonNull PresetField f) {
                PresetField copy = f.copy();
                copy.setOptional(true);
                currentItem.addField(copy);
            }

            /**
             * Set the background colour from attributes
             * 
             * @param attr the attributes
             * @param field the PresetFiels
             */
            private void setBackground(@NonNull Attributes attr, @NonNull PresetField field) {
                String backgroundString = attr.getValue(BACKGROUND);
                if (backgroundString != null) {
                    try {
                        field.setBackgroundColour(Integer.parseInt(backgroundString, 16));
                    } catch (NumberFormatException nfex) {
                        Log.e(DEBUG_TAG, "Error parsing colour value " + nfex.getMessage());
                    }
                }
            }

            /**
             * Set the region values from attributes
             * 
             * @param attr the attributes
             * @param element the Regionalizable element
             */
            private void setRegions(@NonNull Attributes attr, @NonNull Regionalizable element) {
                element.setRegions(attr.getValue(REGIONS));
                element.setExcludeRegions(TRUE.equals(attr.getValue(EXCLUDE_REGIONS)));
            }

            /**
             * Get the value of the height attribute if not present return zero
             * 
             * @param attr the current attributes
             */
            private int getHeight(@NonNull Attributes attr) {
                String height = attr.getValue(HEIGHT_ATTR);
                if (height != null) {
                    try {
                        return Integer.parseInt(height);
                    } catch (NumberFormatException e) {
                        Log.e(DEBUG_TAG, "Invalid int value " + height);
                    }
                }
                return 0;
            }

            /**
             * Extract the label text and add a field if supportLabels is true
             * 
             * @param supportLabels flag
             * @param attr XML attributes
             * @return the label text or null
             */
            @Nullable
            private String addLabelField(boolean supportLabels, @NonNull Attributes attr) {
                String labelText = attr.getValue(TEXT);
                if (supportLabels && labelText != null) {
                    PresetLabelField labelField = new PresetLabelField(labelText, attr.getValue(TEXT_CONTEXT));
                    setBackground(attr, labelField);
                    setRegions(attr, labelField);
                    currentItem.addField(labelField);
                    labelField.setOptional(inOptionalSection);
                    addedLabel = true;
                }
                return labelText;
            }

            /**
             * If a checkgroup or combo/multselect has the same text value as a preceding label, remove the label
             * 
             * Often the label ends with a double colon, so we check for that too
             * 
             * @param text the text to check
             */
            private void removeDuplicatedLabel(@Nullable String text) {
                if (currentLabel != null && (currentLabel.equals(text) || currentLabel.equals(text + ":"))) {
                    currentItem.removeLastLabel();
                }
            }

            /**
             * Set values by calling a method
             * 
             * As this might take longer and include network calls it needs to be done async, however on the other hand
             * this may cause concurrent modification exception and have to be looked at
             * 
             * @param key the key we want values for
             * @param valuesFrom the method spec as a String
             * @param keyType what kind of key this is
             * @param item the PresetItem we want to add this to
             * @param inOptionalSection if this key optional
             * @param delimiter delimiter for multi-valued keys
             * @param valuesContext translation context, currently unused
             */
            private void setValuesFromMethod(final String key, final String valuesFrom, final PresetKeyType keyType, final PresetItem item,
                    final boolean inOptionalSection, final String delimiter) {
                item.addTag(inOptionalSection, key, keyType, (StringWithDescription[]) null, delimiter, MatchType.KEY_VALUE);
                new ExecutorTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void param) {
                        Object result = de.blau.android.presets.Util.invokeMethod(valuesFrom, key);
                        PresetComboField field = (PresetComboField) item.getField(key);
                        synchronized (field) {
                            if (result instanceof String[]) {
                                int count = ((String[]) result).length;
                                StringWithDescription[] valueArray = new StringWithDescription[count];
                                for (int i = 0; i < count; i++) {
                                    StringWithDescription swd = new StringWithDescription(((String[]) result)[i]);
                                    valueArray[i] = swd;
                                }
                                field.setValues(valueArray);
                            } else if (result instanceof StringWithDescription[]) {
                                field.setValues((StringWithDescription[]) result);
                            }
                        }
                        return null;
                    }
                }.execute();
            }

            @Override
            public void endElement(String uri, String localName, String name) throws SAXException {
                Log.v(DEBUG_TAG, "End Element: </" + name + "> State: " + state + " InOptional: " + inOptionalSection);
                if (LINKED_KEYS_ELEMENT.equals(name)) {
                    if (!isInLinkedKeys) {
                        Log.w(DEBUG_TAG, "End tag </" + LINKED_KEYS_ELEMENT + "> found outside of expected context. Ignoring.");

                    } else {
                        if (currentListEntrySwd != null && collectedLinkedKeys != null && !collectedLinkedKeys.isEmpty()) {
                            try {
                                if (currentListEntrySwd instanceof ExtendedStringWithDescription) {
                                    ((ExtendedStringWithDescription) currentListEntrySwd).setLinkedKeys(collectedLinkedKeys);
                                } else if (currentListEntrySwd instanceof StringWithDescriptionAndIcon) {
                                    ((StringWithDescriptionAndIcon) currentListEntrySwd).setLinkedKeys(collectedLinkedKeys);
                                }else {
                                    Log.w(DEBUG_TAG, "Cannot set linked keys on list entry '" + currentListEntrySwd.getValue() + "' of type " + currentListEntrySwd.getClass().getSimpleName());
                                }
                                Log.v(DEBUG_TAG, "Attached linked keys to list entry '" + currentListEntrySwd.getValue() + "': " + collectedLinkedKeys);
                            } catch (ClassCastException e) {
                                Log.e(DEBUG_TAG,"Error casting StringWithDescription subtype to set linked keys", e);
                            } catch (Exception e) {
                                Log.e(DEBUG_TAG,"Error setting linked keys on " + currentListEntrySwd.getClass().getSimpleName(), e);
                            }
                        } else {
                            Log.v(DEBUG_TAG, "Closing " + LINKED_KEYS_ELEMENT + " with no keys to attach or no active list entry.");
                        }
                        isInLinkedKeys = false;
                        collectedLinkedKeys = null;
                    }
                    return;
                }

                if (LIST_ENTRY.equals(name)) {
                    currentListEntrySwd = null;
                    isInLinkedKeys = false;
                    collectedLinkedKeys = null;
                    Log.v(DEBUG_TAG, "Finished scope for list_entry.");
                    return;
                }

                if (OPTIONAL.equals(name)) {
                    if (!inOptionalSection) {
                        Log.w(DEBUG_TAG, "Mismatched </optional> tag encountered.");
                    }
                    inOptionalSection = false;
                    return;
                }
                switch (name) {
                    case PRESETS:
                        chunks = null;
                        groupstack.clear();
                        state = null;
                        Log.i(DEBUG_TAG, "Finished parsing presets.");
                        break;

                    case GROUP:
                        if (!groupstack.isEmpty()) {
                            groupstack.pop();
                        } else {
                            Log.w(DEBUG_TAG,"Mismatched </group> tag encountered.");
                        }
                        break;

                    case ITEM:
                        if (state != PARSE_STATE.ITEM) Log.e(DEBUG_TAG,"</item> tag encountered but state is " + state);
                        if (currentItem instanceof PresetItem) {
                            preset.addToIndices((PresetItem)currentItem);
                            if (!currentItem.isDeprecated()) ((PresetItem)currentItem).buildSearchIndex();
                            preset.translateItem((PresetItem)currentItem);
                        }
                        currentItem = null;
                        listKey = null;
                        listValues = null;
                        imageCount = 0;
                        checkGroup = null;
                        currentLabel = null;
                        addedLabel = false;
                        inOptionalSection = false;
                        state = PARSE_STATE.TOP;
                        Log.v(DEBUG_TAG,"Reset major state, returning to TOP");
                        break;

                    case CHUNK:
                        if (state != PARSE_STATE.CHUNK) Log.e(DEBUG_TAG,"</chunk> tag encountered but state is " + state);
                        if (currentItem instanceof PresetChunk) {
                            chunks.put(currentItem.getName(), (PresetChunk) currentItem);
                            Log.v(DEBUG_TAG, "Stored chunk: " + currentItem.getName());
                        }
                        currentItem = null;
                        listKey = null;
                        listValues = null;
                        imageCount = 0;
                        checkGroup = null;
                        currentLabel = null;
                        addedLabel = false;
                        inOptionalSection = false;
                        state = PARSE_STATE.TOP;
                        Log.v(DEBUG_TAG,"Reset major state, returning to TOP");
                        break;

                    case COMBO_FIELD:
                    case MULTISELECT_FIELD:
                        if (listKey != null && listValues != null && currentItem != null) {
                            Log.d(DEBUG_TAG, "End combo/multi: listKey=" + listKey + ", listValues size=" + listValues.size() + ", currentItem=" + currentItem.getName());
                            PresetComboField field = null;
                            PresetField potentialField = currentItem.getField(listKey);
                            if (potentialField instanceof PresetComboField) {
                                field = (PresetComboField) potentialField;
                            } else if (potentialField != null){
                                Log.w(DEBUG_TAG, "Field for key '"+listKey+"' is not a PresetComboField, type: " + potentialField.getClass().getSimpleName());
                            }

                            if (field != null) {
                                StringWithDescription[] valueArray = listValues.toArray(new StringWithDescription[0]);
                                field.setValues(valueArray);
                                field.setUseImages(imageCount > 0);
                                Log.d(DEBUG_TAG, "SUCCESS: Attached " + valueArray.length + " list entries to field '" + listKey + "'.");
                            } else {
                                Log.e(DEBUG_TAG, "ERROR: Could not find PresetComboField for key '" + listKey + "' in item '" + currentItem.getName() + "' to attach list entries.");
                            }
                        } else {
                            Log.v(DEBUG_TAG, "Skipping list_entry attachment for combo/multi end: listKey=" + listKey + ", listValues=" + (listValues != null) + ", currentItem=" + (currentItem != null));
                        }
                        listKey = null;
                        listValues = null;
                        imageCount = 0;
                        break;
                    case CHECKGROUP:
                        if (checkGroup != null && currentItem != null) {
                            currentItem.addField(checkGroup);
                        }
                        checkGroup = null;
                        checkGroupCounter++;
                        break;

                    case SEPARATOR:

                        break;

                    default:
                        if (state == PARSE_STATE.TOP && !PRESETS.equals(name) && !GROUP.equals(name)) {
                            Log.w(DEBUG_TAG, "Unknown or unexpected end tag </" + name + "> at TOP level.");
                        }
                }
            }

            /**
             * If the preset isn't the default, add preset directory to path
             * 
             * @param preset the Preset
             * @param imagePath the path
             * @return the correct path to the image
             */
            @NonNull
            private String getImagePath(@NonNull Preset preset, @NonNull String imagePath) {
                return preset.isDefault() ? imagePath : preset.getDirectory().getAbsolutePath() + "/" + imagePath;
            }
        });
    }

    /**
     * Returns a list of icon URLs referenced by a preset
     * 
     * @param presetDir a File object pointing to the directory containing this preset
     * @return a List of http and https URLs as string
     */
    @NonNull
    public static List<String> parseForURLs(@NonNull File presetDir) {
        final List<String> urls = new ArrayList<>();
        String presetFilename = Preset.getPresetFileName(presetDir);
        if (presetFilename == null) { // no preset file found
            return urls;
        }
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance(); // NOSONAR
            SAXParser saxParser = factory.newSAXParser();

            saxParser.parse(new File(presetDir, presetFilename), new DefaultHandler() {
                /**
                 * ${@inheritDoc}.
                 */
                @Override
                public void startElement(String uri, String localName, String name, Attributes attr) throws SAXException {
                    if (GROUP.equals(name) || ITEM.equals(name)) {
                        String url = attr.getValue(ICON);
                        if (isUrl(url)) {
                            urls.add(url);
                        }
                    }
                }
            });
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Error parsing " + presetFilename + " for URLs", e);
        }
        return urls;
    }

    /**
     * Check for an url
     * 
     * @param url the url
     * @return true if the check passes
     */
    public static boolean isUrl(@Nullable String url) {
        return url != null && (url.startsWith(Schemes.HTTP + "://") || url.startsWith(Schemes.HTTPS + "://"));
    }
}
