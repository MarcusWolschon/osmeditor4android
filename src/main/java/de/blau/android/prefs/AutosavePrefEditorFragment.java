package de.blau.android.prefs;

import android.os.Bundle;
import android.util.Log;
import de.blau.android.R;

/**
 * Fragment for autosave preferences.
 */
public class AutosavePrefEditorFragment extends ExtendedPreferenceFragment {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Log.d(DEBUG_TAG, "onCreatePreferences " + rootKey);
        setPreferencesFromResource(R.xml.autosave_preferences, rootKey);

        setRestartRequiredMessage(R.string.config_autosaveSaveState_key);
        setRestartRequiredMessage(R.string.config_autosaveSaveChanges_key);
        setRestartRequiredMessage(R.string.config_autosaveInterval_key);
        setRestartRequiredMessage(R.string.config_autosaveChanges_key);
        setRestartRequiredMessage(R.string.config_autosaveMaxFiles_key);
        
        setTitle();
    }
}
