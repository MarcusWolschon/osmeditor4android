package de.blau.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.view.View;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

/**
 *
 * @author simon
 *
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ModeTest {

    Main     main   = null;
    Logic    logic  = null;
    View     v      = null;
    UiDevice device = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        main = mActivityRule.getActivity();
        logic = App.getLogic();
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.unlock(device);
        TestUtils.stopEasyEdit(main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        main.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                App.getLogic().setMode(main, Mode.MODE_EASYEDIT);
            }
        });
    }

    /**
     * Lock, unlock, cycle through the modes
     */
    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void lock() {
        UiObject lock = device.findObject(new UiSelector().resourceId(device.getCurrentPackageName() + ":id/floatingLock"));

        logic.setLocked(true);
        logic.setZoom(main.getMap(), 20);
        main.getMap().invalidate();
        main.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                main.updateActionbarEditMode();
            }
        });
        device.waitForIdle();
        UiObject map = device.findObject(new UiSelector().resourceId(device.getCurrentPackageName() + ":id/map_view"));
        assertTrue(map.exists());
        assertTrue(logic.isLocked());
        try {
            assertTrue(!lock.isSelected());
        } catch (UiObjectNotFoundException e1) {
            fail(e1.getMessage());
        }

        UiObject tip = device.findObject(new UiSelector().textStartsWith(main.getString(R.string.tip_locked_mode)));
        try {
            map.click();
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        assertTrue(tip.waitForExists(5000));
        TestUtils.clickText(device, true, main.getString(R.string.okay), true, false); // for the tip alert
        device.waitForIdle();

        // need to be adapted for new menu
        App.getLogic().setMode(main, Mode.MODE_EASYEDIT); // start from a known state

        try {
            lock.click();
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        assertTrue(!logic.isLocked());

        assertEquals(Mode.MODE_EASYEDIT, logic.getMode()); // start with this and cycle through the modes

        // check simple actions menu has the expected content
        TestUtils.clickSimpleButton(device);
        assertFalse(TestUtils.findText(device, false, main.getString(R.string.menu_add_node_address)));
        TestUtils.clickAt(device, 200, 200); // make menu go away

        try {
            TestUtils.longClick(device, lock);
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        TestUtils.clickText(device, false, main.getString(R.string.mode_tag_only), true, false);
        assertEquals(Mode.MODE_TAG_EDIT, logic.getMode());

        try {
            TestUtils.longClick(device, lock);
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        TestUtils.clickText(device, false, main.getString(R.string.mode_address), true, false);
        assertEquals(Mode.MODE_ADDRESS, logic.getMode());
        TestUtils.clickSimpleButton(device);
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.menu_add_node_address)));
        TestUtils.clickAt(device, 200, 200); // make menu go away

        try {
            TestUtils.longClick(device, lock);
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        TestUtils.clickText(device, false, main.getString(R.string.mode_indoor), true, false);
        assertEquals(Mode.MODE_INDOOR, logic.getMode());

        try {
            TestUtils.longClick(device, lock);
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        TestUtils.clickText(device, false, main.getString(R.string.mode_correct), true, false);
        assertEquals(Mode.MODE_CORRECT, logic.getMode());

        try {
            TestUtils.longClick(device, lock);
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        TestUtils.clickText(device, false, main.getString(R.string.mode_easy), true, false);
        assertEquals(Mode.MODE_EASYEDIT, logic.getMode());
    }
}