package de.blau.android.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.App;
import de.blau.android.nsi.Names.NameAndTags;
import de.blau.android.nsi.Names.TagMap;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.osm.Tags;
import de.blau.android.presets.Preset;
import de.blau.android.presets.PresetElement;
import de.blau.android.presets.PresetField;
import de.blau.android.presets.PresetFixedField;
import de.blau.android.presets.PresetItem;
import de.blau.android.presets.PresetKeyType;
import de.blau.android.util.collections.MultiHashMap;

public final class SearchIndexUtils {

    private static final String DEBUG_TAG       = "SearchIndex";
    private static Pattern      deAccentPattern = null;         // cached regex

    /**
     * Private constructor
     */
    private SearchIndexUtils() {
        // nothing
    }

    /**
     * normalize a string for the search index, currently only works for latin scripts
     * 
     * @param n String to normalize
     * @return normalized String
     */
    public static String normalize(@NonNull String n) {
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
                case '-':
                case ':':
                case '+':
                case ';':
                    if (b.length() > 0 && !Character.isWhitespace(b.charAt(b.length() - 1))) {
                        b.append(' ');
                    }
                    break;
                case '\'':
                    break;
                default:
                    // IGNORE
                }
            }
        }
        return b.toString();
    }

    /**
     * Remove accents from a string
     * 
     * @param str String to work on
     * @return String without accents
     */
    private static String deAccent(@NonNull String str) {
        String nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD);
        if (deAccentPattern == null) {
            deAccentPattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        }
        return deAccentPattern.matcher(nfdNormalizedString).replaceAll("");
    }

    /**
     * Slightly fuzzy search in the synonyms, preset index and name suggestion index for presets and return them
     * 
     * @param ctx Android Context
     * @param term search term
     * @param type OSM object "type"
     * @param maxDistance maximum edit distance to return
     * @param limit max number of results
     * @param regions current regions or null
     * @return a List containing up to limit PresetItems found
     */
    @NonNull
    public static List<PresetElement> searchInPresets(@NonNull Context ctx, @NonNull String term, @Nullable ElementType type, int maxDistance, int limit,
            @Nullable List<String> regions) {
        String country = GeoContext.getCountryIsoCode(regions);
        term = SearchIndexUtils.normalize(term);
        // synonyms first
        Map<IndexSearchResult, IndexSearchResult> rawResult = new HashMap<>();
        for (IndexSearchResult isr : App.getSynonyms(ctx).search(ctx, term, type, maxDistance)) {
            rawResult.put(isr, isr);
        }

        // search in presets
        List<MultiHashMap<String, PresetItem>> presetSeachIndices = new ArrayList<>();
        presetSeachIndices.add(App.getTranslatedPresetSearchIndex(ctx));
        presetSeachIndices.add(App.getPresetSearchIndex(ctx));

        Set<String> terms = new HashSet<>();
        terms.add(term);
        List<String> temp = Arrays.asList(term.split("\\s"));
        if (temp.size() > 1) {
            terms.addAll(temp);
        }

        for (MultiHashMap<String, PresetItem> index : presetSeachIndices) {
            for (String s : index.getKeys()) {
                for (String t : terms) {
                    int distance = s.indexOf(t);
                    if (distance == -1) {
                        distance = OptimalStringAlignment.editDistance(s, term, maxDistance);
                        if (distance == -1) { // way out
                            continue;
                        }
                    } else {
                        distance = 0; // literal substring match, we don't want to weight this worse than a fuzzy match
                    }
                    if (distance <= maxDistance) {
                        Set<PresetItem> presetItems = index.get(s);
                        int weight = distance * presetItems.size(); // if there are a lot of items for a term, penalize
                        for (PresetItem pi : presetItems) {
                            if ((type == null || pi.appliesTo(type)) && pi.appliesIn(country)) {
                                IndexSearchResult isr = new IndexSearchResult(rescale(term, weight, pi), pi);
                                addToResult(rawResult, isr.weight, isr);
                            }
                        }
                    }
                }
            }
        }

        // search in NSI
        if (App.getPreferences(ctx).nameSuggestionPresetsEnabled()) {
            MultiHashMap<String, NameAndTags> nsi = App.getNameSearchIndex(ctx);
            Set<String> names = nsi.getKeys();
            Preset[] presets = App.getCurrentPresets(ctx);
            Preset preset = Preset.dummyInstance();
            for (String name : names) {
                int distance = name.indexOf(term);
                if (distance == -1) {
                    distance = OptimalStringAlignment.editDistance(name, term, maxDistance);
                } else {
                    distance = 0;
                }
                if ((distance >= 0 && distance <= maxDistance)) {
                    Set<NameAndTags> nats = nsi.get(name);
                    for (NameAndTags nat : nats) {
                        if (nat.inUseIn(regions)) {
                            TagMap tags = nat.getTags();
                            PresetItem pi = Preset.findBestMatch(presets, tags, null, null, false, null);
                            PresetItem namePi = new PresetItem(preset, null, nat.getName(), pi == null ? null : pi.getIconpath(), null);

                            for (Entry<String, String> entry : tags.entrySet()) {
                                namePi.addTag(entry.getKey(), PresetKeyType.TEXT, entry.getValue(), null, null);
                            }
                            if (pi != null) {
                                Map<String, PresetField> fields = pi.getFields();
                                for (Entry<String, PresetField> entry : fields.entrySet()) {
                                    String key = entry.getKey();
                                    if (!tags.containsKey(key)) {
                                        namePi.addField(entry.getValue());
                                    }
                                }
                            }
                            IndexSearchResult isr = new IndexSearchResult(rescale(term, distance, namePi), namePi);
                            // penalize results that aren't shops etc
                            if (namePi.hasKey(Tags.KEY_MAN_MADE)) {
                                isr.weight += 5;
                            }
                            addToResult(rawResult, isr.weight, isr);
                        }
                    }
                }
            }
        }

        // sort and return results
        List<IndexSearchResult> tempResult = new ArrayList<>(rawResult.values());
        Collections.sort(tempResult, IndexSearchResult.weightComparator);
        List<PresetElement> result = new ArrayList<>();
        for (IndexSearchResult isr : tempResult) {
            result.add(isr.item);
        }

        Log.d(DEBUG_TAG, "found " + result.size() + " results");
        if (!result.isEmpty()) {
            return result.subList(0, Math.min(result.size(), limit));
        }
        return result;
    }

    /**
     * Add a search result to the results map, not adding duplicates but always using the result with the lowest weight
     * 
     * @param result a Map (for efficiency reasons not a List or a Set) containing the individual search results
     * @param weight the weight
     * @param isr the current search result
     */
    public static void addToResult(@NonNull Map<IndexSearchResult, IndexSearchResult> result, int weight, @NonNull IndexSearchResult isr) {
        IndexSearchResult tempIsr = result.get(isr);
        if (tempIsr != null) {
            if (tempIsr.weight > weight) {
                tempIsr.weight = weight;
            }
        } else {
            result.put(isr, isr);
        }
    }

    /**
     * Give exact and partial matches best positions
     * 
     * @param term the search term
     * @param weight original weight
     * @param pi the PresetItem
     * @return the new weight
     */
    private static int rescale(@NonNull String term, int weight, @NonNull PresetItem pi) {
        int actualWeight = weight;
        String name = SearchIndexUtils.normalize(pi.getName());
        String translatedName = SearchIndexUtils.normalize(pi.getTranslatedName());
        if (name.equals(term) || translatedName.equals(term) || checkPresetValues(term, pi)) {
            // exact name or value match
            actualWeight = weight - 20;
        } else if (term.length() >= 3) {
            int pos = translatedName.indexOf(term);
            if (pos == 0) { // starts with the term
                actualWeight = weight - 15;
            } else if (pos > 0) {
                actualWeight = weight - 10;
            }
        }
        return actualWeight;
    }

    /**
     * This checks for an exact match against one of the fixed preset values
     * 
     * @param term the search term
     * @param pi the PresetItem
     * @return true if there is an exact match
     */
    private static boolean checkPresetValues(@NonNull String term, @NonNull PresetItem pi) {
        Collection<PresetFixedField> fixedFields = pi.getFixedTags().values();
        for (PresetFixedField f : fixedFields) {
            if (SearchIndexUtils.normalize(f.getValue().getValue()).equals(term)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return match is any of term in the name index
     * 
     * @param ctx Android Context
     * @param name name we are searching for
     * @param maxDistance maximum distance in "edits" the result can be away from name
     * @return a NameAndTags object for the term
     */
    @Nullable
    public static NameAndTags searchInNames(@NonNull Context ctx, @NonNull String name, int maxDistance) {
        MultiHashMap<String, NameAndTags> namesSearchIndex = App.getNameSearchIndex(ctx);
        NameAndTags result = null;
        int lastDistance = Integer.MAX_VALUE;
        name = SearchIndexUtils.normalize(name);
        for (String key : namesSearchIndex.getKeys()) {
            int distance = OptimalStringAlignment.editDistance(key, name, maxDistance);
            if (distance >= 0 && distance <= maxDistance && distance < lastDistance) {
                Set<NameAndTags> list = namesSearchIndex.get(key);
                for (NameAndTags nt : list) {
                    if (result == null || nt.getCount() > result.getCount()) {
                        result = nt;
                    }
                }
                lastDistance = distance;
                if (distance == 0) { // no point in searching for better results
                    return result;
                }
            }
        }
        return result;
    }
}
