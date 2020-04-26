package de.blau.android.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

/**
 * Workaround deprecation of onAttach(Activity activity) See https://code.google.com/p/android/issues/detail?id=183358
 * 
 * @author simon
 *
 */
public abstract class BaseFragment extends Fragment {

    private static final String DEBUG_TAG = "BaseFragment";

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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(DEBUG_TAG, "onSaveInstanceState");
        Log.w(DEBUG_TAG, "onSaveInstanceState bundle size " + Util.getBundleSize(outState));
    }
}
