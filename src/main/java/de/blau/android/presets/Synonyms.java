package de.blau.android.presets;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.google.gson.stream.JsonReader;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.App;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.util.IndexSearchResult;
import de.blau.android.util.OptimalStringAlignment;
import de.blau.android.util.SearchIndexUtils;
import de.blau.android.util.collections.MultiHashMap;

/**
 * Support for using synonym lists retrieved from iDs repo
 * 
 * @author simon
 *
 */
public class Synonyms {
    static final String DEBUG_TAG = "Synonyms";

    private static final String SYNONYMS_PREFIX = "synonyms/synonyms.";
    private static final String SYNONYMS_EN     = "synonyms/synonyms.en";

    private MultiHashMap<String, String> synonyms = new MultiHashMap<>(false); // names -> tags

    /**
     * Construct a new instance
     * 
     * @param ctx an Android Context
     */
    public Synonyms(@NonNull Context ctx) {
        Log.d(DEBUG_TAG, "Parsing configuration files");
        AssetManager assetManager = ctx.getAssets();
        InputStream is = null;
        Locale locale = Locale.getDefault();
        Log.d(DEBUG_TAG, "Locale " + locale);
        try {
            is = assetManager.open(SYNONYMS_PREFIX + locale);
        } catch (IOException ioex) {
            try {
                is = assetManager.open(SYNONYMS_PREFIX + locale.getLanguage());
            } catch (IOException ioex2) {
                Log.d(DEBUG_TAG, "No synonym file found for " + locale + " or " + locale.getLanguage());
            }
        }
        parse(is);

        // always add English synonyms
        try {
            is = assetManager.open(SYNONYMS_EN);
            parse(is);
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "Reading " + SYNONYMS_EN + " failed " + e.getMessage());
        }
    }

    /**
     * Read and parse a synonym file from an InputStream
     * 
     * @param is the InputStream
     */
    public void parse(@Nullable InputStream is) {
        if (is != null) {
            try (JsonReader reader = new JsonReader(new InputStreamReader(is))) {
                String presetName = null;
                reader.beginObject();
                while (reader.hasNext()) {
                    presetName = reader.nextName(); // landuse/military/bunker
                    try {
                        reader.beginArray();
                        while (reader.hasNext()) { // synonyms
                            String synonym = reader.nextString();
                            if (synonym != null && !"".equals(synonym)) {
                                synonyms.add(SearchIndexUtils.normalize(synonym), presetName);
                            }
                        }
                        reader.endArray(); // key
                    } catch (IOException e) {
                        // this is not documented, but it seems to work to simply continue
                        Log.e(DEBUG_TAG, "reading synonyms array " + e.getMessage());
                    }
                }
                reader.endObject();
            } catch (IOException e) {
                Log.e(DEBUG_TAG, "reading synonyms " + e.getMessage());
            }
        }
    }

    /**
     * Try to find term in the list of synonyms
     * 
     * @param ctx Android Context
     * @param term the normalized (with SearchIndexUtils.normalize) search term
     * @param type type of OsmElement the PresetItem should apply to
     * @param maxDistance maxDistance for fuzzy matching
     * @return List containing the found PresetItems
     */
    @NonNull
    public List<IndexSearchResult> search(@NonNull Context ctx, @NonNull String term, @Nullable ElementType type, int maxDistance) {
        Log.d(DEBUG_TAG, "Searching for " + term + " type " + type);
        Map<IndexSearchResult, IndexSearchResult> result = new HashMap<>();
        Preset[] presets = App.getCurrentPresets(ctx);
        for (String s : synonyms.getKeys()) {
            int distance = s.indexOf(term);
            if (distance == -1) {
                distance = OptimalStringAlignment.editDistance(s, term, maxDistance);
            } else {
                distance = 0; // literal substring match, we don't want to weight this worse than a fuzzy match
            }
            if ((distance >= 0 && distance <= maxDistance)) {
                Set<String> presetNames = synonyms.get(s);
                for (String presetName : presetNames) {
                    String[] parts = presetName.split("/");
                    Set<PresetItem> items = new HashSet<>();
                    int len = parts.length;
                    if (len == 1) {
                        items.addAll(getPresetItems(type, presets, parts[0] + "\t"));
                    } else if (len >= 2) {
                        items.addAll(getPresetItems(type, presets, parts[0] + "\t" + parts[1]));
                        if (len > 2) {
                            items.addAll(getPresetItems(type, presets, parts[len - 2] + "\t" + parts[len - 1]));
                        }
                    }
                    for (PresetItem pi : items) {
                        IndexSearchResult isr = new IndexSearchResult(distance, pi);
                        SearchIndexUtils.addToResult(result, distance, isr);
                    }
                }
            }
        }
        return new ArrayList<>(result.values());
    }

    /**
     * Get the preset items for a tag or key
     * 
     * @param type the element type or null for all
     * @param presets the currently configured presets
     * @param presetKey the tag or key we are looking for
     * @return a Set of PresetItem
     */
    @NonNull
    public Set<PresetItem> getPresetItems(@Nullable ElementType type, @NonNull Preset[] presets, @NonNull String presetKey) {
        Set<PresetItem> result = new HashSet<>();
        for (Preset preset : presets) {
            if (preset != null) {
                Set<PresetItem> items = preset.getItemByTag(presetKey);
                if (items != null) {
                    for (PresetItem pi : items) {
                        if (!pi.isDeprecated() && (type == null || pi.appliesTo(type))) {
                            result.add(pi);
                        }
                    }
                }
            }
        }
        return result;
    }
}
