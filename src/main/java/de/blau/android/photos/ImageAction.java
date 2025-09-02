package de.blau.android.photos;

import java.io.Serializable;

public class ImageAction implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Action {
        NOTHING, UPLOAD, ADDTOELEMENT, ADDTONOTE
    }

    private Action action;
    private long   id;
    private String elementType;

    /**
     * @return the elementType
     */
    public String getElementType() {
        return elementType;
    }

    /**
     * @param elementType the elementType to set
     */
    public void setElementType(String elementType) {
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
    public Action getAction() {
        return action;
    }

    /**
     * @param action the action to set
     */
    public void setAction(Action action) {
        this.action = action;
    }
}
