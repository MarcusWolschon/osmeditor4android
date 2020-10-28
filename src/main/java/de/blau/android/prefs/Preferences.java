package de.blau.android.prefs;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import de.blau.android.R;
import de.blau.android.contract.Urls;
import de.blau.android.osm.Server;
import de.blau.android.presets.Preset;
import de.blau.android.resources.DataStyle;
import de.blau.android.resources.TileLayerSource.Category;
import de.blau.android.util.BrokenAndroid;
import de.blau.android.util.Sound;

/**
 * Convenience class for parsing and holding the application's SharedPreferences.
 * 
 * @author mb
 */
public class Preferences {
    private static final String DEBUG_TAG = "Preferences";

    private final AdvancedPrefDatabase advancedPrefs;

    private final boolean isStatsVisible;
    private final boolean isToleranceVisible;
    private final boolean isAntiAliasingEnabled;
    private final boolean isKeepScreenOnEnabled;
    private final boolean useBackForUndo;
    private final boolean largeDragArea;
    private final boolean tagFormEnabled;
    private String        scaleLayer;
    private final String  mapProfile;
    private final String  followGPSbutton;
    private String        fullscreenMode;
    private final String  mapOrientation;
    private int           gpsInterval;
    private float         gpsDistance;
    private float         maxStrokeWidth;
    private int           tileCacheSize;                 // in MB
    private int           mapillaryCacheSize;            // in MB
    private int           downloadRadius;                // in m
    private float         maxDownloadSpeed;              // in km/h
    private final int     autoPruneNodeLimit;
    private final int     panAndZoomLimit;
    private int           bugDownloadRadius;
    private float         maxBugDownloadSpeed;           // in km/h
    private Set<String>   taskFilter;                    // can't be final
    private final boolean forceContextMenu;
    private final boolean enableNameSuggestions;
    private final boolean nameSuggestionPresetsEnabled;
    private final boolean autoApplyPreset;
    private final boolean closeChangesetOnSave;
    private final boolean splitActionBarEnabled;
    private String        gpsSource;
    private final String  gpsTcpSource;
    private String        offsetServer;
    private final String  osmoseServer;
    private final String  mapRouletteServer;
    private String        taginfoServer;
    private String        mapillaryApi;
    private String        mapillaryImages;
    private final boolean showCameraAction;
    private final boolean useInternalPhotoViewer;
    private final boolean generateAlerts;
    private final boolean groupAlertsOnly;
    private int           maxAlertDistance;
    private final boolean lightThemeEnabled;
    private Set<String>   addressTags;                   // can't be final
    private final boolean voiceCommandsEnabled;
    private final boolean leaveGpsDisabled;
    private final boolean allowFallbackToNetworkLocation;
    private final boolean showIcons;
    private final boolean showWayIcons;
    private int           maxInlineValues;
    private int           maxTileDownloadThreads;
    private int           notificationCacheSize;
    private int           autoLockDelay;
    private final boolean alwaysDrawBoundingBoxes;
    private final boolean jsConsoleEnabled;
    private final boolean hwAccelerationEnabled;
    private final int     connectedNodeTolerance;
    private final int     orthogonalizeThreshold;
    private final boolean autoformatPhoneNumbers;
    private final int     gnssTimeToStale;
    private final int     uploadOkLimit;
    private final int     uploadWarnLimit;
    private final int     dataWarnLimit;
    private final boolean useBarometricHeight;
    private final boolean useUrlForFeedback;
    private final int beepVolume;

    private static final String DEFAULT_MAP_PROFILE = "Color Round Nodes";

    private final SharedPreferences prefs;

    private final Resources r;

    /**
     * Construct a new instance
     * 
     * @param ctx Android context
     * @throws IllegalArgumentException
     * @throws NotFoundException
     */
    @SuppressLint("NewApi")
    public Preferences(@NonNull Context ctx) throws IllegalArgumentException, NotFoundException {
        prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        r = ctx.getResources();
        advancedPrefs = new AdvancedPrefDatabase(ctx);

        // we're not using acra.disable - ensure it isn't present
        if (prefs.contains("acra.disable")) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("acra.disable");
            editor.commit();
        }
        // we *are* using acra.enable
        if (!prefs.contains("acra.enable")) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("acra.enable", true);
            editor.commit();
        }

        maxStrokeWidth = getIntPref(R.string.config_maxStrokeWidth_key, 16);

        tileCacheSize = getIntPref(R.string.config_tileCacheSize_key, 100);
        mapillaryCacheSize = getIntPref(R.string.config_mapillaryCacheSize_key, 100);

        downloadRadius = getIntPref(R.string.config_extTriggeredDownloadRadius_key, 50);
        maxDownloadSpeed = getIntPref(R.string.config_maxDownloadSpeed_key, 10);
        autoPruneNodeLimit = getIntPref(R.string.config_autoPruneNodeLimit_key, 5000);
        panAndZoomLimit = getIntPref(R.string.config_panAndZoomLimit_key, 17);

        bugDownloadRadius = getIntPref(R.string.config_bugDownloadRadius_key, 200);
        maxBugDownloadSpeed = getIntPref(R.string.config_maxBugDownloadSpeed_key, 30);

        taskFilter = new HashSet<>(Arrays.asList(r.getStringArray(R.array.bug_filter_defaults)));
        taskFilter = prefs.getStringSet(r.getString(R.string.config_bugFilter_key), taskFilter);

        isStatsVisible = prefs.getBoolean(r.getString(R.string.config_showStats_key), false);
        isToleranceVisible = prefs.getBoolean(r.getString(R.string.config_showTolerance_key), true);
        isAntiAliasingEnabled = prefs.getBoolean(r.getString(R.string.config_enableAntiAliasing_key), true);
        tagFormEnabled = prefs.getBoolean(r.getString(R.string.config_tagFormEnabled_key), true);
        isKeepScreenOnEnabled = prefs.getBoolean(r.getString(R.string.config_enableKeepScreenOn_key), false);
        useBackForUndo = prefs.getBoolean(r.getString(R.string.config_use_back_for_undo_key), false);
        largeDragArea = prefs.getBoolean(r.getString(R.string.config_largeDragArea_key), false);
        enableNameSuggestions = prefs.getBoolean(r.getString(R.string.config_enableNameSuggestions_key), true);
        nameSuggestionPresetsEnabled = prefs.getBoolean(r.getString(R.string.config_enableNameSuggestionsPresets_key), true);
        autoApplyPreset = prefs.getBoolean(r.getString(R.string.config_autoApplyPreset_key), true);
        closeChangesetOnSave = prefs.getBoolean(r.getString(R.string.config_closeChangesetOnSave_key), true);
        splitActionBarEnabled = prefs.getBoolean(r.getString(R.string.config_splitActionBarEnabled_key), true);
        scaleLayer = prefs.getString(r.getString(R.string.config_scale_key), "SCALE_METRIC");
        String tempMapProfile = prefs.getString(r.getString(R.string.config_mapProfile_key), null);
        // check if we actually still have the profile
        if (DataStyle.getStyle(tempMapProfile) == null) {
            if (DataStyle.getStyle(DEFAULT_MAP_PROFILE) == null) {
                Log.w(DEBUG_TAG, "Using builtin default profile instead of " + tempMapProfile + " and " + DEFAULT_MAP_PROFILE);
                mapProfile = DataStyle.getBuiltinStyleName(); // built-in fall back
            } else {
                Log.w(DEBUG_TAG, "Using default profile");
                mapProfile = DEFAULT_MAP_PROFILE;
            }
        } else {
            mapProfile = tempMapProfile;
        }
        gpsSource = prefs.getString(r.getString(R.string.config_gps_source_key), "internal");
        gpsTcpSource = prefs.getString(r.getString(R.string.config_gps_source_tcp_key), "127.0.0.1:1958");
        gpsDistance = getIntPref(R.string.config_gps_distance_key, 2);
        gpsInterval = getIntPref(R.string.config_gps_interval_key, 1000);

        forceContextMenu = prefs.getBoolean(r.getString(R.string.config_forceContextMenu_key), false);

        offsetServer = prefs.getString(r.getString(R.string.config_offsetServer_key), Urls.DEFAULT_OFFSET_SERVER);
        osmoseServer = prefs.getString(r.getString(R.string.config_osmoseServer_key), Urls.DEFAULT_OSMOSE_SERVER);
        mapRouletteServer = prefs.getString(r.getString(R.string.config_maprouletteServer_key), Urls.DEFAULT_MAPROULETTE_SERVER);
        taginfoServer = prefs.getString(r.getString(R.string.config_taginfoServer_key), Urls.DEFAULT_TAGINFO_SERVER);
        mapillaryApi = prefs.getString(r.getString(R.string.config_mapillaryApi_key), Urls.DEFAULT_MAPILLARY_API_V3);
        mapillaryImages = prefs.getString(r.getString(R.string.config_mapillaryImages_key), Urls.DEFAULT_MAPILLARY_IMAGES);

        showCameraAction = prefs.getBoolean(r.getString(R.string.config_showCameraAction_key), true);
        useInternalPhotoViewer = prefs.getBoolean(r.getString(R.string.config_useInternalPhotoViewer_key), true);

        generateAlerts = prefs.getBoolean(r.getString(R.string.config_generateAlerts_key), false);
        maxAlertDistance = getIntPref(R.string.config_maxAlertDistance_key, 100);
        groupAlertsOnly = prefs.getBoolean(r.getString(R.string.config_groupAlertsOnly_key), false);

        // light theme now always default
        lightThemeEnabled = prefs.getBoolean(r.getString(R.string.config_enableLightTheme_key), true);

        addressTags = new HashSet<>(Arrays.asList(r.getStringArray(R.array.address_tags_defaults)));
        addressTags = prefs.getStringSet(r.getString(R.string.config_addressTags_key), addressTags);

        voiceCommandsEnabled = prefs.getBoolean(r.getString(R.string.config_voiceCommandsEnabled_key), false);

        leaveGpsDisabled = prefs.getBoolean(r.getString(R.string.config_leaveGpsDisabled_key), false);
        allowFallbackToNetworkLocation = prefs.getBoolean(r.getString(R.string.config_gps_network_key), true);

        showIcons = prefs.getBoolean(r.getString(R.string.config_showIcons_key), true);

        showWayIcons = prefs.getBoolean(r.getString(R.string.config_showWayIcons_key), true);

        followGPSbutton = prefs.getString(r.getString(R.string.config_followGPSbutton_key), r.getString(R.string.follow_GPS_left));

        // this is slightly complex, but only needs to be done once
        String fullscreenModeKey = r.getString(R.string.config_fullscreenMode_key);
        fullscreenMode = prefs.getString(fullscreenModeKey, null);
        if (fullscreenMode == null) { // was never set, determine value
            BrokenAndroid brokenAndroid = new BrokenAndroid(ctx);
            fullscreenMode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && !brokenAndroid.isFullScreenBroken() ? r.getString(R.string.full_screen_auto)
                    : r.getString(R.string.full_screen_never);
            prefs.edit().putString(fullscreenModeKey, fullscreenMode).commit();
        }

        mapOrientation = prefs.getString(r.getString(R.string.config_mapOrientation_key), r.getString(R.string.map_orientation_auto));

        maxInlineValues = getIntPref(R.string.config_maxInlineValues_key, 4);

        autoLockDelay = getIntPref(R.string.config_autoLockDelay_key, 60);

        notificationCacheSize = getIntPref(R.string.config_notificationCacheSize_key, 5);

        maxTileDownloadThreads = getIntPref(R.string.config_maxTileDownloadThreads_key, 4);

        alwaysDrawBoundingBoxes = prefs.getBoolean(r.getString(R.string.config_alwaysDrawBoundingBoxes_key), false);

        jsConsoleEnabled = prefs.getBoolean(r.getString(R.string.config_js_console_key), false);

        hwAccelerationEnabled = prefs.getBoolean(r.getString(R.string.config_enableHwAcceleration_key), false);

        connectedNodeTolerance = getIntPref(R.string.config_connectedNodeTolerance_key, 2);

        orthogonalizeThreshold = getIntPref(R.string.config_orthogonalizeThreshold_key, 15);

        autoformatPhoneNumbers = prefs.getBoolean(r.getString(R.string.config_autoformatPhoneNumbers_key), true);

        gnssTimeToStale = getIntPref(R.string.config_gnssTimeToStale_key, 60);

        uploadOkLimit = getIntPref(R.string.config_uploadOk_key, 50);
        uploadWarnLimit = getIntPref(R.string.config_uploadWarn_key, 200);

        dataWarnLimit = getIntPref(R.string.config_dataWarn_key, 50000);

        useBarometricHeight = prefs.getBoolean(r.getString(R.string.config_useBarometricHeight_key), false);

        useUrlForFeedback = prefs.getBoolean(r.getString(R.string.config_useUrlForFeedback_key), false);
        
        beepVolume = getIntPref(R.string.config_beepVolume_key, Sound.BEEP_DEFAULT_VOLUME);
    }

    /**
     * Get an integer valued preference
     * 
     * @param keyResId the res id
     * @param def default value
     * @return the stored preference or the default if none found
     */
    int getIntPref(int keyResId, int def) {
        String key = r.getString(keyResId);
        try {
            return prefs.getInt(r.getString(keyResId), def);
        } catch (ClassCastException e) {
            Log.w(DEBUG_TAG, "error retrieving pref for " + key);
            return def;
        }
    }

    /**
     * @return the maximum width of a stroke
     */
    public float getMaxStrokeWidth() {
        return maxStrokeWidth;
    }

    /**
     * @return the size of the tile cache in MB
     */
    public int getTileCacheSize() {
        return tileCacheSize;
    }

    /**
     * @return the size of the tile cache in MB
     */
    public int getMapillaryCacheSize() {
        return mapillaryCacheSize;
    }

    /**
     * Check if some debugging stats should be shown on screen
     * 
     * @return true if turned on
     */
    public boolean isStatsVisible() {
        return isStatsVisible;
    }

    /**
     * Check if touch tolerance areas should be shown on screen
     * 
     * @return true if the areas should be shown
     */
    public boolean isToleranceVisible() {
        return isToleranceVisible;
    }

    /**
     * Check if anti-aliasing should be used
     * 
     * @return true if anti-aliasing should be used
     */
    public boolean isAntiAliasingEnabled() {
        return isAntiAliasingEnabled;
    }

    /**
     * Check if we should autoformat phone numbers
     * 
     * @return true if we should
     */
    public boolean autoformatPhoneNumbers() {
        return autoformatPhoneNumbers;
    }

    /**
     * Check if the form based tag editor should be shown in the property editor
     * 
     * @return true if the form based editor should be used
     */
    public boolean tagFormEnabled() {
        return tagFormEnabled;
    }

    /**
     * Check if we want to keep the screen on
     * 
     * @return true if the screen should be kept on
     */
    public boolean isKeepScreenOnEnabled() {
        return isKeepScreenOnEnabled;
    }

    /**
     * Check if the back key should be used for undo
     * 
     * @return true if the back key should be used for undo
     */
    public boolean useBackForUndo() {
        return useBackForUndo;
    }

    /**
     * Check if we should show a large drag area around nodes
     * 
     * @return true if we should show a large drag area
     */
    public boolean largeDragArea() {
        return largeDragArea;
    }

    /**
     * Get kind of scale that should be displayed
     * 
     * @return mode value from scale_values
     */
    public String scaleLayer() {
        return scaleLayer;
    }

    /**
     * Set the kind of scale that should be displayed
     * 
     * @param mode value from scale_values
     */
    public void setScaleLayer(String mode) {
        scaleLayer = mode;
        prefs.edit().putString(r.getString(R.string.config_scale_key), mode).commit();
    }

    /**
     * Get the current data rendering style
     * 
     * @return the name of the current data renderign style
     */
    @NonNull
    public String getMapProfile() {
        return mapProfile;
    }

    /**
     * Get the currently used API server
     * 
     * @return the current Server object
     */
    @NonNull
    public Server getServer() {
        return advancedPrefs.getServerObject();
    }

    /**
     * Get the name of the current API configuration
     * 
     * @return the name or null if not found
     */
    @Nullable
    public String getApiName() {
        API api = advancedPrefs.getCurrentAPI();
        return api != null ? api.name : null;
    }

    /**
     * Get the currently active Presets
     * 
     * @return an array holding the Presets
     */
    @NonNull
    public Preset[] getPreset() {
        return advancedPrefs.getCurrentPresetObject();
    }

    /**
     * Check if we should show icons on nodes
     * 
     * @return true if icons should be shown
     */
    public boolean getShowIcons() {
        return showIcons;
    }

    /**
     * Check if we should show icons on closed ways
     * 
     * @return true if icons should be shown
     */
    public boolean getShowWayIcons() {
        return showWayIcons;
    }

    /**
     * Get the current GPS/GNSS source
     * 
     * @return a String with the source name
     */
    public String getGpsSource() {
        return gpsSource;
    }

    /**
     * Set the GPS/GNSS source to use
     * 
     * @param sourceRes the string resource id of the source
     */
    public void setGpsSource(int sourceRes) {
        gpsSource = r.getString(sourceRes);
        prefs.edit().putString(r.getString(R.string.config_gps_source_key), gpsSource).commit();
    }

    /**
     * Get the current GPS/GNSS tcp source
     * 
     * @return a String with the source name
     */
    public String getGpsTcpSource() {
        return gpsTcpSource;
    }

    /**
     * Get the configured minimum time between GPS/GNSS location fixes
     * 
     * @return interval between GPS/GNSS location fixes in miliseconds
     */
    public int getGpsInterval() {
        return gpsInterval;
    }

    /**
     * Get the configured minimum distance between GPS/GNSS location fixes
     * 
     * @return distance between GPS/GNSS location fixes in meters
     */
    public float getGpsDistance() {
        return gpsDistance;
    }

    /**
     * Set minimum distance between GPS/GNSS location fixes
     * 
     * @param distance the distance between GPS/GNSS location fixes in meters
     */
    public void setGpsDistance(int distance) {
        prefs.edit().putInt(r.getString(R.string.config_gps_distance_key), distance).commit();
        gpsDistance = distance;
    }

    /**
     * Check if we are allowed to fall back to Network locations
     * 
     * @return true if the fallback is allowed
     */
    public boolean isNetworkLocationFallbackAllowed() {
        return allowFallbackToNetworkLocation;
    }

    /**
     * Always show the selection context menu if more than one element is in the click tolerance
     * 
     * @return true if we should always show the context menu
     */
    public boolean getForceContextMenu() {
        return forceContextMenu;
    }

    /**
     * Check if we should use the name suggestion index
     * 
     * @return true if we should use the index
     */
    public boolean getEnableNameSuggestions() {
        return enableNameSuggestions;
    }

    /**
     * Get the configured download radius for data
     * 
     * @return the radius in meters
     */
    public int getDownloadRadius() {
        return downloadRadius;
    }

    /**
     * Get the configured maximum speed up to which we still auto-download data
     * 
     * @return the maximum speed for autodownloads
     */
    public float getMaxDownloadSpeed() {
        return maxDownloadSpeed;
    }

    /**
     * Set maximum speed for autodownloads
     * 
     * @param maxDownloadSpeed max speed in km/h to set
     */
    public void setMaxDownloadSpeed(float maxDownloadSpeed) {
        this.maxDownloadSpeed = maxDownloadSpeed;
        prefs.edit().putInt(r.getString(R.string.config_maxDownloadSpeed_key), (int) maxDownloadSpeed).commit();
    }

    /**
     * Get the number of Nodes at which we start attempting a prune
     * 
     * @return the number of Nodes we consider the limit
     */
    public int getAutoPruneNodeLimit() {
        return autoPruneNodeLimit;
    }

    /**
     * Get the minimum zoom for pan and zoom auto-download
     * 
     * @return the current limit
     */
    public int getPanAndZoomLimit() {
        return panAndZoomLimit;
    }

    /**
     * Get the configured download radius for tasks
     * 
     * @return the radius in meters
     */
    public int getBugDownloadRadius() {
        return bugDownloadRadius;
    }

    /**
     * Get the configured maximum speed up to which we still auto-download tasks
     * 
     * @return the maximum speed for autodownloads
     */
    public float getMaxBugDownloadSpeed() {
        return maxBugDownloadSpeed;
    }

    /**
     * Get the currently enabled task types
     * 
     * @return a set containing the task types as strings
     */
    public Set<String> taskFilter() {
        return taskFilter;
    }

    /**
     * Set the task filter
     * 
     * @param tasks the tasks to use, if null the default setting will be set (all on)
     */
    public void setTaskFilter(@Nullable Set<String> tasks) {
        if (tasks == null) {
            tasks = new HashSet<>(Arrays.asList(r.getStringArray(R.array.bug_filter_defaults)));
        }
        taskFilter = tasks;
        prefs.edit().putStringSet(r.getString(R.string.config_bugFilter_key), tasks).commit();
    }

    /**
     * Is automatically applying presets for name suggestions turned on
     * 
     * @return true if automatically applying presets for name suggestions should be used
     */
    public boolean nameSuggestionPresetsEnabled() {
        //
        return nameSuggestionPresetsEnabled;
    }

    /**
     * Check how we should handle changesets
     * 
     * @return true if we should close changesets on save
     */
    public boolean closeChangesetOnSave() {
        return closeChangesetOnSave;
    }

    /**
     * Check if we should split the action bar
     * 
     * @return true if we should show the action bar at top and bottom of the screen
     */
    public boolean splitActionBarEnabled() {
        return splitActionBarEnabled;
    }

    /**
     * Get the configured offset database server
     * 
     * @return base url for the server
     */
    public String getOffsetServer() {
        return offsetServer;
    }

    /**
     * Set the configured offset server
     * 
     * @param url base url for the server
     */
    public void setOffsetServer(@NonNull String url) {
        this.offsetServer = url;
        prefs.edit().putString(r.getString(R.string.config_offsetServer_key), url).commit();
    }

    /**
     * Get the configured OSMOSE server
     * 
     * @return base url for the server
     */
    public String getOsmoseServer() {
        return osmoseServer;
    }

    /**
     * Get the configured MapRoulette server
     * 
     * @return base url for the server
     */
    public String getMapRouletteServer() {
        return mapRouletteServer;
    }

    /**
     * Get the configured taginfo server
     * 
     * @return base url for the server
     */
    public String getTaginfoServer() {
        return taginfoServer;
    }

    /**
     * Set the configured taginfo server
     * 
     * @param url base url for the server
     */
    public void setTaginfoServer(@NonNull String url) {
        this.taginfoServer = url;
        prefs.edit().putString(r.getString(R.string.config_taginfoServer_key), url).commit();
    }

    /**
     * Get the configured mapillary API server
     * 
     * @return base url (including version) for the server
     */
    public String getMapillaryApiUrl() {
        return mapillaryApi;
    }

    /**
     * Set the configured mapillary API server
     * 
     * @param url base url (with version) for the server
     */
    public void setMapillaryApiUrl(@NonNull String url) {
        this.mapillaryApi = url;
        prefs.edit().putString(r.getString(R.string.config_mapillaryApi_key), url).commit();
    }

    /**
     * Get the configured mapillary images server
     * 
     * @return base url for the server
     */
    public String getMapillaryImagesUrl() {
        return mapillaryImages;
    }

    /**
     * Set the configured mapillary images server
     * 
     * @param url base url for the server
     */
    public void setMapillaryImagesUrl(@NonNull String url) {
        this.mapillaryImages = url;
        prefs.edit().putString(r.getString(R.string.config_mapillaryImages_key), url).commit();
    }

    /**
     * Check if we should show a camera button on the main map screen
     * 
     * @return true if the camera button should be shown
     */
    public boolean showCameraAction() {
        return showCameraAction;
    }

    /**
     * Check if we should use the internal photo viewer
     * 
     * @return true if the internal photo viewer should be used
     */
    public boolean useInternalPhotoViewer() {
        return useInternalPhotoViewer;
    }

    /**
     * Check if we should generate alerts (Android Notifications)
     * 
     * @return true if we should generate alerts
     */
    public boolean generateAlerts() {
        return generateAlerts;
    }

    /**
     * Check if we should generate alerts only once per group or always
     * 
     * @return true if we should generate alerts once per group or always
     */
    public boolean groupAlertOnly() {
        return groupAlertsOnly;
    }

    /**
     * Check if the light theme is enabled
     * 
     * @return true if the light theme is enabled
     */
    public boolean lightThemeEnabled() {
        return lightThemeEnabled;
    }

    /**
     * Get the address tags we currently want to use
     * 
     * @return a Set containing the tags
     */
    public Set<String> addressTags() {
        return addressTags;
    }

    /**
     * Get the maximum distance in meters from the current location that we generate alerts for
     * 
     * @return the maximum distance in meters
     */
    public int getMaxAlertDistance() {
        return maxAlertDistance;
    }

    /**
     * Check if voice commands are enabled
     * 
     * @return true if voice commands are enabled
     */
    public boolean voiceCommandsEnabled() {
        return voiceCommandsEnabled;
    }

    /**
     * Check if we should attempt to turn GPS on is disabled
     * 
     * @return true if we shouldn't try
     */
    public boolean leaveGpsDisabled() {
        return leaveGpsDisabled;
    }

    /**
     * Get the position of the on screen follow location button
     * 
     * @return one of LEFT, RIGHT and NONE
     */
    public String followGPSbuttonPosition() {
        return followGPSbutton;
    }

    /**
     * Get the fullscreen mode
     * 
     * @return one of AUTO, NEVER, FORCE and NOSTATUSBAR
     */
    public String getFullscreenMode() {
        return fullscreenMode;
    }

    /**
     * Get the way the map screen should respond to orientation changes
     * 
     * @return one of AUTO, CURRENT, PORTRAIT and LANDSCAPE
     */
    public String getMapOrientation() {
        return mapOrientation;
    }

    /**
     * Get the max number of items that will be displayed inline in the tag form editor
     * 
     * @return the max number on inline values displayed
     */
    public int getMaxInlineValues() {
        return maxInlineValues;
    }

    /**
     * Get the configured number of tile download threads
     * 
     * @return the configured number, if smaller than 1 we return 1
     */
    public int getMaxTileDownloadThreads() {
        if (maxTileDownloadThreads < 1) {
            Log.e(DEBUG_TAG, "Download threads limit smaller than 1");
            return 1;
        }
        return Math.max(1, maxTileDownloadThreads);
    }

    /**
     * Get the configured number notifications that we want to keep in the cache
     * 
     * @return the configured number, if smaller than 1 we return 1
     */
    public int getNotificationCacheSize() {
        if (notificationCacheSize < 1) {
            Log.e(DEBUG_TAG, "Notification cache size smaller than 1");
            return 1;
        }
        return notificationCacheSize;
    }

    /**
     * Get the number of seconds we should wait before locking the display
     * 
     * @return delay in seconds till we auto-lock
     */
    public int getAutolockDelay() {
        return 1000 * autoLockDelay;
    }

    /**
     * Turn auto download with a fixed download size around the current position on
     * 
     * @param enabled if true auto-download will be enabled
     */
    public void setAutoDownload(boolean enabled) {
        prefs.edit().putBoolean(r.getString(R.string.config_autoDownload_key), enabled).commit();
    }

    /**
     * Check if auto download with a fixed download size around the current position is enabled
     * 
     * @return true if enabled
     */
    public boolean getAutoDownload() {
        String key = r.getString(R.string.config_autoDownload_key);
        if (!prefs.contains(key)) {
            // create the entry
            setAutoDownload(false);
        }
        return prefs.getBoolean(key, false);
    }

    /**
     * Turn auto download based on the current view box on
     * 
     * @param enabled if true auto-download will be enabled
     */
    public void setPanAndZoomAutoDownload(boolean enabled) {
        prefs.edit().putBoolean(r.getString(R.string.config_panAndZoomDownload_key), enabled).commit();
    }

    /**
     * Check if auto download based on the current view box on is enabled
     * 
     * @return true if enabled
     */
    public boolean getPanAndZoomAutoDownload() {
        String key = r.getString(R.string.config_panAndZoomDownload_key);
        if (!prefs.contains(key)) {
            // create the entry
            setPanAndZoomAutoDownload(false);
        }
        return prefs.getBoolean(key, false);
    }

    /**
     * Set the contrast value
     * 
     * @param cValue the contrast value
     */
    public void setContrastValue(float cValue) {
        prefs.edit().putFloat(r.getString(R.string.config_contrastValue_key), cValue).commit();
    }

    /**
     * Get the contrast value
     * 
     * @return the contrast value
     */
    public float getContrastValue() {
        String key = r.getString(R.string.config_contrastValue_key);
        if (!prefs.contains(key)) {
            // create the entry
            setContrastValue(0);
        }
        return prefs.getFloat(key, 0);
    }

    /**
     * Set if we should autodownload tasks
     * 
     * @param on if true we will autodownload
     */
    public void setBugAutoDownload(boolean on) {
        prefs.edit().putBoolean(r.getString(R.string.config_bugAutoDownload_key), on).commit();
    }

    /**
     * Check if we should autodownload tasks
     * 
     * @return true if we should autodownload
     */
    public boolean getBugAutoDownload() {
        String key = r.getString(R.string.config_bugAutoDownload_key);
        if (!prefs.contains(key)) {
            // create the entry
            setBugAutoDownload(false);
        }
        return prefs.getBoolean(key, false);
    }

    /**
     * Set if we should show a GPS position marker
     * 
     * @param on if true the marker will be shown
     */
    public void setShowGPS(boolean on) {
        prefs.edit().putBoolean(r.getString(R.string.config_showGPS_key), on).commit();
    }

    /**
     * Check if we should show a GPS marker
     * 
     * @return true if we show a marker
     */
    public boolean getShowGPS() {
        String key = r.getString(R.string.config_showGPS_key);
        if (!prefs.contains(key)) {
            // create the entry
            setShowGPS(true);
        }
        return prefs.getBoolean(key, true);
    }

    /**
     * Check if the BoundingBoxes for downloaded data should be shown when the map display is locked
     * 
     * @return true if the BoundingBoxes should always be shown
     */
    public boolean getAlwaysDrawBoundingBoxes() {
        return alwaysDrawBoundingBoxes;
    }

    /**
     * Enable/disable the tag filter
     * 
     * @param on if true the tag filter will be enabled
     */
    public void enableTagFilter(boolean on) {
        prefs.edit().putBoolean(r.getString(R.string.config_tagFilter_key), on).commit();
    }

    /**
     * Check if the tag filter is enabled
     * 
     * @return true if enabled
     */
    public boolean getEnableTagFilter() {
        String key = r.getString(R.string.config_tagFilter_key);
        if (!prefs.contains(key)) {
            // create the entry
            enableTagFilter(false);
        }
        return prefs.getBoolean(key, false);
    }

    /**
     * Enable/disable the preset filter
     * 
     * @param on if true the preset filter will be enabled
     */
    public void enablePresetFilter(boolean on) {
        prefs.edit().putBoolean(r.getString(R.string.config_presetFilter_key), on).commit();
    }

    /**
     * Check if the preset filter is enabled
     * 
     * @return true if enabled
     */
    public boolean getEnablePresetFilter() {
        String key = r.getString(R.string.config_presetFilter_key);
        if (!prefs.contains(key)) {
            // create the entry
            enablePresetFilter(false);
        }
        return prefs.getBoolean(key, false);
    }

    /**
     * Set the current default Geocoder
     * 
     * @param index index of the Geocoder
     */
    public void setGeocoder(int index) {
        prefs.edit().putInt(r.getString(R.string.config_geocoder_key), index).commit();
    }

    /**
     * Get the current default Geocoder
     * 
     * @return index of the Geocoder
     */
    public int getGeocoder() {
        String key = r.getString(R.string.config_geocoder_key);
        if (!prefs.contains(key)) {
            // create the entry
            setGeocoder(0);
        }
        return prefs.getInt(key, 0);
    }

    /**
     * Set if searches should be limited to the current ViewBox
     * 
     * @param on if true limit searches to the current ViewBox
     */
    public void setGeocoderLimit(boolean on) {
        prefs.edit().putBoolean(r.getString(R.string.config_geocoderLimit_key), on).commit();
    }

    /**
     * Check if we should limit searches to the current ViewBox
     * 
     * @return true if searches should be limited
     */
    public boolean getGeocoderLimit() {
        String key = r.getString(R.string.config_geocoderLimit_key);
        if (!prefs.contains(key)) {
            // create the entry
            setGeocoderLimit(false);
        }
        return prefs.getBoolean(key, false);
    }

    /**
     * Check if the JS console is enabled
     * 
     * @return true if enabled
     */
    public boolean isJsConsoleEnabled() {
        return jsConsoleEnabled;
    }

    /**
     * Get the current background category
     * 
     * @return the current Category or null for all
     */
    @Nullable
    public Category getBackgroundCategory() {
        String key = r.getString(R.string.config_background_category_key);
        if (!prefs.contains(key)) {
            // create the entry
            setBackgroundCategory(null);
        }
        String categoryString = prefs.getString(key, null);
        return categoryString != null ? Category.valueOf(categoryString) : null;
    }

    /**
     * Set the current background imagery category
     * 
     * @param category the current Category or null for all
     */
    public void setBackgroundCategory(@Nullable Category category) {
        prefs.edit().putString(r.getString(R.string.config_background_category_key), category != null ? category.name() : null).commit();
    }

    /**
     * Get the current overlay category
     * 
     * @return the current Category or null for all
     */
    @Nullable
    public Category getOverlayCategory() {
        String key = r.getString(R.string.config_overlay_category_key);
        if (!prefs.contains(key)) {
            // create the entry
            setOverlayCategory(null);
        }
        String categoryString = prefs.getString(key, null);
        return categoryString != null ? Category.valueOf(categoryString) : null;
    }

    /**
     * Set the current overlay imagery category
     * 
     * @param category the current Category or null for all
     */
    public void setOverlayCategory(@Nullable Category category) {
        prefs.edit().putString(r.getString(R.string.config_overlay_category_key), category != null ? category.name() : null).commit();
    }

    /**
     * Check if hardware acceleration is enabled
     * 
     * @return true if hardware acceleration is enabled
     */
    public boolean hwAccelerationEnabled() {
        return hwAccelerationEnabled;
    }

    /**
     * Enable/disable the simple actions
     * 
     * @param on value to set
     */
    public void enableSimpleActions(boolean on) {
        prefs.edit().putBoolean(r.getString(R.string.config_simpleActions_key), on).commit();
    }

    /**
     * Check if the simply actions are enabled
     * 
     * @return true if enabled
     */
    public boolean areSimpleActionsEnabled() {
        String key = r.getString(R.string.config_simpleActions_key);
        if (!prefs.contains(key)) {
            // create the entry
            enableSimpleActions(true);
        }
        return prefs.getBoolean(key, true);
    }

    /**
     * If (highway) end nodes are less than this difference away from a way they could connect to fail validation
     * 
     * @return the tolerance in meters
     */
    public int getConnectedNodeTolerance() {
        return connectedNodeTolerance;
    }

    /**
     * The current threshold if exceeded we do not square/straighten a way
     * 
     * @return the threshold in °
     */
    public int getOrthogonalizeThreshold() {
        return orthogonalizeThreshold;
    }

    /**
     * Set the file to use for the EGM
     * 
     * @param egm an Uri for the file
     */
    public void setEgmFile(@Nullable Uri egm) {
        prefs.edit().putString(r.getString(R.string.config_egmFile_key), egm == null ? null : egm.toString()).commit();
    }

    /**
     * Retrieve an Uri for the EGM file
     * 
     * @return the Uri, or null if it hasn't been set (that is not downloaded)
     */
    @Nullable
    public Uri getEgmFile() {
        String uriString = prefs.getString(r.getString(R.string.config_egmFile_key), null);
        if (uriString != null) {
            return Uri.parse(uriString);
        }
        return null;
    }

    /**
     * Allow / disallow all networks for downloads
     * 
     * @param on value to set
     */
    public void setAllowAllNetworks(boolean on) {
        prefs.edit().putBoolean(r.getString(R.string.config_allowAllNetworks_key), on).commit();
    }

    /**
     * Check if all networks are allowed for downloads
     * 
     * @return true if enabled
     */
    public boolean allowAllNetworks() {
        String key = r.getString(R.string.config_allowAllNetworks_key);
        if (!prefs.contains(key)) {
            // create the entry
            setAllowAllNetworks(false);
        }
        return prefs.getBoolean(key, false);
    }

    /**
     * Check is we should auto-apply the best preset when the PropertyEditor is started
     * 
     * @return true if we should auto-apply the best preset
     */
    public boolean autoApplyPreset() {
        return autoApplyPreset;
    }

    /**
     * Get the number of seconds that a fix needs to be old to be considered stale
     * 
     * @return the number of seconds that a fix needs to be old to be considered stale
     */
    public int getGnssTimeToStale() {
        return gnssTimeToStale;
    }

    /**
     * Get the limit below which we don't show a warning
     * 
     * @return the number of elements we still consider OK to have a pending upload
     */
    public int getUploadOkLimit() {
        return uploadOkLimit;
    }

    /**
     * Get the limit below which we don't show an error
     * 
     * @return the number of elements we still consider to not be an error to have a pending upload
     */
    public int getUploadWarnLimit() {
        return uploadWarnLimit;
    }

    /**
     * Get the limit at which we show a warning that too much data is loaded
     * 
     * @return the number of elements at which we start showing a warning
     */
    public int getDataWarnLimit() {
        return dataWarnLimit;
    }

    /**
     * Get if we should use barometric height instead of GPS derived
     * 
     * @return if we should use barometer derived height values
     */
    public boolean useBarometricHeight() {
        return useBarometricHeight;
    }

    /**
     * Get f we should use an Url instead of the builtin reporter activity
     * 
     * @return if we should use an Url instead of the builtin reporter activity
     */
    public boolean useUrlForFeedback() {
        return useUrlForFeedback;
    }

    /**
     * Get the volume for beeping
     * 
     * @return an int between 0 and 100
     */
    public int getBeepVolume() {
        return beepVolume;
    }
    
    /**
     * Get a string from shared preferences
     * 
     * @param prefKey preference key as a string resource
     * @return the strings or null if nothing was found
     */
    @Nullable
    public String getString(int prefKey) {
        try {
            String key = r.getString(prefKey);
            return prefs.getString(key, null);
        } catch (Exception ex) {
            Log.e(DEBUG_TAG, "getString " + ex.getMessage());
            return null;
        }
    }

    /**
     * Save a string to shared preferences
     * 
     * @param prefKey preference key as a string resource
     * @param s string value to save
     */
    public void putString(int prefKey, @Nullable String s) {
        try {
            String key = r.getString(prefKey);
            prefs.edit().putString(key, s).commit();
        } catch (Exception ex) {
            Log.e(DEBUG_TAG, "putString " + ex.getMessage());
        }
    }
}
