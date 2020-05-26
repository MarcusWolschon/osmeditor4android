package de.blau.android.resources;

import de.blau.android.osm.BoundingBox;

/**
 * Small container class for layer configuration information
 * @author simon
 *
 */
public class LayerEntry {
    String      id;
    String      title;
    String      tileUrl;
    String      thumbnailUrl;
    String      license;
    String      provider;
    BoundingBox box;
    double      gsd;
    long        startDate = -1;
    long        endDate   = -1;

    @Override
    public String toString() {
        return title;
    }
}
