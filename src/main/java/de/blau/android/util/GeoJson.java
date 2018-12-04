package de.blau.android.util;

import java.util.ArrayList;
import java.util.List;

import com.mapbox.geojson.CoordinateContainer;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

import android.support.annotation.NonNull;
import de.blau.android.osm.BoundingBox;

public class GeoJson {
    /**
     * Calculate the bounding boxes of a GeoJson Polygon feature
     * 
     * @param f The GeoJson feature
     * @return a List of BoundingBoxes, empty in no Polygons were found
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public static List<BoundingBox> getBoundingBoxes(@NonNull Feature f) {
        List<BoundingBox> result = new ArrayList<>();
        Geometry g = f.geometry();
        if (g instanceof Polygon) {
            for (List<Point> l : ((CoordinateContainer<List<List<Point>>>) g).coordinates()) {
                BoundingBox box = null;
                for (Point p : l) {
                    if (box == null) {
                        box = new BoundingBox(p.longitude(), p.latitude());
                    } else {
                        box.union(p.longitude(), p.latitude());
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
