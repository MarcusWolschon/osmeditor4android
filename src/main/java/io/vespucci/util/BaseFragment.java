package io.vespucci.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

/**
 * Workaround deprecation of onAttach(Activity activity) See https://code.google.com/p/android/issues/detail?id=183358
 * and some other issues
 * 
 * @author simon
 *
 */
public abstract class BaseFragment extends Fragment {

    private static final String DEBUG_TAG = BaseFragment.class.getSimpleName().substring(0, Math.min(23, BaseFragment.class.getSimpleName().length()));

    @TargetApi(23)
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        onAttachToContext(context);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            onAttachToContext(activity);
        }
    }

    /**
     * Replacement for the onAttach callbacks
     * 
     * @param context the Android Context
     */
    protected abstract void onAttachToContext(@NonNull Context context);

    @Override
    public void onDestroyView() {
        // remove onFocusChangeListeners or else bad things might happen (at least with API 23)
        ViewGroup v = (ViewGroup) getView();
        if (v != null) {
            loopViews(v);
        }
        super.onDestroyView();
        Log.d(DEBUG_TAG, "onDestroyView");
    }

    /**
     * Recursively loop over the child Views of the ViewGroup and remove onFocusChangeListeners, might be worth it to
     * make this more generic
     * 
     * @param viewGroup the ViewGroup
     */
    private void loopViews(@NonNull ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View v = viewGroup.getChildAt(i);
            if (v instanceof ViewGroup) {
                this.loopViews((ViewGroup) v);
            } else {
                viewGroup.setOnFocusChangeListener(null);
            }
        }
    }
  
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(DEBUG_TAG, "onSaveInstanceState");
        Log.w(DEBUG_TAG, "onSaveInstanceState bundle size " + Util.getBundleSize(outState));
    }
}
