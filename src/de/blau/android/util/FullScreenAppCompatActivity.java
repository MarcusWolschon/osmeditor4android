package de.blau.android.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.view.View;


public class FullScreenAppCompatActivity extends AppCompatActivity {
	
	/**
	 * See http://stackoverflow.com/questions/32294607/call-requires-api-level-11current-min-is-9-android-app-activityoncreateview
	 * This is a workaround google refusing to fix bugs (even trivial ones) in a timely manner
	 */
	@SuppressLint("NewApi")
	public View onCreateView(View parent, String name, Context context, AttributeSet attrs)
	{
	    if(Build.VERSION.SDK_INT >= 11)
	      return super.onCreateView(parent, name, context, attrs);
	    return null;
	}
	
	@SuppressLint("NewApi")
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
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
}
