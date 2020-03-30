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
import androidx.annotation.NonNull;
import android.util.Log;
import de.blau.android.R;
import de.blau.android.util.DateFormatter;
import de.blau.android.util.SavingHelper;

/**
 * An OSMOSE bug
 * 
 * @author Simon Poole
 */
public final class OsmoseBug extends Bug implements Serializable {

    private static final String DEBUG_TAG = OsmoseBug.class.getSimpleName();

    /**
     * 
     */
    private static final long serialVersionUID = 4L;

    // we currently don't actually use these fields
    private int    item;
    private int    source;
    private int    bugclass; // class
    private long   subclass;
    private String username;

    /**
     * Parse an InputStream containing Osmose task data
     * 
     * @param is the InputString
     * @return a List of OsmoseBug
     * @throws IOException for JSON reading issues
     * @throws NumberFormatException if a number conversion fails
     */
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
                        bug.subclass = reader.nextLong();
                        bug.subtitle = reader.nextString();
                        bug.title = reader.nextString();
                        bug.level = reader.nextInt();
                        try {
                            bug.update = DateFormatter.getDate(DATE_PATTERN_OSMOSE_BUG_UPDATED_AT, reader.nextString()).getTime();
                        } catch (java.text.ParseException pex) {
                            bug.update = new Date().getTime();
                        }
                        bug.username = reader.nextString();
                        reader.endArray();
                        result.add(bug);
                    }
                    reader.endArray();
                }
            }
            reader.endObject();
        } catch (IOException | IllegalStateException | NumberFormatException ex) {
            Log.d(DEBUG_TAG, "Parse error, ignoring " + ex);
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

    @Override
    public String getDescription() {
        return "Osmose: " + (notEmpty(subtitle) ? subtitle : title);
    }

    @Override
    public String getDescription(@NonNull Context context) {
        return getBugDescription(context, R.string.osmose_bug);
    }

    @Override
    public String getLongDescription(Context context, boolean withElements) {
        return getBugLongDescription(context, R.string.osmose_bug, withElements);
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
