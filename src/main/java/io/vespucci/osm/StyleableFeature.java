package io.vespucci.osm;

import androidx.annotation.Nullable;
import io.vespucci.resources.DataStyle.FeatureStyle;

public interface StyleableFeature {
    /**
     * Get the rendering style for this way
     * 
     * @return the style or null if not set
     */
    public FeatureStyle getStyle();

    /**
     * Set the rendering style for this way
     * 
     * @param fp the style to set, null to reset
     */
    public void setStyle(@Nullable FeatureStyle fp);
}
