package io.vespucci.layer.tasks;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import android.annotation.SuppressLint;
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

    private static final String TAG = "taskConfigurationDialog";

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

    @SuppressLint("NewApi")
    @NonNull
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        final FragmentActivity activity = getActivity();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        Resources r = activity.getResources();
        Set<String> taskFilter = new HashSet<>(Arrays.asList(r.getStringArray(R.array.bug_filter_defaults)));
        final String prefKey = r.getString(R.string.config_bugFilter_key);
        taskFilter = prefs.getStringSet(prefKey, taskFilter);

        String[] prefValues = r.getStringArray(R.array.bug_filter_values);
        final int prefLength = prefValues.length;
        final boolean[] checked = new boolean[prefLength];
        for (int i = 0; i < prefLength; i++) {
            checked[i] = taskFilter.contains(prefValues[i]);
        }

        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.config_bugFilter_title);

        builder.setMultiChoiceItems(R.array.bug_filter_entries, checked, (dialog, which, isChecked) -> checked[which] = isChecked);
        builder.setPositiveButton(R.string.okay, (dialog, which) -> {
            Set<String> newTaskFilter = new HashSet<>();
            for (int i = 0; i < prefLength; i++) {
                if (checked[i]) {
                    newTaskFilter.add(prefValues[i]);
                }
            }
            prefs.edit().putStringSet(prefKey, newTaskFilter).commit();
            updatePrefs(activity);
        });

        return builder.create();
    }
}
