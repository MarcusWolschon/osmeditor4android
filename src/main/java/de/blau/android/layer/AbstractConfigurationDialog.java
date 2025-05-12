package de.blau.android.layer;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.SizedFixedDialogFragment;

/**
 * Configuration dialog that replicates the settings in the Preferences
 * 
 * @author Simon Poole
 *
 */
public abstract class AbstractConfigurationDialog extends SizedFixedDialogFragment {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, AbstractConfigurationDialog.class.getSimpleName().length());
    private static final String DEBUG_TAG = AbstractConfigurationDialog.class.getSimpleName().substring(0, TAG_LEN);

    /**
     * Display a dialog allowing the user to change some properties of the current layer
     * 
     * @param activity the calling Activity
     * @param instance the dialog instance to show
     * @param tag the tag to use for the fragment manager
     */
    protected static void showDialog(@NonNull FragmentActivity activity, @NonNull AbstractConfigurationDialog instance, @NonNull String tag) {
        dismissDialog(activity, tag);
        try {
            instance.show(activity.getSupportFragmentManager(), tag);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * Dismiss the dialog
     * 
     * @param activity the calling Activity
     * @param tag the tag to use for the fragment manager
     */
    private static void dismissDialog(@NonNull FragmentActivity activity, @NonNull String tag) {
        de.blau.android.dialogs.Util.dismissDialog(activity, tag);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(true);
    }

    /**
     * Update the preference object everywhere
     * 
     * @param activity the calling Activity
     */
    protected void updatePrefs(@Nullable final FragmentActivity activity) {
        if (activity != null) {
            Preferences p = new Preferences(activity);
            if (activity instanceof Main) {
                ((Main) activity).updatePrefs(p);
            }
            App.getLogic().setPrefs(p);
            App.getLogic().getMap().setPrefs(getContext(), p);
            App.getLogic().getMap().invalidate();
        } else {
            Log.e(DEBUG_TAG, "null activity in updatePrefs");
        }
    }
}
