package de.blau.android;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.acra.ACRA;
import org.acra.annotation.AcraCore;
import org.acra.annotation.AcraDialog;
import org.acra.annotation.AcraHttpSender;
import org.acra.sender.HttpSender;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.ScriptableObject;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.serializers.FSTMapSerializer;

import com.faendir.rhino_android.AndroidContextFactory;
import com.faendir.rhino_android.RhinoAndroidHelper;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.multidex.MultiDex;
import androidx.preference.PreferenceManager;
import de.blau.android.contract.Paths;
import de.blau.android.filter.PresetFilter;
import de.blau.android.net.OkHttpTlsCompat;
import de.blau.android.net.UserAgentInterceptor;
import de.blau.android.nsi.Names;
import de.blau.android.nsi.Names.NameAndTags;
import de.blau.android.osm.DiscardedTags;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.photos.Photo;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.MRUTags;
import de.blau.android.presets.Preset;
import de.blau.android.presets.PresetItem;
import de.blau.android.presets.Synonyms;
import de.blau.android.propertyeditor.PropertyEditorActivity;
import de.blau.android.resources.DataStyle;
import de.blau.android.services.util.MapTileFilesystemProvider;
import de.blau.android.tasks.TaskStorage;
import de.blau.android.util.AreaTags;
import de.blau.android.util.FileUtil;
import de.blau.android.util.GeoContext;
import de.blau.android.util.NotificationCache;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.TagClipboard;
import de.blau.android.util.Util;
import de.blau.android.util.collections.MultiHashMap;
import de.blau.android.util.rtree.RTree;
import de.blau.android.validation.BaseValidator;
import de.blau.android.validation.Validator;
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil;
import okhttp3.OkHttpClient;

@AcraCore(resReportSendSuccessToast = R.string.report_success, resReportSendFailureToast = R.string.report_failure, logcatArguments = { "-t", "500", "-v",
        "time" })
@AcraHttpSender(httpMethod = HttpSender.Method.POST, uri = "https://acrarium.vespucci.io/", resCertificate = R.raw.isrg_root_x1)
@AcraDialog(resText = R.string.crash_dialog_text, resCommentPrompt = R.string.crash_dialog_comment_prompt, resTheme = R.style.Theme_AppCompat_Light_Dialog)

public class App extends Application implements android.app.Application.ActivityLifecycleCallbacks {
    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, App.class.getSimpleName().length());
    private static final String DEBUG_TAG = App.class.getSimpleName().substring(0, TAG_LEN);

    private static final String     RHINO_LAZY_LOAD = "lazyLoad";
    private static App              currentInstance;
    private static StorageDelegator delegator       = new StorageDelegator();
    private static TaskStorage      taskStorage     = new TaskStorage();
    private static OkHttpClient     httpClient;
    private static final Object     httpClientLock  = new Object();
    private static String           userAgent;
    private static final Random     random          = new Random();

    /**
     * The logic that manipulates the model. (non-UI)
     */
    private static Logic logic;

    /**
     * The currently selected presets
     */
    private static Preset[]                         currentPresets;
    private static Preset                           currentRootPreset;
    private static final Object                     currentPresetsLock              = new Object();
    private static MultiHashMap<String, PresetItem> presetSearchIndex               = null;
    private static final Object                     presetSearchIndexLock           = new Object();
    private static MultiHashMap<String, PresetItem> translatedPresetSearchIndex     = null;
    private static final Object                     translatedPresetSearchIndexLock = new Object();

    private static MRUTags mruTags = null;

    /**
     * Synonym list
     */
    private static Synonyms     synonyms;
    private static final Object synonymsLock = new Object();

    /**
     * name index related stuff
     */
    private static Names                             names                = null;
    private static final Object                      namesLock            = new Object();
    private static MultiHashMap<String, NameAndTags> namesSearchIndex     = null;
    private static final Object                      namesSearchIndexLock = new Object();

    /**
     * Geo index to on device photos
     */
    private static RTree<Photo> photoIndex;

    /**
     * Various attributes that are regional
     */
    private static GeoContext   geoContext     = null;
    private static final Object geoContextLock = new Object();

    /**
     * Various tags that should automatically be removed from objects
     */
    private static DiscardedTags discardedTags     = null;
    private static final Object  discardedTagsLock = new Object();

    /**
     * Cache of recent notifications for tasks
     */
    private static NotificationCache taskNotifications;
    private static final Object      taskNotificationsLock = new Object();

    /**
     * Cache of recent notifications for OSM data issues
     */
    private static NotificationCache osmDataNotifications;
    private static final Object      osmDataNotificationsLock = new Object();

    /**
     * Rhino related objects
     */
    private static RhinoAndroidHelper                rhinoHelper;
    private static org.mozilla.javascript.Scriptable rhinoScope;
    private static final Object                      rhinoLock = new Object();

    /**
     * The default element validator
     */
    private static final Object defaultValidatorLock = new Object();
    private static Validator    defaultValidator;

    /**
     * The clipboard for tags
     */
    private static TagClipboard tagClipboard;
    private static final Object tagClipboardLock = new Object();

    /**
     * Utility class for phone number formating
     */
    private static PhoneNumberUtil phoneNumberUtil;
    private static final Object    phoneNumberUtilLock = new Object();

    /**
     * Tile cache
     */
    private static MapTileFilesystemProvider mapTileFilesystemProvider;
    private static final Object              mapTileFilesystemProviderLock = new Object();

    /**
     * DataStyles
     */
    private static DataStyle    dataStyle;
    private static final Object dataStyleLock = new Object();

    /**
     * Implied area tags
     */
    private static AreaTags     areaTags;
    private static final Object areaTagsLock = new Object();

    private static Configuration configuration = null;

    private static boolean propertyEditorRunning;

    private ScheduledThreadPoolExecutor autosaveExecutor = new ScheduledThreadPoolExecutor(1);

    private static FSTConfiguration singletonConf;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
        setupFST();
    }

    /**
     * Setup the FST singleton
     */
    private static void setupFST() {
        singletonConf = FSTConfiguration.createAndroidDefaultConfiguration();
        singletonConf.registerSerializer(TreeMap.class, new FSTMapSerializer(), true);
    }

    @Override
    public void onCreate() {
        ACRA.init(this);
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
        setupMisc(this);
        setConfiguration(getResources().getConfiguration());
        // register a broadcast receiver for DeX mode
        // this will remain registered as long as the
        // application exists
        IntentFilter desktopModeFilter = new IntentFilter("android.app.action.ENTER_KNOX_DESKTOP_MODE");
        desktopModeFilter.addAction("android.app.action.EXIT_KNOX_DESKTOP_MODE");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(new DesktopModeReceiver(), desktopModeFilter, RECEIVER_EXPORTED);
        } else {
            registerReceiver(new DesktopModeReceiver(), desktopModeFilter);
        }
    }

    /**
     * Setup misc singletons
     */
    private static void setupMisc(@NonNull App app) {
        String appName = app.getString(R.string.app_name);
        String appVersion = app.getString(R.string.app_version);
        userAgent = appName + "/" + appVersion;
        currentInstance = app;
    }

    /**
     * Retrieve the saved Configuration object
     * 
     * This is used to determine what bits of the configuration have changed and may be stale, in practical terms this
     * should never be null
     * 
     * @return the saved Configuration or null
     */
    @Nullable
    public static Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Save the current Configuration object
     * 
     * @param c a COnfiguration
     */
    public static void setConfiguration(@NonNull Configuration c) {
        configuration = new Configuration(c);
    }

    /**
     * Get the current StorageDelegator instance
     * 
     * @return the current StorageDelegator
     */
    @NonNull
    public static StorageDelegator getDelegator() {
        return delegator;
    }

    /**
     * Get the current TaskStorage instance
     * 
     * @return the current TaskStorage
     */
    @NonNull
    public static TaskStorage getTaskStorage() {
        return taskStorage;
    }

    /**
     * Get the current running App instance
     * 
     * @return the current App instance
     * 
     * @deprecated using this is bad practice and should avoided as far as possible
     */
    @Deprecated
    @Nullable
    public static App getCurrentInstance() {
        return currentInstance;
    }

    /**
     * Get the Resources from the current running App instance
     * 
     * Mainly used in situations in which we need to display, potentially translated, text and don't have an Context
     * available
     * 
     * @return a Resources instance
     * 
     * @deprecated using this is bad practice and should avoided as far as possible
     */
    @Deprecated
    @Nullable
    public static Resources resources() {
        if (currentInstance != null) {
            return currentInstance.getResources();
        }
        return null;
    }

    /**
     * Get the OkHttpClient singleton
     * 
     * @return the OkHttpClient for this app
     */
    @NonNull
    public static OkHttpClient getHttpClient() {
        synchronized (httpClientLock) {
            if (httpClient == null) {
                OkHttpClient.Builder builder = OkHttpTlsCompat.getBuilder(new OkHttpClient.Builder());
                builder.addNetworkInterceptor(new UserAgentInterceptor(userAgent));
                httpClient = builder.build();
            }
            return httpClient;
        }
    }

    /**
     * Get the currently enabled Presets If the presets haven't been read yet, this will do so
     * 
     * @param ctx Android Context
     * @return an array of Preset
     */
    @NonNull
    public static Preset[] getCurrentPresets(@NonNull Context ctx) {
        synchronized (currentPresetsLock) {
            if (currentPresets == null) {
                try {
                    Util.clearDataLayerIconCaches(false);
                    currentPresets = getPreferences(ctx).getPreset();
                    mruTags = new MRUTags();
                    mruTags.load(ctx);
                    if (logic != null && logic.getFilter() instanceof PresetFilter) {
                        // getCurrentPresets will be called if the presets have been
                        // changed and the preset need to be re-referenced
                        logic.getFilter().init(ctx);
                    }
                } catch (OutOfMemoryError oome) {
                    Util.runOnUiThread(ctx, () -> ScreenMessage.toastTopError(ctx, R.string.out_of_memory_title));
                }
            }
            return currentPresets;
        }
    }

    /**
     * Get the current Preferences object held by Logic or create a new one
     * 
     * @param ctx an Android Context
     * @return a Preferences instance
     */
    @NonNull
    public static Preferences getPreferences(@NonNull Context ctx) {
        boolean havePrefs = logic != null && logic.getPrefs() != null;
        if (!havePrefs) {
            Log.e(DEBUG_TAG, "Logic null or doesn't have a Preference object");
        }
        return havePrefs ? logic.getPrefs() : new Preferences(ctx);
    }

    /**
     * Get a single Preset containing all currently active Presets
     * 
     * @param ctx Android Context
     * @return a Preset containing all currently active Presets
     */
    @NonNull
    public static Preset getCurrentRootPreset(@NonNull Context ctx) {
        synchronized (currentPresetsLock) {
            if (currentRootPreset == null) {
                Preset[] presets = App.getCurrentPresets(ctx);
                currentRootPreset = Preset.dummyInstance();
                currentRootPreset.addToRootGroup(presets);
            }
        }
        return currentRootPreset;
    }

    /**
     * Resets the current presets, causing them to be re-parsed when they are re-requested
     */
    public static void resetPresets() {
        synchronized (currentPresetsLock) {
            currentPresets = null;
            currentRootPreset = null;
            presetSearchIndex = null;
            translatedPresetSearchIndex = null;
        }
        synchronized (defaultValidatorLock) {
            defaultValidator = null;
        }
        delegator.resetProblems();
    }

    /**
     * Get the object holding the most-recently-used tags
     * 
     * @return an MRUTags instance
     */
    @NonNull
    public static MRUTags getMruTags() {
        synchronized (currentPresetsLock) {
            if (mruTags == null) {
                mruTags = new MRUTags();
            }
        }
        return mruTags;
    }

    /**
     * Get the preset search index
     * 
     * @param ctx an Android Context
     * @return a MultiHashMap with the index
     */
    @NonNull
    public static MultiHashMap<String, PresetItem> getPresetSearchIndex(@NonNull Context ctx) {
        synchronized (presetSearchIndexLock) {
            if (presetSearchIndex == null) {
                presetSearchIndex = Preset.getSearchIndex(getCurrentPresets(ctx));
            }
            return presetSearchIndex;
        }
    }

    /**
     * Get the translated preset search index
     * 
     * @param ctx an Android Context
     * @return a MultiHashMap with the index
     */
    @NonNull
    public static MultiHashMap<String, PresetItem> getTranslatedPresetSearchIndex(@NonNull Context ctx) {
        synchronized (translatedPresetSearchIndexLock) {
            if (translatedPresetSearchIndex == null) {
                translatedPresetSearchIndex = Preset.getTranslatedSearchIndex(getCurrentPresets(ctx));
            }
            return translatedPresetSearchIndex;
        }
    }

    /**
     * Return a object containing the current (Locale specific) list of preset synonyms
     * 
     * Caches the object returned
     * 
     * @param ctx Android Context
     * @return a Synonyms instance
     */
    @NonNull
    public static Synonyms getSynonyms(@NonNull Context ctx) {
        synchronized (synonymsLock) {
            if (synonyms == null) {
                synonyms = new Synonyms(ctx);
            }
            return synonyms;
        }
    }

    /**
     * Get a normalized name to Names entries map
     * 
     * @param ctx Android Context
     * @return a map between normalized names and NameAndTags objects
     */
    @NonNull
    public static MultiHashMap<String, NameAndTags> getNameSearchIndex(@NonNull Context ctx) {
        getNames(ctx);
        synchronized (namesSearchIndexLock) {
            if (namesSearchIndex == null) {
                namesSearchIndex = names.getSearchIndex();
            }
            return namesSearchIndex;
        }
    }

    /**
     * Return the object containing the canonical name data
     * 
     * @param ctx Android context
     * @return an instance of Names
     */
    @NonNull
    public static Names getNames(@NonNull Context ctx) {
        synchronized (namesLock) {
            if (names == null) {
                // this should be done async if it takes too long
                names = new Names(ctx);
            }
            return names;
        }
    }

    /**
     * Returns the current in-memory index, use resetPhotoIndex to initialize/reset
     * 
     * @return a RTree with the index
     */
    public static RTree<Photo> getPhotoIndex() {
        return photoIndex;
    }

    /**
     * Empty the index
     */
    public static void resetPhotoIndex() {
        photoIndex = new RTree<>(20, 50);
    }

    /**
     * Set up the GeoCOntext object
     * 
     * @param ctx Android Context
     */
    public static void initGeoContext(@NonNull Context ctx) {
        synchronized (geoContextLock) {
            if (geoContext == null && !Util.smallHeap()) {
                geoContext = new GeoContext(ctx);
            }
        }
    }

    /**
     * Get the GeoContext object
     * 
     * @param ctx Android Context
     * @return the current GeoContext object
     */
    @Nullable
    public static GeoContext getGeoContext(@NonNull Context ctx) {
        initGeoContext(ctx);
        return geoContext;
    }

    /**
     * Get the GeoContext object
     * 
     * @return the GeoCOntext object or null if it hasn't been created yet
     */
    @Nullable
    public static GeoContext getGeoContext() {
        return geoContext;
    }

    /**
     * Get the TagClipboard object
     * 
     * @param ctx Android Context
     * @return the current GeoContext object
     */
    @NonNull
    public static TagClipboard getTagClipboard(@NonNull Context ctx) {
        synchronized (tagClipboardLock) {
            if (tagClipboard == null) {
                tagClipboard = new TagClipboard();
                tagClipboard.restore(ctx);
            }
            return tagClipboard;
        }
    }

    /**
     * Get the GeoContext object
     * 
     * @param ctx Android Context
     * @return the current GeoContext object
     */
    @NonNull
    public static DiscardedTags getDiscardedTags(@NonNull Context ctx) {
        synchronized (discardedTagsLock) {
            if (discardedTags == null) {
                discardedTags = new DiscardedTags(ctx);
            }
        }
        return discardedTags;
    }

    /**
     * Get the current instance of Logic
     * 
     * @return the current Logic instance
     */
    @Nullable
    public static synchronized Logic getLogic() {
        return logic;
    }

    /**
     * Allocate new logic, logic contains some state and should only exist once
     * 
     * @return a new instance of the Logic class
     */
    public static synchronized Logic newLogic() {
        if (logic == null) {
            logic = new Logic();
        }
        return logic;
    }

    /**
     * Return the cache for task notifications, allocate if necessary
     * 
     * @param ctx Android Context
     * @return the notification cache
     */
    public static NotificationCache getTaskNotifications(Context ctx) {
        synchronized (taskNotificationsLock) {
            if (taskNotifications == null) {
                taskNotifications = new NotificationCache(ctx);
            }
            return taskNotifications;
        }
    }

    /**
     * If the cache is empty replace it
     * 
     * @param ctx Android Context
     * @param cache a NotificationCache object
     */
    public static void setTaskNotifications(@NonNull Context ctx, @NonNull NotificationCache cache) {
        synchronized (taskNotificationsLock) {
            if (taskNotifications == null || taskNotifications.isEmpty()) {
                taskNotifications = cache;
                taskNotifications.trim(ctx);
            }
        }
    }

    /**
     * Return the cache for osm data notifications, allocate if necessary
     * 
     * @param ctx Android Context
     * @return the current NotificationCache object
     */
    @NonNull
    public static NotificationCache getOsmDataNotifications(@NonNull Context ctx) {
        synchronized (osmDataNotificationsLock) {
            if (osmDataNotifications == null) {
                osmDataNotifications = new NotificationCache(ctx);
            }
            return osmDataNotifications;
        }
    }

    /**
     * If the cache is empty replace it
     * 
     * @param ctx Android Context
     * @param cache a NotificationCache object
     */
    public static void setOsmDataNotifications(@NonNull Context ctx, @NonNull NotificationCache cache) {
        synchronized (osmDataNotificationsLock) {
            if (osmDataNotifications == null || osmDataNotifications.isEmpty()) {
                osmDataNotifications = cache;
                osmDataNotifications.trim(ctx);
            }
        }
    }

    /**
     * Return a rhino helper for scripting
     * 
     * @param ctx android context
     * @return a RhinoAndroidHelper
     */
    public static RhinoAndroidHelper getRhinoHelper(@NonNull Context ctx) {
        synchronized (rhinoLock) {
            if (rhinoHelper == null) {
                rhinoHelper = new RhinoAndroidHelper(ctx) {
                    @Override
                    protected AndroidContextFactory createAndroidContextFactory(File cacheDirectory) {
                        return new AndroidContextFactory(cacheDirectory) {
                            @Override
                            protected boolean hasFeature(org.mozilla.javascript.Context cx, int featureIndex) {
                                if (featureIndex == org.mozilla.javascript.Context.FEATURE_ENABLE_XML_SECURE_PARSING) {
                                    return false;
                                }
                                return super.hasFeature(cx, featureIndex);
                            }
                        };
                    }
                };
            }
            return rhinoHelper;
        }
    }

    /**
     * Return a sandboxed rhino scope for scripting
     * 
     * Allows access to the java package but not to the app internals FIXME not clear if we can use the same scope the
     * whole time
     * 
     * @param ctx android context
     * @return rhino scope
     */
    public static org.mozilla.javascript.Scriptable getRestrictedRhinoScope(@NonNull Context ctx) {
        synchronized (rhinoLock) {
            if (rhinoScope == null) {
                org.mozilla.javascript.Context c = rhinoHelper.enterContext();
                try {
                    // this is a fairly hackish way of sandboxing, but it does work
                    rhinoScope = new ImporterTopLevel(c);
                    c.evaluateString(rhinoScope, "java", RHINO_LAZY_LOAD, 0, null);
                    // note any classes loaded here need to be kept in the ProGuard configuration
                    c.evaluateString(rhinoScope, "importClass(com.mapbox.turf.TurfMeasurement)", RHINO_LAZY_LOAD, 0, null);
                    c.evaluateString(rhinoScope, "importClass(Packages.de.blau.android.osm.BoundingBox)", RHINO_LAZY_LOAD, 0, null);
                    c.evaluateString(rhinoScope, "importClass(Packages.de.blau.android.util.GeoMath)", RHINO_LAZY_LOAD, 0, null);
                    c.evaluateString(rhinoScope, "importClass(Packages.de.blau.android.util.collections.LongPrimitiveList)", RHINO_LAZY_LOAD, 0, null);
                    ((ScriptableObject) rhinoScope).sealObject();
                } finally {
                    org.mozilla.javascript.Context.exit();
                }
            }
            return rhinoScope;
        }
    }

    /**
     * Return a Validator instance, allocate if necessary
     * 
     * @param ctx Android Context
     * @return a Validator instance
     */
    public static Validator getDefaultValidator(@NonNull Context ctx) {
        synchronized (defaultValidatorLock) {
            if (defaultValidator == null) {
                defaultValidator = new BaseValidator(ctx);
            }
            return defaultValidator;
        }
    }

    /**
     * Get an new instance of the phone number utilities class
     * 
     * @param ctx an Android Context
     * @return a PhoneNumberUtil instance
     */
    @Nullable
    public static PhoneNumberUtil getPhoneNumberUtil(@NonNull Context ctx) {
        synchronized (phoneNumberUtilLock) {
            if (phoneNumberUtil == null) {
                phoneNumberUtil = PhoneNumberUtil.createInstance(ctx);
            }
            return phoneNumberUtil;
        }
    }

    /**
     * Get a MapTileProvider for tiles cached on device
     * 
     * @param ctx am Android Context
     * @return a MapTileFilesystemProvider or null
     */
    @Nullable
    public static MapTileFilesystemProvider getMapTileFilesystemProvider(@NonNull Context ctx) {
        synchronized (mapTileFilesystemProviderLock) {
            if (mapTileFilesystemProvider == null) {
                mapTileFilesystemProvider = MapTileFilesystemProvider.getInstance(ctx);
            }
            return mapTileFilesystemProvider;
        }
    }

    /**
     * Get the DataStyle object
     * 
     * @param ctx am Android Context
     * @return a DataStyle object
     */
    @NonNull
    public static DataStyle getDataStyle(@NonNull Context ctx) {
        synchronized (dataStyleLock) {
            if (dataStyle == null) {
                dataStyle = new DataStyle(ctx);
                dataStyle.getStylesFromFiles(ctx);
            }
            return dataStyle;
        }
    }

    /**
     * Get the AreaTags object
     * 
     * @param ctx am Android Context
     * @return an AreaTags object
     */
    @NonNull
    public static AreaTags getAreaTags(@NonNull Context ctx) {
        synchronized (areaTagsLock) {
            if (areaTags == null) {
                areaTags = new AreaTags(ctx);
            }
            return areaTags;
        }
    }

    /**
     * Get the userAgent string for this version of the app
     * 
     * @return the userAgent
     */
    public static String getUserAgent() {
        return userAgent;
    }

    /**
     * Get the current serialisation singleton
     * 
     * @return an instance of FSTConfiguration
     */
    @NonNull
    public static FSTConfiguration getFSTInstance() {
        return singletonConf;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (activity instanceof PropertyEditorActivity) {
            synchronized (this) {
                propertyEditorRunning = true;
            }
            return;
        }
        if (activity instanceof Main) {
            startAutosave();
        }
    }

    /**
     * Start auto save for edits
     */
    private void startAutosave() {
        Log.i(DEBUG_TAG, "Starting autosave");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean saveState = prefs.getBoolean(getString(R.string.config_autosaveSaveState_key), true);
        final boolean saveChanges = prefs.getBoolean(getString(R.string.config_autosaveSaveChanges_key), true);
        final int interval = prefs.getInt(getString(R.string.config_autosaveInterval_key), 5);
        final int changes = prefs.getInt(getString(R.string.config_autosaveChanges_key), 1);
        final int maxFiles = prefs.getInt(getString(R.string.config_autosaveMaxFiles_key), 5);
        autosaveExecutor.scheduleAtFixedRate(() -> {
            if (delegator.isDirty() && delegator.getApiElementCount() >= changes) {
                if (logic != null && saveState) {
                    logic.save(this);
                }
                if (saveChanges) {
                    try {
                        final File autosaveDir = FileUtil.getPublicDirectory(FileUtil.getPublicDirectory(), Paths.DIRECTORY_PATH_AUTOSAVE);
                        File outfile = new File(autosaveDir, SavingHelper.getExportFilename(delegator));
                        try (FileOutputStream fout = new FileOutputStream(outfile); OutputStream outputStream = new BufferedOutputStream(fout)) {
                            delegator.export(outputStream);
                        }
                        FileUtil.pruneFiles(autosaveDir, maxFiles);
                    } catch (Exception e) {
                        Log.e(DEBUG_TAG, "Autosave failed" + e.getMessage());
                    }
                }
            }
        }, interval, interval, TimeUnit.MINUTES);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        // unused
    }

    @Override
    public void onActivityResumed(Activity activity) {
        // unused
    }

    @Override
    public void onActivityPaused(Activity activity) {
        // unused
    }

    @Override
    public void onActivityStopped(Activity activity) {
        // unused
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        // unused
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (activity instanceof PropertyEditorActivity) {
            synchronized (this) {
                propertyEditorRunning = false;
            }
        }
    }

    /**
     * Check if the PropertyEditor has been started
     * 
     * @return true if the PropertyEditor has been started
     */
    public static boolean isPropertyEditorRunning() {
        return propertyEditorRunning;
    }

    /**
     * Retrieve the app wide Random instance
     * 
     * @return the Random instance
     */
    public static Random getRandom() {
        return random;
    }
}