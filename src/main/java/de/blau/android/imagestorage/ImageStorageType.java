package de.blau.android.imagestorage;

import androidx.annotation.NonNull;
import de.blau.android.R;
import de.blau.android.osm.Tags;

public enum ImageStorageType {
    LOCAL(0, ""), WIKIMEDIA_COMMONS(R.string.storage_wikimedia_commons, Tags.KEY_WIKIMEDIA_COMMONS), PANORAMAX(R.string.storage_panoramax,
            Tags.KEY_PANORAMAX), MAPILLARY(R.string.storage_mapillary, Tags.KEY_MAPILLARY);

    private final int    nameRes;
    private final String key;

    /**
     * Create an ImageStorageType
     * 
     * @param nameRes a resource id for a display name
     * @param key the OSM key for tagging on elements
     */
    ImageStorageType(int nameRes, @NonNull String key) {
        this.nameRes = nameRes;
        this.key = key;
    }

    /**
     * Get a resource id for the name of the storage
     * 
     * @return a string resource id
     */
    public int getNameRes() {
        return nameRes;
    }

    /**
     * Get the appropriate key for the OSM tag
     * 
     * @return the key
     */
    @NonNull
    public String getKey() {
        return key;
    }
}
