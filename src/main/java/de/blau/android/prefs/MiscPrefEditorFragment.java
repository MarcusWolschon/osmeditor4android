package de.blau.android.prefs;

import android.os.Bundle;
import android.util.Log;
import de.blau.android.R;

/**
 * Fragment for misc preferences.
 */
public class MiscPrefEditorFragment extends ExtendedPreferenceFragment {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Log.d(DEBUG_TAG, "onCreatePreferences " + rootKey);
        setPreferencesFromResource(R.xml.misc_preferences, rootKey);

        setRestartRequiredMessage(R.string.config_enableHwAcceleration_key);
        
        setTitle();
    }
}
