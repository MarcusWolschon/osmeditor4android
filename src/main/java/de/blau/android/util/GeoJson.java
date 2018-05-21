package de.blau.android.util;

import java.util.ArrayList;
import java.util.List;

import com.mapbox.services.commons.geojson.Feature;
import com.mapbox.services.commons.geojson.Geometry;
import com.mapbox.services.commons.geojson.Polygon;
import com.mapbox.services.commons.models.Position;

import android.support.annotation.NonNull;
import de.blau.android.osm.BoundingBox;

public class GeoJson {
    /**
     * Calculate the bounding boxes of a GeoJson Polygon feature
     * 
     * @param f The GeoJson feature
     * @return a List of BoundingBoxes, empty in no Polygons were found
     */
    @NonNull
    public static List<BoundingBox> getBoundingBoxes(@NonNull Feature f) {
        List<BoundingBox> result = new ArrayList<>();
        Geometry<?> g = f.getGeometry();
        if (g instanceof Polygon) {
            for (List<Position> l : ((Polygon) g).getCoordinates()) {
                BoundingBox box = null;
                for (Position p : l) {
                    if (box == null) {
                        box = new BoundingBox(p.getLongitude(), p.getLatitude());
                    } else {
                        box.union(p.getLongitude(), p.getLatitude());
                    }
                }
                if (box != null) {
                    result.add(box);
                }
            }
        }
        return result;
    }
}
