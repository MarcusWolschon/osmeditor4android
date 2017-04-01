package de.blau.android;

import android.content.Context;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.support.test.uiautomator.Until;
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
			clickButton(mDevice, "allow");
			clickButton(mDevice, "zulassen");
		}
	}

	public static void dismissStartUpDialogs(Context ctx) {
		UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
		clickButton(mDevice, ctx.getResources().getString(R.string.okay));
		clickButton(mDevice, ctx.getResources().getString(R.string.location_load_dismiss));
	}
	
	public static void selectIntentRecipient(Context ctx) {
		UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
		clickButton(mDevice, "Vespucci");
		clickButton(mDevice, "Just once");
		clickButton(mDevice, "Nur diesmal");
	}

	static void clickButton(UiDevice device, String text) {
		for (;;) {
			// Note: contrary to "text", "textStartsWith" is case insensitive
			device.wait(Until.findObject(By.clickable(true).textStartsWith(text)), 500);
			UiObject button = device.findObject(new UiSelector().clickable(true).textStartsWith(text)); 
			if (button.exists()) {
				try {
					button.click();
				} catch (UiObjectNotFoundException e) {
					Log.e("TestUtils", "Object vanished.");
				}
			} else {
				return;
			}
		}
	}
}


