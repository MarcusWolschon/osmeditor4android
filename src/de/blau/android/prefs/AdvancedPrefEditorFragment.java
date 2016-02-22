package de.blau.android.prefs;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v4.preference.PreferenceFragmentCompat;
import android.util.Log;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.dialogs.DataLossActivity;
import de.blau.android.prefs.AdvancedPrefDatabase.API;

public class AdvancedPrefEditorFragment extends PreferenceFragmentCompat {

	private Resources r;
	private String KEY_PREFAPI;
	private String KEY_PREFPRESET;
	private String KEY_PREFLOGIN;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d("AdvancedPrefEditor", "onCreate");
		
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.advancedpreferences);
		r = getResources();
		KEY_PREFAPI = r.getString(R.string.config_api_button_key);
		KEY_PREFPRESET = r.getString(R.string.config_presetbutton_key);
		KEY_PREFLOGIN = r.getString(R.string.config_loginbutton_key);
		fixUpPrefs();	
	}
	
	@Override
	public void onResume() {
		Log.d("AdvancedPrefEditor", "onResume");
		super.onResume();
		Preference apipref = getPreferenceScreen().findPreference(KEY_PREFAPI);
		AdvancedPrefDatabase db = new AdvancedPrefDatabase(getActivity());
		API current = db.getCurrentAPI();
		if (current.id.equals(AdvancedPrefDatabase.ID_DEFAULT)) {
			apipref.setSummary(R.string.config_apibutton_summary);
		} else {
			apipref.setSummary(current.name.equals("") ? current.url : current.name);
		}
		Preference loginpref = getPreferenceScreen().findPreference(KEY_PREFLOGIN);
		// setSummary doesn't like being passed a NULL pointer .... disabled the code for now
		// loginpref.setSummary(current.id.equals(AdvancedPrefDatabase.ID_DEFAULT) ? R.string.config_username_summary : null);
		loginpref.setSummary(R.string.config_username_summary);
	}
	
	/** Perform initialization of the advanced preference buttons (API/Presets) */
	private void fixUpPrefs() {
		
		Preference presetpref = getPreferenceScreen().findPreference(KEY_PREFPRESET);
		presetpref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Log.d("AdvancedPrefEditor", "onPreferenceClick");
				PresetEditorActivity.start(getActivity());
				return true;
			}
		});
		
		Preference apipref = getPreferenceScreen().findPreference(KEY_PREFAPI);
		apipref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Log.d("AdvancedPrefEditor", "onPreferenceClick 2");
				Intent intent = new Intent(getActivity(), AdvancedPrefEditor.class);
				if (Main.hasChanges()) {
					DataLossActivity.showDialog(getActivity(), intent, -1);
				} else {
					startActivity(intent);
				}
				return true;
			}
		});
		
	}
}
