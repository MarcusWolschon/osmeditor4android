package de.blau.android.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import com.mapbox.services.api.utils.turf.TurfException;
import com.mapbox.services.api.utils.turf.TurfJoins;
import com.mapbox.services.commons.geojson.Feature;
import com.mapbox.services.commons.geojson.FeatureCollection;
import com.mapbox.services.commons.geojson.Geometry;
import com.mapbox.services.commons.geojson.Point;
import com.mapbox.services.commons.geojson.Polygon;
import com.mapbox.services.commons.models.Position;

import android.content.Context;
import android.content.res.AssetManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import de.blau.android.Logic;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Way;

/**
 * Class to determine certain general properties of the environment we are mapping in from the geographic location
 * 
 * @author simon
 *
 */
public class GeoContext {

    private static final String DEBUG_TAG = "GeoContext";
    private final FeatureCollection imperialAreas;
    private final List<BoundingBox> imperialBoxes;
    private final FeatureCollection driveLeftAreas;
    private final List<BoundingBox> driveLeftBoxes;

    /**
     * Implicit assumption that the list will be short and that it is OK to read in synchronously
     * 
     * @param context Android Context
     */
    public GeoContext(@NonNull Context context) {
        Log.d(DEBUG_TAG, "Initalizing");
        AssetManager assetManager = context.getAssets();

        imperialAreas = getGeoJsonFromAssets(assetManager, "imperial.json");
        imperialBoxes = getBoundingBoxes(imperialAreas);
        driveLeftAreas = getGeoJsonFromAssets(assetManager, "drive-left.json");
        driveLeftBoxes = getBoundingBoxes(driveLeftAreas);
    }

    /**
     * Read a GeoJson file from assets
     * 
     * @param assetManager an AssetManager
     * @param fileName the name of the file
     * @return a GeoJson FeatureCollection
     */
    @Nullable
    FeatureCollection getGeoJsonFromAssets(@NonNull AssetManager assetManager, @NonNull String fileName) {
        InputStream is = null;
        try {
            is = assetManager.open(fileName);
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            StringBuilder sb = new StringBuilder();
            int cp;
            while ((cp = rd.read()) != -1) {
                sb.append((char) cp);
            }
            is.close();
            return FeatureCollection.fromJson(sb.toString());
        } catch (IOException e) {
            Log.e("GeoContext", "Unable to read file " + fileName + " exception " + e);
            return null;
        } finally {
            SavingHelper.close(is);
        }
    }

    /**
     * Check if a coordinate is in a territory that uses imperial units
     * 
     * @param lon longitude
     * @param lat latitude
     * @return true if the territory uses imperial units
     */
    public boolean imperial(double lon, double lat) {
        return inside(lon, lat, imperialAreas);
    }

    /**
     * Check if an OsmElement is in a territory that uses imperial units
     * 
     * @param e the OsmElement
     * @return true if the territory uses imperial units
     */
    public boolean imperial(@NonNull OsmElement e) {
        if (e instanceof Node) {
            return imperial((Node) e);
        } else if (e instanceof Way) {
            return imperial((Way) e);
        } else {
            return false; // FIXME handle relations
        }
    }

    /**
     * Check if a Node is in a territory that uses imperial units
     * 
     * @param n the Node
     * @return true if the territory uses imperial units
     */
    public boolean imperial(@NonNull Node n) {
        if (checkIsIn(n, imperialBoxes)) {
            return imperial(n.getLon()/1E7D, n.getLat()/1E7D);
        }
        return false;
    }

    /**
     * Check if a Way is in a territory that uses imperial units
     * 
     * Note the check uses the centroid of the way, this might not be the best choice
     * 
     * @param w the Way
     * @return true if the territory uses imperial units
     */
    public boolean imperial(@NonNull Way w) {
        if (checkIntersect(w, imperialBoxes)) {
            double[] coords = Logic.centroidLonLat(w);
            return imperial(coords[0], coords[1]);
        }
        return false;
    }

    /**
     * Check if a Ways BoundingBox intersects with another
     * 
     * @param w the Way
     * @param boxes a List of BoundingBoxes to test against
     * @return true if there is an intersection otherwise false
     */
    public boolean checkIntersect(@NonNull Way w, @NonNull List<BoundingBox> boxes) {
        for (BoundingBox box : boxes) {
            if (w.getBounds().intersects(box)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a Node is covered by a BoundingBox
     * 
     * @param n the Node
     * @param boxes a List of BoundingBoxes to test against
     * @return true if there is an intersection otherwise false
     */
    public boolean checkIsIn(@NonNull Node n, @NonNull List<BoundingBox> boxes) {
        for (BoundingBox box : boxes) {
            if (box.isIn(n.getLon(), n.getLat())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a coordinate is in a territory that drives on the left hand sides
     * 
     * @param lon longitude
     * @param lat latitude
     * @return true if the territory that drives on the left hand side
     */
    public boolean driveLeft(double lon, double lat) {
        return inside(lon, lat, driveLeftAreas);       
    }

    /**
     * Check if a Node is in a territory that drives on the left hand side
     * 
     * @param n the Node
     * @return true if the territory that drives on the left hand sides
     */
    public boolean driveLeft(@NonNull Node n) {
        if (checkIsIn(n, driveLeftBoxes)) {
            return driveLeft(n.getLon()/1E7D, n.getLat()/1E7D);
        }
        return false;
    }

    /**
     * Check if a Way is in a territory that drives on the left hand side
     * 
     * Note the check uses the centroid of the way, this might not be the best choice
     * 
     * @param w the Way
     * @return true if the territory that drives on the left hand side
     */
    public boolean driveLeft(@NonNull Way w) {
        if (checkIntersect(w, driveLeftBoxes)) {
            double[] coords = Logic.centroidLonLat(w);
            return driveLeft(coords[0], coords[1]);
        }
        return false;
    }
    
    /**
     * Check if a coordinate is covered by a GeoJson FeatureCollection
     * 
     * @param lon longitude
     * @param lat latitude
     * @param fc  the FeatureCollection
     * @return true if the coordinate is in the bounds of the FeatureCollection
     */
    private boolean inside(double lon, double lat, @NonNull FeatureCollection fc) {
        if (fc != null) {
        Point p = Point.fromCoordinates(Position.fromCoordinates(lon, lat));
        for (Feature f : fc.getFeatures()) {
            Geometry<?> g = f.getGeometry();
            try {
                if (g instanceof Polygon && TurfJoins.inside(p, (Polygon) g)) {
                    return true;
                }
            } catch (TurfException e) {
                return false;
            }
        }
        } else {
            Log.e(DEBUG_TAG, "inside called with null FeatureCollection");
        }
        return false;
    }

    /**
     * Calculate the bounding boxes of a GeoJson FeatureCollection
     * 
     * @param fc The GeoJson feature
     * @return a List of BoundingBoxes, empty in no Polygons were found
     */
    @NonNull
    List<BoundingBox> getBoundingBoxes(@Nullable FeatureCollection fc) {
        List<BoundingBox> result = new ArrayList<>();
        if (fc != null) {
            for (Feature f : fc.getFeatures()) {
                result.addAll(getBoundingBoxes(f));
            }
        } else {
            Log.e(DEBUG_TAG, "getBoundingboxes called with null FeatureCollection");
        }
        return result;
    }

    /**
     * Calculate the bounding boxes of a GeoJson Polygon feature
     * 
     * @param f The GeoJson feature
     * @return a List of BoundingBoxes, empty in no Polygons were found
     */
    @NonNull
    List<BoundingBox> getBoundingBoxes(@NonNull Feature f) {
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
