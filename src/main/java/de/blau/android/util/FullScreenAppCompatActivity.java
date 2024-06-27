package de.blau.android.util;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import androidx.annotation.NonNull;
import de.blau.android.R;
import de.blau.android.prefs.Preferences;

/**
 * Handle determining if we are on a system with soft navigation buttons and switch to full screen mode
 * 
 * @author Simon Poole
 *
 */
public abstract class FullScreenAppCompatActivity extends ConfigurationChangeAwareActivity {

    private static final String DEBUG_TAG = FullScreenAppCompatActivity.class.getSimpleName().substring(0,
            Math.min(23, FullScreenAppCompatActivity.class.getSimpleName().length()));

    private static final int FULLSCREEN_UI = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN;
    private static final int NAV_HIDDEN    = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;

    private boolean fullScreen = false;
    private boolean hideStatus = false;
    private Handler handler;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler(Looper.getMainLooper());
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    @Override
    protected void onResume() {
        super.onResume();
        View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(visibility -> {
            Log.d(DEBUG_TAG, "onSystemUiVisibilityChange " + Integer.toHexString(visibility));
            if ((visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                if (fullScreen) {
                    // in immersive mode directly hiding seems to work
                    hideSystemUI();
                }
            } else {
                // no UI changes for now
            }
        });
        if (fullScreen) {
            synchronized (handler) {
                handler.removeCallbacks(navHider);
                // FIXME there seems to be a timing issue or similar that causes the nav bar to re-appear just after it
                // has been hidden by
                // onWindowFocusChanged, this makes sure that the bar is re-hidden. The 1.5 seconds seems to work.
                handler.postDelayed(navHider, 1500);
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) { // hiding UI first and then call super seems to avoid status bar overlaying actionbar
            Log.d(DEBUG_TAG, "onWindowFocusChanged");
            hideSystemUI();
        }
        super.onWindowFocusChanged(hasFocus);
    }

    private Runnable navHider = this::hideSystemUI;

    /**
     * Return true if we are in full screen mode
     * 
     * @return true if in full screen mode
     */
    public boolean isFullScreen() {
        return fullScreen;
    }

    /**
     * Return true if we are not showing the status bar
     * 
     * @return true if we are not showing the status bar
     */
    protected boolean statusBarHidden() {
        return hideStatus && !Util.isInMultiWindowModeCompat(this);
    }

    /**
     * Turn off a soft button navigation button, note this only works if the main view of the app actually has focus
     */
    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    private void hideSystemUI() {
        if (fullScreen) {
            Log.d(DEBUG_TAG, "hiding nav bar");
            View view = getWindow().getDecorView();
            int fullScreenMode = (statusBarHidden() ? FULLSCREEN_UI : 0) | NAV_HIDDEN;
            view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | fullScreenMode | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    /**
     * This is likely not all too reliable
     * 
     * @param prefs the current Preferences object
     * @return true if we should use the full screen layout
     */
    protected boolean useFullScreen(@NonNull Preferences prefs) {
        fullScreen = false;
        hideStatus = false;
        String fullScreenPref = prefs.getFullscreenMode();
        if (fullScreenPref.equals(getString(R.string.full_screen_auto))) {
            final boolean hasNavBar = hasNavBar(getResources());
            final int edgeToEdgeEnabled = isEdgeToEdgeEnabled(getResources());
            final boolean hasBack = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
            final boolean hasHome = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_HOME);
            fullScreen = Build.VERSION.SDK_INT < Build.VERSION_CODES.R && ((hasNavBar && edgeToEdgeEnabled == 0) || (!hasBack && !hasHome));
            Log.d(DEBUG_TAG, "full screen auto " + fullScreen + " hasNavBar " + hasNavBar + " isEdgeToEdgeEnabled " + edgeToEdgeEnabled + " KEYCODE_BACK "
                    + hasBack + " KEYCODE_HOME " + hasHome);
        } else if (fullScreenPref.equals(getString(R.string.full_screen_never))) {
            fullScreen = false;
            Log.d(DEBUG_TAG, "full screen never");
        } else if (fullScreenPref.equals(getString(R.string.full_screen_force))) {
            fullScreen = true;
            Log.d(DEBUG_TAG, "full screen force");
        } else if (fullScreenPref.equals(getString(R.string.full_screen_no_statusbar))) {
            fullScreen = true;
            hideStatus = true;
            Log.d(DEBUG_TAG, "full screen no statusbar");
        }
        return fullScreen;
    }

    /**
     * Test if the device has a navigation bar
     * 
     * This uses an undocumented internal resource id, but there is nothing else prior to Android 11 / API 30
     * 
     * @param resources to retrieve the setting from
     * @return true if the device has a navigation bar
     * 
     * @see <a href=
     *      "https://stackoverflow.com/questions/28983621/detect-soft-navigation-bar-availability-in-android-device-progmatically">Detect
     *      soft navigation bar availability</a>
     */
    private static boolean hasNavBar(@NonNull Resources resources) {
        int id = resources.getIdentifier("config_showNavigationBar", "bool", "android");
        return id > 0 && resources.getBoolean(id);
    }

    /**
     * Determine which navigation mode the device supports
     * 
     * This uses an undocumented internal resource id, but there is nothing else prior to Android 11 / API 30
     * 
     * @param resources to retrieve the setting from
     * @return 0 : Navigation is displaying with 3 buttons, 1 : displaying with 2 button(Android P navigation mode), 2 :
     *         Full screen gesture(Gesture on android Q)
     *
     * @see <a href=
     *      "https://stackoverflow.com/questions/56689210/how-to-detect-full-screen-gesture-mode-in-android-10">How to
     *      detect full screen gesture mode in android 10</a>
     */
    private static int isEdgeToEdgeEnabled(@NonNull Resources resources) {
        int resourceId = resources.getIdentifier("config_navBarInteractionMode", "integer", "android");
        if (resourceId > 0) {
            return resources.getInteger(resourceId);
        }
        return 0;
    }
}
