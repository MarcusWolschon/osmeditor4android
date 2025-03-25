package io.vespucci.dialogs;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import io.vespucci.R;
import io.vespucci.listener.DoNothingListener;
import io.vespucci.util.InfoDialogFragment;

/**
 * A generic dialog fragment to display some info on layers
 * 
 * @author simon
 *
 */
public abstract class LayerInfo extends InfoDialogFragment {

    private static final String DEBUG_TAG = LayerInfo.class.getSimpleName().substring(0, Math.min(23, LayerInfo.class.getSimpleName().length()));

    private static final String TAG = "fragment_layer_info";

    /**
     * Show an info dialog
     * 
     * @param activity the calling Activity
     * @param layerInfoFragment an instance of fragment we are going to show
     * @param <T> a class that extends LayerInfo
     */
    public static <T extends LayerInfo> void showDialog(@NonNull FragmentActivity activity, T layerInfoFragment) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            layerInfoFragment.show(fm, TAG);
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
        io.vespucci.dialogs.Util.dismissDialog(activity, TAG);
    }

    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = new AlertDialog.Builder(getActivity());
        DoNothingListener doNothingListener = new DoNothingListener();
        builder.setPositiveButton(R.string.done, doNothingListener);
        builder.setView(createView(null));
        return builder.create();
    }
}
