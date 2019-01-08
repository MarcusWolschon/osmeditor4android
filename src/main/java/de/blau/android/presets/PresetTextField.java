package de.blau.android.presets;

import android.support.annotation.NonNull;

public class PresetTextField extends PresetField implements PresetFieldJavaScript {
    /**
     * Script for pre-filling text fields
     */
    String javascript = null;

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

    @Override
    PresetField copy() {
        return new PresetTextField(this);
    }

    @Override
    public String toString() {
        return super.toString() + " javascript: " + javascript;
    }
}
