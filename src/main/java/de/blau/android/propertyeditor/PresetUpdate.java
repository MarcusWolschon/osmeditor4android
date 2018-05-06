package de.blau.android.propertyeditor;

import android.support.annotation.Nullable;
import de.blau.android.osm.OsmElement.ElementType;

/**
 * Interface for updating preset related things
 */
interface PresetUpdate {
    /**
     * 
     * @param type type of OsmElement or null to not change
     */
    void update(@Nullable ElementType type);
}
