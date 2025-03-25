package io.vespucci.osm;

import static io.vespucci.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Log;
import androidx.annotation.NonNull;

/**
 * Capabilities of the API server we are connected to
 * 
 * Default values are as of February 2022
 * 
 * @author Simon Poole
 */
public class Capabilities {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, Capabilities.class.getSimpleName().length());
    private static final String DEBUG_TAG = Capabilities.class.getSimpleName().substring(0, TAG_LEN);

    public enum Status {
        // TODO add unknown status for when we haven't determined the status yet
        ONLINE, READONLY, OFFLINE
    }

    static final String VERSION_TAG          = "version";
    static final String MINIMUM_KEY          = "minimum";
    static final String MAXIMUM_KEY          = "maximum";
    static final String AREA_TAG             = "area";
    static final String NOTE_AREA_TAG        = "note_area";
    static final String TRACEPOINTS_TAG      = "tracepoints";
    static final String PER_PAGE_KEY         = "per_page";
    static final String WAYNODES_TAG         = "waynodes";
    static final String RELATIONMEMBERS_TAG  = "relationmembers";
    static final String CHANGESETS_TAG       = "changesets";
    static final String MAXIMUM_ELEMENTS_KEY = "maximum_elements";
    static final String TIMEOUT_TAG          = "timeout";
    static final String SECONDS_KEY          = "seconds";
    static final String STATUS_TAG           = "status";
    static final String DATABASE_KEY         = "database";
    static final String API_KEY              = "api";
    static final String GPX_KEY              = "gpx";
    static final String BLACKLIST_TAG        = "blacklist";
    static final String REGEX_KEY            = "regex";

    private static final int    DEFAULT_TIMEOUT                   = 300;
    private static final int    DEFAULT_MAX_ELEMENTS_IN_CHANGESET = 10000;
    private static final int    DEFAULT_MAX_RELATION_MEMBERS      = 32000;
    public static final int     DEFAULT_MAX_WAY_NODES             = 2000;
    private static final int    DEFAULT_MAX_TRACEPOINTS_PER_PAGE  = 5000;
    private static final float  DEFAULT_MAX_NOTE_AREA             = 25f;
    private static final float  DEFAULT_MAX_AREA                  = 0.25f;
    private static final String DEFAULT_MIN_VERSION               = "0.6";
    private static final String DEFAULT_MAX_VERSION               = "0.6";
    public static final int     DEFAULT_MAX_STRING_LENGTH         = 255;

    // API related
    private String minVersion             = DEFAULT_MIN_VERSION;
    private String maxVersion             = DEFAULT_MAX_VERSION;
    private float  maxArea                = DEFAULT_MAX_AREA;
    private float  maxNoteArea            = DEFAULT_MAX_NOTE_AREA;
    private int    maxTracepointsPerPage  = DEFAULT_MAX_TRACEPOINTS_PER_PAGE;
    private int    maxWayNodes            = DEFAULT_MAX_WAY_NODES;
    private int    maxRelationMembers     = DEFAULT_MAX_RELATION_MEMBERS;
    private int    maxElementsInChangeset = DEFAULT_MAX_ELEMENTS_IN_CHANGESET;
    private int    maxStringLength        = DEFAULT_MAX_STRING_LENGTH;        // this is not provided by the API yet
    private int    timeout                = DEFAULT_TIMEOUT;
    private Status dbStatus               = Status.OFFLINE;
    private Status apiStatus              = Status.OFFLINE;
    private Status gpxStatus              = Status.OFFLINE;
    // policy
    private List<String> imageryBlacklist = new ArrayList<>();

    /**
     * Convert status as a string to Status values
     * 
     * @param s status string
     * @return the Status
     */
    @NonNull
    public static Status stringToStatus(String s) {
        if (s == null) {
            return Status.OFFLINE;
        }
        switch (s) {
        case "online":
            return Status.ONLINE;
        case "readonly":
            return Status.READONLY;
        default:
            return Status.OFFLINE;
        }
    }

    /**
     * Create a default imagery blacklist
     * 
     * @return a List of regexp Strings
     */
    @NonNull
    private static List<String> defaultBlacklist() {
        List<String> blacklist = new ArrayList<>();
        blacklist.add(".*\\.google(apis)?\\..*/(vt|kh)[\\?/].*([xyz]=.*){3}.*");
        blacklist.add("http://xdworld\\.vworld\\.kr:8080/.*");
        blacklist.add(".*\\.here\\.com[/:].*");
        return blacklist;
    }

    /**
     * Get default capabilities for a read/write server
     * 
     * @return a Capabilities object
     */
    @NonNull
    public static Capabilities getDefault() {
        Capabilities d = new Capabilities();
        d.imageryBlacklist.addAll(defaultBlacklist());
        d.setStatus(Status.ONLINE);
        return d;
    }

    /**
     * Get default capabilities for a read-only server
     * 
     * @return a Capabilities object
     */
    @NonNull
    public static Capabilities getReadOnlyDefault() {
        Capabilities d = new Capabilities();
        d.imageryBlacklist.addAll(defaultBlacklist());
        d.setStatus(Status.READONLY);
        return d;
    }

    /**
     * Get the imagery blacklist
     * 
     * @return a List of regexp Strings
     */
    @NonNull
    public List<String> getImageryBlacklist() {
        return imageryBlacklist;
    }

    /**
     * Set the imagery blacklist
     * 
     * @param imageryBlacklist a List of regexp Strings
     */
    public void setImageryBlacklist(@NonNull List<String> imageryBlacklist) {
        this.imageryBlacklist = imageryBlacklist;
    }

    /**
     * @return the minVersion
     */
    @NonNull
    public String getMinVersion() {
        return minVersion;
    }

    /**
     * @param minVersion the minVersion to set
     */
    public void setMinVersion(@NonNull String minVersion) {
        this.minVersion = minVersion;
    }

    /**
     * @return the maxVersion
     */
    @NonNull
    public String getMaxVersion() {
        return maxVersion;
    }

    /**
     * @param maxVersion the maxVersion to set
     */
    public void setMaxVersion(@NonNull String maxVersion) {
        this.maxVersion = maxVersion;
    }

    /**
     * Get the maximum size of a download area for data in square degrees
     * 
     * @return the maximum size of a download area for data in square degrees
     */
    public float getMaxArea() {
        return maxArea;
    }

    /**
     * Set the maximum size of a download area for data in square degrees
     * 
     * @param areaMax the maximum size of a download area for data in square degrees
     */
    void setMaxArea(float areaMax) {
        this.maxArea = areaMax;
    }

    /**
     * Get the maximum size of a download area for notes in square degrees
     * 
     * @return the maximum size of a download area for notes in square degrees
     */
    public float getMaxNoteArea() {
        return maxNoteArea;
    }

    /**
     * Set the maximum size of a download area for notes in square degrees
     * 
     * @param noteAreaMax the maximum size of a download area for notes in square degrees
     */
    void setMaxNoteArea(float noteAreaMax) {
        this.maxNoteArea = noteAreaMax;
    }

    /**
     * @return the maxTracepointsPerPage
     */
    int getMaxTracepointsPerPage() {
        return maxTracepointsPerPage;
    }

    /**
     * @param maxTracepointsPerPage the maxTracepointsPerPage to set
     */
    void setMaxTracepointsPerPage(int maxTracepointsPerPage) {
        this.maxTracepointsPerPage = maxTracepointsPerPage;
    }

    /**
     * Get the maximum number of Nodes in an OSM Way
     * 
     * @return the maximum number of Nodes in an OSM Way
     */
    public int getMaxWayNodes() {
        return maxWayNodes;
    }

    /**
     * Set the maximum number of Nodes in an OSM Way
     * 
     * @param maxWayNodes the maximum number of Nodes in an OSM Way
     */
    void setMaxWayNodes(int maxWayNodes) {
        this.maxWayNodes = maxWayNodes;
    }

    /**
     * Get the maximum number of members an OSM Relation can have
     * 
     * @return the maximum number of members an OSM Relation can have
     */
    public int getMaxRelationMembers() {
        return maxRelationMembers;
    }

    /**
     * Set the maximum number of members an OSM Relation can have
     * 
     * @param maxRelationMembers the maximum number of members an OSM Relation can have
     */
    void setMaxRelationMembers(int maxRelationMembers) {
        this.maxRelationMembers = maxRelationMembers;
    }

    /**
     * @return the maxElementsInChangeset
     */
    int getMaxElementsInChangeset() {
        return maxElementsInChangeset;
    }

    /**
     * @param maxElementsInChangeset the maxElementsInChangeset to set
     */
    void setMaxElementsInChangeset(int maxElementsInChangeset) {
        this.maxElementsInChangeset = maxElementsInChangeset;
    }

    /**
     * @return the maxStringLength
     */
    public int getMaxStringLength() {
        return maxStringLength;
    }

    /**
     * @param maxStringLength the maxStringLength to set
     */
    public void setMaxStringLength(int maxStringLength) {
        this.maxStringLength = maxStringLength;
    }

    /**
     * @return the timeout
     */
    int getTimeout() {
        return timeout;
    }

    /**
     * @param timeout the timeout to set
     */
    void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * Get the DB status
     * 
     * @return the dbStatus
     */
    @NonNull
    public Status getDbStatus() {
        return dbStatus;
    }

    /**
     * @param dbStatus the dbStatus to set
     */
    public void setDbStatus(@NonNull Status dbStatus) {
        this.dbStatus = dbStatus;
    }

    /**
     * Get the API status
     * 
     * @return the apiStatus
     */
    @NonNull
    public Status getApiStatus() {
        return apiStatus;
    }

    /**
     * @param apiStatus the apiStatus to set
     */
    public void setApiStatus(@NonNull Status apiStatus) {
        this.apiStatus = apiStatus;
    }

    /**
     * Get the GPX API status
     * 
     * @return the gpxStatus
     */
    @NonNull
    public Status getGpxStatus() {
        return gpxStatus;
    }

    /**
     * @param gpxStatus the gpxStatus to set
     */
    public void setGpxStatus(@NonNull Status gpxStatus) {
        this.gpxStatus = gpxStatus;
    }

    /**
     * Set all server status values to the same
     * 
     * @param status the status to set
     */
    private void setStatus(@NonNull Status status) {
        this.dbStatus = status;
        this.apiStatus = status;
        this.gpxStatus = status;
    }

    /**
     * Create a new Capabilities object from an InputStream in XML format
     * 
     * @param parser an XmlPullParser instance
     * @param is the InputStream
     * @return a Capabilities object
     * @throws XmlPullParserException if parsing fails
     * @throws IOException if an IO operation fails
     */
    @NonNull
    static Capabilities parse(@NonNull XmlPullParser parser, @NonNull InputStream is) throws XmlPullParserException, IOException {
        parser.setInput(is, null);
        int eventType;
        Capabilities result = new Capabilities();
        // very hackish just keys on tag names and not in which section of the response we are
        while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
            try {
                String tagName = parser.getName();
                if (eventType == XmlPullParser.START_TAG) {
                    switch (tagName) {
                    case Capabilities.VERSION_TAG:
                        result.setMinVersion(parser.getAttributeValue(null, Capabilities.MINIMUM_KEY));
                        result.setMaxVersion(parser.getAttributeValue(null, Capabilities.MAXIMUM_KEY));
                        Log.d(DEBUG_TAG, "getCapabilities min/max API version " + result.getMinVersion() + "/" + result.getMaxVersion());
                        break;
                    case Capabilities.AREA_TAG:
                        String maxArea = parser.getAttributeValue(null, Capabilities.MAXIMUM_KEY);
                        if (maxArea != null) {
                            result.setMaxArea(Float.parseFloat(maxArea));
                        }
                        Log.d(DEBUG_TAG, "getCapabilities maximum area " + maxArea);
                        break;
                    case Capabilities.NOTE_AREA_TAG:
                        String noteMaxArea = parser.getAttributeValue(null, Capabilities.MAXIMUM_KEY);
                        if (noteMaxArea != null) {
                            result.setMaxNoteArea(Float.parseFloat(noteMaxArea));
                        }
                        Log.d(DEBUG_TAG, "getCapabilities note maximum area " + noteMaxArea);
                        break;
                    case Capabilities.TRACEPOINTS_TAG:
                        String perPage = parser.getAttributeValue(null, Capabilities.PER_PAGE_KEY);
                        if (perPage != null) {
                            result.setMaxTracepointsPerPage(Integer.parseInt(perPage));
                        }
                        Log.d(DEBUG_TAG, "getCapabilities maximum #tracepoints per page " + perPage);
                        break;
                    case Capabilities.WAYNODES_TAG:
                        String maximumWayNodes = parser.getAttributeValue(null, Capabilities.MAXIMUM_KEY);
                        if (maximumWayNodes != null) {
                            result.setMaxWayNodes(Integer.parseInt(maximumWayNodes));
                        }
                        Log.d(DEBUG_TAG, "getCapabilities maximum #nodes in a way " + maximumWayNodes);
                        break;
                    case Capabilities.RELATIONMEMBERS_TAG:
                        String maximumRelationMembers = parser.getAttributeValue(null, Capabilities.MAXIMUM_KEY);
                        if (maximumRelationMembers != null) {
                            result.setMaxRelationMembers(Integer.parseInt(maximumRelationMembers));
                        }
                        Log.d(DEBUG_TAG, "getCapabilities maximum #members in a relation " + maximumRelationMembers);
                        break;
                    case Capabilities.CHANGESETS_TAG:
                        String maximumElements = parser.getAttributeValue(null, Capabilities.MAXIMUM_ELEMENTS_KEY);
                        if (maximumElements != null) {
                            result.setMaxElementsInChangeset(Integer.parseInt(maximumElements));
                        }
                        Log.d(DEBUG_TAG, "getCapabilities maximum elements in changesets " + maximumElements);
                        break;
                    case Capabilities.TIMEOUT_TAG:
                        String seconds = parser.getAttributeValue(null, Capabilities.SECONDS_KEY);
                        if (seconds != null) {
                            result.setTimeout(Integer.parseInt(seconds));
                        }
                        Log.d(DEBUG_TAG, "getCapabilities timeout seconds " + seconds);
                        break;
                    case Capabilities.STATUS_TAG:
                        result.setDbStatus(Capabilities.stringToStatus(parser.getAttributeValue(null, Capabilities.DATABASE_KEY)));
                        result.setApiStatus(Capabilities.stringToStatus(parser.getAttributeValue(null, Capabilities.API_KEY)));
                        result.setGpxStatus(Capabilities.stringToStatus(parser.getAttributeValue(null, Capabilities.GPX_KEY)));
                        Log.d(DEBUG_TAG, "getCapabilities service status DB " + result.getDbStatus() + " API " + result.getApiStatus() + " GPX "
                                + result.getGpxStatus());
                        break;
                    case Capabilities.BLACKLIST_TAG:
                        if (result.getImageryBlacklist() == null) {
                            result.setImageryBlacklist(new ArrayList<>());
                        }
                        String regex = parser.getAttributeValue(null, Capabilities.REGEX_KEY);
                        if (regex != null) {
                            result.getImageryBlacklist().add(regex);
                        }
                        Log.d(DEBUG_TAG, "getCapabilities blacklist regex " + regex);
                        break;
                    default:
                        Log.w(DEBUG_TAG, "getCapabilities unknown tag " + tagName);
                    }
                }
            } catch (NumberFormatException e) {
                Log.e(DEBUG_TAG, "Problem accessing capabilities", e);
            }
        }
        return result;
    }
}
