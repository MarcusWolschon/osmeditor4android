package de.blau.android.presets;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import ch.poole.poparser.Po;
import de.blau.android.presets.Preset.MatchType;
import de.blau.android.presets.Preset.UseLastAsDefault;
import de.blau.android.presets.Preset.ValueType;

public abstract class PresetField {
    private static final String DEBUG_TAG = "PresetField";

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
    private String textContext;
    String         valueContext;

    /**
     * Value type
     */
    ValueType valueType = null;

    /**
     * Use last as default
     */
    UseLastAsDefault useLastAsDefault = UseLastAsDefault.FALSE;
    
    /**
     * Construct a new PresetField
     * 
     * @param key the key
     */
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
    @NonNull
    public String getKey() {
        return key;
    }

    /**
     * @return the hint
     */
    @Nullable
    public String getHint() {
        return hint;
    }

    /**
     * Set a short description for this tag/field
     * 
     * @param hint the hint to set
     */
    void setHint(@Nullable String hint) {
        this.hint = hint;
    }

    /**
     * Get the default value for this field
     * 
     * @return the defaultValue
     */
    @Nullable
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Set teh default value for this field
     * 
     * @param defaultValue the defaultValue to set
     */
    void setDefaultValue(@Nullable String defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * Set the text translation context
     * 
     * @param textContext the translation context
     */
    public void setTextContext(@Nullable String textContext) {
        this.textContext = textContext;
    }

    /**
     * Get the text translation context
     * 
     * @return the textContext
     */
    String getTextContext() {
        return textContext;
    }

    /**
     * Set the match type for this field
     * 
     * @param match the match type
     */
    public void setMatchType(String match) {

        MatchType type = null;
        switch (match) {
        case "none":
            type = MatchType.NONE;
            break;
        case "key":
            type = MatchType.KEY;
            break;
        case "key!":
            type = MatchType.KEY_NEG;
            break;
        case "keyvalue":
            type = MatchType.KEY_VALUE;
            break;
        case "keyvalue!":
            type = MatchType.KEY_VALUE_NEG;
            break;
        }
        if (type != null) {
            matchType = type;
        } else {
            Log.e(DEBUG_TAG, "setMatchType PresetField for key " + key + " is null");
        }
    }

    /**
     * Set if the previous value should be used as default
     * 
     * @param useLastAsDefault the value to set
     */
    public void setUseLastAsDefault(@NonNull UseLastAsDefault useLastAsDefault) {
        this.useLastAsDefault = useLastAsDefault;
    }
    
    /**
     * Get the value of useLastAsDefault
     * 
     * @return and indication if we should use the last value as default for this field
     */
    public UseLastAsDefault getUseLastAsDefault() {
        return useLastAsDefault;
    }
    
    /**
     * Method that creates a copy of the element
     * 
     * @return a PresetField instance
     */
    abstract PresetField copy();

    /**
     * Translate a String
     * 
     * @param text the text to translate
     * @param po the translation object
     * @param context a translation context of null
     * @return the translated String
     */
    String translate(@NonNull String text, @NonNull Po po, @Nullable String context) {
        return context != null ? po.t(context, text) : po.t(text);
    }

    /**
     * Translate the translatable parts of this PresetField
     * 
     * Note this cannot be undone
     * 
     * @param po the object holding the translations
     */
    public void translate(@NonNull Po po) {
        hint = translate(hint, po, textContext);
    }

    @Override
    public String toString() {
        return key + " (" + hint + ") default: " + defaultValue + " match: " + matchType + " opt: " + optional + " i18n: " + i18n + " textCtx: " + textContext
                + " valueCtx: " + valueContext + " valueType: " + valueType;
    }
}
