package de.blau.android.dialogs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;
import static de.blau.android.dialogs.Util.hasState;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.CancelableDialogFragment;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.ThemeUtils;

/**
 * Simplyfy a List of GeoPoint and potentially create an/multiple OSM ways from it
 */
public class SimplificationDialog extends CancelableDialogFragment {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, SimplificationDialog.class.getSimpleName().length());
    private static final String DEBUG_TAG = SimplificationDialog.class.getSimpleName().substring(0, TAG_LEN);

    private static final String TAG = "fragment_simplification";

    private static final String TITLE_KEY                    = "title_key";
    private static final String USE_JITTER_FILTER_KEY        = "use_jitter_filter";
    public static final String  JITTER_FILTER_KEY            = "jitter_filter";
    public static final String  JITTER_FILTER_TOLERANCE_KEY  = "jitter_filter_tolerance";
    public static final String  SIMPLIFY_KEY                 = "simplify";
    public static final String  SIMPLIFY_TOLERANCE_KEY       = "simplify_tolerance";
    public static final String  SIMPLIFY_FRAGMENT_RESULT_KEY = "SIMPLIFY_FRAGMENT_RESULT_KEY";

    private int      titleRes;
    private boolean  useJitterFilter;
    private CheckBox jitterFilter;
    private EditText jitterFilterTolerance;
    private CheckBox simplify;
    private EditText simplifyTolerance;

    /**
     * Show a dialog asking for simplification parameters
     * 
     * @param activity the calling FragmentActivity
     * @param titleRes resource id for the title
     *
     * 
     */
    public static void show(@NonNull AppCompatActivity activity, int titleRes, boolean useJitterFilter) {
        Log.d(DEBUG_TAG, "show for activity");
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();

            SimplificationDialog simplificationFragment = newInstance(titleRes, useJitterFilter);
            simplificationFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    public static void show(@NonNull Fragment fragment, int titleRes, boolean useJitterFilter) {
        Log.d(DEBUG_TAG, "show for fragment");
        try {
            FragmentManager fm = fragment.getChildFragmentManager();
            SimplificationDialog simplificationFragment = newInstance(titleRes, useJitterFilter);
            simplificationFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * Dismiss the Dialog
     * 
     * @param activity the calling FragmentActivity
     */
    private static void dismissDialog(@NonNull AppCompatActivity activity) {
        de.blau.android.dialogs.Util.dismissDialog(activity, TAG);
    }

    /**
     * Create new instance of this object
     * 
     * @param titleRes resource id for the title
     * 
     * @return a SimplificationDialog instance
     */
    private static SimplificationDialog newInstance(int titleRes, boolean useJitterFilter) {
        SimplificationDialog f = new SimplificationDialog();
        Bundle args = new Bundle();
        args.putInt(TITLE_KEY, titleRes);
        args.putBoolean(USE_JITTER_FILTER_KEY, useJitterFilter);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @NonNull
    @Override
    @SuppressLint("InflateParams")
    public AppCompatDialog onCreateDialog(Bundle savedState) {
        if (savedState != null) {
            Log.d(DEBUG_TAG, "Recreating from saved state");
            getArgsOrState(savedState);
        } else {
            getArgsOrState(getArguments());
        }
        final FragmentActivity activity = getActivity();

        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(activity);
        ((Main) activity).descheduleAutoLock();
        enableAutolockReschedule();

        final Preferences prefs = App.getPreferences(activity);
        View layout = inflater.inflate(useJitterFilter ? R.layout.jitter_simplification : R.layout.simplification, null);

        if (useJitterFilter) {
            jitterFilter = ((CheckBox) layout.findViewById(R.id.jitter_filter_check));
            jitterFilterTolerance = ((EditText) layout.findViewById(R.id.jitter_filter_tolerance));

            jitterFilter.setChecked(hasState(savedState, JITTER_FILTER_KEY) ? savedState.getBoolean(JITTER_FILTER_KEY) : prefs.getJitterFilter());
            try {
                jitterFilterTolerance.setText(Float.toString(
                        hasState(savedState, JITTER_FILTER_TOLERANCE_KEY) ? savedState.getInt(JITTER_FILTER_TOLERANCE_KEY) : prefs.getJitterFilterTolerance()));
            } catch (NumberFormatException nfex) {
                jitterFilterTolerance.setText(Float.toString(prefs.getJitterFilterTolerance()));
            }
            simplify = ((CheckBox) layout.findViewById(R.id.simplify_check));
            simplify.setChecked(hasState(savedState, SIMPLIFY_KEY) ? savedState.getBoolean(SIMPLIFY_KEY) : prefs.getSimplify());
        }

        simplifyTolerance = ((EditText) layout.findViewById(R.id.simplify_tolerance));
        try {
            simplifyTolerance.setText(
                    Float.toString(hasState(savedState, SIMPLIFY_TOLERANCE_KEY) ? savedState.getInt(SIMPLIFY_TOLERANCE_KEY) : prefs.getSimplifyTolerance()));
        } catch (NumberFormatException nfex) {
            simplifyTolerance.setText(Float.toString(prefs.getSimplifyTolerance()));
        }

        AppCompatDialog dialog = ThemeUtils.getAlertDialogBuilder(activity).setTitle(titleRes).setView(layout).setNeutralButton(R.string.cancel, null)
                .setPositiveButton(R.string.set, null).create();

        dialog.setOnShowListener(d -> {
            Button positive = ((AlertDialog) d).getButton(DialogInterface.BUTTON_POSITIVE);
            positive.setOnClickListener(view -> {
                try {
                    Bundle resultBundle = new Bundle();
                    if (useJitterFilter) {
                        resultBundle.putBoolean(JITTER_FILTER_KEY, jitterFilter.isChecked());
                        resultBundle.putFloat(JITTER_FILTER_TOLERANCE_KEY, Float.parseFloat(jitterFilterTolerance.getText().toString()));
                        resultBundle.putBoolean(SIMPLIFY_KEY, simplify.isChecked());
                    }
                    resultBundle.putFloat(SIMPLIFY_TOLERANCE_KEY, Float.parseFloat(simplifyTolerance.getText().toString()));

                    getParentFragmentManager().setFragmentResult(SIMPLIFY_FRAGMENT_RESULT_KEY, resultBundle);
                    dialog.dismiss();
                } catch (NumberFormatException nfex) {
                    ScreenMessage.toastTopError(activity, activity.getString(R.string.toast_invalid_number_format, nfex.getLocalizedMessage()));
                }
            });
        });

        return dialog;
    }

    /**
     * Get arguments or state from a Bundle
     * 
     * @param bundle the bundle
     */
    private void getArgsOrState(@NonNull Bundle bundle) {
        titleRes = bundle.getInt(TITLE_KEY);
        useJitterFilter = bundle.getBoolean(USE_JITTER_FILTER_KEY);
    }

    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(DEBUG_TAG, "onSaveInstanceState");
        outState.putInt(TITLE_KEY, titleRes);
        outState.putBoolean(USE_JITTER_FILTER_KEY, useJitterFilter);

        if (useJitterFilter) {
            outState.putBoolean(JITTER_FILTER_KEY, jitterFilter.isChecked());
            outState.putFloat(JITTER_FILTER_TOLERANCE_KEY, Float.parseFloat(jitterFilterTolerance.getText().toString()));
        }

        outState.putBoolean(SIMPLIFY_KEY, simplify.isChecked());
        outState.putFloat(SIMPLIFY_TOLERANCE_KEY, Float.parseFloat(simplifyTolerance.getText().toString()));
    }
}
