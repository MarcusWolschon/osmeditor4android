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
import android.support.annotation.NonNull;
import android.util.Log;
import de.blau.android.R;
import de.blau.android.util.DateFormatter;
import de.blau.android.util.SavingHelper;

/**
 * A custom task/bug provided by the user, based on the OSMOSE format 
 * 
 * @author Simon Poole
 */
public final class CustomBug extends Bug implements Serializable {

    private static final String DEBUG_TAG = CustomBug.class.getSimpleName();

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Parse an InputStream containing bugs in JSON format
     * 
     * @param is the InputStream
     * @return a List of CustomBugs
     * @throws IOException for JSON reading issues
     * @throws NumberFormatException if a number conversion fails
     */
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
        return "Custom: " + (subtitle != null && subtitle.length() != 0 ? subtitle : title);
    }

    @Override
    public String getDescription(@NonNull Context context) {
        return getBugDescription(context, R.string.custom_bug);
    }

    @Override
    public String getLongDescription(@NonNull Context context, boolean withElements) {
        return getBugLongDescription(context, R.string.custom_bug, withElements);
    }

    @Override
    public String bugFilterKey() {
        return "CUSTOM";
    }

    @Override
    public boolean canBeUploaded() {
        return false;
    }

    /**
     * Generate a descriptive JSON header
     * 
     * @return a String containing the header
     */
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
