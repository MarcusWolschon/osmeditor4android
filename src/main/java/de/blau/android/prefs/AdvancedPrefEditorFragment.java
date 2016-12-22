package de.blau.android.prefs;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.dialogs.DataLossActivity;
import de.blau.android.prefs.AdvancedPrefDatabase.API;

public class AdvancedPrefEditorFragment extends PreferenceFragmentCompat {

	private Resources r;
	private String KEY_PREFAPI;
	private String KEY_PREFPRESET;
	private String KEY_PREFFULLSCREEN;
	private String KEY_PREFLOGIN;
	private String KEY_PREFGEOCODER;
	
	@Override
	public void onCreatePreferences(Bundle arg0, String arg1) {
		addPreferencesFromResource(R.xml.advancedpreferences);
		r = getResources();
		KEY_PREFAPI = r.getString(R.string.config_api_button_key);
		KEY_PREFPRESET = r.getString(R.string.config_presetbutton_key);
		KEY_PREFFULLSCREEN = r.getString(R.string.config_fullscreenMode_key);
		KEY_PREFLOGIN = r.getString(R.string.config_loginbutton_key);
		KEY_PREFGEOCODER = r.getString(R.string.config_geocoder_button_key);
		fixUpPrefs();		
	}
	
	@Override
	public void onResume() {
		Log.d("AdvancedPrefEditor", "onResume");
		super.onResume();
		// final Preferences prefs = new Preferences(getActivity());
		Preference apipref = getPreferenceScreen().findPreference(KEY_PREFAPI);
		AdvancedPrefDatabase db = new AdvancedPrefDatabase(getActivity());
		API current = db.getCurrentAPI();
		if (current.id.equals(AdvancedPrefDatabase.ID_DEFAULT)) {
			apipref.setSummary(R.string.config_apibutton_summary);
		} else {
			apipref.setSummary(current.name.equals("") ? current.url : current.name);
		}
		Preference loginpref = getPreferenceScreen().findPreference(KEY_PREFLOGIN);
		loginpref.setSummary(current.user != null && !"".equals(current.user)?current.user:r.getString(R.string.config_username_summary));
	}
	
	/** Perform initialization of the advanced preference buttons (API/Presets) */
	private void fixUpPrefs() {
		
		Preference presetPref = getPreferenceScreen().findPreference(KEY_PREFPRESET);
		presetPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Log.d("AdvancedPrefEditor", "onPreferenceClick");
				PresetEditorActivity.start(getActivity());
				return true;
			}
		});
		
		Preference apiPref = getPreferenceScreen().findPreference(KEY_PREFAPI);
		apiPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Log.d("AdvancedPrefEditor", "onPreferenceClick 2");
				Intent intent = new Intent(getActivity(), APIEditorActivity.class);
				if (Main.hasChanges()) {
					DataLossActivity.showDialog(getActivity(), intent, -1);
				} else {
					startActivity(intent);
				}
				return true;
			}
		});
		
		Preference geocoderPref = getPreferenceScreen().findPreference(KEY_PREFGEOCODER);
		geocoderPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Log.d("AdvancedPrefEditor", "onPreferenceClick");
				GeocoderEditorActivity.start(getActivity());
				return true;
			}
		});
	}
	
	@Override
	public void onDisplayPreferenceDialog(Preference preference) {
	    DialogFragment fragment;
	    if (preference instanceof LoginDataPreference) {
	        fragment = LoginDataPreferenceFragment.newInstance(preference);
	        fragment.setTargetFragment(this, 0);
	        fragment.show(getFragmentManager(),
	                "android.support.v7.preference.PreferenceFragment.LOGINDATA");
	    } else if (preference instanceof MultiSelectListPreference) {
	        fragment = MultiSelectListPreferenceDialogFragment.newInstance(preference.getKey());
	        fragment.setTargetFragment(this, 0);
	        fragment.show(getFragmentManager(),
	                "android.support.v7.preference.PreferenceFragment.MULTISELECTLIST");
	    } else super.onDisplayPreferenceDialog(preference);
	}
}
