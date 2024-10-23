package de.blau.android.presets;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

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
import java.io.Serializable;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ch.poole.osm.josmfilterparser.Condition;
import ch.poole.poparser.Po;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.contract.FileExtensions;
import de.blau.android.contract.Paths;
import de.blau.android.osm.DiscardedTags;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.osm.OsmXml;
import de.blau.android.osm.Tags;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.PresetEditorActivity;
import de.blau.android.search.Wrapper;
import de.blau.android.util.Hash;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.SearchIndexUtils;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.Value;
import de.blau.android.util.collections.MultiHashMap;

/**
 * This class loads and represents JOSM preset files.
 * 
 * Presets can come from one of three sources: a) the default preset, which is loaded from the default asset locations
 * (see below) b) an APK-based preset, which is loaded from an APK c) a downloaded preset, which is downloaded to local
 * storage by {@link PresetEditorActivity}
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
public class Preset implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, Preset.class.getSimpleName().length());
    private static final String DEBUG_TAG = Preset.class.getSimpleName().substring(0, TAG_LEN);

    static final String COMBO_DELIMITER       = ",";
    static final String MULTISELECT_DELIMITER = ";";

    private static final String DEFAULT_PRESET_TRANSLATION = "preset_";

    /** name of the preset XML file in a preset directory */
    public static final String PRESETXML = "preset.xml";

    // hardwired layout stuff
    public static final int SPACING = 5;

    /**
     * Global condition cache
     */
    private static Map<String, Condition> conditionCache = new HashMap<>();

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
    private transient PresetIconManager iconManager;

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
     * @param useTranslations if true use included translations
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws NoSuchAlgorithmException
     */
    public Preset(@NonNull Context ctx, @NonNull File directory, boolean useTranslations)
            throws ParserConfigurationException, SAXException, IOException, NoSuchAlgorithmException {
        this.directory = directory;
        rootGroup = new PresetGroup(this, null, "", null);
        rootGroup.setItemSort(false);

        // noinspection ResultOfMethodCallIgnored
        directory.mkdir();

        InputStream fileStream = null;
        InputStream poFileStream = null;
        try { // NOSONAR
            isDefault = AdvancedPrefDatabase.ID_DEFAULT.equals(directory.getName());
            if (isDefault) {
                Log.i(DEBUG_TAG, "Loading default preset");
                iconManager = new PresetIconManager(ctx, null, null);
                fileStream = iconManager.openAsset(PRESETXML, true);
                if (useTranslations) {
                    // get translations
                    Locale locale = Locale.getDefault();
                    String language = locale.getLanguage();
                    poFileStream = iconManager.openAsset(DEFAULT_PRESET_TRANSLATION + locale + "." + FileExtensions.PO, true);
                    if (poFileStream == null) {
                        poFileStream = iconManager.openAsset(DEFAULT_PRESET_TRANSLATION + language + "." + FileExtensions.PO, true);
                    }
                }
            } else {
                Log.i(DEBUG_TAG, "Loading downloaded preset, directory=" + directory);
                iconManager = new PresetIconManager(ctx, directory.getAbsolutePath(), null);
                String presetFilename = getPresetFileName(directory);
                if (presetFilename == null) {
                    throw new IOException(ctx.getString(R.string.toast_missing_preset_file, directory));
                }

                Log.i(DEBUG_TAG, "Preset file name " + presetFilename);
                fileStream = new FileInputStream(new File(directory, presetFilename));
                if (useTranslations) {
                    // get translations
                    presetFilename = presetFilename.substring(0, presetFilename.length() - 4);
                    // try to open .po files either with the same name as the preset file or the standard
                    // name
                    poFileStream = getPoInputStream(directory, presetFilename + "_", Locale.getDefault());
                    if (poFileStream == null) {
                        poFileStream = getPoInputStream(directory, DEFAULT_PRESET_TRANSLATION, Locale.getDefault());
                    }
                }
            }

            po = de.blau.android.util.Util.parsePoFile(poFileStream);

            try (DigestInputStream hashStream = new DigestInputStream(fileStream, MessageDigest.getInstance("SHA-256"))) {
                PresetParser.parseXML(this, hashStream, App.getPreferences(ctx).supportPresetLabels());
                // Finish hash
                String hashValue = Hash.toHex(hashStream.getMessageDigest().digest());
                // in theory, it could be possible that the stream parser does not read the entire file
                // and maybe even randomly stops at a different place each time.
                // in practice, it does read the full file, which means this gives the actual sha256 of the file,
                // - even if you add a 1 MB comment after the document-closing tag.
                mru = PresetMRUInfo.getMRU(directory, hashValue);
            }
            Log.d(DEBUG_TAG, "search index length: " + searchIndex.getKeys().size());
        } finally {
            SavingHelper.close(poFileStream);
            SavingHelper.close(fileStream);
        }
    }

    /**
     * Get an input stream for a .po file, try full locale string first then just the language
     * 
     * @param directory the directory where the file is located
     * @param presetFilename the filename
     * @param locale the Locale
     * @return the InputStream or null if it doesn't exist
     */
    @Nullable
    private FileInputStream getPoInputStream(@NonNull File directory, @NonNull String presetFilename, @NonNull Locale locale) throws FileNotFoundException {
        try {
            return new FileInputStream(new File(directory, presetFilename + locale.toString() + "." + FileExtensions.PO));
        } catch (FileNotFoundException fnfe) {
            try {
                return new FileInputStream(new File(directory, presetFilename + locale.getLanguage() + "." + FileExtensions.PO));
            } catch (FileNotFoundException fnfe2) {
                return null;
            }
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
    public PresetIconManager getIconManager(@NonNull Context ctx) {
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
     * @return the isDefault
     */
    public boolean isDefault() {
        return isDefault;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * @return the shortDescription
     */
    public String getShortDescription() {
        return shortDescription;
    }

    /**
     * @param shortDescription the shortDescription to set
     */
    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
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
            } else if (field instanceof PresetTagField) {
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
    void translateItem(@NonNull PresetItem item) {
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
    static String getPresetFileName(@NonNull File presetDir) {
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
     * @param directory the directory to set
     */
    public void setDirectory(File directory) {
        this.directory = directory;
    }

    /**
     * Get the directory the preset is stored in
     * 
     * @return the directory
     */
    @Nullable
    public File getDirectory() {
        return directory;
    }

    /** @return the root group of the preset, containing all top-level groups and items */
    @Nullable
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
    public PresetItem getItemByName(@NonNull String name, @Nullable List<String> regions) {
        return getElementByName(rootGroup, name, regions, false);
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
    public PresetItem getItemByName(@NonNull String name, @Nullable List<String> regions, boolean deprecated) {
        return getElementByName(rootGroup, name, regions, deprecated);
    }

    /**
     * Recursively descend the Preset, starting at group and return a PresetItem with name
     * 
     * @param group the starting PresetGroup
     * @param name the name
     * @param regions a list of regions (country/state) to filter by
     * @param deprecated if true only return deprecated items, if false, just non-deprecated ones
     * @return a matching PresetItem or null
     */
    @Nullable
    private PresetItem getElementByName(@NonNull PresetGroup group, @NonNull String name, @Nullable List<String> regions, boolean deprecated) {
        List<PresetElement> elements = regions == null ? group.getElements() : PresetElement.filterElementsByRegion(group.getElements(), regions);
        for (PresetElement element : elements) {
            final boolean isDeprecated = element.isDeprecated();
            if (element instanceof PresetItem && name.equals(element.getName()) && !(isDeprecated ^ deprecated)) {
                return (PresetItem) element;
            } else if (element instanceof PresetGroup) {
                PresetItem result = getElementByName((PresetGroup) element, name, regions, deprecated);
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
     * @param regions a list of regions (country/state) to filter by
     * @param deprecated if true only return deprecated items, if false, just non-deprecated ones
     * @return the PresetElement or null if not found
     */
    @Nullable
    public static PresetElement getElementByPath(@NonNull PresetGroup group, @NonNull PresetElementPath path, @Nullable List<String> regions,
            boolean deprecated) {
        int size = path.getPath().size();
        if (size > 0) {
            String segment = path.getPath().get(0);
            List<PresetElement> elements = regions == null ? group.getElements() : PresetElement.filterElementsByRegion(group.getElements(), regions);
            for (PresetElement e : elements) {
                if (segment.equals(e.getName())) {
                    final boolean isDeprecated = e.isDeprecated();
                    if (size == 1 && !(isDeprecated ^ deprecated)) {
                        return e;
                    } else if (e instanceof PresetGroup) {
                        PresetElementPath newPath = new PresetElementPath(path);
                        newPath.getPath().remove(0);
                        return getElementByPath((PresetGroup) e, newPath, regions, deprecated);
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
     * @param regions list of regions to filter on
     * @return the view
     */
    @NonNull
    public static View getRecentPresetView(@NonNull Context ctx, @NonNull Preset[] presets, @Nullable PresetClickHandler handler, @Nullable ElementType type,
            @Nullable List<String> regions) {
        Preset dummy = new Preset();
        PresetGroup recent = new PresetGroup(dummy, null, "recent", null);
        recent.setItemSort(false);
        PresetMRUInfo.addToPresetGroup(recent, presets, regions);
        return recent.getGroupView(ctx, handler, type, null, null, null); // we've already filtered on region
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
     * @param regions regions to filter on
     * 
     */
    public void putRecentlyUsed(@NonNull PresetItem item, @Nullable List<String> regions) {
        if (mru != null) {
            mru.putRecentlyUsed(item, regions);
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
            try {
                if (element instanceof PresetItem) {
                    String json = ((PresetItem) element).toJSON();
                    if (result.length() != 0) {
                        result.append(",\n");
                    }
                    result.append(json);
                }
            } catch (UnsupportedOperationException ex) {
                // ignore
            }
        });
        return result.toString();
    }

    /**
     * Finds the preset item best matching an OSM element, or null if no preset item matches. To match, all (mandatory)
     * tags of the preset item need to be in the tag set. The preset item does NOT need to have all tags in the tag set,
     * but the tag set needs to have all (mandatory) tags of the preset item.
     * 
     * If multiple items match, the most specific one (i.e. having most tags) wins. If there is a draw, no guarantees
     * are made.
     * 
     * This version takes any match_expression attributes in the candidate presets in to account
     * 
     * @param context an Android context
     * @param presets presets to match against
     * @param tags tags to check against (i.e. tags of a map element)
     * @param regions if not null this will be taken in to account wrt scoring
     * @param osmElement
     * @return @return null, or the "best" matching item for the element
     */
    public static PresetItem findBestMatch(@NonNull Context context, @Nullable Preset[] presets, @Nullable Map<String, String> tags,
            @Nullable List<String> regions, @NonNull OsmElement osmElement) {
        if (tags == null || presets == null) {
            Log.e(DEBUG_TAG, "findBestMatch " + (tags == null ? "tags null" : "presets null"));
            return null;
        }
        // Build candidate list
        Set<PresetItem> possibleMatches = new LinkedHashSet<>();
        buildPossibleMatches(possibleMatches, presets, tags, false, null);
        // check match expressions
        Wrapper wrapper = new Wrapper(context);
        wrapper.setElement(osmElement);
        for (PresetItem candidate : new ArrayList<>(possibleMatches)) {
            String matchExpression = candidate.getMatchExpression();
            if (matchExpression != null) {
                Condition condition = de.blau.android.search.Util.getCondition(conditionCache, matchExpression);
                wrapper.setElement(osmElement);
                if (condition != null && !condition.eval(Wrapper.toJosmFilterType(osmElement), wrapper, tags)) {
                    possibleMatches.remove(candidate);
                }
            }
        }
        return findBestMatch(tags, regions, null, possibleMatches);
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
     * @param regions if not null this will be taken in to account wrt scoring
     * @param ignoreTags Map of keys to ignore
     * @return null, or the "best" matching item for the given tag set
     */
    @Nullable
    public static PresetItem findBestMatch(@Nullable Preset[] presets, @Nullable Map<String, String> tags, @Nullable List<String> regions,
            Map<String, String> ignoreTags) {
        return findBestMatch(presets, tags, regions, null, false, ignoreTags);
    }

    private static final int FIXED_TAG_WEIGHT = 1000;                 // weight per fixed tag
    private static final int RELATION_WEIGHT  = 2 * FIXED_TAG_WEIGHT; // additional weight for relations
    private static final int DOWNGRADE_WEIGHT = 200;

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
     * @param regions if not null this will be taken in to account wrt scoring
     * @param elementType if not null the ElementType will be considered
     * @param useAddressKeys use addr: keys if true
     * @param ignoreTags Map of keys to ignore
     * @return a preset or null if none found
     */
    @Nullable
    public static PresetItem findBestMatch(@Nullable Preset[] presets, @Nullable Map<String, String> tags, @Nullable List<String> regions,
            @Nullable ElementType elementType, boolean useAddressKeys, @Nullable Map<String, String> ignoreTags) {
        if (tags == null || presets == null) {
            Log.e(DEBUG_TAG, "findBestMatch " + (tags == null ? "tags null" : "presets null"));
            return null;
        }

        // Build candidate list
        Set<PresetItem> possibleMatches = new LinkedHashSet<>();
        buildPossibleMatches(possibleMatches, presets, tags, false, ignoreTags);
        // if we only have address keys retry
        if (useAddressKeys && possibleMatches.isEmpty()) {
            buildPossibleMatches(possibleMatches, presets, tags, true, ignoreTags);
        }

        return findBestMatch(tags, regions, elementType, possibleMatches);
    }

    /**
     * Finds the preset item best matching a certain tag set, or null if no preset item matches. To match, all
     * (mandatory) tags of the preset item need to be in the tag set. The preset item does NOT need to have all tags in
     * the tag set, but the tag set needs to have all (mandatory) tags of the preset item.
     * 
     * If multiple items match, the most specific one (i.e. having most tags) wins. If there is a draw, no guarantees
     * are made.
     * 
     * @param tags tags to check against (i.e. tags of a map element)
     * @param regions if not null this will be taken in to account wrt scoring
     * @param elementType if not null the ElementType will be considered
     * @param possibleMatches candidate matches
     * @return the best match or null
     */
    private static PresetItem findBestMatch(Map<String, String> tags, List<String> regions, ElementType elementType, Set<PresetItem> possibleMatches) {
        int bestMatchStrength = 0;
        PresetItem bestMatch = null;
        // Find best
        // always prioritize presets with fixed keys that match
        for (PresetItem possibleMatch : possibleMatches) {
            int fixedTagCount = possibleMatch.getFixedTagCount(regions) * FIXED_TAG_WEIGHT
                    + (ElementType.RELATION == elementType && possibleMatch.isFixedTag(Tags.KEY_TYPE) ? RELATION_WEIGHT : 0);
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
                if (regions != null && !possibleMatch.appliesIn(regions)) {
                    // downgrade so much that recommended tags can't compensate
                    matches -= DOWNGRADE_WEIGHT;
                }
                if (elementType != null && !possibleMatch.appliesTo(elementType)) {
                    // downgrade even more
                    matches -= DOWNGRADE_WEIGHT;
                }
                if (recommendedTagCount > 0) {
                    matches = matches + possibleMatch.matchesRecommended(tags, regions);
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
        // Build candidate list
        Set<PresetItem> possibleMatches = buildPossibleMatches(new LinkedHashSet<>(), presets, tags, false, null);

        // Find match
        for (PresetItem possibleMatch : possibleMatches) {
            if (possibleMatch.getFixedTagCount() > 0) { // has required tags
                if (possibleMatch.matches(tags)) {
                    return possibleMatch;
                }
            } else if (possibleMatch.getRecommendedKeyCount() > 0 && possibleMatch.matchesRecommended(tags, null) > 0) {
                return possibleMatch;
            }
        }
        return null;
    }

    /**
     * Return a set of presets that could match the tags
     * 
     * @param possibleMatches Set to hold result
     * @param presets current presets
     * @param tags the tags
     * @param useAddressKeys use address keys
     * @param ignoreTags Map of tags to ignore
     * @return set of presets
     */
    @NonNull
    private static Set<PresetItem> buildPossibleMatches(@NonNull Set<PresetItem> possibleMatches, @NonNull Preset[] presets, @NonNull Map<String, String> tags,
            boolean useAddressKeys, @Nullable Map<String, String> ignoreTags) {
        for (Preset p : presets) {
            if (p == null) {
                continue;
            }
            for (Entry<String, String> tag : tags.entrySet()) {
                final String key = tag.getKey();
                final String value = tag.getValue();
                final String ignoreValue = ignoreTags != null ? ignoreTags.get(key) : null;
                final boolean ignore = "".equals(ignoreValue) || value.equals(ignoreValue);
                if ((useAddressKeys || !key.startsWith(Tags.KEY_ADDR_BASE)) && !ignore) {
                    String tagString = key + "\t";
                    possibleMatches.addAll(p.objectItems.get(tagString)); // for stuff that doesn't have fixed
                                                                          // values
                    possibleMatches.addAll(p.objectItems.get(tagString + value));
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
    static List<PresetElement> filterElements(@NonNull List<PresetElement> originalElements, @Nullable ElementType type, @Nullable OsmElement osmElement) {
        List<PresetElement> filteredElements = new ArrayList<>();
        Wrapper wrapper = null;
        Map<String, String> tags = null;
        for (PresetElement e : originalElements) {
            if (!e.isDeprecated() && (e.appliesTo(type) || ((e instanceof PresetSeparator) && !filteredElements.isEmpty()
                    && !(filteredElements.get(filteredElements.size() - 1) instanceof PresetSeparator)))) {
                // only add separators if there is a non-separator element above them
                if (osmElement != null && e instanceof PresetItem) {
                    String matchExpression = ((PresetItem) e).getMatchExpression();
                    if (matchExpression != null) {
                        if (wrapper == null) {
                            wrapper = new Wrapper();
                            wrapper.setElement(osmElement);
                            tags = osmElement.getTags();
                        }
                        Condition condition = de.blau.android.search.Util.getCondition(conditionCache, matchExpression);
                        if (condition != null && !condition.eval(Wrapper.toJosmFilterType(osmElement), wrapper, tags)) {
                            continue;
                        }
                    }
                }
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
    public static boolean hasKeyValue(@Nullable PresetTagField field, @Nullable String value) {
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
        for (Preset p : presets) {
            if (p != null) {
                if (type == null) {
                    result.addAll(p.autosuggestNodes.get(key));
                    result.addAll(p.autosuggestWays.get(key));
                    result.addAll(p.autosuggestClosedways.get(key));
                    result.addAll(p.autosuggestRelations.get(key));
                    result.addAll(p.autosuggestAreas.get(key));
                } else {
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
     * @pamam array of presets to dump
     * @param output the File to save to
     * @return true if things worked
     */
    public static boolean generateTaginfoJson(@NonNull Context ctx, @NonNull Preset[] presets, @NonNull File output) {
        try (FileOutputStream fout = new FileOutputStream(output); PrintStream outputStream = new PrintStream(new BufferedOutputStream(fout))) {
            tagInfoHeader(outputStream, "Vespucci", "https://raw.githubusercontent.com/MarcusWolschon/osmeditor4android/master/taginfo.json",
                    "Presets for Vespucci (OSM editor for Android) and JOSM.");
            outputStream.println("\"tags\":[");
            Collection<String> discardedKeys = new DiscardedTags(ctx).getKeys();
            for (String key : discardedKeys) {
                outputStream.println("{\"description\":\"Automatically discarded\",\"key\":\"" + key + "\"},");
            }
            outputStream.println("{\"description\":\"Used for validation\",\"key\":\"check_date\"},");
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
     * Output a header for a taginfo project file
     * 
     * @param outputStream the OutputStream
     * @param projectName name of the (sub-)project
     * @param dataUrl the location of the file
     * @param description a description of the contents
     */
    public static void tagInfoHeader(@NonNull PrintStream outputStream, @NonNull String projectName, @NonNull String dataUrl, @NonNull String description) {
        outputStream.println("{");
        outputStream.println("\"data_format\":1,");
        outputStream.println("\"data_url\":\"" + dataUrl + "\",");
        outputStream.println("\"project\":{");
        outputStream.println("\"name\":\"" + projectName + "\",");
        outputStream.println("\"description\":\"" + description + "\",");
        outputStream.println("\"project_url\":\"https://github.com/MarcusWolschon/osmeditor4android\",");
        outputStream.println("\"doc_url\":\"http://vespucci.io/\",");
        outputStream
                .println("\"icon_url\":\"https://raw.githubusercontent.com/MarcusWolschon/osmeditor4android/master/src/main/res/drawable/vespucci_logo.png\",");
        outputStream.println("\"contact_name\":\"Simon Poole\",");
        outputStream.println("\"contact_email\":\"info@vespucci.io\",");
        outputStream.println("\"keywords\":[");
        outputStream.println("\"editor\"");
        outputStream.println("]},");
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
        s.startTag("", PresetParser.PRESETS);
        for (PresetElement e : getRootGroup().getElements()) {
            e.toXml(s);
        }
        s.endTag("", PresetParser.PRESETS);
        s.endDocument();
    }
}
