package de.blau.android.prefs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import de.blau.android.App;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.contract.Urls;
import de.blau.android.osm.Server;
import de.blau.android.presets.Preset;
import de.blau.android.presets.PresetElementPath;
import de.blau.android.presets.PresetItem;
import de.blau.android.resources.DataStyle;
import de.blau.android.resources.TileLayerSource.Category;
import de.blau.android.resources.symbols.TriangleDown;
import de.blau.android.util.BrokenAndroid;
import de.blau.android.util.Sound;

/**
 * Convenience class for parsing and holding the application's SharedPreferences.
 * 
 * @author mb
 * @author simon
 */
public class Preferences {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, Preferences.class.getSimpleName().length());
    private static final String DEBUG_TAG = Preferences.class.getSimpleName().substring(0, TAG_LEN);

    private static final String ACRA_ENABLE  = "acra.enable";
    private static final String ACRA_DISABLE = "acra.disable";

    private static final String USE_LAST_PREF     = "use_last_";
    private static final String USE_OPTIONAL_PREF = "use_optional_";

    private final AdvancedPrefDatabase advancedPrefs;

    private final boolean     isStatsVisible;
    private final boolean     isToleranceVisible;
    private final boolean     isAntiAliasingEnabled;
    private final boolean     isKeepScreenOnEnabled;
    private final boolean     useBackForUndo;
    private boolean           largeDragArea;
    private final boolean     tagFormEnabled;
    private String            scaleLayer;
    private String            mapProfile;
    private final String      followGPSbutton;
    private String            fullscreenMode;
    private final String      mapOrientation;
    private int               gpsInterval;
    private float             gpsDistance;
    private float             maxStrokeWidth;
    private int               tileCacheSize;                 // in MB
    private final boolean     preferRemovableStorage;
    private int               mapillaryCacheSize;            // in MB
    private int               downloadRadius;                // in m
    private float             maxDownloadSpeed;              // in km/h
    private final int         autoPruneBoundingBoxLimit;
    private final int         autoPruneNodeLimit;
    private final int         autoPruneTaskLimit;
    private final int         panAndZoomLimit;
    private int               bugDownloadRadius;
    private float             maxBugDownloadSpeed;           // in km/h
    private Set<String>       taskFilter;
    private final boolean     forceContextMenu;
    private final boolean     enableNameSuggestions;
    private final boolean     nameSuggestionPresetsEnabled;
    private final boolean     autoApplyPreset;
    private final boolean     closeChangesetOnSave;
    private final boolean     splitActionBarEnabled;
    private String            gpsSource;
    private final String      gpsTcpSource;
    private final String      osmWiki;
    private String            offsetServer;
    private final String      osmoseServer;
    private final String      mapRouletteServer;
    private String            taginfoServer;
    private String            overpassServer;
    private String            oamServer;
    private String            mapillarySequencesUrlV4;
    private String            mapillaryImagesUrlV4;
    private final int         mapillaryMinZoom;
    private final boolean     showCameraAction;
    private final String      cameraApp;
    private final boolean     useInternalPhotoViewer;
    private final boolean     scanMediaStore;
    private final boolean     generateAlerts;
    private final boolean     groupAlertsOnly;
    private int               maxAlertDistance;
    private final String      theme;
    private final boolean     overrideCountryAddressTags;
    private final Set<String> addressTags;
    private final int         neighbourDistance;
    private final boolean     voiceCommandsEnabled;
    private final boolean     leaveGpsDisabled;
    private final boolean     allowFallbackToNetworkLocation;
    private final boolean     showIcons;
    private final boolean     showWayIcons;
    private int               maxInlineValues;
    private int               maxTileDownloadThreads;
    private int               notificationCacheSize;
    private int               autoLockDelay;
    private final boolean     alwaysDrawBoundingBoxes;
    private final boolean     jsConsoleEnabled;
    private final boolean     hwAccelerationEnabled;
    private final int         connectedNodeTolerance;
    private final int         orthogonalizeThreshold;
    private final boolean     autoformatPhoneNumbers;
    private final int         gnssTimeToStale;
    private final int         uploadOkLimit;
    private final int         uploadWarnLimit;
    private final int         uploadCheckerInterval;
    private final int         dataWarnLimit;
    private boolean           useBarometricHeight;
    private final boolean     useUrlForFeedback;
    private final int         beepVolume;
    private final int         maxOffsetDistance;
    private final Set<String> enabledValidations;
    private final int         autoNameCap;
    private boolean           wayNodeDragging;
    private final boolean     splitWindowForPropertyEditor;
    private boolean           newTaskForPropertyEditor;
    private final boolean     useImperialUnits;
    private final boolean     supportPresetLabels;
    private final int         longStringLimit;
    private String            gpxLabelSource;
    private String            gpxSymbol;
    private int               gpxLabelMinZoom;
    private float             gpxStrokeWidth;
    private String            geoJsonSymbol;
    private String            geoJsonLabelSource;
    private int               geoJsonLabelMinZoom;
    private float             geoJsonStrokeWidth;
    private boolean           zoomWithKeys;
    private final int         minCircleNodes;
    private final double      maxCircleSegment;
    private final double      minCircleSegment;
    private final Set<String> poiKeys;

    public static final String DEFAULT_MAP_STYLE     = "Color Round Nodes";
    public static final String DEFAULT_PEN_MAP_STYLE = "Pen Round Nodes";

    private final SharedPreferences prefs;

    private final Resources r;

    /**
     * Construct a new instance
     * 
     * @param ctx Android context
     */
    @SuppressLint("NewApi")
    public Preferences(@NonNull Context ctx) {
        prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        r = ctx.getResources();
        advancedPrefs = new AdvancedPrefDatabase(ctx);

        // we're not using acra.disable - ensure it isn't present
        if (prefs.contains(ACRA_DISABLE)) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(ACRA_DISABLE);
            editor.commit();
        }
        // we *are* using acra.enable
        if (!prefs.contains(ACRA_ENABLE)) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(ACRA_ENABLE, true);
            editor.commit();
        }

        maxStrokeWidth = getIntPref(R.string.config_maxStrokeWidth_key, 16);

        tileCacheSize = getIntPref(R.string.config_tileCacheSize_key, 100);
        preferRemovableStorage = prefs.getBoolean(r.getString(R.string.config_preferRemovableStorage_key), true);
        mapillaryCacheSize = getIntPref(R.string.config_mapillaryCacheSize_key, de.blau.android.layer.mapillary.MapillaryOverlay.MAPILLARY_DEFAULT_CACHE_SIZE);

        downloadRadius = getIntPref(R.string.config_extTriggeredDownloadRadius_key, 50);
        maxDownloadSpeed = getIntPref(R.string.config_maxDownloadSpeed_key, 10);
        autoPruneBoundingBoxLimit = getIntPref(R.string.config_autoPruneBoundingBoxLimit_key, de.blau.android.layer.data.MapOverlay.DEFAULT_DOWNLOADBOX_LIMIT);
        autoPruneNodeLimit = getIntPref(R.string.config_autoPruneNodeLimit_key, de.blau.android.layer.data.MapOverlay.DEFAULT_AUTOPRUNE_NODE_LIMIT);
        autoPruneTaskLimit = getIntPref(R.string.config_autoPruneTaskLimit_key, de.blau.android.layer.tasks.MapOverlay.DEFAULT_AUTOPRUNE_TASK_LIMIT);
        panAndZoomLimit = getIntPref(R.string.config_panAndZoomLimit_key, de.blau.android.layer.data.MapOverlay.PAN_AND_ZOOM_LIMIT);

        bugDownloadRadius = getIntPref(R.string.config_bugDownloadRadius_key, 200);
        maxBugDownloadSpeed = getIntPref(R.string.config_maxBugDownloadSpeed_key, 30);

        taskFilter = new HashSet<>(Arrays.asList(r.getStringArray(R.array.bug_filter_defaults)));
        taskFilter = prefs.getStringSet(r.getString(R.string.config_bugFilter_key), taskFilter);

        isStatsVisible = prefs.getBoolean(r.getString(R.string.config_showStats_key), false);
        isToleranceVisible = prefs.getBoolean(r.getString(R.string.config_showTolerance_key), true);
        isAntiAliasingEnabled = prefs.getBoolean(r.getString(R.string.config_enableAntiAliasing_key), true);
        tagFormEnabled = prefs.getBoolean(r.getString(R.string.config_tagFormEnabled_key), true);
        supportPresetLabels = prefs.getBoolean(r.getString(R.string.config_supportPresetLabels_key), true);
        isKeepScreenOnEnabled = prefs.getBoolean(r.getString(R.string.config_enableKeepScreenOn_key), false);
        useBackForUndo = prefs.getBoolean(r.getString(R.string.config_use_back_for_undo_key), false);
        largeDragArea = prefs.getBoolean(r.getString(R.string.config_largeDragArea_key), false);
        enableNameSuggestions = prefs.getBoolean(r.getString(R.string.config_enableNameSuggestions_key), true);
        nameSuggestionPresetsEnabled = prefs.getBoolean(r.getString(R.string.config_enableNameSuggestionsPresets_key), true);
        autoApplyPreset = prefs.getBoolean(r.getString(R.string.config_autoApplyPreset_key), true);
        closeChangesetOnSave = prefs.getBoolean(r.getString(R.string.config_closeChangesetOnSave_key), true);
        splitActionBarEnabled = prefs.getBoolean(r.getString(R.string.config_splitActionBarEnabled_key), true);
        scaleLayer = prefs.getString(r.getString(R.string.config_scale_key), r.getString(R.string.scale_metric));
        mapProfile = prefs.getString(r.getString(R.string.config_mapProfile_key), null);
        gpsSource = prefs.getString(r.getString(R.string.config_gps_source_key), r.getString(R.string.gps_source_internal));
        gpsTcpSource = prefs.getString(r.getString(R.string.config_gps_source_tcp_key), "127.0.0.1:1958");
        gpsDistance = getIntPref(R.string.config_gps_distance_key, 2);
        gpsInterval = getIntPref(R.string.config_gps_interval_key, 1000);
        gpxLabelSource = prefs.getString(r.getString(R.string.config_gpx_label_source_key), r.getString(R.string.gpx_automatic));
        gpxSymbol = prefs.getString(r.getString(R.string.config_gpx_symbol_key), TriangleDown.NAME);
        gpxLabelMinZoom = getIntPref(R.string.config_gpx_label_min_zoom_key, Map.SHOW_LABEL_LIMIT);
        // DataStyle may not be available here, so use a constant for the default
        gpxStrokeWidth = prefs.getFloat(r.getString(R.string.config_gpx_stroke_width_key), DataStyle.DEFAULT_GPX_STROKE_WIDTH);
        geoJsonLabelSource = prefs.getString(r.getString(R.string.config_geojson_label_source_key), "");
        geoJsonSymbol = prefs.getString(r.getString(R.string.config_geojson_symbol_key), TriangleDown.NAME);
        geoJsonLabelMinZoom = getIntPref(R.string.config_geojson_label_min_zoom_key, Map.SHOW_LABEL_LIMIT);
        // DataStyle may not be available here, so use a constant for the default
        geoJsonStrokeWidth = prefs.getFloat(r.getString(R.string.config_geojson_stroke_width_key), DataStyle.DEFAULT_GEOJSON_STROKE_WIDTH);

        forceContextMenu = prefs.getBoolean(r.getString(R.string.config_forceContextMenu_key), false);

        osmWiki = prefs.getString(r.getString(R.string.config_osmWiki_key), Urls.DEFAULT_OSM_WIKI);
        offsetServer = prefs.getString(r.getString(R.string.config_offsetServer_key), Urls.DEFAULT_OFFSET_SERVER);
        osmoseServer = prefs.getString(r.getString(R.string.config_osmoseServer_key), Urls.DEFAULT_OSMOSE_SERVER);
        mapRouletteServer = prefs.getString(r.getString(R.string.config_maprouletteServer_key), Urls.DEFAULT_MAPROULETTE_SERVER);
        taginfoServer = prefs.getString(r.getString(R.string.config_taginfoServer_key), Urls.DEFAULT_TAGINFO_SERVER);
        overpassServer = prefs.getString(r.getString(R.string.config_overpassServer_key), Urls.DEFAULT_OVERPASS_SERVER);
        oamServer = prefs.getString(r.getString(R.string.config_oamServer_key), Urls.DEFAULT_OAM_SERVER);

        mapillarySequencesUrlV4 = prefs.getString(r.getString(R.string.config_mapillarySequencesUrlV4_key), Urls.DEFAULT_MAPILLARY_SEQUENCES_URL_V4);
        mapillaryImagesUrlV4 = prefs.getString(r.getString(R.string.config_mapillaryImagesUrlV4_key), Urls.DEFAULT_MAPILLARY_IMAGES_V4);
        mapillaryMinZoom = getIntPref(R.string.config_mapillary_min_zoom_key, de.blau.android.layer.mapillary.MapillaryOverlay.MAPILLARY_DEFAULT_MIN_ZOOM);

        showCameraAction = prefs.getBoolean(r.getString(R.string.config_showCameraAction_key), true);
        cameraApp = prefs.getString(r.getString(R.string.config_selectCameraApp_key), "");
        useInternalPhotoViewer = prefs.getBoolean(r.getString(R.string.config_useInternalPhotoViewer_key), true);
        scanMediaStore = prefs.getBoolean(r.getString(R.string.config_indexMediaStore_key), false);

        generateAlerts = prefs.getBoolean(r.getString(R.string.config_generateAlerts_key), true);
        maxAlertDistance = getIntPref(R.string.config_maxAlertDistance_key, 100);
        groupAlertsOnly = prefs.getBoolean(r.getString(R.string.config_groupAlertsOnly_key), false);

        theme = getThemePref(prefs, r);

        overrideCountryAddressTags = prefs.getBoolean(r.getString(R.string.config_overrideRegionAddressTags_key), false);

        addressTags = prefs.getStringSet(r.getString(R.string.config_addressTags_key),
                new HashSet<>(Arrays.asList(r.getStringArray(R.array.address_tags_defaults))));

        neighbourDistance = getIntPref(R.string.config_neighbourDistance_key, 50);

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
            fullscreenMode = !brokenAndroid.isFullScreenBroken() ? r.getString(R.string.full_screen_auto) : r.getString(R.string.full_screen_never);
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

        uploadCheckerInterval = getIntPref(R.string.config_uploadChecker_key, 6);

        dataWarnLimit = getIntPref(R.string.config_dataWarn_key, 50000);

        useBarometricHeight = prefs.getBoolean(r.getString(R.string.config_useBarometricHeight_key), false);

        useUrlForFeedback = prefs.getBoolean(r.getString(R.string.config_useUrlForFeedback_key), false);

        beepVolume = getIntPref(R.string.config_beepVolume_key, Sound.BEEP_DEFAULT_VOLUME);

        maxOffsetDistance = getIntPref(R.string.config_maxOffsetDistance_key, 100);

        enabledValidations = prefs.getStringSet(r.getString(R.string.config_enabledValidations_key),
                new HashSet<>(Arrays.asList(r.getStringArray(R.array.validations_values))));
        autoNameCap = getIntFromStringPref(R.string.config_nameCap_key, 1);

        wayNodeDragging = prefs.getBoolean(r.getString(R.string.config_wayNodeDragging_key), false);

        splitWindowForPropertyEditor = prefs.getBoolean(r.getString(R.string.config_splitWindowForPropertyEditor_key), false);
        newTaskForPropertyEditor = prefs.getBoolean(r.getString(R.string.config_newTaskForPropertyEditor_key), false);

        useImperialUnits = prefs.getBoolean(r.getString(R.string.config_useImperialUnits_key), false);

        longStringLimit = getIntPref(R.string.config_longStringLimit_key, 80);

        zoomWithKeys = prefs.getBoolean(r.getString(R.string.config_zoomWithKeys_key), false);

        minCircleNodes = getIntPref(R.string.config_minCircleNodes_key, 6);
        maxCircleSegment = getFloatFromStringPref(R.string.config_maxCircleSegment_key, 2.0f);
        minCircleSegment = getFloatFromStringPref(R.string.config_minCircleSegment_key, 0.5f);

        poiKeys = prefs.getStringSet(r.getString(R.string.config_poi_keys_key), new HashSet<>(Arrays.asList(r.getStringArray(R.array.poi_keys_defaults))));
    }

    /**
     * @return the maximum width of a stroke
     */
    public float getMaxStrokeWidth() {
        return maxStrokeWidth;
    }

    /**
     * Get the maximum tile cache size
     * 
     * @return the size of the tile cache in MB
     */
    public int getTileCacheSize() {
        return tileCacheSize;
    }

    /**
     * Check if we should prefer removable storage over built in for tiles
     * 
     * @return true if we should prefer removable storage
     */
    public boolean preferRemovableStorage() {
        return preferRemovableStorage;
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
     * Enable or disable the large drag area
     * 
     * @param enabled if true enable the large drag area
     */
    public void setLargeDragArea(boolean enabled) {
        largeDragArea = enabled;
        prefs.edit().putBoolean(r.getString(R.string.config_largeDragArea_key), enabled).commit();
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
     * Side effect: if the currently configured style doesn't exist, we fallback to an existing one, and set the current
     * style to that
     * 
     * @param currentStyles current styles
     * @return the name of the current data rendering style
     */
    @NonNull
    public String getDataStyle(@NonNull DataStyle currentStyles) {
        // check if we actually still have the profile
        if (currentStyles.getStyle(mapProfile) == null) {
            Log.w(DEBUG_TAG, "Style " + mapProfile + " missing, replacing by default");
            setDataStyle(currentStyles.getStyle(DEFAULT_MAP_STYLE) == null ? DataStyle.getBuiltinStyleName() : DEFAULT_MAP_STYLE);
        }
        return mapProfile;
    }

    /**
     * Set the current data rendering style
     * 
     * @param dataStyle the name of the current data rendering style
     */
    public void setDataStyle(@NonNull String dataStyle) {
        mapProfile = dataStyle;
        prefs.edit().putString(r.getString(R.string.config_mapProfile_key), dataStyle).commit();
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
     * Get the url of the current API configuration
     * 
     * @return the url or null if not found
     */
    @Nullable
    public String getApiUrl() {
        API api = advancedPrefs.getCurrentAPI();
        return api != null ? api.url : null;
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
     * Get the number of BoundingBoxes at which we start attempting a prune
     * 
     * @return the number of BoundingBoxes we consider the limit
     */
    public int getAutoPruneBoundingBoxLimit() {
        return autoPruneBoundingBoxLimit;
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
     * Get the number of Tasks at which we start attempting a prune
     * 
     * @return the number of Tasks we consider the limit
     */
    public int getAutoPruneTaskLimit() {
        return autoPruneTaskLimit;
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
     * Get the configured OSM wiki url
     * 
     * @return base url for the server
     */
    public String getOsmWiki() {
        return osmWiki;
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
     * Get the configured overpass server
     * 
     * @return base url for the server
     */
    public String getOverpassServer() {
        return overpassServer;
    }

    /**
     * Set the configured overpass server
     * 
     * @param url base url for the server
     */
    public void setOverpassServer(@NonNull String url) {
        this.overpassServer = url;
        prefs.edit().putString(r.getString(R.string.config_overpassServer_key), url).commit();
    }

    /**
     * Get the configured OpenAerialMap server
     * 
     * @return base url for the server
     */
    public String getOAMServer() {
        return oamServer;
    }

    /**
     * Set the configured OpenAerialMap server
     * 
     * @param url base url for the server
     */
    public void setOAMServer(@NonNull String url) {
        this.oamServer = url;
        prefs.edit().putString(r.getString(R.string.config_oamServer_key), url).commit();
    }

    /**
     * Get the configured mapillary sequence url
     * 
     * @return the url for retrieving sequences
     */
    public String getMapillarySequencesUrlV4() {
        return mapillarySequencesUrlV4;
    }

    /**
     * Set the configured mapillary sequence url
     * 
     * @param url the url for retrieving sequences
     */
    public void setMapillarySequencseUrlV4(@NonNull String url) {
        this.mapillarySequencesUrlV4 = url;
        prefs.edit().putString(r.getString(R.string.config_mapillarySequencesUrlV4_key), url).commit();
    }

    /**
     * Get the configured mapillary images url
     * 
     * @return the url for retrieving images
     */
    public String getMapillaryImagesUrlV4() {
        return mapillaryImagesUrlV4;
    }

    /**
     * Set the configured mapillary images url
     * 
     * @param url the url for retrieving images
     */
    public void setMapillaryImagesUrlV4(@NonNull String url) {
        this.mapillaryImagesUrlV4 = url;
        prefs.edit().putString(r.getString(R.string.config_mapillaryImagesUrlV4_key), url).commit();
    }

    /**
     * Get the minimum zoom for mapillary data to be displayed
     * 
     * @return the minimum zoom
     */
    public int getMapillaryMinZoom() {
        return mapillaryMinZoom;
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
     * Get the package name for the preferred camera app
     * 
     * @return the package name or an empty string for the system default
     */
    @NonNull
    public String getCameraApp() {
        return cameraApp;
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
     * Check if we should scane the media store for photos
     * 
     * @return true if we should scan the MediaStore
     */
    public boolean scanMediaStore() {
        return scanMediaStore;
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
     * Get the configured theme
     * 
     * @return the current theme
     */
    @NonNull
    public String getTheme() {
        return theme;
    }

    /**
     * Check if we are following the system theme
     * 
     * @return true if we are following the system theme
     */
    public boolean followingSystemTheme() {
        return theme.equals(r.getString(R.string.theme_follow));
    }

    /**
     * Check if the light theme is enabled
     * 
     * @return true if the light theme is enabled
     */
    public boolean lightThemeEnabled() {
        return lightThemeEnabled(theme, r);
    }

    /**
     * Check if the light theme is enabled
     * 
     * @return true if the light theme is enabled
     */
    private static boolean lightThemeEnabled(@NonNull String theme, @NonNull Resources r) {
        if (theme.equals(r.getString(R.string.theme_follow))) {
            // use our copy here that will have been updated by onConfigurationChanged
            final Configuration configuration = App.getConfiguration();
            final int uiMode = configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK;
            return uiMode == Configuration.UI_MODE_NIGHT_NO;
        }
        return theme.equals(r.getString(R.string.theme_light));
    }

    /**
     * Static version for use when we don't want to recreate this class
     * 
     * @param context an Android Context
     * @return true if the light theme is enabled
     */
    public static boolean lightThemeEnabled(@NonNull Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Resources r = context.getResources();
        return lightThemeEnabled(getThemePref(prefs, r), r);
    }

    /**
     * Static utility to get the current theme mode
     * 
     * @param prefs a SharedPreferences object
     * @param r an Resources object
     * @return the current theme mode
     */
    @NonNull
    private static String getThemePref(@NonNull SharedPreferences prefs, @NonNull Resources r) {
        return prefs.getString(r.getString(R.string.config_theme_key),
                r.getString(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? R.string.theme_follow : R.string.theme_light));
    }

    /**
     * Check if we want to override country specific address tags
     * 
     * @return true if we should override
     */
    public boolean overrideCountryAddressTags() {
        return overrideCountryAddressTags;
    }

    /**
     * Get the address tags we currently want to use
     * 
     * @return a Set containing the tags
     */
    @NonNull
    public Set<String> addressTags() {
        return addressTags;
    }

    /**
     * Get the distance in meters up to which two addresses are considered neighbours
     * 
     * @return a distance in meters
     */
    public int getNeighbourDistance() {
        return neighbourDistance;
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
     * Get the enabled validations
     * 
     * @return a Set of strings
     */
    public Set<String> getEnabledValidations() {
        return enabledValidations;
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
     * Get the interval between reminders that you should upload
     * 
     * @return the number of hours between reminders
     */
    public int getUploadCheckerInterval() {
        return uploadCheckerInterval;
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
     * Enable/disable using barometric elevation when recording GPX files
     * 
     * @param on if true barometric elevation will be enabled
     */
    public void enableBarometricHeight(boolean on) {
        useBarometricHeight = on;
        prefs.edit().putBoolean(r.getString(R.string.config_useBarometricHeight_key), on).commit();
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
     * Get the maximum distance up to which we automatically apply locally stored offsets
     * 
     * @return the distance in meters
     */
    public double getMaxOffsetDistance() {
        return maxOffsetDistance;
    }

    /**
     * Enable/disable snapping behaviour when creating new ways
     * 
     * @param on if true the tag filter will be enabled
     */
    public void enableWaySnap(boolean on) {
        prefs.edit().putBoolean(r.getString(R.string.config_waySnap_key), on).commit();
    }

    /**
     * Check if the snapping behaviour when creating new ways is enabled
     * 
     * @return true if enabled
     */
    public boolean isWaySnapEnabled() {
        String key = r.getString(R.string.config_waySnap_key);
        if (!prefs.contains(key)) {
            // create the entry
            enableWaySnap(true);
        }
        return prefs.getBoolean(key, false);
    }

    /**
     * Get the name input type for caps
     * 
     * The value is shifted so that in can directly be used by setInputType
     * 
     * @return the flag to set
     */
    public int getAutoNameCap() {
        return autoNameCap << 13;
    }

    /**
     * CHeck if dragging ow way nodes of the selected way is enabled
     * 
     * @return true if enabled
     */
    public boolean isWayNodeDraggingEnabled() {
        return wayNodeDragging;
    }

    /**
     * Enable or disable the way node dragging
     * 
     * @param enabled if true enable way node dragging
     */
    public void setWayNodeDragging(boolean enabled) {
        wayNodeDragging = enabled;
        prefs.edit().putBoolean(r.getString(R.string.config_wayNodeDragging_key), enabled).commit();
    }

    /**
     * Check if we should try to use split window functionality for the PropertyEditor
     * 
     * @return true if we should use split windows
     */
    public boolean useSplitWindowForPropertyEditor() {
        return splitWindowForPropertyEditor;
    }

    /**
     * Enable or disable using FLAG_ACTIVITY_NEW_TASK for the PropertyEditor
     * 
     * @param enabled if true set FLAG_ACTIVITY_NEW_TASK for the PropertyEditor
     */
    public void setNewTaskForPropertyEditor(boolean enabled) {
        newTaskForPropertyEditor = enabled;
        prefs.edit().putBoolean(r.getString(R.string.config_newTaskForPropertyEditor_key), enabled).commit();
    }

    /**
     * Check if we should set FLAG_ACTIVITY_NEW_TASK for the PropertyEditor
     * 
     * @return true if we should set FLAG_ACTIVITY_NEW_TASK
     */
    public boolean useNewTaskForPropertyEditor() {
        return newTaskForPropertyEditor;
    }

    /**
     * Check if we should use imperial units when measuring in countries that use them or stay with metrix
     * 
     * @return true if we really should use imperial units
     */
    public boolean useImperialUnits() {
        return useImperialUnits;
    }

    /**
     * Check if we should display labels in the form editor or not
     * 
     * @return true if we should display labels
     */
    public boolean supportPresetLabels() {
        return supportPresetLabels;
    }

    /**
     * Get the limit at which we start showing a modal for long strings in the property editor
     * 
     * @return the length from which we will start showing a modal for editing text
     */
    public int getLongStringLimit() {
        return longStringLimit;
    }

    /**
     * Get the current default GPX label source
     * 
     * @return a String with the default source name
     */
    public String getGpxLabelSource() {
        return gpxLabelSource;
    }

    /**
     * Set the current default GPX label source
     * 
     * @param labelSource the default label source
     */
    public void setGpxLabelSource(@NonNull String labelSource) {
        gpxLabelSource = labelSource;
        putString(R.string.config_gpx_label_source_key, labelSource);
    }

    /**
     * Get the current default GPX waypoint symbol
     * 
     * @return a String with the default symbol name
     */
    public String getGpxSynbol() {
        return gpxSymbol;
    }

    /**
     * Set the current GPX waypoint symbol
     * 
     * @param symbol the symbol name
     */
    public void setGpxSymbol(@NonNull String symbol) {
        gpxSymbol = symbol;
        putString(R.string.config_gpx_symbol_key, gpxSymbol);
    }

    /**
     * Get the current default GPX min. zoom level to display labels at
     * 
     * @param the default GPX min. zoom level to display labels at
     */
    public int getGpxLabelMinZoom() {
        return gpxLabelMinZoom;
    }

    /**
     * Set the current default GPX min. zoom level to display labels at
     * 
     * @param the deault GPX min. zoom level to display labels at
     */
    public void setGpxLabelMinZoom(int zoom) {
        gpxLabelMinZoom = zoom;
        prefs.edit().putInt(r.getString(R.string.config_gpx_label_min_zoom_key), gpxLabelMinZoom).commit();
    }

    /**
     * Get the current default GPX stroke width
     * 
     * @param the GPX default stroke width
     */
    public float getGpxStrokeWidth() {
        return gpxStrokeWidth;
    }

    /**
     * Set the current default GPX stroke width
     * 
     * @param the default GPX stroke width
     */
    public void setGpxStrokeWidth(float width) {
        gpxStrokeWidth = width;
        prefs.edit().putFloat(r.getString(R.string.config_gpx_stroke_width_key), gpxStrokeWidth).commit();
    }

    /**
     * Get the current default GeoJSON label source
     * 
     * @return a String with the default source name
     */
    public String getGeoJsonLabelSource() {
        return geoJsonLabelSource;
    }

    /**
     * Set the current default GeoJSON label source
     * 
     * @param labelSource the default label source
     */
    public void setGeoJsonLabelSource(@NonNull String labelSource) {
        geoJsonLabelSource = labelSource;
        putString(R.string.config_geojson_label_source_key, labelSource);
    }

    /**
     * Get the current default GeoJSON point symbol
     * 
     * @return a String with the default symbol name
     */
    public String getGeoJsonSynbol() {
        return geoJsonSymbol;
    }

    /**
     * Set the current GeoJSON point symbol
     * 
     * @param symbol the symbol name
     */
    public void setGeoJsonSymbol(@NonNull String symbol) {
        geoJsonSymbol = symbol;
        putString(R.string.config_geojson_symbol_key, symbol);
    }

    /**
     * Get the current default GeoJSON min. zoom level to display labels at
     * 
     * @param the default GeoJSON min. zoom level to display labels at
     */
    public int getGeoJsonLabelMinZoom() {
        return geoJsonLabelMinZoom;
    }

    /**
     * Set the current default GeoJSON min. zoom level to display labels at
     * 
     * @param the deault GeoJSON min. zoom level to display labels at
     */
    public void setGeoJsonLabelMinZoom(int zoom) {
        geoJsonLabelMinZoom = zoom;
        prefs.edit().putInt(r.getString(R.string.config_geojson_label_min_zoom_key), zoom).commit();
    }

    /**
     * Get the current default GeoJSON stroke width
     * 
     * @param the GeoJSON default stroke width
     */
    public float getGeoJsonStrokeWidth() {
        return geoJsonStrokeWidth;
    }

    /**
     * Set the current default GeoJSON stroke width
     * 
     * @param the default GeoJSON stroke width
     */
    public void setGeoJsonStrokeWidth(float width) {
        geoJsonStrokeWidth = width;
        prefs.edit().putFloat(r.getString(R.string.config_geojson_stroke_width_key), width).commit();
    }

    /**
     * Support using the volume keys for zooming
     * 
     * @return true if the volume keys should be used
     */
    public boolean zoomWithKeys() {
        return zoomWithKeys;
    }

    /**
     * Enable/diable the zoom with keys preference
     * 
     * @param enable value to set
     */
    public void setZoomWithKeys(boolean enable) {
        zoomWithKeys = enable;
        prefs.edit().putBoolean(r.getString(R.string.config_zoomWithKeys_key), enable).commit();
    }

    /**
     * Get the minimum number of nodes a circle should have after circulize or create circle operations
     * 
     * @return the minimal number of nodes
     */
    public int getMinCircleNodes() {
        return minCircleNodes;
    }

    /**
     * Get the max distance two nodes on a circle should have after circulize or create circle operations
     * 
     * @return the min. distance between two circle nodes
     */
    public double getMaxCircleSegment() {
        return maxCircleSegment;
    }

    /**
     * Get the max distance two nodes on a circle should have after circulize or create circle operations
     * 
     * @return the min. distance between two circle nodes
     */
    public double getMinCircleSegment() {
        return minCircleSegment;
    }

    /**
     * Get the keys we currently want to use for the nearby poi display
     * 
     * @return a Set containing the tags
     */
    @NonNull
    public Set<String> poiKeys() {
        return poiKeys;
    }

    /**
     * Get the preset path for item
     * 
     * @param context an Android context
     * @param item the PrestItem
     * 
     * @return a String with the path or null
     */
    @Nullable
    private static String getPresetElementPath(@NonNull Context context, @NonNull PresetItem item) {
        PresetElementPath path = item.getPath(App.getCurrentRootPreset(context).getRootGroup());
        return path != null ? path.toString() : null;
    }

    public boolean applyWithLastValues(@NonNull Context ctx, @NonNull PresetItem item) {
        return getBoolean(USE_LAST_PREF + getPresetElementPath(ctx, item));
    }

    public void setApplyWithLastValues(@NonNull Context ctx, @NonNull PresetItem item, boolean enable) {
        putBoolean(USE_LAST_PREF + getPresetElementPath(ctx, item), enable);
    }

    public boolean applyWithOptionalTags(@NonNull Context ctx, @NonNull PresetItem item) {
        return getBoolean(USE_OPTIONAL_PREF + getPresetElementPath(ctx, item));
    }

    public void setApplyWithOptionalTags(@NonNull Context ctx, @NonNull PresetItem item, boolean enable) {
        putBoolean(USE_OPTIONAL_PREF + getPresetElementPath(ctx, item), enable);
    }

    /**
     * Get an integer valued preference from a string pref
     * 
     * @param keyResId the res id
     * @param def default value
     * @return the stored preference or the default if none found
     */
    private int getIntFromStringPref(int keyResId, int def) {
        try {
            String temp = prefs.getString(r.getString(keyResId), Integer.toString(def));
            return Integer.parseInt(temp);
        } catch (ClassCastException | NumberFormatException e) {
            return def;
        }
    }

    /**
     * Get an float valued preference from a string pref
     * 
     * @param keyResId the res id
     * @param def default value
     * @return the stored preference or the default if none found
     */
    private float getFloatFromStringPref(int keyResId, float def) {
        try {
            String temp = prefs.getString(r.getString(keyResId), Float.toString(def));
            return Float.parseFloat(temp);
        } catch (ClassCastException | NumberFormatException e) {
            return def;
        }
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

    /**
     * Get a boolean value from shared preferences
     * 
     * @param key the preference key
     * @return the value or false if nothing was found
     */
    private boolean getBoolean(@NonNull String key) {
        return prefs.getBoolean(key, false);
    }

    /**
     * Save a boolean to shared preferences
     * 
     * @param key preference key
     * @param b boolean value to save
     */
    private void putBoolean(@NonNull String key, boolean b) {
        prefs.edit().putBoolean(key, b).commit();
    }

    /**
     * Close anything that needs closing
     */
    public void close() {
        if (advancedPrefs != null) {
            advancedPrefs.close();
        }
    }
}
