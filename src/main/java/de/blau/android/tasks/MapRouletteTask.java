package de.blau.android.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.gson.stream.JsonReader;

import android.content.Context;
import android.graphics.Canvas;
import android.util.Log;
import androidx.annotation.NonNull;
import de.blau.android.App;
import de.blau.android.R;

public class MapRouletteTask extends LongIdTask {

    private static final long serialVersionUID = 2L;

    private static final String DEBUG_TAG = "MapRouletteTask";

    private static final String ID          = "id";
    private static final String PARENT_NAME = "parentName";
    private static final String PARENT_ID   = "parentId";
    private static final String POINT       = "point";
    private static final String STATUS      = "status";
    private static final String LON         = "lng";
    private static final String LAT         = "lat";

    protected static BitmapWithOffset cachedIconClosed;
    protected static BitmapWithOffset cachedIconChangedClosed;
    protected static BitmapWithOffset cachedIconOpen;
    protected static BitmapWithOffset cachedIconChanged;

    private long   parentId   = -1;
    private String parentName = null;

    @Override
    public String getDescription() {
        return "MapRoulette: " + parentName;
    }

    @Override
    public String getDescription(Context context) {
        return getDescription();
    }

    @Override
    public Date getLastUpdate() {
        return new Date(); // FIXME
    }

    @Override
    public String bugFilterKey() {
        return "MAPROULETTE";
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
        final Map<Long, MapRouletteChallenge> challenges = App.getTaskStorage().getChallenges();
        try (JsonReader reader = new JsonReader(new InputStreamReader(is))) {
            reader.beginArray();
            while (reader.hasNext()) {
                reader.beginObject();
                MapRouletteTask task = new MapRouletteTask();
                task.open();
                while (reader.hasNext()) {
                    switch (reader.nextName()) {
                    case ID:
                        task.id = reader.nextLong();
                        Log.d(DEBUG_TAG, "got maproulette task is " + task.id);
                        break;
                    case PARENT_NAME:
                        task.parentName = reader.nextString();
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
                        task.setState(State.values()[reader.nextInt()]); // FIXME, this assumes that the state mapping
                                                                         // doesn't change
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
    public void drawBitmapOpen(Context context, Canvas c, float x, float y, boolean selected) {
        drawIcon(context, cachedIconOpen, c, R.drawable.roulette_open, x, y, selected);
    }

    @Override
    public void drawBitmapChanged(Context context, Canvas c, float x, float y, boolean selected) {
        drawIcon(context, cachedIconChanged, c, R.drawable.roulette_changed, x, y, selected);
    }

    @Override
    public void drawBitmapChangedClosed(Context context, Canvas c, float x, float y, boolean selected) {
        drawIcon(context, cachedIconChangedClosed, c, R.drawable.roulette_closed_changed, x, y, selected);
    }

    @Override
    public void drawBitmapClosed(Context context, Canvas c, float x, float y, boolean selected) {
        drawIcon(context, cachedIconClosed, c, R.drawable.roulette_closed, x, y, selected);
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
}
