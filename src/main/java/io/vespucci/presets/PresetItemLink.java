package io.vespucci.presets;

import java.io.Serializable;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PresetItemLink implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private final String presetName;
    private final String text;
    private final String textContext;

    /**
     * Container for links to Presets
     * 
     * @param presetName the name of the target Preset, note that these are not guaranteed to be unique
     * @param text an optional text to display for the link
     * @param textContext the translation context for the text
     */
    public PresetItemLink(@NonNull String presetName, @Nullable String text, @Nullable String textContext) {
        this.presetName = presetName;
        this.text = text;
        this.textContext = textContext;
    }

    /**
     * @return the presetName
     */
    @NonNull
    public String getPresetName() {
        return presetName;
    }

    /**
     * @return the text
     */
    @Nullable
    String getText() {
        return text;
    }

    /**
     * @return the textContext
     */
    @Nullable
    String getTextContext() {
        return textContext;
    }

    @Override
    public int hashCode() {
        return Objects.hash(presetName, text, textContext);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PresetItemLink)) {
            return false;
        }
        PresetItemLink other = (PresetItemLink) obj;
        return Objects.equals(presetName, other.presetName) && Objects.equals(text, other.text)
                && Objects.equals(textContext, other.textContext);
    }
}
