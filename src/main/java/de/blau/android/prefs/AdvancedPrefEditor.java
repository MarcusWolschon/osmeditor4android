package de.blau.android.prefs;

import de.blau.android.R;

/**
 * 
 * @author Simon Poole
 *
 */
public class AdvancedPrefEditor extends PrefEditorActivity {

    @Override
    ExtendedPreferenceFragment newEditorFragment() {
        return new AdvancedPrefEditorFragment();
    }

    @Override
    int getHelpTopic() {
        return R.string.help_advanced_preferences;
    }
}
