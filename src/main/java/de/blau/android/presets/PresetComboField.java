package de.blau.android.presets;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import ch.poole.poparser.Po;
import de.blau.android.util.StringWithDescription;

public class PresetComboField extends PresetField {

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
     * Combo and multiselect editable property
     */
    boolean editable = false;

    /**
     * Sort values or not
     */
    boolean sort = true;

    /**
     * Add combo and multiselect values to search index
     */
    boolean valuesSearchable = true;

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
        this.editable = field.editable;
        this.sort = field.sort;
        this.valuesSearchable = field.valuesSearchable;
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
        this.editable = field.editable;
        this.sort = field.sort;
        this.valuesSearchable = field.valuesSearchable;
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
    PresetField copy() {
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

    @Override
    public void translate(@NonNull Po po) {
        super.translate(po);
        if (getValues() != null) {
            for (StringWithDescription value : getValues()) {
                if (value != null && value.getDescription() != null) {
                    value.setDescription(translate(value.getDescription(), po, valuesContext));
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(super.toString());
        s.append(" multiSelect: " + multiSelect);
        s.append(" delimiter: " + delimiter);
        s.append(" editable: " + editable);
        s.append(" sort: " + sort);
        s.append(" valuesSearchable: " + valuesSearchable);
        s.append(" values: ");
        for (StringWithDescription v : values) {
            s.append(" ");
            s.append(v.getValue());
        }
        return s.toString();
    }
}
