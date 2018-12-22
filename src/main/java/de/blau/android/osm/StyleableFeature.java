package de.blau.android.osm;

import android.support.annotation.Nullable;
import de.blau.android.resources.DataStyle.FeatureStyle;

public interface StyleableFeature {
    /**
     * Get the rendering style for this way
     * 
     * @return the style of null if not set
     */
    public FeatureStyle getFeatureProfile();

    /**
     * Set the rendering style for this way
     * 
     * @param fp the style to set, null to reset
     */
    public void setFeatureProfile(@Nullable FeatureStyle fp);
}
