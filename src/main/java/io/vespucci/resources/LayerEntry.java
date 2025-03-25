package io.vespucci.resources;

import java.io.Serializable;

import io.vespucci.osm.BoundingBox;

/**
 * Small container class for layer configuration information
 * 
 * @author simon
 *
 */
public class LayerEntry implements Serializable {

    private static final long serialVersionUID = 1L;

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
