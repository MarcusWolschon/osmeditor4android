package de.blau.android.presets;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
    private boolean i18n = false;

    /**
     * Translation contexts
     */
    private String textContext;
    String         valueContext;

    /**
     * Value type
     */
    private ValueType valueType = null;

    /**
     * Use last as default
     */
    private UseLastAsDefault useLastAsDefault = UseLastAsDefault.FALSE;

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
        this.setI18n(field.isI18n());
        this.textContext = field.textContext;
        this.valueContext = field.valueContext;
        this.setValueType(field.getValueType());
        this.setUseLastAsDefault(field.getUseLastAsDefault());
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
    public void setMatchType(@NonNull String match) {
        matchType = MatchType.fromString(match);
    }

    /**
     * Set the match type for this field
     * 
     * @param match the match type
     */
    public void setMatchType(@Nullable MatchType match) {
        matchType = match;
    }

    /**
     * Get the match type for this field
     * 
     * @return the match type
     */
    @Nullable
    public MatchType getMatchType() {
        return matchType;
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
     * Set if the previous value should be used as default
     * 
     * @param useLastAsDefault the value to set as a String
     */
    public void setUseLastAsDefault(@NonNull String useLastAsDefault) {
        this.useLastAsDefault = UseLastAsDefault.fromString(useLastAsDefault);
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
     * @return the i18n
     */
    boolean isI18n() {
        return i18n;
    }

    /**
     * @param i18n the i18n to set
     */
    void setI18n(boolean i18n) {
        this.i18n = i18n;
    }

    /**
     * @return the valueType
     */
    public ValueType getValueType() {
        return valueType;
    }

    /**
     * @param type the ValueType as string to set
     */
    void setValueType(@Nullable String type) {
        if (type == null) {
            this.valueType = null;
            return;
        }
        this.valueType = ValueType.fromString(type);
    }

    /**
     * @param type the ValueType to set
     */
    void setValueType(@Nullable ValueType type) {
        this.valueType = type;
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
        return key + " (" + hint + ") default: " + defaultValue + " match: " + matchType + " opt: " + optional + " i18n: " + isI18n() + " textCtx: "
                + textContext + " valueCtx: " + valueContext + " valueType: " + getValueType();
    }
}
