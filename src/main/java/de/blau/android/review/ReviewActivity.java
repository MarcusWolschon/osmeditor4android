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
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.ActivityUpdate;
import de.blau.android.util.AuthorisationEnabledActivity;

/**
 * 
 * Wrapper around the Review fragment so that we can run it in split screen mode
 * 
 * @author simon
 *
 */
public class ReviewActivity extends AuthorisationEnabledActivity implements ActivityUpdate {
    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, ReviewActivity.class.getSimpleName().length());
    private static final String DEBUG_TAG = ReviewActivity.class.getSimpleName().substring(0, TAG_LEN);

    private static final String FRAGMENT_TAG = Review.class.getName() + ReviewActivity.class.getName();

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

        Review reviewFragment = (Review) getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (reviewFragment == null) {
            reviewFragment = new Review();
            Bundle args = new Bundle();
            reviewFragment.setArguments(args);

            getSupportFragmentManager().beginTransaction().replace(android.R.id.content, reviewFragment, FRAGMENT_TAG).commit();
        }
    }

    @Override
    public void updateMenu() {
        Log.d(DEBUG_TAG, "updateMenu");
        Intent intent = new Intent(this, Main.class);
        intent.setAction(Main.ACTION_MENU_UPDATE);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        intent.setAction(Main.ACTION_MAP_UPDATE);
        startActivity(intent);
        Review reviewFragment = (Review) getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (reviewFragment != null) {
            reviewFragment.createChangesView();
            reviewFragment.notifyDataSetChanged();
        }
    }
}
