package io.vespucci.layer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class LayerConfig {

    private int       position;
    private LayerType type;
    private boolean   visible;
    private String    contentId;

    /**
     * Construct a new LayerCOnfig instance
     * 
     * @param position the position of the layer
     * @param type the LayerType
     * @param visible if the layer is currently shown
     * @param contentId an id for the content or null
     */
    public LayerConfig(int position, @NonNull LayerType type, boolean visible, @Nullable String contentId) {
        this.position = position;
        this.type = type;
        this.visible = visible;
        this.contentId = contentId;
    }

    /**
     * @return the type
     */
    public LayerType getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(LayerType type) {
        this.type = type;
    }

    /**
     * @return the contentId
     */
    public String getContentId() {
        return contentId;
    }

    /**
     * @param contentId the contentId to set
     */
    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    /**
     * @return the visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * @param visible the visible to set
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * @return the position
     */
    public int getPosition() {
        return position;
    }

    /**
     * @param position the position to set
     */
    public void setPosition(int position) {
        this.position = position;
    }

    @Override
    public String toString() {
        return type.toString() + " " + position + " " + type + " " + visible + " " + contentId;
    }
}
