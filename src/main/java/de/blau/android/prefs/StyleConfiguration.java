package de.blau.android.prefs;

import java.io.Serializable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Data structure class for image stores
 */
public class StyleConfiguration implements Serializable {
    private static final long serialVersionUID = 1L;

    public final String  id;
    public final String  name;
    public final String  description;
    public final String  version;
    public final long    lastupdate;
    public final String  url;
    public final boolean active;

    /**
     * Construct a new class describing a ImageStore
     * 
     * @param id internal id
     * @param name the name
     * @param description a description of its contents 
     * @param version the preset version
     * @param url url for the API
     * @param lastUpdate time and date of last update in milliseconds since the epoch
     * @param active if true the entry is in use
     */
    public StyleConfiguration(@NonNull String id, @NonNull String name, @NonNull String description, @NonNull String version, @Nullable String url,
            @NonNull String lastUpdate, boolean active) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.version = version;
        this.url = url;
        this.active = active;
        long tmpLastupdate;
        try {
            tmpLastupdate = Long.parseLong(lastUpdate);
        } catch (Exception e) {
            tmpLastupdate = 0;
        }
        this.lastupdate = tmpLastupdate;
    }
}
