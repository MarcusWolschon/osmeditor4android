package de.blau.android.review;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.Serializable;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.R;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.ConfigurationChangeAwareActivity;

/**
 * 
 * Wrapper around the Review fragment so that we can run it in split screen mode
 * 
 * @author simon
 *
 */
public class ReviewActivity extends ConfigurationChangeAwareActivity {
    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, ReviewActivity.class.getSimpleName().length());
    private static final String DEBUG_TAG = ReviewActivity.class.getSimpleName().substring(0, TAG_LEN);

    /**
     * Start a new activity with the REview fragment as the contents
     * 
     * @param context the Android Context calling this
     */
    public static <V extends Serializable> void start(@NonNull Context context) { // NOSONAR
        Intent intent = new Intent(context, ReviewActivity.class);
        int flags = Intent.FLAG_ACTIVITY_NEW_TASK;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            flags = flags | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT;
        } else {
            flags = flags | Intent.FLAG_ACTIVITY_CLEAR_TASK;
        }
        intent.setFlags(flags);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(DEBUG_TAG, "onCreate");
        Preferences prefs = new Preferences(this);
        if (prefs.lightThemeEnabled()) {
            setTheme(R.style.Theme_customMain_Light);
        }
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            // No previous state to restore - get the state from the intent
            Log.d(DEBUG_TAG, "Initializing from intent");

        } else {
            Log.d(DEBUG_TAG, "Initializing from saved state");

        }
        String tag = Review.class.getName() + this.getClass().getName();
        Review reviewFragment = (Review) getSupportFragmentManager().findFragmentByTag(tag);
        if (reviewFragment == null) {
            reviewFragment = new Review();
            Bundle args = new Bundle();
            reviewFragment.setArguments(args);

            getSupportFragmentManager().beginTransaction().replace(android.R.id.content, reviewFragment, tag).commit();
        }
    }
}
