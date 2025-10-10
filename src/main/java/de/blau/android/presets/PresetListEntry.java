package de.blau.android.presets;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.util.KeyValue;
import de.blau.android.util.StringWithDescription;

/**
 * This adds support for deprecated and long description fields
 * 
 * @author simon
 *
 */
public class PresetListEntry extends StringWithDescription implements Serializable {
    private static final long serialVersionUID = 3L;

    private boolean        deprecated;
    private String         longDescription;
    private List<KeyValue> extraTags;

    /**
     * Construct a new instance
     * 
     * @param value the value
     */
    public PresetListEntry(@NonNull final String value) {
        super(value);
    }

    /**
     * Construct a new instance
     * 
     * @param value the value
     * @param description the description of the value
     */
    public PresetListEntry(@NonNull final String value, @Nullable final String description) {
        super(value, description);
    }

    /**
     * Construct a new instance from object of a known type
     * 
     * @param o one of StringWithDescription, ValueWIihCOunt or String
     */
    public PresetListEntry(@NonNull Object o) {
        super(o);
        if (o instanceof PresetListEntry) {
            this.deprecated = ((PresetListEntry) o).deprecated;
            this.longDescription = ((PresetListEntry) o).longDescription;
        }
    }

    /**
     * Check if this value is deprecated
     * 
     * @return true if the value is deprecated
     */
    public boolean isDeprecated() {
        return deprecated;
    }

    /**
     * Set this values deprecated status
     * 
     * @param deprecated if true the value is considered deprecated
     */
    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    /**
     * @return the longDescription
     */
    public String getLongDescription() {
        return longDescription;
    }

    /**
     * @param longDescription the longDescription to set
     */
    public void setLongDescription(String longDescription) {
        this.longDescription = longDescription;
    }

    /**
     * Check if this object has a long description
     * 
     * @return true if this has a long description
     */
    public boolean hasLongDescription() {
        return longDescription != null;
    }

    /**
     * Add an extra tag
     * 
     * @param key tag key
     * @param value tag value
     */
    public void addExtraTag(@NonNull String key, @NonNull String value) {
        if (extraTags == null) {
            extraTags = new ArrayList<>();
        }
        extraTags.add(new KeyValue(key, value));
    }

    /**
     * Get the list of extra tags
     * 
     * @return a list of KeyValue objects, potentially empty
     */
    @NonNull
    public List<KeyValue> getExtraTags() {
        return extraTags != null ? extraTags : new ArrayList<>();
    }

    @Override
    public String toString() {
        return super.toString() + (deprecated ? " (deprecated)" : "");
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), deprecated, longDescription);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof PresetListEntry)) {
            return false;
        }
        PresetListEntry other = (PresetListEntry) obj;
        return deprecated == other.deprecated && Objects.equals(longDescription, other.longDescription);
    }
}
