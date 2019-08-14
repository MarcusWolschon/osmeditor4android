package de.blau.android.prefs;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import de.blau.android.R;

/**
 * Simple class for Android's standard-Preference Activity
 * 
 * @author mb
 */
public class PrefEditor extends PrefEditorActivity {

    private static final String DEBUG_TAG = PrefEditor.class.getSimpleName();

    /**
     * Start the PrefEditor activity
     * 
     * @param context Android Context
     */
    public static void start(@NonNull Context context) {
        Intent intent = new Intent(context, PrefEditor.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        Log.d(DEBUG_TAG, "onCreate");
        super.onCreate(savedInstanceState);

        ExtendedPreferenceFragment f = newEditorFragment();

        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, f).commit();
    }

    @Override
    ExtendedPreferenceFragment newEditorFragment() {
        return new PrefEditorFragment();
    }

    @Override
    int getHelpTopic() {
        return R.string.help_preferences;
    }
}
