package de.blau.android.presets;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import ch.poole.poparser.ParseException;
import ch.poole.poparser.Po;
import ch.poole.poparser.TokenMgrError;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.PresetEditorActivity;
import de.blau.android.util.FileUtil;
import de.blau.android.util.Hash;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.SearchIndexUtils;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.StringWithDescriptionAndIcon;
import de.blau.android.util.Util;
import de.blau.android.util.collections.MultiHashMap;
import de.blau.android.views.WrappingLayout;

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
public class Preset implements Serializable {

    private static final String NO                  = "no";
    private static final String VALUE_TYPE          = "value_type";
    private static final String PRESET_NAME         = "preset_name";
    private static final String PRESET_LINK         = "preset_link";
    private static final String SHORT_DESCRIPTION   = "short_description";
    private static final String DISPLAY_VALUE       = "display_value";
    private static final String LIST_ENTRY          = "list_entry";
    private static final String REFERENCE           = "reference";
    private static final String ROLE                = "role";
    private static final String VALUES_SEARCHABLE   = "values_searchable";
    private static final String EDITABLE            = "editable";
    private static final String VALUES_SORT         = "values_sort";
    private static final String VALUES_CONTEXT      = "values_context";
    private static final String SHORT_DESCRIPTIONS  = "short_descriptions";
    private static final String DISPLAY_VALUES      = "display_values";
    private static final String VALUES              = "values";
    private static final String VALUES_FROM         = "values_from";
    private static final String DELIMITER           = "delimiter";
    private static final String COMBO_FIELD         = "combo";
    private static final String MULTISELECT_FIELD   = "multiselect";
    private static final String YES                 = "yes";
    private static final String DISABLE_OFF         = "disable_off";
    private static final String VALUE_OFF           = "value_off";
    private static final String VALUE_ON            = "value_on";
    private static final String CHECK_FIELD         = "check";
    private static final String HREF                = "href";
    private static final String LINK                = "link";
    private static final String I18N                = "i18n";
    private static final String JAVASCRIPT          = "javascript";
    private static final String DEFAULT             = "default";
    private static final String TEXT_CONTEXT        = "text_context";
    private static final String TEXT_FIELD          = "text";
    private static final String TEXT                = "text";
    private static final String VALUE               = "value";
    private static final String NONE                = "none";
    private static final String MATCH               = "match";
    private static final String CHUNK               = "chunk";
    private static final String KEY_ATTR            = "key";
    private static final String OPTIONAL            = "optional";
    private static final String SEPARATOR           = "separator";
    private static final String ID                  = "id";
    private static final String DEPRECATED          = "deprecated";
    private static final String TRUE                = "true";
    private static final String GTYPE               = "gtype";
    private static final String TYPE                = "type";
    private static final String ITEM                = "item";
    private static final String NAME_CONTEXT        = "name_context";
    private static final String ICON                = "icon";
    private static final String NAME                = "name";
    private static final String OBJECT_KEYS         = "object_keys";
    private static final String GROUP               = "group";
    private static final String PRESETS             = "presets";
    private static final String AREA                = "area";
    private static final String MULTIPOLYGON        = "multipolygon";
    private static final String CLOSEDWAY           = "closedway";
    /**
     * 
     */
    private static final long   serialVersionUID    = 7L;
    /** name of the preset XML file in a preset directory */
    public static final String  PRESETXML           = "preset.xml";
    /** name of the MRU serialization file in a preset directory */
    private static final String MRUFILE             = "mru.dat";
    public static final String  APKPRESET_URLPREFIX = "apk:";

    // hardwired layout stuff
    public static final int SPACING = 5;

    //
    private static final int    MAX_MRU_SIZE = 50;
    private static final String DEBUG_TAG    = Preset.class.getName();

    /** The directory containing all data (xml, MRU data, images) about this preset */
    private File directory;

    /**
     * Lists items having a tag. The map key is tagkey+"\t"+tagvalue. tagItems.get(tagkey+"\t"+tagvalue) will give you
     * all items that have the tag tagkey=tagvalue
     */
    private final MultiHashMap<String, PresetItem> tagItems = new MultiHashMap<>();

    /** The root group of the preset, containing all top-level groups and items */
    private PresetGroup rootGroup;

    /** {@link PresetIconManager} used for icon loading */
    private transient PresetIconManager iconManager;

    /** all known preset items in order of loading */
    private List<PresetItem> allItems = new ArrayList<>();

    /** all known preset groups in order of loading */
    private List<PresetGroup> allGroups = new ArrayList<>();

    /** List of all top level object tags used by this preset */
    private List<String> objectKeys = new ArrayList<>();

    public enum PresetKeyType {
        /**
         * arbitrary single value
         */
        TEXT,
        /**
         * multiple values, single select
         */
        COMBO,
        /**
         * multiple values, multiple select
         */
        MULTISELECT,
        /**
         * single value, set or unset
         */
        CHECK
    }

    public enum MatchType {
        NONE, KEY, KEY_NEG, KEY_VALUE, KEY_VALUE_NEG,
    }

    public enum ValueType {
        OPENING_HOURS, OPENING_HOURS_MIXED, CONDITIONAL, INTEGER, WEBSITE, PHONE, WIKIPEDIA, WIKIDATA;

        /**
         * Get a ValueType corresponding to the input String
         * 
         * @param typeString the ValueType as a String
         * @return the ValueType or null if unknown
         */
        @Nullable
        static ValueType fromString(@NonNull String typeString) {
            ValueType type = null;
            if ("opening_hours".equals(typeString)) {
                type = OPENING_HOURS;
            } else if ("opening_hours_mixed".equals(typeString)) {
                type = OPENING_HOURS_MIXED;
            } else if ("conditional".equals(typeString)) {
                type = CONDITIONAL;
            } else if ("integer".equals(typeString)) {
                type = INTEGER;
            } else if ("website".equals(typeString)) {
                type = WEBSITE;
            } else if ("phone".equals(typeString)) {
                type = PHONE;
            } else if ("wikipedia".equals(typeString)) {
                type = WIKIPEDIA;
            } else if ("wikidata".equals(typeString)) {
                type = WIKIDATA;
            }
            return type;
        }
    }

    static final String COMBO_DELIMITER       = ",";
    static final String MULTISELECT_DELIMITER = ";";

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

    /**
     * Serializable class for storing Most Recently Used information. Hash is used to check compatibility.
     */
    protected static class PresetMRUInfo implements Serializable {
        private static final long serialVersionUID = 7708132207266548491L;

        /** hash of current preset (used to check validity of recentPresets indexes) */
        final String presetHash;

        /** indexes of recently used presets (for use with allItems) */
        LinkedList<Integer> recentPresets = new LinkedList<>();

        private volatile boolean changed = false;

        /**
         * Construct a new instance
         * 
         * @param presetHash a hash for the Preset contents
         */
        PresetMRUInfo(String presetHash) {
            this.presetHash = presetHash;
        }

        /**
         * @return true if the MRU has been change
         */
        public boolean isChanged() {
            return changed;
        }

        /**
         * Mark the MRU as changed
         */
        public void setChanged() {
            this.changed = true;
        }
    }

    private final PresetMRUInfo mru;
    private String              externalPackage;

    private static class PresetFileFilter implements FilenameFilter {
        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(".xml");
        }
    }

    /**
     * create a dummy preset
     */
    public Preset() {
        mru = null;
    }

    /**
     * Creates a preset object.
     * 
     * @param ctx context (used for preset loading)
     * @param directory directory to load/store preset data (XML, icons, MRUs)
     * @param externalPackage name of external package containing preset assets for APK presets, null for other presets
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws NoSuchAlgorithmException
     * @throws Exception
     */
    public Preset(Context ctx, File directory, String externalPackage)
            throws ParserConfigurationException, SAXException, IOException, NoSuchAlgorithmException {
        this.directory = directory;
        this.externalPackage = externalPackage;
        rootGroup = new PresetGroup(null, "", null);

        // noinspection ResultOfMethodCallIgnored
        directory.mkdir();

        InputStream fileStream = null;
        try {
            if (directory.getName().equals(AdvancedPrefDatabase.ID_DEFAULT)) {
                Log.i(DEBUG_TAG, "Loading default preset");
                iconManager = new PresetIconManager(ctx, null, null);
                fileStream = iconManager.openAsset(PRESETXML, true);
                // get translations
                InputStream poFileStream = null;
                try {
                    Locale locale = Locale.getDefault();
                    String language = locale.getLanguage();
                    poFileStream = iconManager.openAsset("preset_" + locale + ".po", true);
                    if (poFileStream == null) {
                        poFileStream = iconManager.openAsset("preset_" + language + ".po", true);
                    }
                    if (poFileStream != null) {
                        try {
                            po = new Po(poFileStream);
                        } catch (ParseException ignored) {
                            Log.e(DEBUG_TAG, "Parsing translation file for " + locale + " or " + language + " failed");
                        } catch (TokenMgrError ignored) {
                            Log.e(DEBUG_TAG, "Parsing translation file for " + locale + " or " + language + " failed");
                        }
                    }
                } finally {
                    SavingHelper.close(poFileStream);
                }
            } else if (externalPackage != null) {
                Log.i(DEBUG_TAG, "Loading APK preset, package=" + externalPackage + ", directory=" + directory.toString());
                iconManager = new PresetIconManager(ctx, directory.toString(), externalPackage);
                fileStream = iconManager.openAsset(PRESETXML, false);
                // po = new Po(iconManager.openAsset("preset_"+Locale.getDefault()+".po", false));
            } else {
                Log.i(DEBUG_TAG, "Loading downloaded preset, directory=" + directory.toString());
                iconManager = new PresetIconManager(ctx, directory.toString(), null);
                File indir = new File(directory.toString());
                File[] list = indir.listFiles(new PresetFileFilter());
                if (list != null && list.length > 0) { // simply use the first XML file found
                    String presetFilename = list[0].getName();
                    Log.i(DEBUG_TAG, "Preset file name " + presetFilename);
                    fileStream = new FileInputStream(new File(directory, presetFilename));
                    // get translations
                    presetFilename = presetFilename.substring(0, presetFilename.length() - 4);
                    InputStream poFileStream = null;
                    try {
                        // try to open .po files either with the same name as the preset file or the standard name
                        try {
                            poFileStream = new FileInputStream(new File(directory, presetFilename + "_" + Locale.getDefault() + ".po"));
                        } catch (FileNotFoundException fnfe) {
                            try {
                                poFileStream = new FileInputStream(new File(directory, presetFilename + "_" + Locale.getDefault().getLanguage() + ".po"));
                            } catch (FileNotFoundException fnfe2) {
                                try {
                                    presetFilename = PRESETXML.substring(0, PRESETXML.length() - 4);
                                    poFileStream = new FileInputStream(new File(directory, presetFilename + "_" + Locale.getDefault() + ".po"));
                                } catch (FileNotFoundException fnfe3) {
                                    try {
                                        poFileStream = new FileInputStream(
                                                new File(directory, presetFilename + "_" + Locale.getDefault().getLanguage() + ".po"));
                                    } catch (FileNotFoundException fnfe4) {
                                        // no translations
                                    }
                                }
                            }
                        }
                        if (poFileStream != null) {
                            try {
                                po = new Po(poFileStream);
                            } catch (ParseException ignored) {
                                Log.e(DEBUG_TAG,
                                        "Parsing translation file for " + Locale.getDefault() + " or " + Locale.getDefault().getLanguage() + " failed");
                            } catch (TokenMgrError ignored) {
                                Log.e(DEBUG_TAG,
                                        "Parsing translation file for " + Locale.getDefault() + " or " + Locale.getDefault().getLanguage() + " failed");
                            }
                        }
                    } finally {
                        SavingHelper.close(poFileStream);
                    }
                } else {
                    Log.e(DEBUG_TAG, "Can't find preset file");
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

            mru = initMRU(directory, hashValue);

            Log.d(DEBUG_TAG, "search index length: " + searchIndex.getKeys().size());
        } finally {
            SavingHelper.close(fileStream);
        }
    }

    /**
     * Construct a new preset from existing elements
     * 
     * @param elements list of PresetElements
     */
    public Preset(@NonNull List<PresetElement> elements) {
        mru = null;
        String name = "Empty Preset";
        if (elements != null && !elements.isEmpty()) {
            name = elements.get(0).getName();
        } else {
            Log.e(DEBUG_TAG, "List of PresetElements was null");
            return;
        }
        rootGroup = new PresetGroup(null, name, null);
        addElementsToIndex(rootGroup, elements);
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
                // build tagItems from existing preset
                for (Entry<String, StringWithDescription> entry : ((PresetItem) e).getFixedTags().entrySet()) {
                    tagItems.add(entry.getKey() + "\t" + entry.getValue().getValue(), (PresetItem) e);
                }
                for (Entry<String, StringWithDescription[]> entry : ((PresetItem) e).getRecommendedTags().entrySet()) {
                    String key = entry.getKey();
                    if (entry.getValue() == null || entry.getValue().length == 0) {
                        tagItems.add(key + "\t", (PresetItem) e);
                    } else {
                        for (StringWithDescription v : entry.getValue()) {
                            tagItems.add(key + "\t" + v.getValue(), (PresetItem) e);
                        }
                    }
                }
                for (Entry<String, StringWithDescription[]> entry : ((PresetItem) e).getOptionalTags().entrySet()) {
                    String key = entry.getKey();
                    if (entry.getValue() == null || entry.getValue().length == 0) {
                        tagItems.add(key + "\t", (PresetItem) e);
                    } else {
                        for (StringWithDescription v : entry.getValue()) {
                            tagItems.add(key + "\t" + v.getValue(), (PresetItem) e);
                        }
                    }
                }
            }
        }
    }

    /**
     * Get the PresetIconManager for this Preset
     * 
     * @param ctx Android Context
     * @return the PresetIconManager instance
     */
    private PresetIconManager getIconManager(Context ctx) {
        if (directory != null) {
            if (directory.getName().equals(AdvancedPrefDatabase.ID_DEFAULT)) {
                return new PresetIconManager(ctx, null, null);
            } else if (externalPackage != null) {
                return new PresetIconManager(ctx, directory.toString(), externalPackage);
            } else {
                return new PresetIconManager(ctx, directory.toString(), null);
            }
        } else {
            return new PresetIconManager(ctx, null, null);
        }
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
     * @throws SAXException
     * @throws IOException
     */
    private void parseXML(InputStream input) throws ParserConfigurationException, SAXException, IOException {
        SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();

        saxParser.parse(input, new DefaultHandler() {
            /** stack of group-subgroup-subsubgroup... where we currently are */
            private Stack<PresetGroup>               groupstack        = new Stack<>();
            /** item currently being processed */
            private PresetItem                       currentItem       = null;
            /** true if we are currently processing the optional section of an item */
            private boolean                          inOptionalSection = false;
            /** hold reference to chunks */
            private HashMap<String, PresetItem>      chunks            = new HashMap<>();
            /** store current combo or multiselect key */
            private String                           listKey           = null;
            private ArrayList<StringWithDescription> listValues        = null;
            private String                           valuesContext     = null;
            private String                           delimiter         = null;

            {
                groupstack.push(rootGroup);
            }

            /**
             * ${@inheritDoc}.
             */
            @Override
            public void startElement(String uri, String localName, String name, Attributes attr) throws SAXException {
                if (PRESETS.equals(name)) {
                    String objectKeysTemp = attr.getValue(OBJECT_KEYS);
                    if (objectKeysTemp != null) {
                        String[] tempArray = objectKeysTemp.split("\\s*,\\s*");
                        if (tempArray != null && tempArray.length > 0) {
                            objectKeys.addAll(Arrays.asList(tempArray));
                        }
                    }
                } else if (GROUP.equals(name)) {
                    PresetGroup parent = groupstack.peek();
                    PresetGroup g = new PresetGroup(parent, attr.getValue(NAME), attr.getValue(ICON));
                    String context = attr.getValue(NAME_CONTEXT);
                    if (context != null) {
                        g.setNameContext(context);
                    }
                    groupstack.push(g);
                } else if (ITEM.equals(name)) {
                    if (currentItem != null) {
                        throw new SAXException("Nested items are not allowed");
                    }
                    PresetGroup parent = groupstack.peek();
                    String type = attr.getValue(TYPE);
                    if (type == null) {
                        type = attr.getValue(GTYPE); // note gtype seems to be undocumented
                    }
                    currentItem = new PresetItem(parent, attr.getValue(NAME), attr.getValue(ICON), type);
                    String context = attr.getValue(NAME_CONTEXT);
                    if (context != null) {
                        currentItem.setNameContext(context);
                    }
                    currentItem.setDeprecated(TRUE.equals(attr.getValue(DEPRECATED)));
                } else if (CHUNK.equals(name)) {
                    if (currentItem != null) {
                        throw new SAXException("Nested items are not allowed");
                    }
                    String type = attr.getValue(TYPE);
                    if (type == null) {
                        type = attr.getValue(GTYPE); // note gtype seems to be undocumented
                    }
                    currentItem = new PresetItem(null, attr.getValue(ID), attr.getValue(ICON), type);
                    currentItem.setChunk();
                } else if (SEPARATOR.equals(name)) {
                    new PresetSeparator(groupstack.peek());
                } else if (currentItem != null) { // the following only make sense if we actually found an item
                    if (OPTIONAL.equals(name)) {
                        inOptionalSection = true;
                    } else if (KEY_ATTR.equals(name)) {
                        String key = attr.getValue(KEY_ATTR);
                        String match = attr.getValue(MATCH);
                        if (!inOptionalSection) {
                            if (NONE.equals(match)) {// don't include in fixed tags if not used for matching
                                currentItem.addTag(false, key, PresetKeyType.TEXT, attr.getValue(VALUE));
                            } else {
                                currentItem.addTag(key, PresetKeyType.TEXT, attr.getValue(VALUE), attr.getValue(TEXT));
                            }
                        } else {
                            // Optional fixed tags should not happen, their values will NOT be automatically inserted.
                            currentItem.addTag(true, key, PresetKeyType.TEXT, attr.getValue(VALUE));
                        }
                        if (match != null) {
                            currentItem.setMatchType(key, match);
                        }
                        String textContext = attr.getValue(TEXT_CONTEXT);
                        if (textContext != null) {
                            currentItem.setTextContext(key, textContext);
                        }
                    } else if (TEXT_FIELD.equals(name)) {
                        String key = attr.getValue(KEY_ATTR);
                        currentItem.addTag(inOptionalSection, key, PresetKeyType.TEXT, (String) null);
                        String text = attr.getValue(TEXT);
                        if (text != null) {
                            currentItem.addHint(attr.getValue(KEY_ATTR), text);
                        }
                        String defaultValue = attr.getValue(DEFAULT);
                        if (defaultValue != null) {
                            currentItem.addDefault(key, defaultValue);
                        }
                        String textContext = attr.getValue(TEXT_CONTEXT);
                        if (textContext != null) {
                            currentItem.setTextContext(key, textContext);
                        }
                        String match = attr.getValue(MATCH);
                        if (match != null) {
                            currentItem.setMatchType(key, match);
                        }
                        String javaScript = attr.getValue(JAVASCRIPT);
                        if (javaScript != null) {
                            currentItem.setJavaScript(key, javaScript);
                        }
                        if (TRUE.equals(attr.getValue(I18N))) {
                            currentItem.setI18n(key);
                        }
                        String valueType = attr.getValue(VALUE_TYPE);
                        if (valueType != null) {
                            currentItem.setValueType(key, valueType);
                        }
                    } else if (LINK.equals(name)) {
                        String language = Locale.getDefault().getLanguage();
                        String href = attr.getValue(language.toLowerCase(Locale.US) + ".href");
                        if (href == null) {
                            href = attr.getValue(HREF);
                        }
                        if (href != null) {
                            currentItem.setMapFeatures(href);
                        }
                    } else if (CHECK_FIELD.equals(name)) {
                        String key = attr.getValue(KEY_ATTR);
                        String value_on = attr.getValue(VALUE_ON) == null ? YES : attr.getValue(VALUE_ON);
                        String value_off = attr.getValue(VALUE_OFF) == null ? NO : attr.getValue(VALUE_OFF);
                        String disable_off = attr.getValue(DISABLE_OFF);
                        String values = value_on;
                        // zap value_off if disabled
                        if (disable_off != null && disable_off.equals(TRUE)) {
                            value_off = "";
                        } else {
                            values = value_on + COMBO_DELIMITER + value_off;
                        }
                        StringBuilder displayValuesBuilder = new StringBuilder(); // FIXME this is a bit of a hack as
                                                                                  // there is no display_values
                                                                                  // attribute for checks
                        boolean first = true;
                        for (String v : values.split(COMBO_DELIMITER)) {
                            if (!first) {
                                displayValuesBuilder.append(COMBO_DELIMITER);
                            } else {
                                first = false;
                            }
                            displayValuesBuilder.append(Util.capitalize(v));
                        }
                        currentItem.setSort(key, false); // don't sort
                        currentItem.addTag(inOptionalSection, key, PresetKeyType.CHECK, values, displayValuesBuilder.toString(), null, COMBO_DELIMITER, null);
                        if (!YES.equals(value_on)) {
                            currentItem.addOnValue(key, value_on);
                        }
                        String defaultValue = attr.getValue(DEFAULT) == null ? null : ("on".equals(attr.getValue(DEFAULT)) ? value_on : value_off);
                        if (defaultValue != null) {
                            currentItem.addDefault(key, defaultValue);
                        }
                        String text = attr.getValue(TEXT);
                        if (text != null) {
                            currentItem.addHint(key, text);
                        }
                        String textContext = attr.getValue(TEXT_CONTEXT);
                        if (textContext != null) {
                            currentItem.setTextContext(key, textContext);
                        }
                        String match = attr.getValue(MATCH);
                        if (match != null) {
                            currentItem.setMatchType(key, match);
                        }
                    } else if (COMBO_FIELD.equals(name) || MULTISELECT_FIELD.equals(name)) {
                        boolean multiselect = MULTISELECT_FIELD.equals(name);
                        String key = attr.getValue(KEY_ATTR);
                        delimiter = attr.getValue(DELIMITER);
                        if (delimiter == null) {
                            delimiter = multiselect ? MULTISELECT_DELIMITER : COMBO_DELIMITER;
                        }
                        String values = attr.getValue(VALUES);
                        String displayValues = attr.getValue(DISPLAY_VALUES);
                        String shortDescriptions = attr.getValue(SHORT_DESCRIPTIONS);
                        valuesContext = attr.getValue(VALUES_CONTEXT);
                        String valuesFrom = attr.getValue(VALUES_FROM);
                        final PresetKeyType keyType = multiselect ? PresetKeyType.MULTISELECT : PresetKeyType.COMBO;
                        if (values != null) {
                            currentItem.addTag(inOptionalSection, key, keyType, values, displayValues, shortDescriptions, delimiter, valuesContext);
                        } else if (valuesFrom != null) {
                            setValuesFromMethod(key, valuesFrom, keyType, currentItem, inOptionalSection, delimiter);
                        } else {
                            listKey = key;
                            listValues = new ArrayList<>();
                        }

                        String defaultValue = attr.getValue(DEFAULT);
                        if (defaultValue != null) {
                            currentItem.addDefault(key, defaultValue);
                        }
                        String text = attr.getValue(TEXT);
                        if (text != null) {
                            currentItem.addHint(key, text);
                        }
                        String textContext = attr.getValue(TEXT_CONTEXT);
                        if (textContext != null) {
                            currentItem.setTextContext(key, textContext);
                        }
                        String match = attr.getValue(MATCH);
                        if (match != null) {
                            currentItem.setMatchType(key, match);
                        }
                        String sort = attr.getValue(VALUES_SORT);
                        if (sort != null) {
                            // normally this will not be set because true is the default
                            currentItem.setSort(key, YES.equals(sort) || TRUE.equals(sort));
                        }
                        String editable = attr.getValue(EDITABLE);
                        if (editable != null) {
                            currentItem.setEditable(key, YES.equals(editable) || TRUE.equals(editable));
                        }
                        String searchable = attr.getValue(VALUES_SEARCHABLE);
                        if (searchable != null) {
                            currentItem.setValuesSearchable(key, YES.equals(searchable) || TRUE.equals(searchable));
                        }
                        String valueType = attr.getValue(VALUE_TYPE);
                        if (valueType != null) {
                            currentItem.setValueType(key, valueType);
                        }
                    } else if (ROLE.equals(name)) {
                        String key = attr.getValue(KEY_ATTR);
                        String text = attr.getValue(TEXT);
                        String textContext = attr.getValue(TEXT_CONTEXT);
                        if (textContext != null) {
                            currentItem.setTextContext(key, textContext);
                        }
                        currentItem.addRole(new StringWithDescription(key,
                                po != null && text != null ? (textContext != null ? po.t(textContext, text) : po.t(text)) : text));
                    } else if (REFERENCE.equals(name)) {
                        PresetItem chunk = chunks.get(attr.getValue("ref")); // note this assumes that there are no
                                                                             // forward references
                        if (chunk != null) {
                            if (inOptionalSection) {
                                // fixed tags don't make sense in an optional section, and doesn't seem to happen in
                                // practice
                                if (chunk.getFixedTagCount() > 0) {
                                    Log.e(DEBUG_TAG, "Chunk " + chunk.name + " has fixed tags but is used in an optional section");
                                }
                                currentItem.optionalTags.putAll(chunk.getRecommendedTags());
                            } else {
                                currentItem.fixedTags.putAll(chunk.getFixedTags());
                                if (!currentItem.isChunk()) {
                                    for (Entry<String, StringWithDescription> e : chunk.getFixedTags().entrySet()) {
                                        String key = e.getKey();
                                        StringWithDescription v = e.getValue();
                                        String value = "";
                                        if (v != null && v.getValue() != null) {
                                            value = v.getValue();
                                        }
                                        tagItems.add(key + "\t" + value, currentItem);
                                        currentItem.addToAutosuggest(key, v);
                                    }
                                }
                                currentItem.recommendedTags.putAll(chunk.getRecommendedTags());
                            }

                            addToTagItems(currentItem, chunk.getRecommendedTags());
                            currentItem.optionalTags.putAll(chunk.getOptionalTags());
                            addToTagItems(currentItem, chunk.getOptionalTags());

                            currentItem.hints.putAll(chunk.hints);
                            currentItem.addAllDefaults(chunk.defaults);
                            currentItem.keyType.putAll(chunk.keyType);
                            currentItem.setAllMatchTypes(chunk.matchType);
                            currentItem.addAllRoles(chunk.roles); // FIXME this and the following could lead to
                                                                  // duplicate entries
                            currentItem.addAllLinkedPresetNames(chunk.linkedPresetNames);
                            currentItem.setAllSort(chunk.sort);
                            currentItem.setAllJavaScript(chunk.javascript);
                            currentItem.setAllEditable(chunk.editable);
                            currentItem.setAllValuesSearchable(chunk.valuesSearchable);
                            currentItem.addAllDelimiters(chunk.delimiters);
                            currentItem.addAllI18n(chunk.i18n);
                            currentItem.setAllValueTypes(chunk.valueType);
                        }
                    } else if (LIST_ENTRY.equals(name)) {
                        if (listValues != null) {
                            String v = attr.getValue(VALUE);
                            if (v != null) {
                                String d = attr.getValue(DISPLAY_VALUE);
                                if (d == null) {
                                    d = attr.getValue(SHORT_DESCRIPTION);
                                }
                                String iconPath = attr.getValue(ICON);
                                if (iconPath == null) {
                                    listValues.add(new StringWithDescription(v, po != null ? (valuesContext != null ? po.t(valuesContext, d) : po.t(d)) : d));
                                } else {
                                    listValues.add(new StringWithDescriptionAndIcon(v,
                                            po != null ? (valuesContext != null ? po.t(valuesContext, d) : po.t(d)) : d, iconPath));
                                }
                            }
                        }
                    } else if (PRESET_LINK.equals(name)) {
                        String presetName = attr.getValue(PRESET_NAME);
                        if (presetName != null) {
                            currentItem.addLinkedPresetName(presetName);
                        }
                    }
                } else {
                    Log.d(DEBUG_TAG, name + " must be in a preset item");
                    throw new SAXException(name + " must be in a preset item");
                }
            }

            /**
             * Set values by calling a method
             * 
             * As this might take longer and include network calls it needs to be done async, however on the other hand
             * this may cause concurrent modification expection and have to be looked at
             * 
             * @param key the key we want values for
             * @param valuesFrom the method spec as a String
             * @param keyType what kind of key this is
             * @param item the PresetItem we want to add this to
             * @param inOptionalSection if this key optional
             * @param delimiter delimiter for multi-valued keys
             */
            private void setValuesFromMethod(final String key, final String valuesFrom, final PresetKeyType keyType, final PresetItem item,
                    final boolean inOptionalSection, final String delimiter) {
                (new AsyncTask<Void, Void, Object>() {
                    @Override
                    protected Object doInBackground(Void... params) {
                        Object result = de.blau.android.presets.Util.invokeMethod(valuesFrom, key);
                        if (result instanceof String[]) {
                            int count = ((String[]) result).length;
                            StringWithDescription[] valueArray = new StringWithDescription[count];
                            for (int i = 0; i < count; i++) {
                                StringWithDescription swd = new StringWithDescription(((String[]) result)[i]);
                                valueArray[i] = swd;
                            }
                            item.addTag(inOptionalSection, key, keyType, valueArray, delimiter);
                        } else if (result instanceof StringWithDescription[]) {
                            item.addTag(inOptionalSection, key, keyType, (StringWithDescription[]) result, delimiter);
                        }
                        return null;
                    }
                }).execute();
            }

            void addToTagItems(PresetItem currentItem, Map<String, StringWithDescription[]> tags) {
                if (currentItem.isChunk()) { // only do this on the final expansion
                    return;
                }
                for (Entry<String, StringWithDescription[]> e : tags.entrySet()) {
                    StringWithDescription values[] = e.getValue();
                    String key = e.getKey();
                    tagItems.add(key + "\t", currentItem);
                    if (values != null) {
                        for (StringWithDescription v : values) {
                            String value = "";
                            if (v != null && v.getValue() != null) {
                                value = v.getValue();
                            }
                            tagItems.add(key + "\t" + value, currentItem);
                        }
                        currentItem.addToAutosuggest(key, values);
                    }
                }
            }

            @Override
            public void endElement(String uri, String localName, String name) throws SAXException {
                if (GROUP.equals(name)) {
                    groupstack.pop();
                } else if (OPTIONAL.equals(name)) {
                    inOptionalSection = false;
                } else if (ITEM.equals(name)) {
                    // Log.d(DEBUG_TAG,"PresetItem: " + currentItem.toString());
                    if (!currentItem.isDeprecated()) {
                        currentItem.buildSearchIndex();
                    }
                    currentItem = null;
                    listKey = null;
                    listValues = null;
                } else if (CHUNK.equals(name)) {
                    chunks.put(currentItem.getName(), currentItem);
                    currentItem = null;
                    listKey = null;
                    listValues = null;
                } else if (COMBO_FIELD.equals(name) || MULTISELECT_FIELD.equals(name)) {
                    if (listKey != null && listValues != null) {
                        StringWithDescription[] v = new StringWithDescription[listValues.size()];
                        currentItem.addTag(inOptionalSection, listKey, COMBO_FIELD.equals(name) ? PresetKeyType.COMBO : PresetKeyType.MULTISELECT,
                                listValues.toArray(v), delimiter);
                    }
                    listKey = null;
                    listValues = null;
                }
            }

        });
    }

    /**
     * Initializes Most-recently-used data by either loading them or creating an empty list
     * 
     * @param directory data directory of the preset
     * @param hashValue XML hash value to check if stored data fits the XML
     * @return a MRU object valid for this Preset, never null
     */
    public PresetMRUInfo initMRU(File directory, String hashValue) {
        PresetMRUInfo tmpMRU;
        ObjectInputStream mruReader = null;
        FileInputStream fout = null;
        try {
            fout = new FileInputStream(new File(directory, MRUFILE));
            mruReader = new ObjectInputStream(fout);
            tmpMRU = (PresetMRUInfo) mruReader.readObject();
            if (!tmpMRU.presetHash.equals(hashValue)) {
                throw new InvalidObjectException("hash mismatch");
            }
        } catch (Exception e) {
            tmpMRU = new PresetMRUInfo(hashValue);
            // Deserialization failed for whatever reason (missing file, wrong version, ...) - use empty list
            Log.i(DEBUG_TAG, "No usable old MRU list, creating new one (" + e.toString() + ")");
        } finally {
            SavingHelper.close(mruReader);
            SavingHelper.close(fout);
        }
        return tmpMRU;
    }

    /**
     * Returns a list of icon URLs referenced by a preset
     * 
     * @param presetDir a File object pointing to the directory containing this preset
     * @return an ArrayList of http and https URLs as string, or null if there is an error during parsing
     */
    public static List<String> parseForURLs(File presetDir) {
        final ArrayList<String> urls = new ArrayList<>();
        File[] list = presetDir.listFiles(new PresetFileFilter());
        String presetFilename = null;
        if (list != null) {
            if (list.length > 0) { // simply use the first XML file found
                presetFilename = list[0].getName();
            }
        } else {
            return null;
        }
        try {
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();

            saxParser.parse(new File(presetDir, presetFilename), new DefaultHandler() {
                /**
                 * ${@inheritDoc}.
                 */
                @Override
                public void startElement(String uri, String locaclName, String name, Attributes attr) throws SAXException {
                    if (GROUP.equals(name) || ITEM.equals(name)) {
                        String url = attr.getValue(ICON);
                        if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                            urls.add(url);
                        }
                    }
                }
            });
        } catch (Exception e) {
            Log.e("PresetURLParser", "Error parsing preset", e);
            return null;
        }
        return urls;
    }

    /** @return the root group of the preset, containing all top-level groups and items */
    public PresetGroup getRootGroup() {
        return rootGroup;
    }

    public void setRootGroup(@NonNull PresetGroup rootGroup) {
        this.rootGroup = rootGroup;
    }

    /**
     * Check if this Preset contains a PresetItem
     * 
     * @param pi the PresetItem we are interested in
     * @return true if the item is from this Preset
     */
    public boolean contains(PresetItem pi) {
        return allItems.contains(pi);
    }

    /**
     * Return PresetItems containing the tag in question
     * 
     * @param tag tag in the format: key \t value
     * @return a Set containing the PresetItems or null if none found
     */
    @Nullable
    Set<PresetItem> getItemByTag(@NonNull String tag) {
        return tagItems.get(tag);
    }

    /**
     * Return the index of a PresetItem by sequential search FIXME
     * 
     * @param name the name of the PresetItem
     * @return the index or null if not found
     */
    private Integer getItemIndexByName(@NonNull String name) {
        Log.d(DEBUG_TAG, "getItemIndexByName " + name);
        for (PresetItem pi : allItems) {
            if (pi != null) {
                String n = pi.getName();
                if (n != null && n.equals(name)) {
                    return pi.getItemIndex();
                }
            }
        }
        Log.d(DEBUG_TAG, "getItemIndexByName " + name + " not found");
        return null;
    }

    /**
     * Return a preset by name Note: the names are not guaranteed to be unique, this will simple return the first found
     * 
     * @param name the name to search for
     * @return the preset item or null if not found
     */
    @Nullable
    public PresetItem getItemByName(@NonNull String name) {
        Integer index = getItemIndexByName(name);
        if (index != null) {
            return allItems.get(index);
        }
        return null;
    }

    /**
     * Return a preset by index
     * 
     * @param index the index value
     * @return the preset item or null if not found
     */
    @Nullable
    public PresetItem getItemByIndex(int index) {
        return allItems.get(index);
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
     * Return a PresetElement by identifying it with its place in the hierarchy
     * 
     * @param group PresetGroup to start the search at
     * @param path the path
     * @return the PresetElement or null if not found
     */
    @Nullable
    public static PresetElement getElementByPath(@NonNull PresetGroup group, @NonNull PresetElementPath path) {
        int size = path.path.size();
        if (size > 0) {
            String segment = path.path.get(0);
            for (PresetElement e : group.getElements()) {
                if (segment.equals(e.getName())) {
                    if (size == 1) {
                        return e;
                    } else {
                        if (e instanceof PresetGroup) {
                            PresetElementPath newPath = new PresetElementPath(path);
                            newPath.path.remove(0);
                            return getElementByPath((PresetGroup) e, newPath);
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Return a preset group by index
     * 
     * @param index the index value
     * @return the preset item or null if not found
     */
    @Nullable
    public PresetGroup getGroupByIndex(int index) {
        return allGroups.get(index);
    }

    /**
     * Returns a view showing the most recently used presets
     * 
     * @param ctx Android Context
     * @param presets the array of currently active presets
     * @param handler the handler which will handle clicks on the presets
     * @param type filter to show only presets applying to this type
     * @return the view
     */
    public View getRecentPresetView(Context ctx, Preset[] presets, PresetClickHandler handler, ElementType type) {
        PresetGroup recent = new PresetGroup(null, "recent", null);
        for (Preset p : presets) {
            if (p != null && p.hasMRU()) {
                int allItemsCount = p.allItems.size();
                for (Integer index : p.mru.recentPresets) {
                    if (index < allItemsCount) {
                        recent.addElement(p.allItems.get(index));
                    }
                }
            }
        }
        return recent.getGroupView(ctx, handler, type, null);
    }

    public boolean hasMRU() {
        return mru != null && !mru.recentPresets.isEmpty();
    }

    /**
     * Add a preset to the front of the MRU list (removing old duplicates and limiting the list to 50 entries if needed)
     * 
     * @param item the item to add
     */
    public void putRecentlyUsed(PresetItem item) {
        Integer id = item.getItemIndex();
        if (mru == null) {
            return;
        }
        // prevent duplicates
        if (!mru.recentPresets.remove(id)) { // calling remove(Object), i.e. removing the number if it is in the list,
                                             // not the i-th item
            // preset is not in the list, add linked presets first
            PresetItem pi = allItems.get(id);
            if (pi.getLinkedPresetNames() != null) {
                LinkedList<String> linkedPresetNames = new LinkedList<>(pi.getLinkedPresetNames());
                Collections.reverse(linkedPresetNames);
                for (String n : linkedPresetNames) {
                    if (!mru.recentPresets.contains(id)) {
                        Integer presetIndex = getItemIndexByName(n);
                        if (presetIndex != null) { // null if the link wasn't found
                            if (!mru.recentPresets.contains(presetIndex)) { // only add if not already present
                                mru.recentPresets.addFirst(presetIndex);
                                if (mru.recentPresets.size() > MAX_MRU_SIZE) {
                                    mru.recentPresets.removeLast();
                                }
                            }
                        } else {
                            Log.e(DEBUG_TAG, "linked preset not found for " + n + " in preset " + pi.getName());
                        }
                    }
                }
            }
        }
        mru.recentPresets.addFirst(id);
        if (mru.recentPresets.size() > MAX_MRU_SIZE) {
            mru.recentPresets.removeLast();
        }
        mru.setChanged();
    }

    /**
     * Remove a preset
     * 
     * @param item the item to remove
     */
    public void removeRecentlyUsed(PresetItem item) {
        Integer id = item.getItemIndex();
        // prevent duplicates
        mru.recentPresets.remove(id); // calling remove(Object), i.e. removing the number if it is in the list, not the
                                      // i-th item
        mru.setChanged();
    }

    /**
     * Reset the MRU list
     */
    public void resetRecentlyUsed() {
        mru.recentPresets = new LinkedList<>();
        mru.setChanged();
        saveMRU();
    }

    /** Saves the current MRU data to a file */
    public void saveMRU() {
        if (mru != null && mru.isChanged()) {
            ObjectOutputStream out = null;
            FileOutputStream fout = null;
            try {
                fout = new FileOutputStream(new File(directory, MRUFILE));
                out = new ObjectOutputStream(fout);
                out.writeObject(mru);
            } catch (Exception e) {
                Log.e(DEBUG_TAG, "MRU saving failed", e);
            } finally {
                SavingHelper.close(out);
                SavingHelper.close(fout);
            }
        }
    }

    /**
     * Generate a JSON representation of all PresetItems in this Preset
     * 
     * @return JSON format string
     */
    private String toJSON() {
        StringBuilder result = new StringBuilder();
        for (PresetItem pi : allItems) {
            if (!pi.isChunk()) {
                if (result.length() != 0) {
                    result.append(",\n");
                }
                result.append(pi.toJSON());
            }
        }
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
     * @return null, or the "best" matching item for the given tag set
     */
    public static PresetItem findBestMatch(Preset presets[], Map<String, String> tags) {
        return findBestMatch(presets, tags, false);
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
     * @param useAddressKeys use addr: keys if true
     * @return a preset or null if none found
     */
    public static PresetItem findBestMatch(Preset[] presets, Map<String, String> tags, boolean useAddressKeys) {
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
        final int FIXED_WEIGHT = 100; // always prioritize presets with fixed keys
        for (PresetItem possibleMatch : possibleMatches) {
            int fixedTagCount = possibleMatch.getFixedTagCount() * FIXED_WEIGHT;
            int recommendedTagCount = possibleMatch.getRecommendedTags().size();
            if (fixedTagCount + recommendedTagCount < bestMatchStrength) {
                continue; // isn't going to help
            }
            int matches = 0;
            if (fixedTagCount > 0) { // has required tags
                if (possibleMatch.matches(tags)) {
                    matches = fixedTagCount;
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
    public static PresetItem findMatch(@NonNull Preset presets[], @NonNull Map<String, String> tags) {
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
            } else if (possibleMatch.getRecommendedTags().size() > 0 && possibleMatch.matchesRecommended(tags) > 0) {
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
    private static Set<PresetItem> buildPossibleMatches(Preset[] presets, Map<String, String> tags, boolean useAddressKeys) {
        HashSet<PresetItem> possibleMatches = new HashSet<>();
        for (Preset p : presets) {
            if (p != null) {
                for (Entry<String, String> tag : tags.entrySet()) {
                    String key = tag.getKey();
                    if (Tags.IMPORTANT_TAGS.contains(key) || p.isObjectKey(key) || (key.startsWith(Tags.KEY_ADDR_BASE) && useAddressKeys)) {
                        String tagString = tag.getKey() + "\t";
                        possibleMatches.addAll(p.tagItems.get(tagString)); // for stuff that doesn't have fixed values
                        possibleMatches.addAll(p.tagItems.get(tagString + tag.getValue()));
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
    private static ArrayList<PresetElement> filterElements(ArrayList<PresetElement> originalElements, ElementType type) {
        ArrayList<PresetElement> filteredElements = new ArrayList<>();
        for (PresetElement e : originalElements) {
            if (!e.isDeprecated()) {
                if (e.appliesTo(type)) {
                    filteredElements.add(e);
                } else if ((e instanceof PresetSeparator) && !filteredElements.isEmpty()
                        && !(filteredElements.get(filteredElements.size() - 1) instanceof PresetSeparator)) {
                    // add separators if there is a non-separator element above them
                    filteredElements.add(e);
                }
            }
        }
        return filteredElements;
    }

    /**
     * Represents an element (group or item) in a preset data structure
     */
    public abstract class PresetElement implements Serializable {

        public static final int          ICON_SIZE_DP     = 36;
        /**
         * 
         */
        private static final long        serialVersionUID = 5L;
        String                           name;
        String                           nameContext      = null;
        private String                   iconpath;
        private String                   mapiconpath;
        private transient Drawable       icon;
        private transient BitmapDrawable mapIcon;
        PresetGroup                      parent;
        boolean                          appliesToWay;
        boolean                          appliesToNode;
        boolean                          appliesToClosedway;
        boolean                          appliesToRelation;
        boolean                          appliesToArea;
        private boolean                  deprecated       = false;
        private String                   region           = null;
        private String                   mapFeatures;

        /**
         * Creates the element, setting parent, name and icon, and registers with the parent
         * 
         * @param parent parent ParentGroup (or null if this is the root group)
         * @param name name of the element or null
         * @param iconpath the icon path (either "http://" URL or "presets/" local image reference) or null
         */
        public PresetElement(@Nullable PresetGroup parent, @Nullable String name, @Nullable String iconpath) {
            this.parent = parent;
            this.name = name;
            this.iconpath = iconpath;
            mapiconpath = iconpath;
            icon = null;
            mapIcon = null;
            if (parent != null) {
                parent.addElement(this);
            }
        }

        /**
         * Construct a new PresetElement in this preset from an existing one
         * 
         * @param group PresetGroup this should be added, null if none
         * @param item the PresetElement to copy
         */
        public PresetElement(@Nullable PresetGroup group, @NonNull PresetElement item) {
            this.name = item.name;
            if (group != null) {
                group.addElement(this);
            }
            this.iconpath = item.iconpath;
            mapiconpath = item.iconpath;
            icon = null;
            mapIcon = null;
            if (item.appliesToNode) {
                setAppliesToNode();
            }
            if (item.appliesToWay) {
                setAppliesToWay();
            }
            if (item.appliesToClosedway) {
                setAppliesToClosedway();
            }
            if (item.appliesToArea) {
                setAppliesToArea();
            }
            if (item.appliesToRelation) {
                setAppliesToRelation();
            }
            this.deprecated = item.deprecated;
            this.region = item.region;
            this.mapFeatures = item.mapFeatures;
        }

        /**
         * Get the name of this element
         * 
         * @return the name if set or null
         */
        @Nullable
        public String getName() {
            return name;
        }

        /**
         * Return the name of this preset element, potentially translated
         * 
         * @return the name
         */
        public String getTranslatedName() {
            if (nameContext != null) {
                return po != null ? po.t(nameContext, getName()) : getName();
            }
            return po != null ? po.t(getName()) : getName();
        }

        /**
         * Return the icon for the preset or a place holder
         * 
         * @return a Drawable with the icon or a place holder for it
         */
        @NonNull
        public Drawable getIcon() {
            if (icon == null) {
                icon = getIcon(iconpath, ICON_SIZE_DP);
            }
            return icon;
        }

        /**
         * Return the icon from the preset or a place holder
         * 
         * @param path path to the icon
         * @param iconSize size of the sides of the icon in DP
         * @return a Drawable with the icon or a place holder for it
         */
        @NonNull
        public Drawable getIcon(@Nullable String path, int iconSize) {
            if (iconManager == null) {
                iconManager = getIconManager(App.getCurrentInstance().getApplicationContext());
            }
            if (path != null) {
                return iconManager.getDrawableOrPlaceholder(path, iconSize);
            } else {
                return iconManager.getPlaceHolder(iconSize);
            }
        }

        /**
         * Return the icon from the preset if it exists
         * 
         * @param path path to the icon
         * @return a Drawable with the icon or or null if it can't be found
         */
        @Nullable
        public Drawable getIconIfExists(@Nullable String path) {
            if (iconManager == null) {
                iconManager = getIconManager(App.getCurrentInstance().getApplicationContext());
            }
            if (path != null) {
                return iconManager.getDrawable(path, ICON_SIZE_DP);
            }
            return null;
        }

        /**
         * Get an icon suitable for drawing on the map
         * 
         * @return a small icon
         */
        public BitmapDrawable getMapIcon() {
            if (mapIcon == null && mapiconpath != null) {
                if (iconManager == null) {
                    iconManager = getIconManager(App.getCurrentInstance().getApplicationContext());
                }
                mapIcon = iconManager.getDrawable(mapiconpath, de.blau.android.Map.ICON_SIZE_DP);
                mapiconpath = null;
            }
            return mapIcon;
        }

        /**
         * Get the parent of this PresetElement
         * 
         * @return the parent PresetGroup or null if none
         */
        @Nullable
        public PresetGroup getParent() {
            return parent;
        }

        /**
         * Set the parent PresetGroup
         * 
         * @param pg the parent to set
         */
        public void setParent(PresetGroup pg) {
            parent = pg;
        }

        /**
         * Returns a basic view representing the current element (i.e. a button with icon and name). Can (and should) be
         * used when implementing {@link #getView(PresetClickHandler)}.
         * 
         * @param ctx Android Context
         * @param selected if true highlight the background
         * @return the view
         */
        private TextView getBaseView(@NonNull Context ctx, boolean selected) {
            Resources res = ctx.getResources();
            // GradientDrawable shape = new GradientDrawable();
            // shape.setCornerRadius(8);
            TextView v = new TextView(ctx);
            float density = res.getDisplayMetrics().density;
            v.setText(getTranslatedName());
            v.setTextColor(ContextCompat.getColor(ctx, R.color.preset_text));
            v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            v.setEllipsize(TextUtils.TruncateAt.END);
            v.setMaxLines(2);
            v.setPadding((int) (4 * density), (int) (4 * density), (int) (4 * density), (int) (4 * density));
            Drawable viewIcon = getIcon();
            v.setCompoundDrawables(null, viewIcon, null, null);
            v.setCompoundDrawablePadding((int) (4 * density));
            // this seems to be necessary to work around
            // https://issuetracker.google.com/issues/37003658
            v.setLayoutParams(new LinearLayout.LayoutParams((int) (72 * density), (int) (72 * density)));
            v.setWidth((int) (72 * density));
            v.setHeight((int) (72 * density));
            v.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            v.setSaveEnabled(false);
            return v;
        }

        /**
         * Returns a view representing this element (i.e. a button with icon and name) Implement this in subtypes
         * 
         * @param ctx Android Context
         * @param handler handler to handle clicks on the element (may be null)
         * @param selected highlight this element
         * @return a view ready to display to represent this element
         */
        public abstract View getView(@NonNull Context ctx, @Nullable final PresetClickHandler handler, boolean selected);

        /**
         * Test what kind of elements this PresetElement applies to
         * 
         * @param type the ElementType to check for
         * @return true if applicable
         */
        public boolean appliesTo(ElementType type) {
            switch (type) {
            case NODE:
                return appliesToNode;
            case WAY:
                return appliesToWay;
            case CLOSEDWAY:
                return appliesToClosedway;
            case RELATION:
                return appliesToRelation;
            case AREA:
                return appliesToArea;
            }
            return true; // should never happen
        }

        /**
         * Recursively sets the flag indicating that this element is relevant for nodes
         */
        void setAppliesToNode() {
            if (!appliesToNode) {
                appliesToNode = true;
                if (parent != null) {
                    parent.setAppliesToNode();
                }
            }
        }

        /**
         * Recursively sets the flag indicating that this element is relevant for nodes
         */
        void setAppliesToWay() {
            if (!appliesToWay) {
                appliesToWay = true;
                if (parent != null) {
                    parent.setAppliesToWay();
                }
            }
        }

        /**
         * Recursively sets the flag indicating that this element is relevant for nodes
         */
        void setAppliesToClosedway() {
            if (!appliesToClosedway) {
                appliesToClosedway = true;
                if (parent != null) {
                    parent.setAppliesToClosedway();
                }
            }
        }

        /**
         * Recursively sets the flag indicating that this element is relevant for relations
         */
        void setAppliesToRelation() {
            if (!appliesToRelation) {
                appliesToRelation = true;
                if (parent != null) {
                    parent.setAppliesToRelation();
                }
            }
        }

        /**
         * Recursively sets the flag indicating that this element is relevant for an area
         */
        void setAppliesToArea() {
            if (!appliesToArea) {
                appliesToArea = true;
                if (parent != null) {
                    parent.setAppliesToArea();
                }
            }
        }

        /**
         * Set the OSM wiki (or other) documentation URL for this PresetElement
         * 
         * @param url the URL to set
         */
        void setMapFeatures(String url) {
            if (url != null) {
                mapFeatures = url;
            }
        }

        /**
         * Get the documentation URL (typically from the OSM wiki) for this PresetELement
         * 
         * @return a Uri
         */
        public Uri getMapFeatures() {
            return Uri.parse(mapFeatures);
        }

        void setNameContext(String context) {
            nameContext = context;
        }

        public boolean isDeprecated() {
            return deprecated;
        }

        public void setDeprecated(boolean deprecated) {
            this.deprecated = deprecated;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        /**
         * Get an object documenting where in the hierarchy this element is.
         * 
         * This is essentially the only unique way of identifying a specific preset
         * 
         * @param root PresetGroup that this is relative to
         * @return and object containing the path elements
         */
        public PresetElementPath getPath(PresetGroup root) {
            for (PresetElement e : root.getElements()) {
                if (e.equals(this)) {
                    PresetElementPath result = new PresetElementPath();
                    result.path.add(e.getName());
                    return result;
                } else {
                    if (e instanceof PresetGroup) {
                        PresetElementPath result = getPath((PresetGroup) e);
                        if (result != null) {
                            result.path.add(0, e.getName());
                            return result;
                        }
                    }
                }
            }
            return null;
        }

        /**
         * @return the iconpath
         */
        public String getIconpath() {
            return iconpath;
        }

        @Override
        public String toString() {
            return name + " " + iconpath + " " + mapiconpath + " " + appliesToWay + " " + appliesToNode + " " + appliesToClosedway + " " + appliesToRelation
                    + " " + appliesToArea;
        }

        /**
         * Serialize the element to XML
         * 
         * @param s the XmlSerializer
         * @throws IllegalArgumentException
         * @throws IllegalStateException
         * @throws IOException
         */
        public abstract void toXml(XmlSerializer s) throws IllegalArgumentException, IllegalStateException, IOException;
    }

    /**
     * Represents a separator in a preset group
     */
    public class PresetSeparator extends PresetElement {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        /**
         * Construct a new separator
         * 
         * @param parent the parent PresetGroup
         */
        public PresetSeparator(PresetGroup parent) {
            super(parent, "", null);
        }

        @Override
        public View getView(Context ctx, PresetClickHandler handler, boolean selected) {
            View v = new View(ctx);
            v.setMinimumHeight(1);
            v.setMinimumWidth(99999); // for WrappingLayout
            // this seems to be necessary to work around
            // https://issuetracker.google.com/issues/37003658
            v.setLayoutParams(new LinearLayout.LayoutParams(99999, 1));
            v.setSaveEnabled(false);
            return v;
        }

        @Override
        public void toXml(XmlSerializer s) throws IllegalArgumentException, IllegalStateException, IOException {
            s.startTag("", SEPARATOR);
            s.endTag("", SEPARATOR);
        }
    }

    /**
     * Represents a preset group, which may contain items, groups and separators
     */
    public class PresetGroup extends PresetElement {

        /**
         * 
         */
        private static final long serialVersionUID = 2L;

        private final int groupIndex;

        /** Elements in this group */
        private ArrayList<PresetElement> elements = new ArrayList<>();

        /**
         * Construct a new PresetGroup
         * 
         * @param parent parent ParentGroup (or null if this is the root group)
         * @param name name of the element or null
         * @param iconpath the icon path (either "http://" URL or "presets/" local image reference) or null
         */
        public PresetGroup(@Nullable PresetGroup parent, @Nullable String name, @Nullable String iconpath) {
            super(parent, name, iconpath);
            groupIndex = allGroups.size();
            allGroups.add(this);
        }

        /**
         * Add a PresetElement to this group setting its parent to this
         * 
         * @param element the PresetElement to add
         */
        public void addElement(PresetElement element) {
            addElement(element, true);
        }

        /**
         * Add a PresetElement to this group
         * 
         * @param element the PresetElement to add
         * @param setParent if true set the elements parent to this
         */
        public void addElement(PresetElement element, boolean setParent) {
            elements.add(element);
            if (setParent) {
                element.setParent(this);
            }
        }

        /**
         * Get the PresetElements in this group
         * 
         * @return a List of PresetElements
         */
        public List<PresetElement> getElements() {
            return elements;
        }

        /**
         * Returns a view showing this group's icon
         * 
         * @param handler the handler handling clicks on the icon
         * @param selected highlight the background if true
         * @return a view/button representing this PresetElement
         */
        @Override
        public View getView(@NonNull Context ctx, @Nullable final PresetClickHandler handler, boolean selected) {
            TextView v = super.getBaseView(ctx, selected);
            v.setTypeface(null, Typeface.BOLD);
            if (handler != null) {
                v.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        handler.onGroupClick(PresetGroup.this);
                    }
                });
                v.setOnLongClickListener(new OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        return handler.onGroupLongClick(PresetGroup.this);
                    }
                });
            }
            v.setBackgroundColor(ContextCompat.getColor(ctx, selected ? R.color.material_deep_teal_200 : R.color.dark_grey));
            v.setTag("G" + this.getGroupIndex());
            return v;
        }

        public int getGroupIndex() {
            return groupIndex;
        }

        /**
         * Get a ScrollView for this PresetGroup
         * 
         * @param ctx Android Context
         * @param handler listeners for click events on the View, in null no listeners
         * @param type ElementType the views are applicable for, if null don't filter
         * @param selectedElement highlight the background if true, if null no selection
         * @return a view showing the content (nodes, subgroups) of this group
         */
        public View getGroupView(@NonNull Context ctx, @Nullable PresetClickHandler handler, @Nullable ElementType type,
                @Nullable PresetElement selectedElement) {
            ScrollView scrollView = new ScrollView(ctx);
            scrollView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            scrollView.setSaveEnabled(false);
            return getGroupView(ctx, scrollView, handler, type, selectedElement);
        }

        /**
         * Add Views for all the PresetElements in this group to a ScrollView
         * 
         * @param ctx Android Context
         * @param scrollView the ScrollView to add the PresetElement Views to
         * @param handler listeners for click events on the View, in null no listeners
         * @param type ElementType the views are applicable for, if null don't filter
         * @param selectedElement highlight the background if true, if null no selection
         * @return the supplied ScrollView
         */
        public View getGroupView(@NonNull Context ctx, @NonNull ScrollView scrollView, @Nullable PresetClickHandler handler, @Nullable ElementType type,
                @Nullable PresetElement selectedElement) {
            scrollView.removeAllViews();
            WrappingLayout wrappingLayout = new WrappingLayout(ctx);
            wrappingLayout.setSaveEnabled(false);
            float density = ctx.getResources().getDisplayMetrics().density;
            // wrappingLayout.setBackgroundColor(ctx.getResources().getColor(android.R.color.white));
            wrappingLayout.setBackgroundColor(ContextCompat.getColor(ctx, android.R.color.transparent)); // make
                                                                                                         // transparent
            wrappingLayout.setHorizontalSpacing((int) (SPACING * density));
            wrappingLayout.setVerticalSpacing((int) (SPACING * density));
            List<PresetElement> filteredElements = type == null ? elements : filterElements(elements, type);
            List<View> childViews = new ArrayList<>();
            for (PresetElement element : filteredElements) {
                View v = element.getView(ctx, handler, element.equals(selectedElement));
                if (v.getLayoutParams() == null) {
                    Log.e(DEBUG_TAG, "layoutparams null " + element.getName());
                }
                childViews.add(v);
            }
            wrappingLayout.setWrappedChildren(childViews);
            scrollView.addView(wrappingLayout);
            return scrollView;
        }

        @Override
        public void toXml(XmlSerializer s) throws IllegalArgumentException, IllegalStateException, IOException {
            s.startTag("", GROUP);
            s.attribute("", NAME, getName());
            String iconPath = getIconpath();
            if (iconPath != null) {
                s.attribute("", ICON, getIconpath());
            }
            for (PresetElement e : elements) {
                e.toXml(s);
            }
            s.endTag("", GROUP);
        }
    }

    /** Represents a preset item (e.g. "footpath", "grocery store") */
    public class PresetItem extends PresetElement {

        /**
         * 
         */
        private static final long serialVersionUID = 12L;

        /** "fixed" tags, i.e. the ones that have a fixed key-value pair */
        private LinkedHashMap<String, StringWithDescription> fixedTags = new LinkedHashMap<>();

        /**
         * Tags that are not in the optional section, but do not have a fixed key-value-pair. The map key provides the
         * key, while the map value (String[]) provides the possible values.
         */
        private LinkedHashMap<String, StringWithDescription[]> recommendedTags = new LinkedHashMap<>();

        /**
         * Tags that are in the optional section. The map key provides the key, while the map value (String[]) provides
         * the possible values.
         */
        private LinkedHashMap<String, StringWithDescription[]> optionalTags = new LinkedHashMap<>();

        /**
         * Hints to be displayed in a suitable form
         */
        private HashMap<String, String> hints = new HashMap<>();

        /**
         * Default values
         */
        private HashMap<String, String> defaults = null;

        /**
         * Non standard on values
         */
        private HashMap<String, String> onValue = null;

        /**
         * Roles
         */
        private LinkedList<StringWithDescription> roles = null;

        /**
         * Linked names of presets
         */
        private LinkedList<String> linkedPresetNames = null;

        /**
         * Sort values or not
         */
        private HashMap<String, Boolean> sort = null;

        /**
         * Key to key type
         */
        private HashMap<String, PresetKeyType> keyType = new HashMap<>();

        /**
         * Key to match properties
         */
        private HashMap<String, MatchType> matchType = null;

        /**
         * Key to combo and multiselect delimiters
         */
        private HashMap<String, String> delimiters = null;

        /**
         * Key to combo and multiselect editable property
         */
        private HashMap<String, Boolean> editable = null;

        /**
         * Add combo and multiselect values to search index
         * 
         * Can be removed after values have been added
         */
        private HashMap<String, Boolean> valuesSearchable = null;

        /**
         * Translation contexts
         */
        private HashMap<String, String> textContext  = null;
        private HashMap<String, String> valueContext = null;

        /**
         * Scripts for pre-filling text fields
         */
        private HashMap<String, String> javascript = null;

        /**
         * Key can have i18n variants (name, name:de, name:ru etc)
         */
        private HashMap<String, Boolean> i18n = null;

        /**
         * Key to value type
         */
        private HashMap<String, ValueType> valueType = null;

        /**
         * true if a chunk
         */
        private boolean chunk = false;

        private final int itemIndex;

        /**
         * Construct a new PresetItem
         * 
         * @param parent parent group (or null if this is the root group)
         * @param name name of the element or null
         * @param iconpath the icon path (either "http://" URL or "presets/" local image reference) or null
         * @param types comma separated list of types of OSM elements this applies to or null for all
         */
        public PresetItem(@Nullable PresetGroup parent, @Nullable String name, @Nullable String iconpath, @Nullable String types) {
            super(parent, name, iconpath);
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
                    if (Node.NAME.equals(type)) {
                        setAppliesToNode();
                    } else if (Way.NAME.equals(type)) {
                        setAppliesToWay();
                    } else if (CLOSEDWAY.equals(type)) {
                        setAppliesToClosedway(); // FIXME don't add if it really an area
                    } else if (MULTIPOLYGON.equals(type)) {
                        setAppliesToArea();
                    } else if (AREA.equals(type)) {
                        setAppliesToArea(); //
                    } else if (Relation.NAME.equals(type)) {
                        setAppliesToRelation();
                    }
                }
            }
            itemIndex = allItems.size();
            allItems.add(this);
        }

        /**
         * Construct a new PresetItem in this preset from an existing one adding the necessary bits to the indices
         * 
         * @param group PresetGroup this should be added, null if none
         * @param item the PresetItem to copy
         */
        public PresetItem(@Nullable PresetGroup group, @NonNull PresetItem item) {
            super(group, item);
            this.fixedTags = item.fixedTags;
            this.recommendedTags = item.recommendedTags;
            this.optionalTags = item.optionalTags;
            this.hints = item.hints;
            this.defaults = item.defaults;
            this.onValue = item.onValue;
            this.roles = item.roles;
            this.linkedPresetNames = item.linkedPresetNames;
            this.sort = item.sort;
            this.keyType = item.keyType;
            this.matchType = item.matchType;
            this.delimiters = item.delimiters;
            this.editable = item.editable;
            this.valuesSearchable = item.valuesSearchable;
            this.textContext = item.textContext;
            this.valueContext = item.valueContext;
            this.javascript = item.javascript;
            this.i18n = item.i18n;
            this.valueType = item.valueType;

            if (!chunk) {
                for (Entry<String, StringWithDescription> e : getFixedTags().entrySet()) {
                    StringWithDescription v = e.getValue();
                    String key = e.getKey();
                    String value = "";
                    if (v != null && v.getValue() != null) {
                        value = v.getValue();
                    }
                    tagItems.add(key + "\t" + value, this);
                    addToAutosuggest(key, v);
                }
                for (Entry<String, StringWithDescription[]> e : getRecommendedTags().entrySet()) {
                    StringWithDescription[] values = e.getValue();
                    String key = e.getKey();
                    tagItems.add(key + "\t", this);
                    for (StringWithDescription swd : values) {
                        tagItems.add(e.getKey() + "\t" + swd.getValue(), this);
                    }
                    addToAutosuggest(key, values);
                }
            }

            itemIndex = allItems.size();
            allItems.add(this);
        }

        /**
         * Add the values to the autosuggest maps for the key
         * 
         * @param key the key
         * @param values array of the values
         */
        private void addToAutosuggest(String key, StringWithDescription[] values) {
            if (appliesTo(ElementType.NODE)) {
                autosuggestNodes.add(key, values);
            }
            if (appliesTo(ElementType.WAY)) {
                autosuggestWays.add(key, values);
            }
            if (appliesTo(ElementType.CLOSEDWAY)) {
                autosuggestClosedways.add(key, values);
            }
            if (appliesTo(ElementType.RELATION)) {
                autosuggestRelations.add(key, values);
            }
            if (appliesTo(ElementType.AREA)) {
                autosuggestAreas.add(key, values);
            }
        }

        /**
         * Add the value to the autosuggest maps for the key
         * 
         * @param key the key
         * @param value the value
         */
        private void addToAutosuggest(String key, StringWithDescription value) {
            if (appliesTo(ElementType.NODE)) {
                autosuggestNodes.add(key, value);
            }
            if (appliesTo(ElementType.WAY)) {
                autosuggestWays.add(key, value);
            }
            if (appliesTo(ElementType.CLOSEDWAY)) {
                autosuggestClosedways.add(key, value);
            }
            if (appliesTo(ElementType.RELATION)) {
                autosuggestRelations.add(key, value);
            }
            if (appliesTo(ElementType.AREA)) {
                autosuggestAreas.add(key, value);
            }
        }

        /**
         * build the search index
         */
        void buildSearchIndex() {
            addToSearchIndex(name);
            if (parent != null) {
                String parentName = parent.getName();
                if (parentName != null && parentName.length() > 0) {
                    addToSearchIndex(parentName);
                }
            }
            for (Entry<String, StringWithDescription> entry : fixedTags.entrySet()) {
                StringWithDescription v = entry.getValue();
                addToSearchIndex(entry.getKey());
                String value = v.getValue();
                addToSearchIndex(value);
                addToSearchIndex(v.getDescription());
                // support subtypes
                StringWithDescription[] subtypes = recommendedTags.get(value);
                if (subtypes != null) {
                    for (StringWithDescription subtype : subtypes) {
                        addToSearchIndex(subtype.getValue());
                        addToSearchIndex(subtype.getDescription());
                    }
                    if (valuesSearchable != null) {
                        valuesSearchable.remove(value);
                    }
                }
            }
            for (Entry<String, StringWithDescription[]> entry : recommendedTags.entrySet()) {
                if (valuesSearchable != null && getValuesSearchable(entry.getKey())) {
                    for (StringWithDescription value : entry.getValue()) {
                        addToSearchIndex(value.getValue());
                        addToSearchIndex(value.getDescription());
                    }
                }
            }
            // don't need it anymore
            valuesSearchable = null;
        }

        /**
         * Add a name, any translation and the individual words to the index. Currently we assume that all words are
         * significant
         * 
         * @param term search key to add
         */
        void addToSearchIndex(String term) {
            // search support
            if (term != null) {
                String normalizedName = SearchIndexUtils.normalize(term);
                searchIndex.add(normalizedName, this);
                String[] words = normalizedName.split(" ");
                if (words.length > 1) {
                    for (String w : words) {
                        searchIndex.add(w, this);
                    }
                }
                if (po != null) { // and any translation
                    String normalizedTranslatedName = SearchIndexUtils.normalize(po.t(term));
                    translatedSearchIndex.add(normalizedTranslatedName, this);
                    String[] translastedWords = normalizedName.split(" ");
                    if (translastedWords.length > 1) {
                        for (String w : translastedWords) {
                            translatedSearchIndex.add(w, this);
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
         * @param text description of the tag
         */
        public void addTag(final String key, final PresetKeyType type, @Nullable String value, @Nullable String text) {
            if (key == null) {
                throw new NullPointerException("null key not supported");
            }
            if (value == null) {
                value = "";
            }
            if (text != null && po != null) {
                text = po.t(text);
            }
            fixedTags.put(key, new StringWithDescription(value, text));
            if (!chunk) {
                tagItems.add(key + "\t" + value, this);
                addToAutosuggest(key, value.length() > 0 ? new StringWithDescription(value, text) : null);
            }
            // Log.d(DEBUG_TAG,name + " key " + key + " type " + type);
            keyType.put(key, type);
        }

        /**
         * Adds a recommended or optional tag to the item and populates autosuggest.
         * 
         * @param optional true if optional, false if recommended
         * @param key key name of the tag
         * @param type type of preset field
         * @param value value string from the XML (comma-separated list if more than one possible values)
         */
        public void addTag(boolean optional, String key, PresetKeyType type, String value) {
            addTag(optional, key, type, value, null, null, COMBO_DELIMITER, null);
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
         * @param valuesContext the translation context for values
         */
        public void addTag(boolean optional, String key, PresetKeyType type, String value, String displayValue, String shortDescriptions,
                final String delimiter, String valuesContext) {
            String[] valueArray = (value == null) ? new String[0] : value.split(Pattern.quote(delimiter));
            String[] displayValueArray = (displayValue == null) ? new String[0] : displayValue.split(Pattern.quote(delimiter));
            String[] shortDescriptionArray = (shortDescriptions == null) ? new String[0] : shortDescriptions.split(Pattern.quote(delimiter));
            StringWithDescription[] valuesWithDesc = new StringWithDescription[valueArray.length];
            boolean useDisplayValues = valueArray.length == displayValueArray.length;
            boolean useShortDescriptions = !useDisplayValues && valueArray.length == shortDescriptionArray.length;
            for (int i = 0; i < valueArray.length; i++) {
                String description = null;
                if (useDisplayValues) {
                    description = (po != null && displayValueArray[i] != null)
                            ? (valuesContext != null ? po.t(valuesContext, displayValueArray[i]) : po.t(displayValueArray[i])) : displayValueArray[i];
                } else if (useShortDescriptions) {
                    description = (po != null && shortDescriptionArray[i] != null)
                            ? (valuesContext != null ? po.t(valuesContext, shortDescriptionArray[i]) : po.t(shortDescriptionArray[i]))
                            : shortDescriptionArray[i];
                }
                valuesWithDesc[i] = new StringWithDescription(valueArray[i], description);
            }
            addTag(optional, key, type, valuesWithDesc, delimiter);
        }

        /**
         * Adds a recommended or optional tag to the item and populates autosuggest
         * 
         * @param optional true if optional, false if recommended
         * @param key key name of the tag
         * @param type type of preset field
         * @param valueCollection Collection with the values
         * @param delimiter the delimiter if more than one value is present
         */
        public void addTag(boolean optional, String key, PresetKeyType type, Collection<StringWithDescription> valueCollection, final String delimiter) {
            addTag(optional, key, type, valueCollection.toArray(new StringWithDescription[valueCollection.size()]), delimiter);
        }

        /**
         * Adds a recommended or optional tag to the item and populates autosuggest
         * 
         * @param optional true if optional, false if recommended
         * @param key key name of the tag
         * @param type type of preset field
         * @param valueArray array with the values
         * @param delimiter the delimiter if more than one value is present
         */
        public void addTag(boolean optional, String key, PresetKeyType type, StringWithDescription[] valueArray, final String delimiter) {
            if (!chunk) {
                tagItems.add(key + "\t", this);
                if (valueArray != null && valueArray.length > 0) {
                    for (StringWithDescription v : valueArray) {
                        tagItems.add(key + "\t" + v.getValue(), this);
                    }
                }
                addToAutosuggest(key, valueArray);
            }
            keyType.put(key, type);

            (optional ? optionalTags : recommendedTags).put(key, valueArray);

            // only save delimiter if not default
            if ((type == PresetKeyType.MULTISELECT && !MULTISELECT_DELIMITER.equals(delimiter))
                    || (type == PresetKeyType.COMBO && !COMBO_DELIMITER.equals(delimiter))) {
                addDelimiter(key, delimiter);
            }
        }

        public void addRole(final StringWithDescription value) {
            if (roles == null) {
                roles = new LinkedList<>();
            }
            roles.add(value);
        }

        public void addAllRoles(LinkedList<StringWithDescription> newRoles) {
            if (roles == null) {
                roles = newRoles; // doesn't matter if newRoles is null
            } else if (newRoles != null) {
                roles.addAll(newRoles);
            }
        }

        public List<StringWithDescription> getRoles() {
            return roles != null ? Collections.unmodifiableList(roles) : null;
        }

        /**
         * Save hint for the tag
         * 
         * @param key
         * @param hint
         */
        public void addHint(String key, String hint) {
            hints.put(key, hint);
        }

        /**
         * Return, potentially translated, "text" field from preset
         * 
         * @param key
         * @return
         */
        public String getHint(String key) {
            if (po != null) {
                return po.t(hints.get(key));
            }
            return hints.get(key);
        }

        /**
         * Save default for the tag
         * 
         * @param key
         * @param defaultValue
         */
        public void addDefault(String key, String defaultValue) {
            if (defaults == null) {
                defaults = new HashMap<>();
            }
            defaults.put(key, defaultValue);
        }

        public void addAllDefaults(HashMap<String, String> newDefaults) {
            if (defaults == null) {
                defaults = newDefaults; // doesn't matter if newDefaults is null
            } else if (newDefaults != null) {
                defaults.putAll(newDefaults);
            }
        }

        public String getDefault(String key) {
            return defaults != null ? defaults.get(key) : null;
        }

        /**
         * Save non-standard values for the tag
         * 
         * @param key
         * @param on
         */
        public void addOnValue(String key, String on) {
            if (onValue == null) {
                onValue = new HashMap<>();
            }
            onValue.put(key, on);
        }

        public void addAllOnValues(HashMap<String, String> newOnValues) {
            if (onValue == null) {
                onValue = newOnValues; // doesn't matter if newOnValues is null
            } else if (newOnValues != null) {
                onValue.putAll(newOnValues);
            }
        }

        /**
         * Get the value that should be used for a checked check box
         * 
         * @param key the key for the checkbox
         * @return either default value or what has been set in the preset
         */
        public String getOnValue(String key) {
            if (onValue != null) {
                return onValue.get(key) != null ? onValue.get(key) : YES;
            }
            return YES;
        }

        /**
         * Save non-standard values for the value delimiter
         * 
         * @param key key this delimiter is used for
         * @param delimiter the delimiter
         */
        public void addDelimiter(String key, String delimiter) {
            if (delimiters == null) {
                delimiters = new HashMap<>();
            }
            delimiters.put(key, delimiter);
        }

        public void addAllDelimiters(HashMap<String, String> newDelimiters) {
            if (delimiters == null) {
                delimiters = newDelimiters; // doesn't matter if newOnValues is null
            } else if (newDelimiters != null) {
                delimiters.putAll(newDelimiters);
            }
        }

        public char getDelimiter(String key) {
            return (delimiters != null && delimiters.get(key) != null ? delimiters.get(key)
                    : (getKeyType(key) == PresetKeyType.MULTISELECT ? MULTISELECT_DELIMITER : COMBO_DELIMITER)).charAt(0);
        }

        public void setMatchType(String key, String match) {
            if (matchType == null) {
                matchType = new HashMap<>();
            }
            MatchType type = null;
            if ("none".equals(match)) {
                type = MatchType.NONE;
            } else if ("key".equals(match)) {
                type = MatchType.KEY;
            } else if ("key!".equals(match)) {
                type = MatchType.KEY_NEG;
            } else if ("keyvalue".equals(match)) {
                type = MatchType.KEY_VALUE;
            } else if ("keyvalue!".equals(match)) {
                type = MatchType.KEY_VALUE_NEG;
            }
            matchType.put(key, type);
        }

        public void setAllMatchTypes(HashMap<String, MatchType> newMatchTypes) {
            if (matchType == null) {
                matchType = newMatchTypes; // doesn't matter if newMatchTypes is null
            } else if (newMatchTypes != null) {
                matchType.putAll(newMatchTypes);
            }
        }

        @Nullable
        public MatchType getMatchType(String key) {
            return matchType != null ? matchType.get(key) : null;
        }

        /**
         * Set the ValueType for the key
         * 
         * @param key the key
         * @param type a String for the ValueType
         */
        public void setValueType(@NonNull String key, @Nullable String type) {
            if (valueType == null) {
                valueType = new HashMap<>();
            }
            valueType.put(key, ValueType.fromString(type));
        }

        /**
         * Add a key - ValueType map
         * 
         * @param newValueTypes the additional map
         */
        public void setAllValueTypes(@Nullable HashMap<String, ValueType> newValueTypes) {
            if (valueType == null) {
                valueType = newValueTypes; // doesn't matter if newMatchTypes is null
            } else if (newValueTypes != null) {
                valueType.putAll(newValueTypes);
            }
        }

        /**
         * Get the ValueType for this key
         * 
         * @param key the key to check
         * @return the ValueType of null if none set
         */
        @Nullable
        public ValueType getValueType(String key) {
            return valueType != null ? valueType.get(key) : null;
        }

        /**
         * Record if the values from the combo or multiselect values should be added to the search index
         * 
         * @param key combo/multiselect key
         * @param search if true add to index
         */
        public void setValuesSearchable(String key, boolean search) {
            if (valuesSearchable == null) {
                valuesSearchable = new HashMap<>();
            }
            valuesSearchable.put(key, search);
        }

        /**
         * Add the values in the argument to the list of combo or multiselect that should be added to the search index
         * 
         * @param newValuesSearchable HashMap of keys and flags
         */
        public void setAllValuesSearchable(HashMap<String, Boolean> newValuesSearchable) {
            if (valuesSearchable == null) {
                valuesSearchable = newValuesSearchable;
            } else if (newValuesSearchable != null) {
                valuesSearchable.putAll(newValuesSearchable);
            }
        }

        /**
         * Check if values for this key should be added to the search index
         * 
         * @param key key to check
         * @return true if values should be added to the search index
         */
        public boolean getValuesSearchable(String key) {
            if (valuesSearchable != null) {
                Boolean result = valuesSearchable.get(key);
                return result != null ? result : false;
            }
            return false;
        }

        public void addLinkedPresetName(String presetName) {
            if (linkedPresetNames == null) {
                linkedPresetNames = new LinkedList<>();
            }
            linkedPresetNames.add(presetName);
        }

        public void addAllLinkedPresetNames(LinkedList<String> newLinkedPresetNames) {
            if (linkedPresetNames == null) {
                linkedPresetNames = newLinkedPresetNames; // doesn't matter if newLinkedPresetNames is null
            } else if (newLinkedPresetNames != null) {
                linkedPresetNames.addAll(newLinkedPresetNames);
            }
        }

        @Nullable
        public List<String> getLinkedPresetNames() {
            return linkedPresetNames;
        }

        /**
         * Returns a list of linked preset items
         * 
         * @param noPrimary if true only items will be returned that doen't correspond to primary OSM objects
         * @return list of PresetItems
         */
        @NonNull
        public List<PresetItem> getLinkedPresets(boolean noPrimary) {
            ArrayList<PresetItem> result = new ArrayList<>();
            Log.e(DEBUG_TAG, "Linked presets for " + getName());
            if (linkedPresetNames != null) {
                linkedLoop: for (String n : linkedPresetNames) {
                    Integer index = getItemIndexByName(n); // FIXME this involves a sequential search
                    if (index != null) {
                        PresetItem candidateItem = allItems.get(index);
                        if (noPrimary) { // remove primary objects
                            Set<String> linkedPresetTags = candidateItem.getFixedTags().keySet();
                            if (linkedPresetTags.isEmpty()) {
                                linkedPresetTags = candidateItem.getRecommendedTags().keySet();
                            }
                            for (String k : linkedPresetTags) {
                                if (Tags.IMPORTANT_TAGS.contains(k) || isObjectKey(k)) {
                                    continue linkedLoop;
                                }
                            }
                        }
                        result.add(candidateItem);
                    } else {
                        Log.e(DEBUG_TAG, "Couldn't find linked preset " + n);
                    }
                }
            }
            return result;
        }

        public void setSort(String key, boolean sortIt) {
            if (sort == null) {
                sort = new HashMap<>();
            }
            sort.put(key, sortIt);
        }

        public void setAllSort(HashMap<String, Boolean> newSort) {
            if (sort == null) {
                sort = newSort; // doesn't matter if newSort is null
            } else if (newSort != null) {
                sort.putAll(newSort);
            }
        }

        public boolean sortIt(String key) {
            return (sort == null || sort.get(key) == null) ? true : sort.get(key);
        }

        public void setJavaScript(String key, String script) {
            if (javascript == null) {
                javascript = new HashMap<>();
            }
            javascript.put(key, script);
        }

        public void setAllJavaScript(HashMap<String, String> newJavaScript) {
            if (javascript == null) {
                javascript = newJavaScript; // doesn't matter if newSort is null
            } else if (newJavaScript != null) {
                javascript.putAll(newJavaScript);
            }
        }

        /**
         * Get any JS code associated with the key
         * 
         * @param key key we want to retrieve the code for
         * @return JS code or null if none present
         */
        @Nullable
        public String getJavaScript(@NonNull String key) {
            return javascript == null ? null : javascript.get(key);
        }

        public void setI18n(@NonNull String key) {
            if (i18n == null) {
                i18n = new HashMap<>();
            }
            i18n.put(key, true);
        }

        public void addAllI18n(HashMap<String, Boolean> newI18n) {
            if (i18n == null) {
                i18n = newI18n; // doesn't matter if newI18n is null
            } else if (newI18n != null) {
                i18n.putAll(newI18n);
            }
        }

        /**
         * Check if the key supports i18n variants
         * 
         * @param key key we want to check
         * @return true if the key supports i18n variants
         */
        public boolean supportsI18n(String key) {
            return i18n != null && true == i18n.get(key);
        }

        /**
         * Get all keys in the item that support i18n
         * 
         * @return a Set with the keys or null
         */
        @Nullable
        public Set<String> getI18nKeys() {
            return i18n != null ? i18n.keySet() : null;
        }

        public void setEditable(@NonNull String key, boolean isEditable) {
            if (editable == null) {
                editable = new HashMap<>();
            }
            editable.put(key, isEditable);
        }

        public void setAllEditable(HashMap<String, Boolean> newEditable) {
            if (editable == null) {
                editable = newEditable; // doesn't matter if newSort is null
            } else if (newEditable != null) {
                editable.putAll(newEditable);
            }
        }

        /**
         * Check is the combo or multiselect should be editable
         * 
         * NOTE: contrary to the definition in JOSM the default is false/no
         * 
         * @param key key we want to check
         * @return true if the user can add values
         */
        public boolean isEditable(String key) {
            return (editable == null || editable.get(key) == null) ? false : editable.get(key);
        }

        public void setTextContext(String key, String textContext) {
            if (this.textContext == null) {
                this.textContext = new HashMap<>();
            }
            this.textContext.put(key, textContext);
        }

        @Nullable
        public String getTextContext(String key) {
            return textContext != null ? textContext.get(key) : null;
        }

        public void setValueContext(String key, String valueContext) {
            if (this.valueContext == null) {
                this.valueContext = new HashMap<>();
            }
            this.valueContext.put(key, valueContext);
        }

        @Nullable
        public String getValueContext(String key) {
            return valueContext != null ? valueContext.get(key) : null;
        }

        /**
         * @return the fixed tags belonging to this item (unmodifiable)
         */
        public Map<String, StringWithDescription> getFixedTags() {
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

        public boolean isFixedTag(String key) {
            return fixedTags.containsKey(key);
        }

        public boolean isRecommendedTag(String key) {
            return recommendedTags.containsKey(key);
        }

        public boolean isOptionalTag(String key) {
            return optionalTags.containsKey(key);
        }

        public Map<String, StringWithDescription[]> getRecommendedTags() {
            return Collections.unmodifiableMap(recommendedTags);
        }

        public Map<String, StringWithDescription[]> getOptionalTags() {
            return Collections.unmodifiableMap(optionalTags);
        }

        /**
         * Return a ist of the values suitable for autocomplete, note vales for fixed tags are not returned
         * 
         * @param key key to get values for
         * @return Collection of StringWithDescription objects
         */
        @NonNull
        public Collection<StringWithDescription> getAutocompleteValues(@NonNull String key) {
            Collection<StringWithDescription> result = new LinkedHashSet<>();
            if (recommendedTags.containsKey(key)) {
                result.addAll(Arrays.asList(recommendedTags.get(key)));
            } else if (optionalTags.containsKey(key)) {
                result.addAll(Arrays.asList(optionalTags.get(key)));
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
         * @return the selection type for this key
         */
        public PresetKeyType getKeyType(String key) {
            PresetKeyType result = keyType.get(key);
            if (result == null) {
                return PresetKeyType.TEXT;
            }
            return result;
        }

        /**
         * Checks the fixed tags belonging to this item exist in the given tags
         * 
         * Uses the match value to control actual behavior
         * 
         * @param tagSet Map containing tags to compare against this preset item
         * @return true if the tagSet matches
         */
        public boolean matches(Map<String, String> tagSet) {
            if (name.equals("Addresses")) {
                Log.d(DEBUG_TAG, "matching addresses fixed");
            }
            int matchCount = 0;
            for (Entry<String, StringWithDescription> tag : fixedTags.entrySet()) { // for each own tag
                String key = tag.getKey();
                MatchType type = getMatchType(key);
                if (type == MatchType.NONE) {
                    continue;
                }
                String value = tagSet.get(key);
                if (value == null) { // key doesn't match
                    if ((type == null || type == MatchType.KEY_VALUE_NEG || type == MatchType.KEY_NEG) && value == null) { // key
                                                                                                                           // doesn't
                                                                                                                           // exist
                        matchCount--;
                    }
                    continue;
                }
                if (type == MatchType.KEY) { // key match is all we require
                    matchCount++;
                    continue;
                }
                if (tag.getValue().equals(value)) { // key and value match
                    matchCount++;
                } else if (type == null || type == MatchType.KEY_VALUE_NEG) { // value doesn't match
                    matchCount--;
                }
            }
            return matchCount > 0;
        }

        /**
         * Returns the number of matches between the list of recommended tags (really a misnomer) and the provided tags
         * 
         * Uses the match value to control actual behavior
         * 
         * @param tagMap Map containing the tags
         * @return number of matches
         */
        public int matchesRecommended(Map<String, String> tagMap) {
            int matches = 0;
            for (Entry<String, StringWithDescription[]> tag : recommendedTags.entrySet()) { // for each own tag
                String key = tag.getKey();
                if (tagMap.containsKey(key)) { // key could have null value in the set
                    // value not empty
                    if (getMatchType(key) == MatchType.NONE) {
                        // don't count this
                        break;
                    }
                    if (getMatchType(key) == MatchType.KEY) {
                        matches++;
                        break;
                    }
                    String otherTagValue = tagMap.get(key);
                    for (StringWithDescription v : tag.getValue()) {
                        if (v.equals(otherTagValue)) {
                            matches++;
                            break;
                        }
                    }
                }
            }
            return matches;
        }

        @Override
        public View getView(Context ctx, final PresetClickHandler handler, boolean selected) {
            View v = super.getBaseView(ctx, selected);
            if (handler != null) {
                v.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        handler.onItemClick(PresetItem.this);
                    }

                });
                v.setOnLongClickListener(new OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        return handler.onItemLongClick(PresetItem.this);
                    }
                });
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
        public boolean hasKey(String key) {
            return hasKey(key, true);
        }

        /**
         * Return true if the key is contained in this preset
         * 
         * @param key key to look for
         * @param checkOptional check in optional tags too
         * @return true if the key is present in any category (fixed, recommended, and optional if checkOptional is
         *         true)
         */
        public boolean hasKey(String key, boolean checkOptional) {
            return fixedTags.containsKey(key) || recommendedTags.containsKey(key) || (checkOptional && optionalTags.containsKey(key));
        }

        /**
         * Return true if the key and value is contained in this preset taking match attribute in to account
         * 
         * Note mathe="none" is handled the same as "key" in this method
         * 
         * @param key key to look for
         * @param value value to look for
         * @return true if the key- value combination is present in any category (fixed, recommended, and optional)
         */
        public boolean hasKeyValue(String key, String value) {

            StringWithDescription swd = fixedTags.get(key);
            if (swd != null) {
                if ("".equals(value) || swd.getValue() == null || swd.equals(value) || "".equals(swd.getValue())) {
                    return true;
                }
            }

            MatchType type = getMatchType(key);
            PresetKeyType keyType = getKeyType(key);

            if (recommendedTags.containsKey(key)) {
                if (type == MatchType.KEY || type == MatchType.NONE || keyType == PresetKeyType.MULTISELECT) { // MULTISELECT
                                                                                                               // always
                                                                                                               // editable
                    return true;
                }
                StringWithDescription[] swdArray = recommendedTags.get(key);
                if (swdArray != null && swdArray.length > 0) {
                    for (StringWithDescription v : swdArray) {
                        if ("".equals(value) || v.getValue() == null || v.equals(value) || "".equals(v.getValue())) {
                            return true;
                        }
                    }
                } else {
                    return true;
                }
            }

            if (optionalTags.containsKey(key)) {
                if (type == MatchType.KEY || type == MatchType.NONE || keyType == PresetKeyType.MULTISELECT) { // MULTISELECT
                                                                                                               // always
                                                                                                               // editable
                    return true;
                }
                StringWithDescription[] swdArray = optionalTags.get(key);
                if (swdArray != null && swdArray.length > 0) {
                    for (StringWithDescription v : swdArray) {
                        if ("".equals(value) || v.getValue() == null || v.equals(value) || "".equals(v.getValue())) {
                            return true;
                        }
                    }
                } else {
                    return true;
                }
            }
            return false;
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
            StringBuilder tagStrings = new StringBuilder(" required: ");
            for (Entry<String, StringWithDescription> entry : fixedTags.entrySet()) {
                tagStrings.append(" ");
                tagStrings.append(entry.getKey());
                tagStrings.append("=");
                tagStrings.append(entry.getValue());
            }
            tagStrings.append(" recommended: ");
            for (Entry<String, StringWithDescription[]> entry : recommendedTags.entrySet()) {
                tagStrings.append(" ");
                tagStrings.append(entry.getKey());
                tagStrings.append("=");
                for (StringWithDescription v : entry.getValue()) {
                    tagStrings.append(" ");
                    tagStrings.append(v.getValue());
                }
            }
            tagStrings.append(" optional: ");
            for (Entry<String, StringWithDescription[]> entry : optionalTags.entrySet()) {
                tagStrings.append(" ");
                tagStrings.append(entry.getKey());
                tagStrings.append("=");
                for (StringWithDescription v : entry.getValue()) {
                    tagStrings.append(" ");
                    tagStrings.append(v.getValue());
                }
            }
            return super.toString() + tagStrings.toString();
        }

        void setChunk() {
            chunk = true;
        }

        boolean isChunk() {
            return chunk;
        }

        /**
         * Create a JSON representation of this item
         * 
         * @return JSON format string
         */
        public String toJSON() {
            StringBuilder presetNameBuilder = new StringBuilder(name);
            PresetElement p = getParent();
            while (p != null && p != rootGroup && !"".equals(p.getName())) {
                presetNameBuilder.insert(0, '/');
                presetNameBuilder.insert(0, p.getName());
                p = p.getParent();
            }
            String presetName = presetNameBuilder.toString();
            StringBuilder jsonString = new StringBuilder();
            for (Entry<String, StringWithDescription> entry : fixedTags.entrySet()) {
                if (jsonString.length() != 0) {
                    jsonString.append(",\n");
                }
                jsonString.append(tagToJSON(presetName, entry.getKey(), entry.getValue().getValue()));
            }
            for (Entry<String, StringWithDescription[]> entry : recommendedTags.entrySet()) {
                // check match attribute
                String k = entry.getKey();
                MatchType match = getMatchType(k);
                PresetKeyType type = getKeyType(k);
                if (isEditable(k) || type == PresetKeyType.TEXT || (match != null && match != MatchType.KEY_VALUE)) {
                    if (jsonString.length() != 0) {
                        jsonString.append(",\n");
                    }
                    jsonString.append(tagToJSON(presetName, k, null));
                }
                if (!isEditable(k) && type != PresetKeyType.TEXT && (match == null || match == MatchType.KEY_VALUE || match == MatchType.KEY)) {
                    for (StringWithDescription v : entry.getValue()) {
                        if (jsonString.length() != 0) {
                            jsonString.append(",\n");
                        }
                        jsonString.append(tagToJSON(presetName, k, v.getValue()));
                    }
                }
            }
            for (Entry<String, StringWithDescription[]> entry : optionalTags.entrySet()) {
                // check match attribute
                String k = entry.getKey();
                MatchType match = getMatchType(k);
                PresetKeyType type = getKeyType(k);
                if (isEditable(k) || type == PresetKeyType.TEXT || (match != null && match != MatchType.KEY_VALUE)) {
                    if (jsonString.length() != 0) {
                        jsonString.append(",\n");
                    }
                    jsonString.append(tagToJSON(presetName, k, null));
                }
                if (!isEditable(k) && type != PresetKeyType.TEXT && (match == null || match == MatchType.KEY_VALUE || match == MatchType.KEY)) {
                    for (StringWithDescription v : entry.getValue()) {
                        if (jsonString.length() != 0) {
                            jsonString.append(",\n");
                        }
                        jsonString.append(tagToJSON(presetName, k, v.getValue()));
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
        private String tagToJSON(@NonNull String presetName, @NonNull String key, @Nullable String value) {
            StringBuilder result = new StringBuilder(
                    "{\"description\":\"" + presetName + "\",\"key\": \"" + key + "\"" + (value == null ? "" : ",\"value\": \"" + value + "\""));
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
            Util.groupI18nKeys(i18nKeys, recommendedTags);
            Util.groupI18nKeys(i18nKeys, optionalTags);
        }

        @Override
        public void toXml(XmlSerializer s) throws IllegalArgumentException, IllegalStateException, IOException {
            s.startTag("", chunk ? CHUNK : ITEM);
            s.attribute("", NAME, name);
            String iconPath = getIconpath();
            if (iconPath != null) {
                s.attribute("", ICON, getIconpath());
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
                builder.append(CLOSEDWAY);
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
                builder.append(MULTIPOLYGON);
            }
            s.attribute("", TYPE, builder.toString());
            for (Entry<String, StringWithDescription> entry : fixedTags.entrySet()) {
                s.startTag("", KEY_ATTR);
                s.attribute("", KEY_ATTR, entry.getKey());
                StringWithDescription v = entry.getValue();
                s.attribute("", VALUE, v.getValue());
                String description = v.getDescription();
                if (description != null && !"".equals(description)) {
                    s.attribute("", TEXT, description);
                }
                s.endTag("", KEY_ATTR);
            }
            fieldsToXml(s, recommendedTags);
            if (optionalTags != null && !optionalTags.isEmpty()) {
                s.startTag("", OPTIONAL);
                fieldsToXml(s, optionalTags);
                s.endTag("", OPTIONAL);
            }
            s.endTag("", chunk ? CHUNK : ITEM);
        }

        /**
         * Output the preset files to XML
         * 
         * @param fields a map containing the fields
         * @param s the serializer
         * @throws IOException
         */
        private void fieldsToXml(XmlSerializer s, Map<String, StringWithDescription[]> fields) throws IOException {
            if (fields != null) {
                for (Entry<String, StringWithDescription[]> entry : fields.entrySet()) {
                    // check match attribute
                    String k = entry.getKey();
                    // MatchType match = getMatchType(k);
                    PresetKeyType type = getKeyType(k);
                    switch (type) {
                    case TEXT:
                        s.startTag("", TEXT);
                        s.attribute("", KEY_ATTR, k);
                        s.endTag("", TEXT);
                        break;
                    case COMBO:
                    case MULTISELECT:
                        s.startTag("", type == PresetKeyType.COMBO ? COMBO_FIELD : MULTISELECT_FIELD);
                        s.attribute("", KEY_ATTR, k);
                        for (StringWithDescription v : entry.getValue()) {
                            s.startTag("", LIST_ENTRY);
                            s.attribute("", VALUE, v.getValue());
                            String description = v.getDescription();
                            if (description != null && !"".equals(description)) {
                                s.attribute("", SHORT_DESCRIPTION, v.getDescription());
                            }
                            s.endTag("", LIST_ENTRY);
                        }
                        s.endTag("", type == PresetKeyType.COMBO ? COMBO_FIELD : MULTISELECT_FIELD);
                        break;
                    case CHECK:
                        s.startTag("", CHECK_FIELD);
                        s.attribute("", KEY_ATTR, k);
                        s.endTag("", CHECK_FIELD);
                        break;
                    }
                }
            }
        }
    }

    /** Interface for handlers handling clicks on item or group icons */
    public interface PresetClickHandler {

        /**
         * Called for a normal click on a button showing a PresetItem
         * 
         * @param item the PresetItem
         */
        void onItemClick(@NonNull PresetItem item);

        /**
         * Called for a long click on a button showing a PresetItem
         * 
         * @param item the PresetItem
         * @return true if consumed
         */
        boolean onItemLongClick(@NonNull PresetItem item);

        /**
         * Called for a normal click on a button showing a PresetGroup
         * 
         * @param group the PresetGroup
         */
        void onGroupClick(@NonNull PresetGroup group);

        /**
         * Called for a long click on a button showing a PresetGroup
         * 
         * @param group the PresetGroup
         * @return true if consumed
         */
        boolean onGroupLongClick(@NonNull PresetGroup group);
    }

    /**
     * Get all possible keys for a specific ElementType
     * 
     * @param presets the current Presets
     * @param type the ELementType
     * @return a Collection of keys
     */
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
                default:
                    return null; // should never happen, all cases are covered
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
     * @param type the taype of element, if null all will be assumed
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

    public static MultiHashMap<String, PresetItem> getSearchIndex(Preset[] presets) {
        MultiHashMap<String, PresetItem> result = new MultiHashMap<>();
        for (Preset p : presets) {
            if (p != null) {
                result.addAll(p.searchIndex);
            }
        }
        return result;
    }

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
        return objectKeys.contains(key);
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
     * Build an intent to startup up the correct mapfeatures wiki page
     * 
     * @param ctx Android Context
     * @param p the PresetItem
     * @return an Intent
     */
    @NonNull
    public static Intent getMapFeaturesIntent(Context ctx, PresetItem p) {
        Uri uri = null;
        if (p != null) {
            try {
                uri = p.getMapFeatures();
            } catch (NullPointerException npe) {
                //
                Log.d(DEBUG_TAG, "Preset " + p.getName() + " has no/invalid map feature uri");
            }
        }
        if (uri == null) {
            uri = Uri.parse(ctx.getString(R.string.link_mapfeatures));
        }
        return new Intent(Intent.ACTION_VIEW, uri);
    }

    /**
     * Split multi select values with the preset defined delimiter character
     * 
     * @param values list of values that can potentially be split
     * @param preset the preset that sould be used
     * @param key the key used to determine the delimter value
     * @return list of split values
     */
    @Nullable
    public static ArrayList<String> splitValues(ArrayList<String> values, @NonNull PresetItem preset, @NonNull String key) {
        ArrayList<String> result = new ArrayList<>();
        String delimiter = String.valueOf(preset.getDelimiter(key));
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (v == null) {
                continue;
            }
            for (String s : v.split(Pattern.quote(delimiter))) {
                result.add(s.trim());
            }
        }
        return result;
    }

    /**
     * This is for the taginfo project repo
     * 
     * @param ctx Android Context
     * @param filename the filename to save to
     * @return true if things worked
     */
    public static boolean generateTaginfoJson(Context ctx, String filename) {
        Preset[] presets = App.getCurrentPresets(ctx);

        PrintStream outputStream = null;
        FileOutputStream fout = null;
        try {
            // String filename = new SimpleDateFormat("yyyy-MM-dd'T'HHmmss", Locale.US).format(new Date())+".json";
            File outfile = new File(FileUtil.getPublicDirectory(), filename);
            fout = new FileOutputStream(outfile);
            outputStream = new PrintStream(new BufferedOutputStream(fout));

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
            for (int i = 0; i < presets.length; i++) {
                if (presets[i] != null) {
                    if (i != 0) {
                        outputStream.print(",\n");
                    }
                    String json = presets[i].toJSON();
                    outputStream.print(json);
                }
            }
            outputStream.println("]}");
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Export failed - " + filename + " exception " + e);
            return false;
        } finally {
            SavingHelper.close(outputStream);
            SavingHelper.close(fout);
        }
        return true;
    }

    public void toXml(XmlSerializer s) throws IllegalArgumentException, IllegalStateException, IOException {
        s.startDocument("UTF-8", null);
        s.startTag("", PRESETS);
        for (PresetElement e : getRootGroup().getElements()) {
            e.toXml(s);
        }
        s.endTag("", PRESETS);
        s.endDocument();
    }
}
