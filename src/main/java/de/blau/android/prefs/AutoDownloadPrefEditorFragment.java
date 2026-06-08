package de.blau.android.prefs;

import android.os.Bundle;
import android.util.Log;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import de.blau.android.R;

/**
 * Fragment for auto download preferences.
 */
public class AutoDownloadPrefEditorFragment extends ExtendedPreferenceFragment {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Log.d(DEBUG_TAG, "onCreatePreferences " + rootKey);
        setPreferencesFromResource(R.xml.auto_download_preferences, rootKey);

        CheckBoxPreference autoPrunePref = findPreference(getString(R.string.config_autoPrune_key));
        if (autoPrunePref != null) {
            OnPreferenceChangeListener listener = (preference, newValue) -> {
                boolean autoPruneEnabled = (Boolean) newValue;
                enablePreference(R.string.config_autoPruneBoundingBoxLimit_key, autoPruneEnabled);
                enablePreference(R.string.config_autoPruneNodeLimit_key, autoPruneEnabled);
                enablePreference(R.string.config_autoPruneTaskLimit_key, autoPruneEnabled);
                return true;
            };
            listener.onPreferenceChange(autoPrunePref, autoPrunePref.isChecked());
            autoPrunePref.setOnPreferenceChangeListener(listener);
        }
        
        setTitle();
    }

    private void enablePreference(int keyResource, boolean enabled) {
        Preference pref = findPreference(getString(keyResource));
        if (pref != null) {
            pref.setEnabled(enabled);
        }
    }
}
