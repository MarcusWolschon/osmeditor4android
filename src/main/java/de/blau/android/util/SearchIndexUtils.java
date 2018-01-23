package de.blau.android.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import android.content.Context;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.names.Names.NameAndTags;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.presets.Synonyms;
import de.blau.android.util.collections.MultiHashMap;

public class SearchIndexUtils {

    private static final String DEBUG_TAG       = "SearchIndex";
    private static Pattern      deAccentPattern = null;         // cached regex

    /**
     * normalize a string for the search index, currently only works for latin scripts
     * 
     * @param n String to normalize
     * @return normalized String
     */
    static public String normalize(String n) {
        String r = n.toLowerCase(Locale.US).trim();
        r = deAccent(r);
        
        StringBuilder b = new StringBuilder();
        for (char c : r.toCharArray()) {
            c = Character.toLowerCase(c);
            if (Character.isLetterOrDigit(c)) {
                b.append(c);
            } else if (Character.isWhitespace(c)) {
                if (b.length() > 0 && !Character.isWhitespace(b.charAt(b.length() - 1))) {
                    b.append(' ');
                }
            } else {
                switch (c) {
                case '&':
                case '/':
                case '_':
                case '.':
                    if (b.length() > 0 && !Character.isWhitespace(b.charAt(b.length() - 1))) {
                        b.append(' ');
                    }
                    break;
                case '\'':
                    break;
                }
            }
        }
        return b.toString();
    }

    /**
     * REmove accents from a string
     * 
     * @param str String to work on
     * @return String without accents
     */
    static private String deAccent(String str) {
        String nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD);
        if (deAccentPattern == null) {
            deAccentPattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        }
        return deAccentPattern.matcher(nfdNormalizedString).replaceAll("");
    }

    /**
     * Slightly fuzzy search in the synonyms and preset index for presets and return them, translated items first from
     * the preset index
     * 
     * @param ctx Android Context
     * @param term search term
     * @param type OSM object "type"
     * @param maxDistance maximum edit distance to return
     * @param limit max number of results
     * @return a List containing up to limit PresetItems found
     */
    public static List<PresetItem> searchInPresets(Context ctx, String term, ElementType type, int maxDistance, int limit) {
        term = SearchIndexUtils.normalize(term);
        // synonyms first
        Synonyms synonyms = App.getSynonyms(ctx);
        List<IndexSearchResult> rawResult = synonyms.search(ctx, term, type, maxDistance);

        List<MultiHashMap<String, PresetItem>> presetSeachIndices = new ArrayList<>();
        presetSeachIndices.add(App.getTranslatedPresetSearchIndex(ctx));
        presetSeachIndices.add(App.getPresetSearchIndex(ctx));

        for (MultiHashMap<String, PresetItem> index : presetSeachIndices) {
            for (String s : index.getKeys()) {
                int distance = s.indexOf(term);
                if (distance == -1) {
                    distance = OptimalStringAlignment.editDistance(s, term, maxDistance);
                } else {
                    distance = 0; // literal substring match, we don't want to weight this worse than a fuzzy match
                }
                if ((distance >= 0 && distance <= maxDistance)) {
                    Set<PresetItem> presetItems = index.get(s);
                    for (PresetItem pi : presetItems) {
                        if (type == null || pi.appliesTo(type)) {
                            IndexSearchResult isr = new IndexSearchResult(distance * presetItems.size(), pi);
                            rawResult.add(isr);
                        }
                    }
                }
            }
        }
        Collections.sort(rawResult);
        ArrayList<PresetItem> result = new ArrayList<>();
        for (IndexSearchResult i : rawResult) {
            Log.d(DEBUG_TAG, "found " + i.item.getName());
            if (!result.contains(i.item)) {
                result.add(i.item);
            }
        }
        if (!result.isEmpty()) {
            return result.subList(0, Math.min(result.size(), limit));
        }
        return result; // empty
    }

    /**
     * Return match is any of term in the name index
     * 
     * @param ctx
     * @param term
     * @param type
     * @param maxDistance
     * @return
     */
    public static NameAndTags searchInNames(Context ctx, String term, int maxDistance) {
        Map<String, NameAndTags> namesSearchIndex = App.getNameSearchIndex(ctx);
        NameAndTags result = null;
        int lastDistance = Integer.MAX_VALUE;
        term = SearchIndexUtils.normalize(term);
        for (Entry<String, NameAndTags> entry : namesSearchIndex.entrySet()) {
            int distance = OptimalStringAlignment.editDistance(entry.getKey(), term, maxDistance);
            if (distance >= 0 && distance <= maxDistance) {
                if (distance < lastDistance) {
                    result = entry.getValue();
                    lastDistance = distance;
                    if (distance == 0) { // no point in searching for better results
                        return result;
                    }
                }
            }
        }
        return result;
    }
}
