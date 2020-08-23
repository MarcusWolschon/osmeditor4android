package de.blau.android.presets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PresetLink {

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
    public PresetLink(@NonNull String presetName, @Nullable String text, @Nullable String textContext) {
        this.presetName = presetName;
        this.text = text;
        this.textContext = textContext;
    }

    /**
     * @return the presetName
     */
    @NonNull
    String getPresetName() {
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
        final int prime = 31;
        int result = 1;
        result = prime * result + ((presetName == null) ? 0 : presetName.hashCode());
        result = prime * result + ((text == null) ? 0 : text.hashCode());
        result = prime * result + ((textContext == null) ? 0 : textContext.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PresetLink)) {
            return false;
        }
        PresetLink other = (PresetLink) obj;
        if (presetName == null) {
            if (other.presetName != null) {
                return false;
            }
        } else if (!presetName.equals(other.presetName)) {
            return false;
        }
        if (text == null) {
            if (other.text != null) {
                return false;
            }
        } else if (!text.equals(other.text)) {
            return false;
        }
        if (textContext == null) {
            if (other.textContext != null) {
                return false;
            }
        } else if (!textContext.equals(other.textContext)) {
            return false;
        }
        return true;
    }
}
