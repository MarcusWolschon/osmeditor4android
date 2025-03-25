package io.vespucci.presets;

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ch.poole.poparser.Po;
import io.vespucci.util.StringWithDescription;

public class PresetComboField extends PresetTagField implements PresetFieldJavaScript {
    private static final long serialVersionUID = 2L;

    private StringWithDescription[] values;

    /**
     * Multiple values allowed
     */
    private boolean multiSelect = false;

    /**
     * Combo and multiselect delimiter
     */
    private String delimiter = null;

    /**
     * Script for pre-filling text fields
     */
    private String javascript = null;

    /**
     * Combo and multiselect editable property
     */
    private boolean editable = false;

    /**
     * Sort values or not
     */
    private boolean sort = true;

    /**
     * Add combo and multiselect values to search index
     */
    private boolean valuesSearchable = true;

    /**
     * Translation context
     */
    private String valuesContext = null;

    /**
     * Reference to key of tag holding the number of values this field should hold
     */
    private String valueCountKey;

    private boolean useImages = false;

    /**
     * Constructor
     * 
     * @param key the key for this PresetCheckField
     * @param values an array of StringWithDescription for suggested values
     */
    public PresetComboField(@NonNull String key, @Nullable StringWithDescription[] values) {
        super(key);
        this.values = values;
    }

    /**
     * Constructor that constructs a copy with a different set of suggested values
     * 
     * Note it is not clear if this should set useImages
     * 
     * @param field the PresetComboField to copy
     * @param values an array of StringWithDescription for suggested values
     */
    public PresetComboField(@NonNull PresetComboField field, @Nullable StringWithDescription[] values) {
        super(field);
        this.values = values;
        this.multiSelect = field.multiSelect;
        this.delimiter = field.delimiter;
        this.setEditable(field.isEditable());
        this.setSortValues(field.getSortValues());
        this.setValuesSearchable(field.getValuesSearchable());
        this.valueCountKey = field.valueCountKey;
        this.valuesContext = field.valuesContext;
    }

    /**
     * Copy constructor
     * 
     * @param field PresetComboField to copy
     */
    public PresetComboField(@NonNull PresetComboField field) {
        super(field);
        this.values = field.values;
        this.multiSelect = field.multiSelect;
        this.delimiter = field.delimiter;
        this.setEditable(field.isEditable());
        this.setSortValues(field.getSortValues());
        this.setValuesSearchable(field.getValuesSearchable());
        this.valueCountKey = field.valueCountKey;
        this.valuesContext = field.valuesContext;
        this.useImages = field.useImages;
    }

    /**
     * Get the predefined values for this field
     * 
     * @return an array of StringWithDescription
     */
    @Nullable
    public StringWithDescription[] getValues() {
        return values;
    }

    /**
     * Get the value of the multiselect flag
     * 
     * @return true if the combo will allow multiple selections
     */
    public boolean isMultiSelect() {
        return multiSelect;
    }

    /**
     * Set the multiselect flag
     * 
     * @param multiSelect if true the combo will allow multiple selections
     */
    void setMultiSelect(boolean multiSelect) {
        this.multiSelect = multiSelect;
    }

    /**
     * Set the suggested values for this key
     * 
     * @param valueArray the new values to set
     */
    public void setValues(@Nullable StringWithDescription[] valueArray) {
        values = valueArray;
    }

    @Override
    public PresetTagField copy() {
        return new PresetComboField(this);
    }

    /**
     * Set the translation context for the values
     * 
     * @param valuesContext the valuesContext to set
     */
    void setValuesContext(@Nullable String valuesContext) {
        this.valuesContext = valuesContext;
    }

    /**
     * Get the translation context for the values
     * 
     * @return the valuesContext
     */
    @Nullable
    public String getValuesContext() {
        return valuesContext;
    }

    @Override
    public String getScript() {
        return javascript;
    }

    @Override
    public void setScript(String script) {
        javascript = script;
    }

    @Override
    public void translate(@NonNull Po po) {
        super.translate(po);
        if (getValues() != null) {
            for (StringWithDescription value : getValues()) {
                if (value != null && value.getDescription() != null) {
                    value.setDescription(translate(value.getDescription(), po, getValuesContext()));
                }
            }
        }
    }

    /**
     * Set the delimiter
     * 
     * @param delimiter the delimiter character
     */
    public void setDelimiter(@Nullable String delimiter) {
        this.delimiter = delimiter;
    }

    /**
     * Get the value delimiter
     * 
     * @return the value delimiter
     */
    public char getDelimiter() {
        return delimiter != null && !"".equals(delimiter) ? delimiter.charAt(0) : getDefaultDelimiter();
    }

    /**
     * Get the appropriate default delimter
     * 
     * @return the multiselect or combo delimter
     */
    @NonNull
    private char getDefaultDelimiter() {
        return (multiSelect ? Preset.MULTISELECT_DELIMITER : Preset.COMBO_DELIMITER).charAt(0);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(super.toString());
        s.append(" multiSelect: " + multiSelect);
        s.append(" delimiter: " + delimiter);
        s.append(" editable: " + isEditable());
        s.append(" sort: " + getSortValues());
        s.append(" valuesSearchable: " + getValuesSearchable());
        s.append(" values: ");
        for (StringWithDescription v : values) {
            s.append(" ");
            s.append(v.getValue());
        }
        s.append(" valuesCountKey: " + valueCountKey);
        s.append(" useImages: " + useImages);
        return s.toString();
    }

    /**
     * Check if the values should be sorted for display purposes
     * 
     * @return true if the values should be sorted for display purposes
     */
    boolean getSortValues() {
        return sort;
    }

    /**
     * Set if the values should be sorted for display purposes
     * 
     * @param sort if true the values will be sorted for display purposes
     */
    void setSortValues(boolean sort) {
        this.sort = sort;
    }

    /**
     * Check if this combo is editable (aka can have additional values)
     * 
     * @return true if the tag can have more than the fixed values
     */
    public boolean isEditable() {
        return editable;
    }

    /**
     * Set if this combo is editable (aka can have additional values)
     * 
     * @param editable if true the tag can have additional values
     */
    void setEditable(boolean editable) {
        this.editable = editable;
    }

    /**
     * Check if the values should be added to the search index
     * 
     * @return true if the values should be added to the search index
     */
    boolean getValuesSearchable() {
        return valuesSearchable;
    }

    /**
     * Set if the values should be added to the search index
     * 
     * @param valuesSearchable if true add the values to the search index
     */
    void setValuesSearchable(boolean valuesSearchable) {
        this.valuesSearchable = valuesSearchable;
    }

    /**
     * Name of a key that contains the number of values this field should have
     * 
     * @param valueCountKey the key name
     */
    void setValueCountKey(@Nullable String valueCountKey) {
        this.valueCountKey = valueCountKey;
    }

    /**
     * Get the name of a key containing the number of values this field should have
     * 
     * @return the name of the key with count or null
     */
    @Nullable
    public String getValueCountKey() {
        return valueCountKey;
    }

    /**
     * Set this to true if the fields values contain references to images
     * 
     * @param b the value to set
     */
    public void setUseImages(boolean b) {
        useImages = b;
    }

    /**
     * Check if we should use images or not
     * 
     * @return true if we should use an image selector for this field
     */
    public boolean useImages() {
        return useImages;
    }

    @Override
    public void toXml(XmlSerializer s) throws IllegalArgumentException, IllegalStateException, IOException {
        s.startTag("", isMultiSelect() ? PresetParser.MULTISELECT_FIELD : PresetParser.COMBO_FIELD);
        s.attribute("", PresetParser.KEY_ATTR, key);
        standardFieldsToXml(s);
        if (delimiter != null) {
            s.attribute("", PresetParser.DELIMITER, delimiter);
        }
        s.attribute("", PresetParser.EDITABLE, Boolean.toString(editable));
        s.attribute("", PresetParser.VALUES_SORT, Boolean.toString(sort));
        valuesToXml(s, getValues());
        s.endTag("", isMultiSelect() ? PresetParser.MULTISELECT_FIELD : PresetParser.COMBO_FIELD);
    }

    /**
     * Serialize the values to XML
     * 
     * @param s a XmlSerializer instance
     * @param values the value array
     * @throws IOException if we can't write to the serializer
     */
    static void valuesToXml(@NonNull XmlSerializer s, @NonNull StringWithDescription[] values) throws IOException {
        for (StringWithDescription v : values) {
            s.startTag("", PresetParser.LIST_ENTRY);
            s.attribute("", PresetParser.VALUE, v.getValue());
            String description = v.getDescription();
            if (description != null && !"".equals(description)) {
                s.attribute("", PresetParser.SHORT_DESCRIPTION, v.getDescription());
            }
            s.endTag("", PresetParser.LIST_ENTRY);
        }
    }
}
