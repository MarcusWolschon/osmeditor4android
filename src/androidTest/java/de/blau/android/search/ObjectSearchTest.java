package de.blau.android.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
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
import de.blau.android.util.FileUtil;
import okhttp3.HttpUrl;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ObjectSearchTest {

    public static final int TIMEOUT = 90;
    Context                 context = null;
    AdvancedPrefDatabase    prefDB  = null;
    Main                    main    = null;
    UiDevice                device  = null;
    Map                     map     = null;
    Logic                   logic   = null;
    Preferences             prefs   = null;

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
        prefs = new Preferences(context);
        LayerUtils.removeImageryLayers(context);
        main.getMap().setPrefs(main, prefs);

        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        logic = App.getLogic();
        logic.deselectAll();
        logic.setPrefs(prefs);
        logic.getMap().setPrefs(main, prefs);
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
        TestUtils.zoomToNullIsland(logic, map);
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
            fail(e.getMessage());
        }
        TestUtils.clickButton(device, "android:id/button1", true);
        TestUtils.findText(device, false, main.getString(R.string.actionmode_wayselect), 5000);
        List<OsmElement> selected = logic.getSelectedElements();
        assertEquals(1, selected.size());
        assertTrue(selected.get(0) instanceof Way);
        assertEquals(210558045L, selected.get(0).getOsmId());
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
            fail(e.getMessage());
        }
        UiObject searchEditText = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/text_line_edit"));
        try {
            searchEditText.click();
            searchEditText.setText("\"addr:str.et\"=Kirchstra.*se \"addr:hous.*umber\"=4");
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        TestUtils.clickButton(device, "android:id/button1", true);
        TestUtils.findText(device, false, main.getString(R.string.actionmode_wayselect), 5000);
        List<OsmElement> selected = logic.getSelectedElements();
        assertEquals(1, selected.size());
        assertTrue(selected.get(0) instanceof Way);
        assertEquals(210558045L, selected.get(0).getOsmId());
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
            fail(e.getMessage());
        }
        TestUtils.clickButton(device, "android:id/button1", true);
        TestUtils.findText(device, false, main.getString(R.string.actionmode_multiselect), 5000);
        List<OsmElement> selected = logic.getSelectedElements();
        assertEquals(4, selected.size());
        for (OsmElement e : selected) {
            assertTrue(e instanceof Way);
            assertTrue(e.hasTag(Tags.KEY_BUILDING, "residential"));
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
            fail(e.getMessage());
        }
        TestUtils.clickButton(device, "android:id/button1", true);
        TestUtils.findText(device, false, main.getString(R.string.actionmode_multiselect), 5000);
        List<OsmElement> selected = logic.getSelectedElements();
        assertEquals(9, selected.size());
        for (OsmElement e : selected) {
            assertTrue(e instanceof Way);
            assertTrue(e.hasTag(Tags.KEY_HIGHWAY, "residential"));
        }
    }

    /**
     * Example with overpass
     */
    @Test
    public void overpass() {
        final String fileName = "query.overpass";
        MockWebServerPlus mockServer = new MockWebServerPlus();
        try {
            HttpUrl mockBaseUrl = mockServer.server().url("/");
            System.out.println("mock overpass api url " + mockBaseUrl.toString()); // NOSONAR
            prefs.setOverpassServer(mockBaseUrl.toString());
            mockServer.enqueue("overpass");

            TestUtils.clickOverflowButton(device);
            TestUtils.clickText(device, false, main.getString(R.string.search_objects_title), true, false);
            UiObject searchEditText = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/text_line_edit"));
            try {
                searchEditText.click();
                searchEditText.setText("highway=residential inview");
            } catch (UiObjectNotFoundException e) {
                fail(e.getMessage());
            }
            TestUtils.clickButton(device, "android:id/button2", true);
            assertTrue(TestUtils.findText(device, false, main.getString(R.string.overpass_console), 5000));
            TestUtils.clickButton(device, "android:id/button1", true);
            assertTrue(TestUtils.clickText(device, false, main.getString(R.string.save_code), true));
            TestUtils.selectFile(device, main, null, fileName, true, true);
            TestUtils.clickButton(device, "android:id/button1", true);
            assertTrue(TestUtils.clickText(device, false, main.getString(R.string.load_code), true));
            TestUtils.selectFile(device, main, null, fileName, true);
            TestUtils.clickButton(device, "android:id/button2", false);
            assertTrue(TestUtils.findText(device, false, main.getString(R.string.overpass_query_would_overwrite), 5000));
            assertTrue(TestUtils.clickText(device, false, main.getString(R.string.merge_result), false));
            TestUtils.clickButton(device, "android:id/button2", false);
            assertTrue(TestUtils.findText(device, false, "Downloaded 103", 5000));
        } finally {
            try {
                mockServer.server().close();
            } catch (IOException e) {
                // Ignore
            }
            try {
                File savedFile = new File(FileUtil.getPublicDirectory(), fileName);
                savedFile.delete();
            } catch (IOException e) {
                fail(e.getMessage());
            }
        }
    }

    /**
     * Example with overpass
     */
    @Test
    public void overpassReFilter() {
        App.getDelegator().reset(true);
        MockWebServerPlus mockServer = new MockWebServerPlus();
        try {
            HttpUrl mockBaseUrl = mockServer.server().url("/");
            System.out.println("mock overpass api url " + mockBaseUrl.toString()); // NOSONAR
            prefs.setOverpassServer(mockBaseUrl.toString());
            mockServer.enqueue("overpass-2");

            TestUtils.clickOverflowButton(device);
            TestUtils.clickText(device, false, main.getString(R.string.search_objects_title), true, false);
            UiObject searchEditText = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/text_line_edit"));
            try {
                searchEditText.click();
                searchEditText.setText("type:way \"addr:street\"=Bremgartnerstrasse \"addr:housenumber\"=15 in Dietikon");
            } catch (UiObjectNotFoundException e) {
                fail(e.getMessage());
            }
            TestUtils.clickButton(device, "android:id/button2", true);
            assertTrue(TestUtils.findText(device, false, main.getString(R.string.overpass_console), 5000));
            assertTrue(TestUtils.clickText(device, false, main.getString(R.string.select_result), false));
            TestUtils.clickButton(device, "android:id/button2", false);
            assertTrue(TestUtils.findText(device, false, "Downloaded 63", 5000));
            TestUtils.clickButton(device, "android:id/button3", false);
            TestUtils.findText(device, false, main.getString(R.string.actionmode_multiselect), 5000);
            List<OsmElement> selected = logic.getSelectedElements();
            assertEquals(2, selected.size());
        } finally {
            try {
                mockServer.server().close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
}
