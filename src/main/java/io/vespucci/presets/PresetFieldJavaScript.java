package io.vespucci.presets;

import androidx.annotation.Nullable;

public interface PresetFieldJavaScript {

    /**
     * Get the script if any
     * 
     * @return the script or null
     */
    @Nullable
    String getScript();

    /**
     * Set the script for this field
     * 
     * @param script the script or null
     */
    void setScript(@Nullable String script);
}
