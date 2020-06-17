package de.blau.android.layer.mapillary;

import java.io.Serializable;
import java.util.Date;

import de.blau.android.osm.BoundingBox;
import de.blau.android.util.DateFormatter;

/**
 * This is just a small container for passing around information about a specific image in a sequence
 * 
 * @author simon
 *
 */
class MapillaryImage implements Serializable {
    private static final long serialVersionUID = 1L;

    String      sequenceKey;
    String      username;
    long        capturedAt;
    int         index;
    BoundingBox box;

    @Override
    public String toString() {
        return username + " " + index + " " + DateFormatter.getFormattedString("yyyy-MM-dd HH:mm:ss", new Date(capturedAt));
    }
}
