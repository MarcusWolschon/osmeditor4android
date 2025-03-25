package io.vespucci.layer;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import io.vespucci.App;
import io.vespucci.Main;
import io.vespucci.prefs.Preferences;
import io.vespucci.util.SizedFixedImmersiveDialogFragment;

/**
 * Configuration dialog that replicates the settings in the Preferences
 * 
 * @author Simon Poole
 *
 */
public abstract class AbstractConfigurationDialog extends SizedFixedImmersiveDialogFragment {

    private static final String DEBUG_TAG = AbstractConfigurationDialog.class.getSimpleName().substring(0, Math.min(23, AbstractConfigurationDialog.class.getSimpleName().length()));

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
        io.vespucci.dialogs.Util.dismissDialog(activity, tag);
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
