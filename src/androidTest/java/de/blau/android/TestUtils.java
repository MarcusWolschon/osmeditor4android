package de.blau.android;

import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.util.Log;

/**
 * Grant permissions by clicking on the dialogs, currently only works for English and German
 * @author simon
 *
 */
public class TestUtils {
	public static void grantPermissons() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
			for (;;) {
				UiObject allowPermissions = mDevice.findObject(new UiSelector().text("Allow"));
				if (!allowPermissions.exists()) {
					allowPermissions = mDevice.findObject(new UiSelector().text("ZULASSEN"));
				}
				if (allowPermissions.exists()) {
					try {
						allowPermissions.click();
					} catch (UiObjectNotFoundException e) {
						Log.e("grantWritePermission", "There is no permissions dialog to interact with ");
					}
				} else {
					break;
				}
			}
		}
	}
}
