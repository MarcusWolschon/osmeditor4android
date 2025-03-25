package io.vespucci.layer;

import java.util.ArrayList;
import java.util.List;

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
     * Get the current color for a specific sub-layer of this layer
     * 
     * @param layerName the (sub)layer name
     * @return the color as an int
     */
    default int getColor(@NonNull String layerName) {
        return getColor();
    }

    /**
     * Set the color for the layer
     * 
     * @param color color to use
     */
    public void setColor(int color);

    /**
     * Set the color for a specific sub-layer of this layer
     * 
     * @param layerName the (sub)layer name
     * @param color color to use
     */
    default void setColor(@NonNull String layerName, int color) {
        setColor(color);
    }

    /**
     * Get the stroke width for drawing lines
     * 
     * @return the stroke width in pixels
     */
    public float getStrokeWidth();

    /**
     * Get the stroke width for drawing lines for a specific sub-layer of this layer
     * 
     * @param layerName the (sub)layer name
     * @return the stroke width in pixels
     */
    default float getStrokeWidth(@NonNull String layerName) {
        return getStrokeWidth();
    }

    /**
     * Set the stroke width for lines
     * 
     * @param width stroke width in pixels
     */
    public void setStrokeWidth(float width);

    /**
     * Set the stroke width for lines for a specific sub-layer of this layer
     * 
     * @param layerName the (sub)layer name
     * @param width stroke width in pixels
     */
    default void setStrokeWidth(@NonNull String layerName, float width) {
        setStrokeWidth(width);
    }

    /**
     * Check if we use point symbols or not
     * 
     * @return true if we use point symbols
     */
    default boolean usesPointSymbol() {
        return true;
    }

    /**
     * Symbol for points
     * 
     * @return the Path object used for points
     */
    @Nullable
    default String getPointSymbol() {
        return null;
    }

    /**
     * Symbol for points for a specific sub-layer of this layer
     * 
     * @param layerName the (sub)layer name
     * @return the Path object used for points
     */
    @Nullable
    default String getPointSymbol(@NonNull String layerName) {
        return getPointSymbol();
    }

    /**
     * Set the Path for the symbol for points
     * 
     * @param symbol the name for the symbol
     */
    default void setPointSymbol(@Nullable String symbol) {
        // unimplemented
    }

    /**
     * Set the Path for the symbol for points for a specific sub-layer of this layer
     * 
     * @param layerName the (sub)layer name
     * @param symbol the Path for symbol
     */
    default void setPointSymbol(@NonNull String layerName, @Nullable String symbol) {
        setPointSymbol(symbol);
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
     * Get a list of keys for labeling a specific (sub)layer of this layer
     * 
     * @param layerName the (sub)layer name
     * @return a list of keys, empty if there are none
     */
    @NonNull
    default List<String> getLabelList(@Nullable String layerName) {
        return getLabelList();
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
     * Set the key of the label to use for a specific (sub)layer of this layer
     * 
     * @param layerName the (sub)layer name
     * @param key label key to use
     */
    default void setLabel(@NonNull String layerName, @NonNull String key) {
        setLabel(key);
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

    /**
     * Get the current label field/key for a specific (sub)layer of this layer
     * 
     * @param layerName the (sub)layer name
     * @return the current label field/key or null if none
     */
    @Nullable
    default String getLabel(@NonNull String layerName) {
        return getLabel();
    }

    /**
     * Get a list of (sub-)layers
     * 
     * @return a list of keys, empty if there are none
     */
    @NonNull
    default List<String> getLayerList() {
        return new ArrayList<>();
    }

    /**
     * Get the current minimum zoom for a specific (sub)layer of this layer
     * 
     * @param layerName the (sub)layer name
     * @return a value equal to or larger than 0
     */
    default int getMinZoom(@NonNull String layerName) {
        return 0;
    }

    /**
     * Set the current minimum zoom for a specific (sub)layer of this layer
     * 
     * @param subLayerName the (sub)layer name
     * @param zoom a value equal to or larger than 0
     */
    default void setMinZoom(@NonNull String subLayerName, int zoom) {
        // unimplemented
    }

    /**
     * Get the current maximum zoom for a specific (sub)layer of this layer
     * 
     * @param layerName the (sub)layer name
     * @return a negative number if not set, otherwise the maximum zoom level
     */
    default int getMaxZoom(String layerName) {
        return -1;
    }

    /**
     * Set the current maximum zoom for a specific (sub)layer of this layer
     * 
     * @param subLayerName the (sub)layer name
     * @param zoom a value equal to or larger than 0
     */
    default void setMaxZoom(@NonNull String subLayerName, int zoom) {
        // unimplemented
    }

    /**
     * CHeck if the layer currently supports interactive styling
     * 
     * @return true if interactive styling is enabled
     */
    default boolean stylingEnabled() {
        return true;
    }
}
