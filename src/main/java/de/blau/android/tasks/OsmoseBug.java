package de.blau.android.tasks;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import com.google.gson.stream.JsonReader;

import android.content.Context;
import android.graphics.Canvas;
import android.util.Log;
import androidx.annotation.NonNull;
import de.blau.android.R;
import de.blau.android.util.DateFormatter;

/**
 * An OSMOSE bug
 * 
 * @author Simon Poole
 */
public final class OsmoseBug extends Bug implements Serializable {

    private static final long serialVersionUID = 5L;

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, OsmoseBug.class.getSimpleName().length());
    private static final String DEBUG_TAG = OsmoseBug.class.getSimpleName().substring(0, TAG_LEN);

    private static final String OSMOSE_ISSUES   = "issues";
    private static final String OSMOSE_LAT      = "lat";
    private static final String OSMOSE_LON      = "lon";
    private static final String OSMOSE_ID       = "id";
    private static final String OSMOSE_ITEM     = "item";
    private static final String OSMOSE_CLASS    = "class";
    private static final String OSMOSE_UPDATE   = "update";
    private static final String OSMOSE_TITLE    = "title";
    private static final String OSMOSE_SUBTITLE = "subtitle";
    private static final String OSMOSE_LEVEL    = "level";

    // hardwired stuff used to fixup JOSM derived tests
    private static final String MOUSTACHE_LEFT = "{";
    private static final int    JOSM_ITEM_HIGH = 9200;
    private static final int    JOSM_ITEM_LOW  = 9000;
    private static final int    INDOOR_ITEM    = 2120;

    private String item;
    private int    bugclass; // class

    protected static BitmapWithOffset cachedIconBugClosed;
    protected static BitmapWithOffset cachedIconChangedBugClosed;
    protected static BitmapWithOffset cachedIconBugOpen;
    protected static BitmapWithOffset cachedIconBugChanged;

    /**
     * Setup the icon caches
     * 
     * @param context android Context
     * @param hwAccelerated true if the Canvas is hw accelerated
     */
    public static void setupIconCache(@NonNull Context context, boolean hwAccelerated) {
        cachedIconBugOpen = getIcon(context, R.drawable.bug_open, hwAccelerated);
        cachedIconBugChanged = getIcon(context, R.drawable.bug_changed, hwAccelerated);
        cachedIconChangedBugClosed = getIcon(context, R.drawable.bug_changed_closed, hwAccelerated);
        cachedIconBugClosed = getIcon(context, R.drawable.bug_closed, hwAccelerated);
    }

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
            reader.beginObject();
            while (reader.hasNext()) {
                String key = reader.nextName(); //
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
                            case OSM_IDS:
                                parseIds(reader, bug);
                                break;
                            case OSMOSE_TITLE:
                                bug.setTitle(OsmoseMeta.getAutoString(reader));
                                break;
                            case OSMOSE_SUBTITLE:
                                bug.subtitle = OsmoseMeta.getAutoString(reader);
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
                        fixupJosmSourced(bug);
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
     * JOSM sourced validations have moustache placeholders in title this tries to get rid of them
     * 
     * @param bug the bug to fixup
     */
    private static void fixupJosmSourced(@NonNull OsmoseBug bug) {
        try {
            int item = Integer.parseInt(bug.item);
            if ((item >= JOSM_ITEM_LOW && item < JOSM_ITEM_HIGH || item == INDOOR_ITEM) && bug.subtitle != null && bug.getTitle().contains(MOUSTACHE_LEFT)) {
                bug.setTitle(bug.subtitle);
                bug.subtitle = null;
            }
        } catch (NumberFormatException nfex) {
            Log.e(DEBUG_TAG, "Non nummeric item " + nfex.getMessage());
        }
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
     * Used for when parsing API output
     */
    private OsmoseBug() {
        open();
    }

    @Override
    public String getDescription() {
        return "Osmose: " + (notEmpty(subtitle) ? subtitle : getTitle());
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
    public void drawBitmapOpen(Canvas c, float x, float y, boolean selected) {
        drawIcon(cachedIconBugOpen, c, x, y, selected);
    }

    @Override
    public void drawBitmapChanged(Canvas c, float x, float y, boolean selected) {
        drawIcon(cachedIconBugChanged, c, x, y, selected);
    }

    @Override
    public void drawBitmapChangedClosed(Canvas c, float x, float y, boolean selected) {
        drawIcon(cachedIconChangedBugClosed, c, x, y, selected);
    }

    @Override
    public void drawBitmapClosed(Canvas c, float x, float y, boolean selected) {
        drawIcon(cachedIconBugClosed, c, x, y, selected);
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
        return Objects.equals(id, other.id);
    }
}
