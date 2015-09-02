package de.blau.android;

import java.util.Map;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import de.blau.android.names.Names;
import de.blau.android.names.Names.NameAndTags;
import de.blau.android.osb.BugStorage;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.util.MultiHashMap;

@ReportsCrashes(
	formKey = "",
	reportType = org.acra.sender.HttpSender.Type.JSON,
	httpMethod = org.acra.sender.HttpSender.Method.PUT,
	formUri = "http://acralyzer.vespucci.io/acraproxy",
	mode = ReportingInteractionMode.NOTIFICATION,
	resNotifTickerText = R.string.crash_notif_ticker_text,
	resNotifTitle = R.string.crash_notif_title,
	resNotifText = R.string.crash_notif_text,
	resDialogText = R.string.crash_dialog_text)
public class Application extends android.app.Application {
	public static Main mainActivity;
	static StorageDelegator delegator = new StorageDelegator();
	static BugStorage bugStorage = new BugStorage();
	public static String userAgent;
	static Application currentApplication;
	/**
	 * The currently selected presets
	 */
	private static Preset[] currentPresets;
	private static MultiHashMap<String, PresetItem> presetSearchIndex = null;
	/**
	 * name index related stuff
	 */
	private static Names names = null;
	private static Map<String,NameAndTags> namesSearchIndex = null;
	
	
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
	
	public static BugStorage getBugStorage() {
		return bugStorage;
	}

	public static synchronized Preset[] getCurrentPresets(Context ctx) {
		if (currentPresets == null) {
			Preferences prefs = new Preferences(ctx);
			currentPresets = prefs.getPreset();
		}
		return currentPresets;
	}
	
	/**
	 * Resets the current presets, causing them to be re-parsed
	 */
	public static synchronized void resetPresets() {
		currentPresets = null; 
		presetSearchIndex = null;
	}
	
	public static synchronized MultiHashMap<String, PresetItem> getPresetSearchIndex(Context ctx) {
		if (presetSearchIndex == null) {
			presetSearchIndex = Preset.getSearchIndex(getCurrentPresets(ctx));
		}
		return presetSearchIndex;
	}
	
	public static synchronized Map<String,NameAndTags> getNameSearchIndex(Context ctx) {
		getNames(ctx);
		if (namesSearchIndex == null) {
			// names.dump2Log();
			namesSearchIndex = names.getSearchIndex();
		}
		return namesSearchIndex;
	}

	public static synchronized Names getNames(Context ctx) {
		if (names == null) {
			// this should be done async if it takes too long
			names = new Names(ctx);
		}
		return names;
	}
	
}