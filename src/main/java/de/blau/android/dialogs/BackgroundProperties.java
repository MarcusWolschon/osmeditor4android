package de.blau.android.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.layer.tiles.MapTilesLayer;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.CancelableDialogFragment;
import de.blau.android.util.ThemeUtils;

/**
 * Display a dialog allowing the user to change some properties of the current background
 *
 */
public class BackgroundProperties extends CancelableDialogFragment {

    private static final int    TAG_LEN   = Math.min(23, BackgroundProperties.class.getSimpleName().length());
    private static final String DEBUG_TAG = BackgroundProperties.class.getSimpleName().substring(0, TAG_LEN);

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
        de.blau.android.dialogs.Util.dismissDialog(activity, TAG);
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

    @NonNull
    @SuppressLint("InflateParams")
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.background_properties_title);
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(getActivity());
        DoNothingListener doNothingListener = new DoNothingListener();
        setDialogLayout(getActivity());
        View layout = inflater.inflate(R.layout.background_properties, null);
        SeekBar seeker = (SeekBar) layout.findViewById(R.id.background_contrast_seeker);
        seeker.setProgress(contrast2progress(App.getPreferences(getActivity()).getContrastValue()));
        int layerIndex = getArguments().getInt(LAYERINDEX);
        MapTilesLayer<?> layer = (MapTilesLayer<?>) App.getLogic().getMap().getLayer(layerIndex);
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

    /**
     * Create an OnSeekBarChangeListener for a specific layer
     * 
     * @param layer the layer
     * @return an OnSeekBarChangeListener
     */
    @NonNull
    private OnSeekBarChangeListener createSeekBarListener(@NonNull final MapTilesLayer<?> layer) {
        return new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(final SeekBar seekBar, int progress, final boolean fromTouch) {
                float contrast = progress / 127.5f - 1f; // range from -1 to +1
                layer.setContrast(contrast);
                layer.invalidate();
                App.getPreferences(getActivity()).setContrastValue(contrast);
            }

            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) {
                // required but not used
            }

            @Override
            public void onStopTrackingTouch(final SeekBar arg0) {
                // required but not used
            }
        };
    }

    /**
     * Set up the layout
     * 
     * @param activity the calling Activity
     */
    private static void setDialogLayout(@NonNull Activity activity) {
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(activity);
        View layout = inflater.inflate(R.layout.background_properties, null);
        SeekBar seeker = (SeekBar) layout.findViewById(R.id.background_contrast_seeker);
        seeker.setProgress(contrast2progress(App.getPreferences(activity).getContrastValue()));
    }
}
