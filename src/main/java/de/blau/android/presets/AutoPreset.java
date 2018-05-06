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
import android.util.Log;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.contract.Files;
import de.blau.android.contract.Paths;
import de.blau.android.contract.Urls;
import de.blau.android.osm.Tags;
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

    public static final String ICON = "auto-preset.png";

    private static final MultiHashMap<String, StringWithDescription> HARDWIRED_KEYS = new MultiHashMap<>();
    static {
        StringWithDescription[] standardStuff = new StringWithDescription[] { new StringWithDescription(Tags.KEY_OPENING_HOURS, "Opening Hours"),
                new StringWithDescription(Tags.KEY_WHEELCHAIR, "Wheelchairs"), new StringWithDescription(Tags.KEY_LEVEL, "Level") };
        HARDWIRED_KEYS.add(Tags.KEY_SHOP, standardStuff);
        HARDWIRED_KEYS.add(Tags.KEY_AMENITY, standardStuff);
        HARDWIRED_KEYS.add(Tags.KEY_LEISURE, standardStuff);
    }

    private final Context  context;
    private final Preset[] presets;
    private final String   language;

    public AutoPreset(Context context) {
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
    public Preset fromTaginfo(String term, int maxResults) {
        List<SearchResult> candidateTags = TaginfoServer.searchByKeyword(context, term, -1);

        Preset preset = new Preset();
        PresetGroup group = preset.new PresetGroup(null, "", null);
        preset.setRootGroup(group);
        if (candidateTags != null) {
            for (SearchResult sr : candidateTags) {
                // remove results with empty values
                // and presets that we already have
                if (sr.value != null && !"".equals(sr.value) && !existsInPresets(sr)) {
                    WikiPageResult wikiPage = TaginfoServer.wikiPage(context, sr.key, sr.value, language, null);
                    if (wikiPage != null) {
                        SearchResult stats = TaginfoServer.tagStats(context, sr.key, sr.value);
                        if (stats != null) {
                            Log.d(DEBUG_TAG, "Creating PresetItem for " + wikiPage);
                            AutoPresetItem item = new AutoPresetItem(preset, group, sr.key + " " + sr.value, sr.key + "_empty.png",
                                    null, stats.count);
                            String title = wikiPage.titleOther; // fallback
                            if (wikiPage.title != null) { // local language
                                title = wikiPage.title;
                            } else if (wikiPage.titleEN != null) {
                                title = wikiPage.titleEN;
                            }
                            item.setMapFeatures(Urls.OSM_WIKI + title);
                            if (wikiPage.onNode) {
                                item.setAppliesToNode();
                            }
                            if (wikiPage.onWay) {
                                item.setAppliesToWay();
                            }
                            if (wikiPage.onArea) {
                                item.setAppliesToArea();
                            }
                            if (wikiPage.onRelation) {
                                item.setAppliesToRelation();
                            }
                            Log.e(DEBUG_TAG, "adding " + sr.key + " " + sr.value);
                            item.addTag(sr.key, PresetKeyType.TEXT, sr.value, null);

                            List<String> combinationsFromTaginfo = TaginfoServer.tagCombinations(context, sr.key, sr.value, 10);
                            if (combinationsFromTaginfo != null) {
                                combinationsFromTaginfo.addAll(wikiPage.combinations);
                            } else {
                                combinationsFromTaginfo = wikiPage.combinations;
                            }

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
                                    item.addTag(false, key, PresetKeyType.TEXT, value);
                                    item.setEditable(key, true);
                                } else {
                                    String[] s = key.split(":", 2);
                                    if (Tags.I18N_NAME_KEYS.contains(key) || (s.length == 2 && Tags.I18N_NAME_KEYS.contains(s[0]))) {
                                        // if a name key add without value
                                        item.addTag(false, key, PresetKeyType.TEXT, null);
                                    } else if (!item.hasKey(key)) {
                                        // while we could add values from taginfo here, unluckily the results don't make
                                        // a lot of sense
                                        // List<ValueResult> result = TaginfoServer.keyValues(context, key, 20);
                                        // Log.d(DEBUG_TAG, "values for key " + key + " " + result.size());
                                        // item.addTag(false, key, PresetKeyType.COMBO, result.toArray(new
                                        // ValueResult[result.size()]), Preset.COMBO_DELIMITER);
                                        item.addTag(false, key, PresetKeyType.TEXT, null);
                                        item.setEditable(key, true);
                                    }
                                }
                            }

                            // finally add some hardwired keys, depending on the main key
                            Set<StringWithDescription> hardwiredKeys = HARDWIRED_KEYS.get(sr.key);
                            if (hardwiredKeys != null) {
                                for (StringWithDescription swd : hardwiredKeys) {
                                    String key = swd.getValue();
                                    String description = swd.getDescription();
                                    item.addTag(false, key, PresetKeyType.TEXT, Preset.getAutocompleteValues(presets, null, key), Preset.COMBO_DELIMITER);
                                    if (description != null) {
                                        item.addHint(key, description);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // sort the results
        List<AutoPresetItem> items = new ArrayList<>();
        List<PresetElement> elements = group.getElements();
        for (PresetElement pe : elements) {
            items.add((AutoPresetItem) pe);
        }
        elements.clear();
        Collections.sort(items);
        elements.addAll(items);
        elements.add(0, preset.new PresetSeparator(group));
        return preset;
    }

    /**
     * Check if this tag found by searching already exists in the Presets
     * 
     * @param sr the SearchResult to check
     * @return true if we already have this
     */
    private boolean existsInPresets(SearchResult sr) {
        String tag = sr.key + "\t" + sr.value;
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
    public static void save(final Preset preset) {
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
                    s.setOutput(out, "UTF-8");
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
    public static void readAutoPreset(Context context, Preset[] activePresets, int autopresetPosition)
            throws ParserConfigurationException, SAXException, IOException, NoSuchAlgorithmException {
        String autopresetGroupName = context.getString(R.string.preset_autopreset);

        try {
            File autoIcon = new File(FileUtil.getPublicDirectory(FileUtil.getPublicDirectory(), Paths.DIRECTORY_PATH_AUTOPRESET), AutoPreset.ICON);
            if (!autoIcon.exists()) {
                FileUtil.copyFileFromAssets(context, "images/auto-preset.png",
                        FileUtil.getPublicDirectory(FileUtil.getPublicDirectory(), Paths.DIRECTORY_PATH_AUTOPRESET), AutoPreset.ICON);
                FileUtil.copyFileFromAssets(context, "images/icons/png/amenity_empty.png",
                        FileUtil.getPublicDirectory(FileUtil.getPublicDirectory(), Paths.DIRECTORY_PATH_AUTOPRESET), "amenity_empty.png");
                FileUtil.copyFileFromAssets(context, "images/icons/png/shop_empty.png",
                        FileUtil.getPublicDirectory(FileUtil.getPublicDirectory(), Paths.DIRECTORY_PATH_AUTOPRESET), "shop_empty.png");
                FileUtil.copyFileFromAssets(context, "images/icons/png/tourism_empty.png",
                        FileUtil.getPublicDirectory(FileUtil.getPublicDirectory(), Paths.DIRECTORY_PATH_AUTOPRESET), "tourism_empty.png");
                FileUtil.copyFileFromAssets(context, "images/icons/png/man_made_empty.png",
                        FileUtil.getPublicDirectory(FileUtil.getPublicDirectory(), Paths.DIRECTORY_PATH_AUTOPRESET), "man_made_empty.png");
            }
        } catch (IOException e) {
            // don't fail because of an exception here
            Log.e(DEBUG_TAG, "Icon not found ", e);
        }
        activePresets[autopresetPosition] = new Preset(context, FileUtil.getPublicDirectory(FileUtil.getPublicDirectory(), Paths.DIRECTORY_PATH_AUTOPRESET),
                null);
        Preset autopreset = activePresets[autopresetPosition];
        PresetGroup group = autopreset.getGroupByName(autopresetGroupName);
        if (group == null) {
            autopreset.new PresetGroup(autopreset.getRootGroup(), autopresetGroupName, AutoPreset.ICON);
        }
    }
}
