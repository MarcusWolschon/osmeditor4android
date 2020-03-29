package de.blau.android.presets;

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

import androidx.annotation.NonNull;
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
    @NonNull
    public StringWithDescription getValue() {
        return value;
    }

    @Override
    public PresetField copy() {
        return new PresetFixedField(this);
    }

    @Override
    public void translate(@NonNull Po po) {
        super.translate(po);
        if (value.getDescription() != null) {
            value.setDescription(translate(value.getDescription(), po, getValueContext()));
        }
    }

    @Override
    public void toXml(XmlSerializer s) throws IllegalArgumentException, IllegalStateException, IOException {
        s.startTag("", Preset.KEY_ATTR);
        s.attribute("", Preset.KEY_ATTR, key);
        StringWithDescription v = getValue();
        s.attribute("", Preset.VALUE, v.getValue());
        String description = v.getDescription();
        if (description != null && !"".equals(description)) {
            s.attribute("", Preset.TEXT, description);
        }
        s.endTag("", Preset.KEY_ATTR);
    }

    @Override
    public String toString() {
        return super.toString() + " value: " + value;
    }
}
