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
 * An OSMOSE bug
 * 
 * @author Simon Poole
 */
public class OsmoseBug extends Bug implements Serializable {

    private static final String DEBUG_TAG = OsmoseBug.class.getSimpleName();

    /**
     * 
     */
    private static final long serialVersionUID = 2L;

    // we currently don't actually use these fields
    private int    item;
    private int    source;
    private int    bugclass; // class
    private int    subclass;
    private String username;

    public static List<OsmoseBug> parseBugs(InputStream is) throws IOException, NumberFormatException {
        List<OsmoseBug> result = new ArrayList<>();
        JsonReader reader = new JsonReader(new InputStreamReader(is));
        try {
            // key object
            String key = null;
            reader.beginObject();
            while (reader.hasNext()) {
                key = reader.nextName(); //
                if ("description".equals(key)) {
                    reader.skipValue();
                } else if ("errors".equals(key)) {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        OsmoseBug bug = new OsmoseBug();
                        reader.beginArray();
                        bug.lat = (int) (reader.nextDouble() * 1E7D);
                        bug.lon = (int) (reader.nextDouble() * 1E7D);
                        bug.id = reader.nextLong();
                        bug.item = reader.nextInt();
                        bug.source = reader.nextInt();
                        bug.bugclass = reader.nextInt();
                        bug.elems = reader.nextString();
                        bug.subclass = reader.nextInt();
                        bug.subtitle = reader.nextString();
                        bug.title = reader.nextString();
                        bug.level = reader.nextInt();
                        try {
                            bug.update = DateFormatter.getDate(DATE_PATTERN_OSMOSE_BUG_UPDATED_AT, reader.nextString());
                        } catch (java.text.ParseException pex) {
                            bug.update = new Date();
                        }
                        bug.username = reader.nextString();
                        reader.endArray();
                        result.add(bug);
                    }
                    reader.endArray();
                }
            }
            reader.endObject();
        } catch (IOException | IllegalStateException ex) {
            Log.d(DEBUG_TAG, "Ignoring " + ex);
        } finally {
            SavingHelper.close(reader);
        }
        return result;
    }

    /**
     * Used for when parsing API output
     */
    private OsmoseBug() {
        open();
    }

    /**
     * Get a string descriptive of the bug. This is intended to be used as a short bit of text representative of the
     * bug.
     * 
     * @return The the subtitle, or if not present the title of the bug.
     */
    @Override
    public String getDescription() {
        return "Osmose: " + (subtitle != null && subtitle.length() != 0 ? subtitle : title);
    }

    @Override
    public String getLongDescription(Context context, boolean withElements) {
        StringBuilder result = new StringBuilder("Osmose: " + level2string(context) + "<br><br>" + (subtitle != null && subtitle.length() != 0 ? subtitle : title) + "<br>");
        if (withElements) {
            for (OsmElement osm : getElements()) {
                if (osm.getOsmVersion() >= 0) {
                    result.append("<br>" + osm.getName() + " (" + context.getString(R.string.openstreetbug_not_downloaded) + ") #" + osm.getOsmId());
                } else {
                    result.append("<br>" + osm.getName() + " " + osm.getDescription(false));
                }
                result.append("<br><br>");
            }
        }
        result.append(context.getString(R.string.openstreetbug_last_updated) + ": " + update + " " + context.getString(R.string.id) + ": " + id);
        return result.toString();
    }

    @Override
    public String bugFilterKey() {
        switch (level) {
        case LEVEL_ERROR:
            return "OSMOSE_ERROR";
        case LEVEL_WARNING:
            return "OSMOSE_WARNING";
        case LEVEL_MINOR_ISSUE:
            return "OSMOSE_MINOR_ISSUE";
        default:
            return "?";
        }
    }
}
