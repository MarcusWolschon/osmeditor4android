package de.blau.android.presets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xmlpull.v1.XmlSerializer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ch.poole.poparser.Po;

public class PresetCheckGroupField extends PresetField {

    private Map<String, PresetCheckField> checks = new LinkedHashMap<>();

    /**
     * Construct a new PresetGroupField
     * 
     * @param key the key
     */
    public PresetCheckGroupField(@NonNull String key) {
        super(key);
    }

    /**
     * Construct a new PresetGroupField from an existing one
     * 
     * @param field the field to copy
     */
    public PresetCheckGroupField(PresetCheckGroupField field) {
        super(field);
        checks = field.checks;
    }

    /**
     * Add a PresetCheckField to the group
     * 
     * @param field the PresetCheckField to add
     */
    public void addCheckField(@NonNull PresetCheckField field) {
        checks.put(field.getKey(), field);
    }

    /**
     * Get a List of all PresetCheckFields that belong to the group
     * 
     * @return a List of PresetCheckField
     */
    @NonNull
    public List<PresetCheckField> getCheckFields() {
        return new ArrayList<>(checks.values());
    }

    /**
     * Get a Set of all the keys
     * 
     * @return a Set containing all the keys
     */
    @NonNull
    public Set<String> getKeys() {
        return checks.keySet();
    }

    /**
     * Get the PresetCheckField for a specific key
     * 
     * @param key the key
     * @return a PresetCheckField or null if none found
     */
    @Nullable
    public PresetCheckField getCheckField(@NonNull String key) {
        return checks.get(key);
    }

    @Override
    public PresetField copy() {
        return new PresetCheckGroupField(this);
    }

    @Override
    public void translate(@NonNull Po po) {
        super.translate(po);
        for (PresetCheckField check : getCheckFields()) {
            check.translate(po);
        }
    }

    @Override
    public void toXml(XmlSerializer s) throws IllegalArgumentException, IllegalStateException, IOException {
        s.startTag("", Preset.CHECKGROUP);
        s.attribute("", Preset.KEY_ATTR, key);
        standardFieldsToXml(s);
        for (PresetCheckField check : getCheckFields()) {
            check.toXml(s);
        }
        s.endTag("", Preset.CHECKGROUP);
    }

    /**
     * Get an entry count
     * 
     * @return the number of PresetCheckField in the group
     */
    public int size() {
        return checks.size();
    }
}
