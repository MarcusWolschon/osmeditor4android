package de.blau.android.tasks;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.GeometryAdapterFactory;
import com.mapbox.geojson.gson.GeoJsonAdapterFactory;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.util.GeoJSONConstants;

public class MapRouletteTask extends LongIdTask {

    private static final long serialVersionUID = 4L;

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, MapRouletteTask.class.getSimpleName().length());
    private static final String DEBUG_TAG = MapRouletteTask.class.getSimpleName().substring(0, TAG_LEN);

    static final String FILTER_KEY = "MAPROULETTE";

    private static final String MR_ID       = "id";
    private static final String PARENT_NAME = "parentName";
    private static final String PARENT_ID   = "parentId";
    private static final String POINT       = "point";
    private static final String STATUS      = "status";
    private static final String LON         = "lng";
    private static final String LAT         = "lat";
    private static final String GEOMETRIES  = "geometries";
    private static final String MR_BLURB    = "blurb";

    protected static BitmapWithOffset cachedIconRouletteClosed;
    protected static BitmapWithOffset cachedIconChangedRouletteClosed;
    protected static BitmapWithOffset cachedIconRouletteOpen;
    protected static BitmapWithOffset cachedIconRouletteChanged;

    private long              parentId   = -1;
    private String            parentName = null;
    private String            blurb      = null;
    private FeatureCollection features   = null;

    /**
     * Setup the icon caches
     * 
     * @param context android Context
     * @param hwAccelerated true if the Canvas is hw accelerated
     */
    public static void setupIconCache(@NonNull Context context, boolean hwAccelerated) {
        cachedIconRouletteOpen = getIcon(context, R.drawable.roulette_open, hwAccelerated);
        cachedIconRouletteChanged = getIcon(context, R.drawable.roulette_changed, hwAccelerated);
        cachedIconChangedRouletteClosed = getIcon(context, R.drawable.roulette_closed_changed, hwAccelerated);
        cachedIconRouletteClosed = getIcon(context, R.drawable.roulette_closed, hwAccelerated);
    }

    @Override
    public String getDescription() {
        return "MapRoulette: " + parentName;
    }

    @Override
    public String getDescription(@NonNull Context context) {
        return context.getString(R.string.maproulette_description, parentName);
    }

    @Override
    public Date getLastUpdate() {
        return new Date(); // FIXME
    }

    @Override
    public String bugFilterKey() {
        return FILTER_KEY;
    }

    /**
     * Parse an InputStream containing MapRoulette task data
     * 
     * @param is the InputString
     * @return a List of MapRouletteTask
     * @throws IOException for JSON reading issues
     * @throws NumberFormatException if a number conversion fails
     */
    public static List<MapRouletteTask> parseTasks(@NonNull InputStream is) throws IOException, NumberFormatException {
        List<MapRouletteTask> result = new ArrayList<>();

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapterFactory(GeoJsonAdapterFactory.create());
        gsonBuilder.registerTypeAdapterFactory(GeometryAdapterFactory.create());
        Gson gson = gsonBuilder.create();
        final Map<Long, MapRouletteChallenge> challenges = App.getTaskStorage().getChallenges();
        try (JsonReader reader = gson.newJsonReader(new InputStreamReader(is))) {
            reader.beginArray();
            while (reader.hasNext()) {
                reader.beginObject();
                MapRouletteTask task = new MapRouletteTask();
                task.open();
                while (reader.hasNext()) {
                    switch (reader.nextName()) {
                    case MR_ID:
                        task.id = reader.nextLong();
                        Log.d(DEBUG_TAG, "got maproulette task is " + task.id);
                        break;
                    case PARENT_NAME:
                        task.parentName = reader.nextString();
                        break;
                    case MR_BLURB:
                        task.blurb = reader.nextString();
                        break;
                    case PARENT_ID:
                        task.parentId = reader.nextLong();
                        Log.d(DEBUG_TAG, "got maproulette task parent " + task.parentId);
                        if (!challenges.containsKey(task.parentId)) {
                            challenges.put(task.parentId, null);
                        }
                        break;
                    case POINT:
                        reader.beginObject();
                        while (reader.hasNext()) {
                            switch (reader.nextName()) {
                            case LAT:
                                task.lat = (int) (reader.nextDouble() * 1E7D);
                                break;
                            case LON:
                                task.lon = (int) (reader.nextDouble() * 1E7D);
                                break;
                            default:
                                Log.e(DEBUG_TAG, "Unexpected element in point");
                            }
                        }
                        reader.endObject();
                        break;
                    case STATUS:
                        task.setState(State.values()[reader.nextInt()]);
                        break;
                    case GEOMETRIES:
                        // this should directly be a FeatureCollection, but some tasks don't contain well formed GeoJOSN
                        reader.beginObject();
                        while (reader.hasNext()) {
                            if (GeoJSONConstants.FEATURES.equals(reader.nextName())) {
                                reader.beginArray();
                                TypeAdapter<Feature> adapter = gson.getAdapter(Feature.class);
                                List<Feature> featureList = new ArrayList<>();
                                while (reader.hasNext()) {
                                    featureList.add(adapter.read(reader));
                                }
                                if (!featureList.isEmpty()) {
                                    task.features = FeatureCollection.fromFeatures(featureList);
                                }
                                reader.endArray();
                            } else {
                                reader.skipValue();
                            }
                        }
                        reader.endObject();
                        break;
                    default:
                        reader.skipValue();
                    }
                }
                reader.endObject();
                result.add(task);
            }
            reader.endArray();
        } catch (IOException | IllegalStateException ex) {
            Log.d(DEBUG_TAG, "Ignoring " + ex);
        }
        return result;
    }

    /**
     * @return the parentId
     */
    long getParentId() {
        return parentId;
    }

    @Override
    public void drawBitmapOpen(Canvas c, float x, float y, boolean selected, Paint paint) {
        drawIcon(cachedIconRouletteOpen, c, x, y, selected, paint);
    }

    @Override
    public void drawBitmapChanged(Canvas c, float x, float y, boolean selected, Paint paint) {
        drawIcon(cachedIconRouletteChanged, c, x, y, selected, paint);
    }

    @Override
    public void drawBitmapChangedClosed(Canvas c, float x, float y, boolean selected, Paint paint) {
        drawIcon(cachedIconChangedRouletteClosed, c, x, y, selected, paint);
    }

    @Override
    public void drawBitmapClosed(Canvas c, float x, float y, boolean selected, Paint paint) {
        drawIcon(cachedIconRouletteClosed, c, x, y, selected, paint);
    }

    @Override
    public boolean equals(Object obj) { // NOSONAR
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MapRouletteTask)) {
            return false;
        }
        MapRouletteTask other = ((MapRouletteTask) obj);
        return id == other.id;
    }

    /**
     * @return the geometries
     */
    @Nullable
    public List<Feature> getFeatures() {
        return features != null ? features.features() : null;
    }

    /**
     * Return the name of the challenge
     * 
     * @return the name of the parent challenge
     */
    @Nullable
    public String getChallengeName() {
        return parentName;
    }

    /**
     * @return the blurb
     */
    public String getBlurb() {
        return blurb;
    }

    /**
     * Serialize this object
     * 
     * @param out ObjectOutputStream to write to
     * @throws IOException if writing fails
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeLong(parentId);
        out.writeUTF(parentName);
        out.writeUTF(blurb);
        out.writeUTF(features != null ? features.toJson() : null);
    }

    /**
     * Recreate the object for serialized state
     * 
     * @param in ObjectInputStream to write from
     * @throws IOException if reading fails
     * @throws ClassNotFoundException the target Class isn't defined
     */
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        parentId = in.readLong();
        parentName = in.readUTF();
        blurb = in.readUTF();
        String json = in.readUTF();
        features = json != null ? FeatureCollection.fromJson(json) : null;
    }
}
