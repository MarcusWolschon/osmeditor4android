package de.blau.android.prefs;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import androidx.preference.Preference;
import de.blau.android.R;

public class AdvancedPrefEditorFragment extends ExtendedPreferenceFragment {

    private static final String DEBUG_TAG = "AdvancedPrefEditor";

    private Resources    r;
    AdvancedPrefDatabase db;
    private String       KEY_PREFAPI;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Log.d(DEBUG_TAG, "onCreatePreferences " + rootKey);
        setPreferencesFromResource(R.xml.advancedpreferences, rootKey);
        r = getResources();
        KEY_PREFAPI = r.getString(R.string.config_api_button_key);
        setOnPreferenceClickListeners();
        setTitle();
        db = new AdvancedPrefDatabase(getActivity());
    }

    @Override
    public void onResume() {
        Log.d(DEBUG_TAG, "onResume");
        super.onResume();
        Preference apipref = getPreferenceScreen().findPreference(KEY_PREFAPI);
        if (apipref != null) {
            API current = db.getCurrentAPI();
            if (current.id.equals(AdvancedPrefDatabase.ID_DEFAULT)) {
                apipref.setSummary(R.string.config_apibutton_summary);
            } else {
                apipref.setSummary("".equals(current.name) ? current.url : current.name);
            }
            Preference loginpref = getPreferenceScreen().findPreference(r.getString(R.string.config_loginbutton_key));
            if (loginpref != null) {
                loginpref.setSummary(current.user != null && !"".equals(current.user) ? current.user : r.getString(R.string.config_username_summary));
            }
        }
        setListPreferenceSummary(R.string.config_fullscreenMode_key, true);
        setListPreferenceSummary(R.string.config_mapOrientation_key, false);
        setListPreferenceSummary(R.string.config_gps_source_key, false);
        setEditTextPreferenceSummary(R.string.config_offsetServer_key, false);
        setEditTextPreferenceSummary(R.string.config_osmoseServer_key, false);
        setRestartRequiredMessage(R.string.config_enableLightTheme_key);
        setRestartRequiredMessage(R.string.config_splitActionBarEnabled_key);
        setListPreferenceSummary(R.string.config_followGPSbutton_key, true);
        setTitle();
    }

    /**
     * Set listeners on special Preference entries
     * 
     * If we are just showing a sub-PreferenceScreen some of the keys may not be accessible
     */
    private void setOnPreferenceClickListeners() {
        Preference apiPref = getPreferenceScreen().findPreference(KEY_PREFAPI);
        if (apiPref != null) {
            apiPref.setOnPreferenceClickListener(preference -> {
                Log.d(DEBUG_TAG, "onPreferenceClick 2");
                APIEditorActivity.start(getActivity());
                return true;
            });
        }

        Preference geocoderPref = getPreferenceScreen().findPreference(r.getString(R.string.config_geocoder_button_key));
        if (geocoderPref != null) {
            geocoderPref.setOnPreferenceClickListener(preference -> {
                Log.d(DEBUG_TAG, "onPreferenceClick");
                GeocoderEditorActivity.start(getActivity());
                return true;
            });
        }
    }
}
