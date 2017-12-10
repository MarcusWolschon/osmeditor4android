package de.blau.android;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Assert;

import android.content.Context;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
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
	private static final String DEBUG_TAG = "TestUtils";

	/**
	 * Grant permissions by clicking on the dialogs, currently only works for English and German
	 */
	public static void grantPermissons() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
			boolean notdone = true;
			while (notdone) {
				notdone = clickText(mDevice, true, "allow", true) ||
						clickText(mDevice, true, "zulassen", false);
			}
		}
	}

	public static void dismissStartUpDialogs(Context ctx) {
		UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
		clickText(mDevice, true, ctx.getResources().getString(R.string.okay), false);
		clickText(mDevice, true, ctx.getResources().getString(R.string.location_load_dismiss), false);
	}
	
	public static void selectIntentRecipient(Context ctx) {
		UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
		clickText(mDevice, true, "Vespucci", false);
		clickText(mDevice, true, "Just once", false);
		clickText(mDevice, true, "Nur diesmal", false);
	}

	public static boolean clickText(UiDevice device, boolean clickable, String text, boolean waitForNewWindow) {	
		Log.w(DEBUG_TAG, "Searching for object with " + text);
		// Note: contrary to "text", "textStartsWith" is case insensitive
		BySelector bySelector = null; 
		UiSelector uiSelector = null; 
		//NOTE order of the selector terms is significant
		if (clickable) {
			bySelector = By.clickable(true).textStartsWith(text);
			uiSelector = new UiSelector().clickable(true).textStartsWith(text);
		} else {
			bySelector = By.textStartsWith(text);
			uiSelector = new UiSelector().textStartsWith(text);
		}
		device.wait(Until.findObject(bySelector), 500);
		UiObject button = device.findObject(uiSelector); 
		if (button.exists()) {
			try {
				if (waitForNewWindow) {
					button.clickAndWaitForNewWindow();
				} else {
					button.click();
					Log.e(DEBUG_TAG, ".... clicked");
				}
				return true;
			} catch (UiObjectNotFoundException e) {
				Log.e(DEBUG_TAG, "Object vanished.");
				return false;
			}
		} else {
			Log.e(DEBUG_TAG, "Object not found");
			return false;
		}
	}
	
    public static void clickUp(UiDevice mDevice) {
		UiObject homeButton = mDevice.findObject(new UiSelector().clickable(true).descriptionStartsWith("Navigate up"));
		if (!homeButton.exists()) {
			homeButton = mDevice.findObject(new UiSelector().clickable(true).descriptionStartsWith("Nach oben"));
		}
		try {
			homeButton.clickAndWaitForNewWindow();
		} catch (UiObjectNotFoundException e) {
			Assert.fail(e.getMessage());
		}
    }
    
    public static byte[] readInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int readBytes = -1;
        try {
            while((readBytes = is.read(buffer)) > -1){
                baos.write(buffer,0,readBytes);
            }
        } finally {
            is.close();
        }
        return baos.toByteArray();
    }
}


