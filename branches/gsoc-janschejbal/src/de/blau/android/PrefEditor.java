package de.blau.android;

import de.blau.android.prefs.ListEditActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Toast;

/**
 * Simple class for Android's standard-Preference Activity
 * 
 * @author mb
 */
public class PrefEditor extends PreferenceActivity {
	
	private Resources r;
	private String KEY_PREFCAT_SERVER;
	private String KEY_PREFAPI;
	
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
		r = getResources();
		KEY_PREFCAT_SERVER = r.getString(R.string.config_category_server_key);
		KEY_PREFAPI = r.getString(R.string.config_apiurl_key);
		fixUpPrefs();
	}
	
	private void fixUpPrefs() {
		Preference apipref = getPreferenceScreen().findPreference(KEY_PREFAPI);
		apipref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent intent = new Intent(PrefEditor.this, ListEditActivity.class);
				PrefEditor.this.startActivity(intent);
				return true;
			}
		});
	}


}
