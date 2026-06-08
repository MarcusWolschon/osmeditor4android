package de.blau.android.prefs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import android.os.Bundle;
import android.util.Log;
import de.blau.android.R;

public class AdvancedPrefEditorFragment extends ExtendedPreferenceFragment {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, AdvancedPrefEditorFragment.class.getSimpleName().length());
    private static final String DEBUG_TAG = AdvancedPrefEditorFragment.class.getSimpleName().substring(0, TAG_LEN);

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Log.d(DEBUG_TAG, "onCreatePreferences " + rootKey);
        setPreferencesFromResource(R.xml.advancedpreferences, rootKey);
        setTitle();
    }
}
