package de.blau.android.util;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import de.blau.android.R;
import de.blau.android.prefs.Preferences;

/**
 * Handle determining if we are on a system with soft navigation buttons and switch to full screen mode
 * 
 * @author simon
 *
 */
public abstract class FullScreenAppCompatActivity extends BugFixedAppCompatActivity {

    private static final String DEBUG_TAG  = FullScreenAppCompatActivity.class.getSimpleName();
    private boolean             fullScreen = false;
    private boolean             hideStatus = false;
    private final Handler       handler    = new Handler();

    @SuppressLint("NewApi")
    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            View decorView = getWindow().getDecorView();
            decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    Log.d(DEBUG_TAG, "onSystemUiVisibilityChange " + Integer.toHexString(visibility));
                    if ((visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                        if (fullScreen) {
                            synchronized (handler) {
                                handler.removeCallbacks(navHider);
                                handler.postDelayed(navHider, 2000);
                            }
                        }
                    } else {
                        // no UI changes for now
                    }
                }
            });
        }
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

    private Runnable navHider = new Runnable() {
        @Override
        public void run() {
            hideSystemUI();
        }
    };

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
        return hideStatus;
    }

    /**
     * Turn off a soft button navigation button, note this only works if the main view of the app actually has focus
     */
    @SuppressLint("NewApi")
    private void hideSystemUI() {
        View view = getWindow().getDecorView();
        if (view != null && fullScreen && Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            Log.d(DEBUG_TAG, "hiding nav bar");
            int fullScreenMode = (hideStatus ? View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN : 0)
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | fullScreenMode
                    | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY : View.SYSTEM_UI_FLAG_IMMERSIVE));
        }
    }

    /**
     * This is likely not all too reliable
     * 
     * @return true if we should use the full screen layout
     */
    protected boolean useFullScreen(Preferences prefs) {
        fullScreen = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            String fullScreenPref = prefs.getFullscreenMode();
            if (fullScreenPref.equals(getString(R.string.full_screen_auto))) {
                fullScreen = !KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK) && !KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_HOME);
                Log.d(DEBUG_TAG, "full screen auto " + fullScreen + " KEYCODE_BACK " + KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK) + " KEYCODE_HOME "
                        + KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_HOME));
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
        }
        return fullScreen;
    }
}
