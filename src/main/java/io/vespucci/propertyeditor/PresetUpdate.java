package io.vespucci.propertyeditor;

import androidx.annotation.Nullable;
import io.vespucci.osm.OsmElement.ElementType;

/**
 * Interface for updating preset related things
 */
public interface PresetUpdate {
    /**
     * 
     * @param type type of OsmElement or null to not change
     */
    void update(@Nullable ElementType type);
}
