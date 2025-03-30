package de.blau.android.prefs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import de.blau.android.R;

/**
 * Simple class for Android's standard-Preference Activity
 * 
 * @author mb
 */
public class PrefEditor extends PrefEditorActivity {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, PrefEditor.class.getSimpleName().length());
    private static final String DEBUG_TAG = PrefEditor.class.getSimpleName().substring(0, TAG_LEN);

    /**
     * Start the PrefEditor activity
     * 
     * @param context Android Context
     */
    public static void start(@NonNull Context context) {
        Intent intent = new Intent(context, PrefEditor.class);
        context.startActivity(intent);
    }

    /**
     * Start the PrefEditor activity and wait for a result
     * 
     * @param activity an Activity
     * @param requestCode an int value to identify the request
     */
    public static void start(@NonNull Activity activity, int requestCode) {
        Intent intent = new Intent(activity, PrefEditor.class);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * Start the PrefEditor activity and wait for a result
     * 
     * @param fragment the calling fragment
     * @param requestCode an int value to identify the request
     */
    public static void start(@NonNull Fragment fragment, int requestCode) {
        Intent intent = new Intent(fragment.getContext(), PrefEditor.class);
        fragment.startActivityForResult(intent, requestCode);
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
