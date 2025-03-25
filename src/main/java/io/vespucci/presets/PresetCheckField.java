package io.vespucci.presets;

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ch.poole.poparser.Po;
import io.vespucci.util.StringWithDescription;

public class PresetCheckField extends PresetTagField {
    private static final long serialVersionUID = 2L;

    /**
     * on value
     */
    final StringWithDescription onValue;

    /**
     * off value
     */
    private StringWithDescription offValue = null;

    /**
     * Constructor
     * 
     * @param key the key for this PresetCheckField
     * @param onValue the value when the check is selected
     */
    public PresetCheckField(@NonNull String key, @NonNull StringWithDescription onValue) {
        super(key);
        this.onValue = onValue;
    }

    /**
     * Copy constructor
     * 
     * @param field PresetCheckField to copy
     */
    public PresetCheckField(@NonNull PresetCheckField field) {
        super(field);
        this.onValue = field.onValue;
        this.offValue = field.offValue;
    }

    /**
     * Get the value when the check box is checked
     * 
     * @return the onValue for the check
     */
    @NonNull
    public StringWithDescription getOnValue() {
        return onValue;
    }

    /**
     * @return the offValue
     */
    @Nullable
    public StringWithDescription getOffValue() {
        return offValue;
    }

    /**
     * @param offValue the offValue to set
     */
    public void setOffValue(@Nullable StringWithDescription offValue) {
        this.offValue = offValue;
    }

    /**
     * Check if a value corresponds to the off value
     * 
     * @param value to check
     * @return true if it is the off value
     */
    public boolean isOffValue(String value) {
        return offValue != null && offValue.getValue().equals(value);
    }

    @Override
    public PresetTagField copy() {
        return new PresetCheckField(this);
    }

    @Override
    public void translate(@NonNull Po po) {
        super.translate(po);
        if (onValue.getDescription() != null) {
            onValue.setDescription(translate(onValue.getDescription(), po, getValueContext()));
        }
        if (offValue != null && offValue.getDescription() != null) {
            offValue.setDescription(translate(offValue.getDescription(), po, getValueContext()));
        }
    }

    @Override
    public void toXml(XmlSerializer s) throws IllegalArgumentException, IllegalStateException, IOException {
        s.startTag("", PresetParser.CHECK_FIELD);
        s.attribute("", PresetParser.KEY_ATTR, key);
        standardFieldsToXml(s);
        s.attribute("", PresetParser.DISABLE_OFF, Boolean.toString(offValue == null || "".equals(offValue.getValue())));
        if (offValue != null && !PresetParser.NO.equals(offValue.getValue())) {
            s.attribute("", PresetParser.VALUE_OFF, offValue.getValue());
        }
        if (onValue != null && !PresetParser.YES.equals(onValue.getValue())) {
            s.attribute("", PresetParser.VALUE_ON, onValue.getValue());
        }
        s.endTag("", PresetParser.CHECK_FIELD);
    }

    @Override
    public String toString() {
        return super.toString() + " onValue: " + onValue + " offValue " + offValue + " default " + getDefaultValue();
    }
}
