package de.blau.android.presets;

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ch.poole.poparser.Po;

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
     * This field is deprecated
     */
    private boolean deprecated = false;

    /**
     * Does this key have i18n variants
     */
    private boolean i18n = false;

    /**
     * Translation contexts
     */
    private String textContext;
    private String valueContext;

    /**
     * Value type
     */
    private ValueType valueType = null;

    /**
     * Use last as default
     */
    private UseLastAsDefaultType useLastAsDefault = UseLastAsDefaultType.FALSE;

    /**
     * Construct a new PresetField
     * 
     * @param key the key
     */
    protected PresetField(@NonNull String key) {
        this.key = key;
    }

    /**
     * Copy constructor
     * 
     * @param field PresetField to copy
     */
    protected PresetField(PresetField field) {
        this.key = field.key;
        this.hint = field.hint;
        this.defaultValue = field.defaultValue;
        this.matchType = field.matchType;
        this.optional = field.optional;
        this.deprecated = field.deprecated;
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
     * @return true if deprecated
     */
    public boolean isDeprecated() {
        return deprecated;
    }

    /**
     * @param deprecated make this field deprecated
     */
    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
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
    public void setDefaultValue(@Nullable String defaultValue) {
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
    @Nullable
    String getTextContext() {
        return textContext;
    }

    /**
     * Get the value translation context
     * 
     * @return the valueContext
     */
    @Nullable
    String getValueContext() {
        return valueContext;
    }

    /**
     * Set the value translation context
     * 
     * @param valueContext the valueContext to set
     */
    void setValueContext(@Nullable String valueContext) {
        this.valueContext = valueContext;
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
    public void setUseLastAsDefault(@NonNull UseLastAsDefaultType useLastAsDefault) {
        this.useLastAsDefault = useLastAsDefault;
    }

    /**
     * Set if the previous value should be used as default
     * 
     * @param useLastAsDefault the value to set as a String
     */
    public void setUseLastAsDefault(@NonNull String useLastAsDefault) {
        this.useLastAsDefault = UseLastAsDefaultType.fromString(useLastAsDefault);
    }

    /**
     * Get the value of useLastAsDefault
     * 
     * @return and indication if we should use the last value as default for this field
     */
    public UseLastAsDefaultType getUseLastAsDefault() {
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
    public abstract PresetField copy();

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

    /**
     * Output the field in XML format
     * 
     * @param s an XmlSerialzer instance
     * @throws IllegalArgumentException if the serializer encountered an illegal argument
     * @throws IllegalStateException if the serializer detects an illegal state
     * @throws IOException if writing to the serializer fails
     */
    public abstract void toXml(@NonNull XmlSerializer s) throws IllegalArgumentException, IllegalStateException, IOException;

    /**
     * Output the hint, match, value type and default value fields as a XML attributes
     * 
     * @param s an XmlSerialzer instance
     * @throws IOException
     */
    protected void standardFieldsToXml(@NonNull XmlSerializer s) throws IOException {
        if (hint != null && !"".equals(hint)) {
            s.attribute("", PresetParser.TEXT, hint);
        }
        if (defaultValue != null && !"".equals(defaultValue)) {
            s.attribute("", PresetParser.DEFAULT, defaultValue);
        }
        if (matchType != null) {
            s.attribute("", PresetParser.MATCH, matchType.toString());
        }
        if (valueType != null) {
            s.attribute("", PresetParser.VALUE_TYPE, valueType.toString());
        }
    }

    @Override
    public String toString() {
        return key + " (" + hint + ") default: " + defaultValue + " match: " + matchType + " opt: " + optional + " i18n: " + isI18n() + " textCtx: "
                + textContext + " valueCtx: " + valueContext + " valueType: " + getValueType();
    }
}
