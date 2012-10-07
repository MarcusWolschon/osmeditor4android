package de.blau.android.prefs;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

import de.blau.android.DialogFactory;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.prefs.AdvancedPrefDatabase.API;

public class AdvancedPrefEditor extends SherlockPreferenceActivity {
	
	private Resources r;
	private String KEY_PREFAPI;
	private String KEY_PREFPRESET;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.advancedpreferences);
		r = getResources();
		KEY_PREFAPI = r.getString(R.string.config_apibutton_key);
		KEY_PREFPRESET = r.getString(R.string.config_presetbutton_key);
		fixUpPrefs();
		ActionBar actionbar = getSupportActionBar();
		actionbar.setDisplayHomeAsUpEnabled(true);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		Preference apipref = getPreferenceScreen().findPreference(KEY_PREFAPI);
		AdvancedPrefDatabase db = new AdvancedPrefDatabase(this);
		API current = db.getCurrentAPI();
		if (current.id.equals(AdvancedPrefDatabase.ID_DEFAULT)) {
			apipref.setSummary(R.string.config_apibutton_summary);
		} else {
			apipref.setSummary(current.name.equals("") ? current.url : current.name);
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
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
				startActivity(new Intent(AdvancedPrefEditor.this, PresetEditorActivity.class));
				return true;
			}
		});
		
		Preference apipref = getPreferenceScreen().findPreference(KEY_PREFAPI);
		apipref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
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
