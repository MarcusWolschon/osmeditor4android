package de.blau.android.prefs;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.support.annotation.NonNull;
import android.support.v4.preference.PreferenceFragmentCompat;
import android.util.Log;
import de.blau.android.Application;
import de.blau.android.DebugInformation;
import de.blau.android.LicenseViewer;
import de.blau.android.R;
import de.blau.android.prefs.AdvancedPrefDatabase.API;
import de.blau.android.resources.Profile;
import de.blau.android.views.util.OpenStreetMapTileServer;

/**
 * Simple class for Android's standard-Preference Activity
 * 
 * @author mb
 */
public class PrefEditorFragment extends PreferenceFragmentCompat {
	
	private Resources r;
	private String KEY_MAPBG;
	private String KEY_MAPOL;
	private String KEY_MAPPROFILE;
	private String KEY_PREFICONS;
	private String KEY_ADVPREFS;
	private String KEY_ADDRPREFS;
	private String KEY_LICENSE;
	private String KEY_DEBUG;

	public static void start(@NonNull Context context) {
		Intent intent = new Intent(context, PrefEditorFragment.class);
		context.startActivity(intent);
	}
	
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
		r = getResources();
		KEY_MAPBG = r.getString(R.string.config_backgroundLayer_key);
		KEY_MAPOL = r.getString(R.string.config_overlayLayer_key);
		KEY_MAPPROFILE = r.getString(R.string.config_mapProfile_key);
		KEY_PREFICONS = r.getString(R.string.config_iconbutton_key);
		KEY_ADVPREFS = r.getString(R.string.config_advancedprefs_key);
		KEY_ADDRPREFS = r.getString(R.string.config_addressprefs_key);
		KEY_LICENSE = r.getString(R.string.config_licensebutton_key);
		KEY_DEBUG = r.getString(R.string.config_debugbutton_key);
		fixUpPrefs();
	}
	
	@Override
	public void onResume() {
		Log.d("PrefEditor", "onResume");
		super.onResume();
		
		CheckBoxPreference iconspref = (CheckBoxPreference) getPreferenceScreen().findPreference(KEY_PREFICONS);
		AdvancedPrefDatabase db = new AdvancedPrefDatabase(getActivity());
		API current = db.getCurrentAPI();

		iconspref.setChecked(current.showicon);
		Log.d("PrefEditor", "onResume done");
	}
	
	/** Perform initialization of the advanced preference buttons (API/Presets) */
	private void fixUpPrefs() {
		Preferences prefs = new Preferences(getActivity());
		
		// remove any problematic imagery URLs
		OpenStreetMapTileServer.applyBlacklist(prefs.getServer().getCachedCapabilities().imageryBlacklist);
		
		ListPreference mapbgpref = (ListPreference) getPreferenceScreen().findPreference(KEY_MAPBG);
		String[] ids = OpenStreetMapTileServer.getIds(true);
		mapbgpref.setEntries(OpenStreetMapTileServer.getNames(ids));
		mapbgpref.setEntryValues(ids);
		OnPreferenceChangeListener l = new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Log.d("PrefEditor", "onPreferenceChange");
				String id = (String)newValue;
				String[] ids = OpenStreetMapTileServer.getIds(false); // r.getStringArray(R.array.renderer_ids);
				String[] names = OpenStreetMapTileServer.getNames(ids); // r.getStringArray(R.array.renderer_names);
				for (int i = 0; i < ids.length; i++) {
					if (ids[i].equals(id)) {
						preference.setSummary(names[i]);
						break;
					}
				}
				Application.getDelegator().setImageryRecorded(false);
				return true;
			}
		};
		mapbgpref.setOnPreferenceChangeListener(l);
		l.onPreferenceChange(mapbgpref, prefs.backgroundLayer());
		
		ListPreference mapolpref = (ListPreference) getPreferenceScreen().findPreference(KEY_MAPOL);
		String[] overlayIds = OpenStreetMapTileServer.getOverlayIds(true);
		mapolpref.setEntries(OpenStreetMapTileServer.getOverlayNames(overlayIds));
		mapolpref.setEntryValues(overlayIds);
		OnPreferenceChangeListener ol = new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Log.d("PrefEditor", "onPreferenceChange");
				String id = (String)newValue;
				String[] ids = OpenStreetMapTileServer.getOverlayIds(false); // r.getStringArray(R.array.renderer_ids);
				String[] names = OpenStreetMapTileServer.getOverlayNames(ids); // r.getStringArray(R.array.renderer_names);
				for (int i = 0; i < ids.length; i++) {
					if (ids[i].equals(id)) {
						preference.setSummary(names[i]);
						break;
					}
				}
				Application.getDelegator().setImageryRecorded(false);
				return true;
			}
		};
		mapolpref.setOnPreferenceChangeListener(ol);
		ol.onPreferenceChange(mapolpref, prefs.overlayLayer());
		
		ListPreference mapProfilePref = (ListPreference) getPreferenceScreen().findPreference(KEY_MAPPROFILE);
		String[] profileList = Profile.getProfileList(getActivity());
		mapProfilePref.setEntries(profileList);
		mapProfilePref.setEntryValues(profileList);
		OnPreferenceChangeListener p = new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Log.d("PrefEditor", "onPreferenceChange mapProfile");
				String id = (String)newValue;
				String[] profileList = Profile.getProfileList(getActivity());
				String[] ids = profileList;
				String[] names = profileList;
				for (int i = 0; i < ids.length; i++) {
					if (ids[i].equals(id)) {
						preference.setSummary(names[i]);
						break;
					}
				}
				return true;
			}
		};
		mapProfilePref.setOnPreferenceChangeListener(p);
		p.onPreferenceChange(mapProfilePref, prefs.getMapProfile());
		
		Preference advprefs = getPreferenceScreen().findPreference(KEY_ADVPREFS);
		advprefs.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				Log.d("PrefEditor", "onPreferenceClick");
				startActivity(new Intent(getActivity(), AdvancedPrefEditor.class));
				return true;
			}
		});
		
		CheckBoxPreference iconspref = (CheckBoxPreference) getPreferenceScreen().findPreference(KEY_PREFICONS);
		iconspref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Log.d("PrefEditor", "onPreferenceChange 2");
				AdvancedPrefDatabase db = new AdvancedPrefDatabase(getActivity());
				db.setCurrentAPIShowIcons((Boolean)newValue);
				return true;
			}
		});
		
		Preference licensepref = getPreferenceScreen().findPreference(KEY_LICENSE);
		licensepref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Log.d("PrefEditor", "onPreferenceClick 2");
				startActivity(new Intent(getActivity(), LicenseViewer.class));
				return true;
			}
		});
		
		Preference debugpref = getPreferenceScreen().findPreference(KEY_DEBUG);
		debugpref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Log.d("PrefEditor", "onPreferenceClick 2");
				startActivity(new Intent(getActivity(), DebugInformation.class));
				return true;
			}
		});
	}
	
}
