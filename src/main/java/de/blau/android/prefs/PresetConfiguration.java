package de.blau.android.prefs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Data structure class for Preset data
 * 
 * @author Jan
 * @author Simon
 */
public class PresetConfiguration extends ResourceConfiguration {

    private static final long serialVersionUID = 1L;

    public final String  shortDescription;
    public final boolean useTranslations;

    /**
     * Construct a new configuration for a Preset
     * 
     * @param id the Preset id
     * @param name the Preset name
     * @param version the preset version
     * @param shortDescription the name the author gave the preset
     * @param description a description of its contents
     * @param url an url or an empty string
     * @param lastUpdate time and date of last update in milliseconds since the epoch
     * @param active true if the Preset is active
     * @param useTranslations if true translations included with the preset will be used
     */
    public PresetConfiguration(@NonNull String id, @NonNull String name, @Nullable String version, @Nullable String shortDescription,
            @Nullable String description, @NonNull String url, @NonNull String lastUpdate, boolean active, boolean useTranslations) {
        super(id, name, description, version, url, lastUpdate);
        this.shortDescription = shortDescription;
        this.useTranslations = useTranslations;
    }
}
