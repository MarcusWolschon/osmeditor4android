package de.blau.android.prefs;

import de.blau.android.prefs.AdvancedPrefDatabase.ImageStoreType;

/**
 * Data structure class for image stores
 */
public class ImageStoreConfiguraton {
    public final String         id;
    public final String         name;
    public final ImageStoreType type;
    public final String         url;
    public final boolean        active;

    /**
     * Construct a new class describing a ImageStore
     * 
     * @param id internal id
     * @param name the name
     * @param type the type
     * @param url url for the API
     * @param active if true the entry is in use
     */
    public ImageStoreConfiguraton(String id, String name, ImageStoreType type, String url, boolean active) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.url = url;
        this.active = active;
    }
}
