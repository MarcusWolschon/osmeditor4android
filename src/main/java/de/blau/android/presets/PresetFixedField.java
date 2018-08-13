package de.blau.android.presets;

import de.blau.android.util.StringWithDescription;

public class PresetFixedField extends PresetField {
    final StringWithDescription value;

    /**
     * Constructor
     * 
     * @param key the key
     * @param value the value
     */
    public PresetFixedField(String key, StringWithDescription value) {
        super(key);
        this.value = value;
    }

    /**
     * Copy constructor
     * 
     * @param field PresetFixedField to copy
     */
    public PresetFixedField(PresetFixedField field) {
        super(field);
        this.value = field.value;
    }

    /**
     * Get the value of this field
     * 
     * @return the fixed value
     */
    public StringWithDescription getValue() {
        return value;
    }

    @Override
    PresetField copy() {
        return new PresetFixedField(this);
    }

    @Override
    public String toString() {
        return super.toString() + " value: " + value;
    }
}
