package de.blau.android;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Instrumentation;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import de.blau.android.prefs.API;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.prefs.API.AuthParams;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class SelectionTest {

    Context                 context = null;
    AdvancedPrefDatabase    prefDB  = null;
    Main                    main    = null;
    UiDevice                device  = null;
    Map                     map     = null;
    Logic                   logic   = null;
    private Instrumentation instrumentation;
    private Preferences     prefs;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        device = UiDevice.getInstance(instrumentation);
        context = instrumentation.getTargetContext();
        main = mActivityRule.getActivity();
        prefDB = new AdvancedPrefDatabase(context);
        prefDB.deleteAPI("Test");
        prefDB.addAPI("Test", "Test", "", null, null, new AuthParams(API.Auth.BASIC, "user", "pass", null, null), false);
        prefDB.selectAPI("Test");
        prefs = new Preferences(context);
        prefs.setAutolockDelay(300000L);
        main.updatePrefs(prefs);
        LayerUtils.removeImageryLayers(context);
        map = main.getMap();
        map.setPrefs(main, prefs);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        logic = App.getLogic();
        logic.deselectAll();
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
        TestUtils.loadTestData(main, "test1.osm");
        TestUtils.stopEasyEdit(main);
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

    /**
     * 
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void selectWayWithoutSubareas() {
        TestUtils.loadTestData(main, "test4.osm");
        TestUtils.stopEasyEdit(main);
        map.getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.clickAtCoordinates(device, map, 6.4868802, 46.5014693, true);
        TestUtils.clickAwayTip(device, context);
        //
        assertFalse(TestUtils.findText(device, false, "↘ District de Morges"));
    }

    /**
     * 
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void selectWayWithSubareas() {
        TestUtils.loadTestData(main, "test4.osm");
        TestUtils.stopEasyEdit(main);
        prefs.setIgnoreSubAreas(false);
        App.getLogic().setPrefs(prefs);
        map.getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.clickAtCoordinates(device, map, 6.4868802, 46.5014693, true);
        TestUtils.clickAwayTip(device, context);
        //
        assertTrue(TestUtils.findText(device, false, "↘ District de Morges"));
    }
}
