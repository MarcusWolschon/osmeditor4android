package de.blau.android.prefs;

import java.io.Serializable;

import de.blau.android.prefs.AdvancedPrefDatabase.ImageStorageType;

/**
 * Data structure class for image stores
 */
public class ImageStorageConfiguration implements Serializable {
    private static final long serialVersionUID = 1L;

    public final String           id;
    public final String           name;
    public final ImageStorageType type;
    public final String           url;
    public final boolean          active;

    /**
     * Construct a new class describing a ImageStore
     * 
     * @param id internal id
     * @param name the name
     * @param type the type
     * @param url url for the API
     * @param active if true the entry is in use
     */
    public ImageStorageConfiguration(String id, String name, ImageStorageType type, String url, boolean active) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.url = url;
        this.active = active;
    }
}
