package de.blau.android.layer.mapillary;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mapbox.geojson.CoordinateContainer;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.osm.BoundingBox;
import de.blau.android.util.DateFormatter;
import de.blau.android.util.GeoJson;
import de.blau.android.util.rtree.BoundedObject;

/**
 * Wrapper around mapboxes Feature class makes the object serializable and usable in an RTree
 * 
 * @author Simon Poole
 *
 */
class MapillarySequence implements BoundedObject, Serializable {

    private static final long serialVersionUID = 1;

    private static final String DEBUG_TAG = MapillarySequence.class.getSimpleName();

    static final String         COORDINATE_PROPERTIES_KEY = "coordinateProperties";
    static final String         IMAGE_KEYS_KEY            = "image_keys";
    private static final String CAPTURED_AT_KEY           = "captured_at";
    private static final String USERNAME_KEY              = "username";
    private static final String KEY_KEY                   = "key";
    static final String         CAS_KEY                   = "cas";

    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    Feature     feature;
    String      key;
    BoundingBox box = null;

    /**
     * Constructor
     * 
     * @param f the Feature to wrap
     */
    public MapillarySequence(@NonNull Feature f) {
        this.feature = f;
        key = f.getStringProperty(KEY_KEY);
    }

    @Override
    public BoundingBox getBounds() {
        if (box == null) {
            box = GeoJson.getBounds(feature.geometry());
        }
        return box;
    }

    /**
     * Get the wrapped Feature object
     * 
     * @return the Feature
     */
    @Nullable
    public Feature getFeature() {
        return feature;
    }

    /**
     * Get the keys for the individual images in teh sequence
     * 
     * @return a JsonArray of strings
     */
    @Nullable
    JsonArray getImageKeys() {
        JsonObject coordinateProperties = (JsonObject) feature.getProperty(COORDINATE_PROPERTIES_KEY);
        return coordinateProperties.get(IMAGE_KEYS_KEY).getAsJsonArray();
    }

    /**
     * Get the Points making up the geometry of the sequence
     * 
     * @return a List of Point objects
     */
    @SuppressWarnings("unchecked")
    @Nullable
    List<Point> getPoints() {
        return ((CoordinateContainer<List<Point>>) feature.geometry()).coordinates();
    }

    /**
     * Get a new MapillaryImage object
     * 
     * @param pos the position in the sequence the object is as
     * @return the MapillaryImage
     */
    @NonNull
    MapillaryImage getImage(int pos) {
        MapillaryImage image = new MapillaryImage();
        image.index = pos;
        image.sequenceKey = key;
        image.box = getBounds();
        String capturedAt = feature.getStringProperty(CAPTURED_AT_KEY);
        if (capturedAt != null) {
            try {
                image.capturedAt = DateFormatter.getUtcFormat(TIMESTAMP_FORMAT).parse(capturedAt).getTime();
            } catch (ParseException e) {
                Log.e(DEBUG_TAG, "Unparseable date for " + image.sequenceKey + " " + capturedAt);
            }
        }
        image.username = feature.getStringProperty(USERNAME_KEY);
        return image;
    }

    /**
     * Serialize this object
     * 
     * @param out ObjectOutputStream to write to
     * @throws IOException
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeUTF(feature.toJson());
        out.writeObject(box);
    }

    /**
     * Recreate the object for serialized state
     * 
     * @param in ObjectInputStream to write from
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        String jsonString = in.readUTF();
        feature = Feature.fromJson(jsonString);
        key = feature.getStringProperty(KEY_KEY);
        box = (BoundingBox) in.readObject();
    }
}
