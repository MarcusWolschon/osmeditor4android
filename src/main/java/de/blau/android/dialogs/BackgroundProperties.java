package de.blau.android.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.ImmersiveDialogFragment;
import de.blau.android.util.ThemeUtils;
import de.blau.android.views.layers.MapTilesLayer;

/**
 * Display a dialog allowing the user to change some properties of the current background
 *
 */
public class BackgroundProperties extends ImmersiveDialogFragment {

    private static final String DEBUG_TAG = BackgroundProperties.class.getSimpleName();

    private static final String TAG = "fragment_background_properties";

    private static final String LAYERINDEX = "layer_index";

    /**
     * Display a dialog allowing the user to change some properties of the current background
     * 
     * @param activity the calling Activity
     * @param layerIndex the index of the Layer
     */
    public static void showDialog(@NonNull FragmentActivity activity, int layerIndex) {
        dismissDialog(activity);
        try {
            setDialogLayout(activity);
            FragmentManager fm = activity.getSupportFragmentManager();
            BackgroundProperties backgroundPropertiesFragment = newInstance(layerIndex);
            backgroundPropertiesFragment.show(fm, TAG);
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
     * Get a new BackgroundProperties dialog instance
     * 
     * @param layerIndex the index of the Layer
     * @return a new BackgroundProperties dialog instance
     */
    @NonNull
    private static BackgroundProperties newInstance(int layerIndex) {
        BackgroundProperties f = new BackgroundProperties();
        Bundle args = new Bundle();
        args.putInt(LAYERINDEX, layerIndex);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(DEBUG_TAG, "onAttach");
        if (!(context instanceof Main)) {
            throw new ClassCastException(context.toString() + " can only be called from Main");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(true);
    }

    @NonNull
    @SuppressLint("InflateParams")
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        Preferences prefs = new Preferences(getActivity());
        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.menu_tools_background_properties);
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(getActivity());
        DoNothingListener doNothingListener = new DoNothingListener();
        setDialogLayout(getActivity());
        View layout = inflater.inflate(R.layout.background_properties, null);
        SeekBar seeker = (SeekBar) layout.findViewById(R.id.background_contrast_seeker);
        seeker.setProgress(contrast2progress(prefs.getContrastValue()));
        int layerIndex = getArguments().getInt(LAYERINDEX);
        MapTilesLayer layer = (MapTilesLayer) App.getLogic().getMap().getLayer(layerIndex);
        if (layer != null) {
            seeker.setOnSeekBarChangeListener(createSeekBarListener(layer));
        } else {
            ACRAHelper.nocrashReport(null, "layer null");
        }
        builder.setView(layout);
        builder.setPositiveButton(R.string.okay, doNothingListener);

        return builder.create();
    }

    /**
     * Convert contrast value -1...+1 to a int suitable for a progress bar
     * 
     * @param contrast the contrast value
     * @return a value scaled for a progress bar
     */
    static int contrast2progress(float contrast) {
        return (int) ((contrast + 1) * 127.5f);
    }

    private OnSeekBarChangeListener createSeekBarListener(@NonNull final MapTilesLayer layer) {
        return new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(final SeekBar seekBar, int progress, final boolean fromTouch) {
                Preferences prefs = new Preferences(getActivity());
                float contrast = progress / 127.5f - 1f; // range from -1 to +1
                layer.setContrast(contrast);
                layer.invalidate();
                prefs.setContrastValue(contrast);
            }

            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(final SeekBar arg0) {
            }
        };
    }

    private static void setDialogLayout(Activity activity) {
        Preferences prefs = new Preferences(activity);
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(activity);
        View layout = inflater.inflate(R.layout.background_properties, null);
        SeekBar seeker = (SeekBar) layout.findViewById(R.id.background_contrast_seeker);
        seeker.setProgress(contrast2progress(prefs.getContrastValue()));
    }
}
