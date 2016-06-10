package de.blau.android.propertyeditor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.blau.android.names.Names;
import de.blau.android.presets.Preset.PresetItem;

/**
 * Interface for updating key:value pairs in the TagEditor from other fragments via the activity
 */
abstract interface EditorUpdate {
	/**
	 * Update or add a single key value tupel in the tag editor
	 * @param key
	 * @param value
	 */
	abstract void updateSingleValue(final String key, final String value);

	/**
	 * Update or add multiple keys
	 * @param tags map containing the new key - value tupels
	 * @param flush if true delete all existing tags before appliying the update
	 */
	abstract void updateTags(final Map<String,String>tags,final boolean flush);
	
	/**
	 * Get tags from tag editor
	 * @return
	 */
	abstract LinkedHashMap<String, String> getKeyValueMapSingle(final boolean allowBlanks);
	
	/**
	 * Revert to original tags
	 */
	abstract void revertTags();
	
	/**
	 * delete tag
	 */
	abstract void deleteTag(final String key);
	
	/**
	 * Get the best matching preset
	 * @return
	 */
	abstract PresetItem getBestPreset();	
	
	/**
	 * Get all matching secondary presets (without linked presets)
	 * @return
	 */
	abstract List<PresetItem> getSecondaryPresets();
	
	/**
	 * Get all the matching presets
	 * @return
	 */
	abstract Map<String,PresetItem> getAllPresets();
	
	/**
	 * generate best address tags
	 */
	abstract void predictAddressTags(boolean allowBlanks);
	
	/**
	 * Apply tag suggestion from name index
	 * @param tags
	 */
	abstract void applyTagSuggestions(Names.TagMap tags);
}

