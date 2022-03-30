package de.blau.android.layer.tasks;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.SizedFixedImmersiveDialogFragment;

/**
 * Configuration dialog that replicates the settings in the Preferences
 * 
 * @author Simon Poole
 *
 */
public class ConfigurationDialog extends SizedFixedImmersiveDialogFragment {

    private static final String DEBUG_TAG = "ConfigurationDialog";
    private static final String TAG       = "configurationDialog";

    /**
     * Display a dialog allowing the user to change some properties of the current layer
     * 
     * @param activity the calling Activity
     */
    public static void showDialog(@NonNull FragmentActivity activity) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            ConfigurationDialog configurationDialog = newInstance();
            configurationDialog.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * Dismiss the dialog
     * 
     * @param activity the calling Activity
     */
    private static void dismissDialog(@NonNull FragmentActivity activity) {
        de.blau.android.dialogs.Util.dismissDialog(activity, TAG);
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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(true);
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
            Preferences p = new Preferences(activity);
            if (activity instanceof Main) {
                ((Main) activity).updatePrefs(p);
            }
            App.getLogic().setPrefs(p);
            App.getLogic().getMap().setPrefs(getContext(), p);
            App.getLogic().getMap().invalidate();
        });

        return builder.create();
    }
}
