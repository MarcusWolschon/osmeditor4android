package de.blau.android.prefs;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

import de.blau.android.DialogFactory;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.prefs.AdvancedPrefDatabase.API;
import de.blau.android.util.IntentUtil;

public class AdvancedPrefEditor extends SherlockPreferenceActivity {
	
	private Resources r;
	private String KEY_PREFAPI;
	private String KEY_PREFPRESET;
	private String KEY_PREFLOGIN;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d("AdvancedPrefEditor", "onCreate");
		Preferences prefs = new Preferences(this);
		if (prefs.lightThemeEnabled()) {
			setTheme(R.style.Theme_Sherlock_Light);
		}
		
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.advancedpreferences);
		r = getResources();
		KEY_PREFAPI = r.getString(R.string.config_api_button_key);
		KEY_PREFPRESET = r.getString(R.string.config_presetbutton_key);
		KEY_PREFLOGIN = r.getString(R.string.config_loginbutton_key);
		fixUpPrefs();
		ActionBar actionbar = getSupportActionBar();
		actionbar.setDisplayHomeAsUpEnabled(true);
	}
	
	@Override
	public void onResume() {
		Log.d("AdvancedPrefEditor", "onResume");
		super.onResume();
		Preference apipref = getPreferenceScreen().findPreference(KEY_PREFAPI);
		AdvancedPrefDatabase db = new AdvancedPrefDatabase(this);
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
	
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		Log.d("AdvancedPrefEditor", "onOptionsItemSelected");
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/** Perform initialization of the advanced preference buttons (API/Presets) */
	private void fixUpPrefs() {
		
		Preference presetpref = getPreferenceScreen().findPreference(KEY_PREFPRESET);
		presetpref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Log.d("AdvancedPrefEditor", "onPreferenceClick");
				startActivity(IntentUtil.getPresetEditorActivityIntent(AdvancedPrefEditor.this));
				return true;
			}
		});
		
		Preference apipref = getPreferenceScreen().findPreference(KEY_PREFAPI);
		apipref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Log.d("AdvancedPrefEditor", "onPreferenceClick 2");
				Intent intent = new Intent(AdvancedPrefEditor.this, APIEditorActivity.class);
				if (Main.hasChanges()) {
					DialogFactory.createDataLossActivityDialog(AdvancedPrefEditor.this, intent, -1).show();
				} else {
					startActivity(intent);
				}
				return true;
			}
		});
		
	}
}
