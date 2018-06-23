package de.blau.android.presets;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import ch.poole.openinghoursfragment.ValueWithDescription;
import de.blau.android.util.StringWithDescription;

public class PresetCheckField extends PresetField {
    /**
     * on value
     */
    final StringWithDescription onValue;

    /**
     * on value
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

    @Override
    PresetField copy() {
        return new PresetCheckField(this);
    }

    @Override
    public String toString() {
        return super.toString() + " onValue: " + onValue;
    }
}
