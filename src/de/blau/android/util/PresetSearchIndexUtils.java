package de.blau.android.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import de.blau.android.Application;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.presets.ValueWithCount;
import de.blau.android.presets.Preset.PresetItem;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

public class PresetSearchIndexUtils {
	
	
	private static Pattern deAccentPattern = null; // cached regex
	
	/**
	 * normalize a string for the search index, currently only works for latin scripts
	 * @param n
	 * @return
	 */
	static public String normalize(String n) {
		String r = n.toLowerCase().trim();
		r = deAccent(r);
		StringBuilder b = new StringBuilder();
		for (char c:r.toCharArray()) {
			c = Character.toLowerCase(c);
			if (Character.isLetterOrDigit(c)) {
				b.append(c);
			} else if (Character.isWhitespace(c)) {
				if (b.length() > 0 && !Character.isWhitespace(b.charAt(b.length()-1))) {
					b.append(' ');
				}
			} else {
				switch (c) {
				case '&':
				case '/':
				case '_': 
				case '.': if (b.length() > 0 && !Character.isWhitespace(b.charAt(b.length()-1))) {
							b.append(' ');
						  }; 
						break;
				case '\'': ; break;		
				}
			}
		}
		return b.toString();
	}
	
	@SuppressLint("NewApi")
	static private String deAccent(String str) {
	    String nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD); 
	    if (deAccentPattern  == null) {
	    	deAccentPattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
	    }
	    return deAccentPattern.matcher(nfdNormalizedString).replaceAll("");
	}

	
	/**
	 * Slightly fuzzy search in the preset index for presets and return them
	 * @param ctx
	 * @param term search term
	 * @param type OSM object "type"
	 * @param maxDistance maximum edit distance to return
	 * @param limit max number of results
	 * @return
	 */
	public static List<PresetItem> search(Context ctx, String term, ElementType type, int maxDistance, int limit) {
		MultiHashMap<String, PresetItem> presetSeachIndex = Application.getPresetSearchIndex(ctx);
		TreeSet<IndexSearchResult> sortedResult = new TreeSet<IndexSearchResult>();
		term = PresetSearchIndexUtils.normalize(term);
		for (String s:presetSeachIndex.getKeys()) {
			int distance = OptimalStringAlignment.editDistance(s, term, maxDistance);
			if (distance >= 0 && distance <= maxDistance) {
				Set<PresetItem> presetItems = presetSeachIndex.get(s);
				for (PresetItem pi:presetItems) {
					if (type == null || pi.appliesTo(type)) {
						IndexSearchResult isr = new IndexSearchResult();
						isr.count = distance * presetItems.size();
						isr.item = pi;
						sortedResult.add(isr);
					}
				}
			}
		}
		ArrayList<PresetItem>result = new ArrayList<PresetItem>();
		for (IndexSearchResult i:sortedResult) {
			Log.d("SearchIndex","found " + i.item.getName());
			if (!result.contains(i.item)) {
				result.add(i.item);
			}
		}
		if (result.size() > 0) {
			return result.subList(0, Math.min(result.size(),limit)-1);
		}
		return result; // empty
	}
}
