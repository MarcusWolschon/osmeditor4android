package de.blau.android.layer;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Path;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface StyleableInterface {

    /**
     * Get the current color for this layer
     * 
     * @return the color as an int
     */
    public int getColor();

    /**
     * Set the color for the layer
     * 
     * @param color color to use
     */
    public void setColor(int color);

    /**
     * Get the stroke width for drawing lines
     * 
     * @return the stroke width in pixels
     */
    public float getStrokeWidth();

    /**
     * Set the stroke width for lines
     * 
     * @param width stroke width in pixels
     */
    public void setStrokeWidth(float width);

    /**
     * Symbol for points
     * 
     * @return the Path object used for points
     */
    default Path getPointSymbol() {
        // unused
        return null;
    }

    /**
     * Set the Path for the symbol for points
     * 
     * @param symbol the Path for symbol
     */
    default void setPointSymbol(@NonNull Path symbol) {
        // unimplemented
    }

    /**
     * Set styling parameters back to defaults
     */
    public void resetStyling();

    /**
     * Get a list of keys for labeling
     * 
     * @return a list of keys, null if there are none
     */
    @NonNull
    default List<String> getLabelList() {
        return new ArrayList<>();
    }

    /**
     * Set the key of the label to use
     * 
     * @param key label key to use
     */
    default void setLabel(@NonNull String key) {
        // do nothing as default
    }

    /**
     * Get the current label field/key
     * 
     * @return the current label field/key or null if none
     */
    @Nullable
    default String getLabel() {
        return null;
    }
}
