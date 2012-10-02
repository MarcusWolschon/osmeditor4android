package de.blau.android.util;

import android.annotation.TargetApi;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.os.StrictMode.ThreadPolicy.Builder;

/**
 * An ugly hack to disable the Strict Mode Death Penalty in legacy code sections.
 * @author Jan
 */
@Deprecated
public abstract class UglyHackForStrictMode {
	private static ThreadPolicy originalPolicy = null;
	
	@TargetApi(11)
	public static void beginLegacySection() {
		if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) return; 
		originalPolicy = StrictMode.getThreadPolicy();
		Builder builder = new Builder();
		builder.detectNetwork()
		 .penaltyFlashScreen()
		 .penaltyLog();
		StrictMode.setThreadPolicy(builder.build());
	}
	
	@TargetApi(11)
	public static void endLegacySection() {
		if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) return; 
		if (originalPolicy != null) StrictMode.setThreadPolicy(originalPolicy);
		originalPolicy = null;
	}
}
