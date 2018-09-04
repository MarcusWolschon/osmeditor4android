package de.blau.android.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gson.stream.JsonReader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.Log;
import de.blau.android.R;
import de.blau.android.tasks.Task.BitmapWithOffset;
import de.blau.android.util.DateFormatter;
import de.blau.android.util.SavingHelper;

public class MapRouletteTask extends Task {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private static final String DEBUG_TAG = "MapRouletteTask";

    public static BitmapWithOffset cachedIconClosed;
    public static BitmapWithOffset cachedIconChangedClosed;
    public static BitmapWithOffset cachedIconOpen;
    public static BitmapWithOffset cachedIconChanged;

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
     * @throws IOException
     * @throws NumberFormatException
     */
    public static List<MapRouletteTask> parseTasks(InputStream is) throws IOException, NumberFormatException {
        List<MapRouletteTask> result = new ArrayList<>();
        JsonReader reader = new JsonReader(new InputStreamReader(is));
        try {
            reader.beginArray();
            while (reader.hasNext()) {
                reader.beginObject();
                MapRouletteTask task = new MapRouletteTask();
                task.open();
                while (reader.hasNext()) {
                    switch (reader.nextName()) {
                    case "id":
                        task.id = reader.nextLong();
                        Log.d(DEBUG_TAG,"got maproulette task is " + task.id);
                        break;
                    case "parentName":
                        task.parentName = reader.nextString();
                        break;
                    case "parentId":
                        task.parentId = reader.nextLong();
                        Log.d(DEBUG_TAG,"got maproulette task parent " + task.parentId);
                        break;
                    case "point":
                        reader.beginObject();
                        while (reader.hasNext()) {
                            switch (reader.nextName()) {
                            case "lat":
                                task.lat = (int) (reader.nextDouble() * 1E7D);
                                break;
                            case "lng":
                                task.lon = (int) (reader.nextDouble() * 1E7D);
                                break;
                            }
                        }
                        reader.endObject();
                        break;
                    case "status":
                        task.setState(TaskFragment.pos2state(reader.nextInt()));
                        break;
                    // case "modified":
                    // try {
                    // task.update = DateFormatter.getDate(DATE_PATTERN_OSMOSE_BUG_UPDATED_AT,
                    // reader.nextString());
                    // } catch (java.text.ParseException pex) {
                    // // tasks.update = new Date();
                    // }
                    // break;
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
        } finally {
            SavingHelper.close(reader);
        }
        return result;
    }

    @Override
    public void drawBitmapOpen(Context context, Canvas c, float x, float y) {
        drawIcon(context, cachedIconOpen, c, R.drawable.roulette_open, x, y);
    }

    @Override
    public void drawBitmapChanged(Context context, Canvas c, float x, float y) {
        drawIcon(context, cachedIconChanged, c, R.drawable.roulette_changed, x, y);
    }

    @Override
    public void drawBitmapChangedClosed(Context context, Canvas c, float x, float y) {
        drawIcon(context, cachedIconChangedClosed, c, R.drawable.roulette_changed, x, y);
    }

    @Override
    public void drawBitmapClosed(Context context, Canvas c, float x, float y) {
        drawIcon(context, cachedIconClosed, c, R.drawable.roulette_closed, x, y);
    }
}
