package de.blau.android.presets;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.R;
import de.blau.android.contract.Files;
import de.blau.android.contract.Paths;
import de.blau.android.osm.OsmXml;
import de.blau.android.osm.Tags;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.propertyeditor.CustomPreset;
import de.blau.android.taginfo.TaginfoServer;
import de.blau.android.taginfo.TaginfoServer.SearchResult;
import de.blau.android.taginfo.TaginfoServer.WikiPageResult;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.FileUtil;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.collections.MultiHashMap;

public class AutoPreset {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, AutoPreset.class.getSimpleName().length());
    private static final String DEBUG_TAG = AutoPreset.class.getSimpleName().substring(0, TAG_LEN);

    private static final String INDENT_OUTPUT = "http://xmlpull.org/v1/doc/features.html#indent-output";
    public static final String  ICON          = "auto-preset.png";
    private static final String PNG           = ".png";
    private static final String RAILWAY_ICON  = "auto-preset-railway.png";

    private static final MultiHashMap<String, StringWithDescription> HARDWIRED_KEYS = new MultiHashMap<>();
    static {
        StringWithDescription[] standardStuff = new StringWithDescription[] { new StringWithDescription(Tags.KEY_OPENING_HOURS, "Opening Hours"),
                new StringWithDescription(Tags.KEY_WHEELCHAIR, "Wheelchairs"), new StringWithDescription(Tags.KEY_LEVEL, "Level") };
        HARDWIRED_KEYS.add(Tags.KEY_SHOP, standardStuff);
        HARDWIRED_KEYS.add(Tags.KEY_AMENITY, standardStuff);
        HARDWIRED_KEYS.add(Tags.KEY_LEISURE, standardStuff);
    }

    private static final String[] ICONS     = { AutoPreset.ICON, "auto-preset-amenity.png", "auto-preset-shop.png", "auto-preset-tourism.png",
            "auto-preset-tourism.png", "auto-preset-man_made.png", "auto-preset-man_made.png", "auto-preset-emergency.png", "auto-preset-craft.png",
            "auto-preset-office.png", "auto-preset-military.png", "auto-preset-natural.png", RAILWAY_ICON, RAILWAY_ICON, RAILWAY_ICON,
            "auto-preset-highway.png", "auto-preset-highway.png", "auto-preset-healthcare.png", "auto-preset-landuse.png", "auto-preset-waterway.png",
            CustomPreset.ICON };
    private static final String[] ICONSDEST = { AutoPreset.ICON, "amenity.png", "shop.png", "tourism.png", "leisure.png", "man_made.png", "building.png",
            "emergency.png", "craft.png", "office.png", "military.png", "natural.png", "railway.png", "aeroway.png", "aerialway.png", "highway.png",
            "barrier.png", "healthcare.png", "landuse.png", "waterway.png", CustomPreset.ICON };

    private final Context  context;
    private final Preset[] presets;
    private final String   language;

    /**
     * Construct a new instance
     * 
     * @param context Android Context
     */
    public AutoPreset(@NonNull Context context) {
        this.context = context;
        presets = App.getCurrentPresets(context);
        Locale locale = Locale.getDefault();
        language = locale.getLanguage();
    }

    /**
     * Create a Preset with PresetItems generated from taginfo
     * 
     * @param term the term we are interested in
     * @param maxResults maximum number of results
     * @return a temporary Preset object
     */
    @NonNull
    public Preset fromTaginfo(@NonNull String term, int maxResults) {
        String server = App.getPreferences(context).getTaginfoServer();
        List<SearchResult> candidateTags = TaginfoServer.searchByKeyword(context, server, term, -1);

        Preset preset = Preset.dummyInstance();
        try {
            preset.setIconManager(new PresetIconManager(context,
                    FileUtil.getPublicDirectory(FileUtil.getPublicDirectory(), Paths.DIRECTORY_PATH_AUTOPRESET).getAbsolutePath(), null));
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "Setting icon managery failed " + e.getMessage());
        }
        PresetGroup group = preset.getRootGroup();
        if (candidateTags != null) {
            for (SearchResult sr : candidateTags) {
                // remove results with empty values and presets that we already have
                if (sr.getValue() != null && !"".equals(sr.getValue()) && !existsInPresets(sr)) {
                    String resultKey = sr.getKey();
                    WikiPageResult wikiPage = TaginfoServer.wikiPage(context, server, resultKey, sr.getValue(), language, null);
                    if (wikiPage != null) {
                        SearchResult stats = TaginfoServer.tagStats(context, server, resultKey, sr.getValue());
                        if (stats != null) {
                            Log.d(DEBUG_TAG, "Creating PresetItem for " + wikiPage);
                            String presetIcon = ICON;
                            if (haveIcon(resultKey)) {
                                presetIcon = resultKey + PNG;
                            }
                            AutoPresetItem item = new AutoPresetItem(preset, group, resultKey + " " + sr.getValue(), presetIcon, null, stats.getCount());
                            String title = wikiPage.getTitleOther(); // fallback
                            if (wikiPage.getTitle() != null) { // local language
                                title = wikiPage.getTitle();
                            } else if (wikiPage.getTitleEN() != null) {
                                title = wikiPage.getTitleEN();
                            }
                            item.setMapFeatures(title);
                            if (wikiPage.isOnNode()) {
                                item.setAppliesToNode();
                            }
                            if (wikiPage.isOnWay()) {
                                item.setAppliesToWay();
                            }
                            if (wikiPage.isOnArea()) {
                                item.setAppliesToArea();
                            }
                            if (wikiPage.isOnRelation()) {
                                item.setAppliesToRelation();
                            }
                            Log.d(DEBUG_TAG, "adding " + resultKey + " " + sr.getValue());
                            item.addFixedTag(resultKey, sr.getValue(), null, null);

                            List<String> combinationsFromTaginfo = TaginfoServer.tagCombinations(context, server, resultKey, sr.getValue(), getAppliesTo(item),
                                    10);
                            if (combinationsFromTaginfo != null) {
                                combinationsFromTaginfo.addAll(wikiPage.getCombinations());
                            } else {
                                combinationsFromTaginfo = wikiPage.getCombinations();
                            }
                            // these need to be added at the end or else we will overwrite values we have with better
                            // quality
                            // disabled for now as they seem to create too much noise
                            //
                            // List<String> keyCombinationsFromTaginfo = TaginfoServer.keyCombinations(context,
                            // resultKey, getAppliesTo(item), 10);
                            // combinationsFromTaginfo.addAll(keyCombinationsFromTaginfo);

                            Map<String, String> combinations = new HashMap<>();

                            /*
                             * Turn the messy combination data in to something reasonable
                             */
                            for (String k : combinationsFromTaginfo) {
                                String[] s = k.split("=", 2);
                                if (s.length == 2) {
                                    String value = combinations.get(s[0]);
                                    if (value != null) {
                                        value = value + Preset.COMBO_DELIMITER + s[1];
                                    } else {
                                        value = s[1];
                                    }
                                    combinations.put(s[0], value);
                                } else {
                                    combinations.put(k, null);
                                }
                            }

                            for (Entry<String, String> entry : combinations.entrySet()) {
                                String key = entry.getKey();
                                String value = entry.getValue();
                                if (value != null) {
                                    PresetComboField field = (PresetComboField) item.addTag(false, key, PresetKeyType.COMBO, value, MatchType.KEY_VALUE);
                                    field.setEditable(true);
                                    field.setMatchType(MatchType.NONE);
                                } else {
                                    String[] s = key.split(":", 2);
                                    if (Tags.I18N_KEYS.contains(key) || (s.length == 2 && Tags.I18N_KEYS.contains(s[0]))) {
                                        // if a name key add without value
                                        item.addTag(false, key, PresetKeyType.TEXT, null, MatchType.NONE);
                                    } else if (!item.hasKey(key)) {
                                        // while we could add values from taginfo here, unluckily the results don't make
                                        // a lot of sense, using what we already have in the presets is likely the
                                        // better choice
                                        //
                                        // List<ValueResult> result = TaginfoServer.keyValues(context, key, 20);
                                        // Log.d(DEBUG_TAG, "values for key " + key + " " + result.size());
                                        // item.addTag(false, key, PresetKeyType.COMBO, result.toArray(new
                                        // ValueResult[result.size()]), Preset.COMBO_DELIMITER);
                                        PresetComboField field = (PresetComboField) item.addTag(false, key, PresetKeyType.COMBO,
                                                Preset.getAutocompleteValues(presets, null, key), Preset.COMBO_DELIMITER, MatchType.KEY_VALUE);
                                        field.setEditable(true);
                                    }
                                }
                            }

                            // finally add some hardwired keys, depending on the main key
                            Set<StringWithDescription> hardwiredKeys = HARDWIRED_KEYS.get(resultKey);
                            if (hardwiredKeys != null) {
                                for (StringWithDescription swd : hardwiredKeys) {
                                    String key = swd.getValue();
                                    String description = swd.getDescription();
                                    item.addTag(false, key, PresetKeyType.TEXT, Preset.getAutocompleteValues(presets, null, key), Preset.COMBO_DELIMITER,
                                            MatchType.NONE);
                                    if (description != null) {
                                        item.setHint(key, description);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        List<PresetElement> elements = group.getElements();
        if (elements.isEmpty()) {
            return preset;
        }
        // sort the results
        List<AutoPresetItem> items = new ArrayList<>();
        for (PresetElement pe : elements) {
            items.add((AutoPresetItem) pe);
        }
        elements.clear();
        Collections.sort(items, AutoPresetItem.COMPARATOR);
        Log.d(DEBUG_TAG, "found " + items.size() + " results");
        if (!items.isEmpty()) {
            items = items.subList(0, Math.min(items.size(), maxResults));
        }
        elements.addAll(items);

        return preset;
    }

    /**
     * Get the type of element the item applies to in a taginfo filter compatible way
     * 
     * @param item the PresetItem
     * @return null if it applies to multiple element types, a single type otherwise
     */
    @Nullable
    private String getAppliesTo(@NonNull AutoPresetItem item) {
        String result = null;
        if (item.appliesToNode) {
            result = TaginfoServer.NODES;
        }
        if (item.appliesToWay) {
            if (result != null) {
                return null;
            }
            result = TaginfoServer.WAYS;
        }
        if (item.appliesToRelation) {
            if (result != null) {
                return null;
            }
            result = TaginfoServer.RELATIONS;
        }
        if (item.appliesToArea) {
            return null;
        }
        return result;
    }

    /**
     * Check if we have an icon for a key
     * 
     * @param key the tag key value
     * @return true if we have a specific icon
     */
    private boolean haveIcon(@NonNull String key) {
        for (String icon : ICONSDEST) {
            if (icon.equals(key + PNG)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if this tag found by searching already exists in the Presets
     * 
     * @param sr the SearchResult to check
     * @return true if we already have this
     */
    private boolean existsInPresets(SearchResult sr) {
        String tag = sr.getKey() + "\t" + sr.getValue();
        for (Preset preset : presets) {
            if (preset != null) {
                Set<PresetItem> existingPresets = preset.getItemByTag(tag);
                if (!existingPresets.isEmpty()) {
                    Log.d(DEBUG_TAG, sr.toString() + " exists in Presets");
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Save a Preset to our public directory
     * 
     * @param context Android Context
     * @param preset the Preset to save
     */
    public static void save(@NonNull final Context context, @NonNull final Preset preset) {
        Logic logic = App.getLogic();
        ExecutorTask<Void, Void, Void> save = new ExecutorTask<Void, Void, Void>(logic.getExecutorService(), logic.getHandler()) {
            @Override
            protected Void doInBackground(Void param) throws IOException, XmlPullParserException {
                File outfile = FileUtil.openFileForWriting(context,
                        FileUtil.getPublicDirectory() + "/" + Paths.DIRECTORY_PATH_AUTOPRESET + "/" + Files.FILE_NAME_AUTOPRESET);
                try (FileOutputStream fout = new FileOutputStream(outfile); OutputStream out = new BufferedOutputStream(fout);) { // NOSONAR
                    Log.d(DEBUG_TAG, "Saving to " + outfile.getPath());
                    XmlSerializer s = XmlPullParserFactory.newInstance().newSerializer();
                    s.setFeature(INDENT_OUTPUT, true);
                    s.setOutput(out, OsmXml.UTF_8);
                    preset.toXml(s);
                    s.flush();
                }
                return null;
            }

            @Override
            protected void onBackgroundError(Exception e) {
                Log.e(DEBUG_TAG, "Preset saving failed with " + e.getMessage());
                ScreenMessage.toastTopError(context, context.getString(R.string.saving_preset_failed, e.getLocalizedMessage()));
            }
        };
        save.execute();
    }

    /**
     * Instantiate a Preset from the auto-preset files
     * 
     * @param context Android context
     * @param activePresets array of active presets
     * @param autopresetPosition the position we will add the auto preset
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public static void readAutoPreset(@NonNull Context context, @NonNull Preset[] activePresets, int autopresetPosition)
            throws ParserConfigurationException, SAXException, IOException, NoSuchAlgorithmException {
        String autopresetGroupName = context.getString(R.string.preset_autopreset);

        try {
            File autoPresetDir = FileUtil.getPublicDirectory(FileUtil.getPublicDirectory(), Paths.DIRECTORY_PATH_AUTOPRESET);
            File autoIcon = new File(autoPresetDir, AutoPreset.ICON);
            if (!autoIcon.exists()) {
                for (int i = 0; i < ICONS.length; i++) {
                    copyIcon(context, autoPresetDir, i);
                }
                // add .nomedia file to stop the directory being indexed
                (new File(autoPresetDir, ".nomedia")).createNewFile(); // NOSONAR
            }
        } catch (IOException e) {
            // don't fail because of an exception here
            Log.e(DEBUG_TAG, "Icon/file not found ", e);
        }
        activePresets[autopresetPosition] = new Preset(context, FileUtil.getPublicDirectory(FileUtil.getPublicDirectory(), Paths.DIRECTORY_PATH_AUTOPRESET),
                true);
        Preset autopreset = activePresets[autopresetPosition];
        PresetGroup group = autopreset.getGroupByName(autopresetGroupName);
        if (group == null) {
            new PresetGroup(autopreset, autopreset.getRootGroup(), autopresetGroupName, AutoPreset.ICON);
        }
    }

    /**
     * Copy icon at position i
     * 
     * @param context an Android Context
     * @param autoPresetDir the destination directory
     * @param i the icon position
     */
    private static void copyIcon(@NonNull Context context, @NonNull File autoPresetDir, int i) {
        try {
            FileUtil.copyFileFromAssets(context, "images/" + ICONS[i], autoPresetDir, ICONSDEST[i]);
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "Icon not found " + ICONS[i] + " " + e.getMessage());
        }
    }

    /**
     * Add an PresetItem to the auto preset
     * 
     * @param ctx an Android Context
     * @param item the PresetItem
     * @return true if successful
     */
    public static boolean addItemToAutoPreset(@NonNull Context ctx, @NonNull PresetItem item) {
        Preset[] configuredPresets = App.getCurrentPresets(ctx);
        int autopresetPosition = configuredPresets.length - 1;
        Preset preset = configuredPresets[autopresetPosition];
        if (preset == null) {
            // may happen during testing
            AdvancedPrefDatabase.createEmptyAutoPreset(ctx, configuredPresets, autopresetPosition);
            preset = configuredPresets[autopresetPosition];
        }
        if (preset != null) {
            PresetGroup group = preset.getGroupByName(ctx.getString(R.string.preset_autopreset));
            if (group != null) {
                @SuppressWarnings("unused")
                PresetItem newItem = new PresetItem(preset, group, item);
                AutoPreset.save(ctx, preset);
                return true;
            }
        }
        Log.e(DEBUG_TAG, "Preset null or group not found");
        return false;
    }
}
