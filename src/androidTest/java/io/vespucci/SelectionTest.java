package io.vespucci;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import io.vespucci.R;
import io.vespucci.LayerUtils;
import io.vespucci.prefs.AdvancedPrefDatabase;
import io.vespucci.prefs.Preferences;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class SelectionTest {

    Context              context = null;
    AdvancedPrefDatabase prefDB  = null;
    Main                 main    = null;
    UiDevice             device  = null;
    Map                  map     = null;
    Logic                logic   = null;

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
        map = main.getMap();
        logic = App.getLogic();
        map.setPrefs(main, prefs);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.loadTestData(main, "test1.osm");
        TestUtils.stopEasyEdit(main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        TestUtils.stopEasyEdit(main);
        TestUtils.zoomToNullIsland(App.getLogic(), map);
    }

    /**
     * Show the disambiguation menu on a long click when screen is locked
     */
    @Test
    public void longClickLocked() {
        map.getDataLayer().setVisible(true);
        TestUtils.lock(device);
        TestUtils.sleep(2000);
        TestUtils.longClickAtCoordinates(device, map, 8.3853731, 47.3897688, true);
        TestUtils.clickAwayTip(device, main);
        assertTrue(TestUtils.findText(device, false, "#2205375916", 5000, true));
        assertTrue(TestUtils.clickTextContains(device, false, "#2205375916", true));
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.element_information)));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.Done), true));
    }
}
