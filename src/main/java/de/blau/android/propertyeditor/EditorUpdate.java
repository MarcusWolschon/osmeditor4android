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
     * @param key
     * @param value
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
     * @return
     */
    LinkedHashMap<String, String> getKeyValueMapSingle(final boolean allowBlanks);

    /**
     * Revert to original tags
     */
    void revertTags();

    /**
     * delete tag
     */
    void deleteTag(final String key);

    /**
     * Get the best matching preset
     * 
     * @return
     */
    PresetItem getBestPreset();

    /**
     * Get all matching secondary presets (without linked presets)
     * 
     * @return
     */
    List<PresetItem> getSecondaryPresets();

    /**
     * Get all the matching presets
     * 
     * @return
     */
    Map<String, PresetItem> getAllPresets();

    /**
     * Trigger an update of the presets
     */
    void updatePresets();

    /**
     * generate best address tags
     */
    void predictAddressTags(boolean allowBlanks);

    /**
     * Apply tag suggestion from name index
     * 
     * @param tags
     */
    void applyTagSuggestions(Names.TagMap tags);

    /*
     * Copy/Cut/Paste related stuff
     */
    /**
     * @return true if we have something in out internal clipboard
     */
    boolean pasteIsPossible();

    /**
     * 
     * @param replace currently unsed
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
     * Copy tags to clipboard
     * 
     * @param tags
     */
    void copyTags(Map<String, String> tags);

    /**
     * Apply a preset
     * 
     * @param preset the preset
     * 
     * @param addOptional add optional tags
     */
    void applyPreset(PresetItem preset, boolean addOptional);

}
