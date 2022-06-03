package de.blau.android.tasks;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmElementFactory;
import de.blau.android.osm.Relation;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Way;
import de.blau.android.util.collections.LongPrimitiveList;

/**
 * Subset of the fields and functionality for handling OSMOSE style bugs
 * 
 * @author Simon Poole
 */
public abstract class Bug extends Task implements Serializable {

    private static final long serialVersionUID = 3L;

    private static final String DEBUG_TAG = Bug.class.getSimpleName();

    protected static final int LEVEL_ERROR       = 1;
    protected static final int LEVEL_WARNING     = 2;
    protected static final int LEVEL_MINOR_ISSUE = 3;

    /**
     * Date pattern used to parse the update date from a Osmose bug.
     */
    static final String DATE_PATTERN_OSMOSE_BUG_UPDATED_AT = "yyyy-MM-dd HH:mm:ss z";

    protected String id;

    protected LongPrimitiveList nodes;
    protected LongPrimitiveList ways;
    protected LongPrimitiveList relations;
    protected String            title;
    protected String            subtitle;
    protected int               level;
    protected long              update;   // update date in ms since the epoch

    /**
     * Default constructor
     */
    protected Bug() {
    }

    /**
     * Get the id
     * 
     * @return the id
     */
    String getId() {
        return id;
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
        String state = states[getState().ordinal()];
        String bugName = context.getString(bugNameRes);
        if (notEmpty(title) && notEmpty(subtitle)) {
            return context.getString(R.string.bug_description_2, bugName, title, subtitle, state);
        } else {
            return context.getString(R.string.bug_description, bugName, notEmpty(title) ? title : (notEmpty(subtitle) ? subtitle : ""), state);
        }
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
                notEmpty(title) ? title : "", notEmpty(subtitle) ? subtitle : ""));
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
        result.append(context.getString(R.string.bug_long_description_2, getLastUpdate(), id));
        return result.toString();
    }

    /**
     * Check if a string has something in it
     * 
     * @param s the String
     * @return true if not empty
     */
    public boolean notEmpty(@Nullable String s) {
        return s != null && s.length() != 0;
    }

    /**
     * Get the timestamp of the most recent change.
     * 
     * @return The timestamp of the most recent change.
     */
    @Override
    public Date getLastUpdate() {
        return new Date(update);
    }

    /**
     * Return list of elements from OSMOSE style list
     * 
     * This returns fake elements with version -1 for objects not downloaded
     * 
     * @return list of OsmElement
     */
    public final List<OsmElement> getElements() {
        List<OsmElement> result = new ArrayList<>();
        StorageDelegator storageDelegator = App.getDelegator();
        try {
            if (nodes != null) {
                for (long l : nodes.values()) {
                    result.add(getElementOrDummy(storageDelegator, Node.NAME, l));
                }
            }
            if (ways != null) {
                for (long l : ways.values()) {
                    result.add(getElementOrDummy(storageDelegator, Way.NAME, l));
                }
            }
            if (relations != null) {
                for (long l : relations.values()) {
                    result.add(getElementOrDummy(storageDelegator, Relation.NAME, l));
                }
            }
        } catch (Exception ex) {
            Log.d(DEBUG_TAG, "couldn't retrieve elements " + ex);
        }
        return result;
    }

    /**
     * Get an element from storage and if not present a dummy object
     * 
     * @param storageDelegator the StorageDelegator instance
     * @param type OsmELement type
     * @param id the element id
     * @return an OsmElement
     */
    @NonNull
    private OsmElement getElementOrDummy(@NonNull StorageDelegator storageDelegator, @NonNull String type, long id) {
        OsmElement osm = storageDelegator.getOsmElement(type, id);
        if (osm == null) {
            switch (type) {
            case Node.NAME:
                return OsmElementFactory.createNode(id, -1, -1, (byte) -1, 0, 0);
            case Way.NAME:
                return OsmElementFactory.createWay(id, -1, -1, (byte) -1);
            case Relation.NAME:
                return OsmElementFactory.createRelation(id, -1, -1, (byte) -1);
            default:
                throw new IllegalArgumentException(type + " is not an OSM element type");
            }
        }
        return osm;
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

    @Override
    public int hashCode() { // NOSONAR
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }
}
