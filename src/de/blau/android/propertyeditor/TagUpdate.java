package de.blau.android.propertyeditor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Interface for updating key:value pairs in the TagEditor from other fragments via the activity
 */
abstract interface TagUpdate {
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
	abstract ArrayList<LinkedHashMap<String, String>> getUpdatedTags();
}

