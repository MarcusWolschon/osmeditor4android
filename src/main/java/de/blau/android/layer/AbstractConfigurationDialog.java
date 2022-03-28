package de.blau.android.layer;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.util.SizedFixedImmersiveDialogFragment;

/**
 * Configuration dialog that replicates the settings in the Preferences
 * 
 * @author Simon Poole
 *
 */
public abstract class AbstractConfigurationDialog extends SizedFixedImmersiveDialogFragment {

    private static final String DEBUG_TAG = AbstractConfigurationDialog.class.getSimpleName();

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
}
