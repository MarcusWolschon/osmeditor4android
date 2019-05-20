package de.blau.android.presets;

import android.support.annotation.NonNull;

public class PresetTextField extends PresetField implements PresetFieldJavaScript {
    /**
     * Script for pre-filling text fields
     */
    String javascript = null;

    /**
     * Configured length of the text field
     */
    private int length = 0;

    /**
     * Constructor
     * 
     * @param key key for the PresetTextField
     */
    public PresetTextField(@NonNull String key) {
        super(key);
    }

    /**
     * Copy constructor
     * 
     * @param field PresetTextField to copy
     */
    public PresetTextField(PresetTextField field) {
        super(field);
        this.javascript = field.javascript;
    }

    @Override
    public String getScript() {
        return javascript;
    }

    @Override
    public void setScript(String script) {
        javascript = script;
    }

    /**
     * Get a proposed length for the field
     * 
     * @return the length in characters
     */
    public int length() {
        return length;
    }

    /**
     * Set the length attribute
     * 
     * @param length the length to set in characters
     */
    void setLength(int length) {
        this.length = length;
    }

    @Override
    PresetField copy() {
        return new PresetTextField(this);
    }

    @Override
    public String toString() {
        return super.toString() + " javascript: " + javascript + " length: " + length;
    }
}
