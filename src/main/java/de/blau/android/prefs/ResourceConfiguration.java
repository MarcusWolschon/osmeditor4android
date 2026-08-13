package de.blau.android.prefs;

import java.io.Serializable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Generic resource configuration data structure
 */
public abstract class ResourceConfiguration implements Serializable {
    private static final long serialVersionUID = 1L;

    public final String id;
    public final String name;
    public final String description;
    public final String version;
    public final long   lastupdate;
    public final String url;
    private boolean     active;

    /**
     * Construct a new class describing a resource
     * 
     * @param id internal id
     * @param name the name
     * @param description a description of its contents
     * @param version the preset version
     * @param url url for the API
     * @param lastUpdate time and date of last update in milliseconds since the epoch
     */
    protected ResourceConfiguration(@NonNull String id, @NonNull String name, @Nullable String description, @Nullable String version, @Nullable String url,
            @NonNull String lastUpdate) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.version = version;
        this.url = url;
        long tmpLastupdate;
        try {
            tmpLastupdate = Long.parseLong(lastUpdate);
        } catch (Exception e) {
            tmpLastupdate = 0;
        }
        this.lastupdate = tmpLastupdate;
    }

    /**
     * Set this entry to active
     * 
     * @param b if true this is active
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Check if the entry is active
     * 
     * @return true if active
     */
    public boolean isActive() {
        return active;
    }
}
