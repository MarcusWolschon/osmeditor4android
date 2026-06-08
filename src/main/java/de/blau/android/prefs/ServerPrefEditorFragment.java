package de.blau.android.prefs;

import android.os.Bundle;
import android.util.Log;
import androidx.preference.Preference;
import de.blau.android.R;

/**
 * Fragment for server preferences.
 */
public class ServerPrefEditorFragment extends ExtendedPreferenceFragment {

    private AdvancedPrefDatabase db;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Log.d(DEBUG_TAG, "onCreatePreferences " + rootKey);
        setPreferencesFromResource(R.xml.server_preferences, rootKey);
        db = new AdvancedPrefDatabase(requireActivity());

        Preference apiPref = findPreference(getString(R.string.config_api_button_key));
        if (apiPref != null) {
            apiPref.setOnPreferenceClickListener(preference -> {
                APIEditorActivity.start(requireActivity());
                return true;
            });
        }

        Preference geocoderPref = findPreference(getString(R.string.config_geocoder_button_key));
        if (geocoderPref != null) {
            geocoderPref.setOnPreferenceClickListener(preference -> {
                GeocoderEditorActivity.start(getActivity());
                return true;
            });
        }

        setEditTextPreferenceSummary(R.string.config_offsetServer_key, false);
        setEditTextPreferenceSummary(R.string.config_osmoseServer_key, false);
        setEditTextPreferenceSummary(R.string.config_taginfoServer_key, false);
        setEditTextPreferenceSummary(R.string.config_overpassServer_key, false);
        setEditTextPreferenceSummary(R.string.config_oamServer_key, false);
        
        setTitle();
    }

    @Override
    public void onResume() {
        super.onResume();
        setUpApiPrefs();
    }

    private void setUpApiPrefs() {
        Preference apiPref = findPreference(getString(R.string.config_api_button_key));
        if (apiPref != null) {
            API current = db.getCurrentAPI();
            if (current.id.equals(AdvancedPrefDatabase.ID_DEFAULT)) {
                apiPref.setSummary(R.string.config_apibutton_summary);
            } else {
                apiPref.setSummary("".equals(current.name) ? current.url : current.name);
            }
            Preference loginpref = findPreference(getString(R.string.config_loginbutton_key));
            if (loginpref != null) {
                loginpref.setSummary(current.user != null && !"".equals(current.user) ? current.user : getString(R.string.config_username_summary));
            }
        }
    }
}
