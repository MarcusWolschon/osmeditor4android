package de.blau.android.presets;

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ch.poole.poparser.Po;
import de.blau.android.util.StringWithDescription;

public class PresetComboField extends PresetField implements PresetFieldJavaScript {

    StringWithDescription[] values;

    /**
     * Multiple values allowed
     */
    private boolean multiSelect = false;

    /**
     * Combo and multiselect delimiter
     */
    String delimiter = null;

    /**
     * Script for pre-filling text fields
     */
    String javascript = null;

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
    }

    /**
     * Copy constructor
     * 
     * @param field PresetComboField to copy
     */
    public PresetComboField(PresetComboField field) {
        super(field);
        this.values = field.values;
        this.multiSelect = field.multiSelect;
        this.delimiter = field.delimiter;
        this.setEditable(field.isEditable());
        this.setSortValues(field.getSortValues());
        this.setValuesSearchable(field.getValuesSearchable());
    }

    /**
     * Get the predefined values for this field
     * 
     * @return and array of StringWithDescription
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
    public PresetField copy() {
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

    @Override
    public void toXml(XmlSerializer s) throws IllegalArgumentException, IllegalStateException, IOException {
        s.startTag("", isMultiSelect() ? Preset.MULTISELECT_FIELD : Preset.COMBO_FIELD);
        s.attribute("", Preset.KEY_ATTR, key);
        standardFieldsToXml(s);
        if (delimiter != null) {
            s.attribute("", Preset.DELIMITER, delimiter);
        }
        s.attribute("", Preset.EDITABLE, Boolean.toString(editable));
        s.attribute("", Preset.VALUES_SORT, Boolean.toString(sort));
        for (StringWithDescription v : getValues()) {
            s.startTag("", Preset.LIST_ENTRY);
            s.attribute("", Preset.VALUE, v.getValue());
            String description = v.getDescription();
            if (description != null && !"".equals(description)) {
                s.attribute("", Preset.SHORT_DESCRIPTION, v.getDescription());
            }
            s.endTag("", Preset.LIST_ENTRY);
        }
        s.endTag("", isMultiSelect() ? Preset.MULTISELECT_FIELD : Preset.COMBO_FIELD);
    }
}
