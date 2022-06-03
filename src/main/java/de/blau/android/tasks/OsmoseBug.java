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
import androidx.annotation.NonNull;
import de.blau.android.R;
import de.blau.android.util.DateFormatter;
import de.blau.android.util.collections.LongPrimitiveList;

/**
 * An OSMOSE bug
 * 
 * @author Simon Poole
 */
public final class OsmoseBug extends Bug implements Serializable {
    private static final long serialVersionUID = 4L;

    private static final String DEBUG_TAG = OsmoseBug.class.getSimpleName();

    private static final String OSMOSE_ISSUES    = "issues";
    private static final String OSMOSE_LAT       = "lat";
    private static final String OSMOSE_LON       = "lon";
    private static final String OSMOSE_ID        = "id";
    private static final String OSMOSE_ITEM      = "item";
    private static final String OSMOSE_CLASS     = "class";
    private static final String OSMOSE_UPDATE    = "update";
    private static final String OSMOSE_OSM_IDS   = "osm_ids";
    private static final String OSMOSE_NODES     = "nodes";
    private static final String OSMOSE_WAYS      = "ways";
    private static final String OSMOSE_RELATIONS = "relations";
    private static final String OSMOSE_TITLE     = "title";
    private static final String OSMOSE_SUBTITLE  = "subtitle";
    private static final String OSMOSE_AUTO      = "auto";
    private static final String OSMOSE_LEVEL     = "level";

    private String item;
    private int    bugclass; // class

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
        try (JsonReader reader = new JsonReader(new InputStreamReader(is))) {
            // key object
            String key = null;
            reader.beginObject();
            while (reader.hasNext()) {
                key = reader.nextName(); //
                if (OSMOSE_ISSUES.equals(key)) {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        OsmoseBug bug = new OsmoseBug();
                        reader.beginObject();
                        while (reader.hasNext()) {
                            String jsonName = reader.nextName();
                            switch (jsonName) {
                            case OSMOSE_LAT:
                                bug.lat = (int) (reader.nextDouble() * 1E7D);
                                break;
                            case OSMOSE_LON:
                                bug.lon = (int) (reader.nextDouble() * 1E7D);
                                break;
                            case OSMOSE_ID:
                                bug.id = reader.nextString();
                                break;
                            case OSMOSE_ITEM:
                                bug.item = reader.nextString();
                                break;
                            case OSMOSE_CLASS:
                                bug.bugclass = reader.nextInt();
                                break;
                            case OSMOSE_UPDATE:
                                bug.update = getDateFromString(reader);
                                break;
                            case OSMOSE_OSM_IDS:
                                reader.beginObject();
                                while (reader.hasNext()) {
                                    String elemName = reader.nextName();
                                    switch (elemName) {
                                    case OSMOSE_NODES:
                                        bug.nodes = getElementIds(reader);
                                        break;
                                    case OSMOSE_WAYS:
                                        bug.ways = getElementIds(reader);
                                        break;
                                    case OSMOSE_RELATIONS:
                                        bug.relations = getElementIds(reader);
                                        break;
                                    default:
                                        reader.skipValue();
                                    }
                                }
                                reader.endObject();
                                break;
                            case OSMOSE_TITLE:
                                reader.beginObject();
                                while (reader.hasNext()) {
                                    String titleType = reader.nextName();
                                    if (OSMOSE_AUTO.equals(titleType)) {
                                        bug.title = reader.nextString();
                                    } else {
                                        reader.skipValue();
                                    }
                                }
                                reader.endObject();
                                break;
                            case OSMOSE_SUBTITLE:
                                if (reader.peek() != com.google.gson.stream.JsonToken.NULL) {
                                    reader.beginObject();
                                    while (reader.hasNext()) {
                                        String titleType = reader.nextName();
                                        if (OSMOSE_AUTO.equals(titleType)) {
                                            bug.subtitle = reader.nextString();
                                        } else {
                                            reader.skipValue();
                                        }
                                    }
                                    reader.endObject();
                                } else {
                                    reader.nextNull();
                                }
                                break;
                            case OSMOSE_LEVEL:
                                bug.level = reader.nextInt();
                                break;
                            default:
                                reader.skipValue();
                            }
                        }
                        reader.endObject();
                        result.add(bug);
                    }
                    reader.endArray();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        } catch (IOException | IllegalStateException | NumberFormatException ex) {
            Log.d(DEBUG_TAG, "Parse error, ignoring " + ex);
        }
        return result;
    }

    /**
     * Get a date from a json string
     * 
     * @param reader the JsonReader
     * @return a long indicating seconds since the unix epoch
     * @throws IOException if Json parsing fails
     */
    private static long getDateFromString(@NonNull JsonReader reader) throws IOException {
        try {
            return DateFormatter.getDate(DATE_PATTERN_OSMOSE_BUG_UPDATED_AT, reader.nextString()).getTime();
        } catch (java.text.ParseException pex) {
            return new Date().getTime();
        }
    }

    /**
     * Add an JsonArray of long ids to a list
     * 
     * @param reader the JsonReader
     * @return a LongPrimitiveList
     * @throws IOException if reading the Json fails
     */
    @NonNull
    private static LongPrimitiveList getElementIds(@NonNull JsonReader reader) throws IOException {
        LongPrimitiveList list = new LongPrimitiveList();
        reader.beginArray();
        while (reader.hasNext()) {
            list.add(reader.nextLong());
        }
        reader.endArray();
        return list;
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

    /**
     * @return the item
     */
    public String getOsmoseItem() {
        return item;
    }

    /**
     * @return the bugclass
     */
    public int getOsmoseClass() {
        return bugclass;
    }

    @Override
    public boolean equals(Object obj) { // NOSONAR
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof OsmoseBug)) {
            return false;
        }
        OsmoseBug other = (OsmoseBug) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }
}
