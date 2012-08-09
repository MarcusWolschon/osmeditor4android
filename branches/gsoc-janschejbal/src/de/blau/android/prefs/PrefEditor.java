package de.blau.android.prefs;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import de.blau.android.DialogFactory;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.prefs.AdvancedPrefDatabase.API;

/**
 * Simple class for Android's standard-Preference Activity
 * 
 * @author mb
 */
public class PrefEditor extends PreferenceActivity {
	
	private Resources r;
	private String KEY_PREFAPI;
	private String KEY_PREFLOGIN;
	private String KEY_PREFPRESET;
	private String KEY_PREFICONS;
	
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
		r = getResources();
		KEY_PREFAPI = r.getString(R.string.config_apibutton_key);
		KEY_PREFLOGIN = r.getString(R.string.config_loginbutton_key);
		KEY_PREFPRESET = r.getString(R.string.config_presetbutton_key);
		KEY_PREFICONS = r.getString(R.string.config_iconbutton_key);
		fixUpPrefs();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	
		Preference apipref = getPreferenceScreen().findPreference(KEY_PREFAPI);
		Preference loginpref = getPreferenceScreen().findPreference(KEY_PREFLOGIN);
		CheckBoxPreference iconspref = (CheckBoxPreference) getPreferenceScreen().findPreference(KEY_PREFICONS);
		AdvancedPrefDatabase db = new AdvancedPrefDatabase(this);
		API current = db.getCurrentAPI();
		if (current.id.equals(AdvancedPrefDatabase.ID_DEFAULT)) {
			apipref.setSummary(R.string.config_apibutton_summary);
			loginpref.setSummary(R.string.config_username_summary);
		} else {
			apipref.setSummary(current.name.equals("") ? current.url : current.name);
			loginpref.setSummary(null);
		}
		iconspref.setChecked(current.showicon);
	}
	
	/** Perform initialization of the advanced preference buttons (API/Presets) */
	private void fixUpPrefs() {
		Preference apipref = getPreferenceScreen().findPreference(KEY_PREFAPI);
		apipref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent intent = new Intent(PrefEditor.this, APIEditorActivity.class);
				if (Main.hasChanges()) {
					DialogFactory.createDataLossActivityDialog(PrefEditor.this, intent, -1).show();
				} else {
					startActivity(intent);
				}
				return true;
			}
		});
		
		Preference presetpref = getPreferenceScreen().findPreference(KEY_PREFPRESET);
		presetpref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				startActivity(new Intent(PrefEditor.this, PresetEditorActivity.class));
				return true;
			}
		});
		
		CheckBoxPreference iconspref = (CheckBoxPreference) getPreferenceScreen().findPreference(KEY_PREFICONS);
		iconspref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				AdvancedPrefDatabase db = new AdvancedPrefDatabase(PrefEditor.this);
				db.setCurrentAPIShowIcons((Boolean)newValue);
				return true;
			}
		});
	}
	


}
