package de.blau.android.prefs;

import android.os.Bundle;
import android.util.Log;
import de.blau.android.R;

/**
 * Fragment for layers preferences.
 */
public class LayersPrefEditorFragment extends ExtendedPreferenceFragment {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Log.d(DEBUG_TAG, "onCreatePreferences " + rootKey);
        setPreferencesFromResource(R.xml.layers_preferences, rootKey);

        setRestartRequiredMessage(R.string.config_preferRemovableStorage_key);
        setRestartRequiredMessage(R.string.config_mapillary_min_zoom_key);
        
        setTitle();
    }
}
