package io.vespucci.layer;

import androidx.annotation.NonNull;
import io.vespucci.Map;

public interface LabelMinZoomInterface {

    /**
     * Set minimum zoom to display a label
     * 
     * @param minZoom the zoom level
     */
    default void setLabelMinZoom(int minZoom) {
        // do nothing as default
    }

    /**
     * Set minimum zoom to display a label
     * 
     * @param layerName the (sub)layer name
     * @param minZoom the zoom level
     */
    default void setLabelMinZoom(@NonNull String layerName, int minZoom) {
        setLabelMinZoom(minZoom);
    }

    /**
     * Get the current minimum zoom to display a label
     * 
     * @return the zoom level
     */
    default int getLabelMinZoom() {
        return Map.SHOW_LABEL_LIMIT;
    }

    /**
     * Get the current minimum zoom to display a label for a specific (sub)layer of this layer
     * 
     * @param layerName the (sub)layer name
     * @return the zoom level
     */
    default int getLabelMinZoom(@NonNull String layerName) {
        return getLabelMinZoom();
    }
}
