package de.blau.android.presets;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.google.gson.stream.JsonReader;

import android.content.Context;
import android.content.res.AssetManager;
import android.support.annotation.NonNull;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.util.IndexSearchResult;
import de.blau.android.util.OptimalStringAlignment;
import de.blau.android.util.SavingHelper;
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

    private MultiHashMap<String, String> synonyms = new MultiHashMap<>(false); // names -> tags

    public Synonyms(Context ctx) {
        Log.d(DEBUG_TAG, "Parsing configuration files");
        AssetManager assetManager = ctx.getAssets();
        InputStream is = null;
        Locale locale = Locale.getDefault();
        Log.d(DEBUG_TAG, "Locale " + locale);
        try {
            is = assetManager.open("synonyms/synonyms." + locale);
        } catch (IOException ioex) {
            try {
                is = assetManager.open("synonyms/synonyms." + locale.getLanguage());
            } catch (IOException ioex2) {
                Log.d(DEBUG_TAG, "No synonym file found for " + locale + " or " + locale.getLanguage());
            }
        }

        if (is != null) {
            JsonReader reader = null;
            try {
                reader = new JsonReader(new InputStreamReader(is));

                String presetName = null;
                reader.beginObject();
                while (reader.hasNext()) {
                    presetName = reader.nextName(); // landuse/military/bunker
                    reader.beginArray();
                    while (reader.hasNext()) { // synonyms
                        String synonym = reader.nextString();
                        if (synonym != null && !"".equals(synonym)) {
                            synonyms.add(SearchIndexUtils.normalize(synonym), presetName);
                        }
                    }
                    reader.endArray(); // key
                }
                reader.endObject();
            } catch (IOException e) {
                Log.e(DEBUG_TAG, e.getMessage());
            } finally {
                SavingHelper.close(reader);
                SavingHelper.close(is);
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
    public List<IndexSearchResult> search(@NonNull Context ctx, @NonNull String term, @NonNull ElementType type, int maxDistance) {
        Log.d(DEBUG_TAG, "Searching for " + term + " type " + type);
        List<IndexSearchResult> result = new ArrayList<>();
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
                    String presetKey = parts[0] + "\t";
                    int len = parts.length;
                    if (len >= 2) {
                        presetKey = parts[len - 2] + "\t" + parts[len - 1];
                    }
                    for (Preset preset : presets) {
                        Set<PresetItem> items = preset.getItemByTag(presetKey);
                        if (items != null) {
                            for (PresetItem pi : items) {
                                if (!pi.isDeprecated() && (type == null || pi.appliesTo(type))) {
                                    IndexSearchResult isr = new IndexSearchResult(distance * items.size(), pi);
                                    result.add(isr);
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }
}
