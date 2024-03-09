package de.blau.android.presets;

import androidx.annotation.NonNull;

public abstract class PresetFormattingField extends PresetField {

    private static final long serialVersionUID = 1L;

    protected PresetFormattingField() {
        super();
    }

    protected PresetFormattingField(@NonNull PresetFormattingField field) {
        super(field);
    }
}
