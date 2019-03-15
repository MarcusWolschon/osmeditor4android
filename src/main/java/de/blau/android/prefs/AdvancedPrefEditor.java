package de.blau.android.prefs;

import android.os.Bundle;
import android.support.annotation.Nullable;
import de.blau.android.R;

/**
 * 
 * @author Simon Poole
 *
 */
public class AdvancedPrefEditor extends PrefEditorActivity {

    private static final String DEBUG_TAG = "AdvancedPrefEditor";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, newEditorFragment()).commit();
    }

    @Override
    ExtendedPreferenceFragment newEditorFragment() {
        return new AdvancedPrefEditorFragment();
    }

    @Override
    int getHelpTopic() {
        return R.string.help_advanced_preferences;
    }
}
