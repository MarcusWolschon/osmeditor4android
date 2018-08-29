package de.blau.android.propertyeditor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.blau.android.names.Names;
import de.blau.android.presets.Preset.PresetItem;

/**
 * Interface for updating key:value pairs in the TagEditor from other fragments via the activity
 */
interface EditorUpdate {
    /**
     * Update or add a single key value pair in the tag editor
     * 
     * @param key the key
     * @param value the value
     */
    void updateSingleValue(final String key, final String value);

    /**
     * Update or add multiple keys
     * 
     * @param tags map containing the new key - value pais
     * @param flush if true delete all existing tags before applying the update
     */
    void updateTags(final Map<String, String> tags, final boolean flush);

    /**
     * Get tags from tag editor
     * 
     * @param allowBlanks allow blank values
     * @return a LinkedHashMap of the tags
     */
    LinkedHashMap<String, String> getKeyValueMapSingle(final boolean allowBlanks);

    /**
     * Revert to original tags
     */
    void revertTags();

    /**
     * delete tag
     * 
     * @param key the tag key
     */
    void deleteTag(final String key);

    /**
     * Get the best matching preset
     * 
     * @return the best matching PresetItem
     */
    PresetItem getBestPreset();

    /**
     * Get all matching secondary presets (without linked presets)
     * 
     * @return a List of the secondary PresetItems
     */
    List<PresetItem> getSecondaryPresets();

    /**
     * Get all the matching presets
     * 
     * @return a Map containing a key PresetItem mapping
     */
    Map<String, PresetItem> getAllPresets();

    /**
     * Trigger an update of the presets
     */
    void updatePresets();

    /**
     * generate best address tags
     * 
     * @param allowBlanks allow blank values
     */
    void predictAddressTags(boolean allowBlanks);

    /**
     * Apply tag suggestion from name index
     * 
     * @param tags a map with the tags
     */
    void applyTagSuggestions(Names.TagMap tags);

    /*
     * Copy/Cut/Paste related stuff
     */

    /**
     * 
     * @param replace currently unused
     * @return true if something was pasted
     */
    boolean paste(boolean replace);

    /**
     * @return true if the system clipboard contains text
     */
    boolean pasteFromClipboardIsPossible();

    /**
     * 
     * @param replace currently unsed
     * @return true if something was pasted
     */
    boolean pasteFromClipboard(boolean replace);

    /**
     * Apply a preset
     * 
     * @param preset the preset
     * 
     * @param addOptional add optional tags
     */
    void applyPreset(PresetItem preset, boolean addOptional);

}
