package io.vespucci.prefs;

import android.os.Bundle;
import androidx.annotation.Nullable;
import io.vespucci.R;

/**
 * 
 * @author Simon Poole
 *
 */
public class AdvancedPrefEditor extends PrefEditorActivity {

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
