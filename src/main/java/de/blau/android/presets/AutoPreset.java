package de.blau.android.presets;

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
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.contract.Files;
import de.blau.android.contract.Paths;
import de.blau.android.osm.OsmXml;
import de.blau.android.osm.Tags;
import de.blau.android.presets.Preset.MatchType;
import de.blau.android.presets.Preset.PresetElement;
import de.blau.android.presets.Preset.PresetGroup;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.presets.Preset.PresetKeyType;
import de.blau.android.taginfo.TaginfoServer;
import de.blau.android.taginfo.TaginfoServer.SearchResult;
import de.blau.android.taginfo.TaginfoServer.WikiPageResult;
import de.blau.android.util.FileUtil;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.collections.MultiHashMap;

public class AutoPreset {
    private static final String DEBUG_TAG = AutoPreset.class.getSimpleName();

    public static final String  ICON = "auto-preset.png";
    private static final String PNG  = ".png";

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
            "auto-preset-office.png", "auto-preset-military.png", "auto-preset-natural.png", "auto-preset-railway.png", "auto-preset-railway.png",
            "auto-preset-railway.png", "auto-preset-highway.png", "auto-preset-highway.png", "auto-preset-healthcare.png", "auto-preset-landuse.png",
            "auto-preset-waterway.png" };
    private static final String[] ICONSDEST = { AutoPreset.ICON, "amenity.png", "shop.png", "tourism.png", "leisure.png", "man_made.png", "building.png",
            "emergency.png", "craft.png", "office.png", "military.png", "natural.png", "railway.png", "aeroway.png", "aerialway.png", "highway.png",
            "barrier.png", "healthcare.png", "landuse.png", "waterway.png" };

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
     * @param maxResults maximum number of results (ignored)
     * @return a temporary Preset object
     */
    @NonNull
    public Preset fromTaginfo(@NonNull String term, int maxResults) {
        List<SearchResult> candidateTags = TaginfoServer.searchByKeyword(context, term, -1);

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
                // remove results with empty values
                // and presets that we already have
                if (sr.getValue() != null && !"".equals(sr.getValue()) && !existsInPresets(sr)) {
                    String resultKey = sr.getKey();
                    WikiPageResult wikiPage = TaginfoServer.wikiPage(context, resultKey, sr.getValue(), language, null);
                    if (wikiPage != null) {
                        SearchResult stats = TaginfoServer.tagStats(context, resultKey, sr.getValue());
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
                            Log.e(DEBUG_TAG, "adding " + resultKey + " " + sr.getValue());
                            item.addTag(resultKey, PresetKeyType.TEXT, sr.getValue(), null);

                            List<String> combinationsFromTaginfo = TaginfoServer.tagCombinations(context, resultKey, sr.getValue(), getAppliesTo(item), 10);
                            if (combinationsFromTaginfo != null) {
                                combinationsFromTaginfo.addAll(wikiPage.getCombinations());
                            } else {
                                combinationsFromTaginfo = wikiPage.getCombinations();
                            }
                            // these need to be added at the end or else we will overwrite values we have with better
                            // quality
                            // disabled for now as they seem to create too much noise
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
                                    item.addTag(false, key, PresetKeyType.COMBO, value);
                                    PresetComboField field = (PresetComboField) item.getField(key);
                                    field.editable = true;
                                    field.setMatchType(MatchType.NONE);
                                } else {
                                    String[] s = key.split(":", 2);
                                    if (Tags.I18N_NAME_KEYS.contains(key) || (s.length == 2 && Tags.I18N_NAME_KEYS.contains(s[0]))) {
                                        // if a name key add without value
                                        item.addTag(false, key, PresetKeyType.TEXT, null);
                                    } else if (!item.hasKey(key)) {
                                        // while we could add values from taginfo here, unluckily the results don't make
                                        // a lot of sense, using what we already have in the presets is likely the
                                        // better choice
                                        // List<ValueResult> result = TaginfoServer.keyValues(context, key, 20);
                                        // Log.d(DEBUG_TAG, "values for key " + key + " " + result.size());
                                        // item.addTag(false, key, PresetKeyType.COMBO, result.toArray(new
                                        // ValueResult[result.size()]), Preset.COMBO_DELIMITER);
                                        item.addTag(false, key, PresetKeyType.COMBO, Preset.getAutocompleteValues(presets, null, key), Preset.COMBO_DELIMITER);
                                        item.setEditable(key, true);
                                    }
                                }
                            }

                            // finally add some hardwired keys, depending on the main key
                            Set<StringWithDescription> hardwiredKeys = HARDWIRED_KEYS.get(resultKey);
                            if (hardwiredKeys != null) {
                                for (StringWithDescription swd : hardwiredKeys) {
                                    String key = swd.getValue();
                                    String description = swd.getDescription();
                                    item.addTag(false, key, PresetKeyType.TEXT, Preset.getAutocompleteValues(presets, null, key), Preset.COMBO_DELIMITER);
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
        Collections.sort(items);
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
            result = "nodes";
        }
        if (item.appliesToWay) {
            if (result != null) {
                result = "ways";
            } else {
                return null;
            }
        }
        if (item.appliesToRelation) {
            if (result != null) {
                result = "relations";
            } else {
                return null;
            }
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
                if (existingPresets != null && !existingPresets.isEmpty()) {
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
     * @param preset the Preset to save
     */
    public static void save(@NonNull final Preset preset) {
        AsyncTask<Void, Void, Void> save = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                FileOutputStream fout = null;
                OutputStream out = null;
                try {
                    File outfile = FileUtil
                            .openFileForWriting(FileUtil.getPublicDirectory() + "/" + Paths.DIRECTORY_PATH_AUTOPRESET + "/" + Files.FILE_NAME_AUTOPRESET);
                    Log.d(DEBUG_TAG, "Saving to " + outfile.getPath());
                    XmlSerializer s = XmlPullParserFactory.newInstance().newSerializer();
                    s.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                    fout = new FileOutputStream(outfile);
                    out = new BufferedOutputStream(fout);
                    s.setOutput(out, OsmXml.UTF_8);
                    preset.toXml(s);
                    s.flush();
                } catch (IllegalArgumentException | IllegalStateException | IOException | XmlPullParserException e) {
                    Log.e(DEBUG_TAG, "Saving failed with " + e.getMessage());
                } finally {
                    SavingHelper.close(out);
                    SavingHelper.close(fout);
                }

                return null;
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
                    try {
                        FileUtil.copyFileFromAssets(context, "images/" + ICONS[i], autoPresetDir, ICONSDEST[i]);
                    } catch (IOException e) {
                        Log.e(DEBUG_TAG, "Icon not found " + ICONS[i] + " " + e.getMessage());
                    }
                }
                // add .nomedia file to stop the directory being indexed
                (new File(autoPresetDir, ".nomedia")).createNewFile(); // NOSONAR
            }
        } catch (IOException e) {
            // don't fail because of an exception here
            Log.e(DEBUG_TAG, "Icon not found ", e);
        }
        activePresets[autopresetPosition] = new Preset(context, FileUtil.getPublicDirectory(FileUtil.getPublicDirectory(), Paths.DIRECTORY_PATH_AUTOPRESET),
                null, true);
        Preset autopreset = activePresets[autopresetPosition];
        PresetGroup group = autopreset.getGroupByName(autopresetGroupName);
        if (group == null) {
            autopreset.new PresetGroup(autopreset.getRootGroup(), autopresetGroupName, AutoPreset.ICON);
        }
    }
}
