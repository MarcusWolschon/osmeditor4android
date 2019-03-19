package de.blau.android;

import org.acra.ACRA;
import org.acra.annotation.AcraCore;
import org.acra.annotation.AcraDialog;
import org.acra.annotation.AcraHttpSender;
import org.acra.sender.HttpSender;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.ScriptableObject;
import org.nustaq.serialization.FSTConfiguration;

import com.faendir.rhino_android.RhinoAndroidHelper;

import android.content.Context;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import de.blau.android.names.Names;
import de.blau.android.names.Names.NameAndTags;
import de.blau.android.net.UserAgentInterceptor;
import de.blau.android.osm.DiscardedTags;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.MRUTags;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.presets.Synonyms;
import de.blau.android.tasks.TaskStorage;
import de.blau.android.util.GeoContext;
import de.blau.android.util.NotificationCache;
import de.blau.android.util.OkHttpTlsCompat;
import de.blau.android.util.TagClipboard;
import de.blau.android.util.Util;
import de.blau.android.util.collections.MultiHashMap;
import de.blau.android.util.rtree.RTree;
import de.blau.android.validation.BaseValidator;
import de.blau.android.validation.Validator;
import okhttp3.OkHttpClient;

@AcraCore(resReportSendSuccessToast = R.string.report_success, resReportSendFailureToast = R.string.report_failure)
@AcraHttpSender(httpMethod = HttpSender.Method.POST, uri = "https://acralyzer.vespucci.io/acraproxy")
@AcraDialog(resText = R.string.crash_dialog_text, resCommentPrompt = R.string.crash_dialog_comment_prompt, resTheme = R.style.Theme_AppCompat_Light_Dialog)

public class App extends android.app.Application {
    private static App              currentInstance;
    private static StorageDelegator delegator      = new StorageDelegator();
    private static TaskStorage      taskStorage    = new TaskStorage();
    private static OkHttpClient     httpClient;
    private static final Object     httpClientLock = new Object();
    private static String           userAgent;

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
    private static RTree photoIndex;

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

    private static Configuration configuration = null;

    @Override
    public void onCreate() {
        // The following line triggers the initialization of ACRA
        ACRA.init(this);
        super.onCreate();
        String appName = getString(R.string.app_name);
        String appVersion = getString(R.string.app_version);
        userAgent = appName + "/" + appVersion;
        currentInstance = this;
        setConfiguration(getResources().getConfiguration());
        // register a broadcast receiver for DeX mode
        // this will remain registered as long as the
        // application exists
        IntentFilter desktopModeFilter = new IntentFilter("android.app.action.ENTER_KNOX_DESKTOP_MODE");
        desktopModeFilter.addAction("android.app.action.EXIT_KNOX_DESKTOP_MODE");
        registerReceiver(new DesktopModeReceiver(), desktopModeFilter);
    }

    @Nullable
    public static Configuration getConfiguration() {
        return configuration;
    }

    public static void setConfiguration(Configuration c) {
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

    @Nullable
    public static App getCurrentInstance() {
        return currentInstance;
    }

    @Nullable
    public static Resources resources() {
        return currentInstance.getResources();
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
                OkHttpClient.Builder builder = OkHttpTlsCompat.enableTls12IfNeeded(new OkHttpClient.Builder());
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
                Preferences prefs = new Preferences(ctx);
                currentPresets = prefs.getPreset();
                mruTags = new MRUTags();
                mruTags.load(ctx);
            }
            return currentPresets;
        }
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
    }

    @NonNull
    public static MRUTags getMruTags() {
        synchronized (currentPresetsLock) {
            if (mruTags == null) {
                mruTags = new MRUTags();
            }
        }
        return mruTags;
    }

    @NonNull
    public static MultiHashMap<String, PresetItem> getPresetSearchIndex(@NonNull Context ctx) {
        synchronized (presetSearchIndexLock) {
            if (presetSearchIndex == null) {
                presetSearchIndex = Preset.getSearchIndex(getCurrentPresets(ctx));
            }
            return presetSearchIndex;
        }
    }

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
    public static RTree getPhotoIndex() {
        return photoIndex;
    }

    /**
     * Empty the index
     */
    public static void resetPhotoIndex() {
        photoIndex = new RTree(20, 50);
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
    @NonNull
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
    public static Logic getLogic() {
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
    public static RhinoAndroidHelper getRhinoHelper(Context ctx) {
        synchronized (rhinoLock) {
            if (rhinoHelper == null) {
                rhinoHelper = new RhinoAndroidHelper(ctx);
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
    public static org.mozilla.javascript.Scriptable getRestrictedRhinoScope(Context ctx) {
        synchronized (rhinoLock) {
            if (rhinoScope == null) {
                org.mozilla.javascript.Context c = rhinoHelper.enterContext();
                try {
                    // this is a fairly hackish way of sandboxing, but it does work
                    // rhinoScope = c.initStandardObjects(); // don't seal the individual objects
                    rhinoScope = new ImporterTopLevel(c);
                    c.evaluateString(rhinoScope, "java", "lazyLoad", 0, null);
                    // note any classes loaded here need to be kept in the ProGuard configuration
                    c.evaluateString(rhinoScope, "importClass(Packages.de.blau.android.osm.BoundingBox)", "lazyLoad", 0, null);
                    c.evaluateString(rhinoScope, "importClass(Packages.de.blau.android.util.GeoMath)", "lazyLoad", 0, null);
                    ((ScriptableObject) rhinoScope).sealObject();
                } finally {
                    org.mozilla.javascript.Context.exit();
                }
            }
            return rhinoScope;
        }
    }

    /**
     * Return the cache for task notifications, allocate if necessary
     * 
     * @param ctx Android Context
     * @return the notification cache
     */
    public static Validator getDefaultValidator(Context ctx) {
        synchronized (defaultValidatorLock) {
            if (defaultValidator == null) {
                defaultValidator = new BaseValidator(ctx);
            }
            return defaultValidator;
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
    
    private static FSTConfiguration singletonConf = FSTConfiguration.createAndroidDefaultConfiguration();
    /**
     * Get the current serialisation singleton
     * 
     * @return an instance of FSTConfiguration
     */
    @NonNull
    public static FSTConfiguration getFSTInstance() {
        return singletonConf;
    }
}