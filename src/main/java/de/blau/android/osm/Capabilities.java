package de.blau.android.osm;

import java.util.ArrayList;
import java.util.List;

/**
 * Capabilities of the API server we are connected to
 * 
 * Default values are as of July 2017
 * 
 * @author Simon Poole
 */
public class Capabilities {

    public enum Status {
        ONLINE, READONLY, OFFLINE
    }

    // API related
    private String minVersion             = "0.6";
    private String maxVersion             = "0.6";
    private float  areaMax                = 0.25f;
    private int    maxTracepointsPerPage  = 5000;
    private int    maxWayNodes            = 2000;
    private int    maxElementsInChangeset = 10000;
    private int    maxStringLength        = 255;           // this is not provided by the API yet
    private int    timeout                = 300;
    private Status dbStatus               = Status.OFFLINE;
    private Status apiStatus              = Status.OFFLINE;
    private Status gpxStatus              = Status.OFFLINE;
    // policy
    private List<String> imageryBlacklist = new ArrayList<>();

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

    private static List<String> defaultBlacklist() {
        List<String> blacklist = new ArrayList<>();
        blacklist.add(".*\\.google(apis)?\\..*/(vt|kh)[\\?/].*([xyz]=.*){3}.*");
        blacklist.add("http://xdworld\\.vworld\\.kr:8080/.*");
        blacklist.add(".*\\.here\\.com[/:].*");
        return blacklist;
    }

    public static Capabilities getDefault() {
        Capabilities d = new Capabilities();
        d.imageryBlacklist.addAll(defaultBlacklist());
        // this is wishful thinking
        // TODO add unknown status
        d.setDbStatus(Status.ONLINE);
        d.setApiStatus(Status.ONLINE);
        d.setGpxStatus(Status.ONLINE);
        return d;
    }

    public static Capabilities getReadOnlyDefault() {
        Capabilities d = new Capabilities();
        d.imageryBlacklist.addAll(defaultBlacklist());
        // this is wishful thinking
        // TODO add unknown status
        d.setDbStatus(Status.READONLY);
        d.setApiStatus(Status.READONLY);
        d.setGpxStatus(Status.READONLY);
        return d;
    }

    public List<String> getImageryBlacklist() {
        return imageryBlacklist;
    }

    public void setImageryBlacklist(List<String> imageryBlacklist) {
        this.imageryBlacklist = imageryBlacklist;
    }

    /**
     * @return the minVersion
     */
    public String getMinVersion() {
        return minVersion;
    }

    /**
     * @param minVersion the minVersion to set
     */
    public void setMinVersion(String minVersion) {
        this.minVersion = minVersion;
    }

    /**
     * @return the maxVersion
     */
    public String getMaxVersion() {
        return maxVersion;
    }

    /**
     * @param maxVersion the maxVersion to set
     */
    public void setMaxVersion(String maxVersion) {
        this.maxVersion = maxVersion;
    }

    /**
     * @return the areaMax
     */
    float getAreaMax() {
        return areaMax;
    }

    /**
     * @param areaMax the areaMax to set
     */
    void setAreaMax(float areaMax) {
        this.areaMax = areaMax;
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
     * @return the maxWayNodes
     */
    int getMaxWayNodes() {
        return maxWayNodes;
    }

    /**
     * @param maxWayNodes the maxWayNodes to set
     */
    void setMaxWayNodes(int maxWayNodes) {
        this.maxWayNodes = maxWayNodes;
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
     * @return the dbStatus
     */
    public Status getDbStatus() {
        return dbStatus;
    }

    /**
     * @param dbStatus the dbStatus to set
     */
    public void setDbStatus(Status dbStatus) {
        this.dbStatus = dbStatus;
    }

    /**
     * @return the apiStatus
     */
    public Status getApiStatus() {
        return apiStatus;
    }

    /**
     * @param apiStatus the apiStatus to set
     */
    public void setApiStatus(Status apiStatus) {
        this.apiStatus = apiStatus;
    }

    /**
     * @return the gpxStatus
     */
    public Status getGpxStatus() {
        return gpxStatus;
    }

    /**
     * @param gpxStatus the gpxStatus to set
     */
    public void setGpxStatus(Status gpxStatus) {
        this.gpxStatus = gpxStatus;
    }
}
