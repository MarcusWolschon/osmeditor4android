package de.blau.android.layer.grid;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
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

        builder.setSingleChoiceItems(R.array.scale_entries, selected[0], new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selected[0] = which;
            }
        });

        builder.setPositiveButton(R.string.okay, new OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                prefs.edit().putString(prefKey, prefValues[selected[0]]).commit();
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
