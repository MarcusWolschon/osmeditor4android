package de.blau.android;

import org.acra.ACRA;
import org.acra.annotation.AcraCore;
import org.acra.annotation.AcraDialog;
import org.acra.annotation.AcraHttpSender;
import org.acra.sender.HttpSender;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.ScriptableObject;

import com.faendir.rhino_android.RhinoAndroidHelper;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import de.blau.android.names.Names;
import de.blau.android.names.Names.NameAndTags;
import de.blau.android.net.UserAgentInterceptor;
import de.blau.android.osm.DiscardedTags;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.presets.Synonyms;
import de.blau.android.tasks.TaskStorage;
import de.blau.android.util.GeoContext;
import de.blau.android.util.NotificationCache;
import de.blau.android.util.OkHttpTlsCompat;
import de.blau.android.util.collections.MultiHashMap;
import de.blau.android.util.rtree.RTree;
import de.blau.android.validation.BaseValidator;
import de.blau.android.validation.Validator;
import okhttp3.OkHttpClient;

@AcraCore(resReportSendSuccessToast = R.string.report_success, resReportSendFailureToast = R.string.report_failure)
@AcraHttpSender(httpMethod = HttpSender.Method.POST, uri = "http://acralyzer.vespucci.io/acraproxy")
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
    private static final Object                     currentPresetsLock              = new Object();
    private static MultiHashMap<String, PresetItem> presetSearchIndex               = null;
    private static final Object                     presetSearchIndexLock           = new Object();
    private static MultiHashMap<String, PresetItem> translatedPresetSearchIndex     = null;
    private static final Object                     translatedPresetSearchIndexLock = new Object();

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

    @Override
    public void onCreate() {
        // The following line triggers the initialization of ACRA
        ACRA.init(this);
        super.onCreate();
        String appName = getString(R.string.app_name);
        String appVersion = getString(R.string.app_version);
        userAgent = appName + "/" + appVersion;
        currentInstance = this;
    }

    /**
     * Get the current StorageDelegator instance
     * 
     * @return the current StorageDelegator
     */
    @Nullable
    public static StorageDelegator getDelegator() {
        return delegator;
    }

    @Nullable
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

    @NonNull
    public static Preset[] getCurrentPresets(@NonNull Context ctx) {
        synchronized (currentPresetsLock) {
            if (currentPresets == null) {
                Preferences prefs = new Preferences(ctx);
                currentPresets = prefs.getPreset();
            }
            return currentPresets;
        }
    }

    /**
     * Resets the current presets, causing them to be re-parsed
     */
    public static void resetPresets() {
        synchronized (currentPresetsLock) {
            currentPresets = null;
            presetSearchIndex = null;
            translatedPresetSearchIndex = null;
        }
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
            if (geoContext == null) {
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
}