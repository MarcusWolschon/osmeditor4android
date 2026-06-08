package de.blau.android.prefs;

import android.os.Bundle;
import android.util.Log;
import de.blau.android.R;

/**
 * Fragment for editing preferences.
 */
public class EditingPrefEditorFragment extends ExtendedPreferenceFragment {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Log.d(DEBUG_TAG, "onCreatePreferences " + rootKey);
        setPreferencesFromResource(R.xml.editing_preferences, rootKey);
        setTitle();
    }
}
