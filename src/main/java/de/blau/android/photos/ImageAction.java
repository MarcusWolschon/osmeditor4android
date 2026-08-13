package de.blau.android.photos;

import java.io.Serializable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ImageAction implements Serializable {
    private static final long serialVersionUID = 2L;

    public enum Action {
        NOTHING, UPLOAD, ADDTOELEMENT, ADDTONOTE
    }

    private final Action action;
    private long         id;
    private String       elementType;
    private String       filename;

    /**
     * Construct a new instance
     * 
     * @param action what to doe with the image
     */
    public ImageAction(@NonNull Action action) {
        this.action = action;
    }

    /**
     * @return the elementType
     */
    @Nullable
    public String getElementType() {
        return elementType;
    }

    /**
     * @param elementType the elementType to set
     */
    public void setElementType(@Nullable String elementType) {
        this.elementType = elementType;
    }

    /**
     * @return the id
     */
    public long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * @return the action
     */
    @NonNull
    public Action getAction() {
        return action;
    }

    /**
     * @return the filename
     */
    @Nullable
    public String getFilename() {
        return filename;
    }

    /**
     * @param filename the filename to set
     */
    public void setFilename(@Nullable String filename) {
        this.filename = filename;
    }
}
