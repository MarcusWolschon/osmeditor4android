package de.blau.android.presets;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ch.poole.poparser.Po;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.contract.FileExtensions;
import de.blau.android.contract.Paths;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.osm.OsmXml;
import de.blau.android.osm.Tags;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.PresetEditorActivity;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.ExtendedStringWithDescription;
import de.blau.android.util.Hash;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.SearchIndexUtils;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.StringWithDescriptionAndIcon;
import de.blau.android.util.Value;
import de.blau.android.util.collections.MultiHashMap;

/**
 * This class loads and represents JOSM preset files.
 * 
 * Presets can come from one of three sources: a) the default preset, which is loaded from the default asset locations
 * (see below) b) an APK-based preset, which is loaded from an APK c) a downloaded preset, which is downloaded to local
 * storage by {@link PresetEditorActivity}
 * 
 * For APK-based presets, the APK must have a "preset.xml" file in the asset directory, and may have images in the
 * "images" subdirectory in the asset directory. A preset is considered APK-based if the constructor receives a package
 * name. In the preset editor, use the package name prefixed by the {@link APKPRESET_URLPREFIX} to specify an APK
 * preset.
 * 
 * The preset.xml is loaded from the following sources: a) for the default preset, "preset.xml" in the default asset
 * locations b) for APK-based presets, "preset.xml" in the APK asset directory c) for downloaded presets, "preset.xml"
 * in the preset data directory
 * 
 * Icons referenced in the XML preset definition by relative URL are loaded from the following locations: 1. If a
 * package name is given and the APK contains a matching asset, from the asset ("images/" is prepended to the path) 2.
 * Otherwise, from the default asset location (see below, "images/" is prepended to the path)
 * 
 * Icons referenced in the XML preset by a http or https URL are loaded from the presets data directory, where they
 * should be placed under a name derived from the URL hash by {@link PresetEditorActivity}. Default and APK presets
 * cannot have http/https icons.
 * 
 * If an asset needs to be loaded from the default asset locations, the loader checks for the existence of an APK with
 * the package name specified in {@link PresetIconManager#EXTERNAL_DEFAULT_ASSETS_PACKAGE}. If this package exists and
 * contains a matching asset, it is loaded from there. Otherwise, it is loaded from the Vespucci asset directory. The
 * external default assets package just needs an asset directory that can contain a preset.xml and/or image directory.
 * 
 * @author Jan Schejbal
 */
public class Preset {

    private static final String ALTERNATIVE                = "alternative";
    private static final String USE_LAST_AS_DEFAULT        = "use_last_as_default";
    private static final String DEFAULT_PRESET_TRANSLATION = "preset_";
    static final String         NO                         = "no";
    static final String         VALUE_TYPE                 = "value_type";
    static final String         PRESET_NAME                = "preset_name";
    static final String         PRESET_LINK                = "preset_link";
    static final String         SHORT_DESCRIPTION          = "short_description";
    private static final String DISPLAY_VALUE              = "display_value";
    static final String         LIST_ENTRY                 = "list_entry";
    private static final String REFERENCE                  = "reference";
    private static final String ROLE                       = "role";
    private static final String ROLES                      = "roles";
    private static final String VALUES_SEARCHABLE          = "values_searchable";
    static final String         EDITABLE                   = "editable";
    static final String         VALUES_SORT                = "values_sort";
    private static final String VALUES_CONTEXT             = "values_context";
    private static final String SHORT_DESCRIPTIONS         = "short_descriptions";
    private static final String DISPLAY_VALUES             = "display_values";
    private static final String VALUES                     = "values";
    private static final String VALUES_FROM                = "values_from";
    static final String         DELIMITER                  = "delimiter";
    static final String         COMBO_FIELD                = "combo";
    static final String         MULTISELECT_FIELD          = "multiselect";
    static final String         YES                        = "yes";
    static final String         DISABLE_OFF                = "disable_off";
    static final String         VALUE_OFF                  = "value_off";
    static final String         VALUE_ON                   = "value_on";
    static final String         CHECK_FIELD                = "check";
    static final String         CHECKGROUP                 = "checkgroup";
    static final String         HREF                       = "href";
    static final String         WIKI                       = "wiki";
    static final String         LINK                       = "link";
    private static final String I18N                       = "i18n";
    private static final String JAVASCRIPT                 = "javascript";
    static final String         DEFAULT                    = "default";
    static final String         TEXT_CONTEXT               = "text_context";
    private static final String TEXT_FIELD                 = "text";
    static final String         TEXT                       = "text";
    static final String         VALUE                      = "value";
    private static final String NONE                       = "none";
    static final String         MATCH                      = "match";
    static final String         CHUNK                      = "chunk";
    static final String         KEY_ATTR                   = "key";
    static final String         OPTIONAL                   = "optional";
    static final String         SEPARATOR                  = "separator";
    private static final String ID                         = "id";
    private static final String DEPRECATED                 = "deprecated";
    static final String         TRUE                       = "true";
    private static final String FALSE                      = "false";
    private static final String GTYPE                      = "gtype";
    static final String         TYPE                       = "type";
    static final String         ITEM                       = "item";
    private static final String NAME_CONTEXT               = "name_context";
    static final String         ICON                       = "icon";
    private static final String IMAGE                      = "image";
    static final String         NAME                       = "name";
    private static final String OBJECT_KEYS                = "object_keys";
    static final String         OBJECT                     = "object";
    static final String         GROUP                      = "group";
    private static final String PRESETS                    = "presets";
    static final String         AREA                       = "area";
    static final String         MULTIPOLYGON               = "multipolygon";
    static final String         CLOSEDWAY                  = "closedway";
    private static final String LABEL                      = "label";
    private static final String ITEMS_SORT                 = "items_sort";
    private static final String SPACE                      = "space";
    private static final String LENGTH                     = "length";
    private static final String REGIONS                    = "regions";
    private static final String EXCLUDE_REGIONS            = "exclude_regions";
    private static final String AUTOAPPLY                  = "autoapply";
    private static final String MIN_MATCH                  = "min_match";
    private static final String REGEXP                     = "regexp";
    private static final String COUNT                      = "count";
    private static final String REQUISITE                  = "requisite";
    private static final String MEMBER_EXPRESSION          = "member_expression";
    private static final String REF                        = "ref";
    private static final String VALUE_COUNT_KEY            = "value_count_key";
    private static final String ON                         = "on";
    private static final String DESCRIPTION_ATTR           = "description";
    private static final String SHORTDESCRIPTION_ATTR      = "shortdescription";
    private static final String VERSION_ATTR               = "version";

    static final String COMBO_DELIMITER       = ",";
    static final String MULTISELECT_DELIMITER = ";";

    /** name of the preset XML file in a preset directory */
    public static final String PRESETXML           = "preset.xml";
    public static final String APKPRESET_URLPREFIX = "apk:";

    // hardwired layout stuff
    public static final int SPACING = 5;

    //
    private static final String DEBUG_TAG = Preset.class.getName();

    /** The directory containing all data (xml, MRU data, images) about this preset */
    private File directory;

    /** version of the preset */
    private String version;

    /** the short description which is essentially the "name" */
    private String shortDescription;

    /** the description of the content */
    private String description;

    /**
     * Lists items having a tag. The map key is tagkey+"\t"+tagvalue. tagItems.get(tagkey+"\t"+tagvalue) will give you
     * all items that have the tag tagkey=tagvalue
     */
    private final MultiHashMap<String, PresetItem> tagItems = new MultiHashMap<>();

    /**
     * Lists items that define objects
     */
    private final MultiHashMap<String, PresetItem> objectItems = new MultiHashMap<>();

    /** The root group of the preset, containing all top-level groups and items */
    private PresetGroup rootGroup;

    /** {@link PresetIconManager} used for icon loading */
    private PresetIconManager iconManager;

    /** List of all top level object tags used by this preset */
    private List<String> objectKeys = new ArrayList<>();

    /** Maps all possible keys to the respective values for autosuggest (only key/values applying to nodes) */
    private final MultiHashMap<String, StringWithDescription> autosuggestNodes      = new MultiHashMap<>(true);
    /** Maps all possible keys to the respective values for autosuggest (only key/values applying to ways) */
    private final MultiHashMap<String, StringWithDescription> autosuggestWays       = new MultiHashMap<>(true);
    /** Maps all possible keys to the respective values for autosuggest (only key/values applying to closed ways) */
    private final MultiHashMap<String, StringWithDescription> autosuggestClosedways = new MultiHashMap<>(true);
    /** Maps all possible keys to the respective values for autosuggest (only key/values applying to areas (MPs)) */
    private final MultiHashMap<String, StringWithDescription> autosuggestAreas      = new MultiHashMap<>(true);
    /** Maps all possible keys to the respective values for autosuggest (only key/values applying to closed ways) */
    private final MultiHashMap<String, StringWithDescription> autosuggestRelations  = new MultiHashMap<>(true);

    /** for search support */
    private final MultiHashMap<String, PresetItem> searchIndex           = new MultiHashMap<>();
    private final MultiHashMap<String, PresetItem> translatedSearchIndex = new MultiHashMap<>();

    private Po po = null;

    private final PresetMRUInfo mru;
    private String              externalPackage;
    private final boolean       isDefault;

    private static final FilenameFilter presetFileFilter = (File dir, String name) -> name.endsWith(".xml");
    private static final FileFilter     directoryFilter  = File::isDirectory;

    /**
     * create a dummy preset
     */
    Preset() {
        mru = null;
        isDefault = false;
    }

    /**
     * Create a dummy Preset instance with an empty root PresetGroup
     * 
     * @return a dummy Preset instance
     */
    @NonNull
    public static Preset dummyInstance() {
        Preset preset = new Preset(); // dummy preset to hold the elements of all
        PresetGroup rootGroup = new PresetGroup(preset, null, "", null);
        rootGroup.setItemSort(false);
        preset.setRootGroup(rootGroup);
        return preset;
    }

    /**
     * Creates a preset object.
     * 
     * @param ctx context (used for preset loading)
     * @param directory directory to load/store preset data (XML, icons, MRUs)
     * @param externalPackage name of external package containing preset assets for APK presets, null for other presets
     * @param useTranslations if true use included translations
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws NoSuchAlgorithmException
     */
    public Preset(@NonNull Context ctx, @NonNull File directory, @Nullable String externalPackage, boolean useTranslations)
            throws ParserConfigurationException, SAXException, IOException, NoSuchAlgorithmException {
        this.directory = directory;
        this.externalPackage = externalPackage;
        rootGroup = new PresetGroup(this, null, "", null);
        rootGroup.setItemSort(false);

        // noinspection ResultOfMethodCallIgnored
        directory.mkdir();

        InputStream fileStream = null;
        try {
            isDefault = AdvancedPrefDatabase.ID_DEFAULT.equals(directory.getName());
            if (isDefault) {
                Log.i(DEBUG_TAG, "Loading default preset");
                iconManager = new PresetIconManager(ctx, null, null);
                fileStream = iconManager.openAsset(PRESETXML, true);
                if (useTranslations) {
                    // get translations
                    InputStream poFileStream = null;
                    try {
                        Locale locale = Locale.getDefault();
                        String language = locale.getLanguage();
                        poFileStream = iconManager.openAsset(DEFAULT_PRESET_TRANSLATION + locale + "." + FileExtensions.PO, true);
                        if (poFileStream == null) {
                            poFileStream = iconManager.openAsset(DEFAULT_PRESET_TRANSLATION + language + "." + FileExtensions.PO, true);
                        }
                        po = de.blau.android.util.Util.parsePoFile(poFileStream);
                    } finally {
                        SavingHelper.close(poFileStream);
                    }
                }
            } else {
                final String dir = directory.toString();
                if (externalPackage != null) {
                    Log.i(DEBUG_TAG, "Loading APK preset, package=" + externalPackage + ", directory=" + dir);
                    iconManager = new PresetIconManager(ctx, dir, externalPackage);
                    fileStream = iconManager.openAsset(PRESETXML, false);
                } else {
                    Log.i(DEBUG_TAG, "Loading downloaded preset, directory=" + dir);
                    iconManager = new PresetIconManager(ctx, dir, null);
                    String presetFilename = getPresetFileName(new File(dir));
                    if (presetFilename != null) {
                        Log.i(DEBUG_TAG, "Preset file name " + presetFilename);
                        fileStream = new FileInputStream(new File(directory, presetFilename));
                        if (useTranslations) {
                            // get translations
                            presetFilename = presetFilename.substring(0, presetFilename.length() - 4);
                            InputStream poFileStream = null;
                            try {
                                // try to open .po files either with the same name as the preset file or the standard
                                // name
                                try {
                                    poFileStream = getPoInputStream(directory, presetFilename + "_", Locale.getDefault());
                                } catch (FileNotFoundException fnfe) {
                                    try {
                                        poFileStream = getPoInputStream(directory, DEFAULT_PRESET_TRANSLATION, Locale.getDefault());
                                    } catch (FileNotFoundException fnfe3) {
                                        // no translations
                                    }
                                }
                                po = de.blau.android.util.Util.parsePoFile(poFileStream);
                            } finally {
                                SavingHelper.close(poFileStream);
                            }
                        }
                    } else {
                        throw new IOException(ctx.getString(R.string.toast_missing_preset_file, dir));
                    }
                }
            }

            DigestInputStream hashStream = new DigestInputStream(fileStream, MessageDigest.getInstance("SHA-256"));

            parseXML(hashStream);

            // Finish hash
            String hashValue = Hash.toHex(hashStream.getMessageDigest().digest());
            // in theory, it could be possible that the stream parser does not read the entire file
            // and maybe even randomly stops at a different place each time.
            // in practice, it does read the full file, which means this gives the actual sha256 of the file,
            // - even if you add a 1 MB comment after the document-closing tag.

            // remove chunks - this messes up the index disabled for now
            // for (PresetItem c:new ArrayList<PresetItem>(allItems)) {
            // if (c.isChunk()) {
            // allItems.remove(c);
            // }
            // }

            mru = PresetMRUInfo.getMRU(directory, hashValue);

            Log.d(DEBUG_TAG, "search index length: " + searchIndex.getKeys().size());
        } finally {
            SavingHelper.close(fileStream);
        }
    }

    /**
     * Get an input stream for a .po file, try full locale string first then just the language
     * 
     * @param directory the directory where the file is located
     * @param presetFilename the filename
     * @param locale the Locale
     * @return the InputStream
     * @throws FileNotFoundException if the file does not exist
     */
    @NonNull
    private FileInputStream getPoInputStream(@NonNull File directory, @NonNull String presetFilename, @NonNull Locale locale) throws FileNotFoundException {
        try {
            return new FileInputStream(new File(directory, presetFilename + locale.toString() + "." + FileExtensions.PO));
        } catch (FileNotFoundException fnfe) {
            return new FileInputStream(new File(directory, presetFilename + locale.getLanguage() + "." + FileExtensions.PO));
        }
    }

    /**
     * Construct a new preset from existing elements
     * 
     * @param elements list of PresetElements
     */
    public Preset(@NonNull List<PresetElement> elements) {
        mru = null;
        isDefault = false;
        String name = "";
        if (!elements.isEmpty()) {
            name = elements.get(0).getName();
        } else {
            Log.e(DEBUG_TAG, "List of PresetElements was null");
            return;
        }
        rootGroup = new PresetGroup(this, null, name, null);
        rootGroup.setItemSort(false);
        addElementsToIndex(rootGroup, elements);
    }

    /**
     * Get the PresetIconManager for this Preset
     * 
     * @param ctx Android Context
     * @return the PresetIconManager instance
     */
    @NonNull
    PresetIconManager getIconManager(@NonNull Context ctx) {
        if (iconManager == null) {
            if (directory != null) {
                if (directory.getName().equals(AdvancedPrefDatabase.ID_DEFAULT)) {
                    iconManager = new PresetIconManager(ctx, null, null);
                } else if (externalPackage != null) {
                    iconManager = new PresetIconManager(ctx, directory.toString(), externalPackage);
                } else {
                    iconManager = new PresetIconManager(ctx, directory.toString(), null);
                }
            } else {
                iconManager = new PresetIconManager(ctx, null, null);
            }
        }
        return iconManager;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @return the shortDescription
     */
    public String getShortDescription() {
        return shortDescription;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Recursively add tags from the preset to the index of the new preset
     * 
     * @param group current group
     * @param elements list of PresetElements
     */
    private void addElementsToIndex(PresetGroup group, List<PresetElement> elements) {
        for (PresetElement e : elements) {
            if (e instanceof PresetGroup) {
                addElementsToIndex(group, ((PresetGroup) e).getElements());
            } else if (e instanceof PresetItem) {
                addToIndices((PresetItem) e);
            }
        }
    }

    /**
     * Add a PresetItem to tagItems
     * 
     * @param key tag key
     * @param item the PresetItem
     */
    private void addToTagItems(@NonNull String key, @NonNull PresetItem item) {
        tagItems.add(key + "\t", item);
    }

    /**
     * Add a PresetItem to tagItems
     * 
     * @param key tag key
     * @param value tag value
     * @param item the PresetItem
     */
    private void addToTagItems(@NonNull String key, @NonNull String value, @NonNull PresetItem item) {
        tagItems.add(key + "\t" + value, item);
    }

    /**
     * Add a PresetItem to tagItems
     * 
     * @param key tag key
     * @param value tag value
     * @param item the PresetItem
     */
    private void addToTagItems(@NonNull String key, @NonNull Value value, @NonNull PresetItem item) {
        addToTagItems(key, value.getValue(), item);
    }

    /**
     * Add a PresetItem to the object items
     * 
     * @param key the key
     * @param field the PresetFixedField
     * @param item the PresetItem
     */
    private void addToObjectItems(@NonNull String key, @NonNull PresetFixedField field, @NonNull PresetItem item) {
        if (field.isObject(objectKeys)) {
            objectItems.add(key + "\t" + field.getValue().getValue(), item);
        }
    }

    /**
     * Add a PresetItem to objectItems
     * 
     * @param key tag key
     * @param value tag value
     * @param item the PresetItem
     */
    private void addToObjectItems(@NonNull String key, @NonNull String value, @NonNull PresetItem item) {
        objectItems.add(key + "\t" + value, item);
    }

    /**
     * Add a PresetItem to objectItems
     * 
     * @param key tag key
     * @param item the PresetItem
     */
    private void addToObjectItems(@NonNull String key, @NonNull PresetItem item) {
        objectItems.add(key + "\t", item);
    }

    /**
     * Add a name, any translation and the individual words to the index. Currently we assume that all words are
     * significant
     * 
     * @param term search key to add
     * @param translationContext the translation context if any
     * @param item the PresetItem to add
     */
    void addToSearchIndex(@Nullable String term, @Nullable String translationContext, @NonNull PresetItem item) {
        // search support
        if (term != null) {
            String normalizedName = SearchIndexUtils.normalize(term);
            searchIndex.add(normalizedName, item);
            String[] words = normalizedName.split(" ");
            if (words.length > 1) {
                for (String w : words) {
                    searchIndex.add(w, item);
                }
            }
            if (po != null) { // and any translation
                String normalizedTranslatedName = SearchIndexUtils.normalize(po.t(translationContext, term));
                translatedSearchIndex.add(normalizedTranslatedName, item);
                String[] translastedWords = normalizedName.split(" ");
                if (translastedWords.length > 1) {
                    for (String w : translastedWords) {
                        translatedSearchIndex.add(w, item);
                    }
                }
            }
        }
    }

    /**
     * Add the values to the autosuggest maps for the key
     * 
     * @param item the relevant PresetItem
     * @param key the key
     * @param values array of the values
     */
    private void addToAutosuggest(@NonNull PresetItem item, @NonNull String key, StringWithDescription[] values) {
        if (item.appliesTo(ElementType.NODE)) {
            autosuggestNodes.add(key, values);
        }
        if (item.appliesTo(ElementType.WAY)) {
            autosuggestWays.add(key, values);
        }
        if (item.appliesTo(ElementType.CLOSEDWAY)) {
            autosuggestClosedways.add(key, values);
        }
        if (item.appliesTo(ElementType.RELATION)) {
            autosuggestRelations.add(key, values);
        }
        if (item.appliesTo(ElementType.AREA)) {
            autosuggestAreas.add(key, values);
        }
    }

    /**
     * Add the value to the autosuggest maps for the key
     * 
     * @param item the relevant PresetItem
     * @param key the key
     * @param value the value
     */
    private void addToAutosuggest(@NonNull PresetItem item, @NonNull String key, StringWithDescription value) {
        addToAutosuggest(item, key, new StringWithDescription[] { value });
    }

    /**
     * Add a PresetItem to the Presets indices
     * 
     * @param currentItem the item
     */
    void addToIndices(@NonNull PresetItem currentItem) {
        final StringWithDescription dummy = new StringWithDescription("");
        for (Entry<String, PresetField> e : currentItem.getFields().entrySet()) {
            PresetField field = e.getValue();
            String key = e.getKey();
            if (field instanceof PresetCheckGroupField) {
                for (PresetCheckField check : ((PresetCheckGroupField) field).getCheckFields()) {
                    String checkKey = check.getKey();
                    addToTagItems(checkKey, currentItem);
                    if (isObjectKey(checkKey)) {
                        addToObjectItems(key, currentItem);
                    }
                    addToAutosuggest(currentItem, checkKey, dummy);
                }
            } else {
                addToTagItems(key, currentItem);
                if (field instanceof PresetFixedField) {
                    addToTagItems(key, ((PresetFixedField) field).getValue(), currentItem);
                    addToObjectItems(key, (PresetFixedField) field, currentItem);
                    addToAutosuggest(currentItem, key, ((PresetFixedField) field).getValue());
                } else {
                    boolean isObjectKey = isObjectKey(key);
                    if (isObjectKey) {
                        addToObjectItems(key, currentItem);
                    }
                    if (field instanceof PresetComboField) {
                        StringWithDescription[] values = ((PresetComboField) field).getValues();
                        if (values != null) {
                            for (StringWithDescription v : values) {
                                String value = "";
                                if (v != null && v.getValue() != null) {
                                    value = v.getValue();
                                }
                                addToTagItems(key, value, currentItem);
                                if (isObjectKey) {
                                    addToObjectItems(key, value, currentItem);
                                }
                            }
                            addToAutosuggest(currentItem, key, values);
                        }
                    } else { // text fields and anything else
                        addToAutosuggest(currentItem, key, dummy);
                    }
                }
            }
        }
    }

    /**
     * Remove a PresetItem as far as possible
     * 
     * @param item the PresetItem
     */
    public void deleteItem(@NonNull PresetItem item) {
        for (String key : searchIndex.getKeys()) {
            searchIndex.removeItem(key, item);
        }
        for (String key : translatedSearchIndex.getKeys()) {
            translatedSearchIndex.removeItem(key, item);
        }
        for (String key : tagItems.getKeys()) {
            tagItems.removeItem(key, item);
        }
        for (String key : objectItems.getKeys()) {
            objectItems.removeItem(key, item);
        }
        removeRecentlyUsed(item);
        item.getParent().removeElement(item);
        item.setParent(null);
    }

    /**
     * Set the icon manager
     * 
     * This is typically only used if you want to retrieve icons from a non-standard location
     * 
     * @param mgr the PresetIconManager to use
     */
    public void setIconManager(@NonNull PresetIconManager mgr) {
        iconManager = mgr;
    }

    /**
     * Parses the XML during import
     * 
     * @param input the input stream from which to read XML data
     * @throws ParserConfigurationException
     * @throws SAXException on parsing issues
     * @throws IOException when reading the presets fails
     */
    private void parseXML(@NonNull InputStream input) throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance(); // NOSONAR
        SAXParser saxParser = factory.newSAXParser();

        saxParser.parse(input, new DefaultHandler() {
            /** stack of group-subgroup-subsubgroup... where we currently are */
            private Deque<PresetGroup>          groupstack        = new ArrayDeque<>();
            /** item currently being processed */
            private PresetItem                  currentItem       = null;
            /** true if we are currently processing the optional section of an item */
            private boolean                     inOptionalSection = false;
            /** hold reference to chunks */
            private Map<String, PresetItem>     chunks            = new HashMap<>();
            /** store current combo or multiselect key */
            private String                      listKey           = null;
            private List<StringWithDescription> listValues        = null;
            private int                         imageCount        = 0;
            /** check groups */
            private PresetCheckGroupField       checkGroup        = null;
            private int                         checkGroupCounter = 0;
            /** */
            private String                      currentLabel      = null;

            {
                groupstack.push(rootGroup);
            }

            /**
             * ${@inheritDoc}.
             */
            @Override
            public void startElement(String uri, String localName, String name, Attributes attr) throws SAXException {
                switch (name) {
                case PRESETS:
                    String objectKeysTemp = attr.getValue(OBJECT_KEYS);
                    if (objectKeysTemp != null) {
                        String[] tempArray = objectKeysTemp.split("\\s*,\\s*");
                        if (tempArray.length > 0) {
                            objectKeys.addAll(Arrays.asList(tempArray));
                        }
                    }
                    version = attr.getValue(VERSION_ATTR);
                    shortDescription = attr.getValue(SHORTDESCRIPTION_ATTR);
                    description = attr.getValue(DESCRIPTION_ATTR);
                    break;
                case GROUP:
                    PresetGroup parent = groupstack.peek();
                    PresetGroup g = new PresetGroup(Preset.this, parent, attr.getValue(NAME), attr.getValue(ICON));
                    String imagePath = attr.getValue(IMAGE);
                    if (imagePath != null) {
                        g.setImage(isDefault ? imagePath : directory.toString() + imagePath);
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
                        Log.e(DEBUG_TAG, "Item " + attr.getValue(NAME) + " optional must be nested");
                        throw new SAXException("optional must be nexted");
                    }
                    parent = groupstack.peek();
                    String type = attr.getValue(TYPE);
                    if (type == null) {
                        type = attr.getValue(GTYPE); // note gtype seems to be undocumented
                    }
                    currentItem = new PresetItem(Preset.this, parent, attr.getValue(NAME), attr.getValue(ICON), type);
                    imagePath = attr.getValue(IMAGE);
                    if (imagePath != null) {
                        currentItem.setImage(isDefault ? imagePath : directory.toString() + imagePath);
                    }
                    context = attr.getValue(NAME_CONTEXT);
                    if (context != null) {
                        currentItem.setNameContext(context);
                    }
                    currentItem.setDeprecated(TRUE.equals(attr.getValue(DEPRECATED)));
                    currentItem.setRegions(attr.getValue(REGIONS));
                    currentItem.setExcludeRegions(TRUE.equals(attr.getValue(EXCLUDE_REGIONS)));
                    currentItem.setAutoapply(!FALSE.equals(attr.getValue(AUTOAPPLY)));
                    String minMatchStr = attr.getValue(MIN_MATCH);
                    if (minMatchStr != null) {
                        try {
                            currentItem.setMinMatch(Short.parseShort(minMatchStr));
                        } catch (NumberFormatException e) {
                            Log.e(DEBUG_TAG, "Illegal min_match value " + minMatchStr + " " + e.getMessage());
                        }
                    }
                    checkGroupCounter = 0;
                    break;
                case CHUNK:
                    if (currentItem != null) {
                        throw new SAXException("Nested chunks are not allowed");
                    }
                    if (inOptionalSection) {
                        Log.e(DEBUG_TAG, "Chunk " + attr.getValue(ID) + " optional must be nested");
                        throw new SAXException("optional must be nexted");
                    }
                    type = attr.getValue(TYPE);
                    if (type == null) {
                        type = attr.getValue(GTYPE); // note gtype seems to be undocumented
                    }
                    currentItem = new PresetItem(Preset.this, null, attr.getValue(ID), attr.getValue(ICON), type);
                    currentItem.setChunk();
                    checkGroupCounter = 0;
                    break;
                case SEPARATOR:
                    new PresetSeparator(Preset.this, groupstack.peek());
                    break;
                default:
                    if (currentItem != null) { // the following only make sense if we actually found an item
                        parseItem(name, attr);
                    } else {
                        Log.d(DEBUG_TAG, name + " must be in a preset item");
                        throw new SAXException(name + " must be in a preset item");
                    }
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
                    break;
                case KEY_ATTR:
                    String key = attr.getValue(KEY_ATTR);
                    String match = attr.getValue(MATCH);
                    String textContext = attr.getValue(TEXT_CONTEXT);
                    String isObjectString = attr.getValue(OBJECT);
                    PresetField field = null;
                    if (!inOptionalSection) {
                        if (NONE.equals(match)) {// don't include in fixed tags if not used for matching
                            field = currentItem.addTag(false, key, PresetKeyType.TEXT, attr.getValue(VALUE), MatchType.fromString(match));
                        } else {
                            field = currentItem.addTag(key, PresetKeyType.TEXT, attr.getValue(VALUE), attr.getValue(TEXT), textContext);
                        }
                    } else {
                        // Optional fixed tags should not happen, their values will NOT be automatically inserted.
                        field = currentItem.addTag(true, key, PresetKeyType.TEXT, attr.getValue(VALUE), MatchType.fromString(match));
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
                    break;
                case TEXT_FIELD:
                    key = attr.getValue(KEY_ATTR);
                    if (key == null) {
                        Log.e(DEBUG_TAG, "Item " + attr.getValue(NAME) + " key must be present  in text field");
                        throw new SAXException("key must be present in text field");
                    }
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
                    currentLabel = attr.getValue(TEXT);
                    break;
                case CHECKGROUP:
                    checkGroup = new PresetCheckGroupField(currentItem.getName() + PresetCheckGroupField.class.getSimpleName() + checkGroupCounter);
                    text = attr.getValue(TEXT);
                    if (text != null) {
                        checkGroup.setHint(text);
                    } else if (currentLabel != null) {
                        checkGroup.setHint(currentLabel);
                    }
                    checkGroup.setOptional(inOptionalSection);
                    checkGroup.setDeprecated(TRUE.equals(attr.getValue(DEPRECATED)));
                    break;
                case CHECK_FIELD:
                    key = attr.getValue(KEY_ATTR);
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
                    defaultValue = attr.getValue(DEFAULT) == null ? null : (ON.equals(attr.getValue(DEFAULT)) ? valueOnAttr : valueOffAttr);
                    if (defaultValue != null) {
                        checkField.setDefaultValue(defaultValue);
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
                    break;
                case COMBO_FIELD:
                case MULTISELECT_FIELD:
                    boolean multiselect = MULTISELECT_FIELD.equals(name);
                    key = attr.getValue(KEY_ATTR);
                    if (key == null) {
                        Log.e(DEBUG_TAG, "Item " + attr.getValue(NAME) + " key must be present  in text field");
                        throw new SAXException("key must be present in combo/multiselect field");
                    }
                    String delimiter = attr.getValue(DELIMITER);
                    if (delimiter == null) {
                        delimiter = multiselect ? MULTISELECT_DELIMITER : COMBO_DELIMITER;
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

                    ((PresetComboField) field).setValuesContext(attr.getValue(VALUES_CONTEXT));

                    defaultValue = attr.getValue(DEFAULT);
                    if (defaultValue != null) {
                        field.setDefaultValue(defaultValue);
                    }
                    text = attr.getValue(TEXT);
                    if (text != null) {
                        field.setHint(text);
                    }
                    textContext = attr.getValue(TEXT_CONTEXT);
                    if (textContext != null) {
                        field.setTextContext(textContext);
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
                    break;
                case ROLES:
                    break;
                case ROLE:
                    String roleValue = attr.getValue(KEY_ATTR);
                    text = attr.getValue(TEXT);
                    textContext = attr.getValue(TEXT_CONTEXT);
                    PresetRole role = new PresetRole(roleValue, text == null ? null : translate(text, textContext), attr.getValue(TYPE));
                    role.setMemberExpression(attr.getValue(MEMBER_EXPRESSION));
                    role.setRequisite(attr.getValue(REQUISITE));
                    role.setCount(attr.getValue(COUNT));
                    role.setRegexp(attr.getValue(REGEXP));
                    role.setDeprecated(TRUE.equals(attr.getValue(DEPRECATED)));
                    currentItem.addRole(role);
                    break;
                case REFERENCE:
                    PresetItem chunk = chunks.get(attr.getValue(REF)); // note this assumes that there are no
                                                                       // forward references
                    if (chunk != null) {
                        if (inOptionalSection) {
                            // fixed tags don't make sense in an optional section, and doesn't seem to happen in
                            // practice
                            if (chunk.getFixedTagCount() > 0) {
                                Log.e(DEBUG_TAG, "Chunk " + chunk.name + " has fixed tags but is used in an optional section");
                            }
                            for (PresetField f : chunk.getFields().values()) {
                                key = f.getKey();
                                // don't overwrite exiting fields
                                if (!currentItem.hasKey(key)) {
                                    PresetField copy = f.copy();
                                    copy.setOptional(true);
                                    currentItem.addField(copy);
                                } else {
                                    Log.w(DEBUG_TAG, "PresetItem " + currentItem.getName() + " chunk " + attr.getValue(REF) + " field " + key
                                            + " overwrites existing field");
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
                case LIST_ENTRY:
                    if (listValues != null) {
                        String v = attr.getValue(VALUE);
                        if (v != null) {
                            String displayValue = attr.getValue(DISPLAY_VALUE);
                            String listShortDescription = attr.getValue(SHORT_DESCRIPTION);
                            String listDescription = displayValue != null ? displayValue : listShortDescription;
                            String iconPath = attr.getValue(ICON);
                            String imagePath = attr.getValue(IMAGE);
                            if (imagePath != null) {
                                imagePath = isDefault ? imagePath : directory.toString() + Paths.DELIMITER + imagePath;
                                imageCount++;
                            }
                            ExtendedStringWithDescription swd = iconPath == null && imagePath == null ? new ExtendedStringWithDescription(v, listDescription)
                                    : new StringWithDescriptionAndIcon(v, listDescription, iconPath, imagePath);
                            swd.setDeprecated(TRUE.equals(attr.getValue(DEPRECATED)));
                            if (displayValue != null) { // short description is potentially unused
                                swd.setLongDescription(listShortDescription);
                            }
                            listValues.add(swd);
                        }
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
                    break;
                default:
                    Log.e(DEBUG_TAG, "Unknown start tag in preset item " + name);
                }
                // always zap label after next element
                if (!LABEL.equals(name)) {
                    currentLabel = null;
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
                switch (name) {
                case PRESETS:
                    chunks = null; // we're finished
                    break;
                case GROUP:
                    groupstack.pop();
                    break;
                case OPTIONAL:
                    inOptionalSection = false;
                    break;
                case ITEM:
                    addToIndices(currentItem);
                    if (!currentItem.isDeprecated()) {
                        currentItem.buildSearchIndex();
                    }
                    translateItem(currentItem);
                    currentItem = null;
                    listKey = null;
                    listValues = null;
                    break;
                case CHUNK:
                    chunks.put(currentItem.getName(), currentItem);
                    currentItem = null;
                    listKey = null;
                    listValues = null;
                    break;
                case COMBO_FIELD:
                case MULTISELECT_FIELD:
                    if (listKey != null && listValues != null) {
                        StringWithDescription[] v = new StringWithDescription[listValues.size()];
                        PresetComboField field = (PresetComboField) currentItem.getField(listKey);
                        if (field != null) {
                            field.setValues(listValues.toArray(v));
                            field.setUseImages(imageCount > 0);
                        }
                    }
                    listKey = null;
                    listValues = null;
                    break;
                case CHECKGROUP:
                    currentItem.addField(checkGroup);
                    checkGroup = null;
                    checkGroupCounter++;
                    break;
                case SEPARATOR:
                    break;
                default:
                    if (currentItem == null) {
                        Log.e(DEBUG_TAG, "Unknown end tag " + name);
                    }
                }
            }
        });
    }

    /**
     * Translate a string
     * 
     * @param text the text to translate
     * @param context the translation context of null
     * @return the potentially translated text as a String
     */
    @NonNull
    public String translate(@NonNull String text, @Nullable String context) {
        return po != null ? (context != null ? po.t(context, text) : po.t(text)) : text;
    }

    /**
     * Translate all relevant parts of the PresetField of a PresetItem Note this needs to be done post building the
     * search index
     * 
     * @param item the PresetItem
     */
    private void translateItem(@NonNull PresetItem item) {
        if (po != null) {
            for (PresetField field : item.getFields().values()) {
                field.translate(po);
            }
        }
    }

    /**
     * Get a candidate preset file name in presetDir
     * 
     * Will descend recursively in to sub-directories if preset file is not found at top
     * 
     * @param presetDir the directory
     * @return the file name or null
     */
    @Nullable
    private static String getPresetFileName(@NonNull File presetDir) {
        File[] list = presetDir.listFiles(presetFileFilter);
        if (list != null && list.length > 0) { // simply use the first XML file found
            return list[0].getName();
        } else {
            list = presetDir.listFiles(directoryFilter);
            if (list != null) {
                for (File f : list) {
                    String fileName = getPresetFileName(f);
                    if (fileName != null) {
                        return f.getName() + Paths.DELIMITER + fileName;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns a list of icon URLs referenced by a preset
     * 
     * @param presetDir a File object pointing to the directory containing this preset
     * @return a List of http and https URLs as string, or null if there is an error during parsing
     */
    public static List<String> parseForURLs(@NonNull File presetDir) {
        final List<String> urls = new ArrayList<>();
        String presetFilename = getPresetFileName(presetDir);
        if (presetFilename == null) { // no preset file found
            return null;
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
                        if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                            urls.add(url);
                        }
                    }
                }
            });
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Error parsing " + presetFilename + " for URLs", e);
            return null;
        }
        return urls;
    }

    /** @return the root group of the preset, containing all top-level groups and items */
    public PresetGroup getRootGroup() {
        return rootGroup;
    }

    /**
     * Set the root group of this Preset
     * 
     * @param rootGroup the PresetGroup to set as new root
     */
    public void setRootGroup(@NonNull PresetGroup rootGroup) {
        this.rootGroup = rootGroup;
    }

    /**
     * Check if this Preset contains a PresetItem
     * 
     * @param pi the PresetItem we are interested in
     * @return true if the item is from this Preset
     */
    public boolean contains(@Nullable PresetItem pi) {
        if (pi == null) {
            return false;
        }
        return contains(rootGroup, pi);
    }

    /**
     * Recursively descend the Preset, starting at group and try to find the item
     * 
     * @param group the starting PresetGroup
     * @param item the PresetItem
     * @return true if found
     */
    private boolean contains(@NonNull PresetGroup group, @NonNull PresetItem item) {
        for (PresetElement element : group.getElements()) {
            if (element.equals(item)) {
                return true;
            } else if (element instanceof PresetGroup) {
                boolean found = contains((PresetGroup) element, item);
                if (found) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Return PresetItems containing the tag in question
     * 
     * @param tag tag in the format: key \t value
     * @return a Set containing the PresetItems or null if none found
     */
    @NonNull
    public Set<PresetItem> getItemByTag(@NonNull String tag) {
        return tagItems.get(tag);
    }

    /**
     * Remove an entry for a PresetItem from all tag to PresetItem indices
     * 
     * This is only useful for testing
     * 
     * @param ctx an Android Context
     * @param tag the tag for which we want to remove the entry
     */
    static void removeItem(@NonNull Context ctx, @NonNull String tag) {
        for (Preset preset : App.getCurrentPresets(ctx)) {
            if (preset != null) {
                Set<PresetItem> items = preset.tagItems.get(tag);
                for (PresetItem item : items) {
                    preset.deleteItem(item);
                }
            }
        }
    }

    /**
     * Return a preset by name Note: the names are not guaranteed to be unique, this will simple return the first found
     * 
     * @param name the name to search for
     * @param region a region (country/state) to filter by
     * @return the preset item or null if not found
     */
    @Nullable
    public PresetItem getItemByName(@NonNull String name, @Nullable String region) {
        return getElementByName(rootGroup, name, region, false);
    }

    /**
     * Return a preset by name Note: the names are not guaranteed to be unique, this will simple return the first found
     * 
     * @param name the name to search for
     * @param region a region (country/state) to filter by
     * @param deprecated if true only return deprecated items, if false, just non-deprecated ones
     * @return the preset item or null if not found
     */
    @Nullable
    public PresetItem getItemByName(@NonNull String name, @Nullable String region, boolean deprecated) {
        return getElementByName(rootGroup, name, region, deprecated);
    }

    /**
     * Recursively descend the Preset, starting at group and return a PresetItem with name
     * 
     * @param group the starting PresetGroup
     * @param name the name
     * @param region a region (country/state) to filter by
     * @param deprecated if true only return deprecated items, if false, just non-deprecated ones
     * @return a matching PresetItem or null
     */
    @Nullable
    private PresetItem getElementByName(@NonNull PresetGroup group, @NonNull String name, @Nullable String region, boolean deprecated) {
        List<PresetElement> elements = region == null ? group.getElements() : PresetElement.filterElementsByRegion(group.getElements(), region);
        for (PresetElement element : elements) {
            final boolean isDeprecated = element.isDeprecated();
            if (element instanceof PresetItem && name.equals(((PresetItem) element).getName()) && !(isDeprecated ^ deprecated)) {
                return (PresetItem) element;
            } else if (element instanceof PresetGroup) {
                PresetItem result = getElementByName((PresetGroup) element, name, region, deprecated);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * Get all PresetItems that are applicable for a specific ElementType
     * 
     * @param type the ElementType (null for all)
     * @return a Map with the PresetItems
     */
    @NonNull
    public Map<String, PresetItem> getItemsForType(@Nullable ElementType type) {
        final Map<String, PresetItem> result = new HashMap<>();
        processElements(rootGroup, (PresetElement element) -> {
            if (element instanceof PresetItem) {
                result.put(element.getName(), (PresetItem) element);
            }
        });
        return result;
    }

    /**
     * Find a preset group by name
     * 
     * Has to traverse the whole preset tree.
     * 
     * @param name the preset should have
     * @return the group or null if not found
     */
    @Nullable
    public PresetGroup getGroupByName(@NonNull String name) {
        return getGroupByName(getRootGroup(), name);
    }

    /**
     * Find a preset group by name
     * 
     * @param group PresetGroup to start at
     * @param name the preset should have
     * @return the group or null if not found
     */
    @Nullable
    private PresetGroup getGroupByName(@NonNull PresetGroup group, @NonNull String name) {
        for (PresetElement e : group.getElements()) {
            if (e instanceof PresetGroup) {
                if (name.equals(e.getName())) {
                    return (PresetGroup) e;
                } else {
                    PresetGroup g = getGroupByName((PresetGroup) e, name);
                    if (g != null) {
                        return g;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Recursively traverse the PresetElements and do something on them
     * 
     * @param group PresetGroup to start the traversing at
     * @param handler PresetElementHandler to execute
     */
    private static void processElements(@NonNull PresetGroup group, @NonNull PresetElementHandler handler) {
        for (PresetElement e : group.getElements()) {
            handler.handle(e);
            if (e instanceof PresetGroup) {
                processElements((PresetGroup) e, handler);
            }
        }
    }

    /**
     * Remove all generated icons from the Preset
     */
    public void clearIcons() {
        processElements(getRootGroup(), PresetElement::clearIcons);
    }

    /**
     * Return a PresetElement by identifying it by its place in the hierarchy
     * 
     * @param group PresetGroup to start the search at
     * @param path the path
     * @return the PresetElement or null if not found
     */
    @Nullable
    public static PresetElement getElementByPath(@NonNull PresetGroup group, @NonNull PresetElementPath path) {
        return getElementByPath(group, path, null, false);
    }

    /**
     * Return a PresetElement by identifying it by its place in the hierarchy
     * 
     * @param group PresetGroup to start the search at
     * @param path the path
     * @param region a region (country/state) to filter by
     * @param deprecated if true only return deprecated items, if false, just non-deprecated ones
     * @return the PresetElement or null if not found
     */
    @Nullable
    public static PresetElement getElementByPath(@NonNull PresetGroup group, @NonNull PresetElementPath path, @Nullable String region, boolean deprecated) {
        int size = path.getPath().size();
        if (size > 0) {
            String segment = path.getPath().get(0);
            List<PresetElement> elements = region == null ? group.getElements() : PresetElement.filterElementsByRegion(group.getElements(), region);
            for (PresetElement e : elements) {
                if (segment.equals(e.getName())) {
                    final boolean isDeprecated = e.isDeprecated();
                    if (size == 1 && !(isDeprecated ^ deprecated)) {
                        return e;
                    } else if (e instanceof PresetGroup) {
                        PresetElementPath newPath = new PresetElementPath(path);
                        newPath.getPath().remove(0);
                        return getElementByPath((PresetGroup) e, newPath, region, deprecated);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns a view showing the most recently used presets
     * 
     * @param ctx Android Context
     * @param presets the array of currently active presets
     * @param handler the handler which will handle clicks on the presets
     * @param type filter to show only presets applying to this type
     * @param region region to filter on
     * @return the view
     */
    @NonNull
    public static View getRecentPresetView(@NonNull Context ctx, @NonNull Preset[] presets, @Nullable PresetClickHandler handler, @Nullable ElementType type,
            @Nullable String region) {
        Preset dummy = new Preset();
        PresetGroup recent = new PresetGroup(dummy, null, "recent", null);
        recent.setItemSort(false);
        PresetMRUInfo.addToPresetGroup(recent, presets, region);
        return recent.getGroupView(ctx, handler, type, null, null); // we've already filtered on region
    }

    /**
     * Check if a (non-empty) MRU is present
     * 
     * @return true if a (non-empty) MRU is present
     */
    public boolean hasMRU() {
        return mru != null && !mru.isEmpty();
    }

    /**
     * Get the MRU for this preset
     * 
     * @return the mru
     */
    @Nullable
    PresetMRUInfo getMru() {
        return mru;
    }

    /**
     * Save the MRU
     */
    public void saveMRU() {
        if (mru != null) {
            mru.saveMRU(directory);
        }
    }

    /**
     * Reset the MRU list
     */
    public void resetRecentlyUsed() {
        if (mru != null) {
            mru.resetRecentlyUsed(directory);
        }
    }

    /**
     * Remove a PresetItem from the MRU
     * 
     * @param item the PresetItem to remove
     */
    public void removeRecentlyUsed(@NonNull PresetItem item) {
        if (mru != null) {
            mru.removeRecentlyUsed(item);
        }
    }

    /**
     * Add a preset to the front of the MRU list (removing old duplicates and limiting the list if needed)
     * 
     * @param item the item to add
     * @param region region to filter on
     * 
     */
    public void putRecentlyUsed(@NonNull PresetItem item, @Nullable String region) {
        if (mru != null) {
            mru.putRecentlyUsed(item, region);
        }
    }

    /**
     * Add PresetElements from other Presets to our rootGroup
     * 
     * @param presets array of current presets
     */
    public void addToRootGroup(@NonNull Preset[] presets) {
        if (presets.length > 0) {
            // a bit of a hack ... this adds the elements from other presets to the dummy root group
            List<PresetElement> rootElements = rootGroup.getElements();
            rootElements.clear();
            for (Preset p : presets) {
                if (p != null) {
                    for (PresetElement e : p.getRootGroup().getElements()) {
                        if (!rootElements.contains(e)) { // only do this if not already present
                            rootGroup.addElement(e, true);
                        }
                    }
                }
            }
        }
    }

    /**
     * Generate a JSON representation of all PresetItems in this Preset
     * 
     * @return JSON format string
     */
    @NonNull
    private String toJSON() {
        final StringBuilder result = new StringBuilder();
        processElements(rootGroup, (PresetElement element) -> {
            if (element instanceof PresetItem && !((PresetItem) element).isChunk()) {
                if (result.length() != 0) {
                    result.append(",\n");
                }
                result.append(((PresetItem) element).toJSON());
            }
        });
        return result.toString();
    }

    /**
     * Finds the preset item best matching a certain tag set, or null if no preset item matches. To match, all
     * (mandatory) tags of the preset item need to be in the tag set. The preset item does NOT need to have all tags in
     * the tag set, but the tag set needs to have all (mandatory) tags of the preset item.
     * 
     * If multiple items match, the most specific one (i.e. having most tags) wins. If there is a draw, no guarantees
     * are made.
     * 
     * @param presets presets to match against
     * @param tags tags to check against (i.e. tags of a map element)
     * @param region if not null this will be taken in to account wrt scoring
     * @return null, or the "best" matching item for the given tag set
     */
    @Nullable
    public static PresetItem findBestMatch(@Nullable Preset[] presets, @Nullable Map<String, String> tags, String region) {
        return findBestMatch(presets, tags, region, null, false);
    }

    /**
     * Finds the preset item best matching a certain tag set, or null if no preset item matches. To match, all
     * (mandatory) tags of the preset item need to be in the tag set. The preset item does NOT need to have all tags in
     * the tag set, but the tag set needs to have all (mandatory) tags of the preset item.
     * 
     * If multiple items match, the most specific one (i.e. having most tags) wins. If there is a draw, no guarantees
     * are made.
     * 
     * @param presets presets presets to match against
     * @param tags tags to check against (i.e. tags of a map element)
     * @param region if not null this will be taken in to account wrt scoring
     * @param elementType if not null the ElementType will be considered
     * @param useAddressKeys use addr: keys if true
     * @return a preset or null if none found
     */
    @Nullable
    public static PresetItem findBestMatch(@Nullable Preset[] presets, @Nullable Map<String, String> tags, @Nullable String region,
            @Nullable ElementType elementType, boolean useAddressKeys) {
        int bestMatchStrength = 0;
        PresetItem bestMatch = null;

        if (tags == null || presets == null) {
            Log.e(DEBUG_TAG, "findBestMatch " + (tags == null ? "tags null" : "presets null"));
            return null;
        }

        // Build candidate list
        Set<PresetItem> possibleMatches = buildPossibleMatches(presets, tags, false);
        // if we only have address keys retry
        if (useAddressKeys && possibleMatches.isEmpty()) {
            possibleMatches = buildPossibleMatches(presets, tags, true);
        }
        // Find best
        final int FIXED_WEIGHT = 1000; // always prioritize presets with fixed keys
        for (PresetItem possibleMatch : possibleMatches) {
            int fixedTagCount = possibleMatch.getFixedTagCount() * FIXED_WEIGHT;
            int recommendedTagCount = possibleMatch.getRecommendedKeyCount();
            if (fixedTagCount + recommendedTagCount >= bestMatchStrength) {
                int matches = 0;
                if (fixedTagCount > 0) {
                    if (!possibleMatch.matches(tags)) {
                        continue; // minimum requirement
                    }
                    // has all required tags
                    matches = fixedTagCount;
                }
                if (region != null && !possibleMatch.appliesIn(region)) {
                    // downgrade so much that recommended tags can't compensate
                    matches -= 200;
                }
                if (elementType != null) {
                    if (!possibleMatch.appliesTo(elementType)) {
                        // downgrade even more
                        matches -= 200;
                    } else if (ElementType.RELATION == elementType && possibleMatch.isFixedTag(Tags.KEY_TYPE)) {
                        matches += 2 * FIXED_WEIGHT; // prioritize actual relation presets
                    }
                }
                if (recommendedTagCount > 0) {
                    matches = matches + possibleMatch.matchesRecommended(tags);
                }
                if (matches > bestMatchStrength) {
                    bestMatch = possibleMatch;
                    bestMatchStrength = matches;
                }
            }
        }
        return bestMatch;
    }

    /**
     * Attempt to find a (any) match of the tags with the supplied presets
     * 
     * @param presets presets to search in
     * @param tags tags to match
     * @return a preset or null if none found
     */
    @Nullable
    public static PresetItem findMatch(@NonNull Preset[] presets, @NonNull Map<String, String> tags) {
        if (tags == null || presets == null) {
            Log.e(DEBUG_TAG, "findMatch " + (tags == null ? "tags null" : "presets null"));
            return null;
        }

        // Build candidate list
        Set<PresetItem> possibleMatches = buildPossibleMatches(presets, tags, false);

        // Find match
        for (PresetItem possibleMatch : possibleMatches) {
            if (possibleMatch.getFixedTagCount() > 0) { // has required tags
                if (possibleMatch.matches(tags)) {
                    return possibleMatch;
                }
            } else if (possibleMatch.getRecommendedKeyCount() > 0 && possibleMatch.matchesRecommended(tags) > 0) {
                return possibleMatch;
            }
        }
        return null;
    }

    /**
     * Return a set of presets that could match the tags
     * 
     * @param presets current presets
     * @param tags the tags
     * @param useAddressKeys use address keys
     * @return set of presets
     */
    @NonNull
    private static Set<PresetItem> buildPossibleMatches(@NonNull Preset[] presets, @NonNull Map<String, String> tags, boolean useAddressKeys) {
        Set<PresetItem> possibleMatches = new LinkedHashSet<>();
        for (Preset p : presets) {
            if (p != null) {
                for (Entry<String, String> tag : tags.entrySet()) {
                    String key = tag.getKey();
                    if (useAddressKeys || !key.startsWith(Tags.KEY_ADDR_BASE)) {
                        String tagString = key + "\t";
                        possibleMatches.addAll(p.objectItems.get(tagString)); // for stuff that doesn't have fixed
                                                                              // values
                        possibleMatches.addAll(p.objectItems.get(tagString + tag.getValue()));
                    }
                }
            }
        }
        return possibleMatches;
    }

    /**
     * Filter a list of elements by type
     * 
     * @param originalElements the list to filter
     * @param type the type to allow
     * @return a filtered list containing only elements of the specified type
     */
    @NonNull
    static List<PresetElement> filterElements(@NonNull List<PresetElement> originalElements, @NonNull ElementType type) {
        List<PresetElement> filteredElements = new ArrayList<>();
        for (PresetElement e : originalElements) {
            if (!e.isDeprecated() && (e.appliesTo(type) || ((e instanceof PresetSeparator) && !filteredElements.isEmpty()
                    && !(filteredElements.get(filteredElements.size() - 1) instanceof PresetSeparator)))) {
                // only add separators if there is a non-separator element above them
                filteredElements.add(e);
            }
        }
        return filteredElements;
    }

    /**
     * Get the top level tag if any
     * 
     * @param presets the currently configured presets
     * @param tags the current tags
     * @return the object tag in the form key=value or null if there is none
     */
    @Nullable
    public static String getObjectTag(@NonNull Preset[] presets, @NonNull Map<String, String> tags) {
        for (Preset p : presets) {
            if (p != null) {
                for (Entry<String, String> tag : tags.entrySet()) {
                    String key = tag.getKey();
                    if (p.isObjectKey(key)) {
                        return key + "=" + tag.getValue();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Check if a key-value tupel matches a specific PresetField taking the MatchType in to account This assumes that
     * field is either null or matches the key
     * 
     * @param field the PresetField
     * @param value the value
     * @return true if the tag matches
     */
    public static boolean hasKeyValue(@Nullable PresetField field, @Nullable String value) {
        if (field == null) {
            return false;
        }

        if (field instanceof PresetFixedField) {
            StringWithDescription swd = ((PresetFixedField) field).getValue();
            if ("".equals(value) || swd.getValue() == null || swd.equals(value) || "".equals(swd.getValue())) {
                return true;
            }
        }

        MatchType type = field.matchType;
        if (type == MatchType.KEY || type == MatchType.NONE || field instanceof PresetTextField) {
            return true;
        }

        if (field instanceof PresetComboField) {
            if (((PresetComboField) field).isMultiSelect()) {
                // MULTISELECT always matches ....
                return true;
            }
            StringWithDescription[] swdArray = ((PresetComboField) field).getValues();
            if (swdArray != null && swdArray.length > 0) {
                for (StringWithDescription v : swdArray) {
                    if ("".equals(value) || v.getValue() == null || v.equals(value) || "".equals(v.getValue())) {
                        return true;
                    }
                }
            } else {
                return true;
            }
        } else if (field instanceof PresetCheckField) {
            String on = ((PresetCheckField) field).getOnValue().getValue();
            StringWithDescription off = ((PresetCheckField) field).getOffValue();
            if ("".equals(value) || on == null || on.equals(value) || "".equals(on) || (off != null && off.getValue().equals(value))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get all possible keys for a specific ElementType
     * 
     * @param presets the current Presets
     * @param type the ELementType
     * @return a Collection of keys
     */
    @NonNull
    public static Collection<String> getAutocompleteKeys(@NonNull Preset[] presets, @NonNull ElementType type) {
        Collection<String> result = new LinkedHashSet<>();
        for (Preset p : presets) {
            if (p != null) {
                switch (type) {
                case NODE:
                    result.addAll(p.autosuggestNodes.getKeys());
                    break;
                case WAY:
                    result.addAll(p.autosuggestWays.getKeys());
                    break;
                case CLOSEDWAY:
                    result.addAll(p.autosuggestClosedways.getKeys());
                    break;
                case RELATION:
                    result.addAll(p.autosuggestRelations.getKeys());
                    break;
                case AREA:
                    result.addAll(p.autosuggestAreas.getKeys());
                    break;
                }
            }
        }
        List<String> r = new ArrayList<>(result);
        Collections.sort(r);
        return r;
    }

    /**
     * Get suggested values for a key
     * 
     * @param presets an array holding the currently active Presets
     * @param type the type of element, if null all will be assumed
     * @param key the key we want the values for
     * @return a Collection of the suggested values for key
     */
    @NonNull
    public static Collection<StringWithDescription> getAutocompleteValues(@NonNull Preset[] presets, @Nullable ElementType type, @NonNull String key) {
        Collection<StringWithDescription> result = new LinkedHashSet<>();
        if (type == null) {
            for (Preset p : presets) {
                if (p != null) {
                    result.addAll(p.autosuggestNodes.get(key));
                    result.addAll(p.autosuggestWays.get(key));
                    result.addAll(p.autosuggestClosedways.get(key));
                    result.addAll(p.autosuggestRelations.get(key));
                    result.addAll(p.autosuggestAreas.get(key));
                }
            }
        } else {
            for (Preset p : presets) {
                if (p != null) {
                    switch (type) {
                    case NODE:
                        result.addAll(p.autosuggestNodes.get(key));
                        break;
                    case WAY:
                        result.addAll(p.autosuggestWays.get(key));
                        break;
                    case CLOSEDWAY:
                        result.addAll(p.autosuggestClosedways.get(key));
                        break;
                    case RELATION:
                        result.addAll(p.autosuggestRelations.get(key));
                        break;
                    case AREA:
                        result.addAll(p.autosuggestAreas.get(key));
                        break;
                    default:
                        return Collections.emptyList();
                    }
                }
            }
        }
        List<StringWithDescription> r = new ArrayList<>(result);
        Collections.sort(r);
        return r;
    }

    /**
     * Get a combined search index for an array of Presets
     * 
     * @param presets the array of Presets
     * @return a MultiHashMap mapping (EN) terms to PresetITems
     */
    public static MultiHashMap<String, PresetItem> getSearchIndex(Preset[] presets) {
        MultiHashMap<String, PresetItem> result = new MultiHashMap<>();
        for (Preset p : presets) {
            if (p != null) {
                result.addAll(p.searchIndex);
            }
        }
        return result;
    }

    /**
     * Get a combined search index for an array of Presets
     * 
     * @param presets the array of Presets
     * @return a MultiHashMap mapping (translated) terms to PresetITems
     */
    public static MultiHashMap<String, PresetItem> getTranslatedSearchIndex(Preset[] presets) {
        MultiHashMap<String, PresetItem> result = new MultiHashMap<>();
        for (Preset p : presets) {
            if (p != null) {
                result.addAll(p.translatedSearchIndex);
            }
        }
        return result;
    }

    /**
     * Check if key is a top-level object key for this preset
     * 
     * @param key key to check
     * @return true if key is a top-level key
     */
    public boolean isObjectKey(String key) {
        return objectKeys.contains(key) || Tags.IMPORTANT_TAGS.contains(key);
    }

    /**
     * Get the list of top-level object key for this preset
     * 
     * @return the List top-level object key for this preset
     */
    @NonNull
    public List<String> getObjectKeys() {
        return objectKeys;
    }

    /**
     * Add more object keys to the preset
     * 
     * @param moreKeys a List of additional keys to add
     */
    public void addObjectKeys(@NonNull List<String> moreKeys) {
        objectKeys.addAll(moreKeys);
    }

    /**
     * Split multi select values with the preset defined delimiter character
     * 
     * @param values list of values that can potentially be split
     * @param preset the preset that should be used
     * @param key the key used to determine the delimiter value
     * @return list of split values
     */
    @Nullable
    public static List<String> splitValues(@Nullable List<String> values, @NonNull PresetItem preset, @NonNull String key) {
        return splitValues(values, preset.getDelimiter(key));
    }

    /**
     * Split multi select values with the preset defined delimiter character
     * 
     * @param values list of values that can potentially be split
     * @param delimiter the delimiter character
     * @return list of split values
     */
    @Nullable
    public static List<String> splitValues(@Nullable List<String> values, char delimiter) {
        List<String> result = new ArrayList<>();
        if (values == null) {
            return null;
        }
        String d = Pattern.quote(String.valueOf(delimiter));
        for (String v : values) {
            if (v == null) {
                continue;
            }
            for (String s : v.split(d)) {
                result.add(s.trim());
            }
        }
        return result;
    }

    /**
     * This is for the taginfo project repo
     * 
     * @param ctx Android Context
     * @param output the File to save to
     * @return true if things worked
     */
    public static boolean generateTaginfoJson(@NonNull Context ctx, @NonNull File output) {
        Preset[] presets = App.getCurrentPresets(ctx);

        try (FileOutputStream fout = new FileOutputStream(output); PrintStream outputStream = new PrintStream(new BufferedOutputStream(fout))) {
            outputStream.println("{");
            outputStream.println("\"data_format\":1,");
            outputStream.println("\"data_url\":\"https://raw.githubusercontent.com/MarcusWolschon/osmeditor4android/master/taginfo.json\",");
            outputStream.println("\"project\":{");
            outputStream.println("\"name\":\"Vespucci\",");
            outputStream.println("\"description\":\"Offline editor for OSM data on Android.\",");
            outputStream.println("\"project_url\":\"https://github.com/MarcusWolschon/osmeditor4android\",");
            outputStream.println("\"doc_url\":\"http://vespucci.io/\",");
            outputStream.println(
                    "\"icon_url\":\"https://raw.githubusercontent.com/MarcusWolschon/osmeditor4android/master/src/main/res/drawable/vespucci_logo.png\",");
            outputStream.println("\"keywords\":[");
            outputStream.println("\"editor\"");
            outputStream.println("]},");

            outputStream.println("\"tags\":[");
            int presetsCount = presets.length;
            for (int i = 0; i < presetsCount; i++) {
                if (presets[i] != null) {
                    if (i != 0) {
                        if (i != presetsCount - 1) {
                            outputStream.print(",");
                        }
                        outputStream.println();
                    }
                    String json = presets[i].toJSON();
                    outputStream.print(json);
                }
            }
            outputStream.println("]}");
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Export failed - " + output.getAbsolutePath() + " exception " + e);
            return false;
        }
        return true;
    }

    /**
     * Convert this Preset to XML
     * 
     * @param s an XmlSerializer instance
     * @throws IllegalArgumentException if the serializer encountered an illegal argument
     * @throws IllegalStateException if the serializer detects an illegal state
     * @throws IOException if writing to the serializer fails
     */
    public void toXml(XmlSerializer s) throws IllegalArgumentException, IllegalStateException, IOException {
        s.startDocument(OsmXml.UTF_8, null);
        s.startTag("", PRESETS);
        for (PresetElement e : getRootGroup().getElements()) {
            e.toXml(s);
        }
        s.endTag("", PRESETS);
        s.endDocument();
    }
}
