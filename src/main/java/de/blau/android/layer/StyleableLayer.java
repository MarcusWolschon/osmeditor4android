package de.blau.android.layer;

import java.util.List;

import android.graphics.Path;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public abstract class StyleableLayer extends MapViewLayer {

    /**
     * Get the current color for this layer
     * 
     * @return the color as an int
     */
    public abstract int getColor();

    /**
     * Set the color for the layer
     * 
     * @param color color to use
     */
    public abstract void setColor(int color);

    /**
     * Get the stroke width for drawing lines
     * 
     * @return the stroke width in pixels
     */
    public abstract float getStrokeWidth();

    /**
     * Set the stroke width for lines
     * 
     * @param width stroke width in pixels
     */
    public abstract void setStrokeWidth(float width);

    /**
     * Symbol for points
     * 
     * @return the Path object used for points
     */
    public abstract Path getPointSymbol();

    /**
     * Set the Path for the symbol for points
     * 
     * @param symbol the Path for symbol
     */
    public abstract void setPointSymbol(Path symbol);

    /**
     * Set styling parameters back to defaults
     */
    public abstract void resetStyling();

    /**
     * Get a list of keys for labeling
     * 
     * @return a list og keys, null if there are none
     */
    public List<String> getLabelList() {
        return null;
    }

    /**
     * Set the key of the label to use
     * 
     * @param key label key to use
     */
    public void setLabel(@NonNull String key) {
        // do nothing as default
    }

    /**
     * Get the current label field/key
     * 
     * @return the current label field/key or null if none
     */
    @Nullable
    public String getLabel() {
        return null;
    }
}
