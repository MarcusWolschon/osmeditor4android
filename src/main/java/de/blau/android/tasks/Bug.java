package de.blau.android.tasks;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmElementFactory;
import de.blau.android.osm.Relation;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Way;

/**
 * Subset of the fields and functionality for handling OSMOSE style bugs
 * 
 * @author Simon Poole
 */
public abstract class Bug extends Task implements Serializable {

    static final String DEBUG_TAG         = Bug.class.getSimpleName();
    static final int    LEVEL_ERROR       = 1;
    static final int    LEVEL_WARNING     = 2;
    static final int    LEVEL_MINOR_ISSUE = 3;

    /**
     * Date pattern used to parse the update date from a Osmose bug.
     */
    static final String DATE_PATTERN_OSMOSE_BUG_UPDATED_AT = "yyyy-MM-dd HH:mm:ss z";

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    String elems;
    String title;
    String subtitle;
    int    level;
    Date   update;

    /**
     * Default constructor
     */
    protected Bug() {
    }

    @Override
    public String getDescription() {
        return "Bug: " + (subtitle != null && subtitle.length() != 0 ? subtitle : title);
    }

    @Override
    public String getDescription(@NonNull Context context) {
        return getBugDescription(context, R.string.generic_bug);
    }

    /**
     * Get a long description potentially with references to the relevant elements
     * 
     * @param context the Android Context
     * @param withElements if true add descriptions of the elements
     * @return a String containing the description
     */
    public abstract String getLongDescription(Context context, boolean withElements);

    /**
     * Generate a short description of the bug, called from sub classes
     * 
     * @param context Android COntext
     * @param bugNameRes the resource id of the name of the bug type
     * @return a String with a short description
     */
    protected String getBugDescription(@NonNull Context context, int bugNameRes) {
        String[] states = context.getResources().getStringArray(R.array.bug_state);
        return context.getString(R.string.bug_description, context.getString(bugNameRes), subtitle != null && subtitle.length() != 0 ? subtitle : title,
                states[getState().ordinal()]);
    }

    /**
     * Get a long description potentially with references to the relevant elements, called from sub classes
     * 
     * @param context the Android Context
     * @param bugNameRes the resource id of the name of the bug type
     * @param withElements if true add descriptions of the elements
     * @return a String containing the description
     */
    protected String getBugLongDescription(@NonNull Context context, int bugNameRes, boolean withElements) {
        StringBuilder result = new StringBuilder(context.getString(R.string.bug_long_description_1, context.getString(bugNameRes), level2string(context),
                subtitle != null && subtitle.length() != 0 ? subtitle : title));
        if (withElements) {
            for (OsmElement osm : getElements()) {
                result.append("<br>");
                if (osm.getOsmVersion() < 0) {
                    result.append(context.getString(R.string.bug_element_1, osm.getName(), osm.getOsmId()));
                } else {
                    result.append(context.getString(R.string.bug_element_2, osm.getName(), osm.getDescription(false)));
                }
                result.append("<br><br>");
            }
        }
        result.append(context.getString(R.string.bug_long_description_2, update, id));
        return result.toString();
    }

    /**
     * Get the timestamp of the most recent change.
     * 
     * @return The timestamp of the most recent change.
     */
    @Override
    public Date getLastUpdate() {
        return update;
    }

    /**
     * Return list of elements from OSMOSE style list
     * 
     * This returns fake elements with version -1 for objects not downloaded
     * 
     * @return list of OsmElement
     */
    public final List<OsmElement> getElements() {
        ArrayList<OsmElement> result = new ArrayList<>();
        String[] elements = elems.split("_");
        StorageDelegator storageDelegator = App.getDelegator();
        for (String e : elements) {
            try {
                if (elems.startsWith("way")) {
                    OsmElement osm = storageDelegator.getOsmElement(Way.NAME, Long.valueOf(e.substring(3)));
                    if (osm == null) {
                        osm = OsmElementFactory.createWay(Long.valueOf(e.substring(3)), -1, -1, (byte) -1);
                    }
                    result.add(osm);
                } else if (elems.startsWith("node")) {
                    OsmElement osm = storageDelegator.getOsmElement(Node.NAME, Long.valueOf(e.substring(4)));
                    if (osm == null) {
                        osm = OsmElementFactory.createNode(Long.valueOf(e.substring(4)), -1, -1, (byte) -1, 0, 0);
                    }
                    result.add(osm);
                } else if (elems.startsWith("relation")) {
                    OsmElement osm = storageDelegator.getOsmElement(Relation.NAME, Long.valueOf(e.substring(8)));
                    if (osm == null) {
                        osm = OsmElementFactory.createRelation(Long.valueOf(e.substring(8)), -1, -1, (byte) -1);
                    }
                    result.add(osm);
                }
            } catch (Exception ex) {
                Log.d(DEBUG_TAG, "couldn't retrieve element " + elems + " " + ex);
            }
        }
        return result;
    }

    /**
     * Get the severity level of the bug as a String
     * 
     * @param context Android Context
     * @return a String with the level
     */
    final String level2string(Context context) {
        switch (level) {
        case LEVEL_ERROR:
            return context.getString(R.string.error);
        case LEVEL_WARNING:
            return context.getString(R.string.warning);
        case LEVEL_MINOR_ISSUE:
            return context.getString(R.string.minor_issue);
        default:
            return context.getString(R.string.unknown_error_level);
        }
    }

    /**
     * Get the severity level of the bug
     * 
     * @return the level value
     */
    public final int getLevel() {
        return level;
    }
}
