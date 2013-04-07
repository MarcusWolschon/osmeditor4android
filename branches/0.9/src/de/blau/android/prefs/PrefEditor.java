package de.blau.android.prefs;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;
import de.blau.android.LicenseViewer;
import de.blau.android.R;
import de.blau.android.prefs.AdvancedPrefDatabase.API;

/**
 * Simple class for Android's standard-Preference Activity
 * 
 * @author mb
 */
public class PrefEditor extends SherlockPreferenceActivity {
	
	private Resources r;
	private String KEY_MAPBG;
	private String KEY_PREFLOGIN;
	private String KEY_PREFICONS;
	private String KEY_ADVPREFS;
	private String KEY_LICENSE;
	
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		Log.d("PrefEditor", "onCreate");
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
		r = getResources();
		KEY_MAPBG = r.getString(R.string.config_backgroundLayer_key);
		KEY_PREFLOGIN = r.getString(R.string.config_loginbutton_key);
		KEY_PREFICONS = r.getString(R.string.config_iconbutton_key);
		KEY_ADVPREFS = r.getString(R.string.config_advancedprefs_key);
		KEY_LICENSE = r.getString(R.string.config_licensebutton_key);
		fixUpPrefs();
		
		ActionBar actionbar = getSupportActionBar();
		actionbar.setDisplayHomeAsUpEnabled(true);
	}
	
	@Override
	protected void onResume() {
		Log.d("PrefEditor", "onResume");
		super.onResume();
		
		Preference loginpref = getPreferenceScreen().findPreference(KEY_PREFLOGIN);
		CheckBoxPreference iconspref = (CheckBoxPreference) getPreferenceScreen().findPreference(KEY_PREFICONS);
		AdvancedPrefDatabase db = new AdvancedPrefDatabase(this);
		API current = db.getCurrentAPI();
		// setSummary doesn't like being passed a NULL pointer .... disabled the code for now
		// loginpref.setSummary(current.id.equals(AdvancedPrefDatabase.ID_DEFAULT) ? R.string.config_username_summary : null);
		loginpref.setSummary(R.string.config_username_summary);
		iconspref.setChecked(current.showicon);
		Log.d("PrefEditor", "onResume done");
	}
	
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		Log.d("PrefEditor", "onOptionsItemSelected");
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/** Perform initialization of the advanced preference buttons (API/Presets) */
	private void fixUpPrefs() {
		Preferences prefs = new Preferences(this);
		
		Preference mapbgpref = getPreferenceScreen().findPreference(KEY_MAPBG);
		OnPreferenceChangeListener l = new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Log.d("PrefEditor", "onPreferenceChange");
				String id = (String)newValue;
				String[] ids = r.getStringArray(R.array.renderer_ids);
				String[] names = r.getStringArray(R.array.renderer_names);
				for (int i = 0; i < ids.length; i++) {
					if (ids[i].equals(id)) {
						preference.setSummary(names[i]);
						break;
					}
				}
				return true;
			}
		};
		mapbgpref.setOnPreferenceChangeListener(l);
		l.onPreferenceChange(mapbgpref, prefs.backgroundLayer());
		
		Preference advprefs = getPreferenceScreen().findPreference(KEY_ADVPREFS);
		advprefs.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				Log.d("PrefEditor", "onPreferenceClick");
				startActivity(new Intent(PrefEditor.this, AdvancedPrefEditor.class));
				return true;
			}
		});
		
		CheckBoxPreference iconspref = (CheckBoxPreference) getPreferenceScreen().findPreference(KEY_PREFICONS);
		iconspref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Log.d("PrefEditor", "onPreferenceChange 2");
				AdvancedPrefDatabase db = new AdvancedPrefDatabase(PrefEditor.this);
				db.setCurrentAPIShowIcons((Boolean)newValue);
				return true;
			}
		});
		
		Preference licensepref = getPreferenceScreen().findPreference(KEY_LICENSE);
		licensepref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Log.d("PrefEditor", "onPreferenceClick 2");
				startActivity(new Intent(PrefEditor.this, LicenseViewer.class));
				return true;
			}
		});
		
	}
	
}
