package de.blau.android.presets;

import android.support.annotation.NonNull;
import de.blau.android.presets.Preset.MatchType;
import de.blau.android.presets.Preset.ValueType;

public abstract class PresetField {
    /**
     * Key this field is for
     */
    final String key;

    /**
     * Hint to be displayed in a suitable form
     */
    private String hint;

    /**
     * Default value
     */
    private String defaultValue;

    /**
     * Match properties
     */
    MatchType matchType = MatchType.KEY_VALUE;

    /**
     * Is this field is optional
     */
    private boolean optional = false;

    /**
     * Does this key have i18n variants
     */
    boolean i18n = false;

    /**
     * Translation contexts
     */
    String textContext;
    String valueContext;

    /**
     * Value type
     */
    ValueType valueType = null;

    public PresetField(@NonNull String key) {
        this.key = key;
    }

    /**
     * Copy constructor
     * 
     * @param field PresetField to copy
     */
    public PresetField(PresetField field) {
        this.key = field.key;
        this.hint = field.hint;
        this.defaultValue = field.defaultValue;
        this.matchType = field.matchType;
        this.optional = field.optional;
        this.i18n = field.i18n;
        this.textContext = field.textContext;
        this.valueContext = field.valueContext;
        this.valueType = field.valueType;
    }

    /**
     * @return true if optional
     */
    public boolean isOptional() {
        return optional;
    }

    /**
     * @param optional make this field optional
     */
    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    /**
     * Return the key for this PresetField
     * 
     * @return a String containing the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @return the hint
     */
    public String getHint() {
        return hint;
    }

    /**
     * @param hint the hint to set
     */
    void setHint(String hint) {
        this.hint = hint;
    }

    /**
     * @return the defaultValue
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * @param defaultValue the defaultValue to set
     */
    void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * Method that creates a copy of the element
     * 
     * @return a PresetField instance
     */
    abstract PresetField copy();

    @Override
    public String toString() {
        return key + " (" + hint + ") default: " + defaultValue + " match: " + matchType + " opt: " + optional + " i18n: " + i18n + " textCtx: " + textContext
                + " valueCtx: " + valueContext + " valueType: " + valueType;
    }
}
