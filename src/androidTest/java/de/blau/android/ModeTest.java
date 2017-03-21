package de.blau.android;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.util.Log;
import android.view.View;
import de.blau.android.prefs.Preferences;

/**
 *
 * @author simon
 *
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ModeTest {
	
	Main main = null;
	View v = null;
	UiDevice mDevice = null; 

	@Rule
	public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

	@Before
	public void setup() {
		main = mActivityRule.getActivity();
		TestUtils.grantPermissons();
		TestUtils.dismissStartUpDialogs(main);
		mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
	}

	@Test
	public void lock() {
		Logic logic = App.getLogic();
		logic.setLocked(true);
		logic.setZoom(main.getMap(), 20);
		UiObject map = mDevice.findObject(new UiSelector().resourceId("de.blau.android:id/map_view"));
		Assert.assertTrue(map.exists());
		
		UiObject snack = mDevice.findObject(new UiSelector().textStartsWith(main.getString(R.string.toast_unlock_to_edit)));
		try {
			map.click();
		} catch (UiObjectNotFoundException e) {
			Assert.fail(e.getMessage());
		}		
		Assert.assertTrue(snack.waitForExists(5000));
		
		UiObject lock = mDevice.findObject(new UiSelector().resourceId("de.blau.android:id/floatingLock"));
		try {
			lock.click();
		} catch (UiObjectNotFoundException e) {
			Assert.fail(e.getMessage());
		}	
		Assert.assertTrue(!logic.isLocked());
		
		Assert.assertEquals(Mode.MODE_EASYEDIT, logic.getMode()); // start with this and cycle through the modes
		try {
			lock.longClick();
		} catch (UiObjectNotFoundException e) {
			Assert.fail(e.getMessage());
		}	
		Assert.assertEquals(Mode.MODE_TAG_EDIT, logic.getMode());
		
		try {
			lock.longClick();
		} catch (UiObjectNotFoundException e) {
			Assert.fail(e.getMessage());
		}	
		Assert.assertEquals(Mode.MODE_INDOOR, logic.getMode());
		
		try {
			lock.longClick();
		} catch (UiObjectNotFoundException e) {
			Assert.fail(e.getMessage());
		}	
		Assert.assertEquals(Mode.MODE_EASYEDIT, logic.getMode());
	}
}