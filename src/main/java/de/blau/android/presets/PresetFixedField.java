package de.blau.android.presets;

import android.support.annotation.NonNull;
import ch.poole.poparser.Po;
import de.blau.android.util.StringWithDescription;

public class PresetFixedField extends PresetField {
    final StringWithDescription value;

    /**
     * Constructor
     * 
     * @param key the key
     * @param value the value
     */
    public PresetFixedField(@NonNull String key, @NonNull StringWithDescription value) {
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
    public void translate(@NonNull Po po) {
        super.translate(po);
        if (value.getDescription() != null) {
            value.setDescription(translate(value.getDescription(), po, valueContext));
        }
    }

    @Override
    public String toString() {
        return super.toString() + " value: " + value;
    }
}
