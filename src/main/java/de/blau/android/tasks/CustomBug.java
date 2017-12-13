package de.blau.android.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gson.stream.JsonReader;

import android.content.Context;
import android.util.Log;
import de.blau.android.R;
import de.blau.android.osm.OsmElement;
import de.blau.android.util.DateFormatter;
import de.blau.android.util.SavingHelper;

/**
 * A bug in the OpenStreetBugs database, or a prospective new bug.
 * 
 * @author Simon Poole
 */
public class CustomBug extends Bug implements Serializable {

    private static final String DEBUG_TAG = CustomBug.class.getSimpleName();

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public static List<CustomBug> parseBugs(InputStream is) throws IOException, NumberFormatException {
        ArrayList<CustomBug> result = new ArrayList<>();
        JsonReader reader = new JsonReader(new InputStreamReader(is));
        try {
            // key object
            String key = null;
            reader.beginObject();
            while (reader.hasNext()) {
                key = reader.nextName(); //
                if (key.equals("description")) {
                    reader.skipValue();
                } else if (key.equals("errors")) {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        CustomBug bug = new CustomBug();
                        reader.beginArray();
                        bug.lat = (int) (reader.nextDouble() * 1E7D);
                        bug.lon = (int) (reader.nextDouble() * 1E7D);
                        bug.id = reader.nextLong();
                        bug.elems = reader.nextString();
                        bug.subtitle = reader.nextString();
                        bug.title = reader.nextString();
                        bug.level = reader.nextInt();
                        try {
                            bug.update = DateFormatter.getDate(DATE_PATTERN_OSMOSE_BUG_UPDATED_AT, reader.nextString());
                        } catch (java.text.ParseException pex) {
                            bug.update = new Date();
                        }
                        reader.endArray();
                        result.add(bug);
                    }
                    reader.endArray();
                }
            }
            reader.endObject();
        } catch (IOException ex) {
            Log.d(DEBUG_TAG, "Ignoring " + ex);
        } finally {
            SavingHelper.close(reader);
        }
        return result;
    }

    /**
     * Default constructor
     */
    private CustomBug() {
        open();
    }

    /**
     * Get a string descriptive of the bug. This is intended to be used as a short bit of text representative of the
     * bug.
     * 
     * @return The first comment of the bug.
     */
    @Override
    public String getDescription() {
        return "Custom: " + (subtitle.length() != 0 ? subtitle : title);
    }

    public String getLongDescription(Context context, boolean withElements) {
        String result = "Custom: " + level2string(context) + "<br><br>" + (subtitle.length() != 0 ? subtitle : title) + "<br>";
        if (withElements) {
            for (OsmElement osm : getElements()) {
                if (osm.getOsmVersion() >= 0) {
                    result = result + "<br>" + osm.getName() + " (" + context.getString(R.string.openstreetbug_not_downloaded) + ") #" + osm.getOsmId();
                } else {
                    result = result + "<br>" + osm.getName() + " " + osm.getDescription(false);
                }
                result = result + "<br><br>";
            }
        }
        result = result + context.getString(R.string.openstreetbug_last_updated) + ": " + update + " " + context.getString(R.string.id) + ": " + id;
        return result;
    }

    @Override
    public String bugFilterKey() {
        return "CUSTOM";
    }

    @Override
    public boolean canBeUploaded() {
        return false;
    }

    public static String headerToJSON() {
        return "\"description\": [" + "\"lat\",\"lon\",\"error_id\",\"elems\"," + "\"subtitle\",\"title\",\"level\",\"update\"" + "],";
    }

    /**
     * Generate a JSON representation this element
     * 
     * @return JSON format string
     */
    public String toJSON() {
        StringBuilder result = new StringBuilder("[");
        result.append("\"");
        result.append(lat / 1E7D);
        result.append("\",\"");
        result.append(lon / 1E7D);
        result.append("\",\"");
        result.append(id);
        result.append("\",\"");
        result.append(elems);
        result.append("\",\"");
        result.append(subtitle);
        result.append("\",\"");
        result.append(title);
        result.append("\",\"");
        result.append(level);
        result.append("\",\"");
        result.append(DateFormatter.getFormattedString(DATE_PATTERN_OSMOSE_BUG_UPDATED_AT, update));
        result.append("\"]");
        return result.toString();
    }
}
