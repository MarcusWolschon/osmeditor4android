package de.blau.android;

import android.content.Context;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.util.Log;

/**
 * 
 * @author simon
 *
 */
public class TestUtils {
	/**
	 * Grant permissions by clicking on the dialogs, currently only works for English and German
	 */
	public static void grantPermissons() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
			dismiss(mDevice, "allow");
			dismiss(mDevice, "zulassen");
		}
	}

	public static void dismissStartUpDialogs(Context ctx) {
		UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
		dismiss(mDevice, ctx.getResources().getString(R.string.okay));
		dismiss(mDevice, ctx.getResources().getString(R.string.location_load_dismiss));
	}

	static void dismiss(UiDevice device, String text) {
		for (;;) {
			// Note: contrary to "text", "textStartsWith" is case insensitive
			UiObject allowPermissions = device.findObject(new UiSelector().clickable(true).textStartsWith(text)); 
			if (allowPermissions.exists()) {
				try {
					allowPermissions.click();
				} catch (UiObjectNotFoundException e) {
					Log.e("TestUtils", "Object vanished.");
				}
			} else {
				return;
			}
		}
	}
}


