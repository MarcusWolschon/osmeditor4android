package de.blau.android.prefs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Data structure class for styles
 */
public class StyleConfiguration extends ResourceConfiguration {
    private static final long serialVersionUID = 1L;

    public final boolean custom;

    /**
     * Construct a new class describing a style
     * 
     * @param id internal id
     * @param name the name
     * @param description a description of its contents
     * @param version the preset version
     * @param url url for the API
     * @param lastUpdate time and date of last update in milliseconds since the epoch
     * @param custom if true this is a custom entry
     */
    public StyleConfiguration(@NonNull String id, @NonNull String name, @NonNull String description, @NonNull String version, @Nullable String url,
            @NonNull String lastUpdate, boolean custom) {
        super(id, name, description, version, url, lastUpdate);
        this.custom = custom;
    }
}
