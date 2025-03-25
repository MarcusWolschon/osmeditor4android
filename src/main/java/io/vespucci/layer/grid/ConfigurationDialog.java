package io.vespucci.layer.grid;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import io.vespucci.R;
import io.vespucci.layer.AbstractConfigurationDialog;

/**
 * Configuration dialog that replicates the settings in the Preferences
 * 
 * @author Simon Poole
 *
 */
public class ConfigurationDialog extends AbstractConfigurationDialog {

    private static final String TAG       = "gridConfigurationDialog";

    /**
     * Display a dialog allowing the user to change some properties of the current layer
     * 
     * @param activity the calling Activity
     */
    public static void showDialog(@NonNull FragmentActivity activity) {
        showDialog(activity, newInstance(), TAG);
    }

    /**
     * Get a new ConfigurationDialog dialog instance
     * 
     * @return a new ConfigurationDialog dialog instance
     */
    @NonNull
    private static ConfigurationDialog newInstance() {
        ConfigurationDialog f = new ConfigurationDialog();
        f.setShowsDialog(true);
        return f;
    }

    @NonNull
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        final FragmentActivity activity = getActivity();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        Resources r = activity.getResources();
        final String prefKey = r.getString(R.string.config_scale_key);
        String[] prefValues = r.getStringArray(R.array.scale_values);
        String currentGrid = prefs.getString(prefKey, prefValues[1]);
        final int[] selected = new int[1]; // hack around that this must be final
        for (int i = 0; i < prefValues.length; i++) {
            if (currentGrid.equals(prefValues[i])) {
                selected[0] = i;
                break;
            }
        }

        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.config_scale_title);

        builder.setSingleChoiceItems(R.array.scale_entries, selected[0], (dialog, which) -> selected[0] = which);

        builder.setPositiveButton(R.string.okay, (dialog, which) -> {
            prefs.edit().putString(prefKey, prefValues[selected[0]]).commit();
            updatePrefs(activity);
        });

        return builder.create();
    }
}
