package de.blau.android.prefs;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import de.blau.android.R;
import de.blau.android.osm.BoundingBox;

/**
 * Simple class for Android's standard-Preference Activity
 * 
 * @author mb
 */
public class PrefEditor extends PrefEditorActivity {

    private static final String DEBUG_TAG = PrefEditor.class.getSimpleName();

    static final String CURRENT_VIEWBOX = "VIEWBOX";
    private BoundingBox viewBox         = null;

    /**
     * Start the PrefEditor activity
     * 
     * @param context Android Context
     * @param viewBox the current ViewBox
     */
    public static void start(@NonNull Context context, BoundingBox viewBox) {
        Intent intent = new Intent(context, PrefEditor.class);
        intent.putExtra(CURRENT_VIEWBOX, viewBox);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {

        Log.d(DEBUG_TAG, "onCreate");
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Log.d(DEBUG_TAG, "initializing from intent");
            // No previous state to restore - get the state from the intent
            viewBox = (BoundingBox) getIntent().getSerializableExtra(CURRENT_VIEWBOX);
        } else {
            Log.d(DEBUG_TAG, "initializing from saved state");
            // Restore activity from saved state
            viewBox = (BoundingBox) savedInstanceState.getSerializable(CURRENT_VIEWBOX);
        }

        ExtendedPreferenceFragment f = newEditorFragment();

        Bundle args = new Bundle();
        args.putSerializable(CURRENT_VIEWBOX, viewBox);
        f.setArguments(args);

        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, f).commit();
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        Log.d(DEBUG_TAG, "onSaveInstaceState");
        super.onSaveInstanceState(outState);
        outState.putSerializable(CURRENT_VIEWBOX, viewBox);
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
