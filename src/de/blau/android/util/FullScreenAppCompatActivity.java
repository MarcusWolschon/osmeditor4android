package de.blau.android.util;

import android.annotation.SuppressLint;
import android.os.Build;
import android.util.Log;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import de.blau.android.R;
import de.blau.android.prefs.Preferences;


public class FullScreenAppCompatActivity extends AppCompatActivity {
	
	private static final String DEBUG_TAG = FullScreenAppCompatActivity.class.getSimpleName();
	private boolean fullScreen = false;
	
	@SuppressLint("NewApi")
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (fullScreen) {
			if (hasFocus) {
				getWindow().getDecorView().setSystemUiVisibility(
						View.SYSTEM_UI_FLAG_LAYOUT_STABLE
						| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//						| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
						| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//						| View.SYSTEM_UI_FLAG_FULLSCREEN
						| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
						);}
		}
	}
	
	/**
	 * This is likely not all to reliable
	 * @return true if we should use  the full screen layout
	 */
	protected boolean useFullScreen(Preferences prefs) {
		fullScreen = false;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			String fullscreenPref = prefs.getFullscreenMode();
			if (fullscreenPref.equals(getString(R.string.full_screen_auto))) {
				fullScreen = !KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK) && !KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_HOME);
				Log.d(DEBUG_TAG,"full screen auto " + fullScreen);
			} else if (fullscreenPref.equals(getString(R.string.full_screen_never))) {
				fullScreen = false;
				Log.d(DEBUG_TAG,"full screen never");
			} else if (fullscreenPref.equals(getString(R.string.full_screen_force))) {
				fullScreen = true;
				Log.d(DEBUG_TAG,"full screen force");
			} else if (fullscreenPref.equals(getString(R.string.full_screen_no_statusbar))) {
				fullScreen = true;
				Log.d(DEBUG_TAG,"ful screen no statusbar");
				return false; // ugly hack
			}
		} 
		return fullScreen;
	}
}
