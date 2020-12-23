package de.blau.android.search;

import java.util.ArrayList;
import java.util.List;

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
import de.blau.android.App;
import de.blau.android.LayerUtils;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.TestUtils;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ObjectSearchTest {

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
        TestUtils.loadTestData(main, "test2.osm");
        TestUtils.stopEasyEdit(main);
        map = logic.getMap();
        map.getViewBox().fitToBoundingBox(map, map.getDataLayer().getExtent());
        logic.updateStyle();
        map.getDataLayer().setVisible(true);
        main.runOnUiThread(() -> map.invalidate());
        TestUtils.unlock(device);
        logic.setLastObjectSearches(new ArrayList<>());
        device.waitForWindowUpdate(null, 2000);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        logic.deselectAll();
        TestUtils.zoomToLevel(device, main, 18);
    }

    /**
     * Search for a single object
     */
    @Test
    public void single() {
        TestUtils.clickOverflowButton(device);
        TestUtils.clickText(device, false, main.getString(R.string.search_objects_title), true, false);
        UiObject searchEditText = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/text_line_edit"));
        try {
            searchEditText.click();
            searchEditText.setText("\"addr:street\"=Kirchstrasse \"addr:housenumber\"=4");
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
        TestUtils.clickButton(device, "android:id/button1", true);
        device.waitForWindowUpdate(null, 500);
        List<OsmElement> selected = logic.getSelectedElements();
        Assert.assertEquals(1, selected.size());
        Assert.assertTrue(selected.get(0) instanceof Way);
        Assert.assertEquals(210558045L, selected.get(0).getOsmId());
    }

    /**
     * Search for a single object with a regexp
     */
    @Test
    public void singleWithRegexp() {
        TestUtils.clickOverflowButton(device);
        TestUtils.clickText(device, false, main.getString(R.string.search_objects_title), true, false);
        UiObject checkbox = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/checkbox"));
        try {
            checkbox.click();
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
        UiObject searchEditText = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/text_line_edit"));
        try {
            searchEditText.click();
            searchEditText.setText("\"addr:str.et\"=Kirchstra.*se \"addr:hous.*umber\"=4");
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
        TestUtils.clickButton(device, "android:id/button1", true);
        device.waitForWindowUpdate(null, 500);
        List<OsmElement> selected = logic.getSelectedElements();
        Assert.assertEquals(1, selected.size());
        Assert.assertTrue(selected.get(0) instanceof Way);
        Assert.assertEquals(210558045L, selected.get(0).getOsmId());
    }

    /**
     * Search for multiple objects
     */
    @Test
    public void multiple() {
        TestUtils.clickOverflowButton(device);
        TestUtils.clickText(device, false, main.getString(R.string.search_objects_title), true, false);
        UiObject searchEditText = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/text_line_edit"));
        try {
            searchEditText.click();
            searchEditText.setText("\"addr:housenumber\"=4");
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
        TestUtils.clickButton(device, "android:id/button1", true);
        device.waitForWindowUpdate(null, 500);
        List<OsmElement> selected = logic.getSelectedElements();
        Assert.assertEquals(4, selected.size());
        for (OsmElement e : selected) {
            Assert.assertTrue(e instanceof Way);
            Assert.assertTrue(e.hasTag(Tags.KEY_BUILDING, "residential"));
        }
    }

    /**
     * Preset match
     */
    @Test
    public void preset() {
        TestUtils.clickOverflowButton(device);
        TestUtils.clickText(device, false, main.getString(R.string.search_objects_title), true, false);
        UiObject searchEditText = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/text_line_edit"));
        try {
            searchEditText.click();
            searchEditText.setText("preset:\"Highways|Streets|Residential\"");
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
        TestUtils.clickButton(device, "android:id/button1", true);
        device.waitForWindowUpdate(null, 500);
        List<OsmElement> selected = logic.getSelectedElements();
        Assert.assertEquals(9, selected.size());
        for (OsmElement e : selected) {
            Assert.assertTrue(e instanceof Way);
            Assert.assertTrue(e.hasTag(Tags.KEY_HIGHWAY, "residential"));
        }
    }
}
