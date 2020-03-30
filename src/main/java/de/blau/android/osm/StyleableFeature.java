package de.blau.android.osm;

import androidx.annotation.Nullable;
import de.blau.android.resources.DataStyle.FeatureStyle;

public interface StyleableFeature {
    /**
     * Get the rendering style for this way
     * 
     * @return the style of null if not set
     */
    public FeatureStyle getStyle();

    /**
     * Set the rendering style for this way
     * 
     * @param fp the style to set, null to reset
     */
    public void setStyle(@Nullable FeatureStyle fp);
}
