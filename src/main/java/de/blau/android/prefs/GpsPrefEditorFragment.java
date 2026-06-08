package de.blau.android.prefs;

import android.os.Bundle;
import android.util.Log;
import de.blau.android.R;

/**
 * Fragment for GPS preferences.
 */
public class GpsPrefEditorFragment extends ExtendedPreferenceFragment {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Log.d(DEBUG_TAG, "onCreatePreferences " + rootKey);
        setPreferencesFromResource(R.xml.gps_preferences, rootKey);

        setListPreferenceSummary(R.string.config_gps_source_key, false);
        setEditTextPreferenceSummary(R.string.config_gps_source_tcp_key, false);
        
        setTitle();
    }
}
