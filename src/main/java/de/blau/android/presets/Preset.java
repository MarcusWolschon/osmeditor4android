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
import java.util.Comparator;
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
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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
import de.blau.android.contract.Urls;
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

    private static final String USE_LAST_AS_DEFAULT        = "use_last_as_default";
    private static final String PO                         = ".po";
    private static final String DEFAULT_PRESET_TRANSLATION = "preset_";
    private static final String NO                         = "no";
    private static final String VALUE_TYPE                 = "value_type";
    private static final String PRESET_NAME                = "preset_name";
    private static final String PRESET_LINK                = "preset_link";
    private static final String SHORT_DESCRIPTION          = "short_description";
    private static final String DISPLAY_VALUE              = "display_value";
    private static final String LIST_ENTRY                 = "list_entry";
    private static final String REFERENCE                  = "reference";
    private static final String ROLE                       = "role";
    private static final String ROLES                      = "roles";
    private static final String VALUES_SEARCHABLE          = "values_searchable";
    private static final String EDITABLE                   = "editable";
    private static final String VALUES_SORT                = "values_sort";
    private static final String VALUES_CONTEXT             = "values_context";
    private static final String SHORT_DESCRIPTIONS         = "short_descriptions";
    private static final String DISPLAY_VALUES             = "display_values";
    private static final String VALUES                     = "values";
    private static final String VALUES_FROM                = "values_from";
    private static final String DELIMITER                  = "delimiter";
    private static final String COMBO_FIELD                = "combo";
    private static final String MULTISELECT_FIELD          = "multiselect";
    private static final String YES                        = "yes";
    private static final String DISABLE_OFF                = "disable_off";
    private static final String VALUE_OFF                  = "value_off";
    private static final String VALUE_ON                   = "value_on";
    private static final String CHECK_FIELD                = "check";
    private static final String CHECKGROUP                 = "checkgroup";
    private static final String HREF                       = "href";
    private static final String WIKI                       = "wiki";
    private static final String LINK                       = "link";
    private static final String I18N                       = "i18n";
    private static final String JAVASCRIPT                 = "javascript";
    private static final String DEFAULT                    = "default";
    private static final String TEXT_CONTEXT               = "text_context";
    private static final String TEXT_FIELD                 = "text";
    private static final String TEXT                       = "text";
    private static final String VALUE                      = "value";
    private static final String NONE                       = "none";
    private static final String MATCH                      = "match";
    private static final String CHUNK                      = "chunk";
    private static final String KEY_ATTR                   = "key";
    private static final String OPTIONAL                   = "optional";
    private static final String SEPARATOR                  = "separator";
    private static final String ID                         = "id";
    private static final String DEPRECATED                 = "deprecated";
    private static final String TRUE                       = "true";
    private static final String GTYPE                      = "gtype";
    private static final String TYPE                       = "type";
    private static final String ITEM                       = "item";
    private static final String NAME_CONTEXT               = "name_context";
    private static final String ICON                       = "icon";
    private static final String NAME                       = "name";
    private static final String OBJECT_KEYS                = "object_keys";
    private static final String GROUP                      = "group";
    private static final String PRESETS                    = "presets";
    private static final String AREA                       = "area";
    private static final String MULTIPOLYGON               = "multipolygon";
    private static final String CLOSEDWAY                  = "closedway";
    private static final String LABEL                      = "label";
    private static final String ITEMS_SORT                 = "items_sort";
    private static final String SPACE                      = "space";
    /**
     * 
     */
    private static final long   serialVersionUID           = 7L;
    /** name of the preset XML file in a preset directory */
    public static final String  PRESETXML                  = "preset.xml";
    /** name of the MRU serialization file in a preset directory */
    private static final String MRUFILE                    = "mru.dat";
    public static final String  APKPRESET_URLPREFIX        = "apk:";

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
            switch (typeString) {
            case "opening_hours":
                type = OPENING_HOURS;
                break;
            case "opening_hours_mixed":
                type = OPENING_HOURS_MIXED;
                break;
            case "conditional":
                type = CONDITIONAL;
                break;
            case "integer":
                type = INTEGER;
                break;
            case "website":
                type = WEBSITE;
                break;
            case "phone":
                type = PHONE;
                break;
            case "wikipedia":
                type = WIKIPEDIA;
                break;
            case "wikidata":
                type = WIKIDATA;
                break;
            }
            return type;
        }
    }

    public enum UseLastAsDefault {
        TRUE, FALSE, FORCE;

        /**
         * Get an UseLastAsDefault value from a String
         * 
         * @param value the input string
         * @return the appropriate value of FALSE if is can't be determined
         */
        @NonNull
        public static UseLastAsDefault fromString(@NonNull String value) {
            UseLastAsDefault result = FALSE;
            switch (value) {
            case "true":
                result = TRUE;
                break;
            case "false":
                result = FALSE;
                break;
            case "force":
                result = FORCE;
                break;
            }
            return result;
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
    Preset() {
        mru = null;
    }

    /**
     * Create a dumyy Preset instance with an empty root PresetGroup
     * 
     * @return a dummy Preset instance
     */
    @NonNull
    public static Preset dummyInstance() {
        Preset preset = new Preset(); // dummy preset to hold the elements of all
        PresetGroup rootGroup = preset.new PresetGroup(null, "", null);
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
     * @throws Exception
     */
    public Preset(Context ctx, File directory, String externalPackage, boolean useTranslations)
            throws ParserConfigurationException, SAXException, IOException, NoSuchAlgorithmException {
        this.directory = directory;
        this.externalPackage = externalPackage;
        rootGroup = new PresetGroup(null, "", null);
        rootGroup.setItemSort(false);

        // noinspection ResultOfMethodCallIgnored
        directory.mkdir();

        InputStream fileStream = null;
        try {
            if (directory.getName().equals(AdvancedPrefDatabase.ID_DEFAULT)) {
                Log.i(DEBUG_TAG, "Loading default preset");
                iconManager = new PresetIconManager(ctx, null, null);
                fileStream = iconManager.openAsset(PRESETXML, true);
                if (useTranslations) {
                    // get translations
                    InputStream poFileStream = null;
                    try {
                        Locale locale = Locale.getDefault();
                        String language = locale.getLanguage();
                        poFileStream = iconManager.openAsset(DEFAULT_PRESET_TRANSLATION + locale + PO, true);
                        if (poFileStream == null) {
                            poFileStream = iconManager.openAsset(DEFAULT_PRESET_TRANSLATION + language + PO, true);
                        }
                        po = parserPoFile(poFileStream);
                    } finally {
                        SavingHelper.close(poFileStream);
                    }
                }
            } else if (externalPackage != null) {
                Log.i(DEBUG_TAG, "Loading APK preset, package=" + externalPackage + ", directory=" + directory.toString());
                iconManager = new PresetIconManager(ctx, directory.toString(), externalPackage);
                fileStream = iconManager.openAsset(PRESETXML, false);
            } else {
                Log.i(DEBUG_TAG, "Loading downloaded preset, directory=" + directory.toString());
                iconManager = new PresetIconManager(ctx, directory.toString(), null);
                File indir = new File(directory.toString());
                File[] list = indir.listFiles(new PresetFileFilter());
                if (list != null && list.length > 0) { // simply use the first XML file found
                    String presetFilename = list[0].getName();
                    Log.i(DEBUG_TAG, "Preset file name " + presetFilename);
                    fileStream = new FileInputStream(new File(directory, presetFilename));
                    if (useTranslations) {
                        // get translations
                        presetFilename = presetFilename.substring(0, presetFilename.length() - 4);
                        InputStream poFileStream = null;
                        try {
                            // try to open .po files either with the same name as the preset file or the standard name
                            try {
                                poFileStream = getPoInputStream(directory, presetFilename + "_", Locale.getDefault());
                            } catch (FileNotFoundException fnfe) {
                                try {
                                    poFileStream = getPoInputStream(directory, DEFAULT_PRESET_TRANSLATION, Locale.getDefault());
                                } catch (FileNotFoundException fnfe3) {
                                    // no translations
                                }
                            }
                            po = parserPoFile(poFileStream);
                        } finally {
                            SavingHelper.close(poFileStream);
                        }
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
     * Get an input stream for a .po file, try full locale string first then just the language
     * 
     * @param directory the director where the file is located
     * @param presetFilename the filename
     * @param locale the Locale
     * @return the InputStream
     * @throws FileNotFoundException if the file does not exist
     */
    @NonNull
    private FileInputStream getPoInputStream(@NonNull File directory, @NonNull String presetFilename, @NonNull Locale locale) throws FileNotFoundException {
        try {
            return new FileInputStream(new File(directory, presetFilename + locale.toString() + PO));
        } catch (FileNotFoundException fnfe) {
            return new FileInputStream(new File(directory, presetFilename + locale.getLanguage() + PO));
        }
    }

    /**
     * Create a Po class from an InputStream
     * 
     * @param poFileStream the InputStream
     * @return an Po object or null
     */
    @Nullable
    private Po parserPoFile(@Nullable InputStream poFileStream) {
        if (poFileStream != null) {
            try {
                return new Po(poFileStream);
            } catch (ParseException ignored) {
                Log.e(DEBUG_TAG, "Parsing translation file for " + Locale.getDefault() + " or " + Locale.getDefault().getLanguage() + " failed");
            } catch (TokenMgrError ignored) {
                Log.e(DEBUG_TAG, "Parsing translation file for " + Locale.getDefault() + " or " + Locale.getDefault().getLanguage() + " failed");
            }
        }
        return null;
    }

    /**
     * Construct a new preset from existing elements
     * 
     * @param elements list of PresetElements
     */
    public Preset(@NonNull List<PresetElement> elements) {
        mru = null;
        String name = "";
        if (elements != null && !elements.isEmpty()) {
            name = elements.get(0).getName();
        } else {
            Log.e(DEBUG_TAG, "List of PresetElements was null");
            return;
        }
        rootGroup = new PresetGroup(null, name, null);
        rootGroup.setItemSort(false);
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
                for (Entry<String, PresetField> entry : ((PresetItem) e).fields.entrySet()) {
                    String key = entry.getKey();
                    PresetField field = entry.getValue();
                    if (field instanceof PresetFixedField) {
                        tagItems.add(entry.getKey() + "\t" + ((PresetFixedField) field).getValue(), (PresetItem) e);
                    } else if (field instanceof PresetComboField && ((PresetComboField) field).getValues() != null) {
                        for (StringWithDescription v : ((PresetComboField) field).getValues()) {
                            tagItems.add(key + "\t" + v.getValue(), (PresetItem) e);
                        }
                    } else {
                        tagItems.add(key + "\t", (PresetItem) e);
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
        SAXParserFactory factory = SAXParserFactory.newInstance(); // NOSONAR
        SAXParser saxParser = factory.newSAXParser();

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
            private String                           delimiter         = null;
            /** check groups */
            private PresetCheckGroupField            checkGroup        = null;
            private int                              checkGroupCounter = 0;
            /** */
            private String                           currentLabel      = null;

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
                        if (tempArray != null && tempArray.length > 0) {
                            objectKeys.addAll(Arrays.asList(tempArray));
                        }
                    }
                    break;
                case GROUP:
                    PresetGroup parent = groupstack.peek();
                    PresetGroup g = new PresetGroup(parent, attr.getValue(NAME), attr.getValue(ICON));
                    String context = attr.getValue(NAME_CONTEXT);
                    if (context != null) {
                        g.setNameContext(context);
                    }
                    String itemsSort = attr.getValue(ITEMS_SORT);
                    if (itemsSort != null) {
                        g.setItemSort(YES.equals(itemsSort));
                    }
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
                    currentItem = new PresetItem(parent, attr.getValue(NAME), attr.getValue(ICON), type);
                    context = attr.getValue(NAME_CONTEXT);
                    if (context != null) {
                        currentItem.setNameContext(context);
                    }
                    currentItem.setDeprecated(TRUE.equals(attr.getValue(DEPRECATED)));
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
                    currentItem = new PresetItem(null, attr.getValue(ID), attr.getValue(ICON), type);
                    currentItem.setChunk();
                    checkGroupCounter = 0;
                    break;
                case SEPARATOR:
                    new PresetSeparator(groupstack.peek());
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
                    break;
                case TEXT_FIELD:
                    key = attr.getValue(KEY_ATTR);
                    if (key == null) {
                        Log.e(DEBUG_TAG, "Item " + attr.getValue(NAME) + " key must be present  in text field");
                        throw new SAXException("key must be present in text field");
                    }
                    currentItem.addTag(inOptionalSection, key, PresetKeyType.TEXT, (String) null);
                    String defaultValue = attr.getValue(DEFAULT);
                    if (defaultValue != null) {
                        currentItem.setDefault(key, defaultValue);
                    }
                    String text = attr.getValue(TEXT);
                    if (text != null) {
                        currentItem.setHint(key, text);
                    }
                    textContext = attr.getValue(TEXT_CONTEXT);
                    if (textContext != null) {
                        currentItem.setTextContext(key, textContext);
                    }
                    match = attr.getValue(MATCH);
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
                    String useLastAsDefault = attr.getValue(USE_LAST_AS_DEFAULT);
                    if (useLastAsDefault != null) {
                        currentItem.setUseLastAsDefault(key, useLastAsDefault);
                    }
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
                        currentLabel = null;
                    }
                    checkGroup.setOptional(inOptionalSection);
                    break;
                case CHECK_FIELD:
                    key = attr.getValue(KEY_ATTR);
                    String value_on = attr.getValue(VALUE_ON) == null ? YES : attr.getValue(VALUE_ON);
                    String value_off = attr.getValue(VALUE_OFF) == null ? NO : attr.getValue(VALUE_OFF);
                    String disable_off = attr.getValue(DISABLE_OFF);
                    StringWithDescription valueOn = new StringWithDescription(value_on, de.blau.android.util.Util.capitalize(value_on));
                    StringWithDescription valueOff = null;
                    PresetCheckField field = new PresetCheckField(key, valueOn);
                    if (disable_off == null || !disable_off.equals(TRUE)) {
                        valueOff = new StringWithDescription(value_off, de.blau.android.util.Util.capitalize(value_off));
                        ((PresetCheckField) field).setOffValue(valueOff);
                    }
                    defaultValue = attr.getValue(DEFAULT) == null ? null : ("on".equals(attr.getValue(DEFAULT)) ? value_on : value_off);
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
                    match = attr.getValue(MATCH);
                    if (match != null) {
                        field.setMatchType(match);
                    }
                    field.setOptional(inOptionalSection);
                    useLastAsDefault = attr.getValue(USE_LAST_AS_DEFAULT);
                    if (useLastAsDefault != null) {
                        currentItem.setUseLastAsDefault(key, useLastAsDefault);
                    }
                    if (checkGroup != null) {
                        checkGroup.addCheckField(field);
                    } else {
                        currentItem.fields.put(key, field);
                    }
                    currentItem.addValues(key, valueOff == null ? new StringWithDescription[] { valueOn } : new StringWithDescription[] { valueOn, valueOff });
                    break;
                case COMBO_FIELD:
                case MULTISELECT_FIELD:
                    boolean multiselect = MULTISELECT_FIELD.equals(name);
                    key = attr.getValue(KEY_ATTR);
                    if (key == null) {
                        Log.e(DEBUG_TAG, "Item " + attr.getValue(NAME) + " key must be present  in text field");
                        throw new SAXException("key must be present in combo/multiselect field");
                    }
                    delimiter = attr.getValue(DELIMITER);
                    if (delimiter == null) {
                        delimiter = multiselect ? MULTISELECT_DELIMITER : COMBO_DELIMITER;
                    }
                    String values = attr.getValue(VALUES);
                    String displayValues = attr.getValue(DISPLAY_VALUES);
                    String shortDescriptions = attr.getValue(SHORT_DESCRIPTIONS);
                    String valuesFrom = attr.getValue(VALUES_FROM);
                    final PresetKeyType keyType = multiselect ? PresetKeyType.MULTISELECT : PresetKeyType.COMBO;
                    if (values != null) {
                        currentItem.addTag(inOptionalSection, key, keyType, values, displayValues, shortDescriptions, delimiter);
                    } else if (valuesFrom != null) {
                        setValuesFromMethod(key, valuesFrom, keyType, currentItem, inOptionalSection, delimiter);
                    } else {
                        currentItem.addTag(inOptionalSection, key, keyType, (StringWithDescription[]) null, delimiter);
                        listKey = key;
                        listValues = new ArrayList<>();
                    }
                    ((PresetComboField) currentItem.getField(key)).setValuesContext(attr.getValue(VALUES_CONTEXT));

                    defaultValue = attr.getValue(DEFAULT);
                    if (defaultValue != null) {
                        currentItem.setDefault(key, defaultValue);
                    }
                    text = attr.getValue(TEXT);
                    if (text != null) {
                        currentItem.setHint(key, text);
                    }
                    textContext = attr.getValue(TEXT_CONTEXT);
                    if (textContext != null) {
                        currentItem.setTextContext(key, textContext);
                    }
                    match = attr.getValue(MATCH);
                    if (match != null) {
                        currentItem.setMatchType(key, match);
                    }
                    String sort = attr.getValue(VALUES_SORT);
                    if (sort != null) {
                        // normally this will not be set because true is the default
                        currentItem.setSortValues(key, YES.equals(sort) || TRUE.equals(sort));
                    }
                    String editable = attr.getValue(EDITABLE);
                    if (editable != null) {
                        currentItem.setEditable(key, YES.equals(editable) || TRUE.equals(editable));
                    }
                    String searchable = attr.getValue(VALUES_SEARCHABLE);
                    if (searchable != null) {
                        currentItem.setValuesSearchable(key, YES.equals(searchable) || TRUE.equals(searchable));
                    }
                    valueType = attr.getValue(VALUE_TYPE);
                    if (valueType != null) {
                        currentItem.setValueType(key, valueType);
                    }
                    useLastAsDefault = attr.getValue(USE_LAST_AS_DEFAULT);
                    if (useLastAsDefault != null) {
                        currentItem.setUseLastAsDefault(key, useLastAsDefault);
                    }
                    break;
                case ROLES:
                    break;
                case ROLE:
                    String roleValue = attr.getValue(KEY_ATTR);
                    text = attr.getValue(TEXT);
                    textContext = attr.getValue(TEXT_CONTEXT);
                    PresetRole role = new PresetRole(roleValue, text == null ? null : translate(text, textContext), attr.getValue(TYPE));
                    currentItem.addRole(role);
                    break;
                case REFERENCE:
                    PresetItem chunk = chunks.get(attr.getValue("ref")); // note this assumes that there are no
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
                                if (!currentItem.fields.containsKey(key)) {
                                    // FIXME we should only create new objects once
                                    PresetField copy = f.copy();
                                    copy.setOptional(true);
                                    currentItem.fields.put(copy.getKey(), copy);
                                } else {
                                    Log.e(DEBUG_TAG, "Error in PresetItem " + currentItem.getName() + " chunk " + attr.getValue("ref") + " field " + key
                                            + " overwrites existing field");
                                }
                            }
                        } else {
                            currentItem.fixedTags.putAll(chunk.getFixedTags());
                            if (!currentItem.isChunk()) {
                                for (Entry<String, PresetFixedField> e : chunk.getFixedTags().entrySet()) {
                                    key = e.getKey();
                                    StringWithDescription v = e.getValue().getValue();
                                    String value = "";
                                    if (v != null && v.getValue() != null) {
                                        value = v.getValue();
                                    }
                                    tagItems.add(key + "\t" + value, currentItem);
                                    currentItem.addToAutosuggest(key, v);
                                }
                            }
                            currentItem.fields.putAll(chunk.fields);
                        }
                        addToTagItems(currentItem, chunk.getFields());
                        currentItem.addAllRoles(chunk.roles); // FIXME this and the following could lead to
                                                              // duplicate entries
                        currentItem.addAllLinkedPresetNames(chunk.linkedPresetNames);
                    }
                    break;
                case LIST_ENTRY:
                    if (listValues != null) {
                        String v = attr.getValue(VALUE);
                        if (v != null) {
                            String d = attr.getValue(DISPLAY_VALUE);
                            if (d == null) {
                                d = attr.getValue(SHORT_DESCRIPTION);
                            }
                            String iconPath = attr.getValue(ICON);
                            if (iconPath == null) {
                                listValues.add(new StringWithDescription(v, d));
                            } else {
                                listValues.add(new StringWithDescriptionAndIcon(v, d, iconPath));
                            }
                        }
                    }
                    break;
                case PRESET_LINK:
                    String presetName = attr.getValue(PRESET_NAME);
                    if (presetName != null) {
                        currentItem.addLinkedPresetName(presetName);
                    }
                    break;
                case SPACE:
                    break;
                default:
                    Log.e(DEBUG_TAG, "Unknown start tag in preset item " + name);
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
             * @param valuesContext translation context, currently unused
             */
            private void setValuesFromMethod(final String key, final String valuesFrom, final PresetKeyType keyType, final PresetItem item,
                    final boolean inOptionalSection, final String delimiter) {
                item.addTag(inOptionalSection, key, keyType, (StringWithDescription[]) null, delimiter);
                (new AsyncTask<Void, Void, Object>() {
                    @Override
                    protected Object doInBackground(Void... params) {
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
                                item.addValues(key, valueArray);
                            } else if (result instanceof StringWithDescription[]) {
                                field.setValues((StringWithDescription[]) result);
                                item.addValues(key, (StringWithDescription[]) result);
                            }
                        }
                        return null;
                    }
                }).execute();
            }

            void addToTagItems(PresetItem currentItem, Map<String, PresetField> fields) {
                if (currentItem.isChunk()) { // only do this on the final expansion
                    return;
                }
                StringWithDescription dummy = new StringWithDescription("");
                for (Entry<String, PresetField> e : fields.entrySet()) {
                    PresetField field = e.getValue();
                    String key = e.getKey();
                    if (field instanceof PresetCheckGroupField) {
                        for (PresetCheckField check : ((PresetCheckGroupField) field).getCheckFields()) {
                            String checkKey = check.getKey();
                            tagItems.add(checkKey + "\t", currentItem);
                            currentItem.addToAutosuggest(checkKey, dummy);
                        }
                    } else {
                        tagItems.add(key + "\t", currentItem);
                        if (field instanceof PresetComboField) {
                            StringWithDescription values[] = ((PresetComboField) field).getValues();
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
                        } else {
                            currentItem.addToAutosuggest(key, dummy);
                        }
                    }
                }
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
                            currentItem.addValues(listKey, listValues.toArray(v));
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
            SAXParserFactory factory = SAXParserFactory.newInstance(); // NOSONAR
            SAXParser saxParser = factory.newSAXParser();

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
                preset.tagItems.removeKey(tag);
            }
        }
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
     * Recursively traverse the PresetELements and do something on them
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
        processElements(getRootGroup(), new PresetElementHandler() {
            @Override
            public void handle(PresetElement element) {
                element.icon = null;
                element.mapIcon = null;
            }
        });
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
        int size = path.getPath().size();
        if (size > 0) {
            String segment = path.getPath().get(0);
            for (PresetElement e : group.getElements()) {
                if (segment.equals(e.getName())) {
                    if (size == 1) {
                        return e;
                    } else {
                        if (e instanceof PresetGroup) {
                            PresetElementPath newPath = new PresetElementPath(path);
                            newPath.getPath().remove(0);
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
    public static View getRecentPresetView(Context ctx, Preset[] presets, PresetClickHandler handler, ElementType type) {
        Preset dummy = new Preset();
        PresetGroup recent = dummy.new PresetGroup(null, "recent", null);
        recent.setItemSort(false);
        for (Preset p : presets) {
            if (p != null && p.hasMRU()) {
                int allItemsCount = p.allItems.size();
                for (Integer index : p.mru.recentPresets) {
                    if (index < allItemsCount) {
                        recent.addElement(p.allItems.get(index), false);
                    }
                }
            }
        }
        return recent.getGroupView(ctx, handler, type, null);
    }

    /**
     * Check if a (non-empty) MRU is present
     * 
     * @return true if a (non-empty) MRU is present
     */
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
    public void removeRecentlyUsed(@NonNull PresetItem item) {
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
            int recommendedTagCount = possibleMatch.getRecommendedKeyCount();
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
         * @return the name if set or if null an empty String
         */
        @NonNull
        public String getName() {
            return name != null ? name : "";
        }

        /**
         * Return the name of this preset element, potentially translated
         * 
         * @return the name
         */
        @NonNull
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
        @Nullable
        public BitmapDrawable getMapIcon() {
            if (mapIcon == null && iconpath != null) {
                if (iconManager == null) {
                    iconManager = getIconManager(App.getCurrentInstance().getApplicationContext());
                }
                mapIcon = iconManager.getDrawable(iconpath, de.blau.android.Map.ICON_SIZE_DP);
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
        public void setParent(@Nullable PresetGroup pg) {
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
        public boolean appliesTo(@NonNull ElementType type) {
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
         * Get a list of ElementTypes this PresetItem applies to
         * 
         * @return a List of ElementType
         */
        @NonNull
        public List<ElementType> appliesTo() {
            List<ElementType> result = new ArrayList<>();
            if (appliesToNode) {
                result.add(ElementType.NODE);
            }
            if (appliesToWay) {
                result.add(ElementType.WAY);
            }
            if (appliesToClosedway) {
                result.add(ElementType.CLOSEDWAY);
            }
            if (appliesToRelation) {
                result.add(ElementType.RELATION);
            }
            if (appliesToArea) {
                result.add(ElementType.AREA);
            }
            return result;
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
        public void setMapFeatures(@Nullable String url) {
            if (url != null) {
                mapFeatures = url;
            }
        }

        /**
         * Get the documentation URL (typically from the OSM wiki) for this PresetELement
         * 
         * @return a String containing the full or partial URL for the page
         */
        @Nullable
        public String getMapFeatures() {
            return mapFeatures;
        }

        /**
         * Set the translation context for the name field of this PresetElement
         * 
         * @param context the translation context
         */
        void setNameContext(@Nullable String context) {
            nameContext = context;
        }

        /**
         * Check if the deprecated flag is set
         * 
         * @return true if the PresetELement is deprecated
         */
        public boolean isDeprecated() {
            return deprecated;
        }

        /**
         * Set the deprecated flag
         * 
         * @param deprecated the value to set
         */
        public void setDeprecated(boolean deprecated) {
            this.deprecated = deprecated;
        }

        /**
         * Return the ISO code for the region this PresetELement is intended for
         * 
         * @return the ISO code or null if none set
         */
        @Nullable
        public String getRegion() {
            return region;
        }

        /**
         * Set the ISO code for the region this PresetELement is intended for
         * 
         * @param region the ISO code or null if none should be set
         */
        public void setRegion(@Nullable String region) {
            this.region = region;
        }

        /**
         * Get an object documenting where in the hierarchy this element is.
         * 
         * This is essentially the only unique way of identifying a specific preset
         * 
         * @param root PresetGroup that this is relative to
         * @return an object containing the path elements
         */
        @Nullable
        public PresetElementPath getPath(@NonNull PresetGroup root) {
            for (PresetElement e : root.getElements()) {
                if (e.equals(this)) {
                    PresetElementPath result = new PresetElementPath();
                    result.getPath().add(e.getName());
                    return result;
                } else {
                    if (e instanceof PresetGroup) {
                        PresetElementPath result = getPath((PresetGroup) e);
                        if (result != null) {
                            result.getPath().add(0, e.getName());
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
            return name + " " + iconpath + " " + appliesToWay + " " + appliesToNode + " " + appliesToClosedway + " " + appliesToRelation + " " + appliesToArea;
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
        private static final long serialVersionUID = 3L;

        private final int groupIndex;

        private boolean itemSort = true;

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
         * Sets the flag for item sorting
         * 
         * @param sort if true PresetITems will be sorted
         */
        public void setItemSort(boolean sort) {
            itemSort = sort;
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

        /**
         * Get the index for this PresetGroup
         * 
         * @return the index
         */
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
            if (itemSort) {
                List<PresetItem> tempItems = new ArrayList<>();
                List<PresetGroup> tempGroups = new ArrayList<>();
                List<PresetElement> tempElements = new ArrayList<>(filteredElements);
                filteredElements.clear();
                for (PresetElement element : tempElements) {
                    if (element instanceof PresetItem) {
                        sortAndAddElements(filteredElements, tempGroups);
                        tempItems.add((PresetItem) element);
                    } else if (element instanceof PresetGroup) {
                        sortAndAddElements(filteredElements, tempItems);
                        tempGroups.add((PresetGroup) element);
                    } else { // PresetSeperator
                        sortAndAddElements(filteredElements, tempGroups);
                        sortAndAddElements(filteredElements, tempItems);
                        filteredElements.add(element);
                    }
                }
                sortAndAddElements(filteredElements, tempGroups);
                sortAndAddElements(filteredElements, tempItems);
            }
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

        /**
         * Sort the PresetElements in a temporary List and add them to a target List
         * 
         * @param <T> PresetElement sub-class
         * @param target target List
         * @param temp temp List
         */
        private <T extends PresetElement> void sortAndAddElements(@NonNull List<PresetElement> target, @NonNull List<T> temp) {

            final Comparator<PresetElement> itemComparator = new Comparator<PresetElement>() {
                @Override
                public int compare(@NonNull PresetElement pe1, @NonNull PresetElement pe2) {
                    return pe1.getTranslatedName().compareTo(pe2.getTranslatedName());
                }
            };

            if (!temp.isEmpty()) {
                Collections.sort(temp, itemComparator);
                target.addAll(temp);
                temp.clear();
            }
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

        private static final String HTTP = "http";

        /**
         * 
         */
        private static final long serialVersionUID = 13L;

        /**
         * All fields in the order they are in the Preset file
         */
        private LinkedHashMap<String, PresetField> fields = new LinkedHashMap<>();

        /** "fixed" tags, i.e. the ones that have a fixed key-value pair */
        private Map<String, PresetFixedField> fixedTags = new HashMap<>();

        /**
         * Roles
         */
        private LinkedList<PresetRole> roles = null;

        /**
         * Linked names of presets
         */
        private LinkedList<String> linkedPresetNames = null;

        /**
         * true if a chunk
         */
        private boolean chunk = false;

        private final int itemIndex;

        private transient int recommendedKeyCount = -1;

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
            this.fields = item.fields;
            this.fixedTags = item.fixedTags;
            this.roles = item.roles;
            this.linkedPresetNames = item.linkedPresetNames;

            if (!chunk) {
                for (Entry<String, PresetFixedField> e : getFixedTags().entrySet()) {
                    StringWithDescription v = e.getValue().getValue();
                    String key = e.getKey();
                    String value = "";
                    if (v != null && v.getValue() != null) {
                        value = v.getValue();
                    }
                    tagItems.add(key + "\t" + value, this);
                    addToAutosuggest(key, v);
                }
                for (Entry<String, PresetField> e : getFields().entrySet()) {
                    PresetField field = e.getValue();
                    if (field instanceof PresetCheckGroupField) {
                        for (PresetCheckField check : ((PresetCheckGroupField) field).getCheckFields()) {
                            tagItems.add(check.getKey() + "\t", this);
                        }
                    } else if (!(field instanceof PresetFixedField)) {
                        String key = e.getKey();
                        tagItems.add(key + "\t", this);
                        if (field instanceof PresetComboField) {
                            StringWithDescription[] values = ((PresetComboField) field).getValues();
                            for (StringWithDescription swd : values) {
                                tagItems.add(e.getKey() + "\t" + swd.getValue(), this);
                            }
                            addToAutosuggest(key, values);
                        }
                    }
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
        synchronized void buildSearchIndex() {
            addToSearchIndex(name);
            if (parent != null) {
                String parentName = parent.getName();
                if (parentName != null && parentName.length() > 0) {
                    addToSearchIndex(parentName);
                }
            }
            for (Entry<String, PresetFixedField> entry : fixedTags.entrySet()) {
                PresetFixedField fixedField = entry.getValue();
                StringWithDescription v = fixedField.getValue();
                addToSearchIndex(fixedField.getKey());
                String hint = fixedField.getHint();
                if (hint != null) {
                    addToSearchIndex(hint);
                }
                String value = v.getValue();
                addToSearchIndex(value);
                addToSearchIndex(v.getDescription());
                // support subtypes
                PresetField subTypeField = fields.get(value);
                if (subTypeField instanceof PresetComboField) {
                    StringWithDescription[] subtypes = ((PresetComboField) subTypeField).getValues();
                    if (subtypes != null) {
                        for (StringWithDescription subtype : subtypes) {
                            addToSearchIndex(subtype.getValue());
                            addToSearchIndex(subtype.getDescription());
                        }
                        ((PresetComboField) subTypeField).valuesSearchable = false;
                    }
                }
            }
            for (Entry<String, PresetField> entry : fields.entrySet()) {
                PresetField field = entry.getValue();
                if (!(field instanceof PresetCheckGroupField)) {
                    addToSearchIndex(field.getKey());
                    String hint = field.getHint();
                    if (hint != null) {
                        addToSearchIndex(hint);
                    }
                    if (field instanceof PresetComboField) {
                        if (((PresetComboField) field).valuesSearchable && ((PresetComboField) field).getValues() != null) {
                            for (StringWithDescription value : ((PresetComboField) field).getValues()) {
                                addToSearchIndex(value.getValue());
                                addToSearchIndex(value.getDescription());
                            }
                        }
                    }
                } else {
                    for (PresetCheckField check : ((PresetCheckGroupField) field).getCheckFields()) {
                        addToSearchIndex(check.getKey());
                        String hint = field.getHint();
                        if (hint != null) {
                            addToSearchIndex(hint);
                        }
                        StringWithDescription value = check.getOnValue();
                        addToSearchIndex(value.getValue());
                        addToSearchIndex(value.getDescription());
                        value = check.getOffValue();
                        if (value != null && !"".equals(value.getValue())) {
                            addToSearchIndex(value.getValue());
                            addToSearchIndex(value.getDescription());
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
            PresetFixedField field = new PresetFixedField(key, new StringWithDescription(value, text));

            fixedTags.put(key, field);
            fields.put(key, field);
            if (!chunk) {
                tagItems.add(key + "\t" + value, this);
                addToAutosuggest(key, value.length() > 0 ? new StringWithDescription(value, text) : null);
            }
        }

        /**
         * Adds a recommended or optional tag to the item and populates autosuggest.
         * 
         * @param optional true if optional, false if recommended
         * @param key key name of the tag
         * @param type type of preset field
         * @param value value string from the XML (comma-separated list if more than one possible values)
         */
        public void addTag(boolean optional, @NonNull String key, PresetKeyType type, String value) {
            addTag(optional, key, type, value, null, null, COMBO_DELIMITER);
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
         */
        public void addTag(boolean optional, @NonNull String key, PresetKeyType type, String value, String displayValue, String shortDescriptions,
                final String delimiter) {
            String[] valueArray = (value == null) ? new String[0] : value.split(Pattern.quote(delimiter));
            String[] displayValueArray = (displayValue == null) ? new String[0] : displayValue.split(Pattern.quote(delimiter));
            String[] shortDescriptionArray = (shortDescriptions == null) ? new String[0] : shortDescriptions.split(Pattern.quote(delimiter));
            StringWithDescription[] valuesWithDesc = new StringWithDescription[valueArray.length];
            boolean useDisplayValues = valueArray.length == displayValueArray.length;
            boolean useShortDescriptions = !useDisplayValues && valueArray.length == shortDescriptionArray.length;
            for (int i = 0; i < valueArray.length; i++) {
                String description = null;
                if (useDisplayValues) {
                    description = displayValueArray[i];
                } else if (useShortDescriptions) {
                    description = shortDescriptionArray[i];
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
        public void addTag(boolean optional, @NonNull String key, PresetKeyType type, Collection<StringWithDescription> valueCollection,
                final String delimiter) {
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
        public void addTag(boolean optional, @NonNull String key, PresetKeyType type, StringWithDescription[] valueArray, final String delimiter) {
            addValues(key, valueArray);
            PresetField field = null;
            switch (type) {
            case COMBO:
            case MULTISELECT:
                field = new PresetComboField(key, valueArray);
                ((PresetComboField) field).setMultiSelect(type == PresetKeyType.MULTISELECT);
                if (!MULTISELECT_DELIMITER.equals(delimiter) || !COMBO_DELIMITER.equals(delimiter)) {
                    ((PresetComboField) field).delimiter = delimiter;
                }
                break;
            case TEXT:
                field = new PresetTextField(key);
                break;
            case CHECK:
                Log.e(DEBUG_TAG, "check fields should not be handled here");
                break;
            }
            if (field != null) {
                field.setOptional(optional);
                fields.put(key, field);
            }
        }

        /**
         * Add key and values to tagItems and autosuggest
         * 
         * @param key the key
         * @param valueArray the suggested values
         */
        private synchronized void addValues(String key, StringWithDescription[] valueArray) {
            if (!chunk) {
                tagItems.add(key + "\t", this);
                if (valueArray != null && valueArray.length > 0) {
                    for (StringWithDescription v : valueArray) {
                        tagItems.add(key + "\t" + v.getValue(), this);
                    }
                    addToAutosuggest(key, valueArray);
                } else {
                    addToAutosuggest(key, new StringWithDescription(""));
                }
            }
        }

        /**
         * Add a (non-fixed) PresetField to the PresetItem
         * 
         * @param field the PresetField
         */
        public void addField(@NonNull PresetField field) {
            fields.put(field.key, field);
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
                roles.addAll(newRoles);
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
         * Save default for the tag
         * 
         * @param key tag key this should be set for
         * @param defaultValue default value to set
         */
        public void setDefault(@NonNull String key, @Nullable String defaultValue) {
            PresetField field = fields.get(key);
            if (field != null) {
                field.setDefaultValue(defaultValue);
            }
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
                return (combo.delimiter != null ? combo.delimiter : (combo.isMultiSelect() ? MULTISELECT_DELIMITER : COMBO_DELIMITER)).charAt(0);
            } else {
                Log.e(DEBUG_TAG, "Trying to get delimiter from non-combo field, item " + name + " key " + key + " "
                        + (field != null ? field.getClass().getName() : "null"));
                return COMBO_DELIMITER.charAt(0);
            }
        }

        /**
         * Set the match type for this tag
         * 
         * @param key tag key to set the match type for
         * @param match the match type as String
         */
        public void setMatchType(String key, String match) {
            PresetField field = fields.get(key);
            if (field != null) {
                field.setMatchType(match);
            } else {
                Log.e(DEBUG_TAG, "setMatchType PresetField for key " + key + " is null");
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
         * Set the ValueType for the key
         * 
         * @param key the key
         * @param type a String for the ValueType
         */
        public void setValueType(@NonNull String key, @NonNull String type) {
            PresetField field = fields.get(key);
            if (field != null) {
                field.valueType = ValueType.fromString(type);
            }
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
                return field.valueType;
            }
            return null;
        }

        /**
         * Set the UseLastAsDefault for the key
         * 
         * @param key the key
         * @param type a String for the ValueType
         */
        public void setUseLastAsDefault(@NonNull String key, @NonNull String type) {
            PresetField field = fields.get(key);
            if (field != null) {
                field.setUseLastAsDefault(UseLastAsDefault.fromString(type));
            }
        }

        /**
         * Record if the values from the combo or multiselect values should be added to the search index
         * 
         * @param key combo/multiselect key
         * @param search if true add to index
         */
        public void setValuesSearchable(@NonNull String key, boolean search) {
            PresetField field = fields.get(key);
            if (field instanceof PresetComboField) {
                ((PresetComboField) field).valuesSearchable = search;
            } else {
                Log.e(DEBUG_TAG, "Trying to set values searchable on non-combo field " + (field != null ? field.getClass().getName() : "null") + " key " + key);
            }
        }

        /**
         * Check if values for this key should be added to the search index
         * 
         * @param key key to check
         * @return true if values should be added to the search index
         */
        public boolean getValuesSearchable(@NonNull String key) {
            PresetField field = fields.get(key);
            if (field instanceof PresetComboField) {
                return ((PresetComboField) field).valuesSearchable;
            }
            return false;
        }

        /**
         * Add a linked preset to the PresetItem
         * 
         * @param presetName name of the PresetItem to link to
         */
        public void addLinkedPresetName(String presetName) {
            if (linkedPresetNames == null) {
                linkedPresetNames = new LinkedList<>();
            }
            linkedPresetNames.add(presetName);
        }

        /**
         * Add a LinkedList containing linked presets to the PresetItem
         * 
         * @param newLinkedPresetNames the LinkedList of PresetITem names
         */
        public void addAllLinkedPresetNames(LinkedList<String> newLinkedPresetNames) {
            if (linkedPresetNames == null) {
                linkedPresetNames = newLinkedPresetNames; // doesn't matter if newLinkedPresetNames is null
            } else if (newLinkedPresetNames != null) {
                linkedPresetNames.addAll(newLinkedPresetNames);
            }
        }

        /**
         * Get all linked preset names
         * 
         * @return a list of all linked presets or null if none
         */
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
                                linkedPresetTags = candidateItem.getFields().keySet();
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

        /**
         * Allow alphabetic sorting of values
         * 
         * @param key combo/multiselect key
         * @param sort if true alphabetically sort the entries
         */
        public void setSortValues(@NonNull String key, boolean sort) {
            PresetField field = fields.get(key);
            if (field instanceof PresetComboField) {
                ((PresetComboField) field).sort = sort;
            } else {
                Log.e(DEBUG_TAG, "Trying to set values sort attribute on non-combo field " + (field != null ? field.getClass().getName() : "null"));
            }
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
                return ((PresetComboField) field).sort;
            }
            return false;
        }

        /**
         * Set a javascript script to be executed for the text field
         * 
         * @param key text field key
         * @param script javascript to set or null
         */
        public void setJavaScript(@NonNull String key, @Nullable String script) {
            PresetField field = fields.get(key);
            if (field instanceof PresetFieldJavaScript) {
                ((PresetFieldJavaScript) field).setScript(script);
            } else {
                Log.e(DEBUG_TAG, "Trying to set javascript attribute on field withour the interface " + (field != null ? field.getClass().getName() : "null"));
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
            PresetField field = fields.get(key);
            if (field instanceof PresetFieldJavaScript) {
                return ((PresetFieldJavaScript) field).getScript();
            }
            return null;
        }

        /**
         * Indicate that the field can have i18n variants
         * 
         * @param key text field key
         */
        public void setI18n(@NonNull String key) {
            PresetField field = fields.get(key);
            if (field instanceof PresetTextField) {
                ((PresetTextField) field).i18n = true;
            } else {
                Log.e(DEBUG_TAG, "Trying to set i18n attribute on non-text field " + (field != null ? field.getClass().getName() : "null"));
            }
        }

        /**
         * Check if the key supports i18n variants
         * 
         * @param key key we want to check
         * @return true if the key supports i18n variants
         */
        public boolean supportsI18n(@NonNull String key) {
            PresetField field = fields.get(key);
            if (field instanceof PresetTextField) {
                return ((PresetTextField) field).i18n;
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
                if (field instanceof PresetTextField && ((PresetTextField) field).i18n) {
                    result.add(((PresetTextField) field).key);
                }
            }
            return result;
        }

        /**
         * Set the editable property for the combo/multiselect
         * 
         * @param key combo/multiselect key
         * @param isEditable value to set
         */
        public void setEditable(@NonNull String key, boolean isEditable) {
            PresetField field = fields.get(key);
            if (field instanceof PresetComboField) {
                ((PresetComboField) field).editable = isEditable;
            } else {
                Log.e(DEBUG_TAG, "Trying to set editable attribute on non-combo field " + (field != null ? field.getClass().getName() : "null"));
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
        public boolean isEditable(@NonNull String key) {
            PresetField field = fields.get(key);
            if (field instanceof PresetComboField) {
                return ((PresetComboField) field).editable;
            }
            return false;
        }

        /**
         * Set the text context for this field
         * 
         * @param key the tag key
         * @param textContext the context
         */
        public void setTextContext(@NonNull String key, @Nullable String textContext) {
            PresetField field = fields.get(key);
            if (field != null) {
                field.setTextContext(textContext);
            }
        }

        /**
         * Get the text context for a key
         * 
         * @param key the tag key
         * @return the context
         */
        @Nullable
        public String getTextContext(@NonNull String key) {
            PresetField field = fields.get(key);
            if (field != null) {
                return field.getTextContext();
            }
            return null;
        }

        /**
         * Set the text context for this field
         * 
         * @param key the tag key
         * @param valueContext the context
         */
        public void setValueContext(@NonNull String key, @Nullable String valueContext) {
            PresetField field = fields.get(key);
            if (field != null) {
                field.valueContext = valueContext;
            }
        }

        /**
         * Get the value context for a key
         * 
         * @param key the tag key
         * @return the context
         */
        @Nullable
        public String getValueContext(@NonNull String key) {
            PresetField field = fields.get(key);
            if (field != null) {
                return field.valueContext;
            }
            return null;
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
         * Get the number of "recommended" keys aka non-fixed and non-optional Note: this only calculates the value once
         * and then uses a cached version
         * 
         * @return the number of "recommended" keys
         */
        private int getRecommendedKeyCount() {
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
        public PresetKeyType getKeyType(String key) {
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
        public boolean matches(Map<String, String> tagSet) {
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
                } else {
                    return false; // no point in continuing
                }
            }
            return matchCount == fixedTagsCount;
        }

        /**
         * Returns the number of matches between the list of non-optional tags and the provided tags
         * 
         * Uses the match value to control actual behavior
         * 
         * @param tagMap Map containing the tags
         * @return number of matches
         */
        public int matchesRecommended(Map<String, String> tagMap) {
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
                    if (type == MatchType.KEY) {
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
        public boolean hasKey(@NonNull String key) {
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
            return Preset.hasKeyValue(field, key, value);
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
            while (p != null && p != rootGroup && !"".equals(p.getName())) {
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
                if (isEditable(k) || field instanceof PresetTextField || field instanceof PresetCheckField
                        || (match != null && match != MatchType.KEY_VALUE && match != MatchType.KEY)) {
                    if (jsonString.length() != 0) {
                        jsonString.append(",\n");
                    }
                    jsonString.append(tagToJSON(presetName, k, null));
                }
                if (!isEditable(k) && field instanceof PresetComboField && (match == null || match == MatchType.KEY_VALUE || match == MatchType.KEY)) {
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
            String mapFeatures = getMapFeatures();
            if (mapFeatures != null) {
                s.startTag("", LINK);
                if (mapFeatures.startsWith(Urls.OSM_WIKI) || !mapFeatures.startsWith(HTTP)) {
                    // wiki might or might not be present;
                    mapFeatures = mapFeatures.replace(Urls.OSM_WIKI, "").replace("wiki/", "");
                    s.attribute("", WIKI, mapFeatures);
                } else {
                    s.attribute("", HREF, mapFeatures);
                }
                s.endTag("", LINK);
            }
            for (Entry<String, PresetFixedField> entry : fixedTags.entrySet()) {
                s.startTag("", KEY_ATTR);
                s.attribute("", KEY_ATTR, entry.getKey());
                PresetFixedField field = entry.getValue();
                StringWithDescription v = field.getValue();
                s.attribute("", VALUE, v.getValue());
                String description = v.getDescription();
                if (description != null && !"".equals(description)) {
                    s.attribute("", TEXT, description);
                }
                s.endTag("", KEY_ATTR);
            }
            fieldsToXml(s, fields);
            s.endTag("", chunk ? CHUNK : ITEM);
        }

        /**
         * Output the preset fields to XML Will add optional tags where necessary
         * 
         * @param fields a map containing the fields
         * @param s the serializer
         * @throws IOException
         */
        private void fieldsToXml(XmlSerializer s, Map<String, PresetField> fields) throws IOException {
            boolean inOptional = false;
            if (fields != null) {
                for (Entry<String, PresetField> entry : fields.entrySet()) {
                    PresetField field = entry.getValue();
                    if (field instanceof PresetFixedField) {
                        continue;
                    }
                    // check match attribute
                    String k = entry.getKey();
                    // MatchType match = getMatchType(k);
                    if (!inOptional && field.isOptional()) {
                        s.startTag("", OPTIONAL);
                        inOptional = true;
                    }
                    if (inOptional && !field.isOptional()) {
                        s.endTag("", OPTIONAL);
                        inOptional = false;
                    }
                    if (field instanceof PresetTextField) {
                        s.startTag("", TEXT);
                        s.attribute("", KEY_ATTR, k);
                        s.endTag("", TEXT);
                    } else if (field instanceof PresetComboField) {
                        s.startTag("", ((PresetComboField) field).isMultiSelect() ? MULTISELECT_FIELD : COMBO_FIELD);
                        s.attribute("", KEY_ATTR, k);
                        for (StringWithDescription v : ((PresetComboField) field).getValues()) {
                            s.startTag("", LIST_ENTRY);
                            s.attribute("", VALUE, v.getValue());
                            String description = v.getDescription();
                            if (description != null && !"".equals(description)) {
                                s.attribute("", SHORT_DESCRIPTION, v.getDescription());
                            }
                            s.endTag("", LIST_ENTRY);
                        }
                        s.endTag("", ((PresetComboField) field).isMultiSelect() ? MULTISELECT_FIELD : COMBO_FIELD);
                    } else if (field instanceof PresetCheckField) {
                        s.startTag("", CHECK_FIELD);
                        s.attribute("", KEY_ATTR, k);
                        s.endTag("", CHECK_FIELD);
                    } else {
                        Log.e(DEBUG_TAG, "Unknown PresetField type " + field.getClass().getName());
                    }
                }
                if (inOptional) {
                    s.endTag("", OPTIONAL);
                }
            }
        }
    }

    /**
     * Check if a key-value tupel matches a specific PresetFiel taking the MatchType in to account
     * 
     * @param field the PresetField
     * @param key the key
     * @param value the value
     * @return true if the tag matches
     */
    public static boolean hasKeyValue(PresetField field, @NonNull String key, @Nullable String value) {

        if (field == null) {
            return false;
        }

        if (field instanceof PresetFixedField) {
            StringWithDescription swd = ((PresetFixedField) field).getValue();
            if (swd != null) {
                if ("".equals(value) || swd.getValue() == null || swd.equals(value) || "".equals(swd.getValue())) {
                    return true;
                }
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
     * Split multi select values with the preset defined delimiter character
     * 
     * @param values list of values that can potentially be split
     * @param preset the preset that should be used
     * @param key the key used to determine the delimiter value
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

    /**
     * Convert this Preset to XML
     * 
     * @param s an XmlSerializer instance
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     * @throws IOException
     */
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
