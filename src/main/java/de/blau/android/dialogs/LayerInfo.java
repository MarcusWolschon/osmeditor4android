package de.blau.android.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatDialog;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import de.blau.android.R;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.util.Screen;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

/**
 * A generic dialog fragment to display some info on layers
 * 
 * @author simon
 *
 */
public abstract class LayerInfo extends DialogFragment {

    private static final String DEBUG_TAG = LayerInfo.class.getName();

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

    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = new AlertDialog.Builder(getActivity());
        DoNothingListener doNothingListener = new DoNothingListener();
        builder.setPositiveButton(R.string.done, doNothingListener);
        builder.setView(createView(null));
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout((int) (Screen.getScreenSmallDimemsion(getActivity()) * 0.9), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (!getShowsDialog()) {
            return createView(container);
        }
        return null;
    }

    /**
     * Create the view we want to display
     * 
     * Classes extending LayerInfo need to override this but call through to the super method to get the view
     * 
     * @param container parent view or null
     * @return the View
     */
    protected ScrollView createEmptyView(@Nullable ViewGroup container) {
        LayoutInflater inflater = ThemeUtils.getLayoutInflater(getActivity());
        return (ScrollView) inflater.inflate(R.layout.element_info_view, container, false);
    }

    /**
     * Create the non-standard part of the dialog
     * 
     * @param container the parent view or null
     * @return a View with the content to display
     */
    protected abstract View createView(@Nullable ViewGroup container);

    /**
     * Get the string resource formated as an italic string
     * 
     * @param resId String resource id
     * @return a Spanned containing the string
     */
    protected Spanned toItalic(int resId) {
        return Util.fromHtml("<i>" + getString(resId) + "</i>");
    }
}
