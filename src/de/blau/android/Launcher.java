package de.blau.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import de.blau.android.util.IntentUtil;


public class Launcher extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// This activity exists only to prevent the SplashActivity from having
		// intent-filters for the MAIN LAUNCHER, which is bugged on
		// launchMode="singleTask"
		Intent main = IntentUtil.getMainIntent(this);
		startActivity(main);
		finish();
	}

}
