package de.blau.android;

import java.util.Map;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import de.blau.android.names.Names;
import de.blau.android.names.Names.NameAndTags;
import de.blau.android.net.UserAgentInterceptor;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.tasks.TaskStorage;
import de.blau.android.util.MultiHashMap;
import de.blau.android.util.NotificationCache;
import de.blau.android.util.rtree.RTree;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

@ReportsCrashes(
	reportType = org.acra.sender.HttpSender.Type.JSON,
	httpMethod = org.acra.sender.HttpSender.Method.PUT,
	formUri = "http://acralyzer.vespucci.io/acraproxy",
	mode = ReportingInteractionMode.DIALOG,
	resDialogText = R.string.crash_dialog_text,
	resDialogCommentPrompt = R.string.crash_dialog_comment_prompt)
public class Application extends android.app.Application {
	public static Main mainActivity;
	static StorageDelegator delegator = new StorageDelegator();
	static TaskStorage taskStorage = new TaskStorage();
	static OkHttpClient httpClient;
	private static final Object httpClientLock = new Object();
	public static String userAgent;
	
	static Application currentApplication;
	
	/**
	 * The logic that manipulates the model. (non-UI)<br/>
	 */
	private static Logic logic;
	
	/**
	 * The currently selected presets
	 */
	private static Preset[] currentPresets;
	private static final Object currentPresetsLock = new Object();
	private static MultiHashMap<String, PresetItem> presetSearchIndex = null;
	private static final Object presetSearchIndexLock = new Object();
	private static MultiHashMap<String, PresetItem> translatedPresetSearchIndex = null;
	private static final Object translatedPresetSearchIndexLock = new Object();
	/**
	 * name index related stuff
	 */
	private static Names names = null;
	private static final Object namesLock = new Object();
	private static Map<String,NameAndTags> namesSearchIndex = null;
	private static final Object namesSearchIndexLock = new Object();
	/**
	 * Geo index to on device photos
	 */
	private static RTree photoIndex;
	
	/**
	 * Cache of recent notifications for tasks
	 */
	private static NotificationCache taskNotifications;
	private static final Object taskNotificationsLock = new Object();
	
	/**
	 * Cache of recent notifications for OSM data issues
	 */
	private static NotificationCache osmDataNotifications;
	private static final Object osmDataNotificationsLock = new Object();
	
	
	@Override
	public void onCreate() {
		// The following line triggers the initialization of ACRA
		ACRA.init(this);
		super.onCreate();
		String appName = getString(R.string.app_name);
		String appVersion = getString(R.string.app_version);
		userAgent = appName + "/" + appVersion;
		currentApplication = this;
	}

	public static Application getCurrentApplication() {
		return currentApplication;
	}
	
	public static StorageDelegator getDelegator() {
		return delegator;
	}
	
	public static TaskStorage getTaskStorage() {
		return taskStorage;
	}

	@NonNull
	public static OkHttpClient getHttpClient() {
		synchronized(httpClientLock) {
			if (httpClient == null) {
				OkHttpClient.Builder builder = new OkHttpClient.Builder();
				builder.addNetworkInterceptor(new UserAgentInterceptor(userAgent));
				if (BuildConfig.DEBUG) {
					HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
					httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);
					builder.addNetworkInterceptor(httpLoggingInterceptor);
				}
				httpClient = builder.build();
			}
			return httpClient;
		}
	}

	public static Preset[] getCurrentPresets(Context ctx) {
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
			System.gc(); // not sure if this actually helps
		}
	}

	public static MultiHashMap<String, PresetItem> getPresetSearchIndex(Context ctx) {
		synchronized (presetSearchIndexLock) {
			if (presetSearchIndex == null) {
				presetSearchIndex = Preset.getSearchIndex(getCurrentPresets(ctx));
			}
			return presetSearchIndex;
		}
	}

	public static MultiHashMap<String, PresetItem> getTranslatedPresetSearchIndex(Context ctx) {
		synchronized (translatedPresetSearchIndexLock) {
			if (translatedPresetSearchIndex == null) {
				translatedPresetSearchIndex = Preset.getTranslatedSearchIndex(getCurrentPresets(ctx));
			}
			return translatedPresetSearchIndex;
		}
	}
	
	public static Map<String,NameAndTags> getNameSearchIndex(Context ctx) {
		getNames(ctx);
		synchronized (namesSearchIndexLock) {
			if (namesSearchIndex == null) {
				// names.dump2Log();
				namesSearchIndex = names.getSearchIndex();
			}
			return namesSearchIndex;
		}
	}

	public static Names getNames(Context ctx) {
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
	 * @return
	 */
	public static RTree getPhotoIndex() {
		return photoIndex;
	}
	
	public static void resetPhotoIndex() {
		photoIndex = new RTree(20,50);
	}
	
	/**
	 * @return the logic
	 */
	public static Logic getLogic() {
		return logic;
	}
	
	/**
	 * Allocate new logic, logic contains some state and should only exist once
	 * @param map
	 * @return
	 */
	public synchronized static Logic newLogic() {
		if (logic==null) {
			logic = new Logic();
		}
		return logic;
	}
	
	/**
	 * Return the cache for task notifications, allocate if necessary
	 * @return
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
	 * @param cache
	 */
	public static void setTaskNotifications(Context ctx, NotificationCache cache) {
		synchronized (taskNotificationsLock) {
			if (taskNotifications == null  || taskNotifications.isEmpty()) {
				taskNotifications = cache;
				taskNotifications.trim(ctx);
			}
		}
	}
	
	/**
	 * Return the cache for osm data notifications, allocate if necessary
	 * @return
	 */
	public static NotificationCache getOsmDataNotifications(Context ctx) {
		synchronized (osmDataNotificationsLock) {
			if (osmDataNotifications == null) {
				osmDataNotifications = new NotificationCache(ctx);
			}
			return osmDataNotifications;
		}
	}

	/**
	 * If the cache is empty replace it
	 * @param cache
	 */
	public static void setOsmDataNotifications(Context ctx,NotificationCache cache) {
		synchronized (osmDataNotificationsLock) {
			if (osmDataNotifications == null || osmDataNotifications.isEmpty()) {
				osmDataNotifications = cache;
				osmDataNotifications.trim(ctx);
			}
		}
	}
	
}