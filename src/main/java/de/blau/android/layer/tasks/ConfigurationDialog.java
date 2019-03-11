package de.blau.android.layer.tasks;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatDialog;
import android.util.Log;
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
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            Fragment fragment = fm.findFragmentByTag(TAG);
            if (fragment != null) {
                ft.remove(fragment);
            }
            ft.commit();
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "dismissDialog", isex);
        }
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

    @NonNull
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            throw new IllegalStateException("Only SDK 11 and higher supported");
        }
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

        builder.setMultiChoiceItems(R.array.bug_filter_entries, checked, new OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                checked[which] = isChecked;
            }
        });
        builder.setPositiveButton(R.string.okay, new OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {
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
                App.getLogic().getMap().setPrefs(getContext(), p);
                App.getLogic().getMap().invalidate();
            }
        });

        return builder.create();
    }
}
