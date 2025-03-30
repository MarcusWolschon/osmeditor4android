package de.blau.android.util;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import de.blau.android.presets.PresetTagField;
import de.blau.android.util.collections.MultiHashMap;

public final class SearchIndexUtils {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, SearchIndexUtils.class.getSimpleName().length());
    private static final String DEBUG_TAG = SearchIndexUtils.class.getSimpleName().substring(0, TAG_LEN);

    private static final Pattern DEACCENT_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private static final int OFFSET_MATCH_SUBSTRING             = 10;
    private static final int OFFSET_MATCH_START                 = 15;
    private static final int OFFSET_EXACT_MATCH_WITHOUT_ACCENTS = 20;
    private static final int OFFSET_EXACT_MATCH_WITH_ACCENTS    = 30;
    private static final int MAN_MADE_PENALTY                   = 5;

    /**
     * Private constructor
     */
    private SearchIndexUtils() {
        // nothing
    }

    /**
     * normalize a string for the search index, currently only works fully for latin scripts
     * 
     * @param n String to normalize
     * @return normalized String
     */
    public static String normalize(@NonNull String n) {
        return deAccent(replacePunctuation(n.trim()));
    }

    /**
     * Replace any punctuation and similar chars with whitespace
     * 
     * @param input the input String
     * @return a String with punctuation replaced
     */
    @NonNull
    private static String replacePunctuation(@NonNull String input) {
        StringBuilder b = new StringBuilder();
        for (char c : input.toCharArray()) {
            c = Character.toLowerCase(c);
            if (Character.isLetterOrDigit(c)) {
                b.append(c);
            } else if (Character.isWhitespace(c)) {
                appendSpace(b);
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
                case ',':
                case '\u00A0':
                case '\u200A':
                case '\u2009':
                case '\u2008':
                case '\u2002':
                case '\u2007':
                case '\u3000':
                case '\u2003':
                case '\u2006':
                case '\u2005':
                case '\u2004':
                case '\u2013':
                case '\u2014':
                    appendSpace(b);
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
     * Append a space to a StringBuilder if it doesn't already end with one
     * 
     * @param b the StringBuilder
     */
    private static void appendSpace(@NonNull StringBuilder b) {
        final int length = b.length();
        if (length > 0 && !Character.isWhitespace(b.charAt(length - 1))) {
            b.append(' ');
        }
    }

    /**
     * Remove accents from a string
     * 
     * @param str String to work on
     * @return String without accents
     */
    private static String deAccent(@NonNull String str) {
        String nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD);
        return removeDiacriticalMarks(nfdNormalizedString);
    }

    /**
     * Remove diacritical marks from a NFD normalized input string
     * 
     * @param nfdNormalizedString the input string
     * @return a String with the diacritical marks removed
     */
    @NonNull
    private static String removeDiacriticalMarks(@NonNull String nfdNormalizedString) {
        return DEACCENT_PATTERN.matcher(nfdNormalizedString).replaceAll("");
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
        term = Normalizer.normalize(replacePunctuation(term), Normalizer.Form.NFD); // minimal normalization
        String normalizedTerm = normalize(term);
        // synonyms first
        Map<IndexSearchResult, IndexSearchResult> rawResult = new HashMap<>();
        for (IndexSearchResult isr : App.getSynonyms(ctx).search(ctx, normalizedTerm, type, maxDistance)) {
            rawResult.put(isr, isr);
        }

        // search in presets
        List<MultiHashMap<String, PresetItem>> presetSeachIndices = new ArrayList<>();
        presetSeachIndices.add(App.getTranslatedPresetSearchIndex(ctx));
        presetSeachIndices.add(App.getPresetSearchIndex(ctx));

        Set<String> terms = new HashSet<>();
        terms.add(normalizedTerm);
        List<String> temp = Arrays.asList(normalizedTerm.split("\\s"));
        if (temp.size() > 1) {
            terms.addAll(temp);
        }

        for (MultiHashMap<String, PresetItem> index : presetSeachIndices) {
            for (String s : index.getKeys()) {
                for (String t : terms) {
                    int distance = s.indexOf(t);
                    if (distance == -1) {
                        distance = OptimalStringAlignment.editDistance(s, normalizedTerm, maxDistance);
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
                                IndexSearchResult isr = new IndexSearchResult(rescale(term, normalizedTerm, weight, pi), pi);
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
                int distance = name.indexOf(normalizedTerm);
                if (distance == -1) {
                    distance = OptimalStringAlignment.editDistance(name, normalizedTerm, maxDistance);
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
                                namePi.addFixedTag(entry.getKey(), entry.getValue(), null, null);
                            }
                            if (pi != null) {
                                Map<String, PresetField> fields = pi.getFields();
                                for (Entry<String, PresetField> entry : fields.entrySet()) {
                                    final PresetField value = entry.getValue();
                                    if (value instanceof PresetTagField) {
                                        String key = entry.getKey();
                                        if (!tags.containsKey(key)) {
                                            namePi.addField(value);
                                        }
                                    }
                                }
                            }
                            IndexSearchResult isr = new IndexSearchResult(rescale(term, normalizedTerm, distance, namePi), namePi);
                            // penalize results that aren't shops etc
                            if (namePi.hasKey(Tags.KEY_MAN_MADE)) {
                                isr.weight += MAN_MADE_PENALTY;
                            }
                            addToResult(rawResult, isr.weight, isr);
                        }
                    }
                }
            }
        }

        // sort and return results
        List<IndexSearchResult> tempResult = new ArrayList<>(rawResult.values());
        Collections.sort(tempResult, IndexSearchResult.WEIGHT_COMPARATOR);
        List<PresetElement> result = new ArrayList<>();
        final int size = Math.min(tempResult.size(), limit);
        for (int i = 0; i < size; i++) {
            result.add(tempResult.get(i).item);
        }
        Log.d(DEBUG_TAG, "found " + size + " results");
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
     * As we need to retain accents etc for exact match we split up the individual steps of normalization here
     * 
     * @param originalTerm the original search term
     * @param normalizedTerm the normalized search term
     * @param weight original weight
     * @param pi the PresetItem
     * 
     * @return the new weight
     */
    private static int rescale(@NonNull String originalTerm, @NonNull String normalizedTerm, int weight, @NonNull PresetItem pi) {
        int actualWeight = weight;
        String name = Normalizer.normalize(replacePunctuation(pi.getName()), Normalizer.Form.NFD);
        String translatedName = Normalizer.normalize(replacePunctuation(pi.getTranslatedName()), Normalizer.Form.NFD);
        if (name.equals(originalTerm) || translatedName.equals(originalTerm)) {
            // exact name match with accents
            actualWeight = -OFFSET_EXACT_MATCH_WITH_ACCENTS;
        } else {
            name = removeDiacriticalMarks(name);
            translatedName = removeDiacriticalMarks(translatedName);
            if (name.equals(normalizedTerm) || translatedName.equals(normalizedTerm) || checkPresetValues(normalizedTerm, pi)) {
                // exact name or value match
                actualWeight = weight - OFFSET_EXACT_MATCH_WITHOUT_ACCENTS;
            } else if (normalizedTerm.length() >= 3) {
                int pos = translatedName.indexOf(normalizedTerm);
                if (pos == 0) { // starts with the term
                    actualWeight = weight - OFFSET_MATCH_START;
                } else if (pos > 0) {
                    actualWeight = weight - OFFSET_MATCH_SUBSTRING;
                }
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
            if (normalize(f.getValue().getValue()).equals(term)) {
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
        name = normalize(name);
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
