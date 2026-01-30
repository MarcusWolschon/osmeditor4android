package de.blau.android.util;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.HashMap;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewCompat.OnUnhandledKeyEventListenerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

/**
 * Workaround deprecation of onAttach(Activity activity) See https://code.google.com/p/android/issues/detail?id=183358
 * and some other issues
 * 
 * @author simon
 *
 */
public abstract class BaseFragment extends Fragment {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, BaseFragment.class.getSimpleName().length());
    private static final String DEBUG_TAG = BaseFragment.class.getSimpleName().substring(0, TAG_LEN);

    protected final java.util.Map<String, KeyboardShortcut.Action> actionMap = new HashMap<>();
    protected KeyboardShortcut                                     keyboard;

    @TargetApi(23)
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        onAttachToContext(context);
        keyboard = new KeyboardShortcut(context);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            onAttachToContext(activity);
            keyboard = new KeyboardShortcut(activity);
        }
    }

    /**
     * Replacement for the onAttach callbacks
     * 
     * @param context the Android Context
     */
    protected abstract void onAttachToContext(@NonNull Context context);

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ViewCompat.addOnUnhandledKeyEventListener(view, new KeyEventListener());
    }

    /**
     * A KeyEventListener for unhandled key events.
     * 
     * @author simon
     */
    public class KeyEventListener implements OnUnhandledKeyEventListenerCompat {

        @Override
        public boolean onUnhandledKeyEvent(View v, KeyEvent event) {
            final FragmentActivity activity = getActivity();
            if (activity == null) {
                Log.w(DEBUG_TAG, "onUnhandledKeyEvent no activity");
                return false;
            }
            switch (event.getAction()) {
            case KeyEvent.ACTION_UP:
                // this needs to be here or else this will get reported in main
                if (KeyEvent.KEYCODE_BACK == event.getKeyCode()) {
                    activity.getOnBackPressedDispatcher().onBackPressed();
                }
                Log.w(DEBUG_TAG, "Up key event " + event.toString());
                return true;
            case KeyEvent.ACTION_DOWN:
                char shortcut = Character.toLowerCase((char) event.getUnicodeChar(0));
                KeyboardShortcut.MetaKey metaKey = KeyboardShortcut.MetaKey.fromState(event.getMetaState());
                return keyboard.executeShortcut(metaKey, shortcut, actionMap);
            default:
                Log.w(DEBUG_TAG, "Unknown key event " + event.getAction());
            }
            return false;
        }
    }

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
