package io.vespucci;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import io.vespucci.App;
import io.vespucci.LayerUtils;
import io.vespucci.Logic;
import io.vespucci.Main;
import io.vespucci.Map;
import io.vespucci.osm.ViewBox;
import io.vespucci.prefs.AdvancedPrefDatabase;
import io.vespucci.prefs.Preferences;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class CoordinatesOLCTest {

    public static final int TIMEOUT    = 90;
    MockWebServerPlus       mockServer = null;
    Context                 context    = null;
    AdvancedPrefDatabase    prefDB     = null;
    Main                    main       = null;
    UiDevice                device     = null;
    Map                     map        = null;
    Logic                   logic      = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        main = mActivityRule.getActivity();
        Preferences prefs = new Preferences(context);
        LayerUtils.removeImageryLayers(context);
        main.getMap().setPrefs(main, prefs);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        logic = App.getLogic();
        logic.deselectAll();
        TestUtils.stopEasyEdit(main);
        map = logic.getMap();
        logic.updateStyle();
        map.getDataLayer().setVisible(true);
        map.invalidate();
        TestUtils.unlock(device);
        device.waitForWindowUpdate(null, 2000);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        logic.deselectAll();
        TestUtils.zoomToNullIsland(logic, map);
    }

    /**
     * Go to a coordinate pair
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void coordinates() {
        Assert.assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/menu_gps", true));
        Assert.assertTrue(TestUtils.clickText(device, false, "Go to coordinates", true, false));
        UiObject coordEditText = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/text_line_edit"));
        try {
            coordEditText.click();
            coordEditText.setText("0.1 / 0.2");
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
        TestUtils.clickButton(device, "android:id/button1", true);
        ViewBox box = map.getViewBox();
        double[] center = box.getCenter();
        Assert.assertEquals(0.2D, center[0], 0.01);
        Assert.assertEquals(0.1D, center[1], 0.01);
    }

    /**
     * Search for multiple objects
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void OLC() {
        Assert.assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/menu_gps", true));
        Assert.assertTrue(TestUtils.clickText(device, false, "Go to coordinates", true, false));
        UiObject coordEditText = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/text_line_edit"));
        try {
            coordEditText.click();
            coordEditText.setText("6PH57VP3+PR6");
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
        TestUtils.clickButton(device, "android:id/button1", true);
        ViewBox box = map.getViewBox();
        double[] center = box.getCenter();
        Assert.assertEquals(103.85452D, center[0], 0.01);
        Assert.assertEquals(1.28679D, center[1], 0.01);
    }
}
